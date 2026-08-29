# Spark 性能基线与已实施优化

> 记录 eyelib 渲染/重载热点的 spark profiler 实测基线、已合入的优化及其验证证据。所有数据由 spark profiler 采样得来,可通过 spark URL 在线复现火焰图。
> 配套参考:[spark-profiling-recipe.md](spark-profiling-recipe.md)(spark 集成与采集配方)。

## 测试环境

- MC 1.20.1 Forge 47.1.3,Java 17,Stonecutter active=1.20.1
- eyelib dev build,负载:Actions-and-Stuff mcpack(113 实体定义)
- 采样方式:spark Java sampler(async-profiler 在 ModDevGradle dev JVM 不可用,spark 自动 fallback)

## 已合入的优化

三项优化均已合入主线,经优化后 profiler 复测决定性验证。

### Opt1 · findField 消除异常控制流

- **文件**:`src/main/java/io/github/tt432/eyelib/molang/mapping/api/MolangMappingTree.java`
- **根因**:`findField` 循环用 `Class.getField(name)` + `catch NoSuchFieldException`,字段不存在是常态(每帧大量 `q.variant` 等查询),每次失败触发 `fillInStackTrace`(JVM 遍历调用栈),占稳态渲染 76% CPU。
- **方案**:`Node` 增 `cachedFields` Map,`addNode` 注册类时预建(`getFields()` 索引),`findField` 改为 `Map.get` 返回 null,永不抛异常。
- **语义保持**:多 MolangClass 注册到同一 Node 时,`putIfAbsent` 保持"先注册优先"语义;fieldName 大小写敏感(JVM `getField` 原生行为),缓存 key 不 toLowerCase。

### Opt2 · Pattern 替换为 startsWith 语义

- **文件**:`src/main/java/io/github/tt432/eyelib/client/entity/RenderControllerRuntime.java`
- **根因**:`setup` 在三层嵌套循环里对 part_visibility 的每个 key 反复 `Pattern.compile(k.replace("*", ".*"))`,占稳态渲染 8.9% CPU。
- **方案**:内联 `startsWith` 逻辑(`*` 单独 = 全部、`xxx*` = 前缀、无 `*` = 精确),与同项目 `RenderControllerEntry.matchBonePattern` 的标准实现语义对齐,彻底消除 Pattern.compile 及 matcher 分配。

### Opt3 · Registry.putAll 批量化

- **文件**:`util/manager/ManagerEventPublisher.java`、`util/manager/ManagerEventPublishBridge.java`、`bridge/client/manager/ForgeManagerEventPublisher.java`、`bridge/event/ManagerReplacedEvent.java`(新增)、`util/repository/Repository.java`、`util/registry/Registry.java`、`client/loader/BedrockAddonRuntimeBridge.java`
- **根因**:`Registry.put` 每次 copy-on-write 全量复制快照(O(N²))+ 发逐条事件(Forge `doCastFilter` 占重载 47.8% CPU + 149MB 堆)。`BedrockAddonRuntimeBridge` 对材质/渲染控制器逐条 put,是仅有的两处未批量化热点。
- **方案**:新增 `ManagerReplacedEvent` + `publishManagerReplaced`,`Registry` 覆盖 `putAll`(一次 copy-on-write + 一次事件),bridge 收集后批量 putAll。
- **事件订阅者核实**(实施前已确认安全):3 个 `ManagerEntryChangedEvent` 订阅者均只关心 `ModelManager` / 动画管理器,对材质/RC 的事件零依赖。

### Opt4 · ModelComponent 材质解析缓存

- **文件**:`capability/component/ModelComponent.java`、`bridge/material/RenderTypeResolver.java`
- **根因**:`getRenderType`/`isSolid`/`usesColorMask` 三方法每帧每实体各自 `MaterialManager.INSTANCE.all()` → `BrMaterialResolver.find`(O(n)×2) → `RenderTypeResolver.resolve`(内含 `BrMaterialResolver.resolve` 继承链归并)。同一 entry 每帧被 find 3 次 + resolve 3 次,占稳态渲染 ~2.5% (1484ms)。
- **方案**:ModelComponent 缓存 `cachedEntry` + `cachedMaterial`(ResolvedBrMaterial),按 `matMap == matMapRef` identity 失效。资源重载时 `Registry` snapshot 原子替换 → map identity 变化 → 缓存自动失效。RenderTypeResolver 新增接受 ResolvedBrMaterial 的重载,避免内部重复 resolve。
- **语义保持**:resolve 成功路径复用缓存;resolve 异常路径走旧 fallback;entry==null 路径惰性缓存 fallback。
- **验证**:ModelComponent 路径 find/resolve 从 ~1484ms 降到 12ms,每实体 find 成本下降 36%。

### Opt5 · BrMaterialResolver.resolve 全局缓存

- **文件**:`material/material/BrMaterialResolver.java`
- **根因**:Opt4 覆盖 ModelComponent 路径后,RenderControllerEntry 路径(isAlphaTest/usesColorMask)仍每帧重复调 resolve。resolve 是纯函数,但内部 collectChain 走继承链 → 每层 findBase → find(O(n)×2),且每次分配 ArrayList/LinkedHashSet/ResolvedBrMaterial record。find 的 1064ms self-time 全在继承链递归内部。
- **方案**:BrMaterialResolver 增 `static volatile` 缓存:`IdentityHashMap<BrMaterialEntry, ResolvedBrMaterial>`,按 matMap identity 失效(与 Opt4 同机制)。resolve 命中缓存返回,miss 调 computeResolve(原 resolve 体)并 put。BrMaterialEntry 是 record(不可变),缓存安全。
- **语义保持**:异常路径(循环继承 IllegalStateException)不缓存,直接传播;非线程安全 IdentityHashMap 但 Render thread 单线程访问,重载不调 resolve。
- **验证**:find self-time 1064→480ms(-55%),resolve self-time 120→0ms(-100%),RenderControllerEntry 路径 resolve total 990→4ms(-99%),resolveCache 18 entries 全命中。

### Opt6 · Molang 求值链分配消除

- **文件**:`molang/mapping/api/VariantSelector.java`、`molang/compiler/MolangRuntimeSupport.java`
- **根因**:稳态渲染 Molang 求值链占 ~4600ms(7.7%)。三处分配热点:① `CompileContext.defaults()` 每次新建 record + `Set.of()`,但只用 mappingTree 字段;② `selectQueryVariant` 5 次连续 stream filter pipeline 各分配 ArrayList;③ `computeAvailableHostRoles` 每次 `EnumSet.noneOf` + add,稳态时结果固定。
- **方案**:
  - Opt-B:resolveCall/resolveMemberAccess 直接引用 `MolangMappingRegistries.mappingTree()`(稳定 volatile),消除 CompileContext.defaults() 分配。
  - Opt-A:selectQueryVariant 5 步 stream → 单次 for 遍历 + (specificity, priority) 在线打分,消除 5 个 ArrayList。
  - Opt-C:computeAvailableHostRoles 改返回两个不可变常量 Set(HOST_ROLES_FULL/MINIMAL)。
- **语义保持**:selectQueryVariant 等价语义(最高 specificity 中最高 priority 的最后一个候选);hostRoles 只读 contains,不可变 Set 安全。
- **验证**:MolangRuntimeSupport self 1844→556ms(-70%),VariantSelector self 1648→172ms(-90%),selectQueryVariant 几乎归零。

### Opt7 · molang `this` 绑定字段化

- **文件**:`molang/MolangScope.java`、`molang/MolangValue3.java`、`animation/bedrock/BrBoneKeyFrame.java`、`molang/compiler/MolangBytecodeEmitter.java`
- **根因**:关键帧逐轴求值前 `scope.set("this", v)` 走 `MolangFloat.valueOf`(非 0/1 即 new)+ `ConcurrentHashMap.put`,96 实体约 21,600 次 put + 装箱/帧。
- **方案**:MolangScope 增加 `float thisValue` + `thisSet` 专用字段,`setThis(float)` 写入;`getThis()` 沿 parent 链读取(语义与原 cache 路径一致),未绑定时回退 `get("this")` 兑底。字节码发射器 `BoundThisExpr` 改发 `getThis()` 调用。
- **语义保持**:全部 `this` 写入点均为标量 float(grep 验证);单测 `this`=0(无动画上下文)行为不变。

### Opt8 · 动画空通道短路

- **文件**:`animation/bedrock/BrClipExecutor.java`、`animation/bedrock/BrBoneAnimation.java`、`util/collection/ImmutableFloatTreeMap.java`
- **根因**:无关键帧的通道(position/scale 常为空)仍执行 12 行 `this*` 计算(含 bind 读取)+ 2 次 TreeMap 查找后返回 null。
- **方案**:BrBoneAnimation 增加 `hasRotation/hasPosition/hasScale`(构造后资源不可变),BrClipExecutor 按通道守卫,全空骨骼连同 bind 查询一起跳过。

### Opt9 · MolangMappingTree 名称解析缓存

- **文件**:`molang/mapping/api/MolangMappingTree.java`
- **根因**:`findField`/`findMethod`/`selectQueryVariant` 每次调用做 `toLowerCase` + findNode 遍历 + 变体选择,结果只取决于注册表内容。spark 显示 `StringLatin1.toLowerCase` self 4708ms/60s。
- **方案**:三级 CHM 缓存(值为 `Optional`,`empty`=已解析为 null),`addNode`/`clear`/`normalizeAndValidatePublicationOrder` 时整体失效。selectQueryVariant 键直接复用调用方集合(callShape 为调用方新造、hostRoles 为共享不可变常量)。
- **验证**:`StringLatin1.toLowerCase` self 4708→28ms(-99%),`VariantSelector.selectQueryVariant` self 180→0ms。
- **教训**:首版缓存键用 `List.copyOf`/`Set.copyOf` 防御性拷贝,每次 resolveCall 多 3 次分配,fbo render_work P50 反而 +1.8ms;去掉拷贝后恢复。缓存键设计不得引入超过被缓存计算的成本。

## 验证证据(Opt7-9,2026-07-25)

**clientBenchmark A-B-A 交错(同环境,mixed n96,render_work)**:交错是必需的——同日两次 baseline 差异达 20%(远控 IDD GPU 状态漂移),跨 run 直接对比会得出完全相反的结论。

| 轮次 | fbo P50 | world P50 | fbo P99 | world P99 |
|---|---:|---:|---:|---:|
| 优化后 run1 | 8.43 | 47.57 | 15.42 | 76.42 |
| baseline(同环境) | 10.71 | 59.80 | 15.32 | 91.66 |
| 优化后 run2 | 8.18 | 48.71 | 11.52 | 71.93 |

- **fbo render_work P50: 10.71 → 8.31 ms(均值) = -22.4%**
- **world render_work P50: 59.80 → 48.14 ms(均值) = -19.5%**
- spark 前后链接:before https://spark.lucko.me/SyEtXi9c7c after https://spark.lucko.me/PP3G4voelR(仅作方法级归因,总量不可跨 run 比——60s 窗口 self-time 受帧率与环境影响,见 方法论局限 4)


### T2 · 稳态渲染(60s)

| 热点 | 优化前 self | 优化后 self | 结论 |
|---|---|---|---|
| `Throwable.fillInStackTrace` | 76% (45512ms) | **0.00%** | Opt1 完全消除异常控制流 |
| `java.util.regex.Pattern.*` | 8.9% (5312ms) | **0.05%** | Opt2 几乎消除(残留为非 RCR 路径) |
| `MolangMappingTree.findField` | 高(异常沿栈摊分) | **0.00%** (incl 3.3%) | Map.get 极快 |

- 优化前:https://spark.lucko.me/YsDkEySi5t(59 slime)
- 优化后:https://spark.lucko.me/QgxEIFFBhJ(83 实体,eyelib 接管率 100%)
- 优化后 TOP15 转为 OpenGL 驱动 + stream 开销,均为渲染固有成本。

### T2b · 稳态渲染材质/Molang 求值链(第二轮, 60s 采样)

逐迭代追踪每个优化的方法级 self-time。实体集合因采样时场景自然演化而不同(基线 sheep+slime,中后期多种怪物),绝对值不可跨采样直接比;**方法级 self-time 归一化到每实体后可比**。

| 热点(self-time) | 基线 (SubTask1) | Opt4 后 (SubTask2) | Opt5 后 (SubTask3) | Opt6 后 (SubTask5) |
|---|---|---|---|---|
| `BrMaterialResolver.find` | 1484ms | 1064ms | 480ms | 212ms |
| `BrMaterialResolver.resolve` | (含于 find) | 120ms | **0ms** | **0ms** |
| `MolangRuntimeSupport` | 1948ms | 1844ms | 1844ms | **556ms** |
| `VariantSelector.selectQueryVariant` | 176ms | 1648ms | 1648ms | **~20ms** |
| `Molang$Expr$*.evaluate` | 2124ms | 1472ms | 1140ms | 588ms |
| ModelComponent 路径 find | (in 1484) | **12ms** | **0ms** | **0ms** |
| RenderControllerEntry 路径 resolve total | — | ~990ms | **4ms** | — |

- 基线:https://spark.lucko.me/JxI1zQR418(83 实体,sheep+slime,eyelib 接管 100%)
- Opt4 后:https://spark.lucko.me/FBYXqDfIjr(101 实体)
- Opt5 后:https://spark.lucko.me/KqVu8m2XSn(101 实体)
- Opt6 后:https://spark.lucko.me/fg0vFLPZAK(83 实体)

每实体归一化(控制实体数差异):基线 eyelib self 10136ms/83 = 122ms/实体 → Opt6 后 2964ms/83 = 36ms/实体(**-71%**)。

> Opt6 后 TOP self-time 转为非 eyelib 成本(LinkedHashMap.forEach / Int2ObjectOpenHashMap / HashMap.putVal / GL 驱动),eyelib 最大残留热点 `Expr.evaluate`(字节码动态生成类求值)接近 Molang 固有成本,ROI 不足继续优化。

### T3 · 资源重载(90s)

| 热点 | 优化前 total | 优化后 total | 结论 |
|---|---|---|---|
| `Registry.put` | 48.3% (43484ms) | **0.04%** | Opt3 材质/RC 不再逐条 put |
| `EventBus.doCastFilter` | 47.8% (42988ms) | **0.03%** | 不再 N×ManagerEntryChangedEvent |
| `BedrockAddonRuntimeBridge.replaceFromResourcePack` | 54.6% | **0.11%** | 批量收集 + putAll |
| `MolangMappingTree.findField` | 4.9% (4420ms) | **2.71%** (self 0%) | Opt1 在 reload 也生效 |

- 优化前:https://spark.lucko.me/EA6AAqdMUF
- 优化后:https://spark.lucko.me/hMYwoc7nLo

> **T3 局限**:优化前后 T3 均只采 Render thread(reload 大量工作发生在 worker 线程:codec 解析、.mcpack 解压、SimpleJsonResourceReloadListener),数据**系统性低估**真实 reload 开销。Opt3 的决定性证据是 `Registry.put` / `doCastFilter` 的归因下降(eyelib 调用栈干净),不受此局限影响。

### T4 · 堆摘要(佐证)

| 对象 | 优化前 | 优化后 | 变化 |
|---|---|---|---|
| Forge EventBus cast Lambda(两类合计) | 149MB | 91.7MB | size -38%,实例 -35% |
| `ManagerEntryChangedEvent` / `ManagerReplacedEvent` | — | 0(不可见) | 生命周期短,post 后即 GC |

- 优化前:https://spark.lucko.me/8RhM134XVh
- 优化后:https://spark.lucko.me/txiWm3qS1e
### Opt10 · molang 零参解析绑定缓存（2026-08-28）

- **文件**:`molang/compiler/MolangRuntimeSupport.java`、`molang/mapping/api/MolangMappingTree.java`
- **根因**:Opt6/Opt9 后每次零参 query 求值仍串行 5 次哈希查找——scope.get → findField(缓存) → findMethod(缓存) → selectQueryVariant(缓存,且每次 `new QueryVariantKey` record 分配+hash) → methodHandleOf(缓存)，外加 invokeMethod 的 varargs 槽位重检测与 Object[] 全量装配。
- **方案**:RuntimeSupport 增加 `ZERO_ARG_CACHE`：按 (名称 × host 有无 × 注册表纪元) 缓存完整解析结果 ZeroArgBinding——kind(FIELD/METHOD/NONE) + 预建 MethodHandle + 参数模板（可见参数缺省值与空 varargs 数组预填，host/engine 槽每次现填）。失效检测 = 树引用同一性 + `MolangMappingTree.epoch()`（addNode/clear/normalize 自增）一次 volatile 读，免回调节。键为字节码 ldc 的编译期常量字符串，无防御性拷贝（Opt9 教训）。resolveCall 零参路径共用；非零参路径不变。
- **语义保持**:scope.get 覆盖检查仍在缓存之前（脚本赋值优先）；FIELD/NONE 对 resolveCall 视为未解析（warn+scope.get 兜底不变）；selectQueryVariant 内部首步即 findMethod，先查 findMethod 等价（源码实证）；变体歧义 catch→NONE 与原路径一致。
- **踩坑**:同 publication signature 的变体属注册冲突（静默丢弃）——host 回退变体必须 varargs 形态（同 DefaultRoleFallbackVariantMapping 先例）。
- **诊断开关**:`-Deyelib.molang.zeroArgBinding=false` 回退逐次解析旧路径（benchmark A/B 用，已接入 runClientBenchmark 转发）。
- **验证**:1.20.1 单测全绿（含新契约测试 MolangRuntimeSupportZeroArgBindingTest：epoch 失效重解析/scope 覆盖优先/FULL-MINIMAL 槽独立）；1.20.1/1.21.1 编译绿。benchmark 交错 A/B（fbo slime n384，fresh JVM，30s 测量）：ON 113.57/125.34 vs OFF 105.57/104.29 FPS → **+13.8%**（两轮同向 +7.6%/+20.2%），渲染零错误。

> **T4 是佐证**:Forge EventBus cast Lambda 是通用机制(所有事件 post 共用,非 eyelib 专属),38% 下降部分归因 Opt3、部分归因采样时机与 GC。Opt3 主证据是 T3 CPU 的干净归因。

### Opt11 · 动画采样查找融合 + 常量关键帧短路（2026-08-28）

- **文件**:`util/collection/ImmutableFloatTreeMap.java`、`animation/bedrock/BrBoneAnimationSampler.java`、`animation/bedrock/BrBoneKeyFrame.java`、`molang/MolangValue.java`、`molang/MolangValue3.java`
- **方案**:①`floorHigherIndices(tick)` 单次二分同时定位 floor/higher（打包 long 返回索引对），平行 `Object[] values` 直取替代 Float2ObjectOpenHashMap 哈希探针；CatmullRom 邻接点由「构造不变量 timestamp==sortedKeys[idx]」索引直取（防御性校验失败回退逐次查找）。②`MolangValue.isConstant()`/MolangValue3.allAxesConstant()：三轴全为编译期常量（常量折叠产物）时采样走预读值，跳过逐轴 `getObject`（clearTempVariables+dispatch）；`setThis` 保留维持 this 残态语义；单侧分支返回新建拷贝（下游可能改向量）。
- **语义论证**:常量求值不读 scope/不写 temp/不抛异常；temp.* 的读者自身先 clearTempVariables，常量短路跳过的 clear 不可观察（唯一观察者是 debug 快照的瞬态展示）。
- **验证**:新契约测试 BrBoneAnimationSamplerOptTest 7 例（fused 索引对全部键集×采样点等价旧逐次查找、线性/CatmullRom/常量混合 this 引用全刻度对齐旧算法 oracle、this 引用不被短路、单侧常量返回独立拷贝、空帧 null）。
- **结果**:**FPS 中性**（fbo slime n384 交错 A/B：ON 127.5/121.7 vs OFF 122.1/124.5，方向不一致）。JFR 归因：融合查找生效（ON 采样器栈内 floorHigherIndices 0 样本 vs OFF floorEntry+higherEntry 27%）；**常量短路对本机 A&S 包无效——其史莱姆关键帧是 molang 动态表达式（单侧分支 100% 走 evalWithThis）**，仅对字面量关键帧的包有效。保留依据：查找融合是严格的更少功 + 契约测试覆盖；常量路径对字面量包（多数 vanilla 风格 BE 动画）有效。
- **诊断开关**:`-Deyelib.anim.sampleOpt=false` 回退旧路径（已接入 runClientBenchmark 转发）。

### Opt12 · 渲染热路径 map 查找消除（2026-08-28）

- **文件**:`bridge/client/render/skinning/adapter/BatchSkinningDispatcher.java`、`animation/bedrock/BrBoneAnimation.java`、`animation/bedrock/BrBoneAnimationSampler.java`
- **根因**（JFR 双样本归因，fbo n384 渲染线程）：①`IdentityHashMap.put` self 8.5%——合批 drain 每帧新建 groupIndex/subIndex 两张 IdentityHashMap；②`Collections$UnmodifiableMap.get` 5.4~7.9%——`BrClipExecutor` 每骨骼每帧 3×hasChannel + 3×channel(name) 字符串键查找。
- **方案**:①drain 分组改 ArrayList 线性扫描引用比较（每帧 distinct RenderType/几何为个位数~数十，扫描成本远低于 map 分配+探针）；②`BrBoneAnimation` record → final class（record 禁止额外表字段），构造期预解析三个编译期通道引用（缺失/空帧归 null），hasRotation/lerpRotation 等改字段读+直采，绕过全部字符串键查找；equals/hashCode/toString 维持原 record 语义。采样器新增通道直采重载。
- **验证**:两版本编译绿、1.20.1 全量单测绿。JFR 复测（同条件 60s profile）：IdentityHashMap.put **8.5%→0.0%**、UnmodifiableMap.get **7.9%→1.2%**（残量为 RegistrySnapshot 等其他来源）、BatchSkinning 链 15.6%→8.7%。
- **结果**:fbo n384 中性（124.8/127.6 vs 基线 121.7~127.5，该场景非渲染线程受限）；**world n384 +5.3%**（41.26/42.36 vs 基线 39.0~40.4，区间不重叠）。相对本轮优化前最初基线（33.4）累计 +25%。
- **教训**:帧率不动的 CPU 优化先查受限线程——JFR 证明工作确实消失后，换 CPU 受限场景（world）再下结论。

### Opt13 · molang 零参调用精确签名 invoker + hasHostContext O(1)（2026-08-28）

- **文件**:`molang/compiler/MolangRuntimeSupport.java`、`molang/MolangScope.java`
- **根因**（JFR 行级归因，world n384 渲染线程）：①`hasHostContext` 每次调用经 `HostContext.get(HOST_PRESENCE_MARKER)` 触发 HashMap entrySet 扫描+迭代器分配（marker 全库无人注册、类型为 Object.class，扫描恒等价于"任一 store 非空"）≈4%；②零参 query 每次调用走 `template.clone()`+`invokeWithArguments(Object[])` 泛型分派（`asSpreader`/`MethodType.replaceParameterTypes`/`Arrays.equals`）≈5% 且逐次分配。
- **方案**:①`hasHostContext`/`computeAvailableHostRoles` 改 `MolangScope.hasAnyHost()`（`!roleStore.isEmpty() || !classStore.isEmpty()`，语义等价论证：Object.class.isInstance 匹配任意非 null 条目，扫描命中 ⟺ 非空；唯一分歧为病态 null put）。②零参绑定解析期组合精确签名 `(MolangScope)MolangObject` MethodHandle：const 槽（可见缺省/空 varargs/无角色槽）insertArguments 绑定、host 槽经 `hostSlot` 过滤器现取（新增 `MolangScope.findHost` 无 Optional 等价查找）、engine 槽 identity(scope)、返回统一 asType 到 Object 后 filterReturnValue 包装——调用点 `invokeExact` 无装箱/无分派/零分配。组合失败回退旧泛型路径并 WARN 可观测。
- **实证缺陷**:filterReturnValue **不做装箱**——primitive 返回直接组合抛 IAE，导致生产全部 query 静默回退旧路径而单测全绿（测试只验结果，回退路径结果同样正确）。教训：快路径必须有「真实生效」断言。修复：返回先 asType 到 Object；新增包私有 `hasComposedInvoker(name, fullHost)` 测试钩子，契约测试对 engine/receiver/varargs/field/混合形态断言 invoker 非 null。
- **验证**:新契约测试 MolangRuntimeSupportInvokerTest 8 例（槽位形态等价、host 逐次现取、异常归 Null、epoch 失效重解析、invoker 生效断言）；其中"必需可见参数零参不可解析"用 stash 对照证实为既有语义（非本次引入）。1.20.1 全量单测绿、1.21.1 编译绿。
- **结果**:**world n384 +7~15%，区间不重叠**（ON 43.54/44.10 vs OFF 38.19/40.56；同构建 `-Deyelib.molang.exactInvoker=false` 对照）。修复后 JFR：零参 invokeWithArguments 清零、hasHostContext 扫描清零、template.clone 清零；benchmark 全程 0 条组合失败警告。
- **诊断开关**:`-Deyelib.molang.exactInvoker=false`（已接入 runClientBenchmark 转发）。
- **余项**:resolveCall 非零参路径（math.* 带参调用）仍走 invokeMethod 泛型分派（~2%）；query 体内 `HostContext.get(HostRole)` 的 isInstance 扫描（~2%，在 MolangBuiltInQuery 查询实现内，非 RuntimeSupport）。

### Opt14 · molang 三小热点打包：非零参 shape 缓存 + HostContext 角色 memo + 采样线性扫描（2026-08-28）

- **文件**:`molang/compiler/MolangRuntimeSupport.java`、`molang/MolangScope.java`、`util/collection/ImmutableFloatTreeMap.java`
- **方案**（三项均承接 Opt13 余项 JFR 归因）:
  1. **非零参解析缓存（shapeCache）**:`selectQueryVariant(名称, 调用形, host 角色集)` 结果只取决于 (名称, 逐参 STRING/NUMBER 形态, host 有无, 注册表纪元)——形态压入 long 键（位 0-5 参数数，位 6+i 第 i 参 STRING 标记，位 63 host 有无），缓存 FunctionInfo（含 null 缺失结果；歧义异常不缓存与原路径逐次重试一致）。参数打包/值转换仍逐次走 invokeMethod（值随求值变化不可缓存）。>55 参回退原路径。
  2. **HostContext 角色 memo（roleMemo）**:`get(HostRole)` 三步解析（精确→roleStore isInstance 扫描→classStore 回退）结果 memo 化；失效用**变更纪元**（不用 clear——并发下 clear+回填竞态会无限期供陈旧值，纪元错位条目下次访问即重解析）。**关键配套**：宿主装配（EntityPortAdapter.putHost）每帧以同一实例重写同角色，put/remove 加幂等短路（同引用 put / 不存在键 remove 不动纪元）——否则纪元每帧颠簸，memo 全灭（首版实证 resolveRole 残留 2.9% 即此因）。
  3. **采样线性扫描（linearScan）**:`floorHigherIndices` 对 ≤16 键数组改 `Float.compare` 顺序循环。`Float.compare` 与 `Arrays.binarySearch(float[],float)` 同一全序（-0.0<0.0、NaN 最大、位级相等才算命中；构造期 keys 同序排序），结果逐位等价（含 NaN/-0.0 tick 病态输入），有穷尽契约测试钉死。
- **验证**:新契约测试 3 类——ImmutableFloatTreeMapLinearScanTest（尺寸 1~32 × NaN/±Inf/±0/键间/越界 tick 对二分 oracle 逐位等价）、MolangScopeRoleMemoTest（精确命中/跨角色 isInstance 回退/class 族耦合失效/单线程 scope 同契约）、MolangRuntimeSupportShapeCacheTest（缓存命中一致、STRING/NUMBER 形态分流——**实证发现：注册键为 (名称, publication signature)，同元数不同参数类型属注册冲突**，故重载测试用不同元数构造、epoch 重建/原地 addNode 失效翻正）。1.20.1/1.21.1 全量单测绿。
- **生效证据**（JFR 修复后，opt14b.jfr（工作现场文件，已清理），渲染线程 368 样本）：selectQueryVariant 0.0%、Arrays.binarySearch 0.0%（原 3.4%）、HostContext$1.get 4.9%→0.8%、resolveRole 2.9%→0.5%。
- **结果**:**world n384 +4.3%，区间不重叠**（ON 46.23/46.59 vs OFF 44.22/44.83；同构建三开关对照）。相对最初基线 33.4 累计约 +39%。
- **诊断开关**:`-Deyelib.molang.shapeCache=false`、`-Deyelib.molang.roleMemo=false`、`-Deyelib.anim.linearScanThreshold=0`（均已接入 runClientBenchmark 转发）。
- **余项**:invokeMethod 参数打包/转换仍逐次（~4%，精确 invoker 化需处理 varargs/逐参转换过滤器，复杂度高收益低）；RenderControllerEntry.setupModel 逐实体逐帧 map 迭代（~7%，新发现）；MolangValue3.getX 叶帧 12.8% 为内联归宿帧（动态表达式字节码本体）。

### Opt15 · setupModel 值相等键缓存：分组 + 基础可见性表跨帧复用（2026-08-28）

- **文件**:`capability/component/RenderControllerComponent.java`（Slot 增缓存）、`client/render/controller/RenderControllerEntry.java`（setupModel 重构）
- **方案**:承接 Opt14 余项 JFR（setupModel ~7%）。键 = (geometry 解析值, 逐材质解析值列表, rcColor 值) 逐帧求值后**值相等比较**；命中则复用派生的 材质名→骨骼集 LinkedHashMap 与每组基础可见性表（Int2BooleanOpenHashMap，putAll 拷贝进组件后逐帧叠加 part_visibility 表达式）。molang 表达式求值本身不缓存（动态性完整保留），消除的是值不变时的 map/集合重建。失效：任一表达式值变化（值比较）或 modelVersion 变化（checkModelVersion 连带清空）。生产时序不变量：allBoneIds/matchBones 恒先于 materialGroups 调用，版本先于缓存稳定。
- **验证**:新契约测试 RenderControllerComponentSlotGroupCacheTest 5 例（命中同实例零重建/color 按值比较/任一键分量变化重建/modelVersion 失效/baseVis 对齐回填）。1.20.1/1.21.1 全量单测绿（26.1.2 预存 fastutil NoSuchMethodError 失败与本次无关）。运行时截图：史莱姆半透明层次、蜘蛛发光红眼、牛正常（compute 合批路径，VARIANTS=10/GEOMETRIES=9 实证生效）。
- **结果**:**1.21.1 world n384 +2.9% 区间不重叠**（ON 59.48/59.10 vs OFF 56.50/57.75）；1.20.1 三轮 ON 42.96/44.41/46.05 vs OFF 41.85/44.52/47.67 完全重叠=中性（该版本同场景方差 ±6% 吞没收益）；26.1.2 中性（瓶颈不在此）。保留：分配/map 构建的真实消除 + 无回归。
- **诊断开关**:`-Deyelib.rc.groupCache=false`（已接入 runClientBenchmark 转发）。
- **余项**:setupModel 内逐帧保留成本 = geometry/材质/color 表达式求值本身 + resolveSlotTextures（texture expr 求值 + clamped 解析）+ ModelComponent 分配；再压需表达式级常量折叠。

### Opt16 · 解析链五件：memberAccess 快路径 + bindBones 缓存 + 动画解析缓存 + Float 缓存 + 短路前移（2026-08-28）

- **文件**:`molang/MolangScope.java`（根覆盖位）、`molang/compiler/MolangRuntimeSupport.java`（resolveMemberAccess 快路径）、`molang/type/MolangFloat.java`（小整数缓存）、`animation/bedrock/BrClipExecutor.java`（空通道短路前移到 getData 前）、`capability/RenderData.java`（bindBones 懒缓存+失效契约）、`client/render/EntityRenderOrchestrator.java`+`AttachableItemRenderSetup.java`+`mixin/client/Item{,InHand}RendererMixin.java`（collectBindBones→bindBones 迁移）、`client/render/sync/ClientRenderSyncService.java`（sync 失效点）、`animation/bedrock/controller/BrControllerStateOwner.java`（解析缓存）+`BrControllerExecutor.java`+`BrAnimationController.java`（7 处解析点迁移）
- **方案**:JFR（1.20.1 world n384，opt16-before.jfr（工作现场文件，已清理））归因：Int2Object find 4.0%（getData computeIfAbsent + collectBindBones 每帧重建）、HashMap.getNode 4.0%（RegistrySnapshot 链 + scope.get）、scope.get 遮蔽检查 ~2%、MolangFloat.valueOf 1.6%。五项：①query/math 根覆盖位（putTracked 置位、粘滞、parent 链检查）跳过恒 miss 的 scope.get——grep 实证 query.*/math.* 无写入点（context.* 有，不走快路径）；②MolangFloat [-16,256] 整数缓存；③空通道骨骼不再创建零值 Entry（读侧等价）；④bindBones 按失效点缓存（变更点全集：setupClientEntity 两 clear + sync replaceModelComponents）；⑤动画名解析缓存（守卫 = animations 实例 identity + registry generation）。
- **验证**:契约测试 4 类（MemberFastPath 遮蔽/struct 置位/parent 链、MolangFloatCache 边界、RenderDataBindBones 失效、BrControllerResolveCache generation/identity/null 缓存）+ 1.20.1 全量 251 类零失败 + 运行时截图正确。
- **结果**:串行单客户端 A/B（并发双客户端会互相污染，已改协议）：1.20.1 ON 47.59 vs OFF 46.74（+1.8%，噪声带内）；1.21.1 ON 71.66 vs OFF 70.40（+1.8%）。生效证据由 Opt17 轮 JFR 复核确认（见下）。
- **诊断开关**:`-Deyelib.molang.memberFastPath=false`、`-Deyelib.anim.controllerResolveCache=false`。

### Opt17 · 表达式级深优化：MemberSite 站点单态分发 + spreader invokeExact + rootKeyOf memo（2026-08-28）

- **文件**:`molang/compiler/MolangRuntimeSupport.java`（MemberSite 站点类 + SPREAD_INVOKERS）、`molang/compiler/MolangBytecodeEmitter.java`（站点字段发射）、`molang/MolangScope.java`（ROOT_KEY_MEMO）
- **方案**:Opt16 后 JFR 新热点：①checkCustomized 2.5%——invokeExact 调用点被全部表达式共享（megamorphic），JIT 无法按站点特化；②Arrays.equals←MethodType.equals 3.8% + asSpreaderChecks 1.3%——invokeMethod 逐次 invokeWithArguments 泛型分派；③substring←rootKeyOf 2.9%——3+ 段名每次分配。对策：①发射器为每个可扁平化成员访问生成实例字段（构造期 newMemberSite 初始化），resolve 内联进表达式类专有的 evaluate → 绑定 invoker 按表达式类单态化；站点状态仅 (tree,epoch,绑定)，scope 相关（覆盖位/host）逐次现查，语义与静态路径一致；②invokeMethod 用解析期组合的 asSpreader+asType+WRAP_RESULT 句柄 invokeExact（开关复用 exactInvoker）；③rootKeyOf 纯函数 CHM memo（仅分配形态）。
- **验证**:契约测试 MolangMemberSiteContractTest 5 例（端到端求值/运行时注册重解析/遮蔽/struct 遮蔽/与静态路径一致性）+ 全量回归绿 + 运行时截图正确。JFR 复核（opt17-after.jfr）：MethodType.equals 3.8%→0、substring 2.9%→0、asSpreader→0、checkCustomized 2.5%→1.0%。
- **结果**:**1.20.1 world n384 +8~13%**（51.41/53.03 vs Opt16 46.7~47.6，区间不重叠）；**1.21.1 +5~9%**（75.33/77.05 vs 70.4~71.7，区间不重叠）。
- **余项**:invokeZeroArgMethod 叶 4.8%（内联归属帧，残余 invokeExact+hostSlot 真实工作）；apply 13.2%/getX 5.1% 为表达式字节码本体；dynamic 关键帧逐帧求值无缓存空间（this 逐轴不同）。
- **基准协议修订**:同版本多实例并发跑 benchmark 会互相污染 CPU（此前并发轮 1.21.1 56 vs 串行 70+），一律串行单客户端。
- **累计**:1.20.1 world n384 从最初 33.4 → 51.4~53.0（约 +54~59%）；1.21.1 → 75.3~77.0。

### Opt18 · map 机制消除：setup 引用锚点 + ModelRuntimeData/HostContext 数组化 + RC 组件值键缓存（2026-08-29）

- **文件**:`animation/AnimationComponent.java`（引用锚点短路）、`animation/ModelRuntimeData.java`（Entry[] 数组化）、`molang/mapping/api/HostRole.java`（驻留+序号）、`molang/MolangScope.java`（HostContext 槽位数组）、`capability/component/RenderControllerComponent.java`（Slot 组件值键缓存）、`client/render/controller/RenderControllerEntry.java`（setupModel 值键流程 + evalTextureLayerPaths/toRenderLocations 拆分）、`client/entity/RenderControllerRuntime.java`（evalPartVisibilityBits）、`client/render/EntityRenderOrchestrator.java`（identicalComponents 恒等跳过 + bindBones 失效收紧）
- **归因**（opt18-tick-before.jfr，1.20.1 world n384，深栈 64）：render 线程 map 查找/迭代机制合计 43.4%（Int2ObjectOpenHashMap.get 8.9% / HashMap.getNode 8.3% / HashIterator 7.3% / StringLatin1.hashCode 4.9% / RegularImmutableMap.get 3.4%）；子树份额 SetupStage 30.8%（每帧全量 setupClientEntity！）/ TickStage 25.1% / renderComponents 32.6%。
- **方案**:
  1. **B AnimationComponent.setup 引用短路**：上次入参引用锚点（lastSetupAnimations/Animate），等值异引用（同步解码拷贝，实证存在于生产路径）采纳为新锚点收敛。修复前 JFR 实证 equals 兜底每帧执行（AnimationComponent.setup self 10%）。
  2. **C ModelRuntimeData 数组化**：骨骼 id 全局稠密（GlobalBoneIdHandler 从 0 递增）→ Entry[] 按需扩容 + bindBones 转 Bone[]，读路径零哈希探测（此前 position/rotation 6.7%）。负 id（空白名 -1 兜底）专用槽保持旧 map 语义。
  3. **D HostRole 驻留 + HostContext 数组**：of(name,type) 全局驻留分配稠密序号 id（注册键=(名称,类型) 语义不变），MolangScope 角色存储/memo 改 Object[]/RoleMemoEntry[] 按 id 索引（HostRole.hashCode+memo probe ~2-3%）。
  4. **A' RC 组件值键缓存**：setupModel 键 = 本帧求值出的全部动态输入（geometry 解析值 + 逐材质值 + 逐组纹理路径 + rcColor + part_visibility 隐藏位集），值 = 组件列表。molang 逐帧求值不变（**关键实证**：A&S 史莱姆 RC 表达式全动态——geometry/textures/materials/part_visibility 均读 variable./query.——编译期常量门在真实资产上永不命中，故用值键而非静态门）；命中消除下游重建（PortResourceLocation.parse/clamped/组件分配/vis 叠加）。part_visibility 求值次数从「每组×层」收敛为 1 次（无副作用表达式等价）。setupClientEntity 增 identicalComponents 恒等跳过（全 RC 命中时跳过 clear/addAll 与 bindBones 失效）。
- **验证**:契约测试 4 类 15 例（Slot 值键 4 + ModelRuntimeData 数组 7 + HostRole 数组 5 + setup 引用 3）+ 1.20.1 全量 1682 例绿 + 1.21.1 全量绿 + 运行时截图正确（史莱姆半透明/蜘蛛红眼）。运行时反射实证：锚点字段 == ce.animations()/s.animate()（引用短路生效）、Slot.cachedComponents 非空且内容等于活跃组件。
- **结果**（world n384，45s 串行协议）:
  - 1.20.1:Opt17 态 51.41/53.03 → **66.52/66.88 区间（约 +26~30%）**；componentCache 开关 A/B 中性（66.52 ON vs 66.88 OFF——求值本身在两条路径都跑，缓存只消下游）。
  - 1.21.1:Opt17 态 75.33/77.05 → **111.29/118.84（约 +44~56%）**；OFF 111.78。
  - JFR 机制证据：AnimationComponent.setup 10.0%→0.5%；setupModel self 85→19 样本（下游重建消除）；命中路径残留 = 逐帧 molang 求值（MolangStruct.get 78 + MolangScope.get 62 + livingFloat 38 + cachedComponentsHit 30 等 ≈ 24% render 线程，BE 语义要求逐帧评估）。
- **累计**:1.20.1 world n384 从最初 33.4 → **66.5~66.9（约 2.0×）**；1.21.1 → **111.3~118.8**。
- **方法论勘误（重大）**:jfr print 默认栈深截断（`...`）曾致 TickStage 被低报为 4.0%；`--stack-depth 64` 深栈下真实份额 25.1%（Opt18 后 38.9%）。SetupStage+TickStage 合计 66~77% render 线程——**TickStage/SetupStage 并行化（此前因错误低报被暂缓）实为最大剩余杠杆**。性能声明必须附深栈证据。
- **诊断开关**:`-Deyelib.rc.componentCache=false`。

### Opt19 · SetupStage/TickStage 跨实体并行化（2026-08-29）

- **文件**:`client/render/pipeline/ParallelStageExecutor.java`（新，专用 FJ 池+索引序合并）、`client/render/EntityRenderOrchestrator.java`（SetupStage/TickStage 重构 + tickEntity 抽取 + tickForEntity hoist）、`client/particle/DeferredAnimationParticleSpawner.java`（新，粒子操作队列化）、`molang/compiler/MolangRuntimeSupport.java`（MemberSite volatile Binding 快照）、`model/GlobalBoneIdHandler.java`（static synchronized）、`molang/MolangValue.java`（常量池 CHM）。
- **设计文档**:`design/opt19-parallel-stages-design.md`（线程模型+共享状态全清单）。
- **共享可变状态处置**（逐项实证核查）:
  1. **MemberSite**：表达式实例按字符串全局共享（compileCache）——(tree,epoch,minimal,full) 散装普通写并发下会撕裂（新纪元+旧绑定）。改 volatile 不可变快照：读侧单次取快照，旧纪元一致或新纪元一致，不撕裂。
  2. **GlobalBoneIdHandler**：裸 fastutil computeIfAbsent 并发插入可损坏 → static synchronized（稳态仅查找，无竞争同步成本可忽略）。
  3. **MOLANG_VALUE_CONSTANT_POOL**：Float2ObjectOpenHashMap → ConcurrentHashMap。
  4. **AttachableItemRenderSetup.CACHE**：static WeakHashMap 非线程安全 → tickForEntity hoist 出并行段，join 后按实体序串行（不消费 tickAnimation 产物，实证）。
  5. **粒子 spawn/updatePose/remove + syncedActions（NativeImage GPU 读回）**：必须渲染线程 → DeferredAnimationParticleSpawner 逐实体队列 + deferred 收集，join 后按实体索引序回放（同帧同序，粒子在实体之后的粒子相渲染，时序差异不可观察）。
  6. **已安全不动**：ZERO_ARG_CACHE/METHOD_HANDLES/FIELD_GETTERS/SPREAD_INVOKERS（CHM+不可变条目）、WARNED_MISSING（synchronizedSet）、HostRole REGISTRY（CHM+AtomicInteger）、MolangMappingTree（纪元守卫，帧内只读）。
- **执行模型**：专用 ForkJoinPool（默认 clamp(2,8,processors/2)=8 worker），实体列表按连续索引分 2×workers 片，渲染线程 join 阻塞（submit/join 建立 happens-before：tick 期实体状态写对 worker 可见；worker 对逐实体 cap 的写 join 后对渲染线程可见）。结果/延迟动作按索引序合并——与串行逐实体迭代顺序完全一致。worker 首个异常 join 后渲染线程重抛。
- **验证**:契约测试 5 类（MemberSite 并发锤+纪元翻转窗口无撕裂值、执行器覆盖/顺序/异常/阈值/空表、DeferredSpawner 回放序+防御拷贝+幂等、GlobalBoneIdHandler 并发同 id/异 id/反查、常量池并发同实例）+ 1.20.1 全量 262 类绿 + 1.21.1 全量 263 类绿 + 运行时 51k 次渲染 0 错误 + 截图正确（史莱姆半透明/多类型）。
- **结果**（world n384 slime，45s 串行协议，-Deyelib.parallelStages A/B）:
  - 1.20.1:OFF 64.03/62.52 → ON **106.03/104.13（约 +66~68%）**。
  - 1.21.1:OFF 115.63/109.62 → ON **209.64/192.74（约 +76~81%）**。
  - JFR 机制证据（opt19-on.jfr，--stack-depth 64）：render 线程样本中 SetupStage 14 + TickStage 51（合计 ~15%，原 66~77%）；工作均匀分布在 8 个 worker（distinct javaThreadId ×8，各 ~250-300 样本）。
- **累计**:1.20.1 world n384 从最初 33.4 → **104~106（约 3.1×）**；1.21.1 → **193~210**。
- **诊断开关**:`-Deyelib.parallelStages=false`、`-Deyelib.parallelStages.threads=N`、`-Deyelib.parallelStages.minEntities=N`（默认 8，低于阈值串行）。
### C6' · 派生纹理 download 消除 GPU 读回（2026-08-29）

- **文件**:`bridge/client/render/texture/adapter/NativeImageIO.java`（download 增 DynamicTexture 像素快路径 + DYNAMIC_DOWNLOAD_HITS 探针计数）。
- **归因**:clamped/_color_mask 派生纹理的生成是每纹理每次纹理状态变更一次的事件（needsTextureReload 版本比较触发，syncedAction 执行），稳态逐帧零成本（56k 行深栈 JFR 零帧）——唯一真实成本是 download 的 `glGetTexImage` **同步读回造成派生事件帧的 GPU 管线停顿**。
- **方案**:eyelib 基图全部由 `upload()` 注册为 DynamicTexture，CPU 侧 NativeImage 像素常驻至 close()（1.20.1/1.21.1/26.1.2 三版本 vanilla 源码实证）。download 先查 `instanceof DynamicTexture && getPixels() != null`（26.1.2 加 `!isClosed()`）→ 直接用 CPU 像素（调用方一律 copyImage 深拷贝后使用，就地像素只读），完全跳过 GPU 读回；非 DynamicTexture 回退原路径，行为不变。colorMask 经同一 download 自动受益。无新 GL 机制，三版本统一。
- **验证**:三版本编译绿 + 1.20.1/1.21.1 全量单测绿；运行时探针：世界载入 111 次、蜘蛛生成 +57 次、夜晚合批场景累计 317 次派生全部命中快路径（零 GPU 读回）；**像素级等价**：已注册 clamped 纹理 == 基图 CPU 像素现算 clampAlphaToBinary，逐像素 diff=0；截图正确（史莱姆半透明层次昼/夜正确）。
- **基准**:稳态 FPS 无可测变化（派生非逐帧成本）——本项消除的是重载/首渲染时的偶发停顿，不做 throughput 基准。

## 已排除项

## 已排除项

- **C4 粒子实例化（2026-08-29 定论不实施）**:world n384 深栈 JFR 聚合（56k 行）中 eyelib 粒子渲染零帧；现有路径经 vanilla bufferSource 按纹理合批（每 RenderType 一次 draw），实例化只能省每粒子 4 顶点 CPU 写，无可测收益；且 1.20.1 GL 3.2 需 ARB_instanced_arrays 扩展。若未来有粒子密集场景证据再重估。
- **eyelib 自身堆占用健康**:eyelib 全部类合计 56MB(2.18%),数量级合理,无需优化。
- **javac 97MB**:`/eval` HTTP 调试服务运行时编译的测量伪影,生产无此开销。
- **Server thread 开销**:eyelib 在服务端几乎零开销(Top 30 唯一 eyelib 函数 = DataAttachmentHelper.getOrCreate,0.13%)。

## 待办(超出 spark 能力)

- **byte[] 1.16GB 来源定位**:spark heapsummary 是 class histogram,不显示引用链。需 `/spark heapdump`(.hprof)+ Eclipse MAT 分析 dominator tree / path to GC roots。优化前测得,优化后未复测(可能本就与 eyelib 无关)。
- **ZipFileSystem IndexNode 215MB**:**非 eyelib 产生**。eyelib 用 `java.util.zip.ZipFile`(BedrockAddonLoader.collectFilesFromZip,生命周期在 finally 块统一关闭),不创建 NIO `jdk.nio.zipfs.ZipFileSystem$IndexNode`。215MB IndexNode 应在 Forge/Vanilla 资源包系统或其他 mod 排查。

## 方法论局限

1. **Java sampler safepoint bias**:对 fillInStackTrace / Pattern.compile 这类热点检测可靠,但绝对值是相对排名(self-time 总和可能 > 100%,因 DAG 压缩)。
2. **RenderDoc 影响**:dev 客户端若以 RenderDoc capture 模式启动,CPU/堆有轻微 hook 开销,应在非 RenderDoc 环境复测确认。
3. **单次采样**:每类只做一次 profile,目标是识别瓶颈(非精确基准)。
4. **spark 总量不可跨 run 比较(2026-07-25 实证)**:60s 窗口的 self-time 正比于窗口内帧数 × 每帧成本,且受机器状态(热状态、远控 IDD GPU 驱动状态)影响。两次同代码 run 的 eyelib self 可差 30%+,环境差异足以掩盖 ±20% 的真实优化。spark 用于方法级归因(哪个方法热),**幅度结论必须用 clientBenchmark A-B-A 交错**(baseline/候选在同会话交替跑)。

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

## 已排除项

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

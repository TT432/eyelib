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

## 验证证据(优化前 → 优化后)

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

> **T4 是佐证**:Forge EventBus cast Lambda 是通用机制(所有事件 post 共用,非 eyelib 专属),38% 下降部分归因 Opt3、部分归因采样时机与 GC。Opt3 主证据是 T3 CPU 的干净归因。

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

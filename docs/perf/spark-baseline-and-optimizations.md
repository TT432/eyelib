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

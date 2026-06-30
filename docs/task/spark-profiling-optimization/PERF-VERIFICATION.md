# spark-profiling 优化性能验证报告

> 本报告验证 Opt1/Opt2/Opt3 的实际性能收益：在含优化代码的 1.20.1 dev 客户端上重跑 spark profiler，与优化前基线（`../spark-profiling/` 任务的 T2/T3/T4-results.md）对比。
> 本文件是 `spark-profiling-optimization` 任务（验证+优化）的性能验证产物，**不改写** spark-profiling 原任务文档。静态源码验证见同目录 `VERIFICATION-RESULT.md`。

## 摘要

三个优化全部经重跑 profiler 验证，CPU 主证据均决定性成功：

| 优化 | 主证据（优化前→后） | 结论 |
|---|---|---|
| Opt1 findField 缓存 | T2 `fillInStackTrace` self 76%→**0%**；T3 `findField` 4.9%→2.71%（self 0%） | 决定性成功 |
| Opt2 Pattern→startsWith | T2 `java.util.regex.Pattern*` self 8.9%→**0.05%** | 决定性成功 |
| Opt3 putAll 批量化 | T3 `Registry.put` total 48.3%→**0.04%**；`EventBus.doCastFilter` 47.8%→**0.03%** | 决定性成功（CPU 主证） |
| Opt3 堆影响 | T4 Forge EventBus Lambda 149MB→91.7MB（-38%） | 佐证（非独立决定性证据，见局限） |

## 采集方法

- **客户端**：1.20.1 dev 客户端（spark 经 CurseMaven + `modLocalRuntime` 集成，配方见 `../spark-profiling/tooling-notes.md`）。
- **profiler 触发**：`/eval` 反射 `ClientCommandHandler.runCommand("sparkc profiler start --timeout <N>")`（`/sparkc` 采 client 主线程 = Render thread；**不加 `--thread`**，引号被 brigadier 吞掉会致空采样）。
- **失焦暂停**：进世界后 `mc.options.pauseOnLostFocus=false; mc.setScreen(null);`，否则 Render thread 冻结采不到数据。
- **资源重载**：`mc.reloadResourcePacks()`（F3+T 等效）。
- **堆摘要**：client-side `/sparkc heapsummary` 无 chat 输出，改用 server-side `mc.getSingleplayerServer().getCommands().performPrefixedCommand(src.withPermission(4), "spark heapsummary")`。
- **URL 获取**：读 `versions/1.20.1/run/config/spark/activity.json`（最可靠）。
- **解析**：下载 raw protobuf（`spark-usercontent.lucko.me/<id>`），`/eval` 内 `SparkSamplerProtos.SamplerData.parseFrom(data)`，按 className+methodName 聚合 self-time / total-time。

## T2 稳态渲染（验证 Opt1 + Opt2）

- URL: https://spark.lucko.me/QgxEIFFBhJ
- 数据: `data/t2-postopt-profile.bin` (3.7MB) + `data/t2-postopt-metadata.json`
- rootTotal = 60000（60s），nodes = 26289
- 负载：83 实体**全部 eyelib 接管**（useBuiltInRenderSystem=true）= 26 skeleton + 18 creeper + 13 spider + 12 sheep + 8 zombie + 5 slime + 1 player

> 负载说明：与优化前 T2（59 slime + 12 sheep = 71 实体）的类型/数量不同，但 eyelib 接管率 100% 且总数更多，findField / Pattern 热点被充分触发，**self-time 相对占比的对比有效**。

| 热点 | 优化前 self | 优化后 self | 结论 |
|---|---|---|---|
| `Throwable.fillInStackTrace` | 76% (45512ms) | **0.00%** | Opt1 完全消除异常控制流 ✅ |
| `java.util.regex.Pattern.*` | 8.9% (5312ms) | **0.05%** | Opt2 几乎消除（残留为非 RenderControllerRuntime 路径）✅ |
| `MolangMappingTree.findField` | 高（异常沿栈摊分） | **0.00%**（incl 3.3%） | Map.get 极快，无异常 ✅ |
| `RenderControllerEntry.matchesBonePattern` | —（新方法） | **0.00%** | startsWith 零开销 ✅ |

优化后 TOP15 热点转为 OpenGL 驱动调用 + stream 开销（6.4% `GL20C.nglGetUniformLocation` / 5.6% `stream.evaluate` / 4.9% `GL30C.glBindVertexArray` / 4.3% `StringLatin1.toLowerCase` 等），均为渲染固有成本，非 eyelib 优化范畴。

## T3 资源重载（验证 Opt3 putAll，CPU 主证据）

- URL: https://spark.lucko.me/hMYwoc7nLo
- 数据: `data/t3-postopt-profile.bin` (5.37MB) + `data/t3-postopt-metadata.json`
- rootTotal = 90000（90s），nodes = 36124
- 采集：profiler start --timeout 90 + `mc.reloadResourcePacks()`，重载约 50-60s 落在采样窗口内

| 热点 | 优化前 total | 优化后 total | 结论 |
|---|---|---|---|
| `Registry.put` | 48.3% (43484ms) | **0.04%** | Opt3 材质/RC 不再逐条 put ✅ |
| `EventBus.doCastFilter` | 47.8% (42988ms) | **0.03%** | 不再 N×ManagerEntryChangedEvent 分发 ✅ |
| `BedrockAddonRuntimeBridge.replaceFromResourcePack` | 54.6% | **0.11%** | 批量收集 + putAll ✅ |
| `MolangMappingTree.findField` | 4.9% (4420ms) | **2.71%**（self 0.00%） | Opt1 在 reload 也生效 ✅ |
| `Throwable.fillInStackTrace` | 4296ms | **0.01%** | ✅ |
| `Registry.replaceAll` | — | 0.00% | 实体/attachable/模型仍走 replaceAll（不变） |

优化后 TOP15 转为 GL 驱动 + stream（9.05% `nglGetUniformLocation` / 6.01% `glBindVertexArray` / 4.95% `stream.evaluate`），渲染重建总入口 `RenderStageRegistries$DefaultRegistry.dispatch` total 58.04%。

## T4 堆摘要（验证 Opt3 堆影响，佐证）

- URL: https://spark.lucko.me/txiWm3qS1e
- 数据: `data/t4-postopt-heapsummary-full.json` (34529 entries, sum 1004.8MB)
- 采集：reload 中段（触发后 25s）抓堆

| 对象 | 优化前 | 优化后 | 变化 |
|---|---|---|---|
| Forge EventBus cast Lambda（两类合计） | 149MB（各 3,102,148 实例） | 91.7MB（各 2,002,710 实例） | size -38%，实例 -35% |
| `ManagerEntryChangedEvent` / `ManagerReplacedEvent` 实例 | — | **0**（堆里不可见） | 生命周期短，post 后即 GC，符合预期 |
| `byte[]` | 1.16GB | 273.7MB | 非 Opt3 影响（见局限） |
| ZipFileSystem `IndexNode` | 215MB | 35.8MB | 非 eyelib 产生（V4 判定） |
| 总堆 sum | 2.52GB | 1004.8MB | 采样时机差异为主 |

## 局限与方法论说明

1. **T2 负载差异**：优化后负载（83 实体，多类型）与优化前（71 实体，slime 为主）不同。self-time 占比是相对值，eyelib 接管率 100% 保证热点被触发，对比有效；但绝对时间不可直接比。
2. **T4 是佐证，非独立决定性证据**：
   - Forge EventBus 的两个 cast filter Lambda 是**通用机制**（所有事件 post 共用，非 eyelib 专属），reload 期间 Forge/Vanilla 资源加载事件也会创建它们。38% 的下降部分归因 Opt3（消除了材质/RC 的 N×事件），部分归因采样时机与 GC。
   - 真正干净的 Opt3 堆验证需隔离材质/RC 事件源，但 spark heapsummary 不显示引用链，做不到。
   - 因此 Opt3 的**主证据是 T3 CPU**（`Registry.put` 48.3%→0.04%、`doCastFilter` 47.8%→0.03%，干净归因 eyelib 调用栈），T4 堆数据作印证。
3. **T4 的 byte[] / IndexNode 下降非 Opt3**：本次在 reload 中段抓堆，优化前 T4 在 reload 后抓堆，时机不同 + GC 差异致数值波动；且 V4 已判定 IndexNode 非 eyelib 产生（eyelib 用 `java.util.zip.ZipFile`，不产生 NIO ZipFileSystem IndexNode）。
4. **Opt1 在 T3 reload 也验证**：reload 会重新解析 Molang 映射并触发字段查询，findField 从 4.9%→2.71%（self 0%），fillInStackTrace 从 4296ms→0.01%，证明 Opt1 跨场景生效。

## 数据文件清单

| 基准 | 文件 | spark URL |
|---|---|---|
| T2 稳态 | `data/t2-postopt-profile.bin` + `data/t2-postopt-metadata.json` | https://spark.lucko.me/QgxEIFFBhJ |
| T3 重载 | `data/t3-postopt-profile.bin` + `data/t3-postopt-metadata.json` | https://spark.lucko.me/hMYwoc7nLo |
| T4 堆 | `data/t4-postopt-heapsummary-full.json` | https://spark.lucko.me/txiWm3qS1e |

原始 spark 报告可通过 URL 在线查看火焰图 / 树状图，数据文件可经 `SparkSamplerProtos.SamplerData.parseFrom` 复现本报告数值。

# Spark 性能剖析任务规划

> 任务来源：用 spark（lucko 的 MC 性能分析 mod）对 eyelib 自主设计、运行、分析基准测试，输出优化建议。
> 本文档是规划阶段产物。子任务过程记录在各子任务目录下。

## 1. 问题建模

eyelib 是 Bedrock 渲染规范的 Java 复刻库，性能热点分布在两类路径：

- **稳态路径**：每帧 / 每tick 执行。包括实体渲染、材质绑定、动画控制器/片段执行、Molang 查询、粒子运行时。
- **加载路径**：一次性但每次 F3+T 重置。包括 codec 解析、BrArchive 解压、材质/动画对象构建。

spark 是采样式 profiler（execution sampler + allocation sampler + heap summary），能在真实运行负载下给出热点分布。优化判断 = {spark 测得的占比} × {方法是否属于 eyelib 命名空间} × {代码层面是否存在可优化特征（重复计算、装箱、未缓存、O(n²)）}。

## 2. spark 工具概述（操作要点）

- 集成方式：spark 是独立 Forge mod，jar 放 `run/mods/` 即被 dev 客户端加载。
- 客户端命令前缀：`/spark`（spark 在所有平台注册的标准命令名）。
- 三类基准会用到的命令：
  - `/spark profiler start --thread "Render-Thread" --timeout <s>` — 渲染线程限时采样
  - `/spark profiler start --timeout <s>` — 全线程限时采样（资源重载场景）
  - `/spark heapsummary` — 堆对象摘要
  - `/spark profiler stop` — 停止并上传
- 报告读取（AI 可读路径）：
  - 上传后获得 viewer URL `https://spark.lucko.me/<id>`
  - `GET https://spark.lucko.me/<id>?raw=1&path=<jsonpath>` 返回 JSONPath 过滤后的元数据 JSON
  - `&full=true` 返回完整 sampler 数据（可能数十 MB，慎用）
  - 完整火焰图节点需读 `spark-usercontent.lucko.me/<id>` 的 protobuf（降级路径，用 spark2json 解析）

## 3. 基准测试设计

每个基准测试是一个可独立验证的工作单元：固定负载 + 固定 profiler 配置 + 可识别结论。

### Benchmark A — 稳态渲染 CPU profile
- **目的**：识别每帧渲染中 eyelib 命名空间内的 CPU 热点。
- **负载**：进入 `Debug World` 存档 → 在玩家附近召唤多个 eyelib 接管渲染的实体（slime 等，按 skill 文档 Phase 5 验证 useBuiltIn=true）→ 稳定运行。
- **profiler 配置**：`/spark profiler start --thread "Render-Thread" --timeout 60`。
- **通过判据**：获得一个有效的 spark viewer URL，并能从 raw JSON 读出 Top-N 热点方法。

### Benchmark B — 资源重载 CPU profile
- **目的**：识别 codec/BrArchive/材质对象构建在 reload 期间的开销分布。
- **负载**：profiler 启动 → 触发 F3+T 资源重载 → 等待 reload 完成 → stop。
- **profiler 配置**：`/spark profiler start --timeout 90`（全线程，reload 涉及多线程）。
- **通过判据**：raw JSON 中能识别 `io.github.tt432.eyelib.importer.*` / `eyelib.material.*` 等命名空间的方法占比。

### Benchmark C — 堆内存摘要
- **目的**：识别 eyelib 在堆中的对象大头（缓存、ComponentStore、MaterialManager 等）。
- **负载**：稳态运行后（与 A 同场景）。
- **命令**：`/spark heapsummary`。
- **通过判据**：raw JSON 中能识别 eyelib 命名空间的类占堆比例。

## 4. 子任务划分

| 子任务 | 内容 | 通过判据 |
|---|---|---|
| T1 | 集成 spark（脚本化下载 spark-forge jar 到 `run/mods/`），启动客户端验证 `/spark` 可用 | dev 客户端能执行 `/spark health` 返回结果 |
| T2 | 执行 Benchmark A，拉取 raw JSON 分析 | 获得稳态渲染 Top-N 热点，记录到子任务文档 |
| T3 | 执行 Benchmark B，拉取 raw JSON 分析 | 获得 reload 期间 Top-N 热点 |
| T4 | 执行 Benchmark C，拉取 raw JSON 分析 | 获得堆内 eyelib 类大头 |
| T5 | 汇总三类数据 + 对照代码，输出综合优化建议报告 + 工具演化文档 | 报告中每条建议有 spark 数据支撑 + 代码引用 |

每个子任务独立提交（git commit），失败回滚不影响其他子任务。

## 5. 风险与降级方案

| 风险 | 降级 |
|---|---|
| dev 客户端无法访问外网，spark 上传失败 | `/spark profiler stop --save-to-file` 保存 `.sparkprofile`，用 spark2json Node 工具本地解析 |
| `--thread "Render-Thread"` 过滤后样本太少 | 改用全线程 `*`，事后从 raw JSON 过滤 |
| 现有存档没有 eyelib 接管的实体 | 用 `/eval` 召唤 slime 等实体（参照 `eyelib-debug` skill Phase 5） |
| raw JSON `&full=true` 过大无法 fetch | 用 `&path=` 分段拿元数据 + 关键节点 |
| async-profiler 在 dev JVM 不可用 | spark 自动 fallback 到 Java sampler，无需手动干预 |

### T1 执行中发现的关键事实（2026-06-30）

**规划阶段风险表里的"spark-forge 版本与 forge 不兼容 → 降到 1.10.170-forge 或更早"是错的。** 真相：

1. **Stonecutter 多版本 dev 运行目录是 version-node-specific**：`versions/<mc-version>/run/`，不是项目根 `run/`。`fetch-spark.ps1` 必须传 `-ModsDir versions/1.20.1/run/mods`。
2. **Modrinth 上 spark 1.20.1/forge 兼容版本只有两个**：`1.10.53-forge`（2023-09-05）和 `1.10.42-forge`（2023-06-14）。规划阶段列的 `1.10.170-forge`/`1.10.149-forge`/`1.10.138-forge` 都是 NeoForge / 更高 Forge 专用的，与 1.20.1 legacyforge 完全不兼容。
3. **根本障碍**：spark-forge jar 全部是 **SRG-mapped**（用 SRG 名如 `m_91087_` 编译），而 eyelib dev 环境用 **ModDevGradle + Parchment**（Mojmap+Parchment 名），运行时不提供 SRG 名。`spark-1.10.53-forge.jar` 加载时在 `ForgeClientSparkPlugin.register` 抛 `NoSuchMethodError: 'Minecraft m_91087_()'`。**所有 spark-forge jar 都无法在 ModDevGradle dev 环境直接加载。**
4. **可用 mapping**：`versions/1.20.1/build/moddev/artifacts/intermediateToNamed.srg`（22MB，104k 行 SRG→Mojmap mapping，含 CL/FD/MD）是 ModDevGradle 内部产物，理论可用于 remap。
5. **手工 remap 工具链有坑**：
   - `ForgeAutoRenamingTool 0.1.9-all`（Maven 坐标 `net.minecraftforge:ForgeAutoRenamingTool:0.1.9`，fatjar 后缀 `-all`）：
     - 对含 `META-INF/versions/9/module-info.class` 的 jar 抛 `StringIndexOutOfBoundsException` → 需先解压剔除
     - 剔除 module-info 后跑 remap 在 `Remapper.mapSignature` 抛 NPE（疑似内部 class signature 引用了未在 mapping 内的类型，缺 null check）
   - `SpecialSource 1.11.4`：standalone jar 缺 `joptsimple` 依赖，需自行拼 classpath
6. **未尝试路径**：手工 patch ART 的 NPE、补 SpecialSource classpath 后重试、用 spark 源码本地编译 Mojmap 版本。

### 候选降级方案（需用户决策）

- **A. 继续 remap 路径**：估算 1-3 小时，工程上未证明不可能；产出可复用（未来装任意旧 SRG-Forge mod 都能用）。
- **B. 改用 JFR (Java Flight Recorder)**：JDK 17 内建、零 mod 加载、零工具链折腾。
  - CPU profile 等效事件：`jdk.ExecutionSample`（对标 spark profiler）
  - 内存分配：`jdk.ObjectAllocationSample`
  - 堆摘要：`jmap -histo` 或 JFR 的 `jdk.GCHeapSummary`
  - 触发：JVM 启动参数 `-XX:StartFlightRecording=...` 或运行时 `jcmd <pid> JFR.start`
  - 解析：JDK 自带 `jfr print --events jdk.ExecutionSample <file>` 输出文本，可转 JSON
  - 缺点：无 spark viewer URL 可视化；无 MC-aware 命令内触发，需要 `eyelib_debug_execute` 调 `jcmd` 或预先配 JVM 参数
- **C. 在 ModDevGradle 的 `runs` DSL 里加 SRG-remap 配置**：若 legacyforge 插件支持（未查证），可在 build.gradle 声明自动 remap mods 目录的 jar。

### 最终方案（T1 完成 2026-06-30）

走 **CurseMaven + `modLocalRuntime`**（不是裸 jar 投 mods 目录）：

1. `build.gradle` 加 CurseMaven repo（`exclusiveContent`，`includeGroup "curse.maven"`）。
2. `modLocalRuntime "curse.maven:spark-361579:4738952"`（**必须用 `modLocalRuntime`，不是 `localRuntime`**）。
3. `modLocalRuntime` 配置由 `obfuscation.createRemappingConfiguration(configurations.localRuntime)` 自动创建（见 `LEGACY.md` "Remapping Mod Dependencies"），触发 ModDevGradle 的 SRG→Mojmap remap；remapped 产物回填到 `localRuntime` → `runtimeClasspath`。
4. remapped jar 路径：`~/.gradle/caches/<gradle-ver>/transforms/<hash>/transformed/spark-361579-4738952.jar`。

验证：客户端启动不崩溃，`ModList` 含 `spark:1.10.53`，`/spark health` 返回 1。

**后续 T2/T3/T4 命令前缀统一用 `/spark`（spark 在所有平台注册的标准命令名，不存在 `/sparkc`）。**

**踩坑记录**：
- 裸 jar 投 `run/mods/`（含 Stonecutter 的 `versions/<ver>/run/mods/`）不 remap，spark 因 SRG 名 `m_91087_` 的 `NoSuchMethodError` 崩溃。
- `localRuntime "..."` 也不 remap（直接用 SRG jar），同样崩溃。
- 只有 `modLocalRuntime "..."` 会触发 remap pipeline。
- 手工 remap（ForgeAutoRenamingTool）路径放弃：ART 对 spark jar 的 module-info 报越界、对内部 class signature 报 NPE，连续踩 bug，工作量大且无复用价值。

## 6. 跨域约束（来自 eyelib skill）

- **禁止猜测**：所有"热点"结论必须有 spark raw JSON 数据支撑。
- **数据优先**：测得占比 → 对照代码确认调用链 → 才提优化建议。顺序不可颠倒。
- **不污染发布产物**：spark 仅放 `run/mods/`，不进 `build.gradle` 的 `implementation`/`jarJar`。
- **工具演化**：T1 的下载脚本归档到 `scripts/fetch-spark.ps1`，并在 T5 工具演化文档中记录。

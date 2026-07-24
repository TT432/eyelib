# 渲染 Benchmark 实现规格

## 目标

在 Eyelib dev client 中提供独立、默认关闭、可自动退出的 `clientBenchmark` 模式，覆盖隔离 FBO 吞吐、真实世界帧性能、长期稳定性和 30/60/120 FPS pacing。实现不进入普通客户端热路径；未设置 `eyelib.benchmark.enabled=true` 时不得加载 clientsmoke benchmark 依赖。

## 前置条件

- clientsmoke dev mod 已发布到 Maven Local，并在 benchmark run 中可加载。
- benchmark 世界使用固定 seed `12345` 的 creative flat world。
- 资源包、实体类型和 MC 图形选项由报告元数据固化。

## 架构

- `io.github.tt432.eyelib.debug.benchmark`：运行时 benchmark；允许依赖 Minecraft、Forge/NeoForge 和 clientsmoke dev API。
- `Eyelib` composition root 仅通过反射安装 benchmark，避免发布环境对 clientsmoke 的硬依赖。
- `ClientBenchmarkRunner`：tick/render 事件驱动状态机。
- `BenchmarkScenario`：不可变场景规格；场景类型为 `FBO` 或 `WORLD`，目的为 `THROUGHPUT`、`PACING` 或 `STABILITY`。
- `FrameSampleBuffer`：预分配 primitive arrays；渲染热路径零集合扩容、零逐帧日志。
- `BenchmarkStatistics`：报告阶段计算 percentile、budget miss/hitch、5 秒窗口和 Theil–Sen slope。
- `BenchmarkReportWriter`：输出 metadata/summary JSON、raw frame CSV、1 秒 resource CSV。

## 生命周期

```text
INIT → WORLD_CREATE → WORLD_WAIT → STABILIZE
     → PREPARE → WARMUP → MEASURE → COOLDOWN
     → REPORT → NEXT_SCENARIO → EXIT
```

异常进入 ERROR，写失败报告后退出非零语义由报告 `status=failed` 表达；客户端仍执行正常 stop，避免遗留 Java 进程。

## 采样契约

在每个 render-frame Pre/START：

- `frame_interval_ns = current_start - previous_start`
- `render_work_ns = Minecraft.getFrameTimeNs()`，对应上一完整帧
- `rendered_entity_count` 为上一帧 Eyelib 成功渲染计数增量；无法取得时必须明确标记不可用，不能用配置数量伪装。
- phase 为 `WARMUP` 或 `MEASURE`

低频资源样本每秒记录 heap used/committed、累计 GC count/pause、进程 CPU load。

首帧没有前序 interval，不记录。buffer 满时场景失败，不覆盖或丢弃旧数据。

## 场景矩阵

默认实体 `minecraft:slime`，默认数量阶梯 `1,16,64`，均可用 system property 覆盖。

1. 每个数量一条 uncapped FBO throughput 场景。
2. 每个数量一条 uncapped WORLD throughput 场景。
3. 固定 pacing 数量，分别执行 30/60/120 FPS WORLD 场景。
4. 固定 stability 数量，执行默认 600 秒 WORLD 场景。

默认普通场景 warmup 15 秒、measure 30 秒；稳定性场景 warmup 30 秒、measure 600 秒。短验证运行可通过 Gradle system property 覆盖。

## 场景不变量

- VSync 关闭；uncapped 使用版本支持的最大 FPS limit。
- 固定窗口 game directory、HUD 隐藏、失焦不暂停。
- camera 每 tick 固定在同一位置和朝向。
- WORLD 实体由 integrated server 创建，NoAI、silent、invulnerable、固定位置；每个场景结束清除带 benchmark tag 的实体。
- FBO target 和实体只在场景 prepare 分配，逐帧只执行 render；cleanup 销毁 target。
- 场景实际实体数量不等于配置数量时场景失败。

## 输出

目录：`benchmark-reports/<timestamp>-<scenario>/`

- `metadata.json`：MC/loader/Java、GPU renderer、场景、设置、时长、样本完整性。
- `summary.json`：throughput、P50/P95/P99/P99.9、P99-derived FPS、30/60/120 budgets、hitch、窗口趋势、状态分类。
- `frames.csv`：逐帧 timestamp/interval/render-work/entity-count/phase。
- `resources.csv`：每秒 heap/GC/CPU。
- run 根目录 `benchmark-reports/latest.json`：运行状态与各场景路径。

## 统计不变量

- throughput 为测量期 frame count / 测量期首尾 timestamp，禁止平均瞬时 FPS。
- percentile 在 frame-time 域计算并在字段名中明确。
- `budget_miss` 使用严格 `frame_interval > budget`。
- hitch 阈值为目标 budget 的 2x/3x。
- Theil–Sen 使用 5 秒窗口 P50 的两两斜率中位数，输出 `ms/min`。
- 无硬编码回归门禁；本实现生成建立 baseline 所需证据。

## 异常行为

- 无 integrated server、实体类型不存在、实体创建失败、client smoke 依赖缺失、样本 buffer 溢出、报告写入失败：终止当前 run 并写明确错误。
- benchmark 与 clientsmoke 同时启用：启动时失败，避免两个世界状态机竞争。
- 不主动 GC，不静默剔除 outlier，不使用截图、RenderDoc、spark 或 `/eval`。

## 验收

1. 三个 Stonecutter 节点编译。
2. 统计核心单元测试覆盖 percentile、throughput、budget/hitch、趋势和 buffer overflow。
3. 1.20.1 短配置端到端运行自动进入世界、完成至少一个 FBO 和一个 WORLD 场景、写出可解析报告并自动退出。
4. raw frame 数量、summary frame count、场景配置实体数一致；报告标记无 overflow。
5. 普通 `client` run 未启用 benchmark 时行为不变。

# 渲染 FPS / 稳定性 Benchmark 调研

## 1. 结论

Eyelib 目前没有可复用的 FPS benchmark。现有 spark 数据是热点剖析，不是可比较的性能基准：只覆盖 1.20.1、存在采样偏差、场景实体数不固定，且现有文档明确标注“单次采样”。

推荐后续实现一个独立的 `clientBenchmark` 模式，而不是把长时间性能测试塞进 `@ClientSmoke`：复用 clientsmoke 的启动、固定世界和跨版本 FBO 场景能力，但使用独立生命周期、原始帧记录和 benchmark 报告。

Benchmark 应拆成三个互补工作负载：

1. **隔离 FBO 吞吐**：固定分辨率、固定实体集合，反复执行 Eyelib 实体渲染，测渲染器自身扩展性。
2. **真实世界帧性能**：固定世界、固定相机、真实主 framebuffer，测端到端渲染工作时间和实际帧间隔。
3. **长时间稳定性**：同一压力场景持续运行，检测帧时间随时间上升、抖动增加和内存/GC 相关性；该项直接针对“JE 帧数逐渐降低”的现象。

“low~high FPS”不能用一次运行的 `min/max FPS` 表示。推荐同时采用：

- **负载阶梯**：VSync 关闭、FPS 上限无限，通过实体数量/复杂度从高 FPS 压到低 FPS。
- **帧率上限阶梯**：30 / 60 / 120 FPS，只用于测试不同目标帧率下的 frame pacing；场景必须能持续满足目标 FPS，否则结果属于吞吐不足，不属于 pacing 不稳定。

## 2. 项目现状

### 2.1 可复用能力

| 能力 | 证据 | 可复用范围 |
|---|---|---|
| 固定 clientsmoke 世界 | `ClientSmokeStateMachine`: 固定 seed `12345`、creative、flat world、STABILIZE 阶段 | 启动与世界准备 |
| 跨版本独立 FBO 实体渲染 | `clientsmoke/.../EntitySceneRenderer.java` | 隔离渲染场景 |
| FBO 生命周期与像素验证 | `ClientSmokeVisualHooks.setScene()` | 场景创建；benchmark 不应执行逐帧截图/回读 |
| Eyelib 每帧管线入口 | `EntityRenderOrchestrator.onRenderStage()` → `FramePipeline.run()` | Eyelib 内部阶段耗时归因 |
| 每帧 Pre/Post 事件 | 1.20.1 `RenderTickEvent`；1.21.1/26.1.2 `RenderFrameEvent` | 采样生命周期 |
| 原版 CPU 渲染耗时 | 三版本 `Minecraft.getFrameTimeNs()` | 主指标 `render_work_ns` |
| 原版异步 GPU timer | 三版本 `TimerQuery` | 可选 `gpu_work_ns` |
| 原版完整帧间隔日志 | 1.20.1 `FrameTimer`；1.21.1/26.1.2 `DebugScreenOverlay.logFrameDuration()` | 主指标 `frame_interval_ns` |
| JSON/JUnit 报告 | clientsmoke `handleReport()` | 只复用报告组织方式，不复用测试结果模型 |
| 运行时自动化 | `.mcmcp` + AIDebugServer | 探索和启动；正式测量不使用 `/eval` |

### 2.2 关键纠正

`ClientFrameTimePort.getFrameTime()` 不是性能意义上的“帧耗时”。它返回 partial tick / realtime delta ticks，用于动画插值；不能用于 FPS benchmark。

三版本本地 patched Minecraft 源码表明：

- `Minecraft.getFrameTimeNs()` 从渲染工作开始计时，包含 update/extract/render/blit；在 swap/updateDisplay 和 FPS limiter 之前结束。因此它适合作为 **CPU 侧渲染工作时间**，但不是用户看到的完整帧间隔。
- 完整帧间隔在 swap 和 limiter 之后计算并写入原版 FPS 图数据。因此应另外记录 **相邻帧开始/结束间隔**。
- `TimerQuery` 是异步 GPU timer，避免 `glFinish()` 对流水线造成的系统性干扰；但它是共享/单例式设施，正式设计必须避免与 F3 GPU 图或原版 metrics recorder 同时占用。

### 2.3 现有工具的定位

- **spark**：用于回答“CPU 时间花在哪里”，不用于回答“FPS 是否回归”。仅 1.20.1 可用，且 Java sampler 有 safepoint bias。
- **RenderDoc**：用于 GPU 调试和单帧归因，不用于性能门禁；capture/hook 会改变性能。
- **AIDebugServer `/eval`**：用于原型探索。现有性能文档已观察到运行时 javac 带来明显内存伪影，因此正式 benchmark 不能依赖 `/eval` 编译测量逻辑。
- **ClientSmoke**：适合正确性与单帧视觉验证。当前测试由构造器一次性执行，`durationMs` 是 `System.currentTimeMillis()` 的测试墙钟时间，不具备多帧统计语义。

## 3. 测量模型

### 3.1 原始数据

每个测量帧至少记录：

```text
timestamp_ns
frame_interval_ns       # 相邻完整帧之间的间隔，包含 present/等待/limiter
render_work_ns          # Minecraft.getFrameTimeNs()，不含 present/limiter
gpu_work_ns?            # 可选，异步 TimerQuery
rendered_entity_count
scenario_phase          # warmup / measure / cooldown
```

低频辅助采样（例如每秒一次）应包含：

```text
heap_used_bytes
heap_committed_bytes
gc_count_delta
gc_pause_ms_delta
process_cpu_load
gpu_utilization?        # 仅作佐证，不替代 gpu_work_ns
```

采集路径必须使用预分配 primitive buffer；逐帧禁止日志、字符串格式化、集合扩容、截图和同步 GPU 回读。

### 3.2 汇总指标

主报告以 frame time 为基础：

- `throughput_fps = frame_count / measured_seconds`
- `p50 / p95 / p99 / p99.9 frame_interval_ms`
- `p50 / p95 / p99 render_work_ms`
- `p99-derived FPS = 1e9 / p99(frame_interval_ns)`
- `P1~P99 FPS band`：只作分布范围展示，不使用单帧 min/max
- 各目标预算的 miss ratio：
  - 30 FPS：`33.333 ms`
  - 60 FPS：`16.667 ms`
  - 120 FPS：`8.333 ms`
- hitch 数量与最长连续 miss：`>2× budget`、`>3× budget`
- 5 秒窗口的 `p50`、`p99` 时间序列
- 帧时间趋势斜率，单位 `ms/min`

不要报告“瞬时 FPS 的算术平均值”；正确吞吐是总帧数除以总时间。不要裸称“1% low”，因为工具间存在 percentile、最差 1% 平均和按时间积分三种不同定义。报告中应直接写 `P99 frame time` 及其换算 FPS。

### 3.3 稳定性判定

稳定性不是一个单一分数。建议保留以下可解释判据：

1. **分布宽度**：`p99 - p50`、`p99 / p50`。
2. **预算违约**：miss ratio、hitch count、最长连续 hitch。
3. **时间趋势**：5 秒窗口 p50/p99 的 Theil–Sen slope。
4. **状态分类**：changepoint 分析后标记 `flat / warmup / slowdown / no-steady-state`。
5. **资源相关性**：帧时间恶化是否与 heap、GC pause、rendered entity count 同步。

针对“帧数逐渐降低”，应在帧时间域验证：若负载和实体数不变，但分段 p50/p99 持续上升，且多个独立 JVM 运行均出现正斜率，则可证实 slowdown；只看起点/终点 FPS 会混入非线性换算和偶发卡顿。

## 4. 场景设计

### 4.1 隔离 FBO 吞吐

目的：隔离世界、区块、present 和显示器因素，测 Eyelib 实体渲染成本。

不变量：

- 固定 FBO 尺寸、背景、投影、光照、yaw、partial tick。
- 固定实体定义、模型、材质、动画状态。
- 关闭截图和像素回读，只在场景开始前做一次正确性校验。
- 每帧渲染固定实体序列；负载阶梯的实体数在基线校准后冻结。

候选子场景：

- 单实体固定姿态：基础开销。
- 同构实体数量阶梯：扩展性和每实体增量成本。
- 混合实体集合：覆盖模型、Molang、材质切换。
- 透明/多 pass 重场景：覆盖 blend、cutout、emissive。
- attachable/粒子必须独立成场景，避免主基准无法归因。

限制：26.1.2 是 extract/submit 延迟架构，现有 `EntitySceneRenderer` 还跳过 feature rendering；因此 FBO 结果只能作为 Eyelib 核心几何管线基准，不能替代真实世界端到端指标。

### 4.2 真实世界帧性能

目的：测玩家实际运行时的主 framebuffer 性能与 pacing。

固定：

- flat world、固定 seed、预加载测量区域。
- 固定相机位置/朝向/FOV。
- 固定时间、天气、光照、难度和游戏规则。
- 实体 `NoAI`、固定位置、固定 tick/动画初始状态；禁止自然生成改变负载。
- 测量期间不生成新区块、不重载资源、不打开 GUI、不打印重复警告。
- Eyelib 接管率和每帧可见实体数必须写入报告；负载变化的运行无效。

候选场景：

- 空世界基线。
- 固定单实体。
- 同构实体负载阶梯。
- 固定 A&S 混合集合。

### 4.3 长时间稳定性

目的：检测泄漏、缓存无界增长、GC 压力和逐渐恶化。

建议研究配置：

- 先完成场景稳定和短 warmup，再连续测量至少 10–15 分钟。
- 使用能让基线硬件保持明显余量、但不是完全空闲的固定负载；具体实体数必须由 pilot 决定后冻结。
- 全程保存 5 秒窗口指标，不只保存整段 summary。
- 不在测量中主动 `System.gc()`；强制 GC 会掩盖真实生命周期问题。
- 运行结束记录 heap、GC、Eyelib render/error counters 和重复日志计数。

## 5. low → high FPS 测试矩阵

### 5.1 自然吞吐阶梯

- VSync：关闭。
- FPS limit：无限。
- 分辨率、画质不变。
- 通过固定负载阶梯获得从高 FPS 到低 FPS 的运行区间。

该矩阵回答“负载增加时 Eyelib 的吞吐、尾延迟和扩展性如何变化”。

### 5.2 目标帧率 pacing 阶梯

- FPS limit：30 / 60 / 120。
- 场景固定。
- 只有 uncapped 吞吐显著高于目标时，该档结果才有效。

该矩阵回答“在能满足目标 FPS 时，帧间隔是否稳定”。应报告围绕目标预算的误差、miss ratio 和连续 hitch，而不是平均 FPS。

## 6. 噪声控制与可复现性

### 6.1 必须固定的配置

- MC / Forge / NeoForge / Java 版本、JVM 参数、heap 上限。
- Eyelib commit、资源包 SHA-256、场景定义版本。
- GPU、驱动、显示输出、分辨率、窗口模式。
- VSync、FPS limit、render distance、simulation distance、graphics、AO、cloud、particles、entity shadow、mipmap。
- 电源计划、独显选择、窗口前台状态。

当前环境证据：

- 系统存在多个虚拟显示适配器，但 MC 日志确认三个版本实际使用 `NVIDIA GeForce RTX 4070 Laptop GPU`。
- 当前电源计划为“高性能”。
- 现有各版本 `options.txt` 不一致，例如 render distance 为 12 与 16；VSync 均开启、FPS 上限 120、分辨率未固定。
- 普通 `client` run 配置会设置 `eyelib.minimizeWindow=true`；这与真实性能测试冲突。benchmark 必须使用独立 run config，保持窗口可见且前台。
- `pauseOnLostFocus=true` 会在失焦时暂停，benchmark 必须显式关闭或保证持续前台。

### 6.2 运行协议

- 每个样本使用新 JVM；同一 JVM 内的帧不是独立实验样本。
- baseline/candidate 或版本顺序随机化/交错，避免温度和后台负载随时间单向漂移。
- 每个场景先进行 world/chunk/resource 稳定，再进入 warmup。
- 不假设固定 warmup 一定达到稳态；保存 warmup 数据，并用 changepoint/趋势检测验证。
- 初始至少做多次独立 JVM 运行；最终运行数由置信区间宽度决定，而不是只取“最好一次”。
- 外部干扰导致的异常运行必须标记原因；不能静默删除 outlier。

### 6.3 跨版本解释

三个节点分别使用 Java 17 / 21 / 25，且 26.1.2 采用声明式 extract/submit 渲染架构。跨版本绝对 FPS 可以展示，但不适合作为硬门禁。硬回归判断应比较“同一版本当前提交 vs 该版本基线”；跨版本对比仅用于架构趋势分析。

## 7. 统计协议

每个场景/版本应产生多个独立 JVM summary。比较变更时：

- 对相同机器和场景使用 paired ratio，例如 `candidate_p99 / baseline_p99`。
- 报告中心估计和 95% 置信区间。
- 不采用 best-of-N。
- 初始阶段不设置拍脑袋阈值。先收集 clean baseline，估计 run-to-run 噪声，再制定门禁。
- 门禁至少分别覆盖吞吐、p99、预算 miss 和长期 slope；单一平均 FPS 不能作为唯一门禁。

## 8. 外部观测器评估

### PresentMon

官方文档表明 PresentMon 可逐帧输出 presented/displayed FPS、CPU/GPU 时间、dropped frames、display latency 和 present mode；适合作为 Windows 显示链外部 oracle。

当前结论：**可选，不作为第一版依赖**。

- 本机未安装 `PresentMon.exe`，尚未完成本地验证。
- PresentMon API/文档的主要 runtime 枚举偏 DXGI/D3D9；官方仓库 issue 中有 OpenGL 正常采集的实例，但 Eyelib/Minecraft OpenGL 路径仍需自行做一次对照实验。
- 如果后续验证成功，可用它校验 in-process `frame_interval_ns`，并补充 dropped/displayed frame；不能替代 Eyelib 内部 render-work 计时。

### OpenGL / GPU timer

Khronos `ARB_timer_query` 说明 CPU timer 不能代表异步 GPU 完成时间，`glFinish()` 又会破坏流水线；异步 timer query 是正确方向。Minecraft 三版本已提供 `TimerQuery` 包装，无需从零证明 API 可用性，但需验证共享 query 冲突和 26.1.2 deferred submit 的测量边界。

## 9. 可证伪假设

后续实现前应先用最小实验验证：

1. `getFrameTimeNs()` 与自采 render Pre/Post 时间在三个版本中趋势一致。
2. 自采完整帧间隔与原版 FPS 图/FrameTimer 数据一致。
3. 开启采集器后的 FPS/p99 扰动低于基线 run-to-run 噪声。
4. FBO 单实体耗时随实体数近似单调增长；若不增长，说明测量边界没有覆盖实际提交。
5. 26.1.2 GPU query 结果覆盖 deferred submit 的真实 GPU 工作，而不是只覆盖命令构建。
6. 同一固定场景的可见实体数、Eyelib 接管率和资源包 hash 在多次运行中完全一致。
7. 长时间场景能区分 `flat`、JIT warmup 和持续 slowdown；人为注入稳定递增负载时必须检测出正 slope。
8. PresentMon 若被采用，其 OpenGL presented-frame 序列必须能与 in-process 帧序列对齐。

任何一项失败，都应修正测量模型，不能通过排除异常参数掩盖。

## 10. 推荐的下一工作单元

下一步仍不应直接做完整框架。先做一次“测量可行性实验”：

- 仅 1.20.1，一个固定空世界 + 一个固定 Eyelib 实体场景。
- 同时采 `frame_interval_ns`、`getFrameTimeNs()`、原版 FrameTimer、可选 GPU timer。
- 比较采集开启/关闭的扰动，并运行一次 10 分钟稳定性序列。
- 只有测量边界和扰动验证通过后，再设计跨版本 benchmark harness 与门禁阈值。

## 参考资料

- Intel PresentMon Console Application: https://github.com/GameTechDev/PresentMon/blob/main/README-ConsoleApplication.md
- Intel PresentMon API metrics: https://github.com/GameTechDev/PresentMon/blob/main/IntelPresentMon/PresentMonAPI2/PresentMonAPI.h
- Khronos `ARB_timer_query`: https://registry.khronos.org/OpenGL/extensions/ARB/ARB_timer_query.txt
- Georges et al., *Statistically Rigorous Java Performance Evaluation*: https://www2.ccs.neu.edu/racket/Performance/andy-georges-paper.pdf
- Barrett et al., *Virtual Machine Warmup Blows Hot and Cold*: https://soft-dev.org/pubs/html/barrett_bolz-tereick_killick_mount_tratt__virtual_machine_warmup_blows_hot_and_cold_v6/
- CapFrameX metric definitions and x% low ambiguity: https://www.capframex.com/blog/post/Explanation%20of%20different%20performance%20metrics
- Minecraft Wiki FPS graph (240-frame history): https://minecraft.wiki/w/Debug_screen#FPS_graph

## 内部证据

- `docs/perf/spark-baseline-and-optimizations.md`
- `docs/perf/spark-profiling-recipe.md`
- `build.gradle`
- `.mcmcp`
- `clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java`
- `clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeVisualHooks.java`
- `clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/EntitySceneRenderer.java`
- `src/main/java/io/github/tt432/eyelib/client/render/EntityRenderOrchestrator.java`
- `src/main/java/io/github/tt432/eyelib/bridge/client/ClientFrameTimePort.java`
- `versions/*/build/moddev/artifacts/*-sources.jar` 中的 `net.minecraft.client.Minecraft`、`ClientMetricsSamplersProvider`、`TimerQuery`

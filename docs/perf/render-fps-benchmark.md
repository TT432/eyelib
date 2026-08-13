# Eyelib 渲染 FPS / 稳定性 Benchmark

## 定位

`clientBenchmark` 是独立 dev client 运行模式，采集真实帧间隔、Minecraft 渲染工作时间、Eyelib 实体渲染数以及低频 heap/GC/CPU 数据。默认不会在普通 `client` 或发布环境启动。

工作负载：

- `fbo`：固定尺寸私有 framebuffer，隔离世界渲染与 present，测实体渲染吞吐。
- `world`：固定 flat world、相机与服务端实体，测主 framebuffer 端到端帧性能。
- `pacing`：世界场景分别限制 30/60/120 FPS，测预算违约和 hitch。
- `stability`：长时间世界场景，测 5 秒窗口、Theil–Sen slope 与稳定性分类。

26.1.2 的 FBO 路径沿用 clientsmoke 的 deferred submit 兼容实现，不执行 `renderAllFeatures`；其数据用于同版本回归，不能与 1.20.1/1.21.1 的绝对值直接比较。

## 默认运行

先按项目构建流程发布/构建 clientsmoke，然后执行版本节点的运行任务：

```bat
cmd /c gradlew.bat :1.20.1:runClientBenchmark
cmd /c gradlew.bat :1.21.1:runClientBenchmark
cmd /c gradlew.bat :26.1.2:runClientBenchmark
```

默认矩阵：

- FBO uncapped：实体数 `1,16,64`
- WORLD uncapped：实体数 `1,16,64`
- WORLD pacing：30/60/120 FPS，实体数 16
- WORLD stability：实体数 64，warmup 30 秒，measure 600 秒
- 其他场景：warmup 15 秒，measure 30 秒

运行期间窗口必须保持可见，不要叠加 spark、RenderDoc、F3 GPU 图、截图或 `/eval`。

## 参数覆盖

Gradle 将同名 JVM system property 转发给 client：

```bat
cmd /c gradlew.bat ^
  -Deyelib.benchmark.modes=fbo,world ^
  -Deyelib.benchmark.entity=minecraft:slime ^
  -Deyelib.benchmark.counts=1,16,64 ^
  -Deyelib.benchmark.warmupSeconds=15 ^
  -Deyelib.benchmark.measureSeconds=30 ^
  -Deyelib.benchmark.stabilizeTicks=100 ^
  -Deyelib.benchmark.maxSamples=2000000 ^
  :1.20.1:runClientBenchmark
```

支持参数：

| 属性 | 默认值 | 语义 |
|---|---:|---|
| `eyelib.benchmark.modes` | `fbo,world,pacing,stability` | 逗号分隔场景类型；增加 `mixed` 运行混合实体 FBO/WORLD throughput 场景 |
| `eyelib.benchmark.entity` | `minecraft:slime` | 单实体场景使用的 MC 实体注册表 ID |
| `eyelib.benchmark.entityMix` | `minecraft:slime=24,minecraft:zombie=24,minecraft:skeleton=24,minecraft:cow=24` | `entity_id=count` 逗号分隔；仅由 `mixed` 场景使用，最多 16 种、总数最多 4096 |
| `eyelib.benchmark.counts` | `1,16,64` | FBO/WORLD 单实体 uncapped 数量阶梯 |
| `eyelib.benchmark.warmupSeconds` | `15` | 普通/pacing warmup |
| `eyelib.benchmark.measureSeconds` | `30` | 普通/pacing measure |
| `eyelib.benchmark.pacingCount` | `16` | pacing 场景实体数 |
| `eyelib.benchmark.stabilityCount` | `64` | stability 场景实体数 |
| `eyelib.benchmark.stabilityWarmupSeconds` | `30` | stability warmup |
| `eyelib.benchmark.stabilitySeconds` | `600` | stability measure |
| `eyelib.benchmark.stabilizeTicks` | `100` | 进入世界后的稳定 tick 数 |
| `eyelib.benchmark.fboSize` | `512` | FBO 边长 |
| `eyelib.benchmark.maxSamples` | `2000000` | 每场景帧缓冲容量，满则失败 |
| `eyelib.benchmark.autoExit` | `true` | 完成后关闭客户端 |

混合场景示例（默认 4 种实体、总数 96，比当前单实体最大阶梯 64 高 50%）：

```bat
cmd /c gradlew.bat ^
  -Deyelib.benchmark.modes=mixed ^
  -Deyelib.benchmark.entityMix=minecraft:slime=24,minecraft:zombie=24,minecraft:skeleton=24,minecraft:cow=24 ^
  -Deyelib.benchmark.warmupSeconds=5 ^
  -Deyelib.benchmark.measureSeconds=30 ^
  :1.20.1:runClientBenchmark
```

混合场景默认不加入既有矩阵，避免破坏历史 baseline；显式加入 `mixed` 后会额外运行一个 FBO 和一个 WORLD 场景。FBO 中的实体必须是 `LivingEntity`。报告 metadata 的 `entity_mix` 固化顺序和数量。

压力建议：先用总数 96（约为当前最大 64 的 1.5 倍）验证，再按 128、192 阶梯增加。更高数量确实更容易暴露容量、批处理和 GC 问题，但实体种类多样性本身更容易暴露共享模型姿态、材质/动画状态泄漏；每次只改变总数或组成之一，并与同版本 baseline 对比。

短功能验证示例：

```bat
cmd /c gradlew.bat ^
  -Deyelib.benchmark.modes=fbo,world ^
  -Deyelib.benchmark.counts=1 ^
  -Deyelib.benchmark.warmupSeconds=1 ^
  -Deyelib.benchmark.measureSeconds=3 ^
  -Deyelib.benchmark.stabilizeTicks=5 ^
  :1.20.1:runClientBenchmark
```

该配置只证明生命周期和报告完整性，不能作为性能 baseline。

## 输出

输出目录：

```text
versions/<version>/run/benchmark/benchmark-reports/
  latest.json
  run-<timestamp>/
    run.json
    <scenario>/
      metadata.json
      summary.json
      frames.csv
      resources.csv
```

`frames.csv`：

- `timestamp_ns`
- `frame_interval_ns`
- `render_work_ns`：上一帧 `Minecraft.getFrameTimeNs()`
- `rendered_entity_count`：上一帧 Eyelib 成功渲染增量
- `phase`：`WARMUP` / `MEASURE`

`summary.json` 只统计 `MEASURE`：

- throughput FPS = 帧数 / 首尾帧时间
- frame interval 与 render work 的 P50/P95/P99/P99.9
- P99-derived FPS
- 30/60/120 FPS budget miss、2x/3x hitch、最长连续 miss
- 5 秒窗口 P50/P99
- 窗口 P50 Theil–Sen slope (`ms/min`)
- `flat / slowdown / warmup / insufficient-data`

`resources.csv` 每秒记录 heap used/committed、累计 GC count/pause 和 process CPU load；JVM/OS 管理接口在独立 daemon 线程采样，渲染线程只复制已发布的 primitive snapshot。

## 结果有效性

一个场景必须同时满足：

- `status == "passed"`
- `metadata.frame_sample_overflow == false`
- `metadata.configured_entity_count == metadata.actual_entity_count`
- `summary.frame_count` 等于 `frames.csv` 中 `phase=MEASURE` 的行数
- 运行期间没有改变资源包、相机、窗口、FPS/VSync 或实体负载

Pacing 场景只有在 uncapped 吞吐明显高于目标 FPS 时才有解释力。未满足目标的结果是吞吐不足，不是 pacing 证据。

每次可比较样本必须使用 fresh JVM；baseline/candidate 顺序应交错。不要使用 best-of-N，不要静默删除异常运行。跨 MC 大版本只展示趋势，硬回归判断使用同版本 baseline。

## 统计不变量与采样契约

统计不变量（任何场景、任何版本都必须满足）：

- throughput 定义为测量期帧数 / 测量期首尾时间戳间隔，禁止报告「瞬时 FPS 的算术平均值」。
- percentile 一律在帧时间域计算，字段名必须标明域（如 `p99_frame_interval_ms`）。
- `budget_miss` 使用严格不等式 `frame_interval_ns > budget`。
- hitch 阈值为目标 budget 的 2x/3x。
- 趋势使用 5 秒窗口 P50 两两斜率的 Theil–Sen 中位数，单位 `ms/min`。
- 无硬编码回归门禁；本实现只生成建立 baseline 所需证据，门禁阈值在估计 run-to-run 噪声后另行制定。

采样契约：

- 采样点在 render-frame Pre/START 事件；首帧没有前序 interval，不记录。
- 帧缓冲预分配 primitive array；buffer 满即场景失败，不覆盖、不丢弃旧数据。
- `rendered_entity_count` 必须是上一帧 Eyelib 成功渲染的真实增量；取不到时明确标记不可用，禁止用配置数量伪装。
- VSync 关闭、camera 每 tick 固定、WORLD 实体 NoAI/silent/invulnerable/固定位置、场景结束清除带 benchmark tag 的实体、实际实体数量不等于配置数量时场景失败。
- benchmark 与 clientsmoke 互斥：同时启用时启动失败，避免两个世界状态机竞争。
- 测量中不主动 `System.gc()`，不静默剔除 outlier。

## 测量模型与方法论

计时边界：

- `Minecraft.getFrameTimeNs()` 是 CPU 侧渲染工作时间：从渲染工作开始计时，包含 update/extract/render/blit，在 swap/present 和 FPS limiter 之前结束。因此它不等于用户看到的完整帧间隔，后者必须另行记录相邻帧 Pre/START 时间戳差。
- `ClientFrameTimePort.getFrameTime()` 返回 partial tick 插值用的时间，不是帧耗时，禁止用于 FPS 统计。
- `TimerQuery` 异步 GPU timer 是正确方向（`glFinish()` 会系统性干扰流水线），但它是共享/单例设施，正式使用必须验证与 F3 GPU 图、原版 metrics recorder 的共享 query 冲突，以及 26.1.2 deferred submit 的测量边界。

统计协议：

- 比较变更时对相同机器和场景使用 paired ratio（如 `candidate_p99 / baseline_p99`），报告中心估计和 95% 置信区间。
- 每个样本使用 fresh JVM；同一 JVM 内的帧不构成独立实验样本。
- baseline/candidate 顺序随机化或交错，避免温度和后台负载随时间单向漂移。
- 禁止 best-of-N；运行数由置信区间宽度决定。
- 「1% low」在工具间有 percentile、最差 1% 平均、按时间积分三种定义，存在歧义；报告直接写 `P99 frame time` 及其换算 FPS（`p99-derived FPS = 1e9 / p99(frame_interval_ns)`）。
- 硬回归只与同一 MC 版本的 baseline 比较；跨版本（Java 17/21/25、26.1.2 声明式渲染架构差异）仅用于趋势分析。

稳定性判据（不合并为单一分数，逐项可解释）：

1. 分布宽度：`p99 - p50`、`p99 / p50`。
2. 预算违约：miss ratio、hitch count、最长连续 hitch。
3. 时间趋势：5 秒窗口 P50/P99 的 Theil–Sen slope。
4. 状态分类：`flat / warmup / slowdown / no-steady-state`。
5. 资源相关性：帧时间恶化是否与 heap、GC pause、rendered entity count 同步。

外部观测器：PresentMon 可作为 Windows 显示链外部 oracle（逐帧 presented/displayed FPS、dropped frames、display latency），但其 OpenGL 路径在本项目尚未验证，为可选增强而非第一版依赖；验证通过后可用来校验 in-process `frame_interval_ns`，不能替代内部 render-work 计时。

可证伪假设清单（实现与解释结果前必须用最小实验验证，任一失败则修正测量模型而非排除数据）：

1. `getFrameTimeNs()` 与自采 render Pre/Post 时间在三个版本中趋势一致。
2. 自采完整帧间隔与原版 FPS 图/FrameTimer 数据一致。
3. 开启采集器后的 FPS/P99 扰动低于基线 run-to-run 噪声。
4. FBO 单实体耗时随实体数近似单调增长；若不增长，说明测量边界没有覆盖实际提交。
5. 26.1.2 GPU query 结果覆盖 deferred submit 的真实 GPU 工作，而非只覆盖命令构建。
6. 同一固定场景的可见实体数、Eyelib 接管率和资源包 hash 在多次运行中完全一致。
7. 长时间场景能区分 `flat`、JIT warmup 和持续 slowdown；人为注入稳定递增负载时必须检测出正 slope。
8. 若采用 PresentMon，其 OpenGL presented-frame 序列必须能与 in-process 帧序列对齐。

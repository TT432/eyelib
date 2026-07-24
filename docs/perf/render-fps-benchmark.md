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
| `eyelib.benchmark.modes` | `fbo,world,pacing,stability` | 逗号分隔场景类型 |
| `eyelib.benchmark.entity` | `minecraft:slime` | MC 实体注册表 ID |
| `eyelib.benchmark.counts` | `1,16,64` | FBO/WORLD uncapped 数量阶梯 |
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

`resources.csv` 每秒记录 heap used/committed、累计 GC count/pause 和 process CPU load。

## 结果有效性

一个场景必须同时满足：

- `status == "passed"`
- `metadata.frame_sample_overflow == false`
- `metadata.configured_entity_count == metadata.actual_entity_count`
- `summary.frame_count` 等于 `frames.csv` 中 `phase=MEASURE` 的行数
- 运行期间没有改变资源包、相机、窗口、FPS/VSync 或实体负载

Pacing 场景只有在 uncapped 吞吐明显高于目标 FPS 时才有解释力。未满足目标的结果是吞吐不足，不是 pacing 证据。

每次可比较样本必须使用 fresh JVM；baseline/candidate 顺序应交错。不要使用 best-of-N，不要静默删除异常运行。跨 MC 大版本只展示趋势，硬回归判断使用同版本 baseline。

# C1 GPU 蒙皮 — Benchmark 基线（26.1.2，C1 实施前）

> 采集：2026-08-27 00:08，run-20260827-000842。环境：MC 26.1.2.78 / Java 25.0.3 / NVIDIA /
> Windows 11。场景：mixed-n96（slime/zombie/skeleton/cow 各 24，A&S 1.10 v2 资源包，
> sha256 见 run metadata）。warmup 15s / measure 30s，各 900 MEASURE 帧。
> 代码基线：commit d3f11518（C1 实施前）。

| 场景 | render_work P50 | render_work P95 | render_work P99 | render_work P99.9 |
|---|---:|---:|---:|---:|
| fbo-mixed-n96 | 2.530 ms | 3.076 ms | 3.506 ms | 5.788 ms |
| world-mixed-n96 | 12.478 ms | 15.979 ms | 18.158 ms | 22.201 ms |

有效性检查：两场景 status=passed；configured=actual=96；无 sample overflow。
frame_interval P50 恒为 33.33ms（30 FPS 上限生效），因此帧间隔指标不用于 C1 对比，
只用 render_work（CPU 渲染工作时间，C1 的直接作用域）。
**重要（2026-08-27 读码确认）**：26.1.2 的 FBO 场景经 clientsmoke `EntitySceneRenderer`
渲染，其 26.1.2 分支**跳过 `renderAllFeatures()`**（ShadowFeatureRenderer 在 FBO 上下文
崩 "Color texture is closed"，源码注释），eyelib 的 DeferredRenderSink writer 回调
（submitCustomGeometry 节点）在 FBO 场景**从不执行**——fbo-mixed-n96 的 2.53ms 不含
eyelib 几何渲染成本，只是场景开销下限。**C1 的对比指标以 world-mixed-n96
render_work 为准**（world 路径经 LevelRenderer renderSolid/TranslucentFeatures，writer 真实执行）。
FBO 像素回归若需在 26.1.2 做，需另觅路径（world 截图或 clientsmoke 修复后单独发版）。

对比纪律（render-fps-benchmark.md）：candidate 与 baseline 同机同场景、fresh JVM、
交错顺序；报告 paired ratio，不用 best-of-N。26.1.2 FBO 路径为 clientsmoke deferred
submit 兼容实现，绝对值不与 1.20.1/1.21.1 交叉比较。

历史参照（不可直接比）：1.20.1 同场景 run-20260724-231836 fbo P50 8.57ms / world P50 46.73ms
（Java 17 + MC 1.20.1，仅作趋势参照）。

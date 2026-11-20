# C2 静态实例化（非动画 ClientEntity）Benchmark 结果

> 日期：2026-11-19。决策：`docs/decisions/0048-static-entity-instancing.md`；设计：`design/c2-static-entity-instancing-design.md`；
> 基准方法：`render-fps-benchmark.md`。

## 场景口径

- 实体：`minecraft:cow`（Actions-and-Stuff 1.10 v2 的 Bedrock client entity），阶梯 `64`、`192`；
  场景类型 `fbo`（512² 离屏，纯渲染）+ `world`（世界内，含 vanilla 世界渲染）。
- 三档渲染路径，同一场景同一实体，只有路径开关不同：

| 档 | 开关 | 路径 |
|---|---|---|
| CPU | `-Deyelib.gpuSkinning=false` | 经典 CPU 顶点蒙皮（无 GPU 会话） |
| C1 | `-Deyelib.staticEntities=true -Deyelib.staticInstances=false` | GPU 蒙皮（compute + 逐实体 palette 上传） |
| C2 | `-Deyelib.staticEntities=true` | 静态实例通道（SSBO 实例数据 + `glDrawArraysInstanced`） |

- `-Deyelib.benchmark.staticEntities=true` 把基准实体的 client entity 换成去掉
  `animations`/`animation_controllers` 的副本（`BenchmarkStaticBinder`），这是 C2 的唯一触发条件——
  两次 run 之间不存在其它代码差异，因此 C1/C2 的差值就是静态通道本身。
- 每场景 15s warmup + 30s measure，fresh JVM，`autoExit`；`actual_entity_count` 与配置一致。

## 1.20.1（RTX 4070 Laptop，A&S 1.10 v2）

`render_work_*` 为客户端侧渲染工作量（不含等待垂直同步/限帧），是路径对比的判据。

| 场景 | 档 | FPS | fi_p50 | rw_p50 | rw_p99 |
|---|---|---:|---:|---:|---:|
| fbo n64 | CPU | 94.0 | 10.18 | 9.56 | 15.67 |
| fbo n64 | C1 | 112.4 | 8.62 | 7.93 | 11.90 |
| fbo n64 | C2 | 158.2 | 6.17 | 5.78 | 8.06 |
| fbo n192 | CPU | 42.5 | 23.07 | 22.19 | 29.90 |
| fbo n192 | C1 | 58.3 | 16.87 | 16.11 | 20.88 |
| fbo n192 | C2 | 72.4 | 13.48 | 12.79 | 18.20 |
| world n64 | CPU | 136.7 | 7.05 | 6.53 | 10.78 |
| world n64 | C1 | 179.0 | 5.33 | 4.88 | 7.75 |
| world n64 | C2 | 197.3 | 4.80 | 4.30 | 6.99 |
| world n192 | CPU | 50.7 | 19.06 | 17.96 | 28.01 |
| world n192 | C1 | 72.2 | 13.53 | 12.55 | 17.60 |
| world n192 | C2 | 86.4 | 11.27 | 10.49 | 15.19 |

相对变化（`rw_p50`）：

| 场景 | C1 vs CPU | C2 vs C1 | C2 vs CPU |
|---|---:|---:|---:|
| fbo n64 | −17% | −27% | −40% |
| fbo n192 | −27% | −21% | −42% |
| world n64 | −25% | −12% | −34% |
| world n192 | −30% | −16% | −42% |

## 26.1.2（RTX 4070 Laptop，A&S 1.10 v2）

26.1.2 的基准场景被 30 FPS 上限钳住（`fi_p50` ≈ 33.3 ms，历史全部 26.1.2 报告同此，属既有 harness 限制），
FPS 列不可比，判据取 `render_work`。26.1 的静态通道**不做实例合批**（逐 draw 写 UBO），收益仅来自上传量与
compute 消除。

| 场景 | 档 | FPS | fi_p50 | rw_p50 | rw_p99 |
|---|---|---:|---:|---:|---:|
| fbo n64 | C1 | 30.0 | 33.33 | 3.54 | 5.48 |
| fbo n64 | C2 | 89.1 | 2.48 | 2.28 | 4.80 |
| fbo n192 | C1 | 30.0 | 33.35 | 6.47 | 9.02 |
| fbo n192 | C2 | 30.0 | 33.33 | 5.04 | 8.38 |
| world n64 | C1 | 30.0 | 33.35 | 4.42 | 6.65 |
| world n64 | C2 | 30.0 | 33.36 | 2.99 | 5.13 |
| world n192 | C1 | 30.0 | 33.37 | 10.95 | 16.62 |
| world n192 | C2 | 30.0 | 33.37 | 9.76 | 15.37 |

| 场景 | C2 vs C1 |
|---|---:|
| fbo n64 | −36% |
| fbo n192 | −22% |
| world n64 | −32% |
| world n192 | −11% |

## 1.21.1（RTX 4070 Laptop，A&S 1.10 v2）

| 场景 | 档 | FPS | fi_p50 | rw_p50 | rw_p99 |
|---|---|---:|---:|---:|---:|
| fbo n64 | C1 | 113.7 | 8.59 | 7.85 | 12.08 |
| fbo n64 | C2 | 136.3 | 7.10 | 6.40 | 10.38 |
| fbo n192 | C1 | 49.6 | 19.83 | 18.68 | 26.34 |
| fbo n192 | C2 | 58.4 | 16.75 | 15.81 | 25.02 |
| world n64 | C1 | 151.7 | 6.40 | 5.72 | 9.05 |
| world n64 | C2 | 170.4 | 5.66 | 5.00 | 8.20 |
| world n192 | C1 | 63.9 | 15.48 | 14.24 | 20.64 |
| world n192 | C2 | 78.0 | 12.39 | 11.42 | 17.63 |

| 场景 | C2 vs C1 |
|---|---:|
| fbo n64 | −18% |
| fbo n192 | −15% |
| world n64 | −13% |
| world n192 | −20% |

## 结论

- **≤26.1 静态实例通道（compute + SSBO + `glDrawArraysInstanced`）在 1.20.1 与 1.21.1 上都为正收益**：
  相对 C1 降 `rw_p50` 12–27%，相对经典 CPU 蒙皮降 34–42%；192 实体的绝对收益（1.20.1 fbo 3.3 ms / world 2.1 ms）
  显著大于 64 实体，符合「每帧上传从 `slotCount×128B + 逐顶点 80B` 降为 144B/实例」的结构性解释。
- **26.1.2 同样为正收益（−11%～−36%）但机理不同**：26.1 静态通道逐 draw 写 UBO、不减少 draw 次数，
  收益来自取消 compute 与上传；该版本基准被 30 FPS 上限钳住，只能用 `render_work` 判定。
- **world 场景的收益小于 fbo**（1.20.1 −12%/−16% vs fbo −27%/−21%）：world 的帧时间被 vanilla 世界渲染摊薄，
  与 C1 的结论一致。
- **判定有效的证据**：三次 run 的唯一差异是 `-Deyelib.staticInstances` / `-Deyelib.gpuSkinning` 开关与
  `-Deyelib.benchmark.staticEntities` 绑定；C1/C2 的差值只在静态通道生效时可能出现（否则两 run 走同一路径，
  差异应在噪声内），实测差值 11–36% 远超本机 ±5% 噪声。

## 已知限制

- 单机单 GPU（RTX 4070 Laptop）、单实体模型（A&S 牛）、fbo 512²、每场景 15s warmup + 30s measure、每档单次采样；
  串行 run 而非 ON/OFF 交错，因此只给区间结论，不解释单点小差异。
- 1.20.1 的 fbo 两档与 world n64 的 C1 有 `stability_classification=warmup|slowdown` 标记（窗口内存在漂移），
  对应数值仍落在与其它档不重叠的区间内。
- 26.1.2 的 FPS 列被 30 FPS 上限钳住，不参与对比；`fbo-n64` 的 C2 是唯一突破该上限的场景（89 fps）。

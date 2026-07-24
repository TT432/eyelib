# Eyelib 渲染稳定性泄漏定位规格

## 问题模型

有效 1.20.1 `WORLD stability n64` 运行中，固定实体和相机下出现：

- heap 每分钟 P10：约 2225 → 3021 MiB；Theil–Sen 约 +73.66 MiB/min。
- heap median：约 2375 → 3370 MiB；committed：约 2832 → 4336 MiB。
- 600 秒内 GC count +2675、pause +10976 ms。
- 帧 P50 近似平稳，但末 60 秒窗口 P99 相对首 60 秒上升约 32.8%。

单次运行不能区分以下模型：

1. **GC ergonomics**：committed heap 扩张，但 full-GC 后 live set 稳定。
2. **真实 Java heap retention**：full-GC 后对象数/字节持续增长。
3. **固定上限缓存 warmup**：前期增长，随后 live set 平台化。
4. **每帧无界缓存/监听器/上下文累积**：对象数与时间或帧数近似线性增长。
5. **benchmark 场景生命周期残留**：实体/RenderData 数超过固定 64。
6. **native/GPU 泄漏**：Java live set稳定但进程/native/GPU 资源增长；本轮先证明或排除 Java heap，再进入 native 路径。

## 判别实验

使用 fresh JVM，只运行 `world-stability-n64`：warmup 30 秒、measure 600 秒、autoExit=false。

### 采集

- 启动后 attach JFR `profile` recording，启用 old-object GC-root 路径，覆盖完整场景。
- 在 measurement 开始、约 5 分钟、结束后分别执行 `jcmd GC.class_histogram`。该命令会触发 full GC，因此本轮只用于内存归因，不把 FPS 结果作为性能 baseline。
- 每个 histogram 同时保存 `GC.heap_info`。
- 结束后 dump JFR，再正常关闭客户端。

### 判定

- 若 histogram 总 live bytes 在三次 full GC 后稳定：否定 Java heap leak，转查 native/GPU 或 GC 配置。
- 若总 live bytes 持续增长：按 class 的 `bytes_delta` 和 `instances_delta` 排序。
- 若增长类属于 Eyelib 数据结构：用 JFR `OldObjectSample` GC root chain 与 allocation stack 定位持有者和创建路径。
- 若主要增长为 MC/Forge/JDK 容器节点：继续展开其 value 类型和 GC root，不能仅凭 `HashMap$Node` 下结论。
- `Entity`、`RenderData`、model component 数必须接近固定场景上限；超过或持续增长即指向生命周期清理。
- 只有在两个 fresh JVM 中重复出现同一 live-class 增长和 root chain，才把它定性为泄漏。

## 副作用

- `GC.class_histogram` 触发 stop-the-world full GC，会污染该诊断 run 的帧时间与 GC 指标。
- JFR `profile` 有额外开销；只用于归因，不与无 JFR baseline 比较 FPS。
- 采集文件写入 `work/render-benchmark-leak-investigation/`。

## 第一轮后置条件

- 获得三份 live histogram、heap info 和一份 JFR。
- 给出增长最大的对象类型、增长斜率、是否平台化、至少一个 GC root/allocation 证据。
- 若证据不足，明确下一轮需要的更窄插桩或 heap dump，不猜测责任类。

# 渲染稳定性 Heap 泄漏定位案例（RenderHelper EventBus listener）

记录一次在 1.20.1 `WORLD stability n64` 场景中定位并修复 Java heap 确定性泄漏的完整过程，作为同类问题的诊断模板。

## 泄漏模式

**实例初始化器向全局 (Neo)Forge EventBus 注册监听器是确定性 heap retention。**

`RenderHelper.start()` 每次返回 `new RenderHelper()`；原实现把 `ManagerEntryChangedEventPublisher.addListener(...)` 写在实例初始化器中，于是每创建一个 helper 就向 EventBus 注册一个新 listener。EventBus 以静态强引用（`static EVENT_BUS` → `ConcurrentHashMap listeners`）持有所有 listener，GC 无法回收。在 64 实体多 ModelComponent 负载下约为 **120 个新 listener/帧（约 8,000/秒）**，600 秒测量期 full-GC 后 retained bytes 增长约 904 MiB。

识别特征：histogram 中主导增长的类成组出现且增量一致——`ManagerEntryChangedEventPublisher$$Lambda`、`EventBus$$Lambda`、`Collections$SynchronizedRandomAccessList`、`ConcurrentHashMap$Node`——这是 EventBus listener 注册的典型容器指纹。

## 修复

修复为**幂等单次注册**：`RenderHelper.start()` 调用 `installInvalidationListener()`，由 `AtomicBoolean` 守卫保证只注册一次，DFS model cache 的 manager-entry 失效行为不变（`src/main/java/io/github/tt432/eyelib/client/render/RenderHelper.java`，与 `ModelBakeInvalidationHooks.install()` 同模式）。

注意：不得用 `static {}` 块做此类注册——ADR-0018 Q-2 禁止 `static {}` 业务 wiring（避免正确性依赖类加载顺序，ArchUnit 规则 `applicationMustNotHaveStaticInitializer` 强制），见 [0018-isolated-quiescent-fragments.md](../decisions/0018-isolated-quiescent-fragments.md)。

验证：另一个 fresh JVM 固定 64 实体运行 120 秒，起终点 full-GC histogram 对比——`ManagerEntryChangedEventPublisher$$Lambda` 实例数固定为 2，所有 `EventBus$$Lambda` 固定为 88，full-GC live bytes 无增长（-15.6 MiB 噪声内）。

## 定位方法论

症状先行判别：stability 运行中 heap P10 每分钟约 +74 MiB、GC count/pause 持续上升，但单次运行无法区分 GC ergonomics / 真实 retention / 缓存 warmup / 无界累积 / 生命周期残留 / native 泄漏。判别实验：

1. **fresh JVM** 只跑目标场景（warmup 30 秒、measure 600 秒、autoExit=false），排除跨场景残留。
2. attach **JFR profile** recording，启用 `path-to-gc-roots=true`，覆盖完整场景。
3. 在 measurement 起点、中段、终点执行 `jcmd GC.class_histogram`。该命令**触发 full GC**，因此三次采样对比的是 retained live set 而非分配速率；live bytes 持续增长才可定性为泄漏，稳定则转查 native/GPU。
4. 增长类按 `bytes_delta` / `instances_delta` 排序；若主导增长是 JDK 容器节点（`HashMap$Node` 等），必须继续展开 value 类型与 GC root，不能仅凭容器类下结论。
5. 用 JFR `OldObjectSample` 的 **GC root chain**（本例：listener list → ConcurrentHashMap → EventBus → static EVENT_BUS → Class）和**分配栈**（`EventBus.addListener`）闭合到源码路径。
6. 同一 live-class 增长与 root chain 需在两个 fresh JVM 中重复出现才定性为泄漏。

注意事项：

- `GC.class_histogram` 的 stop-the-world full GC 和 JFR profile 开销都会污染该 run 的帧时间；**诊断 run 的 FPS 不得与无 JFR baseline 比较**，只用于内存归因。
- 修复验证同样用 fresh JVM + 起终点 full-GC histogram 对比，看目标类实例数是否固定、live set 是否停止增长。

## 排除项（本例）

源码与 histogram 均不支持以下为主泄漏：每帧局部对象（`FramePlan`、`ModelVisitContext`）；`ModelComponent` 每帧重建（高分配压力但非 retained 增长）；`RenderData`/实体 attachment（实体数固定 64 无增长）；key 空间受固定资源限定的缓存（`dfsModels`、two-side baked cache、动画 controller data）。

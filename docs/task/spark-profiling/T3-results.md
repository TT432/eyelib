# T3 资源重载 CPU Profile 结果

## 测试条件
- 线程：Render thread（`/sparkc profiler`，无 --thread）
- 采样：90s，Java sampler，interval=4000ns
- 负载：59 个 eyelib slime + 12 sheep（同 T2），fps=4-6（重载期低位）
- 触发：`Minecraft.reloadResourcePacks()`（F3+T 等效）
- profile URL：https://spark.lucko.me/EA6AAqdMUF
- 原始数据：`data/t3-reload-pb.bin`（3.07MB），`data/reload-stacknodes.csv`（22653 节点）

## 核心发现（按 CPU 占比排序）

> 注：self-time 总和超 100% 是 protobuf DAG 压缩结构（共享子节点）的伪影，Top-N 相对排名可靠。

### 1. ⚠️ BedrockAddonRuntimeBridge.replaceFromResourcePack 占 54.6% —— 资源重载头号热点
- `lambda$replaceFromResourcePack$2` = 43624ms（48.5%）
- `Registry.put` = 43484ms（48.3%）
- `Forge EventBus.doCastFilter` = 42988ms（47.8%）—— Registry.put 每次写入都发事件

**根因（源码确认）**：
- `Registry.put`（Registry.java:44-47）每次调用：① `ref.updateAndGet(snap -> snap.with(id, value))` copy-on-write 全量复制快照（逐条 put 致 O(N²)）；② `publisher.publishManagerEntryChanged(...)` 每条都发事件。
- `BedrockAddonRuntimeBridge.replaceFromResourcePack`（BedrockAddonRuntimeBridge.java:60-79）**材质（line 63-64）和渲染控制器（line 76）逐条 `Registry.put`**：
  ```java
  value.materials().forEach(MaterialManager.INSTANCE::put);       // line 64
  RenderControllerManager.INSTANCE.put(key, entry);               // line 76
  ```
- 对比：同方法内实体（line 48）、attachable（line 56）、模型（line 59）已用 `replaceAll` 批量写入。**唯独材质和渲染控制器未走批量路径**。
- `Registry.replaceAll`（Registry.java:50-52）已存在：`ref.set(RegistrySnapshot.copyOf(replacement))` 一次性替换，只复制一次。但材质注释说"叠加而非替换，保留 vanilla 条目"，不能直接 replaceAll。

### 2. 渲染重建阶段占 ~20%（重载后重新初始化所有实体渲染器）
- EntityRenderOrchestrator$TickStage = 17872ms（19.9%）
- BrAnimator.tickAnimation = 17440ms（19.4%）
- EntityRenderOrchestrator.setupClientEntity = 16884ms（18.8%）
- RenderControllerEntry.setupModel = 16400ms（18.2%）
- BrControllerExecutor.tick = 10572ms（11.7%）

### 3. findField 占 4.9%（4420ms）+ fillInStackTrace 4296ms
- 重载期占比从 T2 渲染期的 76% 降到 4.9%（因重载有更大的加载开销），但绝对值仍可观。

## 优化建议（T5 详述）
1. **Registry 增加 putAll 批量方法**：一次 copy-on-write（合并新条目进快照）+ 一次事件（或 batch 事件），消除 O(N²) 复制和 N×事件分发。保留"叠加"语义。材质和渲染控制器改用 putAll。
2. findField 同 T2（预建字段 Map）。

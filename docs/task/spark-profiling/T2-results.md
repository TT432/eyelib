# T2 稳态渲染 CPU Profile 结果

## 测试条件
- 线程：Render thread（client 主线程，`/sparkc profiler`）
- 采样：60s，Java sampler（async-profiler 在 dev JVM 不可用），interval=4000ns
- 负载：59 个 eyelib 接管渲染的 slime（useBuiltInRenderSystem=true）+ 12 sheep
- fps：13（负载确实重）
- profile URL：https://spark.lucko.me/YsDkEySi5t
- 原始数据：`data/t2-render-pb-sparkc.bin`（2.8MB），`data/render-stacknodes.csv`（20932 节点）

## 命令路径（权威）
- **必须用 `/sparkc`（Forge 客户端）**，不是 `/spark`。`/spark` 是 server-side，默认只采 Server thread，永远采不到 Render thread。
- 执行方式：`ClientCommandHandler.runCommand("sparkc profiler start --timeout 60")`（反射调用 static 方法）。
- **不要加 `--thread "Render thread"`**：引号在 runCommand 里会被 brigadier 解析吞掉，导致 threadDumper.ids=[]，采样数据为空（threads field 不存在，protobuf 仅 6KB）。
- client-side `/sparkc profiler`（无 --thread）默认采样 client 主线程 = Render thread，正好是目标。
- 拿 URL：`runCommand("sparkc activity")` → 反射读 `ChatComponent.allMessages`（private 字段，`GuiMessage.toString()` 提取 URL）。

## 核心发现（按 CPU 占比排序）

### 1. ⚠️ fillInStackTrace 占 76% CPU —— 头号优化点
- **45512ms / 60000ms（76%）**，526 个异常节点，**全部来自 `io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.findField`**。
- 根因：`findField`（MolangMappingTree.java:166-170）用 `Class.getField(fieldName)` + `catch NoSuchFieldException` 做字段查找。字段不存在是常态（Molang 查询每帧尝试大量不存在的字段名），每次失败都创建异常 → fillInStackTrace 开销巨大。
- 对比：同文件 `findMethod`（line 211）用 `node.actualFunctions.get(methodName)`（Map 查找），不抛异常，无此问题。
- 影响链：findField ← VariantSelector（self 928ms）← MolangRuntimeSupport（self 916ms）← Molang 查询热路径（每帧执行）。

### 2. 正则 Pattern.compile 占 8.9%（5312ms）
- `RenderControllerRuntime.lambda$setup$2` = 2408ms + `Pattern.<init>` 2224ms + `Pattern.sequence` 680ms。
- 热路径反复编译正则（未缓存），需查源码确认。

### 3. Render thread 调用结构
- `Minecraft.runTick`(59992ms) → `GameRenderer.render`(56484, 94%) → `renderLevel`(56200, 94%) → `LevelRenderer.renderLevel`(56036, 93%) → **Forge `EventBus.post`(51616, 86%)**。
- 86% 时间在 Forge EventBus.post = RenderLevelStageEvent，eyelib 渲染挂载于此。

### 4. eyelib 业务逻辑 self-time 仅 11%（6580ms）
- VariantSelector 928ms、MolangRuntimeSupport 916ms、BrMaterialResolver 792ms、HandwrittenMolangAstParserFrontend 788ms、Tokenizer 568ms。
- 注：这些 self-time 的很大一部分本质是 findField 抛异常的开销（被算到调用链各层）。

## 优化建议（T5 详述）
1. **findField：消除异常控制流**。字段是静态反射结构，在 `addNode` 注册类时预建 `Map<String, Field>`（按名小写），findField 直接 Map.get 返回 null，永不抛异常。预期收益：释放 76% Render thread CPU。
2. **正则编译缓存**：RenderControllerRuntime.setup 中的 Pattern.compile 加缓存。

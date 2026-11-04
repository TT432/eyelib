# ADR-0038：实体姿态管道必须早于光影 shadow pass

## 状态

已接受（2026-11-03）。修复 Iris/Oculus 光影包下自定义实体整片发黑 / 巨大黑色几何面。

## 背景

症状（1.21.1 NeoForge + Iris 1.8.12 + MakeUp-UltraFast 9.6，dokimobs 的 Bedrock 实体）：

- 世界明亮，但 eyelib 实体（如萌化流浪商人）整片发黑，近似只剩环境光；
- 模型自带的大件道具（伞架、背包）在近景时表现为铺满屏幕的黑色平面；
- 关闭光影或关闭着色器包的 shadow casting 后恢复正常；
- 同场景 vanilla 生物（盔甲架）正常。

### 根因（实证）

Iris/Oculus 的 shadow pass 在 `LevelRenderer.renderLevel` 早期（sky 之前）执行，而 eyelib 的
`@OnRenderStage`（SETUP→EFFECT_COMMIT→TICK 管道）原先挂在本帧较晚的
`RenderLevelStageEvent.AFTER_SKY`（26.1 为 `AfterOpaqueBlocks`）上。事件顺序探针实测每帧：

```text
ENTITY shadow=true      ← shadow pass 用上一帧姿态写 shadow map
AFTER_SKY               ← eyelib 本帧姿态/动画 tick
ENTITY shadow=false     ← 主渲染用本帧姿态
```

于是主渲染可见面落在“上一帧自身轮廓”投出的阴影里；动画幅度越大、模型自带遮挡物越多，
整片发黑越明显。vanilla 生物不受影响，因为它们的 `setupAnim` 在 `render()` 内、两次 pass 各算一次。

### 验证

- 冻结实体动画（`AnimationComponent.serializableInfo = null`，姿态静止）后，同样开启 shadow
  casting，模型立即恢复正常亮度 → 排除材质/法线/光照贴图假设；
- 事件顺序探针（Iris `isRenderingShadowPass` + `RenderLevelStageEvent.AFTER_SKY`）逐帧输出上述顺序；
- 修复后同一场景（动画恢复）模型亮度与关闭 shadow casting 时一致。

## 决策

`RenderStageEventAdapter` 改为在**帧渲染开始前**触发 `@OnRenderStage` 管道：

- `>=1.20.6`（NeoForge）：`RenderFrameEvent.Pre`（在 `GameRenderer.render` 之前）；
- `<1.20.6`（Forge）：`TickEvent.RenderTickEvent` 的 `Phase.START`（在 `gameRenderer.render` 之前）。

此时序晚于客户端 tick（实体位置/动画状态为当前帧），早于 shadow pass 与主实体渲染，
因此 shadow map 与主渲染使用同一份 `tickedInfos`。

## 后果

- `@OnRenderStage` 契约从“渲染阶段事件”改为“帧渲染前”；契约不变（方法签名不变），时机语义更早。
- 该时刻 `GameRenderer.renderLevel` 尚未执行 `Camera.setup`，`getMainCamera()` 的位置是上一帧的。
  该坐标仅用于 `SetupStage` 的 `shouldRender` 距离剔除；动画本身不依赖相机位置，
  相机查询类 molang 允许一帧滞后（与原本 `AFTER_SKY` 的当帧值相比是唯一可观察差异）。
- 1.20.1/1.21.1 的 GPU 蒙皮合批窗口仍由 `SkinningBatchStageHook` 的
  `AFTER_SKY`→`AFTER_ENTITIES` 管理，不受本 ADR 影响。
- 26.1.2 的延迟提交（`SubmitNodeCollector`）在 `renderAllFeatures` 读取姿态；管道提前后
  shadow pass 与主 pass 读取同一姿态。

## 验证记录

- 构建：`1.20.1` / `1.21.1` / `26.1.2` compileJava 通过（26.1 的 `Camera.position()` 访问器差异已 `//?` 分支）。
- 实机（1.21.1 dev client + Iris 1.8.12 + MakeUp 9.6 + shadow casting on）：
  修复前动画模型整片黑，修复后与关闭 shadow casting 的亮度一致；阴影 pass 仍按当前帧姿态生成。
- 未覆盖：1.20.1 Oculus 实机（本机无 Oculus 实例）；26.1.2 光影（Iris 尚未支持 26.1）。

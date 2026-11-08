# 2026-11-06 搬运方块（末影人 carried block）渲染

## 背景

Bedrock 的 `query.is_carrying_block` 与 `carrying` 剪辑只负责实体姿态；方块本体由引擎按实体
carried block state 绘制，资源包（`entity/enderman.entity.json` 的
`controller.animation.enderman.carrying`）不定义方块几何。eyelib 接管实体渲染并在
`RenderLivingEvent.Pre` 取消原版渲染后，原版 `CarriedBlockLayer` 不再执行，方块消失。

## 实现

`client/render/CarriedBlockRenderer`：

- **来源**：`RenderSystemPort.getCarriedBlockState(Entity)`；bridge 实现
  `entity instanceof EnderMan ? getCarriedBlock() : null`。不写死方块 id。
- **绘制**：`RenderSystemPort.renderBlockState(BlockState, PoseStack, RenderSink, int light)`：
  - `<26.1`：`Minecraft.getBlockRenderer().renderSingleBlock(...)`；
  - `26.1`：`BlockModelResolver.update` + `BlockModelRenderState.submit(poseStack, sink.submitNodeCollector(), light, NO_OVERLAY, 0)`。
- **锚点**：`lefthand`/`righthand` bind pivot 经 `ModelPoseTransforms.applyBone` 的当前动画矩阵
  变换后的世界中点；朝向/缩放取模型根骨骼当前姿态。资源包没有双手骨骼则不渲染。
- **尺寸/朝向**：0.5 缩放；沿用原版 `CarriedBlockLayer` 的 20°X/45°Y（基岩坐标下 X/Y 取反）。
- **调用点**：`EntityRenderOrchestrator.renderComponents` 内，`setupEntityClientEntityData`
  之后、模型提交之前，每实体每帧一次（`boolean[]` 守卫）。

## 26.1 关键坑

26.1 的 `SubmitNodeCollector` 在 deferred geometry writer（`renderAllFeatures` 阶段，即
`SimpleRenderAction.extraRender` 的回调）里再提交 `BlockModelRenderState` 不会出画。方块提交
必须发生在实体 submit 阶段；把调用从 `extraRender` 移到 `renderComponents` 后三节点均正常。
`HandItemRenderer` 仍保留在 `extraRender`（其 `renderItemDirect` 路径不受影响）。

## 验证

1.20.1 / 1.21.1 / 26.1.2 实机：dirt/TNT 均显示在双手之间；清除 carried block 后消失；原版
渲染不双画。dokimobs 侧证据见 `dokimobs/work/enderman-carried-block.md`。

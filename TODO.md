# TODO
## ./eyelib-animation/src/main/java/io/github/tt432/eyelibanimation/bedrock/controller/BrAnimationController.java
* [./eyelib-animation/src/main/java/io/github/tt432/eyelibanimation/bedrock/controller/BrAnimationController.java:70](./eyelib-animation/src/main/java/io/github/tt432/eyelibanimation/bedrock/controller/BrAnimationController.java#L70): 实现动画完成时的回调逻辑。

## ./eyelib-behavior/src/main/java/io/github/tt432/eyelibbehavior/event/logic/QueueCommand.java
* [./eyelib-behavior/src/main/java/io/github/tt432/eyelibbehavior/event/logic/QueueCommand.java:42](./eyelib-behavior/src/main/java/io/github/tt432/eyelibbehavior/event/logic/QueueCommand.java#L42): 对接实际命令执行系统

## ./eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/material/BrMaterialEntry.java
* [./eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/material/BrMaterialEntry.java:608](./eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/material/BrMaterialEntry.java#L608): 未来运行时变体选择应由Molang查询驱动（如query.has_variant），当前仅按名称匹配

## ./eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/material/BrSamplerState.java
* [./eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/material/BrSamplerState.java:51](./eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/material/BrSamplerState.java#L51): PCF 未实现

## ./eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangBytecodeEmitter.java
* [./eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangBytecodeEmitter.java:164](./eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangBytecodeEmitter.java#L164): 实现箭头访问语义的 HostContext 切换。

## ./src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java
* [./src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java:289](./src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java#L289): 权宜之计
* [./src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java:373](./src/main/java/io/github/tt432/eyelib/client/EntityRenderSystem.java#L373): 1.20.1 没有 Attributes.SCALE，这里自己算 AttributeInstance scaleAttr = livingEntity.getAttribute(Attributes.SCALE); if (scaleAttr != null) { double scaleValue = scaleAttr.getValue(); poseStack.scale((float) scaleValue, (float) scaleValue, (float) scaleValue); }

## ./src/main/java/io/github/tt432/eyelib/client/render/EyelibLivingEntityRenderer.java
* [./src/main/java/io/github/tt432/eyelib/client/render/EyelibLivingEntityRenderer.java:43](./src/main/java/io/github/tt432/eyelib/client/render/EyelibLivingEntityRenderer.java#L43): 修改成使用 layer

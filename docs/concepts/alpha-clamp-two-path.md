# Bedrock 低 alpha 纹理的 MC cutout 兼容方案：双路径纹理

## 根因

Bedrock addon 纹理使用 alpha=3 的 faint 像素做边缘抗锯齿，Bedrock 引擎的 cutout 阈值极低（alpha>0 即通过）。MC `entity_cutout_no_cull` shader 的 alpha threshold=0.5（128/255），导致这些像素全被 discard。

## 全局 clamp 的问题

最初在 `NativeImageIO.fromImportedImageData()` + `upload()` 做全局 alpha binary clamp（alpha>0 → 255）。解决了 alphatest 材质（羊）的渲染，但破坏了 blending 材质（史莱姆）的半透明效果。

## 双路径方案

不分全局 clamp。`RenderControllerEntry.resolveSlotTextures()` 按材质类型为每个纹理图层选择不同路径：

| 路径 | 材质 | alpha 行为 |
|------|------|-----------|
| 原始图层路径 | 普通 / blending | 保留原始 alpha |
| `<ns>:clamped/<原路径>` | alphatest / emissive（且非 colorMask） | clamp alpha → 0/255 binary |

### 实现

`RenderControllerEntry.resolveSlotTextures()`：
1. 按材质名把 `texture.material` 注入 MolangScope，逐图层解析 RC 的 `textures` 表达式（先底层后顶层）
2. `!usesColorMask(name) && (isAlphatestMaterial(name) || isEmissiveMaterial(name))` → 每个图层经 `clampedTexture()` 生成副本：GL download → `clampAlphaToBinary` → upload 为 `clamped/` 前缀路径
3. 其余材质直接使用原始图层路径

emissive 一并 clamp 的原因：BE 发光着色器不丢弃低 alpha（alpha 是发光掩码），而 MC entityTranslucent 着色器在 alpha<0.1 时 discard（A&S 蜘蛛红眼 alpha 仅 1-10 会整体消失）。

### 材质判定

`flagsOf(materialName)` → `MaterialFlags(multitexture, alphatest, emissive, colorMask)`，基于 MaterialManager 材质解析链上的 defines/states 推导（如 ALPHA_TEST / USE_EMISSIVE / USE_ONLY_EMISSIVE / USE_COLOR_MASK），结果按 matMap 引用缓存。

## 注意

- download 后的 NativeImage 生命周期：必须用 `NativeImagePort.copyImage()` 创建独立副本后再 clamp、upload（当前封装在 `clampedTexture()` 的 syncedAction 中）
- 本文早期版本提及的 `TextureLayerMerger.merge()` compute shader **从未在代码库中落地**（≤26.1 vanilla 仅 OpenGL 3.2 core，无 compute；详见 docs/research/2026-08-26-render-gpu-offload.md §3.1），该描述为历史残留，已删除。当前无纹理合并步骤——clamp 逐图层进行

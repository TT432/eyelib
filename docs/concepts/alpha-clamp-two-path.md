# Bedrock 低 alpha 纹理的 MC cutout 兼容方案：双路径纹理

## 根因

Bedrock addon 纹理使用 alpha=3 的 faint 像素做边缘抗锯齿，Bedrock 引擎的 cutout 阈值极低（alpha>0 即通过）。MC `entity_cutout_no_cull` shader 的 alpha threshold=0.5（128/255），导致这些像素全被 discard。

## 全局 clamp 的问题

最初在 `NativeImageIO.fromImportedImageData()` + `upload()` 做全局 alpha binary clamp（alpha>0 → 255）。解决了 alphatest 材质（羊）的渲染，但破坏了 blending 材质（史莱姆）的半透明效果。

## 双路径方案

不分全局 clamp。在 `resolveSlotTexture` 中根据材质类型使用不同纹理路径：

| 路径 | 材质 | alpha 行为 |
|------|------|-----------|
| `complex:textures/...` | blending | 保留原始 alpha |
| `complex:clamped/textures/...` | alphatest | clamp alpha → 0/255 binary |

### 实现

`RenderControllerEntry.resolveSlotTexture()`：
1. 正常 merge + upload → `complex:textures/...`（所有材质共用）
2. `isAlphatestMaterial(materialName)` → 从 GL download 已上传的复杂纹理 → clamp → upload 为 `complex:clamped/textures/...` → 返回 clamped path
3. 非 alphatest 材质返回原始 path

### isAlphatestMaterial 查找逻辑

材质名按三层查找 MaterialManager：
1. 全 key（`"entity_alphatest_change_color:entity_alphatest"`）
2. suffix（key 的 `:` 后部分）
3. name 字段（entry.name()）
→ `entry.hasBlending(matMap)` → !hasBlending 即 alphatest

## 注意

- `NativeImageIO.download()` 内部用 try-with-resources，返回后 NativeImage 被释放。必须用 `NativeImageIO.copyImage()` 创建独立副本后再 clamp
- `TextureLayerMerger.merge()` 的 compute shader 使用 premultiplied alpha (`src.rgb * src.a`)，低 alpha 输入会被乘到接近黑色。**clamp 必须在 merge 之前做**（即对输入纹理 clamp）。当前方案通过 download 已 merge 的纹理再做 clamp 避免了此问题

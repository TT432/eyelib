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

### clamped 上传时机（21.1.21 修复，实机验证）

旧实现仅在 `Slot.needsTextureReload()` 为 true 时入队 clamped 上传动作。实体首帧 variant 未同步时贴图解析键为默认值（如村民 `plains_unskilled`），上传后 slot 被 `markTextureUploaded()` 标记；次帧 variant 同步到位、键变化（`plains_farmer`），但 `needReload` 已为 false → 新键的 `clamped/` 派生纹理永不生成 → 渲染 MissingTexture 永久紫黑（村民职业服装、蜜蜂均因此中招）。

修复后 `clampedTexture()` 的上传动作**始终入队**，渲染线程内执行时再判定：

1. `!needReload && eyelib$hasTexture(clamped)` → 跳过（派生纹理已存在，零重复上传）；
2. 否则 download 基图 → clamp → upload；`hasTexture` 经 `EyelibTextureManagerAccess`/`TextureManagerMixin` 查 `byPath`；
3. 基图未就绪（`NativeImageIO` 对 MissingTexture 返回 null，避免把 16x16 紫黑棋盘固化为 clamped 内容）→ `Slot.markTextureStale()` 抵消 setupModel 先行的 `markTextureUploaded()`，下一帧重新入队重试，直到基图就绪成功上传。

存在性检查必须放在渲染线程：`setupModel` 可能在并行阶段线程执行，键的解析与 `byPath` 状态之间存在时序窗口。

emissive 一并 clamp 的原因：BE 发光着色器不丢弃低 alpha（alpha 是发光掩码），而 MC entityTranslucent 着色器在 alpha<0.1 时 discard（A&S 蜘蛛红眼 alpha 仅 1-10 会整体消失）。

### 材质判定

`flagsOf(materialName)` → `MaterialFlags(multitexture, alphatest, emissive, colorMask)`，基于 MaterialManager 材质解析链上的 defines/states 推导（如 ALPHA_TEST / USE_EMISSIVE / USE_ONLY_EMISSIVE / USE_COLOR_MASK），结果按 matMap 引用缓存。

## 注意

- download 后的 NativeImage 生命周期：必须用 `NativeImagePort.copyImage()` 创建独立副本后再 clamp、upload（当前封装在 `clampedTexture()` 的 syncedAction 中）
- 本文早期版本提及的 `TextureLayerMerger.merge()` compute shader **从未在代码库中落地**（≤26.1 vanilla 仅 OpenGL 3.2 core，无 compute；详见 docs/research/2026-08-26-render-gpu-offload.md §3.1），该描述为历史残留，已删除。当前无纹理合并步骤——clamp 逐图层进行

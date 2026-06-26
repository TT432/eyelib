# RenderType System — 跨版本对比 (1.20.1 / 1.21.1 / 26.1.2)

> 基于三个版本源码的完整对比分析。

## 目录

1. [类/包位置变化](#1-类包位置变化)
2. [状态维度映射](#2-状态维度映射)
3. [可运行时改变 vs 固化到 pipeline](#3-可运行时改变-vs-固化到-pipeline)
4. [工厂方法对应表](#4-工厂方法对应表)
5. [完整性差异](#5-完整性差异)
6. [核心架构范式对比](#6-核心架构范式对比)

---

## 1. 类/包位置变化

| 关注点 | 1.20.1 / 1.21.1 | 26.1.2 |
|---|---|---|
| RenderType 所在包 | `net.minecraft.client.renderer` | `net.minecraft.client.renderer.rendertype` |
| RenderType 基类 | `extends RenderStateShard` | **无基类** (独立类，非 abstract) |
| 状态封装 | `CompositeState` (13 个 RenderStateShard) | `RenderSetup` (组合 RenderPipeline) |
| Pipeline 常量 | 分散在 `RenderType` 内部 | 集中在 `RenderPipelines` 类 (788行) |
| 工厂方法 | `RenderType` 类上 static methods | `RenderTypes` 类上 static methods |
| RenderStateShard | `net.minecraft.client.renderer` (存在) | **不存在** |
| CompositeRenderType | `RenderType` 内部 static class | **不存在** |
| CompositeState/Builder | `RenderType` 内部 static class | **不存在** |
| BlendFunction | GL 常量分散在 TransparencyStateShard | `com.mojang.blaze3d.pipeline.BlendFunction` (独立 record) |
| DepthTest | GL 常量在 DepthTestStateShard | `com.mojang.blaze3d.pipeline.DepthStencilState` (独立 record) |
| 纹理矩阵 | `TexturingStateShard` (GL 端) | `TextureTransform` (纯数学, 供 UBO) |
| Layering | `LayeringStateShard` (GL polygon offset) | `LayeringTransform` (modelView push/pop) |
| OutputTarget | `OutputStateShard` (GL bind) | `OutputTarget` (返回 RenderTarget 对象) |
| Shader 绑定 | `ShaderStateShard` (RenderSystem.setShader) | 固化到 `RenderPipeline.vertexShader` / `fragmentShader` |
| Pipeline/Shader 编译 | 无概念 (GL 端即时设置) | `GlDevice.getOrCompilePipeline` + pipelineCache |
| Forge/NeoForge | `ForgeRenderTypes` 代理(1.20.1) | NeoForge `RegisterRenderPipelinesEvent` |

### 1.1 继承层次对比

```
1.20.1/1.21.1:                    26.1.2:
RenderStateShard                  RenderType (独立类, final 语义)
  └─ RenderType (abstract)          ├─ name: String
       ├─ format/mode/buffer        └─ state: RenderSetup (组合)
       └─ CompositeRenderType            ├─ pipeline: RenderPipeline (全 GPU 状态)
            └─ state: CompositeState     ├─ textures + lightmap + overlay
                 └─ 13 × Shard           ├─ outputTarget
                                         ├─ textureTransform
                                         └─ layeringTransform
```

---

## 2. 状态维度映射 (1.20.1 CompositeState → 26.1.2)

### 2.1 13 维度归属表

| 1.20.1 CompositeState Shard | 1.21.1 | 26.1.2 归属 | 26.1.2 类/字段 |
|---|---|---|---|
| **textureState** (TextureStateShard) | 同 | RenderSetup | `textures: Map<String, TextureBinding>` |
| **shaderState** (ShaderStateShard) | 同 | RenderPipeline | `vertexShader` + `fragmentShader` |
| **transparencyState** (TransparencyStateShard) | 同 | RenderPipeline | `colorTargetState.blendFunction()` |
| **depthTestState** (DepthTestStateShard) | 同 | RenderPipeline | `depthStencilState.depthTest()` |
| **cullState** (CullStateShard) | 同 | RenderPipeline | `cull: boolean` |
| **lightmapState** (LightmapStateShard) | 同 | RenderSetup | `useLightmap: boolean` |
| **overlayState** (OverlayStateShard) | 同 | RenderSetup | `useOverlay: boolean` |
| **layeringState** (LayeringStateShard) | 同 | RenderSetup | `layeringTransform: LayeringTransform` |
| **outputState** (OutputStateShard) | 同 | RenderSetup | `outputTarget: OutputTarget` |
| **texturingState** (TexturingStateShard) | 同 | RenderSetup | `textureTransform: TextureTransform` |
| **writeMaskState** (WriteMaskStateShard) | 同 | RenderPipeline | `colorTargetState.writeMask()` |
| **lineState** (LineStateShard) | 同 | **不存在独立字段** | 线宽通过 UBO 的 `Vector3f` 传递 |
| **colorLogicState** (ColorLogicStateShard) | 同 | **不存在** | 未确认 — ColorTargetState 无逻辑操作字段 |

### 2.2 映射分类

| 类别 | 维度 | 说明 |
|---|---|---|
| **固化到 RenderPipeline** | shader, blend, depthTest, cull, writeMask | GPU pipeline 对象不可变状态 |
| **固化到 RenderSetup** | textures, lightmap, overlay, layering, texturing, outputTarget | CPU 端在 RenderType 构建时确定 |
| **运行时动态值** | line width | 通过 DynamicTransforms UBO 每帧传入 |
| **消失** | colorLogic | 26.1.2 未找到等价机制 |

---

## 3. 可运行时改变 vs 固化到 Pipeline 的维度

### 3.1 1.20.1/1.21.1 模型

**所有维度都可在 RenderType 实例之间不同**，但在单个实例构造后不可变。运行时切换 RenderType 会触发:
1. `setupRenderState()` — 遍历所有 shard setup Runnable
2. 提交绘制
3. `clearRenderState()` — 遍历所有 shard clear Runnable

每次 draw call 前后逐项设置/清除 GL 状态，开销较高但灵活。

### 3.2 26.1.2 模型

**多数维度固化到 RenderPipeline** 对象中：
- Pipeline 在 `build()` 时完成验证
- GPU 端通过 `GlDevice.getOrCompilePipeline()` 编译一次，缓存在 pipelineCache
- 运行时 `renderPass.setPipeline()` 一次性设置全部 GPU 状态
- 切换不同 pipeline 相当于切换预编译的 GPU 状态集

**可变的维度在 RenderSetup**:
- 纹理绑定 (`useLightmap` / `useOverlay` / `textures`) — 每次 `draw()` 时解析
- 纹理矩阵 (`textureTransform`) — 通过 UBO 动态写入
- Layering — 通过 modelViewStack push/pop

### 3.3 关键设计差异

| 特性 | 1.20.1/1.21.1 | 26.1.2 |
|---|---|---|
| Shader 切换方式 | GL 端 `glUseProgram` | Pipeline 对象切换 (setPipeline) |
| Blend 切换方式 | GL 端 `glEnable(GL_BLEND)` + `glBlendFunc` | Pipeline 内置 ColorTargetState |
| Depth 切换方式 | GL 端 `glDepthFunc` | Pipeline 内置 DepthStencilState |
| Cull 切换方式 | GL 端 `glEnable/Disable(GL_CULL_FACE)` | Pipeline 内置 cull: boolean |
| 状态恢复机制 | clearState Runnable | 无需 — setPipeline 整体替换状态 |
| 纹理绑定 | GL 端逐个 `setShaderTexture` | RenderPass.bindTexture + UBO |
| 每帧动态数据 | GL uniform 调用 | UBO (DynamicTransforms) |

---

## 4. 工厂方法对应表

### 4.1 无参数工厂

| 1.20.1 | 1.21.1 | 26.1.2 | 备注 |
|---|---|---|---|
| `solid()` | `solid()` | `solidMovingBlock()` | 26.1.2 名称为 moving block，含义变窄 |
| `cutoutMipped()` | `cutoutMipped()` | — | 未找到直接对应 |
| `cutout()` | `cutout()` | `cutoutMovingBlock()` | 同上 |
| `translucent()` | `translucent()` | `translucentMovingBlock()` | 同上 |
| `translucentMovingBlock()` | `translucentMovingBlock()` | `translucentMovingBlock()` | — |
| `translucentNoCrumbling()` | **移除** | — | — |
| `leash()` | `leash()` | `leash()` | — |
| `waterMask()` | `waterMask()` | `waterMask()` | — |
| `armorGlint()` | **移除** | `armorEntityGlint()` | 名称变更 |
| `armorEntityGlint()` | `armorEntityGlint()` | `armorEntityGlint()` | — |
| `glintTranslucent()` | `glintTranslucent()` | `glintTranslucent()` | — |
| `glint()` | `glint()` | `glint()` | — |
| `glintDirect()` | **移除** | — | — |
| `entityGlint()` | `entityGlint()` | `entityGlint()` | — |
| `entityGlintDirect()` | **移除** | — | — |
| `lightning()` | `lightning()` | `lightning()` | — |
| `tripwire()` | `tripwire()` | — | 未确认 |
| `endPortal()` | `endPortal()` | `endPortal()` | — |
| `endGateway()` | `endGateway()` | `endGateway()` | — |
| `lines()` | `lines()` | `lines()` | — |
| `lineStrip()` | `lineStrip()` | — | 26.1.2 未找到 |
| `debugFilledBox()` | `debugFilledBox()` | `debugFilledBox()` | — |
| `debugQuads()` | `debugQuads()` | `debugQuads()` | — |
| `debugSectionQuads()` | **移除** | — | — |
| `gui()` | `gui()` | — | 26.1.2 GUI 使用 pipeline 直接渲染，不经过 RenderTypes |
| `guiOverlay()` | `guiOverlay()` | — | — |
| `guiTextHighlight()` | `guiTextHighlight()` | — | — |
| `guiGhostRecipeOverlay()` | `guiGhostRecipeOverlay()` | — | — |
| `textBackground()` | `textBackground()` | `textBackground()` | — |
| `textBackgroundSeeThrough()` | `textBackgroundSeeThrough()` | `textBackgroundSeeThrough()` | — |
| — | `clouds()` | — | 26.1.2 使用 `CLOUDS`/`FLAT_CLOUDS` pipeline 直接 |
| — | `dragonRays()` | `dragonRays()` | — |
| — | `dragonRaysDepth()` | `dragonRaysDepth()` | — |

### 4.2 有参数工厂

| 1.20.1 | 1.21.1 | 26.1.2 | 参数变化 |
|---|---|---|---|
| `armorCutoutNoCull(loc)` | `armorCutoutNoCull(loc)` | `armorCutoutNoCull(id)` | ResourceLocation → Identifier |
| `entitySolid(loc)` | `entitySolid(loc)` | `entitySolid(id)` | 同 |
| — | `entitySolidZOffsetForward(id)` | `entitySolidZOffsetForward(id)` | 新增于 1.21.1 |
| `entityCutout(loc)` | `entityCutout(loc)` | `entityCutout(id)` (boolean 版) | 1.21.1 去掉 Boolean 默认重载 |
| — | `entityCutoutCull(loc)` | `entityCutoutCull(id)` | 新增 |
| `entityCutoutNoCull(loc, outline)` | `entityCutoutNoCull(loc, outline)` | — | 26.1.2 合并到 entityCutout(boolean) |
| `entityCutoutNoCullZOffset(loc, outline)` | `entityCutoutNoCullZOffset(loc, outline)` | `entityCutoutZOffset(id, affectsOutline)` | 名称简化 |
| `itemEntityTranslucentCull(loc)` | `itemEntityTranslucentCull(loc)` | `entityTranslucentCullItemTarget(id)` | 名称变更 |
| `entityTranslucentCull(loc)` | `entityTranslucentCull(loc)` | — | 未确认 |
| `entityTranslucent(loc, outline)` | `entityTranslucent(loc, outline)` | `entityTranslucent(id, affectsOutline)` | 同 |
| `entityTranslucentEmissive(loc, outline)` | `entityTranslucentEmissive(loc, outline)` | `entityTranslucentEmissive(id, affectsOutline)` | 同 |
| `entitySmoothCutout(loc)` | `entitySmoothCutout(loc)` | — | 未确认 |
| `entityDecal(loc)` | `entityDecal(loc)` | `createArmorDecalCutoutNoCull(id)` | 名称+用途变更 |
| `entityNoOutline(loc)` | `entityNoOutline(loc)` | — | 未确认 |
| `entityShadow(loc)` | `entityShadow(loc)` | `entityShadow(id)` | 同 |
| `dragonExplosionAlpha(loc)` | `dragonExplosionAlpha(loc)` | — | 未确认, 可能用 ENERGY_SWIRL 替代 |
| `eyes(loc)` | `eyes(loc)` | `eyes(id)` | 同 |
| `energySwirl(loc, u, v)` | `energySwirl(loc, u, v)` | `energySwirl(id, u, v)` | 同 |
| `beaconBeam(loc, colorFlag)` | `beaconBeam(loc, colorFlag)` | `beaconBeam(id, translucent)` | boolean 参数语义变化 |
| `crumbling(loc)` | `crumbling(loc)` | `crumbling(id)` | 同 |
| `text(loc)` | `text(loc)` | `text(id)` | 不再经 Forge 代理 |
| `textIntensity(loc)` | `textIntensity(loc)` | `textIntensity(id)` | 同 |
| `textPolygonOffset(loc)` | `textPolygonOffset(loc)` | `textPolygonOffset(id)` | 同 |
| `textIntensityPolygonOffset(loc)` | `textIntensityPolygonOffset(loc)` | `textIntensityPolygonOffset(id)` | 同 |
| `textSeeThrough(loc)` | `textSeeThrough(loc)` | `textSeeThrough(id)` | 同 |
| `textIntensitySeeThrough(loc)` | `textIntensitySeeThrough(loc)` | `textIntensitySeeThrough(id)` | 同 |
| — | `breezeWind(loc, u, v)` | `breezeWind(id, u, v)` | 新增 |
| — | — | `entityCutoutDissolve(id, maskId)` | 26.1.2 新增 |
| — | — | `armorTranslucent(id)` | 26.1.2 新增 |
| — | — | `endCrystalBeam(id)` | 26.1.2 新增 |
| — | — | `bannerPattern(id)` | 26.1.2 新增 |
| — | — | `itemCutout(id)` | 26.1.2 新增 |
| — | — | `itemTranslucent(id)` | 26.1.2 新增 |
| — | — | `breezeEyes(id)` | 26.1.2 新增 |
| — | — | `blockScreenEffect(id)` | 26.1.2 新增 |
| — | — | `fireScreenEffect(id)` | 26.1.2 新增 |
| `debugLineStrip(width)` | `debugLineStrip(width)` | — | 未确认 |

---

## 5. 完整性差异

### 5.1 Stencil 支持

| 版本 | Stencil 机制 |
|---|---|
| 1.20.1 | 不存在。所有 13 个 CompositeState shard 无 stencil 维度 |
| 1.21.1 | 不存在 |
| 26.1.2 | 通过 **NeoForge StencilTest** 扩展支持: `RenderPipeline.stencilTest: Optional<StencilTest>` + `Builder.withStencilTest()` / `withoutStencilTest()` |

### 5.2 新增能力 (26.1.2 独有)

| 特性 | 说明 |
|---|---|
| Pipeline 编译器缓存 | `GlDevice.getOrCompilePipeline` + `pipelineCache` |
| Snippet 组合系统 | `RenderPipeline.Snippet` 可组合构建块，支持 Builder 链式合并 |
| ShaderDefines | Pipeline 内建 shader macro 定义 (如 `ALPHA_CUTOUT`, `EMISSIVE`, `PER_FACE_LIGHTING` 等) |
| UBO 系统 | `DynamicTransforms`, `Projection`, `Fog`, `Globals`, `Lighting`, `ChunkSection` 等 UBO |
| 独立 Uniform 描述 | `UniformDescription` record 描述每个 uniform 的类型和格式 |
| `VertexFormat.uploadImmediateVertexBuffer` | 即时上传顶点数据 |
| `RenderPass` API | `createCommandEncoder().createRenderPass()` → `setPipeline` → `bindTexture` → `drawIndexed` |
| 多纹理绑定 | `Map<String, TextureAndSampler>` 支持任意数量的采样器绑定 |
| `textureTransform` 独立 | 从 GL 端纹理矩阵操作变为纯数学 `Supplier<Matrix4f>` |
| OutlineProperty 独立枚举 | `RenderSetup.OutlineProperty` (NONE/IS_OUTLINE/AFFECTS_OUTLINE) |
| DISSOLVE shader | `entityCutoutDissolve()` 支持带遮罩的溶解动画 |
| 新的特殊效果 Pipelines | VIGNETTE, CROSSHAIR, MOJANG_LOGO, ENTITY_OUTLINE_BLIT, TRACY_BLIT 等 |

### 5.3 移除的能力 (26.1.2 中消失)

| 特性 | 说明 |
|---|---|
| `setupRenderState`/`clearRenderState` 机制 | 不再有逐个 shard 的 GL 状态设置/恢复 |
| `CompositeState` / `CompositeStateBuilder` | 13 维度状态描述模式完全移除 |
| `RenderStateShard` 类层次 | 整个类消失，包括 BooleanStateShard 等 |
| `BufferBuilder` + `BufferUploader` 组合 | 替换为 `MeshData` |
| `ForgeRenderTypes` 代理 | 替换为直接 `RenderType.create()` |
| `colorLogic` (GL 逻辑操作) | 未找到等价机制 |
| `lineStrip` mode | 未确认等价 |
| `tripwire` | 未确认等价 |
| chunkLayerId (Forge 扩展) | 未确认等价 |
| `CANVAS` texture type | 未确认 |
| `MEDIUM_BUFFER_SIZE` (256KB) | 已移除 |
| `debugSectionQuads` | 未确认 |
| `translucentNoCrumbling` | 未确认 |

### 5.4 缓冲区大小演变

| 常量 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| `BIG_BUFFER_SIZE` | 2,097,152 (2MB) | 4,194,304 (4MB) | 4,194,304 (4MB) |
| `MEDIUM_BUFFER_SIZE` | 262,144 (256KB) | 不存在 | 不存在 |
| `SMALL_BUFFER_SIZE` | 131,072 (128KB) | 786,432 (768KB) | 786,432 (768KB) |
| `TRANSIENT_BUFFER_SIZE` | 256 | 1,536 | 1,536 |

### 5.5 RenderPipeline 数量对比

| 版本 | Pipeline/Shader 常量数 |
|---|---|
| 1.20.1 | ~55 个 ShaderStateShard 常量 + 内联 Lambda |
| 1.21.1 | ~48 个 ShaderStateShard 常量 |
| 26.1.2 | **~90 个 RenderPipeline 常量** (14 个 Snippet + 76 个 Pipeline) |

26.1.2 的 pipeline 粒度更细：每个渲染场景有独立 pipeline (如 `GUI_INVERT`, `VIGNETTE`, `CROSSHAIR` 等)。

---

## 6. 核心架构范式对比

| 维度 | 1.20.1 / 1.21.1 | 26.1.2 |
|---|---|---|
| **状态管理范式** | Runnable 链式设置/恢复 GL 状态 | Pipeline 对象预编译 GPU 状态集 |
| **状态描述粒度** | 13 个独立 Shard 维度 | Pipeline(11 字段) + Setup(10 字段) 两层 |
| **组合方式** | CompositeStateBuilder 链式设置 | Snippet 合并 + Builder 链式设置 |
| **RenderType 构建** | `create(name, format, mode, bufSize, ..., compositeState)` 7参 | `create(name, renderSetup)` 2参 |
| **绘制接口** | `end(BufferBuilder, VertexSorting)` | `draw(MeshData)` |
| **Shader 管理** | GameRenderer 持有 ShaderInstance Supplier | RenderPipeline 持有 shader Identifier |
| **缓存策略** | `Util.memoize` (RenderType 实例缓存) | `Util.memoize` (RenderType 实例) + `pipelineCache` (GPU pipeline 编译缓存) |
| **扩展机制** | `ForgeRenderTypes` 静态代理 (1.20.1), 直接 create (1.21.1) | `RegisterRenderPipelinesEvent` (NeoForge) |
| **纹理采样器** | 约定纹理单元 0, 1, 2 (lightmap/overlay) | 名称绑定 (Sampler0/Sampler1/Sampler2) + 任意 key |
| **多纹理** | MultiTextureStateShard (仅末地传送门使用) | 通用 `Map<String, TextureBinding>` |
| **Uniform 管理** | GL uniform 调用 | UBO + UniformDescription 描述系统 |

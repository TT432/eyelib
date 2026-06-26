# Cross-Version Shader System Differences (1.20.1 / 1.21.1 / 26.1.2)

> 三个版本着色器系统的横向对比。详细单版分析见 `shaders-1.20.1.md` / `shaders-1.21.1.md` / `shaders-26.1.2.md`。

## 目录

1. [类/包变化表](#1-类包变化表)
2. [Shader 加载机制演进](#2-shader-加载机制演进)
3. [Uniform 系统演进](#3-uniform-系统演进)
4. [Shader 定义演进(无 define → ShaderDefines)](#4-shader-定义演进无-define--shaderdefines)
5. [Shader 注册表对应表](#5-shader-注册表对应表)
6. [Sampler 绑定差异](#6-sampler-绑定差异)
7. [后处理 Shader 差异](#7-后处理-shader-差异)
8. [GL 状态集成度对比](#8-gl-状态集成度对比)
9. [对 eyelib 的影响清单](#9-对-eyelib-的影响清单)

---

## 1. 类/包变化表

| 职责 | 1.20.1 (Forge) | 1.21.1 (NeoForge) | 26.1.2 (NeoForge) |
|---|---|---|---|
| Shader 实例 | `ShaderInstance` | 同左 | **已删除**,拆分为 `RenderPipeline`(声明) + `CompiledRenderPipeline`(GPU 编译产物) |
| Shader 注册表 | `GameRenderer`(字段+get*Shader) | 同左 | `RenderPipelines`(静态常量) |
| Uniform 表示 | `Uniform`(int/float/mat 类型) | 同左 | `UniformDescription`(record,name+UniformType) |
| Uniform 上传 | `Uniform.upload()`(glUniform*) | 同左 | `DynamicUniformStorage`(UBO ring buffer,std140) |
| Uniform 类型 | `Uniform.UT_INT1`~`UT_MAT4`(10种) | 同左 | `UniformType`(枚举:UNIFORM_BUFFER/TEXEL_BUFFER) |
| Shader 变异 | 不同 JSON 文件(如 `rendertype_solid.json`) | 同左 | **同一 GLSL 文件 + ShaderDefines**(`#define` 条件编译) |
| 可复用配置 | 无(每 shader 独立 JSON) | 同左 | `RenderPipeline.Snippet`(组合模式) |
| GL Program 管理 | `ProgramManager`(createProgram/link/release) | 同左 | `GpuDevice`(getOrCompilePipeline/clearPipelineCache) |
| 资源重载 | `GameRenderer.reloadShaders()`(逐个 close→新建) | 同左 | `ShaderManager.apply()`(prepare→precompile) |
| Post-processing | `PostChain`(直接解析 JSON) | 同左 | `PostChain`(JSON→Codec:PostChainConfig→load()) |
| Post-effect shader | `EffectInstance` | 同左 | `PostChainConfig.Pass`(数据 record,无独立 EffectInstance) |
| Blend 模式 | `ShaderInstance.blend`(BlendMode) | 同左 | `RenderPipeline.colorTargetState`(ColorTargetState 含 BlendFunction) |
| 深度/模板 | 分散在 `RenderStateShard` | 同左 | `RenderPipeline.depthStencilState`(集成) |
| GLSL 预处理 | `GlslPreprocessor`(函数式) | 同左 | `GlslPreprocessor`(SAM 形式,`ShaderManager` 创建) |

---

## 2. Shader 加载机制演进

| 阶段 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| **配置语言** | JSON(`shaders/core/<name>.json`) | 同左 | **Java Builder API**(无 JSON 配置) |
| **Shader 源** | `.vsh`/`.fsh` 文件,`#import` 展开 | 同左 | 同左(源路径由 Identifier 映射) |
| **编译时机** | ShaderInstance 构造时立即编译 | 同左 | `ShaderManager.apply()` 预编译(或延迟至首次使用) |
| **缓存策略** | `Program.Type` 静态 Map(按名称) | 同左 | `ShaderManager.CompilationCache`(按 source key) + `GpuDevice` 内部缓存 |
| **变异实现** | 不同 JSON 文件(如 `rendertype_solid.json` vs `rendertype_entity_cutout.json`) | 同左 | **同一 GLSL 文件 + ShaderDefines**(如 `"ALPHA_CUTOUT"` 决定是否执行 `if (color.a < ALPHA_CUTOUT) discard`) |
| **可组合性** | 无(每 shader 独立完整) | 同左 | **Snippet 组合**(通过 Builder API 叠加) |
| **失败处理** | 抛 IOException,GameRenderer catch 后降级 | 同左 | 清空整个 pipeline 缓存 + RuntimeException |

### 配置对比

**1.20.1 (JSON)**:
```json
{
  "vertex": "rendertype_entity_cutout",
  "fragment": "rendertype_entity_cutout",
  "samplers": [{"name": "Sampler0"}, {"name": "Sampler1"}],
  "uniforms": [
    {"name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [...]}
  ]
}
```

**26.1.2 (Java Builder)**:
```java
RenderPipeline ENTITY_CUTOUT = register(
    RenderPipeline.builder(ENTITY_SNIPPET)
        .withLocation("pipeline/entity_cutout")
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withShaderDefine("PER_FACE_LIGHTING")
        .withSampler("Sampler1")
        .withCull(false)
        .build()
);
```

---

## 3. Uniform 系统演进

### 3.1 类型系统对比

| 维度 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| Uniform 表示 | `Uniform`(细粒度类型 0-10:int/float/mat) | 同左 | `UniformDescription`(name+UniformType) |
| 类型数量 | 11 种(int1-4,float1-4,mat2-4) | 同左 | 2 种(UNIFORM_BUFFER, TEXEL_BUFFER) |
| 上传机制 | `Uniform.upload()` → `glUniform*`(逐个) | 同左 | `DynamicUniformStorage` → UBO `glBufferSubData`(批量) |
| 内存布局 | `IntBuffer`/`FloatBuffer`(native memory) | 同左 | `ByteBuffer` + std140 对齐(Std140Builder) |
| 内存管理 | 每 Uniform 独立内存,`close()` 释放 | 同左 | Ring buffer 统一管理,`endFrame()` 回收 |
| Shader 声明 | `uniform mat4 ModelViewMat;`(直接声明) | 同左 | `layout(std140) uniform DynamicTransforms { mat4 ModelViewMat; vec4 ColorModulator; ... }`(UBO 块) |

### 3.2 预定义 Uniform 到 UBO 的对应

| 1.20.1 Uniform(GLSL name) | 26.1.2 UBO 块 | UBO 字段 | 类型 |
|---|---|---|---|
| `ModelViewMat`(vec4) | `DynamicTransforms`(`Transform`) | `modelView` | mat4(64 bytes) |
| `ColorModulator`(vec4) | `DynamicTransforms` | `colorModulator` | vec4(16 bytes) |
| `IViewRotMat`/`ChunkOffset`(vec3) | `DynamicTransforms` | `modelOffset` | vec3(16 bytes,std140 padded) |
| `TextureMat`(mat4) | `DynamicTransforms` | `textureMatrix` | mat4(64 bytes) |
| `ProjMat`(mat4) | `Projection` | (投影矩阵) | mat4 |
| `GameTime`(float) | `Globals` | (全局状态) | float |
| `ScreenSize`(vec2) | `Globals` | (屏幕尺寸) | vec2 |
| `FogStart`/`FogEnd`/`FogColor`/`FogShape` | `Fog` | (雾参数) | UBO 块 |
| `Light0_Direction`/`Light1_Direction` | `Lighting` | (光照方向) | UBO 块 |
| `ChunkOffset`(vec3) | `ChunkSection`(`ChunkSectionInfo`) | `x,y,z`(含 visibility 等) | ivec3 + mat4 + float + ivec2 |
| `FogStart`/`FogEnd`/`FogColor`/`FogShape` | `Fog` | (UBO 块) | 未确认 |

### 3.3 DynamicUniforms 的双缓冲机制

26.1.2 的 `DynamicUniformStorage` 实现 **Ring Buffer** 策略:
- `writeUniform()` 写入当前帧 slot,返回 `GpuBufferSlice`
- `endFrame()` 释放所有当前帧分配的 slot
- 自动扩展容量(以 `uniformOffsetAlignment` 对齐)

这替代了 1.20.1 中每个绘制调用前逐个 `glUniform*` 调用的模式,大幅减少 GL API 调用次数。

---

## 4. Shader 定义演进(无 define → ShaderDefines)

### 4.1 变体实现方式对比

| 维度 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 变体策略 | 不同 JSON 文件,不同 GLSL 文件(input 路径) | 同左 | 同一 GLSL 文件 + `#define` 条件编译 |
| 例:实体 cutout | `rendertype_entity_cutout.json` + `rendertype_entity_cutout.vsh` | 同左 | `core/entity.vsh` + `ALPHA_CUTOUT=0.1` + `PER_FACE_LIGHTING` |
| 例:实体 solid | `rendertype_entity_solid.json` + `rendertype_entity_solid.vsh` | 同左 | `core/entity.vsh`(无 ALPHA_CUTOUT define) |
| 例:自发光 | `rendertype_entity_translucent_emissive.json`(专用 shader 文件) | 同左 | `core/entity.vsh` + `EMISSIVE` define |
| define 如何注入 | (不适用) | (不适用) | `ShaderDefines.asSourceDirectives()` → `#define KEY VALUE\n` 预处理追加 |

### 4.2 ShaderDefines 使用模式

```java
// 基础 ENTITY_SNIPPET — 无 define
RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
    .withVertexShader("core/entity")
    .withFragmentShader("core/entity")
    .buildSnippet();

// ENTITY_EMISSIVE_SNIPPET — 加 EMISSIVE define
RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
    .withVertexShader("core/entity")
    .withFragmentShader("core/entity")
    .withShaderDefine("EMISSIVE")     // #define EMISSIVE
    .buildSnippet();

// ENTITY_CUTOUT — 加 ALPHA_CUTOUT + PER_FACE_LIGHTING
RenderPipeline.builder(ENTITY_SNIPPET)
    .withShaderDefine("ALPHA_CUTOUT", 0.1F)  // #define ALPHA_CUTOUT 0.1
    .withShaderDefine("PER_FACE_LIGHTING")    // #define PER_FACE_LIGHTING
    .build();
```

在 GLSL 源码中:
```glsl
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) discard;
#endif

#ifdef EMISSIVE
    // 使用 baseColor.a 和自发光逻辑,跳过光照计算
#else
    // 正常 diffuse + ambient + lightmap 光照
#endif
```

### 4.3 Define 清单汇总

| Define | 类型 | 出现位置 | 语义 |
|---|---|---|---|
| `ALPHA_CUTOUT` | float value | 多个 cutout pipeline | alpha 裁剪阈值 |
| `EMISSIVE` | flag | ENTITY_EMISSIVE, EYES, ENERGY_SWIRL | 自发光(无光照) |
| `APPLY_TEXTURE_MATRIX` | flag | BREEZE_WIND, ENERGY_SWIRL | 纹理 UV 变换 |
| `NO_OVERLAY` | flag | ARMOR_*, EYES, ENERGY_SWIRL | 跳过叠加纹理 |
| `NO_CARDINAL_LIGHTING` | flag | BREEZE_WIND, ENERGY_SWIRL, EYES | 跳过多方向光照 |
| `PER_FACE_LIGHTING` | flag | ARMOR_*, ENTITY_CUTOUT | 逐面光照(非顶点插值) |
| `PORTAL_LAYERS` | int value | END_PORTAL, END_GATEWAY | 传送门特效层数 |
| `DISSOLVE` | flag | ENTITY_CUTOUT_DISSOLVE | 溶解效果 |

---

## 5. Shader 注册表对应表

### 5.1 核心概念映射

| 1.20.1 概念 | 26.1.2 概念 | 说明 |
|---|---|---|
| `GameRenderer.get*Shader()`(方法) | `RenderPipelines.*`(静态常量) | 都是 shader 注册入口 |
| `RenderStateShard.ShaderStateShard` | `RenderSetup.builder(pipeline)` | 都是将 shader 注册到 RenderType 中 |
| `ShaderInstance.apply()`/`clear()` | `RenderSystem.setPipeline()` | 都是绑定/切换 shader |
| JSON `samplers` 数组 | `builder.withSampler(name)` | 都是声明 sampler |
| JSON `uniforms` 数组 | `builder.withUniform(name, type)` | 都是声明 uniform |
| JSON `blend` 节点 | `builder.withColorTargetState(ColorTargetState(blendFn))` | 都是设置混合模式 |
| JSON `attributes` 数组 | `builder.withVertexFormat(fmt, mode)` | 都是声明 vertex layout |

### 5.2 关键 Shader 对应

| 1.20.1 get*Shader() | 26.1.2 RenderPipelines 常量 | 备注 |
|---|---|---|
| `getRendertypeSolidShader()` | `SOLID_BLOCK` / `SOLID_TERRAIN` | 拆分为 block 和 terrain 变体 |
| `getRendertypeCutoutShader()` | `CUTOUT_BLOCK` / `CUTOUT_TERRAIN` | 同上 |
| `getRendertypeCutoutMippedShader()` | `CUTOUT_BLOCK`(统一 cutout) | mipped 变体已合并 |
| `getRendertypeTranslucentShader()` | `TRANSLUCENT_BLOCK` / `TRANSLUCENT_TERRAIN` | 同上 |
| `getRendertypeEntitySolidShader()` | `ENTITY_SOLID` | - |
| `getRendertypeEntityCutoutShader()` | `ENTITY_CUTOUT` | - |
| `getRendertypeEntityCutoutNoCullShader()` | `ENTITY_CUTOUT`(cull=false) | cull 标志内联到 pipeline |
| `getRendertypeEntityTranslucentShader()` | `ENTITY_TRANSLUCENT` | - |
| `getRendertypeEntityTranslucentCullShader()` | `ENTITY_TRANSLUCENT_CULL` | - |
| `getRendertypeEntityTranslucentEmissiveShader()` | `ENTITY_TRANSLUCENT_EMISSIVE` | 语义相同(EMISSIVE define) |
| `getRendertypeEntitySmoothCutoutShader()` | `ENTITY_CUTOUT` | smooth cutout 合并入 cutout |
| `getRendertypeEntityShadowShader()` | `ENTITY_SHADOW` | - |
| `getRendertypeEntityDecalShader()` | `ARMOR_DECAL_CUTOUT_NO_CULL` | 贴花移入 armor 系列 |
| `getRendertypeArmorCutoutNoCullShader()` | `ARMOR_CUTOUT_NO_CULL` | - |
| `getRendertypeBeaconBeamShader()` | `BEACON_BEAM_OPAQUE` / `BEACON_BEAM_TRANSLUCENT` | 按透明与否拆分 |
| `getRendertypeEyesShader()` | `EYES` | - |
| `getRendertypeEnergySwirlShader()` | `ENERGY_SWIRL` | - |
| `getRendertypeOutlineShader()` | `OUTLINE_CULL` / `OUTLINE_NO_CULL` | - |
| `getRendertypeCrumblingShader()` | `CRUMBLING` | - |
| `getRendertypeEndPortalShader()` | `END_PORTAL` | - |
| `getRendertypeEndGatewayShader()` | `END_GATEWAY` | - |
| `getRendertypeLinesShader()` | `LINES` / `LINES_DEPTH_BIAS` | - |
| `getRendertypeTextShader()` | `TEXT` / `GUI_TEXT` | 按 UI/world 拆分 |
| `getRendertypeTextSeeThroughShader()` | `TEXT_SEE_THROUGH` | - |
| `getRendertypeTextIntensityShader()` | `TEXT_INTENSITY` / `GUI_TEXT_INTENSITY` | 按 UI/world 拆分 |
| `getRendertypeTextBackgroundShader()` | `TEXT_BACKGROUND` | - |
| `getRendertypeTextBackgroundSeeThroughShader()` | `TEXT_BACKGROUND_SEE_THROUGH` | - |
| `getRendertypeTextIntensitySeeThroughShader()` | `TEXT_INTENSITY_SEE_THROUGH` | - |
| `getRendertypeGuiShader()` | `GUI` | - |
| `getRendertypeGuiOverlayShader()` | `GUI_OPAQUE_TEXTURED_BACKGROUND` / `GUI_NAUSEA_OVERLAY` | 按具体用途拆分 |
| `getRendertypeGuiTextHighlightShader()` | `GUI_TEXT_HIGHLIGHT` | - |
| `getRendertypeGlintShader()` | `GLINT` | - |
| `getRendertypeGlintTranslucentShader()` | (未确认,可能合并入 GLINT) | - |
| `getRendertypeLeashShader()` | `LEASH` | - |
| `getRendertypeWaterMaskShader()` | `WATER_MASK` | - |
| `getRendertypeLightningShader()` | `LIGHTNING` / `DRAGON_RAYS` | 按普通/龙息拆分 |
| `getRendertypeTripwireShader()` | (未确认,可能合并入 LINES) | - |

### 5.3 26.1.2 新增的 Pipeline(无 1.20.1 对应)

| 26.1.2 Pipeline | 说明 |
|---|---|
| `BREEZE_WIND` | 旋风特效(1.21 新实体) |
| `ENTITY_CUTOUT_DISSOLVE` | 溶解效果(新机制) |
| `DRAGON_RAYS` / `DRAGON_RAYS_DEPTH` | 龙息光束 |
| `SECONDARY_BLOCK_OUTLINE` | 次级方块轮廓 |
| `FLAT_CLOUDS` | 平坦云(1.21 新) |
| `WORLD_BORDER` | 世界边界 |
| `END_SKY` / `SUNRISE_SUNSET` / `STARS` / `CELESTIAL` | 天空渲染拆分为独立 pipeline |
| `ANIMATE_SPRITE_BLIT` / `ANIMATE_SPRITE_INTERPOLATE` | Sprite 动画(新工具) |
| `TRACY_BLIT` | Tracy profiler 集成 |
| `PANORAMA` / `MOJANG_LOGO` / `CROSSHAIR` / `VIGNETTE` | UI/HUD 专用 pipeline(原 GUI shader 拆分) |

---

## 6. Sampler 绑定差异

| 方面 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| Sampler 语义 | Sampler0/1/2 不变 | 同左 | Sampler0/1/2 不变(新增 InSampler/Sprite/CurrentSprite/NextSprite/DissolveMaskSampler) |
| 绑定时机 | `ShaderInstance.apply()` 内通过 `glUniform1i` | 同左 | Pipeline 编译时预绑定(未确认,可能由 GpuDevice 管理) |
| 纹理单元 | `GL_TEXTURE0 + index`(0x84C0+j) | 同左 | (未确认) |
| Sampler 类型 | 仅 sampler2D(GLSL) | 同左 | sampler2D + texelBuffer(新增) |
| 运行时绑定 | `setSampler(name, textureId)` | 同左 | `RenderSetup.withTexture(name, texture)`(声明式) |

---

## 7. 后处理 Shader 差异

| 方面 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 配置语言 | 自定义 JSON 解析 | 同左 | JSON → Codec(类型安全) |
| 数据模型 | `PostChain` 直接解析(无中间模型) | 同左 | `PostChainConfig` record(Codec 中间层) |
| Shader 程序 | `EffectInstance`(独立 shader) | 同左 | `PostChainConfig.Pass`(仅描述 vertex/fragment shader ID) |
| 投影矩阵 | `PostChain.shaderOrthoMatrix`(内嵌) | 同左 | `Projection` + `ProjectionMatrixBuffer`(外部 UBO) |
| 加载入口 | `Minecraft.gameRenderer` 直接创建 | 同左 | `ShaderManager.getPostChain()`(缓存) |
| 输入类型 | 隐式 target/texture(通过 `id:depth` 后缀区分) | 同左 | 显式 `TargetInput`/`TextureInput`(Codec 区分) |
| Aux targets | `addAuxAsset(name, supplier, w, h)` | 同左 | 合并入 Inputs(统一为 samplers) |

---

## 8. GL 状态集成度对比

| 状态 | 1.20.1(分散) | 1.21.1(分散) | 26.1.2(集成) |
|---|---|---|---|
| Blend Mode | `ShaderInstance.blend` | 同左 | `RenderPipeline.colorTargetState` |
| Depth Test | `RenderStateShard.DepthTestStateShard` | 同左 | `RenderPipeline.depthStencilState` |
| Stencil Test | `RenderStateShard.WriteMaskStateShard` | 同左 | `RenderPipeline.depthStencilState`(同字段) |
| Face Culling | `RenderStateShard.CullStateShard` | 同左 | `RenderPipeline.cull` |
| Polygon Mode | `RenderStateShard.LayeringStateShard` | 同左 | `RenderPipeline.polygonMode` |
| Vertex Format | `ShaderInstance.vertexFormat` | 同左 | `RenderPipeline.vertexFormat` + `vertexFormatMode` |
| Sort Key | `RenderType` 内部 | 同左 | `RenderPipeline.sortKey` |
| Shader 变异 | 不同 JSON 文件 | 同左 | `ShaderDefines` + 同一 GLSL 文件 |

---

## 9. 对 eyelib 的影响清单

### 9.1 直接影响

1. **Shader 注册方式变更**:1.20.1/1.21.1 通过 `GameRenderer.get*Shader()` + `RenderStateShard.ShaderStateShard` 注册 → 26.1.2 通过 `RenderPipelines` 静态常量 + `RenderSetup.builder(pipeline)` 注册。eyelib 的多版本适配需要在 Stonecutter 节点中分别实现。

2. **Uniform 设置路径变更**:1.20.1 通过 `shader.safeGetUniform("Name").set(value)` → 26.1.2 通过 `RenderSystem.getDynamicUniforms().writeTransform(...)` 写 UBO。渲染管线中的 uniform 设置需要端口适配。

3. **ShaderInstance 类已删除**:依赖 `ShaderInstance` 的代码无法在 26.1.2 编译,需要条件编译(`//?`)或版本隔离。

4. **PostChain 加载路径变更**:`new PostChain(...)` 在 26.1.2 不再可用,需通过 `ShaderManager.getPostChain()` 获取。

5. **无 JSON shader 配置**:如果需要注册自定义 shader,26.1.2 需要创建 `RenderPipeline` 并通过 `RegisterRenderPipelinesEvent` 注册,不能创建 JSON 文件。

### 9.2 间接影响

6. **EffectInstance 已删除**:后处理效果需要通过 `PostChainConfig` + `ShaderManager` 管理。

7. **GlslPreprocessor 不直接暴露**:26.1.2 的预处理完全由 `ShaderManager` 内部管理。

8. **RenderType 创建模式变更**:26.1.2 使用 `RenderSetup.builder(pipeline).withTexture("Sampler0", tex).createRenderSetup()` 创建 RenderType,而非 `RenderType.create("name", CompositeState)`。

9. **VertexFormat 语义保持一致**:三个版本的 `VertexFormat` 枚举值未变(`BLOCK`/`ENTITY`/`PARTICLE`/`POSITION_COLOR` 等),但 26.1.2 增加了 `vertexFormatMode`(QUADS/TRIANGLES/LINES 等)。

10. **动态 UBO 生命周期**:26.1.2 的 `DynamicUniforms` 需要在帧开始时 `reset()`,帧结束时关闭。eyelib 的自定义渲染循环需要适配此生命周期。

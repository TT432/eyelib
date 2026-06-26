# 着色器系统 — 26.1.2 (NeoForge, RenderPipeline 时代)

> 基于 `.local_ref/mc/26.1.2/sources/` 提取源码。
> 所有路径相对于该目录。

## 目录

1. [类位置与职责](#1-类位置与职责)
2. [Shader 加载与编译流程](#2-shader-加载与编译流程)
3. [ShaderDefines 定义系统](#3-shaderdefines-定义系统)
4. [Uniform 系统 (UniformDescription + UniformType)](#4-uniform-系统-uniformdescription--uniformtype)
5. [DynamicUniforms (UBO)](#5-dynamicuniforms-ubo)
6. [Sampler 描述](#6-sampler-描述)
7. [Pipeline 注册表 (RenderPipelines)](#7-pipeline-注册表-renderpipelines)
8. [Pipeline 绑定机制](#8-pipeline-绑定机制)
9. [后处理 Shader (PostChain via ShaderManager)](#9-后处理-shader-postchain-via-shadermanager)
10. [关键不变量与约束](#10-关键不变量与约束)

---

## 1. 类位置与职责

| 类 | 文件 | 职责 |
|---|---|---|
| `RenderPipeline` | `com/mojang/blaze3d/pipeline/RenderPipeline.java` | Shader pipeline 声明:vertex/fragment shader ID + ShaderDefines + samplers + UniformDescription + 深度/混合状态 |
| `RenderPipeline.Snippet` | (同上,内部 record) | 可复用 shader 配置片段,通过 Builder 组合成完整 pipeline |
| `RenderPipeline.Builder` | (同上,内部类) | Pipeline 构造器,支持链式 API |
| `RenderPipeline.UniformDescription` | (同上,内部 record) | Uniform 描述:name + UniformType + 可选 TextureFormat |
| `RenderPipelines` | `RenderPipelines.java` | 所有 pipeline 常量 + SNIPPET 常量的注册中心 |
| `ShaderManager` | `ShaderManager.java` | Shader 源加载/编译/缓存 + PostChain 管理 |
| `ShaderDefines` | `ShaderDefines.java` | Shader `#define` 系统:values(map) + flags(set) |
| `UniformType` | `com/mojang/blaze3d/shaders/UniformType.java` | Uniform 类型枚举:UNIFORM_BUFFER / TEXEL_BUFFER |
| `DynamicUniforms` | `DynamicUniforms.java` | 动态 UBO:Transform(modelView,colorMod,textureMat) + ChunkSection |
| `DynamicUniformStorage` | `DynamicUniformStorage.java` | 环形缓冲 UBO 管理(ring buffer) |
| `PostChain` | `PostChain.java` | 后处理链(含 passes + targets + ProjectionMatrixBuffer) |
| `PostChainConfig` | `PostChainConfig.java` | 后处理链 JSON/Codec 数据模型 |
| `PostPass` | `PostPass.java` | 单次后处理 pass |
| `ShaderType` | (ShaderManager 内关联) | 标识 vertex/fragment shader 类型 |
| `CompiledRenderPipeline` | (GPU 侧) | 已编译的 GPU pipeline 对象 |
| `GpuDevice` | (GPU 侧) | GPU 设备抽象(编译 pipeline) |

---

## 2. Shader 加载与编译流程

### 2.1 架构概览(与 1.20.1 的重大差异)

```
旧版(1.20.1): ShaderInstance JSON → ShaderInstance 构造函数直接编译 GLSL → ProgramManager.createProgram
新版(26.1.2): RenderPipeline Java 声明 → ShaderManager 收集源码 → GpuDevice 编译 pipeline
```

**关键变化**:shader 不再从 JSON 文件加载!`RenderPipeline` 通过 Java Builder API 声明 vertex/fragment shader 的 Identifier、ShaderDefines、samplers、uniforms 等,**不解析任何 JSON**。

### 2.2 ShaderManager 加载流程

`ShaderManager` 继承 `SimplePreparableReloadListener<ShaderManager.Configs>`,实现标准的资源重载生命周期:

```
ShaderManager
  │
  ├─ prepare(ResourceManager) → ShaderManager.Configs
  │     │
  │     ├─ 扫描 shaders/ 目录资源
  │     │     ├─ ShaderType.byLocation() 判断文件类型(.vsh=vertex, .fsh=fragment)
  │     │     ├─ 或 .glsl 后缀(include 文件)
  │     │     └─ loadShader() 读取源码,经 GlslPreprocessor 展开 #import
  │     │
  │     └─ 扫描 shaders/post_effect/ 目录的 PostChainConfig(JSON→Codec)
  │
  └─ apply(Configs, ResourceManager) → 预编译静态 pipeline
        │
        ├─ 创建 CompilationCache(持有所有 shader 源)
        ├─ RenderPipelines.getStaticPipelines() 获取所有注册的 pipeline
        ├─ device.clearPipelineCache() 清空旧缓存
        ├─ device.precompilePipeline(pipeline, cache::getShaderSource)
        │     └─ 对每个 pipeline:根据 vertexShader/fragmentShader Identifier
        │        查找对应的 GLSL 源 → 编译 GPU pipeline
        └─ 若任一个失败 → 清空缓存,抛 RuntimeException
```

### 2.3 GLSL 源位置

与 1.20.1 不同,GLSL 源不再与 JSON 成对出现。shader ID 直接映射到资源路径:

- `Identifier.withDefaultNamespace("core/entity")` → `shaders/core/entity.vsh` / `shaders/core/entity.fsh`
- `Identifier.withDefaultNamespace("core/rendertype_text")` → `shaders/core/rendertype_text.vsh` / `.fsh`
- `Identifier.withDefaultNamespace("core/particle")` → `shaders/core/particle.vsh` / `.fsh`

GLSL 源文件位于 `shaders/` 目录下,按 `ShaderType` 的扩展名区分(.vsh=vertex, .fsh=fragment)。

### 2.4 ShaderType 枚举

`ShaderType.byLocation(Identifier)` 根据文件扩展名判断:
- `.vsh` → `ShaderType.VERTEX`
- `.fsh` → `ShaderType.FRAGMENT`
- `.glsl` → (include 文件,不编译)

---

## 3. ShaderDefines 定义系统

### 3.1 概述

`ShaderDefines` 是一个 `record(Map<String, String> values, Set<String> flags)`:

- **values**:键值对 macro,如 `ALPHA_CUTOUT=0.5` → `#define ALPHA_CUTOUT 0.5`
- **flags**:仅名称 macro,如 `EMISSIVE` → `#define EMISSIVE`

### 3.2 转换为 GLSL 预处理指令

```java
// ShaderDefines.asSourceDirectives()
public String asSourceDirectives() {
    StringBuilder directives = new StringBuilder();
    for (Entry<String, String> entry : this.values.entrySet()) {
        directives.append("#define ").append(entry.getKey())
                   .append(" ").append(entry.getValue()).append('\n');
    }
    for (String flag : this.flags) {
        directives.append("#define ").append(flag).append('\n');
    }
    return directives.toString();
}
```

### 3.3 Pipeline 中使用的 ShaderDefines

从 `RenderPipelines.java` 提取的 define 清单:

| Define 名称 | 类型 | 用途 Pipeline | 说明 |
|---|---|---|---|
| `ALPHA_CUTOUT` | value(float) | CUTOUT_BLOCK, ENTITY_CUTOUT 等 | Alpha 裁剪阈值(0.5/0.1/0.01) |
| `EMISSIVE` | flag | ENTITY_EMISSIVE_SNIPPET, EYES, ENERGY_SWIRL | 自发光模式(跳过光照计算) |
| `APPLY_TEXTURE_MATRIX` | flag | BREEZE_WIND, ENERGY_SWIRL | 应用纹理矩阵变换 |
| `NO_OVERLAY` | flag | ARMOR_*, BREEZE_WIND, EYES, ENERGY_SWIRL | 无叠加纹理(无 Sampler1) |
| `NO_CARDINAL_LIGHTING` | flag | BREEZE_WIND, ENERGY_SWIRL, EYES | 无方向光照 |
| `PER_FACE_LIGHTING` | flag | ARMOR_*, ENTITY_CUTOUT, ENTITY_TRANSLUCENT | 逐面光照计算 |
| `PORTAL_LAYERS` | value(int) | END_PORTAL(15), END_GATEWAY(16) | 传送门特效层数 |
| `DISSOLVE` | flag | ENTITY_CUTOUT_DISSOLVE | 溶解效果(额外 DissolveMaskSampler) |

### 3.4 withOverrides 合并

```java
public ShaderDefines withOverrides(ShaderDefines defines) {
    // 合并 values(后者覆盖)
    // 合并 flags(求并集)
    // 任一为空则返回另一方
}
```

Snippet 组合时,后添加的 define 会覆盖或追加到已有的 define 上。

---

## 4. Uniform 系统 (UniformDescription + UniformType)

### 4.1 UniformType 枚举

```java
@NonExhaustiveEnum(reason = "Further uniform types such as SSBO and StorageTexelBuffer may be added")
public enum UniformType {
    UNIFORM_BUFFER("ubo"),   // Uniform Buffer Object (std140)
    TEXEL_BUFFER("utb");     // Texel Buffer Object (需要 TextureFormat)
}
```

与 1.20.1 的关键差异:**不再有 `int`/`float`/`mat4` 等细粒度类型**。所有 uniform 通过 UBO(Uniform Buffer Object)传递,类型信息由 Shader 侧的 `layout(std140)` 块声明决定。

### 4.2 UniformDescription

```java
public record UniformDescription(String name, UniformType type, @Nullable TextureFormat textureFormat)
```

- `name`:UBO 块名称(如 `"DynamicTransforms"`, `"Projection"`, `"Fog"`, `"ChunkSection"` 等)
- `type`:`UNIFORM_BUFFER`(UBO) 或 `TEXEL_BUFFER`(texel buffer,如 `"CloudFaces"`)
- `textureFormat`:仅 `TEXEL_BUFFER` 需要(如 `TextureFormat.RED8I`)

### 4.3 统一 UBO 命名的惯例

| UBO 名称 | 出现位置 | 说明 |
|---|---|---|
| `DynamicTransforms` | MATRICES_PROJECTION_SNIPPET | 动态变换(每绘制调用变化) |
| `Projection` | MATRICES_PROJECTION_SNIPPET, TERRAIN_SNIPPET | 投影矩阵(每帧不变) |
| `Fog` | FOG_SNIPPET | 雾参数 |
| `Globals` | GLOBALS_SNIPPET | 全局参数(游戏时间等) |
| `Lighting` | MATRICES_FOG_LIGHT_DIR_SNIPPET | 光照方向和强度 |
| `ChunkSection` | TERRAIN_SNIPPET | chunk 渲染参数(ModelView, 坐标, 纹理尺寸) |
| `LightmapInfo` | LIGHTMAP | lightmap 参数 |
| `CloudInfo` | CLOUDS_SNIPPET | 云参数 |
| `CloudFaces` | CLOUDS_SNIPPET | 云面数据(texel buffer) |
| `SpriteAnimationInfo` | ANIMATE_SPRITE_SNIPPET | sprite 动画信息 |

---

## 5. DynamicUniforms (UBO)

### 5.1 概述

`DynamicUniforms` 管理两个动态 UBO:

```java
public class DynamicUniforms implements AutoCloseable {
    // 28 bytes std140: mat4 + vec4 + vec3 + mat4 (含 padding)
    private final DynamicUniformStorage<Transform> transforms = ...;

    // 24 bytes std140: mat4 + float + ivec2 + ivec3 (含 padding)
    private final DynamicUniformStorage<ChunkSectionInfo> chunkSections = ...;
}
```

### 5.2 Transform UBO (per-draw-call)

```java
public record Transform(Matrix4fc modelView, Vector4fc colorModulator,
                        Vector3fc modelOffset, Matrix4fc textureMatrix)
    implements DynamicUniformStorage.DynamicUniform {
    @Override
    public void write(ByteBuffer buffer) {
        Std140Builder.intoBuffer(buffer)
            .putMat4f(this.modelView)        // 64 bytes
            .putVec4(this.colorModulator)    // 16 bytes
            .putVec3(this.modelOffset)       // 16 bytes (std140 padding)
            .putMat4f(this.textureMatrix);   // 64 bytes
    }
}
```

**对应 1.20.1 的预定义 uniform**:`ModelViewMat`, `ColorModulator`, `IViewRotMat`(→modelOffset), `TextureMat`。

### 5.3 ChunkSectionInfo UBO

```java
public record ChunkSectionInfo(Matrix4fc modelView, int x, int y, int z,
                                float visibility, int textureAtlasWidth,
                                int textureAtlasHeight)
    implements DynamicUniformStorage.DynamicUniform
```

**对应 1.20.1 的 `ChunkOffset` uniform**(概念扩展)。

### 5.4 DynamicUniformStorage 环形缓冲

`DynamicUniformStorage` 使用环形缓冲(Ring Buffer)策略管理 UBO 分配:
- `writeUniform()`:分配一个 UBO slot,写入数据,返回 `GpuBufferSlice`
- `endFrame()`:标记帧结束,释放所有本帧分配的 slot
- 自动管理 UBO 大小扩展(以 `uniformOffsetAlignment` 对齐)

### 5.5 获取 DynamicUniforms

```java
// RenderSystem.getDynamicUniforms()
DynamicUniforms du = RenderSystem.getDynamicUniforms();
// 写入 transform 数据
GpuBufferSlice slice = du.writeTransform(modelView, colorMod, modelOffset, texMat);
// 写入 chunk section 数据
GpuBufferSlice[] slices = du.writeChunkSections(infos);
```

`RenderSystem` 持有全局 `DynamicUniforms` 实例,在渲染帧开始时通过 `reset()` 清理。

---

## 6. Sampler 描述

### 6.1 Pipeline 中的 Sampler

在 `RenderPipeline.Builder` 中通过 `withSampler(String name)` 声明:

```java
public static final RenderPipeline.Snippet ENTITY_SNIPPET = RenderPipeline.builder(...)
    .withSampler("Sampler0")   // 主纹理
    .withSampler("Sampler2")   // Lightmap 纹理
    ...
    .buildSnippet();
```

Sampler 名称保持与 1.20.1 一致的 `Sampler0`/`Sampler1`/`Sampler2` 语义。但**不再是 GLSL uniform 的 sampler2D 通过 glUniform1i 绑定**:sampler 声明只是元数据,实际绑定由 `RenderSetup`(RenderType 构建层)通过 `withTexture("Sampler0", texture)` 指定具体纹理。

### 6.2 特殊 Sampler

从 RenderPipelines 提取的非标准 sampler:

| Sampler 名称 | 出现 Pipeline | 说明 |
|---|---|---|
| `Sampler0` | 大部分 pipeline | 主纹理(diffuse/atlas) |
| `Sampler1` | ENTITY_SOLID, ENTITY_CUTOUT 等 | 叠加纹理(overlay) |
| `Sampler2` | 大部分 pipeline | Lightmap 纹理 |
| `InSampler` | ENTITY_OUTLINE_BLIT, TRACY_BLIT | Blit 操作的输入纹理 |
| `Sprite` | ANIMATE_SPRITE_BLIT | Sprite 动画源帧 |
| `CurrentSprite` | ANIMATE_SPRITE_INTERPOLATE | 插值的当前帧 |
| `NextSprite` | ANIMATE_SPRITE_INTERPOLATE | 插值的下一帧 |
| `DissolveMaskSampler` | ENTITY_CUTOUT_DISSOLVE | 溶解遮罩纹理 |

---

## 7. Pipeline 注册表 (RenderPipelines)

### 7.1 SNIPPET 常量(可复用配置片段)

Snippets 通过组合形成最终 Pipeline。按组合层级:

**基础变换 Snippets**:
| SNIPPET | 包含内容 |
|---|---|
| `MATRICES_PROJECTION_SNIPPET` | DynamicTransforms UBO + Projection UBO |
| `FOG_SNIPPET` | Fog UBO |
| `GLOBALS_SNIPPET` | Globals UBO |
| `MATRICES_FOG_SNIPPET` | MATRICES_PROJECTION + FOG |
| `MATRICES_FOG_LIGHT_DIR_SNIPPET` | MATRICES_PROJECTION + FOG + Lighting UBO |

**布局 Snippets**:
| SNIPPET | 用途 |
|---|---|
| `GENERIC_BLOCKS_SNIPPET` | Sampler0+Sampler2 + BLOCK vertexFormat + DepthStencil |
| `TERRAIN_SNIPPET` | GENERIC_BLOCKS + Projection + ChunkSection UBO + core/terrain VS/FS |
| `BLOCK_SNIPPET` | GENERIC_BLOCKS + MATRICES_PROJECTION + core/block VS/FS |
| `ENTITY_SNIPPET` | MATRICES_FOG_LIGHT_DIR + core/entity VS/FS + Sampler0/Sampler2 + ENTITY vertexFormat |
| `ENTITY_EMISSIVE_SNIPPET` | 同上,加 `EMISSIVE` define,无 Sampler2 |
| `ITEM_SNIPPET` | MATRICES_FOG_LIGHT_DIR + core/item VS/FS + Sampler0/Sampler2 |
| `PARTICLE_SNIPPET` | MATRICES_FOG + core/particle VS/FS + Sampler0/Sampler2 + PARTICLE vertexFormat |
| `WEATHER_SNIPPET` | PARTICLE + TRANSLUCENT blend + cull=false |
| `TEXT_SNIPPET` | MATRICES_PROJECTION + TRANSLUCENT blend + POSITION_COLOR_TEX_LIGHTMAP vertexFormat |
| `GUI_SNIPPET` | MATRICES_PROJECTION + core/gui VS/FS + TRANSLUCENT blend + POSITION_COLOR |
| `GUI_TEXTURED_SNIPPET` | MATRICES_PROJECTION + core/position_tex_color + Sampler0 + TRANSLUCENT blend |
| `GUI_TEXT_SNIPPET` | TEXT_SNIPPET + 无 DepthStencil |
| `LINES_SNIPPET` | MATRICES_FOG + GLOBALS + core/rendertype_lines + TRANSLUCENT blend + cull=false |
| `DEBUG_FILLED_SNIPPET` | MATRICES_PROJECTION + core/position_color + TRANSLUCENT blend |
| `OUTLINE_SNIPPET` | MATRICES_PROJECTION + core/rendertype_outline + Sampler0 |
| `POST_PROCESSING_SNIPPET` | EMPTY vertexFormat + TRIANGLES mode |
| `ANIMATE_SPRITE_SNIPPET` | core/animate_sprite VS + SpriteAnimationInfo UBO + EMPTY format |
| `BEACON_BEAM_SNIPPET` | MATRICES_FOG + core/rendertype_beacon_beam + Sampler0 + BLOCK format |
| `END_PORTAL_SNIPPET` | MATRICES_PROJECTION + FOG + GLOBALS + core/rendertype_end_portal + Sampler0/Sampler1 + POSITION format |
| `CLOUDS_SNIPPET` | MATRICES_FOG + core/rendertype_clouds + TRANSLUCENT blend + CloudInfo + CloudFaces(texel buffer) |

### 7.2 完整 Pipeline 常量清单

按来源 Snippet 分类(共 **70+** pipeline 常量):

#### Block/Terrain (7 个)
| Pipeline 常量 | 基础 Snippet | 特殊设置 |
|---|---|---|
| `SOLID_BLOCK` | BLOCK | - |
| `SOLID_TERRAIN` | TERRAIN | - |
| `WIREFRAME` | TERRAIN | PolygonMode.WIREFRAME |
| `CUTOUT_BLOCK` | BLOCK | ALPHA_CUTOUT=0.5 |
| `CUTOUT_TERRAIN` | TERRAIN | ALPHA_CUTOUT=0.5 |
| `TRANSLUCENT_TERRAIN` | TERRAIN | TRANSLUCENT blend + ALPHA_CUTOUT=0.01 |
| `TRANSLUCENT_BLOCK` | BLOCK | TRANSLUCENT blend + ALPHA_CUTOUT=0.01 |

#### Entity/生物 (16 个)
| Pipeline 常量 | 基础 Snippet | 特殊设置 |
|---|---|---|
| `ENTITY_SOLID` | ENTITY | Sampler1 |
| `ENTITY_SOLID_Z_OFFSET_FORWARD` | ENTITY | Sampler1(同 ENTITY_SOLID,Z偏移变体) |
| `ENTITY_CUTOUT_CULL` | ENTITY | ALPHA_CUTOUT=0.1 + Sampler1 |
| `ENTITY_CUTOUT` | ENTITY | ALPHA_CUTOUT=0.1 + PER_FACE_LIGHTING + cull=false + Sampler1 |
| `ENTITY_CUTOUT_Z_OFFSET` | ENTITY | 同 ENTITY_CUTOUT |
| `ENTITY_CUTOUT_DISSOLVE` | ENTITY | ALPHA_CUTOUT=0.1 + PER_FACE_LIGHTING + DISSOLVE + DissolveMaskSampler |
| `ENTITY_TRANSLUCENT` | ENTITY | ALPHA_CUTOUT=0.1 + PER_FACE_LIGHTING + TRANSLUCENT blend |
| `ENTITY_TRANSLUCENT_EMISSIVE` | ENTITY_EMISSIVE | ALPHA_CUTOUT=0.1 + PER_FACE_LIGHTING + TRANSLUCENT blend |
| `ENTITY_TRANSLUCENT_CULL` | ENTITY | ALPHA_CUTOUT=0.1 + TRANSLUCENT blend + cull=true |
| `ARMOR_CUTOUT_NO_CULL` | ENTITY | ALPHA_CUTOUT=0.1 + NO_OVERLAY + PER_FACE_LIGHTING + cull=false |
| `ARMOR_DECAL_CUTOUT_NO_CULL` | ENTITY | 同上 + DepthStencil(CompareOp.EQUAL) |
| `ARMOR_TRANSLUCENT` | ENTITY | ALPHA_CUTOUT=0.1 + NO_OVERLAY + PER_FACE_LIGHTING + TRANSLUCENT blend |
| `END_CRYSTAL_BEAM` | ENTITY | ALPHA_CUTOUT=0.1 + NO_OVERLAY + cull=false |
| `BANNER_PATTERN` | ENTITY | NO_OVERLAY + TRANSLUCENT blend |
| `BREEZE_WIND` | ENTITY | ALPHA_CUTOUT=0.1 + APPLY_TEXTURE_MATRIX + NO_OVERLAY + NO_CARDINAL_LIGHTING + TRANSLUCENT blend |
| `ENERGY_SWIRL` | MATRICES_FOG | core/entity FS + EMISSIVE + ALPHA_CUTOUT=0.1 + NO_OVERLAY + NO_CARDINAL_LIGHTING + APPLY_TEXTURE_MATRIX + ADDITIVE blend |

#### Item (2 个)
| Pipeline 常量 | 基础 Snippet | 特殊设置 |
|---|---|---|
| `ITEM_CUTOUT` | ITEM | ALPHA_CUTOUT=0.1 |
| `ITEM_TRANSLUCENT` | ITEM | ALPHA_CUTOUT=0.1 + TRANSLUCENT blend |

#### 特效 (9 个)
| Pipeline 常量 | 基础 Snippet | 特殊设置 |
|---|---|---|
| `EYES` | MATRICES_FOG + core/entity FS | EMISSIVE + NO_OVERLAY + NO_CARDINAL_LIGHTING + TRANSLUCENT blend |
| `ENTITY_SHADOW` | MATRICES_FOG + core/rendertype_entity_shadow | TRANSLUCENT blend |
| `BEACON_BEAM_OPAQUE` | BEACON_BEAM | - |
| `BEACON_BEAM_TRANSLUCENT` | BEACON_BEAM | TRANSLUCENT blend |
| `LEASH` | MATRICES_FOG + core/rendertype_leash | Sampler2 + TRIANGLE_STRIP |
| `WATER_MASK` | MATRICES_PROJECTION + core/rendertype_water_mask | TRANSLUCENT blend(可选) |
| `GLINT` | MATRICES_PROJECTION + FOG + GLOBALS + core/glint | GLINT blend + POSITION_TEX format |
| `CRUMBLING` | MATRICES_PROJECTION + core/rendertype_crumbling | SRC_COLOR/DST_COLOR multifactor blend |
| `WORLD_BORDER` | MATRICES_PROJECTION + core/rendertype_world_border | OVERLAY blend + POSITION_TEX format |

#### Text (10 个)
| Pipeline 常量 | 基础 Snippet | 特殊设置 |
|---|---|---|
| `TEXT` | TEXT + FOG + core/rendertype_text | Sampler0/Sampler2 |
| `GUI_TEXT` | GUI_TEXT + FOG + core/rendertype_text | Sampler0/Sampler2 |
| `TEXT_BACKGROUND` | TEXT + FOG + core/rendertype_text_background | Sampler2 + POSITION_COLOR_LIGHTMAP |
| `TEXT_INTENSITY` | TEXT + FOG + core/rendertype_text_intensity | Sampler0/Sampler2 + depth bias |
| `GUI_TEXT_INTENSITY` | GUI_TEXT + FOG + core/rendertype_text_intensity | Sampler0/Sampler2 |
| `TEXT_POLYGON_OFFSET` | TEXT + FOG + core/rendertype_text | Sampler0/Sampler2 + depth bias |
| `TEXT_SEE_THROUGH` | TEXT + core/rendertype_text_see_through | Sampler0 + 无 DepthStencil |
| `TEXT_BACKGROUND_SEE_THROUGH` | TEXT + core/rendertype_text_background_see_through | 无 DepthStencil |
| `TEXT_INTENSITY_SEE_THROUGH` | TEXT + core/rendertype_text_intensity_see_through | Sampler0 + 无 DepthStencil |

#### 天气/天空 (6 个)
| Pipeline 常量 | 基础 Snippet | 特殊设置 |
|---|---|---|
| `FLAT_CLOUDS` | CLOUDS | cull=false |
| `CLOUDS` | CLOUDS | - |
| `SKY` | MATRICES_FOG + core/sky | TRIANGLE_FAN |
| `END_SKY` | MATRICES_PROJECTION + core/position_tex_color | TRANSLUCENT blend |
| `SUNRISE_SUNSET` | MATRICES_PROJECTION + core/position_color | TRANSLUCENT blend + TRIANGLE_FAN |
| `STARS` | MATRICES_PROJECTION + core/stars | OVERLAY blend |

#### 线条 (3 个)
| Pipeline 常量 | 基础 Snippet | 特殊设置 |
|---|---|---|
| `LINES` | LINES | - |
| `LINES_TRANSLUCENT` | LINES | 空 DepthStencil(无深度写) |
| `LINES_DEPTH_BIAS` | LINES | depth bias -1.0/-1.0 |
| `SECONDARY_BLOCK_OUTLINE` | LINES | TRANSLUCENT blend + 空 DepthStencil |

#### 调试 (6 个)
| Pipeline 常量 | 基础 Snippet | 特殊设置 |
|---|---|---|
| `DEBUG_POINTS` | MATRICES_PROJECTION + core/debug_point + core/position_color | POINTS mode |
| `DEBUG_FILLED_BOX` | DEBUG_FILLED | - |
| `DEBUG_QUADS` | DEBUG_FILLED | cull=false |
| `DEBUG_TRIANGLE_FAN` | DEBUG_FILLED | cull=false + TRIANGLE_FAN |
| `WORLD_BORDER` | (见特效) | - |
| `DRAGON_RAYS` | MATRICES_FOG + core/rendertype_lightning | LIGHTNING blend + TRIANGLES |
| `DRAGON_RAYS_DEPTH` | MATRICES_FOG + core/position | 无 color target |

#### GUI (8 个)
| Pipeline 常量 | 基础 Snippet | 特殊设置 |
|---|---|---|
| `GUI` | GUI | - |
| `GUI_INVERT` | GUI | INVERT blend |
| `GUI_TEXT_HIGHLIGHT` | GUI | ADDITIVE blend |
| `GUI_TEXTURED` | GUI_TEXTURED | - |
| `GUI_TEXTURED_PREMULTIPLIED_ALPHA` | GUI_TEXTURED | TRANSLUCENT_PREMULTIPLIED_ALPHA blend |
| `BLOCK_SCREEN_EFFECT` | GUI_TEXTURED | - |
| `FIRE_SCREEN_EFFECT` | GUI_TEXTURED | - |
| `GUI_OPAQUE_TEXTURED_BACKGROUND` | GUI_TEXTURED | 无 blend + 颜色 mask=15 |
| `GUI_NAUSEA_OVERLAY` | GUI_TEXTURED | ADDITIVE blend |

#### Post-processing (4 个)
| Pipeline 常量 | 基础 Snippet | 特殊设置 |
|---|---|---|
| `VIGNETTE` | GUI_TEXTURED | ZERO/ONE_MINUS_SRC_COLOR blend |
| `CROSSHAIR` | GUI_TEXTURED | INVERT blend |
| `MOJANG_LOGO` | GUI_TEXTURED | SRC_ALPHA/ONE blend |
| `ENTITY_OUTLINE_BLIT` | core/screenquad + core/blit_screen | InSampler + ENTITY_OUTLINE_BLIT blend |
| `TRACY_BLIT` | core/screenquad + core/blit_screen | InSampler |

#### Extras (6 个)
| Pipeline 常量 | 基础 Snippet | 特殊设置 |
|---|---|---|
| `END_PORTAL` | END_PORTAL | PORTAL_LAYERS=15 |
| `END_GATEWAY` | END_PORTAL | PORTAL_LAYERS=16 |
| `LIGHTNING` | MATRICES_FOG + core/rendertype_lightning | LIGHTNING blend |
| `OPAQUE_PARTICLE` | PARTICLE | - |
| `TRANSLUCENT_PARTICLE` | PARTICLE | TRANSLUCENT blend |
| `WEATHER_DEPTH_WRITE` | WEATHER | - |
| `WEATHER_NO_DEPTH_WRITE` | WEATHER | 空 DepthStencil |
| `PANORAMA` | MATRICES_PROJECTION + core/panorama | - |
| `OUTLINE_CULL` | OUTLINE | - |
| `OUTLINE_NO_CULL` | OUTLINE | cull=false |
| `LIGHTMAP` | core/screenquad + core/lightmap | LightmapInfo UBO |
| `CELESTIAL` | MATRICES_PROJECTION + core/position_tex | OVERLAY blend |
| `ANIMATE_SPRITE_BLIT` | ANIMATE_SPRITE | core/animate_sprite_blit FS + Sprite sampler |
| `ANIMATE_SPRITE_INTERPOLATE` | ANIMATE_SPRITE | core/animate_sprite_interpolate FS + CurrentSprite/NextSprite samplers |

---

## 8. Pipeline 绑定机制

### 8.1 与 1.20.1 apply() 的差异

26.1.2 的 `RenderPipeline` **不再有 apply()/clear() 方法**。Pipeline 绑定由底层 `GpuDevice` + RenderSystem 管理:

- `CompiledRenderPipeline`:GPU 侧已编译的 pipeline 对象(含 shader program + 绑定状态)
- `RenderSystem.setPipeline(RenderPipeline)`:设置当前 pipeline
- `RenderSystem.getDevice().getOrCompilePipeline(pipeline, sourceProvider)`:编译/获取缓存的 pipeline

### 8.2 RenderPipeline 包含的渲染状态

`RenderPipeline` 不仅包含 shader,还集成了传统上分散在 `RenderStateShard` 中的状态:

| 状态 | RenderPipeline 字段 | 对应 1.20.1 |
|---|---|---|
| Vertex/Fragment Shader | `vertexShader`/`fragmentShader` | `Program`(编译的 GLSL) |
| Shader Defines | `shaderDefines` | (无,1.20.1 无此概念) |
| Samplers | `samplers` | `ShaderInstance.samplerNames` |
| Uniforms | `uniforms` | `ShaderInstance.uniformMap` |
| Depth/Stencil | `depthStencilState` | `RenderStateShard.DepthTestStateShard` |
| Polygon Mode | `polygonMode` | `RenderStateShard.LayeringStateShard.polygonOffset` |
| Face Culling | `cull` | `RenderStateShard.CullStateShard` |
| Color Blend Target | `colorTargetState` | `RenderStateShard.TransparencyStateShard` |
| Vertex Format | `vertexFormat` | `ShaderInstance.vertexFormat` |
| Vertex Mode | `vertexFormatMode` | (GL draw mode:QUADS/TRIANGLES等) |
| Sort Key | `sortKey` | (渲染排序) |

---

## 9. 后处理 Shader (PostChain via ShaderManager)

### 9.1 与 1.20.1 的核心差异

| 方面 | 1.20.1 | 26.1.2 |
|---|---|---|
| 配置格式 | 自定义 JSON 解析(手动 `GsonHelper`) | JSON → Codec(`PostChainConfig.CODEC`) |
| 加载 | `PostChain` 直接解析 JSON | `ShaderManager.loadPostChain()` → `PostChainConfig` → `PostChain.load()` |
| Shader 编译 | `EffectInstance` 内部编译 GLSL | `GpuDevice` 编译 |
| 投影矩阵 | `PostChain` 内嵌正交矩阵 | `Projection` + `ProjectionMatrixBuffer` |
| 管理 | `Minecraft.gameRenderer` 直接持有 PostChain | `ShaderManager` 管理(CompilationCache) |

### 9.2 PostChainConfig Codec 结构

```java
public record PostChainConfig(
    Map<Identifier, InternalTarget> internalTargets,
    List<Pass> passes
) {
    record Pass(Identifier vertexShaderId, Identifier fragmentShaderId,
                List<Input> inputs, Identifier outputTarget,
                Map<String, Float> uniforms) {}
    record TargetInput(String samplerName, Identifier targetId,
                        boolean useDepthBuffer, boolean bilinear) {}  // Input 的一种
    record TextureInput(String samplerName, Identifier location,
                         int width, int height, boolean bilinear) {}  // Input 的另一种
    record InternalTarget(Optional<Integer> width, Optional<Integer> height,
                           boolean persistent, int clearColor) {}
}
```

### 9.3 PostChain 加载

```java
PostChain.load(
    config,           // PostChainConfig(已验证的 Codec 输出)
    textureManager,   // 纹理管理器
    allowedTargets,   // 允许的外部 target(如 LevelTargetBundle.MAIN_TARGET_ID)
    id,               // post_effect 名称标识符
    projection,       // 投影矩阵(默认正交)
    projectionMatrixBuffer // 投影矩阵 UBO 缓冲
)
```

---

## 10. 关键不变量与约束

1. **Pipeline 声明式**:所有 shader 配置通过 Java Builder API 声明,不再有 JSON shader 描述文件。shader 元数据(define/sampler/uniform)和 GL 状态(depth/cull/blend)统一在 `RenderPipeline` 中。
2. **Snippet 组合**:通用 shader 配置片段(Snippet)通过 `RenderPipeline.builder(Snippet...)` 组合。Snippet 之间以追加方式合并 samplers/uniforms/defines。
3. **Pipeline 缓存**:`ShaderManager.apply()` 在资源重载时调用 `GpuDevice.precompilePipeline()` 预编译所有静态 pipeline。编译失败会导致 RuntimeException 并清空整个缓存。
4. **UBO 替代 per-uniform 上传**:26.1.2 不再逐个上传 uniform(glUniform*),改为通过 `DynamicUniformStorage` 写入 UBO ring buffer,shader 侧用 `layout(std140)` 块读取。
5. **ShaderDefines 影响编译**:pipeline 的 `ShaderDefines` 通过 `asSourceDirectives()` 生成 GLSL `#define` 预处理指令,改变同一个 `.vsh`/`.fsh` 文件的编译行为(条件编译变体)。
6. **NeoForge 扩展点**:`RenderPipelines` 提供 `registerCustomPipelines()` 方法触发 `RegisterRenderPipelinesEvent`,允许 mod 注册自定义 pipeline。
7. **PostChain 的 Projection 分离**:post-process 的投影矩阵从 `PostChain` 解耦,提升到 `ShaderManager.postChainProjection` + `ProjectionMatrixBuffer`,实现 UBO 绑定方式。
8. **UniformType 非穷尽**:`@NonExhaustiveEnum` 表示未来可能添加 SSBO、StorageTexelBuffer 等新类型。
9. **无 apply/clear**:不再有 `ShaderInstance.apply()`/`clear()` 模式。Pipeline 切换通过 `RenderSystem.setPipeline()` 完成,GPU 状态统一由底层管理。
10. **Shader 源加载时机**:Shader GLSL 源在 `ShaderManager.prepare()` 阶段全部读入内存并预处理 `#import`,编译在实际使用时或 `apply()` 预编译阶段完成。

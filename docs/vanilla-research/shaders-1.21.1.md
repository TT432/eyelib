# 着色器系统 — 1.21.1 (NeoForge)

> 基于 `.local_ref/mc/1.21.1/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。类路径前缀为 `net/minecraft/client/renderer/`。

## 目录

1. [类位置与职责](#1-类位置与职责)
2. [与 1.20.1 的差异概览](#2-与-1201-的差异概览)
3. [Shader 加载与编译流程](#3-shader-加载与编译流程)
4. [Uniform 系统](#4-uniform-系统)
5. [Sampler 绑定](#5-sampler-绑定)
6. [Shader 注册表](#6-shader-注册表)
7. [apply/clear 机制](#7-applyclear-机制)
8. [后处理 Shader](#8-后处理-shader)
9. [关键不变量与约束](#9-关键不变量与约束)

---

## 1. 类位置与职责

| 类 | 文件(相对于 mc/1.21.1/sources/) | 职责 |
|---|---|---|
| `ShaderInstance` | `ShaderInstance.java` | 核心 shader 实例(与 1.20.1 无架构变化) |
| `Uniform` | `com/mojang/blaze3d/shaders/Uniform.java` | float/int/matrix uniform(GL 上传) |
| `AbstractUniform` | `com/mojang/blaze3d/shaders/AbstractUniform.java` | Uniform 基类 |
| `Program` | `com/mojang/blaze3d/shaders/Program.java` | 单个 vertex/fragment shader stage |
| `ProgramManager` | `com/mojang/blaze3d/shaders/ProgramManager.java` | GL program 创建/链接/释放 |
| `GameRenderer` | `GameRenderer.java` | 持有所有 shader 实例 |
| `PostChain` | `PostChain.java` | 后处理链(JSON→targets+passes) |
| `PostPass` | `PostPass.java` | 单次后处理 pass |
| `EffectInstance` | `com/mojang/blaze3d/shaders/EffectInstance.java` | Post-effect shader 包装 |
| `Shader`(接口) | `com/mojang/blaze3d/shaders/Shader.java` | 通用 shader 接口 |
| `GlslPreprocessor` | `com/mojang/blaze3d/shaders/GlslPreprocessor.java` | GLSL `#import` 预处理 |

---

## 2. 与 1.20.1 的差异概览

1.21.1 着色器系统与 1.20.1 **几乎完全一致**,主要差异为 API 名称的迁移:

| 方面 | 1.20.1 (Forge) | 1.21.1 (NeoForge) |
|---|---|---|
| ShaderInstance 构造函数 | 105 行(含注释) | 78 行(含注释,结构相同但路径解析简化) |
| ResourceLocation 构造 | `new ResourceLocation(a, b)` | `ResourceLocation.fromNamespaceAndPath(a, b)` |
| Shader import hook | `ForgeHooksClient.getShaderImportLocation()` | NeoForge 等效(通过 `IClientLoader` 或 client hook 重定向) |
| GLSL 扩展名 | `.vsh` / `.fsh` | 同左 |
| Shader JSON 路径 | `shaders/core/<name>.json` | 同左 |
| JSON 结构 | attributes/samplers/uniforms/blend | 同左 |
| Uniform 类型 | 0-10 不变 | 同左 |
| 预定义 uniform | 16 个(同 1.20.1 完全一致) | 同左 |
| GameRenderer.get*Shader | 所有方法名不变 | 同左 |
| PostChain | JSON targets/passes(不变) | 同左 |

**结论**:1.21.1 的着色器系统可以视为 1.20.1 的直接迁移。所有核心架构(ShaderInstance JSON→GLSL 编译→Program 链接→Uniform 上传→apply/clear)完全保留。唯一的差异是 Mojang 内部的 `ResourceLocation` API 升级和 NeoForge 的 import hook 实现细节。

---

## 3. Shader 加载与编译流程

与 1.20.1 完全一致。流程概览:

```
ShaderInstance(ResourceProvider, ResourceLocation, VertexFormat)
  ├─ 读取 shaders/core/<name>.json
  ├─ 解析 vertex/fragment/samplers/attributes/uniforms/blend
  ├─ Program.getOrCreate() 编译 GLSL(.vsh/.fsh)
  │     └─ GlslPreprocessor.process() 展开 #import
  ├─ ProgramManager.createProgram() 创建 GL program
  ├─ glBindAttribLocation 绑定 attribute
  ├─ ProgramManager.linkShader() 链接
  ├─ updateLocations() 查询 uniform/sampler location
  └─ 缓存预定义 uniform 引用
```

### JSON 结构(不变)

```json
{
  "vertex": "rendertype_solid",
  "fragment": "rendertype_solid",
  "samplers": [{"name": "Sampler0"}, {"name": "Sampler2"}],
  "attributes": ["Position", "Color", "UV0", "UV2"],
  "uniforms": [
    {"name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [...]},
    {"name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [...]}
  ],
  "blend": {}
}
```

---

## 4. Uniform 系统

与 1.20.1 **完全一致**。Uniform 类型常量(0-10)、内存管理(IntBuffer/FloatBuffer)、dirty 标记、上传机制均无变化。

### 预定义 Uniform(与 1.20.1 完全相同)

| 字段 | GLSL 名称 | 类型 |
|---|---|---|
| `MODEL_VIEW_MATRIX` | `ModelViewMat` | mat4 |
| `PROJECTION_MATRIX` | `ProjMat` | mat4 |
| `INVERSE_VIEW_ROTATION_MATRIX` | `IViewRotMat` | mat3 |
| `TEXTURE_MATRIX` | `TextureMat` | mat4 |
| `SCREEN_SIZE` | `ScreenSize` | vec2 |
| `COLOR_MODULATOR` | `ColorModulator` | vec4 |
| `LIGHT0_DIRECTION` | `Light0_Direction` | vec3 |
| `LIGHT1_DIRECTION` | `Light1_Direction` | vec3 |
| `GLINT_ALPHA` | `GlintAlpha` | float |
| `FOG_START` | `FogStart` | float |
| `FOG_END` | `FogEnd` | float |
| `FOG_COLOR` | `FogColor` | vec4 |
| `FOG_SHAPE` | `FogShape` | int |
| `LINE_WIDTH` | `LineWidth` | float |
| `GAME_TIME` | `GameTime` | float |
| `CHUNK_OFFSET` | `ChunkOffset` | vec3 |

---

## 5. Sampler 绑定

与 1.20.1 完全一致。Sampler0(主纹理)/Sampler1(叠加)/Sampler2(Lightmap)语义不变。纹理单元分配:`GL_TEXTURE0 + sampler_index`。

---

## 6. Shader 注册表

GameRenderer 的 `get*Shader()` 方法清单与 1.20.1 **完全相同**,按类别分组:

### 基础 Position 系列
- `getPositionShader()`
- `getPositionColorShader()`
- `getPositionTexShader()`
- `getPositionColorTexShader()`
- `getPositionColorLightmapShader()`
- `getPositionColorTexLightmapShader()`

### 辅助系列
- `getPositionTexColorShader()`
- `getPositionTexColorNormalShader()`
- `getPositionTexLightmapColorShader()`

### Block/Terrain 系列
- `getRendertypeSolidShader()`
- `getRendertypeCutoutMippedShader()`
- `getRendertypeCutoutShader()`
- `getRendertypeTranslucentShader()`
- `getRendertypeTranslucentMovingBlockShader()`
- `getRendertypeTranslucentNoCrumblingShader()`
- `getRendertypeTripwireShader()`

### Entity 系列
- `getRendertypeEntitySolidShader()`
- `getRendertypeEntityCutoutShader()`
- `getRendertypeEntityCutoutNoCullShader()`
- `getRendertypeEntityCutoutNoCullZOffsetShader()`
- `getRendertypeEntityTranslucentShader()`
- `getRendertypeEntityTranslucentCullShader()`
- `getRendertypeItemEntityTranslucentCullShader()`
- `getRendertypeEntityTranslucentEmissiveShader()`
- `getRendertypeEntitySmoothCutoutShader()`
- `getRendertypeEntityNoOutlineShader()`
- `getRendertypeEntityShadowShader()`
- `getRendertypeEntityAlphaShader()`
- `getRendertypeEntityDecalShader()`
- `getRendertypeArmorCutoutNoCullShader()`

### 特效系列
- `getRendertypeBeaconBeamShader()`
- `getRendertypeEyesShader()`
- `getRendertypeEnergySwirlShader()`
- `getRendertypeLeashShader()`
- `getRendertypeWaterMaskShader()`
- `getRendertypeOutlineShader()`
- `getRendertypeCrumblingShader()`
- `getRendertypeEndPortalShader()`
- `getRendertypeEndGatewayShader()`
- `getRendertypeLinesShader()`
- `getRendertypeLightningShader()`

### Text 系列
- `getRendertypeTextShader()`
- `getRendertypeTextBackgroundShader()`
- `getRendertypeTextIntensityShader()`
- `getRendertypeTextSeeThroughShader()`
- `getRendertypeTextBackgroundSeeThroughShader()`
- `getRendertypeTextIntensitySeeThroughShader()`

### GUI 系列
- `getRendertypeGuiShader()`
- `getRendertypeGuiOverlayShader()`
- `getRendertypeGuiTextHighlightShader()`
- `getRendertypeGuiGhostRecipeOverlayShader()`

### Glint 系列
- `getRendertypeGlintShader()`
- `getRendertypeGlintDirectShader()`
- `getRendertypeGlintTranslucentShader()`
- `getRendertypeArmorGlintShader()`
- `getRendertypeArmorEntityGlintShader()`
- `getRendertypeEntityGlintShader()`
- `getRendertypeEntityGlintDirectShader()`

---

## 7. apply/clear 机制

与 1.20.1 完全一致。`lastAppliedShader`/`lastProgramId` 静态变量跟踪,避免冗余 `glUseProgram` 调用。`apply()` 按序执行:blend → glUseProgram → sampler bind → uniform upload。`clear()` 解绑并恢复状态。

---

## 8. 后处理 Shader

与 1.20.1 完全一致。PostChain(JSON targets+passes)→PostPass(EffectInstance+RenderTarget),时间 uniform 循环 0-20 秒。

---

## 9. 关键不变量与约束

与 1.20.1 **完全一致**:

1. 渲染线程要求(assertOnRenderThread)
2. Program 缓存(同路径复用)
3. Uniform dirty 标记与惰性上传
4. BlendMode 默认值(func=GL_FUNC_ADD)
5. Sampler 纹理单元映射(GL_TEXTURE0 + index)
6. Uniform native 内存管理(close() 释放)
7. PostChain 时间循环(0-20s)

### 1.21.1 特有注意事项

- `ResourceLocation.fromNamespaceAndPath()` 替代 `new ResourceLocation()`,仅影响构造调用方式,不影响语义
- NeoForge 的 GLSL import hook 机制与 Forge 略有不同,但对外行为一致
- ShaderInstance 构造函数代码行数减少(78 vs 105),主要因为路径解析逻辑简化(使用 `ResourceLocation.fromNamespaceAndPath` 一步完成)

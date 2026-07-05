# 着色器系统 — 1.20.1 (Forge)

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。类路径前缀为 `net/minecraft/client/renderer/`。

## 目录

1. [类位置与职责](#1-类位置与职责)
2. [Shader 加载与编译流程](#2-shader-加载与编译流程)
3. [Uniform 系统](#3-uniform-系统)
4. [Sampler 绑定](#4-sampler-绑定)
5. [Shader 注册表](#5-shader-注册表)
6. [apply/clear 机制](#6-applyclear-机制)
7. [后处理 Shader](#7-后处理-shader)
8. [关键不变量与约束](#8-关键不变量与约束)

---

## 1. 类位置与职责

| 类 | 文件 | 职责 |
|---|---|---|
| `ShaderInstance` | `ShaderInstance.java` | 核心 shader 实例,持有 GL program、所有 uniform/sampler、blend mode,提供 `apply()`/`clear()` |
| `Uniform` | `com/mojang/blaze3d/shaders/Uniform.java` | float/int/matrix uniform,GL 上传 |
| `AbstractUniform` | `com/mojang/blaze3d/shaders/AbstractUniform.java` | Uniform 基类(DUMMY_UNIFORM 用) |
| `Program` | `com/mojang/blaze3d/shaders/Program.java` | 单个 vertex/fragment shader stage (GLSL 编译) |
| `ProgramManager` | `com/mojang/blaze3d/shaders/ProgramManager.java` | GL program 创建/链接/释放 |
| `GameRenderer` | `GameRenderer.java` | 持有所有 shader 实例为字段,提供 `get*Shader()` 方法供 `RenderStateShard` 引用 |
| `PostChain` | `PostChain.java` | 后处理链:JSON 描述→targets + passes |
| `PostPass` | `PostPass.java` | 单次后处理 pass:持有 `EffectInstance` |
| `EffectInstance` | `com/mojang/blaze3d/shaders/EffectInstance.java` | Post-effect 的 shader 包装,管理 uniform/sampler/aux targets |
| `Shader`(接口) | `com/mojang/blaze3d/shaders/Shader.java` | 通用 shader 接口:`getId()`/`markDirty()`/`attachToProgram()` |
| `GlslPreprocessor` | `com/mojang/blaze3d/shaders/GlslPreprocessor.java` | GLSL `#import` 扩展预处理(把 include 行替换为实际源) |

---

## 2. Shader 加载与编译流程

### 2.1 JSON 描述格式

Shader 定义存储在 `assets/<namespace>/shaders/core/<name>.json`。JSON 结构:

```json
{
  "vertex":   "rendertype_entity",
  "fragment": "rendertype_entity",
  "samplers":  [{"name": "Sampler0"}, {"name": "Sampler1"}, {"name": "Sampler2"}],
  "attributes": ["Position", "Color", "UV0", "UV1", "UV2", "Normal"],
  "uniforms": [
    {"name": "ModelViewMat",  "type": "matrix4x4", "count": 16, "values": [1.0, ...]},
    {"name": "ProjMat",       "type": "matrix4x4", "count": 16, "values": [1.0, ...]},
    {"name": "ColorModulator","type": "float",      "count": 4,  "values": [1.0, 1.0, 1.0, 1.0]},
    {"name": "FogStart",      "type": "float",      "count": 1,  "values": [0.0]}
  ],
  "blend": {"func": "add", "srcrgb": "src_alpha", "dstrgb": "one_minus_src_alpha"}
}
```

**字段说明**:

- `vertex`/`fragment`:GLSL 文件名(不含 `.vsh`/`.fsh` 扩展名),目录 `shaders/core/`
- `samplers`:每个 sampler 由 `name` 标识;若有 `file` 可直接绑定纹理文件
- `attributes`:VAO attribute 名称列表,顺序与 VertexFormat 的 `getElementAttributeNames()` 对应
- `uniforms`:预定义 uniform,支持类型 `int`/`float`/`matrix2x2`/`matrix3x3`/`matrix4x4`,count 为分量数
- `blend`(可选):混合模式,省略则使用默认(不混合)

### 2.2 编译流程 (ShaderInstance 构造函数,`ShaderInstance.java:89-180`)

```
ShaderInstance(ResourceProvider, ResourceLocation, VertexFormat)
  │
  ├─ 1. 读取 JSON ← shaders/core/<name>.json
  │     ├─ 解析 "vertex" → vertex shader 路径
  │     ├─ 解析 "fragment" → fragment shader 路径
  │     ├─ 解析 "samplers" → parseSamplerNode() (存入 samplerMap/samplerNames)
  │     ├─ 解析 "attributes" → attributeNames 列表
  │     ├─ 解析 "uniforms" → parseUniformNode() (创建 Uniform 对象)
  │     └─ 解析 "blend" → BlendMode
  │
  ├─ 2. 编译 GLSL
  │     Program.getOrCreate() → compileShaderInternal()
  │       ├─ 读取 .vsh/.fsh 文件
  │       ├─ GlslPreprocessor.process() 展开 #import
  │       ├─ GL: glCreateShader → glShaderSource → glCompileShader
  │       └─ 检查编译日志 → 成功返回 shader ID,失败抛 IOException
  │     (已编译的 program 缓存在 Program.Type 的静态 Map<String,Program> 中)
  │
  ├─ 3. 创建 GL program
  │     ProgramManager.createProgram() → GL: glCreateProgram
  │
  ├─ 4. 绑定 attribute location
  │     用 glBindAttribLocation 将 VertexFormat 的 elementAttributeNames 按序绑定
  │
  ├─ 5. 链接 program
  │     ProgramManager.linkShader()
  │       ├─ attachToProgram() → glAttachShader(vertex+fragment)
  │       ├─ glLinkProgram
  │       └─ 检查链接状态 (GL_LINK_STATUS=35714)
  │
  ├─ 6. updateLocations()
  │     ├─ 用 glGetUniformLocation 查询每个 sampler/uniform 的 location
  │     └─ uniform.setLocation() 设置, uniformMap 建立 name→Uniform 映射
  │
  └─ 7. 缓存预定义 uniform 引用
        MODEL_VIEW_MATRIX = getUniform("ModelViewMat")
        PROJECTION_MATRIX  = getUniform("ProjMat")
        ...
```

### 2.3 GLSL 源与 #import

- Vertex shader 扩展名:`.vsh`
- Fragment shader 扩展名:`.fsh`
- 两者均位于 `shaders/core/` 目录
- `#import <path>` 被 `GlslPreprocessor` 预处理:从 `shaders/include/<path>` 或相对当前文件路径展开
- 导入有去重机制:同一路径只展开一次
- Forge 扩展:`ForgeHooksClient.getShaderImportLocation()` 允许 mod 覆盖 import 目标

### 2.4 重新加载

`GameRenderer.reloadShaders(ResourceManager)` 遍历所有 shader 字段,调用 `ShaderInstance.close()` 后重新创建。触发时机:资源重载(F3+T)、视频设置变更。

---

## 3. Uniform 系统

### 3.1 Uniform 类型定义 (`Uniform.java:20-30`)

| 常量 | 值 | GLSL 类型 | GL API |
|---|---|---|---|
| `UT_INT1` | 0 | int | glUniform1i |
| `UT_INT2` | 1 | ivec2 | glUniform2i |
| `UT_INT3` | 2 | ivec3 | glUniform3i |
| `UT_INT4` | 3 | ivec4 | glUniform4i |
| `UT_FLOAT1` | 4 | float | glUniform1f |
| `UT_FLOAT2` | 5 | vec2 | glUniform2f |
| `UT_FLOAT3` | 6 | vec3 | glUniform3f |
| `UT_FLOAT4` | 7 | vec4 | glUniform4f |
| `UT_MAT2` | 8 | mat2 | glUniformMatrix2fv |
| `UT_MAT3` | 9 | mat3 | glUniformMatrix3fv |
| `UT_MAT4` | 10 | mat4 | glUniformMatrix4fv |

`getTypeFromString()` 将 JSON 中的 `"int"`→0, `"float"`→4, `"matrix2x2"`→8, `"matrix3x3"`→9, `"matrix4x4"`→10。

### 3.2 Uniform 内存管理

- int 类型:使用 `IntBuffer` (通过 `MemoryUtil.memAllocInt` 分配)
- float/matrix 类型:使用 `FloatBuffer` (通过 `MemoryUtil.memAllocFloat` 分配)
- dirty 标记:修改值后调用 `markDirty()`,同时通知 parent `Shader.markDirty()`
- `close()` 释放 native memory

### 3.3 Uniform 上传 (`Uniform.upload()`)

```
uniform.upload()
  │
  ├─ dirty=false? → 跳过(上次上传后未修改)
  │
  ├─ type 0-3 → uploadAsInteger() → glUniform1i/2i/3i/4i
  ├─ type 4-7 → uploadAsFloat()   → glUniform1f/2f/3f/4f
  └─ type 8-10→ uploadAsMatrix()  → glUniformMatrix2fv/3fv/4fv (transpose=false)
```

### 3.4 预定义 Uniform 清单

在 `ShaderInstance` 构造函数末尾,按名称从 JSON uniform 中查找并缓存为字段:

| 字段 | GLSL 名称 | 类型 | 说明 |
|---|---|---|---|
| `MODEL_VIEW_MATRIX` | `ModelViewMat` | mat4 | 模型视图矩阵 |
| `PROJECTION_MATRIX` | `ProjMat` | mat4 | 投影矩阵 |
| `INVERSE_VIEW_ROTATION_MATRIX` | `IViewRotMat` | mat3 | 逆视图旋转矩阵(3x3) |
| `TEXTURE_MATRIX` | `TextureMat` | mat4 | 纹理矩阵(动画/UV变换) |
| `SCREEN_SIZE` | `ScreenSize` | vec2 | 屏幕尺寸(宽,高) |
| `COLOR_MODULATOR` | `ColorModulator` | vec4 | 颜色调制(r,g,b,a) |
| `LIGHT0_DIRECTION` | `Light0_Direction` | vec3 | 光照方向0(天空光) |
| `LIGHT1_DIRECTION` | `Light1_Direction` | vec3 | 光照方向1(块光) |
| `GLINT_ALPHA` | `GlintAlpha` | float | 附魔光效 alpha |
| `FOG_START` | `FogStart` | float | 雾近平面距离 |
| `FOG_END` | `FogEnd` | float | 雾远平面距离 |
| `FOG_COLOR` | `FogColor` | vec4 | 雾颜色 |
| `FOG_SHAPE` | `FogShape` | int | 雾形状(0=线性,1=指数) |
| `LINE_WIDTH` | `LineWidth` | float | 线宽(用于线条渲染) |
| `GAME_TIME` | `GameTime` | float | 游戏时间(tick 计数) |
| `CHUNK_OFFSET` | `ChunkOffset` | vec3 | chunk 渲染偏移 |

> 注意:这些字段可能为 `null`(若 shader 未声明对应 uniform)。调用方通过 `safeGetUniform()` 获取(不存在时返回 DUMMY_UNIFORM)。

### 3.5 Uniform 设置流程

渲染代码设置 uniform 的典型模式(以 `GameRenderer` 为例):

```java
// 1. 获取 shader
ShaderInstance shader = gameRenderer.getRendertypeEntitySolidShader();
// 2. 设置 uniform 值
shader.safeGetUniform("ModelViewMat").set(matrix);
shader.safeGetUniform("ProjMat").set(matrix4f);
shader.COLOR_MODULATOR.set(r, g, b, a);
shader.GAME_TIME.set(tickCount);
// 3. apply() 触发 upload
shader.apply();
```

---

## 4. Sampler 绑定

### 4.1 Sampler 语义

| Sampler | 典型纹理 | 说明 |
|---|---|---|
| `Sampler0` | 主纹理(diffuse/atlas) | 方块/实体/物品的 diffuse 颜色纹理 |
| `Sampler1` | 叠加纹理(overlay) | 实体的 secondary 纹理(如羊毛颜色) |
| `Sampler2` | Lightmap 纹理 | 16×16 光照查找表 |

### 4.2 Sampler 配置 (JSON)

```json
"samplers": [
  {"name": "Sampler0"},   // 仅声明,运行时动态绑定
  {"name": "Sampler1"},
  {"name": "Sampler2"}
]
```

若 JSON 中 sampler 有 `"file"` 字段,则绑定静态纹理路径(如后处理 pass)。

### 4.3 apply() 中的 Sampler 绑定 (`ShaderInstance.java:300-331`)

```
apply()
  │
  ├─ glUseProgram(programId)
  │
  ├─ 遍历 samplerLocations:
  │     ├─ glGetUniformLocation → 获取 location
  │     ├─ uploadInteger(location, j) → glUniform1i(sampler_location, texture_unit)
  │     ├─ 根据 samplerMap 中的对象类型:
  │     │   ├─ RenderTarget → getColorTextureId() (FBO 颜色附件)
  │     │   ├─ AbstractTexture → getId() (普通纹理)
  │     │   └─ Integer → 直接使用(纹理 ID)
  │     └─ bindTexture(l) 或 activeTexture + bindTexture
  │
  └─ 上传所有 uniform
```

纹理单元的分配方式:`j` 是 sampler 索引(0-based),GL 纹理单元 = `GL_TEXTURE0 + j` (即 0x84C0 + j)。

### 4.4 clear() 中的 Sampler 解绑 (`ShaderInstance.java:285-297`)

clear() 遍历所有 sampler,若有绑定值则 `_activeTexture(0x84C0 + j)` + `_bindTexture(0)` 解绑。

---

## 5. Shader 注册表

### 5.1 GameRenderer Shader 字段与方法

`GameRenderer` 持有所有 shader 实例为私有字段,通过 `get*Shader()` 方法暴露给 `RenderStateShard.ShaderStateShard`。

#### 基础 Position 系列

| 常量名 | GameRenderer 方法 | 说明 |
|---|---|---|
| `POSITION_SHADER` | `getPositionShader()` | 仅 Position attribute |
| `POSITION_COLOR_SHADER` | `getPositionColorShader()` | Position + Color |
| `POSITION_TEX_SHADER` | `getPositionTexShader()` | Position + TexCoord |
| `POSITION_COLOR_TEX_SHADER` | `getPositionColorTexShader()` | Position + Color + TexCoord |
| `POSITION_COLOR_LIGHTMAP_SHADER` | `getPositionColorLightmapShader()` | Position + Color + Lightmap |
| `POSITION_COLOR_TEX_LIGHTMAP_SHADER` | `getPositionColorTexLightmapShader()` | Position + Color + TexCoord + Lightmap |

#### 辅助 Position 系列 (非 RenderStateShard 直接引用)

| 方法 | 说明 |
|---|---|
| `getPositionTexColorShader()` | Position + TexCoord + Color |
| `getPositionTexColorNormalShader()` | Position + TexCoord + Color + Normal |
| `getPositionTexLightmapColorShader()` | Position + TexCoord + Lightmap + Color |

#### Block/Terrain 系列

| 常量名 | GameRenderer 方法 | 说明 |
|---|---|---|
| `RENDERTYPE_SOLID_SHADER` | `getRendertypeSolidShader()` | 方块不透明渲染 |
| `RENDERTYPE_CUTOUT_MIPPED_SHADER` | `getRendertypeCutoutMippedShader()` | 方块 cutout mipmapped |
| `RENDERTYPE_CUTOUT_SHADER` | `getRendertypeCutoutShader()` | 方块 cutout (alpha test) |
| `RENDERTYPE_TRANSLUCENT_SHADER` | `getRendertypeTranslucentShader()` | 方块半透明 |
| `RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_SHADER` | `getRendertypeTranslucentMovingBlockShader()` | 移动方块半透明 |
| `RENDERTYPE_TRANSLUCENT_NO_CRUMBLING_SHADER` | `getRendertypeTranslucentNoCrumblingShader()` | 半透明无破坏效果 |

#### Entity 系列

| 常量名 | GameRenderer 方法 | 说明 |
|---|---|---|
| `RENDERTYPE_ENTITY_SOLID_SHADER` | `getRendertypeEntitySolidShader()` | 实体不透明 |
| `RENDERTYPE_ENTITY_CUTOUT_SHADER` | `getRendertypeEntityCutoutShader()` | 实体 cutout |
| `RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER` | `getRendertypeEntityCutoutNoCullShader()` | 实体 cutout 无剔除 |
| `RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER` | `getRendertypeEntityCutoutNoCullZOffsetShader()` | 实体 cutout 无剔除+Z偏移 |
| `RENDERTYPE_ENTITY_TRANSLUCENT_SHADER` | `getRendertypeEntityTranslucentShader()` | 实体半透明 |
| `RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER` | `getRendertypeEntityTranslucentCullShader()` | 实体半透明带剔除 |
| `RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER` | `getRendertypeEntityTranslucentEmissiveShader()` | 实体半透明自发光 |
| `RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER` | `getRendertypeEntitySmoothCutoutShader()` | 实体平滑 cutout |
| `RENDERTYPE_ENTITY_NO_OUTLINE_SHADER` | `getRendertypeEntityNoOutlineShader()` | 实体无轮廓(无叠加) |
| `RENDERTYPE_ENTITY_SHADOW_SHADER` | `getRendertypeEntityShadowShader()` | 实体阴影 |
| `RENDERTYPE_ENTITY_ALPHA_SHADER` | `getRendertypeEntityAlphaShader()` | 实体 alpha |
| `RENDERTYPE_ENTITY_DECAL_SHADER` | `getRendertypeEntityDecalShader()` | 实体贴花 |
| `RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER` | `getRendertypeItemEntityTranslucentCullShader()` | 物品实体半透明带剔除 |

#### 特效系列

| 常量名 | GameRenderer 方法 | 说明 |
|---|---|---|
| `RENDERTYPE_BEACON_BEAM_SHADER` | `getRendertypeBeaconBeamShader()` | 信标光束 |
| `RENDERTYPE_EYES_SHADER` | `getRendertypeEyesShader()` | 末影人/蜘蛛眼睛(自发光) |
| `RENDERTYPE_ENERGY_SWIRL_SHADER` | `getRendertypeEnergySwirlShader()` | 充能漩涡 |
| `RENDERTYPE_LEASH_SHADER` | `getRendertypeLeashShader()` | 拴绳 |
| `RENDERTYPE_WATER_MASK_SHADER` | `getRendertypeWaterMaskShader()` | 水面遮罩 |
| `RENDERTYPE_OUTLINE_SHADER` | `getRendertypeOutlineShader()` | 轮廓 |
| `RENDERTYPE_CRUMBLING_SHADER` | `getRendertypeCrumblingShader()` | 方块破坏裂痕 |
| `RENDERTYPE_END_PORTAL_SHADER` | `getRendertypeEndPortalShader()` | 末地传送门 |
| `RENDERTYPE_END_GATEWAY_SHADER` | `getRendertypeEndGatewayShader()` | 末地折跃门 |
| `RENDERTYPE_LINES_SHADER` | `getRendertypeLinesShader()` | 线框 |
| `RENDERTYPE_LIGHTNING_SHADER` | `getRendertypeLightningShader()` | 闪电 |
| `RENDERTYPE_TRIPWIRE_SHADER` | `getRendertypeTripwireShader()` | 绊线 |

#### Text 系列

| 常量名 | GameRenderer 方法 | 说明 |
|---|---|---|
| `RENDERTYPE_TEXT_SHADER` | `getRendertypeTextShader()` | 文字(含 see-through 变体) |
| `RENDERTYPE_TEXT_BACKGROUND_SHADER` | `getRendertypeTextBackgroundShader()` | 文字背景 |
| `RENDERTYPE_TEXT_INTENSITY_SHADER` | `getRendertypeTextIntensityShader()` | 文字强度 |
| `RENDERTYPE_TEXT_SEE_THROUGH_SHADER` | `getRendertypeTextSeeThroughShader()` | 透视文字 |
| `RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH_SHADER` | `getRendertypeTextBackgroundSeeThroughShader()` | 透视文字背景 |
| `RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_SHADER` | `getRendertypeTextIntensitySeeThroughShader()` | 透视文字强度 |

#### GUI 系列

| 常量名 | GameRenderer 方法 | 说明 |
|---|---|---|
| `RENDERTYPE_GUI_SHADER` | `getRendertypeGuiShader()` | GUI 基础 |
| `RENDERTYPE_GUI_OVERLAY_SHADER` | `getRendertypeGuiOverlayShader()` | GUI 叠加 |
| `RENDERTYPE_GUI_TEXT_HIGHLIGHT_SHADER` | `getRendertypeGuiTextHighlightShader()` | GUI 文本高亮 |
| `RENDERTYPE_GUI_GHOST_RECIPE_OVERLAY_SHADER` | `getRendertypeGuiGhostRecipeOverlayShader()` | 配方透明叠加 |

#### Glint 系列(附魔/光效)

| 常量名 | GameRenderer 方法 | 说明 |
|---|---|---|
| `RENDERTYPE_GLINT_SHADER` | `getRendertypeGlintShader()` | 通用附魔光效 |
| `RENDERTYPE_GLINT_DIRECT_SHADER` | `getRendertypeGlintDirectShader()` | 直接附魔光效 |
| `RENDERTYPE_GLINT_TRANSLUCENT_SHADER` | `getRendertypeGlintTranslucentShader()` | 半透明附魔光效 |
| `RENDERTYPE_ARMOR_GLINT_SHADER` | `getRendertypeArmorGlintShader()` | 盔甲附魔光效 |
| `RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER` | `getRendertypeArmorEntityGlintShader()` | 盔甲实体附魔光效 |
| `RENDERTYPE_ENTITY_GLINT_SHADER` | `getRendertypeEntityGlintShader()` | 实体附魔光效 |
| `RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER` | `getRendertypeEntityGlintDirectShader()` | 实体直接附魔光效 |

### 5.2 Shader JSON 文件位置

所有 shader JSON 描述文件位于资源包中的 `shaders/core/` 目录。对应的 GLSL 源为同目录下的 `.vsh`(vertex)和 `.fsh`(fragment)文件。

---

## 6. apply/clear 机制

### 6.1 Shader 状态管理

`ShaderInstance` 维护两个静态变量跟踪当前应用的 shader:

```java
private static ShaderInstance lastAppliedShader;
private static int lastProgramId = -1;
```

### 6.2 apply() 流程

```
ShaderInstance.apply()
  │
  ├─ 断言当前在渲染线程 (RenderSystem.assertOnRenderThread())
  ├─ dirty = false
  ├─ lastAppliedShader = this
  ├─ blend.apply() — 设置 GL 混合模式
  ├─ 若 programId != lastProgramId:
  │     glUseProgram(this.programId) → 切换 shader program
  │     lastProgramId = this.programId
  ├─ 遍历 samplerLocations:
  │     绑定各 sampler 到对应纹理单元
  └─ 遍历 uniforms: uniform.upload() → 上传 dirty uniform
```

### 6.3 clear() 流程

```
ShaderInstance.clear()
  │
  ├─ glUseProgram(0) — 解绑 shader (回到 fixed-function)
  ├─ lastProgramId = -1
  ├─ lastAppliedShader = null
  ├─ 遍历 samplerLocations:
  │     若有绑定,activeTexture + bindTexture(0) 解绑
  └─ 恢复之前的 active texture
```

### 6.4 Blend Mode (`ShaderInstance.java:parseBlendNode()`)

JSON `blend` 节点支持配置:
- `func`:"add"(默认,GL_FUNC_ADD) / "subtract" / "reverse_subtract"
- `srcrgb`:"one"(1/GL_ONE) / "src_alpha"(GL_SRC_ALPHA) / "one_minus_src_alpha" 等
- `dstrgb`:"zero"(0/GL_ZERO) / "one_minus_src_alpha" 等
- `srcalpha`(可选):alpha 通道的源因子
- `dstalpha`(可选):alpha 通道的目标因子

---

## 7. 后处理 Shader

### 7.1 PostChain (`PostChain.java`)

后处理链由 JSON 描述,位于 `assets/<namespace>/shaders/post/<name>.json`。

结构:
```json
{
  "targets": ["<name>"],
  "passes": [
    {
      "name": "pass_name",
      "intarget": "minecraft:main",
      "outtarget": "tmp",
      "auxtargets": [
        {"name": "SamplerName", "id": "texture_id", "width": 256, "height": 256, "bilinear": true}
      ],
      "uniforms": [
        {"name": "Time", "values": [0.0]}
      ]
    }
  ]
}
```

**PostChain 工作流**:

1. **Targets**:先解析 `targets` 数组,为每个 target 创建 `RenderTarget` (FBO)
2. **Passes**:每个 pass 创建 `PostPass`,指定输入/输出 target
3. **Aux Targets**:每个 pass 可绑定额外纹理(depth texture 或外部 .png 纹理)
4. **Uniforms**:每个 pass 可设置 `EffectInstance` 中的 uniform 值
5. **process(float partialTicks)**:按顺序执行所有 pass,时间 uniform 基于 `partialTicks`

### 7.2 PostPass 与 EffectInstance

`PostPass` 持有:
- `EffectInstance`(继承自 ShaderInstance 行为):管理后处理 shader 的 uniform/sampler
- 输入/输出 `RenderTarget`
- Aux asset 映射(名称→纹理 ID 提供者)

`PostPass.process(float time)`:
1. 设置正交投影矩阵
2. 设置时间 uniform
3. 绑定输入 target 的纹理为 sampler
4. push/pop 屏幕区域的投影
5. 绘制全屏四边形

### 7.3 原版后处理效果

- `blur`:模糊
- `deconverge_0`:3D 立体左右分离
- `disolve`:溶解
- `invert`:反色
- `sobel`:边缘检测
- `transparency`:透明通道处理

---

## 8. 关键不变量与约束

1. **渲染线程要求**:所有 shader 相关 GL 操作必须运行在渲染线程。`apply()`/`clear()`/`close()` 方法内部调用 `RenderSystem.assertOnRenderThread()`
2. **Shader 缓存**:编译过的 `Program`(GLSL 源)缓存在 `Program.Type` 静态 Map 中,同路径复用
3. **Uniform dirty 标记**:uniform 值修改后,首次 `apply()` 时才 upload 到 GL。`markDirty()` 向上传播到 `ShaderInstance`
4. **Program ID 去重**:`apply()` 只在 `programId != lastProgramId` 时调用 `glUseProgram`,避免冗余 GL 调用
5. **BlendMode 默认值**:无 JSON `blend` 节点时使用 `new BlendMode()`(即 `func=GL_FUNC_ADD, srcFactor=GL_ONE, dstFactor=GL_ZERO` — 默认无混合)
6. **Sampler 纹理单元**:sampler 索引直接映射到 GL 纹理单元(`GL_TEXTURE0 + index`),最多支持 `samplerLocations.size()` 个
7. **Uniform 内存**:`Uniform` 对象使用 native `IntBuffer`/`FloatBuffer`,必须在不再使用时调用 `close()` 释放
8. **PostChain 时间**:时间 uniform 以 0-20 秒循环 (`time / 20.0F`),溢出后减去 20 重置

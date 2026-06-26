# Cross-Version Lighting Differences (1.20.1 / 1.21.1 / 26.1.2)

> 三个版本光照模型的横向对比。详细单版分析见 `lighting-1.20.1.md` / `lighting-1.21.1.md` / `lighting-26.1.2.md`。
> 本文档聚焦于**对 eyelib 适配有影响的差异**,作为 Task 3(修复项目光照)的依据。

## 目录
1. [类/包位置变化](#1-类包位置变化)
2. [打包格式](#2-打包格式)
3. [常量与 FULL_BRIGHT](#3-常量与-full_bright)
4. [Lightmap 生成机制](#4-lightmap-生成机制)
5. [RenderType 系统](#5-rendertype-系统)
6. [顶点格式](#6-顶点格式)
7. [Shader 接口](#7-shader-接口)
8. [维度行为](#8-维度行为)
9. [对 eyelib 的影响清单](#9-对-eyelib-的影响清单)

---

## 1. 类/包位置变化

| 职责 | 1.20.1 (Forge) | 1.21.1 (NeoForge) | 26.1.2 (NeoForge) |
|---|---|---|---|
| 光照打包/常量 | `net.minecraft.client.renderer.LightTexture` | 同左 | **已删除**,拆为 `net.minecraft.util.LightCoordsUtil`(打包/常量) + `net.minecraft.util.Brightness`(record) |
| Lightmap 纹理管理 | `LightTexture` (CPU 写 NativeImage) | 同左 | `net.minecraft.client.renderer.Lightmap`(GPU 纹理+UBO) |
| Lightmap 状态提取 | (内联于 LightTexture) | 同左 | `net.minecraft.client.renderer.LightmapRenderStateExtractor` |
| Lightmap 状态持有 | (内联) | 同左 | `net.minecraft.client.renderer.state.LightmapRenderState` |
| UI lightmap | (复用 world lightmap) | 同左 | `net.minecraft.client.renderer.UiLightmap` (1×1 白) |
| RenderType | `net.minecraft.client.renderer.RenderType` | 同左 | `net.minecraft.client.renderer.rendertype.RenderType` (移包) + `RenderTypes`(工厂) |
| LevelRenderer.getLightColor | 返回 `int` | 同左 | 重命名为 `LevelRenderer.getLightCoords`,返回 `int` |
| EntityRenderer.getPackedLightCoords | 返回 `int` (调用 `LightTexture.pack`) | 同左 | 返回 `int` (调用 `LightCoordsUtil.pack`),结果存入 `EntityRenderState.lightCoords` |
| `Brightness` record | `net.minecraft.util.Brightness` (block, sky) | 同左 | 同左(但 `pack()` 内部改调 `LightCoordsUtil.pack`) |
| `getBrightness(DimensionType, int)` | `LightTexture.getBrightness` | 同左 | `Lightmap.getBrightness` (静态,仅用于 shadow) |

**1.20.1 → 1.21.1**:几乎无 API 变化,只是 Forge → NeoForge 的包名替换 + `getDarknessGamma` 改用 `getBlendFactor` 直接调用。

**1.21.1 → 26.1.2**:**重大重构**。LightTexture 类被删除,职责拆分为 4 个新类。Gamma/Night vision/Darkness 计算从 Java 搬到 GLSL(`core/lightmap` shader),Java 侧只准备 UBO 数据。

---

## 2. 打包格式

三个版本**完全一致**:`block << 4 | sky << 20`。

| 版本 | pack 函数 | block 提取 | sky 提取 |
|---|---|---|---|
| 1.20.1 | `LightTexture.pack(b, s)` | `LightTexture.block(p) = (p & 0xFFFF) >> 4` | `LightTexture.sky(p) = p >> 20 & 0xFFFF` |
| 1.21.1 | `LightTexture.pack(b, s)` | 同上 | 同上 |
| 26.1.2 | `LightCoordsUtil.pack(b, s)` | `LightCoordsUtil.block(p) = p >> 4 & 15` | `LightCoordsUtil.sky(p) = p >> 20 & 15` |

注意 26.1.2 的 `block`/`sky` 提取 mask 不同(用 `& 15` 而非 `& 0xFFFF`),但因为 `block` 最大 15、`sky` 最大 15,结果等价。

26.1.2 新增 `smoothPack`/`smoothBlock`/`smoothSky`(8-bit per channel,用于 smooth lighting 内部计算,与 eyelib 关系不大)。

---

## 3. 常量与 FULL_BRIGHT

三个版本数值完全相同:`FULL_BRIGHT = 15728880 = 0xF000F0`、`FULL_SKY = 15728640 = 0xF00000`。

| 版本 | FULL_BRIGHT 定义位置 |
|---|---|
| 1.20.1 / 1.21.1 | `LightTexture.FULL_BRIGHT`(int)+ `Brightness.FULL_BRIGHT`(record) |
| 26.1.2 | `LightCoordsUtil.FULL_BRIGHT`(int)+ `Brightness.FULL_BRIGHT`(record)。**没有** `LightTexture.FULL_BRIGHT`(类已删) |

项目 build.gradle 的 `migrate26Renames` task 已经把 `LightTexture.FULL_BRIGHT` → `0xF000F0` 字面量替换,所以数值正确,但**项目里可能仍有地方引用 `LightTexture.pack` / `LightTexture.block` / `LightTexture.sky`**,这些需要改到 `LightCoordsUtil` 或 `Brightness`。

---

## 4. Lightmap 生成机制

| 版本 | 机制 | Java 侧职责 | GPU 侧职责 |
|---|---|---|---|
| 1.20.1 | CPU 逐像素 | 16×16 双重循环,每像素算 RGB,写 NativeImage,upload | 纹理查表 |
| 1.21.1 | 同上 | 同上 | 同上 |
| 26.1.2 | **GPU 全屏三角形** | 准备 `LightmapInfo` UBO(10 字段),触发 1 次 draw call | `core/lightmap` fragment shader 计算 16×16 纹理 |

### 26.1.2 UBO 字段(`LightmapInfo`,Std140 布局)

| 字段 | 类型 | 含义 | 旧版对应 |
|---|---|---|---|
| `skyFactor` | float | 天空光强度(来自 `EnvironmentAttributes.SKY_LIGHT_FACTOR`) | 旧版 `f1 = getSkyDarken() * 0.95 + 0.05` |
| `blockFactor` | float | `blockLightFlicker + 1.4` | 旧版 `f7 = blockLightRedFlicker + 1.5` |
| `nightVisionEffectIntensity` | float | 夜视强度 | 旧版 `f5` |
| `darknessEffectScale` | float | Darkness cos 震荡 × darknessEffectScale 选项 | 旧版 `f4` |
| `bossOverlayWorldDarkening` | float | Boss 雾暗化 | 旧版 `getDarkenWorldAmount` |
| `brightness` | float | `max(0, gamma - darknessBrightnessModifier)` | 旧版 `f14 - f3` 的混合因子 |
| `blockLightTint` | vec3 | 块光色调(来自 `EnvironmentAttributes.BLOCK_LIGHT_TINT`) | 旧版硬编码 `[f9, f10, f11]` 曲线 |
| `skyLightColor` | vec3 | 天空光颜色(来自 `EnvironmentAttributes.SKY_LIGHT_COLOR`) | 旧版 `[f,f,1] lerp [1,1,1] 0.35` |
| `ambientColor` | vec3 | 环境光颜色(来自 `EnvironmentAttributes.AMBIENT_LIGHT_COLOR`) | 旧版 `dimensionType.ambientLight()` |
| `nightVisionColor` | vec3 | 夜视色调(来自 `EnvironmentAttributes.NIGHT_VISION_COLOR`) | 旧版无(夜视只提亮不调色) |

**关键变化**:26.1.2 把所有"维度差异"和"色调"从硬编码 Java 改为**数据驱动的 `EnvironmentAttributes`**,维度 JSON 可以自定义这些颜色。旧的 `forceBrightLightmap`(End)、`constantAmbientLight`(Nether)逻辑全部移到 shader 里基于 `ambientColor`/`skyFactor` 推断。

### `notGamma` 曲线去哪了?

旧版 Java:`notGamma(x) = 1 - (1-x)^4`,每像素应用。

26.1.2:该公式**移入 `core/lightmap.glsl` fragment shader**(源码未提取,但 UBO 传 `brightness` 作为混合因子,shader 内部会做等效计算)。

---

## 5. RenderType 系统

| 版本 | RenderType 本质 | 如何启用 lightmap |
|---|---|---|
| 1.20.1 | 复杂的 `create(...)` 工厂 + `CompositeState` 内部类 | `.setLightmapState(LIGHTMAP)`(LightmapStateShard 调 `turnOnLightLayer/turnOffLightLayer`) |
| 1.21.1 | 同上 | 同上 |
| 26.1.2 | **普通类** 包装 `RenderSetup`,指向 `RenderPipeline` | `RenderSetup.builder(pipeline).useLightmap()` → RenderSetup.getTextures() 把 `gameRenderer.lightmap()` 绑到 `Sampler2` |

### 包路径

| 版本 | RenderType 包 |
|---|---|
| 1.20.1 / 1.21.1 | `net.minecraft.client.renderer.RenderType` |
| 26.1.2 | `net.minecraft.client.renderer.rendertype.RenderType` + `RenderTypes`(工厂) |

项目 build.gradle 的 `migrate26Renames` 已经把 `RenderType.entity\w+(` → `RenderTypes.entity\w+(` 替换,但**静态工厂方法的命名可能不完全对应**(例如 `entityCutoutNoCull` 在 26.1.2 是否还叫这个名字,需要核对 `RenderTypes.java`)。

### LIGHTMAP vs NO_LIGHTMAP 的 RenderType(跨版本一致)

| 带 LIGHTMAP | 不带 LIGHTMAP |
|---|---|
| solid, cutout, cutout_mipped, translucent | lightning |
| entity_solid, entity_cutout, entity_cutout_no_cull | eyes |
| entity_translucent, entity_translucent_cull | entity_translucent_emissive |
| armor_cutout_no_cull | end_portal, end_gateway |
| leash, text, text_background, text_intensity | beacon_beam |
| item_cutout, item_translucent | glint, entity_glint 系列 |
| particles, weather | crumbling, water_mask |
| | lines, dragon_rays |

26.1.2 在工厂方法名上可能有细微变化(如 `ITEM_ENTITY_TRANSLUCENT_CULL` 改名 `itemTranslucent`),但**是否启用 lightmap 的语义不变**。

---

## 6. 顶点格式

三个版本**完全一致**:`UV2` 是 `VertexFormatElement(index=2, type=SHORT, count=2)`,存 packed light 的 low 16 bits(block<<4)和 high 16 bits(sky<<4)。

26.1.2 的 `DefaultVertexFormat` 改用 builder API,但 `UV2` 元素定义和语义不变。

`BLOCK` / `ENTITY`(`NEW_ENTITY` 在 26.1.2 改名 `ENTITY`)格式组成相同。

---

## 7. Shader 接口

| 版本 | Lightmap 采样方式 | 关键 GLSL 行 |
|---|---|---|
| 1.20.1 / 1.21.1 | `texelFetch(Sampler2, UV2 / 16, 0)` | `UV2` 是 `ivec2`,除以 16 得 texel 坐标,查 16×16 纹理 |
| 26.1.2 | 同上(假设 shader 模式不变,但 lightmap 纹理内容由 GPU 生成) | shader 源码未提取,但 `Sampler2` 绑定方式和 UV2 语义未变 |

26.1.2 的 lightmap 生成 shader(`core/lightmap.glsl`)不在提取的源码树里(只有 .java),但其输入(UBO)和输出(16×16 RGBA8 纹理)已知。下游消费 lightmap 的方式(`Sampler2` + `UV2/16`)与旧版一致。

---

## 8. 维度行为

| 维度 | 1.20.1/1.21.1 机制 | 26.1.2 机制 |
|---|---|---|
| Overworld | `hasSkyLight=true, ambientLight=0.0`,正常日照 | 同左;`EnvironmentAttributes` 默认值等效 |
| Nether | `hasSkyLight=false, ambientLight=0.1, constantAmbientLight=true` → sky engine 不工作,lightmap 最低亮度 0.1 | `hasSkyLight` 仍然 false;`ambientColor` 来自 `EnvironmentAttributes.AMBIENT_LIGHT_COLOR`,默认值等效于旧 `ambientLight=0.1` |
| End | `hasSkyLight=false, forceBrightLightmap=true` → lightmap 强制 lerp 到 `(0.99, 1.12, 1.0)` | `forceBrightLightmap` 逻辑移到 shader;通过 `EnvironmentAttributes` 的颜色/强度参数等效实现 |

**关键**:26.1.2 不再有 `DimensionSpecialEffects.forceBrightLightmap()` / `constantAmbientLight()` 的 Java 路径,这些逻辑全部数据驱动化。但 `DimensionType.hasSkyLight` 仍然决定 sky light engine 是否工作。

---

## 9. 对 eyelib 的影响清单

按修复优先级排序(任务3 的待办):

### 高优先级(会导致 26.1.2 编译/运行失败)

1. **`LightTexture.pack` / `block` / `sky` 引用**:在 26.1.2 编译会失败(类已删)。需改用 `LightCoordsUtil` 或 `Brightness` record。
   - 搜索:`grep -r "LightTexture\." src/`

2. **`LightTexture.FULL_BRIGHT` / `FULL_SKY` 引用**:`migrate26Renames` 已替换为 `0xF000F0` 字面量,但**项目自定义代码**可能仍引用。需核对。
   - 搜索:`grep -r "LightTexture.FULL" src/`

3. **`RenderType.entity*(...)` 静态工厂调用**:26.1.2 移到 `RenderTypes`(注意 s),且部分工厂签名可能变。`migrate26Renames` 做了正则替换,但要核对边缘情况。
   - 搜索:`grep -rE "RenderType\.(entity|armor|item|eyes|leash|text|lightning|beacon|end)" src/`

4. **`gameRenderer.lightTexture()` 调用**:26.1.2 该方法已删,改为 `gameRenderer.lightmap()`(返回 `GpuTextureView` 而非 `LightTexture` 实例)。
   - 搜索:`grep -r "lightTexture()" src/`

5. **`LevelRenderer.getLightColor` 调用**:26.1.2 改名 `getLightCoords`。
   - 搜索:`grep -r "LevelRenderer.getLightColor" src/`

### 中优先级(行为差异,不一定编译失败)

6. **`LightTexture.getBrightness(DimensionType, int)` 调用**:26.1.2 改为 `Lightmap.getBrightness(...)`,签名相同。
   - 搜索:`grep -r "LightTexture.getBrightness" src/`

7. **`turnOnLightLayer` / `turnOffLightLayer` 调用**:26.1.2 这些方法已删(lightmap 绑定由 RenderSetup 自动管理)。
   - 搜索:`grep -rE "turn(On|Off)LightLayer" src/`

8. **`DimensionSpecialEffects.forceBrightLightmap()` / `constantAmbientLight()` 覆写**:26.1.2 这两个概念数据驱动化,旧的 override 路径失效。
   - 搜索:`grep -rE "forceBrightLightmap|constantAmbientLight|adjustLightmapColors" src/`

### 低优先级(版本共性问题,可后置)

9. **项目自定义的 lightmap 计算代码**:如果 eyelib 自己实现了类 `notGamma` 或 lightmap 像素生成,需考虑 26.1.2 的 GPU 路径是否还兼容(通常 eyelib 应该依赖原版 lightmap,不应自己生成)。

10. **`EntityRenderer.getPackedLightCoords` 调用**:三个版本签名一致(返回 int),26.1.2 内部改调 `LightCoordsUtil.pack` 但外部调用方无感。但如果 eyelib 直接存了 packed light 到 `EntityRenderState`,26.1.2 的 state 字段名是 `lightCoords`。
    - 搜索:`grep -r "EntityRenderState" src/`

### 验证命令(汇总)

```
# 在项目 src/ 下跑这些 grep,定位需要修复的代码
grep -rn "LightTexture\." src/
grep -rn "lightTexture()" src/
grep -rn "LevelRenderer.getLightColor" src/
grep -rnE "RenderType\.(entity|armor|item|eyes|leash|text|lightning|beacon|end)" src/
grep -rnE "turn(On|Off)LightLayer" src/
grep -rnE "forceBrightLightmap|constantAmbientLight|adjustLightmapColors" src/
grep -rn "LightTexture.getBrightness" src/
```

修复策略建议:优先用 Stonecutter `//? if modern` 分支处理 26.1.2 差异,保持 1.20.1/1.21.1 路径用原版 API。避免在共用代码里硬编码字面量(`0xF000F0`)替换,优先用 `LightCoordsUtil.FULL_BRIGHT`(26.1.2)或 `LightTexture.FULL_BRIGHT`(旧版)的常量引用。

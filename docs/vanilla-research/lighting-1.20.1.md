# 1.20.1 (Forge) Minecraft Vanilla Lighting Model 分析报告

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [LightTexture.java 全分析](#1-lighttexturejava-全分析)
2. [Packed light coordinate system](#2-packed-light-coordinate-system)
3. [LevelRenderer.getLightColor](#3-levelrenderergetlightcolor)
4. [RenderType 与光照相关的 RenderState](#4-rendertype-与光照相关的-renderstate)
5. [EntityRenderer / MobRenderer 光照传递](#5-entityrenderer--mobrenderer-光照传递)
6. [Gamma/Tonemap 曲线](#6-gammatonemap-曲线)
7. [维度相关的行为](#7-维度相关的行为)

---

## 1. LightTexture.java 全分析

**文件**: `net/minecraft/client/renderer/LightTexture.java` (203 行)

### 1.1 常量定义 (第 21–23 行)

```java
public static final int FULL_BRIGHT = 15728880;   // 0xF000F0
public static final int FULL_SKY = 15728640;       // 0xF00000
public static final int FULL_BLOCK = 240;          // 0xF0
```

- `FULL_BRIGHT = 0xF000F0`:block=15 (0xF0)、sky=15 (0xF00000),相加得到 15728880。
- `FULL_SKY = 0xF00000`:sky=15,block=0。
- `FULL_BLOCK = 240 = 15 << 4`:仅 block 光满值 15,左移 4 位。
- 此外在 `net/minecraft/util/Brightness.java` 中有另一套 `FULL_BRIGHT`:

  ```java
  public static Brightness FULL_BRIGHT = new Brightness(15, 15);  // 第 11 行
  ```
  它是 block=15、sky=15 的 record,与 LightTexture 中的 packed int 常量是同一概念的不同表示。

### 1.2 数据结构 (第 24–29 行)

```java
private final DynamicTexture lightTexture;   // 16×16 动态纹理
private final NativeImage lightPixels;       // 像素数据
private final ResourceLocation lightTextureLocation;  // "light_map"
private boolean updateLightTexture;
private float blockLightRedFlicker;          // 火焰/熔炉光闪烁
```

Lightmap 是一个 **16×16** 的 RGBA 纹理,采样方式为 GL_LINEAR(详见 `turnOnLightLayer` 第 63–66 行)。

### 1.3 `tick()` — 块光红色闪烁 (第 52–56 行)

```java
public void tick() {
    this.blockLightRedFlicker += (float)((Math.random() - Math.random()) * Math.random() * Math.random() * 0.1D);
    this.blockLightRedFlicker *= 0.9F;
    this.updateLightTexture = true;
}
```

每 tick 引入随机扰动并指数衰减,用 `*0.9F` 趋近于 0。该值在 lightmap 计算中作为 block light 的缩放因子(`f7`,第 113 行)。

### 1.4 `updateLightTexture(float partialTicks)` — Lightmap 生成核心 (第 85–175 行)

整体流程(逐像素为 16×16 的每个 (j, i) 计算):

```
j = 列索引 = blockLight 等级 (0~15)
i = 行索引 = skyLight 等级 (0~15)
```

**步骤 1:天空暗化因子 (第 91–97 行)**

```java
float f = clientlevel.getSkyDarken(1.0F);   // [0.2, 1.0] 归一化
float f1;
if (clientlevel.getSkyFlashTime() > 0) {
    f1 = 1.0F;                               // 闪电时全亮
} else {
    f1 = f * 0.95F + 0.05F;                  // 混合,最小 0.05
}
```

`getSkyDarken` 位于 `ClientLevel.java:628`:

```java
public float getSkyDarken(float partialTick) {
    float f = this.getTimeOfDay(partialTick);
    float f1 = 1.0F - (Mth.cos(f * ((float)Math.PI * 2F)) * 2.0F + 0.2F);
    f1 = Mth.clamp(f1, 0.0F, 1.0F);
    f1 = 1.0F - f1;
    f1 *= 1.0F - this.getRainLevel(partialTick) * 5.0F / 16.0F;
    f1 *= 1.0F - this.getThunderLevel(partialTick) * 5.0F / 16.0F;
    return f1 * 0.8F + 0.2F;
}
```

返回值 `[0.2, 1.0]`,受时间、雨、雷影响。

**步骤 2:特殊效果因子 (第 99–110 行)**

- `f3` = Darkness 效果 gamma 系数
- `f4` = Darkness 余弦震荡衰减
- `f5` = 夜视/潮涌能量强度 `[0, 1]`,通过 `GameRenderer.getNightVisionScale(player, partialTicks)` 获取

夜视缩放 (`GameRenderer.java:895`):

```java
public static float getNightVisionScale(LivingEntity livingEntity, float nanoTime) {
    MobEffectInstance mobeffectinstance = livingEntity.getEffect(MobEffects.NIGHT_VISION);
    return !mobeffectinstance.endsWithin(200) ? 1.0F :
        0.7F + Mth.sin(((float)mobeffectinstance.getDuration() - nanoTime) * (float)Math.PI * 0.2F) * 0.3F;
}
```

持续时间 > 200 tick 时为 1.0,最后 200 tick 内正弦闪烁。

**步骤 3:天空颜色向量 (第 112 行)**

```java
Vector3f vector3f = (new Vector3f(f, f, 1.0F)).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
```

棕黄色 → 白色的混合,`f` 为天空暗化因子。

**步骤 4:双重循环 — 核心计算 (第 116–167 行)**

```
for i = 0..15 (sky light level)
    for j = 0..15 (block light level)
```

**4a. 基础亮度转换 (第 118–119 行)**

```java
float f8 = getBrightness(clientlevel.dimensionType(), i) * f1;  // 天空亮度因子
float f9 = getBrightness(clientlevel.dimensionType(), j) * f7;  // 块光亮度因子 * 闪烁
```

`getBrightness(dimensionType, lightLevel)` 方法 (第 186–189 行):

```java
public static float getBrightness(DimensionType dimensionType, int lightLevel) {
    float f = (float)lightLevel / 15.0F;                              // 归一化到 [0,1]
    float f1 = f / (4.0F - 3.0F * f);                                 // 自定义 S 曲线映射
    return Mth.lerp(dimensionType.ambientLight(), f1, 1.0F);          // 与 ambientLight 混合
}
```

- 公式 `f1 = f / (4 - 3f)`:在低光照区域提升亮度(块光源在低等级时比线性更亮)。
- `dimensionType.ambientLight()`:维度环境光参数(Overworld=0.0,Nether=0.1,End=0.0),通过与 1.0 做 `lerp` 来强制提升最低亮度。

```
ambientLight=0.0: getBrightness(0)=0.0
ambientLight=0.1: getBrightness(0)=0.1  (最低亮度)
ambientLight=1.0: getBrightness(n)=1.0  (全亮)
```

**4b. 块光颜色曲线 (第 120–122 行)**

```java
float f10 = f9 * ((f9 * 0.6F + 0.4F) * 0.6F + 0.4F);   // 绿色分量系数
float f11 = f9 * (f9 * f9 * 0.6F + 0.4F);               // 蓝色分量系数
vector3f1.set(f9, f10, f11);                              // RGB: 红=线性, 绿=二次, 蓝=三次
```

蓝色衰减最强,绿色次之,红色线性 → **块光偏红/橙**。

**4c. 天空光叠加 (第 128–129 行)**

```java
Vector3f vector3f2 = (new Vector3f((Vector3fc)vector3f)).mul(f8);
vector3f1.add(vector3f2);
```

天空光颜色 `vector3f`(棕黄→白)乘以天空亮度因子,再加到块光颜色上。

**4d. 小量灰化 (第 130 行)**

```java
vector3f1.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
```

4% 混合到灰色,防止颜色过度饱和。

**4e. 世界暗化 (第 131–136 行)**

```java
if (this.renderer.getDarkenWorldAmount(partialTicks) > 0.0F) {
    Vector3f vector3f3 = (new Vector3f((Vector3fc)vector3f1)).mul(0.7F, 0.6F, 0.6F);
    vector3f1.lerp(vector3f3, f12);
}
```

死亡/传送等变暗效果,向红暗偏移。

**4f. 维度特殊效果调整 (第 138 行)**

```java
clientlevel.effects().adjustLightmapColors(clientlevel, partialTicks, f, f7, f8, j, i, vector3f1);
```

各维度 Override 此方法自定义 lightmap 调色。

**4g. 夜视效果 (第 140–147 行)**

```java
if (f5 > 0.0F) {
    float f13 = Math.max(vector3f1.x(), Math.max(vector3f1.y(), vector3f1.z()));
    if (f13 < 1.0F) {
        float f15 = 1.0F / f13;
        Vector3f vector3f5 = (new Vector3f((Vector3fc)vector3f1)).mul(f15);
        vector3f1.lerp(vector3f5, f5);
    }
}
```

将最大颜色分量推到 1.0,然后按夜视强度融合 → 显著增亮。

**4h. Darkness 暗化 (第 149–155 行)**

```java
if (f4 > 0.0F) {
    vector3f1.add(-f4, -f4, -f4);
}
```

直接减去亮度值,使 lightmap 变暗。

**4i. Gamma 应用 (第 157–159 行)**

```java
float f14 = this.minecraft.options.gamma().get().floatValue();
Vector3f vector3f4 = new Vector3f(this.notGamma(vector3f1.x), this.notGamma(vector3f1.y), this.notGamma(vector3f1.z));
vector3f1.lerp(vector3f4, Math.max(0.0F, f14 - f3));
```

详见 [第 6 节 Gamma/Tonemap 曲线](#6-gammatonemap-曲线)。

**4j. 写入像素 (第 162–167 行)**

```java
vector3f1.mul(255.0F);
int k = (int)vector3f1.x();
int l = (int)vector3f1.y();
int i1 = (int)vector3f1.z();
this.lightPixels.setPixelRGBA(j, i, -16777216 | i1 << 16 | l << 8 | k);
```

注意:`setPixelRGBA(j, i, ...)` — j=列(block light), i=行(sky light)。像素格式 ARGB。

### 1.5 `pack` / `block` / `sky` — 打包/解包 (第 192–202 行)

```java
public static int pack(int blockLight, int skyLight) {
    return blockLight << 4 | skyLight << 20;
}

public static int block(int packedLight) {
    return (packedLight & 0xFFFF) >> 4;        // Forge 修复: MC-169806
}

public static int sky(int packedLight) {
    return packedLight >> 20 & '\uffff';
}
```

布局:

```
31  28  27  24  23  20  19  16  15  12  11   8   7   4   3   0
┌───────────────┬───────────────┬───────────────┬───────────────┐
│     sky (5b)  │     unused    │   block (5b)  │   unused      │
│      >> 20    │               │    >> 4       │               │
└───────────────┴───────────────┴───────────────┴───────────────┘
```

`LightTexture.block()` Forge 修复后的 Mask 为 `0xFFFF`(而非原版的 `0xFFFF`)。

### 1.6 `Brightness` record (`net/minecraft/util/Brightness.java`)

```java
public record Brightness(int block, int sky) {
    public static Brightness FULL_BRIGHT = new Brightness(15, 15);

    public int pack() {
        return this.block << 4 | this.sky << 20;
    }

    public static Brightness unpack(int packedBrightness) {
        int i = packedBrightness >> 4 & '\uffff';
        int j = packedBrightness >> 20 & '\uffff';
        return new Brightness(i, j);
    }
}
```

与 `LightTexture.pack/block/sky` 算法完全一致,使用独立类型系统。

---

## 2. Packed Light Coordinate System

### 2.1 顶点格式中的 UV2

`DefaultVertexFormat.java` (第 13 行):

```java
public static final VertexFormatElement ELEMENT_UV2 = new VertexFormatElement(2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
```

- **Index**: 2(即 UV 层 2)
- **Type**: SHORT(16-bit signed integer per component)
- **Usage**: UV
- **Count**: 2

包含它的 VertexFormat 定义:

| Format | 组成 | 用途 |
|--------|------|------|
| `BLOCK` (`:18`) | Position+Color+UV0+**UV2**+Normal+Padding | 方块渲染 |
| `NEW_ENTITY` (`:19`) | Position+Color+UV0+UV1(overlay)+**UV2**+Normal+Padding | 实体渲染 |
| `PARTICLE` (`:20`) | Position+UV0+Color+**UV2** | 粒子 |
| `POSITION_COLOR_LIGHTMAP` (`:24`) | Position+Color+**UV2** | 只带光照 |
| `POSITION_COLOR_TEX_LIGHTMAP` (`:28`) | Position+Color+UV0+**UV2** | 文字等 |
| `POSITION_TEX_LIGHTMAP_COLOR` (`:29`) | Position+UV0+**UV2**+Color | |

### 2.2 VertexConsumer.uv2 的分拆 (VertexConsumer.java:54–55)

```java
default VertexConsumer uv2(int lightmapUV) {
    return this.uv2(lightmapUV & '\uffff', lightmapUV >> 16 & '\uffff');
}
```

将 32-bit packed light 分拆为两个 **16-bit short**:
- `low short` = `lightmapUV & 0xFFFF` = block light 编码
- `high short` = `lightmapUV >> 16 & 0xFFFF` = sky light 编码

### 2.3 BufferBuilder 写入 UV2 的底层 (BufferBuilder.java:325–326)

```java
this.putShort(i + 0, (short)(lightmapUV & '\uffff'));
this.putShort(i + 2, (short)(lightmapUV >> 16 & '\uffff'));
```

### 2.4 着色器中的 Lightmap 采样

**注意**: 1.20.1 的提取源码中不包含原版 GLSL 着色器文件(assets/minecraft/shaders/core/ 下仅有 Forge 添加的 `rendertype_entity_unlit_translucent`)。但通过 Forge 提供的着色器可以确定采样模式。

Forge 追加的 `rendertype_entity_unlit_translucent.vsh` (第 28 行):

```glsl
lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
```

- `Sampler2` = OpenGL 纹理单元 2(由 `LightTexture.turnOnLightLayer()` 绑定 `light_map`)
- `UV2` = 顶点属性中的 lightmap UV,类型 `ivec2`(由 `ELEMENT_UV2` 的 `Type.SHORT` 映射)
- `UV2 / 16`:UV2 是 packed short 对,除以 16 是因为 lightmap 是 16×16 纹理。UV2 的 x 分量 = block light level (0–15),y 分量 = sky light level (0–15)。除以 16 后映射到 [0, 1) 范围,`texelFetch` 用整数坐标直接查询。
- 在 `rendertype_entity_unlit_translucent.fsh` (第 28 行):`color *= lightMapColor;`

> **注**: 原版着色器(rendertype_entity_cutout 等)使用相同的 `Sampler2` + `UV2` 模式。`LightTexture.turnOnLightLayer()` 调用 `RenderSystem.setShaderTexture(2, this.lightTextureLocation)` 将 lightmap 绑定到纹理单元 2。

**LightmapStateShard** (`RenderStateShard.java:363`):

```java
public static class LightmapStateShard extends RenderStateShard.BooleanStateShard {
    public LightmapStateShard(boolean useLightmap) {
        super("lightmap", () -> {
            if (useLightmap) {
                Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            }
        }, () -> {
            if (useLightmap) {
                Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
            }
        }, useLightmap);
    }
}
```

启动对应的 render pass 时绑定纹理单元 2,结束时解绑。

### 2.5 完整数据流

```
LevelRenderer.getLightColor(level, pos)
  → getBrightness(SKY, pos) * 16 + getBrightness(BLOCK, pos) * 16
  → pack → block<<4 | sky<<20
       ↓
EntityRenderer.getPackedLightCoords(entity, partialTick)
  → LightTexture.pack(getBlockLightLevel, getSkyLightLevel)
       ↓
VertexConsumer.uv2(packedLight)
  → BufferBuilder 写入两个 short
       ↓
GPU 着色器: UV2 (ivec2: blockLevel, skyLevel)
  → texelFetch(Sampler2, UV2 / 16, 0) 从 16×16 lightmap 采样
  → 结果 vec4 与 fragment color 相乘
```

---

## 3. LevelRenderer.getLightColor

**文件**: `net/minecraft/client/renderer/LevelRenderer.java`

### 3.1 方法定义 (第 3038–3055 行)

```java
public static int getLightColor(BlockAndTintGetter level, BlockPos pos) {
    return getLightColor(level, level.getBlockState(pos), pos);
}

public static int getLightColor(BlockAndTintGetter level, BlockState state, BlockPos pos) {
    if (state.emissiveRendering(level, pos)) {
        return 15728880;                   // LightTexture.FULL_BRIGHT = 0xF000F0
    } else {
        int i = level.getBrightness(LightLayer.SKY, pos);   // 0..15
        int j = level.getBrightness(LightLayer.BLOCK, pos); // 0..15
        int k = state.getLightEmission(level, pos);         // 块自发光
        if (j < k) {
            j = k;                                          // 取块光和发光中较大者
        }
        return i << 20 | j << 4;            // 与 LightTexture.pack() 等价
    }
}
```

### 3.2 关键行为

- `emissiveRendering`: 在 `BlockBehaviour.java:765` 中定义,默认返回 `false`。一些特殊块(如 powered redstone lamp 的某些状态)会覆盖为返回 `true`,此时直接返回 FULL_BRIGHT。
- **自发光**: `state.getLightEmission()` 返回块的自然发光等级(如 glowstone=15, torch=14)。`j = Math.max(j, k)` 保证块光不小于自发光。
- **返回格式**: `sky<<20 | block<<4`,与 `LightTexture.pack()` 相同。

### 3.3 调用场景

- **方块模型渲染** (ModelBlockRenderer.java:159, 293 等):每个顶点的光照。
- **液体渲染** (LiquidBlockRenderer.java:341):液体面光照。
- **块实体** (BlockEntityRenderDispatcher.java:80):如箱子、熔炉。
- **粒子** (LevelRenderer.java:345, 369):天气粒子光照。

---

## 4. RenderType 与光照相关的 RenderState

**文件**: `net/minecraft/client/renderer/RenderType.java`

### 4.1 带有 LIGHTMAP 的 RenderType

以下 RenderType 在其 CompositeState 中设置了 `setLightmapState(LIGHTMAP)`,意味着着色器会采样 lightmap 纹理(`Sampler2`),并根据顶点 UV2 施加光照:

| RenderType 字段 | VertexFormat | 行号 |
|----------------|-------------|------|
| `SOLID` | BLOCK | :29 |
| `CUTOUT_MIPPED` | BLOCK | :30 |
| `CUTOUT` | BLOCK | :31 |
| `TRANSLUCENT` | BLOCK | :32 (通过 translucentState) |
| `TRANSLUCENT_MOVING_BLOCK` | BLOCK | :33 |
| `TRANSLUCENT_NO_CRUMBLING` | BLOCK | :34 |
| `ARMOR_CUTOUT_NO_CULL` | NEW_ENTITY | :36 |
| `ENTITY_SOLID` | NEW_ENTITY | :40 |
| `ENTITY_CUTOUT` | NEW_ENTITY | :44 |
| `ENTITY_CUTOUT_NO_CULL` | NEW_ENTITY | :48 |
| `ENTITY_CUTOUT_NO_CULL_Z_OFFSET` | NEW_ENTITY | :52 |
| `ITEM_ENTITY_TRANSLUCENT_CULL` | NEW_ENTITY | :56 |
| `ENTITY_TRANSLUCENT_CULL` | NEW_ENTITY | :60 |
| `ENTITY_TRANSLUCENT` | NEW_ENTITY | :64 |
| `ENTITY_SMOOTH_CUTOUT` | NEW_ENTITY | :72 |
| `ENTITY_DECAL` | NEW_ENTITY | :80 |
| `ENTITY_NO_OUTLINE` | NEW_ENTITY | :84 |
| `ENTITY_SHADOW` | NEW_ENTITY | :88 |
| `LEASH` | POSITION_COLOR_LIGHTMAP | :99 |
| `TEXT` | POSITION_COLOR_TEX_LIGHTMAP | :113 |
| `TEXT_BACKGROUND` | POSITION_COLOR_LIGHTMAP | :115 |
| `TEXT_INTENSITY` | POSITION_COLOR_TEX_LIGHTMAP | :117 |
| (等文字变体) | ... | :120–131 |
| `TRIPWIRE` | BLOCK | :133 |
| `ENERGY_SWIRL` | NEW_ENTITY | :269 |

### 4.2 **不**带 LIGHTMAP 的 RenderType

以下 RenderType **没有** `LIGHTMAP` 状态,即不使用光照纹理,颜色取自纹理/顶点颜色直接输出:

| RenderType | VertexFormat | 行号 | 原因 |
|-----------|-------------|------|------|
| `ENTITY_TRANSLUCENT_EMISSIVE` | NEW_ENTITY | :67–69 | 自发光实体(无 lightmap) |
| `EYES` | NEW_ENTITY | :95–97 | 眼睛层,只写 COLOR,无 lightmap |
| `LIGHTNING` | POSITION_COLOR | :132 | 闪电,纯色不光照 |
| `BEACON_BEAM` | BLOCK | :75–77 | 信标光束,无 lightmap |
| `DRAGON_EXPLOSION_ALPHA` | NEW_ENTITY | :91–93 | 末影龙爆炸 alpha |
| `ARMOR_GLINT` / `ARMOR_ENTITY_GLINT` | POSITION_TEX | :101–102 | 附魔光效 |
| `GLINT` / `GLINT_TRANSLUCENT` / `GLINT_DIRECT` | POSITION_TEX | :103–105 | 附魔光效 |
| `ENTITY_GLINT` / `ENTITY_GLINT_DIRECT` | POSITION_TEX | :106–107 | 实体附魔 |
| `CRUMBLING` | BLOCK | :108–110 | 方块破坏动画 |
| `END_PORTAL` / `END_GATEWAY` | POSITION | :134–135 | 末地传送门 |
| `GUI` / `GUI_OVERLAY` | POSITION_COLOR | :144–145 | GUI |
| `LINES` / `LINE_STRIP` | POSITION_COLOR_NORMAL | :136–137 | 调试线框 |

### 4.3 关键观察

- **`entity_translucent_emissive`**: 虽然名为 "translucent",但 **没有** LIGHTMAP(也没有 OVERLAY),意味着它输出纹理颜色 × ColorModulator × vertexColor,完全不依赖 lightmap。
- **`eyes`**: 同样没有 LIGHTMAP,使用 ADDITIVE 混合 + COLOR_WRITE only.
- **`entity_smooth_cutout`** (`:72`): 有 LIGHTMAP,但无 OVERLAY → 主要用在盔甲架上(模型平滑着色,无 overlay)。

---

## 5. EntityRenderer / MobRenderer 光照传递

### 5.1 EntityRenderer.getPackedLightCoords (第 33–36 行)

```java
public final int getPackedLightCoords(T entity, float partialTicks) {
    BlockPos blockpos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
    return LightTexture.pack(this.getBlockLightLevel(entity, blockpos), this.getSkyLightLevel(entity, blockpos));
}
```

### 5.2 EntityRenderer.getBlockLightLevel / getSkyLightLevel (第 38–44 行)

```java
protected int getSkyLightLevel(T entity, BlockPos pos) {
    return entity.level().getBrightness(LightLayer.SKY, pos);   // 0..15
}

protected int getBlockLightLevel(T entity, BlockPos pos) {
    return entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, pos);  // 着火时块光=15
}
```

- 默认取实体眼睛位置的光照。
- 着火实体强制 block light = 15。
- 子类可以 override(例如某些实体在暗处需要更高亮度)。

### 5.3 EntityRenderDispatcher.render (第 133–143 行)

```java
public <E extends Entity> void render(E entity, double x, double y, double z,
        float rotationYaw, float partialTicks, PoseStack poseStack,
        MultiBufferSource buffer, int packedLight) {
    EntityRenderer<? super E> entityrenderer = this.getRenderer(entity);
    // ...
    entityrenderer.render(entity, rotationYaw, partialTicks, poseStack, buffer, packedLight);
}
```

`packedLight` 由调用者传入(通常是级别渲染循环中计算)。

### 5.4 LivingEntityRenderer.render (第 51–137 行)

```java
public void render(T entity, float entityYaw, float partialTicks,
        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    // ... setup model ...
    RenderType rendertype = this.getRenderType(entity, flag, flag1, flag2);
    if (rendertype != null) {
        VertexConsumer vertexconsumer = buffer.getBuffer(rendertype);
        int i = getOverlayCoords(entity, this.getWhiteOverlayProgress(entity, partialTicks));
        this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, i, 1.0F, 1.0F, 1.0F,
            flag1 ? 0.15F : 1.0F);
    }
    // Render layers (armor, eyes, etc.)
    for (RenderLayer<T, M> renderlayer : this.layers) {
        renderlayer.render(poseStack, buffer, packedLight, entity, ...);
    }
}
```

- `packedLight` 原样传递给 `model.renderToBuffer`,后者内部调用 `VertexConsumer.uv2(packedLight)`。
- `getWhiteOverlayProgress` 默认返回 0.0,子类可 override(如 `HurtCrystal`、`WitherRenderer` 等)。

### 5.5 MobRenderer.render (第 41–47 行)

```java
public void render(T p_entity, float entityYaw, float partialTicks,
        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    super.render(p_entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    Entity entity = p_entity.getLeashHolder();
    if (entity != null) {
        this.renderLeash(p_entity, partialTicks, poseStack, buffer, entity);
    }
}
```

Leash 渲染 (`:87–99`) 手动插值两个实体的 block/sky light:

```java
int k = LightTexture.pack(i, j);   // i=插值后的 block, j=插值后的 sky
// ...
consumer.vertex(...).color(...).uv2(k).endVertex();
```

### 5.6 完整调用链

```
LevelRenderer 渲染循环
  → EntityRenderDispatcher.render(entity, ..., packedLight)
    → EntityRenderDispatcher.getPackedLightCoords(entity, partialTick)
      → EntityRenderer.getPackedLightCoords
        → LightTexture.pack(getBlockLightLevel, getSkyLightLevel)
    → EntityRenderer.render(entity, ..., packedLight)
      → (optional) LivingEntityRenderer.render
        → model.renderToBuffer(..., packedLight, ...)
          → VertexConsumer.uv2(packedLight)
            → GPU: texelFetch(Sampler2, UV2/16, 0)
```

### 5.7 FULL_BRIGHT 的使用

`LightTexture.FULL_BRIGHT (0xF000F0)` 在以下关键位置直接使用:

| 位置 | 行 | 原因 |
|------|-----|------|
| `LevelRenderer.getLightColor` — emissiveRendering | :3044 | 方块标记为 emissive 时 |
| `Brightness.FULL_BRIGHT` | Brightness.java:11 | 数据生成/JSON 中预设全亮 |

在实体渲染中**不直接使用** FULL_BRIGHT — 实体通过 `getPackedLightCoords` 实时计算位置光照。

---

## 6. Gamma/Tonemap 曲线

### 6.1 选项范围

`Options.java:511`:

```java
private final OptionInstance<Double> gamma = new OptionInstance<>(
    "options.gamma",
    OptionInstance.noTooltip(),
    ..., OptionInstance.UnitDouble.INSTANCE, 0.5D, ...);
```

- 范围: `0.0` (最小值) 到 `1.0` (最大值)
- 默认值: `0.5`
- `UnitDouble.INSTANCE` 表示范围是 [0.0, 1.0],滑块 0–100。

### 6.2 `notGamma` — 反向 gamma 变换 (LightTexture.java:181–184)

```java
private float notGamma(float value) {
    float f = 1.0F - value;
    return 1.0F - f * f * f * f;
}
```

当 `value=0`:`notGamma=0`;`value=1`:`notGamma=1`。
中间值曲线:`notGamma(x) = 1 - (1-x)^4`。

这是一条**四次方反向 S 曲线** — 大幅提升暗部亮度,亮部变化较小。

### 6.3 Gamma 混合 (LightTexture.java:157–159)

```java
float f14 = this.minecraft.options.gamma().get().floatValue();   // [0, 1], 默认 0.5
Vector3f vector3f4 = new Vector3f(
    this.notGamma(vector3f1.x),
    this.notGamma(vector3f1.y),
    this.notGamma(vector3f1.z));
vector3f1.lerp(vector3f4, Math.max(0.0F, f14 - f3));
```

- 原始颜色 `vector3f1` lerp 到 `notGamma` 变换后的颜色。
- 混合因子 = `max(0, gamma - darknessGamma)`。
  - gamma=0: 因子=0,无变换(原色)。
  - gamma=0.5: 因子=0.5,50% 混合。
  - gamma=1.0: 因子=1.0,完全使用 `notGamma` 颜色。
  - Darkness 效果 (`f3`) 会抵消 gamma。

### 6.4 完整公式示例

若 gamma=1.0(最大值),lightmap 颜色来自完全 `notGamma` 变换后的值,等同于:

```
output = 1 - (1 - input)^4
```

这使暗部区域(洞穴、夜晚)被显著抬升。

---

## 7. 维度相关的行为

### 7.1 维度类型参数 (`DimensionType.java`)

从 `net/minecraft/data/worldgen/DimensionTypes.java` (第 12–15 行):

| 维度 | hasSkyLight | ambientLight | hasCeiling |
|------|------------|-------------|------------|
| Overworld | ✅ true | 0.0 | false |
| Nether | ❌ false | 0.1 | true |
| End | ❌ false | 0.0 | false |
| Overworld Caves | ✅ true | 0.0 | true |

### 7.2 天空光的影响

- **Nether** (`hasSkyLight=false`): `level.getBrightness(LightLayer.SKY, pos)` 始终返回 0。Lightmap 的 sky 列(i=0)被使用,该行颜色较暗。
- **End** (`hasSkyLight=false`): 同样 sky light 为 0。
- **Overworld** (`hasSkyLight=true`): 正常日照循环。

### 7.3 维度特殊效果 (`DimensionSpecialEffects.java`)

| 维度 | 效果类 | forceBrightLightmap | constantAmbientLight | skyType |
|------|--------|---------------------|---------------------|---------|
| Overworld | OverworldEffects | ❌ false | ❌ false | NORMAL |
| Nether | NetherEffects | ❌ false | ✅ true | NONE |
| End | EndEffects | ✅ true | ❌ false | END |

**Nether 的 `constantAmbientLight=true`**:在 `LightTexture.updateLightTexture` 计算中通过 `clientlevel.effects().adjustLightmapColors()` 实现整体亮度提升(Nether 的实现继承自 `IForgeDimensionSpecialEffects`,默认为无操作,但原版 Nether 没有 override `adjustLightmapColors`,所以其亮度主要来自 `ambientLight=0.1` 和 `constantAmbientLight=true`)。

**End 的 `forceBrightLightmap=true`**:当 `flag=true` 时(`LightTexture.java:124–126`):

```java
if (flag) {
    vector3f1.lerp(new Vector3f(0.99F, 1.12F, 1.0F), 0.25F);
    clampColor(vector3f1);
}
```

强制 lightmap 偏向亮白(略偏蓝绿),跳过天空光叠加、灰化、世界暗化等步骤。

### 7.4 `getBrightness` 中的 ambientLight 作用

```java
return Mth.lerp(dimensionType.ambientLight(), f1, 1.0F);
```

- **Overworld** (ambientLight=0.0): 块光/天空光 0 级时亮度为 0。
- **Nether** (ambientLight=0.1): 即使光级别为 0,最低亮度也被推到 `0.1` → lightmap 整体不会全黑。
- **End** (ambientLight=0.0): 与 Overworld 相同基础范围,但 `forceBrightLightmap=true` 使其亮。

### 7.5 光照传播简注

- **Overworld**: 天空光从最高处向下传播,受遮挡减少。
- **Nether**: `hasSkyLight=false` → 天空光引擎不工作,仅块光(岩浆、萤石、下界岩自然发光等)传播。`hasCeiling=true` 也限制天空光存在。
- **End**: 主岛无天空光,仅有龙/水晶发出的块光。

---

## 总结

### 关键编号常量速查

| 常量 | 值 | 含义 |
|------|-----|------|
| `FULL_BRIGHT` (packed) | 0xF000F0 = 15728880 | block=15, sky=15 |
| `FULL_SKY` (packed) | 0xF00000 = 15728640 | sky=15, block=0 |
| `FULL_BLOCK` | 240 = 15<<4 | block=15 |
| `MAX_BRIGHTNESS` | 240 | 光等级最大值(不是 packed) |
| `PACKED_LIGHT_MASK` | 0xFFFF | block 光提取 mask(Forge 修复) |

### Lightmap 像素布局

```
16×16 纹理
    列 j = block light level (0..15)
    行 i = sky light level (0..15)
    像素格式: ARGB, 线性纹理过滤
    纹理单元: GL_TEXTURE2 (Sampler2)
```

### 核心公式流程

```
for each (blockLevel, skyLevel):
    blockBrightness = blockLevel/15 / (4 - 3*blockLevel/15)
    blockBrightness = lerp(ambientLight, blockBrightness, 1)
    skyBrightness = skyLevel/15 / (4 - 3*skyLevel/15)
    skyBrightness = lerp(ambientLight, skyBrightness, 1)

    color = blockBrightness * flicker × [1, 0.6x+0.4, 0.6x²+0.4]  // block 色
    color += skyBrightness × [f, f, 1] lerp [1,1,1] 0.35          // 天空色

    // 维度特效、夜视、暗化、gamma 叠加
    // notGamma(x) = 1 - (1-x)^4
    finalColor = lerp(color, notGamma(color), max(0, gamma - darknessGamma))

    lightmapPixel[blockLevel][skyLevel] = finalColor × 255
```

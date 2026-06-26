# Minecraft 1.21.1 (NeoForge) Vanilla Lighting Model

> **Source tree**: `.local_ref/mc/1.21.1/sources/`
> **Purpose**: Reference for Eyelib rendering integration; document pixel‑accurate lightmap generation, the packed‑light vertex pipeline, and entity/block dispatch.

---

## Table of Contents

1. [LightTexture.java](#1-lighttexturejava)
2. [Packed Light Coordinate System & Shader Pipeline](#2-packed-light-coordinate-system--shader-pipeline)
3. [LevelRenderer.getLightColor](#3-levelrenderergetlightcolor)
4. [RenderType.java Lighting Entries](#4-rendertypejava-lighting-entries)
5. [EntityRenderer / MobRenderer / LivingEntityRenderer Lighting](#5-entityrenderer--mobrenderer--livingentityrenderer-lighting)
6. [Gamma & Tonemap](#6-gamma--tonemap)
7. [Dimension‑Specific Behavior](#7-dimension-specific-behavior)

---

## 1. LightTexture.java

**File**: `net/minecraft/client/renderer/LightTexture.java` (197 lines)

### 1.1 Constants

```java
// LightTexture.java:20-22
public static final int FULL_BRIGHT = 15728880;   // 0xF000F0
public static final int FULL_SKY   = 15728640;   // 0xF00000
public static final int FULL_BLOCK = 240;         // 0xF0
```

Derivation (`pack(15, 15)`):
- `blockLight = 15` → `15 << 4` = `240` = `0xF0`
- `skyLight   = 15` → `15 << 20` = `15728640` = `0xF00000`
- OR: `240 | 15728640` = `15728880` = `0xF000F0`

There is no inner enum/record named `Brightness` in 1.21.1 — these are just `static final int` fields.

### 1.2 Lightmap Texture Creation (Constructor)

```java
// LightTexture.java:31-45
public LightTexture(GameRenderer renderer, Minecraft minecraft) {
    this.renderer = renderer;
    this.minecraft = minecraft;
    this.lightTexture = new DynamicTexture(16, 16, false);       // 16×16 RGBA
    this.lightTextureLocation = this.minecraft.getTextureManager()
        .register("light_map", this.lightTexture);
    this.lightPixels = this.lightTexture.getPixels();

    for (int i = 0; i < 16; i++) {
        for (int j = 0; j < 16; j++) {
            this.lightPixels.setPixelRGBA(j, i, -1);              // white fill
        }
    }
    this.lightTexture.upload();
}
```

- The lightmap is a **16×16** `DynamicTexture` registered under the name `"light_map"`.
- Initial fill is white (`-1` = `0xFFFFFFFF`).

### 1.3 Binding to Texture Unit 2

```java
// LightTexture.java:58-66
public void turnOffLightLayer() {
    RenderSystem.setShaderTexture(2, 0);
}

public void turnOnLightLayer() {
    RenderSystem.setShaderTexture(2, this.lightTextureLocation);
    this.minecraft.getTextureManager().bindForSetup(this.lightTextureLocation);
    RenderSystem.texParameter(3553, 10241, 9729);   // GL_TEXTURE_MIN_FILTER = GL_LINEAR
    RenderSystem.texParameter(3553, 10240, 9729);   // GL_TEXTURE_MAG_FILTER = GL_LINEAR
}
```

- The lightmap is bound to **GL_TEXTURE2** (`Sampler2` in shaders).
- Filtering is **bilinear** (GL_LINEAR for both min and mag).

### 1.4 Tick (Block Light Flicker)

```java
// LightTexture.java:52-56
public void tick() {
    this.blockLightRedFlicker = this.blockLightRedFlicker
        + (float)((Math.random() - Math.random())
                  * Math.random() * Math.random() * 0.1);
    this.blockLightRedFlicker *= 0.9F;
    this.updateLightTexture = true;
}
```

- Accumulates a chaotic noise term that decays by 0.9× per tick.
- Used to introduce red-channel flicker to block light (fire, lava).

### 1.5 Darkness Effects

```java
// LightTexture.java:69-77
private float getDarknessGamma(float partialTick) {
    MobEffectInstance mobeffectinstance =
        this.minecraft.player.getEffect(MobEffects.DARKNESS);
    return mobeffectinstance != null
        ? mobeffectinstance.getBlendFactor(this.minecraft.player, partialTick)
        : 0.0F;
}

private float calculateDarknessScale(LivingEntity entity, float gamma, float partialTick) {
    float f = 0.45F * gamma;
    return Math.max(0.0F,
        Mth.cos(((float)entity.tickCount - partialTick) * (float) Math.PI * 0.025F) * f);
}
```

- DARKNESS effect contributes a gamma‑modulating factor via `getBlendFactor`.
- `calculateDarknessScale` produces a cosine‑wave pulsing darkness for ambient darkening (later subtracted from the lightmap color).

### 1.6 Core Update: `updateLightTexture(float partialTicks)`

**File**: `LightTexture.java:79-169`

This is called once per frame (when `updateLightTexture` flag is set by `tick()`). It regenerates all 256 texels of the 16×16 lightmap.

#### 1.6.1 Sky Darken & Flash

```java
// LightTexture.java:85-91
float f = clientlevel.getSkyDarken(1.0F);
float f1;
if (clientlevel.getSkyFlashTime() > 0) {
    f1 = 1.0F;
} else {
    f1 = f * 0.95F + 0.05F;
}
```

- `getSkyDarken()` (defined in `ClientLevel.java:766-774`) computes:
  ```java
  public float getSkyDarken(float partialTick) {
      float f = this.getTimeOfDay(partialTick);
      float f1 = 1.0F - (Mth.cos(f * (float)(Math.PI * 2)) * 2.0F + 0.2F);
      f1 = Mth.clamp(f1, 0.0F, 1.0F);
      f1 = 1.0F - f1;
      f1 *= 1.0F - this.getRainLevel(partialTick) * 5.0F / 16.0F;
      f1 *= 1.0F - this.getThunderLevel(partialTick) * 5.0F / 16.0F;
      return f1 * 0.8F + 0.2F;
  }
  ```
- `f1` is the **sky darken factor** (0.05 .. 1.0), boosted to 1.0 during lightning flash.

#### 1.6.2 Night Vision & Conduit Power

```java
// LightTexture.java:96-104
float f6 = this.minecraft.player.getWaterVision();
float f5;
if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
    f5 = GameRenderer.getNightVisionScale(this.minecraft.player, partialTicks);
} else if (f6 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
    f5 = f6;
} else {
    f5 = 0.0F;
}
```

- `GameRenderer.getNightVisionScale` (`GameRenderer.java:999-1001`):
  ```java
  public static float getNightVisionScale(LivingEntity livingEntity, float nanoTime) {
      MobEffectInstance mobeffectinstance = livingEntity.getEffect(MobEffects.NIGHT_VISION);
      return !mobeffectinstance.endsWithin(200)
          ? 1.0F
          : 0.7F + Mth.sin(((float)mobeffectinstance.getDuration() - nanoTime)
                            * (float) Math.PI * 0.2F) * 0.3F;
  }
  ```
  Returns 1.0 if effect duration > 200 ticks, otherwise blinks with a sine wave.

#### 1.6.3 Per‑Texel Loop

```java
// LightTexture.java:106-163
Vector3f vector3f = new Vector3f(f, f, 1.0F)
    .lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
float f7 = this.blockLightRedFlicker + 1.5F;
Vector3f vector3f1 = new Vector3f();

for (int i = 0; i < 16; i++) {           // i = sky light level (row)
    for (int j = 0; j < 16; j++) {       // j = block light level (col)
        float f8 = getBrightness(clientlevel.dimensionType(), i) * f1;  // sky luminance
        float f9 = getBrightness(clientlevel.dimensionType(), j) * f7;  // block luminance

        // Red‑channel emphasis for block light (fire glow)
        float f10 = f9 * ((f9 * 0.6F + 0.4F) * 0.6F + 0.4F);     // green
        float f11 = f9 * (f9 * f9 * 0.6F + 0.4F);                 // blue
        vector3f1.set(f9, f10, f11);  // initial: R = blockLum, G = curve, B = curve2
        // ...
    }
}
```

Key observations:
- **`i` is the row index (sky light axis)**, **`j` is the column index (block light axis)**.
- Pixel is written at `(j, i)` → `lightPixels.setPixelRGBA(j, i, ...)`.
- `getBrightness(dimensionType, level)` converts 0‑15 level to a float luminance (see §1.7).

#### 1.6.4 Force Bright Lightmap (End)

```java
// LightTexture.java:117-130
boolean flag = clientlevel.effects().forceBrightLightmap();
if (flag) {
    vector3f1.lerp(new Vector3f(0.99F, 1.12F, 1.0F), 0.25F);
    clampColor(vector3f1);
} else {
    Vector3f vector3f2 = new Vector3f(vector3f).mul(f8);
    vector3f1.add(vector3f2);
    vector3f1.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
    // ... darkenWorldAmount ...
}
```

- When `forceBrightLightmap()` is true (**End dimension**), the color is lerped toward a fixed bright warm tone and sky/block contributions are skipped.

#### 1.6.5 Dimension‑Specific Adjustments

```java
// LightTexture.java:132
clientlevel.effects().adjustLightmapColors(
    clientlevel, partialTicks, f, f7, f8, j, i, vector3f1);
```

- Delegates to `DimensionSpecialEffects.adjustLightmapColors(...)` (NeoForge extension interface).

#### 1.6.6 Night Vision Boost

```java
// LightTexture.java:134-141
if (f5 > 0.0F) {
    float f13 = Math.max(vector3f1.x(), Math.max(vector3f1.y(), vector3f1.z()));
    if (f13 < 1.0F) {
        float f15 = 1.0F / f13;
        Vector3f vector3f5 = new Vector3f(vector3f1).mul(f15);
        vector3f1.lerp(vector3f5, f5);
    }
}
```

- Scales up the brightest channel to 1.0 and lerps by night‑vision strength.

#### 1.6.7 Gamma Correction

```java
// LightTexture.java:151-153
float f14 = this.minecraft.options.gamma().get().floatValue();
Vector3f vector3f4 = new Vector3f(
    this.notGamma(vector3f1.x),
    this.notGamma(vector3f1.y),
    this.notGamma(vector3f1.z()));
vector3f1.lerp(vector3f4, Math.max(0.0F, f14 - f3));
```

- See §6 (Gamma & Tonemap) below.

#### 1.6.8 Final Pixel Packing

```java
// LightTexture.java:156-161
vector3f1.mul(255.0F);
int j1 = 255;
int k = (int)vector3f1.x();
int l = (int)vector3f1.y();
int i1 = (int)vector3f1.z();
this.lightPixels.setPixelRGBA(j, i, 0xFF000000 | i1 << 16 | l << 8 | k);
```

- Packs as `ABGR` (NativeImage internal format): `A=255, B=i1, G=l, R=k`.
- Pixel at column `j` (block light), row `i` (sky light).

### 1.7 `getBrightness(DimensionType, int lightLevel)`

```java
// LightTexture.java:180-184
public static float getBrightness(DimensionType dimensionType, int lightLevel) {
    float f = (float)lightLevel / 15.0F;                     // normalize 0‑15 → 0‑1
    float f1 = f / (4.0F - 3.0F * f);                       // smoothstep curve
    return Mth.lerp(dimensionType.ambientLight(), f1, 1.0F); // ambient‑light blend
}
```

- The curve `f / (4 - 3f)` is a common gamma‑like mapping.
- `dimensionType.ambientLight()` floats between 0.0 and 1.0:
  - Overworld: `0.0`
  - Nether: `0.1`
  - End: `0.0`
- At `ambientLight=0.0`, result = `f1`; at `ambientLight=1.0`, result = `1.0` (always bright).

### 1.8 `pack`, `block`, `sky`

```java
// LightTexture.java:186-196
public static int pack(int blockLight, int skyLight) {
    return blockLight << 4 | skyLight << 20;
}

public static int block(int packedLight) {
    return (packedLight & 0xFFFF) >> 4;  // Forge fix for MC-169806
}

public static int sky(int packedLight) {
    return packedLight >> 20 & 65535;
}
```

- `pack(b, s) = b<<4 | s<<20` — two 4‑bit values packed into one int with 16‑bit spacing.
- `block(packed)`: lower 16 bits, right‑shift 4 → 0‑15 range.
- `sky(packed)`: upper bits, right‑shift 20 → 0‑15 range.

### 1.9 `notGamma`

```java
// LightTexture.java:175-178
private float notGamma(float value) {
    float f = 1.0F - value;
    return 1.0F - f * f * f * f;
}
```

- The inverse gamma curve: `1 - (1 - v)^4`.
- Applied per‑channel, then lerped with the original by `(gamma - darknessGamma)` (see §6).

### 1.10 Differences from 1.20.1 (Forge)

| Aspect | 1.20.1 (Forge) | 1.21.1 (NeoForge) |
|---|---|---|
| **Imports** | `net.minecraftforge.api.distmarker.*` | `net.neoforged.api.distmarker.*` |
| **Imports** | `import org.joml.Vector3fc;` (used) | `Vector3fc` import removed; no casts needed |
| **`getDarknessGamma`** | Checks `mobeffectinstance.getFactorData().isPresent()` then `.getFactorData().get().getFactor(...)` | Calls `mobeffectinstance.getBlendFactor(...)` directly (cleaner API, NeoForge patch) |
| **`Vector3f` construction** | `(new Vector3f(...)).lerp(new Vector3f(...), ...)` with `(Vector3fc)` casts | `new Vector3f(...).lerp(new Vector3f(...), ...)` without casts |
| **Pixel packing** | `-16777216 \| i1 << 16 \| l << 8 \| k` | `0xFF000000 \| i1 << 16 \| l << 8 \| k` (same value, cosmetic) |
| **`sky()` literal** | `'\uffff'` (char literals) | `65535` (int literal) |
| **`block()` comment** | `// Forge: Fix fullbright quads showing dark artifacts. Reported as MC-169806` | Same comment |
| **`clampColor`** | `private static void` | `private static void` (unchanged) |
| **`notGamma`** | `private float` (instance method) | `private float` (instance method, unchanged) |
| **NeoForge extension** | N/A | `LightTexture` implements `net.neoforged.neoforge.client.extensions.ILightTextureExtension` (binary patch) |

---

## 2. Packed Light Coordinate System & Shader Pipeline

### 2.1 Overview

The light model follows this pipeline:

```
LightTexture.pack(b, s) ─► vertex attribute UV2 ─► GL_TEXTURE2 (Sampler2) lookup
```

### 2.2 Vertex Formats

**File**: `com/mojang/blaze3d/vertex/DefaultVertexFormat.java`

Key formats with lightmap support:

| Format | Vertex Attrs | Used For |
|---|---|---|
| `BLOCK` (`:9-16`) | Position, Color, UV0, **UV2**, Normal | Block rendering (solid/cutout/translucent) |
| `NEW_ENTITY` (`:17-25`) | Position, Color, UV0, UV1 (overlay), **UV2**, Normal | Entity rendering |
| `POSITION_COLOR_LIGHTMAP` (`:43-47`) | Position, Color, **UV2** | Leash, text background |
| `POSITION_COLOR_TEX_LIGHTMAP` (`:57-62`) | Position, Color, UV0, **UV2** | Text rendering |
| `PARTICLE` (`:26-31`) | Position, UV0, Color, **UV2** | Particles |

`UV2` is the lightmap coordinate attribute — a 2‑component unsigned short (or `ivec2` in GLSL).

### 2.3 Packed Light → UV2 Vertex Attribute

When a vertex is written, the packed light is set via `VertexConsumer.setLight(packedLight)`.

For **block** rendering (`DefaultVertexFormat.BLOCK`), `UV2` stores the packed int as two 16‑bit unsigned shorts:
- `UV2.x = blockLight * 16`  (lower 16 bits: `packed & 0xFFFF` = `blockLight << 4`)
- `UV2.y = skyLight * 16`    (upper 16 bits: `packed >> 16` = `skyLight << 4`)

### 2.4 Shader Texture Lookup

**Only the NeoForge‑provided shader is available in the extracted sources** at:
`assets/neoforge/shaders/core/rendertype_entity_unlit_translucent.{json,vsh,fsh}`

Vanilla shader files (`.vsh`, `.fsh`, `.json` under `assets/minecraft/shaders/core/`) are **not present** in the extracted source tree. However, the NeoForge unlit shader reveals the standard pattern.

##### Vertex Shader (`rendertype_entity_unlit_translucent.vsh`)

```glsl
// assets/neoforge/shaders/core/rendertype_entity_unlit_translucent.vsh
#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;        // overlay texture coords
in ivec2 UV2;        // lightmap texture coords (packed)
in vec3 Normal;

uniform sampler2D Sampler1;   // overlay texture (GL_TEXTURE1)
uniform sampler2D Sampler2;   // lightmap texture (GL_TEXTURE2)

out vec4 lightMapColor;
// ...

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
    vertexColor = Color;
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);   // ← LIGHTMAP SAMPLING
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
}
```

**Critical line**: `lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);`

- `UV2` is `ivec2` with values `(blockLight*16, skyLight*16)`.
- Dividing by 16 → texel coordinates `(blockLight, skyLight)` in the 16×16 lightmap.
- `texelFetch` does an integer texture lookup (no filtering).
- `Sampler2` = GL_TEXTURE2, bound by `LightTexture.turnOnLightLayer()`.

##### Fragment Shader (`rendertype_entity_unlit_translucent.fsh`)

```glsl
// assets/neoforge/shaders/core/rendertype_entity_unlit_translucent.fsh
void main() {
    vec4 color = texture(Sampler0, texCoord0);   // main texture
    if (color.a < 0.1) discard;
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;                       // ← LIGHTMAP APPLIED
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
```

**Critical line**: `color *= lightMapColor;` — the lightmap RGB multiplies the fragment color, applying the luminance from the lightmap.

### 2.5 Vanilla Shader Convention (Inferred)

All entity/block shaders that include `.setLightmapState(LIGHTMAP)` follow the same pattern:
1. Vertex shader receives `UV2 as ivec2`, does `texelFetch(Sampler2, UV2/16, 0)`.
2. Fragment shader receives `lightMapColor` via varying, does `color *= lightMapColor`.

Shaders that do **not** use `LIGHTMAP` (e.g., `RENDERTYPE_EYES_SHADER`, `RENDERTYPE_LIGHTNING_SHADER`) either hardcode `lightMapColor = vec4(1.0)` or omit the multiplication entirely.

---

## 3. LevelRenderer.getLightColor

**File**: `net/minecraft/client/renderer/LevelRenderer.java:3624-3641`

### 3.1 Two Overloads

```java
// LevelRenderer.java:3624-3626
public static int getLightColor(BlockAndTintGetter level, BlockPos pos) {
    return getLightColor(level, level.getBlockState(pos), pos);
}

// LevelRenderer.java:3628-3641
public static int getLightColor(BlockAndTintGetter level, BlockState state, BlockPos pos) {
    if (state.emissiveRendering(level, pos)) {
        return 15728880;    // FULL_BRIGHT
    } else {
        int i = level.getBrightness(LightLayer.SKY, pos);    // 0‑15
        int j = level.getBrightness(LightLayer.BLOCK, pos);  // 0‑15
        int k = state.getLightEmission(level, pos);          // block‑emitted light 0‑15
        if (j < k) {
            j = k;
        }
        return i << 20 | j << 4;   // manual pack: sky<<20 | block<<4
    }
}
```

### 3.2 Key Details

- **`emissiveRendering`**: a `StatePredicate` on `BlockBehaviour$Properties` (`BlockBehaviour.java:1291-1292`). Defaults to `(state, level, pos) -> false`. When true, returns `FULL_BRIGHT` (no world light computation).
- **`state.getLightEmission`**: returns the block's light emission (e.g., glowstone = 15, torch = 14).
- **`level.getBrightness(LightLayer layer, BlockPos pos)`** (defined in `BlockAndTintGetter.java:14-16`):
  ```java
  default int getBrightness(LightLayer lightType, BlockPos blockPos) {
      return this.getLightEngine().getLayerListener(lightType).getLightValue(blockPos);
  }
  ```
  Queries the **light engine's layer listener** (either sky light or block light), returning 0‑15.
- **Manual packing**: `i << 20 | j << 4` is equivalent to `LightTexture.pack(j, i)` (block goes to lower 16 bits, sky to upper 16 bits). Identical result.

### 3.3 Where It's Called

- `ModelBlockRenderer.java:237,441,863,867,...` — per‑quad light calculation
- `LiquidBlockRenderer.java:394-396` — liquid surface light
- `BlockEntityRenderDispatcher.java:86` — block entity light
- `BrightnessCombiner.java:15-16,21` — combines light from two block entities (e.g., double chest)
- `PaintingRenderer.java:108` — painting entity light
- `BrushableBlockRenderer.java:41` — brushed block light

---

## 4. RenderType.java Lighting Entries

**File**: `net/minecraft/client/renderer/RenderType.java` (1469 lines)

### 4.1 LIGHTMAP / NO_LIGHTMAP State Shards

Defined in `RenderStateShard.java:258-259`:

```java
public static final RenderStateShard.LightmapStateShard LIGHTMAP
    = new RenderStateShard.LightmapStateShard(true);
public static final RenderStateShard.LightmapStateShard NO_LIGHTMAP
    = new RenderStateShard.LightmapStateShard(false);
```

The `LightmapStateShard` (`RenderStateShard.java:473-485`) binds/unbinds the lightmap texture:

```java
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
```

### 4.2 Render Types That Use LIGHTMAP

All standard block/entity render types enable the lightmap. Selected key entries:

| RenderType Field | VertexFormat | Shader | LIGHTMAP | Notes |
|---|---|---|---|---|
| `SOLID` | BLOCK | `rendertype_solid` | Yes | `:26-38` |
| `CUTOUT_MIPPED` | BLOCK | `rendertype_cutout_mipped` | Yes | `:39-51` |
| `CUTOUT` | BLOCK | `rendertype_cutout` | Yes | `:52-64` |
| `TRANSLUCENT` | BLOCK | `rendertype_translucent` | Yes | `:65-67` |
| `TRANSLUCENT_MOVING_BLOCK` | BLOCK | `rendertype_translucent_moving_block` | Yes | `:68-70` |
| `ARMOR_CUTOUT_NO_CULL` | NEW_ENTITY | `rendertype_armor_cutout_no_cull` | Yes | `:71-73` |
| `ENTITY_SOLID` | NEW_ENTITY | `rendertype_entity_solid` | Yes | `:74-85` |
| `ENTITY_CUTOUT` | NEW_ENTITY | `rendertype_entity_cutout` | Yes | `:86-97` |
| `ENTITY_CUTOUT_NO_CULL` | NEW_ENTITY | `rendertype_entity_cutout_no_cull` | Yes | `:98-110` |
| `ENTITY_CUTOUT_NO_CULL_Z_OFFSET` | NEW_ENTITY | `rendertype_entity_cutout_no_cull_z_offset` | Yes | `:111-126` |
| `ITEM_ENTITY_TRANSLUCENT_CULL` | NEW_ENTITY | `rendertype_item_entity_translucent_cull` | Yes | `:127-140` |
| `ENTITY_TRANSLUCENT_CULL` | NEW_ENTITY | `rendertype_entity_translucent_cull` | Yes | `:141-152` |
| `ENTITY_TRANSLUCENT` | NEW_ENTITY | `rendertype_entity_translucent` | Yes | `:153-165` |
| `ENTITY_SMOOTH_CUTOUT` | NEW_ENTITY | `rendertype_entity_smooth_cutout` | Yes | `:179-189` |
| `ENTITY_DECAL` | NEW_ENTITY | `rendertype_entity_decal` | Yes | `:201-213` |
| `ENTITY_NO_OUTLINE` | NEW_ENTITY | `rendertype_entity_no_outline` | Yes | `:214-227` |
| `ENTITY_SHADOW` | NEW_ENTITY | `rendertype_entity_shadow` | Yes | `:228-243` |
| `LEASH` | POSITION_COLOR_LIGHTMAP | `rendertype_leash` | Yes | `:273-284` |
| `TEXT` | POSITION_COLOR_TEX_LIGHTMAP | `rendertype_text` | Yes | `:394-409` |
| `TEXT_BACKGROUND` | POSITION_COLOR_LIGHTMAP | `rendertype_text_background` | Yes | `:410-423` |
| `TEXT_INTENSITY` | POSITION_COLOR_TEX_LIGHTMAP | `rendertype_text_intensity` | Yes | `:424-439` |
| `LIGHTNING` | POSITION_COLOR | `rendertype_lightning` | **No** | `:526-539` |
| `DRAGON_RAYS` | POSITION_COLOR | `rendertype_lightning` | **No** | `:540-552` |
| `END_PORTAL` | POSITION | `rendertype_end_portal` | **No** | `:563-579` |
| `EYES` | NEW_ENTITY | `rendertype_eyes` | **No** | `:254-272` |
| `ENTITY_TRANSLUCENT_EMISSIVE` | NEW_ENTITY | `rendertype_entity_translucent_emissive` | **No** | `:166-178` |
| `ARMOR_ENTITY_GLINT` | POSITION_TEX | `rendertype_armor_entity_glint` | **No** | `:296-311` |
| `GLINT_TRANSLUCENT` | POSITION_TEX | `rendertype_glint_translucent` | **No** | `:312-327` |
| `GLINT` | POSITION_TEX | `rendertype_glint` | **No** | `:328-342` |
| `ENTITY_GLINT` | POSITION_TEX | `rendertype_entity_glint` | **No** | `:343-358` |
| `ENTITY_GLINT_DIRECT` | POSITION_TEX | `rendertype_entity_glint_direct` | **No** | `:359-373` |
| `CRUMBLING` | BLOCK | `rendertype_crumbling` | **No** | `:374-393` |
| `WATER_MASK` | POSITION | `rendertype_water_mask` | **No** | `:285-295` |

### 4.3 Emissive / Unlit Variants

- **`ENTITY_TRANSLUCENT_EMISSIVE`**: Has `OVERLAY` but **no** `LIGHTMAP`. The fragment shader skips the `color *= lightMapColor` step. Used for warden emissive layers (`WardenEmissiveLayer.java`).
- **`EYES`**: No `LIGHTMAP`. Used for spider/enderman/dragon eyes — these are additively blended full‑bright overlays.
- **NeoForge `rendertype_entity_unlit_translucent`** (registered in `ClientHooks.java:788`): Uses `LIGHTMAP` + a custom shader.

### 4.4 Render Types That Use FULL_BRIGHT

No RenderType bakes `FULL_BRIGHT` directly. Instead, `FULL_BRIGHT` is passed as the `packedLight` argument when:
- `state.emissiveRendering()` returns true (see §3.1)
- Entity is a lightning bolt (uses `LightTexture.pack(15, 15)`)
- Specific use‑sites bypass the lightmap by calling `buffer.getBuffer(...)` with `RENDERTYPE_EYES_SHADER` or `RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER`

---

## 5. EntityRenderer / MobRenderer / LivingEntityRenderer Lighting

### 5.1 EntityRenderer.getPackedLightCoords

**File**: `net/minecraft/client/renderer/entity/EntityRenderer.java:39-49`

```java
// EntityRenderer.java:39-41
public final int getPackedLightCoords(T entity, float partialTicks) {
    BlockPos blockpos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
    return LightTexture.pack(
        this.getBlockLightLevel(entity, blockpos),
        this.getSkyLightLevel(entity, blockpos));
}

// EntityRenderer.java:44-49
protected int getSkyLightLevel(T entity, BlockPos pos) {
    return entity.level().getBrightness(LightLayer.SKY, pos);
}
protected int getBlockLightLevel(T entity, BlockPos pos) {
    return entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, pos);
}
```

- Queries per‑position brightness from the `Level`'s light engine.
- On‑fire entities always get `blockLight = 15`.
- `LightTexture.pack` is called explicitly.

### 5.2 LivingEntityRenderer.render

**File**: `net/minecraft/client/renderer/entity/LivingEntityRenderer.java:52-139`

```java
// LivingEntityRenderer.java:52,124-129
public void render(T entity, float entityYaw, float partialTicks,
                   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    // ... NeoForge RenderLivingEvent.Pre hook ...
    RenderType rendertype = this.getRenderType(entity, flag, flag1, flag2);
    if (rendertype != null) {
        VertexConsumer vertexconsumer = buffer.getBuffer(rendertype);
        int i = getOverlayCoords(entity, this.getWhiteOverlayProgress(entity, partialTicks));
        this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, i, ...);
    }
    // ... layers ...
    super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    // ... NeoForge RenderLivingEvent.Post hook ...
}
```

- `packedLight` comes from the caller, ultimately from `EntityRenderer.getPackedLightCoords()`.
- `packedLight` is passed through to `model.renderToBuffer(...)` which sets the `UV2` vertex attribute.

### 5.3 LivingEntityRenderer.getRenderType

```java
// LivingEntityRenderer.java:142-152
@Nullable
protected RenderType getRenderType(T livingEntity, boolean bodyVisible,
                                    boolean translucent, boolean glowing) {
    ResourceLocation resourcelocation = this.getTextureLocation(livingEntity);
    if (translucent) {
        return RenderType.itemEntityTranslucentCull(resourcelocation);
    } else if (bodyVisible) {
        return this.model.renderType(resourcelocation);  // usually entityCutoutNoCull
    } else {
        return glowing ? RenderType.outline(resourcelocation) : null;
    }
}
```

- The model's `renderType` is typically `RenderType.entityCutoutNoCull(texture, false)` (from `EntityModel` base class), which enables `LIGHTMAP`.

### 5.4 White Overlay Progress & Overlay Coords

```java
// LivingEntityRenderer.java:231-233
protected float getWhiteOverlayProgress(T livingEntity, float partialTicks) {
    return 0.0F;
}

// LivingEntityRenderer.java:154-156
public static int getOverlayCoords(LivingEntity livingEntity, float u) {
    return OverlayTexture.pack(
        OverlayTexture.u(u),
        OverlayTexture.v(livingEntity.hurtTime > 0 || livingEntity.deathTime > 0));
}
```

- Overridden in `CreeperRenderer.java:33` to return the creeper's white flash.
- `OverlayTexture.pack(u, v) = u | v << 16`.
- Overlay white‑flash is applied via `mix(overlayColor.rgb, color.rgb, overlayColor.a)` in the fragment shader (see §2.4).

### 5.5 MobRenderer

**File**: `net/minecraft/client/renderer/entity/MobRenderer.java` (22 lines)

`MobRenderer` is a thin subclass of `LivingEntityRenderer` with no lighting overrides. It inherits the full lighting pipeline from `EntityRenderer`.

### 5.6 Leash Rendering

```java
// EntityRenderer.java:105-173
private <E extends Entity> void renderLeash(...) {
    // ...
    int i = this.getBlockLightLevel(entity, blockpos);
    int j = this.entityRenderDispatcher.getRenderer(leashHolder)
                .getBlockLightLevel(leashHolder, blockpos1);
    int k = entity.level().getBrightness(LightLayer.SKY, blockpos);
    int l = entity.level().getBrightness(LightLayer.SKY, blockpos1);

    for (int i1 = 0; i1 <= 24; i1++) {
        float f = (float)i1 / 24.0F;
        int blockLight = (int)Mth.lerp(f, (float)i, (float)j);
        int skyLight   = (int)Mth.lerp(f, (float)k, (float)l);
        int packedLight = LightTexture.pack(blockLight, skyLight);
        // ... vertex setup with .setLight(packedLight)
    }
}
```

- Leash light is linearly interpolated between entity and leash holder along the 24 segments.

---

## 6. Gamma & Tonemap

### 6.1 Options.gamma

**File**: `net/minecraft/client/Options.java:673-680`

```java
private final OptionInstance<Double> gamma = new OptionInstance<>(
    "options.gamma",
    OptionInstance.noTooltip(),
    // ...
    (p_231913_, p_231914_) -> {
        if (p_231914_ == 0.0) return genericValueLabel(p_231913_,
            Component.translatable("options.gamma.min"));
        if (p_231914_ == 0.5) return genericValueLabel(p_231913_,
            Component.translatable("options.gamma.default"));
        return p_231914_ == 1.0
            ? genericValueLabel(p_231913_,
                Component.translatable("options.gamma.max"))
            : genericValueLabel(p_231913_, p_231914_);
    },
    // ...
);
```

- Range: `0.0` (dark/min) to `1.0` (bright/max), default `0.5`.
- Accessed via `this.minecraft.options.gamma().get().floatValue()`.

### 6.2 Gamma Application in LightTexture

```java
// LightTexture.java:151-153
float f14 = this.minecraft.options.gamma().get().floatValue();
Vector3f vector3f4 = new Vector3f(
    this.notGamma(vector3f1.x),
    this.notGamma(vector3f1.y),
    this.notGamma(vector3f1.z));
vector3f1.lerp(vector3f4, Math.max(0.0F, f14 - f3));
```

Where `f3` = `getDarknessGamma(partialTicks) * darknessEffectScale` (0.0 if no Darkness effect).

**Effective lerp factor**: `gamma - darknessGamma` (clamped to 0 at minimum).

The `notGamma` function:

```java
// LightTexture.java:175-178
private float notGamma(float value) {
    float f = 1.0F - value;
    return 1.0F - f * f * f * f;
}
```

This is the inverse of a power‑4 gamma: `inverseGamma(v) = 1 - (1 - v)^4`. When `gamma=1.0` (max brightness), the color is fully pushed toward this inverse curve, brightening shadows. When `gamma=0.0`, the original `vector3f1` values dominate.

### 6.3 Final Post‑Gamma Lerp

```java
// LightTexture.java:154
vector3f1.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
```

A light gray tint is always weakly blended in (4%), done after gamma to keep the renderer from going fully black.

---

## 7. Dimension‑Specific Behavior

### 7.1 `DimensionType.hasSkyLight`

**File**: `net/minecraft/world/level/dimension/DimensionType.java:30` (record component)

| Dimension | `hasSkyLight` | `hasCeiling` | `ambientLight` |
|---|---|---|---|
| Overworld | `true` | `false` | `0.0` |
| Nether | `false` | `true` | `0.1` |
| End | `false` | `false` | `0.0` |

- Nether: `hasSkyLight = false` → the sky light engine is **inactive**; `level.getBrightness(LightLayer.SKY, pos)` always returns 0.
- End: `hasSkyLight = false` → no sky light; but `forceBrightLightmap` compensates (see below).

### 7.2 Force Bright Lightmap (End)

**File**: `net/minecraft/client/renderer/DimensionSpecialEffects.java:86-107`

```java
public static class EndEffects extends DimensionSpecialEffects {
    public EndEffects() {
        super(Float.NaN, false, SkyType.END, true, false);
        //                                          ^^^^ forceBrightLightmap = true
    }
}
```

When `forceBrightLightmap()` is `true`, the lightmap generation skips the normal sky/block combination and instead lerps toward `(0.99, 1.12, 1.0)`:

```java
// LightTexture.java:117-129
boolean flag = clientlevel.effects().forceBrightLightmap();
if (flag) {
    vector3f1.lerp(new Vector3f(0.99F, 1.12F, 1.0F), 0.25F);
    clampColor(vector3f1);
} else {
    // Normal path: sky + block light combination
}
```

This gives the End its perpetually bright‑but‑slightly‑warm lighting.

### 7.3 Constant Ambient Light (Nether)

**File**: `net/minecraft/client/renderer/DimensionSpecialEffects.java:110-124`

```java
public static class NetherEffects extends DimensionSpecialEffects {
    public NetherEffects() {
        super(Float.NaN, true, SkyType.NONE, false, true);
        //                                              ^^^^ constantAmbientLight = true
    }
}
```

- `constantAmbientLight = true` is used by the fog renderer to skip time‑of‑day fog color variation.
- Nether has `ambientLight = 0.1` in `DimensionType`, so the `getBrightness` function (`LightTexture.java:180-184`) starts from a base of 0.1 even at block light level 0.

### 7.4 Sky Light Engine for Nether/End

Since `hasSkyLight = false` in both Nether and End:

- `Level.getBrightness(LightLayer.SKY, pos)` returns 0 for all positions.
- `LevelRenderer.getLightColor` returns `0 | blockLight<<4` (since `i = 0`).
- `EntityRenderer.getSkyLightLevel` returns 0.

However, for the End, `forceBrightLightmap` overrides the entire lightmap to be near‑fullbright, making sky darken moot. For the Nether, the sky darken factor `f` (from `ClientLevel.getSkyDarken`) still varies, but the `hasCeiling=true` flag blocks direct sky access visually.

### 7.5 `ClientLevel.effects().adjustLightmapColors()`

```java
// LightTexture.java:132
clientlevel.effects().adjustLightmapColors(
    clientlevel, partialTicks, f, f7, f8, j, i, vector3f1);
```

This is a NeoForge extension point (`IDimensionSpecialEffectsExtension`) that allows dimension‑specific modifications to each lightmap texel after normal computation. The vanilla built‑in effects (Overworld/Nether/End) don't add extra adjustments here — it's purely a hook for mods.

### 7.6 LevelReader.getMaxLocalRawBrightness

**File**: `net/minecraft/world/level/LevelReader.java:176-184`

```java
default int getMaxLocalRawBrightness(BlockPos pos) {
    return this.getMaxLocalRawBrightness(pos, this.getSkyDarken());
}

default int getMaxLocalRawBrightness(BlockPos pos, int amount) {
    return pos.getX() >= -30000000 && pos.getZ() >= -30000000
        && pos.getX() < 30000000 && pos.getZ() < 30000000
        ? this.getRawBrightness(pos, amount)
        : 15;
}
```

- `getRawBrightness` is implemented by `Level` to combine block light + max(sky light - skyDarken, 0).
- Used by `LightDebugRenderer`, mob spawning checks, and `ScreenEffectRenderer`.

---

## Summary of Key File Paths (relative to `.local_ref/mc/1.21.1/sources/`)

| Component | File | Lines |
|---|---|---|
| LightTexture | `net/minecraft/client/renderer/LightTexture.java` | 1‑197 |
| LevelRenderer.getLightColor | `net/minecraft/client/renderer/LevelRenderer.java` | 3624‑3641 |
| EntityRenderer | `net/minecraft/client/renderer/entity/EntityRenderer.java` | 1‑219 |
| LivingEntityRenderer | `net/minecraft/client/renderer/entity/LivingEntityRenderer.java` | 1‑285 |
| MobRenderer | `net/minecraft/client/renderer/entity/MobRenderer.java` | 1‑22 |
| RenderType (LIGHTMAP entries) | `net/minecraft/client/renderer/RenderType.java` | 26‑579 |
| RenderStateShard (LightmapStateShard) | `net/minecraft/client/renderer/RenderStateShard.java` | 473‑485 |
| DefaultVertexFormat | `com/mojang/blaze3d/vertex/DefaultVertexFormat.java` | 1‑76 |
| OverlayTexture | `net/minecraft/client/renderer/texture/OverlayTexture.java` | 1‑65 |
| DimensionType | `net/minecraft/world/level/dimension/DimensionType.java` | 1‑205 |
| DimensionSpecialEffects | `net/minecraft/client/renderer/DimensionSpecialEffects.java` | 1‑151 |
| BlockAndTintGetter.getBrightness | `net/minecraft/world/level/BlockAndTintGetter.java` | 14‑16 |
| LightLayer | `net/minecraft/world/level/LightLayer.java` | 1‑6 |
| ClientLevel.getSkyDarken | `net/minecraft/client/multiplayer/ClientLevel.java` | 766‑774 |
| GameRenderer.getNightVisionScale | `net/minecraft/client/renderer/GameRenderer.java` | 999‑1001 |
| Options.gamma | `net/minecraft/client/Options.java` | 673‑680 |
| NeoForge unlit shader (vertex) | `assets/neoforge/shaders/core/rendertype_entity_unlit_translucent.vsh` | 1‑32 |
| NeoForge unlit shader (fragment) | `assets/neoforge/shaders/core/rendertype_entity_unlit_translucent.fsh` | 1‑30 |

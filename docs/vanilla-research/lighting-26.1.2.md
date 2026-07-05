# Minecraft 26.1.2 (NeoForge) Vanilla Lighting Model Analysis

> Report generated from sources at `.local_ref/mc/26.1.2/sources/` 源码树由 `scripts/extract-mc-source.py` 重建。

---

## Table of Contents

1. [Lightmap.java](#1-lightmapjava)
2. [Brightness.java](#2-brightnessjava)
3. [LightCoordsUtil.java](#3-lightcoordsutiljava)
4. [LightmapRenderStateExtractor.java](#4-lightmaprenderstateextractorjava)
5. [LightmapRenderState.java](#5-lightmaprenderstatejava)
6. [UiLightmap.java](#6-uilightmapjava)
7. [Render Pipeline Integration](#7-render-pipeline-integration)
8. [RenderType.java (rendertype package)](#8-rendertypejava-rendertype-package)
9. [LevelRenderer — Light Querying](#9-levelrenderer--light-querying)
10. [EntityRenderer — Entity Lighting](#10-entityrenderer--entity-lighting)
11. [Gamma / Darkness / Night Vision Flow](#11-gamma--darkness--night-vision-flow)

---

## 1. Lightmap.java

**Path:** `net/minecraft/client/renderer/Lightmap.java` (97 lines)

### Class Structure

`Lightmap` is `public class Lightmap implements AutoCloseable`. It owns:
- A **16×16 RGBA8 GPU texture** (`GpuTexture`) — the actual lightmap texture
- A **GPU texture view** into that texture
- A **ring buffer (UBO)** for uniform data, sized to hold 10 fields (see below)

```java
// file:3:23-47
public class Lightmap implements AutoCloseable {
    public static final int TEXTURE_SIZE = 16;
    private static final int LIGHTMAP_UBO_SIZE = new Std140SizeCalculator()
        .putFloat().putFloat().putFloat().putFloat().putFloat()
        .putFloat().putVec3().putVec3().putVec3().putVec3()
        .get();
    private final GpuTexture texture;
    private final GpuTextureView textureView;
    private final MappableRingBuffer ubo;

    public Lightmap() {
        GpuDevice device = RenderSystem.getDevice();
        this.texture = device.createTexture("Lightmap", 13, TextureFormat.RGBA8, 16, 16, 1, 1);
        this.textureView = device.createTextureView(this.texture);
        device.createCommandEncoder().clearColorTexture(this.texture, -1);
        this.ubo = new MappableRingBuffer(() -> "Lightmap UBO", 130, LIGHTMAP_UBO_SIZE);
    }
```

### Texture Generation — `render()` method

**The lightmap texture is now GPU-generated**, not CPU-updated pixel-by-pixel like the old `LightTexture.java`. The `render()` method writes a UBO with all the state scalars/colors, then draws a **fullscreen triangle** through the `pipeline/lightmap` pipeline, whose fragment shader (`core/lightmap`) computes the actual 16×16 texture content on the GPU.

Key method:

```java
// file:3:60-89
public void render(LightmapRenderState renderState) {
    if (renderState.needsUpdate) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("lightmap");
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView view = commandEncoder.mapBuffer(this.ubo.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(view.data())
                .putFloat(renderState.skyFactor)
                .putFloat(renderState.blockFactor)
                .putFloat(renderState.nightVisionEffectIntensity)
                .putFloat(renderState.darknessEffectScale)
                .putFloat(renderState.bossOverlayWorldDarkening)
                .putFloat(renderState.brightness)
                .putVec3(renderState.blockLightTint)
                .putVec3(renderState.skyLightColor)
                .putVec3(renderState.ambientColor)
                .putVec3(renderState.nightVisionColor);
        }

        try (RenderPass renderPass = commandEncoder.createRenderPass(
                () -> "Update light", this.textureView, OptionalInt.empty())) {
            renderPass.setPipeline(RenderPipelines.LIGHTMAP);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("LightmapInfo", this.ubo.currentBuffer());
            renderPass.draw(0, 3);
        }

        this.ubo.rotate();
        profiler.pop();
    }
}
```

The UBO `LightmapInfo` is a Std140 block containing the following fields (in order):

| Offset | Type | Source field |
|--------|------|-------------|
| float | skyFactor | World sky light intensity |
| float | blockFactor | Block light flicker + 1.4 |
| float | nightVisionEffectIntensity | Night vision blend factor |
| float | darknessEffectScale | Darkness effect animation |
| float | bossOverlayWorldDarkening | Boss fog darkening |
| float | brightness | Gamma option minus darkness modifier |
| vec3 | blockLightTint | RGB tint for block light |
| vec3 | skyLightColor | RGB color for sky light |
| vec3 | ambientColor | Ambient light color |
| vec3 | nightVisionColor | Night vision color tint |

### The brightness formula (static utility)

```java
// file:3:92-96
public static float getBrightness(DimensionType dimensionType, int level) {
    float v = level / 15.0F;
    float curvedV = v / (4.0F - 3.0F * v);
    return Mth.lerp(dimensionType.ambientLight(), curvedV, 1.0F);
}
```

This is used by shadow rendering in `EntityRenderer.extractShadowPiece()` — not for the lightmap itself.

### GPU-driven generation: the key difference

The `/pipeline/lightmap` pipeline uses:
- **Vertex shader:** `core/screenquad` (two-triangle fullscreen quad)
- **Fragment shader:** `core/lightmap` (generates the 16×16 texture)
- **Uniform:** `LightmapInfo` (the UBO with all state)

This means the 16×16 texture is now **entirely computed on the GPU** — the old `LightTexture.java` in 1.20.1/1.21.1 wrote pixel data to a `NativeImage` on the CPU via nested loops over 16 block-light × 16 sky-light values, applying gamma, night vision, and darkness in Java/ScalableVector. In 26.1.2, all this math moved to the fragment shader `core/lightmap.glsl`.

---

## 2. Brightness.java

**Path:** `net/minecraft/util/Brightness.java` (21 lines)

### Record definition

```java
// file:6:6-21
public record Brightness(int block, int sky) {
    public static final Codec<Integer> LIGHT_VALUE_CODEC = ExtraCodecs.intRange(0, 15);
    public static final Codec<Brightness> CODEC = RecordCodecBuilder.create(
        i -> i.group(
            LIGHT_VALUE_CODEC.fieldOf("block").forGetter(Brightness::block),
            LIGHT_VALUE_CODEC.fieldOf("sky").forGetter(Brightness::sky)
        ).apply(i, Brightness::new)
    );
    public static final Brightness FULL_BRIGHT = new Brightness(15, 15);

    public int pack() {
        return LightCoordsUtil.pack(this.block, this.sky);
    }

    public static Brightness unpack(int packed) {
        return new Brightness(LightCoordsUtil.block(packed), LightCoordsUtil.sky(packed));
    }
}
```

### Key observations

- **`Brightness` is a `record`** with two `int` fields: `block` (0–15) and `sky` (0–15).
- **`FULL_BRIGHT`** is `new Brightness(15, 15)`, which packs to `LightCoordsUtil.FULL_BRIGHT = 15728880` (= `0xF000F0`).
- **`pack()`** delegates to `LightCoordsUtil.pack(block, sky)`.
- **`unpack(int)`** creates a `Brightness` from a packed int — it's the inverse operation.
- It has a full **Codec** for JSON serialization/deserialization.
- **This replaces `LightTexture.pack()` / `LightTexture.FULL_BRIGHT`** from old versions, but **`Brightness` is NOT used universally yet** — most internal rendering still passes around raw `int` packed values and uses `LightCoordsUtil` directly.

---

## 3. LightCoordsUtil.java

**Path:** `net/minecraft/util/LightCoordsUtil.java` (96 lines)

### Utility class (static methods only)

```java
// file:3:3-18, 24-34
public class LightCoordsUtil {
    public static final int FULL_BRIGHT = 15728880;    // 0xF000F0
    public static final int FULL_SKY   = 15728640;     // 0xF00000
    private static final int MAX_SMOOTH_LIGHT_LEVEL = 240;

    public static int pack(int block, int sky) {
        return block << 4 | sky << 20;
    }

    public static int block(int packed) {
        return packed >> 4 & 15;
    }

    public static int sky(int packed) {
        return packed >> 20 & 15;
    }

    public static int withBlock(int coords, int block) {
        return coords & 0xFF0000 | block << 4;
    }

    public static int smoothPack(int block, int sky) {
        return block & 0xFF | (sky & 0xFF) << 16;
    }

    public static int smoothBlock(int packed) {
        return packed & 0xFF;
    }

    public static int smoothSky(int packed) {
        return packed >> 16 & 0xFF;
    }
```

### Bit layout

| Bits | Contents |
|------|----------|
| 3:0 (lower nibble) | Unused in packed, but 7:4 = block (4-bit) |
| 7:4 | Block light (0–15), shifted: `block << 4` |
| 23:20 | Sky light (0–15), shifted: `sky << 20` |
| other bits | Unused |

So `FULL_BRIGHT` = `(15 << 4) | (15 << 20)` = `240 | 15728640` = `15728880`.

The `smooth*` variants work with 0–255 (8-bit per channel) expanded values:

| Bits | Contents |
|------|----------|
| 7:0 | Smooth block (0–240) |
| 23:16 | Smooth sky (0–240) |

### Additional operations

- **`lightCoordsWithEmission(int, int)`**: Takes max of block/sky with an emission value — used for emissive rendering.
- **`smoothBlend`** / **`smoothWeightedBlend`**: 4-way averaging of smooth light values for smooth lighting across block faces.
- **`addSmoothBlockEmission`**: Adds emissive contribution (clamped to 240) to smooth block light.
- **`max(int, int)`**: Per-component max of two packed values.

---

## 4. LightmapRenderStateExtractor.java

**Path:** `net/minecraft/client/renderer/LightmapRenderStateExtractor.java` (93 lines)

### Class overview

```java
// file:22:22-93
public class LightmapRenderStateExtractor {
    public static final Vector3fc WHITE = new Vector3f(1.0F, 1.0F, 1.0F);
    private boolean needsUpdate;
    private final GameRenderer renderer;
    private final Minecraft minecraft;
    private final RandomSource randomSource = RandomSource.create();
    private float blockLightFlicker;
```

### `tick()` — frame-to-frame flicker

```java
// file:35:35-40
public void tick() {
    this.blockLightFlicker = this.blockLightFlicker
        + (this.randomSource.nextFloat() - this.randomSource.nextFloat())
        * this.randomSource.nextFloat() * this.randomSource.nextFloat() * 0.1F;
    this.blockLightFlicker *= 0.9F;
    this.needsUpdate = true;
}
```

Simulates block light flicker with a decaying random walk. Called every tick in `GameRenderer.tick()`.

### `extract()` — fills the LightmapRenderState

This is where all the real computation happens (lines 47–92):

```java
// file:47:47-92
public void extract(LightmapRenderState renderState, float partialTicks) {
    renderState.needsUpdate = this.needsUpdate;
    if (this.needsUpdate) {
        ClientLevel level = this.minecraft.level;
        LocalPlayer player = this.minecraft.player;
        if (level != null && player != null) {
            Camera camera = this.renderer.getMainCamera();

            // Block light: flicker + 1.4 base
            renderState.blockFactor = this.blockLightFlicker + 1.4F;
            renderState.blockLightTint = ARGB.vector3fFromRGB24(
                camera.attributeProbe().getValue(EnvironmentAttributes.BLOCK_LIGHT_TINT, partialTicks));

            // Sky light: from EnvironmentAttributes
            renderState.skyFactor = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partialTicks);
            renderState.skyLightColor = ARGB.vector3fFromRGB24(
                camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, partialTicks));

            // End flash (lightning) override
            EndFlashState endFlashState = level.endFlashState();
            if (endFlashState != null && !this.minecraft.options.hideLightningFlash().get()) {
                float intensity = endFlashState.getIntensity(partialTicks);
                if (this.minecraft.gui.getBossOverlay().shouldCreateWorldFog()) {
                    renderState.skyFactor += intensity / 3.0F;
                } else {
                    renderState.skyFactor += intensity;
                }
            }

            // Ambient color & brightness
            renderState.ambientColor = ARGB.vector3fFromRGB24(
                camera.attributeProbe().getValue(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, partialTicks));

            // Gamma brightness (from Options) with darkness effect subtraction
            float brightnessOption = this.minecraft.options.gamma().get().floatValue();
            float darknessEffectScaleOption = this.minecraft.options.darknessEffectScale().get().floatValue();
            float darknessEffectBrightnessModifier = player.getEffectBlendFactor(MobEffects.DARKNESS, partialTicks)
                * darknessEffectScaleOption;
            renderState.brightness = Math.max(0.0F, brightnessOption - darknessEffectBrightnessModifier);

            // Darkness visual effect (cos wave)
            renderState.darknessEffectScale = this.calculateDarknessScale(player, darknessEffectBrightnessModifier, partialTicks)
                * darknessEffectScaleOption;

            // Night vision
            float waterVision = player.getWaterVision();
            if (player.hasEffect(MobEffects.NIGHT_VISION)) {
                renderState.nightVisionEffectIntensity = GameRenderer.getNightVisionScale(player, partialTicks);
            } else if (waterVision > 0.0F && player.hasEffect(MobEffects.CONDUIT_POWER)) {
                renderState.nightVisionEffectIntensity = waterVision;
            } else {
                renderState.nightVisionEffectIntensity = 0.0F;
            }

            renderState.nightVisionColor = ARGB.vector3fFromRGB24(
                camera.attributeProbe().getValue(EnvironmentAttributes.NIGHT_VISION_COLOR, partialTicks));
            renderState.bossOverlayWorldDarkening = this.renderer.getBossOverlayWorldDarkening(partialTicks);

            this.needsUpdate = false;
        }
    }
}
```

### The `calculateDarknessScale` method

```java
// file:42:42-45
private float calculateDarknessScale(LivingEntity camera, float darknessGamma, float partialTickTime) {
    float darkness = 0.45F * darknessGamma;
    return Math.max(0.0F, Mth.cos((camera.tickCount - partialTickTime)
        * (float) Math.PI * 0.025F) * darkness);
}
```

Produces a pulsing cosine wave multiplied by darkness intensity — the visual "heartbeat" effect of the Darkness mob effect.

---

## 5. LightmapRenderState.java

**Path:** `net/minecraft/client/renderer/state/LightmapRenderState.java` (21 lines)

This is a **mutable data holder** (not a record) used to pass data between the extractor and the renderer:

```java
// file:9:9-21
public class LightmapRenderState {
    public boolean needsUpdate = false;
    public float blockFactor;
    public Vector3fc blockLightTint = LightmapRenderStateExtractor.WHITE;
    public float skyFactor;
    public Vector3fc skyLightColor = LightmapRenderStateExtractor.WHITE;
    public Vector3fc ambientColor = LightmapRenderStateExtractor.WHITE;
    public float brightness;
    public float darknessEffectScale;
    public float nightVisionEffectIntensity;
    public Vector3fc nightVisionColor = LightmapRenderStateExtractor.WHITE;
    public float bossOverlayWorldDarkening;
}
```

Owned by `GameRenderState`:

```java
// file:GameRenderState.java:11:11
public final LightmapRenderState lightmapRenderState = new LightmapRenderState();
```

---

## 6. UiLightmap.java

**Path:** `net/minecraft/client/renderer/UiLightmap.java` (27 lines)

This is a trivial class for UI rendering:

```java
// file:10:10-27
public class UiLightmap implements Auto.Closeable {
    private final DynamicTexture texture = new DynamicTexture("UI Lightmap", 1, 1, false);

    public UiLightmap() {
        NativeImage pixels = this.texture.getPixels();
        pixels.setPixel(0, 0, -1);  // white pixel
        this.texture.upload();
    }

    public GpuTextureView getTextureView() {
        return this.texture.getTextureView();
    }
}
```

A **1×1 white texture** (`-1` = `0xFFFFFFFF`) replaces the lightmap during GUI rendering. This effectively disables lightmap modulation for UI elements — they render at full brightness using just the vertex color.

**Toggle mechanism** in `GameRenderer.java`:

```java
// file:GameRenderer.java:836-842
public GpuTextureView lightmap() {
    return this.useUiLightmap ? this.uiLightmap.getTextureView() : this.lightmap.getTextureView();
}

public GpuTextureView levelLightmap() {
    return this.lightmap.getTextureView();
}
```

The flag `useUiLightmap` is set `true` before `guiRenderer.render()` and `false` after.

---

## 7. Render Pipeline Integration

### 7.1 The LIGHTMAP pipeline

Defined in `RenderPipelines.java` (lines 739–747):

```java
// file:RenderPipelines.java:739-747
public static final RenderPipeline LIGHTMAP = register(
    RenderPipeline.builder()
        .withLocation("pipeline/lightmap")
        .withVertexShader("core/screenquad")
        .withFragmentShader("core/lightmap")
        .withUniform("LightmapInfo", UniformType.UNIFORM_BUFFER)
        .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
        .build()
);
```

This renders with no input vertices (drives its own built-in fullscreen triangle), uses the `LightmapInfo` uniform (the UBO written by `Lightmap.render()`), and outputs to the 16×16 lightmap texture.

### 7.2 How `Sampler2` binds the lightmap

The lightmap texture (or UI lightmap) is bound as `Sampler2` — referenced throughout the pipeline system:

**In `RenderSetup.getTextures()`** (`rendertype/RenderSetup.java`, lines 98–104):

```java
// file:RenderSetup.java:98-104
if (this.useLightmap) {
    result.put(
        "Sampler2",
        new RenderSetup.TextureAndSampler(
            Minecraft.getInstance().gameRenderer.lightmap(),
            RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
        )
    );
}
```

**In chunk rendering** (`ChunkSectionsToRender.java` line 50):

```java
renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightmap(),
    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
```

**In particle rendering** (`ParticleFeatureRenderer.java` line 91):

```java
renderPass.bindTexture("Sampler2", Minecraft.getInstance().gameRenderer.lightmap(),
    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
```

### 7.3 Where Sampler2 appears in pipelines

The `Sampler2` sampler is declared on all world-facing pipeline snippets:

| Snippet | Samplers | Used by |
|---------|----------|---------|
| `GENERIC_BLOCKS_SNIPPET` | Sampler0, Sampler2 | All block/terrain pipelines |
| `ENTITY_SNIPPET` | Sampler0, Sampler2 | All entity/armor/item pipelines |
| `ITEM_SNIPPET` | Sampler0, Sampler2 | ITEM_CUTOUT, ITEM_TRANSLUCENT |
| `PARTICLE_SNIPPET` | Sampler0, Sampler2 | PARTICLES, WEATHER |
| `TEXT` / `TEXT_INTENSITY` | Sampler0, Sampler2 | All text rendering |
| `LEASH` | Sampler2 | Leash rendering |
| `TEXT_BACKGROUND` | Sampler2 | Text background |

### 7.4 DynamicUniforms & GlobalSettingsUniform — no lightmap data

- **`DynamicUniforms.java`**: Contains `Transform` (modelView, colorModulator, modelOffset, textureMatrix) and `ChunkSectionInfo` (modelView, position, visibility, atlas size). **No lightmap-related uniforms.**
- **`GlobalSettingsUniform.java`**: Contains camera position, viewport size, glint alpha, game time, menu blur, RGSS flag. **No lightmap-related uniforms.**

The lightmap data flows **exclusively through the `LightmapInfo` UBO** (Lightmap.java) for generating the texture, and then the **texture is sampled in shaders via `Sampler2`** with vertex attribute `UV2` as texture coordinates.

### 7.5 Vertex format still has UV2

The `UV2` vertex attribute (representing packed block+sky light) is **still present** in all vertex formats:

```java
// file:DefaultVertexFormat.java:9-14
public static final VertexFormat BLOCK = VertexFormat.builder()
    .add("Position", VertexFormatElement.POSITION)
    .add("Color", VertexFormatElement.COLOR)
    .add("UV0", VertexFormatElement.UV0)
    .add("UV2", VertexFormatElement.UV2)           // <-- lightmap coords
    .build();
```

```java
// file:DefaultVertexFormat.java:15-23
public static final VertexFormat ENTITY = VertexFormat.builder()
    .add("Position", VertexFormatElement.POSITION)
    .add("Color", VertexFormatElement.COLOR)
    .add("UV0", VertexFormatElement.UV0)
    .add("UV1", VertexFormatElement.UV1)           // <-- overlay coords
    .add("UV2", VertexFormatElement.UV2)           // <-- lightmap coords
    .add("Normal", VertexFormatElement.NORMAL)
    .padding(1)
    .build();
```

And `UV2` is defined as:

```java
// file:VertexFormatElement.java:20
public static final VertexFormatElement UV2 = register(4, 2, VertexFormatElement.Type.SHORT, false, 2);
```

Two `SHORT` values, used as normalized texture coordinates into the 16×16 lightmap texture.

### 7.6 `RenderSystem.bindDefaultUniforms()` — what it provides

```java
// file:RenderSystem.java:283-303
public static void bindDefaultUniforms(RenderPass renderPass) {
    // Projection UBO → "Projection"
    // Fog UBO → "Fog"
    // GlobalSettings UBO → "Globals"
    // Lighting UBO → "Lighting"
}
```

Note: There is **no "Lightmap" uniform bound by default**. The `LightmapInfo` uniform is bound *only* during the lightmap texture generation pass:
```java
renderPass.setUniform("LightmapInfo", this.ubo.currentBuffer());
```

And during regular rendering, the lightmap is just a 2D texture sampler (`Sampler2`) with the generated 16×16 texture.

---

## 8. RenderType.java (rendertype package)

**Path:** `net/minecraft/client/renderer/rendertype/RenderType.java` (170 lines)

### Not an enum — a regular class

`RenderType` is now a **regular class** wrapping a `RenderSetup`:

```java
// file:26:26-41
public class RenderType {
    private final RenderSetup state;
    private final Optional<RenderType> outline;
    protected final String name;

    private RenderType(String name, RenderSetup state) {
        this.name = name;
        this.state = state;
        this.outline = state.outlineProperty == RenderSetup.OutlineProperty.AFFECTS_OUTLINE
            ? state.textures.values().stream().findFirst()
                .map(texture -> RenderTypes.OUTLINE.apply(texture.location(), state.pipeline.isCull()))
            : Optional.empty();
    }

    public static RenderType create(String name, RenderSetup state) {
        return new RenderType(name, state);
    }
```

Each instance stores:
- `name` (String) — debug label
- `state` (RenderSetup) — all the render configuration
- `outline` (Optional<RenderType>) — auto-created outline rendering pass

### Relationship with RenderPipelines

Each `RenderType` points to a **`RenderPipeline`** via `this.state.pipeline`. The accessor is:

```java
// file:155:155-157
public RenderPipeline pipeline() {
    return this.state.pipeline;
}
```

### How a RenderType is constructed (example from RenderTypes.java)

```java
// file:RenderTypes.java:62-73
private static final Function<Identifier, RenderType> ENTITY_SOLID = Util.memoize(
    texture -> {
        RenderSetup state = RenderSetup.builder(RenderPipelines.ENTITY_SOLID)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup();
        return RenderType.create("entity_solid", state);
    }
);
```

### Named RenderType constants (all from RenderTypes.java, ~663 lines)

| Constant/Factory method | Pipeline | Lightmap? | Overlay? |
|------------------------|----------|-----------|----------|
| `solidMovingBlock()` | SOLID_BLOCK | Yes | No |
| `cutoutMovingBlock()` | CUTOUT_BLOCK | Yes | No |
| `translucentMovingBlock()` | TRANSLUCENT_BLOCK | Yes | No |
| `armorCutoutNoCull(texture)` | ARMOR_CUTOUT_NO_CULL | Yes | Yes |
| `armorTranslucent(texture)` | ARMOR_TRANSLUCENT | Yes | Yes |
| `entitySolid(texture)` | ENTITY_SOLID | Yes | Yes |
| `entityCutout(texture)` | ENTITY_CUTOUT | Yes | Yes |
| `entityTranslucent(texture)` | ENTITY_TRANSLUCENT | Yes | Yes |
| `entityTranslucentEmissive(texture)` | ENTITY_TRANSLUCENT_EMISSIVE | No | Yes |
| `itemCutout(texture)` | ITEM_CUTOUT | Yes | No |
| `itemTranslucent(texture)` | ITEM_TRANSLUCENT | Yes | No |
| `eyes(texture)` | EYES | No | No |
| `beaconBeam(texture)` | BEACON_BEAM_* | No | No |
| `bannerPattern(texture)` | BANNER_PATTERN | Yes | No |
| `text(texture)` | TEXT | Yes | No |
| `textIntensity(texture)` | TEXT_INTENSITY | Yes | No |
| `leash()` | LEASH | Yes | No |
| `lines()` | LINES | No | No |
| `lightning()` | LIGHTNING | No | No |
| `endPortal()` / `endGateway()` | END_PORTAL / END_GATEWAY | No | No |

### How lightmap is used per RenderType

Each RenderType opts into lightmap via:
```java
RenderSetup.builder(pipeline).useLightmap()
```

When `useLightmap` is true, `RenderSetup.getTextures()` binds the current lightmap texture view to `Sampler2`. When false, Sampler2 is not bound and the shader must handle its absence.

### The `draw()` method

`RenderType.draw(MeshData)` immediately submits a draw call:

```java
// file:60:60-133
public void draw(MeshData mesh) {
    // ... pushes model view modifier if needed
    GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
        .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            new Vector3f(), this.state.textureTransform.getMatrix());
    Map<String, RenderSetup.TextureAndSampler> textures = this.state.getTextures();
    // ... uploads vertex/index buffers
    // ... creates render pass, sets pipeline, binds textures
    for (Entry<String, RenderSetup.TextureAndSampler> entry : textures.entrySet()) {
        renderPass.bindTexture(entry.getKey(), entry.getValue().textureView(), entry.getValue().sampler());
    }
    // ... draws indexed
}
```

---

## 9. LevelRenderer — Light Querying

**Path:** `net/minecraft/client/renderer/LevelRenderer.java`

### `getLightCoords()` — the world-level light lookup

```java
// file:LevelRenderer.java:1529-1541
public static int getLightCoords(BlockAndLightGetter level, BlockPos pos) {
    return getLightCoords(LevelRenderer.BrightnessGetter.DEFAULT, level, level.getBlockState(pos), pos);
}

public static int getLightCoords(LevelRenderer.BrightnessGetter brightnessGetter,
        BlockAndLightGetter level, BlockState state, BlockPos pos) {
    if (state.emissiveRendering(level, pos)) {
        return 15728880;                                // FULL_BRIGHT
    } else {
        int packedBrightness = brightnessGetter.packedBrightness(level, pos);
        int block = LightCoordsUtil.block(packedBrightness);
        int blockSelfEmission = state.getLightEmission(level, pos);
        return block < blockSelfEmission
            ? LightCoordsUtil.withBlock(packedBrightness, blockSelfEmission)
            : packedBrightness;
    }
}
```

Returns packed `int` (`LightCoordsUtil.pack` format), NOT `Brightness` record.

### `BrightnessGetter` — functional interface

```java
// file:LevelRenderer.java:1628-1636
@FunctionalInterface
public interface BrightnessGetter {
    LevelRenderer.BrightnessGetter DEFAULT = (level, pos) -> {
        int sky = level.getBrightness(LightLayer.SKY, pos);
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        return LightCoordsUtil.pack(block, sky);
    };

    int packedBrightness(BlockAndLightGetter level, BlockPos pos);
}
```

### Note: `Brightness` record is NOT used here

Despite the new `Brightness` record existing, **`LevelRenderer` still works with raw `int` packed values** through `LightCoordsUtil`. The `Brightness` record appears to be used mainly for serialization (via its Codec) and as a named type in APIs where intentional data transfer matters.

---

## 10. EntityRenderer — Entity Lighting

**Path:** `net/minecraft/client/renderer/entity/EntityRenderer.java`

### `getPackedLightCoords()` — core entity light method

```java
// file:51:51-54
public final int getPackedLightCoords(T entity, float partialTickTime) {
    BlockPos blockPos = BlockPos.containing(entity.getLightProbePosition(partialTickTime));
    return LightCoordsUtil.pack(
        this.getBlockLightLevel(entity, blockPos),
        this.getSkyLightLevel(entity, blockPos)
    );
}
```

Returns packed `int` (not `Brightness`). Individual light components:

```java
// file:56:56-62
protected int getSkyLightLevel(T entity, BlockPos blockPos) {
    return entity.level().getBrightness(LightLayer.SKY, blockPos);
}

protected int getBlockLightLevel(T entity, BlockPos blockPos) {
    return entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, blockPos);
}
```

### Storage in EntityRenderState

The packed light value is stored in a plain `int` field:

```java
// file:entity/state/EntityRenderState.java:30
public int lightCoords = 15728880;   // defaults to FULL_BRIGHT
```

Set during extraction:

```java
// file:EntityRenderer.java:284
state.lightCoords = this.getPackedLightCoords(entity, partialTicks);
```

### Shadow rendering uses Lightmap.getBrightness()

```java
// file:EntityRenderer.java:339
float alpha = Mth.clamp(powerAtDepth * 0.5F
    * Lightmap.getBrightness(level.dimensionType(), brightness), 0.0F, 1.0F);
```

This uses the static `Lightmap.getBrightness()` method that applies the dimension's ambient light curve to the raw brightness level.

---

## 11. Gamma / Darkness / Night Vision Flow

### Gamma (`Options.gamma`)

```java
// file:Options.java:835-853
private final OptionInstance<Double> gamma = new OptionInstance<>(
    "options.gamma",
    OptionInstance.noTooltip(),
    (caption, value) -> { /* 0=min, 50=default, 100=max */ },
    OptionInstance.UnitDouble.INSTANCE,   // range 0.0–1.0
    0.5,                                   // default = 50%
    value -> {}
);
```

Range: **0.0 to 1.0**, default **0.5**.

### How gamma flows into the lightmap

```java
// file:LightmapRenderStateExtractor.java:71-74
float brightnessOption = this.minecraft.options.gamma().get().floatValue();
float darknessEffectScaleOption = this.minecraft.options.darknessEffectScale().get().floatValue();
float darknessEffectBrightnessModifier = player.getEffectBlendFactor(MobEffects.DARKNESS, partialTicks)
    * darknessEffectScaleOption;
renderState.brightness = Math.max(0.0F, brightnessOption - darknessEffectBrightnessModifier);
```

The `brightness` field of `LightmapRenderState` = `gamma_option - darkness_modifier`, clamped to ≥ 0. This value is then uploaded to the GPU via the `LightmapInfo` UBO and used by the `core/lightmap` fragment shader to compute the final lightmap texture.

### Darkness effect

```java
// file:LightmapRenderStateExtractor.java:42-45
private float calculateDarknessScale(LivingEntity camera, float darknessGamma, float partialTickTime) {
    float darkness = 0.45F * darknessGamma;
    return Math.max(0.0F, Mth.cos((camera.tickCount - partialTickTime)
        * (float) Math.PI * 0.025F) * darkness);
}
```

This creates a cosine wave pulsing at frequency `π × 0.025` per tick (approx 80 ticks per full cycle). The amplitude is `0.45 × darknessGamma`. This is the visual "heartbeat" of the Darkness effect.

### Night vision

```java
// file:LightmapRenderStateExtractor.java:78-84
if (player.hasEffect(MobEffects.NIGHT_VISION)) {
    renderState.nightVisionEffectIntensity = GameRenderer.getNightVisionScale(player, partialTicks);
} else if (waterVision > 0.0F && player.hasEffect(MobEffects.CONDUIT_POWER)) {
    renderState.nightVisionEffectIntensity = waterVision;
} else {
    renderState.nightVisionEffectIntensity = 0.0F;
}
```

Night vision scale:

```java
// file:GameRenderer.java:394-397
public static float getNightVisionScale(LivingEntity camera, float a) {
    MobEffectInstance nightVision = camera.getEffect(MobEffects.NIGHT_VISION);
    return !nightVision.endsWithin(200) ? 1.0F
        : 0.7F + Mth.sin((nightVision.getDuration() - a) * (float) Math.PI * 0.2F) * 0.3F;
}
```

When the effect has more than 200 ticks remaining, intensity is 1.0. When expiring, it oscillates between 0.4 and 1.0 via a sine wave.

### Night vision color & other tints

All tints come from `EnvironmentAttributes` via the camera's `attributeProbe()`:

| Field | EnvironmentAttribute | Default value |
|-------|---------------------|--------------|
| `blockLightTint` | `BLOCK_LIGHT_TINT` | `-10100` (0xFFFFD8CC) |
| `skyLightColor` | `SKY_LIGHT_COLOR` | `-1` (0xFFFFFFFF) |
| `ambientColor` | `AMBIENT_LIGHT_COLOR` | `-16777216` (0xFF000000) |
| `nightVisionColor` | `NIGHT_VISION_COLOR` | `-6710887` (0xFF999999) |

These attributes are **spatially interpolated** and **syncable** — meaning dimensions can define custom light colors through data-driven dimension JSON.

---

## Summary of Key Structural Changes vs. Old LightTexture

| Aspect | Old (1.20.1/1.21.1) | New (26.1.2) |
|--------|---------------------|--------------|
| **Lightmap generation** | CPU: `NativeImage` + nested loop over 16×16 pixels | **GPU**: full-screen triangle through `core/lightmap` shader |
| **Texel update** | `setPixelRGBA()` on NativeImage + `upload()` | Compute via GLSL + `UniformBuffer` (LightmapInfo) |
| **Light data type** | `int` packed via `LightTexture.pack(b,s)` | `int` via `LightCoordsUtil.pack(b,s)` OR `Brightness` record |
| **FULL_BRIGHT constant** | `LightTexture.FULL_BRIGHT = 0xF000F0` | `LightCoordsUtil.FULL_BRIGHT = 15728880` / `Brightness.FULL_BRIGHT` |
| **Gamma application** | In Java: `float g = ...;` loop over pixels | In GLSL: fragment shader consumes `brightness` uniform |
| **Night vision** | Java: `scale` applied per pixel | GLSL: `nightVisionEffectIntensity` × `nightVisionColor` uniform |
| **Darkness effect** | Java: `darknessAlpha` per pixel | GLSL: `darknessEffectScale` uniform |
| **Block/sky tint** | Hardcoded (torch=orange, sky=blue-ish) | **Configurable**: `EnvironmentAttributes.BLOCK_LIGHT_TINT`, `SKY_LIGHT_COLOR`, `AMBIENT_LIGHT_COLOR` |
| **UI lightmap** | Not separate; reused world lightmap | `UiLightmap`: dedicated **1×1 white texture** to disable light modulation |
| **RenderType** | Enum-like with many static inner classes | **Plain class** wrapping `RenderSetup` + `RenderPipeline` |
| **Sampler2 binding** | Implicit in rendering pipeline | Explicit via `RenderSetup.useLightmap()` → auto-binds to `gameRenderer.lightmap()` |
| **Vertex UV2** | Still `Short2` attribute | **Still `Short2` attribute** — unchanged |
| **LightTexture class** | Existed as utility | **Deleted entirely** — replaced by `Lightmap` (GPU owner) + `LightCoordsUtil` (math) + `Brightness` (data) |

### Most surprising structural change

**The lightmap texture is no longer computed on the CPU.** In old versions, `LightTexture.java` would loop over 16 block-light levels × 16 sky-light levels, compute gamma, night vision, darkness, and tint in Java, and write each pixel to a `NativeImage`. In 26.1.2, all of this math moved to a **dedicated fragment shader** (`core/lightmap.glsl`). The Java side only extracts game state into a UBO and triggers the draw call. This is a fundamental shift from CPU-driven to GPU-driven lighting that aligns with the new Render Pipeline architecture.

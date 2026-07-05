# 26.1.2 (NeoForge) Minecraft Vanilla Particle System 分析报告

> 基于 `.local_ref/mc/26.1.2/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [架构总览 — 从 PreparableReloadListener 到 extract/submit 分离](#1-架构总览--从-preparablereloadlistener-到-extractsubmit-分离)
2. [ParticleRenderType — 从接口到 record](#2-particlerendertype--从接口到-record)
3. [ParticleEngine — 精简为调度器](#3-particleengine--精简为调度器)
4. [ParticleResources — 新资源管理器](#4-particleresources--新资源管理器)
5. [ParticleGroup 体系 — 分组渲染抽象](#5-particlegroup-体系--分组渲染抽象)
6. [SingleQuadParticle — 内嵌 Layer 和 RenderPipeline](#6-singlequadparticle--内嵌-layer-和-renderpipeline)
7. [Particle.java — getGroup 替代 getRenderType](#7-particlejava--getgroup-替代-getrendertype)
8. [ParticleProvider — 新增 RandomSource 参数](#8-particleprovider--新增-randomsource-参数)
9. [渲染调度 — extract → submit → FeatureRenderer 三阶段](#9-渲染调度--extract--submit--featurerenderer-三阶段)
10. [RenderPipeline 映射 — PARTICLE_SNIPPET → OPAQUE/TRANSLUCENT_PARTICLE](#10-renderpipeline-映射--particle_snippet--opaquetranslucent_particle)
11. [QuadParticleRenderState — GPU Buffer 准备与渲染](#11-quadparticlerenderstate--gpu-buffer-准备与渲染)

---

## 1. 架构总览 — 从 PreparableReloadListener 到 extract/submit 分离

26.1.2 的粒子系统进行了**根本性重构**,将渲染从逻辑更新中完全分离:

```
1.20.1/1.21.1 架构:
  ParticleEngine(implements PreparableReloadListener)
    ├── reload() — 加载纹理 + 注册 providers
    ├── tick()  — 更新粒子
    ├── render() — 直接调用 GL/BufferBuilder 渲染 ← 写入发生在主线程,耦合

26.1.2 架构:
  ParticleResources(implements PreparableReloadListener)
    └── reload() — 加载纹理

  ParticleEngine(不实现 PreparableReloadListener)
    ├── tick()  — 委托给 ParticleGroup.tickParticles()
    └── extract() — 提取粒子渲染状态,不执行 GL 调用

  ParticleGroup<P> (抽象)
    ├── QuadParticleGroup     → QuadParticleRenderState
    ├── ItemPickupParticleGroup → ItemPickupParticleGroup.State
    ├── ElderGuardianParticleGroup → ElderGuardianParticleGroup.State
    └── NoRenderParticleGroup → 空
         ↓ extractRenderState()  → ParticleGroupRenderState
         ↓ submit()              → SubmitNodeCollector
              ↓ FeatureRenderDispatcher
                └── ParticleFeatureRenderer.renderSolid() / renderTranslucent()
```

核心原则: **extract** (收集) → **submit** (提交) → **render** (执行)。

---

## 2. ParticleRenderType — 从接口到 record

**文件**: `net/minecraft/client/particle/ParticleRenderType.java` (12 行)

```java
public record ParticleRenderType(String name) {
    public static final ParticleRenderType SINGLE_QUADS    = new ParticleRenderType("SINGLE_QUADS");
    public static final ParticleRenderType ITEM_PICKUP     = new ParticleRenderType("ITEM_PICKUP");
    public static final ParticleRenderType ELDER_GUARDIANS = new ParticleRenderType("ELDER_GUARDIANS");
    public static final ParticleRenderType NO_RENDER       = new ParticleRenderType("NO_RENDER");
}
```

**完全重新设计**:

| 1.20.1/1.21.1 | 26.1.2 |
|---------------|--------|
| interface,6 种匿名实现 | record,4 个常量 |
| begin()/end() 控制 GL 状态 | 无 begin/end — GL 由 RenderPipeline 管理 |
| isTranslucent() 区分不透明/半透明 | 按 ParticleGroup 类型分离,无 isTranslucent |
| TERRAIN_SHEET / PARTICLE_SHEET_OPAQUE / etc. | SINGLE_QUADS / ITEM_PICKUP / ELDER_GUARDIANS / NO_RENDER |

`ParticleRenderType` 现在只是**分组标签**,不再包含任何渲染逻辑。

---

## 3. ParticleEngine — 精简为调度器

**文件**: `net/minecraft/client/particle/ParticleEngine.java` (157 行)

### 3.1 不再实现 PreparableReloadListener

资源加载完全移交给 `ParticleResources`。

### 3.2 核心数据结构

```java
public class ParticleEngine {
    private static final List<ParticleRenderType> RENDER_ORDER = List.of(
        ParticleRenderType.SINGLE_QUADS,
        ParticleRenderType.ITEM_PICKUP,
        ParticleRenderType.ELDER_GUARDIANS
    );
    private final Map<ParticleRenderType, ParticleGroup<?>> particles
        = Maps.newIdentityHashMap();
    private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
    private final ParticleResources resourceManager;
    private final Map<ParticleRenderType, Function<ParticleEngine, ParticleGroup<?>>>
        particleGroupFactories;  // 可扩展的 ParticleGroup 工厂
}
```

- `particles`: 按 `ParticleRenderType` 分组的 `ParticleGroup<?>` (不再是 Queue)
- `particleGroupFactories`: 允许 Forge/NeoForge 模组注册自定义 ParticleGroup

### 3.3 `tick()` (第 85–109 行)

```java
public void tick() {
    this.particles.forEach((type, group) -> {
        Profiler.get().push(type.name());
        group.tickParticles();  // 委托给 ParticleGroup
        Profiler.get().pop();
    });
    // trackingEmitters + particlesToAdd 处理...
}
```

### 3.4 `extract()` — 替代 render() (第 128–135 行)

```java
public void extract(ParticlesRenderState particlesRenderState,
        Frustum frustum, Camera camera, float partialTickTime) {
    for (ParticleRenderType particleType : this.particleRenderOrder) {
        ParticleGroup<?> particles = this.particles.get(particleType);
        if (particles != null && !particles.isEmpty()) {
            particlesRenderState.add(
                particles.extractRenderState(frustum, camera, partialTickTime));
        }
    }
}
```

- **不**执行任何 GL 调用
- 只提取每个 ParticleGroup 的渲染状态,存入 `ParticlesRenderState`
- 真正的渲染由 `ParticleFeatureRenderer` 在后续 pass 中执行

### 3.5 `createParticleGroup()` — 工厂方法 (第 112–121 行)

```java
private ParticleGroup<?> createParticleGroup(ParticleRenderType type) {
    if (type == ParticleRenderType.ITEM_PICKUP)
        return new ItemPickupParticleGroup(this);
    else if (type == ParticleRenderType.ELDER_GUARDIANS)
        return new ElderGuardianParticleGroup(this);
    else if (this.particleGroupFactories.containsKey(type))
        return this.particleGroupFactories.get(type).apply(this);
    else
        return (ParticleGroup<?>)(type == ParticleRenderType.NO_RENDER
            ? new NoRenderParticleGroup(this)
            : new QuadParticleGroup(this, type));
}
```

- `ITEM_PICKUP` → `ItemPickupParticleGroup` (实体渲染方式)
- `ELDER_GUARDIANS` → `ElderGuardianParticleGroup` (模型渲染方式)
- 其他 → `QuadParticleGroup` (标准单 quad 粒子)
- NO_RENDER → `NoRenderParticleGroup` (不渲染)

---

## 4. ParticleResources — 新资源管理器

**文件**: `net/minecraft/client/particle/ParticleResources.java` (318 行)

### 4.1 职责

接管了原来 `ParticleEngine` 的 `PreparableReloadListener` 和 provider 注册:

```java
public class ParticleResources implements PreparableReloadListener {
    private final Map<Identifier, ParticleProvider<?>> providers;
    private final Map<Identifier, MutableSpriteSet> spriteSets;
    private @Nullable Runnable onReload;
}
```

### 4.2 关键差异

| 特性 | 1.21.1 ParticleEngine | 26.1.2 ParticleResources |
|------|----------------------|-------------------------|
| 纹理图集管理 | 自建 TextureAtlas | 通过 `AtlasManager.PENDING_STITCH` 获取 |
| providers Map | 直接持有 | 直接持有,通过 `getProviders()` 暴露 |
| reload | 异步加载 JSON + stitch | 通过 `currentReload.get(AtlasManager.PENDING_STITCH)` |
| SpriteSet | MutableSpriteSet(内部类) | MutableSpriteSet(内部类) + `first()` 方法 |

### 4.3 `ParticleProvider.getProviders()` (第 285–287 行)

```java
public Map<Identifier, ParticleProvider<?>> getProviders() {
    return this.providers;
}
```

`ParticleEngine` 通过此方法获取 provider 表来创建粒子。

---

## 5. ParticleGroup 体系 — 分组渲染抽象

### 5.1 ParticleGroup<P> (第 16–69 行)

**文件**: `net/minecraft/client/particle/ParticleGroup.java`

```java
public abstract class ParticleGroup<P extends Particle> {
    private static final int MAX_PARTICLES = 16384;
    protected final ParticleEngine engine;
    protected final Queue<P> particles = EvictingQueue.create(16384);

    public abstract ParticleGroupRenderState extractRenderState(
        Frustum frustum, Camera camera, float partialTickTime);
}
```

- 泛型 `P extends Particle`,管理特定类型粒子
- 内置 `EvictingQueue(16384)` 限制最大粒子数
- `tickParticles()` 方法处理粒子的 tick/remove 循环
- 抽象方法 `extractRenderState()` 将粒子数据转为渲染状态

### 5.2 QuadParticleGroup (第 14–41 行)

**文件**: `net/minecraft/client/particle/QuadParticleGroup.java`

```java
public class QuadParticleGroup extends ParticleGroup<SingleQuadParticle> {
    private final ParticleRenderType particleType;
    final QuadParticleRenderState particleTypeRenderState
        = new QuadParticleRenderState();

    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTickTime) {
        for (SingleQuadParticle particle : this.particles) {
            if (frustum.pointInFrustum(particle.x, particle.y, particle.z)) {
                particle.extract(this.particleTypeRenderState, camera, partialTickTime);
            }
        }
        return this.particleTypeRenderState;
    }
}
```

- 遍历粒子,调用 `SingleQuadParticle.extract()` 将数据填入 `QuadParticleRenderState`
- 使用 `frustum.pointInFrustum()` 进行锥体裁剪

### 5.3 ItemPickupParticleGroup (第 19–61 行)

**文件**: `net/minecraft/client/particle/ItemPickupParticleGroup.java`

- 使用 `EntityRenderDispatcher.submit()` 提交实体渲染
- 每个拣取粒子转换为 `EntityRenderState` + 世界偏移

### 5.4 ElderGuardianParticleGroup (第 21–66 行)

**文件**: `net/minecraft/client/particle/ElderGuardianParticleGroup.java`

- 使用 `submitNodeCollector.submitModel()` 提交模型渲染
- 使用自定义的 `PoseStack` 变换

### 5.5 NoRenderParticleGroup

**文件**: `net/minecraft/client/particle/NoRenderParticleGroup.java`

- `extractRenderState()` 返回空 state,不参与渲染

---

## 6. SingleQuadParticle — 内嵌 Layer 和 RenderPipeline

**文件**: `net/minecraft/client/particle/SingleQuadParticle.java` (197 行)

### 6.1 构造函数变化

```java
public SingleQuadParticle(ClientLevel level, double x, double y, double z,
        TextureAtlasSprite sprite) {
    super(level, x, y, z);
    this.sprite = sprite;  // sprite 在构造时传入,不再通过 setSprite 延迟设置
    this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
}
```

- **构造函数直接接收 `TextureAtlasSprite`**,而不是在 `TextureSheetParticle` 中延迟设置
- `TextureSheetParticle` 类**被移除**,其逻辑直接合并到 `SingleQuadParticle`

### 6.2 `extract()` 替代 `render()` (第 45–53 行)

```java
public void extract(QuadParticleRenderState particleTypeRenderState,
        Camera camera, float partialTickTime) {
    Quaternionf rotation = new Quaternionf();
    this.getFacingCameraMode().setRotation(rotation, camera, partialTickTime);
    if (this.roll != 0.0F)
        rotation.rotateZ(Mth.lerp(partialTickTime, this.oRoll, this.roll));
    this.extractRotatedQuad(particleTypeRenderState, camera, rotation, partialTickTime);
}
```

- 不再直接写入 `VertexConsumer`
- 改为将粒子数据(位置+旋转+UV+颜色+光照)写入 `QuadParticleRenderState`

### 6.3 `Layer` record — RenderPipeline 映射 (第 166–196 行)

```java
public record Layer(boolean translucent, Identifier textureAtlasLocation,
        RenderPipeline pipeline) {

    public static final Layer OPAQUE_TERRAIN = new Layer(
        false, TextureAtlas.LOCATION_BLOCKS, RenderPipelines.OPAQUE_PARTICLE);
    public static final Layer TRANSLUCENT_TERRAIN = new Layer(
        true, TextureAtlas.LOCATION_BLOCKS, RenderPipelines.TRANSLUCENT_PARTICLE);
    public static final Layer OPAQUE_ITEMS = new Layer(
        false, TextureAtlas.LOCATION_ITEMS, RenderPipelines.OPAQUE_PARTICLE);
    public static final Layer TRANSLUCENT_ITEMS = new Layer(
        true, TextureAtlas.LOCATION_ITEMS, RenderPipelines.TRANSLUCENT_PARTICLE);
    public static final Layer OPAQUE = new Layer(
        false, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.OPAQUE_PARTICLE);
    public static final Layer TRANSLUCENT = new Layer(
        true, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.TRANSLUCENT_PARTICLE);

    public static Layer bySprite(TextureAtlasSprite sprite) {
        boolean translucent = sprite.transparency().hasTranslucent();
        if (sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS))
            return translucent ? TRANSLUCENT_TERRAIN : OPAQUE_TERRAIN;
        else if (sprite.atlasLocation().equals(TextureAtlas.LOCATION_ITEMS))
            return translucent ? TRANSLUCENT_ITEMS : OPAQUE_ITEMS;
        else
            return translucent ? TRANSLUCENT : OPAQUE;
    }
}
```

**Layer 选择逻辑**:
1. 检查纹理图集: BLOCKS → TERRAIN, ITEMS → ITEMS, 其他 → PARTICLES
2. 检查精灵透明度: hasTranslucent → TRANSLUCENT, 否则 → OPAQUE
3. 每个 Layer 直接关联一个 `RenderPipeline`

### 6.4 `getGroup()` (第 94–96 行)

```java
public ParticleRenderType getGroup() {
    return ParticleRenderType.SINGLE_QUADS;
}
```

- 替代旧的 `getRenderType()`
- 几乎所有单 quad 粒子都返回 `SINGLE_QUADS`

### 6.5 `getLayer()` — 新的抽象方法

```java
protected abstract SingleQuadParticle.Layer getLayer();
```

- 子类必须实现,决定粒子使用哪个渲染管线
- 典型实现: `return RenderPipelines.PARTICLE_SHEET_TRANSLUCENT` (在具体粒子类中)

---

## 7. Particle.java — getGroup 替代 getRenderType

**文件**: `net/minecraft/client/particle/Particle.java` (229 行)

### 7.1 关键变化

| 特性 | 1.20.1/1.21.1 | 26.1.2 |
|------|--------------|--------|
| 渲染方法 | `abstract render(VertexConsumer, Camera, float)` | **移除** |
| 类型标识 | `abstract getRenderType()` → `ParticleRenderType` | `abstract getGroup()` → `ParticleRenderType` |
| RGBA 字段 | 在 Particle 基类中 | 移到 `SingleQuadParticle` 子类 |
| roll 字段 | 在 Particle 基类中 | 移到 `SingleQuadParticle` 子类 |
| ParticleGroup 限制 | `Optional<ParticleGroup>` | `Optional<ParticleLimit>` |
| 光照方法 | `getLightColor(float)` | `getLightCoords(float)` |

### 7.2 `getLightCoords()` (第 187–190 行)

```java
protected int getLightCoords(float a) {
    BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
    return this.level.hasChunkAt(pos)
        ? LevelRenderer.getLightCoords(this.level, pos)
        : 15728640;  // FULL_SKY (不是 0)
}
```

- 方法名从 `getLightColor` 改为 `getLightCoords`
- 未加载区块时返回 `15728640` (FULL_SKY = 0xF00000),而不是 0

---

## 8. ParticleProvider — 新增 RandomSource 参数

**文件**: `net/minecraft/client/particle/ParticleProvider.java` (20 行)

```java
public interface ParticleProvider<T extends ParticleOptions> {
    @Nullable Particle createParticle(T options, ClientLevel level,
        double x, double y, double z, double xAux, double yAux, double zAux,
        RandomSource random);  // ← 新增参数

    interface Sprite<T extends ParticleOptions> {
        @Nullable SingleQuadParticle createParticle(  // ← 返回 SingleQuadParticle 而非 TextureSheetParticle
            T options, ClientLevel level,
            double x, double y, double z, double xAux, double yAux, double zAux,
            RandomSource random);
    }
}
```

**两个重要变化**:
1. `createParticle()` 新增 `RandomSource random` 参数
2. `Sprite<T>` 返回 `SingleQuadParticle` 而不是 `TextureSheetParticle`

---

## 9. 渲染调度 — extract → submit → FeatureRenderer 三阶段

**文件**: `net/minecraft/client/renderer/LevelRenderer.java`

### 9.1 阶段 1: extract (第 607 行)

```java
// 在 prepareRenderState 过程中:
profiler.popPush("particles");
this.minecraft.particleEngine.extract(
    this.levelRenderState.particlesRenderState,
    new Frustum(cullFrustum).offset(-3.0F),
    camera, deltaPartialTick);
```

- 在帧准备阶段调用,只收集数据,不渲染
- Frustum 偏移 -3.0 格,防止粒子在屏幕边缘被过早裁剪

### 9.2 阶段 2: submit (第 701 行)

```java
levelRenderState.particlesRenderState.submit(
    this.submitNodeStorage, levelRenderState.cameraRenderState);
```

- 将粒子渲染状态注册到 `SubmitNodeStorage` 中的对应 pass

### 9.3 阶段 3: FeatureRenderDispatcher 渲染 (第 706、725、751 行)

```java
// 不透明阶段:
this.featureRenderDispatcher.renderSolidFeatures();  // 包含粒子不透明层

// 半透明阶段(方块之后):
this.featureRenderDispatcher.renderTranslucentFeatures();

// 半透明粒子(在方块半透明之后):
this.featureRenderDispatcher.renderTranslucentParticles();
```

**完整顺序**:
```
submitFeatures          → entities, blocks, particles 提交
renderSolidFeatures     → 不透明方块 + 不透明粒子(OPAQUE_PARTICLE pipeline)
renderTranslucentFeatures → 半透明方块 + 半透明模型
renderTranslucentParticles → 半透明粒子(TRANSLUCENT_PARTICLE pipeline)
```

### 9.4 FeatureRenderDispatcher 中的粒子渲染 (第 68、93 行)

**文件**: `net/minecraft/client/renderer/feature/FeatureRenderDispatcher.java`

```java
public void renderSolidFeatures() {
    // ... 其他 feature ...
    this.particleFeatureRenderer.renderSolid(collection);
}

public void renderTranslucentParticles() {
    this.particleFeatureRenderer.renderTranslucent(collection);
}
```

---

## 10. RenderPipeline 映射 — PARTICLE_SNIPPET → OPAQUE/TRANSLUCENT_PARTICLE

**文件**: `net/minecraft/client/renderer/RenderPipelines.java`

### 10.1 PARTICLE_SNIPPET (第 120–127 行)

```java
public static final RenderPipeline.Snippet PARTICLE_SNIPPET =
    RenderPipeline.builder(MATRICES_FOG_SNIPPET)
        .withVertexShader("core/particle")
        .withFragmentShader("core/particle")
        .withSampler("Sampler0")       // 纹理图集
        .withSampler("Sampler2")       // lightmap
        .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .buildSnippet();
```

- 继承 `MATRICES_FOG_SNIPPET` (模型视图矩阵 + 投影矩阵 + 雾参数)
- 着色器: `core/particle` (vertex + fragment)
- Sampler0: 粒子纹理图集
- Sampler2: lightmap 纹理
- 顶点格式: `DefaultVertexFormat.PARTICLE` (Position+UV0+Color+UV2)
- 深度测试: DEFAULT (LESS_THAN_OR_EQUAL, 写入开启)

### 10.2 OPAQUE_PARTICLE (第 592 行)

```java
public static final RenderPipeline OPAQUE_PARTICLE = register(
    RenderPipeline.builder(PARTICLE_SNIPPET)
        .withLocation("pipeline/opaque_particle")
        .build());
```

- 直接继承 PARTICLE_SNIPPET,无额外混合
- 不透明渲染

### 10.3 TRANSLUCENT_PARTICLE (第 593–598 行)

```java
public static final RenderPipeline TRANSLUCENT_PARTICLE = register(
    RenderPipeline.builder(PARTICLE_SNIPPET)
        .withLocation("pipeline/translucent_particle")
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .build());
```

- 继承 PARTICLE_SNIPPET
- 添加半透明混合 `BlendFunction.TRANSLUCENT`

### 10.4 ParticleRenderType → RenderPipeline 映射

| 旧 ParticleRenderType (1.20.1) | 新 Layer (26.1.2) | RenderPipeline |
|-------------------------------|-------------------|----------------|
| TERRAIN_SHEET | OPAQUE_TERRAIN / TRANSLUCENT_TERRAIN | OPAQUE/TRANSLUCENT_PARTICLE |
| PARTICLE_SHEET_OPAQUE | OPAQUE (PARTICLES) | OPAQUE_PARTICLE |
| PARTICLE_SHEET_LIT | OPAQUE (PARTICLES) | OPAQUE_PARTICLE |
| PARTICLE_SHEET_TRANSLUCENT | TRANSLUCENT (PARTICLES) | TRANSLUCENT_PARTICLE |
| CUSTOM | — | 自定义 |
| — | OPAQUE_ITEMS / TRANSLUCENT_ITEMS | OPAQUE/TRANSLUCENT_PARTICLE |

---

## 11. QuadParticleRenderState — GPU Buffer 准备与渲染

**文件**: `net/minecraft/client/renderer/state/level/QuadParticleRenderState.java` (286 行)

### 11.1 设计

`QuadParticleRenderState` 是**数据 → GPU buffer** 的桥梁:

```
SingleQuadParticle.extract()
  → QuadParticleRenderState.add(Layer, x, y, z, rot, scale, uv, color, light)
    → Storage.add() — 存储为 float[12] + int[2] 每粒子

ParticleFeatureRenderer 调用:
  → QuadParticleRenderState.prepare(bufferCache, translucent)
    → 按 Layer.translucent 过滤
    → 遍历 Storage,调用 renderRotatedQuad() 写入 BufferBuilder
    → bufferBuilder.build() → MeshData
    → 写入 ParticleBufferCache (ring buffer)
    → 返回 PreparedBuffers(带 layers 映射)

  → QuadParticleRenderState.render(preparedBuffers, bufferCache, renderPass, textureManager)
    → 绑定 vertex buffer + index buffer
    → 设置 DynamicTransforms uniform
    → for each Layer: renderPass.setPipeline(layer.pipeline)
                    renderPass.bindTexture("Sampler0", ...)
                    renderPass.drawIndexed(...)
```

### 11.2 Storage 结构 (第 204–285 行)

```java
private static class Storage {
    private float[] floatValues;  // 每粒子12个float: x,y,z, xRot,yRot,zRot,wRot, scale, u0,u1,v0,v1
    private int[] intValues;      // 每粒子2个int: color, lightCoords
    private int currentParticleIndex;
}
```

- 紧凑的 SoA 布局
- 初始容量 1024,自动增长

### 11.3 ParticleFeatureRenderer 渲染 (第 32–77 行)

**文件**: `net/minecraft/client/renderer/feature/ParticleFeatureRenderer.java`

```java
public void renderSolid(SubmitNodeCollection nodeCollection) {
    this.render(nodeCollection, false);
}

public void renderTranslucent(SubmitNodeCollection nodeCollection) {
    this.render(nodeCollection, true);
}

private void render(SubmitNodeCollection nodeCollection, boolean translucent) {
    for (ParticleGroupRenderer renderer : nodeCollection.getParticleGroupRenderers()) {
        PreparedBuffers prepared = renderer.prepare(buffer, translucent);
        if (prepared != null) {
            // 选择 render target (半透明→particleTarget, 不透明→mainTarget)
            try (RenderPass renderPass = device.createCommandEncoder()
                    .createRenderPass("Particles", colorTexture, depthTexture)) {
                // 设置 lightmap (Sampler2)
                renderPass.bindTexture("Sampler2", lightmapTexture, clampToEdge(LINEAR));
                renderer.render(prepared, buffer, renderPass, textureManager);
            }
        }
    }
}
```

- `renderSolid()` 渲染不透明粒子到 mainTarget
- `renderTranslucent()` 渲染半透明粒子到 particleTarget (独立 target,用于后续合成)
- Lightmap 直接绑定 `Sampler2` — 不需要 `LightTexture.turnOnLightLayer()`

---

## 总结

### 架构对比

| 层级 | 1.20.1 | 1.21.1 | 26.1.2 |
|------|--------|--------|--------|
| 资源管理 | ParticleEngine(PreparableReloadListener) | ParticleEngine(PreparableReloadListener) | ParticleResources(PreparableReloadListener) |
| 粒子逻辑 | ParticleEngine.tick() | ParticleEngine.tick() | ParticleEngine.tick() → ParticleGroup.tickParticles() |
| 渲染入口 | ParticleEngine.render() (直接 GL) | ParticleEngine.render() + Predicate | ParticleEngine.extract() (无 GL) |
| 渲染执行 | BufferBuilder → Tesselator.end() | BufferBuilder → BufferUploader.drawWithShader() | RenderPass → drawIndexed() |
| 粒子分组 | Map<PRT, Queue> | Map<PRT, Queue> | Map<PRT, ParticleGroup<?>> |
| PRT 定义 | interface + 匿名类 + begin/end | interface + begin/@Nullable + isTranslucent | record 4 常量 |
| 纹理 Sprite | TextureSheetParticle | TextureSheetParticle | 合并入 SingleQuadParticle |
| 着色器 | GameRenderer::getParticleShader | GameRenderer::getParticleShader | PARTICLE_SNIPPET → OPAQUE/TRANSLUCENT_PARTICLE |

### 关键数字

| 常量 | 值 | 含义 |
|------|-----|------|
| MAX_PARTICLES | 16384 | 每个 ParticleGroup 最大粒子数 |
| INITIAL_PARTICLE_CAPACITY | 1024 | Storage 初始容量 |
| FLOATS_PER_PARTICLE | 12 | 每粒子 float 字段数 |
| INTS_PER_PARTICLE | 2 | 每粒子 int 字段数 |

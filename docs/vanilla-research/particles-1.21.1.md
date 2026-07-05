# 1.21.1 (NeoForge) Minecraft Vanilla Particle System 分析报告

> 基于 `.local_ref/mc/1.21.1/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [ParticleEngine.java — 与 1.20.1 的关键差异](#1-particleenginejava--与-1201-的关键差异)
2. [ParticleRenderType — isTranslucent 和 @Nullable begin](#2-particlerendertype--istranslucent-和-nullable-begin)
3. [Particle.java — LifetimeAlpha 与 renderBoundingBox](#3-particlejava--lifetimealpha-与-renderboundingbox)
4. [SingleQuadParticle — FacingCameraMode](#4-singlequadparticle--facingcameramode)
5. [TextureSheetParticle](#5-texturesheetparticle)
6. [ParticleProvider — 签名不变,1.20.1 兼容](#6-particleprovider--签名不变1201-兼容)
7. [粒子渲染调度 — 三阶段渲染](#7-粒子渲染调度--三阶段渲染)
8. [新增粒子类型](#8-新增粒子类型)
9. [BufferUploader 替代 Tesselator.end](#9-bufferuploader-替代-tesselatorend)

---

## 1. ParticleEngine.java — 与 1.20.1 的关键差异

**文件**: `net/minecraft/client/particle/ParticleEngine.java` (651 行)

### 1.1 结构对比

| 特性 | 1.20.1 | 1.21.1 |
|------|--------|--------|
| 接口 | `PreparableReloadListener` | `PreparableReloadListener` (相同) |
| Begin 返回值 | void | `@Nullable BufferBuilder` |
| 渲染方法签名 | `render(PoseStack, BufferSource, LightTexture, Camera, float, Frustum)` | `render(LightTexture, Camera, float, Frustum, Predicate<ParticleRenderType>)` |
| 顶点提交 | `Tesselator.end()` | `BufferBuilder.build()` → `BufferUploader.drawWithShader()` |
| RenderOrder 比较器 | `ForgeHooksClient.makeParticleRenderTypeComparator` | `ClientHooks.makeParticleRenderTypeComparator` |

### 1.2 `render()` 方法 (第 464–503 行)

```java
public void render(LightTexture lightTexture, Camera camera, float partialTick,
        @Nullable Frustum frustum,
        java.util.function.Predicate<ParticleRenderType> renderTypePredicate) {

    lightTexture.turnOnLightLayer();
    RenderSystem.enableDepthTest();
    RenderSystem.activeTexture(GL_TEXTURE2);
    RenderSystem.activeTexture(GL_TEXTURE0);

    for (ParticleRenderType renderType : this.particles.keySet()) {
        if (renderType == ParticleRenderType.NO_RENDER
                || !renderTypePredicate.test(renderType)) continue;
        Queue<Particle> queue = this.particles.get(renderType);
        if (queue != null && !queue.isEmpty()) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = renderType.begin(tesselator, this.textureManager);
            if (bufferbuilder != null) {
                for (Particle particle : queue) {
                    if (frustum != null
                            && !frustum.isVisible(particle.getRenderBoundingBox(partialTick)))
                        continue;
                    particle.render(bufferbuilder, camera, partialTick);
                }
                MeshData meshdata = bufferbuilder.build();
                if (meshdata != null) {
                    BufferUploader.drawWithShader(meshdata);
                }
            }
        }
    }

    RenderSystem.depthMask(true);
    RenderSystem.disableBlend();
    lightTexture.turnOffLightLayer();
}
```

**关键变化**:
- 接受 `Predicate<ParticleRenderType>` 过滤器,用于分离 solid/translucent 粒子的渲染阶段
- `begin()` 现在返回 `@Nullable BufferBuilder`,NO_RENDER 返回 null
- 不再使用 `Tesselator.end()`,改用 `bufferbuilder.build()` → `BufferUploader.drawWithShader(meshdata)`
- 不再接收 `PoseStack` — ModelView 矩阵由 RenderSystem 隐式管理
- 锥体裁剪使用 `getRenderBoundingBox()` 而非 `getBoundingBox()` + `shouldCull()`

### 1.3 新增 `iterateParticles()` (第 600–608 行)

```java
public void iterateParticles(java.util.function.Consumer<Particle> consumer) {
    for (ParticleRenderType type : this.particles.keySet()) {
        if (type == ParticleRenderType.NO_RENDER) continue;
        Iterable<Particle> iterable = this.particles.get(type);
        if (iterable != null) iterable.forEach(consumer);
    }
}
```

- 新增公开方法,允许外部遍历所有活跃粒子

---

## 2. ParticleRenderType — isTranslucent 和 @Nullable begin

**文件**: `net/minecraft/client/particle/ParticleRenderType.java` (119 行)

### 2.1 核心变化

与 1.20.1 的三大差异:

| 变化 | 1.20.1 | 1.21.1 |
|------|--------|--------|
| begin 返回 | void | `@Nullable BufferBuilder` |
| end 方法 | `void end(Tesselator)` | **移除** |
| isTranslucent | 无 | `default boolean isTranslucent()` 返回 `true` |

### 2.2 `isTranslucent()` (第 116–118 行)

```java
default boolean isTranslucent() {
    return true;
}
```

- 默认返回 `true` — 大多数粒子是半透明的
- `PARTICLE_SHEET_OPAQUE` override 返回 `false`
- `PARTICLE_SHEET_LIT` override 返回 `false`

### 2.3 begin() 返回 BufferBuilder

```java
// NO_RENDER:
@Nullable
public BufferBuilder begin(Tesselator t, TextureManager tm) {
    return null;  // 无渲染,返回 null
}

// CUSTOM:
public BufferBuilder begin(Tesselator t, TextureManager tm) {
    RenderSystem.depthMask(true);
    RenderSystem.disableBlend();
    return t.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
    // 现在也开始了 BufferBuilder!
}
```

- `NO_RENDER`: 返回 null,`ParticleEngine.render()` 会跳过渲染
- `CUSTOM`: 现在也开始 BufferBuilder,不再是纯自定义(与 1.20.1 不同)

### 2.4 各类型完整清单

| 类型 | Blend | DepthWrite | 纹理 | Shader | isTranslucent |
|------|-------|-----------|------|--------|---------------|
| TERRAIN_SHEET | enableBlend | true | BLOCKS | — | true |
| PARTICLE_SHEET_OPAQUE | disableBlend | true | PARTICLES | getParticleShader | **false** |
| PARTICLE_SHEET_TRANSLUCENT | enableBlend | true | PARTICLES | — | true |
| PARTICLE_SHEET_LIT | disableBlend | true | PARTICLES | — | **false** |
| CUSTOM | disableBlend | true | —(不设置) | — | true |
| NO_RENDER | — | — | — | — | true(不渲染) |

### 2.5 渲染顺序

```
TERRAIN_SHEET → PARTICLE_SHEET_OPAQUE → PARTICLE_SHEET_LIT
    → PARTICLE_SHEET_TRANSLUCENT → CUSTOM
```

与 1.20.1 完全相同。

---

## 3. Particle.java — LifetimeAlpha 与 renderBoundingBox

**文件**: `net/minecraft/client/particle/Particle.java` (280 行)

### 3.1 新增 `Particle.LifetimeAlpha` record (第 264–279 行)

```java
public static record LifetimeAlpha(
    float startAlpha, float endAlpha,
    float startAtNormalizedAge, float endAtNormalizedAge
) {
    public static final Particle.LifetimeAlpha ALWAYS_OPAQUE
        = new Particle.LifetimeAlpha(1.0F, 1.0F, 0.0F, 1.0F);

    public boolean isOpaque() {
        return this.startAlpha >= 1.0F && this.endAlpha >= 1.0F;
    }

    public float currentAlphaForAge(int age, int lifetime, float partialTick) {
        if (Mth.equal(this.startAlpha, this.endAlpha))
            return this.startAlpha;
        float timeNormalized = Mth.inverseLerp(
            (age + partialTick) / lifetime, startAtNormalizedAge, endAtNormalizedAge);
        return Mth.clampedLerp(timeNormalized, this.startAlpha, this.endAlpha);
    }
}
```

- 声明式定义粒子生命周期的 alpha 变化
- `ALWAYS_OPAQUE`: 始终不透明的便捷常量

### 3.2 新增 `getRenderBoundingBox()` (第 255–258 行)

```java
public AABB getRenderBoundingBox(float partialTicks) {
    return getBoundingBox().inflate(1.0);
}
```

- 替换了 1.20.1 的 `shouldCull()` 机制
- 默认将包围盒扩大 1 格用于摄像机裁切
- 子类(如 `SingleQuadParticle`)可 override 提供精确的 render bounds

### 3.3 新增 `getPos()` (第 259–261 行)

```java
public Vec3 getPos() {
    return new Vec3(this.x, this.y, this.z);
}
```

---

## 4. SingleQuadParticle — FacingCameraMode

**文件**: `net/minecraft/client/particle/SingleQuadParticle.java` (116 行)

### 4.1 FacingCameraMode 接口 (第 108–115 行)

```java
public interface FacingCameraMode {
    FacingCameraMode LOOKAT_XYZ
        = (quaternion, camera, partialTick) -> quaternion.set(camera.rotation());
    FacingCameraMode LOOKAT_Y
        = (quaternion, camera, partialTick) -> quaternion.set(
            0.0F, camera.rotation().y, 0.0F, camera.rotation().w);

    void setRotation(Quaternionf quaternion, Camera camera, float partialTick);
}
```

- `LOOKAT_XYZ`: 粒子始终面向摄像机(默认,与 1.20.1 行为一致)
- `LOOKAT_Y`: 粒子仅绕 Y 轴旋转(保持垂直,如水平漂浮粒子)

### 4.2 渲染重构

1.20.1 的 `render()` 直接在方法内处理旋转和顶点,1.21.1 拆分为三层次:

```
render(buffer, camera, partialTick)
  → getFacingCameraMode().setRotation(quaternion, camera, partialTick)
  → renderRotatedQuad(buffer, camera, quaternion, partialTick)
    → renderRotatedQuad(buffer, quaternion, x, y, z, partialTick)
      → renderVertex() × 4
```

### 4.3 `renderVertex()` (第 63–81 行)

```java
private void renderVertex(VertexConsumer buffer, Quaternionf quaternion,
        float x, float y, float z, float xOffset, float yOffset,
        float quadSize, float u, float v, int packedLight) {
    Vector3f pos = new Vector3f(xOffset, yOffset, 0.0F)
        .rotate(quaternion).mul(quadSize).add(x, y, z);
    buffer.addVertex(pos.x(), pos.y(), pos.z())
        .setUv(u, v)
        .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
        .setLight(packedLight);
}
```

- 使用 `VertexConsumer` 新 API: `addVertex()` → `.setUv()` → `.setColor()` → `.setLight()`
- 与 1.20.1 的 `buffer.vertex().uv().color().uv2().endVertex()` 链式调用等价

### 4.4 `getRenderBoundingBox()` (第 84–87 行)

```java
public AABB getRenderBoundingBox(float partialTicks) {
    float size = getQuadSize(partialTicks);
    return new AABB(this.x - size, this.y - size, this.z - size,
                    this.x + size, this.y + size, this.z + size);
}
```

- 精确的 quad 级别包围盒,用于锥体裁剪

---

## 5. TextureSheetParticle

**文件**: `net/minecraft/client/particle/TextureSheetParticle.java` (55 行)

- 与 1.20.1 几乎完全相同
- 仅代码风格差异(缩进、`@Override` 注解)
- 仍然 `extends SingleQuadParticle`

---

## 6. ParticleProvider — 签名不变,1.20.1 兼容

**文件**: `net/minecraft/client/particle/ParticleProvider.java` (与 1.20.1 相同)

```java
public interface ParticleProvider<T extends ParticleOptions> {
    @Nullable Particle createParticle(T type, ClientLevel level,
        double x, double y, double z,
        double xSpeed, double ySpeed, double zSpeed);
}
```

- 签名与 1.20.1 完全一致
- `Sprite<T>` 子接口仍然返回 `TextureSheetParticle`

---

## 7. 粒子渲染调度 — 三阶段渲染

**文件**: `net/minecraft/client/renderer/LevelRenderer.java`

### 7.1 三阶段渲染 (第 1191–1210 行)

```java
// 阶段 1 (FX 模式: 粒子使用独立 render target)
this.minecraft.particleEngine.render(lightTexture, camera, f, frustum, type -> true);

// 阶段 2 (普通模式: 不透明粒子在方块不透明层之后、半透明层之前)
this.minecraft.particleEngine.render(lightTexture, camera, f, frustum,
    type -> !type.isTranslucent());

// 阶段 3 (普通模式: 半透明粒子在半透明方块之后)
this.minecraft.particleEngine.render(lightTexture, camera, f, frustum,
    type -> type.isTranslucent());
```

### 7.2 渲染管线中的位置

```
普通模式:
  solid_particles  (不透明粒子: PARTICLE_SHEET_OPAQUE + PARTICLE_SHEET_LIT)
    → translucent terrain (半透明方块)
    → particles  (半透明粒子: 其他所有类型)

FX 模式:
  particles  (所有粒子,使用独立 particlesTarget)
```

### 7.3 为什么这样设计

- **不透明粒子先渲染**: 确保它们能被半透明方块正确遮挡(修复 MC-161917)
- **半透明粒子后渲染**: 在半透明方块之后,获得正确的深度排序
- `isTranslucent()` 方法使得引擎能自动区分两类粒子

---

## 8. 新增粒子类型

与 1.20.1 相比,1.21.1 新增:

| 粒子类型 | 对应类 |
|---------|--------|
| GUST / SMALL_GUST | `GustParticle` |
| GUST_EMITTER_LARGE / SMALL | `GustSeedParticle` |
| WHITE_SMOKE | `WhiteSmokeParticle` |
| INFESTED | `SpellParticle.Provider` |
| ITEM_COBWEB | `BreakingItemParticle.CobwebProvider` |
| DUST_PLUME | `DustPlumeParticle` |
| TRIAL_SPAWNER_DETECTED_PLAYER | `TrialSpawnerDetectionParticle` |
| TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS | `TrialSpawnerDetectionParticle` |
| VAULT_CONNECTION | `FlyTowardsPositionParticle.VaultConnectionProvider` |
| DUST_PILLAR | `TerrainParticle.DustPillarProvider` |
| RAID_OMEN / TRIAL_OMEN | `SpellParticle.Provider` |
| OMINOUS_SPAWNING | `FlyStraightTowardsParticle.OminousSpawnProvider` |

---

## 9. BufferUploader 替代 Tesselator.end

### 9.1 1.20.1 方式

```java
Tesselator tesselator = Tesselator.getInstance();
BufferBuilder bufferbuilder = tesselator.getBuilder();
// ... 填充顶点 ...
tesselator.end();  // 内部: bufferbuilder.end() → BufferUploader.drawWithShader()
```

### 9.2 1.21.1 方式

```java
Tesselator tesselator = Tesselator.getInstance();
BufferBuilder bufferbuilder = renderType.begin(tesselator, textureManager);
// ... 填充顶点 ...
MeshData meshdata = bufferbuilder.build();
if (meshdata != null) {
    BufferUploader.drawWithShader(meshdata);
}
```

- 更直接地使用 `BufferUploader`
- `Tesselator.end()` 不再在 `ParticleRenderType.end()` 中被调用
- `ParticleRenderType` 接口**移除**了 `end()` 方法

---

## 总结

### 与 1.20.1 的关键差异

| 特性 | 1.20.1 | 1.21.1 |
|------|--------|--------|
| isTranslucent | 无 | 支持,区分 solid/translucent |
| begin() 返回 | void | `@Nullable BufferBuilder` |
| end() | 有 | **移除** |
| 渲染阶段 | 1 阶段 | 3 阶段(按 isTranslucent) |
| 顶点提交 | Tesselator.end() | BufferUploader.drawWithShader() |
| 裁切 | shouldCull() + getBoundingBox() | getRenderBoundingBox() |
| PoseStack | 在 render() 参数中 | 不传入,由 RenderSystem 管理 |
| FacingCameraMode | 无 | LOOKAT_XYZ / LOOKAT_Y |
| LifetimeAlpha | 无 | 新增 record |
| ParticleProvider 签名 | 8 参数(无 RandomSource) | 8 参数(无 RandomSource) — 相同 |
| TextureSheetParticle | 存在 | 存在(未变) |

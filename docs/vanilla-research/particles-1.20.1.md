# 1.20.1 (Forge) Minecraft Vanilla Particle System 分析报告

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [ParticleEngine.java 全分析](#1-particleenginejava-全分析)
2. [ParticleRenderType 枚举完整清单](#2-particlerendertype-枚举完整清单)
3. [Particle.java 基类](#3-particlejava-基类)
4. [SingleQuadParticle — 单 quad 粒子渲染](#4-singlequadparticle--单-quad-粒子渲染)
5. [TextureSheetParticle — 带纹理的粒子](#5-texturesheetparticle--带纹理的粒子)
6. [ParticleProvider 注册接口](#6-particleprovider-注册接口)
7. [粒子渲染调度 (main-loop particles 阶段)](#7-粒子渲染调度-main-loop-particles-阶段)
8. [粒子注册 (registerProviders)](#8-粒子注册-registerproviders)
9. [纹理图集与 SpriteSet](#9-纹理图集与-spriteset)

---

## 1. ParticleEngine.java 全分析

**文件**: `net/minecraft/client/particle/ParticleEngine.java` (587 行)

### 1.1 核心数据结构 (第 70–92 行)

```java
public class ParticleEngine implements PreparableReloadListener {
    private static final int MAX_PARTICLES_PER_LAYER = 16384;
    private static final List<ParticleRenderType> RENDER_ORDER = ImmutableList.of(
        ParticleRenderType.TERRAIN_SHEET,
        ParticleRenderType.PARTICLE_SHEET_OPAQUE,
        ParticleRenderType.PARTICLE_SHEET_LIT,
        ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT,
        ParticleRenderType.CUSTOM
    );
    private final Map<ParticleRenderType, Queue<Particle>> particles
        = Maps.newTreeMap(ForgeHooksClient.makeParticleRenderTypeComparator(RENDER_ORDER));
    private final Map<ResourceLocation, ParticleProvider<?>> providers;
    private final Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets;
    private final TextureAtlas textureAtlas;
    private final Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts;
    private final Queue<Particle> particlesToAdd;
    private final Queue<TrackingEmitter> trackingEmitters;
}
```

- `particles`: 按 `ParticleRenderType` 分组的粒子队列,每队列最多 16384 个
- `providers`: ParticleType key → ParticleProvider 的注册表
- `spriteSets`: ParticleType key → MutableSpriteSet(纹理精灵集)
- `textureAtlas`: `TextureAtlas.LOCATION_PARTICLES` 图集
- `trackedParticleCounts`: 按 `ParticleGroup` 跟踪的粒子计数,用于限制每组的最大粒子数

### 1.2 `tick()` — 粒子更新 (第 348–376 行)

```
forEach particleRenderType → tickParticleList(该类型的粒子队列)
    → Iterator 遍历:
        tickParticle(particle)  // 调用 particle.tick()
        if (!alive) → iterator.remove() + 更新计数

// 处理 trackingEmitters(追踪发射器)
if (!trackingEmitters.isEmpty())
    → 遍历, tick(), 移除已死亡者

// 将待添加粒子从 particlesToAdd 移入对应分组
if (!particlesToAdd.isEmpty())
    while 取出 particle:
        particles.computeIfAbsent(renderType, EvictingQueue(16384)).add(particle)
```

粒子的添加是**延迟**的 — 先放入 `particlesToAdd`,在下一 tick 才移入正式队列。这是为了避免在粒子渲染遍历期间修改集合。

### 1.3 `render()` — 粒子渲染 (第 423–464 行)

```java
public void render(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
        LightTexture lightTexture, Camera activeRenderInfo, float partialTicks,
        @Nullable Frustum clippingHelper) {
    lightTexture.turnOnLightLayer();      // 绑定 lightmap 到 GL_TEXTURE2
    RenderSystem.enableDepthTest();
    RenderSystem.activeTexture(GL_TEXTURE2);
    RenderSystem.activeTexture(GL_TEXTURE0);

    PoseStack modelViewStack = RenderSystem.getModelViewStack();
    modelViewStack.pushPose();
    modelViewStack.mulPoseMatrix(poseStack.last().pose());
    RenderSystem.applyModelViewMatrix();

    for (ParticleRenderType renderType : this.particles.keySet()) {
        if (renderType == ParticleRenderType.NO_RENDER) continue;
        Iterable<Particle> iterable = this.particles.get(renderType);
        if (iterable != null) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.getBuilder();
            renderType.begin(bufferbuilder, this.textureManager);  // 设置 GL 状态

            for (Particle particle : iterable) {
                if (clippingHelper != null && particle.shouldCull()
                        && !clippingHelper.isVisible(particle.getBoundingBox())) continue;
                particle.render(bufferbuilder, activeRenderInfo, partialTicks);
            }

            renderType.end(tesselator);  // tesselator.end() → GPU draw
        }
    }

    modelViewStack.popPose();
    RenderSystem.applyModelViewMatrix();
    RenderSystem.depthMask(true);
    RenderSystem.disableBlend();
    lightTexture.turnOffLightLayer();
}
```

**关键点**:
- 对所有粒子使用**同一个着色器** `GameRenderer::getParticleShader`
- 每种 `ParticleRenderType` 独立开启一个 BufferBuilder / Tesselator 周期
- `renderType.begin()` 负责设置混合/深度/纹理绑定
- `renderType.end()` 调用 `tesselator.end()` 触发 GPU 绘制
- lightmap 在渲染前绑定,渲染后解绑

### 1.4 `makeParticle()` — 粒子创建 (第 330–333 行)

```java
private <T extends ParticleOptions> Particle makeParticle(
        T particleData, double x, double y, double z,
        double xSpeed, double ySpeed, double zSpeed) {
    ParticleProvider<T> provider = (ParticleProvider<T>)this.providers
        .get(BuiltInRegistries.PARTICLE_TYPE.getKey(particleData.getType()));
    return provider == null ? null
        : provider.createParticle(particleData, this.level, x, y, z, xSpeed, ySpeed, zSpeed);
}
```

### 1.5 `reload()` — 资源重载 (第 235–289 行)

加载粒子 JSON 描述(`assets/<ns>/particles/<name>.json`)和纹理图集:
1. 异步加载所有 `particles/*.json` 文件
2. 异步 stitch 纹理图集
3. 在主线程:upload 图集,bind sprite sets to particle types
4. 日志输出缺失的精灵纹理

---

## 2. ParticleRenderType 枚举完整清单

**文件**: `net/minecraft/client/particle/ParticleRenderType.java` (111 行)

`ParticleRenderType` 是一个 `interface`,包含 6 个预定义匿名实现:

### 2.1 TERRAIN_SHEET

```java
begin(): enableBlend(), defaultBlendFunc(), depthMask(true)
          setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS)
          bufferBuilder.begin(QUADS, DefaultVertexFormat.PARTICLE)
end():   tesselator.end()
```

- 使用**方块图集**(`TextureAtlas.LOCATION_BLOCKS`)
- 混合开启,深度写入开启
- 用于地形破坏粒子(`TerrainParticle`)

### 2.2 PARTICLE_SHEET_OPAQUE

```java
begin(): disableBlend(), depthMask(true)
          setShader(GameRenderer::getParticleShader)
          setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES)
          bufferBuilder.begin(QUADS, DefaultVertexFormat.PARTICLE)
end():   tesselator.end()
```

- 混合**关闭**(不透明)
- 显式设置着色器为 `GameRenderer::getParticleShader`
- 使用粒子图集

### 2.3 PARTICLE_SHEET_TRANSLUCENT

```java
begin(): depthMask(true)
          setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES)
          enableBlend(), defaultBlendFunc()
          bufferBuilder.begin(QUADS, DefaultVertexFormat.PARTICLE)
end():   tesselator.end()
```

- 混合**开启**,默认混合函数
- 使用粒子图集
- 注意:**没有**显式调用 `setShader` — 沿用 `render()` 中设置的 `GameRenderer::getParticleShader`

### 2.4 PARTICLE_SHEET_LIT

```java
begin(): disableBlend(), depthMask(true)
          setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES)
          bufferBuilder.begin(QUADS, DefaultVertexFormat.PARTICLE)
end():   tesselator.end()
```

- 混合关闭
- 使用粒子图集
- 与 `PARTICLE_SHEET_OPAQUE` 的区别:**没有**显式设置着色器,且渲染顺序在 TRANSLUCENT 之前

### 2.5 CUSTOM

```java
begin(): depthMask(true), disableBlend()
          // 注意: 不调用 bufferBuilder.begin()!
end():   // 空操作
```

- **不**开始 BufferBuilder — 期望粒子自己管理绘制(自定义 `render()` 逻辑)
- 混合关闭,深度写入开启

### 2.6 NO_RENDER

```java
begin(): // 空操作
end():   // 空操作
```

- 不渲染,纯逻辑粒子

### 2.7 接口方法

```java
void begin(BufferBuilder builder, TextureManager textureManager);
void end(Tesselator tesselator);
```

### 2.8 渲染顺序

```
TERRAIN_SHEET → PARTICLE_SHEET_OPAQUE → PARTICLE_SHEET_LIT
    → PARTICLE_SHEET_TRANSLUCENT → CUSTOM
```

---

## 3. Particle.java 基类

**文件**: `net/minecraft/client/particle/Particle.java` (240 行)

### 3.1 核心字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `x, y, z` | double | 当前位置 |
| `xo, yo, zo` | double | 上一 tick 位置 |
| `xd, yd, zd` | double | 速度 |
| `age` | int | 当前年龄(tick 计数) |
| `lifetime` | int | 最大寿命 |
| `rCol, gCol, bCol` | float | 颜色(0–1) |
| `alpha` | float | 透明度(0–1) |
| `roll, oRoll` | float | 旋转角度 |
| `gravity` | float | 重力强度(0 为无重力) |
| `friction` | float | 摩擦系数(默认 0.98) |
| `onGround` | boolean | 是否在地面 |
| `hasPhysics` | boolean | 是否有碰撞物理(默认 true) |
| `removed` | boolean | 是否已标记删除 |

### 3.2 抽象方法

```java
public abstract void render(VertexConsumer buffer, Camera renderInfo, float partialTicks);
public abstract ParticleRenderType getRenderType();
```

### 3.3 `getLightColor()` (第 212–215 行)

```java
protected int getLightColor(float partialTick) {
    BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
    return this.level.hasChunkAt(pos)
        ? LevelRenderer.getLightColor(this.level, pos)  // sky<<20 | block<<4
        : 0;
}
```

- 粒子光照基于其**所在位置**的 block/sky light 计算
- 未加载区块返回 0(全暗)

### 3.4 `getParticleGroup()` (第 229–231 行)

```java
public Optional<ParticleGroup> getParticleGroup() {
    return Optional.empty();
}
```

- 默认返回空,子类可 override 来限制该类型的最大粒子数
- `ParticleGroup` 在 `net/minecraft/core/particles/ParticleGroup.java` 中定义,含 `getLimit()` 方法

---

## 4. SingleQuadParticle — 单 quad 粒子渲染

**文件**: `net/minecraft/client/particle/SingleQuadParticle.java` (75 行)

### 4.1 类定义

```java
public abstract class SingleQuadParticle extends Particle {
    protected float quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
}
```

### 4.2 `render()` (第 25–57 行)

核心渲染逻辑 — 在屏幕空间绘制一个面向摄像机的 quad:

```
1. 粒子世界坐标 → 摄像机空间偏移: f, f1, f2
2. 旋转: 若无 roll → 使用 camera.rotation();
         若有 roll → camera.rotation() 再绕 Z 轴旋转
3. 四个顶点: (-1,-1), (-1,1), (1,1), (1,-1) × quadSize × rotation + offset
4. 每个顶点写入: vertex(xyz).uv(u,v).color(rgba).uv2(light).endVertex()
```

**顶点顺序**: 左上 → 左下 → 右下 → 右上 (标准 quad)

**颜色/RGBA 来源**: `Particle` 基类的 `rCol, gCol, bCol, alpha` 字段,直接写入顶点

**光照**: `this.getLightColor(partialTicks)` → `uv2(light)` → 写入顶点 UV2 通道

### 4.3 抽象方法

```java
protected abstract float getU0();  // 纹理左边界
protected abstract float getU1();  // 纹理右边界
protected abstract float getV0();  // 纹理上边界
protected abstract float getV1();  // 纹理下边界
```

---

## 5. TextureSheetParticle — 带纹理的粒子

**文件**: `net/minecraft/client/particle/TextureSheetParticle.java` (50 行)

```java
public abstract class TextureSheetParticle extends SingleQuadParticle {
    protected TextureAtlasSprite sprite;

    // UV 委托给 sprite:
    protected float getU0() { return this.sprite.getU0(); }
    protected float getU1() { return this.sprite.getU1(); }
    protected float getV0() { return this.sprite.getV0(); }
    protected float getV1() { return this.sprite.getV1(); }

    public void pickSprite(SpriteSet sprite) {
        this.setSprite(sprite.get(this.random));  // 随机选择一个精灵
    }

    public void setSpriteFromAge(SpriteSet sprite) {
        if (!this.removed)
            this.setSprite(sprite.get(this.age, this.lifetime));  // 基于年龄选择
    }
}
```

- 将纹理 UV 完全委托给 `TextureAtlasSprite`
- `pickSprite()`: 从 SpriteSet 随机选择纹理
- `setSpriteFromAge()`: 从 SpriteSet 按年龄进度选择纹理(动画粒子)

---

## 6. ParticleProvider 注册接口

**文件**: `net/minecraft/client/particle/ParticleProvider.java` (19 行)

```java
public interface ParticleProvider<T extends ParticleOptions> {
    @Nullable
    Particle createParticle(T type, ClientLevel level,
        double x, double y, double z,
        double xSpeed, double ySpeed, double zSpeed);

    interface Sprite<T extends ParticleOptions> {
        @Nullable
        TextureSheetParticle createParticle(T type, ClientLevel level,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed);
    }
}
```

**三种注册方式** (`ParticleEngine.java`):

1. **直接 Provider**: `register(type, provider)` — 完全自定义的 Provider
2. **Sprite Provider**: `register(type, Sprite provider)` — 自动调用 `pickSprite()`
3. **SpriteParticleRegistration**: `register(type, SpriteParticleRegistration)` — 先创建带 SpriteSet 的 Provider,再生成粒子

---

## 7. 粒子渲染调度 (main-loop particles 阶段)

**文件**: `net/minecraft/client/renderer/LevelRenderer.java`

### 7.1 渲染调用 (第 1378、1393 行)

```java
// 在渲染循环中:
this.minecraft.particleEngine.render(poseStack, bufferSource,
    lightTexture, camera, partialTick, frustum);
```

粒子渲染在 LevelRenderer 的 render 方法中调用,出现在方块渲染**之后**。

### 7.2 完整数据流

```
LevelRenderer.renderLevel()
  → ParticleEngine.render(poseStack, buffer, lightTexture, camera, partialTick, frustum)
    → lightTexture.turnOnLightLayer()              // 绑定 lightmap 到纹理单元 2
    → for each ParticleRenderType (按 RENDER_ORDER):
        → renderType.begin(bufferBuilder, textureManager)  // 设置 GL 状态
        → for each particle:
            → particle.render(bufferBuilder, camera, partialTick)
              → [SingleQuadParticle] 计算 4 顶点 + 写入 buffer
        → renderType.end(tesselator)               // GPU draw call
    → lightTexture.turnOffLightLayer()
```

---

## 8. 粒子注册 (registerProviders)

**文件**: `ParticleEngine.java` 第 95–195 行

粒子在 `ParticleEngine` 构造时注册,约 50 种原版粒子类型:

| 类别 | 粒子类型示例 |
|------|------------|
| 环境 | AMBIENT_ENTITY_EFFECT, UNDERWATER, MYCELIUM, CHERRY_LEAVES |
| 方块交互 | BLOCK, BLOCK_MARKER, FALLING_DUST |
| 爆炸/战斗 | EXPLOSION, EXPLOSION_EMITTER, CRIT, DAMAGE_INDICATOR, SWEEP_ATTACK |
| 液体 | BUBBLE, SPLASH, DRIPPING_WATER/LAVA/HONEY, RAIN |
| 火焰/烟雾 | FLAME, SMOKE, LARGE_SMOKE, SOUL_FIRE_FLAME, CAMPFIRE_* |
| 药水/法术 | EFFECT, INSTANT_EFFECT, ENTITY_EFFECT, WITCH |
| 传送门 | PORTAL, REVERSE_PORTAL |
| 红石 | DUST, DUST_COLOR_TRANSITION, VIBRATION |
| 生物 | HEART, ANGRY_VILLAGER, HAPPY_VILLAGER, GLOW, SQUID_INK |
| Sculk | SCULK_SOUL, SCULK_CHARGE, SCULK_CHARGE_POP, SHRIEK, SONIC_BOOM |

---

## 9. 纹理图集与 SpriteSet

### 9.1 TextureAtlas

- 粒子使用**独立的**纹理图集:`TextureAtlas.LOCATION_PARTICLES` (ResourceLocation "particles")
- `ParticleEngine` 构造时创建并注册到 `TextureManager`
- 方块粒子(`TERRAIN_SHEET`)使用 `TextureAtlas.LOCATION_BLOCKS`

### 9.2 MutableSpriteSet (第 566–580 行)

```java
static class MutableSpriteSet implements SpriteSet {
    private List<TextureAtlasSprite> sprites;

    // 按年龄进度选择精灵(用于动画)
    public TextureAtlasSprite get(int particleAge, int particleMaxAge) {
        return this.sprites.get(particleAge * (this.sprites.size() - 1) / particleMaxAge);
    }

    // 随机选择精灵
    public TextureAtlasSprite get(RandomSource random) {
        return this.sprites.get(random.nextInt(this.sprites.size()));
    }
}
```

### 9.3 粒子 JSON 描述

文件位置: `assets/<namespace>/particles/<name>.json`
格式:
```json
{
  "textures": [
    "namespace:texture1",
    "namespace:texture2"
  ]
}
```

在 `reload()` 中加载,纹理列表绑定到对应 particle type 的 SpriteSet。

---

## 总结

### 关键数字

| 常量 | 值 | 含义 |
|------|-----|------|
| `MAX_PARTICLES_PER_LAYER` | 16384 | 每种 RenderType 最大粒子数 |

### ParticleRenderType 速查

| 类型 | Blend | DepthWrite | 纹理图集 | 显式 Shader |
|------|-------|-----------|---------|----------|
| TERRAIN_SHEET | 开启 | 开启 | BLOCKS | 否 |
| PARTICLE_SHEET_OPAQUE | **关闭** | 开启 | PARTICLES | **是** |
| PARTICLE_SHEET_LIT | **关闭** | 开启 | PARTICLES | 否 |
| PARTICLE_SHEET_TRANSLUCENT | 开启 | 开启 | PARTICLES | 否 |
| CUSTOM | 关闭 | 开启 | 无 | 否 |
| NO_RENDER | — | — | — | — |

### 渲染数据流

```
ParticleProvider.createParticle()
  → ParticleEngine.add() → particlesToAdd
  → next tick: 移入 particles[renderType] 队列
  → ParticleEngine.tick() 更新所有粒子
  → ParticleEngine.render():
      for each renderType:
        begin (GL state)
        for each particle: particle.render(bufferBuilder)
        end (GPU draw)
```

### 顶点格式

粒子的 `DefaultVertexFormat.PARTICLE`:
```
Position(3F) + UV0(2F) + Color(4UB) + UV2(2S)
```

UV2 通道携带 packed light (block<<4 | sky<<20),着色器中通过 `texelFetch(Sampler2, UV2/16, 0)` 采样 16x16 lightmap。

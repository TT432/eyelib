# Minecraft Vanilla Particle System — 三版本跨版本差异分析

> 版本: 1.20.1 (Forge) / 1.21.1 (NeoForge) / 26.1.2 (NeoForge)
> 基于 `docs/vanilla-research/particles-{1.20.1,1.21.1,26.1.2}.md` 总结

## 目录

1. [架构演进概览](#1-架构演进概览)
2. [ParticleRenderType 演变](#2-particlerendertype-演变)
3. [渲染路径演变 — 从直接 GL 到 extract/submit](#3-渲染路径演变--从直接-gl-到-extractsubmit)
4. [粒子基类演变 — Particle.java](#4-粒子基类演变--particlejava)
5. [单 quad 粒子 — SingleQuadParticle](#5-单-quad-粒子--singlequadparticle)
6. [纹理粒子 — TextureSheetParticle](#6-纹理粒子--texturesheetparticle)
7. [Provider 注册接口](#7-provider-注册接口)
8. [渲染调度在主循环中的位置](#8-渲染调度在主循环中的位置)
9. [粒子分组机制](#9-粒子分组机制)
10. [顶点格式与着色器](#10-顶点格式与着色器)
11. [新增/移除粒子类型](#11-新增移除粒子类型)

---

## 1. 架构演进概览

```
1.20.1                      1.21.1                       26.1.2
───────                      ──────                       ──────
ParticleEngine               ParticleEngine               ParticleResources (新)
├─ PreparableReloadListener  ├─ PreparableReloadListener   ├─ PreparableReloadListener
├─ reload (JSON + atlas)     ├─ reload (JSON + atlas)      ├─ reload (JSON via AtlasManager)
├─ tick (in-place)           ├─ tick (in-place)            └─ getProviders()
├─ render (直接 GL draw)     ├─ render (+ Predicate)       ParticleEngine (重构)
└─ registerProviders()       └─ registerProviders()        ├─ tick → ParticleGroup.tickParticles()
                                                            ├─ extract() (无 GL)
ParticleRenderType            ParticleRenderType             ├─ createParticleGroup() 工厂
├─ 6 种匿名实现                ├─ 6 种匿名实现                 └─ 不实现 PreparableReloadListener
├─ begin/end 控制 GL          ├─ begin(@Nullable) 控制 GL
└─ 无 isTranslucent            └─ isTranslucent()            ParticleRenderType (简化)
                                                            ├─ record 4 常量
TextureSheetParticle          TextureSheetParticle           └─ 无 GL 方法
(extends SingleQuadParticle)  (extends SingleQuadParticle)
                                                            SingleQuadParticle
                                                            ├─ Layer record (含 RenderPipeline)
                            粒子渲染:                        ├─ extract() 替代 render()
1 阶段渲染:                  3 阶段渲染:                     └─ 合并 TextureSheetParticle
所有粒子一起                    ├─ solid_particles            粒子渲染:
  draw call                     ├─ translucent terrain         extract → submit → FeatureRenderer
                                └─ particles (translucent)     ├─ renderSolidFeatures (OPAQUE_PARTICLE)
                                                              └─ renderTranslucentParticles (TRANSLUCENT_PARTICLE)
```

---

## 2. ParticleRenderType 演变

| 版本 | 类型 | 数量 | 关键方法 |
|------|------|------|---------|
| 1.20.1 | interface + 匿名类 | 6 个 | `begin(BufferBuilder, TextureManager)`, `end(Tesselator)` |
| 1.21.1 | interface + 匿名类 | 6 个 | `@Nullable begin(Tesselator, TextureManager)`, `isTranslucent()` |
| 26.1.2 | record | 4 个 | 仅 `name()` |

### 常量映射

| 1.20.1 / 1.21.1 | 26.1.2 | 说明 |
|-----------------|--------|------|
| TERRAIN_SHEET | → `SingleQuadParticle.Layer.OPAQUE_TERRAIN` / `TRANSLUCENT_TERRAIN` | 由 Layer 替代 |
| PARTICLE_SHEET_OPAQUE | → `SingleQuadParticle.Layer.OPAQUE` | 由 Layer 替代 |
| PARTICLE_SHEET_LIT | → `SingleQuadParticle.Layer.OPAQUE` | 合并(LIT 和 OPAQUE 渲染相同) |
| PARTICLE_SHEET_TRANSLUCENT | → `SingleQuadParticle.Layer.TRANSLUCENT` | 由 Layer 替代 |
| CUSTOM | → 可通过 `particleGroupFactories` 自定义 | 扩展机制变化 |
| NO_RENDER | → `ParticleRenderType.NO_RENDER` | 保留 |
| — | `ParticleRenderType.SINGLE_QUADS` | 新增,单 quad 粒子标记 |
| — | `ParticleRenderType.ITEM_PICKUP` | 新增,物品拣取粒子标记 |
| — | `ParticleRenderType.ELDER_GUARDIANS` | 新增,远古守卫者粒子标记 |

### GL 状态管理变化

| 版本 | GL 状态控制 |
|------|-----------|
| 1.20.1 | `ParticleRenderType.begin()` 直接调用 `RenderSystem.enableBlend/disableBlend/depthMask/setShaderTexture` |
| 1.21.1 | 同上,但 `begin()` 返回 `@Nullable BufferBuilder` |
| 26.1.2 | 完全由 `RenderPipeline` JSON 描述控制,不通过 Java 代码设置 GL 状态 |

---

## 3. 渲染路径演变 — 从直接 GL 到 extract/submit

### 3.1 1.20.1

```
ParticleEngine.render(poseStack, buffer, lightTexture, camera, partialTick, frustum)
  → lightTexture.turnOnLightLayer()
  → for each ParticleRenderType:
      renderType.begin(bufferBuilder, textureManager)  // GL 状态设置
      for each particle:
          if frustum.isVisible(particle.getBoundingBox()):
              particle.render(bufferBuilder, camera, partialTick)
                  → bufferBuilder.vertex(xyz).uv(uv).color(rgba).uv2(light).endVertex()
      renderType.end(tesselator)  // tesselator.end() → GPU draw
  → lightTexture.turnOffLightLayer()
```

### 3.2 1.21.1

```
ParticleEngine.render(lightTexture, camera, partialTick, frustum, predicate)
  → lightTexture.turnOnLightLayer()
  → for each ParticleRenderType (filtered by predicate):
      bufferBuilder = renderType.begin(tesselator, textureManager)  // 可能返回 null
      if bufferBuilder != null:
          for each particle:
              if frustum.isVisible(particle.getRenderBoundingBox()):
                  particle.render(bufferBuilder, camera, partialTick)
          meshData = bufferBuilder.build()
          if meshData != null:
              BufferUploader.drawWithShader(meshData)  // 直接提交
  → lightTexture.turnOffLightLayer()
```

### 3.3 26.1.2

```
// 阶段 1: extract (prepareRenderState)
ParticleEngine.extract(particlesRenderState, frustum, camera, partialTickTime)
  → for each ParticleRenderType (按 RENDER_ORDER):
      group.extractRenderState(frustum, camera, partialTickTime)
        → [QuadParticleGroup]: particle.extract(QuadParticleRenderState, ...)
            → [SingleQuadParticle]: state.add(layer, x, y, z, rot, scale, uv, color, light)
        → 返回 ParticleGroupRenderState

// 阶段 2: submit
particlesRenderState.submit(submitNodeStorage, cameraRenderState)
  → QuadParticleRenderState.submit() → submitNodeCollector.submitParticleGroup(this)

// 阶段 3: FeatureRenderDispatcher 渲染
FeatureRenderDispatcher.renderSolidFeatures()
  → ParticleFeatureRenderer.renderSolid()
    → for each ParticleGroupRenderer:
        prepared = renderer.prepare(bufferCache, translucent=false)
          → 按 Layer.translucent==false 过滤
          → 写入 BufferBuilder → MeshData → ring buffer
        renderPass.setPipeline(OPAQUE_PARTICLE)
        renderPass.bindTexture("Sampler0", atlas)
        renderPass.drawIndexed(...)

FeatureRenderDispatcher.renderTranslucentParticles()
  → ParticleFeatureRenderer.renderTranslucent()
    → 同上,translucent=true, pipeline=TRANSLUCENT_PARTICLE
```

---

## 4. 粒子基类演变 — Particle.java

| 特性 | 1.20.1 | 1.21.1 | 26.1.2 |
|------|--------|--------|--------|
| 渲染方法 | `abstract render(VertexConsumer, Camera, float)` | 同左 | **移除** |
| 类型方法 | `abstract getRenderType()` | `abstract getRenderType()` | `abstract getGroup()` |
| RGBA 字段 | 在基类中 | 在基类中 | 移到 `SingleQuadParticle` |
| roll 字段 | 在基类中 | 在基类中 | 移到 `SingleQuadParticle` |
| 光照方法 | `getLightColor(float)` → packed light | `getLightColor(float)` | `getLightCoords(float)` |
| 光照默认值 | 0 (无区块时) | 0 | 15728640 (FULL_SKY) |
| 裁切方法 | `shouldCull()` + `getBoundingBox()` | `getRenderBoundingBox(float)` | 无(由 extract 阶段用 Frustum) |
| 限制机制 | `Optional<ParticleGroup>` | `Optional<ParticleGroup>` | `Optional<ParticleLimit>` |
| LifetimeAlpha | 无 | `record LifetimeAlpha` | `record LifetimeAlpha` |
| getPos() | 无 | `Vec3 getPos()` | `Vec3 getPos()` |
| RandomSource 参数 | 字段初始化,非参数 | 字段初始化,非参数 | 构造函数无 RandomSource 字段 |

---

## 5. 单 quad 粒子 — SingleQuadParticle

| 特性 | 1.20.1 | 1.21.1 | 26.1.2 |
|------|--------|--------|--------|
| 继承 | Particle | Particle | Particle |
| 构造函数参数 | (level, x, y, z) / (level, x, y, z, vx, vy, vz) | 同左 | (level, x, y, z, **sprite**) / (level, x, y, z, vx, vy, vz, **sprite**) |
| 渲染入口 | `render(VertexConsumer, Camera, float)` | `render(VertexConsumer, Camera, float)` | `extract(QuadParticleRenderState, Camera, float)` |
| 顶点写入 | 直接写 BufferBuilder | 直接写 BufferBuilder (新 API) | 写 QuadParticleRenderState.Storage |
| UV 来源 | 抽象方法 | 抽象方法 | sprite.getU0/U1/V0/V1() |
| FacingCameraMode | 无 | LOOKAT_XYZ / LOOKAT_Y | LOOKAT_XYZ / LOOKAT_Y |
| RenderType 方法 | 无(在子类中) | 无(在子类中) | `getGroup()` → SINGLE_QUADS |
| Layer | 无 | 无 | `abstract getLayer()` |
| 颜色字段 | 继承自 Particle 基类 | 继承自 Particle 基类 | **自己声明** rCol/gCol/bCol/alpha |
| roll 字段 | 继承自 Particle 基类 | 继承自 Particle 基类 | **自己声明** roll/oRoll |

---

## 6. 纹理粒子 — TextureSheetParticle

| 版本 | 状态 |
|------|------|
| 1.20.1 | `TextureSheetParticle extends SingleQuadParticle` — 管理 sprite,提供 UV 委托,50 行 |
| 1.21.1 | 同上,几乎不变,55 行 |
| 26.1.2 | **移除** — sprite 直接传入 `SingleQuadParticle` 构造函数,UV 委托相同 |

---

## 7. Provider 注册接口

| 特性 | 1.20.1 | 1.21.1 | 26.1.2 |
|------|--------|--------|--------|
| 接口签名 | `createParticle(T, ClientLevel, double×6)` | 同左 | `createParticle(T, ClientLevel, double×6, RandomSource)` |
| Sprite 子接口 | 返回 `TextureSheetParticle` | 返回 `TextureSheetParticle` | 返回 `SingleQuadParticle` |
| 注册位置 | `ParticleEngine.registerProviders()` | `ParticleEngine.registerProviders()` | `ParticleResources.registerProviders()` |
| 注册事件 | `RegisterParticleProvidersEvent` (Forge) | `RegisterParticleProvidersEvent` (NeoForge) | `RegisterParticleProvidersEvent` (NeoForge) |
| Provider Map | 在 ParticleEngine 中 | 在 ParticleEngine 中 | 在 ParticleResources 中,通过 `getProviders()` 暴露 |

---

## 8. 渲染调度在主循环中的位置

### 8.1 1.20.1

```
方块渲染完成
  → ParticleEngine.render(所有粒子,1 阶段)
```

### 8.2 1.21.1

```
方块渲染完成
  ├─ [FX 模式]: ParticleEngine.render(所有粒子) → 独立 particlesTarget
  └─ [普通模式]:
      ├─ solid_particles: ParticleEngine.render(!isTranslucent)  // 不透明粒子
      ├─ translucent terrain                                     // 半透明方块
      └─ particles: ParticleEngine.render(isTranslucent)         // 半透明粒子
```

### 8.3 26.1.2

```
prepareRenderState:
  → ParticleEngine.extract() → 收集粒子渲染状态

submitFeatures:
  → particlesRenderState.submit()

renderSolidFeatures:
  → 不透明方块 + 不透明模型 + ParticleFeatureRenderer.renderSolid()
    → OPAQUE_PARTICLE pipeline

renderTranslucentFeatures:
  → 半透明模型 + 文字等

translucentTerrain:
  → 半透明方块

renderTranslucentParticles:
  → ParticleFeatureRenderer.renderTranslucent()
    → TRANSLUCENT_PARTICLE pipeline, 渲染到 particleTarget
```

**关键差异**: 26.1.2 中不透明粒子在 `renderSolidFeatures` 阶段渲染(与不透明方块同一阶段),半透明粒子在 `renderTranslucentParticles` 阶段渲染(在半透明方块之后)。

---

## 9. 粒子分组机制

| 版本 | 分组结构 | 限制方式 |
|------|---------|---------|
| 1.20.1 | `Map<ParticleRenderType, Queue<Particle>>` 按 GL RenderType 分组 | `MAX_PARTICLES_PER_LAYER=16384` + `ParticleGroup.getLimit()` |
| 1.21.1 | 同上 | 同上 |
| 26.1.2 | `Map<ParticleRenderType, ParticleGroup<?>>` 按逻辑类型分组 | `MAX_PARTICLES=16384` + `ParticleLimit.limit()` |

### 26.1.2 ParticleGroup 子类

| ParticleGroup | 管理粒子类型 | 渲染状态 | 渲染方式 |
|---------------|------------|---------|---------|
| `QuadParticleGroup` | `SingleQuadParticle` | `QuadParticleRenderState` | `ParticleFeatureRenderer` → batch GPU draw |
| `ItemPickupParticleGroup` | `ItemPickupParticle` | `ItemPickupParticleGroup.State` | `EntityRenderDispatcher.submit()` |
| `ElderGuardianParticleGroup` | `ElderGuardianParticle` | `ElderGuardianParticleGroup.State` | `submitNodeCollector.submitModel()` |
| `NoRenderParticleGroup` | any | 空 | 不渲染 |

---

## 10. 顶点格式与着色器

### 10.1 VertexFormat

三版本均使用 `DefaultVertexFormat.PARTICLE`:
```
Position(3F) + UV0(2F) + Color(4UB) + UV2(2S)
```

- UV2 携带 packed light: `(block<<4) | (sky<<20)`,着色器中通过 `ivec2 UV2 / 16` 采样 16×16 lightmap
- 26.1.2 中 lightmap 直接通过 `RenderPass.bindTexture("Sampler2", lightmap, ...)` 绑定

### 10.2 着色器

| 版本 | 着色器 | 设置方式 |
|------|--------|---------|
| 1.20.1 | `GameRenderer::getParticleShader` | `RenderSystem.setShader()` |
| 1.21.1 | `GameRenderer::getParticleShader` | `RenderSystem.setShader()` |
| 26.1.2 | `core/particle` (GLSL) | `RenderPipeline.builder().withVertexShader("core/particle")...` |

### 10.3 纹理图集

| 版本 | 纹理图集来源 |
|------|-----------|
| 1.20.1 | `ParticleEngine` 自建 `TextureAtlas`,注册到 TextureManager |
| 1.21.1 | 同上 |
| 26.1.2 | `AtlasManager.PENDING_STITCH` → `AtlasIds.PARTICLES` |

粒子的纹理图集 ID 在 1.20.1 是 `new ResourceLocation("particles")`,在 1.21.1/26.1.2 是 `Identifier.withDefaultNamespace("particles")`。地形粒子使用 `TextureAtlas.LOCATION_BLOCKS`,26.1.2 新增 `TextureAtlas.LOCATION_ITEMS` 用于物品粒子。

---

## 11. 新增/移除粒子类型

### 1.21.1 新增

GUST, SMALL_GUST, GUST_EMITTER_LARGE/SMALL, WHITE_SMOKE, INFESTED, ITEM_COBWEB, DUST_PLUME,
TRIAL_SPAWNER_DETECTED_PLAYER(_OMINOUS), VAULT_CONNECTION, DUST_PILLAR, RAID_OMEN, TRIAL_OMEN, OMINOUS_SPAWNING

### 26.1.2 新增

TRAIL, PAUSE_MOB_GROWTH, RESET_MOB_GROWTH, COPPER_FIRE_FLAME,
CHERRY_LEAVES(重构为 FallingLeavesParticle), PALE_OAK_LEAVES, TINTED_LEAVES,
BLOCK_CRUMBLE, FIREFLY

### 26.1.2 移除/重构

- `TextureSheetParticle` 移除(合并入 `SingleQuadParticle`)
- `EnchantmentTableParticle` 移除(被 `FlyTowardsPositionParticle` 替代在 1.21.1)
- `MobAppearanceParticle` 移除(被 `ElderGuardianParticle` 替代)
- `CherryParticle` 移除(被 `FallingLeavesParticle` 替代)

---

## 总结

### 演进趋势

1. **分离关注点**: 1.20.1 的 `ParticleEngine` 承担了太多职责(资源加载+逻辑更新+GL渲染)。26.1.2 将其拆分为 `ParticleResources`(资源) + `ParticleEngine`(调度) + `ParticleGroup`(分组逻辑) + `ParticleFeatureRenderer`(渲染执行)。

2. **从即席 GL 到声明式 Pipeline**: 1.20.1/1.21.1 的粒子 GL 状态由 Java 匿名类控制。26.1.2 全部移到 `RenderPipeline` JSON 描述,与方块/实体渲染统一。

3. **从即时渲染到延迟渲染**: 1.20.1/1.21.1 在主线程即时绘制粒子。26.1.2 变为 extract(收集) → submit(提交) → render(独立渲染 pass 执行),使粒子渲染可与其他 feature 统一调度。

4. **粒子分组从 GL 中心变为逻辑中心**: 1.20.1/1.21.1 按 GL 混合状态分组(OPAQUE/TRANSLUCENT/LIT)。26.1.2 按逻辑行为分组(SINGLE_QUADS/ITEM_PICKUP/ELDER_GUARDIANS),GL 配置由 `Layer.renderPipeline` 决定。

5. **类型标记从重量级到轻量级**: `ParticleRenderType` 从包含 GL 逻辑的 interface(6 种,~110 行)变为纯标识 record(4 种,12 行)。

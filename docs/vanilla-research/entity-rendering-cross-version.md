# 实体渲染管线跨版本对比分析

> 基于 `entity-rendering-1.20.1.md`、`entity-rendering-1.21.1.md`、`entity-rendering-26.1.2.md` 整理。
> 仅记录版本间结构性差异,不重复各版本详细内容。

## 目录

1. [架构演进总览](#1-架构演进总览)
2. [EntityRenderDispatcher 接口变化](#2-entityrenderdispatcher-接口变化)
3. [EntityRenderer 签名与职责演变](#3-entityrenderer-签名与职责演变)
4. [渲染状态提取:从实时访问到预计算](#4-渲染状态提取从实时访问到预计算)
5. [RenderState 类型体系(26.1.2 新增)](#5-renderstate-类型体系2612-新增)
6. [Layer 系统演变](#6-layer-系统演变)
7. [阴影系统演变](#7-阴影系统演变)
8. [名牌渲染演变](#8-名牌渲染演变)
9. [拴绳渲染演变](#9-拴绳渲染演变)
10. [火焰渲染演变](#10-火焰渲染演变)
11. [调试可视化演变](#11-调试可视化演变)
12. [事件系统迁移](#12-事件系统迁移)
13. [VertexConsumer API 演变](#13-vertexconsumer-api-演变)
14. [渲染器注册演变](#14-渲染器注册演变)
15. [关键技术不变量](#15-关键技术不变量)

---

## 1. 架构演进总览

```
1.20.1 (Forge)          1.21.1 (NeoForge)        26.1.2 (NeoForge RenderState)
┌──────────────┐        ┌──────────────┐         ┌──────────────────────┐
│ 直接传Entity │   →    │ 直接传Entity │    →   │ Extract              │
│ 同步渲染     │        │ +拴绳渲染    │         │ Entity→RenderState   │
│ 即时绘制     │        │ +Attachment  │         │        ↓             │
│              │        │ +Scale支持   │         │ Submit(延迟)         │
│              │        │ +ServerSide  │         │ RenderState→Collector│
└──────────────┘        └──────────────┘         └──────────────────────┘
```

核心架构变化:

| 维度 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 渲染数据源 | Entity 引用 | Entity 引用 | EntityRenderState 副本 |
| 渲染模式 | 同步即时绘制 | 同步即时绘制 | 延迟提交(SubmitNodeCollector) |
| 线程安全 | 仅主线程 | 仅主线程 | Extract 主线程,Submit 可异步 |
| Player 渲染器 | `PlayerRenderer` | `PlayerRenderer` | `AvatarRenderer` |
| 坐标插值时机 | render 时计算 | render 时计算 | extract 时预计算 |
| 阴影计算时机 | render 时区块查询 | render 时区块查询 | extract 时预计算 ShadowPiece |
| 是否可缓存状态 | 否 | 否 | 是(RenderState 不可变快照) |

---

## 2. EntityRenderDispatcher 接口变化

### 2.1 render() → extractEntity() + submit()

```java
// 1.20.1 / 1.21.1
void render(Entity entity, double x, y, z, float yaw, float pt,
            PoseStack, MultiBufferSource buffer, int packedLight)

// 26.1.2
EntityRenderState extractEntity(Entity entity, float partialTicks)
void submit(EntityRenderState state, CameraRenderState camera,
            double x, y, z, PoseStack, SubmitNodeCollector)
```

`render()` 方法**被完全移除**,替换为两阶段:
1. `extractEntity()`:同步提取状态(游戏线程)
2. `submit()`:延迟提交渲染(可在渲染线程)

### 2.2 数据结构演变

| 字段 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| `renderers` | `Map<EntityType, EntityRenderer>` | `Map<EntityType, EntityRenderer>` | `Map<EntityType, EntityRenderer<?,?>>` |
| `playerRenderers` Key | `String` | `PlayerSkin.Model` | `PlayerModelType` |
| `playerRenderers` Value | `EntityRenderer<? extends Player>` | `EntityRenderer<? extends Player>` | `AvatarRenderer<AbstractClientPlayer>` |
| `level` | 有 | 有 | **移除** |
| `mannequinRenderers` | 无 | 无 | **新增** |
| `shouldRenderShadow` | 有 | 有 | **移除**(逻辑移至 `options.entityShadows()`) |

### 2.3 getRenderer() 新增重载

26.1.2 新增通过 `EntityRenderState` 查找渲染器的重载:
```java
// 老版本:仅支持 Entity 参数
EntityRenderer<? super T> getRenderer(T entity)

// 26.1.2:新增 RenderState 版本
EntityRenderer<?, ? super S> getRenderer(S entityRenderState)
```

### 2.4 prepare() 简化

```java
// 1.20.1/1.21.1
prepare(Level level, Camera camera, Entity crosshairPickEntity)

// 26.1.2
prepare(Camera camera, Entity crosshairPickEntity)
```

Level 参数移除,因为不再需要存储(extract 阶段从 entity 获取)。

---

## 3. EntityRenderer 签名与职责演变

### 3.1 泛型签名

```
1.20.1: EntityRenderer<T extends Entity>
1.21.1: EntityRenderer<T extends Entity>
26.1.2: EntityRenderer<T extends Entity, S extends EntityRenderState>
```

26.1.2 新增第二个泛型参数 `S`,绑定对应的 RenderState 类型。

### 3.2 核心方法对比

| 方法 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 渲染入口 | `render(T entity, yaw, pt, ps, buffer, light)` | 同左 | `submit(S state, ps, collector, camera)` |
| 状态构造 | 无 | 无 | `createRenderState()` / `createRenderState(T, float)` |
| 状态提取 | 无 | 无 | `extractRenderState(T entity, S state, float pt)` |
| 后处理 | 无 | 无 | `finalizeRenderState(T entity, S state)` |
| 偏移获取 | `getRenderOffset(T, float)` | 同左 | `getRenderOffset(S state)` |
| 阴影半径 | `shadowRadius` 字段 | `getShadowRadius(T)` 方法 | `getShadowRadius(S state)` 方法 |
| 阴影强度 | `shadowStrength` 字段 | 同左 | `getShadowStrength(S state)` 方法 |
| 拴绳 | 无(各子类自行处理) | `renderLeash()` 基类方法 | `extractRenderState` 中预计算 LeashState |
| 名牌 | `renderNameTag(T, Component, ps, buffer, light)` | `renderNameTag(T, Component, ps, buffer, light, pt)` | `submitNameDisplay(S, ps, collector, camera)` |
| 光照打包 | `LightTexture.pack()` | 同左 | `LightCoordsUtil.pack()` |

---

## 4. 渲染状态提取:从实时访问到预计算

### 4.1 数据流对比

**1.20.1/1.21.1 模式**:
```
render(entity):
  entity.getX()            // 实时插值
  entity.getYRot()         // 实时计算
  entity.walkAnimation     // 实时访问
  entity.getScale()        // 实时读取
  model.renderToBuffer()   // 即时绘制
  layer.render(entity, ...) // 传入 entity 引用
```

**26.1.2 模式**:
```
extractEntity(entity):
  state.x = lerp(oldX, getX)        // 预计算
  state.bodyRot = solveBodyRot()    // 预计算
  state.walkAnimationPos = ...      // 预计算
  state.scale = entity.getScale()   // 拷贝
  state.shadowPieces.add(...)       // 预计算阴影
  state.nameTag = ...               // 预判断名牌
  state.leashStates = [...]         // 预计算拴绳

submit(state):
  model.setupAnim(state)            // 只用 state
  submitModel(model, state)         // 延迟提交
  layer.submit(state, yRot, xRot)   // 只用 state
```

### 4.2 extractRenderState 调用链(MRO)

每个渲染器的 `extractRenderState()` 遵循父类-子类调用链:

```
EntityRenderer.extractRenderState()
  → 填充 EntityRenderState 字段
LivingEntityRenderer.extractRenderState()
  → 填充 LivingEntityRenderState 字段
ArmedEntityRenderState.extractArmedEntityRenderState()
  → 填充 ArmedEntityRenderState 字段
HumanoidMobRenderer.extractHumanoidRenderState()
  → 填充 HumanoidRenderState 字段
AvatarRenderer.extractRenderState()
  → 填充 AvatarRenderState 字段
```

每一层调用 `super.extractRenderState()` 确保基类字段也被填充。

### 4.3 预计算收益

| 旧架构 | 新架构 |
|---|---|
| render 时多次 `lerp(old, new)` | extract 时一次 `lerp` 存入 state |
| render 时 `getPackedLightCoords()` 查区块 | extract 时预计算 `lightCoords` |
| render 时 `shouldShowName()` 判断 | extract 时判断并置 `nameTag=null` |
| render 时遍历区块查阴影 | extract 时遍历存 ShadowPiece |
| 每帧多次访问 `Entity` 可变字段 | `EntityRenderState` 不可变快照 |

---

## 5. RenderState 类型体系(26.1.2 新增)

### 5.1 类数量

- `EntityRenderState` 的直接子类:约 80+ 个(几乎所有实体渲染器都有对应的 RenderState)
- 层级深度:最多 6 层(`BaseRenderState → EntityRenderState → LivingEntityRenderState → ArmedEntityRenderState → HumanoidRenderState → AvatarRenderState`)
- 每个 RenderState 文件平均约 30-60 行,纯数据字段 + 少量辅助方法

### 5.2 核心中间类

```
EntityRenderState                  ← 所有实体的基状态(x,y,z,age,light,shadow,outline,...)
 ├─ LivingEntityRenderState        ← 所有生物(bodyRot,yRot,xRot,walkAnim,scale,pose,...)
 │   ├─ ArmedEntityRenderState     ← 可持武器(mainArm,handItemState,attackTime,...)
 │   │   └─ HumanoidRenderState    ← 人形(swim,crouch,fallFlying,equipment,...)
 │   │       └─ AvatarRenderState  ← 玩家/人形Avatar(skin,cape,partVisibility,...)
 │   ├─ IllagerRenderState         ← 灾厄村民基类
 │   ├─ UndeadRenderState          ← 亡灵生物基类
 │   └─ ... (约 70+ 子类)
 ├─ ItemEntityRenderState          ← 掉落物
 ├─ ArrowRenderState               ← 箭/投掷物基类
 └─ ... (约 20+ 非生物状态)
```

### 5.3 RenderState 内部类

`EntityRenderState` 包含两个静态内部类:

1. **LeashState**(6字段):拴绳的起点/终点/光照数据
2. **ShadowPiece**(record,5字段):预计算的阴影片(相对位置 + VoxelShape + alpha)

---

## 6. Layer 系统演变

### 6.1 接口变化

```java
// 1.20.1 / 1.21.1
public abstract class RenderLayer<T extends Entity, M extends EntityModel<T>> {
    public abstract void render(PoseStack, MultiBufferSource, int packedLight,
        T entity, float limbSwing, float limbSwingAmount,
        float partialTick, float ageInTicks, float netHeadYaw, float headPitch);
}

// 26.1.2
public abstract class RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>> {
    public abstract void submit(PoseStack, SubmitNodeCollector, int lightCoords,
        S state, float yRot, float xRot);
}
```

### 6.2 参数简化

| 老参数 | 新处理 |
|---|---|
| `limbSwing` | 从 `state.walkAnimationPos` 读取 |
| `limbSwingAmount` | 从 `state.walkAnimationSpeed` 读取 |
| `partialTick` | 从 `state.partialTick` 读取 |
| `ageInTicks` | 从 `state.ageInTicks` 读取 |
| `netHeadYaw` | 从 `state.yRot` 读取 |
| `headPitch` | 从 `state.xRot` 读取 |
| `packedLight` | 从 `state.lightCoords` 读取 |
| `MultiBufferSource` | 替换为 `SubmitNodeCollector` |

Layer 不再需要知道动画参数的计算过程,只需从 RenderState 读取结果。

### 6.3 颜色参数

- 1.20.1:分离 `float red, float green, float blue`
- 1.21.1:打包 `int color`(ARGB)
- 26.1.2:打包 `int color` + `int order`(排序索引)

---

## 7. 阴影系统演变

### 7.1 计算时机

| 版本 | 计算阶段 | 位置 |
|---|---|---|
| 1.20.1 | render 时实时 | `EntityRenderDispatcher.renderShadow()` → 遍历区块 |
| 1.21.1 | render 时实时 | 同1.20.1,但使用 `FastColor.ARGB32.color()` 打包颜色 |
| 26.1.2 | extract 时预计算 | `EntityRenderer.extractShadow()` → 存 `List<ShadowPiece>` |

### 7.2 数据结构

| 版本 | 阴影数据 |
|---|---|
| 1.20.1 | 无存储,每次即时计算并直接绘制顶点 |
| 1.21.1 | 同 1.20.1,vertexConsumer `addVertex` 支持 packed color |
| 26.1.2 | `ShadowPiece(relativeX, relativeY, relativeZ, VoxelShape, alpha)` |

### 7.3 阴影半径获取

| 版本 | 方法 |
|---|---|
| 1.20.1 | `entityrenderer.shadowRadius` (直接读字段) |
| 1.21.1 | `entityrenderer.getShadowRadius(entity)` (方法,含缩放) |
| 26.1.2 | `this.getShadowRadius(state)` (方法,从 state 读缩放) |

### 7.4 条件判断位置

| 版本 | 位置 |
|---|---|
| 1.20.1 | `EntityRenderDispatcher.render()` 内联判断 |
| 1.21.1 | 同 1.20.1 |
| 26.1.2 | `EntityRenderer.extractShadow()` 集中判断,存入 `shadowRadius=0` 表示无阴影 |

---

## 8. 名牌渲染演变

### 8.1 触发时机

| 版本 | 时机 |
|---|---|
| 1.20.1 | EntityRenderer.render() 末尾,基于 shouldShowName() 判断 |
| 1.21.1 | 同1.20.1,但使用 EntityAttachment.NAME_TAG 获取位置 |
| 26.1.2 | EntityRenderer.extractRenderState() 中预判断,存入 state.nameTag/scoreText |

### 8.2 位置计算

| 版本 | 方法 |
|---|---|
| 1.20.1 | `entity.getNameTagOffsetY()` 硬编码 Y 偏移 |
| 1.21.1 | `entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, yRot)` |
| 26.1.2 | 同 1.21.1,在 extract 阶段计算存入 `state.nameTagAttachment` |

### 8.3 Forge 事件

| 版本 | 事件 |
|---|---|
| 1.20.1 | `RenderNameTagEvent(entity, ...)` 在 `MinecraftForge.EVENT_BUS` |
| 1.21.1 | `RenderNameTagEvent(entity, ...)` 在 `NeoForge.EVENT_BUS` |
| 26.1.2 | `RenderNameTagEvent.CanRender(entity, state, ...)` + `RenderNameTagEvent.DoRender(state, ...)` 分离为两个事件 |

26.1.2 将名牌渲染事件拆分为:
- `CanRender`:在 extract 阶段,决定是否渲染+修改内容
- `DoRender`:在 submit 阶段,允许拦截实际绘制

---

## 9. 拴绳渲染演变

### 9.1 渲染位置

| 版本 | 位置 |
|---|---|
| 1.20.1 | 分散在各实体渲染器或根本不处理 |
| 1.21.1 | `EntityRenderer.renderLeash()` 集中处理 |
| 26.1.2 | extract 阶段预计算 `LeashState`,submit 阶段 `submitNodeCollector.submitLeash()` |

### 9.2 四连接拴绳

仅在 26.1.2 支持:
- `supportQuadLeash()` + `supportQuadLeashAsHolder()` 决定是否启用
- 生成 4 个 `LeashState`,而非 1 个
- 使用 `getQuadLeashOffsets()` / `getQuadLeashHolderOffsets()` 获取连接点

### 9.3 光照插值

| 版本 | 方式 |
|---|---|
| 1.21.1 | renderLeash 内部 `lerp` block/sky light |
| 26.1.2 | LeashState 存储端点值,submit 时 lerp |

---

## 10. 火焰渲染演变

### 10.1 相机旋转

| 版本 | 方式 |
|---|---|
| 1.20.1 | `poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()))` |
| 1.21.1 | `renderFlame(poseStack, buffer, entity, Mth.rotationAroundAxis(Mth.Y_AXIS, cameraOrientation, ...))` |
| 26.1.2 | `submitNodeCollector.submitFlame(poseStack, state, quaternion)` — 使用相同 Quaternion |

### 10.2 火焰推进方向

| 版本 | 方向 |
|---|---|
| 1.20.1 | 从前向后(translate z:-0.3, f5 += 0.03) |
| 1.21.1 | 从后向前(translate z:+0.3, f5 -= 0.03) |

### 10.3 渲染模式

| 版本 | 模式 |
|---|---|
| 1.20.1/1.21.1 | 即时绘制 `fireVertex()` |
| 26.1.2 | 延迟提交 `submitNodeCollector.submitFlame()` |

---

## 11. 调试可视化演变

| 功能 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 碰撞箱 | 有 | 有(增强) | 有 |
| 部件实体碰撞箱 | 有 | 有 | 有 |
| 视线方向向量 | 有 | 有 | 有 |
| 载具连接点 | 无 | **新增**(黄色) | 有 |
| 服务端实体 Hitbox | 无 | **新增**(品红色) | 有 |
| `renderHitBoxes` 开关 | 字段 | 字段 | **移除**(使用 options 或其他方式) |

---

## 12. 事件系统迁移

### 12.1 事件总线

| 版本 | 总线 |
|---|---|
| 1.20.1 | `net.minecraftforge.common.MinecraftForge.EVENT_BUS` |
| 1.21.1 | `net.neoforged.neoforge.common.NeoForge.EVENT_BUS` |
| 26.1.2 | `net.neoforged.neoforge.common.NeoForge.EVENT_BUS` |

### 12.2 事件类包名

| 版本 | 包 |
|---|---|
| 1.20.1 | `net.minecraftforge.client.event.*` |
| 1.21.1 | `net.neoforged.neoforge.client.event.*` |
| 26.1.2 | `net.neoforged.neoforge.client.event.*` |

### 12.3 事件参数变化(26.1.2)

| 事件 | 变化 |
|---|---|
| `RenderLivingEvent.Pre` | Entity → EntityRenderState,新增 SubmitNodeCollector |
| `RenderLivingEvent.Post` | 同 Pre 的泛型增加 RenderState 参数 |
| `RenderNameTagEvent` | 拆分为 `CanRender`(extract 阶段) 和 `DoRender`(submit 阶段) |
| `RenderPlayerEvent` | Entity → AvatarRenderState |
| `AddLayers` | 新增 mannequinRenderers 参数 |

---

## 13. VertexConsumer API 演变

### 13.1 顶点构建模式

```java
// 1.20.1:多参单次调用
buffer.vertex(matrix, x, y, z).color(r, g, b, a).uv(u, v)
      .overlayCoords(o1, o2).uv2(light).normal(matrix, nx, ny, nz).endVertex();

// 1.21.1:链式调用
buffer.addVertex(matrix, x, y, z).setColor(color).setUv(u, v)
      .setUv1(o1, o2).setLight(light).setNormal(matrix, nx, ny, nz);

// 26.1.2:提交抽象,不直接接触 VertexConsumer
submitNodeCollector.submitModel(model, state, ...);
```

| 版本 | 顶点 API 特点 |
|---|---|
| 1.20.1 | `vertex().color(r,g,b,a).uv(u,v).overlayCoords().uv2().normal().endVertex()` |
| 1.21.1 | `addVertex().setColor(int).setUv().setUv1().setLight().setNormal()` |
| 26.1.2 | 不直接调 VertexConsumer,通过 `SubmitNodeCollector` 提交 |

### 13.2 颜色打包

| 版本 | 方式 |
|---|---|
| 1.20.1 | `color(float r, float g, float b, float a)` |
| 1.21.1 | `setColor(int)` — 使用 `FastColor.ARGB32.color()` 打包 |
| 26.1.2 | `submitModel(model, ..., int color, ...)` — packed int |

---

## 14. 渲染器注册演变

### 14.1 EntityRenderers

| 方面 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| 注册方法 | `register(EntityType, EntityRendererProvider)` | 同左 | 同左 |
| 创建方法 | `createEntityRenderers()` + `createPlayerRenderers()` | 同左 | `createEntityRenderers()` + `createAvatarRenderers()`(两者共用) |
| Player 渲染器类 | `PlayerRenderer` | `PlayerRenderer` | `AvatarRenderer` |

### 14.2 EntityRendererProvider.Context

| 依赖 | 1.20.1 | 1.21.1 | 26.1.2 |
|---|---|---|---|
| EntityRenderDispatcher | 有 | 有 | 有 |
| ItemRenderer | 有 | 有 | **移除** |
| BlockRenderDispatcher | 有 | 有 | **移除** |
| BlockModelResolver | 无 | 无 | **新增** |
| ItemModelResolver | 无 | 无 | **新增** |
| ItemInHandRenderer | 有 | 有 | 有 |
| EntityModelSet | 有(直接值) | 同左 | 有(从 Supplier) |
| EquipmentAssetManager | 无 | 无 | **新增** |
| AtlasManager | 无 | 无 | **新增** |
| PlayerSkinRenderCache | 无 | 无 | **新增** |
| MapRenderer | 无 | 无 | **新增** |

---

## 15. 关键技术不变量

### 15.1 跨版本恒定的设计

1. **姿态处理顺序**:Death > SpinAttack > SLEEPING > 倒立 > 默认。三个版本完全一致。
2. **坐标系翻转**:`poseStack.scale(-1, -1, 1)` — 所有版本在模型渲染前执行。
3. **模型原点偏移**:`poseStack.translate(0, -1.501, 0)` — 所有版本一致。
4. **Layer 调度位置**:主模型渲染后、poseStack.popPose() 前。
5. **阴影最大半径**:`MAX_SHADOW_RADIUS = 32.0F` — 所有版本一致。
6. **阴影衰减公式**:`weight = (1.0 - distSq/256.0) * shadowStrength` — 所有版本一致。
7. **名牌缩放**:`NAMETAG_SCALE = 0.025F` — 所有版本一致。
8. **火焰纹理**:FIRE_0/FIRE_1 交替,`Sheets.cutoutBlockSheet()` — 所有版本一致。
9. **Overlay 打包**:`OverlayTexture.pack(u, v)` — 所有版本一致。
10. **受伤红色覆盖**:`hurtTime > 0 || deathTime > 0` — 所有版本一致。

### 15.2 版本特有差异汇总

| 特性 | 仅在版本 |
|---|---|
| `LightTexture` 打包光照 | 1.20.1, 1.21.1 |
| `LightCoordsUtil` 打包光照 | 26.1.2 |
| `EntityAttachment` 系统 | 1.21.1+, 26.1.2 |
| `RenderLayer.submit()` | 26.1.2 |
| `SubmitNodeCollector` 延迟提交 | 26.1.2 |
| `EntityRenderState` 状态分离 | 26.1.2 |
| `AvatarRenderer` | 26.1.2 |
| `ClientMannequin` 假人渲染 | 26.1.2 |
| 四连接拴绳(Quad Leash) | 26.1.2 |
| RenderNameTagEvent 拆分(CanRender/DoRender) | 26.1.2 |
| `PlayerSkin.Model` 枚举 | 1.21.1+, 26.1.2 |
| `PlayerModelType` 枚举 | 26.1.2 |
| `FastColor.ARGB32` | 1.21.1+, 26.1.2 |
| `Mth.rotationAroundAxis` | 1.21.1+, 26.1.2 |
| `RenderTypes` 类(替代 `RenderType`) | 26.1.2 |

### 15.3 向后兼容性断裂点

26.1.2 的核心断裂:
1. `EntityRenderer.render()` 签名完全改变 → `submit()` 替代
2. 所有 Layer 从 `render()` → `submit()` 
3. `EntityRenderer` 泛型从 1 个变为 2 个
4. 阴影从即时计算变为预计算 `ShadowPiece`
5. `MultiBufferSource` → `SubmitNodeCollector`
6. 所有 Override 点需要同时实现 `createRenderState()` + `extractRenderState()` + `submit()`

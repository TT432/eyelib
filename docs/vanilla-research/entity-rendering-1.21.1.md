# 1.21.1 (NeoForge) Minecraft Vanilla Entity Rendering Pipeline 分析报告

> 基于 `.local_ref/mc/1.21.1/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [版本变化概述](#1-版本变化概述)
2. [EntityRenderDispatcher — 调度中心](#2-entityrenderdispatcher--调度中心)
3. [EntityRenderer — 基类(新增拴绳)**](#3-entityrenderer--基类新增拴绳)
4. [LivingEntityRenderer — 模型+Layer 渲染](#4-livingentityrenderer--模型layer-渲染)
5. [RenderLayer 系统](#5-renderlayer-系统)
6. [阴影系统](#6-阴影系统)
7. [名牌渲染(使用 EntityAttachment)](#7-名牌渲染使用-entityattachment)
8. [拴绳渲染](#8-拴绳渲染)
9. [调试可视化(Vehicle/Vector/ServerSide)](#9-调试可视化vehiclevelectorserverside)
10. [火焰渲染](#10-火焰渲染)
11. [渲染器注册与重载](#11-渲染器注册与重载)

---

## 1. 版本变化概述

相比 1.20.1,1.21.1 的实体渲染管线发生以下主要变化:

| 变化 | 影响 |
|---|---|
| 玩家渲染器 Key 从 `String` 变为 `PlayerSkin.Model` | 类型安全,支持 `WIDE`/`SLIM` 枚举 |
| EntityRenderer 新增拴绳渲染 | 基类直接处理拴绳,不再分散到各渲染器 |
| 名牌 Attachment 系统 | 使用 `EntityAttachment.NAME_TAG` 代替硬编码 Y 偏移 |
| VertexConsumer API 更新 | `addVertex/setColor/setLight` 链式调用替代旧式多参方法 |
| 阴影颜色打包 | `FastColor.ARGB32.color()` 代替分离的 R,G,B,A float |
| 实体 Scale 支持 | `setupRotations()` 接受 scale 参数,倒立平移用 scale 计算 |
| 服务端调试 Hitbox | 新增 `renderServerSideHitbox` 显示服务端实体位置 |
| `shouldRender()` 拴绳检查 | 被拴住且超出视锥时,检查拴绳持有者可见性 |
| `renderFlame()` Quaternion | 接受 Quaternionf 参数,不再直接读 `camera.getYRot()` |
| NeoForge 事件总线 | 所有 Forge 事件迁移到 `NeoForge.EVENT_BUS` |

---

## 2. EntityRenderDispatcher — 调度中心

**文件**: `net/minecraft/client/renderer/entity/EntityRenderDispatcher.java` (489行)

### 2.1 数据结构 (第59–80行)

```java
public class EntityRenderDispatcher implements ResourceManagerReloadListener {
    private static final RenderType SHADOW_RENDER_TYPE =
        RenderType.entityShadow(ResourceLocation.withDefaultNamespace("textures/misc/shadow.png"));
    private static final float MAX_SHADOW_RADIUS = 32.0F;
    private static final float SHADOW_POWER_FALLOFF_Y = 0.5F;
    public Map<EntityType<?>, EntityRenderer<?>> renderers = ImmutableMap.of();
    private Map<PlayerSkin.Model, EntityRenderer<? extends Player>> playerRenderers = Map.of();
    // ... 其它字段与 1.20.1 相同
}
```

**关键变化**: `playerRenderers` 的 Key 类型从 `String` 变为 `PlayerSkin.Model` 枚举(`WIDE`/`SLIM`),不再使用字符串 `"default"`/`"slim"`。

### 2.2 render() — 核心渲染方法 (第142–195行)

流程与 1.20.1 基本相同,差异:

1. **火焰渲染** (第163–165行):
```java
this.renderFlame(poseStack, buffer, entity,
    Mth.rotationAroundAxis(Mth.Y_AXIS, this.cameraOrientation, new Quaternionf()));
```
传入 `Quaternionf` 而非直接从 Camera 读取 YRot,使用 `cameraOrientation` + `rotationAroundAxis` 计算。

2. **阴影渲染** (第168–177行):使用新增的 `getShadowRadius(entity)` 方法(1.20.1 直接读 `shadowRadius` 字段),允许 LivingEntityRenderer 按 scale 缩放。

3. **调试 Hitbox** (第179–181行):新增颜色参数 `(1.0F, 1.0F, 1.0F)` 和 `renderServerSideHitbox` 逻辑。

### 2.3 getRenderer() — 渲染器查找 (第104–112行)

```java
public <T extends Entity> EntityRenderer<? super T> getRenderer(T entity) {
    if (entity instanceof AbstractClientPlayer abstractclientplayer) {
        PlayerSkin.Model model = abstractclientplayer.getSkin().model();
        EntityRenderer<? extends Player> r = this.playerRenderers.get(model);
        return (EntityRenderer<? super T>)(r != null ? r : this.playerRenderers.get(PlayerSkin.Model.WIDE));
    } else {
        return (EntityRenderer<? super T>)this.renderers.get(entity.getType());
    }
}
```

与 1.20.1 不同:使用 `PlayerSkin.Model` 而非 `getModelName()` 字符串。

### 2.4 renderHitbox() — 调试碰撞箱 (第223–292行)

新增功能:
1. **Vehicle 附着点**:渲染乘客在载具上的挂接位置(黄色线框)。
2. **视线向量**:调用 `renderVector()` 渲染从眼睛出发的视线方向。
3. **服务端实体**:`renderServerSideHitbox()` 可在集成服务器模式下显示服务端实体位置差异(品红色/半透明)。

### 2.5 renderServerSideHitbox() (第197–208行)

通过 `IntegratedServer.getLevel()` 获取服务端 Level,查找同 ID 的服务端实体,在服务端位置渲染品红色 Hitbox,用于调试客户端/服务端位置不同步。

---

## 3. EntityRenderer — 基类(新增拴绳)

**文件**: `net/minecraft/client/renderer/entity/EntityRenderer.java` (219行)

### 3.1 新增常量 (第27–28行)

```java
public static final int LEASH_RENDER_STEPS = 24;
```

### 3.2 shouldRender() — 拴绳可见性 (第52–83行)

```java
public boolean shouldRender(T entity, Frustum camera,
    double camX, double camY, double camZ) {
    // ... 原逻辑 ...
    if (camera.isVisible(aabb)) {
        return true;
    } else {
        if (entity instanceof Leashable leashable) {
            Entity entity = leashable.getLeashHolder();
            if (entity != null) {
                return camera.isVisible(entity.getBoundingBoxForCulling());
            }
        }
        return false;
    }
}
```

新增:若实体自身不可见但被拴住,且拴绳持有者可见,则仍需渲染(拴绳必须可见)。

### 3.3 render() — 基类渲染(含拴绳) (第89–103行)

```java
public void render(T entity, float entityYaw, float partialTick,
    PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
    if (entity instanceof Leashable leashable) {
        Entity leashHolder = leashable.getLeashHolder();
        if (leashHolder != null) {
            this.renderLeash(entity, partialTick, poseStack, bufferSource, leashHolder);
        }
    }
    // ... 名牌渲染(NeoForge 事件) ...
}
```

**关键变化**:拴绳渲染从各子类/外部移到 EntityRenderer 基类,统一处理。

### 3.4 renderLeash() — 拴绳渲染 (第105–141行)

```java
private <E extends Entity> void renderLeash(T entity, float partialTick,
    PoseStack poseStack, MultiBufferSource bufferSource, E leashHolder)
```

流程:
1. 获取拴绳持有者位置 `getRopeHoldPosition()`
2. 获取被拴实体 body rotation(`getPreciseBodyRotation`)
3. 获取拴绳偏移 `getLeashOffset()`,旋转到世界空间
4. 插值实体位置并平移到拴绳连接点
5. 使用 `RenderType.leash()` 渲染 24 段分段(往返 48 个顶点)
6. 光照沿拴绳插值:block/sky 光照在两端的值之间 lerp

**光照插值 (第127–131行)**:
```java
int entityBlockLight = this.getBlockLightLevel(entity, entityEyePos);
int holderBlockLight = dispatcher.getRenderer(leashHolder).getBlockLightLevel(leashHolder, holderEyePos);
int entitySkyLight = level.getBrightness(LightLayer.SKY, entityEyePos);
int holderSkyLight = level.getBrightness(LightLayer.SKY, holderEyePos);
```

每段的光照在两端值之间线性插值。

### 3.5 renderNameTag() — 名牌(使用 Attachment) (第188–214行)

```java
protected void renderNameTag(T entity, Component displayName,
    PoseStack poseStack, MultiBufferSource bufferSource,
    int packedLight, float partialTick)
```

与 1.20.1 不同:
1. 使用 `EntityAttachment.NAME_TAG` 获取名牌位置:`entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick))`
2. 多了 `partialTick` 参数(签名变化)
3. NeoForge `ClientHooks.isNameplateInRenderDistance()` 替代旧的 Forge 方法
4. 名牌缩放:从 `(-0.025, -0.025, 0.025)` 变为 `(0.025, -0.025, 0.025)`,不再有 -1 翻转(因为 LivingEntityRenderer 已翻转)

### 3.6 getShadowRadius() — 新增方法 (第216–217行)

```java
protected float getShadowRadius(T entity) {
    return this.shadowRadius;
}
```

新增虚方法,允许 LivingEntityRenderer 按实体 scale 缩放阴影半径。

---

## 4. LivingEntityRenderer — 模型+Layer 渲染

**文件**: `net/minecraft/client/renderer/entity/LivingEntityRenderer.java` (285行)

### 4.1 render() — 完整渲染流程 (第52–140行)

与 1.20.1 的主要差异:

1. **实体 Scale 支持** (第97–100行):
```java
float f8 = entity.getScale();
poseStack.scale(f8, f8, f8);
float f9 = this.getBob(entity, partialTicks);
this.setupRotations(entity, poseStack, f9, f, partialTicks, f8);
```
在旋转前先按实体 scale 缩放,scale 传入 setupRotations 用于倒立平移计算。

2. **头 Yaw 的 wrapDegrees** (第88行):
```java
f2 = Mth.wrapDegrees(f2);
```
1.20.1 不做 wrap,1.21.1 增加。

3. **模型渲染颜色** (第128–129行):
```java
this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, i,
    flag1 ? 654311423 : -1);
```
半透明时使用 `654311423`(带 alpha 的 packed color)而非 `(1,1,1,0.15)`。

4. **Pre/Post 事件迁移** (第53,139行):使用 `NeoForge.EVENT_BUS.post().isCanceled()` 而非 `MinecraftForge.EVENT_BUS.post()`。

### 4.2 setupRotations() — 姿态变换 (第181–211行)

签名变化:新增 `float scale` 参数。

```java
protected void setupRotations(T entity, PoseStack poseStack,
    float bob, float yBodyRot, float partialTick, float scale)
```

倒立时 (第207–210行):
```java
poseStack.translate(0.0F, (entity.getBbHeight() + 0.1F) / scale, 0.0F);
poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
```

除以 `scale` 使得倒立平移与实体缩放保持一致。

### 4.3 getShadowRadius() — 阴影半径缩放 (第282–284行)

```java
protected float getShadowRadius(T entity) {
    return super.getShadowRadius(entity) * entity.getScale();
}
```

新增重写:阴影半径随实体 scale 缩放。

---

## 5. RenderLayer 系统

**文件**: `net/minecraft/client/renderer/entity/layers/RenderLayer.java` (75行)

### 5.1 与 1.20.1 的差异

1. **颜色参数类型变化**:
   - 1.20.1:`renderColoredCutoutModel` 接受分离的 `(float red, float green, float blue)`
   - 1.21.1:接受单个 `int color`(ARGB packed)

2. **新增 partialTick 参数**:
   - `coloredCutoutModelCopyLayerRender()` 新增 `float partialTick` 参数

3. **NeoForge 注解**:`@OnlyIn(Dist.CLIENT)` 使用 `net.neoforged.api.distmarker` 包。

### 5.2 render() 签名

```java
public abstract void render(PoseStack poseStack, MultiBufferSource bufferSource,
    int packedLight, T livingEntity, float limbSwing, float limbSwingAmount,
    float partialTick, float ageInTicks, float netHeadYaw, float headPitch);
```

与 1.20.1 完全一致。

---

## 6. 阴影系统

### 6.1 阴影调度

`EntityRenderDispatcher.render()` 第168–177行,与 1.20.1 等价但使用 `getShadowRadius(entity)` 获取动态阴影半径。

### 6.2 renderShadow() (第361–390行)

与 1.20.1 基本相同的算法,差异:
- 变量命名规范化(如 `f1 = weight - ...` 替代旧的 `f2`)
- 使用标准 `Mth.floor` 截断

### 6.3 renderBlockShadow() (第392–441行)

**关键变化**:阴影顶点颜色打包方式。

```java
int i = FastColor.ARGB32.color(Mth.floor(f1 * 255.0F), 255, 255, 255);
```

使用 `FastColor.ARGB32.color(alpha, r, g, b)` 打包为 int,而不是 1.20.1 的分离 float 参数。

### 6.4 shadowVertex() (第443–448行)

```java
consumer.addVertex(vector3f.x(), vector3f.y(), vector3f.z(),
    color, u, v, OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 1.0F, 0.0F);
```

使用新 `addVertex` API,颜色为 packed int。

---

## 7. 名牌渲染(使用 EntityAttachment)

### 7.1 Attachment 系统

1.21.1 引入 `EntityAttachment` 系统,名牌位置不再硬编码:

```java
Vec3 vec3 = entity.getAttachments().getNullable(
    EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
```

`EntityAttachment.NAME_TAG` 是预定义的附着点,每种实体可定义不同的名牌偏移量。

### 7.2 shouldShowName() — 可见性逻辑

与 1.20.1 相同(距离/队伍/潜行检查),但 `LivingEntityRenderer` 重写增加 `crosshairPickEntity` 条件。

---

## 8. 拴绳渲染

详见 [3.4 节](#34-renderleash--拴绳渲染-第105-141行)。

拴绳由 24 段组成,每段两个顶点(往返共 48)。光照在两端的 block/sky light 值之间线性插值。使用 `RenderType.leash()` 渲染类型。

VertexConsumer 调用模式更新:
```java
buffer.addVertex(pose, x, y, z).setColor(r, g, b, a).setLight(k);
```

链式调用风格,与 1.20.1 的多参单次调用不同。

---

## 9. 调试可视化(Vehicle/Vector/ServerSide)

### 9.1 renderHitbox() 新增

1.21.1 的 `renderHitbox()` (第223–292行) 相比 1.20.1 新增:
1. **载具连接点**(黄色):`renderLineBox()` 在乘客挂接位置
2. **视线向量**(蓝色):`renderVector()` 从眼睛位置到视线方向 ×2
3. **多部件实体渲染**:PartEntity 遍历

### 9.2 renderVector() (第294–307行)

```java
private static void renderVector(PoseStack poseStack, VertexConsumer buffer,
    Vector3f startPos, Vec3 vector, int color)
```

绘制从 `startPos` 到 `startPos + vector` 的线段。

---

## 10. 火焰渲染

### 10.1 renderFlame() (第309–348行)

```java
private void renderFlame(PoseStack poseStack, MultiBufferSource buffer,
    Entity entity, Quaternionf quaternion)
```

与 1.20.1 的关键差异:
1. 接受 `Quaternionf quaternion` 参数,旋转不再依赖 `this.camera.getYRot()`,使用 `poseStack.mulPose(quaternion)`。
2. 平移方向反转:从 `translate(0,0,-0.3F)` 变为 `translate(0,0,0.3F)`,quad 顶点顺序反转(外翻)。
3. 火焰推进方向:从 `f5 += 0.03F` 变为 `f5 -= 0.03F`。

### 10.2 fireVertex() (第350–359行)

使用新 VertexConsumer API:
```java
buffer.addVertex(matrixEntry, x, y, z)
    .setColor(-1)          // ARGB white
    .setUv(texU, texV)
    .setUv1(0, 10)         // overlay
    .setLight(240)         // FULL_BLOCK
    .setNormal(matrixEntry, 0.0F, 1.0F, 0.0F);
```

颜色从 `(255,255,255,255)` 变为 `-1`(即 `0xFFFFFFFF`)。

---

## 11. 渲染器注册与重载

### 11.1 EntityRenderers

```java
register(EntityType.ZOMBIE, ZombieRenderer::new);
register(EntityType.SKELETON, SkeletonRenderer::new);
// ...
```

注册机制与 1.20.1 相同,使用静态 `PROVIDERS` Map。

### 11.2 资源重载 (onResourceManagerReload,第480–488行)

```java
this.renderers = EntityRenderers.createEntityRenderers(context);
this.playerRenderers = EntityRenderers.createPlayerRenderers(context);
NeoForge.ModLoader.postEvent(new AddLayers(renderers, playerRenderers, context));
```

`NeoForge.ModLoader.postEvent()` 替代 `Forge ModLoader.get().postEvent()`。

### 11.3 getSkinMap() (第476–478行)

```java
public Map<PlayerSkin.Model, EntityRenderer<? extends Player>> getSkinMap() {
    return java.util.Collections.unmodifiableMap(playerRenderers);
}
```

返回类型从 `Map<String, ...>` 变为 `Map<PlayerSkin.Model, ...>`。

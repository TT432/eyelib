# 1.20.1 (Forge) Minecraft Vanilla Entity Rendering Pipeline 分析报告

> 基于 `.local_ref/mc/1.20.1/sources/` 提取源码。 源码树由 `scripts/extract-mc-source.py` 重建。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [架构概览与核心类](#1-架构概览与核心类)
2. [EntityRenderDispatcher — 调度中心](#2-entityrenderdispatcher--调度中心)
3. [EntityRenderer — 基类](#3-entityrenderer--基类)
4. [LivingEntityRenderer — 模型+Layer 渲染](#4-livingentityrenderer--模型layer-渲染)
5. [RenderLayer 系统](#5-renderlayer-系统)
6. [阴影系统](#6-阴影系统)
7. [名牌渲染](#7-名牌渲染)
8. [受伤闪白与 Overlay](#8-受伤闪白与-overlay)
9. [火焰渲染](#9-火焰渲染)
10. [具体渲染器:PlayerRenderer 与 MobRenderer](#10-具体渲染器playerrenderer-与-mobrenderer)
11. [渲染器注册与重载](#11-渲染器注册与重载)

---

## 1. 架构概览与核心类

实体渲染管线的入口是 `EntityRenderDispatcher`，它在 `LevelRenderer.renderEntities()` 中被调用。管线采用**调度器-渲染器**两层模型:

```
LevelRenderer
  └→ EntityRenderDispatcher.render(entity, x, y, z, yaw, pt, poseStack, buffer, packedLight)
       ├→ getRenderer(entity)         → 根据 EntityType 或 PlayerSkin 查找 EntityRenderer
       ├→ entityrenderer.render(...)  → 委托给具体渲染器
       ├→ renderFlame()               → 着火火焰叠加
       ├→ renderShadow()              → 地面阴影(可选)
       └→ renderHitbox()              → 调试碰撞箱(可选)
```

### 核心类位置

| 类 | 文件路径 |
|---|---|
| EntityRenderDispatcher | `net/minecraft/client/renderer/entity/EntityRenderDispatcher.java` (370行) |
| EntityRenderer | `net/minecraft/client/renderer/entity/EntityRenderer.java` (109行) |
| LivingEntityRenderer | `net/minecraft/client/renderer/entity/LivingEntityRenderer.java` (280行) |
| RenderLayer | `net/minecraft/client/renderer/entity/layers/RenderLayer.java` (48行) |
| EntityRenderers | `net/minecraft/client/renderer/entity/EntityRenderers.java` |
| RenderLayerParent | `net/minecraft/client/renderer/entity/RenderLayerParent.java` |
| OverlayTexture | `net/minecraft/client/renderer/texture/OverlayTexture.java` |

---

## 2. EntityRenderDispatcher — 调度中心

**文件**: `net/minecraft/client/renderer/entity/EntityRenderDispatcher.java` (370行)

### 2.1 数据结构 (第57–80行)

```java
public class EntityRenderDispatcher implements ResourceManagerReloadListener {
    private static final RenderType SHADOW_RENDER_TYPE = RenderType.entityShadow(new ResourceLocation("textures/misc/shadow.png"));
    private static final float MAX_SHADOW_RADIUS = 32.0F;
    private static final float SHADOW_POWER_FALLOFF_Y = 0.5F;
    public Map<EntityType<?>, EntityRenderer<?>> renderers = ImmutableMap.of();
    private Map<String, EntityRenderer<? extends Player>> playerRenderers = ImmutableMap.of();
    public final TextureManager textureManager;
    private Level level;
    public Camera camera;
    private Quaternionf cameraOrientation;
    public Entity crosshairPickEntity;
    private final BlockRenderDispatcher blockRenderDispatcher;
    private final ItemRenderer itemRenderer;
    private final ItemInHandRenderer itemInHandRenderer;
    private final Font font;
    public final Options options;
    private final EntityModelSet entityModels;
    private boolean shouldRenderShadow = true;
    private boolean renderHitBoxes;
}
```

- `renderers`: `EntityType → EntityRenderer` 映射,存储所有非玩家实体渲染器。
- `playerRenderers`: `String(skin model name) → EntityRenderer` 映射,玩家皮肤分 `"default"`(Steve宽)和 `"slim"`(Alex窄)。
- `shouldRenderShadow`: 控制是否渲染阴影,可通过 `setRenderShadow(boolean)` 动态开关。

### 2.2 render() — 核心渲染方法 (第133–173行)

```java
public <E extends Entity> void render(E entity, double x, double y, double z,
    float rotationYaw, float partialTicks, PoseStack poseStack,
    MultiBufferSource buffer, int packedLight)
```

**完整流程**:

```
1. getRenderer(entity)          → 查找 EntityRenderer
2. getRenderOffset()            → 获取渲染偏移 Vec3(默认 ZERO)
3. poseStack.translate()        → 平移到世界坐标 + 偏移
4. entityrenderer.render()      → 委托给具体渲染器
5. renderFlame()                → 若 displayFireAnimation()=true
6. poseStack.translate(-offset) → 恢复平移
7. renderShadow()               → 若启用阴影且 shadowRadius>0 且非隐身
8. renderHitbox()               → 若 renderHitBoxes=true(调试)
9. poseStack.popPose()
```

**重要不变量**:
- 整个 `render()` 包裹在 `try-catch(Throwable)` 中,捕获所有异常并生成 `CrashReport`。
- `renderFlame()` 在渲染器之后、阴影之前执行,都在同一个 `pushPose/popPose` 内。
- 阴影的透明度随距离衰减:`weight = (1.0 - distanceToSqr/256.0) * shadowStrength`,最小需要 `weight > 0.0F`。

### 2.3 shouldRender() — 可见性判断 (第128–131行)

```java
public <E extends Entity> boolean shouldRender(E entity, Frustum frustum,
    double camX, double camY, double camZ) {
    EntityRenderer<? super E> entityrenderer = this.getRenderer(entity);
    return entityrenderer.shouldRender(entity, frustum, camX, camY, camZ);
}
```

委托给具体渲染器,渲染器会检查实体自身 `shouldRender()` 标志 + 视锥体裁剪。

### 2.4 getRenderer() — 渲染器查找 (第95–103行)

```java
public <T extends Entity> EntityRenderer<? super T> getRenderer(T entity) {
    if (entity instanceof AbstractClientPlayer) {
        String s = ((AbstractClientPlayer)entity).getModelName();
        EntityRenderer<? extends Player> entityrenderer = this.playerRenderers.get(s);
        return (EntityRenderer)(entityrenderer != null ? entityrenderer : this.playerRenderers.get("default"));
    } else {
        return (EntityRenderer)this.renderers.get(entity.getType());
    }
}
```

- Player:按皮肤模型名称(`getModelName()`)查找,回退到 `"default"`。
- 非Player:按 `EntityType` 查找。
- `renderers` 和 `playerRenderers` 在 `onResourceManagerReload()` 中通过 `EntityRenderers.createEntityRenderers()` / `createPlayerRenderers()` 重新构建。

### 2.5 getPackedLightCoords() — 光照坐标 (第81–83行)

委托给具体渲染器的 `getPackedLightCoords()`,封装了 `LightTexture.pack(blockLight, skyLight)`。

### 2.6 prepare() — 每帧准备 (第105–110行)

```java
public void prepare(Level level, Camera activeRenderInfo, Entity entity) {
    this.level = level;
    this.camera = activeRenderInfo;
    this.cameraOrientation = activeRenderInfo.rotation();
    this.crosshairPickEntity = entity;
}
```

在 `LevelRenderer.renderEntities()` 开始时调用,设置当前关卡、相机、准星指向实体。

---

## 3. EntityRenderer — 基类

**文件**: `net/minecraft/client/renderer/entity/EntityRenderer.java` (109行)

### 3.1 数据结构 (第21–26行)

```java
public abstract class EntityRenderer<T extends Entity> {
    protected static final float NAMETAG_SCALE = 0.025F;
    protected final EntityRenderDispatcher entityRenderDispatcher;
    private final Font font;
    protected float shadowRadius;
    protected float shadowStrength = 1.0F;
}
```

- `shadowRadius`: 阴影半径,各渲染器在构造函数中设置(如 PlayerRenderer 设置 0.5F)。
- `shadowStrength`: 阴影强度乘数,默认 1.0。

### 3.2 getPackedLightCoords() — 光照打包 (第33–36行)

```java
public final int getPackedLightCoords(T entity, float partialTicks) {
    BlockPos blockpos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
    return LightTexture.pack(this.getBlockLightLevel(entity, blockpos),
                             this.getSkyLightLevel(entity, blockpos));
}
```

- 使用 `entity.getLightProbePosition()` 获取光照采样点(通常为眼睛位置)。
- `getBlockLightLevel()`:通常取方块光照,但若实体着火则返回 15。
- `getSkyLightLevel()`:取天空光照。
- `LightTexture.pack()`:将两个 `[0,15]` 值打包为 `int` (block 在低 4 bit,sky 在高 4 bit)。

### 3.3 shouldRender() — 可见性裁剪 (第46–58行)

```java
public boolean shouldRender(T livingEntity, Frustum camera,
    double camX, double camY, double camZ) {
    if (!livingEntity.shouldRender(camX, camY, camZ)) return false;
    if (livingEntity.noCulling) return true;
    AABB aabb = livingEntity.getBoundingBoxForCulling().inflate(0.5D);
    if (aabb.hasNaN() || aabb.getSize() == 0.0D) {
        aabb = new AABB(...); // 回退 2x2x2 包围盒
    }
    return camera.isVisible(aabb);
}
```

三层短路:
1. 实体自身 `shouldRender()` 标志(如旁观者模式玩家不可见)
2. `noCulling` 标志(强制渲染)
3. 膨胀 0.5 的包围盒做视锥体检测;若包围盒无效,使用 2x2x2 回退包围盒

### 3.4 render() — 默认名牌渲染 (第65–71行)

EntityRenderer 基类的 `render()` 只负责名牌:

```java
public void render(T entity, float entityYaw, float partialTick,
    PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    var renderNameTagEvent = new RenderNameTagEvent(...);
    MinecraftForge.EVENT_BUS.post(renderNameTagEvent);
    if (允许渲染) this.renderNameTag(entity, ...);
}
```

- Forge 事件 `RenderNameTagEvent` 可拦截/修改名牌渲染。`LivingEntityRenderer` 重写此方法调用 `super.render()` 在模型渲染之后,即在 poseStack.popPose() 之后。

### 3.5 renderNameTag() — 名牌详细渲染 (第86–108行)

```java
protected void renderNameTag(T entity, Component displayName,
    PoseStack poseStack, MultiBufferSource buffer, int packedLight)
```

流程:
1. 检查距离:`distanceToSqr()` 超阈值不渲染
2. `getNameTagOffsetY()` 获取 Y 偏移
3. `poseStack.mulPose(cameraOrientation())` 使名牌始终朝向相机(billboard)
4. `poseStack.scale(-0.025F, -0.025F, 0.025F)` 缩放 + 翻转(因为之前有 -1,-1 翻转变换)
5. `font.drawInBatch()` 绘制文本,支持 `SEE_THROUGH` 模式(潜行时透过墙壁可见)
6. `deadmau5` 特殊处理:Y 偏移 -10

---

## 4. LivingEntityRenderer — 模型+Layer 渲染

**文件**: `net/minecraft/client/renderer/entity/LivingEntityRenderer.java` (280行)

### 4.1 类声明与泛型

```java
public abstract class LivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>>
    extends EntityRenderer<T> implements RenderLayerParent<T, M>
```

- 两个泛型参数:`T` 为 LivingEntity 子类,`M` 为对应的 EntityModel。
- 实现了 `RenderLayerParent`,提供 model 访问接口给 Layer 使用。

### 4.2 数据结构 (第32–35行)

```java
protected M model;
protected final List<RenderLayer<T, M>> layers = Lists.newArrayList();
```

- `model`: 实体模型实例。
- `layers`: RenderLayer 列表,按添加顺序渲染。通过 `addLayer()` 追加。

### 4.3 render() — 完整渲染流程 (第51–138行)

```
1. Forge Pre 事件(RenderLivingEvent.Pre),可取消渲染
2. poseStack.pushPose()
3. 计算 attackTime → model.attackTime
4. 判断 riding(坐骑) → model.riding
5. 判断 young(幼体) → model.young
6. 计算身体Yaw、头部Yaw、头部相对Yaw
   - 身体Yaw = rotLerp(旧值, 新值)
   - 若坐骑为 LivingEntity,使用坐骑身体Yaw,夹头差至 ±85°,25°以上加速
   - 头Yaw = 头Yaw - 身体Yaw
7. 获取 XRot,若倒立则 XRot 和头Yaw 取反
8. 若 SLEEPING pose:按床方向平移(eyeHeight - 0.1)
9. 获取 bob(ageInTicks)
10. setupRotations() → 姿态变换(Yaw/死亡/SpinAttack/睡眠/倒立)
11. poseStack.scale(-1.0F, -1.0F, 1.0F) → 翻转坐标系
12. scale() → 子类缩放钩子(空实现)
13. poseStack.translate(0.0F, -1.501F, 0.0F) → 模型原点对齐
14. 计算 walkAnimation speed/position(非乘坐且存活)
15. model.prepareMobModel() → 动画前置处理
16. model.setupAnim() → 动画设置
17. 判断透明度模式:
    - isBodyVisible: 实体可见
    - translucent: 不可见但非对玩家不可见 → 半透明 0.15
    - glowing: 发光轮廓
18. getRenderType() → 选择 RenderType
19. model.renderToBuffer() → 主模型渲染
20. For each RenderLayer: layer.render() → 逐层渲染
21. poseStack.popPose()
22. super.render() → 继承自 EntityRenderer 的名牌渲染
23. Forge Post 事件(RenderLivingEvent.Post)
```

### 4.4 getRenderType() — 渲染类型选择 (第141–150行)

```java
protected RenderType getRenderType(T livingEntity, boolean bodyVisible,
    boolean translucent, boolean glowing) {
    ResourceLocation texture = this.getTextureLocation(livingEntity);
    if (translucent)       → RenderType.itemEntityTranslucentCull(texture)
    else if (bodyVisible)  → this.model.renderType(texture)  (通常 entityCutoutNoCull)
    else                   → glowing ? RenderType.outline(texture) : null
}
```

三种模式:
- **正常可见**:使用 model 的默认 RenderType(通常实体为 `entityCutoutNoCull`)。
- **半透明**:不可见实体(如隐身药水效果)但玩家能看到,alpha=0.15。
- **发光轮廓**:实体不可见但有发光效果,使用 `RenderType.outline()`。

### 4.5 setupRotations() — 姿态变换 (第179–210行)

处理五种姿态:

| 条件 | 变换 |
|---|---|
| isShaking(冰冻) | Yaw += cos(tickCount*3.25)*PI*0.4 (抖动) |
| 默认 | mulPose(Y.rotationDegrees(180 - rotationYaw)) |
| deathTime > 0 | mulPose(Z.rotationDegrees(死亡翻转角度)),翻转持续 20 ticks,用 sqrt Easing |
| isAutoSpinAttack | mulPose(X.-90°-XRot),mulPose(Y.tickCount*-75°) |
| SLEEPING | mulPose(Y.床朝向),mulPose(Z.flipDegrees),mulPose(Y.270°) |
| isEntityUpsideDown | translate(0, height+0.1, 0), mulPose(Z.180°) |

### 4.6 getOverlayCoords() — 受伤闪白 (第152–154行)

```java
public static int getOverlayCoords(LivingEntity livingEntity, float u) {
    return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(livingEntity.hurtTime > 0 || livingEntity.deathTime > 0));
}
```

- `u` 分量:由 `getWhiteOverlayProgress()` 控制,默认 0.0(用于生物受伤闪白逐渐消失)。
- `v` 分量:若 `hurtTime > 0` 或 `deathTime > 0`,使用红色 overlay 纹理行(闪红)。

### 4.7 受伤闪白机制

`NoteblockMobRenderer` 等子类重写 `getWhiteOverlayProgress()` 返回非零值(如苦力怕爆炸前),实现白色闪光。OverlayTexture 有 16×16 的 overlay 纹理,行 0(u=v=0)为正常,行 1(v=1)为红色受伤覆盖,`u` 分量控制白色淡化过渡。

### 4.8 isEntityUpsideDown() — 倒立检查 (第270–279行)

Dinnerbone/Grumm 彩蛋:若实体名为这两个字符串(Player 需开启披风模型),则倒立渲染。

---

## 5. RenderLayer 系统

**文件**: `net/minecraft/client/renderer/entity/layers/RenderLayer.java` (48行)

### 5.1 类声明

```java
public abstract class RenderLayer<T extends Entity, M extends EntityModel<T>>
```

泛型参数与 LivingEntityRenderer 一致。

### 5.2 核心方法

```java
public abstract void render(PoseStack poseStack, MultiBufferSource buffer,
    int packedLight, T livingEntity, float limbSwing, float limbSwingAmount,
    float partialTick, float ageInTicks, float netHeadYaw, float headPitch);
```

- Layer 接收实体引用和动画参数,在 model 已 setupAnim 后渲染额外内容。
- `getParentModel()` 获取渲染器的 model。
- 工具方法 `coloredCutoutModelCopyLayerRender()`:复制 model 属性、重新调用 setupAnim、渲染彩色 cutout(用于外层模型如盔甲)。
- 工具方法 `renderColoredCutoutModel()`:直接以给定颜色渲染 cutout 模型。

### 5.3 Layer 典型子类

| Layer | 职责 |
|---|---|
| HumanoidArmorLayer | 人形实体盔甲渲染,分 inner/outer 两层 |
| ItemInHandLayer | 手持物品渲染 |
| ArrowLayer | 身上箭矢渲染 |
| CustomHeadLayer | 自定义头颅(玩家头)渲染 |
| CapeLayer | 玩家披风渲染 |
| ParrotOnShoulderLayer | 肩膀上鹦鹉渲染 |
| SaddleLayer | 马/猪鞍渲染 |
| BeeStingerLayer | 蜜蜂蛰刺渲染 |

### 5.4 Layer 在 LivingEntityRenderer 中的调度

Layer 的渲染在 `model.renderToBuffer()` 之后、`poseStack.popPose()` 之前执行,共享同一个 poseStack 和 packedLight。若实体是 spectator 则跳过所有 Layer。

---

## 6. 阴影系统

### 6.1 阴影调度 (EntityRenderDispatcher.render(),第149–155行)

```java
if (this.options.entityShadows().get() && this.shouldRenderShadow
    && entityrenderer.shadowRadius > 0.0F && !entity.isInvisible()) {
    double d1 = this.distanceToSqr(entity.getX(), entity.getY(), entity.getZ());
    float f = (float)((1.0D - d1 / 256.0D) * (double)entityrenderer.shadowStrength);
    if (f > 0.0F) {
        renderShadow(poseStack, buffer, entity, f, partialTicks,
                     this.level, Math.min(entityrenderer.shadowRadius, 32.0F));
    }
}
```

条件:
1. 选项开启(`entityShadows=true`)
2. 调度器允许(`shouldRenderShadow=true`,默认 true)
3. 渲染器阴影半径 > 0
4. 实体非隐身
5. 距离衰减后权重 > 0(距离 > 16 即 256 平方时为 0)

### 6.2 renderShadow() — 阴影生成 (第251–286行)

```java
private static void renderShadow(PoseStack poseStack, MultiBufferSource buffer,
    Entity entity, float weight, float partialTicks, LevelReader level, float size)
```

流程:
1. 若为 Mob 的幼体,半径减半(`size *= 0.5F`)
2. 插值实体坐标(lerp old/new)
3. 计算有效深度范围:`y - min(weight/0.5, size)` 到 `y`
4. 在半径范围内的每个 (x,z) 列、深度范围内每个 y 层,检查区块
5. 对每个位置调用 `renderBlockShadow()`

### 6.3 renderBlockShadow() — 单方块阴影 (第288–326行)

```java
private static void renderBlockShadow(PoseStack.Pose pose, VertexConsumer vc,
    ChunkAccess chunk, LevelReader level, BlockPos pos,
    double x, double y, double z, float size, float weight)
```

每个位置的阴影生成:
1. 取下方方块(`pos.below()`)
2. 检查:渲染形状非 INVISIBLE + 亮度 > 3 + 碰撞形状满方块
3. 若 VoxelShape 非空,计算 alpha:`weight * 0.5 * LightTexture.getBrightness(dimensionType, maxLocalRawBrightness)`
4. alpha 夹到 `[0, 1]`
5. 取 VoxelShape 的 bounds,计算 UV 和位置,生成 4 个顶点(quad)
6. 使用 `SHADOW_RENDER_TYPE`(`entityShadow`)纹理

**阴影 UV 计算**:
- `u/v = -offset / 2.0 / size + 0.5` —— 投影到 `[0,1]` 纹理空间

**阴影顶点格式** (`shadowVertex`,第328–331行):
```java
buffer.vertex(x, y, z, 1.0F, 1.0F, 1.0F, alpha, texU, texV,
              OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 1.0F, 0.0F);
```
- 颜色:alpha 由阴影深度和亮度决定
- 光照:FULL_BRIGHT (15728880),不受环境光影响
- 法线:(0,1,0) 朝上

---

## 7. 名牌渲染

### 7.1 可见性判断

`LivingEntityRenderer.shouldShowName()` (第237–268行):
- 距离检查:潜行实体 32 方块,非潜行 64 方块
- 队伍可见性:支持 `ALWAYS`/`NEVER`/`HIDE_FOR_OTHER_TEAMS`/`HIDE_FOR_OWN_TEAMS`
- 玩家自身不显示名牌
- `Minecraft.renderNames()` 开关 + 不能是相机实体 + 不能是载具

### 7.2 EntityRenderer.renderNameTag()

详细流程见 [3.5 节](#35-rendernametag--名牌详细渲染-第86-108行)。关键特性:
- Billboard 旋转(`cameraOrientation()`)
- 缩放 0.025(NAMETAG_SCALE)
- SEE_THROUGH 模式(潜行时可透视墙壁)
- `deadmau5` 特殊偏移

---

## 8. 受伤闪白与 Overlay

### 8.1 Overlay 纹理系统

`OverlayTexture` 提供 `pack(u, v)` 方法将两个 4-bit 值打包为 int:
- `u`:白色淡化(0=正常,15=全白),用于受伤白闪、苦力怕爆炸预警等。
- `v`:红色覆盖(0=正常,1=红色),`hurtTime > 0 || deathTime > 0` 时激活。

### 8.2 红色受伤闪烁

`LivingEntityRenderer.getOverlayCoords()`:
- `u` 来自 `getWhiteOverlayProgress()`(默认 0.0,子类可重写)
- `v = hurtTime > 0 || deathTime > 0 ? 1 : 0`

### 8.3 白色闪烁

`getWhiteOverlayProgress()` 被子类(如 CreeperRenderer 爆炸前)重写返回渐变值,使用 `OverlayTexture.u(progress)` 打包。

---

## 9. 火焰渲染

### 9.1 触发条件

`EntityRenderDispatcher.render()` 第144行:
```java
if (entity.displayFireAnimation()) this.renderFlame(poseStack, buffer, entity);
```

### 9.2 renderFlame() 实现 (第206–245行)

- 使用两张火焰纹理 `FIRE_0`/`FIRE_1` 交替渲染
- Billboard 旋转:`mulPose(Y.rotationDegrees(-camera.getYRot()))` 使火焰始终面朝相机
- 按实体高度/宽度缩放
- 每 0.45 单位高度一个火焰 quad,交替使用两张纹理
- 渲染类型:`Sheets.cutoutBlockSheet()`(实体 cutout)
- 光照:packedLight=240(FULL_BLOCK,不受天空光影响)

---

## 10. 具体渲染器:PlayerRenderer 与 MobRenderer

### 10.1 PlayerRenderer

`net/minecraft/client/renderer/entity/player/PlayerRenderer.java`
- 继承 `LivingEntityRenderer<AbstractClientPlayer, PlayerModel>`
- 构造函数添加 Layer 顺序:HumanoidArmorLayer、PlayerItemInHandLayer、ArrowLayer、Deadmau5EarsLayer、CapeLayer、CustomHeadLayer、WingsLayer、ParrotOnShoulderLayer、SpinAttackEffectLayer、BeeStingerLayer
- `getRenderType()`:部分可见模型(皮肤部分)使用 `entityTranslucent()`,通过 `renderHand()` 等方法渲染第一人称手部
- `setModelProperties()`:根据潜行/游泳/飞行/使用物品设置模型 pose
- `getArmPose()`:根据手持物品类型返回 ArmPose(BLOCK/BOW/SPEAR/CROSSBOW 等)

### 10.2 MobRenderer

`net/minecraft/client/renderer/entity/MobRenderer.java`
- 继承 `LivingEntityRenderer<T extends Mob, M extends EntityModel<T>>`
- 添加缩放:`poseStack.scale(scaleX, scaleY, scaleZ)` 处理幼体和发光
- `shouldShowName()`:距离阈值更严格(潜行 32,非潜行 64),非玩家时检查队伍
- 重写 `getShadowRadius()`:base radius × entity scale

---

## 11. 渲染器注册与重载

### 11.1 EntityRenderers (静态注册表)

`EntityRenderers` 维护静态 `Map<EntityType, EntityRendererProvider>`:
```java
EntityRenderers.register(EntityType.ZOMBIE, ZombieRenderer::new);
EntityRenderers.register(EntityType.CREEPER, CreeperRenderer::new);
// ... 所有实体类型
```

### 11.2 资源重载

`EntityRenderDispatcher.onResourceManagerReload()` (第364–369行):
```java
this.renderers = EntityRenderers.createEntityRenderers(context);
this.playerRenderers = EntityRenderers.createPlayerRenderers(context);
Forge EVENT_BUS.post(new AddLayers(renderers, playerRenderers, context));
```

每次资源包重载时重建全部渲染器,确保纹理/模型 Atlas 更新。Forge `AddLayers` 事件允许模组添加自定义 Layer。

### 11.3 EntityRendererProvider.Context

```java
public static class Context {
    EntityRenderDispatcher entityRenderDispatcher;
    ItemRenderer itemRenderer;
    BlockRenderDispatcher blockRenderDispatcher;
    ItemInHandRenderer itemInHandRenderer;
    ResourceManager resourceManager;
    EntityModelSet entityModels;
    Font font;
}
```

通过 Context 注入渲染器所需的所有依赖。

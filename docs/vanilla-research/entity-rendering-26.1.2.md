# 26.1.2 (NeoForge) Minecraft Vanilla Entity Rendering Pipeline 分析报告

> 基于 `.local_ref/mc/26.1.2/sources/` 提取源码。
> 所有路径相对于该目录。如未注明,行号基于各文件完整内容。

## 目录

1. [重大架构重构:EntityRenderState 分离](#1-重大架构重构entityrenderstate-分离)
2. [EntityRenderState 状态类层次](#2-entityrenderstate-状态类层次)
3. [EntityRenderDispatcher — Submit 模型](#3-entityrenderdispatcher--submit-模型)
4. [EntityRenderer — createRenderState/extractRenderState/submit](#4-entityrenderer--createrenderstateextractrenderstatesubmit)
5. [LivingEntityRenderer — Submit 重写](#5-livingentityrenderer--submit-重写)
6. [SubmitNodeCollector — 延迟提交系统](#6-submitnodecollector--延迟提交系统)
7. [RenderLayer — Submit 适配](#7-renderlayer--submit-适配)
8. [阴影系统 — 预计算 ShadowPiece](#8-阴影系统--预计算-shadowpiece)
9. [名牌渲染 — Extract 阶段预判断](#9-名牌渲染--extract-阶段预判断)
10. [拴绳渲染 — LeashState 分离](#10-拴绳渲染--leashstate-分离)
11. [AvatarRenderer — 玩家渲染](#11-avatarrenderer--玩家渲染)
12. [渲染器注册与重载](#12-渲染器注册与重载)

---

## 1. 重大架构重构:EntityRenderState 分离

26.1.2 对实体渲染管线进行了**根本性重构**——将渲染所需数据从 `Entity` 中**提前提取**到独立的 `EntityRenderState` 对象。

### 1.1 重构动机

老架构(1.20.1/1.21.1)的核心问题:
- 渲染过程中直接访问 `Entity` 字段(`getX()`, `getYRot()`, `tickCount` 等)
- 实体可能在渲染途中被修改(多线程风险)
- 渲染逻辑和实体状态耦合,无法独立测试或缓存
- 无法实现延迟提交/排序优化

新架构(26.1.2):
```
老: Entity → getRenderer(entity) → entityrenderer.render(entity, ...)
新: Entity → createRenderState(entity) → extractRenderState(entity, state) → submit(state, ...)
```

### 1.2 两阶段分离

```
Phase 1: EXTRACT (在游戏线程,同步)
  EntityRenderDispatcher.extractEntity(entity, partialTicks)
    → renderer.createRenderState(entity, partialTicks)
      → renderer.createRenderState()          // 构造空状态对象
      → renderer.extractRenderState(entity, state, partialTicks)  // 填充数据
      → renderer.finalizeRenderState(entity, state)  // 后处理(阴影等)

Phase 2: SUBMIT (可在渲染线程,异步安全)
  EntityRenderDispatcher.submit(state, camera, x, y, z, poseStack, collector)
    → renderer.submit(state, poseStack, collector, camera)
```

---

## 2. EntityRenderState 状态类层次

**包**: `net/minecraft/client/renderer/entity/state/`

### 2.1 完整继承链

```
net.neoforged.neoforge.client.renderstate.BaseRenderState
  └─ EntityRenderState
       ├─ LivingEntityRenderState
       │    ├─ ArmedEntityRenderState
       │    │    └─ HumanoidRenderState
       │    │         └─ AvatarRenderState          (玩家/人形 Avatar)
       │    ├─ IllagerRenderState
       │    │    ├─ EvokerRenderState
       │    │    ├─ IllusionerRenderState
       │    │    └─ ...
       │    ├─ UndeadRenderState → ZombieRenderState, SkeletonRenderState, ...
       │    ├─ EquineRenderState → HorseRenderState, DonkeyRenderState, ...
       │    ├─ FelineRenderState → CatRenderState, ...
       │    ├─ ...
       │    └─ (约 70+ LivingEntityRenderState 子类)
       ├─ ItemEntityRenderState
       ├─ ArrowRenderState
       ├─ BoatRenderState
       ├─ MinecartRenderState
       ├─ FallingBlockRenderState
       ├─ ItemFrameRenderState
       ├─ PaintingRenderState
       └─ ... (约 100+ RenderState 类型)
```

### 2.2 EntityRenderState — 基类

**文件**: `net/minecraft/client/renderer/entity/state/EntityRenderState.java` (65行)

```java
public class EntityRenderState extends BaseRenderState {
    public static final int NO_OUTLINE = 0;
    public EntityType<?> entityType;         // 实体类型
    public double x, y, z;                  // 插值后的世界坐标
    public float ageInTicks;                // tickCount + partialTicks
    public float boundingBoxWidth;          // getBbWidth()
    public float boundingBoxHeight;          // getBbHeight()
    public float eyeHeight;                 // getEyeHeight()
    public double distanceToCameraSq;       // 到相机距离平方
    public boolean isInvisible;             // 是否隐身
    public boolean isDiscrete;              // 是否潜行
    public boolean displayFireAnimation;    // 是否着火
    public int lightCoords = 15728880;      // 打包光照 FULL_BRIGHT
    public int outlineColor = 0;            // 发光轮廓颜色,0=无
    public @Nullable Vec3 passengerOffset;   // 矿车乘客偏移
    public @Nullable Component nameTag;      // 名牌文本
    public @Nullable Component scoreText;    // 记分板文本
    public @Nullable Vec3 nameTagAttachment; // 名牌附着点
    public @Nullable List<LeashState> leashStates;  // 拴绳状态列表
    public float shadowRadius;              // 阴影半径
    public final List<ShadowPiece> shadowPieces = new ArrayList<>();  // 预计算阴影片
    public float partialTick;               // 帧插值因子
}
```

**关键设计**:
- `shadowPieces` 不再在 submit 阶段实时区块查询,而是在 extract 阶段预计算
- `leashStates` 替代旧架构的实时拴绳计算
- `nameTag` 和 `scoreText` 在 extract 阶段即决定是否渲染
- 所有坐标使用插值后值(lerp old/new)

### 2.3 LivingEntityRenderState

**文件**: `net/minecraft/client/renderer/entity/state/LivingEntityRenderState.java` (40行)

```java
public class LivingEntityRenderState extends EntityRenderState {
    public float bodyRot;                      // 身体 Yaw
    public float yRot;                         // 头 Yaw 相对于身体的差值
    public float xRot;                         // 俯仰角
    public float deathTime;                    // 死亡时间(含 partialTick)
    public float walkAnimationPos;             // 行走动画位置
    public float walkAnimationSpeed;           // 行走动画速度
    public float scale = 1.0F;                 // 实体 scale (getScale)
    public float ageScale = 1.0F;             // 年龄 scale (getAgeScale)
    public float ticksSinceKineticHitFeedback; // 受击反馈计时
    public boolean isUpsideDown;              // 是否倒立
    public boolean isFullyFrozen;             // 是否冰冻
    public boolean isBaby;                    // 是否幼体
    public boolean isInWater;                 // 是否在水中
    public boolean isAutoSpinAttack;          // 是否旋转攻击
    public boolean hasRedOverlay;             // 是否有红色受伤覆盖
    public boolean isInvisibleToPlayer;       // 是否对玩家不可见
    public @Nullable Direction bedOrientation; // 床朝向(SLEEPING pose)
    public Pose pose = Pose.STANDING;          // 当前 pose
    public final ItemStackRenderState headItem = new ItemStackRenderState();  // 头部物品
    public float wornHeadAnimationPos;         // 头部动画位置
    public SkullBlock.@Nullable Type wornHeadType;     // 头颅类型
    public @Nullable ResolvableProfile wornHeadProfile; // 头颅 Profile
}
```

### 2.4 ArmedEntityRenderState

**文件**: `net/minecraft/client/renderer/entity/state/ArmedEntityRenderState.java` (57行)

添加手持物品和攻击状态:
```java
public HumanoidArm mainArm = HumanoidArm.RIGHT;
public HumanoidArm attackArm = HumanoidArm.RIGHT;
public HumanoidModel.ArmPose rightArmPose, leftArmPose;
public final ItemStackRenderState rightHandItemState, leftHandItemState;
public ItemStack rightHandItemStack, leftHandItemStack;
public SwingAnimationType swingAnimationType;
public float attackTime;
```

**静态工厂方法** `extractArmedEntityRenderState(LivingEntity, ArmedEntityRenderState, ItemModelResolver, partialTicks)`:
- 从 entity 提取主手/攻击手/攻击动画
- 使用 `ItemModelResolver.updateForLiving()` 预解析物品模型
- 复制物品栈(用于渲染比较)

### 2.5 HumanoidRenderState

**文件**: `net/minecraft/client/renderer/entity/state/HumanoidRenderState.java` (33行)

```java
public class HumanoidRenderState extends ArmedEntityRenderState {
    public float swimAmount;
    public float speedValue = 1.0F;
    public float maxCrossbowChargeDuration;
    public float ticksUsingItem;
    public InteractionHand useItemHand = InteractionHand.MAIN_HAND;
    public boolean isCrouching;
    public boolean isFallFlying;
    public boolean isVisuallySwimming;
    public boolean isPassenger;
    public boolean isUsingItem;
    public float elytraRotX, elytraRotY, elytraRotZ;
    public ItemStack headEquipment, chestEquipment, legsEquipment, feetEquipment;
}
```

### 2.6 AvatarRenderState — 玩家渲染状态

**文件**: `net/minecraft/client/renderer/entity/state/AvatarRenderState.java` (40行)

```java
public class AvatarRenderState extends HumanoidRenderState {
    public PlayerSkin skin;                    // 皮肤 texture + model
    public float capeFlap, capeLean, capeLean2; // 披风动画
    public int arrowCount, stingerCount;       // 身上箭/刺数量
    public boolean isSpectator;                // 观察者模式
    public boolean showHat, showJacket, showLeftPants,
                   showRightPants, showLeftSleeve, showRightSleeve, showCape;
    public float fallFlyingTimeInTicks;        // 鞘翅飞行时间
    public boolean shouldApplyFlyingYRot;      // 是否应用飞行偏航
    public float flyingYRot;                   // 飞行偏航角
    public Parrot.Variant parrotOnLeftShoulder, parrotOnRightShoulder;
    public int id;                             // 实体 ID
    public boolean showExtraEars;              // deadmau5 耳朵
    public final ItemStackRenderState heldOnHead;  // 头顶物品(如望远镜)
}
```

---

## 3. EntityRenderDispatcher — Submit 模型

**文件**: `net/minecraft/client/renderer/entity/EntityRenderDispatcher.java` (233行)

### 3.1 数据结构变化

```java
public Map<EntityType<?>, EntityRenderer<?, ?>> renderers = ImmutableMap.of();
private Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers = Map.of();
private Map<PlayerModelType, AvatarRenderer<ClientMannequin>> mannequinRenderers = Map.of();
```

- 泛型变为 `EntityRenderer<?, ?>`(双泛型:Entity + EntityRenderState)
- 不再有 `Level level` 字段(不需要了,因为 extract 时从 entity 获取)
- 新增 `mannequinRenderers`(客户端假人渲染)
- `camera` 改为 `@Nullable`

### 3.2 extractEntity() — 提取渲染状态 (第132–145行)

```java
public <E extends Entity> EntityRenderState extractEntity(E entity, float partialTicks) {
    EntityRenderer<? super E, ?> renderer = this.getRenderer(entity);
    try {
        return renderer.createRenderState(entity, partialTicks);
    } catch (Throwable var8) {
        // CrashReport ...
    }
}
```

**这是新的入口方法**,替代了老架构的直接 `render()`。调用链:
```
extractEntity(entity, pt)
  → renderer.createRenderState(entity, pt)
    → renderer.createRenderState()            // 构造空状态
    → renderer.extractRenderState(entity, state, pt)  // 逐字段拷贝
    → renderer.finalizeRenderState(entity, state)      // 阴影预计算等
    → NeoForge onUpdateEntityRenderState event
```

### 3.3 submit() — 提交渲染 (第147–184行)

```java
public <S extends EntityRenderState> void submit(S renderState, CameraRenderState camera,
    double x, double y, double z, PoseStack poseStack, SubmitNodeCollector submitNodeCollector)
```

**完整流程**:

```
1. getRenderer(renderState)         → 通过 EntityType 或 PlayerModelType 查找
2. getRenderOffset(renderState)     → 从 state.passengerOffset 获取偏移
3. poseStack.pushPose/translate     → 平移到世界坐标
4. renderer.submit(state, poseStack, collector, camera)
5. submitFlame                      → 若 displayFireAnimation=true
6. Avatar 特殊处理                  → 先 restore 平移(用于阴影)
7. submitShadow                     → 若 shadowPieces 非空
8. 非 Avatar 实体 restore           → 恢复平移
9. poseStack.popPose()
```

**与老架构的关键差异**:
- 不传 `MultiBufferSource`,而是 `SubmitNodeCollector`(延迟提交系统)
- 阴影不再实时区块查询,直接提交预计算的 `shadowPieces`
- 火焰通过 `SubmitNodeCollector.submitFlame()` 延迟提交
- `CameraRenderState` 替代 `Camera` 对象

### 3.4 getRenderer() — 双版本查找 (第94–120行)

```java
// 通过 Entity 查找
public <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T entity)

// 通过 EntityRenderState 查找(新增!)
public <S extends EntityRenderState> EntityRenderer<?, ? super S> getRenderer(S entityRenderState)
```

第二个重载根据 `entityRenderState.entityType` 查找渲染器,Player 则通过 `AvatarRenderState.skin.model()` 查找。

### 3.5 prepare() — 简化 (第122–125行)

```java
public void prepare(Camera camera, Entity crosshairPickEntity) {
    this.camera = camera;
    this.crosshairPickEntity = crosshairPickEntity;
}
```

不再需要 `Level` 参数(从 entity 获取)。

---

## 4. EntityRenderer — createRenderState/extractRenderState/submit

**文件**: `net/minecraft/client/renderer/entity/EntityRenderer.java` (361行)

### 4.1 泛型变化

```java
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState>
```

两个泛型参数:
- `T`:实体类型
- `S`:对应的 EntityRenderState 类型

### 4.2 createRenderState() — 工厂方法 (第163–171行)

```java
public abstract S createRenderState();  // 子类实现,构造空状态

public final S createRenderState(T entity, float partialTicks) {
    S state = this.createRenderState();
    this.extractRenderState(entity, state, partialTicks);
    this.finalizeRenderState(entity, state);
    NeoForge.onUpdateEntityRenderState(this, entity, state);
    return state;
}
```

**模板方法模式**:
1. `createRenderState()`:子类定义(如 `return new AvatarRenderState()`)
2. `extractRenderState()`:子类逐层填充(MRO 调用链)
3. `finalizeRenderState()`:后处理(阴影预计算)

### 4.3 extractRenderState() — 基类提取 (第173–285行)

EntityRenderer 基类的 `extractRenderState()` 填充所有 EntityRenderState 字段:

```java
public void extractRenderState(T entity, S state, float partialTicks) {
    state.entityType = entity.getType();
    state.x = Mth.lerp(partialTicks, entity.xOld, entity.getX());
    state.y = Mth.lerp(partialTicks, entity.yOld, entity.getY());
    state.z = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
    state.isInvisible = entity.isInvisible();
    state.partialTick = partialTicks;
    state.ageInTicks = entity.tickCount + partialTicks;
    state.boundingBoxWidth = entity.getBbWidth();
    state.boundingBoxHeight = entity.getBbHeight();
    state.eyeHeight = entity.getEyeHeight();
    // ... 矿车乘客偏移处理 ...
    // ... 名牌判断(距离+shouldShowName) ...
    // ... 记分板文本(距离<100) ...
    // ... 拴绳状态提取(Leashable) ...
    // ... 四连接拴绳(quad leash)处理 ...
    state.displayFireAnimation = entity.displayFireAnimation();
    state.outlineColor = appearsGlowing ? ARGB.opaque(teamColor) : 0;
    state.lightCoords = this.getPackedLightCoords(entity, partialTicks);
}
```

**关键设计**:
- 坐标在 extract 时做 lerp 插值
- 名牌/记分板在 extract 阶段即判断是否渲染(写入 `state.nameTag`/`state.scoreText`)
- 拴绳的 `LeashState` 在 extract 阶段完全计算好
- 四连接拴绳(new minecart system)支持多条并行拴绳

### 4.4 finalizeRenderState() — 阴影预计算 (第287–348行)

```java
protected void finalizeRenderState(T entity, S state) {
    Minecraft minecraft = Minecraft.getInstance();
    Level level = entity.level();
    this.extractShadow(state, minecraft, level);
}
```

调用 `extractShadow()` 预计算 `ShadowPiece` 列表(在 extract 阶段遍历区块)。

### 4.5 extractShadow() — 阴影片提取 (第293–327行)

与老架构 `renderShadow()` 的关键差异:
- **不在 submit 阶段实时查询区块**,而是在 extract 阶段遍历并存储到 `state.shadowPieces`
- `ShadowPiece` 是 record:`ShadowPiece(float relativeX, relativeY, relativeZ, VoxelShape shapeBelow, float alpha)`
- 每个 ShadowPiece 包含:相对于实体的位置、下方方块碰撞形状、透明度
- submit 阶段只需遍历 `shadowPieces` 直接提交顶点

### 4.6 submit() — 基类提交 (第105–113行)

```java
public void submit(S state, PoseStack poseStack,
    SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    // 拴绳提交
    if (state.leashStates != null) {
        for (LeashState leashState : state.leashStates) {
            submitNodeCollector.submitLeash(poseStack, leashState);
        }
    }
    // 名牌提交
    this.submitNameDisplay(state, poseStack, submitNodeCollector, camera);
}
```

### 4.7 submitNameDisplay() — 名牌提交 (第127–149行)

```java
protected void submitNameDisplay(S state, PoseStack poseStack,
    SubmitNodeCollector collector, CameraRenderState camera) {
    poseStack.pushPose();
    if (state.scoreText != null) {
        // NeoForge RenderNameTagEvent.DoRender
        collector.submitNameTag(poseStack, state.nameTagAttachment, offset, state.scoreText,
                                !state.isDiscrete, state.lightCoords, state.distanceToCameraSq, camera);
        poseStack.translate(0, 9.0F * 1.15F * 0.025F, 0);  // 向下偏移
    }
    if (state.nameTag != null) {
        // NeoForge RenderNameTagEvent.DoRender
        collector.submitNameTag(poseStack, state.nameTagAttachment, offset, state.nameTag,
                                !state.isDiscrete, state.lightCoords, state.distanceToCameraSq, camera);
    }
    poseStack.popPose();
}
```

记分板文本渲染后,名牌向下偏移 `9 * 1.15 * 0.025 ≈ 0.259` 单位。

### 4.8 getRenderOffset() — 渲染偏移 (第101–103行)

```java
public Vec3 getRenderOffset(S state) {
    return state.passengerOffset != null ? state.passengerOffset : Vec3.ZERO;
}
```

**签名变化**:从接受 `T entity` 变为接受 `S state`。矿车乘客偏移已预计算在 `state.passengerOffset` 中。

### 4.9 shouldRender() — 裁剪可见性 (第64–91行)

与 1.21.1 相似但新增方法:
- `getBoundingBoxForCulling(T entity)`:可被子类重写(如龙首扩大包围盒)
- `affectedByCulling(T entity)`:控制是否受视锥体裁剪影响

---

## 5. LivingEntityRenderer — Submit 重写

**文件**: `net/minecraft/client/renderer/entity/LivingEntityRenderer.java` (329行)

### 5.1 泛型与类声明

```java
public abstract class LivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState,
    M extends EntityModel<? super S>>
    extends EntityRenderer<T, S>
    implements RenderLayerParent<S, M>
```

三个泛型参数:`T`(Entity),`S`(RenderState),`M`(Model)。Model 的泛型是 `? super S`(逆变)。

### 5.2 submit() — 新渲染方法 (第74–114行)

```java
public void submit(S state, PoseStack poseStack,
    SubmitNodeCollector submitNodeCollector, CameraRenderState camera)
```

**完整流程**:

```
1. Forge Pre 事件(RenderLivingEvent.Pre) → 可取消
2. poseStack.pushPose()
3. SLEEPING pose 处理(床方向平移)
4. poseStack.scale(scale, scale, scale)     → 实体缩放
5. setupRotations(state, poseStack, bodyRot, scale)  → 姿态旋转
6. poseStack.scale(-1, -1, 1)              → 坐标系翻转
7. scale(state, poseStack)                  → 子类缩放钩子
8. poseStack.translate(0, -1.501, 0)       → 模型原点对齐
9. 判断透明度(isBodyVisible/forceTransparent/appearsGlowing)
10. getRenderType() → 选 RenderType
11. submitNodeCollector.submitModel()       → 提交主模型(不再直接渲染!)
12. model.setupAnim(state)                  → 动画设置
13. For each Layer: layer.submit()          → Layer 提交
14. poseStack.popPose()
15. super.submit()                          → 名牌/拴绳提交
16. Forge Post 事件(RenderLivingEvent.Post)
```

### 5.3 关键变化对比

| 1.20.1/1.21.1 | 26.1.2 |
|---|---|
| `model.prepareMobModel(entity, ...)` | 模型准备逻辑移至 `setupAnim(state)` |
| `model.setupAnim(entity, ...)` | `model.setupAnim(state)` |
| `model.renderToBuffer(...)` | `submitNodeCollector.submitModel(model, state, ...)` |
| Layer 接受 Entity + limbSwing 等参数 | Layer 只接受 EntityRenderState + yRot/xRot |
| `buffer.getBuffer(rendertype)` | `RenderTypes.entityTranslucentCullItemTarget` 等新常量 |
| 颜色为 `(r,g,b,a)` | 颜色为 `int`(ARGB packed) |

### 5.4 setupRotations() — 简化 (第164–194行)

```java
protected void setupRotations(S state, PoseStack poseStack, float bodyRot, float entityScale)
```

与老架构差异:
- 所有参数从 `state` 读取(不再直接读 entity)
- 死亡时间使用 float `state.deathTime` 而非 entity 的 int `deathTime`
- 冰冻抖动使用 `Mth.floor(state.ageInTicks)` 而非 `entity.tickCount`
- SpinAttack 使用 `state.ageInTicks * -75` 而非 `(tickCount + partialTicks) * -75`

### 5.5 extractRenderState() — 覆盖 (第254–312行)

```java
public void extractRenderState(T entity, S state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    // ... 计算 bodyRot(headRot 插值 → solveBodyRot) ...
    // ... 计算 yRot/xRot(wrapDegrees) ...
    // ... 倒立翻转 ...
    // ... walkAnimation 提取 ...
    // ... scale/ageScale/pose/bedOrientation ...
    // ... isFullyFrozen/isBaby/isInWater/isAutoSpinAttack ...
    // ... headItem 处理(头颅 vs 普通物品) ...
    // ... deathTime(含 partialTick 的 float) ...
    // ... isInvisibleToPlayer ...
}
```

### 5.6 solveBodyRot() — 身体旋转求解 (第314–328行)

```java
private static float solveBodyRot(LivingEntity entity, float headRot, float partialTicks) {
    if (entity.getVehicle() instanceof LivingEntity riding) {
        // 使用坐骑的身体旋转,夹头差至 ±85°
        float bodyRot = Mth.rotLerp(partialTicks, riding.yBodyRotO, riding.yBodyRot);
        float headDiff = Mth.clamp(Mth.wrapDegrees(headRot - bodyRot), -85.0F, 85.0F);
        bodyRot = headRot - headDiff;
        if (Math.abs(headDiff) > 50.0F) bodyRot += headDiff * 0.2F;
        return bodyRot;
    } else {
        return Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
    }
}
```

将此逻辑从 render() 中提取为纯函数。

---

## 6. SubmitNodeCollector — 延迟提交系统

**文件**: `net/minecraft/client/renderer/SubmitNodeCollector.java` (35行,接口)

### 6.1 架构设计

`SubmitNodeCollector` 是渲染命令的**收集器**,替代了旧架构的 `MultiBufferSource`。主要操作:

```java
// 提交模型
submitNodeCollector.submitModel(model, state, poseStack, renderType, light, overlay, color, ...);

// 提交模型部件
submitNodeCollector.submitModelPart(part, poseStack, renderType, light, overlay, ...);

// 提交名牌
submitNodeCollector.submitNameTag(poseStack, attachment, offset, text, throughWalls, light, distanceSq, camera);

// 提交拴绳
submitNodeCollector.submitLeash(poseStack, leashState);

// 提交火焰
submitNodeCollector.submitFlame(poseStack, state, quaternion);

// 提交阴影
submitNodeCollector.submitShadow(poseStack, shadowRadius, shadowPieces);

// 排序
submitNodeCollector.order(int order) → OrderedSubmitNodeCollector
```

### 6.2 与 MultiBufferSource 的差异

| MultiBufferSource (老) | SubmitNodeCollector (新) |
|---|---|
| 即时获取 VertexConsumer 并绘制 | 延迟收集渲染命令 |
| 按调用顺序渲染 | 可按 RenderType/order 排序 |
| 不透明/半透明混合需手动管理 | 收集器内部管理排序和批次 |
| 每次渲染直接写入 GPU buffer | 统一调度、优化批次 |

---

## 7. RenderLayer — Submit 适配

**文件**: `net/minecraft/client/renderer/entity/layers/RenderLayer.java` (71行)

### 7.1 类声明变化

```java
// 老
public abstract class RenderLayer<T extends Entity, M extends EntityModel<T>>

// 新
public abstract class RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>>
```

泛型参数从 `(Entity, Model)` 变为 `(EntityRenderState, Model)`。

### 7.2 submit() 替代 render()

```java
public abstract void submit(PoseStack poseStack,
    SubmitNodeCollector submitNodeCollector, int lightCoords,
    S state, float yRot, float xRot);
```

**不再接收**:
- `float limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch` — 这些已在 extract 阶段计算到 state 中
- `MultiBufferSource buffer` — 改用 `SubmitNodeCollector`

**简化参数**:
- `yRot` 和 `xRot` 用于 Layer 内的模型 setupAnim(若需要)

### 7.3 工具方法适配

```java
protected static <S extends LivingEntityRenderState> void coloredCutoutModelCopyLayerRender(
    Model<? super S> model, Identifier texture, PoseStack poseStack,
    SubmitNodeCollector submitNodeCollector, int lightCoords, S state, int color, int order)
```

新增 `order` 参数用于提交排序。提交方式:
```java
submitNodeCollector.order(order)
    .submitModel(model, state, poseStack, RenderTypes.entityCutout(texture),
                 lightCoords, overlayCoords, color, null, state.outlineColor, null);
```

`RenderTypes` 是新的 RenderType 常量类(替代了 `RenderType` 类)。

---

## 8. 阴影系统 — 预计算 ShadowPiece

### 8.1 ShadowPiece record

**文件**: `EntityRenderState.java:63`

```java
public record ShadowPiece(float relativeX, float relativeY, float relativeZ,
                           VoxelShape shapeBelow, float alpha) {}
```

每个 ShadowPiece 存储:
- `relativeX/Y/Z`:相对于实体位置的偏移
- `shapeBelow`:下方方块的 VoxelShape(在 submit 时计算 UV)
- `alpha`:透明度(已根据距离/亮度计算)

### 8.2 extractShadow() 流程 (EntityRenderer.java 第293–327行)

与老架构 `renderShadow()` 的差异:
- **Extract 阶段**(同步,游戏线程):遍历区块、计算 alpha、存储 ShadowPiece
- **Submit 阶段**(可异步):直接提交 ShadowPiece,无需再访问区块

提取时跳过区块访问可避免 submit 时的线程安全问题。

### 8.3 阴影提交

`EntityRenderDispatcher.submit()` 第168–170行:
```java
if (!renderState.shadowPieces.isEmpty()) {
    submitNodeCollector.submitShadow(poseStack, renderState.shadowRadius, renderState.shadowPieces);
}
```

---

## 9. 名牌渲染 — Extract 阶段预判断

### 9.1 NameTag 决策 (EntityRenderer.extractRenderState 第196–213行)

```java
if (this.entityRenderDispatcher.camera != null) {
    state.distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr(entity);
    // NeoForge CanRender event
    boolean shouldShowName = event.canRender().isTrue() ||
        (event.canRender().isDefault()
         && state.distanceToCameraSq < 4096.0   // ~64 blocks
         && this.shouldShowName(entity, state.distanceToCameraSq));
    if (shouldShowName) {
        state.nameTag = event.getContent();
        state.nameTagAttachment = entity.getAttachments().getNullable(
            EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTicks));
    }
    // ScoreText: 距离 < 100(=10 blocks) 时显示
    if (state.distanceToCameraSq < 100.0) {
        state.scoreText = entity.belowNameDisplay();
    }
}
```

**关键**:名牌是否渲染在 extract 阶段决定,submit 阶段只需检查 `state.nameTag != null`。

---

## 10. 拴绳渲染 — LeashState 分离

### 10.1 LeashState 内部类

**文件**: `EntityRenderState.java:51-60`

```java
public static class LeashState {
    public Vec3 offset = Vec3.ZERO;       // 被拴实体拴绳连接偏移
    public Vec3 start = Vec3.ZERO;        // 拴绳起点(世界坐标)
    public Vec3 end = Vec3.ZERO;          // 拴绳终点(持有者位置)
    public int startBlockLight = 0;       // 起点方块光
    public int endBlockLight = 0;         // 终点方块光
    public int startSkyLight = 15;        // 起点天空光
    public int endSkyLight = 15;          // 终点天空光
    public boolean slack = true;          // 是否松弛(quad leash 为 false)
}
```

### 10.2 四连接拴绳(Quad Leash)

26.1.2 支持四连接拴绳(新矿车系统):
- `leashCount = 4` 时在 extract 阶段创建 4 个 LeashState
- `supportQuadLeashAsHolder()` + `supportQuadLeash()` 决定是否启用
- 四个拴绳的连接点分别通过 `getQuadLeashOffsets()` 和 `getQuadLeashHolderOffsets()` 获取

### 10.3 拴绳提交

```java
if (state.leashStates != null) {
    for (LeashState leashState : state.leashStates) {
        submitNodeCollector.submitLeash(poseStack, leashState);
    }
}
```

---

## 11. AvatarRenderer — 玩家渲染

**文件**: `net/minecraft/client/renderer/entity/player/AvatarRenderer.java` (325行)

### 11.1 类声明

```java
public class AvatarRenderer<AvatarlikeEntity extends Avatar & ClientAvatarEntity>
    extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel>
```

泛型允许 Player 和 ClientMannequin 共享同一渲染器。

### 11.2 extractRenderState() (第178–207行)

在 `LivingEntityRenderer.extractRenderState()` 基础上,额外提取:
- 手臂 pose(`getArmPose()`)根据物品类型(BLOCK/BOW/CROSSBOW/SPEAR/SPYGLASS 等)
- 皮肤(`entity.getSkin()`)
- 箭/刺计数
- 部件显示标志(hat/jacket/sleeve/pants/cape)
- 飞行数据(capeFlap/capeLean/capeLean2)
- 同伴鹦鹉变体
- 鞘翅飞行 YRot

### 11.3 Layer 列表 (构造函数 第55–72行)

```java
this.addLayer(new HumanoidArmorLayer<>(this, armorSet, equipmentRenderer));
this.addLayer(new PlayerItemInHandLayer<>(this));
this.addLayer(new ArrowLayer<>(this, context));
this.addLayer(new Deadmau5EarsLayer(this, modelSet));
this.addLayer(new CapeLayer(this, modelSet, equipmentAssets));
this.addLayer(new CustomHeadLayer<>(this, modelSet, playerSkinRenderCache));
this.addLayer(new WingsLayer<>(this, modelSet, equipmentRenderer));
this.addLayer(new ParrotOnShoulderLayer(this, modelSet));
this.addLayer(new SpinAttackEffectLayer(this, modelSet));
this.addLayer(new BeeStingerLayer<>(this, context));
```

Guard 条件:`shouldRenderLayers()` 在非 spectator 时返回 true。

### 11.4 第一人称手部渲染

`renderRightHand()`/`renderLeftHand()`:直接提交手臂模型部件,使用 `RenderTypes.entityTranslucent(skin)`。

---

## 12. 渲染器注册与重载

### 12.1 EntityRenderers

**文件**: `net/minecraft/client/renderer/entity/EntityRenderers.java`

```java
public static <T extends Entity> void register(EntityType<? extends T> type,
    EntityRendererProvider<T> renderer) {
    PROVIDERS.put(type, renderer);
}
```

泛型:返回 `EntityRenderer<?, ?>`(双泛型)。

### 12.2 资源重载 (EntityRenderDispatcher.onResourceManagerReload,第212–232行)

```java
this.renderers = EntityRenderers.createEntityRenderers(context);
this.playerRenderers = EntityRenderers.createAvatarRenderers(context);
this.mannequinRenderers = EntityRenderers.createAvatarRenderers(context);
NeoForge.ModLoader.postEvent(new AddLayers(renderers, playerRenderers, mannequinRenderers, context));
```

新增 `mannequinRenderers` 和 `CreateAvatarRenderers`(同时创建 WIDE/SLIM 两套)。

### 12.3 EntityRendererProvider.Context

```
EntityRenderDispatcher dispatcher
BlockModelResolver blockModelResolver       (替代 BlockRenderDispatcher)
ItemModelResolver itemModelResolver         (替代 ItemRenderer)
MapRenderer mapRenderer
ResourceManager resourceManager
EntityModelSet entityModels
EquipmentAssetManager equipmentAssets       (新增)
AtlasManager atlasManager                   (替代 TextureManager)
Font font
PlayerSkinRenderCache playerSkinRenderCache (新增)
```

新增 `BlockModelResolver`/`ItemModelResolver`/`EquipmentAssetManager`/`PlayerSkinRenderCache`,删除了 `ItemRenderer`/`BlockRenderDispatcher`。

# ADR-0047：molang 命名空间委派与单实体渲染 tick / 烘焙预热 API

> **状态**：已实施（2026-11-17）
> **关联**：ADR-0013（基岩动画控制器）、ADR-0026（RenderController 内联）、ADR-0043（装备挂载抽象）
> **触发**：`docs/gap-analysis/downstream-consumer-gaps.md` G7 / G8

---

## 1. 问题

**G7**：`MolangMappingTree.addNode` 取注解类**全部** `public static` 方法，角色解析失败直接抛——于是注解类不得包含任何辅助方法（下游被迫把 `PetOwnerQuery` 与 `PetOwnerDelegates` 拆开，2026-09-26 实机崩溃固化）。同时没有「按名字注入 struct / 命名空间委派」的注册 API：`query.owner.*` 只能手写 scope struct，无生命周期与卸载保证。另有实现缺陷：`addNode` 的 `split("\\\\.")` 与 `findNode` 的 `split("\\.")` 不一致，点分命名空间会建出字面键的不可达节点。

**G8**：时间基准 `(tick + partialTick) / 20` 与 `BrAnimator.tickAnimation` 的五个参数组合散在 7 处下游调用点；`EffectBakePreheat` 复制了「geometry key → 同名 texture → 首个」配对规则。任一侧改动静默漂移（预热失效 = 首帧 40-80ms 卡顿；时间基准变更 = 动画速度错）。

## 2. 决策

### 2.1 molang 注册契约（G7）

- `ParameterRoleResolver` 按注解分流：带 `@MolangFunction` 且解析失败 → **仍抛**（显式契约，保持 `MolangCallableDiscoveryRoleContractTest`）；**无注解**且解析失败 → 跳过该方法 + 一次 WARN。无注解但解析成功仍按方法名注册（`math.*`/`loop`/`query.isItemEquipped` 及下游 `NpcMolang` 依赖此语义）。
- 修 `addNode` 的 split；并让 `findField/findMethod` 通过单一 `probeMember` 支持「多段根路径」探测（先按历史语义「首段根 + 其余整段成员名」，未命中再逐段加深）——保证旧注册（成员名自带点）优先，新增点分命名空间可达。
- 新增命名空间委派：

```java
interface MolangNamespace { @Nullable MolangObject resolve(MolangScope scope, String path); }
MolangNamespaces.register("query.owner", provider) → Registration(AutoCloseable)
```

- 匹配：分段最长前缀（大小写不敏感），等长取后注册者；memo 只缓存「哪个注册命中」并随 `epoch` 失效；空表热路径一次 volatile 读即返回。
- 接入两处解析路径（`resolveMemberAccess` 与编译产物内联的 `MemberSite.resolve`），顺序固定为「scope 覆盖 → 命名空间 → 静态树绑定」——scope 覆盖优先是既有契约（`MolangMemberSiteContractTest`）。
- 命名空间注册表是**进程级**（与注解映射同语义），不随资源重载清理；卸载靠 `Registration.close()`。
- 未覆盖：`query.owner.health()` 调用形态（`resolveCall` 不接命名空间）；节点图可见性。

### 2.2 单实体 tick 与烘焙预热（G8）

```java
record RenderTickResult(ModelRuntimeData tickedInfos, AnimationEffects effects, boolean ticked)
RenderTickResult tickRenderData(RenderData<?> cap, float partialTick)                    // 宿主取 cap.owner（实体时）
RenderTickResult tickRenderData(RenderData<?> cap, RenderHost host, float partialTick)   // 显式宿主

record BakeTarget(Model model, PortResourceLocation texture, PortResourceLocation meshTexture)
enum BakeState { BAKED, PENDING, UNAVAILABLE }
List<BakeTarget> bakeTargets(BrClientEntity ce)     // 静态超集
int warmBakeTargets(List<BakeTarget>)               // 后台预热
int warmRenderData(RenderData<?> cap, @Nullable RenderHost)   // 精确键（含 colorMask/clamped/meshTexture 派生）
BakeState bakeState(RenderData<?> cap, @Nullable RenderHost)
```

- **单一实现**：管线 `TickStage` 与公开 API 都走 `RenderDataTick.tick`。拆分参数只有 `entityOwned`：实体级引导（注册表代际重解析、RC 条件重估触发的组件重建）只给管线，因为它们按**宿主实体类型 id** 查表，会把数据自身的 clientEntity（饰品/特效）冲掉。
- 线程：`collecting` 仅供 `ParallelStageExecutor` worker（回放由管线在渲染线程按实体序执行）；公开 API 与 `immediate` 只能在渲染线程（`MolangScope.singleThreaded()`、`AnimationComponent` 无同步）。
- 生命周期：tick 结束摘掉本帧的 `ANIMATION_PARTICLE_SPAWNER`（旧实现把它留到下一次 tick，游离 keyframe 会写进永不 drain 的队列）。
- 烘焙键单一来源：`RenderOutputs.resolve`（真实提交与预热共用）与 `MeshTextures.resolve`（RC 装配与预热共用）各抽一处。`BakeState` 是门控与诊断的共享词表（`PENDING` 投影为 `RenderSkipReason.NOT_BAKED`）。
- `bakeTargets` 是**超集**：`clamped/`、`_color_mask/`、missing 纹理是运行期派生键，静态枚举推不出来，由渲染门控二次预热兜底；大实体（几何×纹理）可能放大，故活实体应用 `warmRenderData`（精确集）。

## 3. 后果

- 下游删除：手写 `query.owner` struct 注入与按 scope 重复安装/清空、7 处时间基准拼接、5 处 `BrAnimator` 手工调用、`RootAnimationParticleSpawner`/`PORT_ENTITY` 手工注入、geometry→texture 配对复制。
- 有意行为变化：① 无注解 + 角色歧义的方法由「加载崩溃」变为「跳过 + WARN」；② 点分 `@MolangMapping` 名称由「不可达」变为「可达」（成员名自带点的旧写法仍优先）；③ 下游 tick 迁移后获得 `variable.partial_tick`/`attack_time`（动画更平滑、挥击动画生效）；④ 公开 tick 不做 RC 条件重估（现状也没有），特效若依赖条件翻转需自行调用 `setupClientEntity`。
- 未覆盖：不等价于「tick attachable 装备」（`AttachableItemRenderSetup.tickForEntity` 保持独立，其宿主是 `RenderData<ItemStack>`）；不改物品渲染器的时间基准；不合并 `FramePlan.tickResults` 死写路径。

## 4. 验证

- 单测：`MolangNamespacesTest`（最长前缀/后注册优先/大小写/注销/epoch）、`MolangNamespaceContractTest`（静态路径、编译产物站点、scope 覆盖优先、命名空间优先于静态绑定、miss 回落）、`MolangMappingTreeDottedNamespaceTest`、`MolangCallableDiscoveryRoleContractTest`（新增「无注解歧义跳过」）；烘焙侧由 `BakeState` 真值表与 `bakeTargets` 枚举断言覆盖（`PassTierTest` 同批）。
- 三节点 `mcmcp_build` / `mcmcp_test`；`ArchitectureTest` 零新增冻结。
- 运行时：m4mod 特效首帧尖刺（观测 `bakeState` 从 PENDING 到 BAKED 的帧数）、`query.owner.*` 在宠物动画中求值正确。

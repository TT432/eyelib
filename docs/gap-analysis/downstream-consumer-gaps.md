# 下游消费方集成缺口（motions4 视角）

> 状态：评估基线 2026-11-16；**G1–G9 已于 2026-11-17 全部实施**（决策见 ADR-0044～0047，
> 消费方迁移见 `motions4mod`）。下文保留原始证据与建议作为问题记录。
> 范围：`E:/_ideaProjects/motions4-root/motions4mod`（消费基线 21.1.41）对 qylEyelib `:1.21.1` node 的使用面
> 方法：两侧源码对读（下方每条都附证据位置）；**未启动客户端、未截帧**，运行时结论标注为推导
>
> 路径约定：eyelib 源码路径省略前缀 `src/main/java/io/github/tt432/eyelib/`；
> 消费方路径省略前缀 `E:/_ideaProjects/motions4-root/motions4mod/src/main/java/io/github/tt432/motions4mod/`。

## 1. 结论

缺口不在 Bedrock 语义复刻，而在**可嵌入性**：渲染管线按「库是唯一渲染者」设计（单宿主 `Entity`、内部 flush、内部根位姿、内部装备策略），而 motions4 需要的是「渲染原语 + 可控编排」——它已把 eyelib 当渲染运行时使用（模型替换、非实体宿主、外挂锚点、注册表生命周期、反序列化默认值、molang 扩展）。

按严重度共 9 项：P0 两项（宿主抽象、渲染结果不可观测），P1 五项，P2 两项。P0-1 已在真实消费方造成两处静默不渲染（见 §5）。

## 2. 使用面盘点

| 用途 | 依赖的 eyelib 入口 | motions4 位置 |
|---|---|---|
| 实体模型全权接管 | `RenderLivingEvent.Pre` 取消 + `SimpleRenderAction.render()` | `LayerRenderer.java:124,169-190` |
| 非实体/方块锚定宿主 | `renderComponents()` + 伪造 `.entity(player)` + `applyEntityPose(false)` | `client/CropsRenderer.java:168-176`、`client/FarmingCoreRenderer.java:161-169` |
| 外挂锚点（饰品/携带宠物/乘客） | locator 解析 + 嵌套 `renderComponents` + 自维护 `jewelryMap` | `LayerRenderer.java:290-405` |
| 跨实例合批 | 自建 `RenderSink`（RenderType 分桶、自绘、`skinning=null`） | `client/BatchedRenderSink.java:51-139` |
| 注册表生命周期 | `ClientEntityManager.generation()` + `setClientEntity` + `setupClientEntity(String)` | `client/ClientEntityReplacementTracker.java`、`client/CropsRenderer.java:88-100` |
| molang 扩展 | `@MolangMapping`/`@MolangFunction` 扫描 + 手写 scope struct | `molang/PetOwnerQuery.java`、`molang/PetOwnerDelegates.java` |
| 反序列化默认值 | mixin `BrClientEntity.Toggles.CODEC` 的 `<clinit>` | `client/HideArmorDefaults.java`、`mixin/BrClientEntityTogglesMixin.java` |
| 动画采样外包 | `Animation.tickAnimation` + `ModelRuntimeData` 取骨骼 | `camera/CameraManager.java:52-70` |
| 烘焙预热 | `ModelBakePort.twoSideWarmAsync` + 自己复制 geometry→texture 配对规则 | `client/EffectBakePreheat.java:22-43` |

## 3. 缺口清单

### G1 宿主抽象缺失：`RenderData<T>` 泛型，渲染层只认 `Entity`（P0）

**证据**
- `SimpleRenderAction.entity()` 是唯一宿主，且 `entity == null` 时 `renderEntity` **直接 `return false`，无日志、无诊断计数**（`client/render/EntityRenderOrchestrator.java:441-445`）；`renderComponents` 内部再判一次并 `popPose` 跳过（`client/render/EntityRenderOrchestrator.java:523-524`）。
- `renderEntity` 对传入实体执行 `cap.ensureOwner(entity)`，会重新 `init` 覆盖 owner 与 scope 宿主上下文；而 `RenderData<T>` 的 owner 本身是泛型（`capability/RenderData.java`）。
- 走 `renderComponents` 则绕过 `renderEntity` 的每帧准备：`setupExtraMolang`（`variable.partial_tick`、`variable.attack_time`、`EngineEntityVariables`）不执行（`client/render/ClientEntitySetup.java:127`）。

**后果**
- 方块锚定渲染只能塞本地玩家当宿主，`query.*` 语义指向玩家；幼体缩放/身体朝向需 `applyEntityPose(false)` 手工关掉。
- 作物/农耕核心的动画拿不到 partial tick，与实体路径行为不一致。
- 静默返回 `false` 纵容了真实缺陷（§5）。

**建议**：引入 `RenderHost`（Entity / 位置或方块 / 自定义 scope 提供者）；缺失宿主写入 `RenderDiagnostics` 并在 dev 环境抛错；`render()` 与 `renderComponents()` 共享同一「每帧准备」入口。

### G2 渲染结果不可观测：调用方拿不到根位姿（P0）

**证据**
- `SimpleRenderAction.render()` 只返回 `boolean`（`client/render/SimpleRenderAction.java:38`）。
- 根位姿合成（脚本缩放 / 幼体缩放 / 睡眠位移 / 身体朝向）在**包私有** `final class ClientEntitySetup` 内（`client/render/ClientEntitySetup.java:41`、`setupEntityClientEntityData` `client/render/ClientEntitySetup.java:76`），且施加在 `pushPose → submit → popPose` 作用域中，弹出后不可取。

**后果**
- motions4 用 `RenderSink` 装饰器抄「首个 `submit` 的矩阵」（`LayerRenderer.java:252-283`），依赖「首个 submit 一定是模型几何」这条未文档化不变量。
- 早期用 `@Invoker` 跨包访问包私有目标类，运行期 `IllegalAccessError` 直接 FATAL（`E:/_ideaProjects/motions4-root/AGENTS.md` 2026-11-09 记录）。

**建议**：`render()` 返回 `RenderResult { rootPose, submittedPasses, skipReason }`；或把根位姿合成提为公开稳定 API。

### G3 自定义挂载声明缺少公开扩展点（P1）

**证据**
- `SimpleRenderAction.extraRender` 已被删除，**删除本身是合理决策**：ADR-0043 §2.3 记录其唯一用途是手持物且按组件重复调用（多个覆盖层会重复绘制）。
- 替代物 `ProxyAttachable`/`ProxyAttachments`（`client/render/ProxyAttachments.java`）只覆盖 `EquipmentSlot` 四槽 + 主副手，是 application 层内部类，不接收外部声明。

**后果**：饰品、乘客、携带宠物等「自定义锚点内容」必须由下游在库外重算，并复制内部规则——`rightitem/leftitem` 骨骼存在则跳过 locator 手持物（`LayerRenderer.java:317-323`）、locator 命名前缀约定、pass 分档。

**建议**：恢复「模型渲染后、pose 内」的回调插槽（参数含 rootPose + locators），或把代理 attachable 的声明接口对外开放。

### G4 合批能力缺失，`flush` 时机写死在编排里（P1）

**证据**
- 每个 `ModelComponent` 提交后立即 `data.sink().flush()`（`client/render/EntityRenderOrchestrator.java:575`）；pass 分档 `passOrder` 为私有静态（`client/render/EntityRenderOrchestrator.java:474`）。
- `RenderSink` 接口按版本分叉（`multiBufferSource` / `submitNodeCollector`，`bridge/client/render/RenderSink.java:52-54`）。

**后果**：`BatchedRenderSink` 重新实现 RenderType 分桶并**复制** pass 档位不变量；必须显式 `skinning=null` 关闭 GPU 蒙皮才能合批（内部知识）；该实现到 26.1 需重写。

**建议**：`RenderSink` 支持「延迟 flush / 批次作用域」，公开 pass 策略与当前版本蒙皮会话可用性查询。

### G5 注册表绑定语义二分，外部实例不参与代际失效（P1）

**证据**
- `ClientEntityComponent.setClientEntity` 标记 `externallySet`，不参与代际比对（`capability/component/ClientEntityComponent.java`，`isStale` 注释明写「外部持有者负责重设」）。
- 同族 API 两种语义：`setupClientEntity(String, cap)` 在 stale 时按 id 重新查表并**覆盖**外部设置的实例（`client/render/ClientEntitySetup.java:157-176`）。

**后果**：`ClientEntityReplacementTracker` 自建代际监听 + 逐 tick 重放；`CropsRenderer` 按代际整表重建；「首次特效不渲染、第二次同 id 才生效」即 String 重载覆盖所致（`LayerRenderer.java` 内注释记录）。

**建议**：`Registry` 已有变更 publisher，暴露变更事件；API 二分命名（`bindById` 自动重解析 vs `bindInstance` 保持实例），`String` 重载不再覆盖显式实例。

### G6 反序列化缺规范化单点（P1）

**证据**：`Toggles` 的 `hide_armor` 默认值硬编码 `false`（`importer/entity/BrClientEntity.java:60`），而客户端实体有多条解码入口（资源包 loader / addon loader 与聚合 / GUI 导入规划器）。无 SPI。

**后果**：下游只能 mixin 改 `Toggles.CODEC` 的 `<clinit>` 静态字段（`mixin/BrClientEntityTogglesMixin.java`）——库升级即碎；`enable_attachables`、代理 attachable 槽位表同理无声明入口。

**建议**：`ClientEntityNormalizer` 注册表（解码后 hook）或 Toggles 默认值可配置；至少提供公共 decode hook。

### G7 molang 注册契约过宽，且无动态命名空间（P1）

**证据**
- `addNode` 取 `getMethods()` 全部 static 方法，**不按 `@MolangFunction` 过滤**（`molang/mapping/api/MolangMappingTree.java:124-149`），角色解析失败直接抛（`molang/mapping/api/ParameterRoleResolver.java:89-104`）。
- 没有「按名字注入 struct / 命名空间委派」的注册 API。

**后果**：注解类不得包含任何辅助方法，否则 mod 加载崩溃——`PetOwnerQuery` 被迫与 `PetOwnerDelegates` 拆分；`query.owner.*` 只能手写 scope struct 并依赖内部成员解析的覆盖查找，无生命周期与卸载保证。

**建议**：按注解过滤 + 命名空间委派注册 API（含卸载）。

### G8 「半内部」流程外泄：动画 tick 与烘焙配对规则（P2）

**证据**：时间基准 `(ClientTickHandler.getTick() + partialTick) / 20` 与 `BrAnimator.tickAnimation` 的五个参数组合由下游逐处拼（`LayerRenderer.java:357,457`、`client/CropsRenderer.java:158`、`client/NpcRenderer.java:68`、`client/PetPutDownPrediction.java:193`）；`client/EffectBakePreheat.java` 复制了 `setupClientEntity` 的「geometry key → 同名 texture → 首个」兜底规则。

**后果**：任一侧改动静默漂移（预热失效 = 首帧 40-80ms 卡顿；时间基准变更 = 动画速度错）。

**建议**：公开 `tickRenderData(cap, partialTick)`（等价单实体 TickStage）与 `bakeTargets(ce)`。

### G9 阶段订阅顺序无契约（P2）

**证据**：`@OnRenderStage` 钩子存在且经 ASM 跨 mod 扫描（`bridge/client/render/adapter/ForgeRenderStageSubscriberDiscovery.java:34-60`），但注解**无 priority 字段**（`util/event/api/OnRenderStage.java`），订阅者顺序由发现顺序决定。

**后果**：下游无法表达「在 eyelib 的 TickStage 采样 `query.head_yaw` 之前改写 `yBodyRot`」，只能退回 NeoForge 事件 + `EventPriority.HIGHEST`（`LayerRenderer.java:89` `lockRiderRotation`）。

**建议**：注解加 priority，或提供 pre/post 成对钩子。

## 4. 现有公开面（避免误判为缺口）

| 公开 API | 能力 | 下游现状 |
|---|---|---|
| `RenderParams` + `RenderHelper.start().render/renderOnLocator` | 给定位姿自行绘制模型的完整原语（含 bake 缓存与失效） | 未使用，全部走 `SimpleRenderAction` |
| `@OnRenderStage` | 逐帧阶段钩子，早于 Iris shadow pass | 未使用，改用 NeoForge `RenderLevelStageEvent` |
| `EyelibLivingEntityRenderer<T>` | 为实体类型注册渲染器的正规入口 | 未使用，改为取消 `RenderLivingEvent.Pre` 自行绘制 |
| `ModelPoseTransforms.resolveBonePose/resolveLocatorPose/bedrockRootPose` | 骨骼/locator 帧解析 | 已使用 |
| `EntityRenderOrchestrator.setupClientEntity` 三个重载 + `evalConditionMask` | 组件装配与条件求值 | 已使用 |
| 渲染诊断 getter（`getRenderCount/getErrorCount/getLastError/...`） | dev 期计数 | 未使用 |

结论：locator 数学与模型绘制原语都是公开的，真正缺的是**根位姿**与**编排内插槽**。

## 5. 消费者侧证据：两个必然不渲染的调用点

`SimpleRenderAction.builder(...).light(...).animation(...).build().render()` 未调用 `.entity(...)`：

- `client/NpcRenderer.java:81-85`
- `client/PetPutDownPrediction.java:213-218`

`renderEntity` 在 `client/render/EntityRenderOrchestrator.java:443` 直接 `return false`，无日志。motions4 的 `E:/_ideaProjects/motions4-root/work/feedback.json`（2026-10-11）已记录「NPC/宠物路径未排查未修复」。这是消费者侧缺陷，但正是 G1 的静默语义使其长期潜伏。修复点在消费方（补 `.entity()` 或改用方块锚定语义），库侧应按 G1 收敛语义。

## 6. 建议实施顺序

1. **G1 止血**：`renderEntity`/`renderComponents` 的缺失宿主改为显式诊断 + dev 抛错；再设计 `RenderHost`。
2. **G2**：`RenderResult`（rootPose）与 G3 的回调插槽共用同一「渲染产物」概念，一次设计。
3. **G5**：注册表变更事件 + `bindById`/`bindInstance` 二分（下游两处自建代际逻辑可删）。
4. **G6**：解码规范化 SPI（可先做成 Toggles 默认值注册表）。
5. **G4/G7/G8/G9**：按下游痛点热度排。

## 7. 验证边界

- 全部结论来自源码对读（上列 `文件:行`）；**未运行客户端**。§5 的「必然不渲染」是代码推导（`entity == null → return false` 无其他分支），未实机复现；建议用 `mcmcp_launch` + `/eval` 打印 `action.render()` 返回值确认。
- 未分析 1.20.1 / 26.1.2 节点行为差异；motions4 只消费 `:1.21.1`。
- 消费方 `E:/_ideaProjects/motions4-root/work/feedback.json` 与 `E:/_ideaProjects/motions4-root/AGENTS.md`「踩坑」可作为回归清单。

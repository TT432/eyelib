# ADR-0044：可嵌入渲染 API——宿主抽象、渲染产物与外部挂载声明

> **状态**：已实施（2026-11-17）
> **关联**：ADR-0043（装备挂载抽象）、ADR-0019（attachable 缓存生命周期）、ADR-0038（姿态管道早于 shadow pass）
> **触发**：`docs/gap-analysis/downstream-consumer-gaps.md` G1 / G2 / G3（motions4 消费方视角的可嵌入性缺口）

---

## 1. 问题

渲染层把「库是唯一渲染者」写死在三处：

| 现象 | 根因 |
|---|---|
| 方块锚定渲染只能伪造本地玩家当宿主；漏传宿主时**完全静默**（`entity == null → return false`，无日志无计数） | 渲染入口的宿主类型只有 `Entity`，且缺失宿主不是可诊断状态 |
| 调用方拿不到模型根位姿，只能用 `RenderSink` 装饰器抄「首个 `submit` 的矩阵」 | 根位姿在包私有 `ClientEntitySetup` 内合成，施加在 `pushPose → submit → popPose` 作用域中，弹出后不可取 |
| 饰品/乘客/携带宠物只能由下游在库外重算 locator 表、复制「有 `rightitem` 骨骼就跳过 locator 手持物」等内部规则 | `SimpleRenderAction.extraRender` 被删除后没有替代插槽（删除本身正确：按组件重复调用会让装备被画 N 份），代理 attachable 声明是包私有类型且不接收外部声明 |

三者共享同一概念——**一次模型渲染的「宿主 + 产物 + 挂载插槽」**，必须一次设计。

## 2. 决策

### 2.1 宿主抽象：`RenderHost`（G1）

`sealed interface RenderHost`，三种实现（实体 / 锚定 / 自定义作用域），只决定三件事：

| 成员 | 语义 |
|---|---|
| `owner()` | 宿主身份；`RenderData.owner` 必须是它。**实体身份与宿主身份是两个概念** |
| `entity()` | 可选的实体面：挂载物、栓绳、搬运方块、tint 用它；锚定宿主为 `null`，且**不兜底任何实体** |
| `applyTickVariables` / `applySubmitVariables` / `applyBehaviorContext` | 变量注入分层：tick 期只写 `variable.partial_tick`/`attack_time`，提交期额外绑定宿主引擎变量 |
| `applyRootPose(poseStack, cap, partialTick, applyEntityPose)` | 模型根位姿的**唯一**实现（脚本缩放 / 幼体缩放 / 睡眠位移 / 身体朝向） |
| `defaultPackedLight` / `defaultOverlay` | 调用方未显式指定光照/overlay 时的默认值 |

关键约定：

- `bindOwner` **只在 `cap.getOwner() == null` 时**初始化 owner。历史实现无条件 `ensureOwner`，会重建 scope 并冲掉数据自身的变量（作物生长阶段、饰品状态）；借用实体画挂载物的调用方因此长期带着「宿主污染」缺陷。
- 非实体宿主的 `query.*` 一律缺席（0/false/""）。兜底为「最近实体」会让作物模型随玩家状态抖动，比缺席更难排查。
- `render()` 与 `renderComponents()` 共用同一「每帧准备」入口；缺失宿主 → `RenderDiagnostics.recordSkip(MISSING_HOST, …)`（首个跳过抓调用栈）+ 开发环境抛 `IllegalStateException`。生产/`UNKNOWN`（26.1 无法判定）只计数 + 一次 WARN：在无法判定的节点抛错等于随机崩玩家。环境三态由 `EnvironmentPort.productionState()` 提供。

### 2.2 渲染产物：`RenderResult`（G2）

```java
record RenderResult(Matrix4f entityPose, Matrix4f rootPose,
                    int submittedPasses, int drawnComponents, int attachmentDraws,
                    RenderSkipReason skipReason)
```

- `renderResult()` 是新增入口，`render()` = `renderResult().drawn()`（保留布尔签名，不产生两套语义）。
- `renderComponents()` 由 `boolean` 改为返回 `RenderResult`（仓库内与下游三处调用均为语句式，源码兼容）。
- 矩阵空间：`rootPose` = 宿主位姿经 `RenderHost.applyRootPose` 之后，**不含**模型 Y 180° 翻转——与 `ModelPoseTransforms` 的入参一致，也与内置挂载使用的 `modelRoot` 同值。两个矩阵都是新副本（26.1 延迟 writer 执行时原 `PoseStack` 早已弹出）。
- `RenderSkipReason` 是**唯一**的跳过原因词表（`MISSING_HOST`/`SCOPE_UNINITIALIZED`/`ANIMATION_MISSING`/`NO_COMPONENTS`/`NOT_READY`/`NOT_BAKED`），与 ADR-0045 的 `BakeState` 是投影关系（`PENDING` → `NOT_BAKED`）。

### 2.3 外部挂载声明（G3）

```java
record AttachmentDeclaration(String identifier, AttachmentAnchor anchor, AttachmentDraw draw)
interface AttachmentAnchor { List<Resolved> resolve(RenderFrame); }      // boneChain/bone/locator/locatorFamily/modelRoot
interface AttachmentDraw   { int draw(RenderFrame, PoseStack, String anchorName); }
interface AttachmentSource { List<AttachmentDeclaration> declare(RenderFrame); }
AttachmentRegistry.register(source) → Registration(AutoCloseable)
```

执行模型（`AttachmentStage`）：

1. 时机固定在**模型几何提交之前**（26.1 的 submit 节点在几何 writer 内不再接收新内容），位姿为模型根位姿，因此挂载物与本体同姿、随骨骼动画移动。
2. **每实体每帧一次**：`hostBody=false` 的嵌套绘制（饰品/特效/携带宠物）不触发挂载阶段。这消除了「宿主装备被画到被挂载模型骨骼上」的重复绘制隐患（ADR-0043 §2.3 缺陷的另一种形态）；过去只因被挂载模型恰好没有 `head/body/rightitem` 骨骼才不可见。
3. **声明级去重**：同帧同 `identifier` 只画第一次，其余计入 `getAttachmentDeclarationDroppedCount()`。
4. **异常隔离**：单个源/单条声明抛错只跳过它并计数，不拖垮本体。
5. 顺序：真实 `minecraft:attachable` 槽 → 内置代理 → 外部源。外部源在 `declare` 时可见 `RenderFrame.handledSlots()`（**实际画出几何**的槽位），据此避让而不是按骨骼名反推——`rightitem`/`leftitem` 存在性判断这条内部规则因此不再外泄。

内置代理（`ProxyAttachable`）保留为装备策略输入（槽位/几何来源/候选链），由 `ProxyAttachments.declarations` 转成同一份 `AttachmentDeclaration`，使用 `AttachmentAnchor.selfAnchored()`（锚点解析仍在渲染器内：需要在绘制时取骨骼 pivot）。`ProxyAttachments.audit(model)` 与 `ProxyAttachmentsTest` 语义不变。

`RenderFrame` 暴露：宿主与其实体面、`renderData`、`clientEntity`、`models`/`primaryModel`、`tickedInfos`、`partialTick`/`light`/`overlay`、`sink`（+版本分叉的 vanilla 目标访问器）、`entityPose`/`rootPose`、`locator(name)`/`bonePose(name)`/`locatorNames()`（按模型弱引用缓存）、`newPose(matrix)`。

## 3. 后果

- 下游可以删除：`RenderSink` 根位姿装饰器、自采集 locator 表、`rightitem/leftitem` 判据、locator 前缀匹配、手写「首个 submit 假设」。首饰/乘客/携带宠物变成 `AttachmentRegistry.register(...)` + 若干声明。
- **行为变化（有意，共 4 条）**：
  1. `renderComponents` 现在执行提交期变量注入（作物/核心动画获得子帧插值与 `attack_time`）。
  2. owner 不再被渲染无条件覆盖（同一 `RenderData` 换宿主需显式 `cap.init(newOwner)`）。
  3. 缺失宿主从「静默不渲染」变「诊断 + dev 抛错」。
  4. 锚定宿主不再画挂载物（修复「作物上出现玩家手持物」的潜伏错误显示）。
- 未覆盖：外部声明是 additive，不能替换内置装备绘制（槽位 override 未做）；不做按 client entity/模型索引注册表（源自行过滤，避免注册时机与数据加载时机耦合）。

## 4. 验证

- 三节点 `mcmcp_build` / `mcmcp_nullaway` / `mcmcp_test`（含 `ProxyAttachmentsTest`、`AttachablePolicyTest`、`ArchitectureTest`）。
- 单测：`AttachmentAnchorTest`（骨骼链兜底、locator 缺失、`locatorFamily` 前缀语义、`modelRoot` 空间）、`RenderResultTest`、`AttachmentRegistryTest`、`RenderHostTest`。
- 运行时：`getSkipCount(MISSING_HOST)==0`（迁移后），`getAttachmentWithoutBodyCount()==0`，`getAttachmentDeclarationDroppedCount()==0`，`getAttachmentSourceErrorCount()==0`；clientsmoke 断言 `renderResult().rootPose()` 与 sink 首个 submit 的矩阵逐元素相等。
- 实机：三节点装备（铁剑 + 全套铁甲 + 头部物品）外观与 ADR-0043 §5 一致；motions4 的首饰/乘客/携带宠物 A/B 与迁移前逐帧一致。

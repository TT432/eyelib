# ADR-0043：装备挂载抽象——item 骨骼锚点与 body-part 骨骼绑定

> **状态**：已实施（2026-11-08）
> **关联**：ADR-0019（attachable 缓存生命周期）、ADR-0032（薄映射层）、ADR-0018（bridge 只暴露接口）
> **触发**：用户要求「检查实体的 attachment 是否正确附加到了手部，以基岩版为基准；同时查盔甲」，
> 并以「原生能持有工具的僵尸」为入口验证

---

## 1. 问题：挂载被当成「模型摆件 + 硬编码偏移」，而不是基岩版的骨骼挂载

基岩版把装备渲染表达为 **attachable**：几何是第二套模型，`binding`（Molang，通常
`q.item_slot_to_bone_name(context.item_slot)`）把它绑到持有者骨骼上，几何与焊接动画都写在
**持有者模型空间**里（官方文档 *Using Attachables* 两种做法：绑定到骨骼 / 复制骨骼 + 挂到
`rightItem`）。`enable_attachables` 文档原文即「allows the entity to render armor and weapons」，
`hide_armor` 是同一挂载系统的盔甲开关——**盔甲与手持物共用一套挂载语义**。

修复前的实现与这条语义无关：

| 现象 | 根因 |
|------|------|
| 手持物飘在头/肩附近，不在手上 | `HandItemRenderer` 在骨骼帧上再叠加硬编码 `Rx(-90)·Ry(180)` + `translate(-0.25, 0.1, -1.15)`。该平移在骨骼基下被放大到 ~1 格，是「按某一次调参凑出来」的常数，不是任何规范的值 |
| 26.1 上手持物完全不显示 | 挂载物渲染挂在几何 writer 里；26.1 的 `SubmitNodeCollector` 在 `renderAllFeatures` 阶段已不再接收新节点，`ItemInHandRenderer.renderItem(..., submitNodeCollector, ...)` 提交的物品几何被丢弃 |
| 多个渲染组件的实体重复画手持物 | `extraRender` 每个模型组件调用一次（发光/羊毛覆盖层 → 同一物品画 N 次） |
| 萌化实体完全没有盔甲 | `RenderLivingEvent` 被 cancel 后原版 `HumanoidArmorLayer` 不执行，而 eyelib 没有对应渲染路径；`AttachableItemRenderSetup` 只 tick 盔甲槽、从不渲染 |
| attachable 的 `binding` 字段被解析但从不求值 | `Model.Bone.binding()` 一路保留到渲染期，没有消费点 |

## 2. 决策

### 2.1 挂载点 = item 骨骼的 pivot，方向由两套模型空间的关系决定

- 挂载点取 `q.item_slot_to_bone_name(context.item_slot)`：`main_hand → rightitem`、
  `off_hand → leftitem`（骨骼名大小写不敏感，`GlobalBoneIdHandler` 已统一小写）。**骨骼缺失时不绘制**：
  基岩版没有「找不到挂点就退回原点」的可用语义，任何兜底都是错位（doki 模型普遍只提供 `rightitem`，
  因此副手物品不显示，与原版基岩版一致）。
- 骨骼帧由新增的 `ModelPoseTransforms.resolveBonePose(model, data, boneId, rootPose)` 给出：
  模型根位姿 → 链上逐骨 `applyBone`（与模型渲染同一条变换链，含模型 Y 翻转）。这是 locator 解析
  （`resolveLocatorPose`）之外的第二个公开入口，**链数学仍只有 `ModelPoseTransforms` 一处实现**。
- 骨骼帧上 `translate(pivot)` 等于「pivot 点在该帧中的位置」（`Matrix4f.translate` 施加 `R·pivot`），
  即基岩版 attachable 的挂载原点。
- 方向分两类：
  - **attachable 模型**：几何与焊接动画本就写在持有者模型空间的 item 骨骼帧里 → 直接渲染，
    不追加旋转/平移（此前叠加的 `Ry(180)` + 平移属于对同一套变换的重复施加）。
  - **普通物品**（无 attachable 定义）：物品模型在 Java 物品空间，需要 `Rx(-90)`。
    该角度由两套模型空间的关系推导：基岩版模型空间相对 JE 实体模型空间是**绕 Z 轴 180°**
    （x、y 取反、z 不变，行列式 +1，仍是右手系）。把该关系代入 vanilla `ItemInHandLayer` 的
    `Rx(-90)·Ry(180)`，`Ry(180)` 被抵消，只剩 `Rx(-90)`。

### 2.2 盔甲 = vanilla 人形盔甲几何按 body-part 骨骼名绑定

基岩版盔甲的引擎几何（`geometry.humanoid.armor.*`）骨骼名就是身体部件名；JE 的对应素材是
`HumanoidModel` 的盔甲部件——每个部件的 `PartPose` 位于关节，语义等于基岩版骨骼 pivot。因此：

- 部件名 → 持有者骨骼：`head`/`hat → head`、`body → body`、`right_arm/left_arm → rightarm/leftarm`、
  `right_leg/left_leg → rightleg/leftleg`（下划线/大小写差异在此吸收；骨骼不存在则不画）。
- 空间换算用 `scale(-1,-1,1)`（vanilla 模型空间 → 持有者模型空间，即上面的绕 Z 180°）。
- 部件集合按槽位（与 vanilla `setPartVisibility` 等价）：HEAD=`head`(+`hat`)、CHEST=`body`+双臂、
  LEGS=`body`+双腿、FEET=双腿。26.1 的盔甲网格已按槽位 retain 部件且 `hat` 是 `head` 的子骨骼，
  由 `//?` 分支处理。
- 渲染 pass = `ALPHA_TEST` + 禁剔除（对应 vanilla `armorCutoutNoCull`），走宿主 `MultiBufferSource`
  （26.1 的 submit 节点在几何 writer 内不可用）。
- 纹理/顶点色按版本由 adapter 解析（`ArmorRenderAdapter`）：1.20.1 = `ArmorItem` + `DyeableLeatherItem`
  + `ForgeHooksClient.getArmorTexture`；1.21.1 = `ArmorMaterial.layers()` + `ClientHooks.getArmorTexture`
  + `IClientItemExtensions` tint；26.1 = `DataComponents.EQUIPPABLE.assetId` → `equipment/<id>.json`
  （官方 `EquipmentClientInfo.CODEC`，宿主 `EquipmentAssetManager` 实例在 `Minecraft` 构造函数里是
  局部变量、不可达）→ `EquipmentClientInfo.Layer.getTextureLocation`。解析结果按资源包代际缓存，
  由 `@ResourceLoader` 注册的 `ArmorRenderCacheLoader` 在 reload 时清空。

### 2.3 渲染时机与去重

挂载物在**模型几何提交之前**渲染（`EntityRenderOrchestrator.renderComponents` 开头一次）：
根位姿 = 事件位姿副本 + `ClientEntitySetup.setupEntityClientEntityData`（脚本缩放/幼体/身体朝向），
与模型同姿；骨骼帧由 (模型, 本帧动画采样, 根位姿) 现算，不依赖几何通道。于是：

- 26.1 的物品几何不再被延迟 writer 丢弃；
- 去掉了 `SimpleRenderAction.extraRender` 钩子（其唯一用途是手持物，且按组件重复调用）；
- 双手/盔甲槽全空的实体（绝大多数）一次短路返回，不付任何骨骼/纹理成本。

## 3. 后果

- **已覆盖**：主/副手物品、attachable 模型（按 item 骨骼帧渲染）、四类人形盔甲 + 染色 tint、
  受伤 overlay、盔甲随骨骼动画（头/臂/腿）移动、资源包 reload 失效缓存。
- **未覆盖（显式登记）**：
  - `Model.Bone.binding()` 的 Molang 求值仍未实现。当前实现把 attachable 渲染在 item 骨骼帧上，
    等价于 `binding = q.item_slot_to_bone_name(...)` 这一最常见写法；自定义 binding（绑到别的骨骼）
    与 Bedrock 的 ±24px 绑定偏移未复刻。
  - 盔甲 trim / 附魔 glint 未渲染；`hide_armor` 未消费；`BODY` 槽（狼/马铠）不在范围内。
  - 萌化实体上的**盔甲 attachable** 不参与（人形盔甲优先），仅非萌化实体走 `HumanoidArmorLayerMixin`。
  - 非人形持有者（狼/蜘蛛等）没有 body-part 同名骨骼 → 不画盔甲，符合「按名绑定」语义。

## 4. 验证

实机（三节点，均 `spawn_chance=1.0` + 屋顶遮阳 + 铁剑 + 全套铁甲）：

| 节点 | 手持物 | 盔甲 | 证据 |
|------|--------|------|------|
| 1.20.1 | 剑在拳内、刀刃沿手部骨骼朝前上（同实例平移/朝向随骨骼动画变化） | 头盔/胸甲/护腿/靴全部绑定到位 | `versions/1.20.1/run/debug_shots/screenshots/attach_1201_ship.png`（终版构建）、`attach_swing1.png`（抬臂时剑与臂甲同步）、`attach_armor_doki.png` |
| 1.21.1 | 同上 | 同上 | `versions/1.21.1/run/debug_shots/screenshots/attach_1211_ship.png`（终版构建） |
| 26.1.2 | 修复前**完全不显示**（collector 丢弃），修复后显示 | 同上 | `versions/26.1.2/run/debug_shots/screenshots/attach_2612_ship.png`（终版构建）、`attach_2612.png`（修复前，无剑） |

对照与否定证据：

- 同场景原版渲染僵尸（`doki=false`）作参照：物品/盔甲形态一致，仅骨骼姿态不同（原版僵尸双臂前平举）。
- 副手：doki 僵尸无 `leftitem` 骨骼 → 副手盾牌不显示，且**没有**错误的兜底几何（`attach_offhand_missing.png`）。
- 静态核对：`resolveBonePose` 与手写链（root→waist→body→rightarm→righthandxia→rightitem）逐元素同值。

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

- **已覆盖**：主/副手物品、attachable 模型（手持按 item 骨骼帧；**盔甲槽按逐骨骼 binding/同名绑定**，
  见 §5.5）、四类人形盔甲 + 染色 tint、受伤 overlay、盔甲随骨骼动画（头/臂/腿）移动、
  资源包 reload 失效缓存。
- **未覆盖（显式登记）**：
  - Bedrock 的 ±24px 绑定偏移未复刻（`binding` 的 Molang 求值已实现，见 §5.5）。
  - 盔甲 trim / 附魔 glint 未渲染；`hide_armor` 未消费；`BODY` 槽（狼/马铠）不在范围内。
  - 盔甲 attachable 只在**存在 attachable 定义**时接管；没有定义时回退到 vanilla 人形盔甲部件
    （几何是玩家比例，萌化模型上尺寸不匹配，见 dokimobs `docs/bbmodel-pipeline.md` 2026-11-09 追记）。
  - 非萌化实体仍走 `HumanoidArmorLayerMixin`：整件 attachable 挂在单个 `ModelPart` 上，多骨骼部件
    刚性跟随一根骨。
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

## 5. 2026-11-09 修正与扩展（dokimobs 反馈）

### 5.1 body 盔甲部件锚点 = 颈部，不是 body 骨骼 pivot

§2.2 假设「body-part 骨骼的 pivot 就是 vanilla 部件原点」。实机证明该假设只对 zombie 成立：
skeleton/villager/zombie_villager/husk/drowned 的 `body` 骨骼 pivot 在**腰部**
（skeleton head=1.889 vs body=1.114），而 vanilla `body` 盔甲部件原点在**颈部**，导致胸甲/护腿
整体下移 0.6~0.8 格。修正：`piece.part()=="body"` 时仍在 body 骨骼帧内（继承躯干旋转/动画），
但平移锚点换成该模型 `head` 骨骼 pivot；无 head 骨骼回退 body pivot。`head`/`arm`/`leg` 不变。
证据：`ArmorAttachmentRenderer.neckPivot`；dokimobs
`docs/bbmodel-pipeline.md` 2026-11-09 §④（skeleton 修复前后截图）。

### 5.2 head 槽普通物品是独立层，不属于盔甲 attachable

不祥旗帜由 `Raid` 装备在 `EquipmentSlot.HEAD`，原版由 `CustomHeadLayer` 以
`ItemDisplayContext.HEAD` 的物品模型绘制；它不是 `ArmorItem`，因此 §2.2 的盔甲路径永远不会画它。
新增 `client/render/HeadItemRenderer`，与 vanilla 同变换（head 骨骼帧 → pivot →
`scale(-1,-1,1)` → `translateToHead`），在 `renderAttachments` 中先于盔甲/手持物调用；盔甲物品
跳过避免双画。证据：`HeadItemRenderer`、`EntityRenderOrchestrator.renderAttachments`；
dokimobs 2026-11-09 §①。

### 5.3 26.1 off-level 实体渲染：render state → 实体直接映射

26.1 的 `RenderLivingEvent` 不携带实体，原反查只搜索 `level.entitiesForRendering()`。CarryOn
等模组用不入世界的 NBT 假实体 + `extractEntity/submit` 渲染，反查失败 → doki 渲染不接管。
新增 `EntityRenderDispatcherMixin`（仅 26.1）在 `extractEntity` RETURN 记录
`EntityRenderState → WeakReference<LivingEntity>`（只登记 `level.getEntity(id) != entity` 的
off-level 实体）；`RenderLivingEventAdapter` 先查直接映射。`extractEntity` 每次返回新 state，
身份作 key 安全。证据：`EntityRenderDispatcherMixin`、`ExtractedEntityStates`、
`RenderLivingEventAdapter` 26.1 分支；dokimobs 2026-11-09 §⑤（CarryOn 牛截图）。

### 5.4 26.1 `query.is_powered` 不再写死 false

`EntityPortAdapter.isPowered` 的 `>=26.1` 分支曾直接 `return false`（`PowerableMob` 在 26.1 移除）。
苦力怕是唯一实现者，改为 `entity instanceof Creeper c && c.isPowered()`。该查询同时驱动
charged creeper 的基础贴图数组与 armor RC 的 `part_visibility`，写死 false 会让闪电层完全不显示。
证据：`EntityPortAdapter.isPowered`；dokimobs 2026-11-09 §⑥（三节点 charged creeper 截图）。

**验证**：三节点 `compileJava`、`:test`、`:nullawayMain` 全部通过；26.1.2 实机 CarryOn 2.10.0
（服务端 `Player#interactOn` 事件链返回 `Success`、`carry=true`）抱起 doki 牛显示 doki 模型；
1.20.1/1.21.1/26.1.2 的掠夺者旗帜/弩显隐/头部独立转动、skeleton 盔甲、charged creeper 均已
实机截图（见 dokimobs 同节）。未覆盖：CarryOn 客户端键位完整输入链路；`uv_anim` 滚动。

### 5.5 盔甲槽 attachable 参与渲染 + 逐骨骼绑定

§3 曾把「萌化实体上的盔甲 attachable 不参与」登记为缺口：`renderAttachments` 只走
`HeadItemRenderer`/`ArmorAttachmentRenderer`/`HandItemRenderer`，attachables 只 tick 不渲染。
现在盔甲四槽（HEAD/CHEST/LEGS/FEET）先查 `AttachableResolver`，命中即按基岩版语义渲染，
并由 `renderSlotAttachables` 把该槽加入「已被接管」集合，头部物品与 vanilla 人形盔甲两条回退路径
跳过该槽（避免双画）。

绑定规则（对应官方 *Using Attachables* 的两种做法）：

| 情形 | 锚点 |
|---|---|
| 骨骼有 `binding`（Molang，通常 `q.item_slot_to_bone_name(context.item_slot)`） | 求值得到持有者骨骼名 → 该骨骼 pivot 作为子树根位置，几何仍写在挂载物模型空间 |
| 无 `binding` 但骨骼名与持有者同名（`head`/`body`/`rightarm`…） | 持有者同名骨骼驱动（「复制骨骼」做法） |
| 两者都无 | 持有者模型根位姿（基岩模型空间） |

实现要点：

- 骨骼帧由 `ModelPoseTransforms.resolveBonePose` 从**同一帧**的持有者动画采样现算：矩阵 = 持有者骨骼帧
  × `T(持有者骨骼 pivot)` × `T(-挂载物骨骼 pivot)`，即把挂载物骨骼 pivot 对齐到持有者骨骼 pivot；
- 每个**顶层**骨骼单独渲染，其余子树用 `part_visibility` 逐骨骼关掉（`PreBoneFrame` 不继承父隐藏，
  不能只关顶层），子树内保留渲染控制器的 part_visibility；
- `binding` 表达式按源码缓存为 `MolangValue`（编译缓存之外的每帧分配优化），求值失败按「无绑定」降级；
- `ModelPoseTransforms.bedrockRootPose` 提供「持有者根位姿 → 基岩模型空间」的换算，供无绑定骨骼使用。

数据侧（dokimobs）：27 个萌化实体的 client entity description 全部加 `enable_attachables: true`
（此前缺省 false，`AttachableResolver.isAttachableEnabled` 对萌化 holder 恒 false，盔甲/手持 attachable
从未参与）。裸 `enable_attachables` 默认 value 见 `BrClientEntity` codec。

**未改变的行为**：没有 attachable 定义的物品（如原版铁甲）仍走 §2.2 的 vanilla 人形盔甲路径，
外观与 21.1.34 一致；手持 attachable 仍走 item 骨骼帧路径（本次未迁移）。

### 5.6 原版装备渲染收敛为「代理 attachable」（2026-11-12，21.1.40）

**触发**：用户报告「僵尸村民手持物品没有显示」，并要求「把原版渲染分别用一个代理 attachable 管理，
对每个模型检查 attach 目标是否存在」。盘点结论：§2.2/§2.3 的三条原版回退路径（头部物品 / 人形盔甲 /
手持物）是三个各自硬编码的渲染器，槽位归属靠调用方手写、挂载目标（持有者骨骼名）各自隐式约定；
`HandItemRenderer` 只认 `rightitem`/`leftitem`，模型缺骨骼时静默返回，无从发现。

**决策**：新增 `ProxyAttachable` + `ProxyAttachments`（application 层，`client/render/`），把每一类原版
装备渲染表达成与 `minecraft:attachable` 同形的**声明**：

| 声明字段 | 语义 |
|---|---|
| `identifier` | 稳定 id（`eyelib:held_item_main` 等），审计/诊断用 |
| `slot` | 管理的 `EquipmentSlot`（HEAD 槽由「头部物品」与「头盔」两条代理按「是否盔甲物品」分流） |
| `kind` | 几何来源：`HELD_ITEM` / `HEAD_ITEM` / `ARMOR_PIECE` |
| `bindTargets` | 候选链列表，按序取第一个存在于持有者模型的骨骼；全缺失 = 不绘制（基岩语义，无原点兜底） |

`ProxyAttachments.render` 是槽位归属的唯一裁决点，顺序复刻 vanilla（头部物品 → 盔甲 CHEST/LEGS/FEET/HEAD
→ 副手 → 主手）；真实 `minecraft:attachable` 命中的槽位整条跳过（挂载定义优先于原版回退，不会双画）。
三个渲染器改为**单槽**入口（`render(action, holder, model, tickedInfos, modelRoot, proxy, light)`），
绑定骨骼由代理声明解析，不再各自硬编码名字。

**手持物候选链**：`rightitem`/`leftitem`（基岩规范挂点）→ `righthand`/`lefthand` → `righthandlocator`/
`lefthandlocator`。链内只放「手」级别骨骼：手臂/前臂 pivot 在肩/肘，拿它当挂点会把物品摆到关节上
（所以**不**包含 `*forearm`、`*handxia` 这类关节骨）。

**审计入口**：`ProxyAttachments.audit(Model)` 逐代理输出「候选链 → 解析到的骨骼 / MISSING」，
`ProxyAttachmentsTest` 固定了三件事：命中 `rightitem`、沿链兜底到 `righthand`/`righthandlocator`、
只有肩/肘骨骼时报 MISSING（不允许兜底）。

**诊断计数**（`EntityRenderOrchestrator` getter，dev 用）：`getDuplicateSuppressedCount()`
（同帧同姿态重复提交被丢弃的次数）、`getAttachmentWithoutBodyCount()`
（画了挂载物但本体一帧内没提交几何的帧数；正常恒 0）。

**数据侧配套（dokimobs）**：`minecraft:zombie_villager` / `minecraft:villager` 的 15 个职业模型补
`rightitem`/`leftitem` 空骨骼，`zombie`/`husk`/`stray` 补镜像的 `leftitem`。**必须两个村民实体一起补**：
`geometry.unskilled`… 等 14 个几何名在两个条目里同名重复，而 `ModelManager` 全局按名注册（后写覆盖），
只补一方会解析到另一方的旧副本（实测只补 zombie_villager 时运行时仍是 70 根骨骼、无 rightitem）。

**验证**：三节点 `compileJava`/`:test`/`:nullawayMain` 全绿；1.20.1 / 1.21.1 / 26.1.2 实机 A/B
（主手清空 vs 主手石头 + 副手铁剑）：空手无物品、装备后石头出现在拳内、剑在副手；26.1.2 计数
`dup=0`、`attNoBody=0`（130 只活体实体的场景，连续 8.7 万次实体渲染）。详见 dokimobs
`docs/bbmodel-pipeline.md` 同日小节。

**资源路径约定**（本轮踩坑记录）：所有资源加载器都继承 `BrResourcesLoader`，实际扫描
`assets/<命名空间>/eyelib/<directory>/`——attachable 定义在 `assets/<ns>/eyelib/attachables/*.json`
（不是 `assets/<ns>/attachables/`），models/render_controllers/entity/animations 同理。

**未覆盖 / 仍缺失**：`bbmodel:pillager`、`bbmodel:blaze` 的主手无手部骨骼（掠夺者的弩是烘焙几何）；
`villager` 的原版语义是交叉双臂持物，本实现按「手内持物」渲染；`armor_feet` 在 sheep/guardian/enderman
与 `armor_legs` 在无 `body` 骨骼的模型上仍不绘制（结构性缺口，见审计表）。

### 5.7 四个装备渲染开关 + 代理/真实 attachable 的同构化（2026-11-12，21.1.41）

**触发**：用户追问「基岩版是不是有一个 attachable 渲染开关」并要求「代理 attachable 不应该和真实的
attachable 有差别，四个字段都实现一下」。

官方文档（bedrock.dev *Entities* / Microsoft Learn「Client Entity Documentation」）给出四个
`minecraft:client_entity.description` 字段：

| 字段 | 文档语义 | 本库实现 |
|---|---|---|
| `enable_attachables` | 「allows the entity to render armor and weapons」——装备渲染总开关，缺省 false | `AttachablePolicy.allowsAttachables` |
| `hide_armor` | 「armor … should be hidden … This overrides the rendering settings specified by 'enable_attachables'」——只压盔甲 | `AttachablePolicy.allowsArmor` = `allowsAttachables && !hide_armor` |
| `held_item_ignores_lighting` | 手持物是否全亮 | `AttachablePolicy.heldItemLight` → `FULL_BRIGHT` |
| `held_item_scale` | Molang 缩放手持物，缺省 1.0，必须解析为 > 0 | `AttachablePolicy.heldItemScale`（<=0/非有限/求值异常/无作用域 → 1.0） |

**同构化（回答「代理与真实 attachable 不应有差别」）**——三条路径被收敛到同一组函数：

1. **准入**：`AttachableResolver.resolve(holder, stack, slot)` 是装备渲染的唯一准入点，内部走
   `AttachablePolicy.allows(holderCe, isArmorStack(holder, slot, stack))`。盔甲槽物品受
   `enable_attachables && !hide_armor`，其余物品（手持物/头饰/非盔甲头部物品）只受 `enable_attachables`。
   真实 attachable 的四处调用（`AttachableItemRenderSetup.getOrPrepare`、`HumanoidArmorLayerMixin`、
   `ItemInHandRendererMixin`、Molang 查询）与代理 attachable（`ProxyAttachments.render` 的同一判定）
   因此**不可能**对同一份数据给出不同结果。
2. **挂载点**：真实 attachable 的 `binding` 求值结果 / 同名骨骼在 `AttachableItemRenderSetup.holderBoneId`
   里退化成「单元素候选链」，直接调用 `ProxyAttachable.resolveChain`；代理的多候选链也走它。
   「目标缺失就不画」只有一份实现。
3. **槽位归属**：`ProxyAttachments.render` 仍是唯一裁决点，真实 attachable 命中的槽整条跳过（不双画）。

**实现细节**

- `BrClientEntity` 的可选字段已到 DFU `RecordCodecBuilder.group` 上限（16），四个开关收进嵌套
  record `BrClientEntity.Toggles(enable_attachables, hide_armor, held_item_ignores_lighting, held_item_scale)`
  （读同一层 JSON 的四个平铺键，编码时缺省不写回——`absentEquipmentRenderTogglesStayAbsentAfterEncode`
  固定了这条，避免导出污染所有 client entity JSON）；对外仍保留四个同名访问器。
- `RenderData.peekComponent(entity)`（只读、不创建）供渲染期只读查询；旧 `getComponent` 会
  `getOrCreate`，只读路径必须避免给未托管实体落盘空组件。
- `held_item_scale` 在 `HandItemRenderer` 里作用于两条分支（真实 attachable 模型 / 原版物品模型），
  在 `translate(pivot)` 之后施加，即绕挂载点缩放。

**验证（26.1.2 实机，dev 包内逐字段改写 cospack 的 description）**

| 字段 | 期望 | 实测 |
|---|---|---|
| `hide_armor: true`（zombie） | 无盔甲、剑可见 | 铁盔/胸甲消失，铁剑仍在拳内 |
| `enable_attachables: false`（husk） | 盔甲与武器都不画 | 同帧对照：旁边 skeleton（默认 true）有铁盔+胸甲+剑，husk 两者皆无 |
| `held_item_ignores_lighting: true`（drowned，午夜无夜视） | 剑亮、身体暗 | 剑呈亮白，对照 zombie 的剑暗 |
| `held_item_scale: "3.0"`（drowned） | 剑明显变大 | 同帧对照：drowned 的剑比 zombie 的长约 3 倍 |

截图：`versions/26.1.2/run/debug_shots/screenshots/{toggle_hide_armor_zombie,toggle_ab_husk_vs_skeleton,final_dark_pair}.png`
（dokimobs 侧同名记录见 `docs/bbmodel-pipeline.md`）。

**踩坑记录**：改 cospack 里的 description 做验证时，任何一次 `processResources`/`assemble` 都会把
`build/resources` 覆盖回源码版本——dev 验证必须「构建 → 改 build/resources → 启动 → 截图」，
中途重跑构建会让开关静默失效（本轮先在 stray 上浪费了数轮实机排查）。

**未覆盖**：`enable_attachables` 对首人称玩家自身手持 attachable 的影响（玩家无 client entity 定义，
按「默认允许」处理）；`held_item_scale` 对第一人称 attachable 渲染的影响。

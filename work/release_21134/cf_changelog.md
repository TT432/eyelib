## 头部槽物品渲染、盔甲锚点与 26.1 渲染接管

**HEAD 槽普通物品（不祥旗帜）**：此前 eyelib 取消原版 `LivingEntityRenderer` 后只渲染人形盔甲，HEAD 槽里的普通物品（掠夺者不祥旗帜）没有任何渲染路径。新增 `HeadItemRenderer`，按 vanilla `CustomHeadLayer` 的变换（`translate(0,-0.25,0)`、Y 180°、0.625 缩放）以 `ItemDisplayContext.HEAD` 绘制；盔甲物品仍由盔甲层处理，不重复绘制。

**盔甲 body 部件锚点**：vanilla 人形盔甲 `body` 部件原点在颈部，而多数萌化模型的 `body` 骨骼 pivot 在腰部（skeleton/villager/husk/drowned/zombie_villager），胸甲/护腿会整体下移。现在 body 部件仍继承躯干骨骼动画，但锚点改用该模型 `head` 骨骼 pivot（颈部）；zombie 等 pivot 本就重合的模型不受影响。

**26.1 充能状态查询**：`query.is_powered` 的 26.1 分支此前直接返回 false（`PowerableMob` 已移除），导致闪电苦力怕的 charged 贴图与闪电层永远不显示。改为读取 `Creeper.isPowered()`。

**26.1 off-level 实体渲染接管**：26.1 的渲染事件不带实体，原逻辑只能在 `level.entitiesForRendering()` 里按类型+坐标反查。新增 `EntityRenderDispatcherMixin` 在 `extractEntity` 时记录 render state→实体（仅不在世界中的实体），CarryOn 等模组用 NBT 假实体渲染的物品也能走 doki 模型，不再退回原版外观。

**验证**：1.20.1 / 1.21.1 / 26.1.2 三节点 `compileJava`、`:test`、`:nullawayMain` 全部通过；实机验证掠夺者旗帜、skeleton 盔甲、charged creeper 与 CarryOn 抱起萌化生物。

**前置：需要配合 dokimobs 0.3.13 或更高版本。**

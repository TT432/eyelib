## 盔甲槽 attachable 逐骨骼绑定

**新的渲染路径**：盔甲四槽（HEAD/CHEST/LEGS/FEET）的 `minecraft:attachable` 定义现在会被真正渲染。此前它们只被 tick、从不绘制——`LivingEntityRenderer` 取消后只有头部物品 / vanilla 人形盔甲 / 手持物三条路径，萌化实体上的 attachable 完全无效。

**绑定语义（对齐基岩版官方两种做法）**：

- 骨骼声明了 `binding`（Molang，通常 `q.item_slot_to_bone_name(context.item_slot)`）→ 求值得到持有者骨骼名，把挂载物骨骼 pivot 对齐到该骨骼 pivot 并继承其骨骼帧；
- 未声明 `binding` 但骨骼名与持有者同名（`head` / `body` / `rightArm`…）→ 由持有者同名骨骼驱动；
- 两者都没有 → 以持有者模型根位姿为根（基岩模型空间）。

每个顶层骨骼单独渲染、用 `part_visibility` 逐骨骼屏蔽其它子树，因此多骨骼 attachable（胸甲 = body + 双臂）能各自跟随；命中 attachable 的槽由 attachable 接管，头部物品与 vanilla 人形盔甲不再重复绘制。`Model.Bone.binding()` 的 Molang 求值从「已解析未使用」变为真实消费点，clientsmoke 增加对应断言。

ADR-0043 §3 未覆盖清单更新、新增 §5.5 记录绑定规则；版本 21.1.35。

**前置：需要配合 dokimobs 0.3.13 或更高版本**（见 0.3.14，其 27 个萌化实体已开启 `enable_attachables`）。

**验证**：三节点 `compileJava` / `:test` / `:nullawayMain` 全绿；clientsmoke 1.20.1 12/12 passed（含 binding 断言）；1.20.1 / 1.21.1 / 26.1.2 实机探针（`head` 的 binding + 同名 `body` 骨骼）落位正确，同场景原版铁甲仍走回退路径、外观不变。

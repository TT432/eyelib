## 装备挂载：item 骨骼锚定与盔甲渲染

**手持物锚点回到骨骼**：此前在 item 骨骼帧上再叠加固定偏移（`translate(-0.25, 0.1, -1.15)` 与 `Ry(180)`），位置由常数决定，实测整体落在头/肩附近。现在挂载点取 `q.item_slot_to_bone_name(context.item_slot)` 对应骨骼的 pivot（`main_hand → rightitem`、`off_hand → leftitem`，大小写不敏感），骨骼帧由模型根位姿逐骨累积得到（新增 `ModelPoseTransforms.resolveBonePose`，与模型渲染同一条变换链）；普通物品只施加由两套模型空间关系推导出的 `Rx(-90)`，attachable 模型本身写在持有者模型空间，直接按骨骼帧渲染。模型没有对应 item 骨骼时不绘制（不做原点兜底，与基岩版一致——只有 `rightitem` 的模型副手物品不显示）。

**盔甲渲染**：此前接管环境渲染后原版 `HumanoidArmorLayer` 不再执行，萌化实体只 tick 盔甲槽、从不绘制。现在按基岩版挂载语义把 vanilla 人形盔甲部件按 body-part 骨骼名绑定（`head`/`body`/`rightarm`/`leftarm`/`rightleg`/`leftleg`），四槽（头/胸/腿/靴）可见性、染色 tint、受伤 overlay、随骨骼动画移动均生效；纹理与顶点色按版本解析（1.20.1 `ArmorItem` 材质、1.21.1 `ArmorMaterial.layers()`、26.1 `equipment/<id>.json`），并按资源包代际缓存。

**26.1 挂载物完全不显示**：挂载渲染改到模型几何提交之前（每实体渲染一次），不再被 26.1 `SubmitNodeCollector` 的渲染延迟 writer 丢弃；同时删除按渲染组件重复调用的 `extraRender` 钩子，多渲染组件（发光/羊毛覆盖层）的实体不再重复绘制手持物。双手与盔甲槽全空的实体一次短路返回。

**验证**：1.20.1 / 1.21.1 / 26.1.2 三节点实机（以原版可持工具僵尸为入口，铁剑 + 全套铁甲，静止与抬臂）：剑在拳内且随骨骼动画同步、盔甲四件绑定到位；26.1 修复前完全不显示。新增 `resolveBonePose` 三用例单测。

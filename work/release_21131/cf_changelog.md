## 搬运方块（末影人 carried block）渲染

- 新增 `CarriedBlockRenderer`：实体搬运方块时按宿主的 `carriedBlockState` 绘制方块本体；方块模型/贴图/渲染类型全部来自宿主方块渲染器，不写死方块 id。
- 位置取模型 `lefthand`/`righthand` 骨骼当前动画姿态的中点，朝向/缩放取根骨骼姿态；资源包没有双手骨骼时不渲染（不做错误位置兜底）。
- 26.1：方块在实体 submit 阶段提交（deferred geometry writer 阶段 `BlockModelRenderState.submit` 不会出画）。
- 与 `query.is_carrying_block` / `carrying` 控制器解耦：资源包只负责姿态，方块由本渲染补齐；1.20.1 / 1.21.1 / 26.1.2 三节点实机验证。

# Model / Animation LOD 实施计划

## 任务分解

### 1. 纯运行时模型

- 新增三级 `LodLevel`、屏幕阈值与迟滞策略。
- 新增每实体 `LodRuntimeState`，记录级别、屏幕比例和每世界单位像素数。
- 新增 `AnimationLodState`，负责采样调度与姿势插值。
- 扩展 `ModelRuntimeData`：effects-only 写入守卫、深复制、姿势插值。

验证：纯 JUnit 覆盖规格 FR-1/FR-3 与数值边界。

### 2. 渲染和动画接线

- `FramePlan` 增加单调 frame index。
- `EntityRenderOrchestrator.TickStage` 每实体每帧解析 LOD；动画控制/effects 每帧执行，姿势按 1/2/4 帧采样。
- `RenderData` 持有每实体 LOD 状态。
- `BakedBone` 烘焙时预计算局部几何尺寸。
- `RenderHelper` 把实体 LOD 状态放入 `ModelVisitContext`。
- `HighSpeedRenderModelVisitor` 在顶点变换前应用骨骼屏幕像素阈值。

验证：构建、单测；运行时探针比较 FULL/LOW 的 LOD 状态和采样计数。

### 3. Debug 界面

- 修复 `ModelPreviewScreen.performSearch` 的中断实现。
- 增加上一模型/下一模型按钮。
- 增加全局 LOD 强度滑条，并把预览 LOD 状态注入直接 DFS 渲染路径。
- 选择 Manager 模型时使用 vanilla 白色纹理；保留 `.bbmodel` 拖入路径。
- 显示模型、LOD、强度和视图状态。

验证：dev client 中打开 `V` 界面，通过 `/eval` 检查 widget、选中模型与全局强度；截图/运行观察确认滑条改变预览几何。

### 4. 多版本与收尾

- 依次构建 1.20.1、1.21.1、26.1.2；只在 bridge/debug 基础设施中处理版本 API 差异。
- 运行相关单测和 1.20.1 dev-client smoke。
- 处理过程反馈，按规格复核并提交。

## 风险与回退

- 自动骨骼几何剔除可能让高视觉权重小部件消失：阈值以 1/2.5 像素限制，FULL 永不剔除，Debug 强度可立即关闭。
- 动画降采样可能影响 effects：使用不可写姿势输出而不是跳过动画 tick；effects/state machine 仍每帧执行。
- 26.1 GUI API 尚有既有 `extractRenderState` stub：本任务保证编译；运行验收以 active 1.20.1 为准，不扩大为 26.1 整个预览渲染迁移。
- 如果跨版本 FOV API 不一致，把读取封装到现有 bridge Port，而不是退化为固定距离阈值。

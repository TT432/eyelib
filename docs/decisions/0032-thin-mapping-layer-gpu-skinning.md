# ADR-0032：薄映射层原则修订——允许自有 shader 与 GPU 驻留几何（GPU 蒙皮）

## 状态

已接受（2026-08-27）。修订 `docs/concepts/cross-version-render-architecture.md` §9
「eyelib 不自建 VBO / 不自建着色器」原则。驱动：渲染 GPU 化调研
（`docs/research/2026-08-26-render-gpu-offload.md`）C1 项——实体顶点蒙皮迁至
顶点着色器，在完整保留全部功能的前提下消除每帧 CPU 顶点变换与全量顶点重发。

## 背景

原原则（§9 设计原则）：eyelib 是 Bedrock 渲染语义在 JE 基础设施上的薄映射层，
不重写 vanilla 管线，只在「材质语义→RenderType」与「骨骼变换→顶点写入」两个
接缝处做翻译；落地为「不自建 VBO、不自建着色器」两条禁令。

该原则写于 CPU 软件蒙皮时代：每帧骨骼矩阵合成后逐顶点 CPU 变换并写入 vanilla
BufferBuilder，GPU 端仅消费 vanilla entity shader。调研证实这是每帧
`E × B × V` 级 CPU 成本与全量顶点上传的根源，而 Bedrock 几何是刚体骨骼绑定
（每顶点恰好一根骨骼、无权重混合），蒙皮数学与 VS 逐位同构，是安全可搬的最大项。

## 决策

1. **「骨骼变换→顶点写入」接缝迁移为「骨骼变换→骨骼调色板上传 + VS 蒙皮」**：
   - 加载期：烘焙几何带 `boneIndex` 顶点属性，一次性上传为静态 GPU 缓冲
     （26.1.2 用 blaze3d `GpuBuffer`；≤26.1 用 vanilla `VertexBuffer`）。
   - 每帧：CPU 骨骼矩阵合成（保持不变，locator/attachable/AR 仍需 CPU 世界矩阵）
     的产物数组上传为骨骼调色板（26.1.2 用 DynamicUniforms 同构 UBO ring buffer；
     ≤26.1 用 TBO RGBA32F），顶点蒙皮在 VS 内完成。
2. **缓和条款（保留薄映射层精神）**：只允许通过 vanilla/loader 官方抽象使用
   GPU 资源——`RenderPipeline`/`ShaderInstance`（Forge `RegisterShadersEvent`）、
   `VertexBuffer`/`GpuBuffer`、`DynamicUniforms`；**禁止裸 GL 调用**（裸 GL 仅限
   既有诊断/截帧用途）。提交仍走 RenderSink，排序仍委托 vanilla。
3. **功能保留为硬约束**（调研 §4）：partVisibility（骨骼分段 draw range）、
   材质分区/passOrder/透明排序、overlay/lightmap 语义、法线归一化、
   locator/attachable、AR 兼容分支（AR 安装时回退 CPU 路径）。
4. **版本次序**：26.1.2 先行（官方自定义 RenderPipeline + DynamicUniforms 范式），
   随后移植 1.20.1/1.21.1（Forge RegisterShadersEvent + 自定义 VertexFormat +
   TBO 调色板），三版本行为对齐以 FBO 像素回归为准。
5. **死资产决策（调研 P0②，2026-08-27 已执行）**：3 组 `*_chunk` shader、
   `ShaderManager`、`shader_mapping.json` 经核实为零引用死资产，已删除而非接线
   （70bea805）；C1 的 shader 按 §1 新管线需求新写，不复用尸体。

## 后果

- §9 对应关系表中「顶点缓冲系统」「着色器系统」两行的禁令解除，替换为
  「经 vanilla 抽象持有的自有 GPU 资源」；其余各行（不重写管线、接入点、
  上传委托 vanilla）不变。
- CPU 蒙皮路径在 C1 落地期间保留为回退分支（AR 兼容 + 浮点回归兜底），
  三版本 FBO 像素回归对齐后方可考虑移除。
- 浮点一致性风险（CPU double 中间值 vs GPU fp32）由 RenderDoc GetPostVSData
  数值对比定容差，验证方法见调研报告 §5 C1。

## 验证

- 本 ADR 为原则修订，实现验证随 C1 各阶段执行：RenderDoc GetPostVSData 顶点
  数值对比 + clientsmoke FBO 像素回归 + mixed-n96 benchmark 前后对比
  （调研报告 §6 P1/P2 验证列）。

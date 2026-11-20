# ADR-0048：非动画 ClientEntity 的静态实例化渲染

> **状态**：已实施（2026-11-17）
> **关联**：ADR-0032（薄映射层 / GPU 蒙皮）、ADR-0039（Iris 下 26.1 禁蒙皮）、ADR-0042（实体 pass 状态单一权威）
> **触发**：非动画 client entity（如装饰/场景道具）仍按动画实体支付每实体骨骼调色板上传与逐顶点蒙皮成本。

---

## 1. 问题

C1 GPU 蒙皮（ADR-0032）把骨骼调色板作为逐实体数据上传：`slotCount × 2 mat4`
（≤26.1 合批路径按实体打包 palette + 64B meta；26.1 路径按会话在 UBO ring 里分配 block），
compute 路径再逐顶点写出 80B 的蒙皮结果。对**没有任何动画姿态**的 client entity，
每个实例的骨骼世界矩阵只差一个实体根位姿，上述数据全部是重复项：

- 同一模型的 N 个静止实例，每帧上传 N 份等值 palette；
- compute 路径仍逐实例 dispatch 并写 `顶点数 × 80B`；
- CPU 回退路径仍逐顶点做 `transformPos/transformNormal` 并全量写顶点。

`ModelRuntimeData` 无 entry 时 `ModelPoseTransforms.applyBone` 取骨骼 bind 值，
因此 bind 姿态下每骨骼世界矩阵 = 实体根位姿 × 常量骨骼链——可把常量烘进几何。

## 2. 决策

1. **逐帧静态判定**：一次提交的所有骨骼都没有 `ModelRuntimeData` entry，且渲染器允许
   静态通道（AR 路径除外），走静态几何路径。定义级静态（client entity 的
   `animations`/`animate` 为空）用作快路径，跳过逐骨骼扫描。
2. **几何摊平**：为每个 `BakedModel` 额外烘焙一份「bind 姿态摊平」的模型空间几何
   （含模型 Y 180° 与骨骼链 bind 变换），与现有带 boneIndex 的蒙皮几何并存——
   同一模型可能若干帧有动画、若干帧没有。几何为 GPU 驻留静态缓冲，按骨骼分段，
   `part_visibility` 用分段 draw 表达。
3. **逐实例数据**只有 root 位姿（mat4）、法线阵（mat3）、tint、light UV、overlay UV
   （40 float = 160B，std140 对齐），与 Carbon 的通用实例布局（144B）同量级；不再有 palette。
4. **节点实现差异**（能力边界，来自一手 API 证据）：
   - **≤26.1（compute 模式，GL ≥ 4.6）**：实例数据走 SSBO（`gl_InstanceID` 索引，160B/实例），
     `glDrawArraysInstanced` 对同一 (几何, RenderType, 可见性分段) 一批一次 draw。
     不用逐实例顶点属性：1.20.1 的 `VertexFormatElement` 只允许非 UV usage 的 index 为 0，
     13 个属性无法在单个 `VertexFormat` 中表达；静态通道与 compute 合批同门控（GL ≥ 4.6），SSBO 无额外门槛。
     新增 4 个静态变体 shader（cutout/solid/translucent/emissive）。
   - **26.1.2**：blaze3d 的 `VertexArrayCache` 不设置属性除数（仅 `bindVertexArray(VertexFormat,
     GlBuffer)`，无逐实例属性入口），**不能**用顶点属性实例化；改为「静态几何 + 逐 draw
     `InstanceTransform` UBO（std140，160B）」：几何仍只驻留一份、无 compute、上传量从 palette
     降为 160B/实体。draw 次数与现状相同（26.1 原本就是逐实体区间 draw）。
5. **不退化的边界**：静态路径与 C1 会话共用同一 drain 时序与分组（RenderType 首见序、组内
   提交序），半透明语义不变；任何不满足条件的提交（有动画采样、AR、Iris 光影、未知
   RenderType、可见性模式数爆炸）回退 C1/CPU 路径，保持既有行为。
6. **定义级预热**：client entity 定义无动画时，静态判定走常量布尔，跳过逐骨骼扫描；
   GPU 几何仍按模型首次静态提交惰性创建（一次性）。

## 3. 后果

- 非动画实体的每帧上传从 `slotCount × 128B + 逐顶点 80B` 降为 `144B/实例`（≤26.1）
  或 `96B/实例`（26.1），且 ≤26.1 的 draw 次数从「每 RenderType 组一次」保持为
  「每组内每 (几何, 可见性) 一次」——批量更大。
- 每个 `BakedModel` 最多多一份静态几何的 GPU 内存；资源重载时与蒙皮几何同批释放。
- 静态判定是逐帧的：同一实体在动画启停之间会切换路径，两条路径的顶点数学必须一致
  （由打包器单测与实机像素回归保证）。
- 26.1 不减少 draw 次数：收益限于上传量与 compute 消除。

## 4. 验证

- 单测：`StaticGeometryPackerTest`（摊平位置/法线/UV、slot 升序与零顶点跳过、可见分段合并、空几何）与
  `ModelRuntimeDataEntryTest`（逐帧判定边界）通过；1.20.1 全量测试绿。
- 编译：三节点 `compileJava` 绿；NullAway 三节点无违规。
- clientsmoke：1.20.1 12/12、1.21.1 9/9、26.1.2 9/9 全通过（1.21.1/26.1.2 的 smoke run 目录需放入
  A&S 资源包，否则 `ClientEntityManager` 为空，实体捕获测试以 `ANIMATION_MISSING` 失败——与本次改动无关）。
- 1.20.1 实机 A/B（同一会话、同一实体）：非动画 client entity 绑定后静态通道生效
  （`STATIC_GEOMETRIES=6`），与 CPU 路径（反射切 `Mode.VERTEX_SHADER` 使 `createSession` 返回 null）
  对比，模型区域像素差 0（max 1）、天空 0；恢复动画绑定后 C1 compute 路径渲染正常。
- 1.21.1 实机 A/B（同一会话）：静态通道生效（`STATIC_GEOMETRIES=6`），与 CPU 参照逐像素对比，
  牛模型区域差异仅来自背景中移动的史莱姆（模型本身零差异）。
- 26.1.2 实机：静态通道生效（`STATIC_GEOMETRIES=6`）并正确渲染绑定姿态；CPU 对照
  （`-Deyelib.gpuSkinning=false` 重启）因跨会话相机/时间漂移无法逐像素对齐，几何与位姿目检一致——
  26.1 特有代码（UBO 布局、管线派生、draw）以实机可见正确性与共享打包器单测为准。
- 基准（`docs/perf/c2-static-entity-instancing.md`，A&S 牛，64/192 实体，fbo + world）：相对 C1 的
  `render_work_p50` 降低 1.20.1 12–27%（相对经典 CPU 蒙皮 34–42%）、1.21.1 13–20%、26.1.2 11–36%；
  26.1 无实例合批，收益来自消除 compute 与上传。三档之间只有静态开关不同，差值远超本机 ±5% 噪声，
  因此差值本身即是静态通道生效的行为证据。
- 验证中发现并修复的 26.1 缺陷：① `VertexFormat` 用 vanilla `NORMAL`（byte×3）导致顶点体积
  23B 非 4 对齐 → 换 FLOAT×3 自定义元素（32B）；② `RenderPass.draw` 需要先
  `setVertexBuffer(0, …)`，否则 draw 时顶点缓冲为 null。

# 基岩版复刻对比方法

## 用途

本文件定义以真实 Bedrock addon 为 oracle、验证 eyelib 渲染复刻的可重复方法。它记录稳定的判定规则与证据顺序，不记录某次实体对比的提交历史或截图清单。

## Oracle 优先级

1. Mojang Creator 文档
2. 实际 `.mcpack` / `.brarchive` 数据
3. Bedrock Wiki
4. 项目内部文档

内部文档只能解释已经观察到的实现约束；当它与前三项冲突时，回到外部规范或真实数据重新验证。

## 对比装置

- JE 与 BE 使用相同坐标、相同实体朝向、相同光照和相同时间；JE FOV 设为 60。
- 关闭生物生成、昼夜循环和天气；为亡灵提供遮阳且均匀的摄影棚背景。
- BE 的实体可能由随机数抽取变体，不能把单个实例的姿态或贴图当作确定答案；变体系统应使用一排实体观察分布。
- 对比状态依赖动画时，使用单个受控实体，隔离互射、死亡和环境因素；群系、实体类型和装备必须在两侧对齐。

## 证据顺序

视觉差异按以下顺序排查，禁止先猜材质或混合因子：

1. 模型是否加载，模型部件是否可见。
2. 顶点是否提交到 buffer。
3. 顶点位置、UV、颜色是否与几何数据一致。
4. 片元是否产生，深度测试是否遮挡。
5. 着色器 alpha discard 阈值是否丢弃有效像素。
6. 混合因子、光照和覆盖色是否改变最终颜色。

RenderDoc 用于 GPU 顶点和像素历史；`mcmcp_execute`/`/eval` 用于查询 `RenderData`、Molang scope、模型组件和动画状态。探针应读取实际运行实例，多个客户端并存时先确认调试端口没有被旧进程占用。

## 已确认的 Bedrock 语义

### RenderController 纹理层

`RenderController.textures` 数组表达按顺序绑定的纹理层，不等于每个材质都自动合成所有层。只有支持多采样/掩码的材质族才合成多层；单采样材质只读取第 0 层。实现和测试必须同时检查 RC 的层数组与最终材质能力。

### 发光、覆盖色和渲染排序

- cutout 应在半透明、加法和 emissive pass 之前完成深度写入；按 RC 列表顺序渲染可能使身体 pass 遮住发光 pass。
- emissive 纹理的 alpha 是发光掩码，不能套用 MC entityTranslucent 的高 alpha discard 阈值；低 alpha 输入需要在对应路径 clamp。
- additive emissive 使用声明的 `(SourceAlpha, One)` 语义；不能用 `(One, One)` 绕过 alpha 闸门，否则透明区域的 RGB 会被加到画面上。
- `overlay_color` 是受伤/特殊状态覆盖层，需配合 overlay alpha；不能作为常驻顶点染色。

详细的 alpha 双路径、材质路由和逐 pass 求值规则分别见 [低 alpha 双路径](alpha-clamp-two-path.md)、[材质渲染链](material-rendering-chain.md) 和 [逐材质 pass 求值](texture-material-per-pass-evaluation.md)。

### 姿态与 attachable

收集的骨骼姿态不包含 pivot 平移。附着物或 locator 在使用骨骼姿态时必须额外应用 pivot 平移，否则会落到实体原点。实体级 `RenderData` 与 attachable 自身的 `RenderData` 作用域也必须区分：父级 setup 修改父实体作用域，attachable 动画读取自身作用域。

### texture_mesh 与 Blockbench

当 Bedrock `texture_meshes` 的坐标或 UV 不明确时，使用 Blockbench Bedrock codec 加载同一 JSON 读取内部坐标作为 oracle。当前已验证的坐标约定为 `position=(-x,-y,z)`、`rotation=(-rx,-ry,rz)`、`local_pivot=(x,y,-z)`，scale 不变；图像右侧对应 `-x`，图像下方对应 `+z`，厚度沿 `-y`。

## 运行时排错约束

- 客户端 `player.teleportTo` 可能被 integrated server 回弹；需要服务端命令传送后再采样。
- 宽体实体放置时保留墙距；蜘蛛等实体贴墙会触发 suffocation，不能把死亡误判成渲染消失。
- 查询链中未实现 query 返回 `MolangNull` 而非数值零；变体错误应沿表达式逐项检查 null 传播。
- `mcmcp_execute` 的结果必须配合实体 ID、版本和采样时刻记录；不要把过期客户端的探针结果归因于当前源码。

## 相关入口

- [实体渲染验证工作流](entity-verification-workflow.md)
- [Molang 作用域调试](../molang/molang-scope-eval-debugging.md)
- [RenderDoc 调试 Skill](../../.opencode/skills/eyelib-renderdoc/SKILL.md)
- [Bedrock 复刻 Skill](../../.opencode/skills/bedrock-parity/SKILL.md)

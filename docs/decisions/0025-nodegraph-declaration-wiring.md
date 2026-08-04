# ADR-0025：声明连线语义 + 闭包导入 + 诊断中心

状态：已接受（2026-08-04）
规格：docs/specs/nodegraph-declaration-wiring.md

## 背景

1. 声明表 ref.* 历史上不连线、组装器全局扫描收集：画布上看不出 ref 归属，用户反馈
   「导入悦灵后 ref.animation、ref.geometry 没有连接到该连的地方」。
2. UI 导入 ClientEntity 不带入其引用的 RenderController/AnimationController，
   用户「没看到 RenderController」。
3. 诊断刷聊天栏，悦灵级导入数百条，噪声不可回溯。

## 决策

### D1 声明 = 连线

entity.root 新增 5 个 multi 声明输入端口（geometries/textures/materials/animations/
animation_controllers，类型与 ref.* 输出一一对应）。组装器、验证器（REF_CONFLICT）、
KnownRefTables 统一只认连线；扫描语义废止。一次性迁移 v2→v3 为旧图补线
（含裸 ref.rc 补 rc.condition_entry、子图游离 ref 移主图），迁移前后构建产物等价（有测试）。

备选「连线仅作可视化、语义仍扫描」被否：双语义必然漂移，违反单一惯例原则。
代价：子图中的 ref 无法再声明（迁移移动无连线的；有连线的报 REF_NOT_CONNECTED 引导）——
声明本就属于装配根，代价可接受。

### D2 闭包导入

UI 导入 ClientEntity 时跟随导入主图 ref.rc / ref.ac 引用的 RC/AC 文档为独立图库。
RC 文档源：注册表优先（EntityJsonService 编码回文件形态——覆盖 vanilla/BedrockAddonLoader
来源，与运行时实体所见一致），资源目录扫描兜底；AC 注册表不留存，只能扫资源目录。
找不到记 CLOSURE_MISS warning，不阻断。

教训：初版只扫 `eyelib/render_controllers` 资源目录，vanilla RC 全部 miss——
vanilla 资产不走 BrResourcesLoader 管线，注册表才是运行时真相。

### D3 诊断中心 + 浮动面板

聊天栏不再出现任何 error/warning 诊断（成功类一句确认保留）。DiagnosticsCenter
进程内单例只留最新一批（IDEA Problems 语义），按文档分节；两版编辑器左下角
「问题」开关 + E/W 徽标 + 浮动列表，行带 nodeUid 点击选中并居中节点。

## 后果

- 图语义更硬：声明关系显式可见、可验证（REF_NOT_CONNECTED）、可迁移。
- 导入一步到位：实体 + RC/AC 闭包，悦灵实测 6 RC 自动跟随。
- format_version=3；加载路径（资源包 loader / EprojectIo）统一链式迁移。
- 诊断可回溯、可定位节点；聊天栏回归社交用途。

## 验证

- domain 201 测试绿（新增声明连线组装/REF_NOT_CONNECTED×7/迁移 v3×6 等）。
- 实机两版：悦灵闭包导入（6 RC 0 miss）→ 61 声明 ref 全接线、验证器 0 诊断 →
  7 库构建注入 → 渲染与 pack 一致；面板开关/徽标/分节/行点击聚焦（1.20.1 端到端
  selected=[geo0]）；断线场景 REF_NOT_CONNECTED 正确报出并定位。

# ADR-0028：ldlib1 退役，节点图编辑器全版本统一 LDLib2

## 状态

已接受（2026-08-07，用户决策）。修订 ADR-0021 的「1.20.1 走 LDLib 1.x
graphprocessor」路线；规格文档中 ldlib1 接线描述自此成为历史记录。

## 背景

ADR-0021 时 LDLib2 没有 1.20.1 Forge 版本，节点图编辑器被迫双实现：
1.20.1 走 LDLib 1.x（`editor/ldlib1` + `workbench/ldlib1`），1.21.1/26.1.2 走
LDLib2 nodegraphtookit。双实现的代价已实证：每个编辑器功能要两端各做一遍
（规格中 ldlib1/ldlib2 路径并列），ldlib1 端还持续消耗适配修补
（ColorConfigurator 弹窗、TextFieldWidget 等 LDLib 1.x 固有问题）。

2026-08-06 起局面变化：weiliangyan 的 LDLib2-1.20.1-Forge 移植版出现，
其 release r8 在 1.20.1 boot 即 NoSuchMethodError（48 处 ResourceLocation 1.21 API
泄漏），经 TT432 fork 源码级修复（提交 26b6500）后在 1.20.1 实机跑通完整编辑器
（打开/渲染/保存/Ctrl+S）。

## 决策

1. **删除 ldlib1 编辑器实现**：`client/nodegraph/editor/ldlib1/`（17 文件）、
   `client/nodegraph/workbench/ldlib1/`（11 文件）及对应测试，共 29 文件。
2. **全版本统一 LDLib2**：`NodegraphGate` 不再按版本分流，恒路由
   `Ldlib2NodegraphEditor`；`LdlibCompat` 全版本检测 `ldlib2` modId。
3. **依赖切换**：1.20.1 移除 `curse.maven:ldlib-626676:7652228`（LDLib 1.0.49），
   仅保留 fork 坐标 `com.github.tt432:ldlib2-forge-1.20.1:2.2.27+forge.1.20.1-fork.1`
   （mavenLocal；上游修复合入前以此为来源）；mods.toml 声明改 `ldlib2 [2.2.27,)`
   （该可选依赖声明后被 ADR-0030 删除——LDLib2 改 jarJar 内嵌发布）。
4. **双编辑器交付规则废止**：AGENTS.md「节点图功能默认 ldlib1+ldlib2 双端交付」
   同步删除——编辑器适配层只剩一端，新功能只需在 LDLib2 适配层实现。

## 后果

- 编辑器新功能单端实现，消除双端对齐负担与「漏做一端」整类错误。
- 1.20.1 编辑器能力对齐 1.21.1（nodegraphtookit 画布、内建黑板/检查器/小地图），
  失去 ldlib1 特有交互（graphprocessor 画布手感），用户无感知迁移成本
  （图文档 domain 模型不变，存档兼容）。
- 1.20.1 的 LDLib2 依赖 fork 构建产物（mavenLocal 手动安装）；上游
  （weiliangyan 或 lowdragmc）合入 1.20.1 支持后应切回正式坐标。
- 共享层（`preview`、`AssetSuggestions`、`DiagnosticsCenter`、workbench 服务）
  不受影响——ldlib2 适配层原本就在用。
- 规格文档（nodegraph-visual-molang / nodegraph-workbench /
  nodegraph-color-and-inline-literals / nodegraph-eproject-variables /
  json-view-molang-debug）中 ldlib1 接线章节保留为历史，头部已加退役标注。

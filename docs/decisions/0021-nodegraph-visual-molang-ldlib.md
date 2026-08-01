# ADR-0021: 节点图可视化 Molang 语言与 LDLib 可选前置

**Status:** Accepted
**Date:** 2026-08-02
**Author:** @TT432
**Related:** 遵循 [ADR-0016](0016-bridge-extraction-standard.md)（ACL 职责边界）、扩展 [ADR-0002](0002-module-boundaries.md)（新增 `eyelib.nodegraph` 模块）、遵循 [ADR-0015](0015-stonecutter-multi-version.md)（`//?` 版本条件注释栖息地规则 + allowlist 扩展）、设计规格 [docs/specs/nodegraph-visual-molang.md](../specs/nodegraph-visual-molang.md)

## Context

 eyelib 的 Bedrock 资源（ClientEntity / RenderController / AnimationController）手写 JSON 门槛高：molang 表达式散在十几个字段里，geometry/texture/material 短名表与 RC molang 引用必须手工保持一致。需要一个**可视化作者工具**：UE 蓝图风格的节点图编辑器，生成 molang 并组装 ClientEntity。

技术前提：LDLib 已提供两套节点图工具包——1.20.1 的 LDLib 1.x `graphprocessor`（NodeGraphProcessor 移植）与 1.21.1+/26.1 的 LDLib2 `nodegraphtookit`（Unity GraphToolkit 移植，含子图/分组/黑板/undo）。两者模型与序列化格式完全不兼容。

### 核心矛盾

1. **两套不兼容的 LDLib 图 API**：若以任一 LDLib 的图模型/序列化为权威格式，跨版本不可移植，且运行时（组装 ClientEntity 并渲染）被迫依赖可选前置。
2. **可选前置 vs 运行时可用**：要求「装了 ldlib 才启用编辑器」，但没装时已经做好的图仍应能产出 ClientEntity。
3. **架构约束**：domain 零 MC import（ADR-0016）、`//?` 注释栖息地受 ArchUnit 限制（ADR-0015 §4）、版本特定 loader API 只能住 bridge。
4. **语言设计**：直接做「molang AST 的节点化」会把 AST 的噪声（优先级、语句 vs 表达式）暴露给用户；UE 蓝图的 Exec/Data 双流更贴合 molang 的 ExprSet 语义。

## Decision

### D1 权威图模型在 domain，LDLib 仅作编辑器前端

新增 domain 模块 `io.github.tt432.eyelib.nodegraph`（零 MC / 零 LDLib import，ArchUnit 约束内）：图文档 JSON Schema（`GraphLibrary` = 1 主图 + N 命名子图）、节点类型目录（`NodeTypes`）、验证器、图→molang 代码生成器、图→Bedrock JSON 组装器。**编辑器只是图文档的视图**（打开时翻译为 LDLib 图模型，保存时翻译回文档）；运行时（`client.nodegraph.NodegraphBuildService`：验证→代码生成→组装→现有 CODEC 往返→注入各 Manager）完全不依赖 LDLib。

### D2 语言语义：ExprSet 统一槽模型

EVM（Eyelib Visual Molang）不是 molang AST 包装：端口类型（exec/float/bool/string/array/any/SLOT/6 种资源引用）、值节点数据流（DAG）、exec 链（反向汇入槽、禁 fan-out）、exec 槽与表达式槽统一产出 molang ExprSet 字符串。共享子表达式提取 `temp.gN`、全括号化、别名规范形（只用 query./variable./temp./math. 全名）。query/math 节点目录运行时从 `MolangMappingTree` 枚举（不硬编码 316 个函数）。

### D3 子图与分组在文档层实现

子图（表达式子图：命名输入 + 单输出；内联展开 + `temp.sg<K>_` 名称隔离 + 深度上限 32）与分组（画布 placemat、黑板变量组）均为文档层概念，不依赖 LDLib 原生能力。LDLib2 映射到 local subgraph/PlacematModel/GroupModel；LDLib 1.x 降级（潜入=切换显示的图，placemat 仅持久化）。

### D4 LDLib 适配层 = 第三方库 ACL，允许 `//?` 整文件包裹

`client.nodegraph.editor.ldlib1`（1.20.1，graphprocessor）与 `client.nodegraph.editor.ldlib2`（1.21.1/26.1.2，nodegraphtookit）是 LDLib 的 ACL 翻译层，性质等同 bridge 之于 MC。因 1.20.1 无 LDLib2、1.21.1+ 无 LDLib 1.x，两包必须**整文件 `//?` 包裹做单版本编译隔离**——`StonecutterCommentPlacementTest.ALLOWED_PACKAGE_ROOTS` 显式加入 `client/nodegraph/`（与 debug/smoke 的 client tooling 同性质）。

### D5 版本特定 loader API 仍只住 bridge

可选前置检测（ModList/LoadingModList）不走 application：`bridge.client.compat.ldlib.LdlibCompat`（与 ARCompat 同模式）返回布尔；`client.nodegraph.NodegraphGate` 只持有编辑器实现类的**类名字符串**，确认前置存在后反射加载——未装 LDLib 时主链路零 LDLib 类加载。blaze3d 屏幕尺寸经 `bridge.ui.UiPort` 暴露。

### D6 构建集成

| 节点 | 坐标 | 仓库 | 运行时 |
|---|---|---|---|
| 1.20.1 | `curse.maven:ldlib-626676:7652228` | CurseMaven | `modLdlibCompile`+`modLocalRuntime`（SRG→mojmap remap） |
| 1.21.1 | `com.lowdragmc.ldlib2:ldlib2-neoforge-1.21.1:2.2.32:all` | firstdark snapshots | compileOnly + copy 至 run/mods（`ldlib2Jar` **transitive=false**，ldlib2 pom 的传递依赖 fastutil/kotlin/taffy/yoga 非合法 mod 文件，会触发 LoadingErrorScreen；neoforge 提升至 21.1.248 ≥ ldlib2 要求的 21.1.216） |
| 26.1.2 | `com.lowdragmc.ldlib2:ldlib2-neoforge-26.1:26.1.2.27.a` | 同上 | compileOnly + localRuntime（MDG 2.x 自动注册） |

mods.toml / neoforge.mods.toml 声明 optional 依赖（ordering=AFTER, side=CLIENT）。不 jarJar 嵌入产物。

### D7 组装走 JSON 往返

组装器只产 Gson JSON 树（domain 不依赖 importer）；client 层用现有 `BrClientEntity.CODEC` / `RenderControllers.CODEC` / `BrAnimationControllerSet.CODEC` 解析注入。免费获得 schema 校验；产物 JSON 同时是导出落盘形态。

## Consequences

### 正面

- 图文档跨版本一致、可 code review、可离线单测（71 个 domain 单测零 MC 运行）。
- 未装 LDLib 时图仍可构建渲染；装了三版本均可编辑。
- 验证/代码生成/组装的正确性独立于编辑器演化。
- query 节点目录自动跟随 eyelib/第三方注册的 molang 函数。

### 代价与风险

- 两处编辑器适配层的翻译代码是双份维护面（LDLib 1.x 与 LDLib2 API 差异大）——接受，以 domain 契约为单一事实源。
- LDLib2 为快照版 API，可能漂移——锁定 2.2.32 / 26.1.2.27.a，适配层集中。
- 1.20.1 graphprocessor 无 undo/子图/分组——文档层补齐，画布分组在 1.20.1 不可见（降级，不报错）。
- 1.21.1 节点 neoforge 从 21.1.115 升至 21.1.248——同 minor 线，风险低。
- 26.1.2 模型预览因 `ModelPreviewScreen` 渲染路径未迁移（既有债务）降级为纹理预览 + 文本。

### 验证

- domain 单测 71 个（验证器 37 + 代码生成 18 + 组装器 10 + 其余 6），1.20.1/1.21.1 gradle test 全绿（26.1.2 测试源集被既有 NativeImageIOTest 预存失败阻塞，见 feedback）。
- 1.20.1 / 1.21.1 dev client 内：编辑器打开（截图）、图文档 → 构建 → 注入 → 实体挂载 → Bedrock 模型渲染（截图）。

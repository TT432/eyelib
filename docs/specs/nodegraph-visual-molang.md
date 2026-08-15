# 节点图可视化 Molang 语言与 ClientEntity 制作器 — 设计规格

> 状态：Draft v1（2026-08-01）
> ⚠ ldlib1 已退役（2026-08-07，ADR-0028）：本文 ldlib1 接线描述为历史记录，当前全版本统一 LDLib2。
> 前置调研：LDLib 1.20.1 (graphprocessor) / LDLib2 (nodegraphtookit) API、eyelib 消费端（BrClientEntity/RC/AC/MolangValue）
> 关联：ADR（待补）、`docs/concepts/architecture.md`、`docs/decisions/0002-module-boundaries.md`

## 0. 目标与非目标

### 目标
1. 以 **LDLib 为可选前置**，提供 UE 蓝图风格的节点图编辑器，用于编写生成 Molang 的**图形语言**（非 Molang AST 的忠实复制，是独立设计的领域语言）。
2. 支持**子图**（可复用、可潜入编辑）与**分组**（画布注释框 + 黑板变量分组）。
3. 模型、纹理等资源引用节点支持**预览**。
4. 最终产物是**可用的 ClientEntity**（注入运行时管理器后可被现有渲染消费链完整渲染）。
5. 三个版本节点全部支持：1.20.1 (LDLib 1.x graphprocessor)、1.21.1 / 26.1.2 (LDLib2 nodegraphtookit)。

### 非目标
- 不做通用图灵完备可视化编程（无函数定义/递归）；语言只覆盖 Molang 表达能力。
- 不替代 JSON 手写管线；图文档是 ClientEntity 的**另一种作者源**。
- 不在服务端执行图（ClientEntity 是客户端概念）。

## 1. 核心架构决策

### D1. 权威图模型在 eyelib domain，LDLib 仅作编辑器前端

```
┌─ io.github.tt432.eyelib.nodegraph（新 domain 模块，零 MC / 零 LDLib import，ArchUnit 约束）─┐
│  图文档 Schema（JSON，版本无关）  节点类型系统  类型检查  诊断                            │
│  图 → Molang 代码生成            图 → BrClientEntity 组装                                 │
└──────────────────────────────────────────────────────────────────────────────────────────┘
        ▲ 纯 Java 调用                            ▲ 纯 Java 调用
┌───────┴─────────────┐                ┌─────────┴───────────────────────┐
│ 运行时加载管线        │                │ 编辑器（装了 LDLib 才启用）         │
│ client.nodegraph     │                │ client.nodegraph.editor          │
│ 读图 JSON → 组装      │                │  ├ ldlib1 适配（1.20.1）          │
│ → ClientEntityManager│                │  └ ldlib2 适配（1.21.1/26.1.2）   │
└─────────────────────┘                └─────────────────────────────────┘
```

**推论**：未安装 LDLib 时，已创作的图文档仍可加载并产出 ClientEntity（运行时路径无 LDLib 依赖）；只有「编辑」能力缺失。这严格满足「装了 ldlib 时才启用」且不让 LDLib 渗入运行时。

**理由**：LDLib 1.x graphprocessor 与 LDLib2 nodegraphtookit 的模型/序列化格式完全不兼容；若以其一为权威格式，跨版本不可移植。自有 JSON Schema 是跨版本、可 code review、可单元测试（零 MC）的唯一选择。

### D2. 编辑器 = 翻译层，不做权威存储

LDLib 编辑器（GraphView/GraphViewWidget）只是图文档的**视图与编辑前端**：打开时 `图文档 → LDLib 图模型`；编辑动作实时或保存时 `LDLib 图模型 → 图文档`。LDLib 自带的 NBT 序列化仅服务于其 undo/dirty 会话状态，不落盘为权威格式。

### D3. 语言是「生成 Molang 的图语言」，不是 Molang AST 包装

设计单位为**领域节点**（查询、变量、数学、资源引用、实体字段插槽），而非逐节点的 Molang 语法树。代码生成器负责：数据流 → 表达式树、共享子表达式提取 `temp`、语句序列化、别名规范化（走 `MolangRootAliasCanonicalizer` 同等规则：全名输出，不用 q/t/v/c 缩写，输出即规范形）。

### D4. query/math 节点目录来自运行时注册表

节点目录不硬编码 316 个 query 函数：编辑器在运行时从 `MolangMappingTree`（@MolangMapping 注解扫描产物）枚举 `query.*` / `math.*` 可用函数及其签名（名称、参数 arity、host roles），动态生成节点类型。好处：与 eyelib 实际注册的查询自动同步；第三方 mod 注册的 query 自动出现。domain 层只定义「Query 调用节点」这一个通用节点类型（参数：函数名 + 实参端口形状），目录是编辑器关注点。

### D5. 子图在权威层实现，不依赖 LDLib 原生子图

LDLib2 有 SubgraphNodeModel，LDLib 1.x graphprocessor 没有子图。为保证跨版本语义一致，子图是**图文档层概念**：

- 图文档库（GraphLibrary）= 一个 JSON 文件内含 1 个主图 + N 个命名子图。
- 子图种类：**表达式子图**（命名输入端口若干 + 单一输出值）与**执行子图**（exec in/out + 命名输入/输出端口）。
- 子图节点（SubgraphCall）在主图中引用子图名；代码生成时**内联展开**（参数替换 + 名称隔离 `temp.sg<uid>_<local>`）。
- 编辑器「潜入」= 切换当前编辑的图为该子图（面包屑导航），两版 LDLib 均可实现（LDLib2 用 GraphEditorView breadcrumb 原生机制或自绘；1.20.1 用自绘面包屑 + GraphViewWidget.loadGraph 切换）。

### D6. 分组

- **画布分组**（Placemat）：标题 + 颜色 + 成员节点 uid 集合 + 包围盒（随成员自动扩展）。文档层持久化；LDLib2 映射到 PlacematModel（原生），1.20.1 用自绘覆盖层（只读视觉，不支持则降级为仅文档数据，不报错）。
- **黑板变量分组**：变量声明带可选 group 路径（`a/b/c`），编辑器黑板按树展示。LDLib2 原生支持（GroupModel）；1.20.1 的 ParameterPanelWidget 无分组 → 变量名平铺展示全路径，语义不变。

### D7. 可选前置隔离

- 所有 `import com.lowdragmc.*` 只出现在 `client.nodegraph.editor.ldlib1|ldlib2` 包及其直接入口工厂。
- 入口门：`ModList.get().isLoaded("ldlib2")`（1.21.1/26.1.2）/ `isLoaded("ldlib")`（1.20.1）；门后通过**隔离类加载**（专用 holder 类，仅在门内反射/ServiceLoader 触达）创建编辑器，主链路类不得静态引用 LDLib 类型。
- mods.toml / neoforge.mods.toml 声明 `type="optional"` 依赖，保证装了 LDLib 时加载顺序正确。
- 构建：`compileOnly` + dev runtime（ModDevGradle `localRuntime`/`additionalRuntimeClasspath`）引入，不 jarJar 嵌入产物。

## 2. 图语言设计（EVM — Eyelib Visual Molang）

### 2.1 端口类型系统

| 类型 | 说明 | 兼容规则 |
|---|---|---|
| `exec` | 执行流（仅语句上下文） | 仅 exec↔exec；1 输入多输出（fan-out 按连线顺序?否——见 2.4） |
| `float` | 数值（Molang 主类型） | bool→float 隐式；float→bool 隐式（非 0 即真） |
| `bool` | 布尔（语义标注，物即 float 0/1） | 见上 |
| `string` | 字符串 | string↔string；资源引用节点产出 string 子类型 |
| `array` | Molang 数组（仅 query 返回值可产出） | 仅 for_each 输入/索引消费 |
| `object` | struct 对象（2026-08-14 起，见 nodegraph-object-and-typed-ports.md） | 仅接 object/any/unknown；与 number/string/array 隔离 |
| `any` | 未推导（未连接端口默认值推导用） | 与任何类型兼容（诊断降级为 warning） |

资源引用类型（`geometry_ref` / `texture_ref` / `material_ref` / `animation_ref` / `ac_ref` / `rc_ref`）是 string 的语义子类型：只能由对应引用节点产出，只能接入对应插槽，保证 ClientEntity 组装时信息完备（引用集合 = 实体的 geometry/texture/material/animation 声明表来源）。

### 2.2 节点目录（domain 层节点类型注册表）

**常量**：NumberConst(float)、BoolConst、StringConst。
**变量**：VariableGet / VariableSet（黑板变量，variable.* 根）、TempGet / TempSet（temp.* 根，不进黑板，图内自由命名）。
**查询**：QueryCall（函数名 + 动态实参端口；成员访问形 `query.foo` 与调用形 `query.foo(a,b)` 统一）、MathCall（math.* 同构）。
**运算**：BinaryOp(+ - * / % == != < <= > >= && ||)、UnaryOp(- !)、Ternary(cond ? a : b)、NullCoalesce(a ?? b)。
**执行流**（exec pins）：Sequence（顺序执行 n 路）、SetVar（exec 版赋值）、Loop(count, body exec, break/continue 口)、ForEach(array, 迭代变量, body)、Break / Continue、Return(value)（表达式上下文的显式返回，可省——隐式以最后语句为返回值）。
**资源引用**：GeometryRef / TextureRef / MaterialRef / AnimationRef / AnimationControllerRef / RenderControllerRef（产出对应 ref 类型；节点体含预览与标识符字段；短名默认由标识符派生、不显式管理——见 nodegraph-shortname-elimination.md / ADR-0023）。
**实体装配**（ClientEntity 文档专用）：EntityRoot（文档根，见 §2.5）、RenderControllerSlot、AnimationControllerSlot、AnimateEntry。
**子图**：SubgraphCall（引用库内子图）、SubgraphInput / SubgraphOutput（子图定义侧端口锚点）。
**注释**：StickyNote（便签，纯文档）。

### 2.3 表达式上下文与执行上下文

每个「求值槽」声明其上下文种类：
- **表达式槽**（scale、transition 条件、geometry/texture/material 选择式、blend weight、RC color）：图必须归约为单值；生成单个表达式字符串。
- **执行时机**（v13 起为源模型，规格 nodegraph-event-nodes / ADR-0029）：initialize、pre_animation、parent_setup 是 event.* 事件节点（EXEC OUT 源）；on_entry/on_exit 是 ac.state 的 EXEC OUT 端口。exec 链从时机源出发；生成 `stmt; stmt; …` 序列；尾表达式作为返回值（Molang ExprSet 语义）。

### 2.4 代码生成规则（不变量）

1. **DAG**：数据流端口连接必须无环；环 = 编译错误（诊断指向成环节点）。
2. **确定性**：同一图文档 → 同一输出字符串（节点按拓扑序、同层按 uid 字典序；temp 编号递增）。
3. **共享子表达式**：出度 ≥ 2 的非平凡表达式节点 → 提取 `temp.gN = <expr>;`，引用处替换为 `temp.gN`。常量/单标识符不提取。
4. **优先级安全**：生成表达式按 Molang 13 级优先级插入括号——宁可多余括号，不可改变语义（输出全括号化的中缀，再经最小括号化 pass？否：v1 全括号化，保证正确性优先）。
5. **别名规范形**：输出只用 `query./variable./temp./context./math.` 全名。
6. **exec fan-out 禁止**：exec 输出端口最多 1 条出边（UE 蓝图 Sequence 节点解决分叉），违反 = 错误。
7. **未连接输入**：有默认值的用默认值；无默认值 = 编译错误（除非 any 类型槽且槽位允许缺省）。
8. **资源引用完备性**：EntityRoot 装配时，所有 ref 节点的有效短名/标识符对必须两两不冲突（同有效短名不同标识符 = 错误；有效短名 = 显式覆盖 ?: 标识符派生，ADR-0023）。
9. **子图展开深度**：子图引用链必须无环（递归子图 = 错误）；展开深度上限 32（防御性）。

### 2.5 ClientEntity 文档与 EntityRoot

ClientEntity 文档 = 一个主图，含唯一 EntityRoot 节点。EntityRoot 的插槽（对应 `BrClientEntity` 字段，标注上下文）：

| 插槽 | 字段 | 上下文 | 备注 |
|---|---|---|---|
| identifier | identifier | 配置字符串 | 非图 |
| initialize | scripts.initialize | event.initialize 节点 | v13 事件节点（ADR-0029） |
| pre_animation | scripts.pre_animation | event.pre_animation 节点 | 同上 |
| parent_setup | scripts.parent_setup | event.parent_setup 节点 | 同上 |
| scale / scaleX / scaleY / scaleZ | scripts.scale* | 表达式 | |
| animate[] | scripts.animate | 表达式(blend weight) + AnimationRef/ACRef | 动态条目 |
| render_controllers[] | render_controllers + renderControllerConditions | RCRef + 条件表达式 | |
| geometry[] | geometry 声明表 | GeometryRef | 有效短名→标识符（短名派生/default 别名见 ADR-0023） |
| textures[] | textures 声明表 | TextureRef | 有效短名→路径 |
| materials[] | materials 声明表 | MaterialRef | 有效短名→材质名 |
| animations[] | animations 声明表 | AnimationRef/ACRef | 有效短名→标识符（ref.ac 同发本表，ADR-0023 D5） |

**RC/AC 文档**（独立图文档类型，同样可制作）：
- RC 文档：geometry(表达式)、textures[](表达式)、materials[](pattern+表达式)、part_visibility[](骨骼 pattern+条件表达式)、color/is_hurt/on_fire/overlay(表达式组)、ignoreLighting、arrays。
- AC 文档：状态集合（每状态：on_entry/on_exit EXEC OUT 时机源端口（v13，ADR-0029）、animations 条目+blend 表达式、transitions 条目+条件表达式、blend_transition、blend_via_shortest_path）、initial_state。

### 2.6 组装输出（Build 动作）

`图文档库 → 验证 → 代码生成 → JSON → BrClientEntity.CODEC 解析 → 运行时注入`。

刻意走 **JSON 往返**（生成 Bedrock 格式 JSON 文本再经现有 CODEC 解析）：免费获得 schema 校验与序列化兼容性证明；产物 JSON 同时是「导出到资源包」的落盘形态。

注入点（复用现有管线，不新造）：
- ClientEntity：`ClientEntityManager.replaceAll/put`
- RC：`RenderControllerManager.put`
- AC：`AnimationAssetRegistry.stageControllers`（经 BrAnimationControllers.fromSchemaSet）
- 运行时预览：`EntityRenderOrchestrator.setupClientEntity(ce, cap)` 等价路径挂载到目标实体

## 3. 编辑器设计

### 3.1 入口与宿主

- 打开方式：客户端命令（如 `/eyelib nodegraph`）+ 管理器屏幕（EyelibManagerScreen）入口按钮。门控：无 LDLib 时入口不可见/提示。
- 1.21.1/26.1.2：`GraphEditorView`（LDLib2 编辑器框架 View，含保存/脏标记/面包屑/黑板/检查器/小地图）宿主于 LDLib2 ModularUI 屏。
- 1.20.1：`GraphViewWidget`（画布 + NodePanelWidget 节点面板 + ParameterPanelWidget 变量面板）宿主于 LDLib 1.x ModularUI 屏；保存/脏标记/子图潜入由 eyelib 侧补齐。

### 3.2 文档库与落盘

- 图文档库存放：`config/eyelib/nodegraph/<name>.json`（开发期可热重载）。
- 资源包管线：图文档也可放资源包 `eyelib/nodegraph/*.json` 随包分发（运行时加载器，SimpleJsonResourceReloadListener 风格，与 BrClientEntityLoader 并列）。

### 3.3 预览

- **纹理预览**：节点内嵌 64×64 区域，经 MC 纹理管理器加载 blit（棋盘格底衬透明）。
- **模型预览**：复用 `debug.client.gui.ModelPreviewScreen` 的离屏渲染路径，把 Bedrock 模型渲染进节点预览区（或悬浮预览窗）。
- **实体预览**：Build 后一键 spawn/挂载测试实体到展示场地（debug 模块已有生物展示场地）。

### 3.4 诊断

图验证/代码生成诊断（错误/警告，含节点 uid 定位）在编辑器图日志区（LDLib2 GraphLogger 机制 / 1.20.1 DebugPanelWidget）与聊天栏双通道展示。

## 4. 构建与依赖（事实已验证）

| 版本节点 | 坐标 | 仓库 | modId | 状态 |
|---|---|---|---|---|
| 1.20.1 | `curse.maven:ldlib-626676:7652228`（1.0.49-forge） | cursemaven（已有） | `ldlib` | 已验证 200 + graphprocessor 在内 |
| 1.21.1 | `com.lowdragmc.ldlib2:ldlib2-neoforge-1.21.1:2.2.32:all` | `https://maven.firstdark.dev/snapshots`（新增） | `ldlib2` | 已验证 200 |
| 26.1.2 | `com.lowdragmc.ldlib2:ldlib2-neoforge-26.1:26.1.2.27.a`（无 classifier） | 同上 | `ldlib2` | 已验证 200；toolkit API 与 2.2.x 文件级近乎一致 |

LDLib 1.20.1 为 SRG 名（Forge 1.20.1 reobf 生态），ModDevGradle legacyforge 需经 `localRuntime` remap（同 spark 先例）。LDLib2 为 mojmap。

## 5. 子任务规格

### T1 构建集成
- **前置**：firstdark 仓库可达；cursemaven 已有。
- **后置**：三节点 `compileJava` 通过（`compileOnly` 引入）；dev client 运行时 mods 列表出现 ldlib/ldlib2；mods.toml 含 optional 依赖声明。
- **不变量**：不引入 LDLib 的传递依赖冲突；产物 jar 不嵌入 LDLib 类。
- **异常**：仓库不可达 → 构建失败信息含坐标与仓库 URL。
- **副作用**：`.gradle` 缓存新增制品。

### T2 图文档 Schema 与 Codec
- **前置**：无（domain 纯 Java）。
- **后置**：GraphLibrary/Graph/Node/Port/Wire/Variable/Group/StickyNote 模型 + JSON Codec 往返稳定（decode∘encode = id）；非法文档产生结构化诊断（路径定位）。
- **不变量**：Schema 版本字段 `format_version`；未知字段拒绝（strict）。
- **异常**：格式错误 → DataResult 错误，不抛未受检异常。
- **副作用**：无。

### T3 节点类型系统与验证
- **前置**：T2。
- **后置**：§2.2 节点目录注册表；类型兼容矩阵（§2.1）检查器；DAG 环检测；未连接/默认值规则；资源引用完备性检查；诊断列表含节点 uid。
- **不变量**：验证纯函数（同输入同输出）；不依赖 MC/LDLib。
- **异常**：未知节点类型/端口 → 错误诊断，不 NPE。

### T4 代码生成（图 → Molang）
- **前置**：T3。
- **后置**：表达式槽生成单表达式；执行槽生成语句序列；满足 §2.4 全部不变量；输出经 eyelib `MolangCompilerImpl` 编译通过（对接测试：生成字符串 parseExprSet 成功）。
- **不变量**：§2.4 确定性、括号安全、别名规范形、temp 提取。
- **异常**：验证失败时禁止产出半成品字符串（fail-fast，返回诊断）。

### T5 子图与分组
- **前置**：T4。
- **后置**：表达式/执行子图定义、调用、内联展开（名称隔离）；递归/深度检测；画布分组与变量分组持久化。
- **异常**：子图缺失引用 = 错误诊断。

### T6 ClientEntity/RC/AC 组装
- **前置**：T4。
- **后置**：EntityRoot → BrClientEntity JSON（经 CODEC 往返解析成功）；RC/AC 文档同构；引用表冲突检测（§2.4-8）。
- **异常**：缺 identifier 等必填 = 错误。

### T7 LDLib2 编辑器适配（1.21.1/26.1.2）
- **前置**：T1、T3（模型只读接口）、T5。
- **后置**：图文档 ↔ LDLib2 GraphModel 双向映射；GraphEditorView 打开编辑、保存回写文档；黑板/分组/便签/子图潜入可用；节点目录含 query/math 动态节点（D4）。
- **异常**：LDLib2 缺失 → 入口隐藏（T1 门）。

### T8 LDLib1 编辑器适配（1.20.1）
- **前置**：T1、T3、T5。
- **后置**：图文档 ↔ BaseGraph 双向映射；GraphViewWidget 打开编辑；保存/子图潜入/分组降级策略按 D5/D6。
- **异常**：同上。

### T9 预览与运行时注入
- **前置**：T6、T7 或 T8。
- **后置**：纹理/模型预览渲染；Build 动作注入各 Manager；测试实体挂载渲染可见。
- **异常**：引用缺失资源 → 诊断列表 + 聊天栏警告，不崩溃。

### T10 测试与验收
- **前置**：T2–T9。
- **后置**：domain 单测（T2/T3/T4/T5/T6 黄金样本 + 往返）；1.20.1 与 1.21.1 dev client 内编辑器打开、建图、Build、实体渲染烟雾验证（截图证据）。
- **不变量**：ArchUnit 对 `eyelib.nodegraph` 零 MC import 生效。

## 6. 风险与开放问题

| 风险 | 等级 | 缓解 |
|---|---|---|
| LDLib2 toolkit 为快照版 API，可能漂移 | 中 | 锁定 2.2.32 / 26.1.2.27.a；适配层集中 |
| 1.20.1 graphprocessor 无 undo/子图/分组 | 中 | eyelib 侧补齐（文档层 + 自绘），接受视觉降级 |
| 模型预览离屏渲染在 GUI 缩放下的正确性 | 中 | 复用 ModelPreviewScreen 既有路径 |
| LDLib 1.20.1 SRG remap 在 ModDevGradle legacyforge 下的可行性 | 中 | spark 已有同路径先例（build.gradle 注释） |
| Molang 表达式全括号化导致超长字符串 | 低 | 最小括号化 pass 后置优化（非 v1） |

## 7. 实施记录（2026-08-02）

已实现并验证（证据：domain 单测 71 个、三节点编译、1.20.1/1.21.1 客户端冒烟截图）：

- T1–T10 全部落地；验证器 19 项检查、代码生成（含 temp 提取与子图内联）、三汇编器、两版编辑器适配、纹理/模型预览基础设施、运行时构建管线与实体渲染闭环。
- 已知降级（均非本特性缺陷，见 work/feedback.json）：
  1. **26.1.2 世界内冒烟被预存渲染崩溃阻塞**（`Not building!`，无 ldlib2 亦复现）；编辑器代码与 1.21.1 全共享，编译与翻译单测覆盖。
  2. 1.20.1 画布分组按 D6 降级为仅文档持久化；1.21.1 dev runtime 需 neoforge ≥ 21.1.216（已升 21.1.248）。
  3. 26.1.2 模型预览按 §3.3 既定降级为纹理预览 + 文本（26.1 GUI 渲染路径未迁移）。

### 6.1 模型预览根因记录（fb_msaxt2mctep9，已修复）

调试初期模型预览在 1.20.1/1.21.1 不出图，一度误判为 entitySolid 着色器与 GUI 上下文不兼容。
实测（探针四边形 + 矩阵 dump + RenderDoc 截帧）证明顶点与矩阵均正确但零像素，
**根因是 `GuiGraphics.enableScissor` 被传入 LDLib 画布坐标**：画布 zoom/pan 只作用于 pose，
不作用于剪刀矩形（它把入参当屏幕坐标），导致整个绘制被剪空。修复：预览不做 scissor，
渲染改用与 `GuiGraphics.innerBlit` 同款的 Tesselator + position_tex 直接 drawWithShader 路径，
并加包围盒自动取景。ModelPreviewScreen 同病同药（其搜索赋值块被注释的预存 bug 一并修复）。

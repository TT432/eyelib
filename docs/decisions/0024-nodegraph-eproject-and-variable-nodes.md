# ADR-0024：eproject 容器、变量节点化与资产选择器

> 状态：已接受（2026-08-05）
> 规格：docs/specs/nodegraph-eproject-variables.md

## 背景

节点图编辑器已能导入/构建/调试完整实体闭包（ADR-0021/0022/0023），但：

1. 图库以单 JSON 散放在资源包或 config，一个实体的 7 库闭包无容器承载，保存语义缺失。
2. 变量靠 `var.get` 名选项节点引用——不是蓝图体验；变量与节点的绑定是字符串而非结构。
3. ref.* 标识符手敲，与运行时注册表脱节。

## 决策

### D1 eproject = OPC 布局的双形态容器

文件夹形态与 zip 形态**内部布局完全一致**（`[Content_Types].xml` / `_rels/.rels` /
`project.json` / `libraries/*.json`）。选 OPC 而非自定义格式：标准是现成的（ZIP +
内容类型 + 关系），工具生态可直接打开，且前向兼容（未知条目忽略）零成本。
**写回按来源形态**——用户怎么放的就怎么写回，不做形态转换。

### D2 一个项目 = 多图库

eproject 容纳 N 个 GraphLibrary（实体闭包：主实体 + RC/AC 库）。
管理器键 `{project}/{lib}` 与资源包键区分。项目内库自闭包；跨项目引用不做。

### D3 变量是节点，不是字符串

`variable` 节点 + `PortType.VARIABLE`：变量节点连线到值端口 = 隐式读；
连线到 `exec.set_var.target` = 写身份。理由：

- 「直接用就是访问」（用户原话）——读取零样板，不需要 var.get 中介。
- 写入必须显式接变量——set_var 的 target 是引脚而非名选项，写谁一目了然，
  重命名级联是结构改写而非字符串替换。
- VARIABLE 可接任意值端口（类型矩阵宽松），验证器做声明类型交叉检查（严格化在语义层）。

### D4 temp 与黑板变量分流

`exec.set_temp` 承接 temp 赋值（temp 是瞬态 codegen 实现细节，不进面板）；
黑板变量（variable.*）独占变量面板。`var.get`/`exec.set_var(root=*)` 旧形态经
`GraphMigrations` 一次性迁移（format_version 2），无兼容双轨。

### D5 资产候选是 domain 元数据 + client 供给

domain 只在 `NodeOptionDef` 挂 `suggestionKey` 字符串；候选列表由 client 运行时
从注册表供给（ModelManager / AnimationRegistries / RenderControllerManager /
MaterialManager / AddonTextureRegistry+资源扫描）。domain 保持零 MC 依赖；
下拉不锁死自由输入（外部契约逃生舱原则不变）。

### D6 编辑器能力按 LDLib 代际分流

ldlib2 复用内建 Blackboard（变量增删改 + 拖入画布 + Ctrl+S 命令链现成）；
ldlib1 自建变量面板（LDLib1 ParameterPanelWidget 只读，无增删）。
不做跨代 UI 抽象——两代编辑器的桥接成本低于抽象成本（ADR-0021 D2 同款判断）。

## 后果

- format_version 升到 2；旧图加载即迁移，不回写原文件。
- 变量重命名是图结构改写（variable 节点 name 同步），不再有悬空字符串引用。
- eproject 与资源包管线并存；config 下的项目可编辑，资源包库只读（保存另建项目）。

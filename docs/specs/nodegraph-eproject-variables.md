# eproject 项目容器 + 变量节点化 + 资产选择器

> 状态：实施中
> 前置：nodegraph-visual-molang.md、nodegraph-shortname-elimination.md、nodegraph-workbench.md
> ADR：0024

## 1. 目标

把节点图编辑器从「单库 JSON 文档」推进到「蓝图工程」：

1. **eproject 容器**：一个项目 = 一个实体的完整闭包（主实体 + 其 RC/AC 等多个图库），
   OPC 布局，文件夹/单文件双形态。
2. **变量节点化**：变量是一等公民。变量面板（UE My Blueprint 风）拖出的节点就是变量本身；
   直接连线 = 读取；`exec.set_var` 通过 target 引脚接变量节点 = 写入。
3. **资产选择器**：ref.geometry / ref.animation 等从运行时注册表下拉选择，不再手敲标识符。
4. **Ctrl+S 保存**。

## 2. eproject 格式

### 2.1 物理形态

| 形态 | 路径 | 说明 |
|---|---|---|
| 文件夹 | `$config/eyelib/{project_name}/` | 直接可读写的目录结构 |
| 单文件 | `$config/eyelib/{project_name}.eproject` | ZIP（OPC 布局） |

两种形态的内部布局**完全一致**（文件夹 = 解压态的包）：

```
[Content_Types].xml          OPC 内容类型声明（json/xml Default）
_rels/.rels                  OPC 包关系 → project.json
project.json                 项目元数据
libraries/<libId>.json       每个图库一份（GraphLibrary.CODEC 文档）
```

project.json：

```json
{
  "format": "eyelib-eproject",
  "version": 1,
  "name": "my_entity"
}
```

### 2.2 读取与写回

- 读取：目录含 `project.json` → 文件夹形态；常规文件且以 `.eproject` 结尾 → zip 形态。
  未知条目忽略（前向兼容）；`format` 不符或 `version` 高于当前 → 报错。
- 写回：**按来源形态写回**。文件夹 → 写文件夹（清理 `libraries/` 下陈旧 `.json`）；
  单文件 → 写 zip（临时文件 + 原子 move）。
- libId 合法字符 `[a-z0-9_./-]`，拒绝 `..`。

### 2.3 加载管线

- `EprojectService`（client）启动时扫描 `$config/eyelib/`，把项目库注册进
  `GraphLibraryManager`，键 = `{projectName}/{libId}`（与资源包键 `eyelib:nodegraph/x` 区分）。
- 编辑器会话绑定「库键 → 项目引用（路径 + 形态）」；Ctrl+S 把项目内全部库写回来源形态。
- 不来自任何项目的库（资源包 / 内存导入）：保存时在 `$config/eyelib/` 下新建**文件夹形态**项目，
  项目名 = 库键末段（冲突加后缀）。
- 资源包管线（`eyelib/nodegraph/*.json`）保留不变，与 eproject 并存。

## 3. 变量节点化

### 3.1 模型变更

**新端口类型 `PortType.VARIABLE`**（序列化名 `"variable"`）：

- `VARIABLE.isAssignableTo(target)`：target 为任意值类型（FLOAT/INT/BOOL/STRING/ARRAY/ANY/VARIABLE）→ true；
  EXEC/SLOT → false。即变量节点可直接喂任何值端口（**隐式读**）。
- 其它类型 → VARIABLE：仅 VARIABLE 自身（ANY 按通配规则兼容，但验证器严格化，见 §3.3）。

**新节点 `variable`**（Kind.VARIABLE，category variable）：

- 选项：`name`（不带根的变量名，与 `VariableDecl.name()` 一致）。
- 输出：`out`，PortType.VARIABLE。
- 语义：这个节点**就是变量本身**。连线到值端口 = 读取（codegen 发射 `variable.<name>`）；
  连线到 `exec.set_var.target` = 提供写入身份。

**`exec.set_var` 重定义**：

- 选项：`root`/`name` **移除**。
- 输入：`exec_in`、`target`（VARIABLE）、`value`（ANY，默认 0）。
- codegen：`variable.<name> = <value>;`，target 由连线上的 variable 节点提供。

**新节点 `exec.set_temp`**（Kind.EXEC_SET_TEMP）：

- 选项：`name`（temp 名，可带不带 `temp.` 前缀）。
- 输入：`exec_in`、`value`（ANY，默认 0）。承接原 `exec.set_var(root=temp)`。

`temp.get` / `context.get` 维持名选项形态（temp 是瞬态、context 是宿主契约，不进变量面板）。

### 3.2 变量面板

- 列出 `GraphData.variables()`：名称、类型、默认值；支持增/删/重命名/改类型。
- **拖拽到画布** → 在落点创建 `variable` 节点（name 绑定该变量）。
- 重命名级联：图内全部 `variable` 节点的 `name` 同步改写（图改写纯函数 + 编辑器接线）。
- ldlib2：复用 LDLib2 内建 Blackboard（增删改/拖拽/分组现成），变量模型桥接到我们的 GraphData。
- ldlib1：自建面板（LDLib1 ParameterPanelWidget 只读且无增删），行拖拽经
  `setDraggingProvider` → 画布 `setDraggingConsumer` drop 建节点。

### 3.3 验证器

- 检查 19 更新：`variable` 节点的 `name` 必须已声明（UNDECLARED_VARIABLE，warning）。
- 新检查：`exec.set_var.target` 必须连线到 `variable` 种类节点（SET_TARGET_NOT_VARIABLE，error）；
  未连线同样 error。
- 软检查：variable 节点声明类型与其消费端口类型不匹配 → warning（TYPE_MISMATCH 既有通道）。

### 3.4 迁移（format_version 1 → 2）

`GraphMigrations.migrate(GraphLibrary)` 纯函数，加载路径（资源包 loader / EprojectIo / 反编译）统一调用：

| 旧 | 新 |
|---|---|
| `var.get`（name=variable.foo） | `variable`（name=foo） |
| `exec.set_var` root=variable | 新 `exec.set_var` + 自动生成 variable 节点（置于原节点左上）连线 target |
| `exec.set_var` root=temp | `exec.set_temp`（name 保留） |

迁移后 `format_version = 2`。旧文档只读迁移，不回写原文件（回写由保存动作决定）。

### 3.5 反编译器

- `variable.x` 读 → `variable` 节点；`variable.x = ...` → `exec.set_var` + variable 节点连线。
- `temp.x = ...` → `exec.set_temp`。
- 黑板变量收集改读 `variable` 节点名（原 var.get 通道删除）。

## 4. 资产选择器

### 4.1 domain 元数据

`NodeOptionDef` 增加可选字段 `suggestionKey`（纯元数据字符串，domain 不解析）：

| 节点.选项 | suggestionKey | client 数据源 |
|---|---|---|
| ref.geometry.identifier | `geometry` | `ModelManager.INSTANCE` 键（Bedrock geometry 全名） |
| ref.animation.identifier | `animation` | `AnimationRegistries.animation()` 键，前缀 `animation.` |
| ref.ac.identifier | `ac` | 同上，前缀 `controller.animation.` |
| ref.rc.identifier | `rc` | `RenderControllerManager.INSTANCE` 键 |
| ref.material.material | `material` | `MaterialManager` 条目的裸名（`name:variant` 键前缀去重） |
| ref.texture.path | `texture` | `AddonTextureRegistry`（新增 keys 枚举）+ 原版 ResourceManager `textures/` 扫描 |
| entity.root.identifier | （暂不做） | — |

### 4.2 编辑器渲染

- client 侧 `AssetSuggestions`：suggestionKey → `Supplier<List<String>>`，运行时从注册表取（COW 快照）。
- ldlib1：`EvmNode.buildOptionConfigurator` 分派点扩展——带 suggestionKey 的 STRING/IDENTIFIER
  选项渲染为可输入下拉（SelectorConfigurator 候选动态提供）。
- ldlib2：`EvmNodeBase.onDefineOptions` 接线 `withConfigurable` 自定义 SelectorConfigurator。
- 下拉只是候选供给，不锁死自由输入（外部契约逃生舱，与 short_name 覆盖同理）。

## 5. Ctrl+S

- ldlib1：EditorRoot（WidgetGroup 根）覆写 `keyPressed`，`Screen.isSave`（Ctrl+S）→ 保存。
- ldlib2：LDLib2 内建 SAVE 命令链（ModularUI→CommandEvents.SAVE→notifySaved），接线到 EprojectService。
- 保存成功/失败在编辑器内 toast 反馈。

## 6. 非目标

- 不做项目间库依赖/引用（项目内库自闭包，跨项目契约仍走有效短名派生）。
- 不做 temp/context 变量的面板化。
- 不做 texture 的文件夹浏览对话框（候选列表 + 自由输入足够）。

## 7. 验证结果（2026-08-05）

- 单测：1.20.1 / 1.21.1 全量绿（含架构门禁；新增 GraphMigrationsTest 5 项、
  GraphVariableOpsTest 5 项、PortType VARIABLE 矩阵 2 项、EprojectIoTest 9 项、
  验证器 SET_TARGET_NOT_VARIABLE 3 项）；26.1.2 编译绿。
- 实机（1.20.1，EprojectSmoke2 世界）：
  - 变量面板：新增 speed:FLOAT 经 Host 路径落库，面板行渲染（改名/类型/删按钮在位）；
  - 保存按钮：未绑定库 → 新建文件夹项目 config/eyelib/main/（OPC 四件齐全），
    变量与新节点形态（variable/exec.set_var）落盘；二次保存写回原项目不分叉；
  - loadAll 回读：main/main 加载，variables 与 format_version=2 一致；
  - 构建 + 挂载渲染：eyelib:ng_smoke_test 注入并接管猪渲染（vanilla 模型消失）；
  - 资产下拉：SuggestionConfigurator 挂在 ref 节点选项上，候选层渲染（geometry=2969 /
    animation=2560 / texture=10222 / material=202 / rc=972 运行时枚举）。
- 实机（1.21.1，EpSmoke21 世界）：
  - LDLib2 SAVE 链（notifySaved）落盘文件夹项目，二次保存无分叉；
  - Blackboard 在位（内建变量面板），4 个资产 Selector 挂在 ref 节点；
  - zip 形态 mainzip.eproject 加载成功（directoryForm=false，库内容完整）。

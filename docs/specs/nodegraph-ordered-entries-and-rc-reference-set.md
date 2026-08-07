# 有序条目链 + RC 引用集收口（format v6）

> 状态：**已实施并实机验证（2026-08-07）**。用户拍板（2026-08-06）：链式拓扑；
> rc.root 删 decl_*、ref.rc 保留。
> 修订关系：取代 nodegraph-inline-render-controller §2.1（rc.root decl_* 端口）与
> 「SLOT 条目 uid 字典序收集」语义；short_name 机制不变（§6 调查结论）。
>
> 验证结果：单测新增 8 例全绿（链序发射/链验证×4/carve-out/迁移×2/导入链序）；
> 1.20.1/1.21.1 test+nullaway、26.1.2 编译全绿；实机悦灵闭包导入（811 节点）→
> 构建 0 错误：6 个 RC 的 textures/materials/part_visibility 数组与 pack 原版（ROOT
> subpack）逐一相等（链序 = JSON 数组序）；声明表 = 原版减 9 个无引用非协议行
> （jehvjy/nshdul/wlipmw 等混淆短名，模型 texture_meshes 未引用——D9 构建警告静默），
> 协议行（default/material）全保留；编辑器实开 rc.root 无 decl_* 端口。

前置：nodegraph-inline-render-controller（v4 内联 RC）、nodegraph-shortname-elimination、
nodegraph-color-and-inline-literals（v5 颜色端口）。

## 1. 问题

1. **条目顺序不可控**：rc.root 的 `textures`/`materials`/`part_visibility` 三个 SLOT 列表
   按 uid 字典序收集条目，而 Bedrock 语义里顺序有效——materials 首匹配优先、
   part_visibility 按序求值、textures 数组位置对应多纹理模型的 mesh 顺序。
2. **decl_* 冗余**：rc.root 的 `decl_geometries/decl_textures/decl_materials` 是纯粹的
   声明管道，但其下方的 `geometry`/`textures`/`materials` 三个值端口已承载
   「声明+引用」——一根线两个语义（v4 §1），声明端口对用户是噪声。

## 2. R1：有序条目链

### 2.1 拓扑

三类条目节点（`list.entry` / `material.entry` / `part_visibility.entry`）新增
**单连接 SLOT 输入 `next`**：数组顺序 = 链序。

```
rc.root.textures ← e1.entry；e1.next ← e2.entry；e2.next ← e3.entry …
```

- rc.root 的列表端口只接**链头**；`entry` 输出保持单连接（一个条目只能属于一条链）。
- 链对自动布局更友好（链式紧凑，替代条目扇）。

### 2.2 组装语义

- `AssemblySupport.chainSources(graph, rootUid, portId)`：取链头（多头按 uid 序兜底），
  沿 `next` 走访（visited 集防环），产出**有序**条目列表。
- textures → value 表达式列表（链序）；materials → [{pattern: value}]（链序，首匹配优先）；
  part_visibility → [{bone_pattern: condition}]（链序；同 pattern 后来者覆盖、去重保序——不变）。
- 不变量：同一输入 JSON，组装输出确定（链序唯一决定条目序）。

### 2.3 验证器

- SLOT_WHITELIST 新增：`<entry>.next ← <entry>`（三类各一）；rc.root 三端口白名单不变（接链头）。
- `LIST_MULTI_HEAD`（ERROR）：rc.root 列表端口直连多条目。
- `LIST_CYCLE`（ERROR）：next 链成环（SLOT 边不参与既有值环检测，链环单独查）。
- `ENTRY_ORPHAN`（WARNING）：条目节点不在任何到达 rc.root 的链上（不会出现在输出）。

## 3. R2：RC 引用集收口

### 3.1 端口变化

- **rc.root 删除** `decl_geometries/decl_textures/decl_materials`。
  删除后 rc.root 上能接受 ref 连线的端口恰好只剩 `geometry`/`textures`/`materials`
  （condition 是 FLOAT、颜色是 COLOR、arrays 是 TEXT 选项——类型系统天然排除 ref），
  既有可达性模型（DeclarationTables）语义自动收口为「**底下三个端口决定引用集**」：
  接进三端口（含表达式树内部嵌套，如 `query.x ? geometry.a : geometry.b`）的 ref 入表。
- **ref.rc 保留** decl_*：它没有值端口，decl_* 是外部 RC 表行的唯一声明通道
  （共享 RC 实体喂短名的场景）。设计性 miss（外部 RC 引用未声明短名 → 运行时
  default 回退）不需要任何声明，不受影响。

### 3.2 协议短名 carve-out

`default`（全类别）与 `texture.material`（ref.texture）是运行时协议常量
（nodegraph-shortname-elimination D4 已确立「不剥」先例）。此类 ref **在图中出现即入表**，
无需接线；验证器 REF_NOT_CONNECTED 对协议短名 ref 豁免。

理由：导入保真——allay 类资产的协议行（模型 texture_meshes 契约、空值回退落点）
不挂在任何字段表达式上，纯三端口模型无法表达，没有 carve-out 会破坏运行时回退
（A&S 实机教训）。

### 3.3 导入（反编译）剩余行处理

`attachLeftoverEntries`（实体表中被 RC 字段引用后剩余的表行）：

- 锚点 rc.root：协议短名行 → 建不接线 ref 节点（carve-out 覆盖）；
  非协议行 → 建不接线 ref 节点 + INFO 诊断（`DECL_LEFTOVER_DROPPED`：该行不进表，
  多为死重或 arrays 原文引用——后者属 §7 非目标）。
- 锚点 ref.rc：接 decl_*（不变）。

## 4. 迁移 v5 → v6（GraphMigrations 链式，CURRENT_FORMAT_VERSION=6）

CLIENT_ENTITY / RENDER_CONTROLLER 库主图：

1. rc.root 三列表端口的多条目线 → 按 uid 字典序（= v5 发射序，保产物等价）排成链：
   首条目线保留，其余线重定向为 `e_i.entry → e_{i-1}.next`。
2. rc.root decl_* 连线 → 删线；源 ref 节点保留（协议短名由 carve-out 接管；
   非协议行自此不进表——与导入同规则）。
3. format_version → 6。ANIMATION_CONTROLLER 库与 ref.rc 不变。

## 5. 异常行为

| 情形 | 行为 |
|---|---|
| 列表端口多头 | 验证器 LIST_MULTI_HEAD；组装器按 uid 序取头兜底（尽力产出） |
| next 链成环 | 验证器 LIST_CYCLE；组装走访 visited 截断 |
| 条目不在链上 | ENTRY_ORPHAN 警告；不发射 |
| 非协议剩余行（导入/迁移） | 不进表 + INFO 诊断；需要时用户接线到三端口 |
| arrays 原文 TEXT 内的裸短名 | 无声明通道（D10 暂留逃生舱）；结构化 arrays 属路线图 |

## 6. R3：short_name 消除审计结论（本次不改代码）

现状（2026-08-04 起）：`short_name` 选项默认空 = 按标识符派生（ShortNames.effective），
authoring 闭环内零管理。必须保留显式短名的仅剩三类外部契约：外部/vanilla RC 互操作、
模型 texture_meshes / per-bone material 短名（F8）、导入保真。选项保留即逃生舱，
不进一步消除；R2 不新增短名需求（协议行照旧靠显式 short_name 表达）。

## 7. 非目标

- `render_controllers`（RC_REF multi）与 `scripts.animate` 的顺序语义（Bedrock 侧
  顺序无意义或本轮不动）。
- 结构化 arrays（array.entry）——路线图后续项；届时 arrays 内 ref 获得声明通道。
- ref.rc 的 decl_* 重命名/UI 调整。

## 8. 验证标准

1. 单测：链序发射（textures/materials/part_visibility）、LIST_MULTI_HEAD/LIST_CYCLE/
   ENTRY_ORPHAN、迁移 v5→v6（旧多头图 → 链化后产物逐一相等）、decl_* 移除后
   三端口可达性收口、协议 carve-out（不接线 default/material 入表）、导入剩余行诊断。
2. 悦灵实机：重导入 → 条目链序 = pack 数组序；构建实体 JSON 与 pack 原版逐字段相等
   （重点：textures 数组序、materials 首匹配序、协议行保留）。
3. 三版本编译 + 1.20.1/1.21.1 test/nullaway 全绿；实机打开编辑器确认 rc.root 无
   decl_* 端口、条目链可连。

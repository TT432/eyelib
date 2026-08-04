# 内联 RenderController：实体画布内的 RC 编辑

版本 v4（format_version=4）。前置：nodegraph-declaration-wiring（D1 声明=连线）。

## 1. 关系模型（为什么 geo ref 直接接 ClientEntity 没有意义）

Bedrock 文档间的真实语义关系：

| 文档 | 角色 | 消费关系 |
|---|---|---|
| ClientEntity | 宿主 | 声明表（短名→资产）、scripts/animate、render_controllers 列表（带条件） |
| RenderController | 消费者 | **geometry/texture/material 短名的唯一消费端**（geometry/textures/materials 字段 + arrays 动态索引） |
| AnimationController | 消费者 | 动画短名的消费端之一（状态机列动画短名）；另一消费端是实体 animate 脚本 |

推论：

1. **geometry/texture/material 的语义锚点是 RC，不是实体**。实体声明表里的这些行，
   存在的唯一目的是被某个 RC 按短名引用。把 ref.geometry 接到 entity.root 上，
   只是"声明了一行表"，没有任何消费端——是纯粹的样板连线。
2. **动画/AC 不同**：它们有两个消费端（实体 animate 脚本 + AC 状态机），且 AC 是独立文档，
   因此动画/AC 声明保留在实体级（entity.root.animations / animation_controllers）是正当的。
3. **ref 接 RC = 声明 + 引用**（一根线表达两个语义）；**裸字符串 / arrays 原文 = 仅引用**
   （设计性 miss：短名不在表中，运行时回退 default）。这一区分正好覆盖 A&S 等
   真实资产库的两种用法。

因此 v4 把 RC 编辑收进实体画布：rc.root 节点直接放在 CLIENT_ENTITY 主图，
geo/tex/mat ref 接到 RC 锚点（rc.root / ref.rc），实体声明表由组装器**派生**。

## 2. 节点类型变化

### 2.1 rc.root（可放 CLIENT_ENTITY 主图 / RENDER_CONTROLLER 库）

新增端口：

| 端口 | 方向 | 类型 | 语义 |
|---|---|---|---|
| `condition` | IN | FLOAT（默认 1） | 实体 render_controllers 条目的条件表达式（内联时有效） |
| `controller` | OUT | RC_REF | 接 entity.root.render_controllers = 本实体定义并挂载该 RC |
| `decl_geometries` | IN multi | GEOMETRY_REF | 仅声明（fields/arrays 以裸字符串引用但需在表的条目） |
| `decl_textures` | IN multi | TEXTURE_REF | 同上 |
| `decl_materials` | IN multi | MATERIAL_REF | 同上 |

decl_ 前缀避免与 rc.root 自身的 `textures`/`materials` SLOT 端口撞名。

已有端口不变：`geometry`（STRING，可接 ref.geometry——REF→STRING 隐式转换，
发射 `geometry.<有效短名>` 并同时入声明表）、`textures`/`materials`/`part_visibility`（SLOT 条目，
条目 value 可接 ref——同理声明+引用）、颜色通道、`arrays`（TEXT 选项原文）、`ignore_lighting`。

### 2.2 ref.rc（引用外部 RC：vanilla / 共享 / 独立 RC 库）

新增端口：`condition`（FLOAT 默认 1）、`decl_geometries`/`decl_textures`/`decl_materials`（IN multi，
**为该外部 RC 准备表行**——外部 RC 的文档不在本图，但它按短名消费的条目必须声明）。

### 2.3 entity.root

- **删除** `geometries`/`textures`/`materials` 三端口（语义锚点移至 RC）。
- `render_controllers`：SLOT（rc.condition_entry）→ **IN multi RC_REF**，
  直连 rc.root.controller 或 ref.rc.ref。
- 保留 `animations` / `animation_controllers`（动画双消费端，§1 推论 2）。

### 2.4 rc.condition_entry：删除

条件收进 rc.root / ref.rc 的 `condition` 端口。

## 3. 组装语义

### 3.1 声明表派生（ClientEntityAssembler）

geometry/textures/materials 三表 = **可达性模型**（`DeclarationTables`）：
ref.{geometry,texture,material} 在主图中存在到任一 **RC 锚点**（rc.root / ref.rc）的
连线路径 → 入表。覆盖声明端口直连、geometry 字段端口直连、条目 value、以及**嵌套在
表达式树内部**的情形（悦灵 RC 的 `query.x ? geometry.a : geometry.b` 变体选择）。
无路径 → 不声明（验证器 REF_NOT_CONNECTED）。

同有效短名 putIfAbsent（冲突由验证器 REF_CONFLICT 报告，范围与本集合一致）。
default 别名规则不变。animations 表 = entity.root 两端口（不变）。
类别不匹配的声明线（如 ref.texture 接 decl_geometries）报 INVALID_DECLARATION_REF，
但可达性模型下该 ref 仍入其本类别表（尽力产出：错误由诊断承担，表保持语义完整）。

### 3.2 render_controllers 列表

`render_controllers` 端口连线源（uid 字典序）：

- **rc.root** → 内联 RC：条目 = identifier（condition 恒 "1" → 纯字符串，否则
  `{identifier: condition}`）；同时将该 rc.root 组装为 RC 文档条目，汇入
  `AssemblyResult.extraDocs` 的合并 render_controllers 文档。
- **ref.rc** → 外部引用：条目 = identifier + condition（同上格式），不产文档。

### 3.3 AssemblyResult

新增 `extraDocs: List<JsonObject>`（默认空）。实体构建且含内联 RC 时 = 单个合并的
`{"format_version":"1.8.0","render_controllers":{...}}` 文档。client 构建服务注册进
RenderControllerManager。内联 RC id 与其它图库 rc.root 撞 id → 构建服务警告（后注册胜出）。

### 3.4 RENDER_CONTROLLER 库（独立 RC 库）不变

rc.root 新端口在独立库中闲置（condition/controller 无消费端）；组装逻辑复用同一
assembleEntry。

## 4. 验证器

- **REF_NOT_CONNECTED 重写**（WARNING，仅 CLIENT_ENTITY 主图）：
  - ref.{geometry,texture,material} 未接入任何 RC 锚点（声明端口 / rc.root 字段端口 /
    条目 value）→ 「不会进入声明表」；
  - ref.rc / rc.root 的 controller 未接 entity.root.render_controllers → 「不会出现在
    render_controllers」；
  - 动画/AC 规则不变；子图 ref 规则不变；占位 ref（标识符空）豁免不变。
- **REF_CONFLICT**：范围 = §3.1 派生集合（与组装器一致）。
- **DUPLICATE_RC_ID**（ERROR）：主图两个内联 rc.root 的 identifier 相同。
- SLOT_WHITELIST：移除 render_controllers 条目（不再是 SLOT）；rc.condition_entry 清理。

## 5. 迁移 v3 → v4（GraphMigrations 链式）

仅 CLIENT_ENTITY 库：

1. 每个 rc.condition_entry：rc 线源（ref.rc）的 `ref` 直连 entity.root.render_controllers；
   condition 线（若有）移到该 ref.rc 的 `condition` 端口；同一 ref.rc 有多条 entry 时先者胜，
   其余删除；删除 entry 节点及其线。
2. entity.root `geometries`/`textures`/`materials` 上的声明线 → 重定向到主图第一个
   ref.rc（uid 序）的对应 `decl_*` 声明端口；无 ref.rc → 断线（REF_NOT_CONNECTED 会提示）。
3. format_version → 4。RENDER_CONTROLLER / ANIMATION_CONTROLLER 库不变（新端口闲置）。

## 6. 导入（反编译）

`JsonGraphImporters.importClientEntity` 变体接受 RC 文档解析器
`Function<String, Optional<JsonObject>>`（domain 纯接口；client ImportClosure 提供
注册表优先 + 资源目录扫描的实现）：

1. 实体声明表先入内存 KnownTables（纯数据，不再直接建 entity 级 geo/tex/mat ref）；
2. 逐 render_controllers 条目：resolver 命中 → 将 RC 文档**内联反编译进同一主图**
   （rc.root + 值节点 + ref 接线：字段引用接字段端口/条目 value，表内未被字段引用的
   条目接该 rc.root 声明端口；controller → render_controllers；条件 → condition）；
   未命中 → ref.rc（+ CLOSURE_MISS warning），表中条目挂第一个 RC 锚点的声明端口；
3. 动画/AC 导入不变（ref.animation/ref.ac → entity.root；AC 闭包仍为独立库）；
4. 布局：controller 线使 RC 簇连入主图，GraphLayout 分层自动把 RC 簇排在 entity.root
   左侧，无需多簇特判。

## 7. 不变量与兼容

- 独立 RC 库（kind=render_controller）继续支持：共享 RC 的高级用法保留；
  实体经 ref.rc 引用之。
- AC 不内联（双消费端 + 独立文档语义；若未来需要，对称方案可复用本规格）。
- 构建产物与 v3 等价：同一悦灵资产，v3（7 库）与 v4（1 库内联）构建出的实体 JSON 与
  RC 文档逐一相等。

## 8. 验收

1. 悦灵重导入 = 单库：6 个 rc.root 内联、controller/condition 接线齐全、geo/tex/mat
   ref 全部接 RC 锚点、entity.root 无三声明端口。
2. 构建：实体 JSON 与 pack 原版逐字段相等；extraDocs 的 6 个 RC 文档与 v3 独立库产物相等。
3. v3 旧图（ng_smoke、eproject 存档）加载自动迁移 v4：condition_entry 消失、声明线
   重定向 ref.rc、构建产物不变。
4. 验证器：断 controller 线 → REF_NOT_CONNECTED；双 rc.root 同 id → DUPLICATE_RC_ID。
5. 实机：导入→构建→注入，渲染与 pack 原版一致。

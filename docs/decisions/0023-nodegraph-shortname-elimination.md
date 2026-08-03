# ADR-0023：节点图短名称消除——派生短名与协议名保留

日期：2026-08-03　状态：已接受（用户拍板）　规格：docs/specs/nodegraph-shortname-elimination.md

## 背景

Bedrock 实体格式的资源引用是两级间接：实体 description 声明表（短名 → 资产标识），
RC/AC/animate 再用短名引用。节点图语言此前把短名直接暴露给用户管理（ref.* 节点
同时携带 short_name + identifier），跨图库（实体库 ↔ RC 库）一致性零保障；
对「基于已有 cliententity 制作」的工作流（混淆包：12 纹理/13 几何/30 动画短名）
短名是纯噪声。

关键约束（代码验证，规格 §2 F1-F10）：短名是**运行时契约键**，输出 JSON 格式不可消除；
且部分短名由**外部定义**（模型文件 texture_meshes/per-bone material、外部 RC/AC 文档、
vanilla `default` 惯例）。因此消除范围是 **authoring 层的管理负担**，不是格式本身。

## 决策（用户拍板 2026-08-03）

### D1. 有效短名 = 显式覆盖 ? 显式 : derive(标识符)

- `derive` = identity+sanitize（'/'→'.'、lowercase、非法字符→'_'、段首数字前插、
  折叠多余 '.'），domain 纯函数 `ShortNames`。
- **纯函数两端各算**：实体组装器建表与 RC/AC/animate 发射套用同一函数，
  跨图库契约一致性从「用户手工同步」变为「构造保证」。多点短名合法性的运行时依据：
  molang 成员访问经 bytecode 重 join 为扁平串做 scope 查找。
- 显式覆盖（short_name 非空）是逃生舱：外部文档互操作、模型内短名、导入保留。

### D2. 单资产表自动发 `default` 别名

保住运行时多处 `get("default")` 回退（texture_mesh 回退无二级兜底）与 vanilla RC
`geometry.default` 惯例。多资产表不发（歧义），需要时显式覆盖。

### D3. 导入保留 + 一键规范化 + 跨文档关联回填

- 导入默认填显式 short_name（往返保真、外部不断）；`ShortNameOps.normalize` 是用户主动的
  图库级规范化动作（编辑器「规范化」按钮）。
- RC/AC 导入携 `KnownRefTables`（client 收集：注册表实体声明表 + 已导入实体图库），
  裸短名 ref 命中即回填标识符（预览可用）；回填优先「render_controllers 列出该 RC」的
  实体（collectForRc）——A&S 式跨实体共享 RC 中，非关联短名是**设计性 miss**
  （miss→default 回退），全局回填会造成预览错配。
- 运行时纹理值带 `.png`（CODEC 层补），收集时剥除——图内 path 选项不带后缀。

### D4. 协议短名不剥（实机修正）

`default`（全类别）与 `material`（ref.texture 的 Bedrock `texture.material` 协议字）
在规范化时保留。它们不是可派生的资产引用而是运行时协议常量；悦灵实机证实剥掉
`default` 后多资产实体的回退链落空、渲染错模型。

### D5. ref.ac 改发 animations 表（修预存缺陷）

eyelib 运行时只读 animations 表（AnimationAssetRegistry 把 AC 与动画同注册表），
此前 assembler 把 ref.ac 发进 `animation_controllers` 导致 animate 引 AC 运行时解析不到。
改发 animations（Bedrock animate 命名空间两表合并；vanilla 同款）。导入侧仍兼容
`animation_controllers` 字段。

### D6. 验证器按有效短名查冲突

REF_CONFLICT 改按有效短名（ref.animation/ref.ac 共享 animations 命名空间）；
新增 INVALID_SHORT_NAME（显式覆盖非 molang 合法路径 / 有效短名空）。

## 实机发现并修复的预存缺陷（超出原规划）

- **RC 条件双重渲染**：`render_controllers` 数组与 `render_controller_conditions` map
  同 id 时导入曾生成重复 rc.condition_entry → 运行时同 RC 渲染两遍（悦灵实机组件转储
  暴露：11 vs 7）。修复：导入按 id 合并（map 优先，与运行时合并语义一致）。
- **F3 潜伏 bug**：`RenderControllerEntry.get` 的 `replace(type+".","")` 全量替换对多点
  短名误剥前缀 → 仅剥首个类别前缀。

## 验证

悦灵全闭包（7 图库）规范化重建后，钉死随机变体变量，**逐组件解析转储与 pack 原版
完全一致**（7 组件 几何|纹理|材质 三元组逐一相等），渲染视觉一致；单测全绿（含架构
门禁）；三版本编译绿。证据见规格 §6。

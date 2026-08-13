# 规格：节点图短名称消除（Short Name Elimination）

状态：**已批准**（2026-08-03 用户拍板：D1 identity+sanitize、D4 导入默认保留原短名、D2 单资产自动 default 别名、D6 ref.ac 改发 animations 表）。关联：ADR-0021、nodegraph-visual-molang.md（§2.5/§2.6）、nodegraph-workbench.md。

## 1. 问题陈述

Bedrock 实体格式中，资源引用走两级间接：实体 description 声明表（短名 → 资产标识），
RC/AC/animate 等处再用短名引用。当前节点图语言把短名直接暴露给用户：

- `ref.geometry/texture/material/animation/ac` 节点同时携带 `short_name` 与
  `identifier/path/material` 两个选项，用户手填并保持二者一致。
- **跨图库同步零保障**：RC 图库的表达式槽发射 `geometry.<short_name>` molang，
  实体图库的声明表按 `short_name` 收集——两个独立图库靠用户手工维持短名一致；
  验证器 `REF_CONFLICT` 只能查同一图库可达集内的冲突。
- 反编译 RC 时 `geometry.x` 只能恢复短名（标识符丢失），ref 节点无预览。
- 短名是**机器键**，不是人类意图；对「基于已有 cliententity 制作」的工作流
  （悦灵类实体：textures 12 项、geometry 13 项、animations 30 项混淆短名）是纯粹噪声。

目标：**authoring 层消除短名管理**——用户只选资产（带预览），短名由系统派生；
输出 JSON 仍为 Bedrock 兼容的短名契约（格式不变，运行时不变）。

## 2. 已验证的运行时事实

短名在输出 JSON 中是**运行时契约键**，不可从格式中移除。以下为代码验证的事实：

| # | 事实 | 证据 |
|---|---|---|
| F1 | RC 装配时把实体三表注册为 molang scope 扁平键：`texture.<short>` / `geometry.<short>` / `material.<short>`（**lowercase**） | RenderControllerEntry.initArrays:98-110 |
| F2 | molang 成员访问 `a.b.c.d` 经 bytecode 重 join 为扁平串后 `scope.get(dottedName)`——**多点短名在 scope 通道合法** | MolangBytecodeEmitter.memberAccessName:639 / MolangRuntimeSupport.resolveMemberAccess:81 |
| F3 | scope miss 时回退：`map.get(value.context().toLowerCase().replace(type+".",""))`——`replace` 是**全量替换**，多点短名若含类别前缀片段会被剥错（潜伏 bug，scope 命中时走不到） | RenderControllerEntry.get:478 |
| F4 | RC arrays 元素是 molang 源串，运行时 `scope.get(s.toLowerCase())` **不经解析**——任意字符可用 | RenderControllerEntry.initArrays:88-97 |
| F5 | animate 键是 JSON 键，`animations.get(name)` 直查，**不经 molang 解析、不 lowercase** | AnimationComponent.setup:94 |
| F6 | Bedrock 规范中 AC 短名声明在 `animation_controllers`（list of 单键 map）；eyelib 运行时 **只读 `animations` 表**（AnimationAssetRegistry 把 AC 与动画放同一注册表，animate 解析只查 `animations`）——当前 assembler 把 ref.ac 发进 `animation_controllers` = **animate 引 AC 在运行时解析不到（预存缺陷）** | BrClientEntity:31-32 / AnimationComponent.setup / bedrock.dev pig 示例 |
| F7 | 运行时硬编码 `"default"` 回退多处：RC geometry null 回退（有二级 `values().first()` 兜底）、**textureMesh 回退（无兜底，miss 即跳过 mesh）**、材质动态纹理回退 | RenderControllerEntry:120,231-233,312 / NodeAssetPreview:100（有兜底）/ EntityRenderOrchestrator:604（有兜底） |
| F8 | **模型文件内** `texture_meshes[].texture` 与 per-bone `material` 是短名，由**模型作者**定义，经实体表解析——模型↔实体的**外部契约**，与纹理路径无关 | BedrockGeometryModel.TextureMeshDef / Model.Bone.material / RenderControllerEntry:230 |
| F9 | 短名在 molang 成员访问中须为合法标识符路径：段 = `[a-zA-Z_][a-zA-Z0-9_]*`，`.` 分段；`/`、`-`、段首数字非法；**须小写**（scope 注册 lowercase，查找不 lowercase） | molang 词法 / F1 / MolangRootAliasCanonicalizer |
| F10 | 混淆短名跨表复用合法（allay：`wlqxdl` 同是纹理/几何/动画/材质短名）——各类别命名空间独立 | allay.json 导出 |

## 3. 契约分析：哪些短名可派生

| 通道 | 短名定义方 | 可派生？ |
|---|---|---|
| RC geometry/textures/materials 槽（molang） | 图作者（图内自产自销） | ✅ 纯函数 |
| RC arrays（结构化后） | 图作者 | ✅ |
| scripts.animate / AC state animations | 图作者 | ✅（JSON 键，任意串） |
| 外部 RC/AC 文档（非图产，含 vanilla RC 的 `geometry.default` 惯例） | 外部文档 | ❌ 需显式短名 |
| 模型 `texture_meshes` / per-bone `material` | **模型文件**（外部资产） | ❌ 需显式短名（可自动检测+提示） |
| 导入的既有文档 | 原文档 | ❌ 保留原契约（可选规范化） |

**结论**：短名不能全局消除（外部契约存在），但能对**图内自闭环**（多数情况）消除管理负担。
有效短名规则：**`effective = 显式覆盖 ? 覆盖 : derive(标识符)`**，逐 ref 节点。

## 4. 设计决策

### D1 派生函数（domain 纯函数，`nodegraph.ShortNames`）

```
derive(category, identifier) = sanitize(identifier)
sanitize(s):
  1. texture 类别：'/' → '.'
  2. 全串 lowercase
  3. 非法字符（非 [a-z0-9_.]）→ '_'
  4. 每个 '.' 分段若首字符非 [a-z_] → 段首插 '_'
```

- geometry：`geometry.oreville_ans.tgsary` → 原样（identity，多点合法——F2）。
- texture：`textures/oreville/ans/czm` → `textures.oreville.ans.czm`。
- material：`entity_alphatest` → 原样。
- animation/AC：identity（F5，不过 molang 解析；统一走同一函数无害）。
- **纯函数、无状态**：实体组装器建表与 RC/AC/animate 发射两端各自计算，结果必然一致——
  跨图库契约一致性从「用户手工同步」变为「同函数两端各算」。这是本规格的核心收益。
- sanitize 碰撞（`a/b` 与 `a.b`）由验证器检出（D5）。

### D2 `default` 别名

- 某类别表的**不同资产恰 1 个**且无任何显式 `default` 覆盖时，组装器额外发
  `"default": <资产>` 别名（同一资产两键）。
- 保住 F7 的运行时回退与 vanilla/外部 RC 的 `geometry.default` 惯例（单资产情形）。
- 多资产表不发（歧义；需要 default 的场景走显式覆盖——用户意图，不猜测）。

### D3 显式覆盖（逃生舱）

- ref 节点保留 `short_name` 选项，**默认空 = 派生**；非空 = 显式覆盖（编辑器字段始终显示有效短名）。
- 覆盖场景：外部文档互操作（vanilla RC）、模型 texture_meshes/per-bone material 名（F8）、
  导入保留（D4）。
- 覆盖值同样过 sanitize 校验（F9），非法 → 验证错误。

### D4 导入策略：默认保留 + 可选规范化 + 跨文档关联

- **保留**（默认）：导入实体时声明表 → ref 节点填显式 `short_name`（原契约原样保留，
  往返保真，同 pack 未导入文档不断）。
- **规范化**（用户主动动作，图库级）：清除全部显式覆盖 → 回到派生。适用「全闭包已导入」
  （实体 + 其 RC + AC 全在图内）。便签提示「同 pack 未导入文档若引用旧短名将断」。
- **跨文档关联**：同批导入实体 + RC 时，RC 侧 `geometry.x` 反编译出的裸短名 ref
  用实体表**反查补全标识符**（短名+类别 → 标识符），ref 变为完整节点（有预览）；
  未匹配 → 维持裸短名显式覆盖（现状行为）。
- **协议短名不剥**（实机修正）：规范化保留 `default`（全类别）与 `material`（仅 ref.texture）。
  这两类不是可派生的资产引用，而是运行时协议常量：`default` 是 geometry null 回退 /
  `texture.material` 动态值回退 / texture_mesh 回退的统一落点（F7），`texture.material`
  是 Bedrock 动态材质纹理协议字。A\&S 悦灵实机验证：剥掉 `default` 后多资产实体回退落空，
  渲染错模型。
- **「设计性 miss」语义**（实机发现）：RC 引用宿主实体表中不存在的短名是合法的
  （miss → default 回退），A\&S 跨实体共享 RC 大量使用。因此回填按「列出该 RC 的实体」
  优先过滤（`KnownRefTables.collectForRc`），非关联实体表仅兑底；且运行时纹理值带
  `.png` 后缀（CODEC 层补），收集时必须剥除——图内 path 选项不带后缀。

### D5 验证器变更

- `REF_CONFLICT` 改按**有效短名**检测：同类别、同有效短名、不同标识 → error
  （覆盖 sanitize 碰撞、显式↔派生碰撞、显式↔显式碰撞）；ref.animation 与 ref.ac
  共享 animations 命名空间检测（D6）。
- 新增 `INVALID_SHORT_NAME`：显式覆盖非 molang 合法成员路径（仅会发射为 molang 的
  geometry/texture/material 类别，F9）或有效短名为空 → error。
- ~~DERIVED_COLLISION_WITH_DEFAULT~~：不单设码——REF_CONFLICT 已覆盖全部真实碰撞
  （别名仅在无 default 键时追加，无碰撞面）。

### D6 ref.ac 表归属修复（F6 预存缺陷）

- assembler：ref.ac 短名**改发进 `animations` 表**（与 oreville 包同款、Bedrock 引擎实际行为——
  animate 命名空间是两表合并的），`animation_controllers` 字段停止发射。
- 配套：反编译遇到 `animation_controllers` 字段仍按 ref.ac 导入（兼容既有 JSON）。
- 备选方案（运行时合并两表）不取：多发一个运行时不读的字段无意义，animations 表是
  Bedrock 事实标准路径（vanilla 也把 controller 声明进 animations）。

### D7 运行时小修

- `RenderControllerEntry.get` 的 `replace(type+".","")` → `replaceFirst`（F3 潜伏 bug，
  多点派生短名下会被触发：scope 未注册时的 fallback 路径）。

### D8 编辑器

- ref 节点：标识符字段为主（现有预览不变）；`short_name` 为普通字段，**始终显示有效短名**
  （底层空 = 自动派生并随 identifier 即时刷新，用户可输入覆盖、清空恢复自动）。两版编辑器同。
- 图库级「规范化短名」按钮（D4）：一键清显式覆盖。
- 资产检查器（W1）对 ref 节点的「跳转资产定义」以标识符为准（现状已是）。

### D9 模型短名覆盖度检查（client 构建期警告，非 domain）

- NodegraphBuildService 构建实体时：收集图内 ref.geometry 标识符 → 查模型资产的
  `texture_meshes[].texture` 短名 → 实体纹理表有效短名集合未覆盖（且无 `default` 回退键）
  → **build 警告**（UNCOVERED_TEXTURE_MESH，不阻断）。
- per-bone `material` 短名当前运行时**无消费端**（仅导入存储），不检查。
- domain 不碰模型加载（架构约束），检查放 client 层。

### D10 arrays 暂留 TEXT 逃生舱

- rc.root 的 arrays TEXT 选项本任务不结构化；文档注明元素短名按 D1 规则派生
  （如 `Array.skins` 元素写 `texture.textures.entity.foo`）。
- 结构化 arrays 节点（array.entry）列路线图后续项。

## 5. 分阶段实施

| 阶段 | 内容 | 验证 |
|---|---|---|
| P1 | domain：`ShortNames` 纯函数 + ref 节点选项语义变更（short_name 默认空）+ codegen EmitSession 有效短名 + 三个 assembler 有效短名/D2 别名/D6 表归属 + D5 验证器 | 单测：派生函数矩阵、碰撞检测、别名规则、ref.ac→animations、往返 |
| P2 | decompiler：D4 保留/跨文档关联 + 规范化动作（domain 纯函数 `normalize(GraphLibrary)`） | 单测：悦灵全量导入→规范化→重组装，表键全派生化；RC+实体同批导入标识符补全 |
| P3 | 编辑器两版：short_name 有效值字段（自动生成+可覆盖）+ 规范化按钮 | 客户端截图验证 |
| P4 | client：D7 运行时小修 + D9 构建警告 + NodegraphBuildService 诊断透传 | 悦灵规范化重建 → 渲染截图对比（应无变化） |
| P5 | 文档：本规格状态→已批准、ADR-0023、nodegraph-visual-molang.md §2.5/2.6 修订 | — |

兼容性：图库 JSON schema 不变（short_name 选项保留，空=派生）；存量图库
（short_name 已填）自动走显式覆盖路径，行为不变。

## 6. 验证结果（2026-08-04 实机）

1. 单测全绿（含架构门禁）：ShortNames 派生矩阵、ShortNameOps normalize/backfill、
   验证器碰撞/非法检查、组装器派生+别名+ref.ac 表归属、往返测试。
2. **悦灵全闭包验证**（A\&S 包：11 几何、10 纹理、5 材质、30 动画、6 RC 含条件）：
   导入（保留）→ RC 回填（32 个裸短名 ref 全部补全标识符）→ 规范化（剥 58+26 个显式短名，
   保留 3 个 `default`）→ 7 库全部构建成功 → 钉死随机变体变量后**逐组件解析转储与 pack
   原版完全一致**（7 组件 几何|纹理|材质 三元组逐一相等）→ 渲染视觉一致。
3. 新 authoring 闭环：ref 节点默认派生（无需触短名字段）→ 构建渲染正常；
   存量显式短名图库（ng_smoke）走覆盖路径兼容。
4. 三版本（1.20.1 / 1.21.1 / 26.1.2）编译全绿。

### 实机发现并修复的预存缺陷

- **RC 条件双重渲染**（decompile 保真）：实体 JSON 的 `render_controllers` 数组与
  `render_controller_conditions` map 含同一 RC id 时，导入曾生成两批 rc.condition_entry
  → 组装回出重复条目 → 运行时同一 RC 渲染两遍。修复：按 id 合并（map 优先，与运行时
  合并语义一致），回归测试 `renderControllerArrayAndConditionsMapMergeById`。
- F3（replace 全量替换）修复为仅剥首个类别前缀（D7）。
- F6（ref.ac 表归属）修复见 D6。

## 7. 风险与开放问题

| 风险 | 缓解 |
|---|---|
| 规范化后同 pack 未导入文档（外部 RC/AC）引用旧短名断裂 | 默认保留；规范化为显式用户动作 + 警告文案 |
| 派生短名长（`texture.textures.oreville.ans.czm`），JSON 可读性降 | 机器键本不供人读；图内/徽标显示资产尾段 |
| 模型 texture_meshes 短名与派生不一致 → mesh 静默跳过 | D9 构建警告 + D2 default 别名兜底单资产情形 |
| `default` 别名与同包外部文档的多资产预期冲突 | 仅单资产表发别名；多资产需显式 |
| ~~剥掉 default 后运行时回退落空~~（实机证实） | **已转为设计：协议名不剥（D4）** |

**用户拍板（2026-08-03）**：D1 identity+sanitize；D4 导入默认保留原短名；D2 单资产自动
default 别名；D6 ref.ac 改发 animations 表。

# 动画/AC 的 molang 变量引用（read:/write: 命名端口 + AC 图化）

> 状态：已实现并实机验证（2026-08-08）。
> 动机：nodegraph-init-default-fold §3「变量作用域注意」——variable.* 是实体级
> 作用域，但引用点分散在动画与 AC 文件里，实体库图内不可见。本特性把这些
> 跨文件引用显式建模。
> 修订：取代 v6 的 `decl_variables` 桶端口（用户决策 2026-08-08：「一切节点都要有
> 类型且指代明确」）。**v8 再修订（用户决策 2026-08-08：「AC 应当类似
> RenderController 图化，而不是类似 Animation」）——ref.ac 撤除命名变量端口，
> AC 的变量引用由 AC 图自身承担（ac.state 的 on_entry/on_exit exec 链内 variable
> 节点即真实引用，AC 库本就是图）；动画无图形态，ref.animation 保留命名端口作为
> 唯一通道。** v6→v7 迁移剥除旧桶接线；v7→v8 迁移剥除 ref.ac 的 var_refs 快照、
> 指向其 read:/write: 端口的连线与失连 declvar- 节点，重新导入自动恢复为 v8 形态。

## 1. 需求

1. `ref.animation` 节点对该动画引用的全部 molang 变量有明确的输入端口。
2. ~~`ref.ac` 做与 `ref.rc`（v6 decl_* 通道）类似的处理~~（v8 废止）→ **AC 图化**：
   AC 的变量引用在 AC 图内以 variable 节点呈现（与 RC 的 rc.root 图同构），
   ref.ac 保持纯引用节点。
3. **每根连线的含义自明**：站在编辑器前能直接看出「这个动画 **读**还是**写**
   哪个变量」——端口按变量命名、按读/写分列。
4. 导入时自动从动画文档提取并接线，使实体库图呈现动画的变量引用面。

## 2. 设计

### 2.1 命名端口（仅 ref.animation）

- `ref.animation` 的输入端口集由节点选项快照 `var_refs`
  （`{"reads":[...],"writes":[...]}`，导入时写入）动态驱动（同 query.call 的
  签名驱动端口机制）：
- 每个读取变量一个 `read:<name>` 端口（label `v.<name>`）；
- 每个写入变量一个 `write:<name>` 端口（label `v.<name>`）——**v9 起为右侧输出**
  （左读右写，位置即语义，label 不带「读/写」文字）；注意端口必须拆进输入/输出
  两个 provider（曾把 write 混在输入 provider 里返回，端口全部渲架在左侧，2026-08-09 修复）；
- 同一变量既读又写 = 两个端口（如 edfatt 的节流器模式）。
- 端口 `PortType.VARIABLE` 单连接。`read:`/`write:` 前缀是声明通道标记
  （`NodeTypes.isVarRefPort`）：EmitSession 值发射计数与 GraphValidator
  UNCONNECTED_INPUT 检查统一豁免——声明通道不产生输出、空连线不是错误；
  REF_MISUSE（检查11）同样豁免 write: 输出边（2026-08-09 修复）。
- 每个（变量名, ref 节点）对一个 `variable` 节点（uid `declvar-<refUid>-<name>`），
  其 `out` 接 read 端口、ref 的 write 输出接其 `in`——一个节点可同时喂
  该变量的 read 与 write 两个端口。**不要按名全图共享**（2026-08-09 实证：
  共享节点被分层钉在远处，边横跨全图）；也不要在 wire() 里自排坐标——
  wire 在 ImportClosure 层运行（建库布局之后），位置由接线后的重布局按
  「声明通道共位子列」决定（GraphLayout：var-ref/写入边不参与最长路径分层，
  纯声明变量节点贴主 ref 左侧子列、纯写入变量节点贴写入方右侧子列）。
- VARIABLE→VARIABLE 同型直连，兼容矩阵无需修改。
- **编辑器往返**：快照无 NodeOptionDef（不进模型选项），LDLib2 模型往返会丢——
  `LibraryContext.varRefs` 侧表持有（同 variableScopes pattern）：加载登记、
  onDefinePorts 合并进端口视图、保存回写。
- **AC（图化）**：ref.ac 无变量端口。AC 库的 ac.state on_entry/on_exit exec 链里
  的 variable 节点就是变量引用的图形态；AC 图的变量声明由导入通用收集
  （collectVariables）自动补齐。

### 2.2 变量提取（MolangVariableRefs，decompile 包）

对原始 JSON 文档递归收集字符串值，逐个经 MolangTokenizer 切词，匹配
`variable.<name>` / `v.<name>`（IDENT DOT IDENT 三连）：

- 三连后紧跟单等号（EQUAL）= **写**（赋值左值）；`==` 是 EQUAL_EQUAL 不算；
- 其余出现 = **读**（比较、`??` 合并、函数实参、纯读取位）；
- `temp.`/`t.` 不收集（表达式局部）；语法残缺的字符串跳过（不阻断导入）。

统一从**原始文档**提取（不依赖图翻译保真度——AC 的 on_exit、transition 条件、
动画 keyframe molang 全部覆盖）。

### 2.3 导入接线（AnimationVariableDecls，decompile 包）

`ImportClosure.importWithClosure` 后处理：

1. 实体库主图中每个 `ref.animation`（identifier 非空）→ 注册表暂存的原始 schema
   文档 → LazyScan 回落（v8 起不再处理 ref.ac——AC 变量引用走 AC 图）；
2. `AnimationVariableDecls.wire`：ref 节点写 `var_refs` 快照 → 按（名, ref）对
   新增 variable 节点 + read:/write: 连线 → 新变量名并入图声明（type UNKNOWN、
   scope VARIABLE、无默认值）→ 产出新 GraphLibrary 重新注册。

### 2.3b AC 归位（2026-08-07 补充）

Bedrock 实体常把 AC 混声明在 `animations` 表（值为 `controller.animation.*`）。
导入起这类条目归位为 **ref.ac + entity.root.animation_controllers 端口**（此前一律
ref.animation，导致 ref.ac 的变量引用处理与 AC 库闭包导入都不触发）。导出时两端口
仍合写回 `animations` 表（ClientEntityAssembler D6），产物不变。animate 短名解析
（resolveAnimationRef）跨两表命中同一节点，不受影响。

AC 库闭包导入（ImportClosure）走**双通道**：注册表暂存的单条目控制器体（包成
`{animation_controllers: {<id>: ...}}` 文件文档）→ LazyScan 回落——brarchive 编码包
没有明文资源目录 JSON，纯扫描通道会全 miss（2026-08-08 实证修复；顺带修
KnownRefTables.collect() 对 ImmutableCollections.contains(null) 的潜伏 NPE）。

### 2.4 与折叠/内联的关系（结构独立，无需代码豁免）

命名端口接线在导入**后处理**（折叠/内联完成之后），且每个（名, ref）对使用
独立的 variable 节点实例——写入节点与声明节点不共享。折叠只删写入节点，
声明节点与连线原样保留（回归测试 foldLeavesDeclNodesIntact 守护，夹具已换
read: 端口）。注意顺序依赖：若未来把接线移进导入流水线，同名声明节点会触发
折叠的「同名第二个 variable 节点 → 绑定歧义」保护。

## 3. 前置 / 后置 / 不变量 / 异常

- 前置：闭包导入完成（实体库已注册）；包文档经资源桥可扫（包未挂载 = 提取为空，静默）。
- 后置：ref 节点带 `var_refs` 快照与命名端口；新增 variable 节点与 read:/write:
  连线；图声明补齐新变量名。
- 不变量：**不改变导出产物**——read:/write: 是纯元数据，组装器不读取这些端口；
  ref.animation/ref.ac 的导出语义（identifier/short_name）不变。
- 异常：文档缺失/语法残缺 = 该 ref 无快照（静默）；变量节点重名复用图声明合并去重。

## 4. 非目标

- temp.* 不声明（表达式局部）；
- 不做快照的实时刷新（ref 指向的 AC 图被编辑后，快照在下次导入时更新；
  被引内容不在管理器时以快照为准）；
- 不反向约束动画内容（不验证动画是否真的用了声明的变量——提取即真相）。

## 5. 验证

- 单测：提取（v./variable. 别名、temp 排除、残缺跳过、嵌套递归、**读/写分列**、
  `==` 非写、出现序）；接线（快照落位、read:/write: 命名端口与 label、声明合并、
  布局位置、空输入 no-op、ref.ac 跳过）；v6→v7 迁移剥除；v7→v8 迁移剥除 ref.ac；
  折叠对声明连线的结构豁免。
- 实机：悦灵闭包重导——edfatt 等变量按读/写接入引用它的动画 ref 命名端口
  （渲染层实证端口可见）、INIT_DEFAULT_FOLD 折叠数不减少、构建产物不变、
  编辑器打开正常、保存往返快照不丢。

# 对象变量与节点端口类型传播

> 状态：已实现（2026-08-14）。修订 nodegraph-visual-molang §2.1（端口类型表加 object）与
> nodegraph-variable-table §2.4（OBJECT 不再是 ANY 别名）。
> 依据：Mojang molang syntax-guide「Structs」——struct 是运行时真对象值（隐式定义、
> 整体赋值、任意嵌套）；临时 struct 限制按 compatibility-semantics-matrix 列为 Targeted，
> 不硬编码进核心引擎。

## 1. 需求

1. query.call / ternary 等节点的输出端口携带具体类型，不再一律 Object（ANY）。
2. molang 系统支持对象特性：`variable.qpptaw` 是对象，`variable.qpptaw.r/.x/.y/.z`
   是 int 成员（悦灵 qpptaw 场景）。
3. 变量面板按对象层级布局：object 父行 + 成员行缩进。
4. 类型推断区分 string 与 object。

## 2. 设计

### 2.1 域类型：PortType.OBJECT

- `PortType.OBJECT`（struct 对象，isValue）。兼容矩阵：OBJECT 仅接 OBJECT/ANY/UNKNOWN，
  与 number/string/array 全隔离；VARIABLE→OBJECT 隐式读放行（与既有身份语义一致）。
- `EvmTypeHandles` 重映射：OBJECT/ARRAY→内建 `TypeHandles.OBJECT`；ANY 改自定义 handle
  `eyelib:any`（显示 "Any"）。旧混淆根源：声明下拉的「object」曾存为 any。
- **迁移 v11→v12**：全部图的 VariableDecl.type==any → object（保留 group/default/scope）；
  子图接口参数同规则。旧文件中 any 声明的唯一来源是 object 下拉与导入产物。
- OBJECT 声明的默认值不导出 initialize（根上写标量会遮蔽成员；成员默认值声明在成员上）。

### 2.2 molang struct 运行时

- `MolangStruct`（type 包）：LinkedHashMap 成员；getPath/setPath 点分路径；
  setPath 对缺失/标量中间层按 BE 隐式语义重建；asFloat 0 / asBoolean false。
- `MolangScope` 结构化读写：`variable/temp/context/query/math` 根前缀取前两段为根键
  （`variable.qpptaw.r` → 根 `variable.qpptaw` + 路径 `r`），其余点名取首段；
  两段名（`variable.foo`）保持扁平零变化。根缺失才委托 parent（根级遮蔽）；
  temp struct 以根键登记，clearTempVariables 整棵清除。
- 整体赋值 = 引用共享（深拷贝 Deferred，见 compatibility-semantics-matrix）。
- emitter：BoundMemberAccessExpr 的非扁平 owner（call/index/arrow 结果）回退为
  emit owner + `MolangRuntimeSupport.memberAccess(owner, name)`；
  非扁平赋值目标返回 null（旧行为生成裸名垃圾键）。
- `MolangRuntimeSupport.wrapJavaResult` 支持 Map→MolangStruct（Java query 返回对象通道）。
- binder 箭头赋值脱糖：`a->b.c = v` ⇒ `a->(b.c = v)`（parser 把箭头目标解析为
  MemberAccess(…ArrowAccess(a,b)…)，hoistArrow 沿 owner 链上提）。
- `MolangType.OBJECT`：infer 识别 MolangStruct，format 输出 `{r: 1, x: 2}`。

### 2.3 query/math.call 返回类型端口

- `MolangReturnTypes`（client.nodegraph）：函数全名→PortType。优先级：手工覆盖表 →
  MolangMappingTree.findMethod 全变体反射（float/double→FLOAT、int 系→INT、boolean→BOOL、
  String→STRING、List/数组→ARRAY、Map→OBJECT；变体冲突→ANY）→ findField 静态字段 → ANY。
  .emolang 自定义函数恒 ANY。
- `EvmNodeBase.onDefinePorts` 对 QUERY_CALL/MATH_CALL 的 out 端口按函数返回类型建 handle；
  function 选项变更时 LDLib2 重跑 defineNode 自动刷新。域 NodeTypes 静态定义不变（ANY），
  验证器/codegen 不受影响。

### 2.4 ternary/null_coalesce 连线传播

- 域层 `NodeTypePropagation`：merge 规则——同型→该型、未知（null/ANY/UNKNOWN/VARIABLE
  身份/非值型）让位、number 混合→FLOAT、冲突→ANY。
- 编辑器 `EvmTypePropagation.refresh(GraphModel)`：全图定点迭代；variable 读出口取声明
  handle（显示层是 VARIABLE 身份）；`PortModel.setDataTypeHandle` 写回（UID 重键内部处理，
  domain Wire 存 (nodeUid, portId) 不受影响）。
- 钩子：`EvmGraphModel` 重写 addWire/removeWire → super 后 refresh，覆盖用户连线、
  翻译加载、undo 反序列化；VariablesPanel 类型变更把 refresh 放进 EvmUndo mutation。
- 旧图安全：translator createWire 不查 canAssignTo；收窄只影响新连线检查。

### 2.5 变量面板对象成员布局

- `VariableDisplayGrouping`（纯函数）：object 声明为父，点分最长前缀成员紧随其后按深度
  缩进（8px/级）；成员保持声明顺序、多级嵌套累进、孤儿点名平铺、筛选态平铺。
- 改名父不级联（扁平名语义）。

### 2.6 导入路径声明类型推断

- `VariableDeclInference.inferWrittenTypes`：exec.set_var target→variable.in 配对，
  value=连线源静态类型（ANY/身份=未知）或行内字面量（`MolangLiterals.portTypeOf`：
  bool→BOOL、整数→INT、其余数字→FLOAT、string→STRING）；strictMerge 归并（冲突粘性
  ANY→移除条目）；缺失→UNKNOWN。`completeObjectParents` 给点分名补 OBJECT 父链。
- `MolangVariableRefs` 保留成员链全名（`variable.qpptaw.r`→"qpptaw.r"）；
  var_ref 端口 id 把 '.' 编码为 ':'（LDLib2 端口 id 禁 '.'；molang 名不含 ':'，无碰撞）。

## 3. 前置/后置/不变量

- 不变量：两段名变量读写逐字节等价；域 GraphData 不持久化解析后端口类型；
  codegen 确定性不受影响。
- 后置：object 变量面板分组缩进显示；query.call out 随函数切换即时换型；
  ternary 连线/断线即时传播/回退。

## 4. 非目标

- struct 深拷贝赋值语义（Deferred）。
- temp struct 的 BE 版本化限制（Targeted 策略，不硬编码）。
- 行内字面值参与 ternary 传播（非目标）。

## 5. 验证

- 单测：MolangStructTest 15 例（官方文档 5 等价表达式、scope 兼容、箭头脱糖）；
  PortTypeTest objectIsStructOnly；GraphMigrationsTest v12 3 例；MolangReturnTypesTest 4 例；
  NodeTypePropagationTest 6 例；VariableDisplayGroupingTest 5 例；VariableDeclInferenceTest 9 例；
  MolangVariableRefsTest/AnimationVariableDeclsTest/JsonGraphImportersTest 增补。
- 实机（1.20.1）：面板布局截图（qpptaw=object + r/x/y/z=int 缩进）；query.get_name→out STRING；
  ternary const.string→STRING、冲突→ANY、断线→回退 INT（variable 声明类型路径）；
  struct 运行时 `variable.qpptaw={}; .r=5` 读写/MolangStruct/OBJECT 格式化。

# SubTask5: Molang 求值链分配消除

## 背景

SubTask3 后剩余热点（eyelib self-time top）：
- **MolangRuntimeSupport: 1844ms**（resolveCall 分配压力，#1）
- **VariantSelector: 1648ms**（selectQueryVariant 5×stream，#2）
- Molang$Expr$b03dc783.evaluate: 1140ms（字节码求值，本次不动）
- MolangScope$1.get: 856ms

Molang 求值链合计 ~4600ms（7.7%），是当前最大优化目标。

## 根因

### VariantSelector.selectQueryVariant（molang/mapping/api/VariantSelector.java:24-74）
纯函数（tree+name+callShape+hostRoles → FunctionInfo），但执行 **5 次连续 stream pipeline**：
arityCandidates → compatibilityCandidates → hostRoleCandidates → specificityCandidates → priorityCandidates，
每次 `.stream().filter().toList()` 分配新 ArrayList。共 5 次分配 + 5 次 spliterator 创建。

### MolangRuntimeSupport.resolveCall（molang/compiler/MolangRuntimeSupport.java:79-115）
每次调用：
1. `CompileContext.defaults()` → `new CompileContext(mappingTree, NORMAL, Set.of())`（每次新建 record + Set.of()）
2. `new ArrayList<>()` ×2（visibleArgs + callShape）
3. `computeAvailableHostRoles` → `EnumSet.noneOf(...)`（每次新建）
4. selectQueryVariant（5×stream）
5. invokeMethod → `new Object[paramCount]`

`MolangMappingRegistries.mappingTree()` 是 volatile 字段直接返回（稳定实例），CompileContext.defaults() 的包装纯属浪费。

## 改动（三个分配消除，语义保持）

### Opt-A: VariantSelector.selectQueryVariant 单次遍历化
- 5 步 stream filter → 单次 for 遍历 + (specificity, priority) 在线打分
- 等价语义：最高 specificity 中最高 priority 的最后一个候选
- 消除 5 个 ArrayList + stream pipeline overhead
- 纯算法等价，不改 API

### Opt-B: MolangRuntimeSupport mappingTree 直接引用
- `CompileContext.defaults().mappingTree()` → `MolangMappingRegistries.mappingTree()`
- 消除每次 new CompileContext record + Set.of()
- mappingTree 是 volatile 字段读，replaceAll 时自动拿到新实例（语义保持）

### Opt-C: computeAvailableHostRoles 常量化
- 预建两个不可变 Set（HOST_ROLES_FULL / HOST_ROLES_MINIMAL）
- 消除每次 EnumSet.noneOf + add
- selectQueryVariant 只读 contains，不可变 Set 安全

## 规格说明

### 不变量
- selectQueryVariant(tree, name, callShape, hostRoles) 返回值与原实现逐字节等价
- resolveCall/resolveMemberAccess 行为不变

### 后置条件
- VariantSelector 内部不再分配 ArrayList（除 methodData.functionInfos() 本身的 List）
- MolangRuntimeSupport.resolveCall 不再创建 CompileContext record

### 异常行为
- selectQueryVariant 仍返回 null（无匹配）/ 原 priorityCandidates.last()
- 不抛新异常

### 副作用
- 无（纯性能优化，无状态变更）

## 验收
- 编译 exit 0（:1.20.1:compileJava）
- spark 复测：VariantSelector self < 600ms（基线 1648ms），MolangRuntimeSupport self < 1200ms（基线 1844ms）
- 无效则回滚

## 涉及文件
- `src/main/java/io/github/tt432/eyelib/molang/mapping/api/VariantSelector.java`
- `src/main/java/io/github/tt432/eyelib/molang/compiler/MolangRuntimeSupport.java`

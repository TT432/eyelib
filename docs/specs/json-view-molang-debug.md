# 规格：实体 JSON 查看器与 Molang 调试器

状态：已实现（2026-08-02）。关联：ADR-0021（节点图）、nodegraph-visual-molang.md（节点图规格）。

## 1. 目标

1. **JSON 读取**：按 id 读取并展示特定 Entity（行为实体，`minecraft:entity`）与
   ClientEntity（客户端实体，`minecraft:client_entity`）的 JSON。
2. **Molang 调试**：对游戏内实体做 molang 运行时调试——scope 变量监视、
   watch 表达式逐帧求值、类型化显示。
3. **用户面向类型**：molang 运行时是动态类型（数字一律 float、bool 即 0/1），
   面向用户必须呈现类型：`number:{float,int,bool} / string`（另有 array/dynamic）。

## 2. 用户面向类型系统（`molang.type.MolangType`）

- 值：`FLOAT / INT / BOOL / STRING / ARRAY / DYNAMIC`；`isNumber()`=前三者。
- 运行时推断 `infer(MolangObject)`：只能可靠区分 string/array/number；
  数字恒推 FLOAT。**int 与 bool 在运行时不存在，必须来自声明**（端口/变量声明、
  watch 显示类型选择）。
- 显示名：`number<float>` / `number<int>` / `number<bool>` / `string` / `array` / `dynamic`。
- 格式化：float 去尾零；int 向零取整；bool 显示 `true (1)`（保留底层值）；string 单引号；
  array 递归。

### 2.1 节点图 PortType 对齐

- `PortType` 新增 `INT`；number 三子类型（FLOAT/INT/BOOL）在 `isAssignableTo`
  中双向互通（molang 运行时同值域）。序列化小写 `"int"`，旧图格式兼容。
- 节点目录：`const.int` 节点（整数字面量，codegen 复用 formatNumber 去 .0）；
  `exec.loop` 的 count 端口 FLOAT→INT。
- 编辑器映射：ldlib1 INT→`Integer.class`（三向互通转换器 + `number<…>` 显示名）；
  ldlib2 INT→内置 `TypeHandles.INT`。常量配置器按声明端口类型分派（不再按 JSON 字面量种类）。

## 3. JSON 查看器

### 3.1 功能

- 两个标签页：客户端实体（ClientEntityManager）/ 行为实体（BehaviorEntityManager）。
- id 列表（可过滤）+ JSON 文本区（滚动、语法着色）。

### 3.2 前置条件 / 后置条件 / 不变量

- 前置：对应 manager 已加载（资源 reload 或 addon 加载完成后）。
- 后置：无（只读功能，不改变任何运行时状态）。
- 不变量：JSON 由 CODEC `encodeStart(JsonOps)` 生成，与加载路径互逆
  （`BrClientEntity.CODEC` / `BehaviorEntity.CODEC`，encode 失败→显示错误而非崩溃）。
- 异常：id 不存在或 encode 失败 → 界面显示「无定义」/错误文本，不抛到界面层。

## 4. Molang 调试器

### 4.1 功能

- 目标选择：当前 client level 实体列表（可过滤、可刷新）。
- scope 监视：当前目标的 `MolangScope` 键值（variable/temp/context，含 parent 链标注），
  逐帧刷新，每行显示 `名称 / 类型 / 值`。
- watch 表达式：用户输入 molang 表达式 + 显示类型（auto/float/int/bool/string），
  逐帧对当前目标 scope 求值；错误红字；可删除。

### 4.2 前置条件 / 后置条件 / 不变量

- 前置：在世界里；目标实体存在。eyelib 渲染的实体经 RenderData capability 持有
  MolangScope；无 scope 的目标优雅降级（提示「无 molang 上下文」）。
- 后置：无（求值只读；不写 scope、不改实体状态）。watch 求值产生的异常被捕获显示。
- 线程：全部求值发生在渲染线程（屏幕 render 路径），与 scope 的正常读写同线程，
  与 `docs/molang/molang-scope-eval-debugging.md` 记载的 /eval HTTP 线程限制相区别。
- 表达式编译走现有管线（常量折叠 + 编译缓存），不在调试器内新建编译路径。

## 5. 入口

管理器屏幕（EyelibManagerScreen）新增入口按钮。两屏幕均基于 eyelib 自有
UIScreen 框架（不依赖 LDLib），三个版本（1.20.1/1.21.1/26.1.2）可用。

## 6. 非目标

- 不做 molang 断点/单步（编译到 JVM 字节码，无解释器插桩点；需要时另行立项改
  MolangBytecodeEmitter）。
- 不写回 JSON（只读查看；编辑走现有导入/节点图构建管线）。
- watch 列表不持久化。

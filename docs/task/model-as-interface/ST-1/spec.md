# ST-1 规格说明：Model 接口化核心 + 调用点迁移 + 单测

## 范围

把 `record Model` 改写为 `interface Model`，新增 package-private 的 `final class SimpleModel` 作为默认实现，
抽出 `IndexInitializer` 承担派生索引计算，迁移所有 `new Model(...)` 调用点到 `Model.of(...)` 工厂，
并补 spec-based 单测。

**ST-1 完成后**：worktree 编译通过、NullAway 通过、单测全绿。代码可独立提交（一个 commit）。

## 文件改动清单

### 新增

| 路径 | 职责 | 可见性 |
|---|---|---|
| `src/main/java/io/github/tt432/eyelib/model/SimpleModel.java` | `Model` 的默认实现，持有 5 个 final 字段；canonical 构造器调用 `IndexInitializer`；提供 5 个 `static of(...)` 工厂 | package-private（`final class`，无 `public`） |
| `src/main/java/io/github/tt432/eyelib/model/IndexInitializer.java` | 派生索引计算（toplevelBones / Bone.children / ModelLocator.groupLocatorMap / GroupLocator.children） | package-private（`final class`，无 `public`） |
| `src/test/java/io/github/tt432/eyelib/model/ModelIndexTest.java` | spec-based 单测 | public class |

### 改写

| 路径 | 改动 |
|---|---|
| `src/main/java/io/github/tt432/eyelib/model/Model.java` | `@With record Model` → `public interface Model`；5 个 accessor 方法签名；嵌套 record（Bone/Cube/Face/Vertex/TextureMesh/Face.Rect）原样保留为接口嵌套类型；`CODEC` 与 5 个 `static of(...)` 工厂移到接口里，委托给 `SimpleModel` |

### 迁移（5 处调用点）

| 调用点 | 改动 |
|---|---|
| `Model.CODEC`（自身，方法引用 `Model::new`） | 改为 `SimpleModel::new` 或等价 lambda |
| `model/Models.java:42`（`new Model(name, allBones, locator, EMPTY_VISIBLE_BOX)` 4-arg） | `Model.of(name, allBones, locator, EMPTY_VISIBLE_BOX)` |
| `model/Models.java:100`（同上 4-arg） | 同上 |
| `model/ImportedModelBuilder.java:23`（`new Model(name, allBones, visibleBox)` 3-arg） | `Model.of(name, allBones, visibleBox)` |
| `client/model/ModelPartModel.java:43`（`new Model(name, allBones)` 2-arg） | `Model.of(name, allBones)` |

## 不变量（ST-1 完成后必须成立）

### I-1: Model 是纯接口契约
```java
public interface Model {
    String name();
    Int2ObjectMap<Model.Bone> toplevelBones();
    Int2ObjectMap<Model.Bone> allBones();
    ModelLocator locator();
    VisibleBox visibleBox();
    // ... CODEC, of(...), 嵌套 record
}
```
不允许在 Model 接口里持有任何状态字段（除了 `static final` 常量与 CODEC）。

### I-2: SimpleModel 构造后派生索引一致
对任意 `SimpleModel m = SimpleModel.of(name, allBones, locator, visibleBox)`：
- `m.toplevelBones()` == `{ id → bone | allBones[id].parent() == -1 }`
- 对每个 `bone ∈ m.allBones().values()`：`bone.children()` == `{ child.id() → child | allBones[child.id()].parent() == bone.id() }`
- `m.locator().groupLocatorMap()` 的 keySet == `m.toplevelBones().keySet()`
- 对每个 `topBone ∈ m.toplevelBones().values()`：`m.locator().groupLocatorMap().get(topBone.id()) == topBone.locator()`（引用相等）
- 对每个 `bone ∈ m.allBones().values()` 且 `bone.children()` 非空：对每个 `child ∈ bone.children().values()`：`bone.locator().children().get(child.id()) == child.locator()`（引用相等）

### I-3: locator 索引重建是幂等的
若传入的 `locator.groupLocatorMap` 已有内容（CODEC 路径），SimpleModel 构造器仍会用 `bone.locator()` 覆盖。
覆盖结果与原值引用相等（假设数据自洽）。这是统一构造器路径的必要副作用。

### I-4: Bone 字段访问统一走 accessor
原 `record Model` 的构造器内通过 nest-mate 字段直访（`bone.parent`, `bone.locator`）。
迁移到 `IndexInitializer`（独立 class，非 Bone 的 nest-mate）后，**必须**用 accessor（`bone.parent()`, `bone.locator()`）。
record accessor 返回同一引用，语义不变。

### I-5: 字段可变性保持
`allBones`、`toplevelBones`、每个 `Bone.children`、`ModelLocator.groupLocatorMap`、每个 `GroupLocator.children` 的 Map **内容是可变的**。
这是当前 record Model 的行为（fastutil Int2ObjectOpenHashMap 是 mutable）。ST-1 保持此行为不变。
SimpleModel 的字段引用是 final，但 Map 内容可被外部修改——与 record 行为一致。

## 前置条件

- worktree 在 `refactor/model-as-interface` 分支
- HEAD = `ba4d14c8`（commit before ST-1）
- `Model.java` 当前是 `@With record Model`，5 components
- 5 处调用点（详见上「迁移」表）均存在且编译通过
- `src/test/java/io/github/tt432/eyelib/model/` 目录不存在（需创建）

## 后置条件

### 必须满足
- `eyelib_debug_build` 退出码 0
- `eyelib_debug_nullaway` 无报错
- `eyelib_debug_test` 全绿（含新增 ModelIndexTest）
- 没有任何 `new Model(...)` 调用残留（grep 验证）
- 没有 `new SimpleModel(...)` 在 model 包外残留（SimpleModel package-private）

### 不在 ST-1 范围
- `ModelPartModel implements Model`（ST-2 做）
- `MODULES.md` 重生成（ST-3 做）
- `package-info.java` 更新（如果需要，ST-3 做；当前 package-info 描述是「模型定义模块」，仍然准确）
- commit（ST-3 做）

## 异常行为

保持与原 record 一致，不做主动修复：

| 输入 | 当前行为 | ST-1 行为 |
|---|---|---|
| allBones 中某 bone 的 parent 引用不存在的 id | NPE（`allBones.get(parent)` 返回 null，`.children.put` NPE） | 保留 |
| allBones 中存在 parent 循环 | StackOverflowError（initLocator 递归无终止） | 保留 |
| locator 入参为 null | NullAway 编译期拒绝（@NullMarked） | 保留 |
| allBones 入参为 null | NullAway 编译期拒绝 | 保留 |

## 副作用（与原 record 行为一致）

SimpleModel 构造器执行：
1. **修改入参 allBones 中每个 Bone 的 children 字段对应的 Map**：根据 parent 关系，把每个 child bone 加到 parent 的 children Map 里
2. **修改入参 locator.groupLocatorMap**：把每个 toplevelBone 的 locator 加入
3. **修改每个 toplevelBone 及其子孙 Bone.locator 字段对应的 GroupLocator.children Map**：递归建立 child bone 的 locator 索引

调用方在调用 `Model.of(...)` 后，传入的 allBones/locator 已被填充——与原 record 行为一致。

## 测试规格（ModelIndexTest）

### T-1: toplevelBones 派生正确
构造 allBones：bone 0 (parent=-1), bone 1 (parent=0), bone 2 (parent=0), bone 3 (parent=1)。
`Model.of("t", allBones)` 后断言：
- `m.toplevelBones().keySet()` == {0}
- `m.toplevelBones().get(0).children().keySet()` == {1, 2}
- `m.allBones().get(1).children().keySet()` == {3}

### T-2: locator 索引派生正确（ImportedModelBuilder 路径）
构造 allBones 同 T-1，每个 Bone 自带 `new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of())` locator。
`Model.of("t", allBones)` 后断言：
- `m.locator().groupLocatorMap().keySet()` == {0}
- `m.locator().groupLocatorMap().get(0) == m.toplevelBones().get(0).locator()`（引用相等）
- `m.locator().groupLocatorMap().get(0).children().keySet()` == {1, 2}
- `m.locator().groupLocatorMap().get(0).children().get(1).children().keySet()` == {3}
- `m.locator().groupLocatorMap().get(0).children().get(1) == m.allBones().get(1).locator()`（引用相等）

### T-3: locator 索引重建幂等（CODEC 路径）
先构造一个"已填充"的 ModelLocator（groupLocatorMap 已有内容，但和 toplevelBones.locator 是不同实例）。
传给 `Model.of(name, allBones, locator, visibleBox)` 4-arg 工厂。
断言构造后 `groupLocatorMap` 中的 GroupLocator **引用**已被替换为 `bone.locator()`（引用相等）。

### T-4: CODEC 往返
编码一个 SimpleModel → JSON → 解码 → 断言所有 accessor 字段值相等（包括嵌套 bone 树结构）。

### T-5: Bone 字段访问通过 accessor
间接验证 I-4：T-1~T-3 的断言全部用 accessor（`bone.parent()`、`bone.children()`、`bone.locator()`），编译通过即说明 IndexInitializer 用了 accessor。

## 设计决策记录

### D-ST1-1: 嵌套 record 保留为接口嵌套类型
**决策**：`Bone/Cube/Face/Vertex/TextureMesh/Face.Rect` 作为 `interface Model` 的嵌套 record，类型名仍是 `Model.Bone` 等。
**理由**：调用点零改动（大量使用 `Model.Bone` 类型引用）。嵌套 record 与接口 nest mate，CODEC 仍能正常工作。
**代价**：Model 接口长度仍较大（~200 行），但嵌套类型是数据定义，符合"接口 + 数据"的分层。

### D-ST1-2: IndexInitializer 始终执行 locator 重建
**决策**：无论 locator 入参是否预填，构造器都执行 #4 风格的递归 `initLocator(bone)`。
**理由**：统一构造器路径，消除 #2/#4 的分支语义。CODEC 路径下多一次重建（幂等），ImportedModelBuilder 路径下从空 locator 重建。
**代价**：CODEC 解码出的 `groupLocatorMap` 内容被丢弃，用 `bone.locator()` 覆盖。理论上两者一致（数据自洽），但若 CODEC 解出的 groupLocatorMap 与 bone 树不一致（数据 bug），SimpleModel 会"修正"它——这是 invariant 保障，不是 bug 掩盖。

### D-ST1-3: Bone 字段访问从 nest-mate 直访改为 accessor
**决策**：`IndexInitializer` 作为独立 class，访问 Bone 字段全部用 accessor。
**理由**：IndexInitializer 不再是 Bone 的 nest mate（不在 Model 接口内部），无法直访 private 字段。record accessor 返回同一引用，语义不变。
**代价**：代码字面量略长（`bone.parent()` vs `bone.parent`），无运行时开销（record accessor 是 final 字段直返）。

### D-ST1-4: SimpleModel 提供 5 个 static of(...) 工厂
**决策**：`SimpleModel.of(...)` 重载对应原 5 个 record 构造器签名，`Model.of(...)` 委托。
**理由**：迁移调用点时只需 `new Model(...)` → `Model.of(...)`，签名零学习成本。
**代价**：工厂方法数量较多，但每个都对应明确语义。

### D-ST1-5: EMPTY_VISIBLE_BOX 和 VECTOR3F_CODEC 留在 Model 接口
**决策**：`EMPTY_VISIBLE_BOX` 公开常量、`VECTOR3F_CODEC` 私有常量都保留在 `interface Model` 内。
**理由**：`EMPTY_VISIBLE_BOX` 是公共 API（外部调用点 `Models.java` 用到），`VECTOR3F_CODEC` 是 `TextureMesh.CODEC` 用到——两者都依赖嵌套类型，留在接口里最自然。

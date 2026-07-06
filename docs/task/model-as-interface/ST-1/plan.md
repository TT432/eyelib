# ST-1 执行计划

> 规格见 `spec.md`。本计划面向具体执行，步骤明确。

## 步骤总览

```
Step 0: 环境检查
Step 1: 写 SimpleModel.java
Step 2: 写 IndexInitializer.java
Step 3: 重写 Model.java 为 interface
Step 4: 迁移 5 处调用点
Step 5: 写 ModelIndexTest
Step 6: 闸门验证（编译 → NullAway → 单测）
Step 7: grep 残留检查
```

## Step 0: 环境检查

- [ ] `git -C E:\_ideaProjects\qylEyelib_model_interface status` 应是 clean（只有 docs/task 未跟踪）
- [ ] `git -C E:\_ideaProjects\qylEyelib_model_interface log --oneline -1` 应是 `ba4d14c8`
- [ ] 当前 active version 是 `1.20.1`（读 `stonecutter.gradle`）

## Step 1: 写 SimpleModel.java

路径：`src/main/java/io/github/tt432/eyelib/model/SimpleModel.java`

骨架：
```java
package io.github.tt432.eyelib.model;

// imports ...

final class SimpleModel implements Model {
    private final String name;
    private final Int2ObjectMap<Bone> toplevelBones;
    private final Int2ObjectMap<Bone> allBones;
    private final ModelLocator locator;
    private final VisibleBox visibleBox;

    SimpleModel(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox) {
        this.name = name;
        this.allBones = allBones;
        this.locator = locator;
        this.visibleBox = visibleBox;
        this.toplevelBones = new Int2ObjectOpenHashMap<>();
        IndexInitializer.fillIndices(allBones, toplevelBones, locator);
    }

    // 5 个 static of(...) 工厂，签名对应原 5 个 record 构造器

    @Override public String name() { return name; }
    @Override public Int2ObjectMap<Bone> toplevelBones() { return toplevelBones; }
    @Override public Int2ObjectMap<Bone> allBones() { return allBones; }
    @Override public ModelLocator locator() { return locator; }
    @Override public VisibleBox visibleBox() { return visibleBox; }
}
```

5 个工厂签名：
1. `of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox)` → 4-arg 主入口
2. `of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator)` → 委托 #1 + EMPTY_VISIBLE_BOX
3. `of(String name, Int2ObjectMap<Bone> allBones, VisibleBox visibleBox)` → 创建空 ModelLocator，委托 #1
4. `of(String name, Int2ObjectMap<Bone> allBones)` → 委托 #3 + EMPTY_VISIBLE_BOX
5. canonical 5-arg 不对外暴露（只用于 CODEC 解码，4-arg 路径覆盖）

**注意**：原 record 有 5 个构造器但 CODEC 用的是 4-arg（#2）。canonical 5-arg 是 record 自动生成的，实际无人调用。
SimpleModel 只需 5 个 accessor + 4 个 `of(...)` 工厂 + 1 个构造器。

## Step 2: 写 IndexInitializer.java

路径：`src/main/java/io/github/tt432/eyelib/model/IndexInitializer.java`

```java
package io.github.tt432.eyelib.model;

// imports ...

final class IndexInitializer {
    private IndexInitializer() {}

    static void fillIndices(
            Int2ObjectMap<Model.Bone> allBones,
            Int2ObjectMap<Model.Bone> toplevelBones,
            ModelLocator locator) {
        // 派生 toplevelBones 和 Bone.children（来自原 #2 构造器）
        allBones.forEach((integer, bone) -> {
            if (bone.parent() == -1) {
                toplevelBones.put(integer, bone);
            } else {
                allBones.get(bone.parent()).children().put(bone.id(), bone);
            }
        });

        // 派生 ModelLocator.groupLocatorMap 和 GroupLocator.children（来自原 #4 构造器）
        for (Int2ObjectMap.Entry<Model.Bone> entry : toplevelBones.int2ObjectEntrySet()) {
            locator.groupLocatorMap().put(entry.getIntKey(), entry.getValue().locator());
            initLocator(entry.getValue());
        }
    }

    private static void initLocator(Model.Bone bone) {
        var groupLocator = bone.locator();
        for (Int2ObjectMap.Entry<Model.Bone> entry : bone.children().int2ObjectEntrySet()) {
            groupLocator.children().put(entry.getIntKey(), entry.getValue().locator());
            initLocator(entry.getValue());
        }
    }
}
```

**关键差异（vs 原 record）**：
- `bone.parent` → `bone.parent()`
- `bone.id` → `bone.id()`
- `bone.locator` → `bone.locator()`
- `bone.children` → `bone.children()`
- `allBones.get(bone.parent).children.put(...)` → `allBones.get(bone.parent()).children().put(...)`

## Step 3: 重写 Model.java 为 interface

路径：`src/main/java/io/github/tt432/eyelib/model/Model.java`

```java
package io.github.tt432.eyelib.model;

// imports ...

public interface Model {
    VisibleBox EMPTY_VISIBLE_BOX = VisibleBox.EMPTY;
    Codec<Vector3f> VECTOR3F_CODEC = ImporterCodecs.VECTOR3F;  // 给 TextureMesh.CODEC 用，可保留 private

    Codec<Model> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        Codec.STRING.fieldOf("name").forGetter(Model::name),
        GlobalBoneIdHandler.map(Bone.CODEC).fieldOf("all_bones").forGetter(Model::allBones),
        ModelLocator.CODEC.fieldOf("locator").forGetter(Model::locator),
        VisibleBox.CODEC.optionalFieldOf("visible_box", EMPTY_VISIBLE_BOX).forGetter(Model::visibleBox)
    ).apply(ins, SimpleModel::of));  // 4-arg of 工厂

    String name();
    Int2ObjectMap<Bone> toplevelBones();
    Int2ObjectMap<Bone> allBones();
    ModelLocator locator();
    VisibleBox visibleBox();

    // 5 个 static of(...) 工厂，全部委托 SimpleModel.of
    static Model of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox) {
        return SimpleModel.of(name, allBones, locator, visibleBox);
    }
    static Model of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator) {
        return SimpleModel.of(name, allBones, locator);
    }
    static Model of(String name, Int2ObjectMap<Bone> allBones, VisibleBox visibleBox) {
        return SimpleModel.of(name, allBones, visibleBox);
    }
    static Model of(String name, Int2ObjectMap<Bone> allBones) {
        return SimpleModel.of(name, allBones);
    }

    // 嵌套 record（原样保留，删除 @With 注解——@With 只对 record 有意义，对接口嵌套 record 仍可用但无副作用）
    @With
    record Bone(...) { /* 原样 */ }

    @With record Cube(...) { /* 原样 */ }
    @With record Face(...) { /* 原样 */ }
    @With record Vertex(...) { /* 原样 */ }
    @With record TextureMesh(...) { /* 原样 */ }
}
```

**细节**：
- 接口里的 `private static final` 字段是 Java 17 支持的
- VECTOR3F_CODEC 可改为 `private static final`，因为它只在 TextureMesh.CODEC 内部用
- 所有嵌套 record 的 `@With` 注解保留（lombok @With 对 record 生成 with$... 方法，对接口嵌套 record 仍有效）

**风险**：lombok @With 在 interface 内嵌套 record 上是否工作？需要验证。如果不工作，把 @With 去掉——但要看哪些地方用了 with 方法。

让我在执行时 grep `Bone.with`/`Cube.with` 等。如果没有 with 调用，去掉 @With 最干净。

## Step 4: 迁移 5 处调用点

### 4a: `Model.CODEC` 自身（Step 3 已处理）
方法引用从 `Model::new`（4-arg 构造器）改为 `SimpleModel::of`（4-arg of 工厂）。
**注意**：`Model::new` 已经不可用（接口无构造器），所以这里必须改。

### 4b: `model/Models.java:42`
读取 Models.java 上下文，确认 line 42 是 `new Model(...)`。
改为 `Model.of(...)`。

### 4c: `model/Models.java:100`
同 4b。

### 4d: `model/ImportedModelBuilder.java:23`
改为 `Model.of(...)`。

### 4e: `client/model/ModelPartModel.java:43`
改为 `Model.of(...)`。

## Step 5: 写 ModelIndexTest

路径：`src/test/java/io/github/tt432/eyelib/model/ModelIndexTest.java`

按 spec.md 的 T-1~T-5 实现。

**辅助**：构造测试用 Bone 时，用最小构造器：
```java
new Model.Bone(
    id, parent,
    new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(),  // pivot, rotation, position, scale
    null,  // binding
    new Int2ObjectOpenHashMap<>(),  // children
    List.of(),  // cubes
    new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of())  // locator
)
```
（用 10-arg 简化构造器，剩 reset/material/textureMeshes 走默认值）

**T-4 CODEC 往返**：
- 构造 SimpleModel → 编码到 JsonElement → 解码回 Model → 断言
- 用 `Codec.encode/parse`，不需要真实 JSON 文件

## Step 6: 闸门验证

按顺序执行：

1. `eyelib_debug_build`（编译 main + test）
2. `eyelib_debug_nullaway`（NullAway + ErrorProne）
3. `eyelib_debug_test`（带 filter `io.github.tt432.eyelib.model.*`）

任一失败：读 `build/_mcp_gradle_out.txt` / `_mcp_gradle_err.txt` 诊断。

## Step 7: grep 残留检查

```bash
# 在 worktree 根
grep -rn "new Model(" src/main/java src/test/java
# 应无结果（除非有 Model. 嵌套类的 new，但 record Model 不该被 new）

grep -rn "new SimpleModel(" src/main/java
# 应只在 SimpleModel.java 内部出现（如果有），其他位置不应有
```

预期：5 处 `new Model(...)` 全部消失，替换为 `Model.of(...)`。

## 风险与应对

### R-1: lombok @With 在 interface 嵌套 record 上不工作
**应对**：grep 全仓库查 `.with` 调用对 Model.Bone/Cube/Face/Vertex/TextureMesh 的影响。
如果 with 方法被调用，保留 @With 注解（仍可能工作，因为 lombok 处理 record 与外围类型无关）；
如果没调用，直接去掉 @With。

### R-2: NullAway 对 interface Model 的嵌套 record 报警
**应对**：嵌套 record 在 @NullMarked 包内，所有 @Nullable 已显式标注。预期无新增报错。
如果有报错，先查根因（不要用注解静默消除）。

### R-3: CODEC 方法引用 `SimpleModel::of` 推导失败
**应对**：Java 方法引用需要精确签名匹配。4-arg `of(String, Int2ObjectMap, ModelLocator, VisibleBox)` 应能匹配 `RecordCodecBuilder.apply` 期待的 4-arg Function。
若失败，改用 lambda：`.apply(ins, (name, allBones, locator, visibleBox) -> SimpleModel.of(name, allBones, locator, visibleBox))`。

### R-4: ModelPartModel.createModel:43 用了 2-arg `new Model(name, allBones)`
**应对**：对应 `Model.of(name, allBones)`（2-arg）。但 SimpleModel 的 2-arg of 内部要委托给 4-arg，会创建空 ModelLocator。
原 record 的 2-arg 构造器 #5 委托 #4，#4 创建空 ModelLocator。语义一致。

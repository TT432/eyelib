# Model 抽成接口 — 设计方案

> 状态：**待审批**
> 范围：`io.github.tt432.eyelib.model.Model`（及其嵌套类型）
> worktree：`E:\_ideaProjects\qylEyelib_model_interface`，分支 `refactor/model-as-interface`

---

## 1. 现状梳理

### 1.1 当前 `Model` 的形态

`src/main/java/io/github/tt432/eyelib/model/Model.java` 当前是一个 `@With record`，主构造器（record canonical）5 个组件：

```java
public record Model(
    String name,
    Int2ObjectMap<Bone> toplevelBones,   // ← 派生字段，被 record 强制为 component
    Int2ObjectMap<Bone> allBones,
    ModelLocator locator,
    VisibleBox visibleBox
)
```

同时定义了 4 个用户自定义构造器（绕过 canonical）：

| # | 签名 | 用途 | 是否计算 toplevelBones |
|---|------|------|------------------------|
| 1 | `(name, toplevelBones, allBones, locator, visibleBox)` | canonical（record 自动） | 否（直接信任入参） |
| 2 | `(name, allBones, locator, visibleBox)` | CODEC + `Models.add/sub` | **是**（遍历 allBones 填充 toplevelBones + bone.children） |
| 3 | `(name, allBones, locator)` | 便捷 | 委托给 #2 |
| 4 | `(name, allBones, visibleBox)` | `ImportedModelBuilder.build` | **是**（同时初始化 `locator.groupLocatorMap` + locator.children） |
| 5 | `(name, allBones)` | `ModelPartModel.createModel` | 委托给 #4 |

### 1.2 toplevelBones、bone.children、locator.children 的派生性质

这三个数据都是从 `allBones` 派生出来的索引：

- `toplevelBones`：`allBones` 中 `parent == -1` 的子集
- `bone.children`：`allBones` 中 parent 指向当前 bone 的子项
- `locator.groupLocatorMap` / `locator.children`：按 bone 树拓扑镜像

`Model.CODEC` 只编码 4 个输入字段（`name / all_bones / locator / visible_box`），**不编码** `toplevelBones`，已确认它是派生。

### 1.3 调用面统计（main 源码）

| 访问器/构造 | 调用次数（main） | 关键位置 |
|---|---|---|
| `new Model(...)`（4 个构造器变体） | 5 处 | `ImportedModelBuilder:23`、`Models:42/100`、`ModelPartModel:43`、（CODEC 反射） |
| `.toplevelBones()` | 7 处 | `ModelVisitor:34`、`ModelRenderer:32`、`TwoSideModelBakeInfo:46/63`、`EmissiveModelBakeInfo:51/64`、`Models:38` |
| `.allBones()` | 6 处 | `RenderControllerEntry:156/467`、`Models:34/90/91`、`RenderControllerRuntime:47` |
| `.locator()` / `.visibleBox()` / `.name()` | 多处 | CODEC getter + 渲染管线 |
| 嵌套类型 `Model.Bone/Cube/Face/Vertex/TextureMesh` | 大量 | 全渲染/导入模块 |

---

## 2. record 形态"丢失了什么信息"

> 你说"抽成 record 反而丢失信息"。下面把"信息"逐类拆开，每条都附代码证据。

### 2.1 「输入 vs 派生」的语义边界丢失（最严重）

**问题**：record 强制每个字段都是同等地位的 component，全部对外暴露为访问器，且全部出现在 canonical 构造器签名中。

**代码证据**：

- `toplevelBones` 是从 `allBones` 派生的索引，但 canonical 构造器签名把它和 `allBones` 并列为入参。
- 这意味着**任何调用方都能通过 canonical 构造器（或 `withToplevelBones(...)`）传入一个与 `allBones` 不一致的 `toplevelBones`**，破坏了"派生 = f(输入)"的不变量。
- 4 个用户自定义构造器各自重复了"遍历 allBones 填充派生索引"的逻辑（#2、#4 各一份，互相不一致：#4 还会顺便初始化 locator children，#2 不会），这是因为 record 不允许在构造完成后修改字段，只能通过构造器链绕路。

**信息丢失的精确表述**：类型签名无法表达"`toplevelBones = f(allBones)`"，调用方读签名时无法分辨哪些字段是输入、哪些是派生。

### 2.2 「构造不变量」的执行点丢失

**问题**：record 的紧凑构造器（compact constructor）只能修改参数本身并委托给 canonical，不能"先计算派生数据再赋值给非 component 字段"。

**代码证据**：当前构造器 #2 和 #4 都在 `this(...)` 之后修改 `toplevelBones`：

```java
public Model(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox) {
    this(name, new Int2ObjectOpenHashMap<>(), allBones, locator, visibleBox);
    allBones.forEach((integer, bone) -> {
        if (bone.parent == -1) {
            toplevelBones.put(integer, bone);
        } else {
            allBones.get(bone.parent).children.put(bone.id, bone);
        }
    });
}
```

这里 `toplevelBones` 是 `new Int2ObjectOpenHashMap<>()` 先塞进 canonical，然后**事后填充**。这违反了 record "构造完成后不可变"的契约——只是因为 `Int2ObjectOpenHashMap` 是可变 map 才"刚好能跑"。这是一个隐性的可变状态泄漏，等于在 record 上挖了个后门。

**信息丢失的精确表述**：record 表面承诺"不可变值对象"，实际上 `toplevelBones` / `bone.children` / `locator.children` 都是构造后被填充的可变 map。读者看到 record 关键字会误判其不可变性。

### 2.3 多态扩展能力丢失

**问题**：Java record 是 final 的，不能被继承。

**代码证据**：

- `ModelPartModel` 是一个**独立**的 record（`client/model/ModelPartModel.java`），它的字段（`name / toplevelBones / allBones`）和 Model 的核心契约重叠，但只能通过 `createModel()` **拷贝转换**成 `Model`，而无法直接"是"一个 Model。
- `BedrockGeometryModel` 是另一种独立形态（导入中间数据）。
- 如果未来想让 ModelPart-backed 模型直接进入渲染管线（避免一次拷贝），record 形态阻断了这条路。

**信息丢失的精确表述**：当前用 `record` 表达"模型"，等于声明"模型只能是这一种数据形态"。但项目里已经存在 ModelPart-backed、Bedrock JSON、Blockbench `.bbmodel` 三种来源，每种都有自己的最佳数据载体。Model 是 record，强制所有来源先转换成它，丢失了"模型"这个概念的多态性。

### 2.4 CODEC ↔ 构造器绑定脆弱（次要）

`Model.CODEC` 通过方法引用 `Model::new` 绑到 4-arg 构造器（依赖重载按参数类型解析）。

- 如果未来重构删了 4-arg 构造器，CODEC 编译错误（不是显式绑定）。
- CODEC 字段定义在 record 体内，紧贴构造器，重构时容易遗漏。

这不是"丢失信息"，而是 record 形态带来的**隐藏耦合**，提一下。

---

## 3. 设计目标

按重要性排序：

1. **G1 恢复"输入 vs 派生"的语义边界**：`toplevelBones` 等派生索引不再作为构造入参暴露给任意调用方。
2. **G2 保留所有现有调用点的源码兼容**：`Model.Bone/Cube/...`、`.toplevelBones()`、`.allBones()`、`Model.CODEC` 等签名不变，调用方零改动或仅改 import。
3. **G3 为多态形态留扩展点**：接口允许 `ModelPartModel`（或未来的 `EmptyModel`）实现 `Model`。
4. **G4 构造不变量集中**：派生索引的计算逻辑收敛到单一实现点，不再散落在 4 个构造器里。
5. **G5 不退化现有性能**：渲染管线热路径（`toplevelBones()`/`allBones()` 在 bake/render 的访问）不能因为接口派发或派生重算而变慢。

---

## 4. 候选方案

下面三个方案是"目标形态"的候选。每个方案都包含：接口签名、默认实现、CODEC 绑定方式、迁移成本。

### 方案 A：Model 改 `interface` + `SimpleModel`（普通 final class）实现

> **核心思路**：接口暴露访问器，实现用普通 class（不是 record）把派生索引藏成 `private final` 字段。

```java
// io.github.tt432.eyelib.model.Model
public interface Model {
    String name();
    Int2ObjectMap<Model.Bone> allBones();
    ModelLocator locator();
    VisibleBox visibleBox();
    Int2ObjectMap<Model.Bone> toplevelBones();  // 派生访问器，保留为接口方法以兼容现有调用

    // 嵌套 record（与现状完全一致）
    @With record Bone(...) { }
    @With record Cube(...) { }
    @With record Face(...) { }
    @With record Vertex(...) { }
    @With record TextureMesh(...) { }

    // 静态工厂（替代当前 4 个公开构造器）
    static Model of(String name, Int2ObjectMap<Bone> allBones) { ... }
    static Model of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator) { ... }
    static Model of(String name, Int2ObjectMap<Bone> allBones, VisibleBox visibleBox) { ... }
    static Model of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox) { ... }

    // CODEC：内部委托给 SimpleModel.CODEC，对外仍是 Codec<Model>
    Codec<Model> CODEC = SimpleModel.CODEC
        .xmap(s -> (Model) s, m -> (SimpleModel) m);
}
```

```java
// io.github.tt432.eyelib.model.SimpleModel
public final class SimpleModel implements Model {
    private final String name;
    private final Int2ObjectMap<Model.Bone> allBones;
    private final ModelLocator locator;
    private final VisibleBox visibleBox;
    private final Int2ObjectMap<Model.Bone> toplevelBones;  // 构造时一次性算完，外部不可变

    SimpleModel(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox) {
        this.name = name;
        this.allBones = allBones;
        this.locator = locator;
        this.visibleBox = visibleBox;
        this.toplevelBones = new Int2ObjectOpenHashMap<>();
        // 一次遍历完成 toplevelBones + bone.children + locator.children（合并当前 #2/#4 的逻辑）
        IndexInitializer.init(allBones, toplevelBones, locator);
    }

    // 访问器（手写，不再有 with*，因为 SimpleModel 不再是 record）
    @Override public String name() { return name; }
    @Override public Int2ObjectMap<Bone> allBones() { return allBones; }
    @Override public ModelLocator locator() { return locator; }
    @Override public VisibleBox visibleBox() { return visibleBox; }
    @Override public Int2ObjectMap<Bone> toplevelBones() { return toplevelBones; }

    public static final Codec<SimpleModel> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        Codec.STRING.fieldOf("name").forGetter(SimpleModel::name),
        GlobalBoneIdHandler.map(Bone.CODEC).fieldOf("all_bones").forGetter(SimpleModel::allBones),
        ModelLocator.CODEC.fieldOf("locator").forGetter(SimpleModel::locator),
        VisibleBox.CODEC.optionalFieldOf("visible_box", Model.EMPTY_VISIBLE_BOX).forGetter(SimpleModel::visibleBox)
    ).apply(ins, SimpleModel::new));
}
```

**评估**：

| 维度 | 结果 |
|---|---|
| G1 输入 vs 派生 | ✅ 完全恢复。toplevelBones 不出现在构造器签名 |
| G2 调用点兼容 | ⚠ 5 处 `new Model(...)` 改成 `Model.of(...)`；其余零改动 |
| G3 多态扩展 | ✅ 接口允许其他实现 |
| G4 不变量集中 | ✅ 派生索引计算收敛到 `IndexInitializer.init` |
| G5 性能 | ✅ 派生索引构造时缓存，访问 O(1) |
| 失去什么 | `Model` 不再能用 `@With`（lombok wither）；但当前只有 `Models.add/sub` 用了 `with*`，那是对 `Model.Bone.withCubes` 等嵌套 record 的，不依赖 Model 本身的 wither |

**迁移点清单**：
- `ImportedModelBuilder:23`：`new Model(...)` → `Model.of(...)`
- `Models:42`、`Models:100`：`new Model(...)` → `Model.of(...)`
- `ModelPartModel:43`：`new Model(...)` → `Model.of(...)`
- `Models:59/69/74/94/96`：`existingBone.withCubes(...)` / `boneB.withId(...)` 等 → 不变（这些是 `Model.Bone` 的 wither，Bone 仍是 record）
- CODEC 调用方（`BedrockGeometryImporter` 等）→ 不变（`Codec<Model>` 仍是 `Codec<Model>`）

---

### 方案 B：Model 改 `interface` + `SimpleModel` 仍是 record

> **核心思路**：保留 record 实现的简洁性，承认 `toplevelBones` 是 component（但仍控制谁能构造）。

```java
public interface Model {
    String name();
    Int2ObjectMap<Model.Bone> toplevelBones();
    Int2ObjectMap<Model.Bone> allBones();
    ModelLocator locator();
    VisibleBox visibleBox();

    static Model of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox) {
        // 工厂方法内构造 record 并保证 toplevelBones 一致
        var toplevel = IndexInitializer.computeToplevel(allBones);
        return new SimpleModel(name, toplevel, allBones, locator, visibleBox);
    }
    // ...其他重载、嵌套类型、CODEC
}

public record SimpleModel(
    String name,
    Int2ObjectMap<Model.Bone> toplevelBones,
    Int2ObjectMap<Model.Bone> allBones,
    ModelLocator locator,
    VisibleBox visibleBox
) implements Model { }
```

**评估**：

| 维度 | 结果 |
|---|---|
| G1 输入 vs 派生 | ⚠ 部分恢复。`SimpleModel` record 仍把 toplevelBones 暴露为 component + wither。但因为构造器是包级/受控的，外部不能直接 `new SimpleModel(...)`。属于"通过访问控制补救"。 |
| G2 调用点兼容 | ⚠ 同方案 A |
| G3 多态扩展 | ✅ |
| G4 不变量集中 | ✅ 工厂方法集中 |
| G5 性能 | ✅ |
| 失去什么 | `toplevelBones` 在类型签名上**仍是 component**，"输入 vs 派生"的语义边界对实现类型而言依然丢失（只对外部调用方恢复） |

**对比 A**：B 更简洁但妥协了 G1 在实现层面的纯粹性。

---

### 方案 C：Model 改 `sealed interface` + 多个 record permits

> **核心思路**：明确声明"Model 有有限种形态"，每种一个 record 实现。

```java
public sealed interface Model
        permits Model.Bedrock, Model.FromModelPart, Model.Empty {

    String name();
    Int2ObjectMap<Model.Bone> allBones();
    ModelLocator locator();
    VisibleBox visibleBox();
    Int2ObjectMap<Model.Bone> toplevelBones();

    record Bedrock(...) implements Model { }
    record FromModelPart(...) implements Model { }
    record Empty(...) implements Model { }
    // 嵌套 Bone/Cube/... 不变
}
```

**评估**：

| 维度 | 结果 |
|---|---|
| G1 输入 vs 派生 | ⚠ 看具体 record 实现。同方案 B |
| G2 调用点兼容 | ⚠ + 需要决定 ModelPartModel/EmptyModel 是否要被纳入 permits |
| G3 多态扩展 | ⚠ sealed 限制扩展（这是 seal 的目的） |
| G4 不变量集中 | ✅ |
| G5 性能 | ✅ |
| 失去什么 | 设计成本最高，需要先回答"到底有几种 Model 形态" |

**对比 A/B**：C 是"多态形态最多"的方案，但当前代码里**没有真正多态需求**（ModelPartModel 还在用 `createModel()` 转换、EmptyEntityModel 不是 Model）。在没有真实第二实现之前上 sealed，有过度设计风险。

---

## 5. 推荐与决策点

### 5.1 推荐：**方案 A**

**理由**：
1. 方案 A 是唯一在"实现层面"也恢复 G1 的方案。方案 B/C 只在"对外接口层面"恢复，看 `SimpleModel` 内部时 `toplevelBones` 仍是 component，"丢失的信息"在实现里依然丢失。
2. 当前**无真实多态需求**，sealed（方案 C）的代价不值得。方案 A 留接口扩展点，等真有 ModelPartModel 直接实现 Model 的需求时再升级到 sealed。
3. 迁移面小：5 处 `new Model(...)` → `Model.of(...)`，其余调用方零改动。
4. `SimpleModel` 作为普通 final class，把派生索引藏成 `private final`，构造一次完成，与现状"事后填充可变 map"的隐性可变状态彻底切割。

### 5.2 需要你审批的决策点

| # | 决策点 | 选项 | 我的推荐 |
|---|---|---|---|
| D1 | 选哪个方案？ | A / B / C | **A** |
| D2 | 默认实现的类名？ | `SimpleModel` / `DefaultModel` / `BedrockModel` / `ModelImpl` / `StandardModel` | **`SimpleModel`**（最克制，不暗示来源或权威性） |
| D3 | 默认实现的可见性？ | `public`（外部可 `new`）/ package-private（只能通过 `Model.of` 构造） | **package-private**（保护不变量，呼应 G1） |
| D4 | 是否同时把 `ModelPartModel` 改成 `implements Model`？ | 是 / 否 | **否**（本次只动 Model，避免范围蔓延；多态升级作为后续单独任务） |
| D5 | 派生索引的初始化逻辑放哪？ | `SimpleModel` 构造器内 / 独立 `IndexInitializer` 工具类 / `Model` 接口静态方法 | **独立 `IndexInitializer`**（便于单测，且未来其他 Model 实现可复用） |
| D6 | 是否补单元测试？ | 是（给 `IndexInitializer` 加 spec-based 单测）/ 否 | **是**（这是派生不变量的唯一保护，必须测） |
| D7 | 是否在本次任务里更新 `package-info.java` 和 `MODULES.md`？ | 是 / 否 | **是**（包结构不变，但模块定位需要更新——`model` 包现在定义"模型契约接口"而非"模型 record"） |

---

## 6. 迁移影响范围（基于方案 A + 推荐 D2~D7）

### 6.1 新增文件

- `src/main/java/io/github/tt432/eyelib/model/SimpleModel.java`
- `src/main/java/io/github/tt432/eyelib/model/IndexInitializer.java`（package-private 工具类）
- `src/test/java/io/github/tt432/eyelib/model/IndexInitializerTest.java`

### 6.2 修改文件

| 文件 | 改动 |
|---|---|
| `model/Model.java` | record → interface；嵌套 record（Bone/Cube/Face/Vertex/TextureMesh）保留；新增 `static of(...)` 工厂；CODEC 委托给 `SimpleModel.CODEC` |
| `model/package-info.java` | 描述更新（"模型契约接口"） |
| `MODULES.md` | 重生成（`:1.20.1:generateModulesMd`） |
| `importer/model/importer/ImportedModelBuilder.java:23` | `new Model(...)` → `Model.of(...)` |
| `client/model/Models.java:42,100` | `new Model(...)` → `Model.of(...)` |
| `client/model/ModelPartModel.java:43` | `new Model(...)` → `Model.of(...)` |

### 6.3 不需要改动的文件

所有访问 `.toplevelBones()` / `.allBones()` / `.locator()` / `.visibleBox()` / `.name()` 的 13+ 处调用点零改动（接口保留同名方法）。
所有使用 `Model.Bone/Cube/Face/Vertex/TextureMesh` 的代码零改动（嵌套类型保留）。
`ModelManager.INSTANCE`（`Registry<Model>`）零改动。

### 6.4 验证闸门

按 AGENTS.md "闸门矩阵"：本次属于"代码（不动包结构）"。

- [ ] `eyelib_debug_build` 退出码 0
- [ ] `eyelib_debug_nullaway` 无报错
- [ ] `eyelib_debug_test` 全绿（含新增 `IndexInitializerTest`）
- [ ] `:1.20.1:generateModulesMd` 产物随改动提交（`MODULES.md` 内容会变，因为 Model 从 record 变 interface）

不需要 clientsmoke（无渲染/资源/Mixin 改动）。

---

## 7. 待确认事项

请在审批时回答：

1. **D1~D7 的选择**（同意推荐 / 有调整）。
2. **"丢失信息"指认校验**：第 2 节列了 4 类丢失（2.1 输入 vs 派生、2.2 构造不变量执行点、2.3 多态扩展、2.4 CODEC 脆弱）。这是否覆盖了你心里"丢失信息"的全部含义？有没有我没列到的？
3. **是否允许在 worktree 提交**：本次任务在 `refactor/model-as-interface` 分支上进行，每完成一个里程碑（接口抽取、IndexInitializer 单测、迁移调用点、闸门通过）做一次 commit，最后由你 cherry-pick 或 merge 回 `1.20.1`。

---

> 审批通过后我会按"子任务设计 → 子任务执行 → 子任务验证 → 提交"的流程推进，每个子任务完成后单独压缩上下文。

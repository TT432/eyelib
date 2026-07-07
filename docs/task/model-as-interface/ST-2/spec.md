# ST-2 规格：Bone 接口化 + ModelPartModel implements Model

## 范围

1. **Bone 接口化**：`Model.Bone` 从 `@With record` 改为 interface，新建 `SimpleBone`（`@With record`）作为默认实现
2. **ModelPartModel implements Model**：让 `ModelPartModel` 直接实现 `Model` 接口，删除 `createModel()` 转换路径
3. **ModelPartModel.Bone implements Model.Bone**：从 record 改为 class，13 accessor 映射真实字段或默认值

## 必要性论证

`ModelPartModel impl Model` 要求 `allBones()` 返回 `Int2ObjectMap<Model.Bone>`。Java 泛型不变性要求 `ModelPartModel.Bone impl Model.Bone` 才能将 `Int2ObjectMap<ModelPartModel.Bone>` 赋值给 `Int2ObjectMap<Model.Bone>`。当前 `Model.Bone` 是 final record，无法被 impl。因此 **Bone 接口化是 ModelPartModel impl Model 的必要前提**。

## 不变量

### I-1: Model.Bone 是纯接口契约

interface Model.Bone 包含：
- 13 accessor 声明（id/parent/pivot/rotation/position/scale/binding/children/cubes/locator/reset/material/textureMeshes）
- `Codec<Bone> CODEC`（静态字段，`apply(ins, SimpleBone::new)`）
- `of(...)` 工厂（13-arg canonical + 10-arg 简化，转发 SimpleBone 构造器）
- `withId/withParent/withCubes/withChildren` 4 个静态 with 工厂（返回 SimpleBone）
- 嵌套类型不变（Cube/Face/Vertex/TextureMesh 仍是 Model 的嵌套 record）

### I-2: SimpleBone 是 @With record

- 文件 `model/SimpleBone.java`，包私有（class 级别 = package-private）
- `@With record SimpleBone(13 字段) implements Model.Bone`
- 字段顺序与原 Model.Bone record 完全一致（CODEC 依赖此顺序）
- 10-arg 简化构造器（委托 13-arg，reset=false/material=null/textureMeshes=List.of()）
- @With 生成 with 方法（协变返回 SimpleBone，但因 SimpleBone 包私有，外部包不可见）
- 无 CODEC（CODEC 留在 Model.Bone 接口）

### I-3: ModelPartModel.Bone 改为 class implements Model.Bone

从 record 改为 class，字段：
- `id` (int, final)
- `parent` (int, final)
- `modelPart` (ModelPart, final) —— 不在 Model.Bone 契约，保留 public accessor 供 Data 类使用
- `cubes` (List<Model.Cube>, final)
- `children` (`Int2ObjectMap<Model.Bone>`, final) —— 类型从 `Int2ObjectMap<ModelPartModel.Bone>` 改为接口类型

13 accessor 映射：
| accessor | 返回值 |
|---|---|
| id() | 真实字段 |
| parent() | 真实字段 |
| pivot() | 共享 `static final Vector3fc ZERO = new Vector3f()` |
| rotation() | 共享 ZERO |
| position() | 共享 ZERO |
| scale() | 共享 `static final Vector3fc ONE = new Vector3f(1)` |
| binding() | `null` |
| children() | 真实字段 |
| cubes() | 真实字段 |
| locator() | 每次 `new GroupLocator(new Int2ObjectOpenHashMap<>(), new ArrayList<>())`（防御性，因 GroupLocator.children 可变） |
| reset() | `false` |
| material() | `null` |
| textureMeshes() | `List.of()` |

保留方法：
- `modelPart()` —— Data 类需要（Data 的 9 个方法通过 `model.modelPart()` 访问 vanilla ModelPart）
- `add(Int2ObjectMap<Model.Bone> bones)` —— 递归加入 allBones，内部 `children.values()` 返回 `Collection<Model.Bone>`，强转 `(Bone) e` 调用递归

删除方法：
- `createBone()` —— 不再需要 ModelPartModel.Bone → Model.Bone 转换

### I-4: ModelPartModel implements Model

- `record ModelPartModel` → `record ModelPartModel implements Model`
- 补 `locator()` accessor：每次返回 `new ModelLocator(new Int2ObjectOpenHashMap<>())`（防御性，因 groupLocatorMap 可变）
- 补 `visibleBox()` accessor：返回 `EMPTY_VISIBLE_BOX`（VisibleBox 是 record 不可变，共享安全）
- `toplevelBones`/`allBones` 字段类型从 `Int2ObjectMap<ModelPartModel.Bone>` 改为 `Int2ObjectMap<Model.Bone>`
- compact constructor 中 `toplevelBones.values().forEach(b -> b.add(allBones))` 改为强转 `((Bone) b).add(allBones)`
- 删除 `createModel()`

### I-5: Model.Bone 的 4 个静态 with 工厂

```java
static Bone withId(Bone bone, int newId);
static Bone withParent(Bone bone, int newParent);
static Bone withCubes(Bone bone, List<Model.Cube> newCubes);
static Bone withChildren(Bone bone, Int2ObjectMap<Bone> newChildren);
```

实现：通过 bone 的 13 accessor 读取字段，`new SimpleBone(...)` 构造并返回。服务 Models.java 合并逻辑。

### I-6: 零改动的访问点（accessor 签名不变）

以下代码通过接口类型 `Model.Bone` 操作，Bone 接口化后 accessor 签名不变，**零改动**：
- `ModelRuntimeData`（position/rotation/scale 方法签名用 `Model.Bone`，实现只读 `bone.id()`/`bone.position()` 等）
- `ModelVisitor` / `DFSModel`
- `TwoSideModelBakeInfo` / `EmissiveModelBakeInfo` / `ModelBakeInfo`
- `AnimationView` / 各 Visitor

### I-7: Models.java 迁移

5 处 with 实例方法调用改为 Model.Bone 静态工厂：
- L59: `existingBone.withCubes(mergedCubes)` → `Bone.withCubes(existingBone, mergedCubes)`
- L69: `boneB.withId(newId).withParent(newParentId)` → `Bone.withParent(Bone.withId(boneB, newId), newParentId)`
- L74: `newBone.withParent(newParentId)` → `Bone.withParent(newBone, newParentId)`
- L93-94: `.withCubes(new ArrayList<>()).withChildren(new Int2ObjectOpenHashMap<>())` → `Bone.withChildren(Bone.withCubes(entry.getValue(), new ArrayList<>()), new Int2ObjectOpenHashMap<>())`
- L96: `.withChildren(new Int2ObjectOpenHashMap<>())` → `Bone.withChildren(entry.getValue(), new Int2ObjectOpenHashMap<>())`

### I-8: ImportedModelBuilder 迁移

L36: `new Model.Bone(...)` 13-arg → `Model.Bone.of(...)` 13-arg 工厂（跨包，of 是 public static）

### I-9: Bone.CODEC 迁移

Bone.CODEC（留在 Model.Bone 接口内）的 `apply(ins, Bone::new)` → `apply(ins, SimpleBone::new)`（SimpleBone 包私有构造器，Model 接口同包，可见）。Stonecutter `//?` 双版本结构不变。

## 设计决策

### D-ST2-1: 采用"接口静态 with 工厂"（方案 H），非"接口声明 with 实例方法"（方案 A）

**方案 A**（接口声明 4 个 with 实例方法）的 withChildren 对 ModelPartModel.Bone 语义不合理：children 应由 ModelPart 决定，不应被外部替换。

**方案 H**（接口 4 个静态工厂）返回 SimpleBone，对 ModelPartModel.Bone 的语义是"合并/转换后丢失 ModelPart 信息"——符合 Models.merge 语义（合并结果总是 SimpleModel）。

方案 H 不强制所有 impl 支持 with 实例方法，接口更干净。ModelPartModel.Bone 不需要 impl with。

### D-ST2-2: ModelPartModel.Bone 返回默认值（不从 ModelPart 提取）

pivot/rotation/position/scale 返回默认值，与当前 `createBone()` 转换语义一致。理由：
- ModelPartModel 的动画数据通过 Data 类（访问 modelPart）获取，不通过 Model.Bone 字段
- ModelRuntimeData 接收 SimpleBone（来自 CODEC/Importer），当前不接收 ModelPartModel.Bone（dead code）
- 保持改动范围最小

### D-ST2-3: ModelPartModel.Bone.locator() 每次返回 new

GroupLocator.children 是可变 map。如果 Models.merge 把 ModelPartModel.Bone 转成 SimpleBone（通过静态工厂），SimpleBone 构造器调 IndexInitializer，会修改 locator.children。共享空 GroupLocator 会被污染。每次 new 是防御性措施。

### D-ST2-4: ModelPartModel.Bone 改为 class（非 record）

理由：
- record 字段是 public final，暴露 modelPart 等内部字段（封装差）
- 需要缓存 static final Vector3fc 常量（避免每次 new）
- impl 13 accessor 需要自定义方法体（非 record 自动生成）

### D-ST2-5: ModelPartModel 保留 record

ModelPartModel 仍是 record（4 字段），补 locator()/visibleBox() 方法。locator() 每次返回 new（防御性），visibleBox() 返回常量。record 的 toplevelBones/allBones 字段是可变 map（构造时填充），与 record 不可变语义有冲突，但这是当前代码现状，不在 ST-2 改动范围。

## 测试规格

### T-1: SimpleBone @With 生成正确（编译验证）
编译通过即说明 @With 在 impl interface 的 record 上工作。

### T-2: Model.Bone 静态工厂
`src/test/java/io/github/tt432/eyelib/model/BoneInterfaceTest.java`：
- 构造 SimpleBone（通过 `Model.Bone.of` 13-arg）
- 调用 4 个静态工厂（withId/withParent/withCubes/withChildren）
- 验证返回值是 SimpleBone，修改字段正确，其他字段引用相等（assertSame）

### T-3: ModelPartModel.Bone 的 13 accessor
`src/test/java/io/github/tt432/eyelib/client/model/ModelPartModelInterfaceTest.java`：
- 构造简单 ModelPart（空 cubes/children）
- 创建 `new ModelPartModel("test", modelPart)`
- 从 allBones 取出 ModelPartModel.Bone
- 验证 id/parent/cubes/children 真实值
- 验证 pivot/rotation/position（ZERO）/scale（ONE）/binding(null)/material(null)/textureMeshes(List.of())/reset(false) 默认值
- 验证 locator() 返回非 null 且 children 为空

### T-4: ModelPartModel implements Model
同测试文件：
- 验证 name() = "test"
- 验证 toplevelBones()/allBones() 返回非空 map（如果有子 bone）
- 验证 locator() 返回非 null，groupLocatorMap 为空
- 验证 visibleBox() == EMPTY_VISIBLE_BOX

### T-5: createModel/createBone 已删除（grep 验证）
grep 全仓库 `createModel` 和 `createBone`，确认无残留调用。

## 异常行为

- ModelPartModel.Bone.add() 的强转 `(Bone) e`：如果 children 包含非 ModelPartModel.Bone 对象，抛 ClassCastException。当前 children 只在构造器填充 ModelPartModel.Bone，**安全**。
- Model.Bone 静态 with 工厂：接受任何 Model.Bone 实现，返回 SimpleBone（可能丢失原始 impl 特有数据）。符合 Models.merge 语义。

## 副作用

- Bone 接口化后，所有 `Model.Bone` 类型的变量在运行时可能是 SimpleBone 或 ModelPartModel.Bone（多态）
- ModelPartModel 不再需要 createModel() 转换，减少一次 allBones 遍历
- Models.merge 对 ModelPartModel 输入会丢失 ModelPart 信息（返回 SimpleBone）

## 风险

- R-1: SimpleBone @With 协变返回 —— Java 支持，@With 在 record 上生成的方法返回 record 本身
- R-2: ModelPartModel.Bone.children 类型改接口后内部强转 —— 安全（只存 ModelPartModel.Bone）
- R-3: Bone.CODEC `apply(ins, SimpleBone::new)` —— SimpleBone 包私有构造器，CODEC 在 Model 接口（同包），可见
- R-4: ModelPartModel.Bone.locator() 每次返回 new —— 性能损失（dead code，可接受）
- R-5: ImportedModelBuilder 跨包调用 `Model.Bone.of` —— of 是接口 public static，跨包可见
- R-6: ModelPartModel.Data 方法签名用 `ModelPartModel.Bone` —— Bone 改 class 后仍可用，零改动
- R-7: ModelPartModel 从 record 改为 `record implements Model` —— record 可以 impl interface，Java 支持

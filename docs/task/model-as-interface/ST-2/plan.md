# ST-2 执行计划

## 前置条件
- worktree 分支 `refactor/model-as-interface`，HEAD = `9c7a0013`（ST-1 commit）
- git status clean（除 clientsmoke submodule 已 init）
- 规格文档：`docs/task/model-as-interface/ST-2/spec.md` 已审批

## Step 0: 准备验证
- 确认 worktree git status clean
- 确认 HEAD = 9c7a0013

## Step 1: 新建 `model/SimpleBone.java`
从 `Model.java` 的 `Model.Bone` record 提取为独立文件：
- `@With record SimpleBone(13 字段) implements Model.Bone`
- 包私有 class
- 字段顺序与原 Model.Bone record 完全一致
- 10-arg 简化构造器（委托 13-arg）
- 无 CODEC（CODEC 留在 Model.Bone 接口）

**验证点**：SimpleBone 编译通过（依赖 Model.Bone interface，但此时 Model.Bone 还是 record → 先做 Step 2 再编译）

## Step 2: 重写 `model/Model.java` 的 Model.Bone 为 interface
- 把 `@With record Bone(...)` 改为 `interface Bone`
- 13 accessor 声明（无方法体）
- CODEC：`apply(ins, Bone::new)` → `apply(ins, SimpleBone::new)`（Stonecutter 双版本保留）
- `of(...)` 工厂：13-arg canonical + 10-arg 简化（转发 SimpleBone）
- 4 个静态 with 工厂：withId/withParent/withCubes/withChildren
- 删除原 record 的 10-arg 简化构造器（已搬到 SimpleBone）
- 嵌套 record Cube/Face/Vertex/TextureMesh 不动

**验证点**：编译 `:1.20.1:compileJava`，SimpleBone + Model 编译通过

## Step 3: 重写 `client/model/ModelPartModel.java`

### 3a: ModelPartModel 顶层
- `record ModelPartModel(...)` → `record ModelPartModel(...) implements Model`
- 补 `@Override public ModelLocator locator()` —— 每次 new 空 ModelLocator
- 补 `@Override public VisibleBox visibleBox()` —— 返回 EMPTY_VISIBLE_BOX
- toplevelBones/allBones 字段类型 → `Int2ObjectMap<Model.Bone>`
- compact constructor: `((Bone) b).add(allBones)` 强转
- 删除 `createModel()`

### 3b: ModelPartModel.Bone
- `record Bone(...)` → `class Bone implements Model.Bone`
- 字段：id/parent/modelPart/cubes/children（children 类型 = `Int2ObjectMap<Model.Bone>`）
- static final Vector3fc ZERO/ONE 常量
- 构造器逻辑不变（从 ModelPart 建 Bone 树）
- impl 13 accessor（按 spec I-3 表格映射）
- 保留 `modelPart()` public accessor
- 保留 `add(Int2ObjectMap<Model.Bone>)`（内部强转 `(Bone) e`）
- 删除 `createBone()`

### 3c: ModelPartModel.Data
- 零改动（方法签名用 ModelPartModel.Bone，访问 modelPart()，Bone 改 class 后仍可用）

**验证点**：编译 `:1.20.1:compileJava`

## Step 4: 迁移 `client/model/Models.java`
5 处 with 实例方法调用改为 Model.Bone 静态工厂（按 spec I-7）

**验证点**：编译通过

## Step 5: 迁移 `importer/model/importer/ImportedModelBuilder.java`
L36: `new Model.Bone(...)` → `Model.Bone.of(...)`

**验证点**：编译通过

## Step 6: 闸门验证（编译 + NullAway + 单测）

### 6a: 编译
```powershell
.\gradlew.bat :1.20.1:compileJava :1.20.1:compileTestJava 2>&1 | Tee-Object build\_worktree_compile.txt
```

### 6b: NullAway
```powershell
.\gradlew.bat :1.20.1:nullawayMain 2>&1 | Tee-Object build\_worktree_nullaway.txt
```
失败时查根因，**禁止用注解静默消除**。

### 6c: 单测
先写单测（Step 6.5），再跑：
```powershell
.\gradlew.bat :1.20.1:test 2>&1 | Tee-Object build\_worktree_test.txt
```
预存失败（BedrockGeometryImporterTest/BedrockImportedModelDataTest 的 fixture 路径问题）忽略，确认新增测试全绿。

## Step 6.5: 写单测（在 6c 之前）

### `src/test/java/io/github/tt432/eyelib/model/BoneInterfaceTest.java`（T-2）
- 构造 SimpleBone（通过 `Model.Bone.of` 13-arg）
- 测试 4 个静态工厂

### `src/test/java/io/github/tt432/eyelib/client/model/ModelPartModelInterfaceTest.java`（T-3, T-4）
- 构造简单 ModelPart
- 测试 ModelPartModel.Bone 的 13 accessor
- 测试 ModelPartModel 的 5 accessor

**注意**：如果 `src/test/java/io/github/tt432/eyelib/client/model/` 目录不存在，需创建并加 `package-info.java`（@NullMarked）。

## Step 7: grep 残留检查
- grep `createModel` —— 应零命中
- grep `createBone` —— 应零命中
- grep `new Model.Bone` —— 应零命中（改为 Model.Bone.of 或 SimpleBone::new）
- grep `\.withId\(|\.withParent\(|\.withCubes\(|\.withChildren\(` —— 在 Models.java 应零命中（改为静态工厂）

## Step 8: MODULES.md 检查
- Model.Bone record → interface，但包结构不变（io.github.tt432.eyelib.model）
- 新增 SimpleBone（同包）
- 跑 `:1.20.1:generateModulesMd`，检查 `eyelib.model` 行是否变化
- 如果变化只是预存差异（attachment/ui），`git checkout -- MODULES.md` 还原
- 如果因 SimpleBone 新增导致描述变化，保留并提交

## Step 9: commit
```powershell
git add src/main/java/io/github/tt432/eyelib/model/SimpleBone.java
git add src/main/java/io/github/tt432/eyelib/model/Model.java
git add src/main/java/io/github/tt432/eyelib/client/model/ModelPartModel.java
git add src/main/java/io/github/tt432/eyelib/client/model/Models.java
git add src/main/java/io/github/tt432/eyelib/importer/model/importer/ImportedModelBuilder.java
git add src/test/java/io/github/tt432/eyelib/model/BoneInterfaceTest.java
git add src/test/java/io/github/tt432/eyelib/client/model/  # 含 package-info + 测试
git add docs/task/model-as-interface/ST-2/
git commit -m "refactor(model): Bone 接口化 + ModelPartModel implements Model"
```

**禁止 `git add -A`**（会污染 clientsmoke submodule）。

## clientsmoke 评估
ST-2 改动 ModelPartModel（client/model 包），但 ModelPartModel 是 dead code（零运行时调用）。改动涉及：
- Model.Bone record → interface（影响所有 Model.Bone 使用者，但 accessor 签名不变）
- Models.java with 调用迁移（合并逻辑变化，但 Models.merge 输入仍是 SimpleModel）

**决策**：ST-2 不需要 clientsmoke。ModelPartModel 是 dead code，改动不触发运行时行为变化。Models.java 的 with 迁移是等价改写（静态工厂返回 SimpleBone，与原 record with 语义一致）。如果闸门验证发现运行时敏感问题，再补 clientsmoke。

## 风险应对
- 编译失败：查 SimpleBone/Model.Bone interface 的方法签名匹配
- NullAway 失败：查 ModelPartModel.Bone class 的 nullability（@NullMarked 包下，字段默认非 null）
- 单测失败：先 `git stash` 复现，排除预存失败

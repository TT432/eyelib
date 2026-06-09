# Domain 提取操作手册

> 子代理专用。每次提取一个模块的一个 Port，按本文流程操作。

## 前置条件

- 已读完 `docs/architecture/0010-hexagonal-architecture.md`（知道为什么拆）
- 已读完 `docs/architecture/domain-module-map.md`（知道当前模块需要哪些 Port）
- 已读完 `docs/architecture/port-design-template.md`（知道 Port 怎么写）
- `eyelib-bridge` 子项目骨架已创建（可在第一批 Port 提取前完成）
- 如用 WSL：编译命令用 `cmd.exe /c gradlew.bat`，不用 `./gradlew`

## 提取流程

### Step 1：确认目标任务

从 `domain-module-map.md` 中取出当前待处理的 Port。例如：

> 任务：为 `eyelib-material` 创建 `PortStringRepresentable`，替换所有枚举中的 `net.minecraft.util.StringRepresentable`

### Step 2：定位所有 MC 接触点

```bash
# 在目标模块中搜索所有 MC import
grep -rn "import net.minecraft" src/main/java/ --include="*.java"
```

对结果分类：
- **枚举序列化**（`implements StringRepresentable`）→ 改实现 `PortStringRepresentable`
- **资源路径**（`new ResourceLocation(ns, path)`）→ 改用 `PortResourceLocation`
- **渲染类型**（`RenderType`, `RenderStateShard`）→ 改用 Port 接口
- 其他 → 判断是否需要新建 Port 或直接迁移到 bridge

**⚠️ 重复枚举检查：** eyelib-material 的 `gl/` 和 `shared/` 两处有同名枚举（如 BlendFactor、GLStates、DepthFunc 等）。`shared/` 版本是抽象版（用于 CODEC），`gl/` 版本映射 GL 常量。两处**都需要**改 implements。

### Step 3：创建 Port 接口

在目标模块的 `src/main/java/io/github/tt432/<模块包>/port/` 下创建。严格按 `port-design-template.md` 的模板。

**必须遵守：**
- 接口不加 `public` 修饰符（同包可见足够，bridge 会依赖此模块）
- 加 `@NullMarked`
- 不能 import 任何 `net.minecraft.*`

### Step 4：修改 domain 代码

按接触点类型改：

**枚举类：**
```java
// 改前
public enum BlendFactor implements StringRepresentable {

// 改后
public enum BlendFactor implements PortStringRepresentable {
```

**ResourceLocation：**
```java
// 改前
new ResourceLocation("eyelib:models/entity")

// 改后
PortResourceLocation.of("eyelib", "models/entity")
```

**RenderType：**
```java
// 改前
public RenderType getRenderType(ResourceLocation texture) {

// 改后
public PortRenderPass getRenderPass() {
    return new PortRenderPass() {
        public boolean requiresAlphaTest() { return hasDefine("ALPHA_TEST"); }
        public boolean requiresBlending() { return hasBlending(); }
        public boolean requiresCulling() { return !hasDefine("DISABLE_CULLING"); }
    };
}
```

**核心原则：domain 代码改成输出 Port 类型的数据，不输出 MC 类型。**

### Step 5：编译验证

```bash
# WSL 环境 → 必须用 Windows 侧 gradlew.bat（./gradlew 在 WSL 下太慢）
cmd.exe /c "cd /d E:\_ideaProjects\qylEyelib && gradlew.bat :eyelib-material:compileJava --no-configuration-cache"
```

编译必须零错误。**警告也要修**（如 unused import）。

### Step 6：运行 ArchUnit 规则（如果已配置）

如果 `acceptance-gates.md` 中的 ArchUnit 规则已生效：

```bash
cmd.exe /c "cd /d E:\_ideaProjects\qylEyelib && gradlew.bat :eyelib-material:test --no-configuration-cache --tests *ArchUnit*"
```

### Step 7：迁移胶水代码到 bridge

将之前在 domain 模块中依赖 MC 实现的代码迁移到 `eyelib-bridge`。

**⚠️ 纯 Port 替换不需要迁移。** 像 `PortStringRepresentable` 替换 `StringRepresentable` 这种纯接口替换，只在 domain 侧改 implements 即可，不产生 bridge 代码。只有以下情况才需要迁移：

- domain 代码中调用了 MC 类的具体方法（如 `RenderType.m_...()`）
- domain 代码中创建了 MC 类型的实例（如 `new BufferBuilder(...)`）

```bash
# 例如：RenderTypeResolver 内部调用 MC 的 RenderType 工厂
# → 整体移动到 eyelib-bridge/src/main/java/.../material/RenderTypeResolverAdapter.java
```

bridge 中的实现类实现 domain 的 Port 接口，内部可以自由 import MC 类型。

### Step 8：更新调用方

root 模块或其他模块中对被迁移类的引用需要改 import。原则上：
- Port 接口的引用保持不变（在 domain 模块中）
- 具体 MC 实现的引用改为 bridge 中的 Adapter 类

### Step 9：运行全局编译

```bash
cmd.exe /c "cd /d E:\_ideaProjects\qylEyelib && gradlew.bat :compileJava --no-configuration-cache"
```

根模块和其他使用了被修改模块的子项目必须编译通过。

## 常见陷阱

### 1. Port 接口 import 了 MC 类型

```java
// ❌ 在 domain 模块的 Port 接口中（它在 domain 侧！）
import net.minecraft.resources.ResourceLocation; // 编译会通过，但违反了隔离
public interface PortXxx {
    ResourceLocation toResource(); // ← 失败了
}
```

检查方法：在 Port 接口文件中 `grep "import net.minecraft"`。

### 2. 枚举改了 implements 但忘了改调用方

```java
// 改前：codec 中用 StringRepresentable.CODEC
public static final Codec<BlendFactor> CODEC = StringRepresentable.fromEnum(BlendFactor::values);

// 改后：需要改为自己的 fromEnum 工具
// 最简单：在 PortStringRepresentable 接口中加 static 辅助方法
static <T extends PortStringRepresentable> Codec<T> codec(Supplier<T[]> values) { ... }
```

### 3. 忘记处理 bridge 的依赖

新建 `eyelib-bridge` 后，它的 `build.gradle` 需要显式依赖目标 domain 模块：

```groovy
dependencies {
    implementation project(':eyelib-material')
    implementation project(':eyelib-molang')
    // ... etc
}
```

### 4. 在 bridge 中重新发明 Port 接口

> bridge 不能定义新的 Port。它只实现 domain 中已有的 Port。

如果发现需要新的抽象能力但 domain 中还没 Port → 先在 domain 中定义，再在 bridge 中实现。

### 5. 一次改太多模块

> 每次只改一个模块的一个 Port。

修改范围过大导致编译难以排查。PortStringRepresentable 涉及 3 个模块 → 分 3 次子代理任务。

## 验收清单

每个子代理任务完成后，架构师检查：

- [ ] domain 模块中不再有 `import net.minecraft`（`grep` 验证）
- [ ] domain 模块的 `test` 编译通过（`gradle :module:test`）
- [ ] 全局编译通过（`gradle :compileJava`）
- [ ] 现有测试无退化（新增失败的测试必须有理有据）
- [ ] 新 Port 接口遵守 `port-design-template.md` 规范

---
name: eyelib-domain-extraction
description: Extract a Port from a domain module in the hexagonal architecture refactoring — locate MC contact points, create Port interfaces, migrate glue code to bridge, and verify with ArchUnit.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
  tags: eyelib, hexagonal, domain, port, extraction, refactoring
  related-skills: eyelib-hexagonal-gates, eyelib-build
---

# Eyelib Domain 提取

子代理专用。每次提取一个模块的一个 Port，按本文流程操作。

## 前置条件

- 已读完 ADR-0010（六边形架构，为什么拆）
- 已读完 `docs/architecture/domain-module-map.md`（当前模块需要哪些 Port）
- `bridge` 包已存在（ADR-0014 后为包非子项目）
- 编译用 JetBrains MCP(`jetbrain_build_project` 或 `jetbrain_run_gradle_tasks`),**禁止 shell gradlew**(详见 AGENTS.md Tooling Restrictions)

## 提取流程

### Step 1：确认目标任务

从 `domain-module-map.md` 取出待处理 Port。例：
> 任务：为 `material` 模块创建 `PortStringRepresentable`，替换所有 `net.minecraft.util.StringRepresentable`

### Step 2：定位所有 MC 接触点

```bash
grep -rn "import net.minecraft" src/main/java/ --include="*.java"
```

分类：
- **枚举序列化**（`implements StringRepresentable`）→ 改实现 `PortStringRepresentable`
- **资源路径**（`new ResourceLocation(ns, path)`）→ 改用 `PortResourceLocation`
- **渲染类型**（`RenderType`, `RenderStateShard`）→ 改用 Port 接口
- 其他 → 判断是否新建 Port 或直接迁移到 bridge

**重复枚举检查：** material 模块的 `gl/` 和 `shared/` 两处有同名枚举（BlendFactor、GLStates、DepthFunc）。`shared/` 是抽象版（CODEC 用），`gl/` 映射 GL 常量。两处都需要改 implements。

### Step 3：创建 Port 接口

在目标模块 `src/main/java/io/github/tt432/<模块包>/port/` 下创建。

必须遵守：
- 接口不加 `public` 修饰符
- 不加类级 `@NullMarked`（依赖包的 `package-info.java`）
- 不能 import 任何 `net.minecraft.*`

### Step 4：修改 domain 代码

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

**RenderType → 语义 Port：**
```java
// 改后：domain 输出 Port 语义，不输出 MC 类型
public PortRenderPass getRenderPass() {
    return new PortRenderPass() {
        public boolean requiresAlphaTest() { return hasDefine("ALPHA_TEST"); }
        public boolean requiresBlending() { return hasBlending(); }
        public boolean requiresCulling() { return !hasDefine("DISABLE_CULLING"); }
    };
}
```

### Step 5：编译验证

经 JetBrains MCP 跑 `jetbrain_build_project` 或 `jetbrain_run_gradle_tasks(["compileJava"])`(ADR-0014 后单 project,无 `:eyelib-material:` 子项目前缀)。

零错误，警告也要修。

### Step 6：ArchUnit（如已配置）

经 JetBrains MCP 跑 `jetbrain_run_gradle_tasks(["test"])`,若要指定测试类可加 `--tests "*ArchUnit*"` 脚本参数。

> 注: ADR-0015 ArchUnit freeze 模式骨架尚未落地,本步骤在骨架恢复后才执行。

### Step 7：迁移胶水代码到 bridge

**纯 Port 替换不需要迁移。** 像 `PortStringRepresentable` 替换——只在 domain 侧改 implements，不产生 bridge 代码。只有以下情况才迁移：

- domain 调用了 MC 类的具体方法（如 `RenderType.m_...()`）
- domain 创建了 MC 类型实例（如 `new BufferBuilder(...)`）

bridge 中实现类实现 domain 的 Port 接口，内部可自由 import MC 类型。

### Step 8：更新调用方

root 或其他模块中被迁移类的引用改 import：
- Port 接口引用保持不变（在 domain 模块中）
- MC 实现引用改为 bridge 中 Adapter 类

### Step 9：全局编译

经 JetBrains MCP 跑 `jetbrain_build_project`(整 project) 或 `jetbrain_run_gradle_tasks(["compileJava"])`。

## Port 设计规范

### 粒度假说

Port 只在被至少两个 MC 代码路径调用时才创建。单一 use case → 直接暴露具体类型给 bridge。

### 语义驱动

Port 表达 domain 的需求语义，不是 MC API 镜像：

```java
// ❌ 机械翻译
public interface PortMinecraft { Level getLevel(); Camera getCamera(); }
// ✅ 语义表达
public interface PortRenderContext { long dayTime(); long gameTime(); float cameraYaw(); }
```

### 返回值类型

只能用 Java 标准库、`util` 包类型、或同 domain 模块类型。禁止 `Object`。

### 命名规范

| 位置 | 格式 | 示例 |
|------|------|------|
| 包路径 | `<模块>/port/` | `src/main/java/io/github/tt432/eyelib/material/port/` |
| 接口名 | `Port<语义>` | `PortRenderPass`, `PortEntity` |
| 实现类 | `<语义>Adapter` | `RenderPassAdapter`（在 bridge） |

### 接口模板

```java
/** @author TT432 */
interface PortXxx {
    ReturnType someProperty();
    @Nullable ReturnType optionalProperty();
    void doSomething(InputType input);
    static PortXxx of(Args args) { ... }
}
```

### fromEnum() 辅助方法

Port 替代 `StringRepresentable` 时必须提供：

```java
public interface PortStringRepresentable {
    String getSerializedName();

    static <T extends Enum<T> & PortStringRepresentable> Codec<T> fromEnum(Supplier<T[]> values) {
        return Codec.STRING.xmap(
                name -> Arrays.stream(values.get())
                        .filter(e -> e.getSerializedName().equals(name))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Unknown: " + name)),
                PortStringRepresentable::getSerializedName
        );
    }
}
```

### 禁止项

| 禁止 | 正确做法 |
|------|----------|
| Port import `net.minecraft.*` | Java 标准库或 domain 自有类型 |
| Port 方法返回 `Object` | sealed 类型或泛型 |
| Port 放 bridge 模块 | Port 由 domain 定义 |
| 一个 Port 超 10 方法 | 拆成多个窄接口 |
| 每个 MC 类建一个 Port | 按语义分组 |

## Common Pitfalls

1. **Port 接口 import 了 MC 类型** — `grep "import net.minecraft"` 验证
2. **枚举改了 implements 忘了改 codec** — `StringRepresentable.fromEnum()` → `PortStringRepresentable.fromEnum()`
3. ~~**bridge 的 build.gradle 缺 domain 依赖** — `implementation project(':eyelib-material')`~~（ADR-0014 前子项目时代踩坑，当前单 project 无此问题）
4. **bridge 中重新发明 Port** — bridge 只能实现 domain 已有 Port，不能定义新的
5. **一次改太多模块** — 每次只改一个模块的一个 Port。PortStringRepresentable 涉及 3 模块 → 分 3 次
6. **忘了重复枚举** — material 的 `gl/` 和 `shared/` 同名枚举两处都要改

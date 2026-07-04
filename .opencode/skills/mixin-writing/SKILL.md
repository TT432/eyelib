---
name: mixin-writing
description: Write Mixin accessors and injectors for eyelib. Use when replacing reflection with @Accessor/@Invoker, injecting into vanilla methods, or debugging Mixin AP compilation errors. Covers Forge 1.20.1 (Mixin AP active) and NeoForge 1.21.1 (no Mixin AP) dual-version patterns.
---

## 核心理念

Mixin 是**编译时声明的运行时字节码修改**。用 @Accessor/@Invoker 替代反射获取 private/protected/package-private 成员访问权，保留类型安全和重构友好性。

```
反射（弱）                    Mixin Accessor（强）
Field.get(obj)                @Accessor interface → 类型安全调用
Constructor.newInstance()     @Invoker("<init>") → 类型安全构造
```

### 铁律

- **Accessor interface 必须注册到 mixins.json**，否则运行时不生效
- **Accessor 名前缀用 `eyelib$`**（避免与其他 mod 冲突）
- **跨版本差异用整文件级 `//?` 条件化**（非逐方法条件）
- **Mixin AP 只在 legacyForge 1.20.1 启用**，NeoForge 1.21.1 不启用

---

## @Accessor：字段访问

### 基本模式

```java
@Mixin(TargetClass.class)
public interface TargetAccessor {
    @Accessor("fieldName")
    ReturnType eyelib$getFieldName();

    @Accessor("fieldName")
    void eyelib$setFieldName(ReturnType value);
}
```

### 使用方式

```java
// 非 final 类：直接 cast
TargetAccessor accessor = (TargetAccessor) targetInstance;
ReturnType value = accessor.eyelib$getFieldName();

// final 类：双重 cast 绕过编译器
TargetAccessor accessor = (TargetAccessor) (Object) targetInstance;
```

### @Accessor 类型检查陷阱（Mixin AP 0.8.5）

**Mixin AP 的类型检查是逆协变的**：检查 `字段类型.isAssignableFrom(返回类型)`，而非正确的 `返回类型.isAssignableFrom(字段类型)`。

| 返回类型 | 字段类型 | Mixin AP 结论 | 实际 |
|---|---|---|---|
| `boolean` | `boolean` | ✓ 通过 | ✓ |
| `int` | `int` | ✓ 通过 | ✓ |
| `Vector3f` | `Vector3f` | ✓ 通过 | ✓ |
| `Map<String, ModelPart>` | `Map` | ✓ 通过 | ✓ |
| **`Object`** | `T toAvoid` (泛型) | **✗ 失败** | ✓（运行时安全） |
| **`Object[]`** | `Polygon[]` | **✗ 失败** | ✓（运行时安全） |

**解决：返回宽类型的 accessor 用整文件级 `//? if >=1.20.6` 条件化，仅在 1.21.1（无 Mixin AP）编译。**

---

## @Invoker：方法/构造器调用

### 构造器调用

```java
@Mixin(TargetClass.class)
public interface TargetConstructorAccessor {
    @Invoker("<init>")
    static TargetClass eyelib$create(ArgType1 arg1, ArgType2 arg2);
}
```

### private 方法调用

```java
@Mixin(TargetClass.class)
public interface TargetMethodAccessor {
    @Invoker("privateMethodName")
    ReturnType eyelib$callPrivateMethod(TargetClass self, ArgType arg);
}
```

---

## 跨版本条件化模式

### 整文件级条件（推荐）

当 accessor 仅在某一版本需要时（如 1.21.1 的 private 化字段），用整文件级 `//?` 条件化：

```java
//? if >=1.20.6 {
package io.github.tt432.eyelib.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.class)
public interface ModelPartAccessor {
    @Accessor("children")
    Map<String, ModelPart> eyelib$getChildren();

    @Accessor("cubes")
    List<ModelPart.Cube> eyelib$getCubes();
}
//?}
```

1.20.1 语境：整个文件被 stonecutter 注释掉（`/* */` 包裹），Mixin AP 不处理。
1.21.1 语境：文件正常编译，无 Mixin AP 约束。

### mixins.json 注册策略

**条件化的 accessor 不需要从 mixins.json 条件移除。** Mixin AP 只处理 source set 中存在的类。被 `//?` 注释掉的文件不在 source set 中，注册名会被忽略（1.20.1）或正常加载（1.21.1）。

---

## final 类的 cast 问题

Java 编译器拒绝 `(Interface) finalClassInstance`（final 类不能实现外部 interface）。

```java
// ModelPart 在 1.21.1 是 final class
// 编译错误：inconvertible types
ModelPartAccessor acc = (ModelPartAccessor) modelPart;

// 修复：双重 cast
ModelPartAccessor acc = (ModelPartAccessor) (Object) modelPart;
```

---

## @Shadow vs @Accessor

| 特性 | @Shadow | @Accessor |
|---|---|---|
| 适用 | abstract class mixin | interface mixin |
| 用途 | 在 mixin 内部引用目标字段 | 从外部获取字段访问 |
| 运行时 | 重定向到真实字段 | 生成 getter/setter |
| 典型场景 | @Inject 方法体内读字段 | 外部类需读 private 字段 |

---

## MixinExtras 注入器（优先于 vanilla）

| MixinExtras | 替代的 vanilla | 用途 |
|---|---|---|
| `@WrapOperation` | `@Redirect` | 包装方法调用（更灵活，不冲突） |
| `@ModifyExpressionValue` | `@ModifyArg` / `@ModifyVariable` | 修改表达式值 |
| `@Expression` | `@At(value="INVOKE", ordinal=N)` | 精确锚点（支持链式调用） |
| `@Local` | `LocalCapture` | 捕获局部变量 |

---

## 常见坑

### 1. searge 映射名

1.20.1 Forge 用 searge 映射。@Accessor/@Invoker 的 `value` 必须是**开发环境名**（parchment/mojmap），Mixin AP 通过 refmap 转换为 searge。如果字段名在 searge 中不存在，Mix AP 报 "Could not locate @Accessor target"。

**验证方法**：检查 `build/mixin/eyelib.refmap.json`，确认 accessor 有映射条目。

### 2. package-private 内部类

`ModelPart.Polygon` 和 `ModelPart.Vertex` 在 1.21.1 是 package-private。@Mixin targets 用字符串避免 import：

```java
@Mixin(targets = "net.minecraft.client.model.geom.ModelPart$Polygon")
public interface ModelPartPolygonAccessor { ... }
```

### 3. 泛型字段

`AvoidEntityGoal<T>` 的 `toAvoid` 字段是 `protected T`。@Accessor 返回 `Object` 会被 Mixin AP 拒绝（逆协变类型检查）。解决：整文件条件化到 1.21.1 只。

### 4. @Coerce 不支持 @Accessor

`@Coerce` 注解只支持 `@Inject`/`@Redirect`，不支持 `@Accessor`/`@Invoker`。无法用它绕过类型检查。

---

## 编译验证

```groovy
// build.gradle — Mixin AP 只在 legacyForge 启用
if (isLegacyForge) {
    mixin {
        add sourceSets.main, "eyelib.refmap.json"
        config "eyelib.mixins.json"
    }
    annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'
}
```

```bash
# 通过 eyelib-debug MCP 或 bash gradlew 编译
eyelib_debug_build version="1.20.1"
# 或 bash: gradlew :1.20.1:compileJava :1.21.1:compileJava
```

如果 Mixin AP 报错：
1. 检查 `eyelib.refmap.json` 是否有该 accessor 的映射
2. 检查字段名是否是开发环境名（非 searge 名）
3. 检查返回类型是否窄于或等于字段类型（避免逆协变 bug）
4. 如果返回类型必须宽（Object/Object[]），用 `//? if >=1.20.6` 条件化到 1.21.1 只

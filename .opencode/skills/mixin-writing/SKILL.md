---
name: mixin-writing
description: Write Mixin accessors and injectors for eyelib. Use when replacing reflection with @Accessor/@Invoker, injecting into vanilla methods, or debugging Mixin AP compilation errors. Covers Forge 1.20.1 (Mixin AP active) and NeoForge 1.21.1 (no Mixin AP) dual-version patterns.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
---

# mixin-writing

为 eyelib 编写 Mixin accessor 与注入器：用 @Accessor/@Invoker 替代反射获取 private/protected/package-private 成员访问权，保留类型安全与重构友好性；覆盖 Forge 1.20.1（启用 Mixin AP）与 NeoForge 1.21.1（无 Mixin AP）双版本模式。

## When to use
- 用 @Accessor/@Invoker 替代反射访问私有成员
- 向 vanilla 方法注入逻辑（@Inject/MixinExtras）
- 调试 Mixin AP 编译错误（refmap、逆协变类型检查）
- 编写跨 1.20.1/1.21.1 双版本的 Mixin 代码
- Do NOT use when: 非 Mixin 的普通反射工具编写
- Do NOT use when: 与 eyelib Stonecutter 双版本布局无关的单版本项目

## Rules
- Accessor interface 必须注册到 mixins.json，否则运行时不生效
- Accessor 方法名前缀用 `eyelib$`，避免与其他 mod 冲突
- 跨版本差异用整文件级 `//?` 条件化（非逐方法条件）；仅在某一版本需要的 accessor（如 1.21.1 的 private 化字段、宽返回类型、泛型字段）整文件包进 `//? if >=1.20.6` 块，仅在该版本编译
- [when version=1.20.1 legacyForge] 启用 Mixin AP（annotationProcessor + refmap 配置）；被 `//?` 注释掉的文件不在 source set 中，Mixin AP 不处理
- [when version=1.21.1 NeoForge] NEVER 启用 Mixin AP；文件正常编译，无 Mixin AP 约束
- [when version=1.20.1 (searge runtime)] @Accessor/@Invoker 的 `value` 用开发环境名（parchment/mojmap），不用 searge 名；1.20.1 Forge 运行时是 searge 映射，Mixin AP 通过 refmap 转换，字段名在 searge 中不存在时报 "Could not locate @Accessor target"
- [when Mixin AP active (1.20.1)] NEVER 让 @Accessor 返回宽于字段类型的类型（如泛型字段返回 Object、Polygon[] 字段返回 Object[]）——Mixin AP 0.8.5 的类型检查是逆协变的（检查 `字段类型.isAssignableFrom(返回类型)`），会拒绝运行时安全但返回类型更宽的 accessor
- PREFER 注入器优先用 MixinExtras 而非 vanilla：@WrapOperation 替代 @Redirect（包装方法调用，更灵活不冲突）、@ModifyExpressionValue 替代 @ModifyArg/@ModifyVariable、@Expression 替代 @At(value="INVOKE", ordinal=N)（精确锚点，支持链式调用）、@Local 替代 LocalCapture
- NEVER 用 @Coerce 绕过 @Accessor/@Invoker 的类型检查——@Coerce 只支持 @Inject/@Redirect
- package-private 内部类（如 1.21.1 的 ModelPart.Polygon/Vertex）的 @Mixin targets 用字符串全限定名（`$` 分隔）避免 import
- 对 final 类实例获取 accessor 用双重 cast `(Accessor)(Object) instance`——Java 编译器拒绝 `(Interface) finalClassInstance`（inconvertible types）；非 final 类直接 cast
- NEVER 从 mixins.json 条件移除被 `//?` 注释掉的 accessor 注册——Mixin AP 只处理 source set 中存在的类，注册名在 1.20.1 被忽略、1.21.1 正常加载
- PREFER mixin 内部引用目标字段用 @Shadow（abstract class mixin，@Inject 方法体内读字段，运行时重定向到真实字段）；从外部类获取 private 字段访问用 @Accessor（interface mixin，生成 getter/setter）

## Workflow
1. 编译双版本验证：mcmcp_build version="1.20.1"，或 bash 执行 gradlew :1.20.1:compileJava :1.21.1:compileJava
2. 验证 searge 映射：检查 build/mixin/eyelib.refmap.json，确认 accessor 有映射条目
3. 若 Mixin AP 报错，按序排查：1) 检查 eyelib.refmap.json 是否有该 accessor 的映射；2) 检查字段名是否开发环境名（非 searge 名）；3) 检查返回类型是否窄于或等于字段类型（避免逆协变 bug）；4) 返回类型必须宽（Object/Object[]）时用 `//? if >=1.20.6` 条件化到 1.21.1 只 [fallback]

## Output
- Type: mixin-code
- 注册到 mixins.json 的 @Accessor/@Invoker interface（方法名带 eyelib$ 前缀） (required)
- 1.20.1 与 1.21.1 双版本编译通过 (required) — gradlew :1.20.1:compileJava :1.21.1:compileJava 均成功
- eyelib.refmap.json 中存在 accessor 映射条目（1.20.1）
- Stop when: 双版本编译通过且 refmap 映射确认后完成

<!-- locked residual (verbatim, do not edit) -->
```java
@Mixin(TargetClass.class)
public interface TargetAccessor {
    @Accessor("fieldName")
    ReturnType eyelib$getFieldName();

    @Accessor("fieldName")
    void eyelib$setFieldName(ReturnType value);
}
```
```java
// 非 final 类：直接 cast
TargetAccessor accessor = (TargetAccessor) targetInstance;
ReturnType value = accessor.eyelib$getFieldName();

// final 类：双重 cast 绕过编译器
TargetAccessor accessor = (TargetAccessor) (Object) targetInstance;
```
```java
@Mixin(TargetClass.class)
public interface TargetConstructorAccessor {
    @Invoker("<init>")
    static TargetClass eyelib$create(ArgType1 arg1, ArgType2 arg2);
}
```
```java
@Mixin(TargetClass.class)
public interface TargetMethodAccessor {
    @Invoker("privateMethodName")
    ReturnType eyelib$callPrivateMethod(TargetClass self, ArgType arg);
}
```
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
```java
@Mixin(targets = "net.minecraft.client.model.geom.ModelPart$Polygon")
public interface ModelPartPolygonAccessor { ... }
```
```java
// ModelPart 在 1.21.1 是 final class
// 编译错误：inconvertible types
ModelPartAccessor acc = (ModelPartAccessor) modelPart;

// 修复：双重 cast
ModelPartAccessor acc = (ModelPartAccessor) (Object) modelPart;
```
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
| 返回类型 | 字段类型 | Mixin AP 结论 | 实际 |
|---|---|---|---|
| `boolean` | `boolean` | ✓ 通过 | ✓ |
| `int` | `int` | ✓ 通过 | ✓ |
| `Vector3f` | `Vector3f` | ✓ 通过 | ✓ |
| `Map<String, ModelPart>` | `Map` | ✓ 通过 | ✓ |
| **`Object`** | `T toAvoid` (泛型) | **✗ 失败** | ✓（运行时安全） |
| **`Object[]`** | `Polygon[]` | **✗ 失败** | ✓（运行时安全） |
**Accessor 名前缀用 `eyelib$`**（避免与其他 mod 冲突）
```bash
# 通过 mcmcp 拓展 或 bash gradlew 编译
mcmcp_build version="1.20.1"
# 或 bash: gradlew :1.20.1:compileJava :1.21.1:compileJava
```
**验证方法**：检查 `build/mixin/eyelib.refmap.json`，确认 accessor 有映射条目。

# 行为组件实现规范

> 本文档定义 eyelib 中 `io.github.tt432.eyelib.behavior` 包下 Bedrock 实体组件的实现契约。
> 所有组件（已有和新增）必须符合此规范。违反规范的代码是 bug。

## 基本模式

每个 Bedrock 组件对应一个 Java `record`，实现 `Component` 接口，提供静态 `Codec`。

### 模式 A: 标记组件（空字段 `{}`）

Bedrock JSON 中仅作为存在性标记，无数据字段。

```java
/**
 * minecraft:xxx — 简短描述。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Xxx() implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Xxx INSTANCE = new Xxx();
    public static final com.mojang.serialization.Codec<Xxx> CODEC = io.github.tt432.eyelib.util.codec.EyelibCodec.unit(INSTANCE);

    @Override
    public String id() { return "xxx"; }
}
```

**强制约束:**
- `INSTANCE` 必须 `public static final`
- 必须 import `com.mojang.serialization.Codec`，不 import `io.github.tt432.eyelib.behavior.component.Component`（用全限定名避免与 property 子包命名冲突）

### 模式 B: 数据组件（有字段）

```java
/**
 * minecraft:xxx — 简短描述。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Xxx(
        String requiredField,
        int optionalField
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final com.mojang.serialization.Codec<Xxx> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.create(ins -> ins.group(
                    com.mojang.serialization.Codec.STRING.fieldOf("required_field").forGetter(Xxx::requiredField),
                    com.mojang.serialization.Codec.INT.optionalFieldOf("optional_field", 0).forGetter(Xxx::optionalField)
            ).apply(ins, Xxx::new));

    @Override
    public String id() { return "xxx"; }
}
```

### 模式 C: 含 JSON 保留字段的组件

当 Bedrock 规范中某个字段是复杂嵌套对象（如 filter、event trigger），且尚未有 typed codec 时，用 `JsonObject` 保留原始数据：

```java
// 在类内定义共享 CODEC:
private static final Codec<JsonObject> JSON_FIELD = Codec.STRING.xmap(
        com.google.gson.JsonParser::parseString,
        com.google.gson.JsonElement::toString
).xmap(
        e -> e.getAsJsonObject(),
        o -> o
);
```

**注意:** `Codec.JSON_ELEMENT` **不存在**于 DFU 中，禁止使用。

## 强制约束

### API 存在性
| 禁止 (不存在) | 正确替代 |
|---|---|
| `Codec.JSON_ELEMENT` | `Codec.STRING.xmap(JsonParser::parseString, JsonElement::toString).xmap(...)` |
| `RecordCodecBuilder.group()` 超过 16 个参数 | 拆分为嵌套 MapCodec |

### CODEC 字段访问
- `RecordCodecBuilder` 的 `forGetter` 参数无条件使用 `ClassName::fieldName` 方法引用
- 不在此处写 lambda（如 `r -> r.fieldName()`）— 方法引用更短且类型推断正确

### 字段命名
- Java record component 使用 snake_case，与 Bedrock JSON key 一致
- `id()` 返回 Bedrock key 中 `minecraft:` 后缀部分（如 `"collision_box"`、`"is_baby"`）
- `@Nullable` 注解用 `org.jspecify.annotations.Nullable`

### required vs optional
检查 Bedrock schema form JSON 确定字段是 required 还是 optional。规则：
- 有明确默认值的 → `optionalFieldOf("name", defaultValue)`
- Bedrock 标记为 required 且无默认值 → `fieldOf("name")`
- Bedrock 标记为 optional 但无默认值 → `optionalFieldOf("name")`（生成 `Optional<T>`）

### 禁止项
- 禁止在 switch 中使用 lambda `{}` 块（`default -> { ...; return ...; }` 除外）
- 禁止 import `Component` 接口（用全限定名 `io.github.tt432.eyelib.behavior.component.Component`，避免同名冲突）
- 禁止 `INSTANCE` 为 private
- 禁止在非目标文件中写入无关代码

## DISPATCH_CODEC 接线

`ComponentGroup.DISPATCH_CODEC` 的 switch 按字母序排列所有 case。格式：
```java
case "minecraft:xxx" -> Xxx.CODEC;
```
default 分支保留 `EmptyComponent` 日志+兜底逻辑。

## 验证命令

通过 JetBrains MCP 调用(见 AGENTS.md Tooling Restrictions + `eyelib-build` SKILL):

- 编译: `jetbrain_build_project`(或 `jetbrain_run_gradle_tasks(["compileJava"])`)
- 测试: `jetbrain_run_gradle_tasks([":1.20.1:test"])`,active Stonecutter node 前缀见 `stonecutter.gradle`

## 验收标准

1. 编译零错误
2. CODEC 能正确反序列化一个最小 Bedrock JSON 示例（往返测试）
3. `id()` 返回值与 `minecraft:` key 后缀一致
4. `@author TT432` 类级 Javadoc 存在；包内 `package-info.java` 带 `@NullMarked`

# `texture.material` — per-material-pass 纹理求值

## Bedrock 规范

### 官方文档来源

| 源 | 路径 | 关键信息 |
|---|---|---|
| RC Schema | `minecraft-creator/content/forms/visual/render_controller.v1.8.0.form.json` | `textures`: `stringArray`, 每个元素是 Molang 字符串 |
| RC 示例 | `creator/Documents/Animations/AnimationRenderController.md` | 所有示例用显式短名（`Texture.default`、`Array.skins[...]`） |
| Molang Query | 官方 Molang Query Functions 文档 | **无 `texture.material` 查询函数** |

### Bedrock 引擎行为

`texture.material` 不是标准 Molang 查询函数，是 Bedrock 引擎的**运行时注入**机制：

- 渲染每个材质槽时，引擎将当前材质的 **short name** 注入 scope 的 `material` 变量
- `texture.material` 求值 → `texture.<shortName>` → `entity.textures[shortName]`
- render armor → `texture.armor` → entity.textures["armor"]
- render kipfdc → `texture.kipfdc` → entity.textures["kipfdc"]
- 每个材质 pass 得到**不同的纹理**

## 根因：MolangMappingTree 字段查找

### 完整编译器链路

1. Parser: `"texture.material"` → `MemberAccessExpr(owner=IdentifierExpr("texture"), memberName="material")`
2. Binder: → `BoundMemberAccessExpr(owner=BoundIdentifierExpr("texture"), member="material")`
3. Codegen（第149-153行）: `memberAccessName()` 递归收集段 → `"texture.material"` → 生成 `invokestatic(resolveMemberAccess, scope, "texture.material")`
4. `resolveMemberAccess`（第41-78行）:
   a. `scope.get("texture.material")` — 精确 key 查找，不命中 → `MolangNull`
   b. `mappingTree.findField("texture.material")` — **这里才是关键！**

### `MolangMappingTree.findField` 对 dotted name 的处理（第161-187行）

```java
// "texture.material" → scopeName="texture", fieldName="material"
int i = name.indexOf(".");
String scopeName = name.substring(0, i);  // "texture"
String fieldName = name.substring(i + 1);  // "material"

Node node = findNode(scopeName);           // 查找注册在 "texture" scope 下的 MolangClass
for (MolangClass clazz : node.actualClasses) {
    return new FieldData(clazz, clazz.classInstance.getField(fieldName));
    // ↑ 反射查找字段！若某 MolangClass 有 `material` 字段，直接返回其值
}
```

**如果 Molang 映射系统注册了一个 `texture` scope 的类，且该类有 `material` 静态字段，`resolveMemberAccess` 就不会返回 `MolangNull`，而是返回该字段的值。**

### `MolangFloat(0.0)` 的来源

运行时 `/eval` 验证：`new MolangValue("texture.material").getObject(scope)` 返回 `MolangFloat(0.0)`，但 `scope.get("texture.material")` 返回 `MolangNull`。这说明 `resolveMemberAccess` 中的 `findField` 命中了某个注册在 `texture` scope 下的 MolangClass，且该类的 `material` 字段值为 `0.0f`（或其 `asFloat()` 被调用）。

这个字段的值是 **编译时静态的**（`field.get(null)` 读取静态字段），不依赖 scope 上下文——所以无论 scope 里怎么 override，`texture.material` 始终返回同一个值。

## Molang 编译器正确性验证（2026-06-05）

`texture.material` 的 AST 解析、编译为字节码、运行时求值全链路已验证正确：

| 阶段 | 路径 | 结果 |
|------|------|------|
| Tokenizer | `texture` → IDENTIFIER, `.` → DOT, `material` → IDENTIFIER | ✅ |
| Parser | `parsePrimary → parsePostfix → MemberAccessExpr(IdentifierExpr("texture"), "material")` | ✅ |
| Binder | `BoundMemberAccessExpr(owner=BoundIdentifierExpr("texture"), memberName="material")` | ✅ |
| Bytecode | `resolveMemberAccess(scope, "texture.material")` | ✅ |
| Runtime | `scope.get("texture.material")` → `MolangNull` → `findField`(null) → `findMethod`(null) → `MolangNull` | ✅ |
| Fallback | `get()` 方法 `MolangNull` → `map.get("material")` → null → `"minecraft:null"` | ✅ |

**⚠️ Janino 变量污染陷阱**：`/eval` 中多次调用共用进程，Janino 累积旧变量名。症状：先调用的变量值"泄漏"到后调用，导致 `scope.get()` / `resolveMemberAccess()` 返回错误值（如 `MolangFloat(0.0)`、被污染的 `MolangString`）。每次 `/eval` 用唯一变量名前缀（如 `_t1`, `_t2`）。

当前 `resolveSlotTexture` 用 scope override hack 绕过问题，**这是掩盖而非修复**。正确方案：

1. **查 Molang 映射注册**：找到注册在 `texture` scope 下的 `MolangClass` 和其 `material` 字段，理解为什么值是 `0.0f`
2. **注册实体纹理到 Molang 映射树**：将 entity textures map 注册为 `texture` scope 下的可动态查询的 Molang 对象
3. **或者注册 `material` 为动态字段/方法**：返回当前渲染的材质短名，使 `texture.material` → `texture.<shortName>` → `entity.textures[shortName]`
    // evaluate Molang expression — finds overridden value
    result = mv.getObject(scope);
} finally {
    if (hadOld) scope.set("texture.material", saved);
    else scope.remove("texture.material");
}
```

### `MolangFloat(0.0)` 的 `get` 回退陷阱

`RenderControllerEntry.get()` 中，Molang 求值返回 `MolangFloat(0.0)`（非 Null/String/DynamicObject/Array）时走 else 分支：
```java
var r = map.get(object.asString().toLowerCase().replace(type + ".", ""));
```
`MolangFloat(0.0).asString()` = `"0.0"` → `map.get("0.0")` → null → `"minecraft:null"` → `complex:minecraft_null`。scope override 是避免此问题的正确方案。

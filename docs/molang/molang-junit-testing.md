# Molang 纯 JUnit 测试陷阱 (2026-06-08)

## 映射树为空 — 需手动注册

`CompileContext.defaults()` 使用 `MolangMappingTree.INSTANCE`，该单例在 Forge 启动时通过 `ForgeMolangMappingDiscovery` 填充。

在纯 JUnit 测试中（不启动 MC），映射树为空 → `math.abs()`、`math.sin()` 等运行时解析失败为 `MolangNull`。

修复：在 `@BeforeEach` 中手动注册：
```java
@BeforeEach
void setUp() {
    compiler = new MolangCompilerImpl();
    if (MolangMappingTree.INSTANCE.findClasses("math").isEmpty()) {
        MolangMappingTree.INSTANCE.addNode("math",
            new MolangMappingTree.MolangClass(MolangMath.class, true));
    }
}
```

注意：`findClasses()` 返回 `List`（非 `findClass`），`MolangClass` 是内部 record。

## MolangValue 无 asFloat()

`MolangValue` 继承 `MolangObject`，后者的 `asFloat()` 需要 scope 参数：
- `molangValue.eval(scope).asFloat()` — 需要 MolangScope
- 在构造测试数据时直接比较 `MolangValue` 对象：
  ```java
  assertEquals(MolangValue.getConstant(10f), steady.spawnRate());
  ```
  而非 `assertEquals(10f, steady.spawnRate().asFloat(), 0.001f)`。

## for_each 循环在纯 JUnit 中不可用

`for_each` 语法需要 mappingTree 中注册对应的函数，纯 JUnit 中未注册 → `ExpressionCompileException`。
避免在纯 JUnit 测试中使用 `for_each`，或手动注册对应的处理函数。

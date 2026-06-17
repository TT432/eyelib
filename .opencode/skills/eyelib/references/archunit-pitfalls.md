# ArchUnit 陷阱与模式 (2026-06-08)

## ArchUnit 1.3.0 API 限制

项目使用的 ArchUnit 1.3.0 有明显 API 缺口：

### areNot() 不存在
```java
// ❌ 不可用
.that().areNot(EXCLUDED)
.that().are(not(EXCLUDED))

// ✅ 正确：内联 DescribedPredicate
.that(DescribedPredicate.describe("not excluded",
    c -> !(c.getSimpleName().equals("SomeClass"))))
```

### 包名尾缀匹配陷阱
`getPackageName().contains(".client.")` 不匹配 `io.github.tt432.pkg.client`（无尾随点）。
使用 regex: `getPackageName().matches(".*\\.client($|\\..*)")`

### Inner class 排除陷阱
`c.getSimpleName().equals("OuterClass")` 不匹配 `OuterClass$1` / `OuterClass$Inner`。
使用 `c.getName().startsWith("full.package.OuterClass")` 或按 package 排除。

### Inner class 声明位置
`MolangClass` 是 `MolangMappingTree` 内部 record，需要 `import ...MolangMappingTree.MolangClass`。
`findClasses()` 返回 `List`，不是 `findClass` 单个对象。

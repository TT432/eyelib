# 骨骼级材质 — 多 ModelComponent 渲染

> ✅ **已修正（2026-06-05）**：Mojang 官方文档确认 `materials` 使用**分区覆盖**语义（后覆盖前，每个骨骼一个材质）。
> `*` 不再特殊提取，作为普通槽位按数组位置参与覆盖。
> 详见 `references/rc-materials-discrepancies.md`。

## 背景

Bedrock RC 的 `materials` 数组支持骨骼级材质分配 pattern（如 `armor*`、`2dh923d6f5fg72f4q*`、`*`）。
每个骨骼 pattern 可对应不同材质（外皮 blending、内核 alphatest 等）。

## 实现模式

### 数据结构

`materials` 字段类型为 `List<MolangMapEntry>`（`MolangMapEntry` 在 `eyelib-molang` 包），保留 JSON `objectArray` 的顺序和重复键。

### RenderControllerEntry.setupModel()

返回 `List<ModelComponent>`。核心算法：遍历所有槽位（含 `*`），`boneMaterialMap.put()` 自动实现后面覆盖。

```java
// 按 materials 数组顺序处理所有槽位，后面覆盖前面
Map<Integer, String> boneMaterialMap = new LinkedHashMap<>();
for (var entry : materials) {
    String pattern = entry.key();
    String materialName = get(scope, entry.value(), "material", entity.materials());
    Set<Integer> matchedBones = matchBonePattern(pattern, models);
    for (int boneId : matchedBones) {
        boneMaterialMap.put(boneId, materialName); // 后面覆盖前面
    }
}

// 按材质名分组骨骼 → 每材质一个 ModelComponent
Map<String, Set<Integer>> materialBoneGroups = new LinkedHashMap<>();
for (var entry : boneMaterialMap.entrySet()) {
    materialBoneGroups.computeIfAbsent(entry.getValue(), k -> new LinkedHashSet<>())
                      .add(entry.getKey());
}
```

### matchBonePattern()

按 Bedrock RC 骨骼 pattern 匹配模型骨骼名：

- `*` 后缀 → 前缀匹配（`armor*` → `boneName.startsWith("armor")`）
- 无 `*` → 精确匹配
- 纯 `"*"` 或空前缀 → 匹配全部骨骼

```java
private static Set<Integer> matchBonePattern(String pattern, Collection<Model> models) {
    boolean isPrefix = pattern.endsWith("*");
    String lookup = isPrefix ? pattern.substring(0, pattern.length() - 1) : pattern;
    for (Model model : models) {
        for (var boneEntry : model.allBones().int2ObjectEntrySet()) {
            int boneId = boneEntry.getIntKey();
            if (boneId < 0) continue;
            String boneName = GlobalBoneIdHandler.get(boneId);
            if (boneName == null) continue;
            boolean match = pattern.equals("*") || lookup.isEmpty()
                    || (isPrefix ? boneName.startsWith(lookup) : boneName.equals(lookup));
            if (match) result.add(boneId);
        }
    }
}
```

### partVisibility

`ModelVisitor.visitBone()` 用 `params.partVisibility().getOrDefault(bone.id(), true)` → 不在 map 中的骨骼**默认可见**。
每个组件的 partVisibility 需全骨骼初始 `false`，组件负责的骨骼设 `true`，再交由 `evalPartVisibility` 叠加 RC 条件。

### EntityRenderSystem 适配

```java
components.addAll(renderControllerEntry.setupModel(...));
```

## 史莱姆案例

Actions & Stuff 史莱姆 RC：

| Slot | Pattern | 材质 | 覆盖后结果 |
|---|---|---|---|
| 0 | `armor*` | armor | （被 slot 2 覆盖） |
| 1 | `2dh...*` | kipfdc | （被 slot 2 覆盖） |
| 2 | `*` | klduzy | **全部骨骼 → klduzy** |
| 3 | `2dh...` | kipfdc | 精确 2dh 骨骼 → kipfdc（覆盖 slot 2） |

最终：2 个 ModelComponent — klduzy（7骨骼，alphatest 基底层）+ kipfdc（1骨骼，blending 外皮）。

## 关键文件

| 文件 | 改动 |
|---|---|
| `eyelib-molang/.../MolangMapEntry.java` | **新建** — 有序键值对 record |
| `RenderControllerEntry.java` | `materials` 字段 Map→List<MolangMapEntry>，`setupModel()` 简化 |
| `BrRenderControllerEntry.java` | CODEC 保留顺序和重复键 |
| `EntityRenderSystem.java` | `add` → `addAll` |
| `BrMaterialEntry.java` | `get/add/sub` name-index fallback；`add/sub` 用 `getBase(material)` |

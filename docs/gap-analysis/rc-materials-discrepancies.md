# RC materials → ModelComponent 拆分差异（已全部修复）

来源对比：Mojang Creator 文档 `AnimationRenderController.md`（"Saddle will override Mane, which will override TailA"）vs eyelib 2026-06-05 之前的实现。

## F1（已修复）：`*` 被特殊化为"剩余骨骼"而非普通槽位

**Mojang 文档**：Horse RC 示例明确 `materials` 数组按顺序处理，后面覆盖前面。`*` 就是普通槽位。

**旧 eyelib 行为**：`*` 被单独取出（`materials.get("*")`），跳过非 `*` 槽位的反向去重流程，最后处理"剩余骨骼"。

**修复**：`setupModel()` 遍历所有槽位（含 `*`），`boneMaterialMap.put(boneId, materialName)` 自动实现后面覆盖。`*` 不再特殊处理。

## F2（已修复）：CODEC Map 折叠丢失同 pattern 多条目

**Schema**：`materials` 是 `objectArray`，允许 `[{ "*": "a" }, { "*": "b" }]`。

**旧 CODEC**：`Codec.unboundedMap(...).listOf().xmap(putAll merge)` 折叠为 `{ "*": "b" }`。

**修复**：新增 `MolangMapEntry(String key, MolangValue value)` record（在 `molang` 包）。`materials` 字段类型从 `Map<String, MolangValue>` 改为 `List<MolangMapEntry>`，保留顺序和重复键。

## F3（已修复）：`BrMaterialEntry.getRenderType(materials)` 缺少 alphatest 检查

`BrMaterialEntry.getRenderType(texture, materials)` 缺少 `isAlphatest(materials)` 检查，alphatest 材质错误返回 `entitySolid`。

**修复**：加一行 `if (isAlphatest(materials)) return RenderType.entityCutoutNoCull(texture);`，与 `ModelComponent.getRenderType()` 一致。

## F4（架构）：分区模型方向正确

Mojang 文档确认 `materials` 是分区/覆盖语义（后面覆盖前面），不创建重叠 pass。eyelib 的分区模型方向正确。

## F5（已修复）：`RenderTypeResolver.resolve()` 缺少 alphatest/cutout 路由

**修复**：加 `case "minecraft:cutout_no_cull"` → `entityCutoutNoCull` 分支。

## 影响文件

| 文件 | 改动 |
|---|---|
| `src/main/java/io/github/tt432/eyelib/molang/.../MolangMapEntry.java` | **新建** |
| `src/main/java/io/github/tt432/eyelib/importer/.../BrRenderControllerEntry.java` | `materials` 字段 Map→List<MolangMapEntry> |
| `.../RenderControllerEntry.java` | 同上 + `setupModel()` 重写 |
| `src/main/java/io/github/tt432/eyelib/material/.../BrMaterialEntry.java` | +1行 alphatest 检查 |
| `src/main/java/io/github/tt432/eyelib/material/.../RenderTypeResolver.java` | +1 case 分支 |

## G1（待实现）：运行时变体选择仅按名称匹配，非 Molang 驱动

**Bedrock 语义**：材质变体（`variants`）在运行时由 Molang 查询（如 `query.has_variant`）驱动选择——同一材质在不同变体下解析为不同条目。

**eyelib 现状**：`BrMaterialEntry.getVariant(variantName)` 仅按名称匹配（`BrMaterialResolver` 收集变体到 `ResolvedBrMaterial.variants`），尚无 Molang 驱动的运行时选择逻辑，也无渲染管线消费方。

**方向**：以 Molang 查询结果作为变体键，接入材质解析链路；待渲染管线接入 `ResolvedBrMaterial` 后实施。

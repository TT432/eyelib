# Bedrock RC materials 覆盖语义 — eyelib 差距分析

> **2026-06-05 修正**：此文档原先错误声称 Bedrock 使用"重叠 pass"模型（同一骨骼出现在多个 pass）。
> Mojang 官方 Creator 文档（AnimationRenderController.md）中 Horse RC 示例明确说
> **"Saddle will override Mane, which will override TailA"** —— 这是**分区覆盖**语义，每个骨骼最终只有一个材质。

来源：Mojang Creator 文档 + 官方 schema form (`render_controller.v1.8.0.form.json`)

## Bedrock 实际语义（官方文档确认）

### `materials` 是 `objectArray` — 按序覆盖，后面覆盖前面

```json
"materials": [
    { "*": "Material.default" },
    { "TailA": "Material.horse_hair" },
    { "Mane": "Material.horse_hair" },
    { "*Saddle*": "Material.horse_saddle" }
]
```

Mojang 官方注释：**"Saddle will override Mane, which will override TailA"**。

处理顺序：
1. `*` → 所有骨骼 = default
2. `TailA` → TailA 骨骼被覆盖为 horse_hair
3. `Mane` → Mane 骨骼被覆盖为 horse_hair
4. `*Saddle*` → Saddle 骨骼被覆盖为 horse_saddle

最终每个骨骼只有一个材质。这是**分区覆盖**模型，不是重叠 pass。

### 关键语义

- **按数组顺序处理，后覆盖前**——每条 entry 是材质**赋值**，不是独立的 render pass
- **`*` 是普通槽位**，在数组中的位置决定它覆盖谁、被谁覆盖
- **同一骨骼只有一个最终材质**——不同 slot 不会为同一骨骼创建多个 render pass
- **GPU pass 数 = 唯一材质数**，不是槽位数（同材质的骨骼可以被合批）

### 史莱姆案例（Actions & Stuff）——按官方语义重新分析

```json
"materials": [
    { "armor*": "Material.armor" },                     // slot 0
    { "2dh923d6f5fg72f4q*": "Material.kipfdc" },        // slot 1
    { "*": "Material.klduzy" },                          // slot 2
    { "2dh923d6f5fg72f4q": "Material.kipfdc" }           // slot 3
]
```

按序覆盖：
1. armor* 骨骼 → armor
2. 2dh...* 骨骼 → kipfdc（覆盖与 armor* 重叠的骨骼）
3. **全部骨骼** → klduzy（覆盖 slot 0 和 slot 1 的所有骨骼！）
4. 2dh...（精确）→ kipfdc（覆盖 slot 2 对此骨骼的赋值）

最终：
| 骨骼 | 材质 |
|---|---|
| armor* | klduzy（被 slot 2 覆盖） |
| 2dh...（精确） | kipfdc（slot 3 覆盖 slot 2） |
| 2dh...*（非精确） | klduzy（被 slot 2 覆盖） |
| body 等 | klduzy |

## eyelib 当前实现

eyelib 反向去重产生分区，方向正确，但 `*` 的处理有偏差：

- ✅ 分区覆盖模型本身方向正确
- ❌ `*` 被特殊提取为 `wildcardMaterial`，在特定 pattern 之后才处理"剩余骨骼"
- ❌ 这导致 `*` 不参与覆盖竞争：`*` 之前的 pattern 不被 `*` 覆盖，`*` 之后的 pattern 也不覆盖 `*`

## 具体差异（已全部修复 2026-06-05）

### D1: Map 折叠丢失重复 pattern ✅ 已修复
CODEC 从 `Map` 改为 `List<MolangMapEntry>`，保留顺序和重复键。

### D2: `*` 被特殊化为剩余骨骼 ✅ 已修复
`setupModel()` 重写：所有槽位统一按序覆盖，`*` 不再特殊处理。

### D3: BrMaterialEntry.getRenderType() 缺少 alphatest 检查 ✅ 已修复
添加了 `if (isAlphatest(materials)) return entityCutoutNoCull`。

## 修复方向

1. **CODEC 层面**：`materials` 和 `part_visibility` 应保留为有序列表（List of pattern→value pairs），而非 Map
2. **`*` 处理**：移除特殊化，作为普通 slot 参与统一的按序覆盖逻辑
3. **BrMaterialEntry.getRenderType()**：添加 `isAlphatest()` 检查

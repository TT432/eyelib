# Bedrock RC materials 字段规范

来源：基岩版官方 schema form（`minecraft-creator/content/forms/visual/render_controller.v1.8.0.form.json`）

## Schema 定义

```json
{
  "id": "materials",
  "dataType": "objectArray",
  "subForm": {
    "fields": [
      {
        "dataType": "molang",
        "id": "[a-zA-Z0-9_.:*]+",
        "title": "+",
        "isRequired": true
      }
    ]
  }
}
```

`materials` 是 `objectArray`（对象数组），每个元素是一对 `{ pattern: MolangExpr }`。

## 关键语义（来自 Bedrock Wiki）

- `"*"` = 所有骨骼（"apply this material to all bones in our model"）
- 其他 key（如 `"armor*"`）匹配特定骨骼名/前缀
- **Later-overrides-earlier**：后面的 pattern 覆盖前面的（来源：Horse RC "Saddle will override Mane, which will override TailA"）
- 每个 material slot 在引擎中创建独立的 render call/draw pass
- **同 pattern 允许多次出现**（如两个 `*`），每次独立创建 pass

## eyelib 实现对照（当前状态）

> ⚠️ eyelib 当前采用分区模型，与 Bedrock 重叠 pass 语义存在架构偏差。
> 完整差异分析见 `references/bedrock-materials-pass-semantics.md`。
- `arrays.materials` 可定义材质数组，供 Molang 索引选择

## eyelib 实现对照

| 概念 | Bedrock | eyelib |
|------|---------|--------|
| 数据结构 | objectArray → 多个 {pattern:expr} | LinkedHashMap（保序） |
| `"*"` 通配 | 所有骨骼 | `wildcardMaterial = materials.get("*")` |
| 骨骼匹配 | 前缀 `armor*` / 精确 | `matchBonePattern()` |
| 覆盖语义 | 后覆盖前 | 反向去重 |
| 渲染次数 | 每 slot 一次 | 每 slot 一个 ModelComponent |

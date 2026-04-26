# Bedrock Add-On 行为文件层参考

## 目的

这一组文档继续向下展开到 Behavior Pack 的文件层，回答的问题是：

1. 某个行为资源族应该放在哪个路径。
2. 顶层对象名和主要区段是什么。
3. 哪些是官方稳定参考，哪些只是教程、样例或实践归纳。

## 当前覆盖的资源族

- `behavior_entities`
- `表类资源`（`blocks`、`items`、`recipes`、`loot_tables`、`trading`、`spawn_rules`、`structures`）

## 递归索引

如果你已经不是在看“文件层”，而是在看“字段层 / 子字段层 / registry frontier”，优先跳到：

- `递归字段索引-bp-entity-table.md`

这页会把 `minecraft:entity`、`minecraft:spawn_rules`、`minecraft:block`、`minecraft:item`、各种 recipe subtype、loot/trading 结构，继续拆到原子字段或明确的开放 registry 边界。

## 阅读方式

- 想知道“这个行为文件该放哪儿” → 看对应专题页的“位置矩阵”
- 想知道“顶层对象和主要区段是什么” → 看对应专题页的“结构矩阵”
- 想知道“这是正式参考还是样例/教程实践” → 看每页里的“规范性”列与“边界说明”

## 当前未单独展开的项

- BP `texts/` 路径在 `都是什么结构.md` 中已有结构层记录，但本轮没有单独拆出 Behavior Pack localization 专题页。

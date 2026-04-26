# Bedrock Add-On 资源文件层参考

## 目的

这一组文档继续向下展开到“资源文件层”，回答的问题是：

1. 某个资源族应该放在哪个路径。
2. 顶层对象名和主要区段是什么。
3. 哪些内容属于官方稳定参考，哪些只是教程、样例或社区实践。

## 当前覆盖的资源族

- `render_controllers`
- `client_entity`
- `particles`
- `sounds`
- `materials`
- `animations`
- `animation_controllers`
- `geometry / models`
- `attachables`
- `localization`
- `texture indices`
- `ui / fogs`

> `texts/*.lang` 现在已经有官方教程路径（O8-ENTITYTYPES）与仓内 importer 证据（I6、I7），所以纳入资源文件层参考；但它目前仍更适合作为轻量 family page，而不是进一步拆成多张字段专题页。

## 证据分层

这一层文档现在统一按五种证据写：

1. **S 类**：官方机器可读 schema，优先用来确认对象名、字段名、嵌套层级与可选项边界。
2. **O 类**：官方文档页、reference 页、validation 页，优先用来确认概念、路径入口、字段语义和版本说明。
3. **R 类**：真实样例仓库，既包含官方样例，也包含高可信社区样例，用来说明“现实里确实这样写过”。
4. **C 类**：社区解释，只用来补边界，不单独抬升为规范。
5. **I 类**：仓内 importer / fixture 证据，只说明 Eyelib 当前是否识别、如何分类、哪里还没建 typed schema。

尤其要分开两件事：

- **官方存在该资源族**
- **Eyelib 当前已经对该资源族建了 typed schema**

例如 `fogs/` 在官方文档和官方样例里都成立，但仓内当前仍属于 `NO_TYPED_SCHEMA_YET`；这不能写成“官方不规范”，只能写成“仓内暂未建模”。

## 阅读方式

- 想知道“这个资源文件该放哪儿” → 先看对应专题页的“位置矩阵”
- 想知道“顶层对象和主要字段是什么” → 看对应专题页的“结构矩阵”
- 想知道“这是正式参考还是样例实践” → 看每页里的“规范性”列与“边界说明”

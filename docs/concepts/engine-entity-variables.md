# Bedrock 引擎提供的实体变量（engine entity variables）

## 问题

Bedrock 资源包的 `variable.*` 不全是脚本写入的：一部分由引擎按实体状态直接提供。
官方羊驼就是例子——`render_controllers/llama.render_controllers.json`（v1/v2）引用

```json
"(query.mark_variant == 1 && variable.DecorTextureIndex == 0)
    ? Texture.decor_wandering_trader
    : Array.decor[variable.DecorTextureIndex]"
```

而 `entity/llama.entity.json` 与动画都不写 `DecorTextureIndex`，值完全来自引擎
（0 = 无装饰，1..16 = 地毯颜色 id + 1）。Java 侧必须按 JE 实体状态复刻。

## 设计

不加实体专用的渲染回调，而是把「引擎变量」作为一类可扩展能力：

| 组件 | 位置 | 职责 |
|------|------|------|
| `EngineEntityVariable` | `molang`（application） | 规范化变量名（如 `variable.DecorTextureIndex`）的枚举 |
| `RenderSystemPort#engineVariable(entity, variable)` | `bridge/client/adapter` | 宿主按实体状态解析；`Float.NaN` = 不适用于该实体 |
| `EngineEntityVariables` | `client/render`（application） | 统一绑定到 `MolangScope`：惰性 supplier，已在 scope 中的同名变量不覆盖 |
| `ClientEntitySetup#setupExtraMolang` | `client/render` | 每帧调用一次 `bind`（首次实际绑定，后续 `contains` 短路） |

规则：

- **惰性**：绑定的是 supplier，读时才解析；装备变化下一帧自动生效，无逐帧 `set`。
- **包内优先**：`scripts.initialize`/`animate` 或动画关键帧已写同名变量时跳过绑定
  （Bedrock 里 variable 本来就是实体上的可写存储）。
- **版本差异留在 adapter**：JE 的 `Llama` 包名（`animal.horse` / `animal.equine`）、
  装备槽常量（`inventory` vs `getInventory()`）不出现在 application。
- **只有官方拼写进入枚举**；大小写差异由 Molang 作用域规范化解决（见下）。

现有内置变量：

| 变量 | 适用实体 | 值 |
|------|----------|----|
| `variable.DecorTextureIndex` | `Llama` | 0 = 无装饰；1..16 = `WoolCarpetBlock` 颜色 id + 1 |

## 与其它语义的耦合（本次一并修正）

1. **变量名大小写不敏感**：官方 Molang 文档「Case Sensitivity」明确除字符串外全部不敏感。
   `MolangScope` 与 `MolangStruct` 在存取时统一转小写（无大写字母时零分配），
   因此 `variable.DecorTextureIndex` 与 `variable.decortextureindex` 是同一个变量。
2. **`==`/`!=` 的数值语义**：编译产物调用 `MolangObject#equalsF`；旧默认实现用对象
   `equals`，惰性 supplier 变量与字面量 `0` 会判不等。现改为数值类型之间按 `asFloat`
   比较（官方「all numerical values are floats」），`nEqualsF = 1 - equalsF`，
   非数值类型仍按对象相等。

## 验证

- 三版本实机：行商羊驼第二层为 `trader_llama_decor`，普通羊驼为 `decor_none`，
  运行时 `modelComponents=3`、`renderType=minecraft:llama`。
- 1.20.1 用小写 `variable.decortextureindex` 读回 `0.0`，验证大小写不敏感。
- 单测：`MolangEqualityTest`（数值跨惰性供应商相等、数值/字符串/空值不相等）、
  `MolangScopeTest`（变量与 struct 成员大小写不敏感）。

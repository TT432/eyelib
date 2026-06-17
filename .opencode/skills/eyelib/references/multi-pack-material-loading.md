# 多 Pack/Addon .material 加载行为

完整的加载流程与分析，包括与 Bedrock 官方合并规则的差异。

## 加载流程（两阶段）

### 阶段1：资源重载 → BrMaterialLoader

```
MC ResourceManager 扫描所有资源包
  → 收集 eyelib/materials/*.material（所有 pack 的所有文件）
  → BrMaterialLoader.apply()
      → 逐文件解析 BrMaterial.CODEC
      → 全部 flatten 进一个 LinkedHashMap
      → MaterialManager.INSTANCE.replaceAll(flattened)
```

关键行为：

- **同路径文件**（如两个 pack 都有 `eyelib/materials/entity.material`）→ MC 层按 pack 优先级只保留高优先级版本。低优先级文件内容不会进入 `apply()`。
- **不同文件名**（`pack_a.material` / `pack_b.material`）→ 两者都被收集，flatten 到同一 map。
- **同 key 冲突** → `LinkedHashMap.forEach(flattened::put)`，后迭代到的覆盖前。迭代顺序取决于 `LoaderParsingOps.parseBySourceKey` 中 `Map.forEach` 遍历的 `Map<ResourceLocation, JsonElement>` 顺序（MC 内部是 pack 优先级排序的 LinkedHashMap）。
- **`replaceAll` = 全量替换**：清空 MaterialManager，写入扁平化所有条目。此时 MaterialManager 里只有 `.material` 文件的内容（包括 eyelib 自己的 `vanilla.material`）。

### 阶段2：Addon 加载 → BedrockAddonRuntimeBridge

```
BedrockAddonAutoLoader 扫描 resourcepacks/*.mcpack / *.mcaddon
  → BedrockAddonLoader.load() 解析每个 addon
  → BedrockAddonRuntimeBridge.replaceFromResourcePack()
      → MaterialManager.INSTANCE::put  逐条叠加
```

关键行为：

- **`put()` = 增量叠加**：不清空，在阶段1的基础上逐条写入。同 key 覆盖。
- 多个 addon → 按 `resourcepacks/` 目录扫描顺序逐个 bridge，**后加载的 addon 的同名条目覆盖先加载的**。
- Bridge 注释明确写着「叠加而非替换，保留 BrMaterialLoader 加载的 vanilla 条目」——此前已修复的 bug（原来用 `replaceAll` 把 vanilla 清掉了，见材料继承陷阱）。

## 与 Bedrock 官方合并规则的差异

### Bedrock 多文件合并规则

来源：`/mnt/e/_____基岩版文档/bedrock-wiki/docs/documentation/material-config-description.md`

Bedrock 通过 `fancy.json`/`sad.json`/`common.json` 等质量配置文件显式列出要加载的 .material 文件列表。当同一材质名称出现在多个文件中时：

1. **普通字段**：后加载覆盖前。
2. `defines`、`states`、`samplerStates` 支持 `+`（添加）和 `-`（删除）前缀。
3. **合并顺序**：全部覆盖操作 → 全部添加操作 → 全部删除操作。
4. **删除终极优先级**：任何文件声明了 `-defines: [MACRO_3]`，合成后的材质必定没有 MACRO_3，无论其他文件怎么 add。

### eyelib 对照

| 方面 | Bedrock | Eyelib |
|------|---------|--------|
| 文件列表 | 质量配置 JSON 显式列出 | MC 资源重载 flat scan |
| 同 key 普通字段 | 后加载覆盖前 | LinkedHashMap 迭代最后写入胜 |
| `+defines`/`-defines` | 跨文件合并操作符：覆盖→添加→删除，删除最终优先 | CODEC 解析为 `defines.add()`/`defines.sub()`，仅在单条**继承链**内由 `ModifyAble.toList()` 解析，**无跨文件合并逻辑** |
| `+states`/`-states` | 同上 | 同上 |
| 同路径文件的处理 | 质量配置决定加载顺序 | MC pack 优先级决定，低优先级文件直接被丢弃 |

**核心差距**：Bedrock 的 `-defines` 具有跨文件终极优先级。eyelib 做不到——eyelib 的 `+`/`-` 只在 `ModifyAble` 继承链（parent→child）内生效，多条目同名冲突简单用 `LinkedHashMap.put()` 覆盖。

## 场景速查

| 场景 | 行为 |
|------|------|
| Pack A 和 Pack B 都有 `entity.material`（同名路径） | MC 按优先级只取一个，另一个被丢弃 |
| Pack A 有 `a.material`，Pack B 有 `b.material`，都定义了 `"foo:bar"` | 两者都被加载，`foo:bar` = 后迭代到的那个 |
| .mcpack addon 的材质 | 在 `.material` 文件之后加载，`put()` 覆盖同名 key |
| 两个 .mcpack 都有同名材质条目 | 后扫描到的 addon 覆盖前一个 |
| `+defines` 跨两个 pack 文件 | eyelib 不合并——每个 entry 独立 CODEC 解析，后迭代到的完全覆盖前一个 |

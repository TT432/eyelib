# Gap Analysis: loot_tables + trading + spawn_rules + structures

> 调查日期：2026-06-02
> 范围：eyelib 实现 vs 基岩版行为包规范

---

## 1. 概览矩阵

| 资源族 | 路径模式 | 文件格式 | family 分类 | importer 状态 | runtime 消费者 |
|---|---|---|---|---|---|
| loot_tables | `loot_tables/**/*.json` | JSON | LOOT_TABLE | ✅ managed (BrLootTable) | ❌ 无 |
| trading | `trading/**/*.json` | JSON | TRADING | ⚠️ 死代码 (BrTrading 存在但未接线) | ❌ 无 |
| spawn_rules | `spawn_rules/*.json` | JSON | SPAWN_RULE | ✅ managed (BrSpawnRule) | ❌ 无 |
| structures | `structures/*.mcstructure` | 二进制 NBT | STRUCTURE | ❌ unmanaged (无 codec) | ❌ 无 |

---

## 2. loot_tables — 字段级差距

### 2.1 现有实现

- **文件**: `BrLootTable.java`（`io.github.tt432.eyelibimporter.addon`）
- **顶层结构**: `BrLootTable` → `pools: List<BrLootTablePool>`
- **Pool 结构**: `rolls`（Either<Integer, Range>）+ `entries` + `conditions`（raw）+ `functions`（raw）
- **Entry 结构**: `type` + `name` + `weight` + `quality` + `conditions`（raw）+ `functions`（raw）
- **接线**: `BedrockAddonLoader.processEntry` 中的 `case LOOT_TABLE` 通过 `parseAndStore(entry, BrLootTable.CODEC, lootTableFiles)` 解析

### 2.2 已建模 vs 未建模字段

| Bedrock 字段 | 层级 | eyelib 状态 | 备注 |
|---|---|---|---|
| `pools` | root | ✅ typed `List<BrLootTablePool>` | |
| `pools[*].rolls` | pool | ✅ Either<Integer, Range> | 整数或 {min, max} |
| `pools[*].tiers` | pool | ❌ 缺少 | 含 `initial_range`, `bonus_rolls`, `bonus_chance`；用于实体装备表（如 zombie_equipment.json） |
| `pools[*].conditions` | pool | ⚠️ raw ObjectValue 列表 | 无 typed schema（如 `random_regional_difficulty_chance`, `random_difficulty_chance`） |
| `pools[*].functions` | pool | ⚠️ raw ObjectValue 列表 | 无 typed schema |
| `pools[*].entries` | pool | ✅ typed `List<BrLootTablePoolEntry>` | |
| `entries[*].type` | entry | ✅ typed `String` | 但无类型枚举校验（item/loot_table/empty/seeded） |
| `entries[*].name` | entry | ✅ typed `String` | |
| `entries[*].weight` | entry | ✅ typed `int`（默认 1） | |
| `entries[*].quality` | entry | ✅ typed `int`（默认 0） | |
| `entries[*].conditions` | entry | ⚠️ raw ObjectValue 列表 | 无 typed schema |
| `entries[*].functions` | entry | ⚠️ raw ObjectValue 列表 | 无 typed schema（如 set_count, set_damage, enchant_random_gear, set_data, looting_enchant, furnace_smelt 等） |

### 2.3 关键差距

1. **`pools[*].tiers` 缺失**: Bedrock 官方 zombie_equipment.json 第二个 pool 包含 `tiers` 字段，当前 codec 解析会失败
2. **functions/conditions 无 typed schema**: 保留为 raw `BedrockResourceValue.ObjectValue`，无法在 importer 层验证或使用
3. **无 runtime 消费者**: `BedrockAddonAggregate` 中 `lootTableFiles()` 返回 Map，但 eyelib 根模块未引用

---

## 3. trading — 字段级差距

### 3.1 现有实现

- **文件**: `BrTrading.java`（`io.github.tt432.eyelibimporter.trading` 包）
- **顶层结构**: `BrTrading` → `tiers: List<BrTier>`
- **Tier 结构**: `totalExpRequired` + Either<List\<BrTrade\>, List\<BrGroup>>
- **Group 结构**: `numToSelect` + `trades`
- **Trade 结构**: `wants` + `gives`（皆 List\<BrTradeEntry\>）
- **TradeEntry**: Either\<BrTradeItem, List\<BrTradeItem\>\>（单件或 choice）
- **TradeItem**: `item` + `quantity: BrQuantity`
- **接线**: ❌ **未接线** — `BedrockAddonLoader.processEntry` 中**没有** `case TRADING`，文件落入 `default` 分支 → 以 `OUTSIDE_IMPORTER_SCOPE` 存入 unmanaged 资源

### 3.2 已建模 vs 未建模字段

| Bedrock 字段 | 层级 | eyelib 状态 | 备注 |
|---|---|---|---|
| `tiers` | root | ✅ typed `List<BrTier>` | |
| `tiers[*].total_exp_required` | tier | ✅ typed `int`（默认 0） | |
| `tiers[*].trades` | tier | ✅ 作为 simple 格式的 left Either | 不含 `groups` 的直接格式 |
| `tiers[*].groups` | tier | ✅ 作为 group 格式的 right Either | |
| `groups[*].num_to_select` | group | ✅ typed `int`（默认 0） | |
| `groups[*].trades` | group | ✅ typed `List<BrTrade>` | |
| `trades[*].wants` | trade | ✅ typed `List<BrTradeEntry>` | |
| `trades[*].gives` | trade | ✅ typed `List<BrTradeEntry>` | |
| `trades[*].trader_exp` | trade | ❌ 缺少 | 交易获得经验值 |
| `trades[*].max_uses` | trade | ❌ 缺少 | 最大使用次数 |
| `trades[*].reward_exp` | trade | ❌ 缺少 | 是否奖励经验 |
| `wants[*].item` | entry item | ✅ typed `String` | |
| `wants[*].quantity` | entry item | ✅ typed `BrQuantity` | 支持整数或 {min, max} |
| `wants[*].price_multiplier` | entry item | ❌ 缺少 | 经济交易价格乘数 |
| `gives[*].item` | entry item | ✅ typed `String` | |
| `gives[*].quantity` | entry item | ✅ typed `BrQuantity` | |
| `gives[*].functions` | entry item | ❌ 缺少 | 用于给出售物品附加 enchant_book_for_trading 等 |

### 3.3 关键差距

1. **整个 BrTrading 为死代码**: 类存在、CODEC 完整，但未被 `BedrockAddonLoader.processEntry` 引用，`TRADING` 族的文件全部落入 unmanaged
2. **`trader_exp`, `max_uses`, `reward_exp` 缺失**: 这三个字段在 Bedrock 经济交易格式中是必选项
3. **`price_multiplier` 缺失**: want 物品的经济价格乘数
4. **`gives[*].functions` 缺失**: 给出售物品附加函数（如附魔书交易）
5. **`economy_trades/` 子目录**: 部分换皮版文件在 `trading/economy_trades/` 子目录下，当前的分类路径 `trading/**/*.json` 是否覆盖取决于 classify 逻辑（当前 `path.startsWith("trading/")` 可以匹配 `trading/economy_trades/*.json`，OK）

---

## 4. spawn_rules — 字段级差距

### 4.1 现有实现

- **文件**: `BrSpawnRule.java`（`io.github.tt432.etyelibimporter.addon`）
- **顶层结构**: `BrSpawnRule` → `formatVersion` + `identifier` + `populationControl` + `conditions`
- **接线**: `processEntry` 中 `case SPAWN_RULE` 通过 `BrSpawnRule.parse(readJsonFile(...))` 解析

### 4.2 已建模 vs 未建模字段

| Bedrock 字段 | 层级 | eyelib 状态 | 备注 |
|---|---|---|---|
| `format_version` | root | ✅ typed `String` | |
| `minecraft:spawn_rules` | root | ⚠️ parse 方法手动提取 | |
| `minecraft:spawn_rules.description.identifier` | description | ✅ typed `String` | parse 方法中提取 |
| `minecraft:spawn_rules.description.population_control` | description | ✅ typed `String` | parse 方法中提取 |
| `minecraft:spawn_rules.conditions` | conditions | ⚠️ raw ObjectValue 列表 | 完整结构未 typed 化 |
| `conditions[*].minecraft:spawns_on_surface` | condition | ❌ 无 typed | 布尔标记 |
| `conditions[*].minecraft:spawns_underground` | condition | ❌ 无 typed | 布尔标记 |
| `conditions[*].minecraft:brightness_filter` | condition | ❌ 无 typed | `min`, `max`, `adjust_for_weather` |
| `conditions[*].minecraft:difficulty_filter` | condition | ❌ 无 typed | `min`, `max`（难度字符串） |
| `conditions[*].minecraft:weight` | condition | ❌ 无 typed | `default` 权重 |
| `conditions[*].minecraft:herd` | condition | ❌ 无 typed | `min_size`, `max_size` |
| `conditions[*].minecraft:permute_type` | condition | ❌ 无 typed | 带 weight/entity_type 的数组 |
| `conditions[*].minecraft:biome_filter` | condition | ❌ 无 typed | filter 表达式（test/operator/value） |
| `conditions[*].minecraft:density_limit` | condition | ❌ 无 typed | `surface`, `underground` 密度限制 |
| `conditions[*].minecraft:is_persistent` | condition | ❌ 无 typed | 布尔标记 |
| `conditions[*].minecraft:spawns_on_block_filter` | condition | ❌ 无 typed | 方块列表过滤 |
| `conditions[*].minecraft:spawns_above_block_filter` | condition | ❌ 无 typed | 过滤距离+X |

### 4.3 关键差距

1. **conditions 的完整结构未 typed 化**: 每个 condition 包含 `minecraft:*` 组件，全部保留为 raw ObjectValue
2. **CODEC 的顶层结构不正确**: `BrSpawnRule.CODEC` 试图在根级找 `format_version`, `identifier` 等字段，但实际 JSON 根级是 `format_version` + `minecraft:spawn_rules`（嵌套对象），`parse` 方法处理正确但 CODEC 路径不一致（CODEC 要求 identifier 在根级，实际在 `minecraft:spawn_rules.description.identifier`）
3. **parse 方法与 CODEC 不一致**: `parse` 方法正确处理嵌套结构，但 `CODEC` 字段映射错误 — 如果通过 `parseAndStore`（使用 CODEC）会解析失败（注意当前 `processEntry` 用的是 `parse` 而非 `parseAndStore`，所以实际不会失败）
4. **无 runtime 消费者**: aggregate 中有 `spawnRulesFiles` map 但根模块未使用

---

## 5. structures — 差距

### 5.1 现有实现

- **文件**: ❌ 无
- **顶层结构**: 二进制 `.mcstructure` 文件（NBT 格式，非 JSON）
- **接线**: `processEntry` 中无 `case STRUCTURE`，通过 `default` 分支 → `OUTSIDE_IMPORTER_SCOPE`

### 5.2 关键差距

1. **零实现**: 完全没有 codec、parser 或数据模型
2. **非 JSON 格式**: `.mcstructure` 是二进制 NBT 格式，不能使用现有的 JSON codec 基础设施
3. **`BedrockResourceFamily.classify` 可识别**但立即落入 unmanaged
4. **可能使用 `.bin`/自定义格式**: 不同版本可能有不同的序列化格式

---

## 6. eyelib-util 通用 JSON codec 工具

| 工具 | 位置 | 用途可复用性 |
|---|---|---|
| `ImporterCodecUtil.JSON_ELEMENT_CODEC` | eyelib-importer | 将任意 JSON 元素编解码为 Codec 通道 |
| `ImporterCodecUtil.OBJECT_VALUE_CODEC` | eyelib-importer | 期望 JSON object 的 codec，用于 raw 字段占位 |
| `ImporterCodecUtil.BEDROCK_RESOURCE_VALUE_CODEC` | eyelib-importer | 通用 BedrockResourceValue codec |
| `ImporterCodecUtil.dispatchedMap` | eyelib-importer | 按 key 分发 codec 的 Map codec |
| `eyelib-util` 模块 | eyelib-util | 仅有 `TupleCodec`，无通用 JSON codec 工具 |

**结论**: 通用 JSON codec 工具集中在 `eyelib-importer` 模块的 `ImporterCodecUtil` 中，`eyelib-util` 模块没有相关能力。如果要将 loot/trading/spawn 的 typed schema 下沉到公共层，需要移动或复制 codec 工具到 `eyelib-util`。

---

## 7. 集成测试案例

### 7.1 loot_tables 测试

| 用例编号 | 场景 | Bedrock JSON 关键特征 | 预期 eyelib 行为 |
|---|---|---|---|
| LT-01 | 简单掉落表：整数 rolls | `{"pools":[{"rolls":1,"entries":[{"type":"item","name":"minecraft:stick","weight":1}]}]}` | BrLootTable 成功解析，pools[0].rolls=Left(1) |
| LT-02 | 范围 rolls | `{"pools":[{"rolls":{"min":1,"max":3},"entries":[...]}]}` | BrLootTable 成功解析，pools[0].rolls=Right({min:1, max:3}) |
| LT-03 | 多条件+函数 | 带 `conditions` 和 `functions` 的 pool（如 zombie_equipment.json） | 解析成功但 conditions/functions 保留为 raw ObjectValue |
| LT-04 | ❌ tiers 字段 | 包含 `tiers: {initial_range, bonus_rolls, bonus_chance}` 的 pool | **codec 解析失败** — BrLootTablePool 缺少 tiers 字段 |
| LT-05 | 子目录嵌套 | `loot_tables/entities/zombie.json`, `loot_tables/gameplay/fishing/junk.json` | classify 正确 → LOOT_TABLE |
| LT-06 | 文件不存在 | 无 `loot_tables/` 目录 | 不触发，aggregate 中为空 Map |
| LT-07 | 非法 JSON | 语法错误 | `SCHEMA_PARSE_FAILED` 警告，落入 unmanaged |

### 7.2 trading 测试

| 用例编号 | 场景 | Bedrock JSON 关键特征 | 预期 eyelib 行为 |
|---|---|---|---|
| TR-01 | ⚠️ 简单交易格式（无 groups） | `{"tiers":[{"trades":[{"wants":[...],"gives":[...]}]}]}` | **当前落入 unmanaged**（BrTrading 未接线） |
| TR-02 | ⚠️ 完整格式（含 groups） | `{"tiers":[{"total_exp_required":0,"groups":[{"num_to_select":1,"trades":[...]}]}]}` | **当前落入 unmanaged** |
| TR-03 | ⚠️ 经济交易格式 | 含 `price_multiplier`, `trader_exp`, `max_uses`, `reward_exp` | **当前落入 unmanaged**；即使接线，`price_multiplier` 等会导致 CODEC 忽略或失败 |
| TR-04 | ⚠️ `economy_trades/` 子目录 | `trading/economy_trades/librarian_trades.json` | classify 正确 → TRADING，但落入 unmanaged |
| TR-05 | ⚠️ 带 functions 的 gives | `"gives":[{"item":"minecraft:enchanted_book","functions":[...]}]` | **当前落入 unmanaged**；即使接线，functions 字段未建模 |

### 7.3 spawn_rules 测试

| 用例编号 | 场景 | Bedrock JSON 关键特征 | 预期 eyelib 行为 |
|---|---|---|---|
| SR-01 | 标准生成规则 | 带 `minecraft:spawn_rules` 嵌套的完整 spawn rule（如 zombie.json） | BrSpawnRule.parse 成功，conditions 保留为 raw ObjectValue |
| SR-02 | 多 condition 数组 | 多个 condition 对象 | 解析成功，conditions 列表中多个 raw ObjectValue |
| SR-03 | condition 内各种 minecraft:* 组件 | brightness_filter, weight, herd, biome_filter 等 | 保留为 raw ObjectValue，无 typed 提取 |
| SR-04 | ❌ CODEC 直接解析 | 使用 `BrSpawnRule.CODEC.parse(JsonOps.INSTANCE, json)` | **CODEC 路径错误** — 尝试在根级找 `identifier` 字段，实际在 `minecraft:spawn_rules.description.identifier` |
| SR-05 | 文件不存在 | 无 `spawn_rules/` 目录 | 不触发，aggregate 中为空 Map |

### 7.4 structures 测试

| 用例编号 | 场景 | 文件特征 | 预期 eyelib 行为 |
|---|---|---|---|
| ST-01 | ❌ .mcstructure 文件 | `structures/test.mcstructure`（二进制 NBT） | `classify` → STRUCTURE，落入 unmanaged（OUTSIDE_IMPORTER_SCOPE） |
| ST-02 | ❌ 多结构 | `structures/village_house.mcstructure` + 其他 | 全部落入 unmanaged |

### 7.5 交叉/集成测试

| 用例编号 | 场景 | 预期 eyelib 行为 |
|---|---|---|
| INT-01 | 完整行为包含所有四种文件 | LOOT_TABLE 和 SPAWN_RULE 解析为 managed typed；TRADING 和 STRUCTURE 落入 unmanaged |
| INT-02 | 检查 aggregate 中各 Map 内容 | `lootTableFiles()` 含 BrLootTable；`spawnRulesFiles()` 含 BrSpawnRule；无 `tradeFiles()` 或 `structureFiles()` 方法 |
| INT-03 | unmanaged 资源中 TRADING/STRUCTURE | `unmanagedResources` 包含对应条目，reason 为 OUTSIDE_IMPORTER_SCOPE |

---

## 8. 行动建议优先级

| 优先级 | 项目 | 工作量 | 影响 |
|---|---|---|---|
| P0 | 将 BrTrading 接线到 processEntry | 小（+3 行 switch case + import） | 消除死代码，使 trading 从 unmanaged 变为 managed |
| P0 | 修复 BrSpawnRule.CODEC 路径 | 小（修改 CODEC fieldOf 路径） | 使 CODEC 与 parse 方法一致，避免未来误用 |
| P1 | BrTrading 补充缺失字段 | 中（添加 trader_exp, max_uses, reward_exp, price_multiplier, gives.functions） | 完整建模 Bedrock 经济交易格式 |
| P1 | BrLootTablePool 补充 tiers 字段 | 小（添加 tiers: Optional<BrTiers>） | 修复实体装备表解析失败 |
| P2 | 将 functions/conditions 从 raw 提升为 typed | 大（需分析所有 function/condition 类型） | 使 loot/spawn 深层结构可验证、可消费 |
| P3 | .mcstructure 解析器 | 极大（需实现 NBT 反序列化） | 使 structure 从 unmanaged 变为 managed |
| P3 | runtime 消费者 | 大（需在 eyelib 根模块添加对应功能） | 使 managed 数据在游戏内生效 |

---

## 9. 总结

- **loot_tables** 和 **spawn_rules** 已被 eyelib importer 正确解析为 managed typed 数据，但深层的 functions/conditions 仍保留为 raw，且缺少部分字段（tiers）。
- **trading** 存在完整的 `BrTrading` codec 但被忽略（死代码），需要接线。
- **structures** 完全没有实现，且因其二进制格式需要全新的解析基础设施。
- 所有四种资源均无根模块 runtime 消费者。
- `eyelib-util` 模块没有通用 JSON codec 工具；相关工具集中在 `eyelib-importer` 的 `ImporterCodecUtil` 中。

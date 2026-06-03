# Gap Analysis: loot_tables + trading + spawn_rules + structures

> 调查日期：2026-06-02（v2 — 代码级验证 + 对照 E 盘文档）
> 范围：eyelib 实现 vs 基岩版行为包规范（Bedrock Wiki + MS Creator 官方参考）

---

## 1. 概览矩阵（代码验证结果）

| 资源族 | 路径模式 | 文件格式 | family 分类 | importer 状态 | aggregate 暴露 | runtime 消费者 |
|---|---|---|---|---|---|---|
| loot_tables | `loot_tables/**/*.json` | JSON | LOOT_TABLE | ✅ managed (BrLootTable) | ✅ `lootTableFiles()` on aggregate | ❌ 无 |
| trading | `trading/**/*.json` | JSON | TRADING | ❌ 死代码 (BrTrading 未接线) | ❌ 无 `tradeFiles()` 方法 | ❌ 无 |
| spawn_rules | `spawn_rules/*.json` | JSON | SPAWN_RULE | ✅ managed (BrSpawnRule.parse) | ✅ `spawnRulesFiles` on aggregate | ❌ 无 |
| structures | `structures/*.mcstructure` | 二进制 NBT | STRUCTURE | ❌ unmanaged (无 codec) | ❌ 无 | ❌ 无 |

> **验证方式**：逐行审查 `BedrockAddonLoader.processEntry`（第 178-225 行）和 `BedrockAddonSideAggregate`（第 21-138 行）源代码确认。

---

## 2. loot_tables — 字段级差距

### 2.1 现有实现（已代码确认）

- **文件**: `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/addon/BrLootTable.java`（146 行）
- **包**: `io.github.tt432.eyelibimporter.addon`
- **顶层结构**: `BrLootTable` → `pools: List<BrLootTablePool>`
- **Pool 结构**: `rolls`（Either<Integer, Range>）+ `entries` + `conditions`（raw）+ `functions`（raw）
- **Entry 结构**: `type` + `name` + `weight` + `quality` + `conditions`（raw）+ `functions`（raw）
- **接线方式**: `processEntry` 第 217 行 → `acc.parseAndStore(entry, BrLootTable.CODEC, acc.lootTableFiles)`
- **aggregate 暴露**: `BedrockAddonAggregate.lootTableFiles()` → `behaviorPack.lootTableFilesView()`

### 2.2 文档对照（Bedrock Wiki + MS Creator 官方）

E 盘文档源：
- `/mnt/e/_____基岩版文档/bedrock-wiki/docs/loot/loot-tables.md` — 详细描述了 loot tables 结构
- `/mnt/e/_____基岩版文档/minecraft-creator/creator/Reference/Content/LootTableReference/Examples/LootTableComponents/loot_table.md` — MS 官方参考

**官方确认的 Pool 字段：**
| 字段 | 类型 | 描述 |
|---|---|---|
| `rolls` | 整数 或 {min,max} | ⚠️ 文档默认值为 1（目前 CODEC 未设置默认值，字段为必填） |
| `bonus_rolls` | 整数 | 基于幸运属性的额外 roll 次数 |
| `conditions` | 条件数组 | 触发条件 |
| `entries` | 条目数组 | 可选条目列表 |
| `tiers` | 对象 | 层级条目选择配置（`initial_range`, `bonus_rolls`, `bonus_chance`） |

**官方确认的 entry type 枚举：**
- `item` — 物品条目 ✅
- `loot_table` — 引用另一个 loot table ✅
- `empty` — 空条目 ✅
- `seeded` — 基于种子的条目 ❌ 当前 type 字段是 String 无校验

### 2.3 已建模 vs 未建模字段

| Bedrock 字段 | 层级 | eyelib 状态 | 备注 |
|---|---|---|---|
| `pools` | root | ✅ typed `List<BrLootTablePool>` | |
| `pools[*].rolls` | pool | ✅ Either<Integer, Range> | 整数或 {min, max} |
| `pools[*].bonus_rolls` | pool | ❌ 缺少 | 基于 luck 属性的额外 rolls |
| `pools[*].tiers` | pool | ❌ **缺少** | 含 `initial_range(int)`, `bonus_rolls(int)`, `bonus_chance(float)`；用于实体装备表 |
| `pools[*].conditions` | pool | ⚠️ raw ObjectValue 列表 | 无 typed schema |
| `pools[*].functions` | pool | ⚠️ raw ObjectValue 列表 | 无 typed schema |
| `pools[*].entries` | pool | ✅ typed `List<BrLootTablePoolEntry>` | |
| `entries[*].type` | entry | ⚠️ String 无枚举校验 | 应校验为 item/loot_table/empty/seeded |
| `entries[*].name` | entry | ✅ typed `String` | |
| `entries[*].weight` | entry | ✅ typed `int`（默认 1） | |
| `entries[*].quality` | entry | ✅ typed `int`（默认 0） | |
| `entries[*].conditions` | entry | ⚠️ raw ObjectValue 列表 | 无 typed schema |
| `entries[*].functions` | entry | ⚠️ raw ObjectValue 列表 | 无 typed schema |

### 2.4 文档确认的关键缺失

1. **`pools[*].tiers` 缺失**: 
   - Bedrock Wiki 文档明确描述了 tiered pool（第 139-198 行）
   - MS 官方 loot_table 参考文档也列出了 `tiers` 字段
   - 官方 zombie_equipment.json 第二个 pool 包含 tiers
   - 当前 BrLootTablePool 只有 4 个字段：rolls, entries, conditions, functions
   - **影响**：会直接导致 CODEC 解析失败（多余字段被忽略，但这个缺失导致无法使用）

2. **`bonus_rolls` 在 pool 层面缺失**
   - Wiki 文档描述：bonus_rolls 基于玩家 luck 属性增加 roll 次数
   - 在 weighted random pool 中可用

3. **entry type 枚举无校验**
   - 当前 `type` 是 String，任何值都能通过
   - 官方只支持 4 种：item, loot_table, empty, seeded

---

## 3. trading — 字段级差距

### 3.1 现有实现（已代码确认）

- **文件**: `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/trading/BrTrading.java`（139 行）
- **包**: `io.github.tt432.eyelibimporter.trading`（注意：**非** addon 包，与 BrLootTable 等不在同一包）
- **顶层结构**: `BrTrading` → `tiers: List<BrTier>`
- **接线**: ❌ **未接线** — `processEntry` switch 中**没有** `case TRADING`（第 178-225 行）
- **目前状态**: 落入 `default → captureUnmanaged` 分支 → `OUTSIDE_IMPORTER_SCOPE`
- **aggregate**: 既无 `tradeFiles` 字段在 `PackAccumulator`，也无 `tradeFiles` 在 `BedrockAddonSideAggregate` 或 `BedrockAddonPack`

### 3.2 文档对照（Bedrock Wiki）

E 盘文档源：
- `/mnt/e/_____基岩版文档/bedrock-wiki/docs/loot/trade-tables.md` — 完整 702 行的交易表文档
- `/mnt/e/_____基岩版文档/bedrock-wiki/docs/loot/trading-behavior.md` — 实体对接文档

**Wiki 文档确认的完整 trade 结构：**
```json
{
    "tiers": [
        {
            "groups": [
                {
                    "num_to_select": 1,
                    "trades": [
                        {
                            "wants": [
                                {
                                    "item": "minecraft:book",
                                    "quantity": { "min": 2, "max": 4 },
                                    "price_multiplier": 0.5
                                }
                            ],
                            "gives": [
                                {
                                    "item": "minecraft:enchanted_book",
                                    "functions": [ { "function": "enchant_book_for_trading", ... } ]
                                }
                            ],
                            "max_uses": 7,
                            "trader_exp": 3,
                            "reward_exp": false
                        }
                    ]
                }
            ]
        },
        {
            "total_exp_required": 28,
            "trades": [ ... ]
        }
    ]
}
```

### 3.3 已建模 vs 未建模字段

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
| `trades[*].trader_exp` | trade | ❌ **缺少** | 交易获得经验值（Wiki 文档第 354-366 行确认） |
| `trades[*].max_uses` | trade | ❌ **缺少** | 最大使用次数（Wiki 文档第 324-338 行确认） |
| `trades[*].reward_exp` | trade | ❌ **缺少** | 是否奖励经验（Wiki 文档第 341-350 行确认） |
| `wants[*].item` | entry item | ✅ typed `String` | |
| `wants[*].quantity` | entry item | ✅ typed `BrQuantity` | 支持整数或 {min, max} |
| `wants[*].price_multiplier` | entry item | ❌ **缺少** | 经济交易价格乘数（Wiki 文档第 479-500 行确认） |
| `gives[*].item` | entry item | ✅ typed `String` | |
| `gives[*].quantity` | entry item | ✅ typed `BrQuantity` | |
| `gives[*].functions` | entry item | ❌ **缺少** | 用于给出售物品附加 enchant_book_for_trading 等（Wiki 文档完整例子确认） |
| `wants[*].choice` | entry | ✅ typed `List<BrTradeItem>` | Wiki 文档第 368-397 行确认 |
| `choice[*].item` | choice item | ✅ typed `String` | |
| `choice[*].quantity` | choice item | ✅ typed `BrQuantity` | |
| `choice[*].price_multiplier` | choice item | ❌ **缺少** | choice 内部也需要 price_multiplier，当前缺失 |

### 3.4 关键差距

1. **🔴 P0: 整个 BrTrading 为死代码**
   - 类存在、CODEC 完整（139 行），但 `processEntry` 中无 `case TRADING`
   - `BedrockResourceFamily.classify` 正确 → `TRADING`
   - `unmanagedReasonFor(TRADING)` → `OUTSIDE_IMPORTER_SCOPE`
   - PackAccumulator **没有** `tradeFiles` 字段
   - BedrockAddonPack **没有** `tradeFiles` 字段
   - BedrockAddonSideAggregate **没有** `tradeFiles` 字段
   - **修复需要**：
     - PackAccumulator 加 `tradeFiles` 字段
     - processEntry 加 `case TRADING`
     - BedrockAddonPack 加 `tradeFiles` 参数
     - BedrockAddonSideAggregate 加 `tradeFiles` 字段和视图方法
     - BedrockAddonAggregate 加 `tradeFiles()` 访问方法

2. **🔴 P1: trading/ 子目录仍需确认**
   - `BedrockResourceFamily.classify` 中：`path.startsWith("trading/")` → TRADING
   - `trading/economy_trades/*.json` 匹配正确
   - 但 `trading/**/*.json` 的 glob 需要确认 `listFiles` 递归扫描逻辑

3. **🟡 P1: BrTrade 缺少 3 个字段**
   - `trader_exp`: int，默认 1
   - `max_uses`: int，默认 7（负值表示无限使用）
   - `reward_exp`: boolean，默认 true

4. **🟡 P1: BrTradeItem 缺少 price_multiplier**
   - `price_multiplier`: float，默认 0
   - 影响 wants 和 choice 内部的 items

5. **🟡 P2: gives items 缺少 functions**
   - gives 中的 items 可以有 functions 数组（如 `enchant_book_for_trading`）
   - 目前 BrTradeItem 只有 `item` + `quantity`

---

## 4. spawn_rules — 字段级差距

### 4.1 现有实现（已代码确认）

- **文件**: `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/addon/BrSpawnRule.java`（38 行）
- **包**: `io.github.tt432.eyelibimporter.addon`
- **顶层结构**: `BrSpawnRule` → `formatVersion` + `identifier` + `populationControl` + `conditions`
- **接线**: `processEntry` 第 204-205 行 → `acc.spawnRulesFiles.put(entry.effectivePath(), BrSpawnRule.parse(readJsonFile(entry.file())))`
- **注意**: 使用 `parse()` 方法，**不**使用 `CODEC`（因为 CODEC 路径有误）
- **aggregate**: `BedrockAddonSideAggregate` 第 38 行有 `spawnRulesFiles` 字段
- **aggregate 暴露**: 在 `fromSidePacks` 中通过 `spawnRulesFiles.putAll(pack.spawnRulesFiles())` 合并

### 4.2 CODEC 路径问题（代码确认）

```java
// BrSpawnRule.java 第 19-24 行 — CODEC 路径错误
public static final Codec<BrSpawnRule> CODEC = RecordCodecBuilder.create(ins -> ins.group(
    Codec.STRING.fieldOf("format_version")...       // ✓ 在根级
    Codec.STRING.fieldOf("identifier")...            // ✗ 在根级，实际在 minecraft:spawn_rules.description.identifier
    Codec.STRING.fieldOf("population_control")...    // ✗ 在根级，实际在 minecraft:spawn_rules.description.population_control
    ImporterCodecUtil.OBJECT_VALUE_CODEC.listOf()
        .fieldOf("conditions")...                    // ✗ 在根级，实际在 minecraft:spawn_rules.conditions
).apply(ins, BrSpawnRule::new));
```

```java
// BrSpawnRule.java 第 26-37 行 — parse() 方法路径正确
public static BrSpawnRule parse(JsonObject root) {
    String formatVersion = root.get("format_version").getAsString();          // ✓
    JsonObject spawnRules = root.getAsJsonObject("minecraft:spawn_rules");    // ✓
    JsonObject description = spawnRules.getAsJsonObject("description");       // ✓
    String identifier = description.get("identifier").getAsString();          // ✓
    ...
}
```

### 4.3 文档对照（Bedrock Wiki）

E 盘文档源：
- `/mnt/e/_____基岩版文档/bedrock-wiki/docs/entities/spawn-rules.md` — 157 行文档

**文档确认的完整 spawn rule 结构：**
```json
{
    "format_version": "1.8.0",
    "minecraft:spawn_rules": {
        "description": {
            "identifier": "minecraft:zombie",
            "population_control": "monster"
        },
        "conditions": [
            {
                "minecraft:spawns_on_surface": {},
                "minecraft:spawns_underground": {},
                "minecraft:brightness_filter": { "min": 0, "max": 7, "adjust_for_weather": true },
                "minecraft:difficulty_filter": { "min": "easy", "max": "hard" },
                "minecraft:weight": { "default": 100 },
                "minecraft:herd": { "min_size": 2, "max_size": 4 },
                "minecraft:permute_type": [ { "weight": 95 }, { "weight": 5, "entity_type": "minecraft:zombie_villager" } ],
                "minecraft:biome_filter": { "test": "has_biome_tag", "operator": "==", "value": "monster" }
            }
        ]
    }
}
```

### 4.4 已建模 vs 未建模字段

| Bedrock 字段 | 层级 | eyelib 状态 | 备注 |
|---|---|---|---|
| `format_version` | root | ✅ typed `String` | |
| `minecraft:spawn_rules` | root wrapper | ⚠️ parse 方法手动提取 | CODEC 无此包装层 |
| `minecraft:spawn_rules.description.identifier` | description | ✅ typed `String` | CODEC 路径错误但 parse 正确 |
| `minecraft:spawn_rules.description.population_control` | description | ✅ typed `String` | CODEC 路径错误但 parse 正确 |
| `minecraft:spawn_rules.conditions` | conditions | ⚠️ raw ObjectValue 列表 | 全部 components 未 typed |
| `conditions[*].minecraft:spawns_on_surface` | condition | ❌ 无 typed | 空对象 |
| `conditions[*].minecraft:spawns_underground` | condition | ❌ 无 typed | 空对象 |
| `conditions[*].minecraft:spawns_underwater` | condition | ❌ 无 typed | 空对象 |
| `conditions[*].minecraft:brightness_filter` | condition | ❌ 无 typed | {min, max, adjust_for_weather} |
| `conditions[*].minecraft:difficulty_filter` | condition | ❌ 无 typed | {min, max} 字符串难度 |
| `conditions[*].minecraft:weight` | condition | ❌ 无 typed | {default: int} |
| `conditions[*].minecraft:herd` | condition | ❌ 无 typed | {min_size, max_size, event?, event_skip_count?} |
| `conditions[*].minecraft:permute_type` | condition | ❌ 无 typed | [{weight, entity_type?}] 数组 |
| `conditions[*].minecraft:biome_filter` | condition | ❌ 无 typed | {test, operator, value} |
| `conditions[*].minecraft:density_limit` | condition | ❌ 无 typed | {surface, underground} |
| `conditions[*].minecraft:height_filter` | condition | ❌ 无 typed | {min, max} |
| `conditions[*].minecraft:spawns_on_block_filter` | condition | ❌ 无 typed | {blocks, distance} |
| `conditions[*].minecraft:spawns_above_block_filter` | condition | ❌ 无 typed | {blocks, distance} |
| `conditions[*].minecraft:spawns_on_block_prevented_filter` | condition | ❌ 无 typed | 方块标识符数组 |
| `conditions[*].minecraft:disallow_spawns_in_bubble` | condition | ❌ 无 typed | 空对象 |
| `conditions[*].minecraft:spawns_lava` | condition | ❌ 无 typed | 空对象 |
| `conditions[*].minecraft:distance_filter` | condition | ❌ 无 typed | {min, max} |
| `conditions[*].minecraft:is_experimental` | condition | ❌ 无 typed | 空对象 |
| `conditions[*].minecraft:world_age_filter` | condition | ❌ 无 typed | {min, max} |
| `conditions[*].minecraft:delay_filter` | condition | ❌ 无 typed | {min, max} |
| `conditions[*].minecraft:mob_event_filter` | condition | ❌ 无 typed | {event} |
| `conditions[*].minecraft:is_persistent` | condition | ❌ 无 typed | 空对象 |
| `conditions[*].minecraft:player_in_village_filter` | condition | ❌ 无 typed | {distance}? |

### 4.5 关键差距

1. **🟡 P0: CODEC 顶层路径错误**
   - CODEC 假设 identifier/population_control/conditions 在根级
   - 实际在 `minecraft:spawn_rules.description.*` 和 `minecraft:spawn_rules.conditions`
   - 当前使用 parse() 方法绕过，但 CODEC 仍会误导未来使用者

2. **🟠 P2: 14+ condition components 未 typed**
   - 每个 condition 包含 1-N 个 `minecraft:*` 组件
   - 全部保留为 raw ObjectValue
   - 各组件有不同的模式（空对象、单值、{min,max}、filter 表达式、数组）

3. **🔴 P0（已经通过 parse() 绕过）**: 当前不会失败因为使用 `parse()` 而非 `parseAndStore()`

---

## 5. structures — 差距

### 5.1 现有状态（代码确认）

- **codec**: ❌ 无 — 没有任何 Java 文件
- **classify**: ✅ 正确 → `path.startsWith("structures/")` → `STRUCTURE`
- **unmanagedReasonFor**: `OUTSIDE_IMPORTER_SCOPE`
- **接线**: `default → captureUnmanaged`
- **aggregate**: ❌ 无 `structureFiles` 字段
- **PackAccumulator**: ❌ 无 `structureFiles` 字段

### 5.2 文档对照

E 盘文档源：
- `/mnt/e/_____基岩版文档/bedrock-wiki/docs/nbt/mcstructure.md` — 156 行完整格式描述
- `/mnt/e/_____基岩版文档/bedrock-wiki/docs/world-generation/structure-features.md` — 394 行特征教程
- `/mnt/e/_____基岩版文档/bedrock-wiki/docs/world-generation/jigsaw-structures.md` — 751 行拼图结构文档
- `/mnt/e/_____基岩版文档/minecraft-creator/creator/Reference/Content/FeaturesReference/Examples/Features/minecraft_structure_template_feature.md` — MS 官方参考

**`.mcstructure` 格式（NBT，Little-Endian，无压缩）：**
```
format_version: int          # 当前始终 1
size: [int, int, int]        # [width, height, depth]
structure: compound {
    block_indices: [
        [int...],             # 主层 block 索引（ZYX 顺序）
        [int...]              # 第二层（waterlogged 等）
    ]
    entities: [compound...]   # 实体 NBT
    palette: compound {
        default: compound {
            block_palette: [
                { name: string, states: compound, version: int }
            ]
            block_position_data: compound {
                <index>: compound {
                    block_entity_data: compound
                    tick_queue_data: [compound...]
                }
            }
        }
    }
}
structure_world_origin: [int, int, int]
```

**结构 ID 规则：**
- `structures/house.mcstructure` → `mystructure:house`（根目录用默认命名空间）
- `structures/village/house.mcstructure` → `village:house`（子目录命名空间）
- `structures/namespace/house.mcstructure` → `namespace:house`

**相关 JSON 资源族（`features/` 和 `feature_rules/`）：**
- `features/*.json`: `minecraft:structure_template_feature` 引用 .mcstructure 文件
- `feature_rules/*.json`: 特征放置规则
- 这些是 JSON 文件，可以被现有 JSON codec 基础设施解析
- 当前 FEATURE 和 FEATURE_RULE 也是 unmanaged

### 5.3 关键差距

1. **🔴 P3: 零实现**
   - 无 codec、parser、数据结构
   - 需要 NBT 反序列化（Little-Endian，无压缩）
   - 不能复用现有 JSON codec 基础设施

2. **🟠 P2: 相关的 JSON 资源（features/feature_rules）也是 unmanaged**
   - features/ 中的 structure_template_feature 是 JSON，可以用 JSON codec
   - 但这些文件也是直接落入 unmanaged
   - 不过 features/ 不在本 gap 分析的范围内，属于另一个主题

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

## 7. 集成测试案例（代码级验证）

### 7.1 loot_tables 测试

当前：**0 个测试**（`src/test` 目录无 BrLootTable 或 loot 相关测试文件）

| 用例编号 | 场景 | Bedrock JSON 关键特征 | 预期 eyelib 行为 |
|---|---|---|---|
| LT-01 | 简单掉落表：整数 rolls | `{"pools":[{"rolls":1,"entries":[{"type":"item","name":"minecraft:stick","weight":1}]}]}` | BrLootTable 成功解析，pools[0].rolls=Left(1) |
| LT-02 | 范围 rolls | `{"pools":[{"rolls":{"min":1,"max":3},"entries":[...]}]}` | BrLootTable 成功解析，pools[0].rolls=Right({min:1, max:3}) |
| LT-03 | 多条件+函数 | 带 `conditions` 和 `functions` 的 pool（如 zombie_equipment.json） | 解析成功但 conditions/functions 保留为 raw ObjectValue |
| **LT-04** | **🔴 tiers 字段** | 包含 `tiers: {initial_range:2, bonus_rolls:3, bonus_chance:0.095}` 的 pool | **当前 CODEC 解析成功但 tiers 被静默忽略**（因为没有 `tiers` 字段，额外字段无 error） |
| LT-05 | 子目录嵌套 | `loot_tables/entities/zombie.json`, `loot_tables/gameplay/fishing/junk.json` | classify 正确 → LOOT_TABLE |
| LT-06 | 文件不存在 | 无 `loot_tables/` 目录 | 不触发，aggregate 中为空 Map |
| LT-07 | 非法 JSON | 语法错误 | `SCHEMA_PARSE_FAILED` 警告，落入 unmanaged |

### 7.2 trading 测试

当前：**0 个测试**（无 BrTrading 测试文件）

| 用例编号 | 场景 | Bedrock JSON 关键特征 | 预期 eyelib 行为 |
|---|---|---|---|
| **TR-01** | **🔴 简单格式（无 groups）** | `{"tiers":[{"trades":[{"wants":[{"item":"a"}],"gives":[{"item":"b"}]}]}]}` | **当前落入 unmanaged**（BrTrading 未接线） |
| **TR-02** | **🔴 完整格式（含 groups）** | `{"tiers":[{"total_exp_required":0,"groups":[{"num_to_select":1,"trades":[...]}]}]}` | **当前落入 unmanaged** |
| **TR-03** | **🔴 经济交易格式（带 price_multiplier/trader_exp/etc）** | 含 `price_multiplier`, `trader_exp`, `max_uses`, `reward_exp` | **当前落入 unmanaged**；即使接线，这些字段不存在于 BrTrade/BrTradeItem 中 |
| TR-04 | `economy_trades/` 子目录 | `trading/economy_trades/librarian_trades.json` | classify 正确 → TRADING，但落入 unmanaged |
| **TR-05** | **🔴 带 functions 的 gives** | `"gives":[{"item":"minecraft:enchanted_book","functions":[...]}]` | **当前落入 unmanaged**；即使接线，functions 字段未建模会静默忽略 |
| TR-06 | CODEC 单元测试 | 直接测 BrTrading.CODEC 解析 | 验证 tiers/groups/trades/wants/gives/choice 的 Either 编解码正确性 |

### 7.3 spawn_rules 测试

当前：**0 个测试**（无 BrSpawnRule 测试文件）

| 用例编号 | 场景 | Bedrock JSON 关键特征 | 预期 eyelib 行为 |
|---|---|---|---|
| SR-01 | 标准生成规则 | 带 `minecraft:spawn_rules` 嵌套的完整 spawn rule（如 zombie.json） | BrSpawnRule.parse 成功，conditions 保留为 raw ObjectValue |
| SR-02 | 多 condition 数组 | 多个 condition 对象（permute_type + weight + biome_filter） | 解析成功，conditions 列表中多个 raw ObjectValue |
| SR-03 | condition 内各种 minecraft:* 组件 | brightness_filter, weight, herd, biome_filter 等 | 保留为 raw ObjectValue，无 typed 提取 |
| **SR-04** | **🟡 CODEC 直接解析** | 使用 `BrSpawnRule.CODEC.parse(JsonOps.INSTANCE, json)` | **CODEC 路径错误** — 尝试在根级找 `identifier` 字段，实际在 `minecraft:spawn_rules.description.identifier` → **解析失败** |
| SR-05 | 文件不存在 | 无 `spawn_rules/` 目录 | 不触发，aggregate 中为空 Map |

### 7.4 structures 测试

| 用例编号 | 场景 | 文件特征 | 预期 eyelib 行为 |
|---|---|---|---|
| ST-01 | **🔴 .mcstructure 文件** | `structures/test.mcstructure`（二进制 NBT） | `classify` → STRUCTURE，落入 unmanaged（OUTSIDE_IMPORTER_SCOPE） |
| ST-02 | **🔴 多结构+子目录** | `structures/village_house.mcstructure`, `structures/village/house.mcstructure` | 全部落入 unmanaged |
| ST-03 | 结构命名空间解析 | 验证 `structures/a.mcstructure` → `mystructure:a`，`structures/ns/a.mcstructure` → `ns:a` | 仅理论，无实现 |

### 7.5 交叉/集成测试

| 用例编号 | 场景 | 预期 eyelib 行为 |
|---|---|---|
| **INT-01** | **🔴完整 BP 含所有四种文件** | LOOT_TABLE 和 SPAWN_RULE 解析为 managed typed；TRADING 和 STRUCTURE 落入 unmanaged |
| **INT-02** | **🔴aggregate Map 内容验证** | `lootTableFiles()` 含 BrLootTable；`spawnRulesFiles()` 含 BrSpawnRule；无 `tradeFiles()` 或 `structureFiles()` 方法 |
| INT-03 | unmanaged 资源中 TRADING/STRUCTURE | `unmanagedResources` 包含对应条目，reason 为 OUTSIDE_IMPORTER_SCOPE |
| **INT-04** | **🟡 processEntry switch case 完整性** | 验证 processEntry 的 switch 覆盖了多少 enum 值 | 当前 20 个 enum 值中只处理了 14 个，TRADING/STRUCTURE/FEATURE/FEATURE_RULE 等 6 个落入 default |

---

## 8. 行动建议优先级

| 优先级 | 项目 | 工作量 | 影响 | 依赖 |
|---|---|---|---|---|
| **🔴 P0** | **将 BrTrading 接线到 processEntry** | 小（PackAccumulator 加字段 + BedrockAddonPack 加参数 + processEntry 加 case + aggregate 暴露） | 消除死代码，使 trading 从 unmanaged 变为 managed | 无 |
| **🟡 P0** | **修复 BrSpawnRule.CODEC 路径** | 极小（修改 CODEC 的 fieldOf 路径指向 `minecraft:spawn_rules.description.*`） | 使 CODEC 与 parse 方法一致，避免未来误用 | 无 |
| **🔴 P1** | **BrTrade 补充缺失字段** | 中（添加 trader_exp, max_uses, reward_exp → BrTrade 构造 + CODEC） | 完整建模 Bedrock 经济交易格式 | P0 接线完成后 |
| **🔴 P1** | **BrTradeItem 补充 price_multiplier** | 小（添加 float priceMultiplier, 默认 0） | 经济交易价格乘数支持 | P0 接线完成后 |
| **🟡 P1** | **BrLootTablePool 补充 tiers + bonus_rolls** | 小（添加 tiers: Optional<BrTiers> + bonusRolls: Optional<Integer>） | 修复实体装备表解析时 tiers 被静默忽略的问题 | 无 |
| **🟡 P1** | **BrLootTablePoolEntry type 枚举校验** | 小（改为 enum 或 sealed interface） | 数据质量提升 | 无 |
| **🟠 P2** | **BrTradeItem gives 补充 functions** | 中（添加 functions: List<ObjectValue>） | 附魔书等带函数交易支持 | P0 接线完成后 |
| **🟠 P2** | **将 functions/conditions 从 raw 提升为 typed** | 大（需分析所有 function 类型和 condition 类型，创建 typed schema） | 使 loot/spawn 深层结构可验证、可消费 | 无 |
| **🟠 P2** | **spawn_rules condition components typed** | 大（14+ 个 minecraft:* 组件各有不同 schema） | 生成规则深层结构可用 | 无 |
| **🔴 P3** | **.mcstructure 解析器** | 极大（需实现 NBT Little-Endian 反序列化 + 数据结构） | 使 structure 从 unmanaged 变为 managed | 需新建模块或依赖 |
| **🔴 P3** | **features/feature_rules JSON 接线** | 中（先只做 JSON 解析和 aggregate 存储，不做 runtime） | 使与 structure 关联的特征进入 managed | 无 |
| **🔴 P3** | **runtime 消费者** | 大（需在 eyelib 根模块添加对应功能） | 使 managed 数据在游戏内生效 | 所有 P0-P2 完成后 |

---

## 9. 结论

对照 E 盘文档进行代码级验证后，总结如下：

### 已正确实现（✅）
- **loot_tables**: BrLootTable 已通过 CODEC 接线、解析、聚合到 aggregate。支持 rolls（整数/范围）、entries（type/name/weight/quality）、raw conditions/functions
- **spawn_rules**: BrSpawnRule 已通过 parse() 接线、解析、聚合。支持 format_version、identifier、population_control、raw conditions

### 已建模但未接线（死代码，🔴 P0）
- **trading**: BrTrading（139 行完整 CODEC）存在于 `eyelibimporter.trading` 包中，完全未接线。需要添加 aggregate 字段、processEntry case、PackAccumulator 字段、BedrockAddonPack 参数

### 缺失字段（🟡 P1）
- **loot_tables**: `tiers`（tiered pool）、`bonus_rolls`（pool 层面）
- **trading**: `trader_exp`、`max_uses`、`reward_exp`（BrTrade）、`price_multiplier`（BrTradeItem）、`functions`（gives items）

### 零实现（🔴 P3）
- **structures**: 二进制 .mcstructure（NBT Little-Endian），需要全新解析器
- **features/feature_rules**: JSON 但也是 unmanaged

### 深层未 typed（🟠 P2）
- loot_tables 的 functions/conditions（保留为 raw ObjectValue）
- spawn_rules 的 14+ condition components（全部 raw）
- trading 的 gives functions（raw，如果接线了）

### 代码设计问题
- **BrSpawnRule.CODEC 路径错误**: CODEC 假设扁平结构，实际 JSON 有嵌套
- **BrLootTablePoolEntry.type 无枚举校验**: String 类型不限制为 item/loot_table/empty/seeded
- **通用 JSON codec 工具在 eyelib-importer**: `ImporterCodecUtil` 不对外暴露，eyelib-util 无等效工具
- **aggregate 字段限制**: `BedrockAddonSideAggregate` 已有 18 个字段（突破 RecordCodecBuilder 16 字段限制的方式是不序列化 spawnRulesFiles 和 lootTableFiles）

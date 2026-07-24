# Actions-and-Stuff-1.10-v2.mcpack 实体组件解析错误盘点

> 来源：反馈 fb_mryncnhexovx。调查日期 2026-07-25。
> 日志：`versions/1.20.1/run/benchmark/logs/latest.log`（2026-07-25 00:34:21 启动）与 `versions/1.20.1/run/logs/latest.log`（00:37:45），两组内容一致，行号以 benchmark 日志为准。

## 结论

**无 P0（渲染正确性）问题**：所有解析失败均为 gameplay/AI 行为组件，渲染引擎（粒子、实体模型、材质）不受影响。slime 场景渲染成功的结论成立，但实体行为覆盖率受限。

## 一、组件组解析失败（codec 不兼容，整组丢弃）

入口：`BehaviorPackPublication.parseComponentGroup()`（`src/main/java/io/github/tt432/eyelib/common/behavior/BehaviorPackPublication.java:127`），经 `ComponentGroup.DISPATCH_CODEC`（`ComponentGroup.java:29-203`）解码失败。

| 组件 | 次数 | 日志行 | 根因 | 代码位置 |
|---|---|---|---|---|
| minecraft:spawn_entity | 2 | latest.log:87, 1749 | `entities` 要求 listOf，mcpack 用单对象简写 | `behavior/component/property/SpawnEntity.java:56` |
| minecraft:environment_sensor | 4 | latest.log:1466-1467, 1489-1492, 2853-2858, 2902-2907 | `triggers` 同上，单对象简写不兼容 | `behavior/component/property/EnvironmentSensor.java:35` |
| minecraft:equippable | 2 | latest.log:1326-1328, 2186-2189 | `interact_text` 必填，鞍具槽可省略 | `behavior/component/property/Equippable.java:32` |
| minecraft:spell_effects | 2 | latest.log:895-897, 926-928 | codec 要求 `add_spell_effects`，Bedrock 原生 key 是 `add_effects` | `behavior/component/property/SpellEffects.java:20` |
| minecraft:shooter | 2 | latest.log:1151-1154, 1256-1258 | `pots` 必填，mcpack 省略（默认空表） | `behavior/component/property/Shooter.java:28` |
| minecraft:entity_sensor | ≥1 | latest.log:120 | 顶级 `event` 必填，含 `subsensors` 时可省略 | `behavior/component/property/EntitySensor.java:28` |
| minecraft:equipment | ≥1 | debug.log:1276 | `slot_drop_chance` 必填，仅给 `table` 时省略 | `behavior/component/property/Equipment.java:17` |
| minecraft:attack | 1 | latest.log:2900-2904 | `damage` 仅 either(int,string)，mcpack 用 `[3,9]` 范围数组 | `behavior/component/property/Attack.java:15` |
| minecraft:block_sensor | ≥1 | latest.log:952 | 同类字段约束过严 | — |

## 二、事件过滤器解析失败

`BaseFilter.CODEC`（`src/main/java/io/github/tt432/eyelib/behavior/event/filter/base/BaseFilter.java:29-49`）仅识别 `actor_health`，其他 test 值抛 `IllegalStateException`，事件整体丢弃（仅影响 AI，不影响渲染）。

| filter test | 次数 | 受影响事件 |
|---|---|---|
| has_component | 6+ | minecraft:entity_born, start_exploding(_forced) 等 |
| enum_property | 7+ | start_unrolling, unroll, ageable_grow_up, start_peeking, roll_up, stop_peeking, threat_detected |
| on_fire | 1 | no_threat_detected |
| is_difficulty | 2 | entity_spawned, attacked |
| is_family | 1 | hive_destroyed |
| is_variant | 1 | add_can_ride |
| bool_property | 1 | find_hive_timeout |

## 三、未注册组件（静默降级 EmptyComponent）

- `minecraft:conditional_bandwidth_optimization`（大量）、`minecraft:damage_sensor`（大量）、`minecraft:behavior.*` 系列十几种（look_at_entity / pickup_items / panic / float / tempt / breed / melee_box_attack / random_stroll / look_at_player / nearest_attackable_target 等）、`minecraft:breedable`、`minecraft:angry`、`minecraft:attack_cooldown`、`minecraft:attack_damage`、`minecraft:dash`、`minecraft:buoyant`、`minecraft:grows_crop`、`minecraft:transformation`、`minecraft:custom_hit_test`、`minecraft:game_event_movement_tracking`、`minecraft:equip_item`、`minecraft:behavior.equip_item`。

## 四、优先级

- **P1（小修复，覆盖面最广）**：spawn_entity / environment_sensor 单对象简写分支；equippable.interact_text 与 shooter.pots 改 optional。
- **P2**：spell_effects 兼容 `add_effects` key；equipment.slot_drop_chance 改 optional；entity_sensor 顶级 event 与 subsensors 互斥处理；attack.damage 支持范围数组。
- **P3（大型功能开发）**：BaseFilter 补全 filter test 体系；behavior.* AI 组件注册。

## 五、P1+P2 修复记录（2026-07-25，CodecCompatFix）

文档 oracle：`E:\_____基岩版文档\minecraft-creator\creator\Reference\Content\EntityReference\Examples\EntityComponents\`。

| 项 | 修复 | 文档依据 |
|---|---|---|
| spawn_entity.entities | 单对象/数组兼容（`ChinExtraCodecs.singleOrList`） | minecraftComponent_spawn_entity.md："Can be a single object or an array of objects"（Sniffer 官方示例为单对象） |
| environment_sensor.triggers | 同上 | minecraftComponent_environment_sensor.md："Can be an array of trigger objects or a single trigger object"（Cave Spider/Player 官方示例为单对象） |
| equippable.interact_text | 改 optional（默认 ""） | minecraftComponent_equippable.md：interact_text Default `*not set*` |
| shooter.pots | 改 optional（默认空表） | minecraftComponent_shooter.md：现行 schema 无 pots 字段（旧版遗留），Blaze/Llama 示例均省略 |
| spell_effects | key 改为 `add_effects`/`remove_effects`（干净切换，旧 key 非 Bedrock 合法）；`remove_effects` 按文档为单个 String，用 singleOrList 兼容；两者均可省（`{}` 合法） | minecraftComponent_spell_effects.md：属性表 + Player `minecraft:clear_raid_omen_spell_effect` 空组件示例 |
| equipment.slot_drop_chance | 改 optional（默认空表） | minecraftComponent_equipment.md：slot_drop_chance Default `*not set*` |
| entity_sensor 顶级 event | 改 optional（默认 ""） | minecraftComponent_entity_sensor.md：顶级属性仅 find_players_only / relative_range / subsensors，**无顶级 event**；Parrot 官方示例仅 subsensors |
| attack.damage | 支持 `[min,max]` 范围数组（序列化为 `"[min, max]"` 字符串） | minecraftComponent_attack.md："Can be a number, an array [min, max], or an object with range_min and range_max properties" |

### 核对后超出原报告描述的附带修复（均有 mcpack 实际输入佐证，不修复则 P1/P2 不生效）

1. **SpawnEntry.spawn_entity 改 optional（默认 ""）**：文档原文 "leave empty to spawn the item defined by spawn_item instead"；mcpack armadillo 形态（latest.log:86）无 `spawn_entity` key，仅修单对象简写仍会失败。
2. **SpellEntry.amplifier 改 optional（默认 0）**：文档属性表仅列 effect/duration/display_on_screen_animation；mcpack 实际输入（latest.log:895）无 amplifier。
3. **EntitySensor.SubSensor.range 兼容 `[horizontal, vertical]` 数组**：文档类型为 "a, b coordinate array"（默认 [10,10]）；mcpack 输入为 `"range":[7.0,2.0]`，record 仅存 float，取水平距离（encode 写回标量）。
4. **JSON_OBJECT_CODEC 修复（SpawnEntity/EnvironmentSensor/EntitySensor）**：原实现 `Codec.STRING.xmap(JsonParser::parseString)` 只接受“字符串内嵌 JSON”，对真实 JSON 对象报 "Not a string"；mcpack 的 `filters`/`event_filters` 均为内联对象。改为 `Codec.PASSTHROUGH` 转换（同 ImporterCodecUtil.JSON_ELEMENT_CODEC 模式）。BlockSensor/HurtOnCondition/Scheduler 存在同款问题，未在本次范围内。

### 记录在案的已知限制（不放松约束，留待后续）

- **equipment.slot_drop_chance 字符串数组简写**：文档允许 `"slot_drop_chance":["mainhand"]`（slot 名字符串数组），当前 SlotDrop 仅支持对象数组；mcpack 未出现该形态，未实现。
- **attack.damage 对象形态 `{range_min, range_max}`**：文档合法但 mcpack 未出现，未实现。
- **filters 数组形态**（如原版 chicken `"filters":[...]`）：record 字段为 JsonObject 无法承载数组，仍硬失败；需改 record 类型，超出本次范围。

### 验证

- 新增 `src/test/java/io/github/tt432/eyelib/behavior/McpackComponentCodecCompatTest.java`（14 用例，输入均为上述日志中的 mcpack 实际 JSON 形态）：真实执行 14/14 通过（test-results XML timestamp 2026-07-24T20:32:05Z，failures=0）。
- 回归：`io.github.tt432.eyelib.behavior.*` 与 `io.github.tt432.eyelib.importer.addon.*` 测试任务真实执行（Gradle 输出 `:test` 无 UP-TO-DATE/FROM-CACHE 标记），全部通过。
- `mcmcp_build` 1.20.1 BUILD SUCCESSFUL。

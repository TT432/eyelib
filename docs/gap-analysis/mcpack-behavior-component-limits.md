# Mcpack 行为组件解析已知限制（P3）

> 以 Actions-and-Stuff-1.10-v2.mcpack 为对照输入，经 `BehaviorPackPublication.parseComponentGroup()` → `ComponentGroup.DISPATCH_CODEC` 路径实测确认的未实现项。均为 gameplay/AI 行为覆盖面限制，**不影响渲染正确性**。字段简写与可选性问题（P1/P2）已修复，见 [concepts/behavior-component-pitfalls.md](../concepts/behavior-component-pitfalls.md)。

## 一、事件过滤器：BaseFilter 仅识别 `actor_health`

### 症状

Bedrock 事件 JSON 中 `filters` 的 `test` 值除 `actor_health` 外一律抛 `IllegalStateException`，**整个事件被丢弃**（只影响 AI，不影响渲染）。

### 根因

`src/main/java/io/github/tt432/eyelib/behavior/event/filter/base/BaseFilter.java` 的 dispatch codec 两个方向都只实现了 `actor_health` 一个分支，其余 test 值走 `else { throw new IllegalStateException(...) }`。

mcpack 中实际出现但无法识别的 test 值：

| test | 受影响事件示例 |
|---|---|
| has_component | minecraft:entity_born, start_exploding(_forced) 等 |
| enum_property | start_unrolling, unroll, ageable_grow_up, start_peeking, roll_up, stop_peeking, threat_detected |
| on_fire | no_threat_detected |
| is_difficulty | entity_spawned, attacked |
| is_family | hive_destroyed |
| is_variant | add_can_ride |
| bool_property | find_hive_timeout |

### 修复方向

按 Bedrock 官方过滤器文档补全 filter test 体系：每个 test 一个 record + CODEC，注册进 BaseFilter 的 dispatch 双向分支。

## 二、`minecraft:behavior.*` AI 组件未注册

### 症状

大量 `behavior.*` 组件解析时打 `Unknown component type` 警告并**静默降级为 `EmptyComponent`**（`ComponentGroup.java` line 209），实体 AI 目标全部丢失。包括 look_at_entity / pickup_items / panic / float / tempt / breed / melee_box_attack / random_stroll / look_at_player / nearest_attackable_target / equip_item 等十几种，以及 `breedable`、`angry`、`attack_cooldown`、`attack_damage`、`dash`、`buoyant`、`grows_crop`、`transformation`、`custom_hit_test`、`game_event_movement_tracking`、`conditional_bandwidth_optimization`、`damage_sensor` 等非 behavior 前缀组件。

### 修复方向

大型功能开发：逐组件建 record + CODEC 并注册进 `ComponentGroup.DISPATCH_CODEC`。优先级按目标 mcpack 实际出现频率排。

## 三、已确认合法但未实现的字段形态

以下形态 Bedrock 官方文档允许但 mcpack 未实际出现，未实现；出现时仍会解析失败：

1. **`equipment.slot_drop_chance` 字符串数组简写**：文档允许 `"slot_drop_chance": ["mainhand"]`（slot 名字符串数组），当前 `Equipment.SlotDrop` 仅支持对象数组。
2. **`attack.damage` 对象形态** `{ "range_min": x, "range_max": y }`：文档合法，当前 `Attack.DAMAGE_CODEC` 只兼容数字 / 字符串 / `[min, max]` 数组。
3. **`filters` 数组形态**：如原版 chicken 的 `"filters": [...]`。当前 record 字段类型为 `JsonObject` 无法承载数组，硬失败；修复需改 record 字段类型（`JsonObject` → `JsonElement` 或专用过滤器模型）。

另：`BlockSensor` / `HurtOnCondition` / `Scheduler` 的 `filters` 字段仍是旧的 `Codec.STRING.xmap(JsonParser::parseString)` 实现，对真实内联 JSON 对象报 `Not a string`；需按 `EntitySensor` 等组件的 `Codec.PASSTHROUGH` 模式统一改造。

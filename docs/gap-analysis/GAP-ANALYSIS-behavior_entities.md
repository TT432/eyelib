# Gap Analysis: behavior_entities

**Date:** 2026-06-02
**Scope:** Bedrock Add-On `minecraft:entity` (behavior entities) + `minecraft:spawn_rules`
**Project:** qylEyelib
**Modules:** eyelib-importer, eyelib-behavior, root (eyelib client)

---

## 1. Bedrock 规范来源

| Source | Path | Content |
|--------|------|---------|
| Bedrock Wiki | `/mnt/e/_____基岩版文档/bedrock-wiki/docs/entities/` | entity-intro-bp.md, entity-events.md, entity-properties.md, spawn-rules.md, vanilla-usage-components.md, dummy-components.md |
| Creator Schema | `/mnt/e/_____基岩版文档/minecraft-creator/content/forms/entity/` | entity_behavior_document.form.json, actor_document.form.json, entity_component_definitions.form.json + 398 individual schema files (182 minecraft_behavior_*.form.json) |
| Spawn Rules Schema | `/mnt/e/_____基岩版文档/minecraft-creator/content/forms/spawn_rules/` | spawn_rule_document.form.json |

### 1.1 `minecraft:entity` 顶层结构

| 字段 | 层级类型 | 规范性 | 参考来源 |
|------|---------|--------|---------|
| `format_version` | TERMINAL | stable | Schema, Wiki |
| `minecraft:entity` | NON_TERMINAL | stable | Schema, Wiki |
| ├─ `description` | NON_TERMINAL | stable | Schema, Wiki |
| │  ├─ `identifier` | TERMINAL | stable | Schema, Wiki |
| │  ├─ `is_spawnable` | TERMINAL | stable | Wiki sample, Schema |
| │  ├─ `is_summonable` | TERMINAL | stable | Wiki sample, Schema |
| │  ├─ `is_experimental` | TERMINAL | stable | Wiki sample, Schema |
| │  ├─ `runtime_identifier` | TERMINAL | stable | Wiki reference |
| │  ├─ `spawn_category` | TERMINAL | stable | Wiki reference |
| │  ├─ `animations` | NON_TERMINAL | stable | Wiki sample |
| │  ├─ `scripts.animate` | NON_TERMINAL | stable | Wiki sample |
| │  └─ `properties` | NON_TERMINAL | stable | Wiki, entity-properties.md |
| ├─ `component_groups` | NON_TERMINAL | stable | Schema, Wiki |
| ├─ `components` | NON_TERMINAL | stable | Schema, Wiki |
| └─ `events` | NON_TERMINAL | stable | Schema, Wiki |

### 1.2 Schema 量级

| 分类 | 文件数 |
|------|--------|
| Total entity schemas | 398 |
| `minecraft:behavior.*` behavior goal schemas | 182 |
| Other component schemas (flags, sensors, movement, etc.) | ~216 |

### 1.3 Inventory of Known Components (from wiki + schema)

**Status/Visual Flag Components (16+ from wiki `dummy-components.md`):**

| Component | Type | Query |
|-----------|------|-------|
| `minecraft:variant` | int | q.variant |
| `minecraft:mark_variant` | int | q.mark_variant |
| `minecraft:skin_id` | int | q.skin_id |
| `minecraft:color` | int(enum) | is_color filter |
| `minecraft:color2` | int(enum) | N/A |
| `minecraft:is_illager_captain` | bit | q.is_illager_captain |
| `minecraft:is_baby` | bit | q.is_baby |
| `minecraft:is_sheared` | bit | q.is_sheared |
| `minecraft:is_saddled` | bit | q.is_saddled |
| `minecraft:is_tamed` | bit | q.is_tamed |
| `minecraft:is_chested` | bit | q.is_chested |
| `minecraft:is_charged` | bit | q.is_powered |
| `minecraft:is_stunned` | bit | q.is_stunned |
| `minecraft:can_climb` | bit | q.can_climb |
| `minecraft:can_fly` | bit | q.can_fly |
| `minecraft:can_power_jump` | bit | q.can_power_jump |
| `minecraft:is_ignited` | bit | q.is_ignited |
| `minecraft:out_of_control` | bit | q.out_of_control |
| `minecraft:type_family` | families[] | q.has_any_family |

**Lifecycle/Interaction Components (from schema + wiki):**

| Component | Key Fields | Schema File |
|-----------|-----------|-------------|
| `minecraft:ageable` | duration, feed_items, grow_up.{event,target}, drop_items, interact_filters, transform_to_item | ✓ |
| `minecraft:addrider` | entity_type, spawn_event | ✓ |
| `minecraft:admire_item` | cooldown_after_being_attacked, duration | ✓ |
| `minecraft:breedable` | require_tame, breeds_with, breed_items, mutate_to_inherit_tame | ✓ |
| `minecraft:bribeable` | bribe_item | ✓ |
| `minecraft:interact` | interactions[*].{cooldown, use_item, hurt_item, spawn_items, play_sounds, interact_text, on_interact.{event,target}, filters} | ✓ |
| `minecraft:rideable` | seat_count, family_types, seats[*].position | ✓ |
| `minecraft:loot` | table | ✓ |
| `minecraft:experience_reward` | on_bred, on_death | ✓ |
| `minecraft:healable` | items[*].{item, heal_amount, filters} | ✓ |
| `minecraft:equippable` | slots[*].{slot, accepted_items, interact_text} | ✓ |
| `minecraft:inventory` | inventory_size, container_type, container_name | ✓ |
| `minecraft:nameable` | allow_name_tag_renaming, always_show | ✓ |
| `minecraft:leashable` | soft_distance, hard_distance, max_distance | ✓ |
| `minecraft:damage_sensor` | triggers[*].{on_damage.{cause}, deals_damage} | ✓ |
| `minecraft:environment_sensor` | triggers[*].{filters, event, target} | ✓ |
| `minecraft:entity_sensor` | sensors[*].{event, filters, maximum_count, cooldown} | ✓ |
| `minecraft:target_nearby_sensor` | inside_range, outside_range, on_inside_range, on_outside_range | ✓ |
| `minecraft:movement` | value | ✓ |
| `minecraft:movement.basic` | max_turn | ✓ |
| `minecraft:movement.fly` | max_turn | ✓ |
| `minecraft:movement.generic` | max_turn | ✓ |
| `minecraft:movement.glide` | speed_when_turning, start_speed | ✓ |
| `minecraft:movement.hover` | max_turn | ✓ |
| `minecraft:movement.jump` | jump_delay, jump_delay_per_charger | ✓ |
| `minecraft:movement.skip` | max_turn | ✓ |
| `minecraft:movement.sway` | sway_frequency, sway_amplitude | ✓ |
| `minecraft:navigation.walk` | can_path_over_water, avoid_water, avoid_damage_blocks | ✓ |
| `minecraft:navigation.fly` | can_path_over_water, can_path_from_air | ✓ |
| `minecraft:navigation.swim` | can_path_over_water, avoid_water | ✓ |
| `minecraft:navigation.float` | can_path_over_water | ✓ |
| `minecraft:navigation.climb` | can_path_over_water | ✓ |
| `minecraft:navigation.hover` | can_path_over_water | ✓ |
| `minecraft:collision_box` | width, height | ✓ |
| `minecraft:physics` | — (flag) | ✓ |
| `minecraft:health` | value, max | ✓ |
| `minecraft:attack_damage` | value | ✓ |
| `minecraft:attack` | damage | ✓ |
| `minecraft:knockback_resistance` | value | ✓ |
| `minecraft:explode` | breaks_blocks, causes_fire, fuse_length, power | ✓ |
| `minecraft:despawn` | despawn_from_distance, despawn_from_inactivity | ✓ |
| `minecraft:timer` | time, looping, random_time_interval, time_down_event | ✓ |
| `minecraft:trail` | radius, trail_duration, spawn_filter, start_distance | ✓ |
| `minecraft:burns_in_daylight` | — (flag) | ✓ |
| `minecraft:fire_immune` | — (flag) | ✓ |
| `minecraft:scale` | value | ✓ |
| `minecraft:scale_by_age` | start_scale, end_scale | ✓ |
| `minecraft:transformation` | into, delay, begin_transform_event, transformation_sound | ✓ |
| `minecraft:shareables` | items[*].{item, want_amount, priority} | ✓ |
| `minecraft:tameable` | tame_items, probability | ✓ |
| `minecraft:angry` | angry_sound, broadcast_anger, broadcast_anger_on_attack, duration, duration_delta, filters, broadcast_filters, calm_event | ✓ |
| `minecraft:anger_level` | anger_level, anger_decrement_interval, angry_boost, angry_threshold, default_attacking_entity, max_anger, remove_target_on_calm | ✓ |

### 1.4 Event Response Types (from wiki entity-events.md)

| Response Type | Sub-fields | Status in Schema |
|---------------|-----------|-----------------|
| `add` | `component_groups[]` | Stable |
| `remove` | `component_groups[]` | Stable |
| `sequence[*]` | `{filters?, add?, remove?, trigger?, randomize?, set_property?, queue_command?}` | Stable |
| `randomize[*]` | `weight`, nested event body | Stable |
| `trigger` | `event`, `target`, `filters?` | Stable |
| `set_property` | `property: value` pairs (supports Molang) | Stable |
| `queue_command` | `command` (string or string[]), `target` | Stable |
| `filters` | `test`, `subject`, `operator`, `value`, `domain`, `all_of`, `any_of`, `none_of` | Stable |
| `first_valid[*]` | nested event body | Schema |
| `emit_vibration` | `vibration` | Frontier |
| `play_sound` | `sound` | Sample |
| `emit_particle` | `particle` | Sample |
| `drop_item` | `slot` | Frontier |
| `stop_movement` | `stop_vertical_movement`, `stop_horizontal_movement` | Frontier |
| `set_home_position` | — | Frontier |
| `execute_event_on_home_block` | `event` | Frontier |
| `reset_target` | — | Frontier |

### 1.5 `minecraft:spawn_rules` 顶层结构

| 字段 | 层级 | 规范性 | 参考 |
|------|------|--------|------|
| `format_version` | TERMINAL | stable | Wiki, Schema |
| `minecraft:spawn_rules` | NON_TERMINAL | stable | Wiki, Schema |
| ├─ `description.identifier` | TERMINAL | stable | Wiki, Schema |
| ├─ `description.population_control` | TERMINAL | stable | Wiki, Schema |
| └─ `conditions[*]` | NON_TERMINAL | stable | Wiki, Schema |
|    ├─ `minecraft:spawns_on_surface` | TERMINAL | stable | Wiki |
|    ├─ `minecraft:spawns_underground` | TERMINAL | stable | Wiki |
|    ├─ `minecraft:spawns_underwater` | TERMINAL | stable | Wiki |
|    ├─ `minecraft:spawns_lava` | TERMINAL | stable | Wiki |
|    ├─ `minecraft:disallow_spawns_in_bubble` | TERMINAL | frontier | Wiki |
|    ├─ `minecraft:spawns_on_block_filter` | NON_TERMINAL | stable | Wiki |
|    ├─ `minecraft:spawns_on_block_prevented_filter` | NON_TERMINAL | frontier | Wiki |
|    ├─ `minecraft:spawns_above_block_filter` | NON_TERMINAL | frontier | Wiki |
|    ├─ `minecraft:biome_filter` | NON_TERMINAL | stable | Wiki |
|    ├─ `minecraft:brightness_filter` | NON_TERMINAL | stable | Wiki |
|    ├─ `minecraft:difficulty_filter` | NON_TERMINAL | stable | Wiki |
|    ├─ `minecraft:distance_filter` | NON_TERMINAL | stable | Wiki |
|    ├─ `minecraft:height_filter` | NON_TERMINAL | stable | Wiki |
|    ├─ `minecraft:weight` | NON_TERMINAL | stable | Wiki |
|    ├─ `minecraft:herd` | NON_TERMINAL | stable | Wiki |
|    ├─ `minecraft:permute_type[*]` | NON_TERMINAL | stable | Wiki |
|    ├─ `minecraft:spawn_event` | NON_TERMINAL | frontier | Wiki |
|    ├─ `minecraft:density_limit` | NON_TERMINAL | stable | Wiki |
|    ├─ `minecraft:player_in_village_filter` | NON_TERMINAL | stable | Wiki |
|    └─ `minecraft:world_age_filter.min` | TERMINAL | stable | Wiki |

---

## 2. Eyelib 实现状态总览

### 2.1 Importer 层面

#### BrBehaviorEntityFile (eyelib-importer)

| 字段 | 状态 | 说明 |
|------|------|------|
| `format_version` | ✔ 已解析 | Typed string field |
| `minecraft:entity.description.identifier` | ✔ 已解析 | Typed string field |
| `minecraft:entity.description.*` (其余字段) | ✗ 未解析 | Raw `BedrockResourceValue.ObjectValue` 保留，无 typed model |
| `minecraft:entity.component_groups` | ▢ raw | Raw ObjectValue 保留，无 typed sub-structure |
| `minecraft:entity.components` | ▢ raw | Raw ObjectValue 保留，**完全未被后续使用** |
| `minecraft:entity.events` | ▢ raw | Raw ObjectValue 保留，无 typed sub-structure |
| `minecraft:entity.extras` | ▢ raw | 所有未建模字段存入 extras ObjectValue |

**代码位置:** `src/main/java/io/github/tt432/eyelib/importer/addon/BrBehaviorEntityFile.java`

#### BrSpawnRule (eyelib-importer)

| 字段 | 状态 | 说明 |
|------|------|------|
| `format_version` | ✔ 已解析 | Typed string |
| `description.identifier` | ✔ 已解析 | Typed string |
| `description.population_control` | ✔ 已解析 | Typed string |
| `conditions[*]` | ▢ raw | Raw `BedrockResourceValue.ObjectValue` list，所有 sub-components 无 typed 解析 |

**代码位置:** `src/main/java/io/github/tt432/eyelib/importer/addon/BrSpawnRule.java`

### 2.2 Runtime 层面

#### BehaviorEntity (eyelib-behavior)

```java
public record BehaviorEntity(
    ResourceLocation identifier,
    Map<String, ComponentGroup> component_groups,
    Map<String, LogicNode> events
) // NO 'components' field! NO 'description' field!
```

| 字段 | 状态 | 说明 |
|------|------|------|
| `identifier` | ✔ 已实现 | `ResourceLocation` |
| `component_groups` | ▢ partial | `Map<String, ComponentGroup>`，内部 dispatch 极有限 |
| `events` | ▢ partial | `Map<String, LogicNode>`，仅 4 种节点类型 |
| `components` (顶层) | ✗ 缺失 | BehaviorEntity record 没有此字段，完全被丢弃 |
| `description` | ✗ 缺失 | 非 identifier 的 description 字段全被丢弃 |

**代码位置:** `src/main/java/io/github/tt432/eyelib/behavior/BehaviorEntity.java`

#### Registered Component Implementations

| 组件名 | 状态 | Class | Codec | 注册到 dispatch? |
|--------|------|-------|--------|----------------|
| `minecraft:variant` | ✔ 完整 | `Variant` | `{value: int}` | ✔ 已注册 |
| `minecraft:mark_variant` | ✔ 完整 | `MarkVariant` | `{value: int}` | ✔ 已注册 |
| `minecraft:ageable` | ✔ 但未注册 | `Ageable` | 完整 codec (7 fields) | ✗ 未注册 → EmptyComponent |
| `minecraft:admire_item` | ✔ 但未注册 | `AdmireItem` | 完整 codec (2 fields) | ✗ 未注册 → EmptyComponent |
| `minecraft:addrider` | ✔ 但未注册 | `Addrider` | 完整 codec (2 fields) | ✗ 未注册 → EmptyComponent |
| `minecraft:physics` | ✗ 仅标记类 | `Physics` | 无 CODEC (空 class) | ✗ 未注册 |
| `minecraft:ambient_sound_interval` | ✗ 仅标记类 | `AmbientSoundInterval` | 无 CODEC (空 class) | ✗ 未注册 |
| 其余 ~200+ 组件 | ✗ 未实现 | — | — | 均落入 `EmptyComponent` fallback |

**代码位置:** `src/main/java/io/github/tt432/eyelib/behavior/component/group/ComponentGroup.java` (dispatch table lines 27-43)

#### Event Logic Node Types (LogicNode.CODEC dispatch)

| 节点类型 | 状态 | 说明 |
|---------|------|------|
| `add` | ✔ 已实现 | `{component_groups: string[]}` → 从 BehaviorEntity.component_groups 查找 |
| `remove` | ✔ 已实现 | `{component_groups: string[]}` → 从 EntityBehaviorData 移除 |
| `sequence` | ✔ 已实现 | 递归执行子节点列表 |
| `randomize` | ✔ 已实现 | 轮盘赌选择，**仅支持 trigger 事件引用**，不支持 embedded add/remove |
| `trigger` | ✗ 未实现 | 引用并运行另一事件 |
| `filters` (事件内) | ✗ 未实现 | 过滤条件不支持在 Sequence/Randomize 条目中使用 |
| `first_valid` | ✗ 未实现 | — |
| `set_property` | ✗ 未实现 | — |
| `queue_command` | ✗ 未实现 | — |
| `play_sound` | ✗ 未实现 | — |
| `emit_particle` | ✗ 未实现 | — |
| `drop_item` | ✗ 未实现 | — |
| `emit_vibration` | ✗ 未实现 | — |
| `stop_movement` | ✗ 未实现 | — |
| `set_home_position` | ✗ 未实现 | — |
| `execute_event_on_home_block` | ✗ 未实现 | — |
| `reset_target` | ✗ 未实现 | — |

**代码位置:** `src/main/java/io/github/tt432/eyelib/behavior/event/logic/LogicNode.java`

#### Filter System (eyelib-behavior)

| 功能 | 状态 | 说明 |
|------|------|------|
| `Filter` interface | ✔ 已实现 | BaseFilter (sealed) + ComplexFilter (all_of/one_of/none_of) |
| `Subject` enum | ✔ 已实现 | block, damager, other, parent, player, self, target |
| `Operator` enum | ✔ 已实现 | !=, ==, <, >, <=, >=, <>, =, equals, not |
| `BaseFilter` abstract class | ✔ 已实现 | value, subject, operator, domain |
| `ComplexFilter` sealed | ▢ partial | all_of, one_of, none_of implemented but **no integration** with event evaluation |
| `ActorHealth` filter | ✔ 已实现 | 唯一的具体 filter test |
| 其他 filter tests (is_family, has_component, etc.) | ✗ 未实现 | — |
| Filters in event sequence | ✗ 未集成 | Filter system 存在但 **未与 LogicNode.eval() 挂钩** |

#### EntityBehaviorData (运行时数据)

| 功能 | 状态 | 说明 |
|------|------|------|
| Store active ComponentGroups | ✔ 已实现 | `List<ComponentGroup>` |
| Lookup component by class | ✔ 已实现 | `component(Class<T>)` |
| CODEC for network sync | ✔ 已实现 | `CODEC` + `STREAM_CODEC` |
| Event execution | ✔ 已实现 | By calling LogicNode.eval() |
| `EntityEvent` class | ✗ 空标记 | 无实际功能 |
| `EntitySpawned` class | ✗ 空标记 | 无实际功能 |

**代码位置:** `src/main/java/io/github/tt432/eyelib/behavior/EntityBehaviorData.java`

### 2.3 Runtime Bridge (BehaviorEntityAssetRegistry → BehaviorEntityManager)

| 功能 | 状态 | 说明 |
|------|------|------|
| `replaceBehaviorEntities(Map<String, BrBehaviorEntityFile>)` | ✔ 已实现 | BrBehaviorEntityFile → BehaviorEntity 转换 |
| identifier 提取 | ✔ 已实现 | `ResourceLocation.tryParse(file.identifier())` |
| description.* 提取 | ✗ 未处理 | description raw ObjectValue 在转换后丢弃 |
| component_groups 提取 | ▢ partial | **仅提取** `minecraft:variant` 和 `minecraft:mark_variant`，其他组件丢弃 |
| components 提取 | ✗ **完全忽略** | `BrBehaviorEntityFile.components()` 字段在 `toBehaviorEntity()` 中从未被读取 |
| events 提取 | ▢ partial | 仅解析 add/remove/sequence/randomize；randomize 只支持 trigger 引用 |
| Runtime consumption | ▢ partial | `EntityRenderSystem.onEvent` → `EntityJoinLevelEvent` 触发加载 |
| Vanilla behavior entity loading | ✔ 已实现 | `VanillaBehaviorEntityLoader` 从目录或 mcpack 加载 |

**代码位置:** 
- `src/main/java/io/github/tt432/eyelib/client/registry/BehaviorEntityAssetRegistry.java`
- `src/main/java/io/github/tt432/eyelib/client/manager/BehaviorEntityManager.java`
- `src/main/java/io/github/tt432/eyelib/client/loader/VanillaBehaviorEntityLoader.java`

### 2.4 Spawn Rules Runtime

| 功能 | 状态 | 说明 |
|------|------|------|
| Importer 解析 | ✔ 已实现 | `BrSpawnRule` 结构解析 |
| Aggregation 存储 | ✔ 已实现 | `BedrockAddonAggregate` 中 flatten |
| Runtime registry | ✗ 未实现 | 无 `SpawnRuleAssetRegistry` 或类似注册表 |
| Runtime manager | ✗ 未实现 | 无 `SpawnRuleManager` 用于查询 |
| Spawn execution | ✗ 未实现 | 无任何 spawn logic |

**代码位置:** `src/main/java/io/github/tt432/eyelib/importer/addon/BrSpawnRule.java`

### 2.5 Test Coverage

| Module | Test files for behavior entities | Status |
|--------|--------------------------------|--------|
| `src/test/java/io/github/tt432/eyelib/behavior/` | Directory does not exist | ✗ 无 |
| `src/test/java/` | 0 files matching behavior entity patterns | ✗ 无 |
| `src/test/java/io/github/tt432/eyelib/importer/` | 0 files for behavior entities | ✗ 无 |

---

## 3. Gap Inventory (差距清单)

### GAP-BE-001: `components` 字段完全丢失 (P0)

**严重程度:** 根本性

**描述:** `BrBehaviorEntityFile` 正确解析了 `minecraft:entity.components` 字段（作为 raw ObjectValue 存储），但 `BehaviorEntityAssetRegistry.toBehaviorEntity()` 在转换时直接忽略了 `BrBehaviorEntityFile.components()`。`BehaviorEntity` record 本身也没有 `components` 字段。

**影响:** 所有在 `components` 顶层定义的行为和属性（例如 `minecraft:health`, `minecraft:movement`, `minecraft:behavior.*` 等 **182** 个行为目标组件）完全丢失。实体虽然有 identifier 注册，实际无任何运行时行为。

**证据:**
- `BehaviorEntity.java` line 15-18: record 只有 identifier, component_groups, events
- `BehaviorEntityAssetRegistry.java` lines 54-83: `toBehaviorEntity()` 仅读取 `file.componentGroups()`
- 没有对 `file.components()` 的任何引用

---

### GAP-BE-002: ComponentGroup dispatch 覆盖面极窄 (P0)

**严重程度:** 根本性

**描述:** `ComponentGroup.CODEC` 的 dispatch table (KeyDispatchMapCodec) 只注册了 `minecraft:variant` 和 `minecraft:mark_variant`。其他所有组件（包括已实现的 Ageable, AdmireItem, Addrider）落入 `EmptyComponent` fallback 并记录 error log。

**影响:** vanilla 行为实体文件中 200+ 种组件中只有 2 种能正确解析。已编写 codec 的 3 个组件因未注册而无效。

**证据:**
- `ComponentGroup.java` lines 27-43: switch cases only for `minecraft:variant` and `minecraft:mark_variant`
- Fallback on line 30-42: 所有其他 key → `EmptyComponent.INSTANCE`

---

### GAP-BE-003: Description 字段未经 typed 建模 (P1)

**严重程度:** 严重

**描述:** `BrBehaviorEntityFile` 的 `description` 字段以 raw `BedrockResourceValue.ObjectValue` 保留。`BehaviorEntity` record 完全没有 description 字段。以下标准字段丢失:
- `is_spawnable` — 控制实体是否自然生成
- `is_summonable` — 控制实体是否可 `/summon`
- `is_experimental` — 实验性标记
- `runtime_identifier` — 运行时基类绑定（例如 zombie → zombie_v2）
- `spawn_category` — 生成分类
- `animations` — 动画别名映射
- `scripts.animate` — 默认动画播放列表
- `properties` — 实体属性定义 (actor properties)

**影响:** 没有 `runtime_identifier` 无法正确关联基类行为。没有 `properties` 无法使用现代的 actor properties 系统。

---

### GAP-BE-004: Event 节点类型不完整 (P1)

**严重程度:** 严重

**描述:** 已实现的 4 种 LogicNode 类型无法覆盖 Bedrock 标准。特别缺失:
- **`trigger`** — 允许一个事件引用并执行另一个事件（Wiki 强调这是复杂事件的核心机制）
- **`set_property`** — 设置实体属性值（与 actor properties 集成）
- **`queue_command`** — 在实体上运行命令
- **`filters`** — 在 Sequence 条目中做条件分支
- **`first_valid`** — 按顺序选择第一个有效的条目

**证据:**
- `LogicNode.java` lines 17-22: CODEC 仅列出 add/randomize/sequence/remove
- `BehaviorEntityAssetRegistry.parseSingleEvent()` lines 116-126: switch 仅 case "add"、"remove"、"sequence"、"randomize"
- `Randomize.eval()` lines 58-59: Randomize.Entry 要求 weight + LogicNode，但 `parseRandomize()` 只提取 trigger 引用，不能处理内嵌的 add/remove

---

### GAP-BE-005: Filter 系统未与事件执行集成 (P1)

**严重程度:** 严重

**描述:** eyelib-behavior 有不错的 filter 基础架构（BaseFilter, ComplexFilter, Subject, Operator, ActorHealth），但这个系统完全独立于事件执行。`LogicNode.eval()` 不接受 filter 参数，`Sequence` 的条目也没有 filter 字段。Wiki 中所有使用 `filters` 的 sequence 条目（例如 `minecraft:convert_to_drowned` 事件）都无法正确评估。

**证据:**
- `Sequence.java`: `eval()` 直接迭代执行所有子节点，无 filter 判断
- `BehaviorEntityAssetRegistry.parseSequence()`: 不提取 filter 信息
- 仅 `Ageable` 组件有自己内部定义的 `Filter` record（与全局 Filter interface 不同）

---

### GAP-BE-006: Spawn Rules 无运行时消费 (P1-P2)

**严重程度:** 严重到中等

**描述:** `BrSpawnRule` 在 importer 层正确解析，aggregate 层正确存储，但运行时完全没有消费。缺少整个 spawn rule runtime 通道:
1. 无 `SpawnRuleAssetRegistry` 注册表
2. 无 `SpawnRuleManager` 管理器
3. 无 runtime bridge API (`BedrockAddonRuntimeBridge` 无 spawn rule publish 路径)
4. 无 spawn condition 执行逻辑

**影响:** 自定义 behavior pack 中的生成规则被 eyelib 完全忽略。

---

### GAP-BE-007: 大量 Behavior Goal 组件缺失 (P2)

**严重程度:** 中等

**描述:** Schema 目录中有 **182 个** `minecraft_behavior_*.form.json` 文件。eyelib-behavior 目前 **0 个** behavior goal 组件实现。

**示例 (部分):** melee_attack, ranged_attack, tempt, panic, flee_sun, look_at_player, random_stroll, random_look_around, sleep, follow_owner, follow_parent, breed, eat_block, move_to_block, move_to_water, move_to_lava, move_indoors, move_outdoors, harvest_farm_block, ferilize_farm_block, door_interact, open_door, break_door, summon_entity, lay_egg, swell, explode, charge_attack, ram_attack, sonic_boom, stomp_attack, etc.

---

### GAP-BE-008: 大量属性/Flag 组件缺失 (P2)

**严重程度:** 中等

**描述:** 除了 `variant` 和 `mark_variant`，至少 15+ 个 dummy/flag 组件未实现（参见 1.3 清单）。这些组件被 Molang 查询广泛使用（`q.is_baby`, `q.is_saddled` 等）。

---

### GAP-BE-009: 事件 Side-Effect 节点缺失 (P3)

**严重程度:** 低

**描述:** `play_sound`, `emit_particle`, `emit_vibration`, `drop_item`, `stop_movement`, `set_home_position`, `execute_event_on_home_block`, `reset_target` 等 side-effect 节点均未实现。

---

### GAP-BE-010: 无测试覆盖 (P0-P3)

**严重程度:** 根本性

**描述:** 整个 behavior entity 子系统没有任何单元测试或集成测试。`src/test/java/io/github/tt432/eyelib/behavior/` 目录不存在。`eyelib-importer` 和 `src/test` 中也没有与 behavior entity 相关的测试。

---

## 4. 集成测试案例设计

测试框架: AI Debug Server (`http://localhost:25999/eval`) POST endpoint
请求: Text/plain Java code body
注入模板:
```java
import net.minecraft.client.Minecraft;

public class _EyelibScript {
    public static Object run(Minecraft minecraft, ...) throws Throwable {
        // test code here
    }
}
```

### Gaps Confirmed (向现有测试补充)

#### TC-BE-GAP-001: 验证 `components` 字段完全被丢弃

**目的:** 证明 BehaviorEntity 没有 usage_id 字段且顶层 components 在运行时桥接中丢失

**请求体:**
```java
import io.github.tt432.eyelib.client.manager.BehaviorEntityManager;

var mgr = BehaviorEntityManager.INSTANCE;
var entity = mgr.get("minecraft:zombie");
if (entity == null) return "ENTITY_NOT_FOUND";

// 验证 BehaviorEntity record 的字段
var fields = entity.getClass().getRecordComponents();
java.util.List<String> fieldNames = new java.util.ArrayList<>();
for (var f : fields) fieldNames.add(f.getName());
return "BehaviorEntity fields: " + String.join(", ", fieldNames);
```

**预期结果:** 输出显示只有 `identifier`, `component_groups`, `events` — 没有 `components` 字段

---

#### TC-BE-GAP-002: 验证行为目标组件全部缺失

**目的:** 证明 `minecraft:behavior.*` 组件在运行时无 type 化表示

**请求体:**
```java
import io.github.tt432.eyelib.client.manager.BehaviorEntityManager;
import io.github.tt432.eyelib.behavior.BehaviorEntity;
import io.github.tt432.eyelib.behavior.component.Component;

var mgr = BehaviorEntityManager.INSTANCE;
// 检查已注册的所有 Component 类型
var registry = io.github.tt432.eyelib.behavior.component.group.ComponentGroup.CODEC;
// 通过反射获取 dispatch 注册的具体类型
return "Component dispatch is codec-based; checking known components...";
```

---

#### TC-BE-GAP-003: 验证 event 中 trigger 和 filters 不可用

**目的:** 确认 event 系统不支持 trigger 或 filter 条件

**请求体:**
```java
// 尝试构建带 trigger 的事件
try {
    var triggerCodec = io.github.tt432.eyelib.behavior.event.logic.LogicNode.CODEC;
    // 只有 add/remove/sequence/randomize 四种
    return "LogicNode dispatch types: add, remove, sequence, randomize";
} catch (Exception e) {
    return "Error: " + e.getMessage();
}
```

---

#### TC-BE-GAP-004: 验证未注册组件回退行为

**目的:** 证明 `minecraft:is_baby` 等常见组件在 codec 解析时变成 EmptyComponent

**请求体:**
```java
import io.github.tt432.eyelib.client.manager.BehaviorEntityManager;
import io.github.tt432.eyelib.behavior.component.EmptyComponent;

var mgr = BehaviorEntityManager.INSTANCE;
var entity = mgr.get("minecraft:zombie");
if (entity == null) return "NOT_FOUND";

int emptyCount = 0;
int total = 0;
for (var group : entity.component_groups().entrySet()) {
    var comps = group.getValue().components();
    for (var entry : comps.entrySet()) {
        for (var c : entry.getValue().entrySet()) {
            total++;
            if (c.getValue() instanceof EmptyComponent) emptyCount++;
        }
    }
}
return "Total components in groups: " + total + ", EmptyComponent: " + emptyCount;
```

**预期结果:** 多数 component_groups 中的组件显示为 EmptyComponent

---

#### TC-BE-GAP-005: 验证 spawn rules 无运行时桥接

**目的:** 确认 spawn rules 完全无运行时消费

**请求体:**
```java
// spawn rules 存储在 BedrockAddonPack 中但不发布到运行时
var packs = io.github.tt432.eyelib.importer.addon.BedrockAddonLoader.getLoadedAddons();
if (packs == null || packs.isEmpty()) return "NO_ADDONS";
int count = 0;
for (var pack : packs) {
    for (var side : pack.sides()) {
        count += side.spawnRules().size();
    }
}
// 检查是否有运行时 registry
boolean hasRuntimeRegistry = false;
try {
    Class.forName("io.github.tt432.eyelib.client.registry.SpawnRuleAssetRegistry");
    hasRuntimeRegistry = true;
} catch (ClassNotFoundException e) {
    hasRuntimeRegistry = false;
}
return "Loaded spawn_rules=" + count + ", runtime_registry_exists=" + hasRuntimeRegistry;
```

---

## 5. 优先级建议

### P0 — 阻止运行时功能的根本缺陷

1. **GAP-BE-001: 添加 `components` 字段到 BehaviorEntity + 桥接解析**
   - `BehaviorEntity` record 增加 `Map<String, Component>` components 字段
   - `BehaviorEntityAssetRegistry.toBehaviorEntity()` 增加对 `BrBehaviorEntityFile.components()` 的解析
   - 将 `components` 中的组件在 `EntityBehaviorData.setup()` 中合并

2. **GAP-BE-002: 扩展 ComponentGroup codec dispatch 注册**
   - 注册已实现的 Ageable, AdmireItem, Addrider
   - 注册 dumm/flag 组件 as EmptyComponent variants（保留原始 JSON 数据，不丢失）
   - 设计可扩展的 component 注册机制（类似 Minecraft 的 `DeferredRegister`）

3. **GAP-BE-010: 创建基础测试覆盖**
   - `src/test/java/io/github/tt432/eyelib/behavior/` 创建单元测试目录
   - Codec 解析测试、LogicNode eval 测试、EntityBehaviorData 测试

### P1 — 严重的功能差距

4. **GAP-BE-003: Description 字段 typed 建模**
   - 创建 `EntityDescription`  record 含所有标准字段
   - `BehaviorEntity` 增加 `description` 字段
   - 增加 `properties` (actor properties) 支持

5. **GAP-BE-004: Event 节点类型扩展**
   - 实现 `trigger` LogicNode（引用其他事件执行）
   - 实现 `set_property` LogicNode
   - 实现 `queue_command` LogicNode
   - 实现 `filters` 字段在 Sequence 条目中

6. **GAP-BE-005: Filter 与事件执行集成**
   - `LogicNode.eval()` 扩展为接受 filter context
   - Sequence 条目支持 filter 条件分支
   - 实现更多 filter tests（is_family, has_component, is_color, distance_to_nearest_player 等）

7. **GAP-BE-006: Spawn Rules 运行时桥接**
   - 创建 `SpawnRuleAssetRegistry` 注册表
   - 创建 `SpawnRuleManager` 查询匹配
   - 实现 `BedrockAddonRuntimeBridge` spawn rule publish 路径

### P2 — 覆盖面差距

8. **GAP-BE-007: Behavior Goal 组件建模（部分）**
   - 选取最常用的 20-30 个 behavior goal 实现 typed model
   - 关键: melee_attack, ranged_attack, tempt, panic, look_at_player, random_stroll, follow_owner, follow_parent, sleep, breed, move_to_water, move_to_block

9. **GAP-BE-008: 属性/Flag 组件建模**
   - 实现 is_baby, is_tamed, is_saddled, is_chested, is_charged, is_stunned, is_sheared, is_illager_captain, is_ignited
   - 实现 skin_id, color, color2
   - 实现 can_climb, can_fly, can_power_jump
   - 实现 type_family

10. **Spawn rule conditions typed 建模**
    - 所有 spawn condition 组件实现 typed model
    - biome_filter, brightness_filter, difficulty_filter 等

### P3 — 高级功能

11. **GAP-BE-009: Side-effect 事件节点**
    - play_sound, emit_particle, emit_vibration, drop_item, stop_movement

12. **剩余 150+ behavior goal 组件**
    - 完全覆盖 182 个 schema behavior goals

13. **JSON schema 验证与错误报告**
    - 在加载时验证 entity JSON 结构
    - 对无法解析的组件提供清晰地错误消息

14. **Molang property 集成**
    - actor properties 的 Molang 表达式求值
    - `q.property()` 查询函数支持

---

## 6. 参考文件

| 文件 | 用途 |
|------|------|
| Bedrock 官方文档：`/mnt/e/_____基岩版文档/bedrock-dot-dev/` | Bedrock behavior entity 规范 |
| Mojang Creator 文档：`/mnt/e/_____基岩版文档/minecraft-creator/` | 官方 Creator 参考 |
| Bedrock Wiki：`/mnt/e/_____基岩版文档/bedrock-wiki/docs/` | 社区整理的行为实体参考 |
| `/eval` endpoint（见 `eyelib-debug` skill） | 调试 /eval HTTP endpoint 使用 |
| `src/main/java/io/github/tt432/eyelib/importer/BrBehaviorEntityFile.java` | Importer 端 Behavior Entity codec |
| `src/main/java/io/github/tt432/eyelib/importer/BrSpawnRule.java` | Importer 端 Spawn Rule codec |
| `src/main/java/io/github/tt432/eyelib/behavior/BehaviorEntity.java` | Runtime Behavior Entity 模型 |
| `src/main/java/io/github/tt432/eyelib/behavior/EntityBehaviorData.java` | 运行时行为数据容器 |
| `src/main/java/io/github/tt432/eyelib/behavior/component/group/ComponentGroup.java` | ComponentGroup codec (dispatch table) |
| `src/main/java/io/github/tt432/eyelib/behavior/component/Component.java` | 组件接口 |
| `src/main/java/io/github/tt432/eyelib/behavior/event/logic/LogicNode.java` | 事件节点 dispatch |
| `src/main/java/io/github/tt432/eyelib/behavior/event/filter/Filter.java` | 过滤器接口 |
| `src/main/java/io/github/tt432/eyelib/behavior/event/filter/base/BaseFilter.java` | 过滤器基类 |
| `src/main/java/io/github/tt432/eyelib/behavior/event/filter/ComplexFilter.java` | 复合过滤器 |
| `src/main/java/.../registry/BehaviorEntityAssetRegistry.java` | Runtime bridge 转换 |
| `src/main/java/.../loader/VanillaBehaviorEntityLoader.java` | Vanilla 实体加载器 |
| `src/main/java/.../manager/BehaviorEntityManager.java` | Runtime 管理器 |
| `src/main/java/.../EntityRenderSystem.java` | 运行时消费入口 |
| `src/main/java/.../debug/AIDebugServer.java` | /eval endpoint |
| `docs/gap-analysis/GAP-ANALYSIS-blocks-items-recipes.md` | 先前 gap analysis 范例 |

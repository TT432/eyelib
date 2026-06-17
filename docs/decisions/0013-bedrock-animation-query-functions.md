# ADR-0013 附录 A：基岩版 query.* 函数完整清单（316 个）

**Parent:** [ADR-0013](0013-bedrock-animation-controller-and-calculation.md)
**Source:** Minecraft Windows x64 1.26.2101.0 UWP，从 mc_strings.txt 提取并去重

> 本清单由 RTTI 与字符串扫描去重得到，**不代表每个函数都在所有 MolangVersion 下可用**。部分函数可能受 `experimental_molang_features` 或版本开关控制。
>
> 标注约定：
> - **[obs]** = eyelib 已确认实现（详见 `io.github.tt432.eyelib.molang.mapping.MolangQuery` 与 root 模块的 `MolangQuery.java`）
> - **[gap]** = eyelib 未实现或待核对
> - **[deprecated]** = 字符串里有 DEPRECATED 标记

## A

- query.above_top_solid
- query.actor_count
- query.all
- query.all_animations_finished
- query.all_tags
- query.anger_level
- query.anim_time
- query.any
- query.any_animation_finished
- query.any_tag
- query.approx_eq
- query.armor_color_slot
- query.armor_damage_slot
- query.armor_material_slot
- query.armor_texture_slot
- query.average_frame_time

## B

- query.base_swing_duration
- query.block_face
- query.block_has_all_tags
- query.block_has_any_tag
- query.block_neighbor_has_all_tags
- query.block_neighbor_has_any_tag
- query.block_property
- query.block_state
- query.blocking
- query.body_x_rotation
- query.body_y_rotation
- query.bone_aabb
- query.bone_orientation_matrix
- query.bone_orientation_trs
- query.bone_origin
- query.bone_rotation

## C

- query.camera_distance_range_lerp
- query.camera_rotation
- query.can_climb
- query.can_damage_nearby_mobs
- query.can_dash
- query.can_fly
- query.can_power_jump
- query.can_swim
- query.can_walk
- query.cape_flap_amount
- query.cardinal_block_face_placed_on
- query.cardinal_facing
- query.cardinal_facing_2d
- query.cardinal_player_facing
- query.client_max_render_distance
- query.client_memory_tier
- query.combine_entities
- query.cooldown_time
- query.cooldown_time_remaining
- query.count
- query.current_squish_value

## D

- query.dash_cooldown_progress
- query.death_ticks
- query.debug_output
- query.distance_from_camera

## E

- query.effect_emitter_count
- query.effect_particle_count
- query.entity_biome_has_all_tags
- query.entity_biome_has_any_identifier
- query.entity_biome_has_any_tags
- query.equipment_count
- query.equipped_item_all_tags
- query.equipped_item_any_tag
- query.equipped_item_is_attachable
- query.eye_target_x_rotation
- query.eye_target_y_rotation

## F

- query.facing_target_to_range_attack
- query.frame_alpha

## G

- query.get_actor_info_id
- query.get_animation_frame
- query.get_default_bone_pivot
- query.get_equipped_item_name
- query.get_level_seed_based_fraction
- query.get_locator_offset
- query.get_name
- query.get_pack_setting
- query.get_root_locator_offset
- query.graphics_mode_is_any
- query.ground_speed

## H

- query.had_component_group
- query.has_any_family
- query.has_any_leashed_entity_of_type
- query.has_armor_slot
- query.has_biome_tag
- query.has_block_property
- query.has_block_state
- query.has_cape
- query.has_collision
- query.has_dash_cooldown
- query.has_gravity
- query.has_head_gear
- query.has_owner
- query.has_player_rider
- query.has_property
- query.has_rider
- query.has_target
- query.head_roll_angle
- query.head_x_rotation
- query.head_y_rotation
- query.health
- query.heartbeat_interval
- query.heartbeat_phase
- query.heightmap
- query.hurt_direction
- query.hurt_time

## I

- query.in_range
- query.invulnerable_ticks
- query.is_admiring
- query.is_alive
- query.is_angry
- query.is_attached
- query.is_attached_to_entity
- query.is_avoiding_block
- query.is_avoiding_mobs
- query.is_baby
- query.is_breathing
- query.is_bribed
- query.is_carrying_block
- query.is_casting
- query.is_celebrating
- query.is_celebrating_special
- query.is_charged
- query.is_charging
- query.is_chested
- query.is_cooldown_category
- query.is_crawling
- query.is_critical
- query.is_croaking
- query.is_dancing
- query.is_delayed_attacking
- query.is_digging
- query.is_eating
- query.is_eating_mob
- query.is_elder
- query.is_emerging
- query.is_emoting
- query.is_enchanted
- query.is_feeling_happy
- query.is_fire_immune
- query.is_first_person
- query.is_ghost
- query.is_gliding
- query.is_grazing
- query.is_idling
- query.is_ignited
- query.is_illager_captain
- query.is_in_contact_with_water
- query.is_in_lava
- query.is_in_love
- query.is_in_ui
- query.is_in_water
- query.is_in_water_or_rain
- query.is_interested
- query.is_invisible
- query.is_item_equipped
- query.is_item_name_any
- query.is_jump_goal_jumping
- query.is_jumping
- query.is_laying_down
- query.is_laying_egg
- query.is_leashed
- query.is_levitating
- query.is_lingering
- query.is_local_player
- query.is_moving
- query.is_name_any
- query.is_on_fire
- query.is_on_ground
- query.is_on_screen
- query.is_onfire
- query.is_orphaned
- query.is_owner_identifier_any
- query.is_pack_setting_enabled
- query.is_pack_setting_selected
- query.is_persona_or_premium_skin
- query.is_playing_dead
- query.is_powered
- query.is_pregnant
- query.is_ram_attacking
- query.is_resting
- query.is_riding
- query.is_riding_any_entity_of_type
- query.is_rising
- query.is_roaring
- query.is_rolling
- query.is_saddled
- query.is_scared
- query.is_scenting
- query.is_searching
- query.is_selected_item
- query.is_shaking
- query.is_shaking_wetness
- query.is_sheared
- query.is_shield_powered
- query.is_silent
- query.is_sitting
- query.is_sleeping
- query.is_sneaking
- query.is_sneezing
- query.is_sniffing
- query.is_sonic_boom
- query.is_spectator
- query.is_sprinting
- query.is_stackable
- query.is_stalking
- query.is_standing
- query.is_stunned
- query.is_swimming
- query.is_tamed
- query.is_transforming
- query.is_using_item
- query.is_wall_climbing
- query.item_in_use_duration
- query.item_is_charged
- query.item_max_use_duration
- query.item_remaining_use_duration
- query.item_slot_to_bone_name

## K

- query.key_frame_lerp_time
- query.kinetic_weapon_damage_duration
- query.kinetic_weapon_delay
- query.kinetic_weapon_dismount_duration
- query.kinetic_weapon_knockback_duration

## L

- query.last_frame_time
- query.last_hit_by_player
- query.last_input_mode_is_any
- query.leashed_entity_count
- query.lie_amount
- query.life_span
- query.life_time
- query.lod_index
- query.log
- query.main_hand_item_max_duration
- query.main_hand_item_use_duration

## M

- query.mark_variant
- query.max_durability
- query.max_health
- query.max_trade_tier
- query.maximum_frame_time
- query.minimum_frame_time
- query.model_scale
- query.modified_distance_moved
- query.modified_move_speed
- query.modified_swing_duration
- query.moon_brightness
- query.moon_phase
- query.movement_direction

## N

- query.noise

## O

- query.on_fire_time
- query.out_of_control
- query.overlay_alpha
- query.owner_identifier

## P

- query.player_level
- query.position
- query.position_delta
- query.previous_squish_value
- query.property

## R

- query.relative_block_has_all_tags
- query.relative_block_has_any_tag
- query.remaining_durability
- query.ride_body_x_rotation
- query.ride_body_y_rotation
- query.ride_head_x_rotation
- query.ride_head_y_rotation
- query.rider_body_x_rotation
- query.rider_body_y_rotation
- query.rider_head_x_rotation
- query.rider_head_y_rotation
- query.roll_counter
- query.rotation_to_camera

## S

- query.scoreboard
- query.server_memory_tier
- query.shake_angle
- query.shake_time
- query.shield_blocking_bob
- query.show_bottom
- query.sit_amount
- query.skin_id
- query.sleep_rotation
- query.sneeze_counter
- query.spellcolor
- query.standing_scale
- query.state_time
- query.structural_integrity
- query.surface_particle_color
- query.surface_particle_texture_coordinate
- query.surface_particle_texture_size
- query.swell_amount
- query.swelling_dir
- query.swim_amount

## T

- query.tail_angle
- query.target_x_rotation
- query.target_y_rotation
- query.texture_frame_index
- query.ticks_since_last_kinetic_weapon_hit
- query.time_of_day
- query.time_since_last_vibration_detection
- query.time_stamp
- query.timer_flag_1
- query.timer_flag_2
- query.timer_flag_3
- query.total_emitter_count
- query.total_particle_count
- query.touch_only_affects_hotbar
- query.trade_tier

## U

- query.unhappy_counter

## V

- query.variant
- query.vertical_speed

## W

- query.walk_distance
- query.wing_flap_position
- query.wing_flap_speed

## Y

- query.yaw_speed

## 备注

**疑似噪声/截断项**（在源文件中作为独立 token 出现，可能是 ASCII 提取器把相邻字符串切断）：

- `query.heH` / `query.noH` / `query.prH` —— 形如 `xxxH` 后缀，疑似被某种 hex dump 切断；实际函数名可能是 `query.health`（已存在）/ `query.noise`（已存在）/ `query.property`（已存在）的邻近字符串。不计入有效函数。

**已确认的 DEPRECATED 标记**（schema doc 字符串显式标注）：

- `query.dash_cooldown_progress` —— schema doc：`DEPRECATED. DO NOT USE AFTER 1.20.40. Please see camel.entity.json script.pre_animation for example of how to now process dash cooldown.`

**参数特殊约束**（schema doc 提示）：

- `query.head_y_rotation` / `query.rider_head_y_rotation` / `query.ride_head_y_rotation`：对马/僵尸马/骷髅马/驴/骡需提供「角度 clamp 值」第二参数；对凋灵需提供 `[0..2]` 的 head-index。否则报错：`Error: passing incorrect number of parameters to query.head_y_rotation - horses, zombie horses, skeleton horses, donkeys and mules require a clamp value in degrees while withers require a head-index [0..2] - otherwise value must be 0`。

## 网易版 3.8.x 独有 query 函数（23 个）

以下函数在网易版 UnitTest.dll 中出现但国际版 1.26 minidump 中未命中：

**功能函数**：
- `query.day` / `query.daytime` / `query.gametime` — 时间相关
- `query.get_height_at` — 获取指定位置高度
- `query.get_nearby_entities` — 获取附近实体
- `query.get_neighborhood_is_biome` — 邻近方块是否为指定生物群系
- `query.get_relative_block_state` — 相对方块状态
- `query.is_biome` — 是否在指定生物群系
- `query.is_connect` — 网易版联网状态
- `query.is_cooldown_type` — 冷却类型
- `query.check_some_block_property` — 方块属性检查
- `query.get_equipped_item_full_name` — 装备物品全名
- `query.get_equipped_item_is_netease_shield` / `query.get_equipped_item_is_shield` — 网易版盾牌检测
- `query.allowed` / `query.also_allowed` / `query.also_disallowed` / `query.disallowed` — 权限相关
- `query.anim_pos` / `query.anim_speed` — 动画位置/速度
- `query.delta_time` / `query.function_name` / `query.mod` — 基础函数
- `query.get_name_test` — 名称测试（用于 `== 'rabbit'` 字符串比较）

**测试/调试函数**（仅 UnitTest.dll 存在，正式版应不可用）：
- `query.foo` / `query.moo` / `query.anim_test_1` / `query.experimental_test`
- `query.invalid_query_test` / `query.not_a_valid_function` / `query.sum_test` / `query.test_invalid_molang`
- `query.valid_always` / `query.valid_early` / `query.valid_late` / `query.valid_mid`
- `query.m_begin` / `query.m_disjoint` / `query.m_end` / `query.m_freq` / `query.m_ptr` — 内部数学/迭代器

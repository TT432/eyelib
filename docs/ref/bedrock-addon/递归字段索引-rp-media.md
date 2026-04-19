# Bedrock Add-On 递归字段索引：RP 媒体 / 资源族

## 1. 覆盖范围

本页覆盖：

- `particles`
- `sounds.json` / `sound_definitions.json`
- `materials`
- `textures/item_texture.json` / `textures/terrain_texture.json`
- `minecraft:fog_settings`
- `ui` JSON 资源

## 2. `particle_effect`

- `NON_TERMINAL` `particle_effect`
  - `NON_TERMINAL` `description`
    - `TERMINAL` `identifier`
    - `NON_TERMINAL` `basic_render_parameters`
      - `TERMINAL` `material`
      - `TERMINAL` `texture`
  - `NON_TERMINAL` `components`
    - `NON_TERMINAL` emitter components
      - `NON_TERMINAL` `minecraft:emitter_local_space`
      - `NON_TERMINAL` `minecraft:emitter_initialization`
      - `NON_TERMINAL` `minecraft:emitter_rate_instant`
      - `NON_TERMINAL` `minecraft:emitter_rate_steady`
      - `NON_TERMINAL` `minecraft:emitter_rate_manual`
      - `NON_TERMINAL` `minecraft:emitter_lifetime_looping`
      - `NON_TERMINAL` `minecraft:emitter_lifetime_once`
      - `NON_TERMINAL` `minecraft:emitter_lifetime_expression`
      - `NON_TERMINAL` `minecraft:emitter_lifetime_events`
      - `NON_TERMINAL` `minecraft:emitter_shape_point`
      - `NON_TERMINAL` `minecraft:emitter_shape_sphere`
      - `NON_TERMINAL` `minecraft:emitter_shape_box`
      - `NON_TERMINAL` `minecraft:emitter_shape_custom`
      - `NON_TERMINAL` `minecraft:emitter_shape_entity-aabb`
      - `NON_TERMINAL` `minecraft:emitter_disc`
    - `NON_TERMINAL` particle components
      - `NON_TERMINAL` `minecraft:particle_initial_speed`
      - `NON_TERMINAL` `minecraft:particle_initial_spin`
      - `NON_TERMINAL` `minecraft:particle_motion_dynamic`
      - `NON_TERMINAL` `minecraft:particle_motion_parametric`
      - `NON_TERMINAL` `minecraft:particle_motion_collision`
      - `NON_TERMINAL` `minecraft:particle_appearance_billboard`
      - `NON_TERMINAL` `minecraft:particle_appearance_tinting`
      - `NON_TERMINAL` `minecraft:particle_appearance_lighting`
      - `NON_TERMINAL` `minecraft:particle_lifetime_expression`
      - `NON_TERMINAL` `minecraft:particle_lifetime_events`
      - `NON_TERMINAL` `minecraft:particle_expire_if_in_blocks`
      - `NON_TERMINAL` `minecraft:particle_expire_if_not_in_blocks`
      - `NON_TERMINAL` `minecraft:particle_lifetime_kill-plane`
  - `NON_TERMINAL` `curves`
    - `TERMINAL` named curve definition（自由 map，文档只给概念与样例）
  - `NON_TERMINAL` `events`
    - `TERMINAL` `sequence`
    - `TERMINAL` `randomize`
    - `TERMINAL` `particle_effect.effect`
    - `TERMINAL` `particle_effect.type`
    - `TERMINAL` `particle_effect.pre_effect_expression`
    - `TERMINAL` `sound_effect.event_name`
    - `TERMINAL` `expression`
    - `TERMINAL` `log`

来源：O7-PARTICLE-INTRO、O7-PARTICLE-DOC、R7

> 注意：粒子 component 名称在这一页只被当成 registry frontier，不当成已经递归到底的原子字段。

## 3. `sounds.json`

- `NON_TERMINAL` `sounds.json`
  - `NON_TERMINAL` `individual_named_sounds`
    - `NON_TERMINAL` `sounds` map
      - `NON_TERMINAL` named sound entry
        - `TERMINAL` `pitch`
        - `TERMINAL` `sound`
        - `TERMINAL` `volume`
  - `NON_TERMINAL` `individual_event_sounds`
    - `NON_TERMINAL` `events` map
      - `NON_TERMINAL` event entry
        - `TERMINAL` `pitch`
        - `TERMINAL` `sound`
        - `TERMINAL` `volume`

来源：O7-SOUND-INTRO、R7

> 注意：这一节写的是**官方文档/官方样例视角**下的 `sounds.json` 结构；`递归字段索引-importer-support.md` 里的 `entity_sounds` / `block_sounds` / `interactive_block_sounds` / `individual_event_sounds` 是 **Eyelib parser 分区视角**，两者不能直接当成同一层树。

## 4. `sound_definitions.json`

- `NON_TERMINAL` `sound_definitions.json`
  - `TERMINAL` `format_version`
  - `NON_TERMINAL` `sound_definitions`
    - `NON_TERMINAL` sound definition entry
      - `TERMINAL` `__use_legacy_max_distance`
      - `TERMINAL` `category`
      - `TERMINAL` `max_distance`
      - `TERMINAL` `min_distance`
      - `NON_TERMINAL` `sounds`
        - `TERMINAL` string item
        - `NON_TERMINAL` object item
          - `TERMINAL` `name`
          - `TERMINAL` `volume`
          - `TERMINAL` `load_on_low_memory`

来源：O7-SOUND-ADD、R7

## 5. `materials`

- `NON_TERMINAL` `materials` root object
  - `TERMINAL` `version`
  - `NON_TERMINAL` material entry `child:parent`
    - `TERMINAL` `+defines`
    - `TERMINAL` `-defines`
    - `TERMINAL` `+states`
    - `TERMINAL` `-states`
    - `TERMINAL` `blendSrc`
    - `TERMINAL` `blendDst`
    - `TERMINAL` `alphaSrc`
    - `TERMINAL` `alphaDst`

来源：O7-MATERIALS

## 6. `textures/item_texture.json` / `textures/terrain_texture.json`

- `NON_TERMINAL` `textures/item_texture.json`
  - `TERMINAL` `resource_pack_name`
  - `TERMINAL` `texture_name`
  - `NON_TERMINAL` `texture_data`
    - `NON_TERMINAL` alias entry
      - `TERMINAL` `textures`

- `NON_TERMINAL` `textures/terrain_texture.json`
  - `TERMINAL` `resource_pack_name`
  - `TERMINAL` `texture_name`
  - `TERMINAL` `padding`
  - `TERMINAL` `num_mip_levels`
  - `NON_TERMINAL` `texture_data`
    - `NON_TERMINAL` alias entry
      - `TERMINAL` `textures`

来源：O8-ITEMS、O8-BLOCKS、R7

## 7. `minecraft:fog_settings`

- `NON_TERMINAL` `minecraft:fog_settings`
  - `NON_TERMINAL` `description`
    - `TERMINAL` `identifier`
  - `NON_TERMINAL` `distance`
    - `NON_TERMINAL` `air`
      - `TERMINAL` `fog_start`
      - `TERMINAL` `fog_end`
      - `TERMINAL` `fog_color`
      - `TERMINAL` `render_distance_type`
    - `NON_TERMINAL` `weather`
      - `TERMINAL` `fog_start`
      - `TERMINAL` `fog_end`
      - `TERMINAL` `fog_color`
      - `TERMINAL` `render_distance_type`
    - `NON_TERMINAL` `water`
      - `TERMINAL` `fog_start`
      - `TERMINAL` `fog_end`
      - `TERMINAL` `fog_color`
      - `TERMINAL` `render_distance_type`
      - `TERMINAL` `transition_fog`
      - `TERMINAL` `init_fog`
      - `TERMINAL` `min_percent`
    - `NON_TERMINAL` `lava`
      - `TERMINAL` `fog_start`
      - `TERMINAL` `fog_end`
      - `TERMINAL` `fog_color`
      - `TERMINAL` `render_distance_type`
    - `NON_TERMINAL` `powder_snow`
      - `TERMINAL` `fog_start`
      - `TERMINAL` `fog_end`
      - `TERMINAL` `fog_color`
      - `TERMINAL` `render_distance_type`
  - `NON_TERMINAL` `volumetric`
    - `NON_TERMINAL` `density`
      - `NON_TERMINAL` `air`
        - `TERMINAL` `max_density`
        - `TERMINAL` `max_density_height`
        - `TERMINAL` `zero_density_height`
        - `TERMINAL` `uniform`
      - `NON_TERMINAL` `water`
        - `TERMINAL` `max_density`
        - `TERMINAL` `max_density_height`
        - `TERMINAL` `zero_density_height`
        - `TERMINAL` `uniform`
    - `NON_TERMINAL` `media_coefficients`
      - `NON_TERMINAL` `air`
        - `TERMINAL` `scattering`
        - `TERMINAL` `absorption`
      - `NON_TERMINAL` `water`
        - `TERMINAL` `scattering`
        - `TERMINAL` `absorption`
      - `NON_TERMINAL` `cloud`
        - `TERMINAL` `scattering`
        - `TERMINAL` `absorption`

来源：S5、R7

## 8. UI JSON 资源

- `NON_TERMINAL` JSON UI screen file
  - `NON_TERMINAL` `ui_element` root
    - `NON_TERMINAL` `controls`
    - `NON_TERMINAL` `bindings`
    - `NON_TERMINAL` `variables`
    - `TERMINAL` `texture`
    - `TERMINAL` `text`
    - `TERMINAL` `type`
    - `TERMINAL` `size`
    - `TERMINAL` `offset`
    - `TERMINAL` `anchor_from`
    - `TERMINAL` `anchor_to`
    - `TERMINAL` `visible`

来源：S6、R7

> 注意：`controls`、`bindings`、`variables` 仍是开放 registry / 子树前沿，当前页只把它们定位到下一层入口。

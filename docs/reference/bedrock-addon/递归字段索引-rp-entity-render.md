# Bedrock Add-On 递归字段索引：RP 实体 / 渲染 / 动画

## 1. 说明

本页覆盖 Resource Pack 里最核心的实体可视化链：

- `client_entity`
- `attachables`
- `render_controllers`
- `animation_controllers`
- `animations`
- `geometry/models`

写法同样使用：

- `NON_TERMINAL`：继续拆
- `TERMINAL`：已到原子字段

## 2. `minecraft:client_entity`

- `TERMINAL` `format_version`
- `NON_TERMINAL` `minecraft:client_entity`
  - `NON_TERMINAL` `description`
    - `TERMINAL` `identifier`
    - `TERMINAL` `min_engine_version`
    - `NON_TERMINAL` `materials`
      - `TERMINAL` material alias
      - `TERMINAL` material ref
    - `NON_TERMINAL` `textures`
      - `TERMINAL` texture alias
      - `TERMINAL` texture ref/path
    - `NON_TERMINAL` `geometry`
      - `TERMINAL` geometry alias
      - `TERMINAL` geometry ref
    - `NON_TERMINAL` `animations`
      - `TERMINAL` animation alias
      - `TERMINAL` animation ref
    - `NON_TERMINAL` `animation_controllers`
      - `TERMINAL` short alias
      - `TERMINAL` controller ref
    - `NON_TERMINAL` `scripts`
      - `TERMINAL` `pre_animation`
      - `NON_TERMINAL` `animate`
        - `TERMINAL` bare animation/controller 名
        - `NON_TERMINAL` conditional map entry
          - `TERMINAL` short name
          - `TERMINAL` Molang condition / blend weight
      - `TERMINAL` `scale`
      - `TERMINAL` `initialize`（仅 S4 schema 证据；缺少稳定 prose/reference 页逐字段说明）
      - `TERMINAL` `variables.*`（仅 S4 schema 证据；缺少稳定 prose/reference 页逐字段说明）
    - `NON_TERMINAL` `particle_effects`
      - `TERMINAL` effect alias
      - `TERMINAL` particle ref
    - `NON_TERMINAL` `sound_effects`
      - `TERMINAL` sound alias
      - `TERMINAL` sound ref
    - `NON_TERMINAL` `render_controllers`
      - `NON_TERMINAL` controller entry
        - `TERMINAL` controller id
        - `TERMINAL` conditional entry
    - `NON_TERMINAL` `locators`
      - `TERMINAL` locator name
      - `TERMINAL` target bone
      - `TERMINAL` offset vector
    - `TERMINAL` `enable_attachables`
    - `TERMINAL` `held_item_ignores_lighting`
    - `TERMINAL` `hide_armor`
    - `NON_TERMINAL` `spawn_egg`
      - `TERMINAL` `base_color`
      - `TERMINAL` `overlay_color`
      - `TERMINAL` `texture`
      - `TERMINAL` `texture_index`

来源：O7-CEINTRO、O8-ANIM-OV、R7、S4

## 3. `minecraft:attachable`

- `TERMINAL` `format_version`
- `NON_TERMINAL` `minecraft:attachable`
  - `NON_TERMINAL` `description`
    - `TERMINAL` `identifier`
    - `NON_TERMINAL` `item`
      - `TERMINAL` item id
      - `TERMINAL` Molang condition
    - `NON_TERMINAL` `materials` / `textures` / `geometry` / `animations`
      - `TERMINAL` alias
      - `TERMINAL` ref
    - `NON_TERMINAL` `scripts.animate`
      - `TERMINAL` bare animation 名
      - `NON_TERMINAL` conditional map entry
        - `TERMINAL` short name
        - `TERMINAL` condition / value
    - `NON_TERMINAL` `render_controllers`
      - `TERMINAL` controller id

来源：O8-ATTACH、R7、S4

## 4. `render_controllers`

- `TERMINAL` `format_version`
- `NON_TERMINAL` `render_controllers`
  - `NON_TERMINAL` `controller.render.*`
    - `NON_TERMINAL` `arrays`
      - `NON_TERMINAL` `geometries`
        - `TERMINAL` array name
        - `TERMINAL` geometry ref item
      - `NON_TERMINAL` `materials`
        - `TERMINAL` array name
        - `TERMINAL` material ref item
      - `NON_TERMINAL` `textures`
        - `TERMINAL` array name
        - `TERMINAL` texture ref item
    - `TERMINAL` `geometry`
    - `NON_TERMINAL` `materials`
      - `TERMINAL` part pattern
      - `TERMINAL` material expression/ref
    - `NON_TERMINAL` `textures`
      - `TERMINAL` texture expression/ref
    - `NON_TERMINAL` `part_visibility`
      - `TERMINAL` part pattern
      - `TERMINAL` visibility expression / bool
    - `NON_TERMINAL` `color`
      - `TERMINAL` `r` / `g` / `b` / `a`
    - `NON_TERMINAL` `overlay_color`
      - `TERMINAL` `r` / `g` / `b` / `a`
    - `NON_TERMINAL` `is_hurt_color`
      - `TERMINAL` `r` / `g` / `b` / `a`
    - `NON_TERMINAL` `on_fire_color`
      - `TERMINAL` `r` / `g` / `b` / `a`
    - `NON_TERMINAL` `uv_anim`
      - `TERMINAL` `offset.x`
      - `TERMINAL` `offset.y`
      - `TERMINAL` `scale.x`
      - `TERMINAL` `scale.y`
    - `TERMINAL` `filter_lighting`
    - `TERMINAL` `ignore_lighting`
    - `TERMINAL` `light_color_multiplier`
    - `TERMINAL` `rebuild_animation_matrices`

来源：O7-RCREF、O7-RCTUT、R7、S4

## 5. `animation_controllers`

- `TERMINAL` `format_version`
- `NON_TERMINAL` `animation_controllers`
  - `NON_TERMINAL` `controller.animation.*`
    - `TERMINAL` `initial_state`
    - `NON_TERMINAL` `states`
      - `TERMINAL` state name
      - `NON_TERMINAL` state object
        - `NON_TERMINAL` `animations`
          - `TERMINAL` bare animation name
          - `NON_TERMINAL` conditional blend item
            - `TERMINAL` animation name
            - `TERMINAL` blend weight / Molang condition
        - `NON_TERMINAL` `transitions`
          - `TERMINAL` target state
          - `TERMINAL` Molang condition
        - `NON_TERMINAL` `variables`
          - `TERMINAL` variable name
          - `TERMINAL` `input`
          - `NON_TERMINAL` `remap_curve`
            - `TERMINAL` curve key
            - `TERMINAL` curve value
        - `NON_TERMINAL` `blend_transition`
          - `TERMINAL` numeric shorthand
          - `NON_TERMINAL` curve-map form
        - `TERMINAL` `blend_via_shortest_path`
        - `NON_TERMINAL` `particle_effects`
          - `TERMINAL` `effect`
          - `TERMINAL` `locator`
          - `TERMINAL` `pre_effect_script`
          - `TERMINAL` `bind_to_actor`
        - `NON_TERMINAL` `sound_effects`
          - `TERMINAL` `effect`
          - `TERMINAL` `locator`
        - `TERMINAL` `on_entry`
        - `TERMINAL` `on_exit`

来源：O8-ANIMCTRL-REF、O8-ANIMCTRL-INTRO、R7、R8、S4

## 6. `animations`

- `TERMINAL` `format_version`
- `NON_TERMINAL` `animations`
  - `NON_TERMINAL` `animation.*`
    - `TERMINAL` `anim_time_update`
    - `TERMINAL` `animation_length`
    - `TERMINAL` `blend_weight`
    - `TERMINAL` `loop`
    - `TERMINAL` `loop_delay`
    - `TERMINAL` `override_previous_animation`
    - `TERMINAL` `start_delay`
    - `TERMINAL` `relative_to.rotation`
    - `NON_TERMINAL` `bones`
      - `TERMINAL` bone name
      - `NON_TERMINAL` bone transform
        - `NON_TERMINAL` `position`
          - `TERMINAL` static vector
          - `NON_TERMINAL` keyed timeline
            - `TERMINAL` time key
            - `TERMINAL` value vector / Molang
            - `TERMINAL` `pre`
            - `TERMINAL` `post`
            - `TERMINAL` `lerp_mode`
        - `NON_TERMINAL` `rotation`（同上）
        - `NON_TERMINAL` `scale`（同上）
    - `NON_TERMINAL` `timeline`
      - `TERMINAL` time key
      - `TERMINAL` command / Molang statement
    - `NON_TERMINAL` `sound_effects`
      - `TERMINAL` time key
      - `TERMINAL` `effect`
      - `TERMINAL` `locator`
    - `NON_TERMINAL` `particle_effects`
      - `TERMINAL` time key
      - `TERMINAL` `effect`
      - `TERMINAL` `locator`
      - `TERMINAL` `pre_effect_script`
      - `TERMINAL` `bind_to_actor`

来源：O8-ANIM-OV、O8-ENTITYMODEL、R7、S4

## 7. `minecraft:geometry`

- `TERMINAL` `format_version`
- `NON_TERMINAL` `minecraft:geometry`
  - `NON_TERMINAL` model item
    - `NON_TERMINAL` `description`
      - `TERMINAL` `identifier`
      - `TERMINAL` `texture_width`
      - `TERMINAL` `texture_height`
      - `TERMINAL` `visible_bounds_width`
      - `TERMINAL` `visible_bounds_height`
      - `TERMINAL` `visible_bounds_offset`
    - `NON_TERMINAL` `bones`
      - `TERMINAL` `name`
      - `TERMINAL` `parent`
      - `TERMINAL` `pivot`
      - `TERMINAL` `rotation`
      - `TERMINAL` `mirror`
      - `TERMINAL` `binding`
      - `TERMINAL` `inflate`
      - `TERMINAL` `debug`
      - `TERMINAL` `render_group_id`
      - `NON_TERMINAL` `locators`
        - `TERMINAL` locator name
        - `TERMINAL` simple vector form
        - `NON_TERMINAL` object form
          - `TERMINAL` `offset`
          - `TERMINAL` `rotation`
          - `TERMINAL` `ignore_inherited_scale`
      - `NON_TERMINAL` `cubes`
        - `NON_TERMINAL` cube item
          - `TERMINAL` `origin`
          - `TERMINAL` `size`
          - `NON_TERMINAL` `uv`
            - `TERMINAL` simple `[x,y]`
            - `TERMINAL` face map
            - `TERMINAL` `uv`
            - `TERMINAL` `uv_size`
            - `TERMINAL` `material_instance`
            - `TERMINAL` `uv_rotation`
          - `TERMINAL` `pivot`
          - `TERMINAL` `rotation`
          - `TERMINAL` `inflate`
          - `TERMINAL` `mirror`
          - `TERMINAL` `reset`
      - `NON_TERMINAL` `texture_meshes`
        - `TERMINAL` `local_pivot`
        - `TERMINAL` `position`
        - `TERMINAL` `rotation`
        - `TERMINAL` `scale`
        - `TERMINAL` `texture`
        - `TERMINAL` `use_pixel_depth`
      - `NON_TERMINAL` `poly_mesh`
        - `TERMINAL` `normalized_uvs`
        - `TERMINAL` `polys`
        - `TERMINAL` `positions`
        - `TERMINAL` `normals`
        - `TERMINAL` `uvs`

来源：O8-ENTITYMODEL、O8-ATTACH、R7、S4

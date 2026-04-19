# Bedrock Add-On 递归字段索引：Eyelib importer 支持面

## 1. 说明

本页不是官方规范页，而是把 **Eyelib 当前 importer 的递归支持面**单独列出来。

- `typed`：已有明确 codec / parser
- `partial`：只建了部分语义，深层内容仍保留 raw tree
- `raw`：基本只包裹原始结构
- `unmanaged`：当前 importer scope 外或尚无 typed schema

## 2. typed 主干

- `typed` manifest
  - `format_version`
  - `header`
    - `header.version`：三元数组 / semver 字符串
    - `header.min_engine_version`：三元数组 / semver 字符串
  - `modules`
    - `modules[*].description`
    - `modules[*].language`
    - `modules[*].entry`
    - `modules[*].version`：三元数组 / semver 字符串
  - `dependencies`
    - `dependencies[*].uuid`
    - `dependencies[*].module_name`
    - `dependencies[*].version`：三元数组 / semver 字符串
  - `metadata`
  - `capabilities`
  - `settings`（列表项仍是 raw object）
  - 其余 manifest 顶层字段保留在 `extraFields`
  - `metadata` 未显式建模的子字段保留在 `metadata.extraFields`
- `typed` `BrClientEntity`
  - `identifier`
  - `min_engine_version`
  - `materials`
  - `textures`
  - `geometry`
  - `animations`
  - `animation_controllers`
  - `particle_effects`
  - `sound_effects`
  - `render_controllers`
  - `scripts`
  - `spawn_egg`
- `typed` animations
- `typed` animation controllers
- `typed` render controllers
- `typed` geometry models (`.geo.json` / `.bbmodel`)
- `typed` materials
- `typed` localization
- `typed` binary `sounds/**` assets（作为 `soundFiles` 聚合）

## 3. partial / raw 主干

- `partial` behavior entities
  - 已建：`format_version`、`identifier`、`description`、`component_groups`、`components`、`events`
  - 但深层 payload 仍保留 raw object
- `partial` particles
  - 已建：`description.basic_render_parameters`、`curves`
  - `events` 当前只保留占位语义节点，未保留事件 payload
  - `components` 仍是 raw `BedrockResourceValue`
- `partial` sounds
  - `sounds.json`：已分 `entity_sounds` / `block_sounds` / `interactive_block_sounds` / `individual_event_sounds`
  - 但内部树仍是 raw object
- `partial` sound definitions
  - 已建 `format_version` 与 `sound_definitions`
  - 但 definition body 仍是 raw object
- `raw` texture metadata
  - `textures/*.json` 当前按 texture metadata 家族保留 raw object wrapper
- `raw` texture indices family
  - `textures/item_texture.json`
  - `textures/terrain_texture.json`
  - `textures/flipbook_textures.json`
  - `textures/texture_list.json`
  - `blocks.json`
  - `biomes_client.json`
  - 当前按 family 进入 managed raw wrapper，而不是落到 unmanaged

## 4. unmanaged / no typed schema yet

- `unmanaged` 且 `OUTSIDE_IMPORTER_SCOPE`
  - `item`
  - `block`
  - `recipe`
  - `loot table`
  - `spawn rule`
  - `trading`
  - `feature`
  - `feature rule`
  - `structure`
  - `script`
  - `biome`
- `unmanaged` 且 `NO_TYPED_SCHEMA_YET`
  - `ui`
  - `fog`
  - `unknown_json`
  - `unknown_text`
  - `unknown_binary`

## 5. 边界

1. importer **不支持** ≠ Bedrock **不合法**。
2. 本页只能说明 Eyelib 当前“认识到哪一层”。
3. 字段合法性仍应回到 `来源清单.md` 的 `S/O/R/C` 体系判断。
4. importer 的 manifest 支持面要分清两层：
   - 比 `递归字段索引-manifest.md` 的“官方字段树”更窄，因为很多 preview/world-template/script 细节并未显式建模；
   - 又比旧版摘要更宽，因为 `format_version`、`capabilities`、`settings`、`module.description`、`soundFiles`、`languageFiles` 都已经被实际保留或解析。

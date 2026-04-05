# Blockbench Bedrock Geometry Export Fields Reference

## Scope
- This document records the Bedrock geometry JSON fields that Blockbench exports.
- It covers both the modern exporter in `../blockbench/js/io/formats/bedrock.js` and the legacy exporter in `../blockbench/js/io/formats/bedrock_old.js`.
- It is intended as a debugging reference for comparing Eyelib's Bedrock importer against Blockbench's export-side schema.

## Source Files
- Modern exporter: `../blockbench/js/io/formats/bedrock.js`
- Legacy exporter: `../blockbench/js/io/formats/bedrock_old.js`
- Format registration: `../blockbench/js/main_formats.ts`

## Format Variants

| Variant | Primary file | Top-level wrapper | UV encoding | Notes |
|---|---|---|---|---|
| Modern Bedrock geometry | `../blockbench/js/io/formats/bedrock.js` | `format_version` + `minecraft:geometry` | box UV and per-face UV | Main current export path |
| Legacy Bedrock geometry | `../blockbench/js/io/formats/bedrock_old.js` | `format_version` + `geometry.<name>` | box UV only | Compatibility export path |

## Version Selection Summary
- Modern exporter default: `1.12.0`
- Raised to `1.16.0` when bone `binding` is exported
- Raised to `1.21.0` when per-face `uv_rotation` is exported
- Raised to `1.21.20` when `item_display_transforms` is exported
- Legacy exporter is fixed at `1.10.0`

## Field Inventory

The tables below describe the fields that Blockbench's exporters actually emit.

## Constraint Notes
- This document records **exporter-enforced** constraints, not the full Bedrock schema.
- When Blockbench copies editor state directly into JSON without clamping, the range is documented as **unbounded number in exporter code**.
- Array arity is exact where the exporter writes literal arrays such as `[x, y, z]` or `[u, v]`.
- Modern exporter evidence comes from `../blockbench/js/io/formats/bedrock.js` and `../blockbench/js/display_mode/display_mode.js`.
- Legacy exporter evidence comes from `../blockbench/js/io/formats/bedrock_old.js`.

## Modern Exporter Field Tables

### Top Level And Description

| Path | Type | Shape | Value / range | Emission | Source |
|---|---|---|---|---|---|
| `format_version` | string | scalar | One of `1.12.0`, `1.16.0`, `1.21.0`, `1.21.20` | Always | `bedrock.js#getFormatVersion` |
| `minecraft:geometry` | array<object> | length `1` in current compile output | Always `[entitymodel]` | Always | `bedrock.js#compile` |
| `minecraft:geometry[0].description` | object | fixed object | Contains identifier, texture size, and optional visible bounds | Always | `bedrock.js#compile` |
| `...description.identifier` | string | scalar | `geometry.` + model identifier, geometry name, or `unknown` | Always | `bedrock.js#compile` |
| `...description.texture_width` | number | scalar | `Project.texture_width` or fallback `16`; exporter code does not clamp | Always | `bedrock.js#compile` |
| `...description.texture_height` | number | scalar | `Project.texture_height` or fallback `16`; exporter code does not clamp | Always | `bedrock.js#compile` |
| `...description.visible_bounds_width` | number | scalar | Derived from `calculateVisibleBox()[0]`; exporter falls back to `0` | Conditional | `bedrock.js#compile` |
| `...description.visible_bounds_height` | number | scalar | Derived from `calculateVisibleBox()[1]`; exporter falls back to `0` | Conditional | `bedrock.js#compile` |
| `...description.visible_bounds_offset` | array<number> | exactly `3` | Always `[0, visibleBoxY, 0]` when emitted | Conditional | `bedrock.js#compile` |
| `minecraft:geometry[0].bones` | array<object> | `0..N` | Bone list from `compileGroup`, plus optional catch bone for loose elements | Conditional | `bedrock.js#compile` |
| `minecraft:geometry[0].item_display_transforms` | object | slot-name map | Keys come from `DisplayMode.slots`; emitted only when at least one slot exports | Conditional | `bedrock.js#compile` |

### Bone Objects

| Path | Type | Shape | Value / range | Emission | Source |
|---|---|---|---|---|---|
| `...bones[i].name` | string | scalar | Group name | Always | `bedrock.js#compileGroup` |
| `...bones[i].parent` | string | scalar | Parent group name | Conditional | `bedrock.js#compileGroup` |
| `...bones[i].pivot` | array<number> | exactly `3` | Model-space pivot; X is sign-flipped before export | Always | `bedrock.js#compileGroup` |
| `...bones[i].rotation` | array<number> | exactly `3` | Degrees; X/Y sign-flipped, Z preserved; no clamp in exporter | Conditional | `bedrock.js#compileGroup` |
| `...bones[i].binding` | string | scalar | Copied from `g.bedrock_binding` | Conditional | `bedrock.js#compileGroup` |
| `...bones[i].reset` | boolean | scalar | Exporter only writes `true` | Conditional | `bedrock.js#compileGroup` |
| `...bones[i].mirror` | boolean | scalar | Bone-level exporter only writes `true` and only in box-UV mode | Conditional | `bedrock.js#compileGroup` |
| `...bones[i].material` | string | scalar | Copied from `g.material` | Conditional | `bedrock.js#compileGroup` |
| `...bones[i].cubes` | array<object> | `1..N` when present | Cube list for exported cube children | Conditional | `bedrock.js#compileGroup` |
| `...bones[i].locators` | object | locator-name map | Values are either vec3 arrays or locator objects | Conditional | `bedrock.js#compileGroup` |
| `...bones[i].texture_meshes` | array<object> | `1..N` when present | Texture mesh list | Conditional | `bedrock.js#compileGroup` |

### Cube Objects

| Path | Type | Shape | Value / range | Emission | Source |
|---|---|---|---|---|---|
| `...cubes[j].origin` | array<number> | exactly `3` | Model units; X exported as `-(x + sizeX)` | Always | `bedrock.js#compileCube` |
| `...cubes[j].size` | array<number> | exactly `3` | Model units; copied from `cube.size()` | Always | `bedrock.js#compileCube` |
| `...cubes[j].inflate` | number | scalar | Model units; omitted when falsy/zero | Conditional | `bedrock.js#compileCube` |
| `...cubes[j].pivot` | array<number> | exactly `3` | Present only for rotated cubes; X sign-flipped | Conditional | `bedrock.js#compileCube` |
| `...cubes[j].rotation` | array<number> | exactly `3` | Degrees; X/Y sign-flipped, Z preserved; no clamp in exporter | Conditional | `bedrock.js#compileCube` |
| `...cubes[j].mirror` | boolean | scalar | Boolean override relative to `bone.mirror`; can be `true` or `false` when emitted | Conditional | `bedrock.js#compileCube` |
| `...cubes[j].uv` (box mode) | array<number> | exactly `2` | Texel UV origin `[u, v]`; copied from `cube.uv_offset` | Conditional | `bedrock.js#compileCube` |
| `...cubes[j].uv` (per-face mode) | object | face-name map | Face keys iterate from `cube.faces`; emitted only for faces with non-null texture | Conditional | `bedrock.js#compileCube` |

### Per-Face UV Objects

| Path | Type | Shape | Value / range | Emission | Source |
|---|---|---|---|---|---|
| `...uv.<face>` | object | face entry | Face keys come from `cube.faces`; in practice Bedrock cube faces are `north`, `east`, `south`, `west`, `up`, `down` | Conditional | `bedrock.js#compileCube` |
| `...uv.<face>.uv` | array<number> | exactly `2` | Texel-space origin `[u, v]` | Always for exported face | `bedrock.js#compileCube` |
| `...uv.<face>.uv_size` | array<number> | exactly `2` | Texel-space size `[du, dv]`; `up`/`down` are rewritten to negative sizes with shifted origin | Always for exported face | `bedrock.js#compileCube` |
| `...uv.<face>.uv_rotation` | number | scalar | Copied from `face.rotation`; exporter does not clamp here | Conditional | `bedrock.js#compileCube` |
| `...uv.<face>.material_instance` | string | scalar | Copied from `face.material_name` | Conditional | `bedrock.js#compileCube` |

### Locators

| Path | Type | Shape | Value / range | Emission | Source |
|---|---|---|---|---|---|
| `...locators` | object | locator-name map | Empty maps are omitted | Conditional | `bedrock.js#compileGroup` |
| `...locators.<name>` (simple form) | array<number> | exactly `3` | Position vec3 with X sign-flipped | Conditional | `bedrock.js#compileGroup` |
| `...locators.<name>.offset` | array<number> | exactly `3` | Position vec3 with X sign-flipped | Conditional | `bedrock.js#compileGroup` |
| `...locators.<name>.rotation` | array<number> | exactly `3` | Degrees; `[-x, -y, z]`; only for rotatable locators in object form | Conditional | `bedrock.js#compileGroup` |
| `...locators.<name>.ignore_inherited_scale` | boolean | scalar | Exporter only writes `true` | Conditional | `bedrock.js#compileGroup` |
| Locator key naming | string key | scalar | `NullObject` keys are prefixed with `_null_`; other keys use object name | Conditional | `bedrock.js#compileGroup` |

### Texture Meshes

| Path | Type | Shape | Value / range | Emission | Source |
|---|---|---|---|---|---|
| `...texture_meshes[k].texture` | string | scalar | Texture identifier/name from `texture_name` | Always for exported texture mesh | `bedrock.js#compileGroup` |
| `...texture_meshes[k].position` | array<number> | exactly `3` | Model units; X sign-flipped; Y offset by bone pivot and then sign-flipped | Always for exported texture mesh | `bedrock.js#compileGroup` |
| `...texture_meshes[k].rotation` | array<number> | exactly `3` | Degrees; `[-x, -y, z]`; omitted when all zero | Conditional | `bedrock.js#compileGroup` |
| `...texture_meshes[k].local_pivot` | array<number> | exactly `3` | Model units; Z sign-flipped; omitted when all zero | Conditional | `bedrock.js#compileGroup` |
| `...texture_meshes[k].scale` | array<number> | exactly `3` | Scale multipliers; omitted when all ones | Conditional | `bedrock.js#compileGroup` |

### Item Display Transforms

`item_display_transforms` iterates `DisplayMode.slots`, which includes:
`thirdperson_righthand`, `thirdperson_lefthand`, `firstperson_righthand`, `firstperson_lefthand`, `ground`, `gui`, `head`, `fixed`, and `on_shelf`.

| Path | Type | Shape | Value / range | Emission | Source |
|---|---|---|---|---|---|
| `...item_display_transforms.<slot>` | object | slot object | Exported only when the slot has any non-default transform data | Conditional | `bedrock.js#compile`, `display_mode.js#DisplaySlot.exportBedrock` |
| `...<slot>.rotation` | array<number> | exactly `3` | Rotation values copied directly from slot state | Always within exported slot | `DisplaySlot.exportBedrock` |
| `...<slot>.translation` | array<number> | exactly `3` | Translation values copied directly from slot state | Always within exported slot | `DisplaySlot.exportBedrock` |
| `...<slot>.scale` | array<number> | exactly `3` | Scale multipliers copied from slot state and sign-adjusted by mirror flags | Always within exported slot | `DisplaySlot.exportBedrock` |
| `...<slot>.rotation_pivot` | array<number> | exactly `3` | Pivot vec3 copied directly from slot state | Always within exported slot | `DisplaySlot.exportBedrock` |
| `...<slot>.scale_pivot` | array<number> | exactly `3` | Pivot vec3 copied directly from slot state | Always within exported slot | `DisplaySlot.exportBedrock` |
| `...gui.fit_to_frame` | boolean | scalar | Present only on the `gui` slot | Conditional | `DisplaySlot.exportBedrock` |

## Legacy Exporter Field Tables

### Top Level And Geometry Object

| Path | Type | Shape | Value / range | Emission | Source |
|---|---|---|---|---|---|
| `format_version` | string | scalar | Fixed to `1.10.0` in wrapped output | Always in wrapped output | `bedrock_old.js#compile` |
| `geometry.<name>` | object | single dynamic key | Key is `geometry.` + geometry name, project name, or `unknown` | Always in wrapped output | `bedrock_old.js#compile` |
| `geometry.<name>.texturewidth` | number | scalar | Texel width from `Project.texture_width`; exporter does not clamp | Always | `bedrock_old.js#compile` |
| `geometry.<name>.textureheight` | number | scalar | Texel height from `Project.texture_height`; exporter does not clamp | Always | `bedrock_old.js#compile` |
| `geometry.<name>.visible_bounds_width` | number | scalar | Derived from `calculateVisibleBox()[0]`; exporter falls back to `0` | Conditional | `bedrock_old.js#compile` |
| `geometry.<name>.visible_bounds_height` | number | scalar | Derived from `calculateVisibleBox()[1]`; exporter falls back to `0` | Conditional | `bedrock_old.js#compile` |
| `geometry.<name>.visible_bounds_offset` | array<number> | exactly `3` | Always `[0, visibleBoxY, 0]` when emitted | Conditional | `bedrock_old.js#compile` |
| `geometry.<name>.bones` | array<object> | `0..N` | Bone list from exported groups and optional catch bone | Conditional | `bedrock_old.js#compile` |

### Legacy Bone, Cube, And Locator Fields

| Path | Type | Shape | Value / range | Emission | Source |
|---|---|---|---|---|---|
| `...bones[i].name` | string | scalar | Group name | Always | `bedrock_old.js#compile` |
| `...bones[i].parent` | string | scalar | Parent group name | Conditional | `bedrock_old.js#compile` |
| `...bones[i].pivot` | array<number> | exactly `3` | Model-space pivot; X sign-flipped | Always | `bedrock_old.js#compile` |
| `...bones[i].rotation` | array<number> | exactly `3` | Degrees; `[-x, -y, z]`; no clamp in exporter | Conditional | `bedrock_old.js#compile` |
| `...bones[i].reset` | boolean | scalar | Exporter only writes `true` | Conditional | `bedrock_old.js#compile` |
| `...bones[i].mirror` | boolean | scalar | Bone-level exporter only writes `true` and only in box-UV mode | Conditional | `bedrock_old.js#compile` |
| `...bones[i].material` | string | scalar | Copied from group material | Conditional | `bedrock_old.js#compile` |
| `...bones[i].cubes` | array<object> | `1..N` when present | Cube list | Conditional | `bedrock_old.js#compile` |
| `...bones[i].locators` | object | locator-name map | Values are vec3 arrays only in legacy export | Conditional | `bedrock_old.js#compile` |
| `...cubes[j].origin` | array<number> | exactly `3` | Model units; X exported as `-(x + sizeX)` | Always | `bedrock_old.js#compile` |
| `...cubes[j].size` | array<number> | exactly `3` | Model units; copied from `obj.size()` | Always | `bedrock_old.js#compile` |
| `...cubes[j].uv` | array<number> | exactly `2` | Texel UV origin `[u, v]`; box UV only | Always | `bedrock_old.js#compile` |
| `...cubes[j].inflate` | number | scalar | Model units; emitted only when numeric and truthy | Conditional | `bedrock_old.js#compile` |
| `...cubes[j].mirror` | boolean | scalar | Boolean override relative to `bone.mirror`; can be `true` or `false` when emitted | Conditional | `bedrock_old.js#compile` |
| `...locators.<name>` | array<number> | exactly `3` | Position vec3 with X sign-flipped | Conditional | `bedrock_old.js#compile` |

## Modern Vs Legacy Surface Differences

| Feature | Modern `bedrock.js` | Legacy `bedrock_old.js` |
|---|---|---|
| Top-level wrapper | `format_version` + `minecraft:geometry` | `format_version` + `geometry.<name>` |
| Texture size keys | `description.texture_width`, `description.texture_height` | `texturewidth`, `textureheight` |
| Visible bounds location | Inside `description` | On geometry object root |
| Cube UV encoding | box UV and per-face UV object | box UV only |
| Per-cube `pivot` / `rotation` | Supported | Not emitted |
| Bone `binding` | Supported | Not emitted |
| `texture_meshes` | Supported | Not emitted |
| `item_display_transforms` | Supported | Not emitted |
| Locator object form | Supports `offset`, `rotation`, `ignore_inherited_scale` | Not emitted |

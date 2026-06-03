# Gap Analysis: blocks + items + recipes (Updated)

> **Date**: 2026-06-02  
> **Scope**: Blocks, Items, Recipes in qylEyelib vs Bedrock Behavior Pack spec  
> **Project**: qylEyelib (eyelib-importer module)

---

## 1. Bedrock Standard Summary

### Sources Validated
| Source | Path | Used For |
|--------|------|----------|
| Bedrock-Wiki blocks-intro | `bedrock-wiki/docs/blocks/blocks-intro.md` | Block format, description, components |
| Bedrock-Wiki block-components | `bedrock-wiki/docs/blocks/block-components.md` | Block component reference |
| Bedrock-Wiki items-intro | `bedrock-wiki/docs/items/items-intro.md` | Item format, description, components |
| Bedrock-Wiki item-components | `bedrock-wiki/docs/items/item-components.md` | Item component reference (40+ components) |
| Bedrock-Wiki recipes | `bedrock-wiki/docs/loot/recipes.md` | Full recipe spec — 7 types, ~794 lines |
| Official schema: recipe_shaped | `creator/content/forms/recipe/recipe_shaped.form.json` | Shaped recipe field spec |
| Official schema: recipe_shapeless | `creator/content/forms/recipe/recipe_shapeless.form.json` | Shapeless recipe field spec |
| Official schema: recipe_furnace | `creator/content/forms/recipe/recipe_furnace.form.json` | Furnace recipe field spec |
| Official schema: recipe_brewing_mix | `creator/content/forms/recipe/recipe_brewing_mix.form.json` | Brewing mix field spec |
| Official schema: recipe_smithing_transform | `creator/content/forms/recipe/recipe_smithing_transform.form.json` | Smithing transform field spec |
| Official schema: recipe_smithing_trim | `creator/content/forms/recipe/recipe_smithing_trim.form.json` | Smithing trim field spec |

### Blocks — Full Structure (from Wiki)
```json
{
  "format_version": "1.26.10",
  "minecraft:block": {
    "description": {
      "identifier": "wiki:custom_block",
      "menu_category": {
        "category": "construction",
        "group": "minecraft:itemGroup.name.concrete",
        "is_hidden_in_commands": false
      }
    },
    "components": { ... }
  }
}
```
- **description**: `identifier` (required), `menu_category` (optional: category, group, is_hidden_in_commands)
- **components**: key-value map of component names → any JSON value
- File path: `BP/blocks/*.json`

### Items — Full Structure (from Wiki)
```json
{
  "format_version": "1.26.10",
  "minecraft:item": {
    "description": {
      "identifier": "wiki:custom_item",
      "menu_category": {
        "category": "items",
        "group": "minecraft:itemGroup.name.swords",
        "is_hidden_in_commands": false
      }
    },
    "components": { ... }
  }
}
```
- **description**: `identifier` (required), `menu_category` (optional)
- **components**: key-value map (40+ known component names)
- File path: `BP/items/*.json`

### Recipes — 7 Types (from Wiki + Schema)

| Type Key | Fields | Tags Required? |
|----------|--------|----------------|
| `minecraft:recipe_shaped` | description, tags, pattern, key, result, [group, priority, unlock, assume_symmetry] | ✅ Yes |
| `minecraft:recipe_shapeless` | description, tags, ingredients, result, [group, priority, unlock] | ✅ Yes |
| `minecraft:recipe_furnace` | description, tags, input, output | ✅ Yes |
| `minecraft:recipe_brewing_mix` | description, tags, input, reagent, output | ✅ Yes |
| `minecraft:recipe_brewing_container` | description, tags, input, reagent, output | ✅ Yes |
| `minecraft:recipe_smithing_transform` | description, tags, template, base, addition, result | ✅ Yes |
| `minecraft:recipe_smithing_trim` | description, tags, template, base, addition | ✅ Yes |

**Key spec details verified from schema & wiki:**
- `result` in shaped recipes can be **single item descriptor OR array** (multiple outputs)
- `result` in shapeless recipes can be single item or array (of 1)
- `unlock` array: each entry has `item` (optional string) and/or `context` (optional string like `"PlayerInWater"`)
- `priority`: integer, lower = higher priority, defaults to 0
- `group`: optional string for recipe book grouping
- `assume_symmetry`: boolean (shaped only), defaults to true
- `tags` is required on **all** recipe types per the official schema
- Ingredient items can be string (`"minecraft:stone"`) or object (`{"item":"minecraft:stone","data":0,"count":1}`)
- The `data` field is deprecated in 1.20.0+; use flattened item identifiers

---

## 2. Existing Implementation Survey

### 2.1 `BrBlock` — ❌ DOES NOT EXIST
- No `io.github.tt432.eyelibimporter.block` package
- No `BrBlock.java` anywhere in the project

### 2.2 `BrItem` — ✅ EXISTS (dead code)
- **File**: `eyelib-importer/.../item/BrItem.java` (35 lines)
- **Current fields**: `formatVersion`, `identifier`, `components` (raw ObjectValue)
- **Missing fields**: `menu_category` (which includes `category`, `group`, `is_hidden_in_commands`)
- **Codec**: `BrItem.CODEC` exists but never referenced outside file
- **Runtime**: No `case ITEM ->` in `processEntry()`, no `itemFiles` in pipeline layers

### 2.3 `BrRecipe` — ✅ EXISTS (dead code)
- **File**: `eyelib-importer/.../recipe/BrRecipe.java` (250 lines)
- **Current types**: All 7 recipe types implemented as sealed interface records
- **Codec**: `BrRecipe.CODEC` exists with type-key dispatch, but never referenced outside file
- **Runtime**: No `case RECIPE ->` in `processEntry()`, no `recipeFiles` in pipeline layers

### 2.4 Pipeline State — Current `processEntry()` Switch
```
default -> captureUnmanaged(acc, entry, unmanagedReasonFor(entry.family()), false, null);
```
BLOCK, ITEM, RECIPE all fall to `default` → `OUTSIDE_IMPORTER_SCOPE` unmanaged.

### Compare: Wired Reference (BrLootTable)
- Codec ✅ → `case LOOT_TABLE` ✅ → `acc.lootTableFiles` ✅ → `BedrockAddonPack.lootTableFiles` ✅ → `BedrockAddonSideAggregate.lootTableFiles` ✅ → `Aggregate.fromPacks()` ✅

---

## 3. Gap Items Summary

### Gap A: Blocks — Full Implementation (HIGH priority)
- **Severity**: CRITICAL
- **BrBlock.java**: Does not exist
- **Required**:
  1. Create `io.github.tt432.eyelibimporter.block` package
  2. Create `BrBlock.java` — record with:
     - `formatVersion` (String)
     - `identifier` (String)
     - `menuCategory` (optional — nested record with `category`, `group`, `is_hidden_in_commands`)
     - `components` (ObjectValue, raw is acceptable for initial wiring)
  3. Add `case BLOCK -> acc.parseAndStore(entry, BrBlock.CODEC, acc.blockFiles)` to `processEntry()`
  4. Add `LinkedHashMap<String, BrBlock> blockFiles` to PackAccumulator, BedrockAddonPack, BedrockAddonSideAggregate
  5. Wire aggregation in `BedrockAddonAggregate.fromPacks()`

### Gap B: Items — Wiring (HIGH priority)
- **Severity**: HIGH
- **BrItem.java**: Exists but dead code
- **Required**:
  1. Add `case ITEM -> acc.parseAndStore(entry, BrItem.CODEC, acc.itemFiles)` to `processEntry()`
  2. Add `LinkedHashMap<String, BrItem> itemFiles` to all pipeline layers

### Gap C: Recipes — Wiring (HIGH priority)
- **Severity**: HIGH
- **BrRecipe.java**: Exists but dead code
- **Required**:
  1. Add `case RECIPE -> acc.parseAndStore(entry, BrRecipe.CODEC, acc.recipeFiles)` to `processEntry()`
  2. Add `LinkedHashMap<String, BrRecipe> recipeFiles` to all pipeline layers

### Gap D: Item Codec — Missing `menu_category` (MEDIUM priority)
- **Severity**: MEDIUM
- **Details**: BrItem only captures `identifier` from description, but Bedrock standard has `menu_category` with:
  - `category` (String) — creative inventory tab
  - `group` (String, optional) — item group within tab
  - `is_hidden_in_commands` (boolean, optional)
- **Impact**: Round-trip fidelity loss — item files re-serialized without menu_category would lose creative tab info
- **Fix**: Add optional `menuCategory` field to BrItem record

### Gap E: Block Codec — Description needs `menu_category` (MEDIUM priority)
- **Details**: When creating BrBlock, model the full description including optional `menuCategory`

### Gap F: Recipe Field Completeness — Medium priority gaps (MEDIUM priority)

**F1: Missing `tags` on 4 recipe types**
- BrewingMix, BrewingContainer, SmithingTransform, SmithingTrim all lack `tags` field
- Bedrock spec (official schema + wiki) requires tags on ALL recipe types
- These four types have `tags` as a required field

**F2: Missing optional fields on shaped/shapeless**
- `group` (String) — recipe book grouping
- `priority` (int, default 0) — conflict resolution
- `unlock` (List of UnlockCondition) — recipe unlocking (1.20.30+)
- `assume_symmetry` (Boolean, shaped only)

**F3: Result should support array form**
- Shaped recipe result can be an array of item descriptors (multiple outputs)
- Shapeless result can also be an array (of 1)
- Current `RecipeResult` only handles single item

### Gap G: BrIngredient missing `tag` field (LOW priority)
- Official schema shows ingredients can have a `tag` field (e.g., `"minecraft:planks"`)
- Wiki says "Selecting recipe inputs by item tags is not supported" — so this is likely schema-documentation mismatch
- Skip for now

### Gap H: No runtime bridge (LOW priority)
- Same as loot tables/spawn rules — runtime bridge is a downstream concern

---

## 4. Pipeline Modification Checklist

### 4.1 Files to modify
| File | Changes |
|------|---------|
| `BedrockAddonLoader.java` | Add `case ITEM ->`, `case BLOCK ->`, `case RECIPE ->` in `processEntry()` |
| `BedrockAddonLoader.java` | Add `itemFiles`, `blockFiles`, `recipeFiles` fields to PackAccumulator |
| `BedrockAddonLoader.java` | Pass these fields in `build()` to `BedrockAddonPack` |
| `BedrockAddonPack.java` | Add `LinkedHashMap<String, BrItem> itemFiles`, `BrBlock blockFiles`, `BrRecipe recipeFiles` to record |
| `BedrockAddonSideAggregate.java` | Add same 3 fields to record and CODEC (note: 16-field limit workaround needed) |
| `BedrockAddonAggregate.java` | Add aggregation (`putAll` loops) in `fromSidePacks()` |

### 4.2 Files to create
| File | Purpose |
|------|---------|
| `eyelib-importer/.../block/BrBlock.java` | Block data record with codec |
| `eyelib-importer/.../block/package-info.java` | Package annotation |

### 4.3 Files to update (codec enrichment)
| File | Changes |
|------|---------|
| `BrItem.java` | Add optional `menu_category` to description |
| `BrRecipe.java` | Add `tags` to BrewingMix, BrewingContainer, SmithingTransform, SmithingTrim |
| `BrRecipe.java` | Add optional `group`, `priority`, `unlock`, `assume_symmetry` to Shaped/Shapeless |
| `BrRecipe.java` | Support array-form result |

---

## 5. Integration Test Design (Updated)

### 5.1 Block Tests

| # | Scenario | Input | Expected |
|---|----------|-------|----------|
| T1 | Minimal block JSON | `{"format_version":"1.20.10","minecraft:block":{"description":{"identifier":"test:simple_block"},"components":{}}}` | BrBlock.identifier = "test:simple_block", components empty |
| T2 | Block with menu_category | Include `menu_category: { category: "construction", group: "minecraft:itemGroup.name.concrete" }` | menuCategory parsed correctly |
| T3 | Block with basic components | `minecraft:destructible_by_mining`, `minecraft:geometry` in components | components as ObjectValue, no crash |
| T4 | Block without format_version | Omit format_version | Default/empty string, still parses |
| T5 | Block with extra fields | Unknown fields in components | Silent retention via ObjectValue |
| T6 | Loader integration | Place `blocks/test.block.json` in BP, load via loader | Block appears in `addon.aggregate().behaviorPack().blockFiles()` |

### 5.2 Item Tests

| # | Scenario | Input | Expected |
|---|----------|-------|----------|
| T7 | Minimal item JSON | `{"format_version":"1.20.10","minecraft:item":{"description":{"identifier":"test:simple_item"},"components":{}}}` | BrItem.identifier = "test:simple_item" |
| T8 | Item with menu_category | Include menu_category with category and group | menuCategory parsed |
| T9 | Item with display_name component | `"minecraft:display_name":{"value":"Test Item"}` in components | components as ObjectValue |
| T10 | Codec roundtrip | Encode then decode BrItem | Identity preserved |
| T11 | Loader integration | Place `items/test.item.json` in BP | Item in `addon.aggregate().behaviorPack().itemFiles()` |

### 5.3 Recipe Tests

| # | Scenario | Input | Expected |
|---|----------|-------|----------|
| T12 | Shaped recipe | Full shaped with pattern, key, result | BrRecipe.Shaped with correct fields |
| T13 | Shaped with group/priority/unlock/assume_symmetry | Optional fields present | Correctly parsed |
| T14 | Shaped with array result | `"result": [{"item":"a","count":1}, "b"]` | Parsed as multiple outputs |
| T15 | Shapeless recipe | Shapeless with ingredients list | BrRecipe.Shapeless |
| T16 | Furnace recipe | Furnace with input and output | BrRecipe.Furnace |
| T17 | Brewing mix | Brewing with tags | BrRecipe.BrewingMix with tags field |
| T18 | Brewing container | Brewing container | BrRecipe.BrewingContainer with tags field |
| T19 | Smithing transform | Full transform | BrRecipe.SmithingTransform with tags field |
| T20 | Smithing trim | Trim (no result) | BrRecipe.SmithingTrim with tags field |
| T21 | All 7 types dispatch | Any recipe type → decode | Correct subtype returned |
| T22 | Invalid recipe type | Unknown type key | Error: "No recipe type key found" |
| T23 | Ingredient as string | `"minecraft:stone"` instead of object | RecipeIngredient("minecraft:stone",0,1) |
| T24 | Ingredient as object | `{"item":"minecraft:diamond","count":3}` | RecipeIngredient("minecraft:diamond",0,3) |
| T25 | Unlock with context | `"unlock":[{"context":"PlayerInWater"}]` | UnlockCondition parsed |
| T26 | Loader integration | Place `recipes/test.recipe.json` in BP | Recipe in `behaviorPack.recipeFiles()` |
| T27 | Recipe codec roundtrip | Encode then decode for all 7 types | Identity preserved for each type |

### 5.4 Pure Codec Tests (can run via /eval)

```java
// Block parse
var blockJson = JsonParser.parseString("""
  {"format_version":"1.20.10","minecraft:block":{
    "description":{"identifier":"test:my_block"},
    "components":{"minecraft:geometry":"geometry.test"}
  }}""");
BrBlock block = BrBlock.CODEC.parse(JsonOps.INSTANCE, blockJson).getOrThrow(...);

// Item parse
var itemJson = JsonParser.parseString("""
  {"format_version":"1.20.10","minecraft:item":{
    "description":{"identifier":"test:my_item"},
    "components":{"minecraft:icon":"wiki:custom_item"}
  }}""");
BrItem item = BrItem.CODEC.parse(JsonOps.INSTANCE, itemJson).getOrThrow(...);

// Recipe parse
var recipeJson = JsonParser.parseString("""
  {"format_version":"1.20.10","minecraft:recipe_shaped":{
    "description":{"identifier":"test:my_recipe"},
    "tags":["crafting_table"],
    "pattern":["AA","AB"],
    "key":{"A":"minecraft:stone","B":"minecraft:diamond"},
    "result":{"item":"minecraft:stick","count":4}
  }}""");
BrRecipe recipe = BrRecipe.CODEC.parse(JsonOps.INSTANCE, recipeJson).getOrThrow(...);
```

---

## 6. Implementation Sequence (Recommended)

### Phase 1 — Wire existing codecs (Items + Recipes) — 1 session
1. Add `itemFiles` / `recipeFiles` to PackAccumulator, BedrockAddonPack, BedrockAddonSideAggregate, BedrockAddonAggregate
2. Add `case ITEM ->` / `case RECIPE ->` in `processEntry()`
3. Write tests T7-T11, T12-T27
4. Verify with `/eval` codec tests

### Phase 2 — Implement BrBlock — 1 session
1. Create `io.github.tt432.eyelibimporter.block` package
2. Create `BrBlock.java` with full description (identifier + menu_category + components)
3. Same wiring pattern as Phase 1 (`blockFiles` pipeline)
4. Write tests T1-T6

### Phase 3 — Enrich BrItem with menu_category — 0.5 session
1. Add optional `menuCategory` field to BrItem record
2. Update CODEC accordingly
3. Write roundtrip test T10

### Phase 4 — Enrich BrRecipe — 1 session
1. Add `tags` to BrewingMix, BrewingContainer, SmithingTransform, SmithingTrim
2. Add optional `group`, `priority`, `unlock` fields to Shaped/Shapeless
3. Add `assume_symmetry` to Shaped
4. Support array-form result for Shaped/Shapeless
5. Write roundtrip tests T27, edge case tests T13, T14, T25

### Phase 5 — Runtime bridge (Future)
- Same as loot tables/spawn rules — downstream concern

---

## 7. Appendix: Official Schema Field Reference

### Shaped Recipe (from recipe_shaped.form.json)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| format_version | string | ✅ | |
| description.identifier | string | ✅ | |
| tags | string[] | ✅ | e.g. ["crafting_table"] |
| pattern | string[] | ✅ | 1-3 rows of 1-3 chars |
| key | map<string, item> | ✅ | |
| result | item OR item[] | ✅ | Array for multi-output |
| group | string | ❌ | Recipe book grouping |
| priority | int | ❌ | Lower = higher priority |
| unlock | unlock[] | ❌ | [{item, context}] |
| assume_symmetry | boolean | ❌ | Default true |

### Shapeless Recipe (from recipe_shapeless.form.json)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| format_version | string | ✅ | |
| description.identifier | string | ✅ | |
| tags | string[] | ✅ | |
| ingredients | item[] | ✅ | |
| result | item OR item[] | ✅ | Array of 1 |
| group | string | ❌ | |
| priority | int | ❌ | |
| unlock | unlock[] | ❌ | |

### Furnace Recipe (from recipe_furnace.form.json)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| format_version | string | ✅ | |
| description.identifier | string | ✅ | |
| tags | string[] | ✅ | furnace, smoker, blast_furnace, campfire |
| input | item string | ✅ | Simple string |
| output | item string | ✅ | Simple string |

### Brewing Mix (from recipe_brewing_mix.form.json)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| format_version | string | ✅ | |
| description.identifier | string | ✅ | |
| tags | string[] | ✅ | ["brewing_stand"] |
| input | item string | ✅ | e.g. "minecraft:potion_type:awkward" |
| reagent | item string | ✅ | e.g. "minecraft:blaze_powder" |
| output | item string | ✅ | e.g. "minecraft:potion_type:strength" |

### Smithing Transform (from recipe_smithing_transform.form.json)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| format_version | string | ✅ | |
| description.identifier | string | ✅ | |
| tags | string[] | ✅ | ["smithing_table"] |
| template | item string | ✅ | |
| base | item string | ✅ | |
| addition | item string | ✅ | |
| result | item string | ✅ | |

### Smithing Trim (from recipe_smithing_trim.form.json)
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| format_version | string | ✅ | |
| description.identifier | string | ✅ | |
| tags | string[] | ✅ | ["smithing_table"] |
| template | item OR object | ✅ | Can have item or tag |
| base | item OR object | ✅ | Can have item or tag |
| addition | item OR object | ✅ | Can have item or tag |

---

## 8. File-level Reference (Updated)

| File | Status | Lines | Notes |
|------|--------|-------|-------|
| `item/BrItem.java` | ✅ Exists, dead code | 35 | Missing `menu_category` |
| `item/package-info.java` | ✅ Exists | 7 | |
| `recipe/BrRecipe.java` | ✅ Exists, dead code | 250 | Missing `tags` on 4 types, missing optional fields |
| `recipe/package-info.java` | ✅ Exists | 7 | |
| **BrBlock.java** | ❌ Does not exist | — | Full implementation needed |
| **block/package-info.java** | ❌ Does not exist | — | |
| `addon/BrLootTable.java` | ✅ Wired reference | 146 | Pattern to follow |
| `addon/BedrockAddonLoader.java` | ⚠️ Missing ITEM/BLOCK/RECIPE cases | 926 | `processEntry()` default falls to unmanaged |
| `addon/BedrockAddonPack.java` | ⚠️ Missing itemFiles/blockFiles/recipeFiles | 74 | Add 3 new fields |
| `addon/BedrockAddonSideAggregate.java` | ⚠️ Missing same fields | 138 | 16-field CODEC limit — needs workaround |
| `addon/BedrockAddonAggregate.java` | ⚠️ Missing aggregation | 249 | Add putAll loops in `fromSidePacks()` |
| `addon/BedrockResourceFamily.java` | ✅ Has ITEM/BLOCK/RECIPE | 91 | Enum values already exist |

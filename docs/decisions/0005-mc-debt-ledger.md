# ADR-0005: Functional Module Debt Ledger

**Status:** Active
**Context:** After extracting functional modules (particle, material, importer, attachment, Molang, animation, behavior), remaining cross-module debt blocks full ownership extraction. Also tracks Bedrock format implementation gaps identified during addon pipeline analysis.
**Decision:** Track remaining debt items with clear status indicators. Root keeps only code that genuinely coordinates multiple feature modules or is required by the root mod entrypoint.
**Consequences:** Future refactoring phases have a prioritized list of remaining cross-module dependencies and format gaps to address.

---

# Functional Module Debt Ledger

## Purpose
- Record remaining cross-module debt that blocks further functional ownership extraction.
- Track Bedrock format implementation gaps that could become ongoing blind spots.
- Functional ownership rule: particle code → `:eyelib-particle`, material → `:eyelib-material`, importer → `:eyelib-importer`, attachment → `:eyelib-attachment`, Molang → `:eyelib-molang`, animation → `:eyelib-animation`, behavior → `:eyelib-behavior`.
- Root keeps only code that genuinely coordinates multiple feature modules or is required by the root mod entrypoint.

## Key

| Status | Meaning |
|--------|---------|
| ✅ Done | Resolved — no further action needed |
| Stable | Implemented to a maintainable degree; remaining work not a priority |
| Gap | Known format/feature gap — not necessarily a bug, but a tracked blind spot |
| Partial | Partially implemented; significant work remains |

## Cross-Module Debt

| ID | Problem | Status |
|---|---|---|
| FM-004 | ~~`ParticleSpawnService` — root compatibility facade for packet entrypoints and capability context.~~ **Resolved 2026-06-02.** `ParticleSpawnService` deleted. Suppliers injected via `ParticleSpawnRuntimeAdapter.configure()` in `ManagerEventLifecycleHooks`. | ✅ Done |
| FM-008 | Root-coupled attachment packets — Phase 0 (network self-registration) ✅, Phase 1 (hooks+mixin relocation) ✅, Phase 2 (payload conversion) ✅. Remaining ~9 files keep legitimate API/DTO references within allowed dependency direction. | Stable |
| FM-014 | Shared channel entrypoints and context-free handler dispatch — root network package owns shared registration/delegation. | Stable |
| FM-015 | `LivingEntityRendererAccessor` — client-render-owned accessor mixin. | Stable |

## Bedrock Format Implementation Gaps

Gaps identified during systematic Bedrock addon pipeline analysis against E: drive documentation (`/mnt/e/_____基岩版文档/`).

| ID | Subcategory | Bedrock Standard | Eyelib Status | Notes |
|---|---|---|---|---|
| BG-001 | `behavior_entities` components | `minecraft:entity.components` with ~200+ possible component types | ✅ RawComponent fallback covers unknown; 6 typed (variant, mark_variant, ageable, admire_item, addrider, health) | RawComponent preserves unknown components as JSON. Typed expansion can be additive. |
| BG-002 | behavior_entities events | ~15 event node types (trigger, queue_command, set_property, filters, etc.) | ✅ 7 types implemented (add, remove, sequence, randomize, trigger, queue_command, run_command) + SequenceEntry filter gating | P0 complete. Remaining: set_property, sound_effect, particle_effect, etc. |
| BG-003 | spawn_rules runtime | Condition evaluator for biome/block filters | ✅ SpawnRuleRegistry + SpawnRuleEvaluator (biome filter) | MVP complete. Full condition tree evaluation deferred. |
| BG-004 | items format | `minecraft:item` with menu_category, all components | ✅ BrItem wired + menu_category field | Field-complete for basic item definition. |
| BG-005 | blocks format | `minecraft:block` with description + components | ✅ BrBlock created + wired | Codec exists; components as raw. |
| BG-006 | recipes format | 7 recipe types (shaped, shapeless, furnace, brewing_mix, brewing_container, smithing_transform, smithing_trim) | ✅ BrRecipe wired + all missing fields filled (group, tags, priority, assume_symmetry, result array) | Codec complete for all 7 types. |
| BG-007 | trading format | `minecraft:entity.trading` with tiers, trades, price_multiplier | ✅ BrTrading wired + fields (trader_exp, max_uses, reward_exp, price_multiplier) | Codec complete. |
| BG-008 | loot_tables format | Pools with entries, functions, conditions, tiers, bonus_rolls | ✅ BrLootTable wired + bonus_rolls/tiers | Codec complete for basic pools. Functions/conditions as raw. |
| BG-009 | structures (`.mcstructure`) | Bedrock Little-Endian NBT binary format | ⚠️ Not implemented | **JE has its own `.nbt` format with built-in `NbtIo`/`StructureTemplate` API.** Bedrock format requires LE-NBT parser + block state mapping table. Low priority for a JE mod — recommend implementing JE `.nbt` import first, defer `.mcstructure` until cross-platform use case emerges. See `docs/gap-analysis/STRUCTURE_FORMAT_COMPARISON.md`. |

## Newly Wired Pipeline (2026-06-02)

The following resource families were moved from dead code / missing to active Bedrock addon pipeline:

| Resource | Pipeline Path | Status |
|----------|--------------|--------|
| Items | `items/*.json` → BrItem → BedrockAddonAggregate.itemFiles() | ✅ |
| Blocks | `blocks/*.json` → BrBlock → BedrockAddonAggregate.blockFiles() | ✅ |
| Recipes | `recipes/*.json` → BrRecipe → BedrockAddonAggregate.recipeFiles() | ✅ |
| Trading | `trading/*.json` → BrTrading → BedrockAddonAggregate.tradeFiles() | ✅ |
| Behavior entities | `entities/*.json` → BrBehaviorEntityFile → BehaviorEntity(components) | ✅ components field fixed |
| Spawn rules | `spawn_rules/*.json` → BrSpawnRule → SpawnRuleRegistry | ✅ CODEC fixed + runtime |

Key: all data flows through `PackAccumulator` → `BedrockAddonPack` → `BedrockAddonSideAggregate` → `BedrockAddonAggregate`.

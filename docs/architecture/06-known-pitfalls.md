# Known Pitfalls & Anti-Patterns

Lessons learned from the module separation milestones. These patterns were discovered through hands-on refactoring work.

## Critical

### 1. Runtime Adaptation vs Schema Duplicate

**Symptom:** Root `BrBoneAnimationDefinition` and importer `BrBoneAnimationSchema` have similar field names → mistaken as duplicates.

**Why:** Both describe bone animation but at different semantic layers.

**Prevention:** Check the consumer. Schema is consumed by Codec (importer), Definition is consumed by Sampler/Executor (root runtime). Different consumers = not a duplicate.

**Detection:** Different package names and different fields (Definition has sortedKeys, compiled channels) = not duplicate.

### 2. Deleting Active Br*Executor as "Dead Code"

**Symptom:** Deleting `bedrock/` classes without reference verification.

**Why:** Successful deletion of `KeyFrame.java` in v1.4 creates the false impression that all bedrock/ code is legacy.

**Consequences:** Deleting `BrClipExecutor` or `BrControllerExecutor` breaks the animation pipeline: `EntityRenderSystem → BrAnimator → Animation.tickAnimationUntyped() → BrClipExecutor.tick()`.

**Prevention:** Run IDE "Find References" (scope: project_production_files) on **every** `.java` file in `client/animation/bedrock/`. Delete only zero-reference files.

### 3. Moving EyelibAttachableData to Attachment Module

**Symptom:** Seeing it references attachment types and assuming it "belongs" in the attachment module.

**Why:** `EyelibAttachableData` is a Forge `@Mod.EventBusSubscriber` registry hub with `DeferredRegister` and `RegistryObject<DataAttachmentType<...>>` constants.

**Consequences:** Moving it would break Forge bootstrap—it depends on root's `Eyelib.MOD_ID`, registers to the MOD event bus, and is wired via `mc/impl/bootstrap/EyelibMod`.

**Detection:** Contains `@Mod.EventBusSubscriber`, `DeferredRegister`, `RegistryObject`, `IForgeRegistry` → root-only concerns.

## Moderate

### 4. Extracting EntityBehaviorData Codec Mechanically

**Symptom:** Seeing `CODEC` and `STREAM_CODEC` static fields and assuming the entire codec belongs in attachment.

**Why:** The codec serializes behavior-specific fields coupled to `MolangQuery` variant/markVariant lookups.

**Prevention:** If the codec's `RecordCodecBuilder` references root types or root behavior methods → cannot extract.

### 5. Moving Root-Dependent Code to Preprocessing

**Symptom:** PREP-01 finds parse/bake patterns in root and moves them to preprocessing unconditionally.

**Prevention:** Verify imports. If the file imports `io.github.tt432.eyelib.capability`, `io.github.tt432.eyelib.client.animation`, or `io.github.tt432.eyelib.mc.impl` → cannot move to preprocessing.

### 6. Stale `.class` Files from Deleted Sources

**Symptom:** Deleted source files leave `.class` files in `bin/`, causing IDE reference false positives.

**Prevention:** Run `clean build` before deletion phases to ensure `bin/` matches `src/`.

## Minor

### 7. Stale Module Name in README

**Symptom:** README still references `eyelib-processor` (old name before v1.4 rename).

**Prevention:** Full-text search for `eyelib-processor` (not containing `eyelib-preprocessing`) across all documentation.

### 8. Deleting Legacy Compatibility Pointer README

**Symptom:** `mixin/README.md` looks short and appears to be "empty/obsolete."

**Why:** It is an intentional legacy compatibility pointer directing readers to the new location.

**Detection:** Readme contains "legacy", "compatibility pointer", or "phased out" keywords.

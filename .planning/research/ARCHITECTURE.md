# Architecture Patterns: Eyelib v1.5 深度结构清理

**Domain:** Multi-module Forge rendering library (Java 17, Forge 1.20.1)
**Researched:** 2026-05-12
**Overall confidence:** HIGH

## Recommended Architecture

```
                    ┌──────────────────────────────────┐
                    │          ROOT (: ::)             │
                    │  namespace: io.github.tt432.     │
                    │            eyelib                 │
                    │                                  │
                    │  ┌─────────────────────────────┐ │
                    │  │ capability/                 │ │
                    │  │  EyelibAttachableData   ←──┼─┼── (registry hub, refs attachment types)
                    │  │  RenderData              ←──┼─┼── (runtime owner, refs attachment info types)
                    │  │  EntityBehaviorData          │ │
                    │  │  ItemInHandRenderData        │ │
                    │  │  component/                  │ │
                    │  │    AnimationComponent    ←──┼─┼── (runtime state, refs AnimationComponentInfo)
                    │  │    ModelComponent        ←──┼─┼── (runtime state, refs ModelComponentInfo)
                    │  │    ClientEntityComponent     │ │
                    │  │    RenderControllerComponent │ │
                    │  └─────────────────────────────┘ │
                    │                                  │
                    │  ┌─────────────────────────────┐ │
                    │  │ client/animation/           │ │
                    │  │  Animation<T> (port system)  │ │
                    │  │  BrAnimator (tick dispatch)  │ │
                    │  │  AnimationApplier            │ │
                    │  │  bedrock/                    │ │
                    │  │    BrAnimation        ←─────┼─┼── BrAnimationSet (importer)
                    │  │    BrAnimationEntry   ←─────┼─┼── BrAnimationEntrySchema (importer)
                    │  │    BrBoneAnimation    ←─────┼─┼── BrBoneAnimationSchema (importer)
                    │  │    BrBoneKeyFrame     ←─────┼─┼── BakedBoneKeyFrame (preprocessing)
                    │  │    controller/                │ │
                    │  │      BrAnimationController ←─┼─┼── BrAnimationControllerSchema (importer)
                    │  │      BrControllerExecutor     │ │
                    │  └─────────────────────────────┘ │
                    │                                  │
                    │  ┌─────────────────────────────┐ │
                    │  │ client/loader/              │ │
                    │  │  Br*Loader.java       ←─────┼─┼── LoaderParsingOps (preprocessing)
                    │  │                              │ │
                    │  │ client/model/                │ │
                    │  │  ModelBakeInvalidationHooks ←┼─┼── EmissiveModelBakeInfo (preprocessing)
                    │  │                              │ │
                    │  │ client/render/sync/          │ │
                    │  │  RenderSyncApplyOps    ←─────┼─┼── AnimationComponentInfo (attachment)
                    │  │                              │ │
                    │  │ mc/impl/network/packet/      │ │
                    │  │  AnimationComponentSyncPkt←──┼─┼── AnimationComponentInfo (attachment)
                    │  │  DataAttachmentUpdatePacket ←┼─┼── EyelibAttachableData (root self)
                    │  └─────────────────────────────┘ │
                    └──────┬───────────┬───────────────┘
                           │ api       │ api
              ┌────────────┼───────────┼──────────────┐
              │            ▼           ▼              │
              │  ┌─────────────────────────────┐      │
              │  │ :eyelib-attachment          │      │
              │  │ ns: eyelibattachment         │      │
              │  │                              │      │
              │  │ capability/                  │      │
              │  │  AnimationComponentInfo  ────┼──► :eyelib-molang
              │  │  ModelComponentInfo          │      │
              │  │  ExtraEntityData             │      │
              │  │  ExtraEntityUpdateData       │      │
              │  │  EntityStatistics            │      │
              │  │                              │      │
              │  │ network/                     │      │
              │  │  DataAttachmentSyncPacket    │      │
              │  │                              │      │
              │  │ dataattach/                  │      │
              │  │  DataAttachmentType          │      │
              │  │                              │      │
              │  │ Deps: :eyelib-util,          │      │
              │  │       :eyelib-molang          │      │
              │  └─────────────────────────────┘      │
              │                                       │
              │  ┌─────────────────────────────┐      │
              │  │ :eyelib-preprocessing        │      │
              │  │ ns: eyelibpreprocessing      │      │
              │  │                              │      │
              │  │ loader/LoaderParsingOps ─────┼──► :eyelib-importer (compileOnly)
              │  │ animation/baked/             │      │
              │  │   BoneAnimationBaker ────────┼──► :eyelib-importer
              │  │   BakedBoneKeyFrame          │      │
              │  │ manager/reload/              │      │
              │  │   ManagerResourceReloadPlan  │      │
              │  │ model/bake/                  │      │
              │  │   BakedModel, BakeInfo       │      │
              │  │ particle/flipbook/           │      │
              │  │   ParticleFlipbookSummary    │      │
              │  │                              │      │
              │  │ Deps: :eyelib-importer (co), │      │
              │  │       :eyelib-molang         │      │
              │  └─────────────────────────────┘      │
              │                                       │
              │  ┌─────────────────────────────┐      │
              │  │ :eyelib-importer             │      │
              │  │ ns: eyelibimporter           │      │
              │  │                              │      │
              │  │ animation/bedrock/           │      │
              │  │   BrAnimationSet             │      │
              │  │   BrAnimationEntrySchema     │      │
              │  │   BrBoneAnimationSchema      │      │
              │  │   BrBoneKeyFrameSchema       │      │
              │  │   controller/                │      │
              │  │     BrAnimationControllerSchema│    │
              │  │     BrAnimationControllerSet │      │
              │  │     BrAcStateDefinition      │      │
              │  │     BrAcState (raw schema)   │      │
              │  └─────────────────────────────┘      │
              └───────────────────────────────────────┘
```

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| **Root `capability/`** | Forge registry bootstrap, runtime component state, attachment wiring hub | `:eyelib-attachment` (consumes data types), root render/sync (consumes runtime state) |
| **Root `client/animation/`** | Runtime animation execution, playback state, port system, schema-to-runtime adaptation | `:eyelib-importer` (consumes schema), `:eyelib-preprocessing` (consumes baker), `:eyelib-molang` |
| **`EyelibAttachableData`** | Central registry: defines all `DataAttachmentType` constants, bridges root runtime to attachment data/codec types | `:eyelib-attachment` capability/ and dataattach/ packages, Forge deferred registry |
| **`RenderData<T>`** | Per-entity runtime render state holder, codec/deserializes sync payloads from attachment info types | `AnimationComponentInfo`, `ModelComponentInfo` (attachment types), `AnimationComponent` (root component state) |
| **`BrAnimationEntry`** | Bedrock clip executor: schema→runtime adaptation, Molang evaluation, bone sampling | `BrAnimationEntrySchema` (importer), `BrBoneAnimation`, `BrBoneKeyFrame`, `MolangScope` |
| **`BrBoneAnimation`** | Bone channel container: adapts importer schema via `BoneAnimationBaker`, does Catmull-Rom lerp | `BrBoneKeyFrameSchema` (importer), `BoneAnimationBaker` (preprocessing), `BrBoneKeyFrame` |
| **`BrAnimationController`** | State machine controller: adapts importer `BrAnimationControllerSchema` to runtime `StateMachineAnimation` | `BrAnimationControllerDefinition` (importer), `BrControllerExecutor` |
| **`LoaderParsingOps`** (preprocessing) | Platform-free JSON parsing/translation from gson→codec | Root `Br*Loader` classes (material, animation, controller, entity, attachable) |
| **`ManagerResourceReloadPlan`** (preprocessing) | Path classification and texture-key planning | Root `ManagerResourceImportPlanner` (GUI import flow) |
| **`BoneAnimationBaker`** (preprocessing) | Schema→baked conversion for bone keyframes | Root `BrBoneAnimation.fromSchema()`, `BrBoneKeyFrame.fromSchema()` |
| **`AnimationComponentInfo`** (attachment) | Sync-serializable snapshot of animation bindings (animations + animate) | Root `AnimationComponent`, `RenderData.codec()`, `AnimationComponentSyncPacket` |
| **`ModelComponentInfo`** (attachment) | Sync-serializable snapshot of model bindings | Root `ModelComponent`, `RenderData.codec()` |
| **`ExtraEntityData/Update`** (attachment) | Entity behavior data/codec types | Root `EyelibAttachableData` (registry), `MolangQuery` (Molang MC bindings), sync runtime |

### Data Flow

**Capability Sync Flow (Server → Client):**
```
Server: EntityExtraDataRuntimeHooks.onLivingTick()
  → DataAttachmentHelper.getOrCreate(EyelibAttachableData.EXTRA_ENTITY_DATA.get(), entity)
  → ExtraEntityDataUpdater.update(current, flags)          (pure logic in common/runtime/)
  → DataAttachmentHelper.setLocal(key, entity, updated)
  → DataAttachmentSyncRuntime.syncTrackedAndSelf(key, entity, updated)
  → ExtraEntityDataPacket(entityId, data)                  (root-coupled, refs attachment type)
  → EyelibNetworkTransport.send(player, packet)
Client: NetClientHandlers → DataAttachmentSyncRuntime.handleExtraDataUpdate()
  → DataAttachmentHelper.setLocal(key, entity, data)       (attachment-owned type stored)
```

**Animation Sync Flow (Server → Client):**
```
Server: AnimationComponent.setup() → serializableInfo = new AnimationComponentInfo(...)
  → RenderData.sync() → ClientRenderSyncService.sync(this)
  → AnimationComponentSyncPacket(entityId, animationInfo)  (refs attachment:AnimationComponentInfo)
  → EyelibNetworkTransport.send(player, packet)
Client: NetClientHandlers → ClientRenderSyncService.apply(packet)
  → RenderSyncApplyOps.applyAnimationInfo(component::setInfo, animationInfo)
  → AnimationComponent.setInfo(AnimationComponentInfo)     (deserializes attachment type)
```

**Animation Execution Flow (Client Render):**
```
EntityRenderSystem.onEvent()
  → RenderData.getComponent(entity) → EntityRenderSystem.setupClientEntity()
  → BrAnimator.tickAnimation(AnimationComponent, scope, effects, ticks)
    → for each (Animation<?>, MolangValue): Animation.tickAnimationUntyped(data, ...)
      → BrAnimationEntry.tickAnimation() → BrClipExecutor.tick()
        → BrBoneAnimation.lerp() → BrBoneAnimationSampler.sample()
          → BrBoneKeyFrame.catmullromLerp() / linearLerp()
        → BrBoneAnimation.lerpPosition/Rotation/Scale()
```

**Schema Load Flow (Client Resource Reload):**
```
BrAnimationLoader (root) → Forge reload listener
  → LoaderParsingOps.parseBySourceKey(jsonMap, BrAnimationSet.CODEC)  (preprocessing)
  → BrAnimationSet (importer schema) → BrAnimation.fromSchemaSet()
  → manager store publish → AnimationComponent.onManagerEntryChanged()
```

## Patterns to Follow

### Pattern 1: Root-as-Consumer, Submodule-as-Data-Owner
**What:** Root runtime code references submodule-owned data/codec types but never the reverse.
**When:** Whenever sync payload types, schema definitions, or pure data records need to cross the module boundary.
**Example:**
```java
// Root EyelibAttachableData.java — consumes attachment types
import io.github.tt432.eyelibattachment.capability.ExtraEntityData;
// ...
public static final RegistryObject<DataAttachmentType<ExtraEntityData>> EXTRA_ENTITY_DATA =
    DATA_ATTACHMENTS.register("extra_entity_data", () ->
        new DataAttachmentType<>(..., ExtraEntityData::empty, ExtraEntityData.CODEC, ExtraEntityData.STREAM_CODEC));
```

### Pattern 2: Bridge Event Hooks (No Reverse Dep)
**What:** Root `mc/impl/` hooks subscribe to Forge events and bridge to submodule code, preventing submodule→root dependencies.
**When:** Preprocessing, attachment, or other submodule code needs to react to Minecraft lifecycle events without importing root packages.
**Example:**
```java
// Root ModelBakeInvalidationHooks.java
import io.github.tt432.eyelibpreprocessing.model.bake.EmissiveModelBakeInfo;
// ...
MinecraftForge.EVENT_BUS.addListener(event ->
    EmissiveModelBakeInfo.INSTANCE.invalidateModel(event.getEntryName()));
```

### Pattern 3: Schema→Runtime Adaptation
**What:** Importer owns raw schema/codec types; root runtime owns adaptation layer that converts schema to runtime executors.
**When:** Bedrock JSON formats need codec parsing (importer) vs. runtime execution (root).
**Example:**
```java
// Root BrBoneAnimation.java adapts importer schema via preprocessing baker
import io.github.tt432.eyelibimporter.animation.bedrock.BrBoneAnimationSchema;   // importer schema
import io.github.tt432.eyelibpreprocessing.animation.baked.BoneAnimationBaker;   // preprocessing baker
// ...
public static BrBoneAnimation fromSchema(BrBoneAnimationSchema schema) {
    return new BrBoneAnimation(
        toImmutableMap(BoneAnimationBaker.bakeBoneAnimation(schema.rotation())),
        toImmutableMap(BoneAnimationBaker.bakeBoneAnimation(schema.position())),
        toImmutableMap(BoneAnimationBaker.bakeBoneAnimation(schema.scale()))
    );
}
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: Reverse Dependency (Submodule → Root)
**What:** A submodule imports from root runtime packages (`io.github.tt432.eyelib.mc.impl`, `io.github.tt432.eyelib.capability`, etc.).
**Why bad:** Creates circular build dependency, prevents independent testing, violates module boundary.
**Instead:** Use bridge hooks in root `mc/impl/` that consume submodule code. Use event-driven invalidation via `ModelBakeInvalidationHooks`.

### Anti-Pattern 2: Schema Duplication Between Root and Importer
**What:** Copying schema/codec types into root that already exist in `:eyelib-importer`.
**Why bad:** Two sources of truth for the same Bedrock JSON format, schema drift, double maintenance.
**Instead:** Importer owns the canonical schema; root runtime creates adaptation wrappers that reference importer types.
**Note:** Root has BrAnimationEntryDefinition, BrBoneAnimationDefinition etc. — these are RUNTIME adapters with additional compiled state, NOT schema duplicates of importer's BrAnimationEntrySchema, BrBoneAnimationSchema. Verify carefully.

### Anti-Pattern 3: New Code in Drained Packages
**What:** Adding new Java source to `src/main/java/io/github/tt432/eyelib/util/` or `client/particle/bedrock/`.
**Why bad:** These packages have been drained/migrated; new code violates the cleanup intent.
**Instead:** Use `:eyelib-util` for shared utilities, `:eyelib-particle` for particle code.

## Scalability Considerations

| Concern | Cleanup Target | Integration Risk |
|---------|---------------|------------------|
| Capability registry (EyelibAttachableData) | Root stays as Forge registry hub; verifies all attachment type refs are one-way | LOW — current state already verified one-way |
| Animation bedrock runtime | All bedrock/ code is ACTIVE runtime — cleanup targets invalid interfaces only | MEDIUM — need precise reference analysis |
| Preprocessing consumers | 5 root loader files, 2 animation files, 1 GUI planner, 1 model invalidation hook | LOW — already properly integrated |
| Attachment capability consumers | ~38 reference sites in root, all through EyelibAttachableData registry indirection | LOW — well-established pattern |
| Mixin config | relocated to `mc/impl/mixin/`, legacy `mixin/` README is intentional pointer | NONE — no change needed |

## Namespace Mapping for v1.5 Cleanup

| Current Location | Target Namespace | Action |
|-----------------|------------------|--------|
| Root `capability/EyelibAttachableData` | STAYS (root) | Registry hub, no move |
| Root `capability/RenderData` | STAYS (root) | Runtime owner, no move |
| Root `capability/component/*` | STAYS (root) | Runtime component state, no move |
| Root `capability/EntityBehaviorData` | STAYS (root) | Runtime owner with custom CODEC/STREAM_CODEC |
| Root `capability/ItemInHandRenderData` | STAYS (root) | Runtime owner, refs RenderData |
| Attachment `capability/AnimationComponentInfo` | STAYS (attachment) | Data/codec type, already correct |
| Attachment `capability/ModelComponentInfo` | STAYS (attachment) | Data/codec type, already correct |
| Attachment `capability/ExtraEntityData` | STAYS (attachment) | Data/codec type, already correct |
| Attachment `capability/ExtraEntityUpdateData` | STAYS (attachment) | Data/codec type, already correct |
| Attachment `capability/EntityStatistics` | STAYS (attachment) | Data/codec type, already correct |
| Root `client/animation/bedrock/*` (ACTIVE) | STAYS (root) | Runtime executors, not schema |
| Root `client/animation/bedrock/*` (DEAD) | DELETE | Invalid interfaces with zero references |
| Root `client/animation/AnimationApplier` | STAYS (root) | Moved here in v1.4 Phase 15, correct owner |
| Root `client/animation/Animation.java` (port system) | STAYS (root) | Core runtime abstraction |
| Importer `animation/bedrock/controller/Br*` | STAYS (importer) | Schema/codec, already correct |
| Preprocessing `animation/baked/*` | STAYS (preprocessing) | Bake helpers, already correct |
| Preprocessing `loader/*` | STAYS (preprocessing) | Parse helpers, already correct |
| Preprocessing `manager/reload/*` | STAYS (preprocessing) | Planning helpers, already correct |
| Preprocessing `model/bake/*` | STAYS (preprocessing) | Model bake helpers, already correct |

## Cleaning Up Requirements: Integration Points

### CAP-01: Capability Residual Audit & Migration

**What already moved (v1.4):**
- `ExtraEntityData.java` — moved to `eyelib-attachment/.../capability/ExtraEntityData.java`
- `ExtraEntityUpdateData.java` — moved to `eyelib-attachment/.../capability/ExtraEntityUpdateData.java`
- `EntityStatistics.java` — moved to `eyelib-attachment/.../capability/EntityStatistics.java`
- `AnimationComponentInfo.java` — moved to `eyelib-attachment/.../capability/AnimationComponentInfo.java`
- `ModelComponentInfo.java` — moved to `eyelib-attachment/.../capability/ModelComponentInfo.java`

**What remains in root (intentionally):**
- `EyelibAttachableData.java` — Forge registry bootstrap hub; must stay in root
- `RenderData.java` — Runtime per-entity render state; must stay in root
- `EntityBehaviorData.java` — Runtime behavior state with custom codecs
- `ItemInHandRenderData.java` — Runtime item render state
- `component/AnimationComponent.java` — Runtime animation binding state
- `component/ModelComponent.java` — Runtime model binding state
- `component/ClientEntityComponent.java` — Runtime client entity state
- `component/RenderControllerComponent.java` — Runtime render controller state

**CAP-01 Integration Points:**
1. `EyelibAttachableData` → `DataAttachmentType<ExtraEntityData>` (attachment type as generic parameter)
2. `EyelibAttachableData` → `DataAttachmentType<ExtraEntityUpdateData>` (attachment type as generic parameter)
3. `EyelibAttachableData` → `DataAttachmentType<EntityStatistics>` (attachment type as generic parameter)
4. `RenderData.codec()` → `ModelComponentInfo.CODEC` (attachment codec in mojang serialization)
5. `RenderData.codec()` → `AnimationComponentInfo.CODEC` (attachment codec in mojang serialization)

**Verification:** Confirm no root `capability/*.java` source file is a "data/codec-only" type that belongs in attachment. The split criterion: data/codec-only → attachment; runtime owner/registry → root.

**Residual concern:** `EntityBehaviorData.java` is in root with CODEC/STREAM_CODEC but also has runtime behavior — needs audit whether the codec portion should be extracted to attachment.

### ANIM-01: Clean Invalid Animation Interfaces

**Current state of `client/animation/` (22 items + bedrock/):**
- 14 top-level files (active): Animation, AnimationApplier, AnimationClipDefinition, AnimationChannelDefinition, AnimationDefinition, AnimationEffect, AnimationEffects, AnimationExecutionPort, AnimationIdentityPort, AnimationKeyframeDefinition, AnimationLookup, AnimationRuntimePortSet, AnimationRuntimes, AnimationStatePort, BrAnimator, LegacyAnimationRuntimeAdapter, NamedTrackContainerDefinition, NamedTrackDefinition, RuntimeParticlePlayData, StateMachineAnimation, StateMachineAnimationDefinition, TrackAnimationDefinition
- 1 deleted: KeyFrame.java (v1.4)
- bedrock/ (active runtime): BrAnimation, BrAnimationChannel, BrAnimationEntry, BrAnimationPlaybackState, BrBoneAnimation, BrBoneAnimationSampler, BrBoneKeyFrame, BrClipExecutor, BrClipStateOwner, BrEffectsKeyFrameDefinition (+ definition types)
- bedrock/controller/ (active runtime): BrAnimationController, BrAnimationControllers, BrControllerExecutor, BrControllerStateOwner

**Dead code audit approach (per ANIM-01):**
1. For each file in `client/animation/`, run IDE "Find References" — flag any with zero usages outside test files
2. For each file in `bedrock/` and `bedrock/controller/`, verify it's referenced by `BrAnimator`, `EntityRenderSystem`, `AnimationComponent`, or manager loaders
3. `KeyFrame.java` already deleted — verify no stale `.class` in bin/ (clean build required)

**Active integration points (DO NOT DELETE):**
- `BrAnimator.tickAnimation()` — called by `EntityRenderSystem` → `AnimationComponent`
- `BrAnimationEntry.fromSchema(BrAnimationEntrySchema)` — called by `BrAnimation.fromSchemaSet(BrAnimationSet)` → loader path
- `BrAnimationController.fromSchema(name, BrAnimationControllerSchema)` — called by `BrAnimationControllers` → loader path
- `BrBoneAnimation.fromSchema(BrBoneAnimationSchema)` — called by `BrAnimationEntry` codec
- `BrBoneKeyFrame.fromSchema(timestamp, BrBoneKeyFrameSchema)` — called by `BrBoneAnimation.toImmutableMap()`
- All `*Definition` types — runtime compiled/optimized forms of schema types, actively used by samplers/executors

### PREP-01: Scan for Code Belonging in Preprocessing

**Already correctly in preprocessing:**
| File | Consumer(s) in Root | Integration Point |
|------|-------------------|-------------------|
| `LoaderParsingOps.java` | BrAnimationLoader, BrMaterialLoader, BrRenderControllerLoader, BrClientEntityLoader, BrAttachableLoader, BrAnimationControllerLoader | Import + method call |
| `ManagerResourceReloadPlan.java` | ManagerResourceImportPlanner | Import + method call |
| `ManagerResourceBatchPlanner.java` | ManagerResourceImportPlanner | Import + method call |
| `BoneAnimationBaker.java` | BrBoneAnimation, BrBoneKeyFrame | Import + method call |
| `BakedBoneKeyFrame.java` | BrBoneAnimation, BrBoneKeyFrame via BoneAnimationBaker | Return type |
| `BakedModel.java` / `BakedModels.java` / `*BakeInfo.java` | Root model pipeline | Import + usage |
| `ParticleFlipbookSummary.java` / `Ops.java` | Root particle pipeline | Import + usage |

**Scan approach (per PREP-01):**
1. Search root `src/main/java/` for any class that imports `com.mojang.serialization.Codec` and `com.google.gson.JsonElement` in the same file — these are parser candidates that might belong in preprocessing
2. Search for any file in root `client/loader/` or `client/gui/manager/` that contains `Codec.parse(JsonOps.INSTANCE, ...)` patterns — check if they duplicate `LoaderParsingOps` logic
3. Search root for any `TreeMap<Float, ...>` based "baking" loops — check if they duplicate `BoneAnimationBaker` patterns

### DUP-01: Duplicate Code Detection

**Known near-duplicates to verify:**
1. **Definition vs Schema types:** Root bedrock `Br*Definition` types (e.g., `BrBoneAnimationDefinition`, `BrAnimationEntryDefinition`, `BrBoneKeyFrameDefinition`) are NOT duplicates of importer `Br*Schema` types — they are runtime-compiled adaptations with additional pre-computed state (e.g., `ImmutableFloatTreeMap`, sorted keys). **Focus DUP-01 on accidental copies, not intentional adaptation layers.**

2. **Controller types:** Root `bedrock/controller/BrAnimationController` uses importer's `BrAnimationControllerDefinition` (which wraps importer's `BrAcStateDefinition` which wraps importer's `BrAcState`). This is a proper three-layer architecture: importer raw schema → importer parsed definition → root runtime controller. No duplication.

3. **Potential candidates for DUP-01:**
   - Search for classes with identical method signatures across root and submodules
   - Search for copy-paste codec patterns (same RecordCodecBuilder fields in different classes)
   - Focus on root `capability/` vs attachment `capability/` for leftover data type shadows

### DOCS-01: README Audit

**READMES checked and status:**
| README | Status | Action |
|--------|--------|--------|
| `client/animation/README.md` | CURRENT | References v1.4 topology, Stage 1-5 split description |
| `client/particle/README.md` | CURRENT | Phase 10-14 ownership accurately documented |
| `util/README.md` | CURRENT | Accurately reports "no Java source remains after Phase 19" |
| `mixin/README.md` | LEGACY POINTER | Intentionally kept as compatibility pointer → `mc/impl/mixin/` |
| `eyelib-preprocessing/.../README.md` | CURRENT | Responsibilities, boundary rules, consumers documented |
| `molang/grammer/README.md` | LEGACY MARKER | Intentionally kept per AGENTS.md |
| All `mc/impl/*/README.md` | CURRENT | Accurately documents current ownership |

**DOCS-01 focus:** Verify no README exists in a drained/deleted directory. Check `capability/README.md` (nonexistent — OK). Check if any README references `eyelib-processor` (old name) instead of `eyelib-preprocessing` (new name).

## Build Order Considerations

For cleanup phases, order matters to prevent broken compilation:

```
Phase A: DOCS-01 (no compilation impact)
    ↓
Phase B: ANIM-01 (delete dead code, run find-references first)
    ↓
Phase C: DUP-01 (search and report, no deletions until verified)
    ↓
Phase D: PREP-01 (scan and report, may trigger moves)
    ↓
Phase E: CAP-01 (final migration audit, may involve moves)
```

**Rationale:** Documentation changes first (zero risk). Dead code deletion next (if references verified). Duplicate scan third (read-only analysis). Preprocessing scan fourth (may identify moves). Capability audit last (most complex, may involve files touched by ANIM-01).

**Critical gate:** After each phase, run `jetbrain_build_project` to verify compilation. Run `nullawayMain` for null-safety.

## Sources

- Codebase analysis via JetBrains IDE MCP tools (references, glob patterns, text search)
- `MODULES.md` — canonical module inventory
- `docs/architecture/01-module-boundaries.md` — boundary rules and ownership map
- `docs/architecture/02-side-boundaries.md` — side constraints
- `.planning/PROJECT.md` — milestone requirements and v1.4 state
- `build.gradle` and `settings.gradle` — dependency declarations
- All confidence: HIGH (verified against IDE reference analysis, source code inspection, and architecture docs)

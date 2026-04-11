# common-runtime

## Scope
- Shared behavior logic, commands, and selected custom events.
- Main paths: `common/`, `mc/impl/common/command/`, selected `event/`

## Why it is MC-facing
- Mixes side-neutral logic with MC entity/event/command types.

## Final isolation status
- Hard-import slice status: advanced (command hotspot quarantined).
- Final `mc/api + mc/impl` isolation status: pending re-baseline.
- Expected final state for this module: pure deterministic update logic may live outside `mc/impl`, but Forge subscribers, commands, packet sends, goal inspection, and Minecraft-backed event/entity logic must be isolated to allowed `mc/impl` packages.

## Target seam
- Extract pure behavior/state logic where possible.
- Keep event subscribers and command registration in `mc/impl`.

## Implemented seam design
- Extracted pure update helpers under `common/runtime`:
  - `EntityStatisticsUpdater` for deterministic horizontal-distance accumulation.
  - `ExtraEntityDataUpdater` for deterministic `ExtraEntityData` flag updates from precomputed goal-observation booleans.
- Extracted deterministic command seam under `common/runtime`:
  - `ParticleCommandRuntime` for suggestion filtering, spawn-request shaping, and success-message formatting with platform-type-free types (`String`, primitives, `List`, `Predicate`, `Supplier`).
- Kept Forge/Minecraft runtime collection and wiring in mc-facing handlers:
  - `EntityStatisticsHandler` still owns `LivingTickEvent` access, attachment read/write, and tracked sync trigger.
  - `EntityExtraDataHandler` still owns Forge event subscription, mob-goal inspection (`WrappedGoal`, `RangedAttackGoal`, `AvoidEntityGoal`, `EatBlockGoal`), attachment write, and packet send.
- Quarantined concentrated command runtime wiring to allowed MC-owned location:
  - `mc/impl/common/command/EyelibParticleCommand` now owns Forge `RegisterCommandsEvent` subscription, Brigadier tree wiring, `ResourceLocation` parsing/validation, `ServerPlayer` access, and packet dispatch.

## Explicit MC-bound keep list (remaining)
- Forge subscribers and event classes.
- Packet sends / tracked sync triggers.
- Mob-goal inspection logic source (`goalSelector` / goal classes).

## Deliverables
- Design ports around behavior/runtime hooks.
- Add tests for extracted pure logic.
- Implement split and document what remains MC-bound.

## Dependencies
- After `capability-dataattach`, `network-sync`, and `client-model-animation-entity`.

## Subagent assignment
- Category: `deep`
- Verification: JetBrains MCP build/tests only.

## Checklist
- [x] Interface design
- [x] Tests
- [x] Implementation
- [x] JetBrains MCP verification

## Final isolation checklist
- [ ] Re-baseline remaining Minecraft/Forge references for this module.
- [ ] Confine Forge subscribers, commands, packet sends, and Minecraft-backed event/entity logic to allowed `mc/impl` packages.
- [ ] Keep deterministic runtime helpers free of Minecraft/Forge types.
- [ ] Re-run JetBrains MCP verification for the final package layout.
- [ ] Pass rule-based boundary scan for this module.

## Re-baseline notes for final isolation
- `EntityExtraDataHandler` and `EntityStatisticsHandler` still directly depend on Forge subscribers, Minecraft entity/event types, mob-goal inspection, and packet send paths; they remain legacy MC-facing package residents until moved under allowed `mc/impl` ownership.
- The remaining gap is broader than event wiring: `common/behavior/**` still contains Minecraft-bound definition/filter types such as `BehaviorEntity` and `ComponentGroup` (`ResourceLocation`) plus filter enums implementing `StringRepresentable`, so this area is not yet platform-type-free.
- The concentrated command hotspot is now quarantined in `mc/impl/common/command/EyelibParticleCommand`; remaining follow-up is to keep new command-side deterministic behavior in platform-type-free runtime seams and avoid reintroducing MC/Forge imports under legacy `command/**`.
- More specifically, `common/behavior` is still tied to Minecraft serialization conventions at the schema level: `BehaviorEntity` and `component/group/ComponentGroup` depend on `ResourceLocation`, while `event/filter/Subject` and `event/filter/Operator` directly implement `StringRepresentable`. Final isolation must either replace those schema contracts with platform-free ids/enums or quarantine the whole behavior-schema surface into Minecraft-owned code.
- This module therefore needs two end-state tracks: move handler/runtime wiring into `mc/impl`, and separately redesign any surviving behavior definition/filter contracts so they no longer expose Minecraft types outside `mc/impl`.
- Local package guidance now exists at `src/main/java/io/github/tt432/eyelib/common/README.md` and `src/main/java/io/github/tt432/eyelib/common/behavior/README.md`; keep both aligned with any later schema/runtime quarantine decisions.

## Verification (JetBrains MCP only)
- `test --tests io.github.tt432.eyelib.common.runtime.ParticleCommandRuntimeTest --tests io.github.tt432.eyelib.common.runtime.CommonRuntimeUpdaterTest` ✅
- `build` ✅

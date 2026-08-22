# ADR-0007: Known Pitfalls & Anti-Patterns

**Status:** Accepted  
**Context:** During hands-on module separation refactoring work, several recurring anti-patterns were discovered that caused wasted effort or breakage.  
**Decision:** Document 10 known pitfalls with symptoms, root causes, and prevention strategies. Organize by severity: Critical, Moderate, Minor.  
**Consequences:** Contributors and AI assistants have a reference to avoid common mistakes during refactoring.

---

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

**Consequences:** Moving it would break Forge bootstrap—it depends on root's `Eyelib.MOD_ID`, registers to the MOD event bus, and is wired via `Eyelib.java`.

**Detection:** Contains `@Mod.EventBusSubscriber`, `DeferredRegister`, `RegistryObject`, `IForgeRegistry` → root-only concerns.

## Moderate

### 4. Extracting EntityBehaviorData Codec Mechanically

**Symptom:** Seeing `CODEC` and `STREAM_CODEC` static fields and assuming the entire codec belongs in attachment.

**Why:** The codec serializes behavior-specific fields coupled to `MolangQuery` variant/markVariant lookups.

**Prevention:** If the codec's `RecordCodecBuilder` references root types or root behavior methods → cannot extract.

### 5. Moving Root-Dependent Code to Preprocessing

**Symptom:** PREP-01 finds parse/bake patterns in root and moves them to preprocessing unconditionally.

**Prevention:** Verify imports. If the file imports `io.github.tt432.eyelib.capability` → cannot move to preprocessing.

### 6. Stale `.class` Files from Deleted Sources

**Symptom:** Deleted source files leave `.class` files in `bin/`, causing IDE reference false positives.

**Prevention:** Run `clean build` before deletion phases to ensure `bin/` matches `src/`.

### 7. Uncaught Exceptions in NeoForge Render-Loop Event Listeners Propagate

**Symptom:** An NPE in a third-party `RenderFrameEvent.Pre` listener (motions4mod `CameraManager`, crash export 2026-08-18) propagated through `EventBus.post` (bus 8.0.5, NeoForge 21.1.x) and crashed the client instead of being logged and skipped.

**Why:** NeoForge bus 8.x does not swallow listener exceptions on render-loop events. Listeners ordered after the throwing one silently skip that frame; when the throw repeats every frame (e.g., a null-key registry lookup during the empty-snapshot window of an F3+T reload), per-frame subsystems such as eyelib's `ParticleRenderHooks` recycling stall before the eventual crash.

**Prevention:** Never let a per-frame listener throw on expected runtime states (null lookups, empty registries); registry getters must tolerate null keys (see `RegistrySnapshot.get`). When diagnosing "recycling/render hook stopped", check the log for exceptions thrown by other listeners of the same event first.

## Minor

### 8. Registry 批量写入事件断档导致 F3+T 资源不重载

**Symptom:** F3+T 后模型几何、在场实体动画、addon 纹理保持旧值；Manager 单文件热更却正常。

**Why:** `Registry` 三档写入的事件产出不对称——`put` 发 `ManagerEntryChangedEvent`（有订阅者：dfsModels/烘焙缓存/AnimationComponent），`putAll`/`removeAll` 发 `ManagerReplacedEvent`（曾零订阅者），`replaceAll` 曾完全无事件。重载路径全部走批量写入（Br loader `replaceAll`、addon 桥 `putAll`），而失效钩子只订了逐条事件。纹理侧另有 vanilla 坑：`TextureManager` 重载对 `DynamicTexture` 是 no-op（`DynamicTexture.load` 空实现，1.20.1 反编译实证），且 `TextureManagerMixin` 在 byPath 命中时短路，addon 基图 GPU 副本永不更新；`clamped/`/`_color_mask/` 派生与 `COLOR_MASK_CACHE` 从陈旧基图回读，连锁陈旧。

**Prevention:** 给 `Registry` 新增/修改写入路径时对照失效矩阵：逐条事件、批量事件、generation 三通道各自的订阅者是谁。批量路径的消费者订阅 `ManagerReplacedEventPublisher`（按 managerName 过滤整体失效）；自建 `DynamicTexture` 必须在内容源更换时主动驱逐 byPath 条目（`EyelibTextureManagerAccess.eyelib$evictTextures`），不能指望 vanilla 重载。

### 9. Stale Module Name in README

**Symptom:** README still references `eyelib-processor` (old name before v1.4 rename).

**Prevention:** Full-text search for `eyelib-processor` (not containing `eyelib-preprocessing`) across all documentation.

### 10. Deleting Legacy Compatibility Pointer README

**Symptom:** `mixin/README.md` looks short and appears to be "empty/obsolete."

**Why:** It is an intentional legacy compatibility pointer directing readers to the new location.

**Detection:** Readme contains "legacy", "compatibility pointer", or "phased out" keywords.

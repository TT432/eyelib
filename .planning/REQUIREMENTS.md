# Requirements: Eyelib Module Separation

**Defined:** 2026-05-10
**Milestone:** v1.3 分离 eyelib-util 模块
**Core Value:** Eyelib 的功能模块必须能被独立理解、构建、验证和消费；工具代码共享必须形成清晰 Gradle 模块边界，消除 root util 包集群和子模块间重复的共享代码。

## v1.3 Requirements

Requirements for this milestone. Each maps to exactly one roadmap phase.

### Module Infrastructure

- [ ] **MOD-01**: Maintainer can build `:eyelib-util` as a standalone Forge Gradle subproject (build.gradle + mods.toml + settings.gradle) with zero `project()` dependencies.
- [ ] **MOD-02**: Module documentation states ownership, dependency direction, package namespace `io.github.tt432.eyelibutil`, and allowed integration layers.

### Pre-Migration Audit

- [ ] **AUDIT-01**: Every root/util/* and core/util/* file has a destination routing decision (eyelib-util / functional owner / delete) based on consumer count classification (0/1/N rule).
- [ ] **AUDIT-02**: All wildcard imports (`import io.github.tt432.eyelib.util.*`) in root are replaced with explicit imports.

### Single-Consumer Routing

- [ ] **ROUTE-01**: Single-consumer utility classes are moved to their functional owner (AnimationApplier → client/animation, Models → client/model, ModBridgeServer/BBModelSink → mc/impl/modbridge).
- [ ] **ROUTE-02**: Compatibility shims (ListHelper, EitherHelper) are deleted after their consumers migrate to canonical implementations.

### Code Migration

- [ ] **MIGR-01**: Zero-dependency utility categories (time, color, loader, math, search — 11 files) are migrated into `:eyelib-util` with updated root import sites.
- [ ] **MIGR-02**: Collection utilities (Blackboard, Lists, Collectors, EntryStreams) are migrated into `:eyelib-util`.
- [ ] **MIGR-03**: Resource and texture utilities (ResourceLocations, TexturePaths, TexturePathHelper) are migrated, with ResourceLocations.mod() circular reference resolved.
- [ ] **MIGR-04**: Codec infrastructure (9 codec files + ImmutableFloatTreeMap) is migrated as an atomic unit into `:eyelib-util`.

### Submodule Centralization

- [ ] **CENT-01**: eyelib-attachment's StreamCodec suite (5 files: DualStreamCodec, StreamCodec, StreamEncoder, StreamDecoder, EyelibStreamCodecs) is centralized into `:eyelib-util`.
- [ ] **CENT-02**: eyelib-material's duplicate DispatchedMapCodec is deduplicated and consumers are redirected to the `:eyelib-util` canonical version.

### Verification

- [ ] **VERIFY-01**: root/util/* and core/util/* directories are empty after extraction.
- [ ] **VERIFY-02**: All modules compile (JetBrains MCP Gradle) with zero residual `io.github.tt432.eyelib.util.*` imports and zero behavioral regression.
- [ ] **VERIFY-03**: MODULES.md, architecture docs, and package README files reflect the new module topology.

## Future Requirements

Deferred to future milestones; not required for v1.3 completion.

### Extended Submodule Centralization

- **CENT-F01**: Additional submodule-duplicated code beyond StreamCodec and DispatchedMapCodec may be centralized after v1.3 baseline is stable.
- **CENT-F02**: Dependency scope audit may narrow root connection from broad `api` wiring to `implementation` for internal-only consumers.

### SharedLibraryLoader Audit

- **AUDT-F01**: Native library loading path validation after class relocation; consumer count audit; decision on keep vs. delete vs. repackage.

## Out of Scope

Explicitly excluded from this milestone.

| Feature | Reason |
|---------|--------|
| Rewriting or replacing utility implementations | Pure ownership transfer; no behavioral changes |
| Adding new utility features | Scope is existing code, not new capabilities |
| Removing MC/Forge dependency from eyelib-util | MC-dependent utilities (EyelibCodec, ResourceLocations, Shapes) are valid module members |
| Build system innovation | Clone existing Forge subproject pattern; no new Gradle conventions |
| Cosmetic package rename without ownership transfer | Fails the user's goal of true module separation |
| Deleting or weakening tests to make migration compile | Violates zero behavior regression requirement |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUDIT-01 | 15 | Pending |
| AUDIT-02 | 15 | Pending |
| ROUTE-01 | 15 | Pending |
| ROUTE-02 | 15 | Pending |
| MOD-01 | 16 | Pending |
| MOD-02 | 16 | Pending |
| MIGR-01 | 17 | Pending |
| MIGR-02 | 17 | Pending |
| MIGR-03 | 18 | Pending |
| MIGR-04 | 19 | Pending |
| CENT-01 | 20 | Pending |
| CENT-02 | 20 | Pending |
| VERIFY-01 | 21 | Pending |
| VERIFY-02 | 21 | Pending |
| VERIFY-03 | 21 | Pending |

**Coverage:**
- v1.3 requirements: 15 total
- Mapped to phases: 15
- Unmapped: 0 ✓

---
*Requirements defined: 2026-05-10*
*Last updated: 2026-05-10 after initial definition*

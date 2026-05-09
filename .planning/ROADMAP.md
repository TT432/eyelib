# Roadmap: Eyelib Module Separation

## Milestones

- ✅ **v1.0** — Phases 1-4 (shipped 2026-05-07)
- ✅ **v1.1 ClientSmoke 全自动化** — Phases 5-7 (shipped 2026-05-08)
- 🚧 **v1.2 真正实现 eyelib-particle 的模块分离** — Phases 8-14 (in progress)

## Phases

<details>
<summary>✅ v1.0 — Phases 1-4 (SHIPPED 2026-05-07)</summary>

Full details: `.planning/milestones/v1.0-ROADMAP.md`

- [x] **Phase 1: Module Scaffolding + Config + Annotation Discovery**
- [x] **Phase 2: State Machine + World Lifecycle + Stabilization**
- [x] **Phase 3: Screenshot Capture + Auto-Exit**
- [x] **Phase 4: Test Execution + Report Generation**

</details>

<details>
<summary>✅ v1.1 ClientSmoke 全自动化 — Phases 5-7 (SHIPPED 2026-05-08)</summary>

Full details: `.planning/milestones/v1.1-ROADMAP.md`

**Milestone Goal:** `./gradlew runClientSmoke` 一键启动全流程，零手动配置

- [x] **Phase 5: Gradle Run Configuration & Classpath** (1/1 plan) — completed 2026-05-08
- [x] **Phase 6: Config Override Bridge & State Machine Fixes** (2/2 plans) — completed 2026-05-08
- [x] **Phase 7: Verification & Polish** (2/2 plans) — completed 2026-05-08

</details>

### 🚧 v1.2 真正实现 eyelib-particle 的模块分离 (In Progress)

**Milestone Goal:** 将粒子相关能力从 root runtime 的混合包结构中提升为清晰的 `:eyelib-particle` Gradle 模块边界，同时保持现有粒子加载、命令、网络 spawn/remove、渲染行为零回归。

- [x] **Phase 8: Boundary Contract & Gradle Module Skeleton** - `:eyelib-particle` 成为可构建、可消费、方向明确的 Gradle 模块。 (2/2 plans) — completed 2026-05-09
- [x] **Phase 9: Particle API & Store Seam** - root 通过粒子模块的窄 API 使用 lookup、spawn/remove、store/publication 与初始化能力。 (3/3 plans) — completed 2026-05-09
- [x] **Phase 10: Schema/Runtime Ownership & Adapter** - importer/raw schema 与 executable runtime definition 的所有权和转换契约被锁定。 (completed 2026-05-09)
- [ ] **Phase 11: Runtime Client Core Extraction** - 粒子运行时、发射器、渲染管理与 client hook 迁入粒子模块且保持 side-safe。
- [ ] **Phase 12: Loading & Publication Rewire** - 资源重载、registry 替换、description identifier 发布语义在模块边界后保持不变。
- [ ] **Phase 13: Command & Network Integration Rewire** - `/eyelib particle` 与 spawn/remove packets 保持用户行为兼容，并通过显式平台适配层进入粒子服务。
- [ ] **Phase 14: Verification & Documentation Gate** - 测试、JetBrains MCP Gradle 检查、适用的自动 ClientSmoke 流程与必要硬件检查清单证明模块拆分无回归。

## Phase Details

### Phase 8: Boundary Contract & Gradle Module Skeleton
**Goal**: Maintainer can build and understand `:eyelib-particle` as a real Gradle module with explicit ownership and one-way root → particle dependency direction.
**Depends on**: Phase 7
**Requirements**: PGRAD-01, PGRAD-02, PAPI-02
**Success Criteria** (what must be TRUE):
  1. Maintainer can see `:eyelib-particle` included as a first-class Gradle subproject with its own build metadata, source sets, resources, and root dependency wiring.
  2. Maintainer can read module documentation that states particle ownership, dependency direction, allowed integration layers, and the rule that pure particle core remains free of root/MC/Forge contamination.
  3. Root runtime can depend on the particle module, while `:eyelib-particle` has no dependency on root runtime packages, root managers, root registries, root packets, root capability helpers, or root `mc/impl` classes.
  4. Later verification is documented to use JetBrains MCP Gradle tasks only; no shell Gradle command is required or endorsed.
**Plans**: 2/2 complete — `.planning/phases/08-boundary-contract-gradle-module-skeleton/08-01-PLAN.md`, `.planning/phases/08-boundary-contract-gradle-module-skeleton/08-02-PLAN.md`

### Phase 9: Particle API & Store Seam
**Goal**: Root runtime can use particle capabilities through narrow module-owned APIs instead of owning particle internals directly.
**Depends on**: Phase 8
**Requirements**: PAPI-01, PAPI-03
**Success Criteria** (what must be TRUE):
  1. Root runtime can access particle lookup, spawn/remove, store/publication, and initialization behavior through particle-module API seams.
  2. Any root compatibility facade delegates to particle-module APIs instead of containing particle business logic.
  3. Maintainer can identify every temporary compatibility facade and read why it exists and when it can be removed.
**Plans**: 3/3 complete — `.planning/phases/09-particle-api-store-seam/09-01-PLAN.md`, `.planning/phases/09-particle-api-store-seam/09-02-PLAN.md`, `.planning/phases/09-particle-api-store-seam/09-03-PLAN.md`

### Phase 10: Schema/Runtime Ownership & Adapter
**Goal**: Importer/raw particle schema and executable runtime particle definitions have explicit canonical owners and a tested conversion seam.
**Depends on**: Phase 9
**Requirements**: PSCHEMA-01, PSCHEMA-02, PSCHEMA-03
**Success Criteria** (what must be TRUE):
  1. Maintainer can identify the canonical owner for importer/raw particle schema and the canonical owner for runtime executable particle definitions.
  2. Runtime particle definitions can be created from importer/raw schema through a named adapter or equivalent explicit conversion seam.
  3. Codec/schema behavior and runtime conversion expectations are covered by tests or documented invariants so duplicate `BrParticle` ownership cannot drift silently.
  4. The adapter preserves parity-critical particle fields needed by loading, rendering, Molang, lifetime, and remove behavior.
**Plans**: 1/2 complete — `.planning/phases/10-schema-runtime-ownership-adapter/10-01-PLAN.md`, `.planning/phases/10-schema-runtime-ownership-adapter/10-02-PLAN.md`

### Phase 11: Runtime Client Core Extraction
**Goal**: Existing particle runtime and client rendering behavior lives under `:eyelib-particle` without weakening side boundaries or behavior.
**Depends on**: Phase 10
**Requirements**: PRENDER-01, PRENDER-02
**Success Criteria** (what must be TRUE):
  1. Existing client particle emitter, render manager, material/texture resolution, Molang scope, lifetime, remove semantics, tick/render lifecycle, and logout cleanup behavior are preserved after extraction.
  2. Particle-specific client hooks and Forge bindings live in explicit particle integration layers when appropriate, not in pure particle core/API packages.
  3. Pure particle core remains clean of platform bindings, while platform-specific bindings are side-safe and do not introduce dedicated-server classloading regressions.
  4. Maintainer can follow dependency direction from root integration code into particle runtime without finding a reverse dependency back to root.
**Plans**: 1/6 complete — `.planning/phases/11-runtime-client-core-extraction/11-01-PLAN.md`, `.planning/phases/11-runtime-client-core-extraction/11-02-PLAN.md`, `.planning/phases/11-runtime-client-core-extraction/11-03-PLAN.md`, `.planning/phases/11-runtime-client-core-extraction/11-04-PLAN.md`, `.planning/phases/11-runtime-client-core-extraction/11-05-PLAN.md`, `.planning/phases/11-runtime-client-core-extraction/11-06-PLAN.md`

### Phase 12: Loading & Publication Rewire
**Goal**: Particle resource reload and publication semantics move behind the module boundary without changing observable registry behavior.
**Depends on**: Phase 11
**Requirements**: PLOAD-01, PLOAD-02, PLOAD-03
**Success Criteria** (what must be TRUE):
  1. Resource reload still parses `particles/*.json` and replaces the active particle registry with the same observable reload behavior.
  2. Particle publication continues to key entries by `particle_effect.description.identifier`, not by JSON resource path or other incidental source keys.
  3. Loader, registry, and manager responsibilities are owned by the particle module or by explicit root adapters without reintroducing root-owned particle internals.
  4. Maintainer can trace the reload path from root/Forge integration into particle-module registry publication without hidden ownership duplication.
**Plans**: TBD

### Phase 13: Command & Network Integration Rewire
**Goal**: User-facing particle command and network spawn/remove behavior remain compatible while platform concerns stay in explicit adapters.
**Depends on**: Phase 12
**Requirements**: PNET-01, PNET-02, PNET-03
**Success Criteria** (what must be TRUE):
  1. User can run `/eyelib particle` with the same syntax, suggestions, validation, spawn position behavior, and success message as before extraction.
  2. Spawn/remove packet behavior remains string-keyed and delegates from network handlers into particle services without exposing render internals.
  3. Platform-specific command, player, packet channel, and identifier validation concerns stay in explicit integration adapters.
  4. Pure particle core APIs remain root-independent and platform-light even though platform bindings may live in an appropriate particle or root integration layer.
**Plans**: TBD

### Phase 14: Verification & Documentation Gate
**Goal**: Maintainer can prove the particle module split preserves behavior and leaves the documented architecture consistent.
**Depends on**: Phase 13
**Requirements**: PVERIFY-01, PVERIFY-02
**Success Criteria** (what must be TRUE):
  1. Existing particle-related tests are moved or adapted without weakening assertions.
  2. New boundary, parity, and regression tests cover the module split, including dependency direction, schema/runtime conversion, reload keys, command/network delegation, and side boundaries.
  3. Maintainer can run the planned compile/test checks through JetBrains MCP Gradle tasks only, use automated ClientSmoke flow where applicable, and keep hardware/manual checks separate for runtime behavior that cannot be automatically asserted.
  4. Module, architecture, side-boundary, repo-map, and particle README documentation all match the final ownership and integration boundaries.
**Plans**: TBD

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Module Scaffolding | v1.0 | 5/5 | Complete | 2026-05-07 |
| 2. State Machine + World Lifecycle | v1.0 | 2/2 | Complete | 2026-05-07 |
| 3. Screenshot Capture + Auto-Exit | v1.0 | 2/2 | Complete | 2026-05-07 |
| 4. Test Execution + Report Generation | v1.0 | 1/1 | Complete | 2026-05-07 |
| 5. Gradle Run Configuration & Classpath | v1.1 | 1/1 | Complete | 2026-05-08 |
| 6. Config Override Bridge & State Machine Fixes | v1.1 | 2/2 | Complete | 2026-05-08 |
| 7. Verification & Polish | v1.1 | 2/2 | Complete | 2026-05-08 |
| 8. Boundary Contract & Gradle Module Skeleton | v1.2 | 2/2 | Complete | 2026-05-09 |
| 9. Particle API & Store Seam | v1.2 | 3/3 | Complete | 2026-05-09 |
| 10. Schema/Runtime Ownership & Adapter | v1.2 | 2/2 | Complete   | 2026-05-09 |
| 11. Runtime Client Core Extraction | v1.2 | 3/6 | In Progress|  |
| 12. Loading & Publication Rewire | v1.2 | 0/TBD | Not started | - |
| 13. Command & Network Integration Rewire | v1.2 | 0/TBD | Not started | - |
| 14. Verification & Documentation Gate | v1.2 | 0/TBD | Not started | - |

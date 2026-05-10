# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.3 — 分离 eyelib-util 模块

**Shipped:** 2026-05-10
**Phases:** 7 | **Plans:** 24 | **Requirements:** 15

### What Was Built

- `:eyelib-util` became a Forge leaf module with `eyelibutil` mod identity and `io.github.tt432.eyelibutil` package namespace.
- root/core utility ownership moved into util categories for time, color, loader, math, search, collection, resource, texture, codec, and streamcodec.
- Single-consumer utility classes moved to functional owners rather than becoming shared utility API.
- Attachment stream codec helpers and material `DispatchedMapCodec` duplication were centralized through explicit `:eyelib-util` dependencies.
- Final audit passed with 15/15 requirements, 7/7 phases, 13/13 integration checks, and 4/4 E2E flows.

### What Worked

- Routing manifest first, migration second kept destination decisions auditable before source moves.
- Distinct `io.github.tt432.eyelibutil` namespace avoided split-package ambiguity with root runtime packages.
- Atomic codec migration reduced half-migrated serialization states and kept dependent animation/behavior callers coherent.
- Final audit caught stale README topology and incomplete validation metadata before archive.

### What Was Inefficient

- Several summaries lacked `requirements-completed` frontmatter until milestone audit closure.
- Phase 16 validation status drifted between frontmatter and task/checklist rows, requiring a second audit loop.
- Large uncommitted worktree made archive commit scoping more expensive than if implementation commits had landed per phase.

### Patterns Established

- Shared helpers belong in a leaf utility module only when they are genuinely cross-cutting.
- Domain-specific helpers should move to functional owners, even if they originated under `util/*`.
- Package README files must be checked against final code topology, not only architecture docs.

### Key Lessons

1. Keep SUMMARY `requirements-completed` frontmatter mandatory for every plan that closes a requirement.
2. Validation documents need internal consistency: frontmatter, task rows, and Wave 0 checklist must agree.
3. Milestone audits are useful before archiving because documentation drift can survive successful builds.

### Cost Observations

- Model mix: not tracked.
- Sessions: not tracked.
- Notable: late audit closure added documentation reconciliation work but avoided archiving stale topology claims.

---

## Milestone: v1.2 — 真正实现 eyelib-particle 的模块分离

**Shipped:** 2026-05-09
**Phases:** 7 | **Plans:** 22 | **Requirements:** 18

### What Was Built

- `:eyelib-particle` became the canonical particle Gradle module boundary.
- Particle lookup, runtime definitions, store/publication, command/network packet contracts, and client runtime integration moved behind particle-owned APIs or explicit root/MC adapters.
- Importer/raw particle schema ownership and executable runtime definition ownership were split through named adapter seams.
- Final verification evidence documented boundary tests, MCP verification matrix rows, hardware/manual checks, and non-blocking future packaging items.

### What Worked

- Phase-by-phase extraction kept high-risk runtime, loading, command, and network changes independently verifiable.
- The final documentation gate made ownership decisions discoverable in `MODULES.md`, architecture docs, package README files, and tests.
- Using `ParticleDefinition.identifier()` as the active key clarified reload/publication semantics and avoided source-path leakage.

### What Was Inefficient

- v1.2 archival happened after v1.3 planning had already started, so requirements and audit evidence had to be reconstructed from git history.
- Some phase artifacts were no longer present in the active worktree, requiring audit evidence to reference versioned `HEAD:` content.

### Patterns Established

- New modules should use distinct package roots, for example `io.github.tt432.eyelibparticle`, to avoid split-package ambiguity.
- Root should keep platform/resource/transport adapters only; canonical business logic belongs to the feature module or importer owner.
- Manual/hardware checks should be separated from JetBrains MCP Gradle gates rather than blocking source-level verification.

### Key Lessons

1. Archive shipped milestone requirements before starting the next milestone so active `.planning/REQUIREMENTS.md` remains milestone-scoped.
2. Keep final gate artifacts close to the phase evidence; later audits can reconstruct completion without restoring deleted phase directories.
3. Treat future packaging and manual visual proof as explicit tech debt, not implicit blockers.

### Cost Observations

- Model mix: not tracked.
- Sessions: not tracked.
- Notable: late archival increased planning-doc reconciliation cost.

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Sessions | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.0 | N/A | 4 | Established ClientSmoke module and first planning archive pattern. |
| v1.1 | N/A | 3 | Automated ClientSmoke runtime and CI-style reporting. |
| v1.2 | N/A | 7 | Proved feature-module extraction with explicit root adapter boundaries. |
| v1.3 | N/A | 7 | Proved shared utility leaf-module extraction and root/core util drainage. |

### Cumulative Quality

| Milestone | Tests | Coverage | Zero-Dep Additions |
|-----------|-------|----------|-------------------|
| v1.2 | Boundary, parity, command/network, and documentation tests | Requirement coverage 18/18 | `:eyelib-particle` boundary established |
| v1.3 | Static residual scans, module identity/build checks, documentation audit | Requirement coverage 15/15 | `:eyelib-util` leaf module established |

### Top Lessons

1. Module extraction should lock package namespace and dependency direction before moving runtime code.
2. Final verification gates should distinguish automated source checks, MCP build evidence, and manual/hardware evidence.
3. Milestone archives should not proceed until README topology and validation metadata agree with the final source tree.

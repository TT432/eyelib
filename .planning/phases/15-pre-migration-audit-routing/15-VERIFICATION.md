---
phase: 15-pre-migration-audit-routing
verified: 2026-05-10T09:14:24Z
status: passed
score: 4/4 must-haves verified
overrides_applied: 0
re_verification:
  previous_status: passed
  previous_score: 4/4
  gaps_closed: []
  gaps_remaining: []
  regressions: []
  review_fix_revalidation: "Rechecked 15-REVIEW-FIX changes to utility-routing-manifest.md and MODULES.md; success criteria still pass."
---

# Phase 15: Pre-Migration Audit & Routing Verification Report

**Phase Goal:** Every root/util/* and core/util/* source file has a verified consumer count (0/1/N rule) and a committed destination routing decision; single-consumer utility classes are relocated to their functional owners; all wildcard imports to `eyelib.util.*` are replaced with explicit imports.
**Verified:** 2026-05-10T09:14:24Z
**Status:** passed
**Re-verification:** Yes — review-fix revalidation after manifest and `MODULES.md` documentation fixes

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Maintainer can inspect a routing manifest listing every root/util/* and core/util/* file with verified consumer count and target destination. | ✓ VERIFIED | Review-fix revalidation: `docs/architecture/migration/utility-routing-manifest.md` now explicitly separates the 32-root/5-core pre-move baseline from the current 28-root/5-core inventory. Fresh JetBrains file search found 28 current root util Java files and 5 current core util Java files; each current path is represented in the route table, and the four moved classes remain as historical pre-move route evidence with current owner paths. |
| 2 | Util wildcard imports across root source files return zero results. | ✓ VERIFIED | `jetbrain_search_regex` over Java sources with `import\s+io\.github\.tt432\.eyelib\.util(?:\.[A-Za-z0-9_]+)*\.\*;` returned zero matches. A broader `**/*.java` check also returned zero matches. |
| 3 | `AnimationApplier`, `Models`, `ModBridgeServer`, and `BBModelSink` reside in functional owner packages and compile with zero residual old util path references. | ✓ VERIFIED | Destination files exist at `client/animation/AnimationApplier.java`, `client/model/Models.java`, and `mc/impl/modbridge/{ModBridgeServer,BBModelSink}.java` with matching package declarations. Old util paths do not exist. Residual regex for old package references returned zero source matches. IDE diagnostics for all four files reported `problemCount=0`; JetBrains build returned `isSuccess=true`. |
| 4 | `ListHelper` and `EitherHelper` are cataloged with consumer counts and deletion plans tied to later migration phases. | ✓ VERIFIED | Manifest rows list `ListHelper` as one consumer (`BrBoneKeyFrame`) with delete after Phase 17, and `EitherHelper` as codec-internal consumers (`CodecHelper`, `ChinExtraCodecs`) with delete during Phase 19. Indexed text searches confirmed those consumer sets in source. |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `docs/architecture/migration/utility-routing-manifest.md` | Routing manifest with 0/1/N counts, routes, target phases, and shim deletion plan | ✓ VERIFIED | Contains required sections, route table, `ListHelper`, `EitherHelper`, `ResourceLocations.mod()`, and the four moved classes. |
| `src/main/java/io/github/tt432/eyelib/client/animation/AnimationApplier.java` | Functional owner location for AnimationApplier | ✓ VERIFIED | Package is `io.github.tt432.eyelib.client.animation`; IDE diagnostics clean. |
| `src/main/java/io/github/tt432/eyelib/client/model/Models.java` | Functional owner location for Models | ✓ VERIFIED | Package is `io.github.tt432.eyelib.client.model`; IDE diagnostics clean. |
| `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/ModBridgeServer.java` | Functional owner location for ModBridgeServer | ✓ VERIFIED | Package is `io.github.tt432.eyelib.mc.impl.modbridge`; payload bound `50_000_000` and lifecycle guard remain present; IDE diagnostics clean. |
| `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/BBModelSink.java` | Functional owner location for BBModelSink | ✓ VERIFIED | Package is `io.github.tt432.eyelib.mc.impl.modbridge`; `onModelUpdate(String bbmodelJson)` exists; IDE diagnostics clean. |
| `MODULES.md` and `docs/architecture/01-module-boundaries.md` | Documentation reflects actual Phase 15 owner moves | ✓ VERIFIED | Review-fix revalidation confirmed `MODULES.md` now treats legacy `util/modbridge` as historical route evidence and names `mc/impl/modbridge` as current source ownership. `01-module-boundaries.md` states the four roadmap-named helpers moved to functional owners and does not claim they were deleted. |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| Current root/core util inventory | Routing manifest | One row per current Java source file | ✓ WIRED | Review-fix revalidation inventory: 28 root util + 5 core util files. Manifest route table covers current paths and separately labels historical moved-class rows. |
| `BrAnimationEntry.java` | util codec classes | Explicit imports | ✓ WIRED | Wildcard scan is zero; IDE diagnostics clean. |
| `TupleCodec.java` | `Tuple` nested types | Explicit nested imports | ✓ WIRED | `Tuple.*` wildcard removed; IDE diagnostics clean. |
| Old util/client and util/modbridge moved-class paths | Functional owner packages | IDE-aware moves + residual scan | ✓ WIRED | Old path file search returned no files; old-package regex returned zero source matches. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|---|---|---|---|---|
| Phase 15 artifacts | N/A | Static migration/docs/build phase | N/A | N/A — no dynamic rendering/data-flow artifact introduced. |

### Behavioral Spot-Checks

| Behavior | Command/Tool | Result | Status |
|---|---|---|---|
| Full project remains buildable after routing moves | `jetbrain_build_project(projectPath="E:/_ideaProjects/qylEyelib", rebuild=false)` | `isSuccess=true` | ✓ PASS |
| No util wildcard imports remain | `jetbrain_search_regex` util wildcard import pattern | zero matches | ✓ PASS |
| No old moved-class util package references remain | `jetbrain_search_regex` old moved-class package pattern | zero matches | ✓ PASS |
| Review-fix docs still match current inventory and ownership | `jetbrain_search_file` current util/core-util + destination/old-path checks | 28 root util, 5 core util, four destination files present, old moved-class util paths absent | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| AUDIT-01 | 15-01, 15-03, 15-04 | Every root/util and core/util file has route decision based on 0/1/N classification. | ✓ SATISFIED | Manifest route table covers current file inventory and records class/route/phase/evidence. |
| AUDIT-02 | 15-02, 15-04 | All wildcard imports in root are explicit. | ✓ SATISFIED | Util wildcard regex returned zero matches. |
| ROUTE-01 | 15-03, 15-04 | Single-consumer utility classes moved to functional owners. | ✓ SATISFIED | Four roadmap-named classes are at target packages; old util paths absent; build succeeds. |
| ROUTE-02 | 15-01, 15-04 | Compatibility shims cataloged with deletion plans. | ✓ SATISFIED | Manifest deletion plan ties `ListHelper` to Phase 17 and `EitherHelper` to Phase 19. |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---:|---|---|---|
| `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java` | 28, 31, 32 | Existing `TODO` text in Javadoc parameter notes | ℹ️ Info | Not introduced as an implementation stub for Phase 15; does not affect import cleanup/routing goal. |
| `src/main/java/io/github/tt432/eyelib/client/model/Models.java` | 19 | `return null` for empty merge input | ℹ️ Info | Existing domain behavior for `merge(List<Model>)`; not a Phase 15 stub or hollow implementation. |

### Human Verification Required

None. Phase 15 is static source/docs routing plus build verification; automated evidence covers all success criteria.

### Gaps Summary

No blocking gaps found. Review-fix revalidation confirms the prior documentation nuance is resolved: the manifest now distinguishes the pre-move 32-root/5-core baseline from the current 28-root/5-core inventory, and `MODULES.md` records legacy `util/modbridge` only as historical routing evidence while pointing current source ownership to `mc/impl/modbridge`.

---

_Verified: 2026-05-10T09:14:24Z_
_Verifier: the agent (gsd-verifier)_

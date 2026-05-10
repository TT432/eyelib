# Phase 15 Plan 03: Roadmap-Named Utility Routing Summary

## One-liner

Roadmap-named utility classes were audited with IDE-aware evidence and moved from legacy util packages into their locked functional owner packages without deleting any class.

## Scope Completed

- Updated `docs/architecture/migration/utility-routing-manifest.md` with fresh Plan 03 evidence for `AnimationApplier`, `Models`, `ModBridgeServer`, and `BBModelSink`.
- Moved `AnimationApplier` to `io.github.tt432.eyelib.client.animation` using IDE move semantics.
- Moved `Models` to `io.github.tt432.eyelib.client.model` using IDE move semantics.
- Moved `ModBridgeServer` and `BBModelSink` to `io.github.tt432.eyelib.mc.impl.modbridge` using IDE move semantics.
- Preserved `AnimationApplier.apply`, `Models.merge/add/sub`, `ModBridgeServer`'s `50_000_000` payload bound, `AtomicBoolean` lifecycle guard, single-thread executor, TCP `127.0.0.1` log wording, and `BBModelSink.onModelUpdate(String bbmodelJson)`.

## Changed Files

| File | Change |
|------|--------|
| `docs/architecture/migration/utility-routing-manifest.md` | Refreshed evidence rows and reference-tool note for the four roadmap-named classes. |
| `src/main/java/io/github/tt432/eyelib/client/animation/AnimationApplier.java` | IDE-moved from `util/client`; package updated to `io.github.tt432.eyelib.client.animation`. |
| `src/main/java/io/github/tt432/eyelib/client/model/Models.java` | IDE-moved from `util/client`; package updated to `io.github.tt432.eyelib.client.model`. |
| `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/ModBridgeServer.java` | IDE-moved from `util/modbridge`; package updated to `io.github.tt432.eyelib.mc.impl.modbridge`. |
| `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/BBModelSink.java` | IDE-moved from `util/modbridge`; package updated to `io.github.tt432.eyelib.mc.impl.modbridge`. |
| `.planning/phases/15-pre-migration-audit-routing/15-03-SUMMARY.md` | Added execution summary and verification evidence. |

## Verification Evidence

| Check | Result |
|-------|--------|
| `ide_ide_index_status` | IDE index ready: `isDumbMode=false`, `isIndexing=false`. |
| `ide_ide_find_class` for four named classes | Each class resolved at its old util path before move, confirming the source symbols to migrate. |
| `ide_ide_find_references` | Attempted for all four classes; MCP rejected calls with `Cannot specify both language+symbol and file+line+column`, so the manifest records this tool limitation and fallback indexed evidence. |
| JetBrains indexed text searches | `AnimationApplier`: only declaration; `Models`: declaration plus unrelated plural/local-name hits, no import/qualified/static class use; `ModBridgeServer`: declaration/constructor only; `BBModelSink`: interface declaration plus `ModBridgeServer` field/constructor parameter. |
| IDE move semantics | All four files moved with `ide_ide_move_file` successfully. |
| Immediate diagnostics | `ide_ide_diagnostics` reported `problemCount=0` for `AnimationApplier.java`, `Models.java`, `ModBridgeServer.java`, and `BBModelSink.java`. |
| Residual old package scan | `jetbrain_search_regex` over `src/main/java/**/*.java` for `io.github.tt432.eyelib.util.(client|modbridge).(AnimationApplier|Models|ModBridgeServer|BBModelSink)` returned zero items. |
| Old/new path existence | Old util paths no longer exist; all four destination files exist. |

## Deviations from Plan

- None. The plan required IDE-aware moves and immediate diagnostics; all moves succeeded through IDE move semantics.

## Known Stubs

None found in the moved/modified classes.

## Threat Flags

None. The existing local TCP listener surface was moved without widening behavior, and its payload bound/lifecycle guard were preserved.

## Notes

- Full Gradle/build verification is intentionally deferred to Phase 15 Plan 04 per this plan's verification section.
- No commits were created because the execution request explicitly required `Do not commit changes`.

# Phase 6 - Policy, Specialization, Cache, Reporting, And Cutover

## Goal
- Add compatibility policy packs, runtime specialization, cache semantics, report visibility, and the final cutover sequence in the right order.

## Source Docs
- `eyelib-molang/design/compatibility-semantics-matrix.md`
- `eyelib-molang/design/compatibility-policy-pack-draft.md`
- `eyelib-molang/design/policy-pack-selection-configuration-draft.md`
- `eyelib-molang/design/runtime-specialization-contract-draft.md`
- `eyelib-molang/design/specialization-cache-contract-draft.md`
- `eyelib-molang/design/corpus-linter-runner-draft.md`
- `eyelib-molang/design/corpus-reporter-output-format-draft.md`

## Current Anchors
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileHandler.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/`

## In Scope
- Compatibility policy pack model and selection precedence
- Runtime specialization over bound results + host shape + diagnostics mode
- Specialization cache keys and invalidation rules
- Structured corpus/report output
- Cutover checklist and rollback points for replacing the old compile path

## Out Of Scope
- Premature deletion of transitional shims
- Reporter polish that is not needed for correctness/debuggability
- Root-module platform wiring changes unless separately documented

## Deliverables
- Policy-pack contract with explicit defaults and overrides
- Specialization result contract that is observable in tests/reports
- Cache contract that explains what invalidates specialization results
- Stable report shape for corpus runs
- A cutover checklist naming when the old path can be disabled and later removed

## Entry Gates
- **Phase 6 entry gate**: do not begin policy-pack implementation until flat-vs-layered pack composition is resolved for the cutover slice.
- **Phase 6 entry gate**: do not begin specialization/cutover work until typed pack options are resolved for the public/internal configuration surface.
- **Phase 6 entry gate**: do not begin cache-sensitive specialization work until debug-vs-normal cache identity is resolved.

## Internal Sequence
1. Lock policy-pack selection precedence and defaults.
2. Lock specialization result shape and required-vs-deferred behavior ownership.
3. Lock cache-key identity and invalidation rules.
4. Lock report output shape and parity evidence collection.
5. Review parity and rollback evidence.
6. Perform cutover only after the earlier gates are green.

## Non-Negotiable Policy / Specialization / Cache / Reporting Contracts
- Policy packs own semantic compatibility choices, while diagnostics overlays own severity and trace behavior. Strict/debug behavior must layer on top of a selected base pack instead of becoming a hidden second semantic pack system.
- Effective policy selection precedence must be explicit and testable: runtime/API invocation override -> corpus-case override -> suite default -> project/application default -> engine built-in fallback. Conflicts at the same precedence level fail loudly.
- Runtime specialization consumes bind result, host shape, active policy pack, and diagnostics mode together. It may resolve deferred nodes, but it must not re-parse source, re-run raw alias normalization, or silently guess through ambiguity.
- Specialization cache keys must invalidate on bind-result change, host-shape change, selected policy-pack change, and diagnostics-overlay change. Cache values should preserve the full specialization result shape, not only the selected variant ID.
- Corpus reports must keep stable case IDs, record effective mode plus policy-pack selection for every executed case, and keep corpus-data failures, engine failures, and assertion failures distinct before any cutover starts.

## Cutover Parity Checklist
- Feature-coverage parity is defined for the chosen v1 slice before cutover starts.
- Diagnostics parity is defined for that same slice, including result categories and observable payload shape that the new reporting surface must preserve.
- Rollback artifacts identify the old path and compatibility shim used by each cutover step.
- Any non-parity behavior is either explicitly deferred through the v1 scope matrix or blocked from cutover.
- Downstream consumer parity is green for root runtime, `:eyelib-importer`, and `:eyelib-processor`, not only for `:eyelib-molang` in isolation.

## TDD Slices
1. Add policy-pack matrix tests.
2. Add specialization outcome tests for host-shape and diagnostics-mode variation.
3. Add cache-key/invalidation tests.
4. Add structured report assertions.
5. Perform cutover only after the previous slices are green.

## Verification Gate
- `./gradlew :eyelib-molang:test`
- `./gradlew :eyelib-molang:test :eyelib-importer:test :eyelib-processor:test :test`
- If a dedicated corpus runner or specialization task is introduced, document the exact command before cutover begins.

## Exit Criteria
- Compatibility behavior is explicit and pack-driven instead of being scattered across generic runtime code.
- Specialization and cache semantics are testable and observable.
- The plan includes a named rollback point before any old-path deletion.
- The generated-parser-driven compile path is only retired after the replacement path proves feature coverage and diagnostics parity for the chosen slice.
- Selection precedence, cache invalidation inputs, and report result categories are all locked down in tests or corpus assertions before cutover work begins.
- Cutover happens only after the internal sequence above has been completed in order rather than being bundled into a single undifferentiated implementation step.

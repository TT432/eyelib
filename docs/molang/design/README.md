# Molang Design Notes

## Documents
- `molang-syntax-baseline.md`: source-backed syntax baseline and acceptance corpus for the rewrite discussion.
- `molang-ast-and-semantics-draft.md`: draft architecture for AST, binding, host injection, type layering, and execution.
- `shared-vocabulary-and-phase-ownership-draft.md`: canonical terminology and phase ownership rules for host/query/policy/specialization drafts.
- `host-injection-api-draft.md`: draft API and resolution model for host context, receiver-first callables, and typed injection.
- `query-variant-registry-draft.md`: draft registry and dispatch model for host-backed query variants.
- `callable-discovery-annotation-draft.md`: draft discovery and declaration model that turns annotated/static definitions into host callables and query variants.
- `host-adapter-registry-draft.md`: draft registry model for publishing host objects into stable engine-visible roles.
- `compatibility-semantics-matrix.md`: matrix of official behavior, community-observed quirks, target compatibility level, and first-stage implementation posture.
- `parser-acceptance-corpus.md`: layered acceptance corpus for parser/binder/runtime validation, keyed by evidence level and compatibility posture.
- `parser-strategy-draft.md`: concrete frontend strategy for lexer, statement parsing, Pratt-style expression parsing, recovery, and binder handoff.
- `strict-debug-diagnostics-mode-draft.md`: diagnostic policy for normal, strict, and debug modes across parser, binder, compatibility, and runtime layers.
- `executable-corpus-format-draft.md`: executable on-disk format for acceptance corpus cases, expected shapes, diagnostics, and mode-specific assertions.
- `binder-normalization-contract-draft.md`: explicit contract for how parser AST is normalized into bound semantic nodes, unresolved nodes, traits, and diagnostics.
- `compatibility-policy-pack-draft.md`: draft model for pluggable compatibility packs that carry version-sensitive or quirk-sensitive Molang behavior.
- `corpus-linter-runner-draft.md`: design for loading, linting, executing, and asserting Molang corpus cases across phases, modes, and policy packs.
- `runtime-specialization-contract-draft.md`: contract for specializing bound/query/host-sensitive nodes against host shape, policy pack, and diagnostics mode at execution time.
- `policy-pack-selection-configuration-draft.md`: explicit configuration and precedence model for selecting compatibility policy packs in different environments.
- `corpus-reporter-output-format-draft.md`: normalized result/output format for corpus lint and runner reports.
- `specialization-cache-contract-draft.md`: cache-key and invalidation contract for runtime specialization artifacts.

## Reading Order
1. `molang-syntax-baseline.md`
2. `molang-ast-and-semantics-draft.md`
3. `shared-vocabulary-and-phase-ownership-draft.md`
4. `host-injection-api-draft.md`
5. `query-variant-registry-draft.md`
6. `callable-discovery-annotation-draft.md`
7. `host-adapter-registry-draft.md`
8. `compatibility-semantics-matrix.md`
9. `parser-acceptance-corpus.md`
10. `parser-strategy-draft.md`
11. `strict-debug-diagnostics-mode-draft.md`
12. `executable-corpus-format-draft.md`
13. `binder-normalization-contract-draft.md`
14. `compatibility-policy-pack-draft.md`
15. `corpus-linter-runner-draft.md`
16. `runtime-specialization-contract-draft.md`
17. `policy-pack-selection-configuration-draft.md`
18. `corpus-reporter-output-format-draft.md`
19. `specialization-cache-contract-draft.md`

## Repository Context
- Active engine code lives in `src/main/java/io/github/tt432/eyelib/molang/`.
- ADR-0014 flat-merge 后，原 `eyelib-molang` 子项目已合并为单 project 下的 `molang` 包。Minecraft/Forge 平台桥接代码在 `src/main/java/io/github/tt432/eyelib/bridge/molang/`（参见 [ADR-0010](../../decisions/0010-hexagonal-architecture.md) Port 设计）。
- 引擎代码内部不再有 `generated/` 区（ANTLR 已删除，只剩手写递归下降解析器）。

## Scope
- These documents are design drafts for the future `molang` package rewrite.
- They are discussion artifacts, not implementation commitments.

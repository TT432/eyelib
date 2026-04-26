# Phase 2 - Parser And AST

## Goal
- Introduce the new handwritten frontend and AST contracts in parallel with the current generated-parser path.

## Source Docs
- `eyelib-molang/design/molang-ast-and-semantics-draft.md`
- `eyelib-molang/design/parser-strategy-draft.md`
- `eyelib-molang/design/molang-syntax-baseline.md`
- `eyelib-molang/design/parser-acceptance-corpus.md`
- `docs/architecture/03-generated-code-policy.md`

## Current Anchors
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangExpressionAnalyzer.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`

## In Scope
- Lexer/parser strategy
- Parse tree to AST handoff
- AST node families and source-span ownership
- Parser-only diagnostics and recovery posture

## Out Of Scope
- Final binder semantics
- Host-role publication
- Compatibility pack execution
- Deleting the generated parser path

## Deliverables
- New additive frontend packages/classes for the handwritten parser flow
- AST contracts shaped for later binder normalization
- Parser acceptance tests driven by the corpus phase
- A clear boundary between generated parser usage and the new frontend

## Syntax Baseline Checklist
- Expression coverage for literals, identifiers, grouped expressions, unary/binary precedence, null-coalescing, indexing, member access, and `->` access families.
- Statement/block coverage for `return`, `loop`, `for_each`, `break`, and `continue` in the first accepted slice chosen for the rewrite.
- Text/surface rules for strings, case-insensitive keywords/identifiers where required by the syntax baseline, and source-span ownership across parsed structures.
- Alias spellings may be parsed, but alias canonicalization is deferred to binder ownership rather than frozen in the parser.

## Implementation Notes
- Keep the current generated-parser path working while the new frontend matures.
- Treat `generated/` as read-only.
- Do not make package-layout cleanup the first deliverable; correctness proof comes first.

## Parser Neutrality Rules
- Parser output stays generic about fallback/default semantics. It must not encode policy-pack choices, host-availability choices, or compatibility-specific neutral results.
- Forms such as query omission shorthand and alias spellings may survive into AST/source form, but binder owns canonicalization and semantic reinterpretation.

## Entry Gates
- **Phase 2 gate resolved**: use `BlockExpr` as the long-term AST surface for expression-valued blocks; keep "complex expression" as source/corpus terminology rather than a second AST node family.
- **Phase 2 gate resolved**: parse `loop` / `for_each` as dedicated control-form productions with explicit control-flow AST, not as generic call-like forms lowered later.

## TDD Slices
1. Add parser acceptance cases for the chosen syntax baseline.
2. Add failing parse-shape assertions.
3. Implement minimal lexer/parser support for the smallest accepted slice.
4. Expand recovery and diagnostics only after the basic accept/reject surface is stable.

## Verification Gate
- `./gradlew :eyelib-molang:test`

## Exit Criteria
- The new frontend can parse a proven subset of the corpus without breaking the current engine path.
- AST output is stable enough for binder normalization to begin.
- Parse diagnostics and recovery behavior are documented by tests, not only by prose.
- The accepted syntax subset, source-span preservation rules, and unresolved AST representation decisions are explicit enough that binder work does not begin on guesswork.

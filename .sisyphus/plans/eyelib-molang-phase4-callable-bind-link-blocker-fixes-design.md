# Eyelib Molang Phase 4 Callable Bind-Link Blocker Fixes - Design Brief

## Objective
Define the minimal blocker-fix slice so a fresh implementation subagent can patch binder emission, add one binder regression test, and align Phase 4 roadmap evidence text without widening scope.

## Grounded Inputs (read before implementation)
- `.sisyphus/plans/eyelib-molang-phase4-callable-bind-link-blocker-fixes.md`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinder.java`
- `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableBindLinkContractTest.java`
- `eyelib-molang/ROADMAP.md`

## Scope (exactly three implementation files)
1. `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/MolangBinder.java`
2. `eyelib-molang/src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableBindLinkContractTest.java`
3. `eyelib-molang/ROADMAP.md`

## Exact Binder Guard Change
Target method: `MolangBinder#maybeAddCallableBindLinkRequest(BoundMolang.BoundCallExpr callExpr, BindingState state)`.

Current issue:
- Emission is currently gated only by `query` root check.
- `symbolicCallableName(callExpr.callee())` can return blank (`""`) for non-query callee shapes (for example grouped callees), yet request emission still occurs.

Required change:
1. Compute `symbolicCallableName` once at the top of `maybeAddCallableBindLinkRequest(...)`.
2. If computed name is blank (`isBlank()`), return early (skip emission).
3. Keep existing query-root skip behavior.
4. Reuse computed `symbolicCallableName` when constructing `CallableBindLinkRequest`.

Required invariant:
- **Do not change linker-side validation logic** in `MolangCallableBindLinker`; linker remains the hard malformed-input guard.

## Exact Binder Regression Test Shape
Target file: `MolangCallableBindLinkContractTest`.

Add one new test method (name can follow existing style, e.g. `binderSkipsCallableEmissionWhenSymbolicCallableNameExtractionIsBlank`).

Test setup and assertions:
1. `MolangMappingTree.setupMolangMappingTree(List::of);`
2. Build the AST **manually** instead of parsing source text.
   - Use `SourceSpan.unknown()` for spans.
   - Construct a grouped-callee call shape directly:
     - `new MolangAst.IdentifierExpr(span, "math")`
     - `new MolangAst.MemberAccessExpr(span, owner, "sin")`
     - `new MolangAst.GroupingExpr(span, memberAccess)`
     - `new MolangAst.CallExpr(span, groupedCallee, List.of(new MolangAst.NumberLiteralExpr(span, "30", 30)))`
     - Wrap in `new MolangAst.ExprSet(span, callExpr)`
   - Rationale: `MolangParserFrontends.active()` uses the generated-parser-backed frontend, so parser text is not a reliable way to create a `CallExpr` whose callee is a `GroupingExpr`. Manual AST construction guarantees the binder sees the exact blank-symbolic-name path.
3. Call `binder.bind(ast)` directly.
4. Assert binder does not enqueue malformed callable bind-link requests:
   - `assertTrue(bindResult.callableBindLinkRequests().isEmpty());`
5. Assert query bind-link lane stays untouched:
   - `assertTrue(bindResult.queryBindLinkRequests().isEmpty());`

Notes:
- Keep this as a **binder regression** in the existing callable contract test file.
- Add only the minimal helper/local setup needed for manual AST construction; do not move the regression into unrelated binder test files.
- Do not add linker behavior changes or new diagnostics expectations for this slice.

## Exact `ROADMAP.md` Sections To Update
Update wording anywhere it still claims query-only bind-link evidence. In current file, this is in three places:

1. `## Phase Status` table, row `Phase 4 - Host and query bridge` (Evidence column)
   - Replace query-only phrasing with wording that reflects both query and callable bind-link contract coverage.
2. `### M4 - Resolve Phase 4 host/query gates before implementation`
   - Update the contract-evidence bullet that currently names only `MolangQueryBindLinkContractTest.java` as query-only bind-link coverage.
3. `## Phase 4 Recorded Decisions And Required Test Surfaces`
   - Update the completed-evidence bullet that currently says `MolangQueryBindLinkContractTest.java` verifies a query-only bind-link handoff.

Required evidence wording outcome:
- Mention both:
  - `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangQueryBindLinkContractTest.java`
  - `src/test/java/io/github/tt432/eyelibmolang/mapping/MolangCallableBindLinkContractTest.java`
- Keep phase status/rules unchanged other than evidence-text correction.

## Forbidden Areas
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/binding/link/**`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/**`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileHandler.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/MolangCompileVisitor.java`
- `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/compiler/frontend/**`
- `src/main/java/io/github/tt432/eyelib/mc/impl/molang/**`
- Any files outside the three scoped implementation files listed above

## Implementation Checklist
1. Edit `MolangBinder#maybeAddCallableBindLinkRequest(...)` to compute symbolic callable name first.
2. Add early return when computed symbolic callable name is blank.
3. Keep existing query-root skip and existing request payload shape (`symbolicCallableName`, `visibleCallShape`) otherwise unchanged.
4. Add one binder regression test in `MolangCallableBindLinkContractTest` using a manually constructed `MolangAst.ExprSet` whose `CallExpr` callee is a `GroupingExpr`, with assertions that both bind-link request lists are empty.
5. Update the three `ROADMAP.md` sections listed above so evidence text no longer reads as query-only bind-link coverage.
6. Confirm no edits landed in forbidden paths, especially linker files.

## Verification
- Run via IDE Gradle tooling (JetBrains MCP): `:eyelib-molang:test`
- Expected result: exit code `0`, including the new binder regression test.

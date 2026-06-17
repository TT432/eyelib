# Binder Normalization Contract Draft

## Purpose
- This document defines the contract between parser output and the semantic/bound representation.
- It specifies what the binder must normalize, what it must preserve, what it may defer, and what diagnostics it must produce.

## Relationship To Other Docs
- `parser-strategy-draft.md` defines what the parser should and should not embed.
- `shared-vocabulary-and-phase-ownership-draft.md` defines canonical bridge terms such as `BoundQueryAccess` and phase ownership.
- `host-injection-api-draft.md`, `query-variant-registry-draft.md`, and related docs define the semantic targets the binder must project toward.
- `strict-debug-diagnostics-mode-draft.md` defines how binder diagnostics behave under different modes.

## Repository Boundary Reminder
- This is an engine-side semantic contract.
- It does not define runtime execution or platform-side adapter wiring.

---

## 1. Binder Responsibilities

### 1.1 Core role
- The binder turns parser AST into a semantically normalized tree that is stable enough for:
  - analysis,
  - compatibility policy,
  - host/query dispatch,
  - execution-plan lowering.

### 1.2 What binding is not
- It is not runtime execution.
- It is not compatibility fallback policy itself.
- It is not host publication/adaptation.

---

## 2. Input And Output Contract

## 2.1 Input assumptions
- Parser output preserves:
  - access-family distinction (`.` vs `->`)
  - statement ordering
  - assignment target structure
  - source spans
  - dedicated control-flow AST for `loop` / `for_each`
  - generic identifiers without semantic specialization

## 2.2 Output goals
- Binder output should produce semantic nodes that distinguish at least:
  - local/namespace value access
  - query-backed access
  - host-callable/query candidate-bearing access
  - unresolved-yet-deferred forms
  - structurally invalid forms

---

## 3. Required Normalizations

## 3.1 Alias normalization
- `q/t/v/c` must canonicalize to `query/temp/variable/context`.
- This is a required first-stage normalization.

## 3.2 Root interpretation
- Binder should recognize canonical roots and classify their follow-up chains accordingly.

## 3.3 Query-candidate normalization
- Accesses like `query.foo` should not remain as generic unresolved member chains if they match query surface rules.
- They should become query-oriented semantic nodes or candidate-bearing nodes.

## 3.4 Assignment validation
- Binder must validate whether a syntactic assignment target is semantically writable.
- Invalid writes should become binder errors, not runtime surprises.

---

## 4. Required Preservation

## 4.1 Structural preservation
- Binder must preserve:
  - `.` vs `->`
  - call/index/member chain order
  - block/control-flow ordering
  - source span lineage

## 4.2 Why preservation matters
- analysis depends on structure
- compatibility policy depends on structure
- diagnostics need source correlation
- later execution lowering must remain faithful to parsed intent

---

## 5. Deferred Semantics

## 5.1 Allowed deferrals
- Binder may intentionally leave some nodes unresolved or partially-resolved when full meaning depends on:
  - host shape
  - query variant specialization
  - compatibility policy pack
  - runtime-only facts

## 5.2 Rule for deferral
- Deferred does not mean vague.
- Every deferred node should carry an explicit reason category.

### Example categories
- `NEEDS_HOST_SHAPE`
- `NEEDS_COMPAT_POLICY`
- `NEEDS_QUERY_VARIANT_SELECTION`
- `UNSUPPORTED_IN_CURRENT_MODE`

---

## 6. Draft Bound Node Families

```text
BoundNode
├── BoundExpr
│   ├── BoundLiteral
│   ├── BoundNamespaceAccess
│   ├── BoundStructAccess
│   ├── BoundArrowAccess
│   ├── BoundCall
│   ├── BoundQueryAccess
│   ├── BoundAssignment
│   ├── BoundConditional
│   └── BoundDeferredExpr
└── BoundStmt
    ├── BoundExprStmt
    ├── BoundReturn
    ├── BoundBreak
    ├── BoundContinue
    ├── BoundLoop
    ├── BoundForEach
    └── BoundBlock
```

This is illustrative, not final type naming.

### Canonical query-node rule
- `BoundQueryAccess` is the canonical binder-level query node name for this draft set.
- It should preserve whether the source used omission-style access or explicit call syntax when that distinction matters to compatibility policy.
- Do not introduce a parallel `BoundQueryCall` canonical node name unless a later draft proves a materially different node family is required.

---

## 7. Trait Attachment

## 7.1 Binder-owned semantic traits
- Binder should attach the first meaningful semantic traits available before runtime, such as:
  - requires-runtime-host
  - shape-dependent
  - maybe-deterministic / non-deterministic candidate set
  - assignment / side-effect potential

## 7.2 Why binder attaches traits early
- analysis and partial evaluation need conservative decisions before runtime specialization.

---

## 8. Diagnostics Contract

## 8.1 Binder must diagnose
- invalid assignment target
- impossible parameter-role layout if already visible at bind time
- unsupported root usage
- malformed control-flow placement if parser leaves it structurally representable but semantically invalid

## 8.2 Binder may defer instead of error only when
- later policy/runtime input is genuinely required
- and the deferred state is explicit and testable

## 8.3 Strict/debug implications
- strict mode may elevate some binder deferrals into warnings/errors
- debug mode should expose normalization steps and deferred reasons

---

## 9. Query And Host Projection

## 9.1 Query projection
- Binder should project eligible `query.*` forms toward query-registry lookup nodes.

## 9.2 Host projection
- Binder should not resolve actual host objects.
- It may, however, project calls toward host-callable descriptors and record required semantic roles.

## 9.3 Boundary rule
- Host publication remains outside binder.
- Binder only prepares the semantic request shape that runtime specialization will consume.

---

## 10. Compatibility Policy Interaction

## 10.1 Binder must stay compatibility-aware
- Some constructs are syntactically valid but semantically policy-sensitive.
- Examples:
  - zero-arg query omission
  - `temp.` struct restrictions
  - targeted neutral fallbacks

## 10.2 Recommended rule
- Binder should surface a stable semantic node plus a policy hook point.
- It should not bury compatibility decisions directly inside generic node classes.

---

## 11. Draft Binder Output Payload

```java
record BindResult(
    Object root,
    List<MolangDiagnostic> diagnostics,
    Object summaryTraits
) {}
```

## 11.1 Why a result wrapper matters
- diagnostics and normalized tree should travel together
- tests can assert both structure and warnings
- later phases can consume semantic summaries without re-walking everything immediately

---

## 12. Example Normalizations

## 12.1 Alias case

Input:
```text
q.life_time + t.counter
```

Output intent:
- `q` becomes canonical `query`
- `t` becomes canonical `temp`
- output retains member chain structure

## 12.2 Query candidate case

Input:
```text
query.swell_amount
```

Output intent:
- not just `BoundStructAccess(query, swell_amount)`
- instead a query-oriented semantic node/candidate representation
- canonical binder naming: `BoundQueryAccess`

## 12.3 Deferred compatibility case

Input:
```text
query.is_invisible
```

Output intent:
- may bind to a query-oriented node
- may attach `NEEDS_COMPAT_POLICY` if zero-arg omission remains policy-sensitive

---

## 13. Testing Guidance

## 13.1 Corpus integration
- The executable corpus should support `phase: bind` expected-shape checks against binder output.

## 13.2 Minimum binder assertions
- alias canonicalization
- access-family preservation
- query candidate projection
- diagnostic presence/absence
- deferred-reason tagging where relevant

---

## 14. Open Questions
- Should binder output directly reference registry/callable IDs, or only symbolic names until a later linking step?
- How rich should deferred-reason tagging be in v1?
- Do we want a separate “linking” phase after bind, or should binder own all static projection work?

## 15. Immediate Follow-Up
- compatibility policy pack draft
- corpus linter / runner draft
- runtime specialization contract draft

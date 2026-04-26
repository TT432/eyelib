# Binder Defer Rate Migration Tracker

## Purpose

Track the progression of `MolangBinder` from its current "minimal slice" state
toward full coverage. Each deferred expression family must eventually move from
`UNSUPPORTED_IN_THIS_SLICE` to a specific phase-bound defer reason.

## Current State (2026-04-26)

### Defer Reason Taxonomy

| Reason | Code Enum | Phase Owner | Status |
|---|---|---|---|
| `UNSUPPORTED_IN_THIS_SLICE` | exists | Phase 3 | single generic reason, no granularity |
| `NEEDS_HOST_SHAPE` | NOT IMPLEMENTED | Phase 4 | design draft only |
| `NEEDS_QUERY_VARIANT_SELECTION` | NOT IMPLEMENTED | Phase 4 | design draft only |
| `NEEDS_COMPAT_POLICY` | NOT IMPLEMENTED | Phase 6 | design draft only |

### Deferred Expression Families

| Source Family | AST Node | Defer Reason | Phase Target | Notes |
|---|---|---|---|---|
| `LoopExpr` | `MolangAst.LoopExpr` | `UNSUPPORTED_IN_THIS_SLICE` | Phase 5 | Structural binding complete; runtime execution deferred |
| `ForEachExpr` | `MolangAst.ForEachExpr` | (falls to generic else) | Phase 5 | **NO explicit binder branch** — treated as generic unknown |
| `TernaryConditionalExpr` | `MolangAst.TernaryConditionalExpr` | `UNSUPPORTED_IN_THIS_SLICE` | Phase 5 | Old compile path supports; parity gap |
| `BinaryConditionalExpr` | `MolangAst.BinaryConditionalExpr` | `UNSUPPORTED_IN_THIS_SLICE` | Phase 5 | Old compile path supports; parity gap |
| `BreakStmt` | `MolangAst.BreakStmt` | `UNSUPPORTED_IN_THIS_SLICE` | Phase 5 | Typed deferred node exists; runtime execution deferred |
| `ContinueStmt` | `MolangAst.ContinueStmt` | `UNSUPPORTED_IN_THIS_SLICE` | Phase 5 | Typed deferred node exists; runtime execution deferred |

### Migration Progression

```
Phase 3 (current): UNSUPPORTED_IN_THIS_SLICE — 6 families
                     ↓
Phase 3 widening:   Add NEEDS_HOST_SHAPE, NEEDS_QUERY_VARIANT_SELECTION enums
                     Reclassify families where host/query dependency applies
                     ↓
Phase 4 linkage:    Bind-link resolves symbolic names to candidates
                     Loop/conditional families remain deferred on execution
                     ↓
Phase 5 execution:  Replace UNSUPPORTED_IN_THIS_SLICE with phase-specific reasons
                     Implement runtime execution for each family
                     ↓
Phase 6 complete:   0 UNSUPPORTED_IN_THIS_SLICE entries
```

### Tracking Metrics

| Date | Total Bound Families | Deferred | Defer Rate | Notes |
|---|---|---|---|---|
| 2026-04-26 | ~20 | 6 | ~30% | Initial measurement (estimated) |

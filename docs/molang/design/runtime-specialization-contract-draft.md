# Runtime Specialization Contract Draft

## Purpose
- This document defines how bound semantic nodes become runtime-specialized execution inputs once host shape, compatibility policy pack, and diagnostics mode are known.

## Relationship To Other Docs
- `binder-normalization-contract-draft.md` defines what binder may defer.
- `shared-vocabulary-and-phase-ownership-draft.md` defines specialization ownership and the distinction between policy packs and diagnostics overlays.
- `compatibility-policy-pack-draft.md` defines policy-sensitive decisions.
- `host-adapter-registry-draft.md` and `query-variant-registry-draft.md` define the role publication and variant-selection surfaces runtime specialization must consult.

## Repository Boundary Reminder
- This document is about engine-side specialization semantics.
- It is not the same thing as bytecode generation or low-level execution plan lowering.

---

## 1. Why Specialization Exists

## 1.1 Core problem
- Binder intentionally leaves some nodes partially resolved because they depend on:
  - host shape
  - active policy pack
  - active diagnostics mode
  - query variant selection

## 1.2 Goal
- Convert those deferred semantic nodes into runtime-ready specialized forms without redoing all earlier phases from scratch.

---

## 2. Inputs To Specialization

### Required inputs
- bound tree / bind result
- host shape / host context
- active compatibility policy pack
- diagnostics mode overlay

### Optional inputs
- cached query/callable lookup state
- precomputed variant selection cache

---

## 3. Specialization Responsibilities

## 3.1 Must resolve
- query variant selection when enough host/policy info exists
- host-callable candidate narrowing
- policy-sensitive deferred nodes
- diagnostics escalation/downgrade driven by mode + policy

## 3.2 Must preserve
- semantic structure needed for diagnostics/debugging
- linkage to source spans and binder diagnostics

## 3.3 Must not do
- re-parse source
- re-run raw alias normalization
- silently guess through ambiguity

---

## 4. Draft Specialization Flow

```mermaid
flowchart LR
    A[BindResult] --> B[Host shape available]
    B --> C[Policy pack available]
    C --> D[Mode overlay]
    D --> E[Specialized semantic form]
    E --> F[Execution plan / runtime]
```

---

## 5. Deferred Node Resolution

## 5.1 Example deferrals
- `NEEDS_HOST_SHAPE`
- `NEEDS_COMPAT_POLICY`
- `NEEDS_QUERY_VARIANT_SELECTION`
- `UNSUPPORTED_IN_CURRENT_MODE`

## 5.2 Resolution rule
- Specialization should consume these categories explicitly.
- If a category still cannot be resolved, the result must remain explicit and diagnosable.

---

## 6. Query And Host Specialization

## 6.1 Query specialization
- Consume query candidate sets produced by binder.
- Use host shape + policy pack + mode to choose:
  - exact variant
  - default variant
  - unresolved path with diagnostics

## 6.1.1 Ownership reminder
- Binder may project query candidates, but specialization is the first phase allowed to choose the final host-shape-aware winner.

## 6.2 Host-callable specialization
- Consume callable candidate/projection info.
- Verify required host roles are present.
- Apply strict/debug fallback policy if compatibility would otherwise hide the issue.

## 6.3 Ambiguity rule
- Ambiguity remains an error in all modes.

---

## 7. Output Contract

```java
record SpecializationResult(
    Object root,
    List<MolangDiagnostic> diagnostics,
    Object specializationSummary
) {}
```

## 7.1 Output expectations
- enough structure for runtime/execution-plan lowering
- explicit record of what was specialized
- explicit record of what stayed unresolved

---

## 8. Diagnostics Interaction

## 8.1 Normal mode
- allows documented compatibility behavior

## 8.2 Strict mode
- may elevate already-classified policy-sensitive specialization outcomes into warnings/errors

## 8.3 Debug mode
- should emit structured specialization traces such as:
  - selected host shape
  - candidate query variants
  - chosen policy path
  - fallback reason

---

## 9. Caching Considerations

## 9.1 Suitable cache keys
- bind-result identity/version
- host shape signature
- policy pack ID/version
- diagnostics mode

## 9.2 Why mode matters
- diagnostics overlays can affect outcome classification and traces, so cache keys cannot ignore mode blindly.

---

## 10. Testing Guidance

## 10.1 Corpus integration
- Corpus runner should eventually support specialization-phase assertions for:
  - selected variant IDs
  - selected/default/fallback path
  - resulting diagnostics/traces

## 10.2 Good first assertions
- unresolved in normal mode vs warning/error in strict mode
- exact variant chosen for known host shape
- default variant chosen when expected

---

## 11. Open Questions
- Should specialization produce a distinct tree type, or annotate bound nodes in place?
- How much specialization should happen before execution-plan lowering versus during lowering?
- Should unresolved specialization results be executable in a degraded mode, or always block execution?

## 12. Immediate Follow-Up
- policy pack selection/configuration draft
- corpus reporter/output format draft
- specialization cache contract draft

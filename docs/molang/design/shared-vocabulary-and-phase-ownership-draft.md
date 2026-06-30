# Shared Vocabulary And Phase Ownership Draft

## Purpose
- This document defines the canonical vocabulary and phase ownership rules shared by the Molang rewrite design set.
- It exists to stop adjacent drafts from re-defining the same bridge concepts with slightly different names.

## Relationship To Other Docs
- `molang-ast-and-semantics-draft.md` defines the high-level semantic split.
- `host-injection-api-draft.md`, `host-adapter-registry-draft.md`, `callable-discovery-annotation-draft.md`, and `query-variant-registry-draft.md` all consume the vocabulary here.
- `binder-normalization-contract-draft.md` consumes the phase-ownership rules here.

---

## 1. Canonical Vocabulary

### 1.1 Host publication terms
- **`HostRole<T>`** is the canonical semantic term for a published host capability or subject role.
- Examples:
  - `ENTITY`
  - `LIVING_ENTITY`
  - `SELF_ENTITY`
  - `TARGET_ENTITY`
  - `QUERY_RUNTIME`
- Design docs should prefer **`HostRole`** when discussing semantics.
- If a future implementation wants a storage-specific key object, it may use `HostKey<T>` as an implementation carrier for a `HostRole<T>`, but `HostKey` is not the primary design term.

### 1.2 `HostContext`
- `HostContext` is the runtime container of published host-role values.
- It answers role lookups.
- It is not the same thing as raw owner/object bags.

### 1.3 `HostShape`
- `HostShape` is the stable signature of which host roles are present.
- `HostShape` describes role presence/capability shape, not raw object identity.
- `HostShape` is valid for dispatch, specialization, diagnostics, and caching.

### 1.4 Parameter-role terms
- **`RECEIVER`**: the primary host subject of a callable/query.
- **`VISIBLE_ARG`**: a Molang-visible call argument.
- **`INJECTED_HOST`**: a non-visible host/service dependency resolved from `HostContext`.
- **`SPECIAL_ENGINE_ARG`**: engine-internal execution context not modeled as a published host role.
- Receiver inference is intentionally narrow. The first non-special host parameter may infer `RECEIVER` only when the declaration is otherwise unambiguous. `INJECTED_HOST` and `SPECIAL_ENGINE_ARG` require explicit metadata.

### 1.5 `VisibleArgSpec`
- `VisibleArgSpec` is the normalized signature shape of the Molang-visible argument surface only.
- It does not describe receiver roles or injected host services.
- Query and callable registries may consume it, but discovery should derive it from a single parameter-role model instead of inventing a second parallel signature system.

### 1.6 `CallableTraits`
- `CallableTraits` is the shared trait family attached to discovered and registry-visible callables.
- First-stage shared fields should remain narrowly semantic, such as:
  - deterministic
  - runtime-enumerable
  - side-effect-free
- Additional fields should only be added if multiple phases consume them.

### 1.7 `SourceOrigin`
- `SourceOrigin` is declaration/discovery provenance metadata.
- It exists for diagnostics, conflict reporting, documentation, and tooling.
- It is not part of dispatch specificity and should not become a semantic tie-breaker.

### 1.8 `BoundQueryAccess`
- `BoundQueryAccess` is the canonical binder-level node for query-surface usage.
- It covers both:
  - explicit call spelling such as `query.foo()`
  - omission/call-surface spelling such as `query.foo`
- Binder must preserve which surface spelling was used when policy or compatibility cares about that distinction.
- `BoundQueryCall` should not exist as a second competing binder-level canonical name unless the design later introduces a meaningfully different node family.

### 1.9 `CompatibilityPolicyPack`
- A policy pack owns compatibility-sensitive semantic decisions.
- Examples: targeted omission rules, neutral fallback posture, version-sensitive behavior selection.
- A policy pack does not own raw syntax parsing or host publication.

### 1.10 `DiagnosticsModeOverlay`
- A diagnostics mode overlay is the mode-specific layer applied on top of base semantic selection.
- Examples: normal / strict / debug.
- It owns severity/tracing/escalation behavior.
- It should not silently redefine base compatibility semantics unless a document explicitly marks a behavior as mode-owned.

### 1.11 `BindLinkRef`
- `BindLinkRef` is the narrow handoff record between binder and runtime specialization.
- It carries a stable candidate-set reference and a registry version reference so later phases can specialize without re-parsing or re-projecting semantics.

---

## 2. Phase Ownership Rules

## 2.1 Discovery owns declaration normalization
- Discovery turns declarations into canonical callable descriptors.
- Discovery owns:
  - exported names
  - aliases
  - parameter-role classification
  - base trait attachment
  - source origin metadata
- Discovery does **not** choose runtime variants for a specific host shape.

## 2.2 Host publication owns raw-object adaptation
- Host publication turns raw runtime objects/services into published `HostRole` values.
- Host publication owns:
  - inheritance handling
  - adapter matching
  - publication-site role materialization
  - `HostContext` and `HostShape` creation
- Host publication does **not** choose callable/query winners.

## 2.3 Binder owns semantic projection
- Binder turns parser output into stable semantic nodes.
- Binder owns:
  - root canonicalization
  - alias normalization
  - query/call projection
  - deferred-reason tagging
  - semantic structure for later phases
- Binder does **not** finalize host-shape-dependent or policy-pack-dependent dispatch.

## 2.4 Linker owns narrow bind-to-link resolution
- The linker is the narrow pass between binder and specialization.
- It resolves symbolic query and callable names to stable candidate-set refs and registry version refs.
- It does **not** choose the final host-shape winner.

## 2.5 Compatibility policy owns semantic compatibility choices
- Policy packs own semantic choices that vary by compatibility posture.
- Policy packs may classify decisions such as:
  - accepted
  - defaulted
  - tolerated unresolved
  - targeted compatibility behavior
- Policy packs do **not** own final diagnostic severity mapping.

## 2.6 Diagnostics overlay owns severity and trace behavior
- Diagnostics overlay maps already-classified situations into:
  - silent pass
  - info
  - warning
  - error
  - debug trace payload
- Diagnostics overlay does **not** replace policy-pack selection as the source of semantic compatibility behavior.

## 2.7 Runtime specialization owns final shape-aware selection
- Runtime specialization is the first phase allowed to make final host-shape-aware query/callable choices.
- Runtime specialization owns:
  - final query variant selection
  - final host-callable narrowing
  - shape-aware default-path selection
  - unresolved result reporting after bind-time deferral
- Runtime specialization should consume prior phase outputs; it should not re-invent declaration or binder semantics.

---

## 3. Guardrails For Future Drafts

- Every new bridge term should answer three questions explicitly:
  1. what is it,
  2. who produces it,
  3. who consumes it.
- If a concept already exists in this document, later drafts should extend it rather than rename it locally.
- If a document needs a projection/view type, it should say which canonical type it is derived from.
- If a new behavior changes semantics, it belongs to policy or specialization; if it changes reporting, it belongs to diagnostics overlay.

---

## 4. Immediate Follow-Up
- Update host/query drafts to prefer `HostRole` terminology and to reference this document.
- Update binder/runtime drafts to use `BoundQueryAccess` as the canonical binder-level query node and `BindLinkRef` as the narrow handoff record.
- Update policy docs to keep semantic pack ownership separate from diagnostics overlay ownership.

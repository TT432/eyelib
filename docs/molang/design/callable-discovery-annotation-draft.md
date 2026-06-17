# Callable Discovery And Annotation Draft

## Purpose
- This document defines how implementation methods become engine-visible callable descriptors.
- It bridges the gap between:
  - `host-injection-api-draft.md`
  - `query-variant-registry-draft.md`
  - the earlier AST/semantic binding drafts.

## Scope
- This document is about **declaration and discovery**, not runtime dispatch.
- It answers:
  - how a method/class is declared as Molang-visible,
  - how parameter kinds are discovered,
  - how exported names and traits are produced,
  - how one discovered declaration becomes either a host callable or a query variant.

## Repository Boundary Reminder
- Discovery contracts may live in `:eyelib-molang`.
- Platform-specific discovery wiring still belongs in root-side bootstrap/lifecycle code.
- This draft does not require reflection-based scanning to happen inside core engine code; it only defines the shape of what discovery must produce.

---

## 1. Problem Statement

## 1.1 Current weakness
- The current mapping system mixes together:
  - exported names,
  - traits,
  - static method discovery,
  - scope injection conventions,
  - platform-specific intent.
- It does not clearly describe parameter roles such as receiver vs visible arg vs injected service.

## 1.2 Target outcome
- A declaration should compile into a stable descriptor.
- Downstream binding/runtime code should not need to reverse-engineer method signatures ad hoc.
- Discovery results should be explicit enough for:
  - diagnostics,
  - documentation generation,
  - query variant registration,
  - future code generation or caching.

---

## 2. Core Design Rules

### Rule A: discovery produces descriptors, not executable policy
- Discovery should describe callable shape.
- Runtime dispatch should consume those descriptors later.

### Rule B: exported names are explicit
- Do not derive public Molang surface names from Java method names alone unless that is explicitly the declared fallback rule.

### Rule C: parameter roles must be explicit or inferable by deterministic rules
- Every parameter must become exactly one semantic role.
- If a parameter role cannot be determined confidently, discovery should fail.
- The inference budget is intentionally narrow. `RECEIVER` may be inferred from the first non-special host parameter only when the declaration is otherwise unambiguous. `INJECTED_HOST` and `SPECIAL_ENGINE_ARG` require explicit metadata.

### Rule D: query variants are a specialization of callable discovery
- Query registration should not invent a second, parallel declaration model.
- A query variant is a discovered callable plus query-specific role requirements and registry metadata.

### Rule E: discovery is the canonical declaration phase
- `DiscoveredCallable` is the canonical discovery-phase output.
- Registry-facing descriptor shapes are projections derived from it, not sibling sources of truth.

---

## 3. Discovery Output Model

## 3.1 Base descriptor

```java
record DiscoveredCallable(
    CallableKind kind,
    String exportedName,
    List<String> aliases,
    List<DiscoveredParameter> parameters,
    CallableTraits traits,
    Object implementationHandle,
    SourceOrigin origin
) {}

enum CallableKind {
    TOPLEVEL,
    HOST_CALLABLE,
    QUERY_VARIANT
}
```

## 3.2 Parameter descriptor

```java
record DiscoveredParameter(
    String sourceName,
    ParameterKind kind,
    Object declaredType,
    Object valueKind,
    HostRole<?> hostRole,
    boolean optional
) {}
```

Notes:
- `declaredType` refers to the host-side declaration type.
- `valueKind` is only meaningful for Molang-visible values.
- `hostRole` is only meaningful for receiver/injected-host parameters.
- `CallableTraits` and `SourceOrigin` use the shared meanings defined in `shared-vocabulary-and-phase-ownership-draft.md`.

---

## 4. Suggested Declaration Model

## 4.1 Design goal
- Declarations should be readable to engine authors.
- Discovery should not rely on implicit magic more than necessary.

## 4.2 Draft concepts
- export annotation / declaration
- callable-kind annotation or inferred mode
- parameter-role annotations where required
- trait annotations
- alias support

## 4.3 Example intent

```text
@Query("health")
float health(@Receiver LivingEntity self)

@Query("distance_from_camera")
float distanceFromCamera(@Receiver Entity self, @InjectHost QUERY_RUNTIME runtime)

@Function("math.clamp")
float clamp(float value, float min, float max)
```

This is illustrative only. The exact annotation spellings are not fixed by this document.

---

## 5. Parameter Role Inference Rules

## 5.1 Preferred rule hierarchy
1. explicit annotation/metadata wins
2. callable kind constraints apply
3. limited positional inference applies only where documented
4. otherwise discovery fails

## 5.2 Recommended positional inference
- For host callables/querys, a first host parameter may be inferred as `RECEIVER` if:
  - it is the first non-special parameter,
  - no explicit receiver annotation exists elsewhere,
  - its role is unambiguous.

- Remaining parameters are **not** inferred as host injections by position alone unless the declaration model explicitly allows that.

## 5.3 Bounded special-argument rule
- `INJECTED_HOST` and `SPECIAL_ENGINE_ARG` never come from position alone in this draft.
- If a declaration depends on guessing which parameters are special, discovery fails.

## 5.4 Why inference must stay narrow
- Broad inference creates silent semantic drift.
- Discovery should bias toward explicit failure over “probably what the author meant.”

---

## 6. Callable Kinds

## 6.1 Toplevel callable
- No receiver.
- All user-facing args are visible Molang args.
- Optional special-engine args remain allowed if explicitly declared.

## 6.2 Host callable
- One primary receiver.
- May also require injected host services.
- Exported either under a namespace or a dedicated host-bound surface.

## 6.3 Query variant
- A specialized host callable intended for the query registry.
- Must additionally declare or derive its host-role requirements in a way usable by the query variant registry.

---

## 7. Discovery Pipeline

```mermaid
flowchart LR
    A[Class or declaration source] --> B[Discovery adapter]
    B --> C[Read export metadata]
    C --> D[Resolve parameter roles]
    D --> E[Build DiscoveredCallable]
    E --> F[Normalize into runtime descriptor]
    F --> G[Host callable set / Query variant registry]
```

## 7.1 Discovery adapter
- A discovery adapter may be reflection-backed, generated, or manually registered.
- Engine core should depend on the normalized descriptor result, not on reflection details.

## 7.2 Normalization step
- Normalize aliases, trait defaults, and parameter-role decisions before publishing the callable.

---

## 8. Trait Handling

## 8.1 Trait family
- Keep the same trait family already used in prior drafts:
  - deterministic
  - runtime-enumerable
  - side-effect-free

## 8.2 Discovery responsibility
- Discovery should attach traits explicitly.
- Runtime should not need to infer them from implementation type alone.

## 8.3 Conservative defaults
- If a declaration omits traits, defaults should be conservative unless there is an established project-wide policy.

---

## 9. Query-Specific Normalization

## 9.1 Why queries need extra shaping
- Query variants consume the same callable discovery pipeline, but need query-registry metadata.

## 9.2 Normalized output intent

```java
record DiscoveredQueryVariant(
    DiscoveredCallable callable,
    Set<HostRole<?>> requiredRoles,
    int priority
) {}
```

## 9.3 Ownership rule
- Receiver/injected-host distinction remains owned by the host injection model.
- Query normalization should not redefine parameter-role semantics; it should only project them into registry-friendly metadata.

## 9.4 Visible-arg projection rule
- `VisibleArgSpec` should be derived from `DiscoveredParameter(kind = VISIBLE_ARG)` rather than authored as a second independent declaration model.

---

## 10. Diagnostics

## 10.1 Invalid receiver layout
- More than one receiver candidate without explicit disambiguation should fail discovery.

## 10.2 Invalid visible arg layout
- Host/object types should not silently leak into visible-arg positions.

## 10.3 Missing export metadata
- If a declaration does not define how it becomes visible to Molang, it should stay internal rather than being guessed into the API surface.

---

## 11. Migration From Current Mapping Model

## 11.1 Transitional goal
- Current mapping discovery can remain the source of raw method/class enumeration.
- New discovery should wrap that source and produce normalized descriptors.

## 11.2 Expected migration sequence
1. keep existing discovery entry points
2. add normalized descriptor layer
3. switch binders/registries to consume descriptors
4. retire scope-first/signature-guessing conventions

## 11.3 Explicit non-goal
- This document does not try to freeze the final annotation syntax yet.
- It freezes the semantic obligations of the declaration layer instead.

---

## 12. Open Questions
- Should query declarations have their own dedicated annotation/declaration form, or reuse generic host-callable declarations plus a query export kind?
- Should engine-core discovery be reflection-free in the long term, with generated/manual descriptor registration as the preferred end state?

## 13. Decision Record
- Parameter-role inference is bounded. Explicit metadata is required for injected host and special engine arguments, and receiver inference is only allowed for the first non-special host parameter when it is unambiguous.

## 13. Immediate Follow-Up
- host adapter registry draft
- compatibility semantics matrix
- parser acceptance corpus

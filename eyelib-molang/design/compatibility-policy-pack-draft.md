# Compatibility Policy Pack Draft

## Purpose
- This document defines how compatibility-sensitive Molang behavior is packaged, selected, and applied without polluting the core semantic engine.

## Relationship To Other Docs
- `compatibility-semantics-matrix.md` classifies which behaviors are required, targeted, deferred, or version-sensitive.
- `shared-vocabulary-and-phase-ownership-draft.md` defines the distinction between semantic policy ownership and diagnostics overlays.
- `binder-normalization-contract-draft.md` describes where binder may leave policy-sensitive decisions explicit.
- `strict-debug-diagnostics-mode-draft.md` explains how strict/debug modes interact with compatibility fallbacks.

## Repository Boundary Reminder
- Policy packs are engine-side semantic configuration.
- They are not a replacement for platform-specific runtime wiring.

---

## 1. Why Policy Packs Exist

## 1.1 Core problem
- Molang behavior is not fully formalized and includes:
  - official behavior,
  - community-observed quirks,
  - version-sensitive differences,
  - content-pack expectations.

## 1.2 Design goal
- Keep the core parser/binder/runtime understandable.
- Move variable or quirk-sensitive behavior into explicit policy packs.

---

## 2. Policy Pack Responsibilities

### Pack owns
- targeted or deferred compatibility behaviors
- version-sensitive choices
- optional neutral-fallback policy details
- compatibility decision classification labels used by later diagnostics overlays

### Pack does not own
- basic syntax acceptance
- core AST structure
- host publication mechanics
- registry topology itself
- final warning/error/debug-trace severity mapping

---

## 3. Draft Policy Pack Shape

```java
record CompatibilityPolicyPack(
    String id,
    String versionLabel,
    Object behaviorRules,
    Object decisionMetadata
) {}
```

Exact runtime types remain open; the contract is that a pack is explicit, inspectable, and selectable.

---

## 4. Candidate Policy Areas

| Area | Why it belongs in a pack |
|---|---|
| zero-arg query omission behavior | targeted ecosystem behavior, not core syntax |
| `temp.` struct restrictions | community-observed, policy-sensitive |
| neutral fallback strictness | may vary by environment/use case |
| ternary/version-sensitive behavior | version-sensitive by nature |
| deferred arrow-chain restrictions | insufficiently evidenced for core engine |

---

## 5. Application Points

## 5.1 Binder interaction
- Binder may emit explicit policy-sensitive nodes or deferred reasons.
- Policy pack resolves or classifies them further.

## 5.2 Runtime interaction
- Runtime may consult the active pack when deciding whether to:
  - allow a targeted fallback,
  - choose a neutral default,
  - classify the resulting compatibility posture for later diagnostics overlay handling.

## 5.3 Diagnostics interaction
- Policy pack should classify compatibility decisions in a stable semantic way.
- Diagnostics mode overlay should turn those classifications into:
  - pass
  - info
  - warning
  - error

---

## 6. Draft Evaluation Flow

```mermaid
flowchart LR
    A[Core AST / bound node] --> B[Compatibility decision point]
    B --> C[Active policy pack]
    C --> D[Resolved semantic behavior]
    D --> E[Diagnostics mode overlay]
```

---

## 7. Suggested Pack Profiles

## 7.1 Minimal profile
- Official/core behavior only.
- Avoids community-targeted behaviors unless strongly evidenced.

## 7.2 Compatibility profile
- Includes targeted ecosystem behaviors where the matrix says they are desirable soon.

## 7.3 Debug/migration profile
- Same semantic choices as the selected base pack, but paired with stricter diagnostics overlays.

---

## 8. Policy Pack Selection

## 8.1 Selection should be explicit
- No hidden global guessing.
- Engine invocation/configuration should name the pack directly or select from a clearly documented default.

## 8.2 Recommended fallback
- If no pack is selected, prefer a well-documented default profile rather than silently mixing behaviors.

---

## 9. Example Policy Decisions

## 9.1 Zero-arg query omission
- Core engine: bind as policy-sensitive query-like form
- Policy pack: decide whether omission is accepted silently, warned, or rejected in strict mode

## 9.2 `temp.` structs
- Core engine: parse/bind generically
- Policy pack: decide whether runtime permits, warns, or rejects struct-like usage on `temp`

## 9.3 Neutral fallbacks
- Core/query layer may expose explicit default paths
- Policy pack decides whether absence of a matching non-default path is tolerable or warning-worthy

---

## 10. Testing Implications

## 10.1 Corpus integration
- Executable corpus should be able to name a policy pack when needed.

## 10.2 Example metadata extension

```yaml
policy-pack: compatibility-v1
mode-expectations:
  normal:
    outcome: pass
  strict:
    outcome: diagnostic-warning
```

## 10.3 Why this matters
- Without pack-aware corpus execution, compatibility behaviors become untestable or inconsistent.

---

## 11. Open Questions
- Should policy packs be hierarchical/overridable, or selected as one flat profile at a time?
- How much classification metadata should packs expose for overlays before the model becomes too indirect?
- Do we want policy packs versioned by Minecraft behavior, by project needs, or both?

## 12. Immediate Follow-Up
- corpus linter / runner draft
- runtime specialization contract draft
- policy pack selection/configuration draft

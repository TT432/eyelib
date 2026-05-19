---
name: testing
description: Decision framework for choosing between progressive exploration, unit tests, and smoke tests. Does NOT describe how to write them — see progressive-exploration, unit-test, smoke-test skills for that.
---

## The Three Verification Layers

| Layer | Tool | Trigger | What it catches |
|-------|------|---------|-----------------|
| **Progressive exploration** | AI debug HTTP endpoint | Interactive, step by step | Unknown runtime state, unexpected screens, "what actually happens?" |
| **Unit tests** | JUnit 5 + Gradle | CI / pre-commit / on-demand | Structural invariants, boundary ownership, codec round-trips, null safety |
| **Smoke tests** | ClientSmoke framework | Gradle run configuration | Visual correctness, full lifecycle integration, render output verification |

They form a **funnel, not a pyramid**: exploration is broadest (any question), unit tests are narrowest (specific assertions), smoke tests are deepest (real client rendering).

## The Funnel Model

```
         ┌──────────────────────────────┐
         │   Progressive Exploration    │  ← widest: ask anything at runtime
         │   "Is this screen even open?"│
         └──────────────┬───────────────┘
                        │ narrows to a specific invariant
         ┌──────────────▼───────────────┐
         │      Unit Tests              │  ← narrowest: assert one thing, fast
         │   "This import must not exist"│
         └──────────────┬───────────────┘
                        │ narrows to visual behavior
         ┌──────────────▼───────────────┐
         │      Smoke Tests             │  ← deepest: real rendering, slow
         │   "This region has the right  │
         │    pixel color"               │
         └──────────────────────────────┘
```

## Decision Flow

When you encounter a problem or need to verify something:

1. **Is the answer already in code?** → Read it. Don't run anything.
2. **Is the answer about runtime state or behavior?** → Progressive exploration first. Don't guess.
3. **Now you have a reproducible invariant.** → Write a unit test to lock it in.
4. **Does it involve visual output or full client lifecycle?** → Add a clientSmoke test.

## Why All Three Matter

- **Unit tests alone** tell you code is structurally correct, but not whether runtime state matches expectations. A Mixin may apply but produce wrong behavior.
- **Smoke tests alone** verify integration but are slow and give binary pass/fail — they don't help explore *why* something broke.
- **Exploration alone** is ephemeral — it can't regression-test yesterday's discovery.

A single bug typically requires all three: explore to find the problem → unit test to lock it → smoke test to verify visually.

## Anti-patterns

- Running the client to answer a question that reading the source code can answer
- Writing a smoke test for something a unit test can catch
- Guessing runtime state instead of probing it with the debug endpoint
- Leaving a runtime discovery undocumented (no follow-up unit test)

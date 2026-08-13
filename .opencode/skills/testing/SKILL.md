---
name: testing
description: Decision framework for choosing between progressive exploration, unit tests, and smoke tests. Does NOT describe how to write them — see progressive-exploration, unit-test, smoke-test skills for that.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
---

# testing

Decision framework for choosing between progressive exploration, unit tests, and smoke tests. Does NOT describe how to write them — see progressive-exploration, unit-test, smoke-test skills for that.

## When to use
- When you encounter a problem or need to verify something in this project and must choose a verification method
- Do NOT use when: Does not describe how to write explorations or tests — see progressive-exploration, unit-test, smoke-test skills

## Rules
- NEVER Run the client to answer a question that reading the source code can answer
- NEVER Write a smoke test for something a unit test can catch
- NEVER Guess runtime state instead of probing it with the debug endpoint
- NEVER Leave a runtime discovery undocumented (no follow-up unit test)
- [when 验证目标是回归门禁（“当前输入下测试通过”）] Accept Gradle FROM-CACHE / UP-TO-DATE results: Gradle input tracking is reliable, and a cache hit means identical inputs previously succeeded
- [when 验证目标是行为证据（“本次改动被执行/测量过”，如新测试首次验证、性能测量、运行时诊断）] NEVER Accept test-task FROM-CACHE/UP-TO-DATE or a report showing 0 tests as behavior evidence — it means no test executed this run; instead clear the affected module's build/ directory and the configuration cache (.gradle/configuration-cache), rerun, and cite real execution output
- NEVER Pass --no-build-cache to Gradle: it forces a full rebuild of MC Forge artifacts taking 30+ minutes; instead clear only the affected module's build/ directory and the configuration cache (.gradle/configuration-cache)
- PREFER Treat the three layers as a funnel, not a pyramid: exploration is broadest (any question), unit tests are narrowest (specific assertions), smoke tests are deepest (real client rendering)
- Use progressive exploration (AI debug HTTP endpoint) interactively, step by step, to catch unknown runtime state, unexpected screens, and "what actually happens?" questions
- Use unit tests (JUnit 5 + Gradle, run in CI / pre-commit / on-demand) to catch structural invariants, boundary ownership, codec round-trips, and null safety
- Use smoke tests (ClientSmoke framework, Gradle run configuration) to catch visual correctness, full lifecycle integration, and render output verification
- NEVER Rely on unit tests alone: they tell you code is structurally correct, but not whether runtime state matches expectations (a Mixin may apply but produce wrong behavior)
- NEVER Rely on smoke tests alone: they verify integration but are slow and give binary pass/fail — they don't help explore why something broke
- NEVER Rely on exploration alone: it is ephemeral and can't regression-test yesterday's discovery
- PREFER For a single bug, use all three: explore to find the problem → unit test to lock it → smoke test to verify visually

## Workflow
1. Is the answer already in code? → Read it. Don't run anything. [decision]
2. Is the answer about runtime state or behavior? → Progressive exploration first. Don't guess. [decision]
3. Now you have a reproducible invariant. → Write a unit test to lock it in. [decision]
4. Does it involve visual output or full client lifecycle? → Add a clientSmoke test. [decision]

# Phase 4: Test Execution + Report Generation - Context

**Gathered:** 2026-05-07
**Status:** Ready for planning

<domain>
## Phase Boundary

Close the end-to-end client smoke test pipeline — add TEST_EXEC, REPOSITION, and REPORT states to the state machine, implement test class loading (Class.forName()) and constructor-based execution with failure isolation, sort tests by priority, and generate a Gson-based JSON report summarizing all results. The report is written to disk in the REPORT state before the EXIT state triggers JVM shutdown, ensuring report survival across Runtime.halt(0).

</domain>

<decisions>
## Implementation Decisions

### Test Execution Contract
- **D-01:** Constructor-as-test — `Class.forName()` → `getDeclaredConstructor().newInstance()`. The no-arg constructor body IS the test. No interface or abstract base class required.
- **D-02:** Tests access Minecraft via `Minecraft.getInstance()` directly — no framework injection or constructor parameters.
- **D-03:** No interface required. `@ClientSmoke`-annotated classes only need a public no-arg constructor.
- **D-04:** Execution entry point = constructor body. Object creation IS test execution (class-level granularity, v1 scope).

### State Machine Integration
- **D-05:** Flow: STABILIZE → TEST_EXEC → REPOSITION → HUD_HIDE → SCREENSHOT → (loop back to TEST_EXEC if more tests) → REPORT → EXIT
- **D-06:** Three new enum values: TEST_EXEC, REPOSITION, REPORT — added to ClientSmokeState between SCREENSHOT and EXIT.
- **D-07:** Loop termination: `testIndex >= discoveredTests.size()` — consistent with Phase 3 SCREENSHOT handler guard.
- **D-08:** REPOSITION keeps `testIndex` as-is at the current test position; the existing SCREENSHOT handler already reads `discoveredTests[testIndex].className()` for file naming, so no index manipulation needed in REPOSITION. REPOSITION acts as a semantic "loop back to top" without state mutation.

### Error Handling
- **D-09:** Test constructor throws → catch exception, record as failure in report, log at WARN level, continue to next test via REPOSITION. Never abort on single test failure.
- **D-10:** ClassNotFoundException → mark as failed with error "class not found: {className}", WARN log, advance to next test.
- **D-11:** No timeout mechanism in v1. If a test hangs, `Runtime.halt(0)` is the ultimate guarantee. Per v1 out-of-scope: performance testing not in scope.
- **D-12:** Error formatting: `e.toString()` + `e.getMessage()` + first 5 lines of stack trace as a single string. Compact but informative.

### JSON Report
- **D-13:** REPORT state writes report synchronously to disk, then transitions to EXIT. Report is fully flushed before halt(0).
- **D-14:** Report filename: `report-{yyyyMMdd-HHmmss}.json` — consistent with Phase 3 D-14 screenshot naming.
- **D-15:** Entry fields: `className` (FQCN), `description`, `priority`, `status` ("passed"/"failed"), `durationMs` (long), `error` (object with `message` and `stackTrace`, or null if passed).
- **D-16:** JSON library: Gson (com.google.gson) — already available via Minecraft/Forge 1.20.1 transitive dependencies.

### the agent's Discretion
- Exact `handleTestExec()` implementation details (try-catch structure, result accumulation)
- Report JSON structure (top-level fields: `totalTests`, `passed`, `failed`, `timestamp`, `entries` array)
- REPOSITION handler implementation (may be a no-op or minimal state reset)
- Whether to accumulate results in `List<TestResult>` or write incrementally

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`DiscoveredTest` record**: className, description, priority, modId — Phase 4 reads these for loading and sorting
- **`ClientSmokeStateMachine.testIndex`**: already maintained by Phase 3 SCREENSHOT handler — Phase 4 initializes it and uses it for iteration
- **`transitionTo(newState, reason)`**: existing helper — reused for all Phase 4 state transitions
- **`ClientSmokeConfig`**: all config fields available, no new config entries needed for Phase 4
- **`ClientSmokeState` enum**: 11 values (INIT through EXIT + ERROR), 3 new values to insert
- **`handleStabilize()`**: already waits for Phase 4 to drive TEST_EXEC when `testIndex < discoveredTests.size()`

### Established Patterns
- **Single switch dispatch**: `onClientTick()` switch statement handles all states — Phase 4 adds 3 new case arms
- **Tick-gated transitions**: one state transition per tick, no busy-waiting
- **Terminal guard**: `IDLE || ERROR → return` — EXIT still NOT in guard, REPORT works same way
- **One-shot log guard**: apply stabilizeCompleteLogged pattern to avoid per-tick log spam in REPORT/REPOSITION
- **Forge 1.20.1 event pattern**: single `@Mod.EventBusSubscriber` class with multiple `@SubscribeEvent` methods

### Integration Points
- **STABILIZE → TEST_EXEC handoff**: `handleStabilize()` stays in STABILIZE when `testIndex < discoveredTests.size()`. Phase 4's handler needs to check this condition and transition to TEST_EXEC
- **SCREENSHOT → loop back**: existing `onRenderLevelStage()` increments `testIndex++` and checks `testIndex < discoveredTests.size()` → transitions to HUD_HIDE. Phase 3's this-mechanism stays intact; Phase 4 flows: SCREENSHOT → (loop) TEST_EXEC
- **REPORT → EXIT handoff**: REPORT writes JSON then transitions to EXIT. EXIT handler (Phase 3) handles mc.stop() + halt(0)
- **Gson availability**: Forge 1.20.1 runtime classpath includes `com.google.gson` via Minecraft's transitive dependencies — verify compileOnly or implementation scope

</code_context>

<specifics>
## Specific Ideas

- 测试结果应累积在内存中的 `List<TestResult>` 记录中，并在 REPORT 状态时一次性序列化为 JSON
- `handleStabilize()` 当前的 `testIndex < discoveredTests.size()` guard 需要改为驱动 TEST_EXEC 转换，而不是静默停留在 STABILIZE 状态
- 优先级排序应在 TEST_EXEC 第一次进入时完成（使用 `discoveredTests.sort()` 或等效方式），后续迭代保持已排序顺序
- 计时：在 `handleTestExec()` 中使用 `System.currentTimeMillis()` 记录 `executionStartMs`，计算 `durationMs = System.currentTimeMillis() - executionStartMs`
- 内存中的 `List<TestResult>` 保证报告在 halt(0) 之前持久化，解决 Phase 4 成功标准 #5

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 4-Test Execution + Report Generation*
*Context gathered: 2026-05-07*

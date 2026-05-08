# Domain Pitfalls — ClientSmoke Gradle Task Automation

**Domain:** Gradle task automation for Minecraft mod client-side smoke testing  
**Researched:** 2026-05-08  
**Confidence:** HIGH

## Critical Pitfalls

Mistakes that cause rewrites or major issues.

### Pitfall 1: Hacking Forge's Config Loading Instead of Adding Application-Level Override

**What goes wrong:** Developer tries to inject values into Forge's `ForgeConfigSpec` internal `BitSet` (which stores which config values are "user-defined" vs "default"), or reflectively calls `ModConfig`'s `ConfigFileTypeHandler` to programmatically set config values at runtime. This appears to work in initial testing but breaks when:
- Forge updates change internal representation
- Config file is reloaded (e.g., via `/reload` command or mod reload event) and overwrites injected values
- Multiple mods try the same hack and conflict

**Why it happens:** The temptation is strong because "Forge already reads config, I just need to write to it." But Forge's config system is designed for user-editable files, not programmatic injection. The internal APIs (`BitSet`, `ConfigValue#set()`) are implementation details, not public API.

**Consequences:** Silent configuration corruption; tests not actually enabled despite appearing so; breakage on Forge version updates; impossible to debug because the failure is in Forge's internal state, not user code.

**Prevention:** Add a thin wrapper method in `ClientSmokeConfig` that checks `System.getProperty()` before falling back to `ForgeConfigSpec.get()`. This is application-level code in your own class — fully under your control, trivially testable, and immune to Forge internal changes.

**Detection:** Any code that references `ForgeConfigSpec` internal classes (`ConfigValue#set()`, `BitSet`, `ConfigFileTypeHandler`) or uses reflection to access Forge config internals is a red flag.

### Pitfall 2: Not Changing the Dependency Configuration (Class Not Found at Runtime)

**What goes wrong:** The new `runClientSmoke` Gradle task is created, but `eyelib-clientsmoke` is still gated behind `if (enableSmokeTest == 'true') { localRuntime ... }`. When the task runs, MDGL launches the JVM but the `clientsmoke` mod classes are not on the classpath. Forge either silently skips the mod (if `mods.toml` isn't on classpath), or crashes with `ClassNotFoundException` for `ClientSmokeMod` (if `mods.toml` is found but classes aren't).

**Why it happens:** The developer focuses on the run config and system properties, forgetting that the classpath assembly is a separate concern controlled by Gradle dependency configurations. The `enableSmokeTest` property is a build-time gate, not a runtime gate.

**Consequences:** `runClientSmoke` task launches Minecraft but the smoke test framework never activates. Or worse: `runClientSmoke` crashes on startup with a cryptic classloading error. Both defeat the "one-click" promise.

**Prevention:** Remove the `if (enableSmokeTest == 'true')` guard from the `localRuntime` dependency declaration. Always include `eyelib-clientsmoke` on the runtime classpath. The runtime gating is handled by `ClientSmokeConfig.isEnabled()` returning `false` by default (when no system property is set). This is safe because the `@Mod` constructor guards all smoke test logic behind the `isEnabled()` check.

**Detection:** Verify that `./gradlew runClientSmoke` works immediately after a `clean` build, without first running `runClient` or manually setting any properties. Also verify that `./gradlew runClient` (without smoke) still works and shows no smoke test log lines.

### Pitfall 3: Exit Code Not Propagated Through `Runtime.halt()`

**What goes wrong:** The state machine calls `Runtime.getRuntime().halt(0)` unconditionally (current v1.0 behavior). Gradle always sees exit code 0 → BUILD SUCCESSFUL. A smoke test run where all tests failed is reported as "BUILD SUCCESSFUL" by Gradle, misleading CI pipelines.

**Why it happens:** The v1.0 design used `halt(0)` as an intentional simplification — the assumption was that report inspection (human review) would determine pass/fail. v1.1 adds automation, which needs machine-readable pass/fail via exit code. The fix is mechanically simple (read `testResults`, compute aggregate, pass to `halt()`) but easy to overlook.

**Consequences:** CI pipeline green-lights a broken build. Test failures go unnoticed until someone manually reviews the JSON report. Defeats the purpose of automated smoke testing.

**Prevention:** In `handleExit()`, after the countdown reaches 60 ticks and before calling `halt()`, compute:
```java
boolean allPassed = testResults.stream()
    .allMatch(r -> "passed".equals(r.status()));
int exitCode = allPassed ? 0 : 1;
Runtime.getRuntime().halt(exitCode);
```
Also handle the edge case where `testResults` is empty (no tests discovered) — this should probably be exit code 0 (not a failure, just nothing to test) with a clear log message.

**Detection:** Run `runClientSmoke` with a deliberately failing test. Verify that `./gradlew runClientSmoke` reports BUILD FAILED (not BUILD SUCCESSFUL). Check the exit code with `echo $?` (or `echo %ERRORLEVEL%` on Windows CMD, `echo $LASTEXITCODE` on PowerShell).

### Pitfall 4: No Tests Found → IDLE → Gradle Task Hangs Forever

**What goes wrong:** The Gradle task force-enables smoke testing, but no `@ClientSmoke`-annotated classes exist on the classpath. The state machine flows INIT → CONFIG_LOAD → SCAN → **IDLE** (via `handleScan()`'s empty-test guard). The JVM stays alive with a running Minecraft client on the main menu (or in a blank world), and the Gradle task hangs indefinitely — waiting for a JVM process that will never exit.

**Why it happens:** The v1.0 `handleScan()` logic was designed for manual invocation: "no tests → go idle, let the human decide what to do." In the automation context, there is no human. The IDLE state is a **blocking terminal state** — it halts the state machine but does nothing to exit the JVM.

**Consequences:** CI job hangs until the CI system's global timeout kills it (often 30-60 minutes later). The smoke test task appears to be "still running." No report, no exit code, no feedback.

**Prevention:** When running in force-exit mode (`shouldExitAfterSmoke()` returns `true`), the "no tests" case must transition to EXIT, not IDLE:
```java
private static void handleScan() {
    if (discoveredTests.isEmpty()) {
        if (ClientSmokeConfig.shouldExitAfterSmoke()) {
            LOGGER.info("[ClientSmoke] No tests found — exiting (force-exit mode)");
            transitionTo(ClientSmokeState.REPORT, "No tests — generating empty report then exiting");
        } else {
            transitionTo(ClientSmokeState.IDLE, "No @ClientSmoke tests found");
        }
        return;
    }
    transitionTo(ClientSmokeState.WORLD_CREATE, "Tests found — creating test world");
}
```
The REPORT state generates an empty report (`totalTests=0, passed=0, failed=0`), then transitions to EXIT as normal.

**Detection:** Run `./gradlew runClientSmoke` with no annotated test classes on the classpath. Verify the task completes (doesn't hang) and the exit code is 0.

## Moderate Pitfalls

### Pitfall 5: `Runtime.halt()` on Windows and Gradle Exit Code Capture

**What goes wrong:** On Windows, Gradle's `JavaExec` task may not correctly capture the exit code from `Runtime.halt()`. This is because `halt()` bypasses the normal JVM shutdown sequence — the JVM terminates immediately without returning through the normal `main()` → `System.exit()` path. Some Gradle-on-Windows configurations have had issues detecting exit codes from abruptly terminated JVM processes.

**Prevention:** 
- Test on Windows specifically with both `cmd` and PowerShell
- Verify `gradlew runClientSmoke` exit code with `echo %ERRORLEVEL%` after a failed test run
- If Gradle can't capture the code, consider writing the exit code to a marker file (e.g., `build/smoke-exit-code.txt`) that a `doLast` hook reads
- Fallback plan: use `System.exit(exitCode)` instead of `halt()` — `exit()` runs shutdown hooks, which can hang in modded Minecraft, but may be more reliably detected by Gradle on Windows

**Detection:** Run on actual Windows machine (not WSL). Test both pass and fail scenarios.

### Pitfall 6: `ClientSmokeConfig.ENABLED` Still Being Called Directly Instead of `isEnabled()`

**What goes wrong:** Some call sites in the codebase still call `ClientSmokeConfig.ENABLED.get()` directly (the ForgeConfigSpec value), bypassing the new `isEnabled()` method that checks system properties. The system property override is set correctly, but the code never reads it.

**Why it happens:** The v1.0 code has `ENABLED.get()` at multiple call sites: `ClientSmokeMod` constructor, `ClientSmokeStateMachine.handleInit()`, `ClientSmokeScanner.scan()`. If any one of these is missed during the v1.1 migration, there's a partial override situation — some code paths see the override, others don't.

**Consequences:** Inconsistent behavior. For example, if `ClientSmokeMod` constructor calls `isEnabled()` (correct) but `handleInit()` still calls `ENABLED.get()` (incorrect), the mod starts scanning but the state machine immediately transitions to IDLE because it reads the TOML value (false). Silent failure with confusing log output.

**Prevention:** 
- Make `ENABLED` field `private` (currently `public static final`) so direct access from outside `ClientSmokeConfig` is a compile error
- Grep the entire codebase for `ENABLED.get()` and `EXIT_AFTER_SMOKE.get()` and replace every occurrence
- Add a unit test that sets the system property and verifies `isEnabled()` returns `true` regardless of what `ENABLED.get()` would return

**Detection:** Search for `ENABLED.get()` across the project. Any hit outside `ClientSmokeConfig.isEnabled()` itself is a bug.

### Pitfall 7: Hardcoding `clientsmoke-reports/` Path Instead of Respecting System Property Override

**What goes wrong:** D5 (configurable output directory) is implemented partially: `ClientSmokeConfig` adds a system property check for the report directory, but `handleScreenshot()` or `handleReport()` still hardcodes `FMLPaths.GAMEDIR.get().resolve("clientsmoke-reports")` — ignoring the override.

**Prevention:** Create a single static method `ClientSmokeConfig.getReportDir()` that encapsulates the resolution logic (system property → default). Both `handleScreenshot()` and `handleReport()` call this single method — no duplicate path construction.

## Minor Pitfalls

### Pitfall 8: Forgetting `.gitignore` for Smoke Test Artifacts

**What goes wrong:** `clientsmoke-reports/` and `build/smoke-reports/` are not in `.gitignore`. Developers running smoke tests locally see dirty git status. Someone accidentally commits a PNG screenshot or JSON report.

**Prevention:** Add to root `.gitignore`:
```
# Client smoke test artifacts
clientsmoke-reports/
build/smoke-reports/
```

### Pitfall 9: IDE Run Configuration Name Conflict

**What goes wrong:** If `ideName` is omitted from the `clientSmoke` run config, MDGL may auto-name it identically to the existing `client` run (both are `client()` type). IntelliJ shows duplicate entries or overwrites one.

**Prevention:** Always set a unique `ideName` for the smoke test run:
```groovy
clientSmoke {
    client()
    ideName = "Run Client Smoke Tests"
}
```

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Gradle task creation | #2: Classpath not including clientsmoke mod | Remove `enableSmokeTest` gate on `localRuntime`. Verify with clean build. |
| Config override | #1: Hacking Forge config internals | Use simple `System.getProperty()` wrapper. No reflection, no Forge internals. |
| Config override | #6: Missed `ENABLED.get()` call sites | Make `ENABLED` private. Grep for all call sites. Unit test the override. |
| No-tests hang | #4: State machine goes IDLE, task hangs forever | When `shouldExitAfterSmoke()`, transition to EXIT not IDLE on empty tests. |
| Exit code | #3: `halt(0)` unconditionally | Aggregate `testResults` before `halt()`. Test with deliberately failing test. |
| Exit code | #5: Windows exit code capture | Test on actual Windows. Have `System.exit()` fallback ready. |
| Output paths | #7: Hardcoded path ignoring override | Single `getReportDir()` method, called from both screenshot and report handlers. |
| Repository hygiene | #8: Missing `.gitignore` entries | Add both default paths. |
| IDE integration | #9: Duplicate run config names | Set unique `ideName`. |

## Sources

- **Forge 1.20.1 ForgeConfigSpec source** — Internal `BitSet` tracking of user-defined values (HIGH confidence — verified via Forge 47.1.3 source analysis)
- **JDK 17 `Runtime.halt()` vs `System.exit()` documentation** — https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Runtime.html#halt(int) (HIGH confidence — official JavaDoc)
- **Gradle `JavaExec` exit code handling on Windows** — Known community issue (MEDIUM confidence — multiple StackOverflow/Gradle forum discussions; needs local verification)
- **MDGL `ideName` documentation** — https://github.com/neoforged/ModDevGradle/blob/main/README.md#runs (HIGH confidence — official docs, verified against plugin version 2.0.91)
- **Existing `ClientSmokeConfig.java`** — Call sites for `ENABLED.get()` and `EXIT_AFTER_SMOKE.get()` (HIGH confidence — primary source, directly analyzed)

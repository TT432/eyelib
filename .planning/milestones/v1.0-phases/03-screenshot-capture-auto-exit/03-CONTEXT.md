# Phase 3: Screenshot Capture + Auto-Exit - Context

**Gathered:** 2026-05-07
**Status:** Ready for planning

## Phase Boundary

Implement the output pipeline for the client smoke testing framework — extend the existing state machine with HUD_HIDE / SCREENSHOT / EXIT states, capture clean screenshots (no HUD) on the render thread via `RenderLevelStageEvent.AFTER_LEVEL` using custom framebuffer read + `NativeImage.write()`, output timestamped PNG files to `clientsmoke-reports/screenshots/`, and implement graceful two-phase JVM exit with `mc.stop()` + 3-second delay + `Runtime.getRuntime().halt(0)` when `exitAfterSmoke=true`.

## Implementation Decisions

### State Machine Wiring

- **D-01:** 直接扩展 `ClientSmokeState` enum，在现有 INIT/IDLE/CONFIG_LOAD/SCAN/WORLD_CREATE/WORLD_WAIT/STABILIZE/ERROR 之后追加 HUD_HIDE / SCREENSHOT / EXIT。同一个 enum，同一个 switch 分发。
- **D-02:** HUD_HIDE 和 SCREENSHOT 必须是两个独立状态（不是一个合并状态）。HUD_HIDE 的 tick handler 设 `hideGui=true` 后 transitionTo(SCREENSHOT)；同帧的 `RenderLevelStageEvent.AFTER_LEVEL` handler 检查 `state == SCREENSHOT` 时截图并恢复 `hideGui=false`。
- **D-03:** 测试循环流程（所有测试在同一个世界里）：STABILIZE(Phase2) → TEST_EXEC(Phase4) → HUD_HIDE → SCREENSHOT → NEXT_TEST(回到 TEST_EXEC 如果还有) → EXIT。Phase 3 实现 HUD_HIDE / SCREENSHOT / EXIT，Phase 4 实现 TEST_EXEC / NEXT_TEST。
- **D-04:** EXIT 状态内部倒计数：进入 EXIT 时记录 `exitStartTick`。tick 0 调用 `minecraft.stop()`；后续每个 tick 检查 `elapsedTicks < 60`（3秒 @ 20tps）；达到后调用 `Runtime.getRuntime().halt(0)`。

### Screenshot Capture API

- **D-05:** `RenderLevelStageEvent` 的 `@SubscribeEvent` 方法直接放在 `ClientSmokeStateMachine` 类内（该类已经是 `@Mod.EventBusSubscriber`）。方法内检查 `state == SCREENSHOT` 时才执行截图。
- **D-06:** 不使用 vanilla `Screenshot.grab()`。改为：从 `minecraft.getMainRenderTarget()` 读取 framebuffer，创建 `NativeImage`，用 `NativeImage.write()` 直接输出 PNG。完全控制命名和路径，无聊天消息，无自动递增。
- **D-07:** 截图阶段为 `RenderLevelStageEvent.AFTER_LEVEL`。确保所有 MOD 渲染、实体、粒子、后处理都已完成后才截取。
- **D-08:** PNG 编码和文件写入直接在渲染线程完成（在 `RenderLevelStageEvent` handler 中）。PNG 编码 <10ms，不阻塞帧预算。不需要异步线程池。

### HUD Hiding Timing

- **D-09:** 仅通过 `Minecraft.getInstance().options.hideGui = true` 隐藏 HUD，无需额外处理聊天、调试屏幕或其他覆盖层。
- **D-10:** `hideGui` 恢复在 SCREENSHOT 状态的 tick handler（`TickEvent.Phase.START`）中完成，紧接在同帧的 `RenderLevelStageEvent` handler 截图之后。
- **D-11:** HUD_HIDE 状态在 `TickEvent.Phase.START` 中设 `hideGui=true`。Minecraft 渲染管线在同一帧的 tick 之后读取这个值，确保 HUD 在当前帧就被隐藏。
- **D-12:** HUD_HIDE 的 tick handler 执行完 `hideGui=true` 后立即 `transitionTo(SCREENSHOT)`。同帧的 `RenderLevelStageEvent.AFTER_LEVEL` handler 检查 `if (state != SCREENSHOT) return` 后执行截图+恢复。

### File Naming and Output

- **D-13:** 截图输出到游戏运行目录的相对路径 `./clientsmoke-reports/screenshots/`。开发环境位于 `run/clientsmoke-reports/screenshots/`。
- **D-14:** 文件名格式：`{SimpleClassName}-{yyyyMMdd-HHmmss}.png`。例如 `FooTest-20260507-134659.png`。使用 `java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))`。
- **D-15:** 类名部分使用 `Class.getSimpleName()`（仅简名，不含包名）。完整包名信息由 Phase 4 的 JSON 报告提供。
- **D-16:** 使用 `Files.createDirectories()` 自动创建输出目录树。同名文件（极罕见：同一测试类+同一秒内两次截图）直接覆盖。

### the agent's Discretion

- `NativeImage` 的 framebuffer 数据读取方式（`RenderTarget.readColorPixel()` 或其他方法）—— 由 planner/researcher 根据 Forge 1.20.1 API 确定。
- 状态枚举的命名：TEST_EXEC / NEXT_TEST 的实际枚举值名由 Phase 4 决定，Phase 3 只需知道有这两个占位状态。
- EXIT 状态 tick 计数器和 mc.stop() 调用是否使用 try-catch 包装——默认为安全起见包装。

## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Context
- `.planning/PROJECT.md` — Project description, core value, constraints, key decisions
- `.planning/REQUIREMENTS.md` — v1 requirements (CAP-01–03, EXIT-01–02 mapped to Phase 3)
- `.planning/ROADMAP.md` — Phase 3 success criteria (5 criteria), complete state machine flow
- `.planning/STATE.md` — Current position (Phase 2 complete)

### Existing Code — State Machine
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeState.java` — State enum (INIT → STABILIZE, must extend)
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java` — `@EventBusSubscriber` tick handler + switch dispatch (must add new states + RenderLevelStageEvent handler)
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/ClientSmokeMod.java` — `@Mod` constructor, scanner→state machine bridge (`setDiscoveredTests`)
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/config/ClientSmokeConfig.java` — `ForgeConfigSpec` with `ENABLED`, `SCREENSHOT_DELAY`, `RELOAD_STABILIZE_TICKS`, `EXIT_AFTER_SMOKE`

### Existing Code — Scanner
- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/scanner/ClientSmokeScanner.java` — `DiscoveredTest` record (contains className, priority etc., consumed by state machine)

### Minecraft/Forge API Reference
- `net.minecraftforge.client.event.RenderLevelStageEvent` — Forge 1.20.1 render event, stage constants (AFTER_LEVEL target)
- `net.minecraftforge.event.TickEvent.ClientTickEvent` — Existing tick event, Phase.START
- `com.mojang.blaze3d.pipeline.RenderTarget` — Framebuffer access (`getMainRenderTarget()` → read pixels)
- `com.mojang.blaze3d.platform.NativeImage` — Pixel data container, `write()` for PNG output
- `net.minecraft.client.Minecraft` — `options.hideGui`, `stop()`, `getMainRenderTarget()`
- `System.exit(0)` / `Runtime.getRuntime().halt(0)` — JVM exit

### Earlier Phase Artifacts
- `.planning/phases/01-module-scaffolding-config-annotation-discovery/01-CONTEXT.md` — Phase 1 decisions (config entries D-13 relevant: screenshotDelay, reloadStabilizeTicks, exitAfterSmoke)
- `.planning/research/PITFALLS.md` — Class loading safety, screenshot thread requirements

## Existing Code Insights

### Reusable Assets
- **`ClientSmokeStateMachine.onClientTick()`**: Existing `@SubscribeEvent` method with `switch(state)` dispatch. New `HUD_HIDE` / `SCREENSHOT` / `EXIT` case handlers follow the same pattern as existing `handle*()` methods.
- **`transitionTo(newState, reason)`**: Existing helper method for state transitions with logging. Reuse for all new state transitions.
- **`ClientSmokeConfig`**: `SCREENSHOT_DELAY` and `EXIT_AFTER_SMOKE` config values already defined. Phase 3 reads these at runtime to control behavior.
- **`DiscoveredTest`**: Scanner returns `List<DiscoveredTest>` with className, priority, modId. State machine already stores this via `setDiscoveredTests()`. Phase 3 screenshot handler reads className for file naming.

### Established Patterns
- **Single @EventBusSubscriber class**: Forge 1.20.1 pattern `@Mod.EventBusSubscriber(modid=..., value=Dist.CLIENT)` + nested `@SubscribeEvent` static methods. Phase 3 adds one more `@SubscribeEvent` for `RenderLevelStageEvent` in the same class.
- **Tick-gated state transitions**: Each tick handles at most one state transition. No busy-waiting or recursive calls.
- **Terminal-state short-circuit**: `if (state == IDLE || state == ERROR) return` — EXIT should be added to this guard.
- **One-shot log guard**: `stabilizeCompleteLogged` pattern — apply similar pattern for EXIT completion logging.

### Integration Points
- **STABILIZE → Phase 3 handoff**: After `handleStabilize()` completes, the state machine currently stays in STABILIZE. Phase 3 must add `HUD_HIDE` to the switch and define how STABILIZE transitions out (either STABILIZE → TEST_EXEC or a Phase 3 trigger).
- **Screenshot reads from `discoveredTests`**: The SCREENSHOT handler needs the current test's class name from `discoveredTests[testIndex]`. This depends on Phase 4's test execution loop managing `testIndex`.
- **EXIT state and tick event**: EXIT state calls `mc.stop()` which may stop the tick event loop. The `Runtime.halt(0)` is the final fallback to ensure JVM terminates.

## Specific Ideas

- `handleStabilize()` 完成后不应该自动进入 Phase 3。加一个占位检查：`if (testIndex >= discoveredTests.size()) → EXIT`，否则先等 Phase 4 实现 TEST_EXEC 后再推进。
- SCREENSHOT 状态的 `RenderLevelStageEvent` handler 应该检查 `state == SCREENSHOT` 且当前帧还没截过图（防重复，用 `screenshotTakenThisFrame` boolean）。
- `minecraft.options.hideGui` 的修改应该记录是否改变了用户原有的 F1 设置。虽然 v1 不保存原值，但不应引入副作用。

## Deferred Ideas

None — discussion stayed within phase scope.

---

*Phase: 3-Screenshot Capture + Auto-Exit*
*Context gathered: 2026-05-07*

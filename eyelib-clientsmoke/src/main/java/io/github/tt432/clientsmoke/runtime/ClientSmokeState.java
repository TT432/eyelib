package io.github.tt432.clientsmoke.runtime;

/**
 * Tick-driven state machine states for the {@link ClientSmokeStateMachine}.
 *
 * <p>The state machine consumes this enum via a {@code switch} statement in the
 * {@code onClientTick} handler. Each tick processes exactly one state transition.
 * Terminal states ({@link #IDLE}, {@link #ERROR}) halt further processing.</p>
 *
 * <p>Phase 3 states ({@link #HUD_HIDE}, {@link #SCREENSHOT}, {@link #EXIT}) implement
 * the HUD-hiding, framebuffer capture, and graceful JVM exit pipeline.
 * Phase 4 states ({@link #TEST_EXEC}, {@link #REPOSITION}, {@link #REPORT})
 * implement the test execution loop and JSON report generation.</p>
 */
public enum ClientSmokeState {

    /**
     * Entry point. On the first client tick, checks {@code ClientSmokeConfig.ENABLED}.
     *
     * <ul>
     *   <li>If enabled=false &rarr; transitions to {@link #IDLE}</li>
     *   <li>If enabled=true  &rarr; transitions to {@link #CONFIG_LOAD}</li>
     * </ul>
     */
    INIT,

    /**
     * Terminal state when the framework is disabled ({@code enabled=false}).
     * No further tick processing occurs.
     */
    IDLE,

    /**
     * Config verification. The {@code ForgeConfigSpec} was already parsed and
     * validated during mod construction (Phase 1). Logs current config values
     * and transitions immediately to {@link #SCAN}.
     */
    CONFIG_LOAD,

    /**
     * Scanner results verification. The bytecode scanner ran during mod
     * construction (Phase 1) and results are stored in the state machine's
     * {@code discoveredTests} field.
     *
     * <ul>
     *   <li>If tests found &rarr; transitions to {@link #WORLD_CREATE}</li>
     *   <li>If empty        &rarr; transitions to {@link #IDLE}</li>
     * </ul>
     */
    SCAN,

    /**
     * World creation or reuse. Creates a new creative superflat world (or reuses
     * an existing one with the same name) and joins it. Implemented in Plan 02-02.
     *
     * <ul>
     *   <li>On success &rarr; transitions to {@link #WORLD_WAIT}</li>
     *   <li>On failure &rarr; transitions to {@link #ERROR}</li>
     * </ul>
     */
    WORLD_CREATE,

    /**
     * Player spawn wait. Polls {@code Minecraft.getInstance().player} and
     * {@code .level} until the player entity is spawned and the client world
     * is fully loaded. No log spam — silent polling each tick.
     *
     * <ul>
     *   <li>When player != null and level != null &rarr; transitions to {@link #STABILIZE}</li>
     * </ul>
     */
    WORLD_WAIT,

    /**
     * Render stabilization delay. Waits {@code RELOAD_STABILIZE_TICKS} (default 40)
     * after player spawn before declaring readiness. Chunks and entities need time
     * to render and settle.
     *
     * <p>After stabilization completes, the state machine stays in this state.
     * Phase 3 picks up from {@code STABILIZE} to begin test execution.</p>
     */
    STABILIZE,

    /**
     * Pre-screenshot HUD hiding. Sets {@code minecraft.options.hideGui = true}
     * in the tick handler and immediately transitions to {@link #SCREENSHOT}.
     * The next frame's render pass reads the new {@code hideGui} value and
     * renders without HUD elements.
     *
     * <p>Per D-02: HUD_HIDE and SCREENSHOT are separate states to guarantee
     * that the HUD toggle takes effect before the capture frame.</p>
     */
    HUD_HIDE,

    /**
     * Screenshot capture on the render thread. The {@code RenderLevelStageEvent}
     * handler captures the main framebuffer into a {@code NativeImage}, writes
     * it as a timestamped PNG to {@code clientsmoke-reports/screenshots/}, and
     * restores {@code hideGui} to {@code false} after capture.
     *
     * <p>Per D-06: Uses custom framebuffer read + {@code NativeImage.write()},
     * NOT the vanilla {@code Screenshot.grab()} method.</p>
     */
    SCREENSHOT,

    /**
     * Test execution phase. Loads the test class via {@code Class.forName()},
     * instantiates it via its no-arg constructor, times the execution, and
     * records the result (pass/fail with duration and error details).
     *
     * <p>Per D-01: Constructor body IS the test — no interface or method
     * contract required. The {@code @ClientSmoke}-annotated class only needs
     * a public no-arg constructor.</p>
     *
     * <p>Per D-02: Tests access Minecraft resources via
     * {@code Minecraft.getInstance()} — no injection or constructor parameters.</p>
     *
     * <p>Per D-09/D-10: Exceptions are captured, recorded as failures, and
     * the state machine advances to {@link #REPOSITION} — subsequent tests
     * continue executing without interruption.</p>
     */
    TEST_EXEC,

    /**
     * Loop-back anchor between screenshot capture and the next test.
     * Transitions to {@link #HUD_HIDE} to begin the next screenshot cycle.
     *
     * <p>Per D-08: This is a pass-through state — no state mutation occurs.
     * It exists as a semantic marker in the state flow graph, clarifying
     * the boundary between "test completed + screenshot taken" and
     * "capture next test's screenshot".</p>
     */
    REPOSITION,

    /**
     * Report generation. Serializes all accumulated {@code TestResult}
     * entries via Gson and writes a JSON report to
     * {@code clientsmoke-reports/report-{timestamp}.json}.
     *
     * <p>Per D-13: Report is written synchronously before transitioning
     * to {@link #EXIT}, ensuring the file is fully on disk before
     * {@code Runtime.getRuntime().halt(0)} fires.</p>
     */
    REPORT,

    /**
     * Graceful two-phase JVM exit. On entry, calls {@code mc.stop()} for
     * Forge cleanup. After a 60-tick (3s @ 20 TPS) countdown, calls
     * {@code Runtime.getRuntime().halt(0)} to force-terminate the JVM.
     *
     * <p>Per D-03: reached after all test screenshots are captured.
     * Per D-04: uses tick-counted countdown with halt() as final guarantee.</p>
     */
    EXIT,

    /**
     * Terminal state for failure recovery. Logs the error message at ERROR level
     * and halts all further state machine processing.
     */
    ERROR
}

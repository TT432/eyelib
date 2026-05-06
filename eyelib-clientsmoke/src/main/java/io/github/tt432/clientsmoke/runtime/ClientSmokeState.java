package io.github.tt432.clientsmoke.runtime;

/**
 * Tick-driven state machine states for the {@link ClientSmokeStateMachine}.
 *
 * <p>The state machine consumes this enum via a {@code switch} statement in the
 * {@code onClientTick} handler. Each tick processes exactly one state transition.
 * Terminal states ({@link #IDLE}, {@link #ERROR}) halt further processing.</p>
 *
 * <p>States beyond {@link #STABILIZE} (TEST_EXEC, SCREENSHOT, NEXT_TEST, REPORT, EXIT)
 * are defined in Phase 3+4.</p>
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
     * Terminal state for failure recovery. Logs the error message at ERROR level
     * and halts all further state machine processing.
     */
    ERROR
}

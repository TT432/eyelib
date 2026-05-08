package io.github.tt432.clientsmoke.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for the Client Smoke Test framework.
 *
 * <p>Uses Forge 1.20.1 {@link ForgeConfigSpec} (equivalent to NeoForge's {@code ModConfigSpec}).
 * Config is registered as {@link net.minecraftforge.fml.config.ModConfig.Type#COMMON} and
 * persisted to {@code config/clientsmoke-common.toml} in the game directory.</p>
 *
 * <p><strong>Master switch:</strong> When {@link #ENABLED} is {@code false} (the default),
 * the entire framework is silent — no annotation scanning, no tick event handlers,
 * no state machine, no world creation. This is the safe default state.</p>
 */
public final class ClientSmokeConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ──────────────────────────────────────────────
    // Config entries (per D-13)
    // ──────────────────────────────────────────────

    /**
     * Master switch for the entire client smoke test framework.
     * <p>When {@code false} (default): no scanning, no tick handler, no state machine,
     * no events — the framework is completely silent. Not a single log line beyond
     * the initial mod construction banner.</p>
     * <p>When {@code true}: annotation scanning runs at mod construction time,
     * config is parsed, and the state machine activates on the first client tick.</p>
     */
    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment(
                    "Master switch for the client smoke test framework.",
                    "When false (default): no scanning, no tick handler, no events — completely silent.",
                    "When true: annotation scanning activates at mod construction, state machine starts on first tick."
            )
            .define("enabled", false);

    /**
     * Delay in seconds after world load before the first screenshot is captured.
     * Allows chunks to finish rendering and entities to spawn.
     */
    public static final ForgeConfigSpec.IntValue SCREENSHOT_DELAY = BUILDER
            .comment(
                    "Delay in seconds after world load before first screenshot capture.",
                    "Allows chunks to finish rendering and entities to spawn.",
                    "Default: 5 seconds (100 ticks at 20 TPS)"
            )
            .defineInRange("screenshotDelay", 5, 0, 120);

    /**
     * Number of ticks to wait after player spawn for render stabilization.
     * During this period, the state machine polls but does not proceed to test execution.
     */
    public static final ForgeConfigSpec.IntValue RELOAD_STABILIZE_TICKS = BUILDER
            .comment(
                    "Number of ticks to wait after player spawn for render stabilization.",
                    "During this period, the state machine polls but does not execute tests.",
                    "Default: 40 ticks (2 seconds at 20 TPS)"
            )
            .defineInRange("reloadStabilizeTicks", 40, 0, 200);

    /**
     * Whether to automatically exit the Minecraft client after all tests complete.
     * Uses a two-phase exit: {@code mc.stop()} (graceful) → 3s delay → {@code Runtime.getRuntime().halt(0)} (force).
     */
    public static final ForgeConfigSpec.BooleanValue EXIT_AFTER_SMOKE = BUILDER
            .comment(
                    "Automatically exit Minecraft after all smoke tests complete.",
                    "Uses two-phase exit: mc.stop() → 3s delay → Runtime.halt(0).",
                    "Default: true"
            )
            .define("exitAfterSmoke", true);

    // ──────────────────────────────────────────────
    // Built spec
    // ──────────────────────────────────────────────

    /** The built {@link ForgeConfigSpec} — register this in the {@code @Mod} constructor. */
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // ──────────────────────────────────────────────
    // System property override bridge (per OVRD-01, OVRD-02)
    // ──────────────────────────────────────────────

    /**
     * Returns whether the smoke test framework should be active.
     *
     * <p>Priority order (per OVRD-01):
     * <ol>
     *   <li>System property {@code clientsmoke.enabled} — set by Gradle run config</li>
     *   <li>Fallback to {@link #ENABLED} ForgeConfigSpec value (default: {@code false})</li>
     * </ol></p>
     *
     * <p>When the system property is set to any value, {@link Boolean#parseBoolean(String)}
     * determines the result ({@code "true"} → true, anything else → false).
     * When the system property is absent ({@code null}), falls back to TOML config.</p>
     *
     * @return true if smoke testing should be enabled
     */
    public static boolean isEnabled() {
        String prop = System.getProperty("clientsmoke.enabled");
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        }
        return ENABLED.get();
    }

    /**
     * Returns whether the client should automatically exit after smoke tests complete.
     *
     * <p>Priority order (per OVRD-02):
     * <ol>
     *   <li>System property {@code clientsmoke.autoExit} — set by Gradle run config</li>
     *   <li>Fallback to {@link #EXIT_AFTER_SMOKE} ForgeConfigSpec value (default: {@code true})</li>
     * </ol></p>
     *
     * <p>Parse semantics identical to {@link #isEnabled()}.</p>
     *
     * @return true if the client should exit automatically after tests
     */
    public static boolean shouldExitAfterSmoke() {
        String prop = System.getProperty("clientsmoke.autoExit");
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        }
        return EXIT_AFTER_SMOKE.get();
    }

    private ClientSmokeConfig() {
        // Utility class — no instantiation
    }
}

package io.github.tt432.clientsmoke.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** @author TT432 */
public final class ClientSmokeConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master switch for the client smoke test framework.")
            .define("enabled", false);

    public static final ModConfigSpec.IntValue SCREENSHOT_DELAY = BUILDER
            .comment("Delay in seconds after world load before first screenshot capture.")
            .defineInRange("screenshotDelay", 5, 0, 120);

    public static final ModConfigSpec.IntValue RELOAD_STABILIZE_TICKS = BUILDER
            .comment("Number of ticks to wait after player spawn for render stabilization.")
            .defineInRange("reloadStabilizeTicks", 40, 0, 200);

    public static final ModConfigSpec.BooleanValue EXIT_AFTER_SMOKE = BUILDER
            .comment("Automatically exit Minecraft after all smoke tests complete.")
            .define("exitAfterSmoke", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean isEnabled() {
        String prop = System.getProperty("clientsmoke.enabled");
        if (prop != null) return Boolean.parseBoolean(prop);
        return ENABLED.get();
    }

    public static boolean shouldExitAfterSmoke() {
        String prop = System.getProperty("clientsmoke.autoExit");
        if (prop != null) return Boolean.parseBoolean(prop);
        return EXIT_AFTER_SMOKE.get();
    }

    public static boolean isPreventMouseGrab() {
        String prop = System.getProperty("eyelib.preventMouseGrab");
        if (prop != null) return Boolean.parseBoolean(prop);
        return false;
    }

    public static boolean isMinimizeWindow() {
        String prop = System.getProperty("eyelib.minimizeWindow");
        if (prop != null) return Boolean.parseBoolean(prop);
        return false;
    }

    private ClientSmokeConfig() {}
}

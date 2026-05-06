package io.github.tt432.clientsmoke.runtime;

import io.github.tt432.clientsmoke.ClientSmokeMod;
import io.github.tt432.clientsmoke.config.ClientSmokeConfig;
import io.github.tt432.clientsmoke.scanner.ClientSmokeScanner;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Tick-driven state machine that controls the client smoke test lifecycle.
 *
 * <p>Registered automatically via {@code @Mod.EventBusSubscriber} for the
 * {@link Dist#CLIENT} side. On each {@link TickEvent.ClientTickEvent}
 * (Phase.START), the state machine processes exactly one state transition.</p>
 *
 * <p><strong>State flow (Phase 2):</strong></p>
 * <pre>
 *   INIT → IDLE                          (if enabled=false)
 *   INIT → CONFIG_LOAD → SCAN
 *     → WORLD_CREATE → WORLD_WAIT → STABILIZE   (if enabled=true)
 *   ANY  → ERROR                         (on unhandled exception)
 * </pre>
 *
 * <p>Lifecycle beyond STABILIZE (TEST_EXEC, SCREENSHOT, NEXT_TEST, REPORT, EXIT)
 * is Phase 3+4 scope.</p>
 *
 * @see ClientSmokeState
 * @see ClientSmokeConfig
 */
@Mod.EventBusSubscriber(modid = ClientSmokeMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSmokeStateMachine {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSmokeStateMachine.class);

    private static ClientSmokeState state = ClientSmokeState.INIT;

    private static List<ClientSmokeScanner.DiscoveredTest> discoveredTests = Collections.emptyList();

    /** Game time at which world stabilization began. {@code -1L} = not yet started. */
    private static long stabilizeStartTick = -1L;

    /** World name for the test world. Hardcoded for v1. */
    private static final String WORLD_NAME = "ClientSmokeTest";

    /** Fixed seed for deterministic world generation. */
    private static final long WORLD_SEED = 12345L;

    public static void setDiscoveredTests(List<ClientSmokeScanner.DiscoveredTest> tests) {
        discoveredTests = List.copyOf(tests);
        LOGGER.info("[ClientSmoke] State machine received {} discovered test(s)", discoveredTests.size());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        if (state == ClientSmokeState.IDLE || state == ClientSmokeState.ERROR) {
            return;
        }

        try {
            switch (state) {
                case INIT -> handleInit();
                case CONFIG_LOAD -> handleConfigLoad();
                case SCAN -> handleScan();
                case WORLD_CREATE -> handleWorldCreate();
                case WORLD_WAIT -> handleWorldWait();
                case STABILIZE -> handleStabilize();
                default -> transitionTo(ClientSmokeState.ERROR, "Unknown state: " + state);
            }
        } catch (Exception e) {
            LOGGER.error("[ClientSmoke] State machine error in state {}: {}", state, e.getMessage(), e);
            state = ClientSmokeState.ERROR;
        }
    }

    // ── State handlers ────────────────────────────────────────────

    private static void handleInit() {
        if (!ClientSmokeConfig.ENABLED.get()) {
            transitionTo(ClientSmokeState.IDLE, "Framework disabled — entering idle state");
        } else {
            transitionTo(ClientSmokeState.CONFIG_LOAD, "Framework enabled — loading config");
        }
    }

    private static void handleConfigLoad() {
        LOGGER.info("[ClientSmoke] Config verified — enabled={}, reloadStabilizeTicks={}",
                ClientSmokeConfig.ENABLED.get(), ClientSmokeConfig.RELOAD_STABILIZE_TICKS.get());
        transitionTo(ClientSmokeState.SCAN, "Config loaded — proceeding to scan");
    }

    private static void handleScan() {
        LOGGER.info("[ClientSmoke] Scan complete — {} test(s) in queue", discoveredTests.size());
        if (discoveredTests.isEmpty()) {
            LOGGER.info("[ClientSmoke] No tests found — entering idle state");
            transitionTo(ClientSmokeState.IDLE, "No @ClientSmoke tests found");
        } else {
            transitionTo(ClientSmokeState.WORLD_CREATE, "Tests found — creating test world");
        }
    }

    private static void handleWorldCreate() {
        Minecraft mc = Minecraft.getInstance();

        try {
            if (mc.getLevelSource().levelExists(WORLD_NAME)) {
                // World already exists — reuse it
                LOGGER.info("[ClientSmoke] World '{}' already exists — reusing", WORLD_NAME);
                mc.createWorldOpenFlows().loadLevel(null, WORLD_NAME);
            } else {
                // Create a fresh creative superflat world
                LOGGER.info("[ClientSmoke] Creating new world '{}' (creative flat, seed={})",
                        WORLD_NAME, WORLD_SEED);

                LevelSettings levelSettings = new LevelSettings(
                        WORLD_NAME,
                        GameType.CREATIVE,
                        false,                          // hardcore = false
                        Difficulty.NORMAL,
                        true,                           // allowCommands = true
                        new net.minecraft.world.level.GameRules(),
                        WorldDataConfiguration.DEFAULT
                );

                WorldOptions worldOptions = new WorldOptions(
                        WORLD_SEED,
                        true,   // generateStructures = true
                        false   // bonusChest = false
                );

                // Forge 1.20.1 API: createFreshLevel(String, LevelSettings, WorldOptions, Function<RegistryAccess, WorldDimensions>)
                // Uses FLAT world preset to produce a superflat creative world with fixed seed
                mc.createWorldOpenFlows().createFreshLevel(
                        WORLD_NAME,
                        levelSettings,
                        worldOptions,
                        ClientSmokeStateMachine::createFlatWorldDimensions
                );
            }

            transitionTo(ClientSmokeState.WORLD_WAIT, "World load initiated — waiting for player spawn");
        } catch (Exception e) {
            LOGGER.error("[ClientSmoke] Failed to create/load world '{}': {}", WORLD_NAME, e.getMessage(), e);
            transitionTo(ClientSmokeState.ERROR, "World creation failed: " + e.getMessage());
        }
    }

    /**
     * Creates flat world dimensions for the test world using the {@link WorldPresets#FLAT}
     * preset from the world preset registry.
     *
     * <p>Method reference compatible with
     * {@code WorldOpenFlows.createFreshLevel}'s {@code Function<RegistryAccess, WorldDimensions>} parameter.</p>
     *
     * @param registry the registry access from the world creation datapack context
     * @return the flat world dimensions produced by the FLAT preset
     */
    private static WorldDimensions createFlatWorldDimensions(RegistryAccess registry) {
        return registry.registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.FLAT)
                .value()
                .createWorldDimensions();
    }

    private static void handleWorldWait() {
        LOGGER.info("[ClientSmoke] Waiting for world load (Plan 02-02)");
        transitionTo(ClientSmokeState.STABILIZE, "World wait complete (placeholder)");
    }

    private static void handleStabilize() {
        LOGGER.info("[ClientSmoke] Stabilization pending (Plan 02-02)");
        transitionTo(ClientSmokeState.STABILIZE, "Stabilization complete (placeholder — Phase 2 end)");
    }

    // ── Utils ─────────────────────────────────────────────────────

    private static void transitionTo(ClientSmokeState newState, String reason) {
        LOGGER.info("[ClientSmoke] {} → {} — {}", state, newState, reason);
        state = newState;
    }

    private ClientSmokeStateMachine() {
        // Utility class — no instantiation
    }
}

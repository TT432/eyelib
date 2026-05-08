package io.github.tt432.clientsmoke.runtime;

import io.github.tt432.clientsmoke.ClientSmokeMod;
import io.github.tt432.clientsmoke.config.ClientSmokeConfig;
import io.github.tt432.clientsmoke.scanner.ClientSmokeScanner;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tick-driven state machine that controls the client smoke test lifecycle.
 *
 * <p>Registered automatically via {@code @Mod.EventBusSubscriber} for the
 * {@link Dist#CLIENT} side. On each {@link TickEvent.ClientTickEvent}
 * (Phase.START), the state machine processes exactly one state transition.</p>
 *
 * <p><strong>State flow (Phase 2-3):</strong></p>
 * <pre>
 *   INIT → IDLE                          (if enabled=false)
 *   INIT → CONFIG_LOAD → SCAN
 *     → WORLD_CREATE → WORLD_WAIT → STABILIZE   (if enabled=true)
 *   STABILIZE → HUD_HIDE → SCREENSHOT → EXIT    (Phase 3: capture + exit)
 *   ANY  → ERROR                         (on unhandled exception)
 * </pre>
 *
 * <p>Lifecycle beyond SCREENSHOT (TEST_EXEC, NEXT_TEST, REPORT)
 * is Phase 4 scope.</p>
 *
 * @see ClientSmokeState
 * @see ClientSmokeConfig
 */
@Mod.EventBusSubscriber(modid = ClientSmokeMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSmokeStateMachine {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSmokeStateMachine.class);

    /**
     * Accumulated result of a single test execution.
     *
     * <p>Per D-15: Contains className (FQCN), description, priority,
     * status ("passed"/"failed"), durationMs (wall-clock timing), and
     * error (null if passed; message + first 5 stack trace lines if failed).</p>
     *
     * @param className   fully qualified class name of the test
     * @param description test description from {@code @ClientSmoke} annotation
     * @param priority    execution priority from annotation
     * @param status      "passed" or "failed"
     * @param durationMs  wall-clock duration in milliseconds
     * @param error       error details, or null if passed
     */
    record TestResult(
            String className,
            String description,
            int priority,
            String status,
            long durationMs,
            ErrorInfo error
    ) {}

    /**
     * Error information for failed tests.
     *
     * <p>Per D-12: message is {@code e.toString() + ": " + e.getMessage()};
     * stackTrace is the first 5 lines of the stack trace as a single string.</p>
     */
    record ErrorInfo(String message, String stackTrace) {}

    private static ClientSmokeState state = ClientSmokeState.INIT;

    private static List<ClientSmokeScanner.DiscoveredTest> discoveredTests = Collections.emptyList();

    /** Game time at which world stabilization began. {@code -1L} = not yet started. */
    private static long stabilizeStartTick = -1L;

    /** World name for the test world. Hardcoded for v1. */
    private static final String WORLD_NAME = "ClientSmokeTest";

    /** Fixed seed for deterministic world generation. */
    private static final long WORLD_SEED = 12345L;

    /** Guard to ensure stabilization-complete log fires exactly once. */
    private static boolean stabilizeCompleteLogged = false;

    /** Index into {@link #discoveredTests} for the current test. Phase 4 manages this; Phase 3 reads className for file naming. */
    private static int testIndex = 0;

    /** One-frame guard preventing duplicate screenshot capture in a single render pass. Set true after capture, reset in tick handler. */
    private static boolean screenshotTakenThisFrame = false;

    /** Tick at which EXIT state was entered. {@code -1L} = EXIT not yet entered. D-04: tick-counted countdown. */
    private static long exitStartTick = -1L;

    /** Accumulated test results. Populated by handleTestExec(), serialized by handleReport(). */
    private static final List<TestResult> testResults = new ArrayList<>();

    /** Guard ensuring priority sort happens exactly once (on first TEST_EXEC entry). */
    private static boolean testsSorted = false;

    /** Guard ensuring report is written exactly once. */
    private static boolean reportWritten = false;

    public static void setDiscoveredTests(List<ClientSmokeScanner.DiscoveredTest> tests) {
        discoveredTests = new ArrayList<>(tests);
        LOGGER.info("[ClientSmoke] State machine received {} discovered test(s)", discoveredTests.size());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        // Terminal states: IDLE and ERROR halt processing.
        // EXIT is NOT terminal — its handler must receive ticks for the countdown.
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
                case HUD_HIDE -> handleHudHide();
                case SCREENSHOT -> handleScreenshot();
                case TEST_EXEC -> handleTestExec();
                case REPOSITION -> handleReposition();
                case REPORT -> handleReport();
                case EXIT -> handleExit();
                default -> transitionTo(ClientSmokeState.ERROR, "Unknown state: " + state);
            }
        } catch (Exception e) {
            LOGGER.error("[ClientSmoke] State machine error in state {}: {}", state, e.getMessage(), e);
            state = ClientSmokeState.ERROR;
        }
    }

    /**
     * Captures a screenshot on the render thread during
     * {@link RenderLevelStageEvent.Stage#AFTER_LEVEL}.
     *
     * <p>Per D-06: Reads the main framebuffer into a {@link NativeImage},
     * flips Y for image file orientation, and writes a PNG to
     * {@code clientsmoke-reports/screenshots/} with a timestamped filename.</p>
     *
     * <p>Per D-07: Uses {@code AFTER_LEVEL} stage — all world content,
     * entities, particles, and post-processing are complete.</p>
     *
     * <p>Per D-08: PNG encoding and file I/O are synchronous on the render
     * thread — no async thread pool needed (&lt;10ms for typical framebuffer).</p>
     *
     * <p><strong>Per D-02 + D-12 flow:</strong>
     * <ol>
     *   <li>{@code handleHudHide()} sets {@code hideGui=true}, transitions to SCREENSHOT</li>
     *   <li>Same frame: Minecraft renders without HUD</li>
     *   <li>This handler fires, captures framebuffer, writes PNG</li>
     *   <li>Restores {@code hideGui=false}</li>
     *   <li>Increments testIndex; transitions to EXIT or next test</li>
     * </ol></p>
     *
     * <p>Per D-10: {@code hideGui} is restored here immediately after capture.</p>
     *
     * @param event the render level stage event
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Only capture on AFTER_LEVEL stage
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        // Only capture when state machine is in SCREENSHOT state
        if (state != ClientSmokeState.SCREENSHOT) {
            return;
        }

        // One-frame guard against duplicate captures (belt-and-suspenders with state check)
        if (screenshotTakenThisFrame) {
            return;
        }
        screenshotTakenThisFrame = true;

        Minecraft mc = Minecraft.getInstance();
        NativeImage nativeimage = null;

        try {
            // ── Step 1: Read framebuffer into NativeImage (per D-06 pattern) ──
            // Verified from Forge 1.20.1-47.1.3 sources: RenderTarget.width/height are public int fields
            RenderTarget framebuffer = mc.getMainRenderTarget();
            int width = framebuffer.width;
            int height = framebuffer.height;

            // Per RESEARCH.md Code Examples: create NativeImage, bind color texture, download, flip
            nativeimage = new NativeImage(width, height, false);
            RenderSystem.bindTexture(framebuffer.getColorTextureId());
            nativeimage.downloadTexture(0, true);   // read GL texture into NativeImage
            nativeimage.flipY();                     // OpenGL bottom-left → image top-left

            // ── Step 2: Determine output filename (per D-14, D-15) ──
            // Per D-15: className uses getSimpleName() (no package prefix)
            String testClassName;
            if (discoveredTests.isEmpty()) {
                testClassName = "NoTests";
            } else {
                String fqcn = discoveredTests.get(testIndex).className();
                testClassName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
            }

            // Per D-14: {SimpleClassName}-{yyyyMMdd-HHmmss}.png
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String filename = testClassName + "-" + timestamp + ".png";

            // ── Step 3: Create output directory (per D-13, D-16) ──
            // Per D-13: ./clientsmoke-reports/screenshots/ relative to game dir
            // Per D-16: Files.createDirectories() auto-creates tree
            Path outputDir = FMLPaths.GAMEDIR.get().resolve("clientsmoke-reports").resolve("screenshots");
            Files.createDirectories(outputDir);
            Path outputFile = outputDir.resolve(filename);

            // ── Step 4: Write PNG (per D-06: NativeImage.writeToFile) ──
            nativeimage.writeToFile(outputFile);
            LOGGER.info("[ClientSmoke] Screenshot saved: {}", outputFile.toAbsolutePath());

            // ── Step 5: Restore HUD visibility (per D-02, D-10) ──
            mc.options.hideGui = false;

            // ── Step 6: Advance to next test or exit ──
            testIndex++;
            if (testIndex < discoveredTests.size()) {
                // More tests remain — loop back to test execution
                transitionTo(ClientSmokeState.TEST_EXEC, "Test " + testIndex + "/" + discoveredTests.size() + " — next test");
            } else {
                // All tests complete — generate report before exit
                transitionTo(ClientSmokeState.REPORT, "All " + discoveredTests.size() + " test(s) complete — generating report");
            }

        } catch (Exception e) {
            LOGGER.error("[ClientSmoke] Screenshot capture failed: {}", e.getMessage(), e);
            // Per D-09/D-10: Always attempt to restore HUD on error
            try {
                mc.options.hideGui = false;
            } catch (Exception restoreEx) {
                LOGGER.warn("[ClientSmoke] Failed to restore hideGui after error: {}", restoreEx.getMessage());
            }
            transitionTo(ClientSmokeState.ERROR, "Screenshot capture failed: " + e.getMessage());
        } finally {
            // Per RESEARCH.md Common Pitfalls #2: NativeImage holds off-heap LWJGL memory — must close
            if (nativeimage != null) {
                nativeimage.close();
            }
        }
    }

    // ── State handlers ────────────────────────────────────────────

    private static void handleInit() {
        if (!ClientSmokeConfig.isEnabled()) {
            transitionTo(ClientSmokeState.IDLE, "Framework disabled — entering idle state");
        } else {
            transitionTo(ClientSmokeState.CONFIG_LOAD, "Framework enabled — loading config");
        }
    }

    private static void handleConfigLoad() {
        LOGGER.info("[ClientSmoke] Config verified — enabled={}, reloadStabilizeTicks={}",
                ClientSmokeConfig.isEnabled(), ClientSmokeConfig.RELOAD_STABILIZE_TICKS.get());
        transitionTo(ClientSmokeState.SCAN, "Config loaded — proceeding to scan");
    }

    private static void handleScan() {
        LOGGER.info("[ClientSmoke] Scan complete — {} test(s) in queue", discoveredTests.size());
        if (discoveredTests.isEmpty()) {
            if (ClientSmokeConfig.shouldExitAfterSmoke()) {
                LOGGER.info("[ClientSmoke] No tests found — auto-exit enabled, generating empty report before exit");
                transitionTo(ClientSmokeState.REPORT, "No @ClientSmoke tests found — generating empty report");
            } else {
                LOGGER.info("[ClientSmoke] No tests found — entering idle state");
                transitionTo(ClientSmokeState.IDLE, "No @ClientSmoke tests found");
            }
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
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && mc.level != null) {
            // Player has spawned in the world
            stabilizeStartTick = mc.level.getGameTime();
            LOGGER.info("[ClientSmoke] Player spawned in world '{}' — starting stabilization ({} ticks required)",
                    WORLD_NAME, ClientSmokeConfig.RELOAD_STABILIZE_TICKS.get());
            transitionTo(ClientSmokeState.STABILIZE, "Player spawned — stabilizing render");
        }
        // else: stay in WORLD_WAIT — no log spam, just wait for next tick
    }

    private static void handleStabilize() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            LOGGER.warn("[ClientSmoke] Level unexpectedly null during stabilization — returning to WORLD_WAIT");
            state = ClientSmokeState.WORLD_WAIT;
            return;
        }

        long waitedTicks = mc.level.getGameTime() - stabilizeStartTick;
        long requiredTicks = ClientSmokeConfig.RELOAD_STABILIZE_TICKS.get();

        if (waitedTicks >= requiredTicks && !stabilizeCompleteLogged) {
            // Stabilization complete — Phase 2 endpoint reached (one-shot log)
            stabilizeCompleteLogged = true;
            LOGGER.info("[ClientSmoke] Stabilization complete — waited {} ticks (required: {}). Ready for test execution.",
                    waitedTicks, requiredTicks);
            LOGGER.info("[ClientSmoke] Phase 2 complete — {} test(s) queued. Phase 3 will execute tests.",
                    discoveredTests.size());
            // Phase 4 handoff: enter test execution loop or go to screenshot pipeline
            if (discoveredTests.isEmpty()) {
                if (ClientSmokeConfig.shouldExitAfterSmoke()) {
                    transitionTo(ClientSmokeState.REPORT, "Stabilization complete — no tests, generating empty report before exit");
                } else {
                    transitionTo(ClientSmokeState.HUD_HIDE, "Stabilization complete — entering screenshot pipeline (no tests in queue)");
                }
            } else {
                transitionTo(ClientSmokeState.TEST_EXEC,
                        "Stabilization complete — entering test execution phase (" + discoveredTests.size() + " test(s))");
            }
        }
        // else: stay in STABILIZE — no log spam, just wait for next tick
    }

    /**
     * Hides the HUD before screenshot capture.
     *
     * <p>Per D-02: Sets {@code options.hideGui = true} in the tick handler.
     * The Minecraft render pipeline reads this value at the start of the next
     * render pass (same frame), ensuring the HUD is hidden when the
     * {@code RenderLevelStageEvent.AFTER_LEVEL} handler captures.</p>
     *
     * <p>Per D-12: Transitions to SCREENSHOT immediately after toggling hideGui.</p>
     */
    private static void handleHudHide() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.hideGui = true;
        transitionTo(ClientSmokeState.SCREENSHOT, "HUD hidden — awaiting screenshot capture on render thread");
    }

    /**
     * Tick handler for SCREENSHOT state — resets the one-frame guard.
     *
     * <p>Actual framebuffer capture happens in
     * {@link #onRenderLevelStage(RenderLevelStageEvent)} which fires on the
     * render thread during the same frame.</p>
     *
     * <p>Per D-02: The render event handler transitions out of SCREENSHOT
     * after capture, so this tick handler typically fires only once (the
     * transition from HUD_HIDE sets state=SCREENSHOT; the render event
     * captures and transitions out).</p>
     */
    private static void handleScreenshot() {
        // Reset one-frame capture guard at start of each tick
        screenshotTakenThisFrame = false;
    }

    /**
     * Loads, instantiates, and executes the test class at the current
     * {@link #testIndex}.
     *
     * <p><strong>Per D-01:</strong> Constructor-as-test — the no-arg
     * constructor body IS the test. No interface or method contract.</p>
     *
     * <p><strong>Per D-02:</strong> Tests access Minecraft via
     * {@code Minecraft.getInstance()} — no injection.</p>
     *
     * <p><strong>Per D-09/D-10:</strong> Exceptions are captured,
     * recorded as failures, and the state machine advances to
     * {@link ClientSmokeState#REPOSITION}. Subsequent tests continue
     * without interruption.</p>
     *
     * <p><strong>Per D-11:</strong> No timeout mechanism — if a test
     * hangs, {@code Runtime.halt(0)} in the EXIT state is the ultimate
     * guarantee.</p>
     *
     * <p><strong>Per D-12:</strong> On failure, error info contains
     * {@code e.toString()} + ": " + e.getMessage() for the message,
     * and the first 5 stack trace lines.</p>
     *
     * <p><strong>Per EXEC-03:</strong> Tests are sorted by priority
     * on first TEST_EXEC entry (ascending; equal priority = discovery order).
     * The sort is idempotent — {@code testsSorted} guard prevents re-sorting.</p>
     *
     * <p><strong>Thread safety:</strong> All state machine handlers run on
     * the client tick thread (single-threaded). No concurrent access to
     * {@link #testResults} or {@link #testIndex}.</p>
     */
    private static void handleTestExec() {
        // Priority sort — once, on first entry
        if (!testsSorted) {
            discoveredTests.sort(Comparator.comparingInt(ClientSmokeScanner.DiscoveredTest::priority));
            testsSorted = true;
            LOGGER.info("[ClientSmoke] Tests sorted by priority — {} test(s) ready for execution", discoveredTests.size());
        }

        // Safety guard — bounds check
        if (testIndex >= discoveredTests.size()) {
            LOGGER.warn("[ClientSmoke] testIndex {} out of bounds ({} tests) — transitioning to REPORT",
                    testIndex, discoveredTests.size());
            transitionTo(ClientSmokeState.REPORT, "No more tests — generating report");
            return;
        }

        ClientSmokeScanner.DiscoveredTest discovered = discoveredTests.get(testIndex);
        String className = discovered.className();
        long startMs = System.currentTimeMillis();

        try {
            // Per D-01: Class.forName() + getDeclaredConstructor().newInstance()
            Class<?> testClass = Class.forName(className);
            testClass.getDeclaredConstructor().newInstance();

            long durationMs = System.currentTimeMillis() - startMs;
            testResults.add(new TestResult(
                    className,
                    discovered.description(),
                    discovered.priority(),
                    "passed",
                    durationMs,
                    null
            ));
            LOGGER.info("[ClientSmoke] ✓ PASS — {} ({}) — {}ms",
                    className, discovered.description(), durationMs);

        } catch (ClassNotFoundException e) {
            // Per D-10: class not found — record failure and continue
            long durationMs = System.currentTimeMillis() - startMs;
            ErrorInfo err = buildErrorInfo(e, "class not found: " + className);
            testResults.add(new TestResult(className, discovered.description(),
                    discovered.priority(), "failed", durationMs, err));
            LOGGER.warn("[ClientSmoke] ✗ FAIL — {} — class not found (was the mod loaded?)", className);

        } catch (Exception e) {
            // Per D-09: any other exception — record failure and continue
            long durationMs = System.currentTimeMillis() - startMs;
            ErrorInfo err = buildErrorInfo(e, e.toString());
            testResults.add(new TestResult(className, discovered.description(),
                    discovered.priority(), "failed", durationMs, err));
            LOGGER.warn("[ClientSmoke] ✗ FAIL — {} — {}", className, e.toString());
        }

        // Per D-08: advance to REPOSITION — pass-through to HUD_HIDE for screenshot
        transitionTo(ClientSmokeState.REPOSITION,
                "Test " + (testIndex + 1) + "/" + discoveredTests.size() + " executed — repositioning for screenshot");
    }

    /**
     * Builds compact error info per D-12:
     * message = {@code e.toString()} + ": " + e.getMessage(),
     * stackTrace = first 5 lines of the stack trace.
     */
    private static ErrorInfo buildErrorInfo(Exception e, String message) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        String fullTrace = sw.toString();
        String[] lines = fullTrace.split("\\r?\\n");
        StringBuilder top5 = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            top5.append(line).append('\n');
            count++;
            if (count >= 6) break;  // 1 header + 5 trace lines
        }
        return new ErrorInfo(message, top5.toString().stripTrailing());
    }

    /**
     * Pass-through anchor between test execution and HUD hiding.
     *
     * <p>Per D-08: No state mutation — {@code testIndex} is already
     * pointing at the correct test. This simply transitions to
     * {@link ClientSmokeState#HUD_HIDE} to begin the screenshot cycle
     * for the current test's screen state.</p>
     */
    private static void handleReposition() {
        transitionTo(ClientSmokeState.HUD_HIDE,
                "Repositioned — capturing screenshot for test " + (testIndex + 1) + "/" + discoveredTests.size());
    }

    /**
     * Two-phase JVM exit with graceful Forge shutdown followed by forced termination.
     *
     * <p><strong>Phase 1 — Graceful shutdown (tick 0 of EXIT):</strong>
     * Calls {@code Minecraft.getInstance().stop()} which sets {@code running = false}
     * and fires {@code GameShuttingDownEvent} for mod cleanup. Wrapped in try-catch
     * per RESEARCH.md recommendation (the agent's discretion — safety against
     * unexpected exceptions during shutdown).</p>
     *
     * <p><strong>Phase 2 — Countdown + forced halt:</strong>
     * Each subsequent tick increments the countdown. When {@code elapsedTicks >= 60}
     * (3 seconds at 20 TPS, per D-04), calls {@code Runtime.getRuntime().halt(0)}
     * to force-terminate the JVM. {@code halt()} does NOT run shutdown hooks,
     * avoiding the common modded-Minecraft hang where non-daemon render/network
     * threads prevent clean {@code System.exit()}.</p>
     *
     * <p><strong>Config gating (per EXIT-01):</strong>
     * When {@code exitAfterSmoke=false}, this method transitions to IDLE instead
     * of shutting down — the client stays open for manual inspection.</p>
     *
     * <p><strong>One-shot log guard:</strong>
     * The first call logs "Initiating exit", subsequent ticks silently count down.
     * The final {@code halt()} call logs "Exit complete — halting JVM".</p>
     *
     * <p><strong>Time source (per D-04):</strong>
     * Uses game tick counter ({@code mc.level.getGameTime()}) for the countdown.
     * If {@code mc.level} is null (e.g., after {@code stop()} clears it), falls
     * back to the elapsed wall-clock time since EXIT entry as a safety net.</p>
     *
     * <p>Per D-04: tick 0 calls mc.stop(); subsequent ticks check elapsed; >= 60 → halt(0).</p>
     */
    private static void handleExit() {
        // Per EXIT-01: check config gating
        if (!ClientSmokeConfig.shouldExitAfterSmoke()) {
            transitionTo(ClientSmokeState.IDLE, "exitAfterSmoke=false — entering idle state for manual inspection");
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // Resolve current tick: use game time when available, otherwise fall back to wall clock
        long currentTick;
        if (mc.level != null) {
            currentTick = mc.level.getGameTime();
        } else {
            // Level is null (likely after mc.stop() cleared it) — use wall-clock fallback
            currentTick = System.currentTimeMillis() / 50;  // approximate tick count from wall time
        }

        // Phase 1: graceful shutdown (first EXIT tick only)
        if (exitStartTick < 0) {
            exitStartTick = currentTick;
            LOGGER.info("[ClientSmoke] Initiating two-phase exit — calling mc.stop()");
            try {
                mc.stop();
            } catch (Exception e) {
                LOGGER.error("[ClientSmoke] mc.stop() threw exception: {}", e.getMessage(), e);
                // Fall through to countdown — halt() will still fire as final guarantee
            }
            // Return this tick — countdown starts next tick (per D-04: tick 0 = mc.stop(), subsequent checks)
            return;
        }

        // Phase 2: countdown + forced halt
        long elapsedTicks = currentTick - exitStartTick;
        if (elapsedTicks >= 60) {
            // Per D-04: 60 ticks = 3 seconds at 20 TPS
            LOGGER.info("[ClientSmoke] Exit complete after {} ticks — halting JVM via Runtime.getRuntime().halt(0)",
                    elapsedTicks);
            Runtime.getRuntime().halt(0);
        }
        // else: stay in EXIT state, silently count down each tick (no log spam)
    }

    /**
     * Generates the JSON test report and writes it to disk.
     *
     * <p>Per D-13: Report is written synchronously before transitioning to
     * {@link ClientSmokeState#EXIT}, guaranteeing the file is fully on disk
     * before {@code Runtime.getRuntime().halt(0)} fires (Phase 4 success
     * criterion #5).</p>
     *
     * <p>Per D-14: Filename format is {@code report-{yyyyMMdd-HHmmss}.json}.</p>
     *
     * <p>Per D-15: Report contains {@code totalTests}, {@code passed},
     * {@code failed}, {@code timestamp}, and an {@code entries} array.
     * Per D-16: Uses Gson for serialization.</p>
     *
     * <p><strong>One-shot guard:</strong> {@code reportWritten} ensures
     * the report is generated exactly once.</p>
     */
    private static void handleReport() {
        if (reportWritten) {
            return;
        }
        reportWritten = true;

        // Per D-14: timestamped filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = "report-" + timestamp + ".json";

        // Per D-13: output to clientsmoke-reports/
        Path outputDir = FMLPaths.GAMEDIR.get().resolve("clientsmoke-reports");
        Path outputFile = outputDir.resolve(filename);

        try {
            Files.createDirectories(outputDir);

            long passed = testResults.stream().filter(r -> "passed".equals(r.status())).count();
            long failed = testResults.size() - passed;

            // Per D-15: build report structure
            ReportData report = new ReportData(
                    testResults.size(),
                    (int) passed,
                    (int) failed,
                    timestamp,
                    testResults
            );

            // Per D-16: Gson serialization
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report);
            Files.writeString(outputFile, json);

            LOGGER.info("[ClientSmoke] Report written: {} ({} tests, {} passed, {} failed)",
                    outputFile.toAbsolutePath(), testResults.size(), passed, failed);

        } catch (Exception e) {
            LOGGER.error("[ClientSmoke] Failed to write report to {}: {}", outputFile, e.getMessage(), e);
            transitionTo(ClientSmokeState.ERROR, "Report generation failed: " + e.getMessage());
            return;
        }

        // Report written — proceed to exit
        transitionTo(ClientSmokeState.EXIT, "Report generated — initiating exit");
    }

    /**
     * Report data structure for Gson serialization.
     * Per D-15: Contains totalTests, passed, failed, timestamp, and entries.
     */
    @SuppressWarnings("unused")  // fields read by Gson reflection
    private record ReportData(
            int totalTests,
            int passed,
            int failed,
            String timestamp,
            List<TestResult> entries
    ) {}

    // ── Utils ─────────────────────────────────────────────────────

    private static void transitionTo(ClientSmokeState newState, String reason) {
        LOGGER.info("[ClientSmoke] {} → {} — {}", state, newState, reason);
        state = newState;
    }

    private ClientSmokeStateMachine() {
        // Utility class — no instantiation
    }
}

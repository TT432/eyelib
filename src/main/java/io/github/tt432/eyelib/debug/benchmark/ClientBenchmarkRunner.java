package io.github.tt432.eyelib.debug.benchmark;

import io.github.tt432.eyelib.client.render.EntityRenderOrchestrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if <1.20.6 {
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
//?}

import org.jspecify.annotations.Nullable;
import com.sun.management.OperatingSystemMXBean;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.file.Path;
import java.util.List;

/** Event-driven, dev-only rendering benchmark runner. */
public final class ClientBenchmarkRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientBenchmarkRunner.class);
    private static final long SECOND_NS = 1_000_000_000L;
    private static final int PREPARE_TIMEOUT_TICKS = 600;
    private static @Nullable ClientBenchmarkRunner installed;

    private final List<BenchmarkScenario> scenarios;
    private final BenchmarkReportWriter reportWriter;
    private final @Nullable OperatingSystemMXBean operatingSystemBean;

    private State state = State.INIT;
    private int scenarioIndex;
    private @Nullable BenchmarkScenario scenario;
    private @Nullable BenchmarkWorkload workload;
    private @Nullable FrameSampleBuffer frames;
    private @Nullable ResourceSampleBuffer resources;
    private long stabilizeStartTick = -1L;
    private long prepareStartTick = -1L;
    private long phaseStartNs;
    private long lastFrameStartNs;
    private long lastResourceSampleNs;
    private int lastRenderCount;
    private boolean scenarioReported;
    private boolean terminalHandled;
    private @Nullable String terminalError;
    private @Nullable OptionSnapshot originalOptions;

    private ClientBenchmarkRunner() throws IOException {
        scenarios = ClientBenchmarkConfig.scenarios();
        reportWriter = new BenchmarkReportWriter();
        java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        operatingSystemBean = bean instanceof OperatingSystemMXBean osBean ? osBean : null;
    }

    /** Installs the runner only when explicitly enabled by the dedicated run configuration. */
    public static synchronized void install() {
        if (!ClientBenchmarkConfig.isEnabled() || installed != null) {
            return;
        }
        if (Boolean.getBoolean("clientsmoke.enabled")) {
            throw new IllegalStateException("eyelib benchmark and clientsmoke cannot run simultaneously");
        }
        try {
            installed = new ClientBenchmarkRunner();
            //? if <1.20.6 {
            MinecraftForge.EVENT_BUS.register(installed);
            //?} else {
            NeoForge.EVENT_BUS.register(installed);
            //?}
            LOGGER.info("[Benchmark] Installed {} scenario(s), report directory={}",
                    installed.scenarios.size(), installed.reportWriter.runDirectory());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to install Eyelib benchmark", e);
        }
    }

    @SubscribeEvent
    //? if <1.20.6 {
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
    //?} else {
    public void onClientTick(ClientTickEvent.Pre event) {
    //?}
        Minecraft minecraft = Minecraft.getInstance();
        try {
            switch (state) {
                case INIT -> handleInit(minecraft);
                case WORLD_WAIT -> handleWorldWait(minecraft);
                case STABILIZE -> handleStabilize(minecraft);
                case PREPARE -> handlePrepare(minecraft);
                case WARMUP, MEASURE -> maintainWorkload(minecraft);
                case COOLDOWN -> handleCooldown(minecraft);
                case NEXT_SCENARIO -> handleNextScenario(minecraft);
                case EXIT -> handleExit(minecraft);
                case ERROR -> handleError(minecraft);
                case IDLE -> {
                }
            }
        } catch (Throwable throwable) {
            fail(throwable);
        }
    }

    @SubscribeEvent
    //? if <1.20.6 {
    public void onRenderFrame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
    //?} else {
    public void onRenderFrame(RenderFrameEvent.Pre event) {
    //?}
        if (state != State.WARMUP && state != State.MEASURE) {
            return;
        }
        try {
            sampleFrame(Minecraft.getInstance(), System.nanoTime());
        } catch (Throwable throwable) {
            fail(throwable);
        }
    }

    @SubscribeEvent
    //? if <26.1 {
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
    //?} else {
    public void onRenderLevelStage(RenderLevelStageEvent.AfterLevel event) {
    //?}
        if ((state != State.WARMUP && state != State.MEASURE)
                || scenario == null || scenario.kind() != BenchmarkScenario.Kind.FBO || workload == null) {
            return;
        }
        try {
            workload.render(Minecraft.getInstance());
        } catch (Throwable throwable) {
            fail(throwable);
        }
    }

    private void handleInit(Minecraft minecraft) throws Exception {
        Options options = minecraft.options;
        originalOptions = new OptionSnapshot(
                options.enableVsync().get(),
                options.framerateLimit().get(),
                options.hideGui,
                options.pauseOnLostFocus
        );
        options.enableVsync().set(false);
        options.hideGui = true;
        options.pauseOnLostFocus = false;
        BenchmarkWorldController.openOrCreate(minecraft, ClientBenchmarkConfig.worldName());
        state = State.WORLD_WAIT;
        LOGGER.info("[Benchmark] Opening deterministic world '{}'", ClientBenchmarkConfig.worldName());
    }

    private void handleWorldWait(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        stabilizeStartTick = minecraft.level.getGameTime();
        state = State.STABILIZE;
        LOGGER.info("[Benchmark] World ready; stabilizing for {} ticks", ClientBenchmarkConfig.stabilizeTicks());
    }

    private void handleStabilize(Minecraft minecraft) {
        Level level = minecraft.level;
        if (level == null) {
            state = State.WORLD_WAIT;
            return;
        }
        if (level.getGameTime() - stabilizeStartTick < ClientBenchmarkConfig.stabilizeTicks()) {
            return;
        }
        scenarioIndex = 0;
        beginScenarioPreparation(minecraft);
    }

    private void beginScenarioPreparation(Minecraft minecraft) {
        scenario = scenarios.get(scenarioIndex);
        applyScenarioOptions(minecraft, scenario);
        workload = scenario.kind() == BenchmarkScenario.Kind.FBO
                ? new FboBenchmarkWorkload()
                : new WorldBenchmarkWorkload();
        prepareStartTick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        scenarioReported = false;
        state = State.PREPARE;
        LOGGER.info("[Benchmark] Preparing scenario {}/{}: {}", scenarioIndex + 1, scenarios.size(), scenario.id());
    }

    private void handlePrepare(Minecraft minecraft) throws Exception {
        if (workload == null || scenario == null || minecraft.level == null) {
            throw new IllegalStateException("Scenario preparation state is incomplete");
        }
        if (frames == null) {
            workload.prepare(minecraft, scenario);
            frames = new FrameSampleBuffer(ClientBenchmarkConfig.maxSamples());
            int resourceCapacity = Math.max(16, scenario.warmupSeconds() + scenario.measureSeconds() + 60);
            resources = new ResourceSampleBuffer(resourceCapacity);
            EntityRenderOrchestrator.resetDiagnostics();
            lastRenderCount = 0;
        }
        workload.maintain(minecraft);
        if (workload.isReady(minecraft)) {
            int actualCount = workload.actualEntityCount(minecraft);
            if (actualCount != scenario.entityCount()) {
                throw new IllegalStateException("Scenario entity count mismatch: configured="
                        + scenario.entityCount() + ", actual=" + actualCount);
            }
            phaseStartNs = System.nanoTime();
            lastFrameStartNs = 0L;
            lastResourceSampleNs = 0L;
            state = State.WARMUP;
            LOGGER.info("[Benchmark] Warmup started: {} ({}s)", scenario.id(), scenario.warmupSeconds());
            return;
        }
        if (minecraft.level.getGameTime() - prepareStartTick > PREPARE_TIMEOUT_TICKS) {
            throw new IllegalStateException("Scenario preparation timed out after " + PREPARE_TIMEOUT_TICKS + " ticks");
        }
    }

    private void maintainWorkload(Minecraft minecraft) {
        if (workload == null) {
            throw new IllegalStateException("Active workload is missing");
        }
        workload.maintain(minecraft);
    }

    private void sampleFrame(Minecraft minecraft, long nowNs) {
        if (frames == null || resources == null || scenario == null) {
            throw new IllegalStateException("Sampling state is incomplete");
        }
        BenchmarkPhase phase = state == State.WARMUP ? BenchmarkPhase.WARMUP : BenchmarkPhase.MEASURE;
        if (lastFrameStartNs != 0L) {
            long intervalNs = nowNs - lastFrameStartNs;
            int currentRenderCount = EntityRenderOrchestrator.getRenderCount();
            int renderedEntityCount = currentRenderCount - lastRenderCount;
            lastRenderCount = currentRenderCount;
            if (!frames.record(nowNs, intervalNs, minecraft.getFrameTimeNs(), renderedEntityCount, phase)) {
                throw new IllegalStateException("Frame sample buffer overflow at capacity " + frames.capacity());
            }
        } else {
            lastRenderCount = EntityRenderOrchestrator.getRenderCount();
        }
        lastFrameStartNs = nowNs;

        if (lastResourceSampleNs == 0L || nowNs - lastResourceSampleNs >= SECOND_NS) {
            sampleResources(nowNs);
            lastResourceSampleNs = nowNs;
        }

        long elapsedNs = nowNs - phaseStartNs;
        if (state == State.WARMUP && elapsedNs >= scenario.warmupSeconds() * SECOND_NS) {
            state = State.MEASURE;
            phaseStartNs = nowNs;
            LOGGER.info("[Benchmark] Measurement started: {} ({}s)", scenario.id(), scenario.measureSeconds());
        } else if (state == State.MEASURE && elapsedNs >= scenario.measureSeconds() * SECOND_NS) {
            state = State.COOLDOWN;
        }
    }

    private void sampleResources(long nowNs) {
        if (resources == null) {
            return;
        }
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long gcCount = 0L;
        long gcPauseMs = 0L;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gc.getCollectionCount();
            long time = gc.getCollectionTime();
            if (count > 0L) {
                gcCount += count;
            }
            if (time > 0L) {
                gcPauseMs += time;
            }
        }
        double processCpuLoad = operatingSystemBean == null ? -1.0 : operatingSystemBean.getProcessCpuLoad();
        if (!resources.record(nowNs, heap.getUsed(), heap.getCommitted(), gcCount, gcPauseMs, processCpuLoad)) {
            throw new IllegalStateException("Resource sample buffer overflow at capacity " + resources.capacity());
        }
    }

    private void handleCooldown(Minecraft minecraft) throws Exception {
        if (scenario == null || workload == null || frames == null || resources == null) {
            throw new IllegalStateException("Cooldown state is incomplete");
        }
        int actualEntityCount = workload.actualEntityCount(minecraft);
        if (actualEntityCount != scenario.entityCount()) {
            throw new IllegalStateException("Scenario entity count changed during measurement: configured="
                    + scenario.entityCount() + ", actual=" + actualEntityCount);
        }
        long measuredEyelibRenders = 0L;
        for (int i = 0; i < frames.size(); i++) {
            if (frames.phase(i) == BenchmarkPhase.MEASURE && frames.renderedEntityCount(i) > 0) {
                measuredEyelibRenders += frames.renderedEntityCount(i);
            }
        }
        if (measuredEyelibRenders == 0L) {
            throw new IllegalStateException("No successful Eyelib entity renders were observed during measurement");
        }
        BenchmarkStatistics.Summary summary = BenchmarkStatistics.summarize(frames);
        BenchmarkResult result = new BenchmarkResult(
                scenario,
                frames,
                resources,
                summary,
                "passed",
                null,
                scenario.entityCount(),
                actualEntityCount,
                EntityRenderOrchestrator.getErrorCount(),
                EntityRenderOrchestrator.getLastError()
        );
        Path path = reportWriter.writeScenario(result, minecraft);
        scenarioReported = true;
        LOGGER.info("[Benchmark] Scenario complete: {} — {} FPS, p99={}ms, report={}",
                scenario.id(), summary.throughputFps(), summary.frameIntervalPercentilesMs().p99Ms(), path);
        closeWorkload();
        state = State.NEXT_SCENARIO;
    }

    private void handleNextScenario(Minecraft minecraft) throws IOException {
        scenarioIndex++;
        if (scenarioIndex >= scenarios.size()) {
            reportWriter.finish("passed", null);
            state = State.EXIT;
            LOGGER.info("[Benchmark] All {} scenario(s) completed; report={}", scenarios.size(), reportWriter.runDirectory());
            return;
        }
        beginScenarioPreparation(minecraft);
    }

    private void handleExit(Minecraft minecraft) {
        if (terminalHandled) {
            return;
        }
        terminalHandled = true;
        restoreOptions(minecraft);
        unregister();
        if (ClientBenchmarkConfig.shouldAutoExit()) {
            minecraft.stop();
        } else {
            state = State.IDLE;
        }
    }

    private void handleError(Minecraft minecraft) {
        if (terminalHandled) {
            return;
        }
        terminalHandled = true;
        try {
            if (!scenarioReported && scenario != null && frames != null && resources != null) {
                BenchmarkResult result = new BenchmarkResult(
                        scenario,
                        frames,
                        resources,
                        BenchmarkStatistics.summarize(frames),
                        "failed",
                        terminalError,
                        scenario.entityCount(),
                        workload == null ? 0 : workload.actualEntityCount(minecraft),
                        EntityRenderOrchestrator.getErrorCount(),
                        EntityRenderOrchestrator.getLastError()
                );
                reportWriter.writeScenario(result, minecraft);
            }
            reportWriter.finish("failed", terminalError);
        } catch (Exception reportFailure) {
            LOGGER.error("[Benchmark] Failed to write error report", reportFailure);
        }
        try {
            closeWorkload();
        } catch (Exception closeFailure) {
            LOGGER.error("[Benchmark] Failed to close workload after error", closeFailure);
        }
        restoreOptions(minecraft);
        unregister();
        if (ClientBenchmarkConfig.shouldAutoExit()) {
            minecraft.stop();
        } else {
            state = State.IDLE;
        }
    }

    private void applyScenarioOptions(Minecraft minecraft, BenchmarkScenario selected) {
        minecraft.options.enableVsync().set(false);
        minecraft.options.framerateLimit().set(selected.targetFps() == 0 ? 260 : selected.targetFps());
        minecraft.options.hideGui = true;
        minecraft.options.pauseOnLostFocus = false;
    }

    private void restoreOptions(Minecraft minecraft) {
        if (originalOptions == null) {
            return;
        }
        minecraft.options.enableVsync().set(originalOptions.vsync());
        minecraft.options.framerateLimit().set(originalOptions.framerateLimit());
        minecraft.options.hideGui = originalOptions.hideGui();
        minecraft.options.pauseOnLostFocus = originalOptions.pauseOnLostFocus();
    }

    private void closeWorkload() throws Exception {
        if (workload != null) {
            workload.close();
        }
        workload = null;
        scenario = null;
        frames = null;
        resources = null;
        lastFrameStartNs = 0L;
        lastResourceSampleNs = 0L;
    }

    private void fail(Throwable throwable) {
        if (state == State.ERROR || state == State.EXIT || state == State.IDLE) {
            return;
        }
        terminalError = throwable.toString();
        state = State.ERROR;
        LOGGER.error("[Benchmark] Failed in scenario {}", scenario == null ? "<initialization>" : scenario.id(), throwable);
    }

    private void unregister() {
        //? if <1.20.6 {
        MinecraftForge.EVENT_BUS.unregister(this);
        //?} else {
        NeoForge.EVENT_BUS.unregister(this);
        //?}
        installed = null;
    }

    private enum State {
        INIT,
        WORLD_WAIT,
        STABILIZE,
        PREPARE,
        WARMUP,
        MEASURE,
        COOLDOWN,
        NEXT_SCENARIO,
        EXIT,
        ERROR,
        IDLE
    }

    private record OptionSnapshot(boolean vsync, int framerateLimit, boolean hideGui, boolean pauseOnLostFocus) {
    }
}

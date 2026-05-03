package io.github.tt432.eyelib.client.instrument;

import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.instrument.collector.JvmMetricCollector;
import io.github.tt432.eyelib.client.instrument.db.BackgroundFlushService;
import io.github.tt432.eyelib.client.instrument.event.EventRingBuffer;
import io.github.tt432.eyelib.client.instrument.event.InstrumentEvent;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleRenderManager;
import io.github.tt432.eyelib.client.render.RenderHelper;
import io.github.tt432.eyelibmolang.MolangValue;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "eyelib", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class InstrumentLifecycleHooks {
    private static final Logger LOG = Logger.getLogger(InstrumentLifecycleHooks.class.getName());

    private InstrumentLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (InstrumentConfig.isEnabled()) {
            try {
                BackgroundFlushService.getInstance().install();
                LOG.info("Eyelib instrumentation started");
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to start eyelib instrumentation", e);
            }
        }
    }

    private static void collectFastMetrics() {
        EventRingBuffer buffer = EventRingBuffer.getInstance();

        // JVM metrics (heap, GC, threads)
        JvmMetricCollector jvmCollector = new JvmMetricCollector();
        List<InstrumentEvent> jvmEvents = jvmCollector.poll();
        for (InstrumentEvent e : jvmEvents) {
            buffer.offer(e);
        }

        // Cache size observers — collect from available observers
        // Note: These need access to actual cache instances which are obtained elsewhere
        // For now, collect what's statically available
        collectStaticObservers(buffer);
    }

    private static void collectSlowMetrics() {
        EventRingBuffer buffer = EventRingBuffer.getInstance();
        // MolangDiskCache check — file count
        // The disk cache is not statically accessible; skip for now
        // (Requires MolangCompileCache with disk cache enabled at runtime)
    }

    private static void collectStaticObservers(EventRingBuffer buffer) {
        // BrParticleRenderManager — static fields accessible
        int emitters = BrParticleRenderManager.getEmitterCount();
        int particles = BrParticleRenderManager.getParticleCount();
        buffer.offer(new InstrumentEvent("cache_size", "BrParticleRenderManager", "emitter_count", emitters, "count", null));
        buffer.offer(new InstrumentEvent("cache_size", "BrParticleRenderManager", "particle_count", particles, "count", null));

        // RenderHelper.dfsModels — static field
        int dfsModels = RenderHelper.getDfsModelsSize();
        buffer.offer(new InstrumentEvent("cache_size", "RenderHelper", "dfs_models", dfsModels, "count", null));

        // MolangValue constant pool — static field
        int constantPool = MolangValue.getConstantPoolSize();
        buffer.offer(new InstrumentEvent("cache_size", "MolangValue", "constant_pool_entries", constantPool, "count", null));
    }

    // Using FORGE bus for game events (logout)
    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "eyelib", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            if (InstrumentConfig.isEnabled()) {
                try {
                    BackgroundFlushService.getInstance().shutdown();
                    LOG.info("Eyelib instrumentation stopped on logout");
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to stop eyelib instrumentation on logout", e);
                }
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            if (!InstrumentConfig.isEnabled()) return;

            // Use ClientTickHandler.getTick() for tick counting
            int currentTick = ClientTickHandler.getTick();

            // Every 5 seconds (100 ticks) — collect fast metrics
            if (currentTick % 100 == 0) {
                collectFastMetrics();
            }

            // Every 30 seconds (600 ticks) — collect slow metrics (disk cache)
            if (currentTick % 600 == 0) {
                collectSlowMetrics();
            }
        }
    }

    static {
        // JVM shutdown hook as last resort fallback
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (InstrumentConfig.isEnabled()) {
                try {
                    BackgroundFlushService.getInstance().shutdown();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to flush instrumentation on JVM shutdown", e);
                }
            }
        }, "eyelib-instrument-shutdown"));
    }
}

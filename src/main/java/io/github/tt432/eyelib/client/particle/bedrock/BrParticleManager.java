package io.github.tt432.eyelib.client.particle.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BrParticleManager {
    private static final Map<String, BrParticleEmitter> emitters = new ConcurrentHashMap<>();
    private static final List<BrParticleParticle> particles = Collections.synchronizedList(new ArrayList<>());

    public static void spawnEmitter(final String id, final BrParticleEmitter emitter) {
        if (emitters.containsKey(id)) return;
        Thread.startVirtualThread(() -> emitters.put(id, emitter));
    }

    public static void removeEmitter(final String id) {
        Thread.startVirtualThread(() -> emitters.remove(id));
    }

    public static void spawnParticle(final BrParticleParticle particle) {
        particles.add(particle);
    }

    @EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onEvent(FMLClientSetupEvent event) {
            Thread.startVirtualThread(() -> {
                while (true) {
                    Minecraft instance = Minecraft.getInstance();

                    if (instance.level != null) {
                        emitters.forEach((k, e) -> e.getTimer().setPaused(instance.isPaused()));
                        particles.forEach(e -> e.getTimer().setPaused(instance.isPaused()));

                        emitters.entrySet().removeIf(e -> e.getValue().isRemoved());
                        emitters.values().forEach(BrParticleEmitter::onRenderFrame);
                        particles.removeIf(BrParticleParticle::isRemoved);
                        particles.forEach(BrParticleParticle::onRenderFrame);
                    }

                    Thread.yield();
                }
            });
        }
    }

    @EventBusSubscriber(Dist.CLIENT)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onEvent(ClientTickEvent.Pre event) {
            emitters.values().forEach(BrParticleEmitter::onTick);
        }

        @SubscribeEvent
        public static void onEvent(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                PoseStack poseStack = event.getPoseStack();
                particles.forEach(particle -> {
                    var buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(
                            RenderType.entityCutout(particle.getTexture().withSuffix(".png"))
                    );
                    particle.render(poseStack, buffer);
                });
            }
        }

        @SubscribeEvent
        public static void onEvent(ClientPlayerNetworkEvent.LoggingOut event) {
            emitters.clear();
            particles.clear();
        }
    }
}

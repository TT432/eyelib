package io.github.tt432.eyelib.client.particle.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.util.client.RenderTypeSerializations;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BrParticleRenderManager {
    private static final Map<String, BrParticleEmitter> emitters = new ConcurrentHashMap<>();
    private static final List<BrParticleParticle> particles = new CopyOnWriteArrayList<>();

    public static void spawnEmitter(final String id, final BrParticleEmitter emitter) {
        if (emitters.containsKey(id)) return;
//        Thread.startVirtualThread(() -> emitters.put(id, emitter));
    }

    public static void removeEmitter(final String id) {
//        Thread.startVirtualThread(() -> emitters.remove(id));
    }

    public static void spawnParticle(final BrParticleParticle particle) {
        particles.add(particle);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onEvent(FMLClientSetupEvent event) {
            new Thread(() -> {
                Minecraft instance = Minecraft.getInstance();

                BiConsumer<String, BrParticleEmitter> processEmitters = (k, e) -> e.getTimer().setPaused(instance.isPaused());
                Consumer<BrParticleParticle> processParticles = e -> e.getTimer().setPaused(instance.isPaused());
                Predicate<Map.Entry<String, BrParticleEmitter>> removeEmitters = e -> e.getValue().isRemoved();
                Consumer<BrParticleEmitter> renderEmitters = BrParticleEmitter::onRenderFrame;
                Predicate<BrParticleParticle> removeParticles = BrParticleParticle::isRemoved;
                Consumer<BrParticleParticle> renderParticles = BrParticleParticle::onRenderFrame;

                while (true) {
                    if (instance.level != null) {
                        emitters.forEach(processEmitters);
                        particles.forEach(processParticles);

                        emitters.entrySet().removeIf(removeEmitters);
                        emitters.values().forEach(renderEmitters);
                        particles.removeIf(removeParticles);
                        particles.forEach(renderParticles);
                        Thread.yield();
                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, "Eyelib Particle Thread").start();
        }
    }

    @Mod.EventBusSubscriber(Dist.CLIENT)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onEvent(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START)
                emitters.values().forEach(BrParticleEmitter::onTick);
        }

        @SubscribeEvent
        public static void onEvent(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                PoseStack poseStack = event.getPoseStack();
                particles.forEach(particle -> {
                    String material = particle.getEmitter().getParticle().particleEffect().description().basicRenderParameters().material();
                    RenderTypeSerializations.EntityRenderTypeData factory = RenderTypeSerializations.getFactory(new ResourceLocation(material));
                    var buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(
                            factory.factory().apply(particle.getTexture().withSuffix(".png"))
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

package io.github.tt432.eyelib.client.particle.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.loader.BrParticleLoader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BrParticleManager {
    private static final List<BrParticleEmitter> emitters = Collections.synchronizedList(new ArrayList<>());
    private static final List<BrParticleParticle> particles = Collections.synchronizedList(new ArrayList<>());

    public static void spawnEmitter(final BrParticleEmitter emitter) {
        emitters.add(emitter);
    }

    public static void spawnParticle(final BrParticleParticle particle) {
        particles.add(particle);
    }

    @EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onEvent(FMLClientSetupEvent event) {
            new Thread(() -> {
                while (true) {
                    Minecraft instance = Minecraft.getInstance();

                    synchronized (instance) {
                        if (instance.level != null) {
                            emitters.forEach(e -> e.getTimer().setPaused(instance.isPaused()));
                            particles.forEach(e -> e.getTimer().setPaused(instance.isPaused()));

                            emitters.removeIf(BrParticleEmitter::isRemoved);
                            emitters.forEach(BrParticleEmitter::onRenderFrame);
                            particles.removeIf(BrParticleParticle::isRemoved);
                            particles.forEach(BrParticleParticle::onRenderFrame);
                        }
                    }
                }
            }).start();
        }
    }

    @EventBusSubscriber(Dist.CLIENT)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onEvent(ClientTickEvent.Pre event) {
            emitters.forEach(BrParticleEmitter::onTick);
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

        @SubscribeEvent
        public static void onEvent(ClientPlayerNetworkEvent.LoggingIn event) {
            LocalPlayer player = event.getPlayer();
            emitters.add(new BrParticleEmitter(
                    BrParticleLoader.getParticle(ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "rainbow")),
                    RenderData.getComponent(player).getScope(),
                    player.level(),
                    new Vector3f()
            ));
        }
    }
}

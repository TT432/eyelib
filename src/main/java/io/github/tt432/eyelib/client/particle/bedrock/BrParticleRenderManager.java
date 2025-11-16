package io.github.tt432.eyelib.client.particle.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.util.client.RenderTypeSerializations;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
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

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BrParticleRenderManager {
    private static final Object2ObjectMap<String, BrParticleEmitter> emitters = new Object2ObjectOpenHashMap<>();
    private static final ObjectList<BrParticleParticle> particles = new ObjectArrayList<>();

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

    private static final Predicate<Map.Entry<String, BrParticleEmitter>> removeEmitters = e -> e.getValue().isRemoved();
    private static final Consumer<BrParticleEmitter> renderEmitters = BrParticleEmitter::onRenderFrame;
    private static final Predicate<BrParticleParticle> removeParticles = BrParticleParticle::isRemoved;
    private static final Consumer<BrParticleParticle> renderParticles = BrParticleParticle::onRenderFrame;

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onEvent(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START){
                emitters.object2ObjectEntrySet().removeIf(removeEmitters);
                emitters.values().forEach(renderEmitters);
                particles.removeIf(removeParticles);
                particles.forEach(renderParticles);
            }
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

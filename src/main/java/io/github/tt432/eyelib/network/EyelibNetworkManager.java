package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.loader.BrParticleLoader;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * @author TT432
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibNetworkManager {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Eyelib.MOD_ID).optional();

        registrar.playToClient(ModelComponentSyncPacket.TYPE, ModelComponentSyncPacket.STREAM_CODEC,
                (payload, context) -> {
                    if (Minecraft.getInstance().level == null) return;
                    Entity entity = Minecraft.getInstance().level.getEntity(payload.entityId());
                    if (entity == null) return;
                    RenderData<?> data = RenderData.getComponent(entity);
                    if (data == null) return;
                    data.getModelComponent().setInfo(payload.modelInfo());
                });

        registrar.playToClient(AnimationComponentSyncPacket.TYPE, AnimationComponentSyncPacket.STREAM_CODEC,
                (payload, context) -> {
                    if (Minecraft.getInstance().level == null) return;
                    Entity entity = Minecraft.getInstance().level.getEntity(payload.entityId());
                    if (entity == null) return;
                    RenderData<?> data = RenderData.getComponent(entity);
                    var info = payload.animationInfo();
                    data.getAnimationComponent().setup(info.animationControllers(), info.targetAnimations());
                });

        registrar.playToClient(RemoveParticlePacket.TYPE, RemoveParticlePacket.STREAM_CODEC,
                (payload, context) -> BrParticleManager.removeEmitter(payload.removeId()));

        registrar.playToClient(SpawnParticlePacket.TYPE, SpawnParticlePacket.STREAM_CODEC,
                (payload, context) -> {
                    BrParticle particle = BrParticleLoader.getParticle(payload.particleId());
                    if (particle != null) {
                        BrParticleManager.spawnEmitter(
                                payload.spawnId(),
                                new BrParticleEmitter(
                                        particle,
                                        Minecraft.getInstance().player.getData(EyelibAttachableData.RENDER_DATA).getScope(),
                                        // 防止服务端加载 ClientLevel
                                        (Level) (Object) Minecraft.getInstance().level,
                                        payload.position()
                                )
                        );
                    }
                });
    }
}

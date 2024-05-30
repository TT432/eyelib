package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.AnimatableComponent;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
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
        final PayloadRegistrar registrar = event.registrar(Eyelib.MOD_ID);
        registrar.playToClient(ModelComponentSyncPacket.TYPE,
                ModelComponentSyncPacket.STREAM_CODEC,
                (payload, context) -> {
                    AnimatableComponent<Object> data = Minecraft.getInstance().level.getEntity(payload.entityId())
                            .getData(EyelibAttachableData.ANIMATABLE);
                    data.getModelComponent().setInfo(payload.modelInfo());
                });
        registrar.playToClient(AnimationComponentSyncPacket.TYPE,
                AnimationComponentSyncPacket.STREAM_CODEC,
                (payload, context) -> {
                    AnimatableComponent<Object> data = Minecraft.getInstance().level.getEntity(payload.entityId())
                            .getData(EyelibAttachableData.ANIMATABLE);
                    var info = payload.animationInfo();
                    data.getAnimationComponent().setup(info.animationControllers(), info.targetAnimations());
                });
    }
}

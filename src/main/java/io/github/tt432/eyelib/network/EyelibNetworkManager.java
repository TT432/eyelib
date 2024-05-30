package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.AnimatableComponent;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibNetworkManager {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Eyelib.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    static int id;

    public static void register() {
        INSTANCE.registerMessage(id++,
                ModelComponentSyncPacket.class,
                ModelComponentSyncPacket::encode,
                ModelComponentSyncPacket::decode,
                (payload, context) -> {
                    AnimatableComponent<?> data = Minecraft.getInstance().level.getEntity(payload.entityId())
                            .getCapability(EyelibAttachableData.ANIMATABLE).resolve().orElse(null);
                    if (data == null) return;
                    data.getModelComponent().setInfo(payload.modelInfo());
                });
        INSTANCE.registerMessage(id++,
                AnimationComponentSyncPacket.class,
                AnimationComponentSyncPacket::encode,
                AnimationComponentSyncPacket::decode,
                (payload, context) -> {
                    AnimatableComponent<?> data = Minecraft.getInstance().level.getEntity(payload.entityId())
                            .getCapability(EyelibAttachableData.ANIMATABLE).resolve().orElse(null);
                    if (data == null) return;
                    var info = payload.animationInfo();
                    data.getAnimationComponent().setup(info.animationControllers(), info.targetAnimations());
                });
    }
}

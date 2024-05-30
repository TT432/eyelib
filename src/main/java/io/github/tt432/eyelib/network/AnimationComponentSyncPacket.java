package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.client.animation.component.AnimationComponent;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record AnimationComponentSyncPacket(
        int entityId,
        AnimationComponent.SerializableInfo animationInfo
) {
    public static void encode(AnimationComponentSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeResourceLocation(packet.animationInfo().targetAnimations());
        buf.writeResourceLocation(packet.animationInfo().animationControllers());
    }

    public static AnimationComponentSyncPacket decode(FriendlyByteBuf byteBuf) {
        return new AnimationComponentSyncPacket(
                byteBuf.readInt(),
                new AnimationComponent.SerializableInfo(
                        byteBuf.readResourceLocation(),
                        byteBuf.readResourceLocation()
                )
        );
    }
}

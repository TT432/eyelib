package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * @author TT432
 */
public record AnimationComponentSyncPacket(
        int entityId,
        AnimationComponent.SerializableInfo animationInfo
) implements CustomPacketPayload {
    public static final Type<AnimationComponentSyncPacket> TYPE =
            new Type<>(ResourceLocations.of(Eyelib.MOD_ID, "animation_component"));

    public static final StreamCodec<ByteBuf, AnimationComponentSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            AnimationComponentSyncPacket::entityId,
            AnimationComponent.SerializableInfo.STREAM_CODEC,
            AnimationComponentSyncPacket::animationInfo,
            AnimationComponentSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

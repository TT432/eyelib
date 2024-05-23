package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.animation.component.AnimationComponent;
import io.github.tt432.eyelib.client.animation.component.ModelComponent;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * @author TT432
 */
public record AnimatableComponentSyncPacket(
        int entityId,
        ModelComponent.SerializableInfo modelInfo,
        AnimationComponent.SerializableInfo animationInfo
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AnimatableComponentSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(new ResourceLocation(Eyelib.MOD_ID, "animatable_component"));

    public static final StreamCodec<ByteBuf, AnimatableComponentSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            AnimatableComponentSyncPacket::entityId,
            ModelComponent.SerializableInfo.STREAM_CODEC,
            AnimatableComponentSyncPacket::modelInfo,
            AnimationComponent.SerializableInfo.STREAM_CODEC,
            AnimatableComponentSyncPacket::animationInfo,
            AnimatableComponentSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

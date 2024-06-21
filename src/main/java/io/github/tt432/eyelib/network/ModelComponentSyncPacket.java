package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * @author TT432
 */
public record ModelComponentSyncPacket(
        int entityId,
        ModelComponent.SerializableInfo modelInfo
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ModelComponentSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "model_component"));

    public static final StreamCodec<ByteBuf, ModelComponentSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ModelComponentSyncPacket::entityId,
            ModelComponent.SerializableInfo.STREAM_CODEC,
            ModelComponentSyncPacket::modelInfo,
            ModelComponentSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

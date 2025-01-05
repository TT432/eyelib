package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public record ModelComponentSyncPacket(
        int entityId,
        List<ModelComponent.SerializableInfo> modelInfo
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ModelComponentSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocations.of(Eyelib.MOD_ID, "model_component"));

    public static final StreamCodec<ByteBuf, ModelComponentSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ModelComponentSyncPacket::entityId,
            ByteBufCodecs.collection(ArrayList::new, ModelComponent.SerializableInfo.STREAM_CODEC),
            ModelComponentSyncPacket::modelInfo,
            ModelComponentSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

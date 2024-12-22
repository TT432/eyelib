package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.ExtraEntityData;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * @author TT432
 */
public record ExtraEntityDataPacket(
        int entityId,
        ExtraEntityData data
) implements CustomPacketPayload {
    public static final Type<ExtraEntityDataPacket> TYPE =
            new Type<>(ResourceLocations.of(Eyelib.MOD_ID, "extra_entity_data"));

    public static final StreamCodec<ByteBuf, ExtraEntityDataPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ExtraEntityDataPacket::entityId,
            ExtraEntityData.STREAM_CODEC,
            ExtraEntityDataPacket::data,
            ExtraEntityDataPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

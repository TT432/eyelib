package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.capability.ExtraEntityUpdateData;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * @author TT432
 */
public record ExtraEntityUpdateDataPacket(
        int entityId,
        ExtraEntityUpdateData data
) implements CustomPacketPayload {
    public static final Type<ExtraEntityUpdateDataPacket> TYPE =
            new Type<>(ResourceLocations.of(Eyelib.MOD_ID, "extra_entity_update"));

    public static final StreamCodec<ByteBuf, ExtraEntityUpdateDataPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ExtraEntityUpdateDataPacket::entityId,
            ExtraEntityUpdateData.STREAM_CODEC,
            ExtraEntityUpdateDataPacket::data,
            ExtraEntityUpdateDataPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * @author TT432
 */
public record UpdateDestroyInfoPacket(
        boolean dig
) implements CustomPacketPayload {
    public static final Type<UpdateDestroyInfoPacket> TYPE =
            new Type<>(ResourceLocations.of(Eyelib.MOD_ID, "update_destroy_info"));

    public static final StreamCodec<ByteBuf, UpdateDestroyInfoPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            UpdateDestroyInfoPacket::dig,
            UpdateDestroyInfoPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
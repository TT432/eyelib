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
public record RemoveParticlePacket(
        String removeId
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RemoveParticlePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocations.of(Eyelib.MOD_ID, "remove_particle"));

    public static final StreamCodec<ByteBuf, RemoveParticlePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            RemoveParticlePacket::removeId,
            RemoveParticlePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

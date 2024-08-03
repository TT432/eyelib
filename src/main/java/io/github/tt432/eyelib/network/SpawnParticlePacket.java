package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public record SpawnParticlePacket(
        String spawnId,
        ResourceLocation particleId,
        Vector3f position
) implements CustomPacketPayload {
    public static final Type<SpawnParticlePacket> TYPE =
            new Type<>(ResourceLocations.of(Eyelib.MOD_ID, "spawn_particle"));

    public static final StreamCodec<ByteBuf, SpawnParticlePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SpawnParticlePacket::spawnId,
            ResourceLocation.STREAM_CODEC,
            SpawnParticlePacket::particleId,
            ByteBufCodecs.VECTOR3F,
            SpawnParticlePacket::position,
            SpawnParticlePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

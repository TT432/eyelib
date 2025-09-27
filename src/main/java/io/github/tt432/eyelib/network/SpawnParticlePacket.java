package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public record SpawnParticlePacket(
        String spawnId,
        ResourceLocation particleId,
        Vector3f position
) {
    public static final StreamCodec<SpawnParticlePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(SpawnParticlePacket obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.STRING.encode(obj.spawnId, buf);
            EyelibStreamCodecs.RESOURCE_LOCATION.encode(obj.particleId, buf);
            EyelibStreamCodecs.VECTOR_3_F.encode(obj.position, buf);
        }

        @Override
        public SpawnParticlePacket decode(FriendlyByteBuf buf) {
            var spawnId = EyelibStreamCodecs.STRING.decode(buf);
            var particleId = EyelibStreamCodecs.RESOURCE_LOCATION.decode(buf);
            var position = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
            return new SpawnParticlePacket(spawnId, particleId, position);
        }
    };
}

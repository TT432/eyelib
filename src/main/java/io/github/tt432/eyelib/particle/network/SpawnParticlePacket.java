package io.github.tt432.eyelibparticle.network;

import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;

/** @author TT432 */
public record SpawnParticlePacket(
        String spawnId,
        String particleId,
        Vector3f position
) {
    public static final ParticleStreamCodec<SpawnParticlePacket> STREAM_CODEC = new ParticleStreamCodec<>() {
        @Override
        public void encode(SpawnParticlePacket packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.spawnId);
            buf.writeUtf(packet.particleId);
            buf.writeFloat(packet.position.x());
            buf.writeFloat(packet.position.y());
            buf.writeFloat(packet.position.z());
        }

        @Override
        public SpawnParticlePacket decode(FriendlyByteBuf buf) {
            String spawnId = buf.readUtf();
            String particleId = buf.readUtf();
            Vector3f position = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
            return new SpawnParticlePacket(spawnId, particleId, position);
        }
    };
}
package io.github.tt432.eyelibparticle.network;

import net.minecraft.network.FriendlyByteBuf;

public record RemoveParticlePacket(
        String removeId
) {
    public static final ParticleStreamCodec<RemoveParticlePacket> STREAM_CODEC = new ParticleStreamCodec<>() {
        @Override
        public void encode(RemoveParticlePacket packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.removeId);
        }

        @Override
        public RemoveParticlePacket decode(FriendlyByteBuf buf) {
            String removeId = buf.readUtf();
            return new RemoveParticlePacket(removeId);
        }
    };
}

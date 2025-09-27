package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record RemoveParticlePacket(
        String removeId
) {
    public static final StreamCodec<RemoveParticlePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RemoveParticlePacket obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.STRING.encode(obj.removeId, buf);
        }

        @Override
        public RemoveParticlePacket decode(FriendlyByteBuf buf) {
            var removeId = EyelibStreamCodecs.STRING.decode(buf);
            return new RemoveParticlePacket(removeId);
        }
    };
}

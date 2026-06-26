package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.bridge.network.particle.RemoveParticlePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** @author TT432 */
class RemoveParticlePacketTest {
    @Test
    void packetCarriesStringRemoveIdContract() {
        RemoveParticlePacket packet = new RemoveParticlePacket("not-a-resource-location");

        assertEquals("not-a-resource-location", packet.removeId());
    }

    @Test
    void streamCodecRoundTripsStringRemoveIdContract() {
        RemoveParticlePacket original = new RemoveParticlePacket("not-a-resource-location");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        try {
            RemoveParticlePacket.STREAM_CODEC.encode(original, buf);
            RemoveParticlePacket decoded = RemoveParticlePacket.STREAM_CODEC.decode(buf);

            assertEquals(original.removeId(), decoded.removeId());
        } finally {
            buf.release();
        }
    }
}

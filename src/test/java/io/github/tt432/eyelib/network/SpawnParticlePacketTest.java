package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.particle.network.SpawnParticlePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** @author TT432 */
class SpawnParticlePacketTest {
    @Test
    void packetCarriesStringParticleIdContract() {
        SpawnParticlePacket packet = new SpawnParticlePacket(
                "spawn-id",
                "not-a-resource-location",
                new Vector3f(1F, 2F, 3F)
        );

        assertEquals("spawn-id", packet.spawnId());
        assertEquals("not-a-resource-location", packet.particleId());
        assertEquals(new Vector3f(1F, 2F, 3F), packet.position());
    }

    @Test
    void streamCodecRoundTripsStringParticleIdContract() {
        SpawnParticlePacket original = new SpawnParticlePacket(
                "spawn-id",
                "not-a-resource-location",
                new Vector3f(1F, 2F, 3F)
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        try {
            SpawnParticlePacket.STREAM_CODEC.encode(original, buf);
            SpawnParticlePacket decoded = SpawnParticlePacket.STREAM_CODEC.decode(buf);

            assertEquals(original.spawnId(), decoded.spawnId());
            assertEquals(original.particleId(), decoded.particleId());
            assertEquals(original.position(), decoded.position());
        } finally {
            buf.release();
        }
    }
}

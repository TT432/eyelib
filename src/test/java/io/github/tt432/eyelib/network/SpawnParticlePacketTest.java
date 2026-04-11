package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.mc.impl.network.packet.SpawnParticlePacket;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

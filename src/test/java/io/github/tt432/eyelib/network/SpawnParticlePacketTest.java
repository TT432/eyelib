package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.mc.impl.network.packet.SpawnParticlePacket;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void packetCodecOwnershipStaysInMcNetworkPacketLayer() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/SpawnParticlePacket.java"
        ));

        assertTrue(Pattern.compile("record\\s+SpawnParticlePacket\\s*\\(\\s*String\\s+spawnId,\\s*String\\s+particleId,\\s*Vector3f\\s+position", Pattern.DOTALL)
                .matcher(source)
                .find());
        assertTrue(source.contains("EyelibStreamCodecs.STRING.encode(obj.spawnId, buf);"));
        assertTrue(source.contains("EyelibStreamCodecs.STRING.encode(obj.particleId, buf);"));
        assertTrue(source.contains("var spawnId = EyelibStreamCodecs.STRING.decode(buf);"));
        assertTrue(source.contains("var particleId = EyelibStreamCodecs.STRING.decode(buf);"));
        assertTrue(source.contains("import net.minecraft.network.FriendlyByteBuf;"));
    }
}

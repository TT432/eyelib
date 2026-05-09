package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.mc.impl.network.packet.SpawnParticlePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
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

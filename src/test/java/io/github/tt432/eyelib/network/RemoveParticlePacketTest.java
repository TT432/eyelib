package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.particle.network.RemoveParticlePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void packetCodecOwnershipStaysInParticleModule() throws IOException {
        String source = Files.readString(Path.of(
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/network/RemoveParticlePacket.java"
        ));

        assertTrue(Pattern.compile("record\\s+RemoveParticlePacket\\s*\\(\\s*String\\s+removeId", Pattern.DOTALL)
                .matcher(source)
                .find());
        assertTrue(source.contains("buf.writeUtf(packet.removeId);"));
        assertTrue(source.contains("String removeId = buf.readUtf();"));
        assertTrue(source.contains("import net.minecraft.network.FriendlyByteBuf;"));
    }
}

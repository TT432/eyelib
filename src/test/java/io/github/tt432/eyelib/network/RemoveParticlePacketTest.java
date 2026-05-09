package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.mc.impl.network.packet.RemoveParticlePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void packetCodecOwnershipStaysInMcNetworkPacketLayer() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/RemoveParticlePacket.java"
        ));

        assertTrue(Pattern.compile("record\\s+RemoveParticlePacket\\s*\\(\\s*String\\s+removeId", Pattern.DOTALL)
                .matcher(source)
                .find());
        assertTrue(source.contains("EyelibStreamCodecs.STRING.encode(obj.removeId, buf);"));
        assertTrue(source.contains("var removeId = EyelibStreamCodecs.STRING.decode(buf);"));
        assertTrue(source.contains("import net.minecraft.network.FriendlyByteBuf;"));
    }
}

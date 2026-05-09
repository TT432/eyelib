package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.mc.impl.network.packet.RemoveParticlePacket;
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

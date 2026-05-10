package io.github.tt432.eyelib.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleNetworkDelegationBoundaryTest {
    @Test
    void packetRecordsStayStringKeyedAndCodecOwnedByParticleModule() throws IOException {
        String spawnPacket = Files.readString(Path.of(
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/network/SpawnParticlePacket.java"
        ));
        String removePacket = Files.readString(Path.of(
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/network/RemoveParticlePacket.java"
        ));

        assertTrue(Pattern.compile("record\\s+SpawnParticlePacket\\s*\\(\\s*String\\s+spawnId,\\s*String\\s+particleId,\\s*Vector3f\\s+position", Pattern.DOTALL)
                .matcher(spawnPacket)
                .find());
        assertTrue(spawnPacket.contains("String particleId"));
        assertTrue(spawnPacket.contains("buf.writeUtf(packet.spawnId);"));
        assertTrue(spawnPacket.contains("buf.writeUtf(packet.particleId);"));
        assertTrue(spawnPacket.contains("String spawnId = buf.readUtf();"));
        assertTrue(spawnPacket.contains("String particleId = buf.readUtf();"));
        assertTrue(spawnPacket.contains("import net.minecraft.network.FriendlyByteBuf;"));

        assertTrue(Pattern.compile("record\\s+RemoveParticlePacket\\s*\\(\\s*String\\s+removeId", Pattern.DOTALL)
                .matcher(removePacket)
                .find());
        assertTrue(removePacket.contains("String removeId"));
        assertTrue(removePacket.contains("buf.writeUtf(packet.removeId);"));
        assertTrue(removePacket.contains("String removeId = buf.readUtf();"));
        assertTrue(removePacket.contains("import net.minecraft.network.FriendlyByteBuf;"));
    }

    @Test
    void transportRegistersRemoveThenSpawnThroughClientHandlers() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/mc/impl/network/EyelibNetworkTransport.java"
        ));

        int removeRegistration = source.indexOf("INSTANCE.messageBuilder(RemoveParticlePacket.class, nextId())");
        int spawnRegistration = source.indexOf("INSTANCE.messageBuilder(SpawnParticlePacket.class, nextId())");

        assertTrue(removeRegistration >= 0);
        assertTrue(spawnRegistration > removeRegistration);
        assertTrue(source.contains("onClientHandle(NetClientHandlers::onRemoveParticlePacket)"));
        assertTrue(source.contains("onClientHandle(NetClientHandlers::onSpawnParticlePacket)"));
    }

    @Test
    void clientHandlersDelegateParticlePacketsOnlyToSpawnService() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java"
        ));

        assertTrue(source.contains("ParticleSpawnService.removeEmitter(packet.removeId());"));
        assertTrue(source.contains("ParticleSpawnService.spawnFromPacket(packet);"));
        assertTrue(!source.contains("ParticleRenderManager"));
        assertTrue(!source.contains("ParticleDefinitionRegistry"));
        assertTrue(!source.contains("BrParticleLoader"));
        assertTrue(!source.contains("BrParticleRenderManager"));
    }

    @Test
    void spawnServiceBuildsModuleRequestAndNoOpsMissingRuntimeState() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java"
        ));
        String adapter = Files.readString(Path.of(
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleSpawnRuntimeAdapter.java"
        ));

        assertTrue(source.contains("new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position())"));
        assertTrue(adapter.contains("definitions.get(request.particleId())"));
        assertTrue(source.contains("Minecraft.getInstance().player == null || Minecraft.getInstance().level == null"));
        assertTrue(adapter.contains("if (definition == null)"));
        assertTrue(adapter.contains("if (runtimeEnvironment.isEmpty())"));
    }
}

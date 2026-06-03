package io.github.tt432.eyelib.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
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
    }

    @Test
    void transportRegistersRemoveThenSpawnThroughClientHandlers() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java"
        ));

        int removeRegistration = source.indexOf("RemoveParticlePacket.class,");
        int spawnRegistration = source.indexOf("SpawnParticlePacket.class,");

        assertTrue(removeRegistration >= 0);
        assertTrue(spawnRegistration > removeRegistration);
        assertTrue(source.contains("NetClientHandlers::onRemoveParticlePacket"));
        assertTrue(source.contains("NetClientHandlers::onSpawnParticlePacket"));
    }

    @Test
    void clientHandlersDelegateParticlePacketsOnlyToSpawnService() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java"
        ));

        assertTrue(source.contains("ParticleSpawnRuntimeAdapter.INSTANCE.remove(packet.removeId());"));
        assertTrue(source.contains("ParticleSpawnRuntimeAdapter.INSTANCE.spawn("));
        assertTrue(!source.contains("ParticleRenderManager"));
        assertTrue(!source.contains("ParticleDefinitionRegistry"));
        assertTrue(!source.contains("BrParticleLoader"));
        assertTrue(!source.contains("BrParticleRenderManager"));
    }

    @Test
    void spawnServiceIsDeletedAndAdapterUsesStaticSuppliers() throws IOException {
        // ParticleSpawnService 已被删除：验证文件不存在
        assertTrue(Files.notExists(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java"
        )));

        // ParticleSpawnRuntimeAdapter 现在使用静态 supplier
        String adapter = Files.readString(Path.of(
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleSpawnRuntimeAdapter.java"
        ));

        assertTrue(adapter.contains("environmentSupplier.get()"));
        assertTrue(adapter.contains("parentScopeSupplier.get()"));
        assertTrue(adapter.contains("definitions.get(request.particleId())"));
        assertTrue(adapter.contains("if (definition == null)"));
        assertTrue(adapter.contains("if (runtimeEnvironment.isEmpty())"));
    }
}

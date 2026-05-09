package io.github.tt432.eyelib.client.particle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleRuntimeDelegationBoundaryTest {
    @Test
    void spawnServiceBuildsModuleRuntimeAndDelegatesToModuleRenderManager() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java"
        ));

        assertTrue(source.contains("import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;"));
        assertTrue(source.contains("import io.github.tt432.eyelibparticle.client.ParticleRenderManager;"));
        assertTrue(source.contains("import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleRuntime;"));
        assertTrue(source.contains("new BedrockParticleRuntime("));
        assertTrue(source.contains("ParticleRenderManager.INSTANCE::spawnParticle"));
        assertTrue(source.contains("ParticleRenderManager.INSTANCE.spawnEmitter("));
        assertTrue(source.contains("ParticleRenderManager.INSTANCE.removeEmitter("));
        assertTrue(source.contains("api().spawn(new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position()))"));
        assertTrue(source.contains("api().remove(removeId);"));
    }

    @Test
    void rootRenderManagerIsThinAdapterToModuleRenderManager() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleRenderManager.java"
        ));

        assertTrue(source.contains("import io.github.tt432.eyelibparticle.client.ParticleRenderManager;"));
        assertTrue(source.contains("ParticleRenderManager.INSTANCE.getEmitterCount()"));
        assertTrue(source.contains("ParticleRenderManager.INSTANCE.getParticleCount()"));
        assertTrue(source.contains("ParticleRenderManager.INSTANCE.spawnEmitter("));
        assertTrue(source.contains("ParticleRenderManager.INSTANCE.removeEmitter("));
        assertTrue(source.contains("ParticleRenderManager.INSTANCE.spawnParticle("));
        assertTrue(source.contains("throw new UnsupportedOperationException("));
        assertTrue(source.contains("Legacy root BrParticleParticle cannot be registered"));
        assertTrue(source.contains("Remove this adapter"));
    }

    @Test
    void spawnAndRemovePacketShapesRemainStringKeyed() throws IOException {
        String spawnPacket = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/SpawnParticlePacket.java"
        ));
        String removePacket = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/RemoveParticlePacket.java"
        ));

        assertTrue(Pattern.compile("record\\s+SpawnParticlePacket\\s*\\(\\s*String\\s+spawnId,\\s*String\\s+particleId,\\s*Vector3f\\s+position", Pattern.DOTALL)
                .matcher(spawnPacket)
                .find());
        assertTrue(spawnPacket.contains("EyelibStreamCodecs.STRING.encode(obj.spawnId, buf);"));
        assertTrue(spawnPacket.contains("EyelibStreamCodecs.STRING.encode(obj.particleId, buf);"));
        assertTrue(spawnPacket.contains("EyelibStreamCodecs.VECTOR_3_F.encode(obj.position, buf);"));

        assertTrue(Pattern.compile("record\\s+RemoveParticlePacket\\s*\\(\\s*String\\s+removeId", Pattern.DOTALL)
                .matcher(removePacket)
                .find());
        assertTrue(removePacket.contains("EyelibStreamCodecs.STRING.encode(obj.removeId, buf);"));
    }
}

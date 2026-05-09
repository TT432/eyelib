package io.github.tt432.eyelib.client.particle;

import com.google.gson.JsonParser;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelibparticle.loading.ParticleResourcePublication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleRuntimeDelegationBoundaryTest {
    @AfterEach
    void clearParticles() {
        ParticleDefinitionRegistry.store().clear();
    }

    @Test
    void spawnServiceBuildsModuleRuntimeAndDelegatesToModuleRenderManager() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java"
        ));

        assertTrue(source.contains("import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;"));
        assertTrue(source.contains("import io.github.tt432.eyelibparticle.client.ParticleRenderManager;"));
        assertTrue(source.contains("import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;"));
        assertTrue(source.contains("import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleRuntime;"));
        assertTrue(source.contains("new BedrockParticleRuntime("));
        assertTrue(source.contains("ParticleRenderManager.INSTANCE::spawnParticle"));
        assertTrue(source.contains("ParticleRenderManager.INSTANCE.spawnEmitter("));
        assertTrue(source.contains("ParticleRenderManager.INSTANCE.removeEmitter("));
        assertTrue(source.contains("api().spawn(new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position()))"));
        assertTrue(source.contains("api().remove(removeId);"));
        assertTrue(source.contains("ParticleDefinitionRegistry.store().get(request.particleId())"));
        assertTrue(source.contains("ParticleDefinition definition,"));
        assertTrue(!source.contains("BrParticle.CODEC.encodeStart"));
    }

    @Test
    void animationParticleEffectsResolvePublishedModuleDefinitions() throws IOException {
        ParticleResourcePublication.replaceFromJsonResources(
                Map.of("particles/runtime.particle", JsonParser.parseString(particleJson("eyelib:runtime_particle"))),
                LoggerFactory.getLogger(ParticleRuntimeDelegationBoundaryTest.class)
        );

        assertNotNull(ParticleLookup.definition("eyelib:runtime_particle"));
        assertNull(ParticleLookup.definition("particles/runtime.particle"));

        String animationEntry = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntryDefinition.java"
        ));
        String controllerExecutor = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/animation/bedrock/controller/BrControllerExecutor.java"
        ));

        assertTrue(animationEntry.contains("ParticleLookup.definition(s)"));
        assertTrue(animationEntry.contains("ParticleSpawnService.spawnEmitter(uuid, definition,"));
        assertTrue(!animationEntry.contains("ParticleLookup.get("));
        assertTrue(controllerExecutor.contains("ParticleLookup.definition(effect)"));
        assertTrue(controllerExecutor.contains("ParticleSpawnService.spawnEmitter("));
        assertTrue(controllerExecutor.contains("definition,"));
        assertTrue(!controllerExecutor.contains("ParticleLookup.get("));
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

    private static String particleJson(String identifier) {
        return """
                {
                  "format_version": "1.10.0",
                  "particle_effect": {
                    "description": {
                      "identifier": "%s",
                      "basic_render_parameters": {
                        "material": "particles_alpha",
                        "texture": "textures/particle/particles"
                      }
                    }
                  }
                }
                """.formatted(identifier);
    }
}

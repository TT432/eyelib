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
        String adapter = Files.readString(Path.of(
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleSpawnRuntimeAdapter.java"
        ));

        assertTrue(source.contains("import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;"));
        assertTrue(source.contains("import io.github.tt432.eyelibparticle.client.ParticleSpawnRuntimeAdapter;"));
        assertTrue(source.contains("import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;"));
        assertTrue(source.contains("ParticleSpawnRuntimeAdapter ADAPTER"));
        assertTrue(source.contains("ParticleSpawnService::currentEnvironment"));
        assertTrue(adapter.contains("import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleRuntime;"));
        assertTrue(adapter.contains("new BedrockParticleRuntime("));
        assertTrue(adapter.contains("renderManager::spawnParticle"));
        assertTrue(adapter.contains("renderManager.spawnEmitter("));
        assertTrue(adapter.contains("renderManager.removeEmitter("));
        assertTrue(source.contains("api().spawn(new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position()))"));
        assertTrue(source.contains("api().remove(removeId);"));
        assertTrue(adapter.contains("definitions.get(request.particleId())"));
        assertTrue(source.contains("ParticleDefinition definition,"));
        assertTrue(!source.contains("BrParticle.CODEC.encodeStart"));
        assertTrue(!source.contains("import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;"));
        assertTrue(!source.contains("import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;"));
        assertTrue(!source.contains("publishLegacyParticle"));
    }

    @Test
    void animationParticleEffectsResolvePublishedModuleDefinitions() throws IOException {
        ParticleResourcePublication.replaceFromJsonResources(
                Map.of("particles/runtime.particle", JsonParser.parseString(particleJson("eyelib:runtime_particle"))),
                LoggerFactory.getLogger(ParticleRuntimeDelegationBoundaryTest.class)
        );

        assertNotNull(ParticleDefinitionRegistry.store().get("eyelib:runtime_particle"));
        assertNull(ParticleDefinitionRegistry.store().get("particles/runtime.particle"));

        String animationEntry = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntryDefinition.java"
        ));
        String controllerExecutor = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/animation/bedrock/controller/BrControllerExecutor.java"
        ));
        String command = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommand.java"
        ));

        assertTrue(animationEntry.contains("ParticleDefinitionRegistry.store().get(s)"));
        assertTrue(animationEntry.contains("ParticleSpawnService.spawnEmitter(uuid, definition,"));
        assertTrue(!animationEntry.contains("ParticleLookup.get("));
        assertTrue(controllerExecutor.contains("ParticleDefinitionRegistry.store().get(effect)"));
        assertTrue(controllerExecutor.contains("ParticleSpawnService.spawnEmitter("));
        assertTrue(controllerExecutor.contains("definition,"));
        assertTrue(!controllerExecutor.contains("ParticleLookup.get("));
        assertTrue(command.contains("ParticleDefinitionRegistry.store().names()"));
        assertTrue(!command.contains("ParticleLookup.names()"));
    }

    @Test
    void rootRenderManagerFacadeIsDeletedAndInstrumentationReadsModuleRenderManager() throws IOException {
        String observer = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/instrument/collector/BrParticleObserver.java"
        ));
        String hooks = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/instrument/InstrumentLifecycleHooks.java"
        ));

        assertTrue(Files.notExists(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleRenderManager.java")));
        assertTrue(observer.contains("import io.github.tt432.eyelibparticle.client.ParticleRenderManager;"));
        assertTrue(observer.contains("ParticleRenderManager.INSTANCE.getEmitterCount()"));
        assertTrue(!observer.contains("BrParticleRenderManager.get"));
        assertTrue(hooks.contains("import io.github.tt432.eyelibparticle.client.ParticleRenderManager;"));
        assertTrue(hooks.contains("ParticleRenderManager.INSTANCE.getEmitterCount()"));
        assertTrue(!hooks.contains("BrParticleRenderManager.get"));
    }

    @Test
    void legacyRootEmitterRuntimeAndSchemaTreeAreDeleted() throws IOException {
        assertTrue(Files.notExists(Path.of("src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java")));
        assertTrue(Files.notExists(Path.of("src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleEmitter.java")));
        assertTrue(Files.notExists(Path.of("src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleParticle.java")));
        assertTrue(noJavaSources(Path.of("src/main/java/io/github/tt432/eyelib/client/particle/bedrock/component")));
    }

    @Test
    void spawnAndRemovePacketShapesRemainStringKeyed() throws IOException {
        String spawnPacket = Files.readString(Path.of(
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/network/SpawnParticlePacket.java"
        ));
        String removePacket = Files.readString(Path.of(
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/network/RemoveParticlePacket.java"
        ));

        assertTrue(Pattern.compile("record\\s+SpawnParticlePacket\\s*\\(\\s*String\\s+spawnId,\\s*String\\s+particleId,\\s*Vector3f\\s+position", Pattern.DOTALL)
                .matcher(spawnPacket)
                .find());
        assertTrue(spawnPacket.contains("buf.writeUtf(packet.spawnId);"));
        assertTrue(spawnPacket.contains("buf.writeUtf(packet.particleId);"));
        assertTrue(spawnPacket.contains("buf.writeFloat(packet.position.x());"));

        assertTrue(Pattern.compile("record\\s+RemoveParticlePacket\\s*\\(\\s*String\\s+removeId", Pattern.DOTALL)
                .matcher(removePacket)
                .find());
        assertTrue(removePacket.contains("buf.writeUtf(packet.removeId);"));
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

    private static boolean noJavaSources(Path path) throws IOException {
        if (Files.notExists(path)) {
            return true;
        }

        try (var files = Files.walk(path)) {
            return files.noneMatch(source -> source.toString().endsWith(".java"));
        }
    }
}

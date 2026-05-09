package io.github.tt432.eyelib.client.particle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleFinalSplitBoundaryTest {
    @Test
    void rootFinalSplitDelegatesPacketHandlingThroughParticleSpawnServiceOnly() throws IOException {
        SourceCheck handlers = source("src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java");
        SourceCheck spawnService = source("src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java");

        assertAll(
                () -> handlers.assertContains("import io.github.tt432.eyelib.client.particle.ParticleSpawnService;"),
                () -> handlers.assertContains("ParticleSpawnService.removeEmitter(packet.removeId());"),
                () -> handlers.assertContains("ParticleSpawnService.spawnFromPacket(packet);"),
                () -> handlers.assertNotContains("ParticleRenderManager"),
                () -> handlers.assertNotContains("ParticleDefinitionRegistry"),
                () -> handlers.assertNotContains("BrParticleLoader"),
                () -> handlers.assertNotContains("BrParticleRenderManager"),
                () -> spawnService.assertContains("import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;"),
                () -> spawnService.assertContains("import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;"),
                () -> spawnService.assertContains("import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleRuntime;"),
                () -> spawnService.assertContains("ParticleRenderManager.INSTANCE.spawnEmitter("),
                () -> spawnService.assertContains("api().spawn(new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position()))"),
                () -> spawnService.assertContains("PHASE14_RED_GATE")
        );
    }

    @Test
    void packetAndCommandContractsStayInMcImplAdapters() throws IOException {
        SourceCheck command = source("src/main/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommand.java");
        SourceCheck spawnPacket = source("src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/SpawnParticlePacket.java");
        SourceCheck removePacket = source("src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/RemoveParticlePacket.java");

        assertAll(
                () -> command.assertContains("package io.github.tt432.eyelib.mc.impl.common.command;"),
                () -> command.assertContains("ResourceLocationArgument"),
                () -> command.assertContains("ParticleCommandRuntime.buildSpawnParticleRequest"),
                () -> command.assertContains("new SpawnParticlePacket("),
                () -> spawnPacket.assertContains("package io.github.tt432.eyelib.mc.impl.network.packet;"),
                () -> assertTrue(Pattern.compile("record\\s+SpawnParticlePacket\\s*\\(\\s*String\\s+spawnId,\\s*String\\s+particleId,\\s*Vector3f\\s+position", Pattern.DOTALL)
                        .matcher(spawnPacket.content()).find()),
                () -> spawnPacket.assertContains("EyelibStreamCodecs.STRING.encode(obj.spawnId, buf);"),
                () -> spawnPacket.assertContains("EyelibStreamCodecs.STRING.encode(obj.particleId, buf);"),
                () -> removePacket.assertContains("package io.github.tt432.eyelib.mc.impl.network.packet;"),
                () -> assertTrue(Pattern.compile("record\\s+RemoveParticlePacket\\s*\\(\\s*String\\s+removeId", Pattern.DOTALL)
                        .matcher(removePacket.content()).find()),
                () -> removePacket.assertContains("EyelibStreamCodecs.STRING.encode(obj.removeId, buf);"),
                () -> assertFalse(Files.exists(Path.of("src/main/java/io/github/tt432/eyelib/network/SpawnParticlePacket.java"))),
                () -> assertFalse(Files.exists(Path.of("src/main/java/io/github/tt432/eyelib/network/RemoveParticlePacket.java")))
        );
    }

    @Test
    void rootCompatibilityAdaptersRemainThinModuleDelegates() throws IOException {
        SourceCheck lookup = source("src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java");
        SourceCheck registry = source("src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java");
        SourceCheck manager = source("src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java");
        SourceCheck renderManager = source("src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleRenderManager.java");

        assertAll(
                () -> lookup.assertContains("import io.github.tt432.eyelibparticle.api.ParticleLookupApi;"),
                () -> lookup.assertContains("import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;"),
                () -> lookup.assertContains("ParticleDefinitionRegistry.store().names()"),
                () -> registry.assertContains("import io.github.tt432.eyelibparticle.api.ParticlePublisher;"),
                () -> registry.assertContains("ParticleDefinitionRegistry.publisher()"),
                () -> registry.assertContains("ParticleDefinitionAdapter::fromSchema"),
                () -> manager.assertContains("import io.github.tt432.eyelibparticle.api.ParticleStore;"),
                () -> manager.assertContains("Root legacy compatibility adapter"),
                () -> renderManager.assertContains("import io.github.tt432.eyelibparticle.client.ParticleRenderManager;"),
                () -> renderManager.assertContains("ParticleRenderManager.INSTANCE.spawnEmitter("),
                () -> renderManager.assertContains("throw new UnsupportedOperationException("),
                () -> renderManager.assertNotContains("BrParticleLoader")
        );
    }

    @Test
    void normalFinalGateTestsDoNotReadPlanningArtifacts() throws IOException {
        String source = Files.readString(Path.of(
                "src/test/java/io/github/tt432/eyelib/client/particle/ParticleFinalSplitBoundaryTest.java"));

        assertAll(
                () -> assertFalse(source.contains("." + "planning/")),
                () -> assertFalse(source.contains("VALIDATION" + ".md")),
                () -> assertFalse(source.contains("14-" + "RESEARCH.md")),
                () -> assertFalse(source.contains("14-FINAL-GATE" + "-EVIDENCE.md"))
        );
    }

    private static SourceCheck source(String path) throws IOException {
        return new SourceCheck(path, Files.readString(Path.of(path)));
    }

    private record SourceCheck(String path, String content) {
        void assertContains(String expected) {
            assertTrue(content.contains(expected), () -> path + " should contain: " + expected);
        }

        void assertNotContains(String unexpected) {
            assertFalse(content.contains(unexpected), () -> path + " should not contain: " + unexpected);
        }
    }
}

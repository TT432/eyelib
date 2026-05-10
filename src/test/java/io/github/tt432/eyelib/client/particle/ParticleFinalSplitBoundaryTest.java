package io.github.tt432.eyelib.client.particle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
                () -> spawnService.assertContains("import io.github.tt432.eyelibparticle.client.ParticleSpawnRuntimeAdapter;"),
                () -> spawnService.assertContains("import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;"),
                () -> spawnService.assertContains("ParticleSpawnRuntimeAdapter ADAPTER"),
                () -> spawnService.assertContains("api().spawn(new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position()))")
        );
    }

    @Test
    void packetContractsBelongToParticleModuleAndCommandStaysAsAdapter() throws IOException {
        SourceCheck command = source("src/main/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommand.java");
        SourceCheck spawnPacket = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/network/SpawnParticlePacket.java");
        SourceCheck removePacket = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/network/RemoveParticlePacket.java");

        assertAll(
                () -> command.assertContains("package io.github.tt432.eyelib.mc.impl.common.command;"),
                () -> command.assertContains("ResourceLocationArgument"),
                () -> command.assertContains("ParticleCommandRuntime.buildSpawnParticleRequest"),
                () -> command.assertContains("new SpawnParticlePacket("),
                () -> spawnPacket.assertContains("package io.github.tt432.eyelibparticle.network;"),
                () -> assertTrue(Pattern.compile("record\\s+SpawnParticlePacket\\s*\\(\\s*String\\s+spawnId,\\s*String\\s+particleId,\\s*Vector3f\\s+position", Pattern.DOTALL)
                        .matcher(spawnPacket.content()).find()),
                () -> spawnPacket.assertContains("buf.writeUtf(packet.spawnId);"),
                () -> spawnPacket.assertContains("buf.writeUtf(packet.particleId);"),
                () -> removePacket.assertContains("package io.github.tt432.eyelibparticle.network;"),
                () -> assertTrue(Pattern.compile("record\\s+RemoveParticlePacket\\s*\\(\\s*String\\s+removeId", Pattern.DOTALL)
                        .matcher(removePacket.content()).find()),
                () -> removePacket.assertContains("buf.writeUtf(packet.removeId);"),
                () -> assertFalse(Files.exists(Path.of("src/main/java/io/github/tt432/eyelib/network/SpawnParticlePacket.java"))),
                () -> assertFalse(Files.exists(Path.of("src/main/java/io/github/tt432/eyelib/network/RemoveParticlePacket.java"))),
                () -> assertFalse(Files.exists(Path.of("src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/SpawnParticlePacket.java"))),
                () -> assertFalse(Files.exists(Path.of("src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/RemoveParticlePacket.java")))
        );
    }

    @Test
    void rootLegacyCompatibilityAdaptersHaveBeenDeleted() {
        assertAll(
                () -> assertFalse(Files.exists(Path.of("src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java"))),
                () -> assertFalse(Files.exists(Path.of("src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java"))),
                () -> assertFalse(Files.exists(Path.of("src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java"))),
                () -> assertFalse(Files.exists(Path.of("src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleRenderManager.java")))
        );
    }

    @Test
    void productionCodeDoesNotSpawnThroughRootRenderManagerFacade() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            List<Path> directRootRenderSpawns = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> contains(path, "BrParticleRenderManager.spawn"))
                    .toList();

            assertTrue(directRootRenderSpawns.isEmpty(),
                    () -> "production particle spawns should use module render manager or ParticleSpawnService seams: "
                            + directRootRenderSpawns);
        }
    }

    @Test
    void rootLegacyComponentsAreDeletedAndModuleRuntimeComponentsRemain() throws IOException {
        Path rootComponentDir = Path.of("src/main/java/io/github/tt432/eyelib/client/particle/bedrock/component");
        Path moduleComponentDir = Path.of("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component");

        assertFalse(hasJavaSources(rootComponentDir));
        assertTrue(Files.exists(moduleComponentDir));

        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            List<Path> externalRootComponentReferences = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> contains(path, "io.github.tt432.eyelib.client.particle.bedrock.component"))
                    .toList();

            assertTrue(externalRootComponentReferences.isEmpty(),
                    () -> "root particle components should not be referenced after legacy deletion: "
                            + externalRootComponentReferences);
        }

        SourceCheck readme = source("src/main/java/io/github/tt432/eyelib/client/particle/README.md");
        readme.assertContains("Legacy root `bedrock/**`, `ParticleLookup`, `ParticleManager`, and `ParticleAssetRegistry` have been deleted");
        readme.assertContains("Executable runtime remains in `io.github.tt432.eyelibparticle.runtime.bedrock/**`");
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

    private static boolean contains(Path path, String expected) {
        try {
            return Files.readString(path).contains(expected);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean hasJavaSources(Path path) throws IOException {
        if (Files.notExists(path)) {
            return false;
        }

        try (Stream<Path> files = Files.walk(path)) {
            return files.anyMatch(source -> source.toString().endsWith(".java"));
        }
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

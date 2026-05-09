package io.github.tt432.eyelib.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleFinalDocumentationGateTest {
    @Test
    void finalDocsDescribeParticleSplitClosureWithoutPlanningDependencies() throws IOException {
        String docs = readDocs(
                "MODULES.md",
                "docs/index/repo-map.md",
                "docs/architecture/01-module-boundaries.md",
                "docs/architecture/02-side-boundaries.md",
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md",
                "src/main/java/io/github/tt432/eyelib/client/particle/README.md",
                "src/main/java/io/github/tt432/eyelib/network/README.md",
                "src/main/java/io/github/tt432/eyelib/mc/impl/common/command/README.md",
                "src/main/java/io/github/tt432/eyelib/mc/impl/network/README.md"
        );

        assertAll(
                () -> assertTrue(docs.contains("ParticleDefinitionRegistry")),
                () -> assertTrue(docs.contains("ParticleResourcePublication")),
                () -> assertTrue(docs.contains("ParticleDefinition.identifier()")),
                () -> assertTrue(docs.contains("ParticleDefinitionAdapter")),
                () -> assertTrue(docs.contains("io.github.tt432.eyelibimporter.particle.BrParticle")),
                () -> assertTrue(docs.contains("mc/impl/common/command")),
                () -> assertTrue(docs.contains("mc/impl/network/packet")),
                () -> assertTrue(docs.contains("SpawnParticlePacket(String spawnId, String particleId, Vector3f position)")),
                () -> assertTrue(docs.contains("RemoveParticlePacket(String removeId)")),
                () -> assertTrue(docs.contains("ClientSmoke")),
                () -> assertTrue(docs.contains("hardware")),
                () -> assertTrue(docs.contains("PFUT-02")),
                () -> assertTrue(docs.contains("PFUT-03")),
                () -> assertTrue(docs.contains("PHASE14_RED_GATE"))
        );
    }

    @Test
    void finalDocumentationGateSourceReadsStableDocsOnly() throws IOException {
        String source = Files.readString(Path.of(
                "src/test/java/io/github/tt432/eyelib/docs/ParticleFinalDocumentationGateTest.java"));

        assertAll(
                () -> assertFalse(source.contains("." + "planning/")),
                () -> assertFalse(source.contains("VALIDATION" + ".md")),
                () -> assertFalse(source.contains("14-" + "RESEARCH.md")),
                () -> assertFalse(source.contains("14-FINAL-GATE" + "-EVIDENCE.md"))
        );
    }

    private static String readDocs(String... paths) throws IOException {
        StringBuilder docs = new StringBuilder();
        for (String path : paths) {
            docs.append(Files.readString(Path.of(path))).append('\n');
        }
        return docs.toString();
    }
}

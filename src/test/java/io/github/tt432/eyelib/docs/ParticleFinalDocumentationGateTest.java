package io.github.tt432.eyelib.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleFinalDocumentationGateTest {
    private static final Map<String, List<String>> REQUIRED_STABLE_DOC_ANCHORS = Map.ofEntries(
            entry("MODULES.md", List.of(
                    ":eyelib-particle",
                    "ParticleDefinitionRegistry",
                    "ParticleResourcePublication",
                    "ParticleDefinition.identifier()",
                    "ParticleDefinitionAdapter",
                    "io.github.tt432.eyelibimporter.particle.BrParticle",
                    "mc/impl/common/command",
                    "io.github.tt432.eyelibparticle.network",
                    "ClientSmoke",
                    "hardware",
                    "PFUT-03")),
            entry("docs/decisions/0002-module-boundaries.md", List.of(
                    ":eyelib-particle",
                    "ParticleDefinitionRegistry",
                    "ParticleResourcePublication",
                    "ParticleDefinition.identifier()",
                    "ParticleDefinitionAdapter",
                    "io.github.tt432.eyelibimporter.particle.BrParticle",
                    "mc/impl/common/command",
                    "io.github.tt432.eyelibparticle.network",
                    "ClientSmoke",
                    "PFUT-03")),
            entry("docs/decisions/0003-side-boundaries.md", List.of(
                    ":eyelib-particle",
                    "ParticleDefinitionRegistry",
                    "ParticleResourcePublication",
                    "ParticleDefinition.identifier()",
                    "ParticleDefinitionAdapter",
                    "io.github.tt432.eyelibimporter.particle.BrParticle",
                    "io.github.tt432.eyelibparticle.network",
                    "ClientSmoke",
                    "hardware",
                    "PFUT-03")),
            entry("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/package-info.java", List.of(
                    ":eyelib-particle",
                    "ParticleDefinitionRegistry",
                    "ParticleResourcePublication",
                    "ParticleDefinition.identifier()",
                    "ParticleDefinitionAdapter",
                    "io.github.tt432.eyelibimporter.particle.BrParticle",
                    "io.github.tt432.eyelibparticle.network",
                    "ClientSmoke",
                    "PFUT-03")),
            entry("src/main/java/io/github/tt432/eyelib/client/particle/package-info.java", List.of(
                    ":eyelib-particle",
                    "ParticleDefinitionRegistry",
                    "ParticleResourcePublication",
                    "ParticleDefinition.identifier()",
                    "ParticleDefinitionAdapter",
                    "io.github.tt432.eyelibimporter.particle.BrParticle",
                    "mc/impl/common/command",
                    "io.github.tt432.eyelibparticle.network",
                    "ClientSmoke",
                    "PFUT-03")),
            entry("src/main/java/io/github/tt432/eyelib/network/package-info.java", List.of(
                    "shared channel",
                    "transport delegation",
                    "context-free handler dispatch")));

    @Test
    void finalDocsDescribeParticleSplitClosureWithoutPlanningDependencies() throws IOException {
        assertAll(REQUIRED_STABLE_DOC_ANCHORS.entrySet().stream()
                                             .map(entry -> () -> assertDocumentContains(entry.getKey(), entry.getValue())));
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

    private static void assertDocumentContains(String path, List<String> anchors) throws IOException {
        String doc = Files.readString(Path.of(path));

        assertAll(path, anchors.stream()
                               .map(anchor -> () -> assertTrue(doc.contains(anchor), path + " should contain: " + anchor)));
    }
}

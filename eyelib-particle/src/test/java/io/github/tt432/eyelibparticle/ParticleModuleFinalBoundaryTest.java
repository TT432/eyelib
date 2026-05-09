package io.github.tt432.eyelibparticle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleModuleFinalBoundaryTest {
    @Test
    void pureParticleModulePackagesRejectRootMinecraftAndForgeImports() throws IOException {
        List<String> forbiddenImports = List.of(
                "io.github.tt432.eyelib.client.",
                "io.github.tt432.eyelib.network.",
                "io.github.tt432.eyelib.capability.",
                "io.github.tt432.eyelib.mc.impl.",
                "net.minecraft.",
                "net.minecraftforge."
        );

        try (var paths = Files.walk(projectRoot().resolve("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle"))) {
            List<Path> violatingFiles = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("/client/"))
                    .filter(path -> !path.toString().contains("\\client\\"))
                    .filter(path -> importsIn(path).stream().anyMatch(importLine -> forbiddenImports.stream().anyMatch(importLine::startsWith)))
                    .map(projectRoot()::relativize)
                    .toList();

            assertTrue(violatingFiles.isEmpty(), () -> "Forbidden pure particle imports: " + violatingFiles);
        }
    }

    @Test
    void particleClientIntegrationIsDocumentedAndSideGated() throws IOException {
        SourceCheck moduleReadme = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md");
        SourceCheck sideBoundaries = source("docs/architecture/02-side-boundaries.md");
        SourceCheck renderHooks = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleRenderHooks.java");

        assertAll(
                () -> moduleReadme.assertContains("client integration"),
                () -> moduleReadme.assertContains("Dist.CLIENT"),
                () -> moduleReadme.assertContains("render manager"),
                () -> sideBoundaries.assertContains("Dist.CLIENT"),
                () -> sideBoundaries.assertContains("client adapters"),
                () -> renderHooks.assertContains("import net.minecraftforge.api.distmarker.Dist;"),
                () -> renderHooks.assertContains("@Mod.EventBusSubscriber(value = Dist.CLIENT"),
                () -> renderHooks.assertContains("ParticleRenderManager.INSTANCE")
        );
    }

    @Test
    void finalGateCoversSchemaRuntimeConversionAndLoadingKeys() throws IOException {
        SourceCheck adapterTest = source("eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapterTest.java");
        SourceCheck publicationTest = source("eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublicationTest.java");

        assertAll(
                () -> adapterTest.assertContains("BrParticle.CODEC.parse"),
                () -> adapterTest.assertContains("ParticleDefinitionAdapter.fromSchema"),
                () -> adapterTest.assertContains("definition.identifier()"),
                () -> adapterTest.assertContains("assertSame(schema.particleEffect().events(), definition.events())"),
                () -> publicationTest.assertContains("sourceKeyIsNotPublishedAsActiveIdentifier"),
                () -> publicationTest.assertContains("fullReplacementRemovesStaleEntries"),
                () -> publicationTest.assertContains("ParticleDefinitionRegistry.store().get(\"eyelib:source_first\")"),
                () -> publicationTest.assertContains("ParticleDefinitionRegistry.store().all().keySet()")
        );
    }

    @Test
    void normalModuleFinalGateTestsDoNotReadPlanningArtifacts() throws IOException {
        String source = Files.readString(projectRoot().resolve(
                "eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/ParticleModuleFinalBoundaryTest.java"));

        assertAll(
                () -> assertFalse(source.contains("." + "planning/")),
                () -> assertFalse(source.contains("VALIDATION" + ".md")),
                () -> assertFalse(source.contains("14-" + "RESEARCH.md")),
                () -> assertFalse(source.contains("14-FINAL-GATE" + "-EVIDENCE.md"))
        );
    }

    private static List<String> importsIn(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("import "))
                    .map(line -> line.substring("import ".length(), line.length() - 1))
                    .toList();
        } catch (IOException exception) {
            throw new AssertionError("Unable to scan imports in " + path, exception);
        }
    }

    private static SourceCheck source(String path) throws IOException {
        return new SourceCheck(path, Files.readString(projectRoot().resolve(path)));
    }

    private static Path projectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("MODULES.md"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate project root from user.dir");
    }

    private record SourceCheck(String path, String content) {
        void assertContains(String expected) {
            assertTrue(content.contains(expected), () -> path + " should contain: " + expected);
        }
    }
}

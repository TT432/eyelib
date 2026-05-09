package io.github.tt432.eyelibparticle.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleDefinitionBoundaryTest {
    @Test
    void particleModuleMainSourcesDoNotDeclareDuplicateBrParticle() throws IOException {
        List<String> duplicateDeclarations = List.of("record BrParticle", "class BrParticle");

        try (var paths = Files.walk(projectRoot().resolve("eyelib-particle/src/main/java"))) {
            List<Path> violatingFiles = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path, duplicateDeclarations))
                    .map(projectRoot()::relativize)
                    .toList();

            assertTrue(violatingFiles.isEmpty(), () -> "Duplicate particle-module BrParticle declarations: " + violatingFiles);
        }
    }

    @Test
    void particleModuleMainSourcesRemainFreeOfRootMinecraftAndForgeImports() throws IOException {
        List<String> forbiddenFragments = List.of(
                "import io.github.tt432.eyelib.client.",
                "import io.github.tt432.eyelib.network.",
                "import io.github.tt432.eyelib.capability.",
                "import io.github.tt432.eyelib.mc.impl.",
                "import net.minecraft.",
                "import net.minecraftforge."
        );

        try (var paths = Files.walk(projectRoot().resolve("eyelib-particle/src/main/java"))) {
            List<Path> violatingFiles = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path, forbiddenFragments))
                    .map(projectRoot()::relativize)
                    .toList();

            assertTrue(violatingFiles.isEmpty(), () -> "Forbidden particle module imports: " + violatingFiles);
        }
    }

    @Test
    void adapterSeamAllowsImporterSchemaDependencyWithoutRootRuntimeDependency() throws IOException {
        SourceCheck definition = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinition.java");
        SourceCheck adapter = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapter.java");

        definition.assertContains("import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;");
        definition.assertContains("import io.github.tt432.eyelibimporter.particle.BrParticle;");
        adapter.assertContains("import io.github.tt432.eyelibimporter.particle.BrParticle;");

        definition.assertNotContains("import io.github.tt432.eyelib.client.");
        adapter.assertNotContains("import io.github.tt432.eyelib.client.");
        definition.assertNotContains("import net.minecraft.");
        adapter.assertNotContains("import net.minecraft.");
        definition.assertNotContains("import net.minecraftforge.");
        adapter.assertNotContains("import net.minecraftforge.");
    }

    private static boolean containsAny(Path path, List<String> fragments) {
        try {
            String source = Files.readString(path);
            return fragments.stream().anyMatch(source::contains);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static SourceCheck source(String path) throws IOException {
        Path root = projectRoot();
        Path resolved = root.resolve(path);
        return new SourceCheck(path, Files.readString(resolved));
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

        void assertNotContains(String unexpected) {
            assertFalse(content.contains(unexpected), () -> path + " should not contain: " + unexpected);
        }
    }
}

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
                    .filter(path -> !path.toString().contains("/network/"))
                    .filter(path -> !path.toString().contains("\\network\\"))
                    .filter(path -> hasForbiddenPureParticleReference(path, forbiddenImports))
                    .map(projectRoot()::relativize)
                    .toList();

            assertTrue(violatingFiles.isEmpty(), () -> "Forbidden pure particle references: " + violatingFiles);
        }
    }

    @Test
    void particleClientIntegrationIsDocumentedAndSideGated() throws IOException {
        SourceCheck moduleReadme = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md");
        SourceCheck sideBoundaries = source("docs/architecture/02-side-boundaries.md");
        SourceCheck renderHooks = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleRenderHooks.java");

        assertAll(
                () -> moduleReadme.assertContains("client integration"),
                () -> moduleReadme.assertContains("particle-owned packet codecs"),
                () -> sideBoundaries.assertContains("io.github.tt432.eyelibparticle.network"),
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
                        .map(line -> line.startsWith("static ") ? line.substring("static ".length()) : line)
                        .toList();
        } catch (IOException exception) {
            throw new AssertionError("Unable to scan imports in " + path, exception);
        }
    }

    private static boolean hasForbiddenPureParticleReference(Path path, List<String> forbiddenPrefixes) {
        return importsIn(path).stream()
                              .anyMatch(importLine -> forbiddenPrefixes.stream().anyMatch(importLine::startsWith))
                || forbiddenPrefixes.stream()
                                    .anyMatch(forbiddenPrefix -> strippedSourceIn(path).contains(forbiddenPrefix));
    }

    private static String strippedSourceIn(Path path) {
        try {
            return stripCommentsAndStrings(Files.readString(path));
        } catch (IOException exception) {
            throw new AssertionError("Unable to scan source in " + path, exception);
        }
    }

    private static String stripCommentsAndStrings(String source) {
        StringBuilder stripped = new StringBuilder(source.length());

        for (int index = 0; index < source.length(); index++) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';

            if (current == '/' && next == '/') {
                index = appendSpacesUntilLineEnd(source, stripped, index);
            } else if (current == '/' && next == '*') {
                index = appendSpacesUntilBlockCommentEnd(source, stripped, index);
            } else if (source.startsWith("\"\"\"", index)) {
                index = appendSpacesUntilTextBlockEnd(source, stripped, index);
            } else if (current == '"' || current == '\'') {
                index = appendSpacesUntilLiteralEnd(source, stripped, index, current);
            } else {
                stripped.append(current);
            }
        }

        return stripped.toString();
    }

    private static int appendSpacesUntilLineEnd(String source, StringBuilder stripped, int index) {
        while (index < source.length() && source.charAt(index) != '\n') {
            stripped.append(' ');
            index++;
        }
        if (index < source.length()) {
            stripped.append(source.charAt(index));
        }
        return index;
    }

    private static int appendSpacesUntilBlockCommentEnd(String source, StringBuilder stripped, int index) {
        stripped.append("  ");
        index += 2;
        while (index < source.length()) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
            if (current == '*' && next == '/') {
                stripped.append("  ");
                return index + 1;
            }
            stripped.append(current == '\n' ? '\n' : ' ');
            index++;
        }
        return source.length() - 1;
    }

    private static int appendSpacesUntilTextBlockEnd(String source, StringBuilder stripped, int index) {
        stripped.append("   ");
        index += 3;
        while (index < source.length()) {
            if (source.startsWith("\"\"\"", index)) {
                stripped.append("   ");
                return index + 2;
            }
            stripped.append(source.charAt(index) == '\n' ? '\n' : ' ');
            index++;
        }
        return source.length() - 1;
    }

    private static int appendSpacesUntilLiteralEnd(String source, StringBuilder stripped, int index, char delimiter) {
        stripped.append(' ');
        index++;
        boolean escaped = false;
        while (index < source.length()) {
            char current = source.charAt(index);
            stripped.append(current == '\n' ? '\n' : ' ');
            if (!escaped && current == delimiter) {
                return index;
            }
            escaped = !escaped && current == '\\';
            if (current != '\\') {
                escaped = false;
            }
            index++;
        }
        return source.length() - 1;
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

    private record SourceCheck(
            String path,
            String content
    ) {
        void assertContains(String expected) {
            assertTrue(content.contains(expected), () -> path + " should contain: " + expected);
        }
    }
}

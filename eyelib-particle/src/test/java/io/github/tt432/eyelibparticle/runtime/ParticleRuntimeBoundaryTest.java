package io.github.tt432.eyelibparticle.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ParticleRuntimeBoundaryTest {
    @Test
    void runtimeSourcesRejectRootMinecraftAndForgeReferencesAfterStrippingCommentsAndStrings() throws IOException {
        List<String> forbiddenFragments = List.of(
                "io.github.tt432.eyelib.client.",
                "io.github.tt432.eyelib.network.",
                "io.github.tt432.eyelib.capability.",
                "io.github.tt432.eyelib.mc.impl.",
                "net.minecraft.",
                "net.minecraftforge."
        );

        try (var paths = Files.walk(projectRoot().resolve("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime"))) {
            List<Path> violatingFiles = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path, forbiddenFragments))
                    .map(projectRoot()::relativize)
                    .toList();

            assertTrue(violatingFiles.isEmpty(), () -> "Forbidden pure runtime references: " + violatingFiles);
        }
    }

    @Test
    void particleModuleSourcesDoNotDeclareDuplicateRuntimeBrParticleOwner() throws IOException {
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
    void runtimePackageDocumentationNamesPureRuntimeAndClientIntegrationBoundary() throws IOException {
        String packageInfo = Files.readString(projectRoot().resolve(
                "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java"));

        assertTrue(packageInfo.contains("pure runtime"), "package-info should document pure runtime ownership");
        assertTrue(packageInfo.contains("client integration"), "package-info should document client integration boundary");
    }

    private static boolean containsAny(Path path, List<String> fragments) {
        try {
            String source = stripCommentsAndStringLiterals(Files.readString(path));
            return fragments.stream().anyMatch(source::contains);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String stripCommentsAndStringLiterals(String source) {
        StringBuilder result = new StringBuilder(source.length());
        boolean lineComment = false;
        boolean blockComment = false;
        boolean stringLiteral = false;
        boolean charLiteral = false;
        boolean textBlock = false;

        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            char nextNext = i + 2 < source.length() ? source.charAt(i + 2) : '\0';

            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                    result.append(current);
                } else {
                    result.append(' ');
                }
                continue;
            }

            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    result.append("  ");
                    i++;
                } else {
                    result.append(current == '\n' ? '\n' : ' ');
                }
                continue;
            }

            if (textBlock) {
                if (current == '"' && next == '"' && nextNext == '"') {
                    textBlock = false;
                    result.append("   ");
                    i += 2;
                } else {
                    result.append(current == '\n' ? '\n' : ' ');
                }
                continue;
            }

            if (stringLiteral) {
                if (current == '\\' && next != '\0') {
                    result.append("  ");
                    i++;
                } else if (current == '"') {
                    stringLiteral = false;
                    result.append(' ');
                } else {
                    result.append(current == '\n' ? '\n' : ' ');
                }
                continue;
            }

            if (charLiteral) {
                if (current == '\\' && next != '\0') {
                    result.append("  ");
                    i++;
                } else if (current == '\'') {
                    charLiteral = false;
                    result.append(' ');
                } else {
                    result.append(current == '\n' ? '\n' : ' ');
                }
                continue;
            }

            if (current == '/' && next == '/') {
                lineComment = true;
                result.append("  ");
                i++;
            } else if (current == '/' && next == '*') {
                blockComment = true;
                result.append("  ");
                i++;
            } else if (current == '"' && next == '"' && nextNext == '"') {
                textBlock = true;
                result.append("   ");
                i += 2;
            } else if (current == '"') {
                stringLiteral = true;
                result.append(' ');
            } else if (current == '\'') {
                charLiteral = true;
                result.append(' ');
            } else {
                result.append(current);
            }
        }

        return result.toString();
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
}
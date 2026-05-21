package io.github.tt432.eyelibparticle.loading;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ParticleLoadingBoundaryTest {
    private static final Path LOADING_SOURCE_ROOT = projectRoot().resolve(
            "eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading");
    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "io.github.tt432.eyelib.client",
            "io.github.tt432.eyelib.network",
            "io.github.tt432.eyelib.capability",
            "io.github.tt432.eyelib.mc.impl",
            "net.minecraft",
            "net.minecraftforge"
    );

    @Test
    void loadingPackageRemainsRootMinecraftAndForgeClean() throws IOException {
        assertTrue(Files.isDirectory(LOADING_SOURCE_ROOT), "loading package must exist");

        try (var files = Files.walk(LOADING_SOURCE_ROOT)) {
            List<String> forbiddenImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> importsIn(path).stream())
                    .filter(importLine -> FORBIDDEN_IMPORT_PREFIXES.stream().anyMatch(importLine::startsWith))
                    .toList();

            assertTrue(forbiddenImports.isEmpty(), () -> "forbidden loading imports: " + forbiddenImports);
        }
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
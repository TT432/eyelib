package io.github.tt432.eyelib.importer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** @author TT432 */
class ImporterModuleIdentityTest {
    private static final Path PROJECT_ROOT = projectRoot();
    private static final Path MODULE_ROOT = PROJECT_ROOT.resolve("eyelib-importer");
    private static final Path SOURCE_ROOT = MODULE_ROOT.resolve("src/main/java/io/github/tt432/eyelibimporter");

    @Test
    void buildAndBootstrapKeepForgeModShapeExplicit() throws IOException {
        String build = Files.readString(MODULE_ROOT.resolve("build.gradle"));
        String bootstrap = Files.readString(SOURCE_ROOT.resolve("EyelibResourcesImporterMod.java"));

        assertAll(
                () -> assertTrue(build.contains("id 'net.neoforged.moddev.legacyforge'"),
                        "build.gradle must apply legacyforge plugin"),
                () -> assertTrue(build.contains("eyelibimporter"),
                        "build.gradle must reference eyelibimporter"),
                () -> assertTrue(bootstrap.contains("MOD_ID = \"eyelibimporter\""),
                        "Mod bootstrap must declare eyelibimporter MOD_ID")
        );
    }

    @Test
    void importerMainSourcesDoNotDependOnRootRuntimePackages() throws IOException {
        assertAll(javaSources().map(path -> () -> {
            String source = Files.readString(path);
            assertFalse(source.contains("import io.github.tt432.eyelib."), path + " must not import root runtime packages");
        }));
    }

    @Test
    void directMinecraftAndForgeImportsStayInRootPackage() throws IOException {
        assertAll(javaSources().map(path -> () -> {
            String source = Files.readString(path);
            if (source.contains("import net.minecraft") || source.contains("import net.minecraftforge")) {
                String normalized = SOURCE_ROOT.relativize(path).toString().replace('\\', '/');
                assertTrue(normalized.equals("EyelibResourcesImporterMod.java") || normalized.startsWith("mc/"),
                        normalized + " has direct Minecraft/Forge imports outside expected scope");
            }
        }));
    }

    private static Stream<Path> javaSources() throws IOException {
        List<Path> sources;
        try (Stream<Path> stream = Files.walk(SOURCE_ROOT)) {
            sources = stream.filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
        return sources.stream();
    }

    private static Path projectRoot() {
        Path modulePath = Path.of("eyelib-importer");
        if (Files.exists(modulePath)) {
            return Path.of("");
        }
        return Path.of("..");
    }
}

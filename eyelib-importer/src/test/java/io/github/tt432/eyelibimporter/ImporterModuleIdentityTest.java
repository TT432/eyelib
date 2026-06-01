package io.github.tt432.eyelibimporter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ImporterModuleIdentityTest {
    private static final Path PROJECT_ROOT = projectRoot();
    private static final Path MODULE_ROOT = PROJECT_ROOT.resolve("eyelib-importer");
    private static final Path SOURCE_ROOT = MODULE_ROOT.resolve("src/main/java/io/github/tt432/eyelibimporter");

    @Test
    void moduleDocsDeclareImporterSchemaForgeFunctionalIdentity() throws IOException {
        String readme = Files.readString(SOURCE_ROOT.resolve("package-info.java"));
        String modules = Files.readString(PROJECT_ROOT.resolve("MODULES.md"));
        String sideBoundaries = Files.readString(PROJECT_ROOT.resolve("docs/decisions/0003-side-boundaries.md"));

        assertAll(
                () -> assertTrue(readme.contains("导入器/数据格式模块")),
                () -> assertTrue(readme.contains("Bedrock 资源导入")),
                () -> assertTrue(readme.contains("数据格式编解码")),
                () -> assertTrue(modules.contains("Importer/schema Forge functional module")),
                () -> assertTrue(sideBoundaries.contains("currently an importer/schema Forge functional module"))
        );
    }

    @Test
    void buildAndBootstrapKeepForgeModShapeExplicit() throws IOException {
        String build = Files.readString(MODULE_ROOT.resolve("build.gradle"));
        String modsToml = Files.readString(MODULE_ROOT.resolve("src/main/resources/META-INF/mods.toml"));
        String bootstrap = Files.readString(SOURCE_ROOT.resolve("EyelibResourcesImporterMod.java"));

        assertAll(
                () -> assertTrue(build.contains("id 'net.neoforged.moddev.legacyforge'")),
                () -> assertTrue(build.contains("eyelibimporter")),
                () -> assertTrue(modsToml.contains("modId=\"eyelibimporter\"")),
                () -> assertTrue(bootstrap.contains("@Mod(EyelibResourcesImporterMod.MOD_ID)")),
                () -> assertTrue(bootstrap.contains("MOD_ID = \"eyelibimporter\""))
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
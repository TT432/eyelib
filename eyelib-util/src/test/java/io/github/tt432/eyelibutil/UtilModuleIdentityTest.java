package io.github.tt432.eyelibutil;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilModuleIdentityTest {
    private static final Pattern PROJECT_DEPENDENCY_CALL = Pattern.compile("\\bproject\\s*\\(");
    private static final Path PROJECT_ROOT = projectRoot();
    private static final Path MODULE_ROOT = moduleRoot();

    @Test
    void buildScriptDeclaresLeafForgeUtilModule() throws IOException {
        String build = Files.readString(PROJECT_ROOT.resolve("build.gradle"));

        assertAll(
                () -> assertTrue(build.contains("net.neoforged.moddev.legacyforge")),
                () -> assertTrue(build.contains("eyelibutil")),
                () -> assertTrue(build.contains("sourceSet(sourceSets.main)")),
                () -> assertFalse(PROJECT_DEPENDENCY_CALL.matcher(build).find())
        );
    }

    @Test
    void forgeIdentityUsesSingleUtilModId() throws IOException {
        String modsToml = Files.readString(PROJECT_ROOT.resolve("src/main/resources/META-INF/mods.toml"));
        String bootstrap = Files.readString(MODULE_ROOT.resolve("EyelibUtilMod.java"));

        assertAll(
                () -> assertTrue(modsToml.contains("modId=\"eyelibutil\"")),
                () -> assertTrue(bootstrap.contains("public static final String MOD_ID = \"eyelibutil\"")),
                () -> assertTrue(bootstrap.contains("@Mod(EyelibUtilMod.MOD_ID)"))
        );
    }

    @Test
    void packageBoundaryAvoidsRootSplitPackageAndRecordsActiveScope() throws IOException {
        String packageInfo = Files.readString(MODULE_ROOT.resolve("package-info.java"));

        assertAll(
                () -> assertTrue(packageInfo.contains("package io.github.tt432.eyelibutil;")),
                () -> assertFalse(packageInfo.contains("package io.github.tt432.eyelib.util")),
                () -> assertTrue(packageInfo.contains("must not import root or")),
                () -> assertTrue(packageInfo.contains("resource")),
                () -> assertTrue(packageInfo.contains("texture")),
                () -> assertTrue(packageInfo.contains("codec")),
                () -> assertTrue(packageInfo.contains("streamcodec"))
        );
    }

    private static Path projectRoot() {
        Path rootRelative = Path.of("eyelib-util");
        if (Files.exists(rootRelative.resolve("build.gradle"))) {
            return rootRelative;
        }
        return Path.of(".");
    }

    private static Path moduleRoot() {
        Path rootRelative = Path.of("eyelib-util/src/main/java/io/github/tt432/eyelibutil");
        if (Files.exists(rootRelative)) {
            return rootRelative;
        }
        return Path.of("src/main/java/io/github/tt432/eyelibutil");
    }
}

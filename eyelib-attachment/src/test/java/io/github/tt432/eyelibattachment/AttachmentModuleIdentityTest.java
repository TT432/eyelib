package io.github.tt432.eyelibattachment;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentModuleIdentityTest {
    private static final Path MODULE_ROOT = moduleRoot();

    @Test
    void moduleDocsDeclareMinecraftFunctionalIdentityAndUtilStreamCodecScope() throws IOException {
        String readme = Files.readString(MODULE_ROOT.resolve("README.md"));

        assertAll(
                () -> assertTrue(readme.contains("Minecraft/Forge functional module")),
                () -> assertTrue(readme.contains("FriendlyByteBuf")),
                () -> assertTrue(readme.contains(":eyelib-util")),
                () -> assertTrue(readme.contains("network/")),
                () -> assertTrue(readme.contains("Shared"))
        );
    }

    @Test
    void attachmentModuleDoesNotDependOnRootRuntimePackages() throws IOException {
        assertAll(javaSources().map(path -> () -> {
            String source = Files.readString(path);
            assertFalse(source.contains("import io.github.tt432.eyelib."), path + " must not import root runtime packages");
        }));
    }

    @Test
    void directMinecraftAndForgeImportsStayInAttachmentFacingPackages() throws IOException {
        assertAll(javaSources().map(path -> () -> {
            String source = Files.readString(path);
            if (source.contains("import net.minecraft") || source.contains("import net.minecraftforge")) {
                String normalized = MODULE_ROOT.relativize(path).toString().replace('\\', '/');
                assertTrue(isAllowedMinecraftFacingPath(normalized), normalized + " has direct Minecraft/Forge imports");
            }
        }));
    }

    private static Stream<Path> javaSources() throws IOException {
        List<Path> sources;
        try (Stream<Path> stream = Files.walk(MODULE_ROOT)) {
            sources = stream.filter(path -> path.toString().endsWith(".java"))
                            .toList();
        }
        return sources.stream();
    }

    private static boolean isAllowedMinecraftFacingPath(String path) {
        return path.startsWith("network/")
                || path.startsWith("bootstrap/");
    }

    private static Path moduleRoot() {
        Path rootRelative = Path.of("eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment");
        if (Files.exists(rootRelative)) {
            return rootRelative;
        }
        return Path.of("src/main/java/io/github/tt432/eyelibattachment");
    }
}

package io.github.tt432.eyelibattachment;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class AttachmentModuleIdentityTest {
    private static final Path MODULE_ROOT = moduleRoot();

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
        return !path.contains("/")
                || path.startsWith("network/")
                || path.startsWith("capability/")
                || path.startsWith("bootstrap/")
                || path.startsWith("runtime/")
                || path.startsWith("dataattach/mc/")
                || path.startsWith("mixin/")
                || path.startsWith("sync/");
    }

    private static Path moduleRoot() {
        Path rootRelative = Path.of("eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment");
        if (Files.exists(rootRelative)) {
            return rootRelative;
        }
        return Path.of("src/main/java/io/github/tt432/eyelibattachment");
    }
}
package io.github.tt432.eyelib.client.particle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleSpawnServiceBoundaryTest {
    @Test
    void spawnServiceDelegatesPacketBoundaryThroughModuleSpawnApi() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java"
        ));

        assertTrue(source.contains("import io.github.tt432.eyelibparticle.api.ParticleSpawnApi;"));
        assertTrue(source.contains("import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;"));
        assertTrue(source.contains("ParticleSpawnApi api()"));
        assertTrue(source.contains("api().spawn(new ParticleSpawnRequest("));
        assertTrue(source.contains("api().remove(removeId);"));
        assertTrue(source.contains("Minecraft.getInstance()"));
        assertTrue(source.contains("BrParticleRenderManager"));
    }

    @Test
    void particleModuleMainSourcesStayFreeOfRootRuntimeImports() throws IOException {
        Path sourceRoot = Path.of("eyelib-particle/src/main/java");
        List<String> forbiddenFragments = List.of(
                "import io.github.tt432.eyelib.client.",
                "import io.github.tt432.eyelib.network.",
                "import io.github.tt432.eyelib.capability.",
                "import io.github.tt432.eyelib.mc.impl.",
                "import net.minecraft.",
                "import net.minecraftforge."
        );

        try (var paths = Files.walk(sourceRoot)) {
            List<Path> violatingFiles = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAnyForbiddenImport(path, forbiddenFragments))
                    .toList();

            assertTrue(violatingFiles.isEmpty(), () -> "Forbidden particle module imports: " + violatingFiles);
        }
    }

    private static boolean containsAnyForbiddenImport(Path path, List<String> forbiddenFragments) {
        try {
            String source = Files.readString(path);
            return forbiddenFragments.stream().anyMatch(source::contains);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

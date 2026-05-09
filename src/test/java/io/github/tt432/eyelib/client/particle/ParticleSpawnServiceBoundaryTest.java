package io.github.tt432.eyelib.client.particle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
        try (var paths = Files.walk(sourceRoot)) {
            String forbiddenImports = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .filter(source -> source.contains("import io.github.tt432.eyelib.client.")
                            || source.contains("import io.github.tt432.eyelib.network.")
                            || source.contains("import io.github.tt432.eyelib.capability.")
                            || source.contains("import io.github.tt432.eyelib.mc.impl."))
                    .findFirst()
                    .orElse("");

            assertFalse(forbiddenImports.contains("import io.github.tt432.eyelib."));
        }
    }
}

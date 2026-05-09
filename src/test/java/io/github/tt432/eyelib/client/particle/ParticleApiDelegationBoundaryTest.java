package io.github.tt432.eyelib.client.particle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleApiDelegationBoundaryTest {
    @Test
    void retainedRootFacadesDelegateToParticleModuleApiAndDocumentTransition() throws IOException {
        SourceCheck lookup = source("src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java");
        SourceCheck spawnService = source("src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java");
        SourceCheck registry = source("src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java");
        SourceCheck particleReadme = source("src/main/java/io/github/tt432/eyelib/client/particle/README.md");
        SourceCheck registryReadme = source("src/main/java/io/github/tt432/eyelib/client/registry/README.md");

        lookup.assertContains("import io.github.tt432.eyelibparticle.api.ParticleLookupApi;");
        lookup.assertContains("ParticleLookupApi<BrParticle> api()");
        lookup.assertContains("api().get(id.toString())");
        lookup.assertContains("Transitional");
        lookup.assertContains("Remove this facade after root callers");

        spawnService.assertContains("import io.github.tt432.eyelibparticle.api.ParticleSpawnApi;");
        spawnService.assertContains("import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;");
        spawnService.assertContains("ParticleSpawnApi api()");
        spawnService.assertContains("api().spawn(new ParticleSpawnRequest(");
        spawnService.assertContains("api().remove(removeId);");
        spawnService.assertContains("Transitional");
        spawnService.assertContains("Remove this facade after packet/runtime callers");

        registry.assertContains("import io.github.tt432.eyelibparticle.api.ParticlePublisher;");
        registry.assertContains("ParticlePublisher<BrParticle> publisher()");
        registry.assertContains("publisher().replaceParticles(particles.values())");
        registry.assertContains("particle.particleEffect().description().identifier()");
        registry.assertContains("Transitional");
        registry.assertContains("Remove this facade after root");

        particleReadme.assertContains("transitional root runtime adapter");
        particleReadme.assertContains("removal condition");
        registryReadme.assertContains("transitional root facade");
        registryReadme.assertContains("should be removed after root loaders/tooling migrate");
    }

    @Test
    void particleModuleMainSourcesRemainFreeOfRootMinecraftAndForgeImports() throws IOException {
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

    private static SourceCheck source(String path) throws IOException {
        return new SourceCheck(path, Files.readString(Path.of(path)));
    }

    private record SourceCheck(String path, String content) {
        void assertContains(String expected) {
            assertTrue(content.contains(expected), () -> path + " should contain: " + expected);
        }

        void assertNotContains(String unexpected) {
            assertFalse(content.contains(unexpected), () -> path + " should not contain: " + unexpected);
        }
    }
}

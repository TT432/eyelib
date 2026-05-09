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
        SourceCheck loader = source("src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java");
        SourceCheck particleReadme = source("src/main/java/io/github/tt432/eyelib/client/particle/README.md");
        SourceCheck registryReadme = source("src/main/java/io/github/tt432/eyelib/client/registry/README.md");
        Path obsoleteRootRequest = Path.of("src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnRequest.java");

        lookup.assertContains("import io.github.tt432.eyelibparticle.api.ParticleLookupApi;");
        lookup.assertContains("import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;");
        lookup.assertContains("ParticleLookupApi<BrParticle> api()");
        lookup.assertContains("ParticleDefinitionRegistry.store().names()");
        lookup.assertContains("compatibility");
        lookup.assertContains("Transitional");
        lookup.assertContains("Remove this facade after root callers");

        spawnService.assertContains("import io.github.tt432.eyelibparticle.api.ParticleSpawnApi;");
        spawnService.assertContains("import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;");
        spawnService.assertContains("import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;");
        spawnService.assertContains("import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;");
        spawnService.assertContains("ParticleSpawnApi api()");
        spawnService.assertContains("api().spawn(new ParticleSpawnRequest(");
        spawnService.assertContains("api().remove(removeId);");
        spawnService.assertNotContains("BrParticle.CODEC.encodeStart");
        spawnService.assertContains("Transitional");
        spawnService.assertContains("Remove this facade after packet/runtime callers");

        registry.assertContains("import io.github.tt432.eyelibparticle.api.ParticlePublisher;");
        registry.assertContains("import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;");
        registry.assertContains("import io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapter;");
        registry.assertContains("ParticlePublisher<ParticleDefinition> publisher()");
        registry.assertContains("ParticleDefinitionRegistry.publisher()");
        registry.assertContains("ParticleDefinitionRegistry.publisher().replaceParticles");
        registry.assertContains("Transitional");
        registry.assertContains("Remove this facade after root");

        loader.assertContains("ParticleResourcePublication.replaceFromJsonResources");
        loader.assertContains("entry.getKey().toString()");

        particleReadme.assertContains("transitional root runtime adapter");
        particleReadme.assertContains("removal condition");
        particleReadme.assertContains("do not add a duplicate root request type");
        assertTrue(Files.notExists(obsoleteRootRequest), () -> obsoleteRootRequest + " should not be reintroduced");
        registryReadme.assertContains("transitional root facade");
        registryReadme.assertContains("should be removed after root loaders/tooling migrate");
    }

    @Test
    void particleModuleMainSourcesRemainFreeOfRootMinecraftAndForgeImports() throws IOException {
        Path sourceRoot = Path.of("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle");
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
                    .filter(path -> !path.toString().contains("\\client\\"))
                    .filter(path -> !path.toString().contains("/client/"))
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

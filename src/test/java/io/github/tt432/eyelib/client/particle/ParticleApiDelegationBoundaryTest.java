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
    void retainedRootSpawnFacadeDelegatesToParticleModuleApiAndLegacyRegistryFacadesAreDeleted() throws IOException {
        SourceCheck spawnService = source("src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java");
        SourceCheck loader = source("src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java");
        SourceCheck particleReadme = source("src/main/java/io/github/tt432/eyelib/client/particle/README.md");
        SourceCheck registryReadme = source("src/main/java/io/github/tt432/eyelib/client/registry/README.md");
        SourceCheck loaderReadme = source("src/main/java/io/github/tt432/eyelib/client/loader/README.md");
        SourceCheck moduleReadme = source("eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md");
        SourceCheck moduleBoundaries = source("docs/architecture/01-module-boundaries.md");
        SourceCheck sideBoundaries = source("docs/architecture/02-side-boundaries.md");
        Path obsoleteRootRequest = Path.of("src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnRequest.java");
        List<Path> deletedLegacyFacades = List.of(
                Path.of("src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java"),
                Path.of("src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java"),
                Path.of("src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java"),
                Path.of("src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java")
        );

        deletedLegacyFacades.forEach(path -> assertTrue(Files.notExists(path), () -> path + " should remain deleted"));

        spawnService.assertContains("import io.github.tt432.eyelibparticle.api.ParticleSpawnApi;");
        spawnService.assertContains("import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;");
        spawnService.assertContains("import io.github.tt432.eyelibparticle.client.ParticleSpawnRuntimeAdapter;");
        spawnService.assertContains("import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;");
        spawnService.assertContains("import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;");
        spawnService.assertContains("ParticleSpawnApi api()");
        spawnService.assertContains("api().spawn(new ParticleSpawnRequest(");
        spawnService.assertContains("api().remove(removeId);");
        spawnService.assertNotContains("BrParticle.CODEC.encodeStart");
        spawnService.assertNotContains("import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;");
        spawnService.assertNotContains("import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;");
        spawnService.assertNotContains("import io.github.tt432.eyelib.client.registry.ParticleAssetRegistry;");
        spawnService.assertContains("Transitional");
        spawnService.assertContains("this facade after packet/runtime callers bind directly");

        loader.assertContains("ParticleResourcePublication.replaceFromJsonResources");
        loader.assertContains("entry.getKey().toString()");

        particleReadme.assertContains("`ParticleSpawnService.java`: transitional root facade");
        particleReadme.assertContains("Legacy root `bedrock/**`, `ParticleLookup`, `ParticleManager`, and `ParticleAssetRegistry` have been deleted");
        particleReadme.assertContains("do not add a duplicate root request type");
        assertTrue(Files.notExists(obsoleteRootRequest), () -> obsoleteRootRequest + " should not be reintroduced");
        registryReadme.assertContains("`ParticleAssetRegistry` has been deleted");
        registryReadme.assertContains("ParticleResourcePublication");
        loaderReadme.assertContains("delegates particle publication");
        loaderReadme.assertContains("ParticleResourcePublication");
        moduleReadme.assertContains("active loading/publication");
        moduleReadme.assertContains("ParticleSpawnRuntimeAdapter");
        moduleReadme.assertContains("ParticleResourcePublication");
        moduleReadme.assertContains("ParticleDefinition.identifier()");
        moduleBoundaries.assertContains("ParticleDefinitionRegistry");
        moduleBoundaries.assertContains("ParticleResourcePublication");
        moduleBoundaries.assertContains("ParticleDefinition.identifier()");
        moduleBoundaries.assertContains("Root legacy `client/particle/bedrock/**` schema/runtime tree has been deleted");
        sideBoundaries.assertContains("ResourceLocation adaptation for particle loading remains at root Forge/resource integration boundaries");
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
                    .filter(path -> !path.toString().contains("\\network\\"))
                    .filter(path -> !path.toString().contains("/network/"))
                    .filter(path -> !path.getFileName().toString().equals("EyelibParticleMod.java"))
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

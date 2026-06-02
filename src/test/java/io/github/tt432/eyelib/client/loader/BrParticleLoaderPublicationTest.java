package io.github.tt432.eyelib.client.loader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class BrParticleLoaderPublicationTest {
    @Test
    void preservesParticlesJsonReloadContract() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java"
        ));

        assertTrue(source.contains("super(\"particles\", \"json\")"));
        assertTrue(source.contains("Map<ResourceLocation, JsonElement>"));
        assertTrue(source.contains("entry.getKey().toString()"));
    }

    @Test
    void delegatesPublicationToParticleModule() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java"
        ));

        assertTrue(source.contains("import io.github.tt432.eyelibparticle.loading.ParticleResourcePublication;"));
        assertTrue(source.contains("ParticleResourcePublication.replaceFromJsonResources("));
        assertTrue(!source.contains("client.particle.bedrock.BrParticle"));
        assertTrue(!source.contains("BrParticle.CODEC"));
        assertTrue(!source.contains("ParticleAssetRegistry.replaceParticles(particles)"));
    }
}

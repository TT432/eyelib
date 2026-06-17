package io.github.tt432.eyelib.client.particle;

import com.google.gson.JsonParser;
import io.github.tt432.eyelib.particle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelib.particle.loading.ParticleResourcePublication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** @author TT432 */
class ParticleRuntimeDelegationBoundaryTest {
    @AfterEach
    void clearParticles() {
        ParticleDefinitionRegistry.store().clear();
    }

    @Test
    void spawnServiceBuildsModuleRuntimeAndDelegatesToModuleRenderManager() throws IOException {
        ParticleResourcePublication.replaceFromJsonResources(
                Map.of("particles/runtime.particle", JsonParser.parseString(particleJson("eyelib:runtime_particle"))),
                LoggerFactory.getLogger(ParticleRuntimeDelegationBoundaryTest.class)
        );

        assertNotNull(ParticleDefinitionRegistry.store().get("eyelib:runtime_particle"));
        assertNull(ParticleDefinitionRegistry.store().get("particles/runtime.particle"));
    }

    private static String particleJson(String identifier) {
        return """
                {
                  "format_version": "1.10.0",
                  "particle_effect": {
                    "description": {
                      "identifier": "%s",
                      "basic_render_parameters": {
                        "material": "particles_alpha",
                        "texture": "textures/particle/particles"
                      }
                    }
                  }
                }
                """.formatted(identifier);
    }

}

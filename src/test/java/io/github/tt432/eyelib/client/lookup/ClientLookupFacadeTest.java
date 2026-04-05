package io.github.tt432.eyelib.client.lookup;

import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.AnimationLookup;
import io.github.tt432.eyelib.client.manager.AnimationManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.ParticleManager;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelLookup;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.particle.ParticleLookup;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.molang.MolangScope;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClientLookupFacadeTest {
    @AfterEach
    void tearDown() {
        AnimationManager.INSTANCE.clear();
        ModelManager.INSTANCE.clear();
        ParticleManager.INSTANCE.clear();
    }

    @Test
    void animationLookupExposesSizeAndNamesThroughLookupSeam() {
        AnimationManager.INSTANCE.put("animation.test.idle", new StubAnimation("animation.test.idle"));
        AnimationManager.INSTANCE.put("animation.test.walk", new StubAnimation("animation.test.walk"));

        assertEquals(2, AnimationLookup.size());
        assertEquals(Set.of("animation.test.idle", "animation.test.walk"), Set.copyOf(AnimationLookup.names()));
    }

    @Test
    void modelLookupAllReturnsSnapshotContainingStoredModel() {
        Model model = new Model("geometry.test", new Int2ObjectOpenHashMap<>());
        ModelManager.INSTANCE.put("geometry.test", model);

        Map<String, Model> snapshot = ModelLookup.all();
        ModelManager.INSTANCE.put("geometry.other", new Model("geometry.other", new Int2ObjectOpenHashMap<>()));

        assertEquals(Set.of("geometry.test"), snapshot.keySet());
        assertSame(model, snapshot.get("geometry.test"));
    }

    @Test
    void particleLookupExposesNamesAndGetThroughLookupSeam() {
        BrParticle particle = BrParticle.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, com.google.gson.JsonParser.parseString("""
                {
                  "format_version": "1.10.0",
                  "particle_effect": {
                    "description": {
                      "identifier": "eyelib:test_particle",
                      "basic_render_parameters": {
                        "material": "particles_alpha",
                        "texture": "eyelib:test_particle"
                      }
                    }
                  }
                }
                """)).getOrThrow(false, AssertionError::new);
        ParticleManager.INSTANCE.put("eyelib:test_particle", particle);

        assertEquals(Set.of("eyelib:test_particle"), Set.copyOf(ParticleLookup.names()));
        assertSame(particle, ParticleLookup.get("eyelib:test_particle"));
    }

    private record StubAnimation(String name) implements Animation<Void> {
        @Override
        public void onFinish(Void data) {
        }

        @Override
        public boolean anyAnimationFinished(Void data) {
            return false;
        }

        @Override
        public boolean allAnimationFinished(Void data) {
            return false;
        }

        @Override
        public Void createData() {
            return null;
        }

        @Override
        public void tickAnimation(Void data, Map<String, String> animations, MolangScope scope, float ticks,
                                  float multiplier, ModelRuntimeData renderInfos, AnimationEffects effects,
                                  Runnable animationStartFeedback) {
        }
    }
}

package io.github.tt432.eyelib.client.lookup;

import io.github.tt432.eyelib.animation.Animation;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationLookup;
import io.github.tt432.eyelib.animation.AnimationRegistries;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.molang.MolangScope;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** @author TT432 */
class ClientLookupFacadeTest {
    @AfterEach
    void tearDown() {
        AnimationRegistries.animation().clear();
        ModelManager.INSTANCE.clear();
    }

    @Test
    void animationLookupExposesSizeAndNamesThroughLookupSeam() {
        AnimationRegistries.animation().put("animation.test.idle", new StubAnimation("animation.test.idle"));
        AnimationRegistries.animation().put("animation.test.walk", new StubAnimation("animation.test.walk"));

        assertEquals(2, AnimationLookup.size());
        assertEquals(Set.of("animation.test.idle", "animation.test.walk"), Set.copyOf(AnimationLookup.names()));
    }

    @Test
    void modelLookupAllReturnsSnapshotContainingStoredModel() {
        Model model = Model.of("geometry.test", new Int2ObjectOpenHashMap<>());
        ModelManager.INSTANCE.put("geometry.test", model);

        Map<String, Model> snapshot = ModelManager.INSTANCE.all();
        ModelManager.INSTANCE.put("geometry.other", Model.of("geometry.other", new Int2ObjectOpenHashMap<>()));

        assertEquals(Set.of("geometry.test"), snapshot.keySet());
        assertSame(model, snapshot.get("geometry.test"));
    }

    private record StubAnimation(String name) implements Animation {
        @Override
        public void onFinish(Object data) {
        }

        @Override
        public boolean anyAnimationFinished(Object data) {
            return false;
        }

        @Override
        public boolean allAnimationFinished(Object data) {
            return false;
        }

        @Override
        public Object createData() {
            return null;
        }

        @Override
        public void tickAnimation(Object data, Map<String, String> animations, MolangScope scope, float ticks,
                                  float multiplier, ModelRuntimeData renderInfos, AnimationEffects effects,
                                  Runnable animationStartFeedback) {
        }
    }
}

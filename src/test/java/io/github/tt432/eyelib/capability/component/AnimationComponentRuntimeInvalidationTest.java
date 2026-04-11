package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.AnimationLookup;
import io.github.tt432.eyelib.client.manager.AnimationManager;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnimationComponentRuntimeInvalidationTest {
    @AfterEach
    void tearDown() {
        AnimationManager.writePort().clear();
    }

    @Test
    void managerEventInvalidatesSerializableInfoOnlyForMatchingAnimationEntry() {
        AnimationManager.writePort().put("animation.walk", new TestAnimation("animation.walk"));

        AnimationComponent component = new AnimationComponent();
        component.setup(
                Map.of("controller.main", "animation.walk"),
                Map.of("controller.main", MolangValue.ONE)
        );

        assertNotNull(component.getSerializableInfo());

        AnimationComponent.onManagerEntryChanged("OtherManager", "animation.walk");
        assertNotNull(component.getSerializableInfo());

        AnimationComponent.onManagerEntryChanged(AnimationLookup.managerName(), "animation.idle");
        assertNotNull(component.getSerializableInfo());

        AnimationComponent.onManagerEntryChanged(AnimationLookup.managerName(), "animation.walk");
        assertNull(component.getSerializableInfo());
    }

    private record TestAnimation(String name) implements Animation<Object> {
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
            return new Object();
        }

        @Override
        public void tickAnimation(Object data,
                                  Map<String, String> animations,
                                  MolangScope scope,
                                  float ticks,
                                  float multiplier,
                                  ModelRuntimeData renderInfos,
                                  AnimationEffects effects,
                                  Runnable animationStartFeedback) {
        }
    }
}

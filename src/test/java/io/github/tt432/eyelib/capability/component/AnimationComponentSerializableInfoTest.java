package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.animation.Animation;
import io.github.tt432.eyelib.animation.AnimationComponent;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationManager;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/** @author TT432 */
class AnimationComponentSerializableInfoTest {
    @AfterEach
    void tearDown() {
        AnimationManager.INSTANCE.clear();
    }

    @Test
    void setInfoCarriesBindingsButRecreatesRuntimeAnimationState() {
        TestAnimation animation = new TestAnimation("animation.test.walk");
        AnimationManager.INSTANCE.put(animation.name(), animation);

        AnimationComponent source = new AnimationComponent();
        source.setup(
                Map.of("controller.main", animation.name()),
                Map.of("controller.main", MolangValue.ONE)
        );
        Object sourceRuntimeState = source.getAnimationData(animation.name());

        AnimationComponent target = new AnimationComponent();
        target.setInfo(source.getSerializableInfo());
        Object targetRuntimeState = target.getAnimationData(animation.name());

        assertEquals(source.getSerializableInfo(), target.getSerializableInfo());
        assertNotSame(sourceRuntimeState, targetRuntimeState);
        assertEquals(2, animation.createdStates);
    }

    private static final class TestAnimation implements Animation {
        private final String name;
        private int createdStates;

        private TestAnimation(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

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
            createdStates++;
            return new Object();
        }

        @Override
        public void tickAnimation(Object data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                                  ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback) {
        }
    }
}

package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.animation.Animation;
import io.github.tt432.eyelib.animation.AnimationComponent;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationRegistries;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Opt18-B AnimationComponent.setup 引用短路契约：
 * 同引用逐帧调用零重建（不触发 createData）；等值新引用走 equals 兜底同样零重建；
 * 内容真实变化才重建。
 */
class AnimationComponentSetupRefShortcutTest {
    @AfterEach
    void tearDown() {
        AnimationRegistries.animation().clear();
    }

    @Test
    void sameReferenceSetupIsZeroRebuild() {
        TestAnimation animation = new TestAnimation("animation.test.walk");
        AnimationRegistries.animation().put(animation.name(), animation);

        AnimationComponent component = new AnimationComponent();
        Map<String, String> animations = Map.of("controller.main", animation.name());
        Map<String, MolangValue> animate = Map.of("controller.main", MolangValue.ONE);

        component.setup(animations, animate);
        assertEquals(1, animation.createdStates);

        // 生产路径（setupClientEntity 逐帧）：同一 BrClientEntity/scripts 实例返回同一 map 实例
        component.setup(animations, animate);
        component.setup(animations, animate);
        assertEquals(1, animation.createdStates, "同引用重复 setup 不得重建动画状态");
    }

    @Test
    void equalContentNewInstancesStillSkipRebuild() {
        TestAnimation animation = new TestAnimation("animation.test.walk");
        AnimationRegistries.animation().put(animation.name(), animation);

        AnimationComponent component = new AnimationComponent();
        component.setup(Map.of("controller.main", animation.name()), Map.of("controller.main", MolangValue.ONE));
        assertEquals(1, animation.createdStates);

        // equals 兜底：等值但不同实例（Map.copyOf 形态变化）不得重建
        component.setup(new HashMap<>(Map.of("controller.main", animation.name())),
                new HashMap<>(Map.of("controller.main", MolangValue.ONE)));
        assertEquals(1, animation.createdStates);
    }

    @Test
    void changedContentRebuilds() {
        TestAnimation animation = new TestAnimation("animation.test.walk");
        AnimationRegistries.animation().put(animation.name(), animation);

        AnimationComponent component = new AnimationComponent();
        Map<String, String> animations = Map.of("controller.main", animation.name());
        Map<String, MolangValue> animate = Map.of("controller.main", MolangValue.ONE);
        component.setup(animations, animate);

        component.setup(animations, Map.of("controller.main", new MolangValue("2")));
        assertEquals(2, animation.createdStates, "animate 内容变化必须重建");
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

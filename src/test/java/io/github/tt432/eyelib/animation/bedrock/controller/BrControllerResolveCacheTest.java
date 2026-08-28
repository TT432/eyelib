package io.github.tt432.eyelib.animation.bedrock.controller;

import io.github.tt432.eyelib.animation.Animation;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationRegistries;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.molang.MolangScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link BrControllerStateOwner#resolveAnimation} 解析缓存契约（Opt16）：
 * <ul>
 *     <li>注册表 generation 变更（put/remove/replaceAll）后必须重解析；</li>
 *     <li>currentAnimations 实例替换后必须重解析；</li>
 *     <li>未知名称的 null 结果同样被缓存但随守卫失效。</li>
 * </ul>
 *
 * @author TT432
 */
class BrControllerResolveCacheTest {
    @AfterEach
    void tearDown() {
        AnimationRegistries.animation().clear();
    }

    @Test
    void resolvesThroughAnimationsMapAndRegistry() {
        BrControllerStateOwner owner = new BrControllerStateOwner();
        owner.currentAnimations(Map.of("walk", "animation.test.walk"));
        TestAnimation animation = new TestAnimation("animation.test.walk", new AtomicInteger());
        AnimationRegistries.animation().put("animation.test.walk", animation);

        assertSame(animation, owner.resolveAnimation("walk"));
        // 缓存命中：同实例
        assertSame(animation, owner.resolveAnimation("walk"));
    }

    @Test
    void registryMutationInvalidatesCache() {
        BrControllerStateOwner owner = new BrControllerStateOwner();
        owner.currentAnimations(Map.of("walk", "animation.test.walk"));
        TestAnimation first = new TestAnimation("animation.test.walk", new AtomicInteger());
        AnimationRegistries.animation().put("animation.test.walk", first);
        assertSame(first, owner.resolveAnimation("walk"));

        // 同 id 新实现（资源重载语义）→ generation 变化 → 必须返回新实例
        TestAnimation second = new TestAnimation("animation.test.walk", new AtomicInteger());
        AnimationRegistries.animation().put("animation.test.walk", second);
        assertSame(second, owner.resolveAnimation("walk"));

        // 移除（mod/包卸载语义）→ null
        AnimationRegistries.animation().remove("animation.test.walk");
        assertNull(owner.resolveAnimation("walk"));
    }

    @Test
    void animationsMapReplacementInvalidatesCache() {
        BrControllerStateOwner owner = new BrControllerStateOwner();
        owner.currentAnimations(Map.of("walk", "animation.test.walk"));
        TestAnimation walk = new TestAnimation("animation.test.walk", new AtomicInteger());
        AnimationRegistries.animation().put("animation.test.walk", walk);
        assertSame(walk, owner.resolveAnimation("walk"));

        // 定义替换（同 key 指向不同注册名）→ 新 map 实例 → 重解析
        TestAnimation run = new TestAnimation("animation.test.run", new AtomicInteger());
        AnimationRegistries.animation().put("animation.test.run", run);
        owner.currentAnimations(Map.of("walk", "animation.test.run"));
        assertSame(run, owner.resolveAnimation("walk"));
    }

    @Test
    void unknownNameCachesNullButFollowsGuards() {
        BrControllerStateOwner owner = new BrControllerStateOwner();
        owner.currentAnimations(new HashMap<>(Map.of("walk", "animation.test.missing")));
        assertNull(owner.resolveAnimation("walk"));
        assertNull(owner.resolveAnimation("absent_key"));

        // 缺失动画随后注册 → generation 失效 → 解析到新实例
        TestAnimation late = new TestAnimation("animation.test.missing", new AtomicInteger());
        AnimationRegistries.animation().put("animation.test.missing", late);
        assertSame(late, owner.resolveAnimation("walk"));
    }

    private record TestAnimation(String name, AtomicInteger createDataCalls) implements Animation {
        @Override
        public Object createData() {
            createDataCalls.incrementAndGet();
            return new Object();
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
        public void tickAnimation(Object data, Map<String, String> animations, MolangScope scope, float ticks,
                                  float multiplier, ModelRuntimeData renderInfos, AnimationEffects effects,
                                  Runnable animationStartFeedback) {
        }
    }
}

package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.util.manager.ManagerEventPublishBridge;
import io.github.tt432.eyelib.util.registry.Registry;

/** @author TT432 */
public final class AnimationRegistries {
    // 默认即接事件桥（未 install 时为 NOOP）：NOOP 会让 AnimationComponent 的
    // 条目/批量失效事件在生产环境永不触发，资源重载后在场实体继续播放旧动画。
    private static volatile Registry<Animation> animation =
            new Registry<>("AnimationManager", ManagerEventPublishBridge.publisher());

    private AnimationRegistries() {
    }

    public static Registry<Animation> animation() {
        return animation;
    }

    public static void register(Registry<Animation> registry) {
        animation = registry;
    }
}

package io.github.tt432.eyelibbehavior;

import io.github.tt432.eyelibbehavior.component.Component;
import io.github.tt432.eyelibbehavior.component.group.ComponentGroup;

import java.util.Collections;
import java.util.Map;

/**
 * 顶层 minecraft:entity.components 的 typed 存储。
 * 使用与 ComponentGroup 相同的 KeyDispatchMapCodec 分发策略。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BehaviorComponents(
        Map<String, Component> components
) {
    public static final BehaviorComponents EMPTY = new BehaviorComponents(Collections.emptyMap());

    public static final com.mojang.serialization.Codec<BehaviorComponents> CODEC =
            ComponentGroup.DISPATCH_CODEC
                    .xmap(BehaviorComponents::new, BehaviorComponents::components);
}

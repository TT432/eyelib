package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.animation.AnimationChannelDefinition;
import io.github.tt432.eyelib.util.collection.ImmutableFloatTreeMap;
/**
 * 按时间戳索引关键帧的动画通道。
 *
 * @author TT432
 */
public record BrAnimationChannel<K>(
        String name,
        ImmutableFloatTreeMap<K> keyFrames
) implements AnimationChannelDefinition<K> {
}
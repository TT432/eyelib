package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibanimation.AnimationChannelDefinition;
import io.github.tt432.eyelibutil.collection.ImmutableFloatTreeMap;
import org.jspecify.annotations.NullMarked;

/**
 * 按时间戳索引关键帧的动画通道。
 *
 * @author TT432
 */
@NullMarked
public record BrAnimationChannel<K>(
        String name,
        ImmutableFloatTreeMap<K> keyFrames
) implements AnimationChannelDefinition<K> {
}
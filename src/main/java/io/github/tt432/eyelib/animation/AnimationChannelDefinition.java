package io.github.tt432.eyelibanimation;

import io.github.tt432.eyelibutil.collection.ImmutableFloatTreeMap;
import org.jspecify.annotations.NullMarked;

/**
 * 按时间戳索引关键帧的命名动画通道。
 *
 * @author TT432
 */
@NullMarked
public interface AnimationChannelDefinition<K> {
    String name();

    ImmutableFloatTreeMap<K> keyFrames();
}
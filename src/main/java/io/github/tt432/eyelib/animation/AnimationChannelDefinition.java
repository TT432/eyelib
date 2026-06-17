package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.util.collection.ImmutableFloatTreeMap;
/**
 * 按时间戳索引关键帧的命名动画通道。
 *
 * @author TT432
 */
public interface AnimationChannelDefinition<K> {
    String name();

    ImmutableFloatTreeMap<K> keyFrames();
}
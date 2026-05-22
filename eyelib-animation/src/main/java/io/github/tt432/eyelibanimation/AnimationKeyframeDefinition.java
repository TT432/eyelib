package io.github.tt432.eyelibanimation;

import org.jspecify.annotations.NullMarked;

/**
 * 不可变的关键帧定义，按时间戳索引。
 *
 * @author TT432
 */
@NullMarked
public interface AnimationKeyframeDefinition {
    float timestamp();
}
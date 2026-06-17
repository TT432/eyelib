package io.github.tt432.eyelibanimation;

import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * 命名通道组成的不可变动画定义。
 *
 * @author TT432
 */
@NullMarked
public interface AnimationDefinition<K, C extends AnimationChannelDefinition<K>> {
    Map<String, C> channels();

    C emptyChannel(String name);

    default C channel(String name) {
        C channel = channels().get(name);
        return channel != null ? channel : emptyChannel(name);
    }
}
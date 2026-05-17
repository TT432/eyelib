package io.github.tt432.eyelibanimation;

import java.util.Map;

/**
 * Immutable animation definition composed from named channels.
 */
public interface AnimationDefinition<K, C extends AnimationChannelDefinition<K>> {
    Map<String, C> channels();

    C emptyChannel(String name);

    default C channel(String name) {
        C channel = channels().get(name);
        return channel != null ? channel : emptyChannel(name);
    }
}

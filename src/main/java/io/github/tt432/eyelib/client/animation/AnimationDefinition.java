package io.github.tt432.eyelib.client.animation;

import java.util.Map;

/**
 * Immutable animation definition composed from named channels.
 */
public interface AnimationDefinition<K, C extends AnimationChannelDefinition<K>>
        extends TrackAnimationDefinition<String, C> {
    Map<String, C> channels();

    C emptyChannel(String name);

    @Override
    default Map<String, C> tracks() {
        return channels();
    }

    @Override
    default C emptyTrack(String key) {
        return emptyChannel(key);
    }

    default C channel(String name) {
        return track(name);
    }
}

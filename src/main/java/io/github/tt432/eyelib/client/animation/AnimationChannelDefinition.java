package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.util.ImmutableFloatTreeMap;

/**
 * Named animation channel backed by timestamp-keyed keyframes.
 */
public interface AnimationChannelDefinition<K> {
    String name();

    ImmutableFloatTreeMap<K> keyFrames();
}

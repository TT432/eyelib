package io.github.tt432.eyelibanimation;

import io.github.tt432.eyelibutil.collection.ImmutableFloatTreeMap;

/**
 * Named animation channel backed by timestamp-keyed keyframes.
 */
public interface AnimationChannelDefinition<K> {
    String name();

    ImmutableFloatTreeMap<K> keyFrames();
}

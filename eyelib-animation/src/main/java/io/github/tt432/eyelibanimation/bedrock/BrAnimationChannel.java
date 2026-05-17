package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibanimation.AnimationChannelDefinition;
import io.github.tt432.eyelibutil.collection.ImmutableFloatTreeMap;

/**
 * Runtime animation channel backed by timestamp-keyed keyframes.
 *
 * @author TT432
 */
public record BrAnimationChannel<K>(
        String name,
        ImmutableFloatTreeMap<K> keyFrames
) implements AnimationChannelDefinition<K> {
}

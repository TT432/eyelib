package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelib.client.animation.AnimationChannelDefinition;
import io.github.tt432.eyelib.util.ImmutableFloatTreeMap;

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

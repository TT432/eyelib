package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibanimation.AnimationEffect;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public record BrAnimationEntryEffectTrackDefinition<T>(
        BrAnimationEntryTrackName name,
        AnimationEffect<T> effect
) implements BrAnimationEntryTrackDefinition {
}
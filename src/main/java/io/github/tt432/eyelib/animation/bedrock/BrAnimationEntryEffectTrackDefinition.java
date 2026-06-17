package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.animation.AnimationEffect;
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
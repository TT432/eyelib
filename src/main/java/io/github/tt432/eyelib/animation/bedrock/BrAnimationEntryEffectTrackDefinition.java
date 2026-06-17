package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.animation.AnimationEffect;
/**
 * @author TT432
 */
public record BrAnimationEntryEffectTrackDefinition<T>(
        BrAnimationEntryTrackName name,
        AnimationEffect<T> effect
) implements BrAnimationEntryTrackDefinition {
}
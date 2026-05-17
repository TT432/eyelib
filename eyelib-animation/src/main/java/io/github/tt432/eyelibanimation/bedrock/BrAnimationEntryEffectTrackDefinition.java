package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibanimation.AnimationEffect;

public record BrAnimationEntryEffectTrackDefinition<T>(
        BrAnimationEntryTrackName name,
        AnimationEffect<T> effect
) implements BrAnimationEntryTrackDefinition {
}

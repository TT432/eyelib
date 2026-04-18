package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelib.client.animation.AnimationEffect;

public record BrAnimationEntryEffectTrackDefinition<T>(
        BrAnimationEntryTrackName name,
        AnimationEffect<T> effect
) implements BrAnimationEntryTrackDefinition {
}

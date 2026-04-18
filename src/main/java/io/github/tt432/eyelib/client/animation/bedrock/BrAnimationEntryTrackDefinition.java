package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelib.client.animation.NamedTrackDefinition;

public sealed interface BrAnimationEntryTrackDefinition
        extends NamedTrackDefinition<BrAnimationEntryTrackName>
        permits BrAnimationEntryEffectTrackDefinition, BrAnimationEntryBoneTrackDefinition {
    BrAnimationEntryTrackName name();
}

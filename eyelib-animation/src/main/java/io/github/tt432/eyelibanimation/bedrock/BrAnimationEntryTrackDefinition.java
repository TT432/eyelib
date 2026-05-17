package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibimporter.animation.NamedTrackDefinition;

public sealed interface BrAnimationEntryTrackDefinition
        extends NamedTrackDefinition<BrAnimationEntryTrackName>
        permits BrAnimationEntryEffectTrackDefinition, BrAnimationEntryBoneTrackDefinition {
    BrAnimationEntryTrackName name();
}

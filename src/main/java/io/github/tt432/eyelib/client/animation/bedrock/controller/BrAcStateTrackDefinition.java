package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelib.client.animation.NamedTrackDefinition;

public sealed interface BrAcStateTrackDefinition extends NamedTrackDefinition<BrAcStateTrackName>
        permits BrAcStateAnimationsTrackDefinition, BrAcStateParticleEffectsTrackDefinition,
        BrAcStateSoundEffectsTrackDefinition, BrAcStateTransitionsTrackDefinition {
    BrAcStateTrackName name();
}

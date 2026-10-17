package io.github.tt432.eyelib.importer.animation.bedrock.controller;

import io.github.tt432.eyelib.importer.animation.NamedTrackDefinition;

/** @author TT432 */
public sealed interface BrAcStateTrackDefinition extends NamedTrackDefinition<BrAcStateTrackName>
        permits BrAcStateAnimationsTrackDefinition, BrAcStateParticleEffectsTrackDefinition,
        BrAcStateSoundEffectsTrackDefinition, BrAcStateTransitionsTrackDefinition {
    BrAcStateTrackName name();
}

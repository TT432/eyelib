package io.github.tt432.eyelibimporter.animation.bedrock.controller;

import io.github.tt432.eyelibimporter.animation.NamedTrackDefinition;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public sealed interface BrAcStateTrackDefinition extends NamedTrackDefinition<BrAcStateTrackName>
        permits BrAcStateAnimationsTrackDefinition, BrAcStateParticleEffectsTrackDefinition,
        BrAcStateSoundEffectsTrackDefinition, BrAcStateTransitionsTrackDefinition {
    BrAcStateTrackName name();
}

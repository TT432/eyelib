package io.github.tt432.eyelibimporter.animation.bedrock.controller;

import java.util.List;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrAcStateParticleEffectsTrackDefinition(
        io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateTrackName name,
        List<io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcParticleEffectDefinition> particleEffects
) implements io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateTrackDefinition {
    public BrAcStateParticleEffectsTrackDefinition {
        particleEffects = List.copyOf(particleEffects);
    }
}

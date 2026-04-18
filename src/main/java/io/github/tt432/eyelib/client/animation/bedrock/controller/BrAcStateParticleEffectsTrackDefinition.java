package io.github.tt432.eyelib.client.animation.bedrock.controller;

import java.util.List;

public record BrAcStateParticleEffectsTrackDefinition(
        BrAcStateTrackName name,
        List<BrAcParticleEffectDefinition> particleEffects
) implements BrAcStateTrackDefinition {
    public BrAcStateParticleEffectsTrackDefinition {
        particleEffects = List.copyOf(particleEffects);
    }
}

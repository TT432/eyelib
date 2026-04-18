package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelib.client.animation.NamedTrackContainerDefinition;
import io.github.tt432.eyelibmolang.MolangValue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record BrAcStateTracksDefinition(
        BrAcStateAnimationsTrackDefinition animations,
        BrAcStateParticleEffectsTrackDefinition particleEffects,
        BrAcStateSoundEffectsTrackDefinition soundEffects,
        BrAcStateTransitionsTrackDefinition transitions
) implements NamedTrackContainerDefinition<BrAcStateTrackName, BrAcStateTrackDefinition> {
    public static BrAcStateTracksDefinition of(
            Map<String, MolangValue> animations,
            List<BrAcParticleEffectDefinition> particleEffects,
            List<String> soundEffects,
            Map<String, MolangValue> transitions
    ) {
        return new BrAcStateTracksDefinition(
                new BrAcStateAnimationsTrackDefinition(BrAcStateTrackName.ANIMATIONS, animations),
                new BrAcStateParticleEffectsTrackDefinition(BrAcStateTrackName.PARTICLE_EFFECTS, particleEffects),
                new BrAcStateSoundEffectsTrackDefinition(BrAcStateTrackName.SOUND_EFFECTS, soundEffects),
                new BrAcStateTransitionsTrackDefinition(BrAcStateTrackName.TRANSITIONS, transitions)
        );
    }

    @Override
    public Map<BrAcStateTrackName, BrAcStateTrackDefinition> tracksByName() {
        EnumMap<BrAcStateTrackName, BrAcStateTrackDefinition> result = new EnumMap<>(BrAcStateTrackName.class);
        result.put(animations.name(), animations);
        result.put(particleEffects.name(), particleEffects);
        result.put(soundEffects.name(), soundEffects);
        result.put(transitions.name(), transitions);
        return Map.copyOf(result);
    }

    public Map<BrAcStateTrackName, BrAcStateTrackDefinition> byName() {
        return tracksByName();
    }
}

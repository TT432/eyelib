package io.github.tt432.eyelib.importer.animation.bedrock.controller;

import io.github.tt432.eyelib.importer.animation.NamedTrackContainerDefinition;
import io.github.tt432.eyelib.molang.MolangValue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrAcStateTracksDefinition(
        io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateAnimationsTrackDefinition animations,
        BrAcStateParticleEffectsTrackDefinition particleEffects,
        io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateSoundEffectsTrackDefinition soundEffects,
        io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTransitionsTrackDefinition transitions
) implements NamedTrackContainerDefinition<io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackName, io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackDefinition> {
    public static BrAcStateTracksDefinition of(
            Map<String, MolangValue> animations,
            List<io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcParticleEffectDefinition> particleEffects,
            List<String> soundEffects,
            Map<String, MolangValue> transitions
    ) {
        return new BrAcStateTracksDefinition(
                new io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateAnimationsTrackDefinition(io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackName.ANIMATIONS, animations),
                new BrAcStateParticleEffectsTrackDefinition(io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackName.PARTICLE_EFFECTS, particleEffects),
                new io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateSoundEffectsTrackDefinition(io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackName.SOUND_EFFECTS, soundEffects),
                new io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTransitionsTrackDefinition(io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackName.TRANSITIONS, transitions)
        );
    }

    @Override
    public Map<io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackName, io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackDefinition> tracksByName() {
        EnumMap<io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackName, io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackDefinition> result = new EnumMap<>(io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackName.class);
        result.put(animations.name(), animations);
        result.put(particleEffects.name(), particleEffects);
        result.put(soundEffects.name(), soundEffects);
        result.put(transitions.name(), transitions);
        return Map.copyOf(result);
    }

    public Map<io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackName, io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackDefinition> byName() {
        return tracksByName();
    }
}

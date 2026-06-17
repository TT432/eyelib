package io.github.tt432.eyelibimporter.animation.bedrock.controller;

import java.util.List;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrAcStateSoundEffectsTrackDefinition(
        io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateTrackName name,
        List<String> soundEffects
) implements io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateTrackDefinition {
    public BrAcStateSoundEffectsTrackDefinition {
        soundEffects = List.copyOf(soundEffects);
    }
}

package io.github.tt432.eyelib.client.animation.bedrock.controller;

import java.util.List;

public record BrAcStateSoundEffectsTrackDefinition(
        BrAcStateTrackName name,
        List<String> soundEffects
) implements BrAcStateTrackDefinition {
    public BrAcStateSoundEffectsTrackDefinition {
        soundEffects = List.copyOf(soundEffects);
    }
}

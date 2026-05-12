package io.github.tt432.eyelibimporter.animation.bedrock.controller;

public enum BrAcStateTrackName {
    ANIMATIONS("animations"),
    PARTICLE_EFFECTS("particle_effects"),
    SOUND_EFFECTS("sound_effects"),
    TRANSITIONS("transitions");

    private final String serializedName;

    BrAcStateTrackName(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}

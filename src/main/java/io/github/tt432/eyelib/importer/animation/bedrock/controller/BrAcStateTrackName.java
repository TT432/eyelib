package io.github.tt432.eyelib.importer.animation.bedrock.controller;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
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

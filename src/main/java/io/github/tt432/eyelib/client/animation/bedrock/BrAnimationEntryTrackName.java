package io.github.tt432.eyelib.client.animation.bedrock;

public enum BrAnimationEntryTrackName {
    SOUND_EFFECTS("sound_effects"),
    PARTICLE_EFFECTS("particle_effects"),
    TIMELINE("timeline"),
    BONES("bones");

    private final String serializedName;

    BrAnimationEntryTrackName(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}

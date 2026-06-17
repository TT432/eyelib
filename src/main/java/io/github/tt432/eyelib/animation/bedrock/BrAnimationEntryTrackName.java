package io.github.tt432.eyelib.animation.bedrock;

/**
 * 动画条目轨道名称枚举，对应 sound_effects / particle_effects / timeline / bones。
 *
 * @author TT432
 */
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
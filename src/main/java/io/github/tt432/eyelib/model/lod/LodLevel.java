package io.github.tt432.eyelib.model.lod;

/**
 * Runtime detail tiers shared by model rendering and animation sampling.
 */
public enum LodLevel {
    FULL(1, 0F),
    MEDIUM(2, 1F),
    LOW(4, 2.5F);

    private final int animationSampleInterval;
    private final float minimumBonePixels;

    LodLevel(int animationSampleInterval, float minimumBonePixels) {
        this.animationSampleInterval = animationSampleInterval;
        this.minimumBonePixels = minimumBonePixels;
    }

    public int animationSampleInterval() {
        return animationSampleInterval;
    }

    public float minimumBonePixels() {
        return minimumBonePixels;
    }
}

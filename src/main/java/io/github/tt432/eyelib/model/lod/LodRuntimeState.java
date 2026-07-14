package io.github.tt432.eyelib.model.lod;

/**
 * Per-entity LOD result. It is updated once per render frame and reused by every model component.
 */
public final class LodRuntimeState {
    public static final String MODEL_VISIT_CONTEXT_KEY = "lod_runtime_state";

    private LodLevel level = LodLevel.FULL;
    private float projectedHeight;
    private float pixelsPerUnit = Float.POSITIVE_INFINITY;
    private float minimumBonePixels;

    public void update(float projectedHeight, float pixelsPerUnit, float intensity) {
        float normalizedIntensity = LodPolicy.normalizeIntensity(intensity);
        this.projectedHeight = sanitizeNonNegative(projectedHeight);
        this.pixelsPerUnit = sanitizeNonNegative(pixelsPerUnit);
        this.level = LodPolicy.select(level, this.projectedHeight, normalizedIntensity);
        this.minimumBonePixels = level.minimumBonePixels() * normalizedIntensity;
    }

    public void setPreview(float intensity, float pixelsPerUnit) {
        float normalizedIntensity = LodPolicy.normalizeIntensity(intensity);
        this.projectedHeight = 1F - normalizedIntensity;
        this.pixelsPerUnit = sanitizeNonNegative(pixelsPerUnit);
        this.level = normalizedIntensity < 1F / 3F
                ? LodLevel.FULL
                : normalizedIntensity < 2F / 3F ? LodLevel.MEDIUM : LodLevel.LOW;
        this.minimumBonePixels = 4F * normalizedIntensity;
    }

    public boolean shouldRenderBone(float localSize) {
        if (minimumBonePixels <= 0F) return true;
        return sanitizeNonNegative(localSize) * pixelsPerUnit >= minimumBonePixels;
    }

    public LodLevel level() {
        return level;
    }

    public float projectedHeight() {
        return projectedHeight;
    }

    public float pixelsPerUnit() {
        return pixelsPerUnit;
    }

    public float minimumBonePixels() {
        return minimumBonePixels;
    }

    private static float sanitizeNonNegative(float value) {
        if (Float.isNaN(value) || value < 0F) return 0F;
        return value;
    }
}

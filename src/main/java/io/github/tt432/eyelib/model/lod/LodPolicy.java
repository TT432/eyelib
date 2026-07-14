package io.github.tt432.eyelib.model.lod;

/**
 * Screen-space LOD selection with one-way demotion hysteresis.
 */
public final class LodPolicy {
    static final float FULL_SCREEN_HEIGHT = 0.08F;
    static final float LOW_SCREEN_HEIGHT = 0.025F;
    static final float DEMOTION_HYSTERESIS = 0.85F;

    private LodPolicy() {
    }

    public static LodLevel select(LodLevel current, float projectedHeight, float intensity) {
        float normalizedIntensity = normalizeIntensity(intensity);
        if (normalizedIntensity == 0F || !Float.isFinite(projectedHeight) || projectedHeight < 0F) {
            return LodLevel.FULL;
        }

        float fullThreshold = FULL_SCREEN_HEIGHT * normalizedIntensity;
        float lowThreshold = LOW_SCREEN_HEIGHT * normalizedIntensity;
        return switch (current) {
            case FULL -> {
                if (projectedHeight < lowThreshold * DEMOTION_HYSTERESIS) yield LodLevel.LOW;
                if (projectedHeight < fullThreshold * DEMOTION_HYSTERESIS) yield LodLevel.MEDIUM;
                yield LodLevel.FULL;
            }
            case MEDIUM -> {
                if (projectedHeight >= fullThreshold) yield LodLevel.FULL;
                if (projectedHeight < lowThreshold * DEMOTION_HYSTERESIS) yield LodLevel.LOW;
                yield LodLevel.MEDIUM;
            }
            case LOW -> {
                if (projectedHeight >= fullThreshold) yield LodLevel.FULL;
                if (projectedHeight >= lowThreshold) yield LodLevel.MEDIUM;
                yield LodLevel.LOW;
            }
        };
    }

    public static float normalizeIntensity(float intensity) {
        if (Float.isNaN(intensity)) return 0F;
        if (intensity == Float.POSITIVE_INFINITY) return 1F;
        if (intensity == Float.NEGATIVE_INFINITY) return 0F;
        return Math.max(0F, Math.min(1F, intensity));
    }
}

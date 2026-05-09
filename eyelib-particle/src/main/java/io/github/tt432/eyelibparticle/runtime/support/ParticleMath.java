package io.github.tt432.eyelibparticle.runtime.support;

/**
 * Particle-runtime-owned math helpers copied at the module boundary.
 */
public final class ParticleMath {
    public static final float DEGREES_TO_RADIANS = 0.017453292519943295F;
    public static final float RADIANS_TO_DEGREES = 57.29577951308232F;
    public static final float E = 2.7182818284590452354F;
    public static final float PI = 3.14159265358979323846F;

    private ParticleMath() {
    }

    public static float getWeight(float before, float after, float current) {
        return (current - before) / (after - before);
    }

    public static float lerp(float a, float b, float weight) {
        return a + (b - a) * weight;
    }

    public static float notZero(float value, float defaultValue) {
        return value == 0 ? defaultValue : value;
    }

    public static boolean epsilon(float a, float b, float epsilon) {
        return Math.abs(b - a) < epsilon;
    }

    public static float lerpYaw(float a, float b, float position) {
        a = wrapDegrees(a);
        b = wrapDegrees(b);

        return lerp(a, normalizeYaw(a, b), position);
    }

    public static float normalizeYaw(float a, float b) {
        float diff = a - b;

        if (diff > 180.0D || diff < -180.0D) {
            diff = (float) Math.copySign(360.0D - Math.abs(diff), diff);

            return a + diff;
        }

        return b;
    }

    public static float wrapDegrees(float value) {
        value %= 360.0F;

        if (value >= 180.0F) {
            value -= 360.0F;
        }

        if (value < -180.0F) {
            value += 360.0F;
        }

        return value;
    }
}

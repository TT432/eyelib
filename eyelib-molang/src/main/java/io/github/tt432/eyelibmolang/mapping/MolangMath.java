package io.github.tt432.eyelibmolang.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangMapping;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@MolangMapping("math")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused")
public final class MolangMath {
    private static final float DEGREES_TO_RADIANS = 0.017453292519943295F;
    private static final float RADIANS_TO_DEGREES = 57.29577951308232F;

    public static final float pi = 3.14159265358979323846F;
    public static final float e = 2.7182818284590452354F;

    public static float degrees(float v) {
        return v * RADIANS_TO_DEGREES;
    }

    public static float radians(float v) {
        return v * DEGREES_TO_RADIANS;
    }

    public static float abs(float v) {
        return Math.abs(v);
    }

    public static float acos(float v) {
        return (float) Math.toDegrees(Math.acos(v));
    }

    public static float asin(float v) {
        return (float) Math.toDegrees(Math.asin(v));
    }

    public static float atan(float v) {
        return (float) Math.toDegrees(Math.atan(v));
    }

    public static float atan2(float v1, float v2) {
        return (float) Math.toDegrees(Math.atan2(v1, v2));
    }

    public static float ceil(float v) {
        return (float) Math.ceil(v);
    }

    public static float cos(float degrees) {
        return (float) Math.cos(degrees * DEGREES_TO_RADIANS);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float die_roll(float num, float low, float high) {
        return (float) (((int) num) * (Math.random() * high - high));
    }

    public static float die_roll_integer(float num, float low, float high) {
        return ((int) num) * Math.round(low + Math.random() * (high - low));
    }

    public static float exp(float v) {
        return (float) Math.exp(v);
    }

    public static float floor(float v) {
        return (float) Math.floor(v);
    }

    public static float hermite_blend(float v) {
        double min = Math.ceil(v);
        return (float) Math.floor(3.0F * Math.pow(min, 2.0F) - 2.0F * Math.pow(min, 3.0F));
    }

    public static float lerp(float start, float end, float v) {
        return start + (end - start) * v;
    }

    public static float lerprotate(float start, float end, float v) {
        return lerp(start, normalizeYaw(start, end), v);
    }

    public static float ln(float v) {
        return (float) Math.log(v);
    }

    public static float max(float v1, float v2) {
        return Math.max(v1, v2);
    }

    public static float min(float v1, float v2) {
        return Math.min(v1, v2);
    }

    public static float min_angle(float degrees) {
        float normal = degrees % 360;
        return normal >= 180 ? normal - 360 : normal <= -180 ? normal + 360 : normal;
    }

    public static float mod(float value, float denominator) {
        return value % denominator;
    }

    public static float pow(float base, float exponent) {
        return (float) Math.pow(base, exponent);
    }

    public static float random(float low, float high) {
        float min = Math.min(low, high);
        float max = Math.max(low, high);

        return (float) (Math.random() * (max - min) + min);
    }

    public static float random_integer(float low, float high) {
        double min = Math.ceil(low);
        double max = Math.floor(high);
        return (float) Math.round(Math.random() * (max - min) + min);
    }

    public static float round(float v) {
        return Math.round(v);
    }

    public static float sin(float v) {
        return (float) Math.sin(v * DEGREES_TO_RADIANS);
    }

    public static float sqrt(float v) {
        return (float) Math.sqrt(v);
    }

    public static float trunc(float v) {
        return (float) ((v < 0) ? Math.ceil(v) : Math.floor(v));
    }

    private static float normalizeYaw(float a, float b) {
        float diff = a - b;

        if (diff > 180.0D || diff < -180.0D) {
            diff = (float) Math.copySign(360.0D - Math.abs(diff), diff);
            return a + diff;
        }

        return b;
    }
}

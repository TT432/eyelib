package io.github.tt432.eyelib.util.math;

import net.minecraft.world.phys.Vec3;

/**
 * @author DustW
 */
public class MathE {
    public static final Vec3 X = new Vec3(1, 0, 0);
    public static final Vec3 Y = new Vec3(0, 1, 0);
    public static final Vec3 Z = new Vec3(0, 0, 1);

    /**
     * 从两值之间获取 weight
     */
    public static float getWeight(float before, float after, float current) {
        return (current - before) / (after - before);
    }

    /**
     * 插值
     */
    public static float lerp(float a, float b, float weight) {
        return a + (b - a) * weight;
    }

    public static double notZero(double value, double defaultValue) {
        return value == 0 ? defaultValue : value;
    }

    /**
     * 判断 a 和 b 的差是否在某个区间
     */
    public static boolean epsilon(double a, double b, double epsilon) {
        return Math.abs(b - a) < epsilon;
    }

    /**
     * @see MathE#epsilon(double, double, double)
     */
    public static boolean epsilon(double a, double b) {
        return epsilon(a, b, 0.001);
    }

    public static double limitNumber(double num, double min, double max) {
        return Math.max(min, Math.min(max, num));
    }

    public static float clamp(float x, float min, float max) {
        return (x < min) ? min : ((x > max) ? max : x);
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

package io.github.tt432.eyelib.util.math;

/**
 * @author DustW
 */
public class MathE {
    /**
     * 从两值之间获取 weight
     */
    public static double getWeight(double before, double after, double current) {
        return (current - before) / (after - before);
    }

    /**
     * 插值
     */
    public static double lerp(double a, double b, double weight) {
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

    public static double clamp(double x, double min, double max) {
        return (x < min) ? min : ((x > max) ? max : x);
    }

    public static double lerpYaw(double a, double b, double position) {
        a = wrapDegrees(a);
        b = wrapDegrees(b);

        return lerp(a, normalizeYaw(a, b), position);
    }

    public static double normalizeYaw(double a, double b) {
        double diff = a - b;

        if (diff > 180.0D || diff < -180.0D) {
            diff = Math.copySign(360.0D - Math.abs(diff), diff);

            return a + diff;
        }

        return b;
    }

    public static double wrapDegrees(double value) {
        value %= 360.0D;

        if (value >= 180.0D) {
            value -= 360.0D;
        }

        if (value < -180.0D) {
            value += 360.0D;
        }

        return value;
    }
}

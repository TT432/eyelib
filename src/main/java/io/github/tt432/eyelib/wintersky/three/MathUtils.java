package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 MathUtils 移植（wintersky 依赖的子集）。
 */
public final class MathUtils {

    private MathUtils() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

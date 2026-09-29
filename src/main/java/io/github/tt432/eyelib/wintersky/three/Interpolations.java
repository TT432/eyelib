package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 extras/core/Interpolations.js 移植（CatmullRom、CubicBezier）。
 */
public final class Interpolations {

    private Interpolations() {
    }

    /**
     * Bezier Curves formulas obtained from
     * http://en.wikipedia.org/wiki/Bézier_curve
     */
    public static double catmullRom(double t, double p0, double p1, double p2, double p3) {
        double v0 = (p2 - p0) * 0.5;
        double v1 = (p3 - p1) * 0.5;
        double t2 = t * t;
        double t3 = t * t2;
        return (2 * p1 - 2 * p2 + v0 + v1) * t3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * t2 + v0 * t + p1;
    }

    private static double cubicBezierP0(double t, double p) {
        double k = 1 - t;
        return k * k * k * p;
    }

    private static double cubicBezierP1(double t, double p) {
        double k = 1 - t;
        return 3 * k * k * t * p;
    }

    private static double cubicBezierP2(double t, double p) {
        return 3 * (1 - t) * t * t * p;
    }

    private static double cubicBezierP3(double t, double p) {
        return t * t * t * p;
    }

    public static double cubicBezier(double t, double p0, double p1, double p2, double p3) {
        return cubicBezierP0(t, p0) + cubicBezierP1(t, p1) + cubicBezierP2(t, p2) +
                cubicBezierP3(t, p3);
    }
}

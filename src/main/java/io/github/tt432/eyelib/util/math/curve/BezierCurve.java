package io.github.tt432.eyelib.util.math.curve;

import io.github.tt432.eyelib.util.math.Vec2d;

/**
 * @author DustW
 */
public class BezierCurve extends Curve {
    private final Vec2d[] points;

    public BezierCurve(Vec2d... points) {
        this.points = points;
    }

    @Override
    public Vec2d getPoint(double time) {
        return new Vec2d(
                cubicBezier(time, points[0].getX(), points[1].getX(), points[2].getX(), points[3].getX()),
                cubicBezier(time, points[0].getY(), points[1].getY(), points[2].getY(), points[3].getY())
        );
    }

    static double cubicBezierP0(double t, double p) {
        var k = 1 - t;
        return k * k * k * p;
    }

    static double cubicBezierP1(double t, double p) {
        var k = 1 - t;
        return 3 * k * k * t * p;
    }

    static double cubicBezierP2(double t, double p) {
        return 3 * (1 - t) * t * t * p;
    }

    static double cubicBezierP3(double t, double p) {
        return t * t * t * p;
    }

    static double cubicBezier(double t, double p0, double p1, double p2, double p3) {
        return cubicBezierP0(t, p0) + cubicBezierP1(t, p1) + cubicBezierP2(t, p2) + cubicBezierP3(t, p3);
    }
}

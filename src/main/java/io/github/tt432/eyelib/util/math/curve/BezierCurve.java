package io.github.tt432.eyelib.util.math.curve;

import org.joml.Vector2f;

/**
 * @author DustW
 */
public class BezierCurve extends Curve {
    private final Vector2f[] points;

    public BezierCurve(Vector2f... points) {
        this.points = points;
    }

    @Override
    public Vector2f getPoint(float time) {
        return new Vector2f(
                cubicBezier(time, points[0].x(), points[1].x(), points[2].x(), points[3].x()),
                cubicBezier(time, points[0].y(), points[1].y(), points[2].y(), points[3].y())
        );
    }

    static float cubicBezierP0(float t, float p) {
        var k = 1 - t;
        return k * k * k * p;
    }

    static float cubicBezierP1(float t, float p) {
        var k = 1 - t;
        return 3 * k * k * t * p;
    }

    static float cubicBezierP2(float t, float p) {
        return 3 * (1 - t) * t * t * p;
    }

    static float cubicBezierP3(float t, float p) {
        return t * t * t * p;
    }

    static float cubicBezier(float t, float p0, float p1, float p2, float p3) {
        return cubicBezierP0(t, p0) + cubicBezierP1(t, p1) + cubicBezierP2(t, p2) + cubicBezierP3(t, p3);
    }
}

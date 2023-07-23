package io.github.tt432.eyelib.util.math.curve;

import org.joml.Vector2f;

/**
 * @author DustW
 */
public class SplineCurve extends Curve {
    private final Vector2f[] points;

    public SplineCurve(Vector2f... points) {
        this.points = points;
    }

    @Override
    public Vector2f getPoint(float time) {
        Vector2f result = new Vector2f();
        double p = (points.length - 1) * time;

        int intPoint = (int) Math.floor(p);
        double weight = p - intPoint;

        Vector2f p0 = points[intPoint == 0 ? intPoint : intPoint - 1];
        Vector2f p1 = points[intPoint];
        Vector2f p2 = points[intPoint > points.length - 2 ? points.length - 1 : intPoint + 1];
        Vector2f p3 = points[intPoint > points.length - 3 ? points.length - 1 : intPoint + 2];

        result.set(
                catmullRom(weight, p0.x(), p1.x(), p2.x(), p3.x()),
                catmullRom(weight, p0.y(), p1.y(), p2.y(), p3.y())
        );

        return result;
    }

    double catmullRom(double t, double p0, double p1, double p2, double p3) {
        double v0 = (p2 - p0) * .5;
        double v1 = (p3 - p1) * .5;
        double t2 = t * t;
        double t3 = t2 * t;
        return (2 * p1 - 2 * p2 + v0 + v1) * t3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * t2 + v0 * t + p1;
    }
}

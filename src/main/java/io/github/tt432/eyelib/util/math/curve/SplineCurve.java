package io.github.tt432.eyelib.util.math.curve;

import io.github.tt432.eyelib.util.math.Vec2d;

/**
 * @author DustW
 */
public class SplineCurve extends Curve {
    private final Vec2d[] points;

    public SplineCurve(Vec2d... points) {
        this.points = points;
    }

    public Vec2d getPoint(double time) {
        Vec2d result = new Vec2d();
        double p = (points.length - 1) * time;

        int intPoint = (int) Math.floor(p);
        double weight = p - intPoint;

        Vec2d p0 = points[intPoint == 0 ? intPoint : intPoint - 1];
        Vec2d p1 = points[intPoint];
        Vec2d p2 = points[intPoint > points.length - 2 ? points.length - 1 : intPoint + 1];
        Vec2d p3 = points[intPoint > points.length - 3 ? points.length - 1 : intPoint + 2];

        result.set(
                catmullRom(weight, p0.getX(), p1.getX(), p2.getX(), p3.getX()),
                catmullRom(weight, p0.getY(), p1.getY(), p2.getY(), p3.getY())
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

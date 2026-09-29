package io.github.tt432.eyelib.wintersky.three;

import java.util.ArrayList;
import java.util.List;

/**
 * three.js r134 SplineCurve 移植（Catmull-Rom，getPoint 语义逐字）。
 */
public class SplineCurve extends Curve {

    public final List<Vector2> points;

    public SplineCurve() {
        this(new ArrayList<>());
    }

    public SplineCurve(List<Vector2> points) {
        this.type = "SplineCurve";
        this.points = points;
    }

    @Override
    public Vector2 getPoint(double t) {
        return getPoint(t, new Vector2());
    }

    public Vector2 getPoint(double t, Vector2 optionalTarget) {
        Vector2 point = optionalTarget;

        List<Vector2> points = this.points;
        double p = (points.size() - 1) * t;

        int intPoint = (int) Math.floor(p);
        double weight = p - intPoint;

        Vector2 p0 = points.get(intPoint == 0 ? intPoint : intPoint - 1);
        Vector2 p1 = points.get(intPoint);
        Vector2 p2 = points.get(intPoint > points.size() - 2 ? points.size() - 1 : intPoint + 1);
        Vector2 p3 = points.get(intPoint > points.size() - 3 ? points.size() - 1 : intPoint + 2);

        point.set(
                Interpolations.catmullRom(weight, p0.x, p1.x, p2.x, p3.x),
                Interpolations.catmullRom(weight, p0.y, p1.y, p2.y, p3.y)
        );

        return point;
    }
}

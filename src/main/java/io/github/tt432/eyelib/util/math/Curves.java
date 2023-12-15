package io.github.tt432.eyelib.util.math;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Vector2f;

import java.util.List;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Curves {
    private static final Vector2f tempResult = new Vector2f();

    public static Vector2f lerpSplineCurve(List<Vector2f> points, float time) {
        double p = (points.size() - 1) * time;

        int intPoint = (int) Math.floor(p);
        double weight = p - intPoint;

        Vector2f p0 = points.get(intPoint == 0 ? intPoint : intPoint - 1);
        Vector2f p1 = points.get(intPoint);
        Vector2f p2 = points.get(intPoint > points.size() - 2 ? points.size() - 1 : intPoint + 1);
        Vector2f p3 = points.get(intPoint > points.size() - 3 ? points.size() - 1 : intPoint + 2);

        return tempResult.set(
                catmullRom(weight, p0.x(), p1.x(), p2.x(), p3.x()),
                catmullRom(weight, p0.y(), p1.y(), p2.y(), p3.y())
        );
    }

    private static double catmullRom(double t, double p0, double p1, double p2, double p3) {
        double v0 = (p2 - p0) * .5;
        double v1 = (p3 - p1) * .5;
        double t2 = t * t;
        double t3 = t2 * t;
        return (2 * p1 - 2 * p2 + v0 + v1) * t3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * t2 + v0 * t + p1;
    }
}

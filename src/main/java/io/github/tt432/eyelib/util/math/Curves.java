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
    public static float lerpSplineCurve(List<Vector2f> points, float time) {
        float p = (points.size() - 1) * time;

        int intPoint = (int) Math.floor(p);
        float weight = p - intPoint;

        Vector2f p0 = points.get(intPoint == 0 ? intPoint : intPoint - 1);
        Vector2f p1 = points.get(intPoint);
        Vector2f p2 = points.get(intPoint > points.size() - 2 ? points.size() - 1 : intPoint + 1);
        Vector2f p3 = points.get(intPoint > points.size() - 3 ? points.size() - 1 : intPoint + 2);

        return catmullRom(weight, p0.y(), p1.y(), p2.y(), p3.y());
    }

    public static float catmullRom(float t, float p0, float p1, float p2, float p3) {
        float v0 = (p2 - p0) * .5F;
        float v1 = (p3 - p1) * .5F;
        float t2 = t * t;
        float t3 = t2 * t;
        return (2 * p1 - 2 * p2 + v0 + v1) * t3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * t2 + v0 * t + p1;
    }
}

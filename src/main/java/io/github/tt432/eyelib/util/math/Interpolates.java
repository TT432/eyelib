package io.github.tt432.eyelib.util.math;

import io.github.tt432.eyelib.util.math.curve.BezierCurve;
import io.github.tt432.eyelib.util.math.curve.SplineCurve;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Interpolates {
    @AllArgsConstructor
    public static final class Node {
        float time;
        float value;

        public Vector2f toVec() {
            return new Vector2f(time, value);
        }
    }

    public static float linear(Node a, Node b, float currentTick) {
        float weight = MathE.getWeight(a.time, b.time, currentTick);
        return linear(a.value, b.value, weight);
    }

    public static float linear(float a, float b, float weight) {
        return MathE.lerp(a, b, weight);
    }

    public static double catmullRom(@Nullable Node beforePlus, Node before, Node after, @Nullable Node afterPlus, float currentTick) {
        float weight = MathE.getWeight(before.time, after.time, currentTick);
        List<Vector2f> vectors = new ArrayList<>();

        if (beforePlus != null) vectors.add(beforePlus.toVec());
        vectors.add(before.toVec());
        vectors.add(after.toVec());
        if (afterPlus != null) vectors.add(afterPlus.toVec());

        SplineCurve curve = new SplineCurve(vectors.toArray(new Vector2f[0]));
        float time = (weight + (beforePlus != null ? 1 : 0)) / (vectors.size() - 1);
        return curve.getPoint(time).y;
    }

    public static double bezier(Node before, Node after, float currentTick) {
        var weight = MathE.getWeight(before.time, after.time, currentTick);
        var beforeValue = before.value;
        var afterValue = after.value;
        var timeGap = after.time - before.time;
        var timeHandleBefore = MathE.clamp(before.time, 0, timeGap);
        var timeHandleAfter = MathE.clamp(after.time, -timeGap, 0);

        var curve = new BezierCurve(
                new Vector2f(before.time, beforeValue),
                new Vector2f(before.time + timeHandleBefore, beforeValue + before.value),
                new Vector2f(after.time + timeHandleAfter, afterValue + after.value),
                new Vector2f(after.time, afterValue));

        var time = before.time + (after.time - before.time) * weight;

        var points = curve.getPoints(200);
        final Vector2f[] closest = new Vector2f[1];
        final double[] closestDiff = {Double.MAX_VALUE};

        points.forEach(point -> {
            var diff = Math.abs(point.x - time);
            if (diff < closestDiff[0]) {
                closestDiff[0] = diff;
                closest[0] = point;
            }
        });

        final Vector2f[] secondClosest = new Vector2f[1];
        closestDiff[0] = Double.MAX_VALUE;

        points.forEach(point -> {
            if (point == closest[0]) return;
            var diff = Math.abs(point.x - time);

            if (diff < closestDiff[0]) {
                closestDiff[0] = diff;
                secondClosest[0] = point;
            }
        });

        return MathE.lerp(closest[0].y, secondClosest[0].y,
                MathE.clamp(MathE.lerp(closest[0].x, secondClosest[0].x, time), 0, 1));
    }
}

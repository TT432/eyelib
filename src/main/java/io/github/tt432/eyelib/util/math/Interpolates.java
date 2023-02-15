package io.github.tt432.eyelib.util.math;

import io.github.tt432.eyelib.util.math.curve.BezierCurve;
import io.github.tt432.eyelib.util.math.curve.SplineCurve;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Interpolates {
    @AllArgsConstructor
    public static final class Node {
        double time;
        double value;

        public Vec2d toVec() {
            return new Vec2d(time, value);
        }
    }

    public static double linear(Node a, Node b, double currentTick) {
        double weight = MathE.getWeight(a.time, b.time, currentTick);
        return linear(a.value, b.value, weight);
    }

    public static double linear(double a, double b, double weight) {
        return MathE.lerp(a, b, weight);
    }

    public static double catmullRom(@Nullable Node beforePlus, Node before, Node after, @Nullable Node afterPlus, double currentTick) {
        double weight = MathE.getWeight(before.time, after.time, currentTick);
        List<Vec2d> vectors = new ArrayList<>();

        if (beforePlus != null) vectors.add(beforePlus.toVec());
        vectors.add(before.toVec());
        vectors.add(after.toVec());
        if (afterPlus != null) vectors.add(afterPlus.toVec());

        SplineCurve curve = new SplineCurve(vectors.toArray(new Vec2d[0]));
        double time = (weight + (beforePlus != null ? 1 : 0)) / (vectors.size() - 1);
        return curve.getPoint(time).getY();
    }

    public static double bezier(Node before, Node after, double currentTick) {
        double weight = MathE.getWeight(before.time, after.time, currentTick);
        double beforeValue = before.value;
        double afterValue = after.value;
        double timeGap = after.time - before.time;
        double timeHandleBefore = MathE.clamp(before.time, 0, timeGap);
        double timeHandleAfter = MathE.clamp(after.time, -timeGap, 0);

        var curve = new BezierCurve(
                new Vec2d(before.time, beforeValue),
                new Vec2d(before.time + timeHandleBefore, beforeValue + before.value),
                new Vec2d(after.time + timeHandleAfter, afterValue + after.value),
                new Vec2d(after.time, afterValue));

        var time = before.time + (after.time - before.time) * weight;

        var points = curve.getPoints(200);
        final Vec2d[] closest = new Vec2d[1];
        final double[] closestDiff = {Double.MAX_VALUE};

        points.forEach(point -> {
            var diff = Math.abs(point.getX() - time);
            if (diff < closestDiff[0]) {
                closestDiff[0] = diff;
                closest[0] = point;
            }
        });

        final Vec2d[] secondClosest = new Vec2d[1];
        closestDiff[0] = Double.MAX_VALUE;

        points.forEach(point -> {
            if (point == closest[0]) return;
            var diff = Math.abs(point.getX() - time);

            if (diff < closestDiff[0]) {
                closestDiff[0] = diff;
                secondClosest[0] = point;
            }
        });

        return MathE.lerp(closest[0].getY(), secondClosest[0].getY(),
                MathE.clamp(MathE.lerp(closest[0].getX(), secondClosest[0].getX(), time), 0, 1));
    }
}

package io.github.tt432.eyelib.util.math.curve;

import io.github.tt432.eyelib.util.math.Vec2d;

import java.util.ArrayList;
import java.util.List;

/**
 * @author DustW
 */
public abstract class Curve {
    private static final int DEFAULT_DIVISIONS = 5;

    public abstract Vec2d getPoint(double time);

    public List<Vec2d> getPoints() {
        return getPoints(DEFAULT_DIVISIONS);
    }

    public List<Vec2d> getPoints(int divisions) {
        List<Vec2d> result = new ArrayList<>();

        for (int i = 0; i <= divisions; i++) {
            result.add(getPoint(((double) i) / divisions));
        }

        return result;
    }
}

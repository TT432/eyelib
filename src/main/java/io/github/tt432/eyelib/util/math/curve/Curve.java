package io.github.tt432.eyelib.util.math.curve;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * @author DustW
 */
public abstract class Curve {
    private static final int DEFAULT_DIVISIONS = 5;

    public abstract Vector2f getPoint(float time);

    public List<Vector2f> getPoints() {
        return getPoints(DEFAULT_DIVISIONS);
    }

    public List<Vector2f> getPoints(int divisions) {
        List<Vector2f> result = new ArrayList<>();

        for (int i = 0; i <= divisions; i++) {
            result.add(getPoint(((float) i) / divisions));
        }

        return result;
    }
}

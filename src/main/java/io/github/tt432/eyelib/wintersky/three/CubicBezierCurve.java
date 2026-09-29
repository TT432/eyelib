package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 CubicBezierCurve 移植（getPoint 语义逐字）。
 */
public class CubicBezierCurve extends Curve {

    public Vector2 v0;
    public Vector2 v1;
    public Vector2 v2;
    public Vector2 v3;

    public CubicBezierCurve() {
        this(new Vector2(), new Vector2(), new Vector2(), new Vector2());
    }

    public CubicBezierCurve(Vector2 v0, Vector2 v1, Vector2 v2, Vector2 v3) {
        this.type = "CubicBezierCurve";
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    @Override
    public Vector2 getPoint(double t) {
        return getPoint(t, new Vector2());
    }

    public Vector2 getPoint(double t, Vector2 optionalTarget) {
        Vector2 point = optionalTarget;

        Vector2 v0 = this.v0, v1 = this.v1, v2 = this.v2, v3 = this.v3;

        point.set(
                Interpolations.cubicBezier(t, v0.x, v1.x, v2.x, v3.x),
                Interpolations.cubicBezier(t, v0.y, v1.y, v2.y, v3.y)
        );

        return point;
    }
}

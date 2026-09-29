package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 Curve 基类移植（wintersky 仅经 SplineCurve/CubicBezierCurve 的
 * {@code getPoint(t)} 触达，基类仅承载 type 字段与抽象 getPoint）。
 */
public abstract class Curve {

    public String type = "Curve";

    public abstract Vector2 getPoint(double t);
}

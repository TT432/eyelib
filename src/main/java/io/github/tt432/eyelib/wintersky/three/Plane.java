package io.github.tt432.eyelib.wintersky.three;

import org.jspecify.annotations.Nullable;

/**
 * three.js r134 Plane 移植（wintersky 用到的方法子集）。
 *
 * <p>未移植项（wintersky 未触达）：setFromCoplanarPoints、applyMatrix4、
 * translate、intersectsBox、projectPoint、coplanarPoint。
 */
public class Plane {

    /** normal is assumed to be normalized */
    public final Vector3 normal;
    public double constant;

    public Plane() {
        this(new Vector3(1, 0, 0), 0);
    }

    public Plane(Vector3 normal, double constant) {
        this.normal = normal;
        this.constant = constant;
    }

    public Plane set(Vector3 normal, double constant) {
        this.normal.copy(normal);
        this.constant = constant;
        return this;
    }

    public Plane setComponents(double x, double y, double z, double w) {
        this.normal.set(x, y, z);
        this.constant = w;
        return this;
    }

    public Plane setFromNormalAndCoplanarPoint(Vector3 normal, Vector3 point) {
        this.normal.copy(normal);
        this.constant = -point.dot(this.normal);
        return this;
    }

    public Plane copy(Plane plane) {
        this.normal.copy(plane.normal);
        this.constant = plane.constant;
        return this;
    }

    public Plane normalize() {
        // Note: will lead to a divide by zero if the plane is invalid.
        double inverseNormalLength = 1.0 / this.normal.length();
        this.normal.multiplyScalar(inverseNormalLength);
        this.constant *= inverseNormalLength;
        return this;
    }

    public Plane negate() {
        this.constant *= -1;
        this.normal.negate();
        return this;
    }

    public double distanceToPoint(Vector3 point) {
        return this.normal.dot(point) + this.constant;
    }

    public double distanceToSphere(Sphere sphere) {
        return this.distanceToPoint(sphere.center) - sphere.radius;
    }

    /** JS 版返回交点 target 或 null。 */
    public @Nullable Vector3 intersectLine(Line3 line, Vector3 target) {
        Vector3 direction = line.delta(new Vector3());

        double denominator = this.normal.dot(direction);

        if (denominator == 0) {
            // line is coplanar, return origin
            if (this.distanceToPoint(line.start) == 0) {
                return target.copy(line.start);
            }

            // Unsure if this is the correct method to handle this case.
            return null;
        }

        double t = -(line.start.dot(this.normal) + this.constant) / denominator;

        if (t < 0 || t > 1) {
            return null;
        }

        return target.copy(direction).multiplyScalar(t).add(line.start);
    }

    public boolean intersectsLine(Line3 line) {
        // Note: this tests if a line intersects the plane, not whether it (or its end-points) are coplanar with it.
        double startSign = this.distanceToPoint(line.start);
        double endSign = this.distanceToPoint(line.end);

        return (startSign < 0 && endSign > 0) || (endSign < 0 && startSign > 0);
    }

    public boolean intersectsSphere(Sphere sphere) {
        return sphere.intersectsPlane(this);
    }

    public Vector3 coplanarPoint(Vector3 target) {
        return target.copy(this.normal).multiplyScalar(-this.constant);
    }

    public boolean equals(Plane plane) {
        return plane.normal.equals(this.normal) && (plane.constant == this.constant);
    }

    public Plane clone() {
        return new Plane(new Vector3(), 0).copy(this);
    }
}

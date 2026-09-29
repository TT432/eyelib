package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 Vector2 移植（wintersky 用到的方法子集）。
 */
public class Vector2 {

    public double x;
    public double y;

    public Vector2() {
        this(0, 0);
    }

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2 set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2 setScalar(double scalar) {
        this.x = scalar;
        this.y = scalar;
        return this;
    }

    public Vector2 clone() {
        return new Vector2(this.x, this.y);
    }

    public Vector2 copy(Vector2 v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }

    public Vector2 add(Vector2 v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    public Vector2 addScaledVector(Vector2 v, double s) {
        this.x += v.x * s;
        this.y += v.y * s;
        return this;
    }

    public Vector2 sub(Vector2 v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    public Vector2 subVectors(Vector2 a, Vector2 b) {
        this.x = a.x - b.x;
        this.y = a.y - b.y;
        return this;
    }

    public Vector2 multiplyScalar(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }

    public Vector2 divideScalar(double scalar) {
        return this.multiplyScalar(1 / scalar);
    }

    public double dot(Vector2 v) {
        return this.x * v.x + this.y * v.y;
    }

    public double lengthSq() {
        return this.x * this.x + this.y * this.y;
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public Vector2 normalize() {
        double length = this.length();
        return this.divideScalar(length == 0 ? 1 : length);
    }

    public double distanceTo(Vector2 v) {
        return Math.sqrt(this.distanceToSquared(v));
    }

    public double distanceToSquared(Vector2 v) {
        double dx = this.x - v.x, dy = this.y - v.y;
        return dx * dx + dy * dy;
    }

    public Vector2 negate() {
        this.x = -this.x;
        this.y = -this.y;
        return this;
    }

    public boolean equals(Vector2 v) {
        return (v.x == this.x) && (v.y == this.y);
    }

    public Vector2 fromArray(double[] array) {
        return fromArray(array, 0);
    }

    public Vector2 fromArray(double[] array, int offset) {
        this.x = array[offset];
        this.y = array[offset + 1];
        return this;
    }

    public double[] toArray() {
        return toArray(new double[2], 0);
    }

    public double[] toArray(double[] array, int offset) {
        array[offset] = this.x;
        array[offset + 1] = this.y;
        return array;
    }

    @Override
    public String toString() {
        return "Vector2(" + x + ", " + y + ")";
    }
}

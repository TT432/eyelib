package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 Vector3 移植。
 *
 * <p>未移植项（wintersky 未触达）：camera 投影（project/unproject）、
 * 球/柱坐标、random/randomDirection、Symbol.iterator。
 */
public class Vector3 {

    public double x;
    public double y;
    public double z;

    public Vector3() {
        this(0, 0, 0);
    }

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3 setScalar(double scalar) {
        this.x = scalar;
        this.y = scalar;
        this.z = scalar;
        return this;
    }

    public Vector3 setX(double x) {
        this.x = x;
        return this;
    }

    public Vector3 setY(double y) {
        this.y = y;
        return this;
    }

    public Vector3 setZ(double z) {
        this.z = z;
        return this;
    }

    public Vector3 setComponent(int index, double value) {
        switch (index) {
            case 0 -> this.x = value;
            case 1 -> this.y = value;
            case 2 -> this.z = value;
            default -> throw new IllegalArgumentException("index is out of range: " + index);
        }
        return this;
    }

    public double getComponent(int index) {
        return switch (index) {
            case 0 -> this.x;
            case 1 -> this.y;
            case 2 -> this.z;
            default -> throw new IllegalArgumentException("index is out of range: " + index);
        };
    }

    public Vector3 clone() {
        return new Vector3(this.x, this.y, this.z);
    }

    public Vector3 copy(Vector3 v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }

    public Vector3 add(Vector3 v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }

    public Vector3 addScalar(double s) {
        this.x += s;
        this.y += s;
        this.z += s;
        return this;
    }

    public Vector3 addVectors(Vector3 a, Vector3 b) {
        this.x = a.x + b.x;
        this.y = a.y + b.y;
        this.z = a.z + b.z;
        return this;
    }

    public Vector3 addScaledVector(Vector3 v, double s) {
        this.x += v.x * s;
        this.y += v.y * s;
        this.z += v.z * s;
        return this;
    }

    public Vector3 sub(Vector3 v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }

    public Vector3 subScalar(double s) {
        this.x -= s;
        this.y -= s;
        this.z -= s;
        return this;
    }

    public Vector3 subVectors(Vector3 a, Vector3 b) {
        this.x = a.x - b.x;
        this.y = a.y - b.y;
        this.z = a.z - b.z;
        return this;
    }

    public Vector3 multiply(Vector3 v) {
        this.x *= v.x;
        this.y *= v.y;
        this.z *= v.z;
        return this;
    }

    public Vector3 multiplyScalar(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    public Vector3 multiplyVectors(Vector3 a, Vector3 b) {
        this.x = a.x * b.x;
        this.y = a.y * b.y;
        this.z = a.z * b.z;
        return this;
    }

    public Vector3 applyEuler(Euler euler) {
        return this.applyQuaternion(new Quaternion().setFromEuler(euler));
    }

    public Vector3 applyMatrix4(Matrix4 m) {
        double x = this.x, y = this.y, z = this.z;
        double[] e = m.elements;

        double w = 1 / (e[3] * x + e[7] * y + e[11] * z + e[15]);

        this.x = (e[0] * x + e[4] * y + e[8] * z + e[12]) * w;
        this.y = (e[1] * x + e[5] * y + e[9] * z + e[13]) * w;
        this.z = (e[2] * x + e[6] * y + e[10] * z + e[14]) * w;

        return this;
    }

    public Vector3 applyQuaternion(Quaternion q) {
        double x = this.x, y = this.y, z = this.z;
        double qx = q.getX(), qy = q.getY(), qz = q.getZ(), qw = q.getW();

        // calculate quat * vector
        double ix = qw * x + qy * z - qz * y;
        double iy = qw * y + qz * x - qx * z;
        double iz = qw * z + qx * y - qy * x;
        double iw = -qx * x - qy * y - qz * z;

        // calculate result * inverse quat
        this.x = ix * qw + iw * -qx + iy * -qz - iz * -qy;
        this.y = iy * qw + iw * -qy + iz * -qx - ix * -qz;
        this.z = iz * qw + iw * -qz + ix * -qy - iy * -qx;

        return this;
    }

    public Vector3 transformDirection(Matrix4 m) {
        // input: affine matrix; vector interpreted as a direction
        double x = this.x, y = this.y, z = this.z;
        double[] e = m.elements;

        this.x = e[0] * x + e[4] * y + e[8] * z;
        this.y = e[1] * x + e[5] * y + e[9] * z;
        this.z = e[2] * x + e[6] * y + e[10] * z;

        return this.normalize();
    }

    public Vector3 divide(Vector3 v) {
        this.x /= v.x;
        this.y /= v.y;
        this.z /= v.z;
        return this;
    }

    public Vector3 divideScalar(double scalar) {
        return this.multiplyScalar(1 / scalar);
    }

    public Vector3 min(Vector3 v) {
        this.x = Math.min(this.x, v.x);
        this.y = Math.min(this.y, v.y);
        this.z = Math.min(this.z, v.z);
        return this;
    }

    public Vector3 max(Vector3 v) {
        this.x = Math.max(this.x, v.x);
        this.y = Math.max(this.y, v.y);
        this.z = Math.max(this.z, v.z);
        return this;
    }

    public Vector3 clamp(Vector3 min, Vector3 max) {
        // assumes min < max, componentwise
        this.x = Math.max(min.x, Math.min(max.x, this.x));
        this.y = Math.max(min.y, Math.min(max.y, this.y));
        this.z = Math.max(min.z, Math.min(max.z, this.z));
        return this;
    }

    public Vector3 floor() {
        this.x = Math.floor(this.x);
        this.y = Math.floor(this.y);
        this.z = Math.floor(this.z);
        return this;
    }

    public Vector3 ceil() {
        this.x = Math.ceil(this.x);
        this.y = Math.ceil(this.y);
        this.z = Math.ceil(this.z);
        return this;
    }

    public Vector3 negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        return this;
    }

    public double dot(Vector3 v) {
        return this.x * v.x + this.y * v.y + this.z * v.z;
    }

    public double lengthSq() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public double manhattanLength() {
        return Math.abs(this.x) + Math.abs(this.y) + Math.abs(this.z);
    }

    public Vector3 normalize() {
        double length = this.length();
        return this.divideScalar(length == 0 ? 1 : length);
    }

    public Vector3 setLength(double length) {
        return this.normalize().multiplyScalar(length);
    }

    public Vector3 lerp(Vector3 v, double alpha) {
        this.x += (v.x - this.x) * alpha;
        this.y += (v.y - this.y) * alpha;
        this.z += (v.z - this.z) * alpha;
        return this;
    }

    public Vector3 lerpVectors(Vector3 v1, Vector3 v2, double alpha) {
        this.x = v1.x + (v2.x - v1.x) * alpha;
        this.y = v1.y + (v2.y - v1.y) * alpha;
        this.z = v1.z + (v2.z - v1.z) * alpha;
        return this;
    }

    public Vector3 cross(Vector3 v) {
        return this.crossVectors(this, v);
    }

    public Vector3 crossVectors(Vector3 a, Vector3 b) {
        double ax = a.x, ay = a.y, az = a.z;
        double bx = b.x, by = b.y, bz = b.z;

        this.x = ay * bz - az * by;
        this.y = az * bx - ax * bz;
        this.z = ax * by - ay * bx;

        return this;
    }

    public Vector3 projectOnVector(Vector3 v) {
        double denominator = v.lengthSq();
        if (denominator == 0) return this.set(0, 0, 0);
        double scalar = v.dot(this) / denominator;
        return this.copy(v).multiplyScalar(scalar);
    }

    public Vector3 projectOnPlane(Vector3 planeNormal) {
        Vector3 vector = this.clone().projectOnVector(planeNormal);
        return this.sub(vector);
    }

    public Vector3 reflect(Vector3 normal) {
        // reflect incident vector off plane orthogonal to normal
        // normal is assumed to have unit length
        return this.sub(normal.clone().multiplyScalar(2 * this.dot(normal)));
    }

    public double angleTo(Vector3 v) {
        double denominator = Math.sqrt(this.lengthSq() * v.lengthSq());
        if (denominator == 0) return Math.PI / 2;
        double theta = this.dot(v) / denominator;
        // clamp, to handle numerical problems
        return Math.acos(MathUtils.clamp(theta, -1, 1));
    }

    public double distanceTo(Vector3 v) {
        return Math.sqrt(this.distanceToSquared(v));
    }

    public double distanceToSquared(Vector3 v) {
        double dx = this.x - v.x, dy = this.y - v.y, dz = this.z - v.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public Vector3 setFromMatrixPosition(Matrix4 m) {
        double[] e = m.elements;

        this.x = e[12];
        this.y = e[13];
        this.z = e[14];

        return this;
    }

    public Vector3 setFromMatrixScale(Matrix4 m) {
        double sx = this.setFromMatrixColumn(m, 0).length();
        double sy = this.setFromMatrixColumn(m, 1).length();
        double sz = this.setFromMatrixColumn(m, 2).length();

        this.x = sx;
        this.y = sy;
        this.z = sz;

        return this;
    }

    public Vector3 setFromMatrixColumn(Matrix4 m, int index) {
        return this.fromArray(m.elements, index * 4);
    }

    public boolean equals(Vector3 v) {
        return (v.x == this.x) && (v.y == this.y) && (v.z == this.z);
    }

    public Vector3 fromArray(double[] array) {
        return fromArray(array, 0);
    }

    public Vector3 fromArray(double[] array, int offset) {
        this.x = array[offset];
        this.y = array[offset + 1];
        this.z = array[offset + 2];
        return this;
    }

    public double[] toArray() {
        return toArray(new double[3], 0);
    }

    public double[] toArray(double[] array, int offset) {
        array[offset] = this.x;
        array[offset + 1] = this.y;
        array[offset + 2] = this.z;
        return array;
    }

    @Override
    public String toString() {
        return "Vector3(" + x + ", " + y + ", " + z + ")";
    }
}

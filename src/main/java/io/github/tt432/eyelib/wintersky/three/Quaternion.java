package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 Quaternion 移植（含 x/y/z/w setter 的 onChange 回调机制）。
 *
 * <p>未移植项（wintersky 未触达）：静态 slerp/slerpFlat/multiplyQuaternionsFlat、
 * slerp/slerpQuaternions/rotateTowards/random/fromBufferAttribute。
 */
public class Quaternion {

    private double _x;
    private double _y;
    private double _z;
    private double _w;

    private Runnable onChangeCallback = () -> {
    };

    public Quaternion() {
        this(0, 0, 0, 1);
    }

    public Quaternion(double x, double y, double z, double w) {
        this._x = x;
        this._y = y;
        this._z = z;
        this._w = w;
    }

    public double getX() {
        return this._x;
    }

    public void setX(double value) {
        this._x = value;
        this.onChangeCallback.run();
    }

    public double getY() {
        return this._y;
    }

    public void setY(double value) {
        this._y = value;
        this.onChangeCallback.run();
    }

    public double getZ() {
        return this._z;
    }

    public void setZ(double value) {
        this._z = value;
        this.onChangeCallback.run();
    }

    public double getW() {
        return this._w;
    }

    public void setW(double value) {
        this._w = value;
        this.onChangeCallback.run();
    }

    public Quaternion set(double x, double y, double z, double w) {
        this._x = x;
        this._y = y;
        this._z = z;
        this._w = w;

        this.onChangeCallback.run();
        return this;
    }

    public Quaternion clone() {
        return new Quaternion(this._x, this._y, this._z, this._w);
    }

    public Quaternion copy(Quaternion quaternion) {
        this._x = quaternion._x;
        this._y = quaternion._y;
        this._z = quaternion._z;
        this._w = quaternion._w;

        this.onChangeCallback.run();
        return this;
    }

    public Quaternion setFromEuler(Euler euler) {
        return setFromEuler(euler, true);
    }

    public Quaternion setFromEuler(Euler euler, boolean update) {
        double x = euler.getX(), y = euler.getY(), z = euler.getZ();
        String order = euler.getOrder();

        // http://www.mathworks.com/matlabcentral/fileexchange/
        // 	20696-function-to-convert-between-dcm-euler-angles-quaternions-and-euler-vectors/
        //	content/SpinCalc.m

        double c1 = Math.cos(x / 2);
        double c2 = Math.cos(y / 2);
        double c3 = Math.cos(z / 2);

        double s1 = Math.sin(x / 2);
        double s2 = Math.sin(y / 2);
        double s3 = Math.sin(z / 2);

        switch (order) {
            case "XYZ" -> {
                this._x = s1 * c2 * c3 + c1 * s2 * s3;
                this._y = c1 * s2 * c3 - s1 * c2 * s3;
                this._z = c1 * c2 * s3 + s1 * s2 * c3;
                this._w = c1 * c2 * c3 - s1 * s2 * s3;
            }
            case "YXZ" -> {
                this._x = s1 * c2 * c3 + c1 * s2 * s3;
                this._y = c1 * s2 * c3 - s1 * c2 * s3;
                this._z = c1 * c2 * s3 - s1 * s2 * c3;
                this._w = c1 * c2 * c3 + s1 * s2 * s3;
            }
            case "ZXY" -> {
                this._x = s1 * c2 * c3 - c1 * s2 * s3;
                this._y = c1 * s2 * c3 + s1 * c2 * s3;
                this._z = c1 * c2 * s3 + s1 * s2 * c3;
                this._w = c1 * c2 * c3 - s1 * s2 * s3;
            }
            case "ZYX" -> {
                this._x = s1 * c2 * c3 - c1 * s2 * s3;
                this._y = c1 * s2 * c3 + s1 * c2 * s3;
                this._z = c1 * c2 * s3 - s1 * s2 * c3;
                this._w = c1 * c2 * c3 + s1 * s2 * s3;
            }
            case "YZX" -> {
                this._x = s1 * c2 * c3 + c1 * s2 * s3;
                this._y = c1 * s2 * c3 + s1 * c2 * s3;
                this._z = c1 * c2 * s3 - s1 * s2 * c3;
                this._w = c1 * c2 * c3 - s1 * s2 * s3;
            }
            case "XZY" -> {
                this._x = s1 * c2 * c3 - c1 * s2 * s3;
                this._y = c1 * s2 * c3 - s1 * c2 * s3;
                this._z = c1 * c2 * s3 + s1 * s2 * c3;
                this._w = c1 * c2 * c3 + s1 * s2 * s3;
            }
            default -> System.err.println(
                    "THREE.Quaternion: .setFromEuler() encountered an unknown order: " + order);
        }

        if (update) this.onChangeCallback.run();
        return this;
    }

    public Quaternion setFromAxisAngle(Vector3 axis, double angle) {
        // http://www.euclideanspace.com/maths/geometry/rotations/conversions/angleToQuaternion/index.htm
        // assumes axis is normalized
        double halfAngle = angle / 2, s = Math.sin(halfAngle);

        this._x = axis.x * s;
        this._y = axis.y * s;
        this._z = axis.z * s;
        this._w = Math.cos(halfAngle);

        this.onChangeCallback.run();
        return this;
    }

    public Quaternion setFromRotationMatrix(Matrix4 m) {
        // http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/index.htm
        // assumes the upper 3x3 of m is a pure rotation matrix (i.e, unscaled)
        double[] te = m.elements;

        double m11 = te[0], m12 = te[4], m13 = te[8];
        double m21 = te[1], m22 = te[5], m23 = te[9];
        double m31 = te[2], m32 = te[6], m33 = te[10];

        double trace = m11 + m22 + m33;

        if (trace > 0) {
            double s = 0.5 / Math.sqrt(trace + 1.0);

            this._w = 0.25 / s;
            this._x = (m32 - m23) * s;
            this._y = (m13 - m31) * s;
            this._z = (m21 - m12) * s;

        } else if (m11 > m22 && m11 > m33) {
            double s = 2.0 * Math.sqrt(1.0 + m11 - m22 - m33);

            this._w = (m32 - m23) / s;
            this._x = 0.25 * s;
            this._y = (m12 + m21) / s;
            this._z = (m13 + m31) / s;

        } else if (m22 > m33) {
            double s = 2.0 * Math.sqrt(1.0 + m22 - m11 - m33);

            this._w = (m13 - m31) / s;
            this._x = (m12 + m21) / s;
            this._y = 0.25 * s;
            this._z = (m23 + m32) / s;

        } else {
            double s = 2.0 * Math.sqrt(1.0 + m33 - m11 - m22);

            this._w = (m21 - m12) / s;
            this._x = (m13 + m31) / s;
            this._y = (m23 + m32) / s;
            this._z = 0.25 * s;
        }

        this.onChangeCallback.run();
        return this;
    }

    public Quaternion setFromUnitVectors(Vector3 vFrom, Vector3 vTo) {
        // assumes direction vectors vFrom and vTo are normalized
        double r = vFrom.dot(vTo) + 1;

        if (r < 2.220446049250313e-16 /* Number.EPSILON */) {
            // vFrom and vTo point in opposite directions
            r = 0;

            if (Math.abs(vFrom.x) > Math.abs(vFrom.z)) {
                this._x = -vFrom.y;
                this._y = vFrom.x;
                this._z = 0;
                this._w = r;
            } else {
                this._x = 0;
                this._y = -vFrom.z;
                this._z = vFrom.y;
                this._w = r;
            }
        } else {
            // crossVectors( vFrom, vTo ); // inlined to avoid cyclic dependency on Vector3
            this._x = vFrom.y * vTo.z - vFrom.z * vTo.y;
            this._y = vFrom.z * vTo.x - vFrom.x * vTo.z;
            this._z = vFrom.x * vTo.y - vFrom.y * vTo.x;
            this._w = r;
        }

        return this.normalize();
    }

    public double angleTo(Quaternion q) {
        return 2 * Math.acos(Math.abs(MathUtils.clamp(this.dot(q), -1, 1)));
    }

    public Quaternion identity() {
        return this.set(0, 0, 0, 1);
    }

    public Quaternion invert() {
        // quaternion is assumed to have unit length
        return this.conjugate();
    }

    public Quaternion conjugate() {
        this._x *= -1;
        this._y *= -1;
        this._z *= -1;

        this.onChangeCallback.run();
        return this;
    }

    public double dot(Quaternion v) {
        return this._x * v._x + this._y * v._y + this._z * v._z + this._w * v._w;
    }

    public double lengthSq() {
        return this._x * this._x + this._y * this._y + this._z * this._z + this._w * this._w;
    }

    public double length() {
        return Math.sqrt(this._x * this._x + this._y * this._y + this._z * this._z + this._w * this._w);
    }

    public Quaternion normalize() {
        double l = this.length();

        if (l == 0) {
            this._x = 0;
            this._y = 0;
            this._z = 0;
            this._w = 1;
        } else {
            l = 1 / l;

            this._x = this._x * l;
            this._y = this._y * l;
            this._z = this._z * l;
            this._w = this._w * l;
        }

        this.onChangeCallback.run();
        return this;
    }

    public Quaternion multiply(Quaternion q) {
        return this.multiplyQuaternions(this, q);
    }

    public Quaternion premultiply(Quaternion q) {
        return this.multiplyQuaternions(q, this);
    }

    public Quaternion multiplyQuaternions(Quaternion a, Quaternion b) {
        // from http://www.euclideanspace.com/maths/algebra/realNormedAlgebra/quaternions/code/index.htm
        double qax = a._x, qay = a._y, qaz = a._z, qaw = a._w;
        double qbx = b._x, qby = b._y, qbz = b._z, qbw = b._w;

        this._x = qax * qbw + qaw * qbx + qay * qbz - qaz * qby;
        this._y = qay * qbw + qaw * qby + qaz * qbx - qax * qbz;
        this._z = qaz * qbw + qaw * qbz + qax * qby - qay * qbx;
        this._w = qaw * qbw - qax * qbx - qay * qby - qaz * qbz;

        this.onChangeCallback.run();
        return this;
    }

    public boolean equals(Quaternion quaternion) {
        return (quaternion._x == this._x) && (quaternion._y == this._y)
                && (quaternion._z == this._z) && (quaternion._w == this._w);
    }

    public Quaternion fromArray(double[] array) {
        return fromArray(array, 0);
    }

    public Quaternion fromArray(double[] array, int offset) {
        this._x = array[offset];
        this._y = array[offset + 1];
        this._z = array[offset + 2];
        this._w = array[offset + 3];

        this.onChangeCallback.run();
        return this;
    }

    public double[] toArray() {
        return toArray(new double[4], 0);
    }

    public double[] toArray(double[] array, int offset) {
        array[offset] = this._x;
        array[offset + 1] = this._y;
        array[offset + 2] = this._z;
        array[offset + 3] = this._w;
        return array;
    }

    public Quaternion onChange(Runnable callback) {
        this.onChangeCallback = callback;
        return this;
    }

    @Override
    public String toString() {
        return "Quaternion(" + _x + ", " + _y + ", " + _z + ", " + _w + ")";
    }
}

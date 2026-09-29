package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 Euler 移植（含 onChange 回调机制与 reorder 语义）。
 */
public class Euler {

    public static final String DefaultOrder = "XYZ";
    public static final String[] RotationOrders = {"XYZ", "YZX", "ZXY", "XZY", "YXZ", "ZYX"};

    private double _x;
    private double _y;
    private double _z;
    private String _order;

    private Runnable onChangeCallback = () -> {
    };

    public Euler() {
        this(0, 0, 0, DefaultOrder);
    }

    public Euler(double x, double y, double z) {
        this(x, y, z, DefaultOrder);
    }

    public Euler(double x, double y, double z, String order) {
        this._x = x;
        this._y = y;
        this._z = z;
        this._order = order;
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

    public String getOrder() {
        return this._order;
    }

    public void setOrder(String value) {
        this._order = value;
        this.onChangeCallback.run();
    }

    public Euler set(double x, double y, double z) {
        return set(x, y, z, this._order);
    }

    public Euler set(double x, double y, double z, String order) {
        this._x = x;
        this._y = y;
        this._z = z;
        this._order = order;

        this.onChangeCallback.run();
        return this;
    }

    public Euler clone() {
        return new Euler(this._x, this._y, this._z, this._order);
    }

    public Euler copy(Euler euler) {
        this._x = euler._x;
        this._y = euler._y;
        this._z = euler._z;
        this._order = euler._order;

        this.onChangeCallback.run();
        return this;
    }

    public Euler setFromRotationMatrix(Matrix4 m, String order, boolean update) {
        // assumes the upper 3x3 of m is a pure rotation matrix (i.e, unscaled)
        double[] te = m.elements;
        double m11 = te[0], m12 = te[4], m13 = te[8];
        double m21 = te[1], m22 = te[5], m23 = te[9];
        double m31 = te[2], m32 = te[6], m33 = te[10];

        switch (order) {
            case "XYZ" -> {
                this._y = Math.asin(MathUtils.clamp(m13, -1, 1));

                if (Math.abs(m13) < 0.9999999) {
                    this._x = Math.atan2(-m23, m33);
                    this._z = Math.atan2(-m12, m11);
                } else {
                    this._x = Math.atan2(m32, m22);
                    this._z = 0;
                }
            }
            case "YXZ" -> {
                this._x = Math.asin(-MathUtils.clamp(m23, -1, 1));

                if (Math.abs(m23) < 0.9999999) {
                    this._y = Math.atan2(m13, m33);
                    this._z = Math.atan2(m21, m22);
                } else {
                    this._y = Math.atan2(-m31, m11);
                    this._z = 0;
                }
            }
            case "ZXY" -> {
                this._x = Math.asin(MathUtils.clamp(m32, -1, 1));

                if (Math.abs(m32) < 0.9999999) {
                    this._y = Math.atan2(-m31, m33);
                    this._z = Math.atan2(-m12, m22);
                } else {
                    this._y = 0;
                    this._z = Math.atan2(m21, m11);
                }
            }
            case "ZYX" -> {
                this._y = Math.asin(-MathUtils.clamp(m31, -1, 1));

                if (Math.abs(m31) < 0.9999999) {
                    this._x = Math.atan2(m32, m33);
                    this._z = Math.atan2(m21, m11);
                } else {
                    this._x = 0;
                    this._z = Math.atan2(-m12, m22);
                }
            }
            case "YZX" -> {
                this._z = Math.asin(MathUtils.clamp(m21, -1, 1));

                if (Math.abs(m21) < 0.9999999) {
                    this._x = Math.atan2(-m23, m22);
                    this._y = Math.atan2(-m31, m11);
                } else {
                    this._x = 0;
                    this._y = Math.atan2(m13, m33);
                }
            }
            case "XZY" -> {
                this._z = Math.asin(-MathUtils.clamp(m12, -1, 1));

                if (Math.abs(m12) < 0.9999999) {
                    this._x = Math.atan2(m32, m22);
                    this._y = Math.atan2(m13, m11);
                } else {
                    this._x = Math.atan2(-m23, m33);
                    this._y = 0;
                }
            }
            default -> System.err.println(
                    "THREE.Euler: .setFromRotationMatrix() encountered an unknown order: " + order);
        }

        this._order = order;

        if (update) this.onChangeCallback.run();
        return this;
    }

    public Euler setFromQuaternion(Quaternion q, String order, boolean update) {
        Matrix4 matrix = new Matrix4().makeRotationFromQuaternion(q);
        return this.setFromRotationMatrix(matrix, order, update);
    }

    public Euler setFromVector3(Vector3 v) {
        return setFromVector3(v, this._order);
    }

    public Euler setFromVector3(Vector3 v, String order) {
        return this.set(v.x, v.y, v.z, order);
    }

    public Euler reorder(String newOrder) {
        // WARNING: this discards revolution information -bhouston
        Quaternion quaternion = new Quaternion().setFromEuler(this);
        return this.setFromQuaternion(quaternion, newOrder, true);
    }

    public boolean equals(Euler euler) {
        return (euler._x == this._x) && (euler._y == this._y)
                && (euler._z == this._z) && (euler._order.equals(this._order));
    }

    public Euler fromArray(double[] array, String order) {
        this._x = array[0];
        this._y = array[1];
        this._z = array[2];
        if (order != null) this._order = order;

        this.onChangeCallback.run();
        return this;
    }

    public Object[] toArray() {
        return new Object[]{this._x, this._y, this._z, this._order};
    }

    public Vector3 toVector3() {
        return new Vector3(this._x, this._y, this._z);
    }

    public Vector3 toVector3(Vector3 optionalResult) {
        return optionalResult.set(this._x, this._y, this._z);
    }

    public Euler onChange(Runnable callback) {
        this.onChangeCallback = callback;
        return this;
    }

    @Override
    public String toString() {
        return "Euler(" + _x + ", " + _y + ", " + _z + ", " + _order + ")";
    }
}

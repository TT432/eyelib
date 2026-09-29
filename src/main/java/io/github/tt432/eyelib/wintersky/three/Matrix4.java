package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 Matrix4 移植（列主序 elements，长度 16）。
 *
 * <p>未移植项（wintersky 未触达）：extractBasis/makeBasis、makeRotationFromEuler、
 * makeTranslation/makeRotationX/Y/Z、makePerspective/makeOrthographic、
 * getMaxScaleOnAxis 之外的 shear、fromBufferAttribute 等。
 */
public class Matrix4 {

    public final double[] elements;

    public Matrix4() {
        this.elements = new double[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        };
    }

    public Matrix4 set(double n11, double n12, double n13, double n14,
                       double n21, double n22, double n23, double n24,
                       double n31, double n32, double n33, double n34,
                       double n41, double n42, double n43, double n44) {
        double[] te = this.elements;

        te[0] = n11; te[4] = n12; te[8] = n13; te[12] = n14;
        te[1] = n21; te[5] = n22; te[9] = n23; te[13] = n24;
        te[2] = n31; te[6] = n32; te[10] = n33; te[14] = n34;
        te[3] = n41; te[7] = n42; te[11] = n43; te[15] = n44;

        return this;
    }

    public Matrix4 identity() {
        this.set(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        );
        return this;
    }

    public Matrix4 clone() {
        return new Matrix4().fromArray(this.elements);
    }

    public Matrix4 copy(Matrix4 m) {
        double[] te = this.elements;
        double[] me = m.elements;

        System.arraycopy(me, 0, te, 0, 16);

        return this;
    }

    public Matrix4 copyPosition(Matrix4 m) {
        double[] te = this.elements, me = m.elements;

        te[12] = me[12];
        te[13] = me[13];
        te[14] = me[14];

        return this;
    }

    public Matrix4 extractRotation(Matrix4 m) {
        // this method does not support reflection matrices
        double[] te = this.elements;
        double[] me = m.elements;

        Vector3 v1 = new Vector3();

        double scaleX = 1 / v1.setFromMatrixColumn(m, 0).length();
        double scaleY = 1 / v1.setFromMatrixColumn(m, 1).length();
        double scaleZ = 1 / v1.setFromMatrixColumn(m, 2).length();

        te[0] = me[0] * scaleX;
        te[1] = me[1] * scaleX;
        te[2] = me[2] * scaleX;
        te[3] = 0;

        te[4] = me[4] * scaleY;
        te[5] = me[5] * scaleY;
        te[6] = me[6] * scaleY;
        te[7] = 0;

        te[8] = me[8] * scaleZ;
        te[9] = me[9] * scaleZ;
        te[10] = me[10] * scaleZ;
        te[11] = 0;

        te[12] = 0;
        te[13] = 0;
        te[14] = 0;
        te[15] = 1;

        return this;
    }

    public Matrix4 makeRotationFromQuaternion(Quaternion q) {
        return this.compose(new Vector3(0, 0, 0), q, new Vector3(1, 1, 1));
    }

    public Matrix4 lookAt(Vector3 eye, Vector3 target, Vector3 up) {
        double[] te = this.elements;

        Vector3 z = new Vector3().subVectors(eye, target);

        if (z.lengthSq() == 0) {
            // eye and target are in the same position
            z.z = 1;
        }

        z.normalize();
        Vector3 x = new Vector3().crossVectors(up, z);

        if (x.lengthSq() == 0) {
            // up and z are parallel
            if (Math.abs(up.z) == 1) {
                z.x += 0.0001;
            } else {
                z.z += 0.0001;
            }

            z.normalize();
            x.crossVectors(up, z);
        }

        x.normalize();
        Vector3 y = new Vector3().crossVectors(z, x);

        te[0] = x.x; te[4] = y.x; te[8] = z.x;
        te[1] = x.y; te[5] = y.y; te[9] = z.y;
        te[2] = x.z; te[6] = y.z; te[10] = z.z;

        return this;
    }

    public Matrix4 multiply(Matrix4 m) {
        return this.multiplyMatrices(this, m);
    }

    public Matrix4 premultiply(Matrix4 m) {
        return this.multiplyMatrices(m, this);
    }

    public Matrix4 multiplyMatrices(Matrix4 a, Matrix4 b) {
        double[] ae = a.elements;
        double[] be = b.elements;
        double[] te = this.elements;

        double a11 = ae[0], a12 = ae[4], a13 = ae[8], a14 = ae[12];
        double a21 = ae[1], a22 = ae[5], a23 = ae[9], a24 = ae[13];
        double a31 = ae[2], a32 = ae[6], a33 = ae[10], a34 = ae[14];
        double a41 = ae[3], a42 = ae[7], a43 = ae[11], a44 = ae[15];

        double b11 = be[0], b12 = be[4], b13 = be[8], b14 = be[12];
        double b21 = be[1], b22 = be[5], b23 = be[9], b24 = be[13];
        double b31 = be[2], b32 = be[6], b33 = be[10], b34 = be[14];
        double b41 = be[3], b42 = be[7], b43 = be[11], b44 = be[15];

        te[0] = a11 * b11 + a12 * b21 + a13 * b31 + a14 * b41;
        te[4] = a11 * b12 + a12 * b22 + a13 * b32 + a14 * b42;
        te[8] = a11 * b13 + a12 * b23 + a13 * b33 + a14 * b43;
        te[12] = a11 * b14 + a12 * b24 + a13 * b34 + a14 * b44;

        te[1] = a21 * b11 + a22 * b21 + a23 * b31 + a24 * b41;
        te[5] = a21 * b12 + a22 * b22 + a23 * b32 + a24 * b42;
        te[9] = a21 * b13 + a22 * b23 + a23 * b33 + a24 * b43;
        te[13] = a21 * b14 + a22 * b24 + a23 * b34 + a24 * b44;

        te[2] = a31 * b11 + a32 * b21 + a33 * b31 + a34 * b41;
        te[6] = a31 * b12 + a32 * b22 + a33 * b32 + a34 * b42;
        te[10] = a31 * b13 + a32 * b23 + a33 * b33 + a34 * b43;
        te[14] = a31 * b14 + a32 * b24 + a33 * b34 + a34 * b44;

        te[3] = a41 * b11 + a42 * b21 + a43 * b31 + a44 * b41;
        te[7] = a41 * b12 + a42 * b22 + a43 * b32 + a44 * b42;
        te[11] = a41 * b13 + a42 * b23 + a43 * b33 + a44 * b43;
        te[15] = a41 * b14 + a42 * b24 + a43 * b34 + a44 * b44;

        return this;
    }

    public Matrix4 multiplyScalar(double s) {
        double[] te = this.elements;

        for (int i = 0; i < 16; i++) {
            te[i] *= s;
        }

        return this;
    }

    public double determinant() {
        double[] te = this.elements;

        double n11 = te[0], n12 = te[4], n13 = te[8], n14 = te[12];
        double n21 = te[1], n22 = te[5], n23 = te[9], n24 = te[13];
        double n31 = te[2], n32 = te[6], n33 = te[10], n34 = te[14];
        double n41 = te[3], n42 = te[7], n43 = te[11], n44 = te[15];

        //TODO: make this more efficient
        //( based on http://www.euclideanspace.com/maths/algebra/matrix/functions/inverse/fourD/index.htm )

        return (
                n41 * (
                        +n14 * n23 * n32
                                - n13 * n24 * n32
                                - n14 * n22 * n33
                                + n12 * n24 * n33
                                + n13 * n22 * n34
                                - n12 * n23 * n34
                ) +
                        n42 * (
                                +n11 * n23 * n34
                                        - n11 * n24 * n33
                                        + n14 * n21 * n33
                                        - n13 * n21 * n34
                                        + n13 * n24 * n31
                                        - n14 * n23 * n31
                        ) +
                        n43 * (
                                +n11 * n24 * n32
                                        - n11 * n22 * n34
                                        - n14 * n21 * n32
                                        + n12 * n21 * n34
                                        + n14 * n22 * n31
                                        - n12 * n24 * n31
                        ) +
                        n44 * (
                                -n13 * n22 * n31
                                        - n11 * n23 * n32
                                        + n11 * n22 * n33
                                        + n13 * n21 * n32
                                        - n12 * n21 * n33
                                        + n12 * n23 * n31
                        )

        );
    }

    public Matrix4 transpose() {
        double[] te = this.elements;
        double tmp;

        tmp = te[1]; te[1] = te[4]; te[4] = tmp;
        tmp = te[2]; te[2] = te[8]; te[8] = tmp;
        tmp = te[6]; te[6] = te[9]; te[9] = tmp;

        tmp = te[3]; te[3] = te[12]; te[12] = tmp;
        tmp = te[7]; te[7] = te[13]; te[13] = tmp;
        tmp = te[11]; te[11] = te[14]; te[14] = tmp;

        return this;
    }

    public Matrix4 setPosition(double x, double y, double z) {
        double[] te = this.elements;

        te[12] = x;
        te[13] = y;
        te[14] = z;

        return this;
    }

    public Matrix4 invert() {
        // based on http://www.euclideanspace.com/maths/algebra/matrix/functions/inverse/fourD/index.htm
        double[] te = this.elements;

        double n11 = te[0], n21 = te[1], n31 = te[2], n41 = te[3],
                n12 = te[4], n22 = te[5], n32 = te[6], n42 = te[7],
                n13 = te[8], n23 = te[9], n33 = te[10], n43 = te[11],
                n14 = te[12], n24 = te[13], n34 = te[14], n44 = te[15],

                t11 = n23 * n34 * n42 - n24 * n33 * n42 + n24 * n32 * n43 - n22 * n34 * n43 - n23 * n32 * n44 + n22 * n33 * n44,
                t12 = n14 * n33 * n42 - n13 * n34 * n42 - n14 * n32 * n43 + n12 * n34 * n43 + n13 * n32 * n44 - n12 * n33 * n44,
                t13 = n13 * n24 * n42 - n14 * n23 * n42 + n14 * n22 * n43 - n12 * n24 * n43 - n13 * n22 * n44 + n12 * n23 * n44,
                t14 = n14 * n23 * n32 - n13 * n24 * n32 - n14 * n22 * n33 + n12 * n24 * n33 + n13 * n22 * n34 - n12 * n23 * n34;

        double det = n11 * t11 + n21 * t12 + n31 * t13 + n41 * t14;

        if (det == 0) return this.set(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        double detInv = 1 / det;

        te[0] = t11 * detInv;
        te[1] = (n24 * n33 * n41 - n23 * n34 * n41 - n24 * n31 * n43 + n21 * n34 * n43 + n23 * n31 * n44 - n21 * n33 * n44) * detInv;
        te[2] = (n22 * n34 * n41 - n24 * n32 * n41 + n24 * n31 * n42 - n21 * n34 * n42 - n22 * n31 * n44 + n21 * n32 * n44) * detInv;
        te[3] = (n23 * n32 * n41 - n22 * n33 * n41 - n23 * n31 * n42 + n21 * n33 * n42 + n22 * n31 * n43 - n21 * n32 * n43) * detInv;

        te[4] = t12 * detInv;
        te[5] = (n13 * n34 * n41 - n14 * n33 * n41 + n14 * n31 * n43 - n11 * n34 * n43 - n13 * n31 * n44 + n11 * n33 * n44) * detInv;
        te[6] = (n14 * n32 * n41 - n12 * n34 * n41 - n14 * n31 * n42 + n11 * n34 * n42 + n12 * n31 * n44 - n11 * n32 * n44) * detInv;
        te[7] = (n12 * n33 * n41 - n13 * n32 * n41 + n13 * n31 * n42 - n11 * n33 * n42 - n12 * n31 * n43 + n11 * n32 * n43) * detInv;

        te[8] = t13 * detInv;
        te[9] = (n14 * n23 * n41 - n13 * n24 * n41 - n14 * n21 * n43 + n11 * n24 * n43 + n13 * n21 * n44 - n11 * n23 * n44) * detInv;
        te[10] = (n12 * n24 * n41 - n14 * n22 * n41 + n14 * n21 * n42 - n11 * n24 * n42 - n12 * n21 * n44 + n11 * n22 * n44) * detInv;
        te[11] = (n13 * n22 * n41 - n12 * n23 * n41 - n13 * n21 * n42 + n11 * n23 * n42 + n12 * n21 * n43 - n11 * n22 * n43) * detInv;

        te[12] = t14 * detInv;
        te[13] = (n13 * n24 * n31 - n14 * n23 * n31 + n14 * n21 * n33 - n11 * n24 * n33 - n13 * n21 * n34 + n11 * n23 * n34) * detInv;
        te[14] = (n14 * n22 * n31 - n12 * n24 * n31 - n14 * n21 * n32 + n11 * n24 * n32 + n12 * n21 * n34 - n11 * n22 * n34) * detInv;
        te[15] = (n12 * n23 * n31 - n13 * n22 * n31 + n13 * n21 * n32 - n11 * n23 * n32 - n12 * n21 * n33 + n11 * n22 * n33) * detInv;

        return this;
    }

    public Matrix4 scale(Vector3 v) {
        double[] te = this.elements;
        double x = v.x, y = v.y, z = v.z;

        te[0] *= x; te[4] *= y; te[8] *= z;
        te[1] *= x; te[5] *= y; te[9] *= z;
        te[2] *= x; te[6] *= y; te[10] *= z;
        te[3] *= x; te[7] *= y; te[11] *= z;

        return this;
    }

    public double getMaxScaleOnAxis() {
        double[] te = this.elements;

        double scaleXSq = te[0] * te[0] + te[1] * te[1] + te[2] * te[2];
        double scaleYSq = te[4] * te[4] + te[5] * te[5] + te[6] * te[6];
        double scaleZSq = te[8] * te[8] + te[9] * te[9] + te[10] * te[10];

        return Math.sqrt(Math.max(scaleXSq, Math.max(scaleYSq, scaleZSq)));
    }

    public Matrix4 compose(Vector3 position, Quaternion quaternion, Vector3 scale) {
        double[] te = this.elements;

        double x = quaternion.getX(), y = quaternion.getY(), z = quaternion.getZ(), w = quaternion.getW();
        double x2 = x + x, y2 = y + y, z2 = z + z;
        double xx = x * x2, xy = x * y2, xz = x * z2;
        double yy = y * y2, yz = y * z2, zz = z * z2;
        double wx = w * x2, wy = w * y2, wz = w * z2;

        double sx = scale.x, sy = scale.y, sz = scale.z;

        te[0] = (1 - (yy + zz)) * sx;
        te[1] = (xy + wz) * sx;
        te[2] = (xz - wy) * sx;
        te[3] = 0;

        te[4] = (xy - wz) * sy;
        te[5] = (1 - (xx + zz)) * sy;
        te[6] = (yz + wx) * sy;
        te[7] = 0;

        te[8] = (xz + wy) * sz;
        te[9] = (yz - wx) * sz;
        te[10] = (1 - (xx + yy)) * sz;
        te[11] = 0;

        te[12] = position.x;
        te[13] = position.y;
        te[14] = position.z;
        te[15] = 1;

        return this;
    }

    public Matrix4 decompose(Vector3 position, Quaternion quaternion, Vector3 scale) {
        double[] te = this.elements;

        Vector3 v1 = new Vector3();

        double sx = v1.set(te[0], te[1], te[2]).length();
        double sy = v1.set(te[4], te[5], te[6]).length();
        double sz = v1.set(te[8], te[9], te[10]).length();

        // if determine is negative, we need to invert one scale
        double det = this.determinant();
        if (det < 0) sx = -sx;

        position.x = te[12];
        position.y = te[13];
        position.z = te[14];

        // scale the rotation part
        Matrix4 m1 = this.clone();

        double invSX = 1 / sx;
        double invSY = 1 / sy;
        double invSZ = 1 / sz;

        m1.elements[0] *= invSX;
        m1.elements[1] *= invSX;
        m1.elements[2] *= invSX;

        m1.elements[4] *= invSY;
        m1.elements[5] *= invSY;
        m1.elements[6] *= invSY;

        m1.elements[8] *= invSZ;
        m1.elements[9] *= invSZ;
        m1.elements[10] *= invSZ;

        quaternion.setFromRotationMatrix(m1);

        scale.x = sx;
        scale.y = sy;
        scale.z = sz;

        return this;
    }

    public boolean equals(Matrix4 matrix) {
        double[] te = this.elements;
        double[] me = matrix.elements;

        for (int i = 0; i < 16; i++) {
            if (te[i] != me[i]) return false;
        }

        return true;
    }

    public Matrix4 fromArray(double[] array) {
        return fromArray(array, 0);
    }

    public Matrix4 fromArray(double[] array, int offset) {
        System.arraycopy(array, offset, this.elements, 0, 16);
        return this;
    }

    public double[] toArray() {
        return toArray(new double[16], 0);
    }

    public double[] toArray(double[] array, int offset) {
        double[] te = this.elements;

        System.arraycopy(te, 0, array, offset, 16);

        return array;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Matrix4(");
        for (int i = 0; i < 16; i++) {
            if (i > 0) sb.append(", ");
            sb.append(elements[i]);
        }
        return sb.append(')').toString();
    }
}

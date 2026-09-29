package io.github.tt432.eyelib.wintersky.three;

import java.util.ArrayList;
import java.util.List;

/**
 * three.js r134 Object3D 移植。
 *
 * <p>保留：position/rotation/quaternion/scale（含 Euler↔Quaternion 双向同步回调）、
 * parent/children、matrix/matrixWorld、lookAt、localToWorld/worldToLocal、
 * getWorldPosition/getWorldQuaternion/getWorldScale/getWorldDirection、
 * updateMatrix/updateMatrixWorld/updateWorldMatrix、add/remove/clear/removeFromParent。
 *
 * <p>与 three 渲染管线一致：{@code localToWorld}/{@code worldToLocal} <b>不</b>隐式更新
 * matrixWorld（three 中由 renderer 每帧调用 {@code scene.updateMatrixWorld()} 刷新）。
 * 调用方需等价地先调用 {@link #updateMatrixWorld()}。
 *
 * <p>未移植项（渲染专用状态）：Layers、modelViewMatrix、normalMatrix、
 * castShadow/receiveShadow、frustumCulled、renderOrder、animations、userData 序列化、
 * raycast、traverse*、attach、toJSON、clone/copy。
 */
public class Object3D extends EventDispatcher {

    private static int object3DId = 0;

    public static final Vector3 DefaultUp = new Vector3(0, 1, 0);
    public static boolean DefaultMatrixAutoUpdate = true;

    public final int id = object3DId++;

    public String name = "";
    public String type = "Object3D";

    public Object3D parent = null;
    public final List<Object3D> children = new ArrayList<>();

    public final Vector3 up = DefaultUp.clone();

    public final Vector3 position;
    public final Euler rotation;
    public final Quaternion quaternion;
    public final Vector3 scale;

    public final Matrix4 matrix = new Matrix4();
    public final Matrix4 matrixWorld = new Matrix4();

    public boolean matrixAutoUpdate = DefaultMatrixAutoUpdate;
    public boolean matrixWorldNeedsUpdate = false;

    public boolean visible = true;

    /** three Mesh 标记（lookAt 区分 camera/light 与普通对象）。 */
    public boolean isMesh = false;

    private static final Event addedEvent = new Event("added");
    private static final Event removedEvent = new Event("removed");

    public Object3D() {
        Vector3 position = new Vector3();
        Euler rotation = new Euler();
        Quaternion quaternion = new Quaternion();
        Vector3 scale = new Vector3(1, 1, 1);

        rotation.onChange(() -> quaternion.setFromEuler(rotation, false));
        quaternion.onChange(() -> rotation.setFromQuaternion(quaternion, rotation.getOrder(), false));

        this.position = position;
        this.rotation = rotation;
        this.quaternion = quaternion;
        this.scale = scale;
    }

    public Object3D applyQuaternion(Quaternion q) {
        this.quaternion.premultiply(q);
        return this;
    }

    public Object3D setRotationFromAxisAngle(Vector3 axis, double angle) {
        // assumes axis is normalized
        this.quaternion.setFromAxisAngle(axis, angle);
        return this;
    }

    public Object3D setRotationFromEuler(Euler euler) {
        this.quaternion.setFromEuler(euler, true);
        return this;
    }

    public Object3D setRotationFromMatrix(Matrix4 m) {
        // assumes the upper 3x3 of m is a pure rotation matrix (i.e, unscaled)
        this.quaternion.setFromRotationMatrix(m);
        return this;
    }

    public Object3D setRotationFromQuaternion(Quaternion q) {
        // assumes q is normalized
        this.quaternion.copy(q);
        return this;
    }

    public Vector3 localToWorld(Vector3 vector) {
        return vector.applyMatrix4(this.matrixWorld);
    }

    public Vector3 worldToLocal(Vector3 vector) {
        return vector.applyMatrix4(this.matrixWorld.clone().invert());
    }

    public void lookAt(Vector3 target) {
        lookAtImpl(target);
    }

    public void lookAt(double x, double y, double z) {
        lookAtImpl(new Vector3(x, y, z));
    }

    private void lookAtImpl(Vector3 target) {
        // This method does not support objects having non-uniformly-scaled parent(s)
        Object3D parent = this.parent;

        this.updateWorldMatrix(true, false);

        Vector3 position = new Vector3().setFromMatrixPosition(this.matrixWorld);

        Matrix4 m1 = new Matrix4();
        // isCamera/isLight 不存在于 wintersky 使用面，普通对象走 else 分支
        m1.lookAt(target, position, this.up);

        this.quaternion.setFromRotationMatrix(m1);

        if (parent != null) {
            m1.extractRotation(parent.matrixWorld);
            Quaternion q1 = new Quaternion().setFromRotationMatrix(m1);
            this.quaternion.premultiply(q1.invert());
        }
    }

    public Object3D add(Object3D object) {
        if (object == this) {
            System.err.println("THREE.Object3D.add: object can't be added as a child of itself.");
            return this;
        }

        if (object != null) {
            if (object.parent != null) {
                object.parent.remove(object);
            }

            object.parent = this;
            this.children.add(object);

            object.dispatchEvent(addedEvent);
        } else {
            System.err.println("THREE.Object3D.add: object not an instance of THREE.Object3D.");
        }

        return this;
    }

    public Object3D remove(Object3D object) {
        int index = this.children.indexOf(object);

        if (index != -1) {
            object.parent = null;
            this.children.remove(index);

            object.dispatchEvent(removedEvent);
        }

        return this;
    }

    public Object3D removeFromParent() {
        Object3D parent = this.parent;

        if (parent != null) {
            parent.remove(this);
        }

        return this;
    }

    public Object3D clear() {
        for (int i = 0; i < this.children.size(); i++) {
            Object3D object = this.children.get(i);

            object.parent = null;

            object.dispatchEvent(removedEvent);
        }

        this.children.clear();

        return this;
    }

    public Vector3 getWorldPosition(Vector3 target) {
        this.updateWorldMatrix(true, false);
        return target.setFromMatrixPosition(this.matrixWorld);
    }

    public Quaternion getWorldQuaternion(Quaternion target) {
        this.updateWorldMatrix(true, false);
        this.matrixWorld.decompose(new Vector3(), target, new Vector3());
        return target;
    }

    public Vector3 getWorldScale(Vector3 target) {
        this.updateWorldMatrix(true, false);
        this.matrixWorld.decompose(new Vector3(), new Quaternion(), target);
        return target;
    }

    public Vector3 getWorldDirection(Vector3 target) {
        this.updateWorldMatrix(true, false);
        double[] e = this.matrixWorld.elements;
        return target.set(e[8], e[9], e[10]).normalize();
    }

    public void updateMatrix() {
        this.matrix.compose(this.position, this.quaternion, this.scale);
        this.matrixWorldNeedsUpdate = true;
    }

    public void updateMatrixWorld() {
        updateMatrixWorld(false);
    }

    public void updateMatrixWorld(boolean force) {
        if (this.matrixAutoUpdate) this.updateMatrix();

        if (this.matrixWorldNeedsUpdate || force) {
            if (this.parent == null) {
                this.matrixWorld.copy(this.matrix);
            } else {
                this.matrixWorld.multiplyMatrices(this.parent.matrixWorld, this.matrix);
            }

            this.matrixWorldNeedsUpdate = false;
            force = true;
        }

        // update children
        List<Object3D> children = this.children;

        for (int i = 0, l = children.size(); i < l; i++) {
            children.get(i).updateMatrixWorld(force);
        }
    }

    public void updateWorldMatrix(boolean updateParents, boolean updateChildren) {
        Object3D parent = this.parent;

        if (updateParents && parent != null) {
            parent.updateWorldMatrix(true, false);
        }

        if (this.matrixAutoUpdate) this.updateMatrix();

        if (this.parent == null) {
            this.matrixWorld.copy(this.matrix);
        } else {
            this.matrixWorld.multiplyMatrices(this.parent.matrixWorld, this.matrix);
        }

        // update children
        if (updateChildren) {
            List<Object3D> children = this.children;

            for (int i = 0, l = children.size(); i < l; i++) {
                children.get(i).updateWorldMatrix(false, true);
            }
        }
    }
}

package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 Sphere 移植（wintersky 用到的方法子集）。
 *
 * <p>注意：与 JS 一致，{@code center} 是<b>引用</b>而非拷贝——
 * wintersky 传入粒子 position 向量，碰撞检测读取的是实时位置。
 */
public class Sphere {

    public final Vector3 center;
    public double radius;

    public Sphere() {
        this(new Vector3(), -1);
    }

    public Sphere(Vector3 center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    public Sphere set(Vector3 center, double radius) {
        this.center.copy(center);
        this.radius = radius;
        return this;
    }

    public Sphere copy(Sphere sphere) {
        this.center.copy(sphere.center);
        this.radius = sphere.radius;
        return this;
    }

    public boolean isEmpty() {
        return (this.radius < 0);
    }

    public Vector3 getCenter(Vector3 target) {
        return target.copy(this.center);
    }

    public boolean intersectsPlane(Plane plane) {
        return Math.abs(plane.distanceToPoint(this.center)) <= this.radius;
    }

    public double distanceToPoint(Vector3 point) {
        return (this.center.distanceTo(point) - this.radius);
    }

    public boolean equals(Sphere sphere) {
        return sphere.center.equals(this.center) && (sphere.radius == this.radius);
    }

    public Sphere clone() {
        return new Sphere().copy(this);
    }
}

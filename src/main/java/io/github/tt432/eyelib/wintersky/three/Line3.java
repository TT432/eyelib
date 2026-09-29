package io.github.tt432.eyelib.wintersky.three;

/**
 * three.js r134 Line3 移植（wintersky 用到的方法子集）。
 */
public class Line3 {

    public final Vector3 start;
    public final Vector3 end;

    public Line3() {
        this(new Vector3(), new Vector3());
    }

    public Line3(Vector3 start, Vector3 end) {
        this.start = start;
        this.end = end;
    }

    public Line3 set(Vector3 start, Vector3 end) {
        this.start.copy(start);
        this.end.copy(end);
        return this;
    }

    public Line3 copy(Line3 line) {
        this.start.copy(line.start);
        this.end.copy(line.end);
        return this;
    }

    public Vector3 getCenter(Vector3 target) {
        return target.addVectors(this.start, this.end).multiplyScalar(0.5);
    }

    public Vector3 delta(Vector3 target) {
        return target.subVectors(this.end, this.start);
    }

    public double distanceSq() {
        return this.start.distanceToSquared(this.end);
    }

    public double distance() {
        return this.start.distanceTo(this.end);
    }

    public Vector3 at(double t, Vector3 target) {
        return this.delta(target).multiplyScalar(t).add(this.start);
    }

    public boolean equals(Line3 line) {
        return line.start.equals(this.start) && line.end.equals(this.end);
    }

    public Line3 clone() {
        return new Line3().copy(this);
    }
}

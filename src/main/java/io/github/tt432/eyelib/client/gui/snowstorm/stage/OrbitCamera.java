package io.github.tt432.eyelib.client.gui.snowstorm.stage;

import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.wintersky.three.MathUtils;
import io.github.tt432.eyelib.wintersky.three.Matrix4;
import io.github.tt432.eyelib.wintersky.three.Quaternion;
import io.github.tt432.eyelib.wintersky.three.Vector3;

/**
 * three.js r134 OrbitControls 核心数学 as-is 移植（PerspectiveCamera 路径），
 * 参数取 Snowstorm Preview.vue：target=(0,0.8,0)、zoomSpeed=1.4、screenSpacePanning=true、
 * 相机初始位置 (-6,3,-6)、FOV 45 / near 0.1 / far 3000。
 *
 * <p>输入由调用方以 delta 形式喂入（{@link #rotate}/{@link #dolly}/{@link #pan}），
 * 对应 JS 的 handleMouseMoveRotate / handleMouseWheel / handleMouseMovePan
 * （rotateStart/End 等拖拽起点状态由事件层维护，delta 语义等价）；DOM 事件、
 * pointer FSM、touch、EventDispatcher 不在本类。每次操作内部调用 {@link #update()}，
 * 与 JS 事件处理器行为一致。
 *
 * <p>与 JS 的差异（均不影响数学结果）：
 * <ul>
 *   <li>相机恒为 PerspectiveCamera：pan/dolly 的 OrthographicCamera 分支与
 *       「未知相机类型」降级分支不可达，未移植；zoomChanged 恒 false（仅 ortho 置位）。</li>
 *   <li>object.lookAt(target) 走 three 的 isCamera 分支（wintersky Object3D.lookAt 仅实现
 *       普通对象分支），此处内联 {@code m1.lookAt(position, target, up)}；无 parent，
 *       parent 旋转补偿分支省略。</li>
 *   <li>object.matrix 在 {@link #update()} 末尾 compose（three 由 renderer 每帧 updateMatrix；
 *       pan 读取的是上一次 update 的矩阵，两侧时序一致）。初始为单位矩阵（同 three）。</li>
 *   <li>autoRotate 的 {@code state === STATE.NONE} 条件：无拖拽 FSM，state 恒 NONE，
 *       即 autoRotate 始终生效（Snowstorm autoRotate=false，不可达）。</li>
 *   <li>键盘箭头 pan（handleKeyDown）未移植——数学同为 {@code pan()}，UI 阶段如需直接调
 *       {@link #pan} 即可（注意 JS 键盘路径不乘 panSpeed）。</li>
 *   <li>three r134 Spherical 未在 wintersky.three 中移植，按出处内嵌为私有静态类
 *       （three.js r134 src/math/Spherical.js + Vector3.setFromSphericalCoords）。</li>
 * </ul>
 */
public class OrbitCamera {

    // ============================== Snowstorm Preview.vue 常量 ==============================

    /** PerspectiveCamera(45, 16/9, 0.1, 3000)。 */
    public static final double FOV = 45;
    public static final double ASPECT = 16.0 / 9.0;
    public static final double NEAR = 0.1;
    public static final double FAR = 3000;
    /** View.camera.position.set(-6, 3, -6)。 */
    public static final double INITIAL_X = -6, INITIAL_Y = 3, INITIAL_Z = -6;
    /** View.controls.target.set(0, 0.8, 0)。 */
    public static final double TARGET_X = 0, TARGET_Y = 0.8, TARGET_Z = 0;

    private static final double EPS = 0.000001;
    private static final double TWO_PI = 2 * Math.PI;

    // ============================== OrbitControls 配置字段（as-is 默认值） ==============================

    /** Set to false to disable this control. */
    public boolean enabled = true;

    /** "target" sets the location of focus, where the object orbits around. */
    public final Vector3 target = new Vector3(TARGET_X, TARGET_Y, TARGET_Z);

    /** How far you can dolly in and out (PerspectiveCamera only). */
    public double minDistance = 0;
    public double maxDistance = Double.POSITIVE_INFINITY;

    /** How far you can orbit vertically. Range is 0 to Math.PI radians. */
    public double minPolarAngle = 0;
    public double maxPolarAngle = Math.PI;

    /** How far you can orbit horizontally. [min,max] ⊂ [-2π, 2π], max-min < 2π。 */
    public double minAzimuthAngle = Double.NEGATIVE_INFINITY;
    public double maxAzimuthAngle = Double.POSITIVE_INFINITY;

    /** Set to true to enable damping (inertia); Snowstorm 用 false（阻尼关闭）。 */
    public boolean enableDamping = false;
    public double dampingFactor = 0.05;

    /** Snowstorm: zoomSpeed = 1.4。 */
    public boolean enableZoom = true;
    public double zoomSpeed = 1.4;

    public boolean enableRotate = true;
    public double rotateSpeed = 1.0;

    /** Snowstorm: screenSpacePanning = true。 */
    public boolean enablePan = true;
    public double panSpeed = 1.0;
    public boolean screenSpacePanning = true;

    /** Snowstorm 未开启；保留 as-is。 */
    public boolean autoRotate = false;
    public double autoRotateSpeed = 2.0;

    // ============================== 相机状态（PerspectiveCamera 语义） ==============================

    /** object.position。 */
    public final Vector3 position = new Vector3(INITIAL_X, INITIAL_Y, INITIAL_Z);
    /** object.up（OrbitControls 的轨道轴，恒 (0,1,0)）。 */
    public final Vector3 up = new Vector3(0, 1, 0);
    /** object.quaternion（update() 的 lookAt 写入；舞台渲染的 view 旋转）。 */
    public final Quaternion quaternion = new Quaternion();
    /** object.matrix（compose(position, quaternion, 1)；pan 读取，three 由 renderer 维护）。 */
    public final Matrix4 matrix = new Matrix4();
    /** object.fov（pan 的 targetDistance 计算用）。 */
    public double fov = FOV;

    // ============================== 内部状态（JS closure 变量） ==============================

    /** JS const STATE；无拖拽 FSM，恒 NONE（见类文档）。 */
    private static final int STATE_NONE = -1;
    private int state = STATE_NONE;

    private final Spherical spherical = new Spherical();
    private final Spherical sphericalDelta = new Spherical();
    private double scale = 1;
    private final Vector3 panOffset = new Vector3();
    /** JS zoomChanged（仅 ortho 置位，perspective 恒 false，as-is 保留）。 */
    private boolean zoomChanged = false;

    private final Vector3 lastPosition = new Vector3();
    private final Quaternion lastQuaternion = new Quaternion();

    // update() 的临时量（JS 每次构造 update 闭包时分配一次）
    private final Vector3 offset = new Vector3();
    /** JS quat / quatInverse：构造时由 object.up 计算一次（up 恒 (0,1,0) → 单位四元数）。 */
    private final Quaternion quat = new Quaternion().setFromUnitVectors(up, new Vector3(0, 1, 0));
    private final Quaternion quatInverse = quat.clone().invert();
    private final Matrix4 lookAtMatrix = new Matrix4();

    // for reset（JS target0/position0；zoom0 为 ortho 语义，perspective 无 zoom 字段，略）
    private final Vector3 target0 = target.clone();
    private final Vector3 position0 = position.clone();

    // ============================== 公开方法（JS this.*） ==============================

    /** JS getPolarAngle()。 */
    public double getPolarAngle() {
        return spherical.phi;
    }

    /** JS getAzimuthalAngle()。 */
    public double getAzimuthalAngle() {
        return spherical.theta;
    }

    /** JS getDistance()。 */
    public double getDistance() {
        return position.distanceTo(target);
    }

    /** JS saveState()。 */
    public void saveState() {
        target0.copy(target);
        position0.copy(position);
    }

    /** JS reset()（无 zoom/updateProjectionMatrix/dispatchEvent）。 */
    public void reset() {
        target.copy(target0);
        position.copy(position0);
        update();
        state = STATE_NONE;
    }

    /**
     * JS update()：应用累积的旋转/缩放/平移，返回相机是否变化（JS 的 change 事件条件）。
     * 每次调用把当前位置写入 {@link EditorRuntime#cameraPosition} 接缝
     * （query.distance_from_camera 用）。
     */
    public boolean update() {
        offset.copy(position).sub(target);

        // rotate offset to "y-axis-is-up" space
        offset.applyQuaternion(quat);

        // angle from z-axis around y-axis
        spherical.setFromVector3(offset);

        if (autoRotate && state == STATE_NONE) {
            rotateLeft(getAutoRotationAngle());
        }

        if (enableDamping) {
            spherical.theta += sphericalDelta.theta * dampingFactor;
            spherical.phi += sphericalDelta.phi * dampingFactor;
        } else {
            spherical.theta += sphericalDelta.theta;
            spherical.phi += sphericalDelta.phi;
        }

        // restrict theta to be between desired limits
        double min = minAzimuthAngle;
        double max = maxAzimuthAngle;
        if (Double.isFinite(min) && Double.isFinite(max)) {
            if (min < -Math.PI) min += TWO_PI; else if (min > Math.PI) min -= TWO_PI;
            if (max < -Math.PI) max += TWO_PI; else if (max > Math.PI) max -= TWO_PI;
            if (min <= max) {
                spherical.theta = Math.max(min, Math.min(max, spherical.theta));
            } else {
                spherical.theta = (spherical.theta > (min + max) / 2)
                        ? Math.max(min, spherical.theta)
                        : Math.min(max, spherical.theta);
            }
        }

        // restrict phi to be between desired limits
        spherical.phi = Math.max(minPolarAngle, Math.min(maxPolarAngle, spherical.phi));

        spherical.makeSafe();

        spherical.radius *= scale;

        // restrict radius to be between desired limits
        spherical.radius = Math.max(minDistance, Math.min(maxDistance, spherical.radius));

        // move target to panned location
        if (enableDamping) {
            target.addScaledVector(panOffset, dampingFactor);
        } else {
            target.add(panOffset);
        }

        setFromSpherical(offset, spherical);

        // rotate offset back to "camera-up-vector-is-up" space
        offset.applyQuaternion(quatInverse);

        position.copy(target).add(offset);

        // object.lookAt(target) 的 isCamera 分支（wintersky Object3D 无相机分支，内联；无 parent）
        lookAtMatrix.lookAt(position, target, up);
        quaternion.setFromRotationMatrix(lookAtMatrix);

        // object.matrix：three 由 renderer updateMatrix 维护，此处 update 末尾同步
        matrix.compose(position, quaternion, new Vector3(1, 1, 1));

        // 接缝：query.distance_from_camera（JS 读 View.camera.position.length()）
        EditorRuntime.cameraPosition.copy(position);

        if (enableDamping) {
            sphericalDelta.theta *= (1 - dampingFactor);
            sphericalDelta.phi *= (1 - dampingFactor);
            panOffset.multiplyScalar(1 - dampingFactor);
        } else {
            sphericalDelta.set(0, 0, 0);
            panOffset.set(0, 0, 0);
        }

        scale = 1;

        // update condition is:
        // min(camera displacement, camera rotation in radians)^2 > EPS
        // using small-angle approximation cos(x/2) = 1 - x^2 / 8
        if (zoomChanged
                || lastPosition.distanceToSquared(position) > EPS
                || 8 * (1 - lastQuaternion.dot(quaternion)) > EPS) {
            lastPosition.copy(position);
            lastQuaternion.copy(quaternion);
            zoomChanged = false;
            return true;
        }

        return false;
    }

    /**
     * 视图矩阵（camera world matrix 的逆），供舞台渲染。
     *
     * @param out 输出矩阵（复用调用方分配）
     */
    public Matrix4 viewMatrix(Matrix4 out) {
        return out.copy(matrix).invert();
    }

    /** 当前相机位置（即 {@link #position}，update() 已同步进 EditorRuntime.cameraPosition）。 */
    public Vector3 currentCameraPosition() {
        return position;
    }

    // ============================== 输入入口（JS 事件处理器的 delta 等价物） ==============================

    /**
     * JS handleMouseMoveRotate：左拖旋转。deltaX/deltaY 为本次移动的像素增量
     * （右/下为正），clientHeight 为视口像素高（JS 用 element.clientHeight，yes, height）。
     * 含 onMouseMove 的 enabled/enableRotate 守卫。
     */
    public void rotate(double deltaX, double deltaY, double clientHeight) {
        if (!enabled || !enableRotate) return;
        // rotateDelta = delta * rotateSpeed
        rotateLeft(TWO_PI * deltaX * rotateSpeed / clientHeight);
        rotateUp(TWO_PI * deltaY * rotateSpeed / clientHeight);
        update();
    }

    /**
     * JS handleMouseWheel：滚轮缩放。deltaY &lt; 0 滚上（dollyIn），&gt; 0 滚下（dollyOut）。
     * 含 onMouseWheel 的 enabled/enableZoom 守卫。
     */
    public void dolly(double wheelDeltaY) {
        if (!enabled || !enableZoom) return;
        if (wheelDeltaY < 0) {
            dollyIn(getZoomScale());
        } else if (wheelDeltaY > 0) {
            dollyOut(getZoomScale());
        }
        update();
    }

    /**
     * JS handleMouseMovePan：右拖平移。deltaX/deltaY 为像素增量（右/下为正）。
     * 含 onMouseMove 的 enabled/enablePan 守卫；delta 乘 panSpeed（as-is）。
     */
    public void pan(double deltaX, double deltaY, double clientHeight) {
        if (!enabled || !enablePan) return;
        panImpl(deltaX * panSpeed, deltaY * panSpeed, clientHeight);
        update();
    }

    // ============================== 内部函数（JS internals） ==============================

    private double getAutoRotationAngle() {
        return TWO_PI / 60 / 60 * autoRotateSpeed;
    }

    private double getZoomScale() {
        return Math.pow(0.95, zoomSpeed);
    }

    private void rotateLeft(double angle) {
        sphericalDelta.theta -= angle;
    }

    private void rotateUp(double angle) {
        sphericalDelta.phi -= angle;
    }

    private final Vector3 panLeftV = new Vector3();

    /** JS panLeft(distance, objectMatrix)。 */
    private void panLeft(double distance, Matrix4 objectMatrix) {
        panLeftV.setFromMatrixColumn(objectMatrix, 0); // get X column of objectMatrix
        panLeftV.multiplyScalar(-distance);
        panOffset.add(panLeftV);
    }

    private final Vector3 panUpV = new Vector3();

    /** JS panUp(distance, objectMatrix)。 */
    private void panUp(double distance, Matrix4 objectMatrix) {
        if (screenSpacePanning) {
            panUpV.setFromMatrixColumn(objectMatrix, 1);
        } else {
            panUpV.setFromMatrixColumn(objectMatrix, 0);
            panUpV.crossVectors(up, panUpV);
        }
        panUpV.multiplyScalar(distance);
        panOffset.add(panUpV);
    }

    private final Vector3 panOffsetScratch = new Vector3();

    /**
     * JS pan(deltaX, deltaY)：deltaX/deltaY 为像素，右/下为正。
     * 仅 PerspectiveCamera 分支（ortho/未知相机分支不可达，见类文档）。
     */
    private void panImpl(double deltaX, double deltaY, double clientHeight) {
        // perspective
        panOffsetScratch.copy(position).sub(target);
        double targetDistance = panOffsetScratch.length();

        // half of the fov is center to top of screen
        targetDistance *= Math.tan((fov / 2) * Math.PI / 180.0);

        // we use only clientHeight here so aspect ratio does not distort speed
        panLeft(2 * deltaX * targetDistance / clientHeight, matrix);
        panUp(2 * deltaY * targetDistance / clientHeight, matrix);
    }

    /** JS dollyOut(dollyScale)（perspective 分支）。 */
    private void dollyOut(double dollyScale) {
        scale /= dollyScale;
    }

    /** JS dollyIn(dollyScale)（perspective 分支）。 */
    private void dollyIn(double dollyScale) {
        scale *= dollyScale;
    }

    /**
     * three.js r134 src/math/Spherical.js 内嵌移植（wintersky.three 未含 Spherical；
     * setFromVector3 及 Vector3.setFromSpherical/setFromSphericalCoords 语义并入本类）。
     */
    private static final class Spherical {

        double radius;
        double phi;   // polar angle
        double theta; // azimuthal angle

        Spherical() {
            this(1, 0, 0);
        }

        Spherical(double radius, double phi, double theta) {
            this.radius = radius;
            this.phi = phi;
            this.theta = theta;
        }

        Spherical set(double radius, double phi, double theta) {
            this.radius = radius;
            this.phi = phi;
            this.theta = theta;
            return this;
        }

        /** restrict phi to be between EPS and PI-EPS。 */
        Spherical makeSafe() {
            this.phi = Math.max(EPS, Math.min(Math.PI - EPS, this.phi));
            return this;
        }

        /** three Spherical.setFromVector3 / setFromCartesianCoords。 */
        Spherical setFromVector3(Vector3 v) {
            this.radius = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
            if (this.radius == 0) {
                this.theta = 0;
                this.phi = 0;
            } else {
                this.theta = Math.atan2(v.x, v.z);
                this.phi = Math.acos(MathUtils.clamp(v.y / this.radius, -1, 1));
            }
            return this;
        }
    }

    /** three Vector3.setFromSpherical / setFromSphericalCoords（wintersky Vector3 未含）。 */
    private static Vector3 setFromSpherical(Vector3 out, Spherical s) {
        double sinPhiRadius = Math.sin(s.phi) * s.radius;
        out.x = sinPhiRadius * Math.sin(s.theta);
        out.y = Math.cos(s.phi) * s.radius;
        out.z = sinPhiRadius * Math.cos(s.theta);
        return out;
    }
}

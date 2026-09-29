package io.github.tt432.eyelib.wintersky;

import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import io.github.tt432.eyelib.wintersky.molang.Molang;
import io.github.tt432.eyelib.wintersky.rng.WinterskyRandom;
import io.github.tt432.eyelib.wintersky.three.Constants;
import io.github.tt432.eyelib.wintersky.three.CubicBezierCurve;
import io.github.tt432.eyelib.wintersky.three.Curve;
import io.github.tt432.eyelib.wintersky.three.Object3D;
import io.github.tt432.eyelib.wintersky.three.Plane;
import io.github.tt432.eyelib.wintersky.three.Quaternion;
import io.github.tt432.eyelib.wintersky.three.ShaderMaterial;
import io.github.tt432.eyelib.wintersky.three.SplineCurve;
import io.github.tt432.eyelib.wintersky.three.Vector2;
import io.github.tt432.eyelib.wintersky.three.Vector3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledFuture;

/**
 * wintersky emitter.js 逐字移植。
 */
public class Emitter extends EventClass {

    /** JS constructor options 对象。 */
    public static final class Options {
        public @Nullable String loop_mode;
        public @Nullable String parent_mode;
        public @Nullable Boolean ground_collision;
        /** 透传给 Config（JS options.path）。 */
        public @Nullable String path;
    }

    private static final Vector3 DUMMY_VEC = new Vector3();
    private static final Object3D DUMMY_OBJECT = new Object3D();
    private static final List<String> MATERIAL_TYPES = List.of(
            "particles_alpha", "particles_opaque", "particles_blend", "particles_add");

    // ==================================================================
    // 曲线（emitter.js 顶层函数）
    // ==================================================================

    private static @Nullable Curve createCurveSpline(Config.Curve curve) {
        switch (curve.mode) {
            case "catmull_rom" -> {
                List<Vector2> vectors = new ArrayList<>();
                for (int i = 0; i < curve.nodes.size(); i++) {
                    vectors.add(new Vector2(i - 1, ((Number) curve.nodes.get(i)).doubleValue()));
                }
                return new SplineCurve(vectors);
            }
            case "bezier" -> {
                List<Vector2> vectors = new ArrayList<>();
                for (int i = 0; i < curve.nodes.size(); i++) {
                    vectors.add(new Vector2(i / 3.0, ((Number) curve.nodes.get(i)).doubleValue()));
                }
                while (vectors.size() < 4) {
                    vectors.add(new Vector2(Double.NaN, Double.NaN));
                }
                return new CubicBezierCurve(vectors.get(0), vectors.get(1), vectors.get(2), vectors.get(3));
            }
            default -> {
                return null;
            }
        }
    }

    static double calculateCurve(Emitter emitter, Config.Curve curve, String curveKey,
                                 @Nullable Map<String, Object> params) {
        double position = emitter.Molang.parse(curve.input, params);
        double range = emitter.Molang.parse(curve.range, params);
        if ("bezier_chain".equals(curve.mode)) range = 1;

        position = orZero(position / range);
        if (position == Double.POSITIVE_INFINITY) position = 0;

        switch (curve.mode) {
            case "linear" -> {
                int segments = curve.nodes.size() - 1;
                position *= segments;
                int index = (int) Math.floor(position);
                double blend = position % 1;
                // JS 越界索引得 undefined → NaN 传播
                double nodeA = nodeAt(curve.nodes, index);
                double nodeB = nodeAt(curve.nodes, index + 1);
                double difference = nodeB - nodeA;
                return nodeA + difference * blend;
            }
            case "catmull_rom" -> {
                Curve spline = emitter._cached_curves.get(curveKey);
                if (spline == null) {
                    spline = java.util.Objects.requireNonNull(createCurveSpline(curve));
                    emitter._cached_curves.put(curveKey, spline);
                }
                int segments = curve.nodes.size() - 3;
                position *= segments;
                double pso = (position + 1) / (segments + 2);
                return spline.getPoint(pso).y;
            }
            case "bezier" -> {
                Curve spline = emitter._cached_curves.get(curveKey);
                if (spline == null) {
                    spline = java.util.Objects.requireNonNull(createCurveSpline(curve));
                    emitter._cached_curves.put(curveKey, spline);
                }
                return spline.getPoint(position).y;
            }
            case "bezier_chain" -> {
                List<Config.BezierNode> sortedNodes = new ArrayList<>();
                for (Object node : curve.nodes) {
                    sortedNodes.add((Config.BezierNode) node);
                }
                sortedNodes.sort(Comparator.comparingDouble(a -> a.time));
                int i = 0;
                while (i < sortedNodes.size()) {
                    if (sortedNodes.get(i).time > position) break;
                    i++;
                }
                Config.BezierNode before = i > 0 ? sortedNodes.get(i - 1) : null;
                Config.BezierNode after = i < sortedNodes.size() ? sortedNodes.get(i) : null;

                double beforeTime = before != null ? before.time : 0;
                double beforeRightValue = before != null ? before.right_value : 0;
                double beforeRightSlope = before != null ? before.right_slope : 0;
                double afterTime = after != null ? after.time : 1;
                double afterLeftValue = after != null ? after.left_value : 0;
                double afterLeftSlope = after != null ? after.left_slope : 0;

                double timeDiff = afterTime - beforeTime;
                CubicBezierCurve spline = new CubicBezierCurve(
                        new Vector2(beforeTime + timeDiff * (0 / 3.0), beforeRightValue),
                        new Vector2(beforeTime + timeDiff * (1 / 3.0), beforeRightValue + beforeRightSlope * (1 / 3.0)),
                        new Vector2(beforeTime + timeDiff * (2 / 3.0), afterLeftValue - afterLeftSlope * (1 / 3.0)),
                        new Vector2(beforeTime + timeDiff * (3 / 3.0), afterLeftValue));
                return spline.getPoint((position - beforeTime) / timeDiff).y;
            }
            default -> {
                return 0;
            }
        }
    }

    /** JS array[index]：越界/负索引 → undefined → NaN。 */
    private static double nodeAt(List<Object> nodes, int index) {
        if (index < 0 || index >= nodes.size()) return Double.NaN;
        Object v = nodes.get(index);
        return v instanceof Number n ? n.doubleValue() : Double.NaN;
    }

    /** JS {@code x || 0}（0/NaN → 0；Infinity 保留，由调用处处理）。 */
    private static double orZero(double v) {
        return (v == 0 || Double.isNaN(v)) ? 0 : v;
    }

    // ==================================================================
    // 字段
    // ==================================================================

    public final Scene scene;
    public final List<Emitter> child_emitters = new ArrayList<>();
    public final Config config;
    public final Molang Molang = new Molang();
    public final Object3D local_space = new Object3D();
    public final Object3D global_space = new Object3D();
    public @Nullable ShaderMaterial material;
    public final List<Particle> particles = new ArrayList<>();
    public final List<Particle> dead_particles = new ArrayList<>();
    public double creation_time = 0;
    public @Nullable Emitter parent_emitter = null;
    public double age = 0;
    public double view_age = 0;
    public boolean enabled = false;
    public boolean initialized;
    public boolean paused;
    public String loop_mode = "";
    public String parent_mode = "";
    public boolean ground_collision;
    public @Nullable Vector3 inherited_particle_speed = null;
    public @Nullable String pre_effect_expression = null;
    public double[] random_vars = new double[4];
    public final Map<String, Object> tick_values = new LinkedHashMap<>();
    public final Map<String, Object> creation_values = new LinkedHashMap<>();
    public final Map<String, Curve> _cached_curves = new HashMap<>();
    public double active_time;
    public double sleep_time;
    private @Nullable ScheduledFuture<?> tick_interval;

    public Emitter(Scene scene, Config config) {
        this(scene, config, new Options());
    }

    public Emitter(Scene scene, Config config, Options options) {
        this.scene = scene;
        scene.emitters.add(this);
        this.config = config;
        finishConstruction(scene, options);
    }

    public Emitter(Scene scene, Map<String, Object> config, Options options) {
        this.scene = scene;
        scene.emitters.add(this);
        this.config = new Config(scene, config, options.path);
        finishConstruction(scene, options);
    }

    private void finishConstruction(Scene scene, Options options) {
        this.Molang.variableHandler = (key, params, args) -> {
            Config.Curve curve = this.config.curves.get(key);
            return curve != null ? calculateCurve(this, curve, key, params) : null;
        };

        double globalScale = scene.global_options._scale;
        this.local_space.scale.set(globalScale, globalScale, globalScale);
        this.global_space.scale.set(globalScale, globalScale, globalScale);

        ShaderMaterial material = new ShaderMaterial();
        material.uniforms.put("map", new ShaderMaterial.Uniform("t", this.config.texture));
        material.uniforms.put("materialType", new ShaderMaterial.Uniform("int", 1));
        material.vertexShader = Shaders.VERTEX;
        material.fragmentShader = Shaders.FRAGMENT;
        material.vertexColors = true;
        material.transparent = true;
        material.setAlphaTest(0.2);
        this.material = material;

        String loopMode = options.loop_mode;
        String parentMode = options.parent_mode;
        this.loop_mode = loopMode != null && !loopMode.isEmpty() ? loopMode : scene.global_options.loop_mode;
        this.parent_mode = parentMode != null && !parentMode.isEmpty() ? parentMode : scene.global_options.parent_mode;
        this.ground_collision = options.ground_collision != null ? options.ground_collision : scene.global_options.ground_collision;
        for (int i = 0; i < 4; i++) {
            this.random_vars[i] = WinterskyRandom.nextDouble();
        }

        this.updateMaterial();
    }

    public Object3D getActiveSpace() {
        if (this.config.space_local_position && this.local_space.parent != null) {
            // Add the particle to the local space object if local space is enabled and used
            return this.local_space;
        } else {
            // Otherwise add to global space
            return this.global_space;
        }
    }

    public Emitter clone() {
        Emitter clone = new Emitter(this.scene, this.config);
        clone.loop_mode = this.loop_mode;
        return clone;
    }

    public Map<String, Object> params() {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("variable.entity_scale", 1.0);
        obj.put("variable.emitter_lifetime", this.active_time);
        obj.put("variable.emitter_age", this.age);
        obj.put("variable.emitter_random_1", this.random_vars[0]);
        obj.put("variable.emitter_random_2", this.random_vars[1]);
        obj.put("variable.emitter_random_3", this.random_vars[2]);
        obj.put("variable.emitter_random_4", this.random_vars[3]);
        return obj;
    }

    public @Nullable Object calculate(@Nullable Object input, @Nullable Map<String, Object> variables,
                                      @Nullable String datatype) {
        Object data = null;

        if (input instanceof List<?> list) {
            if ("array".equals(datatype)) {
                List<Double> arr = new ArrayList<>();
                for (Object source : list) {
                    arr.add(getV(source, variables));
                }
                data = arr;
            } else if (list.size() == 4) {
                data = new Plane().setComponents(
                        getV(list.get(0), variables),
                        getV(list.get(1), variables),
                        getV(list.get(2), variables),
                        getV(list.get(3), variables));
            } else if (list.size() == 3) {
                data = new Vector3(
                        getV(list.get(0), variables),
                        getV(list.get(1), variables),
                        getV(list.get(2), variables));
            } else if (list.size() == 2) {
                data = new Vector2(
                        getV(list.get(0), variables),
                        getV(list.get(1), variables));
            }
        } else if ("color".equals(datatype)) {
            // JS 空分支，data 保持 undefined
        } else {
            data = getV(input, variables);
        }
        return data;
    }

    public @Nullable Object calculate(@Nullable Object input, @Nullable Map<String, Object> variables) {
        return calculate(input, variables, null);
    }

    /** calculate 标量路径返回值（JS 恒为 number）。 */
    private static double num(@Nullable Object v) {
        return v instanceof Number n ? n.doubleValue() : Double.NaN;
    }

    /** JS {@code v => this.Molang.parse(v, variables)}。 */
    private double getV(@Nullable Object v, @Nullable Map<String, Object> variables) {
        if (v instanceof Number n) {
            return this.Molang.parse(n.doubleValue());
        }
        return this.Molang.parse((String) v, variables);
    }

    public void updateConfig() {
        this.updateMaterial();
    }

    public void updateFacingRotation(Object3D camera) {
        if (this.particles.isEmpty()) return;
        if (this.config == null) return;

        Quaternion quat = new Quaternion();
        Vector3 vec = new Vector3();

        Quaternion worldQuatInverse;
        // JS：substring(0, 6) == 'rotate' || true —— 恒真，as-is 保留
        if (this.config.particle_appearance_facing_camera_mode.substring(0, Math.min(6, this.config.particle_appearance_facing_camera_mode.length())).equals("rotate") || true) {
            Object3D parent = this.particles.get(0).mesh.parent;
            if (parent == null) return;
            worldQuatInverse = parent.getWorldQuaternion(quat).invert();
        } else {
            worldQuatInverse = quat;
        }

        for (Particle p : this.particles) {
            if (this.config.particle_appearance_facing_camera_mode.startsWith("direction")) {
                if (!"YXZ".equals(p.mesh.rotation.getOrder())) {
                    p.mesh.rotation.setOrder("YXZ");
                }
                vec.copy(p.facing_direction);

                if (vec.y == 1) {
                    vec.y = -1;
                } else if (vec.y == -1) {
                    vec.y = 1;
                    vec.z = -0.00001;
                }
            }
            if ("lookat_direction".equals(this.config.particle_appearance_facing_camera_mode)) {
                if (!"XYZ".equals(p.mesh.rotation.getOrder())) {
                    p.mesh.rotation.setOrder("XYZ");
                }
                vec.copy(p.facing_direction);
            }

            switch (this.config.particle_appearance_facing_camera_mode) {
                case "lookat_xyz" -> p.mesh.lookAt(camera.position);
                case "lookat_y" -> {
                    Vector3 v = vec.copy(camera.position);
                    DUMMY_VEC.set(0, 0, 0);
                    p.mesh.localToWorld(DUMMY_VEC);
                    v.y = DUMMY_VEC.y;
                    p.mesh.lookAt(v);
                }
                case "rotate_xyz" -> {
                    p.mesh.rotation.copy(camera.rotation);
                    p.mesh.quaternion.premultiply(worldQuatInverse);
                }
                case "rotate_y" -> {
                    p.mesh.rotation.copy(camera.rotation);
                    p.mesh.rotation.reorder("YXZ");
                    p.mesh.rotation.setX(0);
                    p.mesh.rotation.setZ(0);
                    p.mesh.quaternion.premultiply(worldQuatInverse);
                }
                case "direction_x" -> {
                    double y = Math.atan2(vec.x, vec.z);
                    double z = Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)));
                    p.mesh.rotation.set(0, y - Math.PI / 2, z);
                }
                case "direction_y" -> {
                    double y = Math.atan2(vec.x, vec.z);
                    double x = Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)));
                    p.mesh.rotation.set(x - Math.PI / 2, y - Math.PI, 0);
                }
                case "direction_z" -> {
                    double y = Math.atan2(vec.x, vec.z);
                    double x = Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)));
                    p.mesh.rotation.set(-x, y, 0);
                }
                case "lookat_direction" -> {
                    DUMMY_OBJECT.position.copy(p.mesh.position);
                    DUMMY_OBJECT.quaternion.setFromUnitVectors(MathUtil.Normals.x, vec);
                    vec.copy(camera.position);
                    Object3D parent = p.mesh.parent;
                    if (parent != null) {
                        parent.add(DUMMY_OBJECT);
                        DUMMY_OBJECT.updateMatrixWorld(true);
                        DUMMY_OBJECT.worldToLocal(vec);
                        parent.remove(DUMMY_OBJECT);
                    }
                    p.mesh.rotation.set(Math.atan2(-vec.y, vec.z), 0, 0, "XYZ");
                    p.mesh.quaternion.premultiply(DUMMY_OBJECT.quaternion);
                }
                case "emitter_transform_xy" -> p.mesh.rotation.set(0, 0, 0);
                case "emitter_transform_xz" -> p.mesh.rotation.set(-Math.PI / 2, 0, 0);
                case "emitter_transform_yz" -> p.mesh.rotation.set(0, Math.PI / 2, 0);
                default -> {
                }
            }
            p.mesh.rotation.setZ(p.mesh.rotation.getZ() + (p.rotation != 0 && !Double.isNaN(p.rotation) ? p.rotation : 0));
        }
    }

    // ==================================================================
    // Controls
    // ==================================================================

    public Emitter start() {
        this.age = 0;
        this.view_age = 0;
        this.enabled = true;
        this.initialized = true;
        this.scene.space.add(this.global_space);
        Map<String, Object> params = this.params();
        this.Molang.resetVariables();
        this.active_time = num(this.calculate(this.config.emitter_lifetime_active_time, params));
        this.sleep_time = num(this.calculate(this.config.emitter_lifetime_sleep_time, params));
        for (int i = 0; i < 4; i++) {
            this.random_vars[i] = WinterskyRandom.nextDouble();
        }
        this.creation_values.clear();

        for (Object line : this.config.variables_creation_vars) {
            this.Molang.parse((String) line, params);
        }
        if (this.pre_effect_expression != null) {
            this.Molang.parse(this.pre_effect_expression, params);
        }

        Map<String, Object> startData = new LinkedHashMap<>();
        startData.put("params", params);
        this.dispatchEvent("start", startData);

        this.updateMaterial();

        for (Object eventId : this.config.emitter_events_creation) {
            this.runEvent(eventId, null);
        }

        if ("instant".equals(this.config.emitter_rate_mode)) {
            this.spawnParticles(num(this.calculate(this.config.emitter_rate_amount, params)));
        } else if ("manual".equals(this.config.emitter_rate_mode)) {
            this.spawnParticles(1);
        }
        return this;
    }

    public Emitter tick(boolean jump) {
        Map<String, Object> params = this.params();
        int tickRate = this.scene.global_options.tick_rate;
        double step = 1.0 / tickRate;
        this._cached_curves.clear();

        // Calculate tick values
        for (Object line : this.config.variables_tick_vars) {
            this.Molang.parse((String) line, params);
        }
        if (!this.config.particle_update_expression.isEmpty()) {
            for (Particle p : this.particles) {
                Map<String, Object> particleParams = p.params();
                for (Object entry : this.config.particle_update_expression) {
                    this.Molang.parse((String) entry, particleParams);
                }
            }
        }
        Map<String, Object> tickData = new LinkedHashMap<>();
        tickData.put("params", params);
        this.dispatchEvent("tick", tickData);

        // Material
        if (!jump) {
            this.updateMaterial();
        }
        // Tick particles
        // JS forEach 语义：长度循环前快照、逐索引访问活列表；
        // p.tick 内 expire→remove 会跳过下一个粒子，as-is 复刻。
        int particleLen = this.particles.size();
        for (int i = 0; i < particleLen; i++) {
            if (i < this.particles.size()) {
                this.particles.get(i).tick(jump);
            }
        }

        double lastAge = this.age;
        this.age += step;
        this.view_age += step;
        // Spawn steady particles
        if (this.enabled && "steady".equals(this.config.emitter_rate_mode)) {
            double pThisTick = num(this.calculate(this.config.emitter_rate_rate, params)) / tickRate;
            double x = 1 / pThisTick;
            double cF = JsSemantics.jsRound(this.age * tickRate);
            if (cF % JsSemantics.jsRound(x) == 0) {
                pThisTick = Math.ceil(pThisTick);
            } else {
                pThisTick = Math.floor(pThisTick);
            }
            this.spawnParticles(pThisTick);
        }
        Map<String, Object> tickedData = new LinkedHashMap<>();
        tickedData.put("params", params);
        tickedData.put("tick_rate", (double) tickRate);
        this.dispatchEvent("ticked", tickedData);

        // Event timeline
        for (Map.Entry<String, Object> e : this.config.emitter_events_timeline.entrySet()) {
            double time = JsonValues.jsParseFloat(e.getKey());
            if (time >= lastAge && time < this.age) {
                this.runEvent(e.getValue(), null);
            }
        }

        // Child emitters
        for (Emitter e : new ArrayList<>(this.child_emitters)) {
            e.tick(jump);
        }

        if ("expression".equals(this.config.emitter_lifetime_mode)) {
            //Expressions
            if (this.enabled && JsSemantics.truthy(this.calculate(this.config.emitter_lifetime_expiration, params))) {
                this.end();
            }
            if (!this.enabled && JsSemantics.truthy(this.calculate(this.config.emitter_lifetime_activation, params))) {
                this.start();
            }
        } else if (this.parent_emitter == null && ("looping".equals(this.loop_mode)
                || ("auto".equals(this.loop_mode) && "looping".equals(this.config.emitter_lifetime_mode)))) {
            //Looping
            if (this.enabled && MathUtil.roundTo(this.age, 5) >= this.active_time) {
                this.end();
            }
            if (!this.enabled && MathUtil.roundTo(this.age, 5) >= this.sleep_time) {
                this.start();
            }
        } else {
            //Once
            if (this.enabled && MathUtil.roundTo(this.age, 5) >= this.active_time) {
                this.end();
            }
        }
        if (this.parent_emitter != null && this.particles.isEmpty() && this.age > this.active_time) {
            MathUtil.removeFromArray(this.parent_emitter.child_emitters, this);
            this.delete();
        }
        return this;
    }

    public Emitter tick() {
        return tick(false);
    }

    public Emitter stop() {
        return stop(false);
    }

    public Emitter stop(boolean clearScene) {
        this.enabled = false;
        this.age = 0;
        if (clearScene) {
            for (Particle particle : new ArrayList<>(this.particles)) {
                particle.remove();
            }
            for (Emitter e : new ArrayList<>(this.child_emitters)) {
                e.delete();
            }
            this.child_emitters.clear();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("clear_scene", clearScene);
        this.dispatchEvent("stop", data);
        return this;
    }

    /** dist 1.3.3 名为 {@code end()} 并 dispatch "end"（git master src 为 expire/"expire"）。 */
    public Emitter end() {
        this.stop();
        this.dispatchEvent("end", null);
        for (Object eventId : this.config.emitter_events_expiration) {
            this.runEvent(eventId, null);
        }
        return this;
    }

    public Emitter jumpTo(double second) {
        int tickRate = this.scene.global_options.tick_rate;
        double oldTime = JsSemantics.jsRound(this.view_age * tickRate);
        double newTime = JsSemantics.jsRound(second * tickRate);
        if ("looping".equals(this.loop_mode)
                || ("auto".equals(this.loop_mode) && "looping".equals(this.config.emitter_lifetime_mode))) {
            newTime = newTime % (JsSemantics.jsRound(this.active_time * tickRate) - 1);
        }
        if (oldTime == newTime) return this;
        if (newTime < oldTime) {
            this.stop(true).start();
        } else if (!this.initialized) {
            this.start();
        }
        double lastViewAge = this.view_age;
        while (JsSemantics.jsRound(this.view_age * tickRate) < newTime - 1) {
            this.tick(true);
            if (this.view_age <= lastViewAge) break;
            lastViewAge = this.view_age;
            if (this.material == null) return this;
        }
        this.tick(false);
        if (this.material == null) return this;
        for (Emitter e : new ArrayList<>(this.child_emitters)) {
            if (e.creation_time > second) {
                e.delete();
                MathUtil.removeFromArray(this.child_emitters, e);
            }
        }
        return this;
    }

    public void updateMaterial() {
        ShaderMaterial mat = this.material;
        if (mat == null) return;
        String materialName = this.config.particle_appearance_material;
        ShaderMaterial.Uniform uniform = mat.uniforms.get("materialType");
        if (uniform != null) {
            uniform.value = MATERIAL_TYPES.indexOf(materialName);
        }
        mat.side = ("particles_alpha".equals(materialName) || "particles_opaque".equals(materialName))
                ? Constants.FrontSide : Constants.DoubleSide;
        mat.blending = "particles_add".equals(materialName) ? Constants.AdditiveBlending : Constants.NormalBlending;
    }

    // ==================================================================
    // Playback Loop
    // ==================================================================

    public Emitter playLoop() {
        if (!this.initialized || this.age == 0) {
            this.start();
        }
        this.paused = false;
        IntervalTimer.clearInterval(this.tick_interval);
        this.tick_interval = IntervalTimer.setInterval(() -> this.tick(false), 1000.0 / this.scene.global_options.tick_rate);
        return this;
    }

    public Emitter toggleLoop() {
        this.paused = !this.paused;
        if (this.paused) {
            IntervalTimer.clearInterval(this.tick_interval);
            this.tick_interval = null;
        } else {
            this.playLoop();
        }
        return this;
    }

    public Emitter stopLoop() {
        IntervalTimer.clearInterval(this.tick_interval);
        this.tick_interval = null;
        this.stop(true);
        this.paused = true;
        return this;
    }

    /** JS spawnParticles：!count（含 NaN）时提前返回；返回实际 clamp 后数量。 */
    public double spawnParticles(double count) {
        if (count == 0 || Double.isNaN(count)) return 0;

        if ("steady".equals(this.config.emitter_rate_mode)) {
            double max = num(this.calculate(this.config.emitter_rate_maximum, this.params()));
            max = orZero(max);
            max = MathUtil.clamp(max, 0, this.scene.global_options.max_emitter_particles);
            count = MathUtil.clamp(count, 0, max - this.particles.size());
        } else {
            count = MathUtil.clamp(count, 0, this.scene.global_options.max_emitter_particles - this.particles.size());
        }
        for (int i = 0; i < count; i++) {
            Particle p;
            if (!this.dead_particles.isEmpty()) {
                p = this.dead_particles.remove(this.dead_particles.size() - 1);
            } else {
                p = new Particle(this);
            }
            p.add();
        }
        return count;
    }

    public void delete() {
        for (Emitter e : new ArrayList<>(this.child_emitters)) {
            e.delete();
        }
        this.child_emitters.clear();
        List<Particle> all = new ArrayList<>(this.particles);
        all.addAll(this.dead_particles);
        for (Particle particle : all) {
            particle.delete();
        }
        this.particles.clear();
        this.dead_particles.clear();
        if (this.local_space.parent != null) this.local_space.parent.remove(this.local_space);
        if (this.global_space.parent != null) this.global_space.parent.remove(this.global_space);
        MathUtil.removeFromArray(this.scene.emitters, this);
        if (this.material != null) {
            this.material.dispose();
        }
        this.material = null;
        this.parent_emitter = null;
    }

    // ==================================================================
    // Events
    // ==================================================================

    public void runEvent(@Nullable Object eventId, @Nullable Particle particle) {
        if (eventId instanceof List<?> list) {
            for (Object newId : list) {
                this.runEvent(newId, particle);
            }
            return;
        }
        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("event_id", eventId);
        eventData.put("particle", particle);
        this.dispatchEvent("event", eventData);

        Map<String, Object> event = JsonValues.asMap(this.config.events.get(eventId));
        if (event != null) {
            runEventSubpart(event, (String) eventId, particle);
        }
    }

    private void runEventSubpart(Map<String, Object> subpart, @Nullable String eventId, @Nullable Particle particle) {
        List<Object> sequence = JsonValues.asList(subpart.get("sequence"));
        if (sequence != null) {
            for (Object part2 : sequence) {
                Map<String, Object> partMap = JsonValues.asMap(part2);
                if (partMap != null) runEventSubpart(partMap, eventId, particle);
            }
        }
        List<Object> randomize = JsonValues.asList(subpart.get("randomize"));
        if (randomize != null) {
            List<Map<String, Object>> options = new ArrayList<>();
            for (Object o : randomize) {
                Map<String, Object> m = JsonValues.asMap(o);
                if (m != null) options.add(m);
            }
            Map<String, Object> pickedOption = MathUtil.getRandomFromWeightedList(options);
            if (pickedOption != null) runEventSubpart(pickedOption, eventId, particle);
        }

        // Run event
        Object expression = subpart.get("expression");
        if (expression != null) {
            this.Molang.parse((String) expression, this.params());
        }
        Object soundEffect = subpart.get("sound_effect");
        if (soundEffect != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("sound_effect", soundEffect);
            data.put("particle", particle);
            data.put("event_id", eventId);
            this.dispatchEvent("play_sound", data);
        }
        Map<String, Object> particleEffect = JsonValues.asMap(subpart.get("particle_effect"));
        if (particleEffect != null) {
            Object effectId = particleEffect.get("effect");
            String identifier = effectId != null ? JsSemantics.toJsString(effectId) : null;
            Config config = identifier != null ? this.scene.child_configs.get(identifier) : null;
            if (config == null && identifier != null && this.scene.hasFetchParticleFile()) {
                Config newConfig = new Config(this.scene);
                this.scene.child_configs.put(identifier, newConfig);
                Object result = this.scene.fetchParticleFile(identifier, this.config);
                if (result instanceof CompletionStage<?> stage) {
                    stage.thenAccept(r -> {
                        loadChildConfig(newConfig, r);
                        createChildEmitter(particleEffect, particle, eventId, newConfig);
                    });
                    return;
                } else if (result != null) {
                    loadChildConfig(newConfig, result);
                }
                config = newConfig;
            }
            createChildEmitter(particleEffect, particle, eventId, config);
        }
    }

    /** JS loadResult：{json, file_path} 或直接 config JSON（向后兼容）。 */
    @SuppressWarnings("unchecked")
    private void loadChildConfig(Config config, @Nullable Object result) {
        Map<String, Object> resultMap = JsonValues.asMap(result);
        if (resultMap == null) return;
        Object json = resultMap.get("json");
        if (json != null) {
            Object filePath = resultMap.get("file_path");
            config.file_path = filePath != null ? JsSemantics.toJsString(filePath) : null;
            Map<String, Object> jsonMap = JsonValues.asMap(json);
            config.setFromJSON(jsonMap != null ? jsonMap : resultMap);
        } else {
            // Backwards compatibility for API change
            config.setFromJSON(resultMap);
        }
    }

    private void createChildEmitter(Map<String, Object> particleEffect, @Nullable Particle particle,
                                    @Nullable String eventId, @Nullable Config config) {
        Emitter emitter = null;
        if (config != null) {
            emitter = new Emitter(this.scene, config, new Options());
            emitter.creation_time = this.age;
            emitter.parent_emitter = this;
            Object preEffect = particleEffect.get("pre_effect_expression");
            emitter.pre_effect_expression = preEffect != null ? JsSemantics.toJsString(preEffect) : null;
            this.child_emitters.add(emitter);

            Object type = particleEffect.get("type");
            if ("emitter_bound".equals(type)) {
                emitter.parent_mode = this.parent_mode;
            } else if ("particle_with_velocity".equals(type) && particle != null) {
                emitter.inherited_particle_speed = new Vector3().copy(particle.speed);
            }
            Vector3 position = new Vector3();
            if (particle != null) {
                particle.mesh.getWorldPosition(position);
            } else {
                this.getActiveSpace().getWorldPosition(position);
            }
            if (this.local_space.parent != null) {
                if (!this.config.space_local_position) {
                    Vector3 offset = this.local_space.getWorldPosition(new Vector3());
                    position.add(offset);
                }
            }
            emitter.getActiveSpace().position.copy(position);

            emitter.start();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("particle_effect", particleEffect);
        data.put("config", config);
        data.put("child_emitter", emitter);
        data.put("event_id", eventId);
        this.dispatchEvent("play_child_particle", data);
    }
}

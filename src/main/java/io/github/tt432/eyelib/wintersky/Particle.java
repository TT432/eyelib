package io.github.tt432.eyelib.wintersky;

import io.github.tt432.eyelib.wintersky.rng.WinterskyRandom;
import io.github.tt432.eyelib.wintersky.three.BufferAttribute;
import io.github.tt432.eyelib.wintersky.three.Float32Array;
import io.github.tt432.eyelib.wintersky.three.Line3;
import io.github.tt432.eyelib.wintersky.three.Mesh;
import io.github.tt432.eyelib.wintersky.three.Object3D;
import io.github.tt432.eyelib.wintersky.three.Plane;
import io.github.tt432.eyelib.wintersky.three.PlaneGeometry;
import io.github.tt432.eyelib.wintersky.three.Quaternion;
import io.github.tt432.eyelib.wintersky.three.ShaderMaterial;
import io.github.tt432.eyelib.wintersky.three.Sphere;
import io.github.tt432.eyelib.wintersky.three.Vector2;
import io.github.tt432.eyelib.wintersky.three.Vector3;
import io.github.tt432.eyelib.wintersky.tinycolor.TinyColor;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * wintersky particle.js 逐字移植。
 */
public class Particle {

    /**
     * JS defaultColor 字面量有重复 key（{r:255, r:255, b:255, a:1}），g 从未定义 → undefined。
     * as-is 复刻：g = NaN（undefined 参与除法的结果）。
     */
    private static final TinyColor.Rgba DEFAULT_COLOR = new TinyColor.Rgba(255, Double.NaN, 255, 1);

    private static final Plane COLLISION_PLANE = new Plane().setComponents(0, 1, 0, 0);

    @SuppressWarnings("unchecked")
    private static TinyColor.Rgba calculateGradient(List<Object> gradient, double percent) {
        int index = 0;
        for (int i = 0; i < gradient.size(); i++) {
            Map<String, Object> point = (Map<String, Object>) gradient.get(i);
            if (point != null && gradientPercent(point) <= percent) index = i;
        }
        Map<String, Object> current = index < gradient.size() ? (Map<String, Object>) gradient.get(index) : null;
        Map<String, Object> next = index + 1 < gradient.size() ? (Map<String, Object>) gradient.get(index + 1) : null;
        if (current != null && next == null) {
            return new TinyColor(gradientColor(current)).toRgb();
        } else if (current == null && next != null) {
            return new TinyColor(gradientColor(next)).toRgb();
        } else if (current != null && next != null) {
            // Interpolate
            double mix = (percent - gradientPercent(current))
                    / (gradientPercent(next) - gradientPercent(current));
            return TinyColor.mix(new TinyColor(gradientColor(current)),
                    new TinyColor(gradientColor(next)), mix * 100).toRgb();
        } else {
            return DEFAULT_COLOR;
        }
    }

    /** gradient 点的 percent 字段（结构良好的配置恒存在）。 */
    private static double gradientPercent(Map<String, Object> point) {
        return ((Number) java.util.Objects.requireNonNull(point.get("percent"))).doubleValue();
    }

    /** gradient 点的 color 字段（结构良好的配置恒存在）。 */
    private static String gradientColor(Map<String, Object> point) {
        return (String) java.util.Objects.requireNonNull(point.get("color"));
    }

    public final Emitter emitter;
    public final PlaneGeometry geometry;
    public final ShaderMaterial material;
    public final Mesh mesh;
    /** JS this.position = this.mesh.position（别名）。 */
    public final Vector3 position;

    /** 非 final：JS 在 direction 分支整体重赋值 this.speed。 */
    public Vector3 speed = new Vector3();
    public final Vector3 acceleration = new Vector3();
    public final Vector3 facing_direction = new Vector3();

    public double age;
    public double loop_time;
    public double current_frame;
    public double[] random_vars = new double[4];
    public double lifetime;
    public double initial_rotation;
    public double rotation_rate;
    public double rotation;

    public Particle(Emitter emitter) {
        this.emitter = emitter;

        this.geometry = new PlaneGeometry(2, 2);
        this.material = java.util.Objects.requireNonNull(this.emitter.material);
        this.mesh = new Mesh(this.geometry, this.material);
        this.position = this.mesh.position;

        Float32Array colors = new Float32Array(16);
        colors.fill(1);
        this.geometry.setAttribute("clr", new BufferAttribute(colors, 4));

        this.add();
    }

    public Map<String, Object> params() {
        Map<String, Object> obj = this.emitter.params();
        obj.put("variable.particle_lifetime", this.lifetime);
        obj.put("variable.particle_age", this.age);
        obj.put("variable.particle_random_1", this.random_vars[0]);
        obj.put("variable.particle_random_2", this.random_vars[1]);
        obj.put("variable.particle_random_3", this.random_vars[2]);
        obj.put("variable.particle_random_4", this.random_vars[3]);
        return obj;
    }

    public Particle add() {
        if (!this.emitter.particles.contains(this)) {
            this.emitter.particles.add(this);
            this.emitter.getActiveSpace().add(this.mesh);
        }

        this.age = this.loop_time = 0;
        this.current_frame = 0;
        for (int i = 0; i < 4; i++) {
            this.random_vars[i] = WinterskyRandom.nextDouble();
        }
        Map<String, Object> params = this.params();

        this.position.set(0, 0, 0);
        this.lifetime = num(this.emitter.calculate(this.emitter.config.particle_lifetime_max_lifetime, params));
        this.initial_rotation = num(this.emitter.calculate(this.emitter.config.particle_rotation_initial_rotation, params));
        this.rotation_rate = num(this.emitter.calculate(this.emitter.config.particle_rotation_rotation_rate, params));
        this.rotation = 0;

        //Init Position:
        boolean surface = this.emitter.config.emitter_shape_surface_only;
        if ("box".equals(this.emitter.config.emitter_shape_mode)) {
            Vector3 size = (Vector3) java.util.Objects.requireNonNull(
                    this.emitter.calculate(this.emitter.config.emitter_shape_half_dimensions, params));

            this.position.x = MathUtil.randomab(-size.x, size.x);
            this.position.y = MathUtil.randomab(-size.y, size.y);
            this.position.z = MathUtil.randomab(-size.z, size.z);

            if (surface) {
                int face = (int) Math.floor(MathUtil.randomab(0, 3));
                int side = (int) Math.floor(MathUtil.randomab(0, 2));
                this.position.setComponent(face, size.getComponent(face) * (side != 0 ? 1 : -1));
            }
        } else if ("entity_aabb".equals(this.emitter.config.emitter_shape_mode)) {
            Vector3 size = new Vector3(0.5, 1, 0.5);

            this.position.x = MathUtil.randomab(-size.x, size.x);
            this.position.y = MathUtil.randomab(-size.y, size.y);
            this.position.z = MathUtil.randomab(-size.z, size.z);

            if (surface) {
                int face = (int) Math.floor(MathUtil.randomab(0, 3));
                int side = (int) Math.floor(MathUtil.randomab(0, 2));
                this.position.setComponent(face, size.getComponent(face) * (side != 0 ? 1 : -1));
            }
        } else if ("sphere".equals(this.emitter.config.emitter_shape_mode)) {
            double radius = num(this.emitter.calculate(this.emitter.config.emitter_shape_radius, params));
            if (surface) {
                this.position.x = radius;
            } else {
                this.position.x = radius * WinterskyRandom.nextDouble();
            }
            this.position.applyEuler(MathUtil.getRandomEuler());
        } else if ("disc".equals(this.emitter.config.emitter_shape_mode)) {
            double radius = num(this.emitter.calculate(this.emitter.config.emitter_shape_radius, params));
            double ang = WinterskyRandom.nextDouble() * Math.PI * 2;
            double dis = surface ? radius : radius * Math.sqrt(WinterskyRandom.nextDouble());

            this.position.x = dis * Math.cos(ang);
            this.position.z = dis * Math.sin(ang);

            Vector3 normal = (Vector3) this.emitter.calculate(this.emitter.config.emitter_shape_plane_normal, params);
            if (normal != null && !normal.equals(MathUtil.Normals.n)) {
                Quaternion q = new Quaternion().setFromUnitVectors(MathUtil.Normals.y, normal);
                this.position.applyQuaternion(q);
            }
        }
        //Speed
        this.speed.set(0, 0, 0);
        String dir = this.emitter.config.particle_direction_mode;
        if ("outwards".equals(dir) && this.emitter.inherited_particle_speed != null) {
            this.speed.copy(this.emitter.inherited_particle_speed);
        } else {
            if ("inwards".equals(dir) || "outwards".equals(dir)) {
                if ("point".equals(this.emitter.config.emitter_shape_mode)) {
                    this.speed.set(1, 0, 0).applyEuler(MathUtil.getRandomEuler());
                } else {
                    this.speed.copy(this.position).normalize();
                    if ("inwards".equals(dir)) {
                        this.speed.negate();
                    }
                }
            } else {
                Vector3 direction = (Vector3) java.util.Objects.requireNonNull(
                        this.emitter.calculate(this.emitter.config.particle_direction_direction, params));
                this.speed = direction.normalize();
            }
            double linearSpeed = num(this.emitter.calculate(this.emitter.config.particle_motion_linear_speed, params));
            this.speed.x *= linearSpeed;
            this.speed.y *= linearSpeed;
            this.speed.z *= linearSpeed;
        }

        Vector3 shapeOffset = (Vector3) this.emitter.calculate(this.emitter.config.emitter_shape_offset, params);
        if (shapeOffset != null) {
            this.position.add(shapeOffset);
        }

        if ("locator".equals(this.emitter.parent_mode)) {
            this.position.x *= -1;
            this.position.y *= -1;
            this.speed.x *= -1;
            this.speed.y *= -1;
        }
        if (!"world".equals(this.emitter.parent_mode) && this.emitter.config.space_local_position
                && !this.emitter.config.space_local_rotation) {
            this.speed.x *= -1;
            this.speed.z *= -1;
        }

        if ("entity_aabb".equals(this.emitter.config.emitter_shape_mode)) {
            this.position.x += 1;
        }

        if (this.emitter.local_space.parent != null) {
            if ("locator".equals(this.emitter.parent_mode)) {
                this.speed.applyQuaternion(this.emitter.local_space.getWorldQuaternion(new Quaternion()));
            }
            if (!this.emitter.config.space_local_rotation) {
                this.position.applyQuaternion(this.emitter.local_space.getWorldQuaternion(new Quaternion()));
            }
            if (!this.emitter.config.space_local_position) {
                Vector3 offset = this.emitter.local_space.getWorldPosition(new Vector3());
                this.position.addScaledVector(offset, 1 / this.emitter.scene.global_options._scale);
            }
        }

        //UV
        this.setFrame(0);

        // Creation event
        for (Object event : this.emitter.config.particle_events_creation) {
            this.emitter.runEvent(event, this);
        }

        return this.tick(false);
    }

    public Particle tick(boolean jump) {
        Map<String, Object> params = this.params();
        double step = 1.0 / this.emitter.scene.global_options.tick_rate;

        for (Object entry : this.emitter.config.particle_render_expression) {
            this.emitter.Molang.parse((String) entry, params);
        }

        //Lifetime
        double lastAge = this.age;
        this.age += step;
        this.loop_time += step;
        if (this.lifetime != 0 && this.age > this.lifetime) {
            this.expire();
        }
        if (io.github.tt432.eyelib.wintersky.molang.JsSemantics.truthy(
                this.emitter.calculate(this.emitter.config.particle_lifetime_expiration_expression, params))) {
            this.expire();
        }

        //Movement
        if ("dynamic".equals(this.emitter.config.particle_motion_mode)) {
            //Position
            double drag = num(this.emitter.calculate(this.emitter.config.particle_motion_linear_drag_coefficient, params));
            Vector3 acc = (Vector3) this.emitter.calculate(this.emitter.config.particle_motion_linear_acceleration, params);
            this.acceleration.copy(acc != null ? acc : new Vector3());
            if (this.emitter.config.space_local_position) {
                if ("locator".equals(this.emitter.parent_mode)) {
                    this.acceleration.x *= -1;
                    this.acceleration.y *= -1;
                }
            } else if (!"world".equals(this.emitter.parent_mode)) {
                this.acceleration.x *= -1;
                this.acceleration.z *= -1;
            }
            this.acceleration.addScaledVector(this.speed, -drag);
            this.speed.addScaledVector(this.acceleration, step);
            this.position.addScaledVector(this.speed, step);

            boolean hasKillPlane = false;
            for (Object v : this.emitter.config.particle_lifetime_kill_plane) {
                if (io.github.tt432.eyelib.wintersky.molang.JsSemantics.truthy(v)) {
                    hasKillPlane = true;
                    break;
                }
            }
            if (hasKillPlane) {
                // Kill Plane
                Plane plane = (Plane) this.emitter.calculate(this.emitter.config.particle_lifetime_kill_plane, params);
                Vector3 startPoint = new Vector3().copy(this.position).addScaledVector(this.speed, -step);
                Vector3 endPoint = new Vector3().copy(this.position);
                if (this.emitter.config.space_local_position && "locator".equals(this.emitter.parent_mode)) {
                    startPoint.x *= -1;
                    startPoint.y *= -1;
                    endPoint.x *= -1;
                    endPoint.y *= -1;
                }
                Line3 line = new Line3(startPoint, endPoint);
                if (plane != null && plane.intersectsLine(line)) {
                    this.expire();
                    return this;
                }
            }
            if (this.emitter.ground_collision && this.emitter.config.particle_collision_toggle
                    && (!io.github.tt432.eyelib.wintersky.molang.JsSemantics.truthy(this.emitter.config.particle_collision_enabled)
                    || io.github.tt432.eyelib.wintersky.molang.JsSemantics.truthy(
                    this.emitter.calculate(this.emitter.config.particle_collision_enabled, params)))) {
                // Collision
                double collisionDrag = this.emitter.config.particle_collision_collision_drag;
                double bounce = this.emitter.config.particle_collision_coefficient_of_restitution;
                double radius = Math.max(this.emitter.config.particle_collision_collision_radius, 0.0001);

                Plane plane = COLLISION_PLANE;
                Sphere sphere = new Sphere(this.position, radius);
                Vector3 previousPos = new Vector3().copy(this.position).addScaledVector(this.speed, -step);
                Line3 line = new Line3(previousPos, this.position);

                boolean intersectsLine = plane.intersectsLine(line);
                if (intersectsLine) {
                    plane.intersectLine(line, this.position);
                }

                if (intersectsLine || plane.intersectsSphere(sphere)) {
                    // Collide
                    if (!this.emitter.config.particle_collision_events.isEmpty()) {
                        double speedLen = this.speed.length();
                        for (Object eventObj : this.emitter.config.particle_collision_events) {
                            Map<String, Object> event = JsonValues.asMap(eventObj);
                            if (event == null || event.get("event") == null) continue;
                            Object minSpeed = event.get("min_speed");
                            if (minSpeed instanceof Number n && n.doubleValue() > speedLen) continue;
                            this.emitter.runEvent(event.get("event"), this);
                        }
                    }
                    if (this.emitter.config.particle_collision_expire_on_contact) {
                        this.expire();
                        return this;
                    }
                    this.position.y = radius * Math.signum(previousPos.y);

                    this.speed.reflect(plane.normal);
                    this.speed.y *= bounce;
                    this.speed.x = Math.signum(this.speed.x)
                            * MathUtil.clamp(Math.abs(this.speed.x) - collisionDrag * step, 0, Double.POSITIVE_INFINITY);
                    this.speed.z = Math.signum(this.speed.z)
                            * MathUtil.clamp(Math.abs(this.speed.z) - collisionDrag * step, 0, Double.POSITIVE_INFINITY);
                }
            }

        } else if ("parametric".equals(this.emitter.config.particle_motion_mode) && !jump) {
            if (joinLength(this.emitter.config.particle_motion_relative_position) > 0) {
                Vector3 v = (Vector3) this.emitter.calculate(this.emitter.config.particle_motion_relative_position, params);
                if (v != null) this.position.copy(v);
            }
            if (joinLength(this.emitter.config.particle_motion_direction) > 0) {
                Vector3 v = (Vector3) this.emitter.calculate(this.emitter.config.particle_motion_direction, params);
                if (v != null) this.speed.copy(v);
            }
            if (this.emitter.config.space_local_position) {
                if ("locator".equals(this.emitter.parent_mode)) {
                    this.position.x *= -1;
                    this.position.y *= -1;
                }
            }
        }

        // Rotation
        if ("dynamic".equals(this.emitter.config.particle_rotation_mode)) {
            double rotDrag = num(this.emitter.calculate(this.emitter.config.particle_rotation_rotation_drag_coefficient, params));
            double rotAcceleration = num(this.emitter.calculate(this.emitter.config.particle_rotation_rotation_acceleration, params));
            rotAcceleration += -rotDrag * this.rotation_rate;
            this.rotation_rate += rotAcceleration * step;
            this.rotation = MathUtil.degToRad(this.initial_rotation + this.rotation_rate * this.age);

        } else if ("parametric".equals(this.emitter.config.particle_rotation_mode)) {
            this.rotation = MathUtil.degToRad(num(this.emitter.calculate(this.emitter.config.particle_rotation_rotation, params)));
        }

        // Facing Direction
        if (this.emitter.config.particle_appearance_facing_camera_mode.startsWith("direction")
                || "lookat_direction".equals(this.emitter.config.particle_appearance_facing_camera_mode)) {
            if ("custom".equals(this.emitter.config.particle_appearance_direction_mode)) {
                Vector3 v = (Vector3) this.emitter.calculate(this.emitter.config.particle_appearance_direction, params);
                if (v != null) {
                    this.facing_direction.copy(v).normalize();
                }
            } else if (this.speed.length() >= (this.emitter.config.particle_appearance_speed_threshold != 0
                    ? this.emitter.config.particle_appearance_speed_threshold : 0.01)) {
                this.facing_direction.copy(this.speed).normalize();
            }
        }

        if (!jump) {
            //Size
            Vector2 size = (Vector2) this.emitter.calculate(this.emitter.config.particle_appearance_size, params);
            if (size != null) {
                this.mesh.scale.x = jsOr(size.x, 0.0001);
                this.mesh.scale.y = jsOr(size.y, 0.0001);
            }

            //UV
            if ("animated".equals(this.emitter.config.particle_texture_mode)) {
                double maxFrame = num(this.emitter.calculate(this.emitter.config.particle_texture_max_frame, params));
                double fps;
                if (this.emitter.config.particle_texture_stretch_to_lifetime && jsTruthy(maxFrame)) {
                    fps = maxFrame / this.lifetime;
                } else {
                    fps = this.emitter.config.particle_texture_frames_per_second;
                }
                if (Math.floor(this.loop_time * fps) > this.current_frame) {
                    this.current_frame = Math.floor(this.loop_time * fps);
                    if (jsTruthy(maxFrame) && this.current_frame >= maxFrame) {
                        if (this.emitter.config.particle_texture_loop) {
                            this.current_frame = 0;
                            this.loop_time = 0;
                            this.setFrame(0);
                        }
                    } else {
                        this.setFrame(this.current_frame);
                    }
                }
            } else {
                this.setFrame(0);
            }

            //Color (ToDo)
            if ("expression".equals(this.emitter.config.particle_color_mode)) {
                Object cObj = this.emitter.calculate(this.emitter.config.particle_color_expression, params, "array");
                if (cObj instanceof List<?> c) {
                    this.setColor(numAt(c, 0), numAt(c, 1), numAt(c, 2), c.size() >= 4 ? numAt(c, 3) : 1);
                }
            } else if ("gradient".equals(this.emitter.config.particle_color_mode)) {
                double i = num(this.emitter.calculate(this.emitter.config.particle_color_interpolant, params));
                double r = num(this.emitter.calculate(this.emitter.config.particle_color_range, params));
                TinyColor.Rgba c = calculateGradient(this.emitter.config.particle_color_gradient, (i / r) * 100);
                this.setColor(c.r / 255, c.g / 255, c.b / 255, c.a);
            } else {
                TinyColor.Rgba c = new TinyColor(this.emitter.config.particle_color_static).toRgb();
                this.setColor(c.r / 255, c.g / 255, c.b / 255, c.a);
            }
        }

        // Event timeline
        for (Map.Entry<String, Object> e : this.emitter.config.particle_events_timeline.entrySet()) {
            double time = JsonValues.jsParseFloat(e.getKey());
            if (time >= lastAge && time < this.age) {
                this.emitter.runEvent(e.getValue(), this);
            }
        }

        return this;
    }

    public Particle expire() {
        for (Object eventId : this.emitter.config.particle_events_expiration) {
            this.emitter.runEvent(eventId, this);
        }
        this.remove();
        return this;
    }

    public Particle remove() {
        MathUtil.removeFromArray(this.emitter.particles, this);
        if (this.mesh.parent != null) this.mesh.parent.remove(this.mesh);
        this.emitter.dead_particles.add(this);
        return this;
    }

    public void delete() {
        if (this.mesh.parent != null) this.mesh.parent.remove(this.mesh);
        this.geometry.dispose();
    }

    public void setColor(double r, double g, double b, double a) {
        BufferAttribute attribute = this.geometry.getAttribute("clr");
        if (attribute == null) return;
        attribute.array.set(new double[]{
                r, g, b, a,
                r, g, b, a,
                r, g, b, a,
                r, g, b, a,
        });
        attribute.setNeedsUpdate(true);
    }

    public void setFrame(double n) {
        if ("full".equals(this.emitter.config.particle_texture_mode)) {
            this.setUV(0, 0,
                    ((Number) this.emitter.config.particle_texture_size.get(0)).doubleValue(),
                    ((Number) this.emitter.config.particle_texture_size.get(1)).doubleValue());
            return;
        }
        Map<String, Object> params = this.params();
        Vector2 uv = (Vector2) this.emitter.calculate(this.emitter.config.particle_texture_uv, params);
        Vector2 size = (Vector2) this.emitter.calculate(this.emitter.config.particle_texture_uv_size, params);
        if (uv == null || size == null) return;
        if (jsTruthy(n)) {
            Vector2 offset = (Vector2) this.emitter.calculate(this.emitter.config.particle_texture_uv_step, params);
            if (offset != null) {
                uv.addScaledVector(offset, n);
            }
        }
        this.setUV(uv.x, uv.y,
                jsOr(size.x, ((Number) this.emitter.config.particle_texture_size.get(0)).doubleValue()),
                jsOr(size.y, ((Number) this.emitter.config.particle_texture_size.get(1)).doubleValue()));
    }

    public void setUV(double x, double y, double w, double h) {
        double epsilon = 0.0;
        BufferAttribute attribute = this.geometry.getAttribute("uv");
        if (attribute == null) return;

        w = (x + w - 2 * epsilon) / ((Number) this.emitter.config.particle_texture_size.get(0)).doubleValue();
        h = (y + h - 2 * epsilon) / ((Number) this.emitter.config.particle_texture_size.get(1)).doubleValue();
        x = (x + (w > 0 ? epsilon : -epsilon)) / ((Number) this.emitter.config.particle_texture_size.get(0)).doubleValue();
        y = (y + (h > 0 ? epsilon : -epsilon)) / ((Number) this.emitter.config.particle_texture_size.get(1)).doubleValue();

        attribute.array.set(new double[]{
                x, 1 - y,
                w, 1 - y,
                x, 1 - h,
                w, 1 - h,
        });
        attribute.setNeedsUpdate(true);
    }

    // ==================================================================
    // 辅助
    // ==================================================================

    /** calculate 返回 Object → double（标量路径必为 Double）。 */
    private static double num(@Nullable Object v) {
        return v instanceof Number n ? n.doubleValue() : Double.NaN;
    }

    private static double numAt(List<?> list, int i) {
        Object v = i < list.size() ? list.get(i) : null;
        return v instanceof Number n ? n.doubleValue() : Double.NaN;
    }

    /** JS array.join('').length。 */
    private static int joinLength(List<Object> list) {
        StringBuilder sb = new StringBuilder();
        for (Object v : list) {
            if (v != null) sb.append(io.github.tt432.eyelib.wintersky.molang.JsSemantics.toJsString(v));
        }
        return sb.length();
    }

    /** JS {@code a || b}（0/NaN → b）。 */
    private static double jsOr(double a, double b) {
        return (a != 0 && !Double.isNaN(a)) ? a : b;
    }

    /** JS number 真值：0/NaN → false。 */
    private static boolean jsTruthy(double v) {
        return v != 0 && !Double.isNaN(v);
    }
}

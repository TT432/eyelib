package io.github.tt432.eyelib.wintersky;

import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import io.github.tt432.eyelib.wintersky.three.Constants;
import io.github.tt432.eyelib.wintersky.three.Texture;
import io.github.tt432.eyelib.wintersky.three.TextureImage;
import io.github.tt432.eyelib.wintersky.tinycolor.TinyColor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * wintersky config.js 逐字移植：扁平 key-value 粒子配置 + JSON 解析。
 *
 * <p>字段为公开 snake_case，与 JS 属性一一对应，便于与 config.js 并排审查。
 * 动态类型字段（molang 数组等）按 JS 运行时表示：String / Double / Boolean /
 * List&lt;Object&gt; / Map&lt;String,Object&gt;。
 */
public class Config {

    /** 内置纹理资源路径（对应 JS 由 bundler 导入的 assets/*.png）。 */
    public static final String MISSING_TEX = "wintersky/textures/missing.png";
    public static final String PARTICLES_TEX = "wintersky/textures/particles.png";
    public static final String FLAME_ATLAS_TEX = "wintersky/textures/flame_atlas.png";
    public static final String SOUL_TEX = "wintersky/textures/soul.png";
    public static final String CAMPFIRE_SMOKE_TEX = "wintersky/textures/campfire_smoke.png";

    /** wintersky 曲线对象（config.curves 的值）。 */
    public static final class Curve {
        public String id = "";
        public String mode = "linear";
        public String input = "0";
        public String range = "0";
        /** 元素为 Double（linear/catmull_rom/bezier）或 {@link BezierNode}（bezier_chain）。 */
        public final List<Object> nodes = new ArrayList<>();
    }

    /** bezier_chain 节点。 */
    public static final class BezierNode {
        public double time;
        public double left_value;
        public double right_value;
        public double left_slope;
        public double right_slope;
    }

    public final Scene scene;
    public final Texture texture;
    public String texture_source_category;
    public @Nullable Runnable onTextureUpdate;

    /** JS {@code this.texture.image}（构造后恒非 null）。 */
    private TextureImage image() {
        return java.util.Objects.requireNonNull(this.texture.image);
    }

    // ==== Config.types 对应的字段（与 JS 属性同名） ====
    public String identifier = "";
    /** JS 允许被 loadResult 赋 undefined。 */
    public @Nullable String file_path = "";
    public final Map<String, Object> events = new LinkedHashMap<>();
    public final Map<String, Curve> curves = new LinkedHashMap<>();
    public boolean space_local_position;
    public boolean space_local_rotation;
    public boolean space_local_velocity;
    public List<Object> variables_creation_vars = new ArrayList<>();
    public List<Object> variables_tick_vars = new ArrayList<>();
    public String emitter_rate_mode = "";
    public String emitter_rate_rate = "";
    public String emitter_rate_amount = "";
    public String emitter_rate_maximum = "";
    public String emitter_lifetime_mode = "";
    public String emitter_lifetime_active_time = "";
    public String emitter_lifetime_sleep_time = "";
    public String emitter_lifetime_activation = "";
    public String emitter_lifetime_expiration = "";
    public List<Object> emitter_events_creation = new ArrayList<>();
    public List<Object> emitter_events_expiration = new ArrayList<>();
    public final Map<String, Object> emitter_events_distance = new LinkedHashMap<>();
    public List<Object> emitter_events_distance_looping = new ArrayList<>();
    public final Map<String, Object> emitter_events_timeline = new LinkedHashMap<>();
    public String emitter_shape_mode = "";
    public List<Object> emitter_shape_offset = new ArrayList<>();
    public String emitter_shape_radius = "";
    public List<Object> emitter_shape_half_dimensions = new ArrayList<>();
    public List<Object> emitter_shape_plane_normal = new ArrayList<>();
    public boolean emitter_shape_surface_only;
    public List<Object> particle_appearance_size = new ArrayList<>();
    public String particle_appearance_material = "";
    public String particle_appearance_facing_camera_mode = "";
    public String particle_appearance_direction_mode = "";
    public double particle_appearance_speed_threshold;
    public List<Object> particle_appearance_direction = new ArrayList<>();
    public List<Object> particle_update_expression = new ArrayList<>();
    public List<Object> particle_render_expression = new ArrayList<>();
    public String particle_direction_mode = "";
    public List<Object> particle_direction_direction = new ArrayList<>();
    public String particle_motion_mode = "";
    public String particle_motion_linear_speed = "";
    public List<Object> particle_motion_linear_acceleration = new ArrayList<>();
    public String particle_motion_linear_drag_coefficient = "";
    public List<Object> particle_motion_relative_position = new ArrayList<>();
    public List<Object> particle_motion_direction = new ArrayList<>();
    public String particle_rotation_mode = "";
    public String particle_rotation_initial_rotation = "";
    public String particle_rotation_rotation_rate = "";
    public String particle_rotation_rotation_acceleration = "";
    public String particle_rotation_rotation_drag_coefficient = "";
    public String particle_rotation_rotation = "";
    public String particle_lifetime_max_lifetime = "";
    public List<Object> particle_lifetime_kill_plane = new ArrayList<>();
    public String particle_lifetime_expiration_expression = "";
    public List<Object> particle_lifetime_expire_in = new ArrayList<>();
    public List<Object> particle_lifetime_expire_outside = new ArrayList<>();
    public List<Object> particle_texture_size = new ArrayList<>();
    public double particle_texture_height;
    public String particle_texture_path = "";
    public String particle_texture_mode = "";
    public List<Object> particle_texture_uv = new ArrayList<>();
    public List<Object> particle_texture_uv_size = new ArrayList<>();
    public List<Object> particle_texture_uv_step = new ArrayList<>();
    public double particle_texture_frames_per_second;
    public String particle_texture_max_frame = "";
    public boolean particle_texture_stretch_to_lifetime;
    public boolean particle_texture_loop;
    public String particle_color_mode = "";
    public String particle_color_static = "";
    public String particle_color_interpolant = "";
    public double particle_color_range;
    public List<Object> particle_color_gradient = new ArrayList<>();
    public List<Object> particle_color_expression = new ArrayList<>();
    public boolean particle_color_light;
    public boolean particle_collision_toggle;
    public String particle_collision_enabled = "";
    public double particle_collision_collision_drag;
    public double particle_collision_coefficient_of_restitution;
    public double particle_collision_collision_radius;
    public boolean particle_collision_expire_on_contact;
    public List<Object> particle_collision_events = new ArrayList<>();
    public List<Object> particle_events_creation = new ArrayList<>();
    public List<Object> particle_events_expiration = new ArrayList<>();
    public final Map<String, Object> particle_events_timeline = new LinkedHashMap<>();

    public Config(Scene scene) {
        this(scene, null, null);
    }

    public Config(Scene scene, @Nullable Map<String, Object> config) {
        this(scene, config, null);
    }

    public Config(Scene scene, @Nullable Map<String, Object> config, @Nullable String path) {
        this.scene = scene;
        this.texture = new Texture(new TextureImage());
        image().onload = () -> {
            this.texture.setNeedsUpdate(true);
            if (this.onTextureUpdate != null) {
                this.onTextureUpdate.run();
            }
        };
        this.texture_source_category = "placeholder";
        reset();
        this.onTextureUpdate = null;

        if (path != null) set("file_path", path);

        Map<String, Object> particleEffect = config != null ? JsonValues.asMap(config.get("particle_effect")) : null;
        if (particleEffect != null && config != null) {
            setFromJSON(config);
        } else if (config != null) {
            // JS Object.assign(this, config)：原样拷贝已知字段（无类型 coercion），未知 key 无法表示、忽略。
            for (Map.Entry<String, Object> e : config.entrySet()) {
                Entry entry = TYPES.get(e.getKey());
                if (entry != null && e.getValue() != null) {
                    entry.set.accept(this, e.getValue());
                }
            }
        }
    }

    public Config reset() {
        image().setSrc(MISSING_TEX);
        this.texture.magFilter = Constants.NearestFilter;
        this.texture.minFilter = Constants.NearestFilter;

        for (Entry entry : TYPES.values()) {
            Object value = defaultValue(entry.kind);
            if (entry.array) {
                List<Object> list = new ArrayList<>();
                if (entry.dimensions > 0) {
                    for (int i = 0; i < entry.dimensions; i++) {
                        if (entry.kind == Kind.OBJECT) value = new LinkedHashMap<String, Object>();
                        list.add(value);
                    }
                }
                entry.set.accept(this, list);
            } else if (entry.kind == Kind.OBJECT && entry.get.apply(this) != null) {
                // JS：清空既有对象（保持引用）
                @SuppressWarnings("unchecked")
                Map<String, Object> existing = (Map<String, Object>) entry.get.apply(this);
                existing.clear();
            } else {
                entry.set.accept(this, value);
            }
        }
        this.emitter_rate_mode = "steady";
        this.emitter_lifetime_mode = "looping";
        this.emitter_shape_mode = "point";
        this.particle_appearance_material = "particles_alpha";
        this.particle_appearance_facing_camera_mode = "rotate_xyz";
        this.particle_appearance_direction_mode = "derive_from_velocity";
        this.particle_appearance_speed_threshold = 0.01;
        this.particle_direction_mode = "outwards";
        this.particle_motion_mode = "dynamic";
        this.particle_rotation_mode = "dynamic";
        this.particle_texture_mode = "static";
        this.particle_color_mode = "static";
        this.particle_color_interpolant = "v.particle_age / v.particle_lifetime";
        this.particle_color_range = 1;

        this.emitter_rate_rate = "4";
        this.emitter_rate_amount = "1";
        this.emitter_rate_maximum = "100";
        this.emitter_lifetime_active_time = "1";
        this.particle_appearance_size = new ArrayList<>(List.of("0.2", "0.2"));
        this.particle_lifetime_max_lifetime = "1";
        this.particle_texture_size = new ArrayList<>(List.of(16.0, 16.0));

        this.texture_source_category = "placeholder";

        return this;
    }

    private static Object defaultValue(Kind kind) {
        return switch (kind) {
            case STRING, MOLANG -> "";
            case NUMBER -> 0.0;
            case BOOLEAN -> false;
            case COLOR -> "#ffffff";
            case OBJECT -> new LinkedHashMap<String, Object>();
        };
    }

    public Config set(String key, @Nullable Object val) {
        Entry entry = TYPES.get(key);
        if (entry == null || val == null) return this;

        Object current = entry.get.apply(this);
        if (entry.array && val instanceof List<?> list) {
            // JS this[key].splice(0, Infinity, ...val)：原地替换内容
            @SuppressWarnings("unchecked")
            List<Object> target = (List<Object>) current;
            target.clear();
            if (entry.kind == Kind.MOLANG) {
                for (Object v : list) {
                    target.add(JsSemantics.toJsString(v));
                }
            } else {
                target.addAll(list);
            }
        } else if (entry.array && entry.kind == Kind.STRING && val instanceof String s) {
            @SuppressWarnings("unchecked")
            List<Object> target = (List<Object>) current;
            target.clear();
            target.add(s);
        } else if (current instanceof String) {
            entry.set.accept(this, JsSemantics.toJsString(val));
        } else if (entry.kind == Kind.NUMBER && val instanceof Number n) {
            entry.set.accept(this, n.doubleValue());
        } else if (entry.kind == Kind.BOOLEAN) {
            entry.set.accept(this, JsSemantics.truthy(val));
        } else if (entry.kind == Kind.OBJECT) {
            @SuppressWarnings("unchecked")
            Map<String, Object> target = (Map<String, Object>) current;
            if (val instanceof Map<?, ?> m) {
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    target.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
        }
        return this;
    }

    // ==================================================================
    // setFromJSON
    // ==================================================================

    public Config setFromJSON(Map<String, Object> data) {
        Map<String, Object> particleEffect = JsonValues.asMap(data.get("particle_effect"));
        if (particleEffect == null) {
            updateTexture();
            return this;
        }
        Map<String, Object> comps = JsonValues.asMap(particleEffect.get("components"));
        Map<String, Object> curves = JsonValues.asMap(particleEffect.get("curves"));
        Map<String, Object> events = JsonValues.asMap(particleEffect.get("events"));
        Map<String, Object> desc = JsonValues.asMap(particleEffect.get("description"));

        if (desc != null && desc.get("identifier") != null) {
            this.identifier = JsSemantics.toJsString(desc.get("identifier"));
        }
        if (desc != null) {
            Map<String, Object> brp = JsonValues.asMap(desc.get("basic_render_parameters"));
            if (brp != null) {
                this.set("particle_texture_path", brp.get("texture"));
                this.set("particle_appearance_material", brp.get("material"));
            }
        }
        if (events != null) {
            for (Map.Entry<String, Object> e : events.entrySet()) {
                this.events.put(e.getKey(), e.getValue());
            }
        }
        if (curves != null) {
            for (Map.Entry<String, Object> e : curves.entrySet()) {
                String key = e.getKey();
                Map<String, Object> jsonCurve = JsonValues.asMap(e.getValue());
                if (jsonCurve == null) continue;
                Curve newCurve = new Curve();
                newCurve.id = key;
                newCurve.mode = JsSemantics.toJsString(jsonCurve.get("type"));
                Object input = jsonCurve.get("input");
                Object range = jsonCurve.get("horizontal_range");
                newCurve.input = JsSemantics.toJsString(JsSemantics.truthy(input) ? input : 0.0);
                newCurve.range = JsSemantics.toJsString(JsSemantics.truthy(range) ? range : 0.0);
                List<Object> nodes = JsonValues.asList(jsonCurve.get("nodes"));
                if (nodes != null && !nodes.isEmpty()) {
                    for (Object value : nodes) {
                        double point = orZero(JsonValues.jsParseFloat(value));
                        newCurve.nodes.add(point);
                    }
                } else {
                    Map<String, Object> nodeMap = JsonValues.asMap(jsonCurve.get("nodes"));
                    if (nodeMap != null && "bezier_chain".equals(jsonCurve.get("type"))) {
                        for (Map.Entry<String, Object> nodeEntry : nodeMap.entrySet()) {
                            Map<String, Object> node = JsonValues.asMap(nodeEntry.getValue());
                            if (node == null) continue;
                            BezierNode point = new BezierNode();
                            point.time = JsonValues.jsParseFloat(nodeEntry.getKey());
                            point.left_value = orZero(JsonValues.jsParseFloat(firstNonNull(node.get("left_value"), node.get("value"))));
                            point.right_value = orZero(JsonValues.jsParseFloat(firstNonNull(node.get("right_value"), node.get("value"))));
                            point.left_slope = orZero(JsonValues.jsParseFloat(firstNonNull(node.get("left_slope"), node.get("slope"))));
                            point.right_slope = orZero(JsonValues.jsParseFloat(firstNonNull(node.get("right_slope"), node.get("slope"))));
                            newCurve.nodes.add(point);
                        }
                    }
                }
                this.curves.put(key, newCurve);
            }
        }

        if (comps != null) {
            Map<String, Object> emitterInit = comp(comps, "emitter_initialization");
            if (emitterInit != null) {
                Object crV = emitterInit.get("creation_expression");
                Object upV = emitterInit.get("per_update_expression");
                if (crV instanceof String s) {
                    this.variables_creation_vars = new ArrayList<>(List.of(s.replaceAll(";+$", "").split(";")));
                }
                if (upV instanceof String s) {
                    this.variables_tick_vars = new ArrayList<>(List.of(s.replaceAll(";+$", "").split(";")));
                }
            }
            Map<String, Object> localSpace = comp(comps, "emitter_local_space");
            if (localSpace != null) {
                this.space_local_position = JsSemantics.truthy(localSpace.get("position"));
                this.space_local_rotation = JsSemantics.truthy(localSpace.get("rotation"));
                this.space_local_velocity = JsSemantics.truthy(localSpace.get("velocity"));
            }
            Map<String, Object> rateManual = comp(comps, "emitter_rate_manual");
            if (rateManual != null) {
                this.set("emitter_rate_mode", "manual");
                this.set("emitter_rate_maximum", rateManual.get("max_particles"));
            }
            Map<String, Object> rateSteady = comp(comps, "emitter_rate_steady");
            if (rateSteady != null) {
                this.set("emitter_rate_mode", "steady");
                this.set("emitter_rate_rate", rateSteady.get("spawn_rate"));
                this.set("emitter_rate_maximum", rateSteady.get("max_particles"));
            }
            Map<String, Object> rateInstant = comp(comps, "emitter_rate_instant");
            if (rateInstant != null) {
                this.set("emitter_rate_mode", "instant");
                this.set("emitter_rate_amount", rateInstant.get("num_particles"));
            }
            Map<String, Object> lifetimeOnce = comp(comps, "emitter_lifetime_once");
            if (lifetimeOnce != null) {
                this.set("emitter_lifetime_mode", "once");
                this.set("emitter_lifetime_active_time", lifetimeOnce.get("active_time"));
            }
            Map<String, Object> lifetimeLooping = comp(comps, "emitter_lifetime_looping");
            if (lifetimeLooping != null) {
                this.set("emitter_lifetime_mode", "looping");
                this.set("emitter_lifetime_active_time", lifetimeLooping.get("active_time"));
                this.set("emitter_lifetime_sleep_time", lifetimeLooping.get("sleep_time"));
            }
            Map<String, Object> lifetimeExpression = comp(comps, "emitter_lifetime_expression");
            if (lifetimeExpression != null) {
                this.set("emitter_lifetime_mode", "expression");
                this.set("emitter_lifetime_activation", lifetimeExpression.get("activation_expression"));
                this.set("emitter_lifetime_expiration", lifetimeExpression.get("expiration_expression"));
            }
            Map<String, Object> lifetimeEvents = comp(comps, "emitter_lifetime_events");
            if (lifetimeEvents != null) {
                this.set("emitter_events_creation", lifetimeEvents.get("creation_event"));
                this.set("emitter_events_expiration", lifetimeEvents.get("expiration_event"));
                this.set("emitter_events_timeline", lifetimeEvents.get("timeline"));
                this.set("emitter_events_distance", lifetimeEvents.get("travel_distance_events"));
                this.set("emitter_events_distance_looping", lifetimeEvents.get("looping_travel_distance_events"));
            }
            Map<String, Object> shapeComponent = comp(comps, "emitter_shape_point");
            if (shapeComponent == null) shapeComponent = comp(comps, "emitter_shape_custom");
            if (shapeComponent != null) {
                this.set("emitter_shape_mode", "point");
                this.set("emitter_shape_offset", shapeComponent.get("offset"));
            }
            Map<String, Object> shapeSphere = comp(comps, "emitter_shape_sphere");
            if (shapeSphere != null) {
                shapeComponent = shapeSphere;
                this.set("emitter_shape_mode", "sphere");
                this.set("emitter_shape_offset", shapeComponent.get("offset"));
                this.set("emitter_shape_radius", shapeComponent.get("radius"));
                this.set("emitter_shape_surface_only", shapeComponent.get("surface_only"));
            }
            Map<String, Object> shapeBox = comp(comps, "emitter_shape_box");
            if (shapeBox != null) {
                shapeComponent = shapeBox;
                this.set("emitter_shape_mode", "box");
                this.set("emitter_shape_offset", shapeComponent.get("offset"));
                this.set("emitter_shape_half_dimensions", shapeComponent.get("half_dimensions"));
                this.set("emitter_shape_surface_only", shapeComponent.get("surface_only"));
            }
            Map<String, Object> shapeDisc = comp(comps, "emitter_shape_disc");
            if (shapeDisc != null) {
                shapeComponent = shapeDisc;
                this.set("emitter_shape_mode", "disc");
                this.set("emitter_shape_offset", shapeComponent.get("offset"));
                Object planeNormal = shapeComponent.get("plane_normal");
                if ("x".equals(planeNormal)) {
                    this.set("emitter_shape_plane_normal", List.of(1.0, 0.0, 0.0));
                } else if ("y".equals(planeNormal)) {
                    this.set("emitter_shape_plane_normal", List.of(0.0, 1.0, 0.0));
                } else if ("z".equals(planeNormal)) {
                    this.set("emitter_shape_plane_normal", List.of(0.0, 0.0, 1.0));
                } else {
                    this.set("emitter_shape_plane_normal", planeNormal);
                }
                this.set("emitter_shape_radius", shapeComponent.get("radius"));
                this.set("emitter_shape_surface_only", shapeComponent.get("surface_only"));
            }
            Map<String, Object> shapeEntityAabb = comp(comps, "emitter_shape_entity_aabb");
            if (shapeEntityAabb != null) {
                this.set("emitter_shape_mode", "entity_aabb");
                this.set("emitter_shape_surface_only", shapeEntityAabb.get("surface_only"));
                shapeComponent = shapeEntityAabb;
            }
            Object shapeDirection = shapeComponent != null ? shapeComponent.get("direction") : null;
            if (shapeDirection != null) {
                if ("inwards".equals(shapeDirection) || "outwards".equals(shapeDirection)) {
                    this.set("particle_direction_mode", shapeDirection);
                } else {
                    this.set("particle_direction_mode", "direction");
                    this.set("particle_direction_direction", shapeDirection);
                }
            }

            Map<String, Object> particleInit = comp(comps, "particle_initialization");
            if (particleInit != null) {
                Object upV = particleInit.get("per_update_expression");
                Object rdV = particleInit.get("per_render_expression");
                if (upV instanceof String s) {
                    this.particle_update_expression = new ArrayList<>(List.of(s.replaceAll(";+$", "").split(";")));
                }
                if (rdV instanceof String s) {
                    this.particle_render_expression = new ArrayList<>(List.of(s.replaceAll(";+$", "").split(";")));
                }
            }

            Map<String, Object> initialSpin = comp(comps, "particle_initial_spin");
            if (initialSpin != null) {
                this.set("particle_rotation_initial_rotation", initialSpin.get("rotation"));
                this.set("particle_rotation_rotation_rate", initialSpin.get("rotation_rate"));
            }
            Object killPlane = comps.get("minecraft:particle_kill_plane");
            if (killPlane != null) {
                // JS comp() 直接返回组件值（4 元素数组），不经对象包装
                this.set("particle_lifetime_kill_plane", killPlane);
            }

            Map<String, Object> motionDynamic = comp(comps, "particle_motion_dynamic");
            if (motionDynamic != null) {
                Object linearAcceleration = motionDynamic.get("linear_acceleration");
                Object linearDragCoefficient = motionDynamic.get("linear_drag_coefficient");
                Object rotationAcceleration = motionDynamic.get("rotation_acceleration");
                Object rotationDragCoefficient = motionDynamic.get("rotation_drag_coefficient");

                if (linearAcceleration != null || linearDragCoefficient != null) {
                    this.set("particle_motion_mode", "dynamic");
                    this.set("particle_motion_linear_acceleration", linearAcceleration);
                    this.set("particle_motion_linear_drag_coefficient", linearDragCoefficient);
                    this.set("particle_motion_linear_speed", 1.0);
                }
                if (linearAcceleration != null || linearDragCoefficient != null) {
                    // JS 原样重复判定（疑似笔误，as-is 保留）
                    this.set("particle_rotation_mode", "dynamic");
                    this.set("particle_rotation_rotation_acceleration", rotationAcceleration);
                    this.set("particle_rotation_rotation_drag_coefficient", rotationDragCoefficient);
                }
            } else {
                this.set("particle_motion_mode", "static");
            }
            Map<String, Object> motionParametric = comp(comps, "particle_motion_parametric");
            if (motionParametric != null) {
                Object relativePosition = motionParametric.get("relative_position");
                Object direction = motionParametric.get("direction");
                Object rotation = motionParametric.get("rotation");

                if (relativePosition != null || direction != null) {
                    this.set("particle_motion_mode", "parametric");
                    this.set("particle_motion_relative_position", relativePosition);
                    this.set("particle_motion_direction", direction);
                }
                if (rotation != null) {
                    this.set("particle_rotation_mode", "parametric");
                    this.set("particle_rotation_rotation", rotation);
                }
            }

            this.set("particle_collision_toggle", comp(comps, "particle_motion_collision") != null);
            Map<String, Object> motionCollision = comp(comps, "particle_motion_collision");
            if (motionCollision != null) {
                this.set("particle_collision_enabled", motionCollision.get("enabled"));
                this.set("particle_collision_collision_drag", motionCollision.get("collision_drag"));
                this.set("particle_collision_coefficient_of_restitution", motionCollision.get("coefficient_of_restitution"));
                this.set("particle_collision_collision_radius", motionCollision.get("collision_radius"));
                this.set("particle_collision_expire_on_contact", motionCollision.get("expire_on_contact"));
                Object collisionEvents = motionCollision.get("events");
                if (collisionEvents instanceof List<?>) {
                    this.set("particle_collision_events", collisionEvents);
                } else if (collisionEvents instanceof Map<?, ?>) {
                    this.set("particle_collision_events", List.of(collisionEvents));
                }
            }
            Object initialSpeed = comps.get("minecraft:particle_initial_speed");
            if (initialSpeed != null) {
                // JS comp() 返回原始值（标量或数组），typeof c !== 'object' 判定
                if (!(initialSpeed instanceof Map<?, ?>) && !(initialSpeed instanceof List<?>)) {
                    this.set("particle_motion_linear_speed", initialSpeed);
                } else {
                    this.set("particle_direction_mode", "direction");
                    this.set("particle_direction_direction", initialSpeed);
                    this.set("particle_motion_linear_speed", 1.0);
                }
            }

            Map<String, Object> lifetimeExpr = comp(comps, "particle_lifetime_expression");
            if (lifetimeExpr != null) {
                Object maxLifetime = lifetimeExpr.get("max_lifetime");
                Object expirationExpr = lifetimeExpr.get("expiration_expression");
                this.set("particle_lifetime_max_lifetime", maxLifetime != null ? maxLifetime : "");
                this.set("particle_lifetime_expiration_expression", expirationExpr != null ? expirationExpr : 0.0);
            }
            Object expireIn = comps.get("minecraft:particle_expire_if_in_blocks");
            if (expireIn instanceof List<?>) {
                this.set("particle_lifetime_expire_in", expireIn);
            }
            Object expireOutside = comps.get("minecraft:particle_expire_if_not_in_blocks");
            if (expireOutside instanceof List<?>) {
                this.set("particle_lifetime_expire_outside", expireOutside);
            }

            Map<String, Object> billboard = comp(comps, "particle_appearance_billboard");
            if (billboard != null) {
                this.set("particle_appearance_size", billboard.get("size"));
                this.set("particle_appearance_facing_camera_mode", billboard.get("facing_camera_mode"));

                Map<String, Object> direction = JsonValues.asMap(billboard.get("direction"));
                Map<String, Object> uv = JsonValues.asMap(billboard.get("uv"));

                if (direction != null) {
                    this.set("particle_appearance_direction_mode", direction.get("mode"));
                    this.set("particle_appearance_speed_threshold", direction.get("min_speed_threshold"));
                    this.set("particle_appearance_direction", direction.get("custom_direction"));
                }

                if (uv != null) {
                    if (JsSemantics.truthy(uv.get("texture_width"))) {
                        this.set("particle_texture_size", List.of(uv.get("texture_width"), uv.get("texture_height")));
                    }
                    Map<String, Object> flipbook = JsonValues.asMap(uv.get("flipbook"));
                    List<Object> uvUv = JsonValues.asList(uv.get("uv"));
                    List<Object> uvSize = JsonValues.asList(uv.get("uv_size"));
                    if (flipbook != null) {
                        this.set("particle_texture_mode", "animated");
                        this.set("particle_texture_uv", flipbook.get("base_UV"));
                        this.set("particle_texture_uv_size", flipbook.get("size_UV"));
                        this.set("particle_texture_uv_step", flipbook.get("step_UV"));
                        this.set("particle_texture_frames_per_second", flipbook.get("frames_per_second"));
                        this.set("particle_texture_max_frame", flipbook.get("max_frame"));
                        this.set("particle_texture_stretch_to_lifetime", flipbook.get("stretch_to_lifetime"));
                        this.set("particle_texture_loop", flipbook.get("loop"));
                    } else if (numberEquals(uv.get("texture_width"), 1) && numberEquals(uv.get("texture_height"), 1)
                            && uvUv != null && !JsSemantics.truthy(uvUv.get(0)) && !JsSemantics.truthy(uvUv.get(1))
                            && uvSize != null && numberEquals(uvSize.get(0), 1) && numberEquals(uvSize.get(1), 1)) {
                        this.set("particle_texture_mode", "full");
                        this.set("particle_texture_uv", uv.get("uv"));
                        this.set("particle_texture_uv_size", uv.get("uv_size"));
                    } else {
                        this.set("particle_texture_mode", "static");
                        this.set("particle_texture_uv", uv.get("uv"));
                        this.set("particle_texture_uv_size", uv.get("uv_size"));
                    }
                } else {
                    this.set("particle_texture_mode", "full");
                }
            }
            if (comp(comps, "particle_appearance_lighting") != null) {
                this.set("particle_color_light", true);
            }
            Map<String, Object> tinting = comp(comps, "particle_appearance_tinting");
            if (tinting != null) {
                Object c = tinting.get("color");

                if (c instanceof String s) {
                    this.set("particle_color_static", parseColor(s));
                } else if (c instanceof List<?> list && list.size() >= 3) {
                    boolean anyString = false;
                    for (int i = 0; i < list.size() && i < 4; i++) {
                        if (list.get(i) instanceof String) anyString = true;
                    }
                    if (anyString) {
                        this.set("particle_color_mode", "expression");
                        this.set("particle_color_expression", c);
                    } else {
                        this.set("particle_color_mode", "static");
                        double r = numberOrZero(list.get(0)) * 255;
                        double g = numberOrZero(list.get(1)) * 255;
                        double b = numberOrZero(list.get(2)) * 255;
                        double a = list.size() > 3 && list.get(3) instanceof Number n ? n.doubleValue() : 1;
                        String color = new TinyColor(r, g, b, a).toHex8String();
                        this.set("particle_color_static", color);
                    }
                } else if (c instanceof Map<?, ?>) {
                    // Gradient
                    Map<String, Object> gradient = java.util.Objects.requireNonNull(JsonValues.asMap(c));
                    this.set("particle_color_mode", "gradient");
                    this.set("particle_color_interpolant", gradient.get("interpolant"));
                    List<Object> gradientPoints = new ArrayList<>();
                    Object gradientValue = gradient.get("gradient");
                    if (gradientValue instanceof List<?> gradientList) {
                        double distance = 100 / (gradientList.size() - 1.0);
                        int i = 0;
                        for (Object colorObj : gradientList) {
                            String color = parseColor(colorObj);
                            double percent = distance * i;
                            Map<String, Object> point = new LinkedHashMap<>();
                            point.put("percent", percent);
                            point.put("color", color);
                            gradientPoints.add(point);
                            i++;
                        }
                    } else {
                        Map<String, Object> gradientMap = JsonValues.asMap(gradientValue);
                        if (gradientMap != null) {
                            double maxTime = 1;
                            for (String time : gradientMap.keySet()) {
                                maxTime = Math.max(JsonValues.jsParseFloat(time), maxTime);
                            }
                            this.set("particle_color_range", maxTime);
                            for (Map.Entry<String, Object> gradientEntry : gradientMap.entrySet()) {
                                String color = parseColor(gradientEntry.getValue());
                                double percent = (JsonValues.jsParseFloat(gradientEntry.getKey()) / maxTime) * 100;
                                Map<String, Object> point = new LinkedHashMap<>();
                                point.put("color", color);
                                point.put("percent", percent);
                                gradientPoints.add(point);
                            }
                        }
                    }
                    this.set("particle_color_gradient", gradientPoints);
                }
            }
            Map<String, Object> particleLifetimeEvents = comp(comps, "particle_lifetime_events");
            if (particleLifetimeEvents != null) {
                this.set("particle_events_creation", particleLifetimeEvents.get("creation_event"));
                this.set("particle_events_expiration", particleLifetimeEvents.get("expiration_event"));
                this.set("particle_events_timeline", particleLifetimeEvents.get("timeline"));
            }
        }

        this.updateTexture();
        return this;
    }

    public Config updateTexture() {
        Object result = this.scene.fetchTexture(this);
        if (result instanceof CompletionStage<?> stage) {
            stage.thenAccept(this::continueLoading);
        } else {
            continueLoading(result);
        }
        return this;
    }

    private void continueLoading(@Nullable Object urlObj) {
        String url = urlObj instanceof String s ? s : null;
        if (url == null || url.isEmpty()) {
            switch (this.particle_texture_path) {
                case "textures/particle/particles" -> {
                    url = PARTICLES_TEX;
                    this.texture_source_category = "built_in";
                }
                case "textures/flame_atlas", "textures/particle/flame_atlas" -> {
                    url = FLAME_ATLAS_TEX;
                    this.texture_source_category = "built_in";
                }
                case "textures/particle/soul" -> {
                    url = SOUL_TEX;
                    this.texture_source_category = "built_in";
                }
                case "textures/particle/campfire_smoke" -> {
                    url = CAMPFIRE_SMOKE_TEX;
                    this.texture_source_category = "built_in";
                }
                default -> {
                    url = MISSING_TEX;
                    this.texture_source_category = "placeholder";
                }
            }
        } else {
            this.texture_source_category = "loaded";
        }
        image().setSrc(url);
    }

    // ==================================================================
    // parseColor（config.js 顶层函数）
    // ==================================================================

    /**
     * 与 npm dist（1.3.3 实际运行时）对齐：数组分支<b>直接返回</b> toHex8String，
     * 不走末尾的 substr 重排（git master src 会重排——dist 构建自更老提交，见 ADR-0034）。
     */
    static String parseColor(@Nullable Object input) {
        if (input instanceof String s && s.startsWith("#")) {
            if (s.length() < 9) {
                input = "#ff" + JsSemantics.jsSubstr(s, 1, 6);
            }
        } else {
            if (input instanceof List<?> list) {
                double r = 255 * elementOrZero(list, 0);
                double g = 255 * elementOrZero(list, 1);
                double b = 255 * elementOrZero(list, 2);
                Object aObj = list.size() > 3 ? list.get(3) : null;
                double a = (aObj instanceof Number n) ? n.doubleValue() : 1;
                return new TinyColor(r, g, b, a).toHex8String();
            }
            input = new TinyColor(input != null ? JsSemantics.toJsString(input) : "").toHex8String();
        }
        String s = (String) input;
        return "#" + JsSemantics.jsSubstr(s, 3, 6) + JsSemantics.jsSubstr(s, 1, 2);
    }

    /** JS {@code (e[i] || 0)} 后参与乘法：字符串走 ToNumber（"0.5"→0.5，非数字→NaN）。 */
    private static double elementOrZero(List<?> list, int i) {
        Object v = list.size() > i ? list.get(i) : null;
        if (!JsSemantics.truthy(v)) return 0;
        return JsSemantics.toNumber(v);
    }


    /** JS {@code parseFloat(x) || 0}。 */
    private static double orZero(double v) {
        return (v == 0 || Double.isNaN(v)) ? 0 : v;
    }

    private static @Nullable Object firstNonNull(@Nullable Object a, @Nullable Object b) {
        // JS a || b（此处用于 left_value||value 等，0 会取后者——与 JS 一致）
        return JsSemantics.truthy(a) ? a : b;
    }

    private static double numberOrZero(@Nullable Object v) {
        return v instanceof Number n ? n.doubleValue() : 0;
    }

    /** JS {@code v == 1}（宽松等于，JSON 数字为 Double）。 */
    private static boolean numberEquals(@Nullable Object v, double expected) {
        if (v instanceof Number n) return n.doubleValue() == expected;
        if (v instanceof String s) {
            double parsed = JsonValues.jsParseFloat(s);
            return !Double.isNaN(parsed) && parsed == expected;
        }
        return false;
    }

    private static @Nullable Map<String, Object> comp(Map<String, Object> comps, String id) {
        return JsonValues.asMap(comps.get("minecraft:" + id));
    }

    // ==================================================================
    // Config.types（Entry 注册表：kind/array/dimensions + 字段访问器）
    // ==================================================================

    enum Kind {STRING, MOLANG, NUMBER, BOOLEAN, COLOR, OBJECT}

    record Entry(Kind kind, boolean array, int dimensions,
                 Function<Config, Object> get, BiConsumer<Config, Object> set) {
    }

    private static final Map<String, Entry> TYPES = new LinkedHashMap<>();

    private static void t(String key, Kind kind, Function<Config, Object> get, BiConsumer<Config, Object> set) {
        TYPES.put(key, new Entry(kind, false, 0, get, set));
    }

    private static void ta(String key, Kind kind, int dimensions,
                           Function<Config, Object> get, BiConsumer<Config, Object> set) {
        TYPES.put(key, new Entry(kind, true, dimensions, get, set));
    }

    @SuppressWarnings("unchecked")
    private static BiConsumer<Config, Object> setStr(BiConsumer<Config, String> setter) {
        return (c, v) -> setter.accept(c, (String) v);
    }

    @SuppressWarnings("unchecked")
    private static BiConsumer<Config, Object> setNum(BiConsumer<Config, Double> setter) {
        return (c, v) -> setter.accept(c, (Double) v);
    }

    @SuppressWarnings("unchecked")
    private static BiConsumer<Config, Object> setBool(BiConsumer<Config, Boolean> setter) {
        return (c, v) -> setter.accept(c, (Boolean) v);
    }

    @SuppressWarnings("unchecked")
    private static BiConsumer<Config, Object> setList(BiConsumer<Config, List<Object>> setter) {
        return (c, v) -> setter.accept(c, (List<Object>) v);
    }

    @SuppressWarnings("unchecked")
    private static BiConsumer<Config, Object> setMap(BiConsumer<Config, Map<String, Object>> setter) {
        return (c, v) -> setter.accept(c, (Map<String, Object>) v);
    }

    static {
        t("identifier", Kind.STRING, c -> c.identifier, setStr((c, v) -> c.identifier = v));
        t("file_path", Kind.STRING, c -> c.file_path, setStr((c, v) -> c.file_path = v));
        t("events", Kind.OBJECT, c -> c.events, setMap((c, v) -> {
            c.events.clear();
            c.events.putAll(v);
        }));
        t("curves", Kind.OBJECT, c -> c.curves, (c, v) -> {
            c.curves.clear();
            c.curves.putAll((Map<String, Curve>) v);
        });
        t("space_local_position", Kind.BOOLEAN, c -> c.space_local_position, setBool((c, v) -> c.space_local_position = v));
        t("space_local_rotation", Kind.BOOLEAN, c -> c.space_local_rotation, setBool((c, v) -> c.space_local_rotation = v));
        t("space_local_velocity", Kind.BOOLEAN, c -> c.space_local_velocity, setBool((c, v) -> c.space_local_velocity = v));
        ta("variables_creation_vars", Kind.STRING, 0, c -> c.variables_creation_vars, setList((c, v) -> c.variables_creation_vars = v));
        ta("variables_tick_vars", Kind.STRING, 0, c -> c.variables_tick_vars, setList((c, v) -> c.variables_tick_vars = v));
        t("emitter_rate_mode", Kind.STRING, c -> c.emitter_rate_mode, setStr((c, v) -> c.emitter_rate_mode = v));
        t("emitter_rate_rate", Kind.MOLANG, c -> c.emitter_rate_rate, setStr((c, v) -> c.emitter_rate_rate = v));
        t("emitter_rate_amount", Kind.MOLANG, c -> c.emitter_rate_amount, setStr((c, v) -> c.emitter_rate_amount = v));
        t("emitter_rate_maximum", Kind.MOLANG, c -> c.emitter_rate_maximum, setStr((c, v) -> c.emitter_rate_maximum = v));
        t("emitter_lifetime_mode", Kind.STRING, c -> c.emitter_lifetime_mode, setStr((c, v) -> c.emitter_lifetime_mode = v));
        t("emitter_lifetime_active_time", Kind.MOLANG, c -> c.emitter_lifetime_active_time, setStr((c, v) -> c.emitter_lifetime_active_time = v));
        t("emitter_lifetime_sleep_time", Kind.MOLANG, c -> c.emitter_lifetime_sleep_time, setStr((c, v) -> c.emitter_lifetime_sleep_time = v));
        t("emitter_lifetime_activation", Kind.MOLANG, c -> c.emitter_lifetime_activation, setStr((c, v) -> c.emitter_lifetime_activation = v));
        t("emitter_lifetime_expiration", Kind.MOLANG, c -> c.emitter_lifetime_expiration, setStr((c, v) -> c.emitter_lifetime_expiration = v));
        ta("emitter_events_creation", Kind.STRING, 0, c -> c.emitter_events_creation, setList((c, v) -> c.emitter_events_creation = v));
        ta("emitter_events_expiration", Kind.STRING, 0, c -> c.emitter_events_expiration, setList((c, v) -> c.emitter_events_expiration = v));
        t("emitter_events_distance", Kind.OBJECT, c -> c.emitter_events_distance, setMap((c, v) -> {
            c.emitter_events_distance.clear();
            c.emitter_events_distance.putAll(v);
        }));
        ta("emitter_events_distance_looping", Kind.OBJECT, 0, c -> c.emitter_events_distance_looping, setList((c, v) -> c.emitter_events_distance_looping = v));
        t("emitter_events_timeline", Kind.OBJECT, c -> c.emitter_events_timeline, setMap((c, v) -> {
            c.emitter_events_timeline.clear();
            c.emitter_events_timeline.putAll(v);
        }));
        t("emitter_shape_mode", Kind.STRING, c -> c.emitter_shape_mode, setStr((c, v) -> c.emitter_shape_mode = v));
        ta("emitter_shape_offset", Kind.MOLANG, 3, c -> c.emitter_shape_offset, setList((c, v) -> c.emitter_shape_offset = v));
        t("emitter_shape_radius", Kind.MOLANG, c -> c.emitter_shape_radius, setStr((c, v) -> c.emitter_shape_radius = v));
        ta("emitter_shape_half_dimensions", Kind.MOLANG, 3, c -> c.emitter_shape_half_dimensions, setList((c, v) -> c.emitter_shape_half_dimensions = v));
        ta("emitter_shape_plane_normal", Kind.MOLANG, 3, c -> c.emitter_shape_plane_normal, setList((c, v) -> c.emitter_shape_plane_normal = v));
        t("emitter_shape_surface_only", Kind.BOOLEAN, c -> c.emitter_shape_surface_only, setBool((c, v) -> c.emitter_shape_surface_only = v));
        ta("particle_appearance_size", Kind.MOLANG, 2, c -> c.particle_appearance_size, setList((c, v) -> c.particle_appearance_size = v));
        t("particle_appearance_material", Kind.STRING, c -> c.particle_appearance_material, setStr((c, v) -> c.particle_appearance_material = v));
        t("particle_appearance_facing_camera_mode", Kind.STRING, c -> c.particle_appearance_facing_camera_mode, setStr((c, v) -> c.particle_appearance_facing_camera_mode = v));
        t("particle_appearance_direction_mode", Kind.STRING, c -> c.particle_appearance_direction_mode, setStr((c, v) -> c.particle_appearance_direction_mode = v));
        t("particle_appearance_speed_threshold", Kind.NUMBER, c -> c.particle_appearance_speed_threshold, setNum((c, v) -> c.particle_appearance_speed_threshold = v));
        ta("particle_appearance_direction", Kind.MOLANG, 3, c -> c.particle_appearance_direction, setList((c, v) -> c.particle_appearance_direction = v));
        ta("particle_update_expression", Kind.STRING, 0, c -> c.particle_update_expression, setList((c, v) -> c.particle_update_expression = v));
        ta("particle_render_expression", Kind.STRING, 0, c -> c.particle_render_expression, setList((c, v) -> c.particle_render_expression = v));
        t("particle_direction_mode", Kind.STRING, c -> c.particle_direction_mode, setStr((c, v) -> c.particle_direction_mode = v));
        ta("particle_direction_direction", Kind.MOLANG, 3, c -> c.particle_direction_direction, setList((c, v) -> c.particle_direction_direction = v));
        t("particle_motion_mode", Kind.STRING, c -> c.particle_motion_mode, setStr((c, v) -> c.particle_motion_mode = v));
        t("particle_motion_linear_speed", Kind.MOLANG, c -> c.particle_motion_linear_speed, setStr((c, v) -> c.particle_motion_linear_speed = v));
        ta("particle_motion_linear_acceleration", Kind.MOLANG, 3, c -> c.particle_motion_linear_acceleration, setList((c, v) -> c.particle_motion_linear_acceleration = v));
        t("particle_motion_linear_drag_coefficient", Kind.MOLANG, c -> c.particle_motion_linear_drag_coefficient, setStr((c, v) -> c.particle_motion_linear_drag_coefficient = v));
        ta("particle_motion_relative_position", Kind.MOLANG, 3, c -> c.particle_motion_relative_position, setList((c, v) -> c.particle_motion_relative_position = v));
        ta("particle_motion_direction", Kind.MOLANG, 3, c -> c.particle_motion_direction, setList((c, v) -> c.particle_motion_direction = v));
        t("particle_rotation_mode", Kind.STRING, c -> c.particle_rotation_mode, setStr((c, v) -> c.particle_rotation_mode = v));
        t("particle_rotation_initial_rotation", Kind.MOLANG, c -> c.particle_rotation_initial_rotation, setStr((c, v) -> c.particle_rotation_initial_rotation = v));
        t("particle_rotation_rotation_rate", Kind.MOLANG, c -> c.particle_rotation_rotation_rate, setStr((c, v) -> c.particle_rotation_rotation_rate = v));
        t("particle_rotation_rotation_acceleration", Kind.MOLANG, c -> c.particle_rotation_rotation_acceleration, setStr((c, v) -> c.particle_rotation_rotation_acceleration = v));
        t("particle_rotation_rotation_drag_coefficient", Kind.MOLANG, c -> c.particle_rotation_rotation_drag_coefficient, setStr((c, v) -> c.particle_rotation_rotation_drag_coefficient = v));
        t("particle_rotation_rotation", Kind.MOLANG, c -> c.particle_rotation_rotation, setStr((c, v) -> c.particle_rotation_rotation = v));
        t("particle_lifetime_max_lifetime", Kind.MOLANG, c -> c.particle_lifetime_max_lifetime, setStr((c, v) -> c.particle_lifetime_max_lifetime = v));
        ta("particle_lifetime_kill_plane", Kind.NUMBER, 4, c -> c.particle_lifetime_kill_plane, setList((c, v) -> c.particle_lifetime_kill_plane = v));
        t("particle_lifetime_expiration_expression", Kind.MOLANG, c -> c.particle_lifetime_expiration_expression, setStr((c, v) -> c.particle_lifetime_expiration_expression = v));
        ta("particle_lifetime_expire_in", Kind.STRING, 0, c -> c.particle_lifetime_expire_in, setList((c, v) -> c.particle_lifetime_expire_in = v));
        ta("particle_lifetime_expire_outside", Kind.STRING, 0, c -> c.particle_lifetime_expire_outside, setList((c, v) -> c.particle_lifetime_expire_outside = v));
        ta("particle_texture_size", Kind.NUMBER, 2, c -> c.particle_texture_size, setList((c, v) -> c.particle_texture_size = v));
        t("particle_texture_height", Kind.NUMBER, c -> c.particle_texture_height, setNum((c, v) -> c.particle_texture_height = v));
        t("particle_texture_path", Kind.STRING, c -> c.particle_texture_path, setStr((c, v) -> c.particle_texture_path = v));
        t("particle_texture_mode", Kind.STRING, c -> c.particle_texture_mode, setStr((c, v) -> c.particle_texture_mode = v));
        ta("particle_texture_uv", Kind.MOLANG, 2, c -> c.particle_texture_uv, setList((c, v) -> c.particle_texture_uv = v));
        ta("particle_texture_uv_size", Kind.MOLANG, 2, c -> c.particle_texture_uv_size, setList((c, v) -> c.particle_texture_uv_size = v));
        ta("particle_texture_uv_step", Kind.MOLANG, 2, c -> c.particle_texture_uv_step, setList((c, v) -> c.particle_texture_uv_step = v));
        t("particle_texture_frames_per_second", Kind.NUMBER, c -> c.particle_texture_frames_per_second, setNum((c, v) -> c.particle_texture_frames_per_second = v));
        t("particle_texture_max_frame", Kind.MOLANG, c -> c.particle_texture_max_frame, setStr((c, v) -> c.particle_texture_max_frame = v));
        t("particle_texture_stretch_to_lifetime", Kind.BOOLEAN, c -> c.particle_texture_stretch_to_lifetime, setBool((c, v) -> c.particle_texture_stretch_to_lifetime = v));
        t("particle_texture_loop", Kind.BOOLEAN, c -> c.particle_texture_loop, setBool((c, v) -> c.particle_texture_loop = v));
        t("particle_color_mode", Kind.STRING, c -> c.particle_color_mode, setStr((c, v) -> c.particle_color_mode = v));
        t("particle_color_static", Kind.COLOR, c -> c.particle_color_static, setStr((c, v) -> c.particle_color_static = v));
        t("particle_color_interpolant", Kind.MOLANG, c -> c.particle_color_interpolant, setStr((c, v) -> c.particle_color_interpolant = v));
        t("particle_color_range", Kind.NUMBER, c -> c.particle_color_range, setNum((c, v) -> c.particle_color_range = v));
        ta("particle_color_gradient", Kind.OBJECT, 0, c -> c.particle_color_gradient, setList((c, v) -> c.particle_color_gradient = v));
        ta("particle_color_expression", Kind.MOLANG, 4, c -> c.particle_color_expression, setList((c, v) -> c.particle_color_expression = v));
        t("particle_color_light", Kind.BOOLEAN, c -> c.particle_color_light, setBool((c, v) -> c.particle_color_light = v));
        t("particle_collision_toggle", Kind.BOOLEAN, c -> c.particle_collision_toggle, setBool((c, v) -> c.particle_collision_toggle = v));
        t("particle_collision_enabled", Kind.MOLANG, c -> c.particle_collision_enabled, setStr((c, v) -> c.particle_collision_enabled = v));
        t("particle_collision_collision_drag", Kind.NUMBER, c -> c.particle_collision_collision_drag, setNum((c, v) -> c.particle_collision_collision_drag = v));
        t("particle_collision_coefficient_of_restitution", Kind.NUMBER, c -> c.particle_collision_coefficient_of_restitution, setNum((c, v) -> c.particle_collision_coefficient_of_restitution = v));
        t("particle_collision_collision_radius", Kind.NUMBER, c -> c.particle_collision_collision_radius, setNum((c, v) -> c.particle_collision_collision_radius = v));
        t("particle_collision_expire_on_contact", Kind.BOOLEAN, c -> c.particle_collision_expire_on_contact, setBool((c, v) -> c.particle_collision_expire_on_contact = v));
        ta("particle_collision_events", Kind.OBJECT, 0, c -> c.particle_collision_events, setList((c, v) -> c.particle_collision_events = v));
        ta("particle_events_creation", Kind.STRING, 0, c -> c.particle_events_creation, setList((c, v) -> c.particle_events_creation = v));
        ta("particle_events_expiration", Kind.STRING, 0, c -> c.particle_events_expiration, setList((c, v) -> c.particle_events_expiration = v));
        t("particle_events_timeline", Kind.OBJECT, c -> c.particle_events_timeline, setMap((c, v) -> {
            c.particle_events_timeline.clear();
            c.particle_events_timeline.putAll(v);
        }));
    }
}

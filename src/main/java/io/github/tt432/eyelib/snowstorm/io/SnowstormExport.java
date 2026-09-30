package io.github.tt432.eyelib.snowstorm.io;

import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.event.EditorEvent;
import io.github.tt432.eyelib.snowstorm.gradient.Gradient;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import io.github.tt432.eyelib.wintersky.three.MathUtils;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * export.js 逐字移植：generateFile() 从 {@link EditorRuntime#Config} + {@link InputStructure#Data}
 * 重建基岩版 particle JSON（format_version 1.10.0）。
 *
 * <p>JS undefined 在返回结构中以 null 表示；返回前 {@link #clean} 递归删除 Map 中 null 值条目
 * （JSON.stringify 丢弃 undefined 属性），List 中的 null 保留（JS 数组空洞 → JSON null）。
 * 浏览器侧的 downloadFile/IO.export/getName 文件落盘不移植（归 MC 侧，ADR-0036 D6）。
 */
public final class SnowstormExport {

    private SnowstormExport() {
    }

    // ==================================================================
    // Config 访问（configGet 共用 input.Input 的公开反射助手 + Config.types 元数据）
    // ==================================================================

    /** JS {@code Config.constructor.types[key]} 的元数据（type.type / type.array）。 */
    private record TypeInfo(String type, boolean array) {
    }

    private static @Nullable Map<?, ?> typesMap;

    /** JS {@code Config[key]}（统一走 {@link Input#configGet}）。 */
    private static @Nullable Object configGet(Config config, String key) {
        return Input.configGet(config, key);
    }

    // ---- InputStructure 骨架访问（字面量键；缺失即 NPE ≈ JS 读 undefined 属性后崩溃） ----

    private static InputStructure.Group group(String subject, String group) {
        return java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(
                InputStructure.Data.get(subject)).group(group));
    }

    private static Input input(String subject, String group, String key) {
        return java.util.Objects.requireNonNull(group(subject, group).inputs.get(key));
    }

    /** JS {@code Config.constructor.types[key]}（Config.TYPES/Entry 为包私有，反射读取）。 */
    private static @Nullable TypeInfo typeInfo(String key) {
        try {
            Map<?, ?> types = typesMap;
            if (types == null) {
                Field f = Config.class.getDeclaredField("TYPES");
                f.setAccessible(true);
                types = (Map<?, ?>) f.get(null);
                typesMap = types;
            }
            Object entry = types.get(key);
            if (entry == null) return null;
            Method kind = entry.getClass().getDeclaredMethod("kind");
            kind.setAccessible(true);
            Method array = entry.getClass().getDeclaredMethod("array");
            array.setAccessible(true);
            String kindName = ((Enum<?>) kind.invoke(entry)).name().toLowerCase(Locale.ROOT);
            return new TypeInfo(kindName, (Boolean) array.invoke(entry));
        } catch (ReflectiveOperationException | SecurityException e) {
            return null;
        }
    }

    // ==================================================================
    // processValue / getValue
    // ==================================================================

    /** export.js processValue(v, type)；{@code jsType} 为 {@code 'molang'/'number'/'string'/...}。 */
    private static @Nullable Object processValue(@Nullable Object v, String jsType) {
        if ("molang".equals(jsType)) {
            if (!Double.isNaN(JsSemantics.toNumber(v))) { // JS: !isNaN(v)
                v = JsonValues.jsParseFloat(v);
            } else if (v instanceof String s && s.contains("\n")) {
                v = s.replaceAll("[\\r\\n]+", "");
            }
            if (!JsSemantics.truthy(v)) v = 0.0;
        } else if ("number".equals(jsType) && !(v instanceof Number)) { // JS: typeof v !== 'number'
            v = orZero(JsonValues.jsParseFloat(v));
        }
        return v;
    }

    /** export.js getValue(key, required)。JS undefined → null。 */
    private static @Nullable Object getValue(String key, boolean required) {
        Object value = configGet(EditorRuntime.Config, key);
        TypeInfo type = typeInfo(key);
        if (type == null) return null;

        if (type.array()) {
            List<Object> result = new ArrayList<>();
            // JS: for (var num of value) —— value 非数组则 JS TypeError；requireNonNull 等价
            for (Object num : java.util.Objects.requireNonNull(JsonValues.asList(value))) {
                Object processed = processValue(num, type.type());
                if ("string".equals(type.type()) && processed instanceof String s && s.contains("\n")) {
                    // JS: value.split(/\s*\n+\s*/g).filter(line => line.length)
                    for (String line : s.split("\\s*\\n+\\s*")) {
                        if (!line.isEmpty()) result.add(line);
                    }
                } else {
                    result.add(processed);
                }
            }
            // JS quirk as-is：无 truthy 元素且非 required → undefined（空数组也是 undefined）
            if (result.stream().noneMatch(JsSemantics::truthy) && !required) return null;
            return result;
        } else {
            Object result = processValue(value, type.type());
            if (!JsSemantics.truthy(result) && !required) return null;
            return result;
        }
    }

    /** JS {@code x || 0}（NaN/0 → 0）。 */
    private static double orZero(double v) {
        return (v == 0 || Double.isNaN(v)) ? 0 : v;
    }

    // ==================================================================
    // 事件格式化
    // ==================================================================

    /** export.js formatEventList。 */
    private static @Nullable Object formatEventList(@Nullable List<Object> list) {
        if (list == null) return null;
        if (list.size() == 1) return list.get(0);
        if (list.size() > 1) return list;
        return null;
    }

    /** export.js formatEventTimeline。 */
    private static @Nullable Map<String, Object> formatEventTimeline(@Nullable Map<String, Object> source) {
        boolean hasData = false;
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, Object> e : source.entrySet()) {
                Object formatted = formatEventList(JsonValues.asList(e.getValue()));
                copy.put(e.getKey(), formatted);
                if (JsSemantics.truthy(formatted)) hasData = true;
            }
        }
        return hasData ? copy : null;
    }

    /** export.js formatEventTimelineLooping。 */
    private static @Nullable List<Object> formatEventTimelineLooping(@Nullable List<Object> source) {
        if (source == null) return null;
        List<Object> copyList = new ArrayList<>();
        for (Object o : source) {
            Map<String, Object> entry = JsonValues.asMap(o);
            if (entry == null) continue;
            Map<String, Object> copy = new LinkedHashMap<>();
            copy.put("distance", entry.get("distance"));
            copy.put("effects", formatEventList(JsonValues.asList(entry.get("effects"))));
            copyList.add(copy);
        }
        return copyList.isEmpty() ? null : copyList;
    }

    /** export.js cleanEvent（作用于 toJson() 产出的全新 Map 树，等价 JS 的 JSON 深拷贝后清理）。 */
    private static @Nullable Map<String, Object> cleanEvent(@Nullable Map<String, Object> subpart) {
        if (subpart == null) return null;
        if (subpart.get("randomize") instanceof List<?> randomize) {
            for (Object o : randomize) {
                Map<String, Object> option = JsonValues.asMap(o);
                if (option != null) {
                    option.remove("uuid");
                    cleanEvent(option);
                }
            }
        }
        if (subpart.get("sequence") instanceof List<?> sequence) {
            for (Object o : sequence) {
                Map<String, Object> option = JsonValues.asMap(o);
                if (option != null) {
                    option.remove("uuid");
                    cleanEvent(option);
                }
            }
        }
        Map<String, Object> particleEffect = JsonValues.asMap(subpart.get("particle_effect"));
        if (particleEffect != null) {
            Object expr = particleEffect.get("pre_effect_expression");
            if (!JsSemantics.truthy(expr)) {
                particleEffect.remove("pre_effect_expression");
            } else if (expr instanceof String s) {
                s = s.trim();
                if (!s.endsWith(";")) s = s + ";";
                particleEffect.put("pre_effect_expression", s);
            }
        }
        return subpart;
    }

    // ==================================================================
    // generateFile
    // ==================================================================

    /** export.js generateFile()：返回基岩 particle JSON 的对象形态（Map 树，undefined 条目已清理）。 */
    public static Map<String, Object> generateFile() {
        Config Config = EditorRuntime.Config;
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("format_version", "1.10.0");
        Map<String, Object> particleEffect = new LinkedHashMap<>();
        file.put("particle_effect", particleEffect);
        Map<String, Object> description = new LinkedHashMap<>();
        particleEffect.put("description", description);
        description.put("identifier", Config.identifier);
        Map<String, Object> basicRenderParameters = new LinkedHashMap<>();
        description.put("basic_render_parameters", basicRenderParameters);
        basicRenderParameters.put("material",
                input("appearance", "appearance", "material").getValue());
        Object texturePath = getValue("particle_texture_path", false);
        basicRenderParameters.put("texture",
                JsSemantics.truthy(texturePath) ? texturePath : "textures/blocks/wool_colored_white");

        //Curves
        Map<String, Object> jsonCurves = new LinkedHashMap<>();
        for (Map.Entry<String, Config.Curve> e : Config.curves.entrySet()) {
            Config.Curve curve = e.getValue();
            Map<String, Object> jsonCurve = new LinkedHashMap<>();
            jsonCurve.put("type", processValue(curve.mode, "string"));
            jsonCurve.put("input", processValue(curve.input, "molang"));
            jsonCurve.put("horizontal_range",
                    "bezier_chain".equals(curve.mode) ? null : processValue(curve.range, "molang"));
            List<Object> nodes = new ArrayList<>(curve.nodes);
            if ("bezier_chain".equals(jsonCurve.get("type"))) {
                Map<String, Object> nodesObj = new LinkedHashMap<>();
                for (Object o : nodes) {
                    Config.BezierNode node = (Config.BezierNode) o;
                    String time = JsSemantics.toJsString(SnowstormUtil.roundTo(node.time, 2));
                    if (!time.contains(".")) time += ".0";
                    Map<String, Object> jsonNode = new LinkedHashMap<>();
                    jsonNode.put("value", node.right_value == node.left_value ? node.left_value : null);
                    jsonNode.put("left_value", node.right_value == node.left_value ? null : node.left_value);
                    jsonNode.put("right_value", node.right_value == node.left_value ? null : node.right_value);
                    jsonNode.put("slope", node.right_slope == node.left_slope ? node.left_slope : null);
                    jsonNode.put("left_slope", node.right_slope == node.left_slope ? null : node.left_slope);
                    jsonNode.put("right_slope", node.right_slope == node.left_slope ? null : node.right_slope);
                    nodesObj.put(time, jsonNode);
                }
                jsonCurve.put("nodes", nodesObj);
            } else {
                jsonCurve.put("nodes", nodes);
            }
            jsonCurves.put(e.getKey(), jsonCurve);
        }
        if (!jsonCurves.isEmpty()) {
            particleEffect.put("curves", jsonCurves);
        }

        //Events
        List<Object> editorEvents = group("events", "events").events;
        if (!editorEvents.isEmpty()) {
            Map<String, Object> events = new LinkedHashMap<>();
            particleEffect.put("events", events);
            for (Object o : editorEvents) {
                EditorEvent entry = (EditorEvent) o;
                // JS: JSON.parse(JSON.stringify(entry.event)) —— toJson() 产出全新 Map 树，等价深拷贝
                events.put(entry.id, cleanEvent(entry.event.toJson()));
            }
        }

        Map<String, Object> comps = new LinkedHashMap<>();
        particleEffect.put("components", comps);

        //Emitter Components
        Object creationVars = getValue("variables_creation_vars", false);
        if (JsSemantics.truthy(creationVars)) {
            String s = join(JsonValues.asList(creationVars), ";") + ";";
            s = s.replaceAll(";;+", ";");
            if (!s.isEmpty()) {
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("creation_expression", s);
                comps.put("minecraft:emitter_initialization", comp);
            }
        }
        Object tickVars = getValue("variables_tick_vars", false);
        if (JsSemantics.truthy(tickVars)) {
            String s = join(JsonValues.asList(tickVars), ";") + ";";
            s = s.replaceAll(";;+", ";");
            if (!s.isEmpty()) {
                Map<String, Object> comp = JsonValues.asMap(comps.get("minecraft:emitter_initialization"));
                if (comp == null) {
                    comp = new LinkedHashMap<>();
                    comps.put("minecraft:emitter_initialization", comp);
                }
                comp.put("per_update_expression", s);
            }
        }
        Object localPosition = getValue("space_local_position", true);
        if (JsSemantics.truthy(localPosition)) {
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("position", localPosition);
            comp.put("rotation", getValue("space_local_rotation", true));
            Object localVelocity = getValue("space_local_velocity", true);
            comp.put("velocity", JsSemantics.truthy(localVelocity) ? localVelocity : null);
            comps.put("minecraft:emitter_local_space", comp);
        }
        //Rate
        Object mode = getValue("emitter_rate_mode", false);
        if ("instant".equals(mode)) {
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("num_particles", getValue("emitter_rate_amount", true));
            comps.put("minecraft:emitter_rate_instant", comp);
        } else if ("steady".equals(mode)) {
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("spawn_rate", getValue("emitter_rate_rate", false));
            comp.put("max_particles", getValue("emitter_rate_maximum", false));
            comps.put("minecraft:emitter_rate_steady", comp);
        } else if ("manual".equals(mode)) {
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("max_particles", getValue("emitter_rate_maximum", false));
            comps.put("minecraft:emitter_rate_manual", comp);
        }
        //Lifetime
        mode = getValue("emitter_lifetime_mode", false);
        if (mode != null) {
            if ("looping".equals(mode)) {
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("active_time", getValue("emitter_lifetime_active_time", false));
                comp.put("sleep_time", getValue("emitter_lifetime_sleep_time", false));
                comps.put("minecraft:emitter_lifetime_looping", comp);
            } else if ("once".equals(mode)) {
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("active_time", getValue("emitter_lifetime_active_time", false));
                comps.put("minecraft:emitter_lifetime_once", comp);
            } else if ("expression".equals(mode)) {
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("activation_expression", getValue("emitter_lifetime_activation", false));
                comp.put("expiration_expression", getValue("emitter_lifetime_expiration", false));
                comps.put("minecraft:emitter_lifetime_expression", comp);
            }
        }
        //Emitter Events
        Map<String, Object> emitterEvents = new LinkedHashMap<>();
        emitterEvents.put("creation_event", formatEventList(new ArrayList<>(Config.emitter_events_creation)));
        emitterEvents.put("expiration_event", formatEventList(new ArrayList<>(Config.emitter_events_expiration)));
        emitterEvents.put("timeline", formatEventTimeline(Config.emitter_events_timeline));
        emitterEvents.put("travel_distance_events", formatEventTimeline(Config.emitter_events_distance));
        emitterEvents.put("looping_travel_distance_events",
                formatEventTimelineLooping(new ArrayList<>(Config.emitter_events_distance_looping)));
        if (JsSemantics.truthy(emitterEvents.get("creation_event"))
                || JsSemantics.truthy(emitterEvents.get("expiration_event"))
                || JsSemantics.truthy(emitterEvents.get("timeline"))
                || JsSemantics.truthy(emitterEvents.get("travel_distance_events"))
                || JsSemantics.truthy(emitterEvents.get("looping_travel_distance_events"))) {
            comps.put("minecraft:emitter_lifetime_events", emitterEvents);
        }
        //Direction
        mode = getValue("particle_direction_mode", false);
        Object direction = null;
        if (mode != null) {
            if ("inwards".equals(mode)) {
                direction = "inwards";
            } else if ("outwards".equals(mode)) {
                direction = "outwards";
            } else if ("direction".equals(mode)) {
                direction = getValue("particle_direction_direction", false);
            }
        }
        //Shape
        mode = getValue("emitter_shape_mode", false);
        if (mode != null) {
            if ("point".equals(mode)) {
                if (direction instanceof String) direction = null;
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("offset", getValue("emitter_shape_offset", false));
                comp.put("direction", direction);
                comps.put("minecraft:emitter_shape_point", comp);
            } else if ("sphere".equals(mode)) {
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("offset", getValue("emitter_shape_offset", false));
                comp.put("radius", getValue("emitter_shape_radius", false));
                comp.put("surface_only", getValue("emitter_shape_surface_only", false));
                comp.put("direction", direction);
                comps.put("minecraft:emitter_shape_sphere", comp);
            } else if ("box".equals(mode)) {
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("offset", getValue("emitter_shape_offset", false));
                comp.put("half_dimensions", getValue("emitter_shape_half_dimensions", false));
                comp.put("surface_only", getValue("emitter_shape_surface_only", false));
                comp.put("direction", direction);
                comps.put("minecraft:emitter_shape_box", comp);
            } else if ("disc".equals(mode)) {
                Object planeNormal = getValue("emitter_shape_plane_normal", false);
                if (planeNormal != null) {
                    // JS: switch (plane_normal.join('')) { '100'→'x' / '010'→'y' / '001'→'z' }
                    String joined = join(JsonValues.asList(planeNormal), "");
                    switch (joined) {
                        case "100" -> planeNormal = "x";
                        case "010" -> planeNormal = "y";
                        case "001" -> planeNormal = "z";
                        default -> {
                        }
                    }
                }
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("offset", getValue("emitter_shape_offset", false));
                comp.put("radius", getValue("emitter_shape_radius", false));
                comp.put("plane_normal", planeNormal);
                comp.put("surface_only", getValue("emitter_shape_surface_only", false));
                comp.put("direction", direction);
                comps.put("minecraft:emitter_shape_disc", comp);
            } else if ("custom".equals(mode)) {
                if (direction instanceof String) direction = null;
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("offset", getValue("emitter_shape_offset", false));
                comp.put("direction", direction);
                comps.put("minecraft:emitter_shape_custom", comp);
            } else if ("entity_aabb".equals(mode)) {
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("surface_only", getValue("emitter_shape_surface_only", false));
                comp.put("direction", direction);
                comps.put("minecraft:emitter_shape_entity_aabb", comp);
            }
        }

        //Particle Components

        // Variables
        Object particleUpdate = getValue("particle_update_expression", false);
        if (JsSemantics.truthy(particleUpdate)) {
            String s = join(JsonValues.asList(particleUpdate), ";") + ";";
            s = s.replaceAll(";;+", ";");
            if (!s.isEmpty()) {
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("per_update_expression", s);
                comps.put("minecraft:particle_initialization", comp);
            }
        }
        Object particleRender = getValue("particle_render_expression", false);
        if (JsSemantics.truthy(particleRender)) {
            String s = join(JsonValues.asList(particleRender), ";") + ";";
            s = s.replaceAll(";;+", ";");
            if (!s.isEmpty()) {
                Map<String, Object> comp = JsonValues.asMap(comps.get("minecraft:particle_initialization"));
                if (comp == null) {
                    comp = new LinkedHashMap<>();
                    comps.put("minecraft:particle_initialization", comp);
                }
                comp.put("per_render_expression", s);
            }
        }

        //Lifetime
        Map<String, Object> lifetimeExpression = new LinkedHashMap<>();
        lifetimeExpression.put("max_lifetime", getValue("particle_lifetime_max_lifetime", false));
        lifetimeExpression.put("expiration_expression", getValue("particle_lifetime_expiration_expression", false));
        comps.put("minecraft:particle_lifetime_expression", lifetimeExpression);
        Object expireIn = getValue("particle_lifetime_expire_in", false);
        if (JsSemantics.truthy(expireIn)) {
            comps.put("minecraft:particle_expire_if_in_blocks", expireIn);
        }
        Object expireOutside = getValue("particle_lifetime_expire_outside", false);
        if (JsSemantics.truthy(expireOutside)) {
            comps.put("minecraft:particle_expire_if_not_in_blocks", expireOutside);
        }

        //Particle Events
        Map<String, Object> particleEvents = new LinkedHashMap<>();
        particleEvents.put("creation_event", formatEventList(new ArrayList<>(Config.particle_events_creation)));
        particleEvents.put("expiration_event", formatEventList(new ArrayList<>(Config.particle_events_expiration)));
        particleEvents.put("timeline", formatEventTimeline(Config.particle_events_timeline));
        if (JsSemantics.truthy(particleEvents.get("creation_event"))
                || JsSemantics.truthy(particleEvents.get("expiration_event"))
                || JsSemantics.truthy(particleEvents.get("timeline"))) {
            comps.put("minecraft:particle_lifetime_events", particleEvents);
        }

        //Spin
        Object initRot = getValue("particle_rotation_initial_rotation", false);
        Object initRotRate = getValue("particle_rotation_rotation_rate", false);
        if (JsSemantics.truthy(initRot) || JsSemantics.truthy(initRotRate)) {
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("rotation", JsSemantics.truthy(initRot) ? initRot : null);
            comp.put("rotation_rate", JsSemantics.truthy(initRotRate) ? initRotRate : null);
            comps.put("minecraft:particle_initial_spin", comp);
        }
        comps.put("minecraft:particle_initial_speed", getValue("particle_motion_linear_speed", false));

        //Motion
        mode = getValue("particle_motion_mode", false);
        if (mode != null) {
            if ("dynamic".equals(mode)) {
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("linear_acceleration", getValue("particle_motion_linear_acceleration", false));
                comp.put("linear_drag_coefficient", getValue("particle_motion_linear_drag_coefficient", false));
                comps.put("minecraft:particle_motion_dynamic", comp);
                if (!JsSemantics.truthy(comps.get("minecraft:particle_initial_speed"))) {
                    comps.put("minecraft:particle_initial_speed", 0.0);
                }
            } else if ("parametric".equals(mode)) {
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("relative_position", getValue("particle_motion_relative_position", false));
                comp.put("direction", getValue("particle_motion_direction", false));
                comps.put("minecraft:particle_motion_parametric", comp);
            }
        }

        //Rotation
        mode = getValue("particle_rotation_mode", false);
        if (mode != null) {
            if ("dynamic".equals(mode)) {
                Object rotationAcceleration = getValue("particle_rotation_rotation_acceleration", false);
                Object rotationDragCoefficient = getValue("particle_rotation_rotation_drag_coefficient", false);
                if (JsSemantics.truthy(rotationAcceleration) || JsSemantics.truthy(rotationDragCoefficient)) {
                    Map<String, Object> dynMo = JsonValues.asMap(comps.get("minecraft:particle_motion_dynamic"));
                    if (dynMo == null) {
                        dynMo = new LinkedHashMap<>();
                        comps.put("minecraft:particle_motion_dynamic", dynMo);
                    }
                    dynMo.put("rotation_acceleration", rotationAcceleration);
                    dynMo.put("rotation_drag_coefficient", rotationDragCoefficient);
                }
            } else if ("parametric".equals(mode)) {
                Object rotation = getValue("particle_rotation_rotation", false);
                if (JsSemantics.truthy(rotation)) {
                    Map<String, Object> comp = JsonValues.asMap(comps.get("minecraft:particle_motion_parametric"));
                    if (comp == null) {
                        comp = new LinkedHashMap<>();
                        comps.put("minecraft:particle_motion_parametric", comp);
                    }
                    comp.put("rotation", rotation);
                }
            }
        }

        //Kill Plane
        comps.put("minecraft:particle_kill_plane", getValue("particle_lifetime_kill_plane", false));

        //Texture
        // JS: facing_camera_mode 为 undefined 时 .substring 抛 TypeError；requireNonNull 等价
        String facingCameraMode = (String) java.util.Objects.requireNonNull(
                getValue("particle_appearance_facing_camera_mode", false));
        Map<String, Object> texComp = new LinkedHashMap<>();
        comps.put("minecraft:particle_appearance_billboard", texComp);
        texComp.put("size", getValue("particle_appearance_size", true));
        texComp.put("facing_camera_mode", facingCameraMode);
        if ((JsSemantics.jsSubstr(facingCameraMode, 0, 9).equals("direction") || "lookat_direction".equals(facingCameraMode))
                && (JsSemantics.toNumber(getValue("particle_appearance_speed_threshold", false)) != 0.01
                || !"derive_from_velocity".equals(getValue("particle_appearance_direction_mode", false)))) {
            Map<String, Object> directionComp = new LinkedHashMap<>();
            texComp.put("direction", directionComp);
            Object directionMode = getValue("particle_appearance_direction_mode", false);
            directionComp.put("mode", directionMode);
            if ("derive_from_velocity".equals(directionMode)) {
                directionComp.put("min_speed_threshold", getValue("particle_appearance_speed_threshold", false));
            } else {
                directionComp.put("custom_direction", getValue("particle_appearance_direction", false));
            }
        }
        if (!"full".equals(getValue("particle_texture_mode", false))) {
            Map<String, Object> uv = new LinkedHashMap<>();
            texComp.put("uv", uv);
            double textureWidth = orZero(jsParseInt(listGet(Config.particle_texture_size, 0)));
            double textureHeight = orZero(jsParseInt(listGet(Config.particle_texture_size, 1)));
            uv.put("texture_width", textureWidth);
            uv.put("texture_height", textureHeight);
            if ("static".equals(getValue("particle_texture_mode", false))) {
                Object uvStart = getValue("particle_texture_uv", false);
                uv.put("uv", uvStart != null ? uvStart : List.of(0.0, 0.0));
                Object uvSize = getValue("particle_texture_uv_size", false);
                uv.put("uv_size", uvSize != null ? uvSize : List.of(textureWidth, textureHeight));
            } else {
                Map<String, Object> flipbook = new LinkedHashMap<>();
                uv.put("flipbook", flipbook);
                flipbook.put("base_UV", getValue("particle_texture_uv", true));
                flipbook.put("size_UV", getValue("particle_texture_uv_size", true));
                flipbook.put("step_UV", getValue("particle_texture_uv_step", true));
                flipbook.put("frames_per_second", getValue("particle_texture_frames_per_second", false));
                flipbook.put("max_frame", getValue("particle_texture_max_frame", false));
                flipbook.put("stretch_to_lifetime", getValue("particle_texture_stretch_to_lifetime", false));
                flipbook.put("loop", getValue("particle_texture_loop", false));
            }
        }
        //Collision
        if (JsSemantics.truthy(getValue("particle_collision_toggle", false))) {
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("enabled", getValue("particle_collision_enabled", false));
            comp.put("collision_drag", getValue("particle_collision_collision_drag", false));
            comp.put("coefficient_of_restitution", getValue("particle_collision_coefficient_of_restitution", false));
            comp.put("collision_radius", getValue("particle_collision_collision_radius", false));
            comp.put("expire_on_contact", getValue("particle_collision_expire_on_contact", false));
            comp.put("events", getValue("particle_collision_events", false));
            comps.put("minecraft:particle_motion_collision", comp);
        }
        if (JsSemantics.truthy(getValue("particle_color_light", false))) {
            comps.put("minecraft:particle_appearance_lighting", new LinkedHashMap<>());
        }
        Object colorMode = getValue("particle_color_mode", false);
        if ("static".equals(colorMode)) {
            Object staticColor = getValue("particle_color_static", false);
            // JS: getValue('particle_color_static').substr(1, 8)（undefined 会抛 TypeError，as-is）
            String value = JsSemantics.jsSubstr(
                    (String) java.util.Objects.requireNonNull(staticColor), 1, 8);
            if (!value.toLowerCase(Locale.ROOT).equals("ffffff")) {
                // JS: value.match(/.{2}/g).map(c => parseInt(c, 16) / 255)
                List<Object> color = new ArrayList<>();
                for (int i = 0; i + 2 <= value.length(); i += 2) {
                    color.add(Integer.parseInt(value.substring(i, i + 2), 16) / 255.0);
                }
                if (color.size() == 3) color.add(1.0);
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("color", color);
                comps.put("minecraft:particle_appearance_tinting", comp);
            }
        } else if ("gradient".equals(colorMode)) {
            Object range = getValue("particle_color_range", false);
            Map<String, Object> color = new LinkedHashMap<>();
            color.put("interpolant", getValue("particle_color_interpolant", false));
            Gradient gradient = (Gradient) input("appearance", "color", "gradient");
            // JS: range || 1（0 也取 1，as-is）
            color.put("gradient", gradient.export(JsSemantics.truthy(range) ? JsSemantics.toNumber(range) : 1));
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("color", color);
            comps.put("minecraft:particle_appearance_tinting", comp);
        } else {
            Object color = getValue("particle_color_expression", false);
            if (color instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Object> colorList = (List<Object>) color;
                for (int i = 0; i < colorList.size(); i++) {
                    Object s = colorList.get(i);
                    if (s instanceof Number n) {
                        colorList.set(i, MathUtils.clamp(n.doubleValue(), 0, 1));
                    }
                }
                // JS: if (!color[3]) color[3] = 1（短数组补空洞 → JSON null，as-is）
                Object c3 = colorList.size() > 3 ? colorList.get(3) : null;
                if (!JsSemantics.truthy(c3)) {
                    while (colorList.size() < 3) colorList.add(null);
                    if (colorList.size() == 3) colorList.add(1.0);
                    else colorList.set(3, 1.0);
                }
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("color", colorList);
                comps.put("minecraft:particle_appearance_tinting", comp);
            }
        }

        clean(file);
        return file;
    }

    /** export.js getName()（JS 内部函数；公开给 MC 侧落盘命名用）。 */
    public static String getName() {
        Object name = input("effect", "meta", "identifier").getValue();
        if (JsSemantics.truthy(name)) {
            return JsSemantics.toJsString(name).replaceFirst("^\\w+:", "");
        }
        return "particles";
    }

    // ==================================================================
    // 内部助手
    // ==================================================================

    /** JS Array.prototype.join：元素经 String() 转换（数字 1→"1"）。 */
    private static String join(@Nullable List<Object> list, String separator) {
        if (list == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(separator);
            sb.append(JsSemantics.toJsString(list.get(i)));
        }
        return sb.toString();
    }

    private static @Nullable Object listGet(List<Object> list, int i) {
        return i < list.size() ? list.get(i) : null;
    }

    /**
     * JS parseInt（数字截断取整 / 字符串解析整数前缀，失败 NaN）。
     * JsonValues 无 jsParseInt，本函数为 export 专用（见偏离记录）。
     */
    private static double jsParseInt(@Nullable Object v) {
        if (v instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return Double.NaN;
            return d < 0 ? Math.ceil(d) : Math.floor(d);
        }
        if (!(v instanceof String s)) return Double.NaN;
        s = s.stripLeading();
        int i = 0;
        if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
        int start = i;
        while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
        if (i == start) return Double.NaN;
        try {
            return Double.parseDouble(s.substring(0, i));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * JSON.stringify 的 undefined 语义：递归删除 Map 中 null 值条目；List 中的 null 保留
     * （JS 数组空洞序列化为 null）。
     */
    @SuppressWarnings("unchecked")
    private static void clean(@Nullable Object node) {
        if (node instanceof Map<?, ?>) {
            Map<String, Object> map = (Map<String, Object>) node;
            map.values().removeIf(java.util.Objects::isNull);
            for (Object v : map.values()) clean(v);
        } else if (node instanceof List<?> list) {
            for (Object v : list) clean(v);
        }
    }
}

package io.github.tt432.eyelib.snowstorm.editor;

import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import io.github.tt432.eyelib.snowstorm.input.InputType;
import io.github.tt432.eyelib.snowstorm.texture.TextureClass;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Snowstorm QuickSetup.vue 的 PRESETS 数据 + 应用逻辑 as-is 移植（纯数据层，UI 不移植）。
 *
 * <p>覆盖：PRESETS（shape/timing/collision/sprite 四组预设 → input 路径值映射）、
 * DEFAULTS、滑杆 watch 回写（speed/amount/particle_lifetime/lighting/random_rotation）、
 * {@code set(key, value)} 预设应用、sprite 选择写 {@code Texture.source}（dataURL）、
 * {@code resetAll()} 与 {@code mounted()} 同步逻辑。
 *
 * <p>偏离（vs JS/Vue）：
 * <ul>
 *   <li>Vue watch 为异步批处理，Java 为同步调用；最终 Config/Input 状态一致，
 *       registerEdit 时序略异。</li>
 *   <li>滑杆状态字段（speed/amount/particle_lifetime）JS 偶尔暂存字符串（预设值直赋），
 *       Java 一律 double；watch 触发时机在「同值不同类型」边界略异，无可观察配置差异。</li>
 *   <li>sprite 的 webpack asset/inline import → classpath 资源
 *       {@code snowstorm/sprites/<id>.png} 运行时转 dataURL（与 webpack.mix.js
 *       generator 完全一致的 {@code data:image/png;base64,} 形式）。</li>
 *   <li>molang 数组元素 toString 原地改写 PRESETS 列表的 JS 怪癖保留（列表可变）。</li>
 *   <li>lucide 图标、show_sprite_license、CSS 动画为 UI 层内容，不移植。</li>
 * </ul>
 */
public final class QuickSetupPresets {

    private QuickSetupPresets() {
    }

    // ==================================================================
    // QuickSetup.vue:159-350 PRESETS
    // ==================================================================

    /** JS PRESETS：{key: {preset: {input.path: value}}}。列表值可变（molang coercion 原地改写，as-is）。 */
    public static final Map<String, Map<String, Map<String, Object>>> PRESETS = new LinkedHashMap<>();

    private static Map<String, Object> preset(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    private static Map<String, Map<String, Object>> group(Object... kv) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            @SuppressWarnings("unchecked")
            Map<String, Object> v = (Map<String, Object>) kv[i + 1];
            map.put((String) kv[i], v);
        }
        return map;
    }

    private static List<Object> list(Object... values) {
        List<Object> list = new ArrayList<>(values.length);
        for (Object v : values) list.add(v);
        return list;
    }

    static {
        PRESETS.put("shape", group(
                "sphere", preset(
                        "emitter.shape.mode", "sphere",
                        "emitter.shape.radius", "1",
                        "emitter.shape.surface_only", false,
                        "motion.motion.direction_mode", "outwards",
                        "emitter.shape.offset", list("0", "0", "0"),
                        "motion.motion.mode", "dynamic",
                        "motion.motion.linear_acceleration", list("0", "0", "0"),
                        "motion.motion.linear_speed", "3"),
                "rain", preset(
                        "emitter.shape.mode", "disc",
                        "emitter.shape.radius", "6",
                        "emitter.shape.surface_only", false,
                        "motion.motion.direction_mode", "direction",
                        "motion.motion.direction", list("0", "-1", "0"),
                        "emitter.shape.offset", list("0", "6", "0"),
                        "motion.motion.mode", "dynamic",
                        "motion.motion.linear_acceleration", list("0", "-6", "0"),
                        "motion.motion.linear_speed", "5"),
                "ring", preset(
                        "emitter.shape.mode", "disc",
                        "emitter.shape.radius", "3",
                        "emitter.shape.surface_only", true,
                        "motion.motion.direction_mode", "direction",
                        "motion.motion.direction", list("0", "1", "0"),
                        "emitter.shape.offset", list("0", "0.5", "0"),
                        "motion.motion.mode", "dynamic",
                        "motion.motion.linear_acceleration", list("0", "0", "0"),
                        "motion.motion.linear_speed", "math.random(1, 4)"),
                "gravitate", preset(
                        "emitter.shape.mode", "sphere",
                        "emitter.shape.radius", "2",
                        "emitter.shape.surface_only", true,
                        "motion.motion.direction_mode", "inwards",
                        "motion.motion.linear_speed", "2",
                        "emitter.shape.offset", list("0", "0", "0"),
                        "motion.motion.mode", "dynamic",
                        "motion.motion.linear_acceleration", list("0", "0", "0"),
                        "lifetime.lifetime.max_lifetime", "1")));

        PRESETS.put("timing", group(
                "burst", preset(
                        "emitter.rate.mode", "instant",
                        "emitter.rate.amount", "100",
                        "emitter.lifetime.mode", "once",
                        "emitter.lifetime.active_time", "1"),
                "steady", preset(
                        "emitter.rate.mode", "steady",
                        "emitter.rate.rate", "60",
                        "emitter.rate.maximum", "400",
                        "emitter.lifetime.mode", "once",
                        "emitter.lifetime.active_time", "1")));

        PRESETS.put("sprite", group(
                "ball", preset(
                        "appearance.appearance.size", list("0.4", "0.4"),
                        "texture.uv.size", list(16.0, 16.0),
                        "texture.uv.uv", list(0.0, 0.0),
                        "texture.uv.uv_size", list(16.0, 16.0),
                        "texture.uv.mode", "full"),
                "dirt", preset(
                        "appearance.appearance.size", list("0.5", "0.5"),
                        "texture.uv.size", list(16.0, 16.0),
                        "texture.uv.uv", list(0.0, 0.0),
                        "texture.uv.uv_size", list(16.0, 16.0),
                        "texture.uv.mode", "full"),
                "leaves", preset(
                        "appearance.appearance.size", list("0.25", "0.25"),
                        "texture.uv.size", list(16.0, 16.0),
                        "texture.uv.uv", list("Math.floor(v.particle_random_3 * 2) * 8", "Math.floor(v.particle_random_4 * 2) * 8"),
                        "texture.uv.uv_size", list(8.0, 8.0),
                        "texture.uv.mode", "static"),
                "smoke", preset(
                        "appearance.appearance.size", list("0.5", "0.5"),
                        "texture.uv.size", list(16.0, 128.0),
                        "texture.uv.uv", list(0.0, 0.0),
                        "texture.uv.uv_size", list(16.0, 16.0),
                        "texture.uv.mode", "animated",
                        "texture.uv.uv_step", list(0.0, 16.0),
                        "texture.uv.frames_per_second", "12",
                        "texture.uv.max_frame", "8",
                        "texture.uv.stretch_to_lifetime", true,
                        "texture.uv.loop", false),
                "dust", preset(
                        "appearance.appearance.size", list("0.3", "0.3"),
                        "texture.uv.size", list(8.0, 64.0),
                        "texture.uv.uv", list(0.0, 0.0),
                        "texture.uv.uv_size", list(8.0, 8.0),
                        "texture.uv.mode", "animated",
                        "texture.uv.uv_step", list(0.0, 8.0),
                        "texture.uv.frames_per_second", "12",
                        "texture.uv.max_frame", "8",
                        "texture.uv.stretch_to_lifetime", true,
                        "texture.uv.loop", false),
                "sparkle", preset(
                        "appearance.appearance.size", list("0.5", "0.5"),
                        "texture.uv.size", list(16.0, 64.0),
                        "texture.uv.uv", list(0.0, 0.0),
                        "texture.uv.uv_size", list(16.0, 16.0),
                        "texture.uv.mode", "animated",
                        "texture.uv.uv_step", list(0.0, 16.0),
                        "texture.uv.frames_per_second", "15",
                        "texture.uv.max_frame", "4",
                        "texture.uv.stretch_to_lifetime", false,
                        "texture.uv.loop", true),
                "magic", preset(
                        "appearance.appearance.size", list("0.25", "0.25"),
                        "texture.uv.size", list(16.0, 128.0),
                        "texture.uv.uv", list("Math.floor(v.particle_random_3 * 2) * 8", "Math.floor(v.particle_random_4 * 2) * 8"),
                        "texture.uv.uv_size", list(8.0, 8.0),
                        "texture.uv.mode", "animated",
                        "texture.uv.uv_step", list(0.0, 16.0),
                        "texture.uv.frames_per_second", "12",
                        "texture.uv.max_frame", "8",
                        "texture.uv.stretch_to_lifetime", true,
                        "texture.uv.loop", false)));

        PRESETS.put("collision", group(
                "none", preset(
                        "motion.collision.toggle", true,
                        "motion.motion.mode", "dynamic",
                        "motion.motion.linear_acceleration", list("", "", ""),
                        "motion.motion.linear_drag_coefficient", 0.0,
                        "motion.collision.collision_radius", 0.0,
                        "motion.collision.collision_drag", 0.0,
                        "motion.collision.condition", "",
                        "motion.collision.coefficient_of_restitution", 0.0,
                        "motion.collision.expire_on_contact", false),
                "solid", preset(
                        "motion.collision.toggle", true,
                        "motion.motion.mode", "dynamic",
                        "motion.motion.linear_acceleration", list(0.0, -10.0, 0.0),
                        "motion.motion.linear_drag_coefficient", 0.1,
                        "motion.collision.collision_radius", 0.2,
                        "motion.collision.collision_drag", 1.0,
                        "motion.collision.condition", "",
                        "motion.collision.coefficient_of_restitution", 0.3,
                        "motion.collision.expire_on_contact", false),
                "smoke", preset(
                        "motion.collision.toggle", true,
                        "motion.motion.mode", "dynamic",
                        "motion.motion.linear_acceleration", list(0.0, 1.0, 0.0),
                        "motion.motion.linear_drag_coefficient", 4.0,
                        "motion.collision.collision_radius", 0.2,
                        "motion.collision.collision_drag", 0.4,
                        "motion.collision.condition", "",
                        "motion.collision.coefficient_of_restitution", 0.0,
                        "motion.collision.expire_on_contact", false),
                "ball", preset(
                        "motion.collision.toggle", true,
                        "motion.motion.mode", "dynamic",
                        "motion.motion.linear_acceleration", list(0.0, -10.0, 0.0),
                        "motion.motion.linear_drag_coefficient", 0.2,
                        "motion.collision.collision_radius", 0.2,
                        "motion.collision.collision_drag", 0.2,
                        "motion.collision.condition", "",
                        "motion.collision.coefficient_of_restitution", 0.6,
                        "motion.collision.expire_on_contact", false),
                "paper", preset(
                        "motion.collision.toggle", true,
                        "motion.motion.mode", "dynamic",
                        "motion.motion.linear_acceleration", list("math.sin(v.particle_age * 90)", -10.0, "math.cos(v.particle_age * 40)"),
                        "motion.motion.linear_drag_coefficient", 5.0,
                        "motion.collision.collision_radius", 0.2,
                        "motion.collision.collision_drag", 10.0,
                        "motion.collision.condition", "",
                        "motion.collision.coefficient_of_restitution", 0.0,
                        "motion.collision.expire_on_contact", false)));
    }

    // ==================================================================
    // QuickSetup.vue:352-386 DEFAULTS / SetupData（UI 专属字段不移植）
    // ==================================================================

    private static final String DEFAULT_SHAPE = "";
    private static final String DEFAULT_TIMING = "";
    private static final double DEFAULT_SPEED = 0;
    private static final double DEFAULT_AMOUNT = 4;
    private static final double DEFAULT_PARTICLE_LIFETIME = 2;
    private static final String DEFAULT_SPRITE = "";
    private static final boolean DEFAULT_LIGHTING = true;
    private static final boolean DEFAULT_RANDOM_ROTATION = false;
    private static final String DEFAULT_COLLISION = "none";

    private static String shape = DEFAULT_SHAPE;
    private static String timing = DEFAULT_TIMING;
    private static double speed = DEFAULT_SPEED;
    private static double amount = DEFAULT_AMOUNT;
    private static double particle_lifetime = DEFAULT_PARTICLE_LIFETIME;
    private static String sprite = DEFAULT_SPRITE;
    private static boolean lighting = DEFAULT_LIGHTING;
    private static boolean random_rotation = DEFAULT_RANDOM_ROTATION;
    private static String collision = DEFAULT_COLLISION;

    public static String getShape() { return shape; }

    public static String getTiming() { return timing; }

    public static double getSpeed() { return speed; }

    public static double getAmount() { return amount; }

    public static double getParticleLifetime() { return particle_lifetime; }

    public static String getSprite() { return sprite; }

    public static boolean isLighting() { return lighting; }

    public static boolean isRandomRotation() { return random_rotation; }

    public static String getCollision() { return collision; }

    // ==================================================================
    // QuickSetup.vue:410-427 watch（v-model 赋值入口，仅在值变化时触发回写）
    // ==================================================================

    /** v-model speed + watch：{@code setInput('motion.motion.linear_speed', value)}。 */
    public static void setSpeed(double value) {
        if (speed != value) {
            speed = value;
            setInput("motion.motion.linear_speed", value);
        }
    }

    /** v-model amount + watch：steady（timing != 'burst'）写 rate，否则写 amount。 */
    public static void setAmount(double value) {
        if (amount != value) {
            amount = value;
            boolean steady = !"burst".equals(timing);
            setInput("emitter.rate." + (steady ? "rate" : "amount"), value);
        }
    }

    /** v-model particle_lifetime + watch。 */
    public static void setParticleLifetime(double value) {
        if (particle_lifetime != value) {
            particle_lifetime = value;
            setInput("lifetime.lifetime.max_lifetime", value);
        }
    }

    /** v-model lighting + watch：{@code setInput('appearance.appearance.light', !value)}。 */
    public static void setLighting(boolean value) {
        if (lighting != value) {
            lighting = value;
            setInput("appearance.appearance.light", !value);
        }
    }

    /** v-model random_rotation + watch。 */
    public static void setRandomRotation(boolean value) {
        if (random_rotation != value) {
            random_rotation = value;
            setInput("motion.rotation.initial_rotation", value ? "math.random(-180, 180)" : "");
        }
    }

    // ==================================================================
    // QuickSetup.vue:429-445 setInput
    // ==================================================================

    /** JS setInput(path, value)：{@code registerEdit('set quick setup option')}。 */
    private static void setInput(String path, @Nullable Object value) {
        Input input = inputAt(path);
        if (input != null) {
            input.set(molangCoerce(input, value));
            EditHistory.registerEdit("set quick setup option");
        }
    }

    /**
     * JS molang 输入的值 coercion：数组元素逐个 toString（原地改写，as-is 怪癖），
     * 数字 toString；其他类型原样。
     */
    @SuppressWarnings("unchecked")
    private static @Nullable Object molangCoerce(Input input, @Nullable Object value) {
        if (input.type == InputType.MOLANG) {
            if (value instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    ((List<Object>) list).set(i, JsSemantics.toJsString(list.get(i)));
                }
            } else if (value instanceof Number) {
                return JsSemantics.toJsString(value);
            }
        }
        return value;
    }

    /** JS {@code this.data[path[0]][path[1]].inputs[path[2]]}（path = 'subject.group.input'）。 */
    private static @Nullable Input inputAt(String path) {
        String[] parts = path.split("\\.");
        InputStructure.Subject subject = InputStructure.Data.get(parts[0]);
        InputStructure.Group group = subject != null ? subject.group(parts[1]) : null;
        return group != null ? group.inputs.get(parts[2]) : null;
    }

    // ==================================================================
    // QuickSetup.vue:446-491 set(key, value)
    // ==================================================================

    /** JS {@code set(key, value)}：应用预设 + 滑杆状态同步 + sprite 纹理 + registerEdit('select quick setup preset')。 */
    public static void set(String key, String value) {
        // this[key] = value（shape/timing/sprite/collision 无 watcher）
        switch (key) {
            case "shape" -> shape = value;
            case "timing" -> timing = value;
            case "sprite" -> sprite = value;
            case "collision" -> collision = value;
            default -> { }
        }
        Map<String, Map<String, Object>> keyPresets = PRESETS.get(key);
        Map<String, Object> preset = keyPresets != null ? keyPresets.get(value) : null;
        if (preset != null) {
            for (Map.Entry<String, Object> e : preset.entrySet()) {
                String presetPath = e.getKey();
                Object presetValue = e.getValue();

                // 滑杆状态同步（赋触发 watcher 回写；JS watch 异步，Java 同步，见类 doc）
                if ("motion.motion.linear_speed".equals(presetPath) && !Double.isNaN(JsSemantics.toNumber(presetValue))) {
                    setSpeed(JsSemantics.toNumber(presetValue));
                }
                if ("emitter.rate.rate".equals(presetPath)) {
                    setAmount(JsSemantics.toNumber(presetValue));
                }
                if ("emitter.rate.amount".equals(presetPath)) {
                    setAmount(JsSemantics.toNumber(presetValue));
                }
                if ("lifetime.lifetime.max_lifetime".equals(presetPath)) {
                    setParticleLifetime(JsSemantics.toNumber(presetValue));
                }
                if ("appearance.appearance.light".equals(presetPath)) {
                    setLighting(JsSemantics.truthy(presetValue));
                }

                Input input = inputAt(presetPath);
                if (input != null) {
                    input.set(molangCoerce(input, presetValue));
                }
            }
        }

        if ("sprite".equals(key)) {
            TextureClass.Texture.source = spriteSource(value);
            TextureClass.Texture.internal_changes = true;
            TextureClass.Texture.update();
            TextureClass.Texture.updateCanvasFromSource();
        }
        EditHistory.registerEdit("select quick setup preset");
    }

    // ==================================================================
    // QuickSetup.vue:474-489 sprite → Texture.source（webpack asset/inline → classpath dataURL）
    // ==================================================================

    private static final Map<String, String> SPRITE_DATA_URLS = new LinkedHashMap<>();

    /** JS switch (value) → sprite dataURL；未命中（含 ''）返回 ''。 */
    private static String spriteSource(String id) {
        return switch (id) {
            case "ball", "dirt", "leaves", "smoke", "dust", "sparkle", "magic" ->
                    SPRITE_DATA_URLS.computeIfAbsent(id, QuickSetupPresets::loadSpriteDataUrl);
            default -> "";
        };
    }

    /** webpack.mix.js generator 完全一致：{@code 'data:image/png;base64,' + base64(文件字节)}。 */
    private static String loadSpriteDataUrl(String id) {
        String path = "/snowstorm/sprites/" + id + ".png";
        try (InputStream in = QuickSetupPresets.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("缺少内置资源 " + path);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ==================================================================
    // QuickSetup.vue:492-502 resetAll / 504-510 mounted
    // ==================================================================

    /**
     * JS resetAll：恢复 DEFAULTS。JS 赋值触发 watched 字段的 watch（仅值变化时），
     * 会把 DEFAULTS 回写进 Input/Config（QuickSetup.resetAll 于 loadFile/startNewProject
     * 中先于 updateConfig 调用，随后被文件值覆盖）——as-is 保留该回写。
     */
    public static void resetAll() {
        shape = DEFAULT_SHAPE;
        timing = DEFAULT_TIMING;
        setSpeed(DEFAULT_SPEED);
        setAmount(DEFAULT_AMOUNT);
        setParticleLifetime(DEFAULT_PARTICLE_LIFETIME);
        sprite = DEFAULT_SPRITE;
        setLighting(DEFAULT_LIGHTING);
        setRandomRotation(DEFAULT_RANDOM_ROTATION);
        collision = DEFAULT_COLLISION;
    }

    /**
     * JS mounted()：{@code QuickSetup.resetAll = () => this.resetAll()} +
     * 从 Input 同步 random_rotation/lighting。由 MC 侧 QuickSetup 标签页打开时调用
     * （JS 语义：标签页从未打开时 QuickSetup.resetAll 保持 stub）。
     */
    public static void mount() {
        EditorRuntime.QuickSetup.resetAll = QuickSetupPresets::resetAll;
        Input initialRotation = inputAt("motion.rotation.initial_rotation");
        setRandomRotation(initialRotation != null
                && "math.random(-180, 180)".equals(initialRotation.getValue()));
        Input light = inputAt("appearance.appearance.light");
        setLighting(light == null || !JsSemantics.truthy(light.getValue()));
    }
}

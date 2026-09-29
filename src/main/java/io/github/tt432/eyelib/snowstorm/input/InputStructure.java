package io.github.tt432.eyelib.snowstorm.input;

import io.github.tt432.eyelib.snowstorm.curve.Curve;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.gradient.Gradient;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * input_structure.js 逐字移植：Data（9 个 subject tab 的 group/inputs 骨架树）+ forEachInput。
 *
 * <p>JS 的对象树在 Java 中为 {@link #Data}（LinkedHashMap，键序与 JS 一致）：
 * {@code Data.effect.space.inputs.local_position} →
 * {@code Data.get("effect").group("space").inputs.get("local_position")}。
 * 类初始化即完成全部 81 个 Input 的构建与 {@code update(Data)}（JS 模块加载语义）。
 */
public final class InputStructure {

    /** JS undefined 哨兵：{@link Group#_selected_mode} 初值（与 null 严格不等，as-is）。 */
    public static final Object UNDEFINED = new Object();

    /** vscode_extension.js：游戏内无 VSCode 通道（ADR-0036 D6），恒 false。 */
    private static final boolean VSCODE = false;

    /** JS group 对象：{@code {label, _folded, inputs}} 或特殊组 {@code {label, _folded, type, events|curves}}。 */
    public static final class Group {
        public String label = "";
        public boolean _folded;
        /** 由持有 mode_groups 的 Input 在 {@link Input#update} 中回写；初值为 JS undefined 哨兵。 */
        public Object _selected_mode = UNDEFINED;
        public final Map<String, Input> inputs = new LinkedHashMap<>();
        /** 特殊组类型：{@code "events"} / {@code "curves"}；普通输入组为 null。 */
        public @Nullable String type;
        /** events 组的事件条目列表（JS events: []，元素为事件模型对象，event 包负责）。 */
        public final List<Object> events = new ArrayList<>();
        /** curves 组的曲线列表（JS curves: []，元素为 curve.Curve）。 */
        public final List<Curve> curves = new ArrayList<>();
    }

    /** JS subject 对象：{@code {label, ...groups}}（label 不参与 group 迭代，as-is）。 */
    public static final class Subject {
        public String label = "";
        public final Map<String, Group> groups = new LinkedHashMap<>();

        public @Nullable Group group(String key) {
            return groups.get(key);
        }
    }

    /** JS Data（input_structure.js 默认导出，亦挂 window.Data）。 */
    public static final Map<String, Subject> Data = new LinkedHashMap<>();

    static {
        build();
        // Setup Data（JS: forEachInput(input => input.update(Data))）
        forEachInput((input, key) -> input.update(Data));
    }

    private InputStructure() {
    }

    /** JS forEachInput：遍历所有 group 的 inputs（typeof group === 'object' 过滤已由结构保证）。 */
    public static void forEachInput(BiConsumer<Input, String> cb) {
        for (Subject subject : Data.values()) {
            for (Group group : subject.groups.values()) {
                for (Map.Entry<String, Input> e : group.inputs.entrySet()) {
                    cb.accept(e.getValue(), e.getKey());
                }
            }
        }
    }

    // ==================================================================
    // 构建助手
    // ==================================================================

    private static Input.Data d() {
        return new Input.Data();
    }

    private static Subject subject(String key, String label) {
        Subject s = new Subject();
        s.label = label;
        Data.put(key, s);
        return s;
    }

    private static Group group(Subject subject, String key, String label) {
        Group g = new Group();
        g.label = label;
        subject.groups.put(key, g);
        return g;
    }

    /** 跨输入引用（onchange/condition 里的 {@code Data.x.y.inputs.z}）。 */
    private static Input in(String subject, String group, String key) {
        return Objects.requireNonNull(Data.get(subject).group(group)).inputs.get(key);
    }

    private static List<Object> list(Object... values) {
        return new ArrayList<>(List.of(values));
    }

    // ==================================================================
    // Data 骨架（input_structure.js 的 const Data，键序 as-is）
    // ==================================================================

    private static void build() {
        Subject setup = subject("setup", "Quick Setup");

        // ============================== effect ==============================
        Subject effect = subject("effect", "File");

        Group effectMeta = group(effect, "meta", "File");
        effectMeta.inputs.put("identifier", new Input(d()
                .id("identifier")
                .label("Identifier")
                .info("This is the name the particle emitter is referred to as. Should have a namespace.")
                .placeholder("space:name")
                .required(true)
                .type(InputType.TEXT)
                .onchange(e -> {
                    Object v = in("effect", "meta", "identifier").getValue();
                    // JS: document.title = (this.value ? this.value + ' - ' : '') + 'Snowstorm'
                    if (Input.documentTitle != null) {
                        Input.documentTitle.accept((JsSemantics.truthy(v) ? JsSemantics.toJsString(v) + " - " : "") + "Snowstorm");
                    }
                })));

        Group effectSpace = group(effect, "space", "Space");
        effectSpace.inputs.put("local_position", new Input(d()
                .id("space_local_position")
                .label("Local Position")
                .info("When enabled and the effect is attached to an entity, the particles will simulate in entity space")
                .type(InputType.CHECKBOX)
                .onchange(e -> {
                    if (!JsSemantics.truthy(in("effect", "space", "local_position").getValue())) {
                        in("effect", "space", "local_rotation").set(false);
                    }
                })));
        effectSpace.inputs.put("local_rotation", new Input(d()
                .id("space_local_rotation")
                .label("Local Rotation")
                .info("When enabled and the effect is attached to an entity, the particle rotation will simulate in entity space. Only works if local position is enabled too.")
                .type(InputType.CHECKBOX)
                .onchange(e -> {
                    if (JsSemantics.truthy(in("effect", "space", "local_rotation").getValue())) {
                        in("effect", "space", "local_position").set(true);
                    }
                })));
        effectSpace.inputs.put("local_velocity", new Input(d()
                .id("space_local_velocity")
                .label("Local Velocity")
                .info("When enabled, the emitter's velocity will be added to the initial particle velocity.")
                .type(InputType.CHECKBOX)));

        // ============================== emitter ==============================
        Subject emitter = subject("emitter", "Emitter");

        Group emitterRate = group(emitter, "rate", "Spawn Amount");
        emitterRate.inputs.put("mode", new Input(d()
                .id("emitter_rate_mode")
                .type(InputType.SELECT)
                .label("Mode")
                .info("")
                .mode_groups("emitter", "rate")
                .options("steady", "Steady", "instant", "Instant", "manual", "Manual")));
        emitterRate.inputs.put("rate", new Input(d()
                .id("emitter_rate_rate")
                .label("Rate")
                .info("How often a particle is emitted, in particles/second.")
                .enabled_modes("steady")
                .required(true)
                .value(4.0)));
        emitterRate.inputs.put("amount", new Input(d()
                .id("emitter_rate_amount")
                .label("Amount")
                .info("How many particles are spawned at once")
                .enabled_modes("instant")
                .required(true)));
        emitterRate.inputs.put("maximum", new Input(d()
                .id("emitter_rate_maximum")
                .label("Maximum")
                .info("Maximum amount of particles that can be active before the emitter stops spawning new ones.")
                .enabled_modes("steady", "manual")
                .required(true)
                .value(100.0)));

        Group emitterLifetime = group(emitter, "lifetime", "Emitter Lifetime");
        emitterLifetime.inputs.put("mode", new Input(d()
                .id("emitter_lifetime_mode")
                .type(InputType.SELECT)
                .label("Mode")
                .info("")
                .mode_groups("emitter", "lifetime")
                .options("looping", "Looping", "once", "Once", "expression", "Expression")));
        emitterLifetime.inputs.put("active_time", new Input(d()
                .id("emitter_lifetime_active_time")
                .label("Active Time")
                .info("How long the emitter will be active for")
                .enabled_modes("looping", "once")
                .required(true)
                .value(1.0)));
        emitterLifetime.inputs.put("sleep_time", new Input(d()
                .id("emitter_lifetime_sleep_time")
                .label("Sleep Time")
                .info("How long the emitter will pause and not emit particles, when used in looping mode.")
                .enabled_modes("looping")));
        emitterLifetime.inputs.put("activation", new Input(d()
                .id("emitter_lifetime_activation")
                .label("Activation")
                .info("When the expression is non-zero, the emitter will start emitting particles")
                .required(true)
                .enabled_modes("expression")));
        emitterLifetime.inputs.put("expiration", new Input(d()
                .id("emitter_lifetime_expiration")
                .label("Expiration")
                .info("Emitter will expire if the expression is non-zero")
                .enabled_modes("expression")));

        Group emitterShape = group(emitter, "shape", "Spawn Shape");
        emitterShape.inputs.put("mode", new Input(d()
                .id("emitter_shape_mode")
                .type(InputType.SELECT)
                .label("Mode")
                .mode_groups("emitter", "shape")
                // JS 注释掉的 custom: 'Custom' 选项不移植
                .options("point", "Point", "sphere", "Sphere", "box", "Box", "disc", "Disc",
                        "entity_aabb", "Entity Bounding Box")));
        emitterShape.inputs.put("offset", new Input(d()
                .id("emitter_shape_offset")
                .label("Offset")
                .info("Specifies the offset from the emitter to emit each particle")
                .axis_count(3)
                .enabled_modes("point", "sphere", "box", "custom", "disc")));
        emitterShape.inputs.put("radius", new Input(d()
                .id("emitter_shape_radius")
                .label("Radius")
                .required(true)
                .info("Sphere or disc radius")
                .enabled_modes("sphere", "disc")));
        emitterShape.inputs.put("half_dimensions", new Input(d()
                .id("emitter_shape_half_dimensions")
                .label("Box Size")
                .info("Half dimensions of the box formed around the emitter")
                .axis_count(3)
                .enabled_modes("box")));
        emitterShape.inputs.put("plane_normal", new Input(d()
                .id("emitter_shape_plane_normal")
                .label("Plane Normal")
                .info("Specifies the normal of the disc plane, the disc will be perpendicular to this direction")
                .axis_count(3)
                .enabled_modes("disc")));
        emitterShape.inputs.put("surface_only", new Input(d()
                .id("emitter_shape_surface_only")
                .label("Surface Only")
                .info("Emit only from the surface of the shape")
                .type(InputType.CHECKBOX)
                .enabled_modes("sphere", "box", "entity_aabb", "disc")));

        // ============================== motion ==============================
        Subject motion = subject("motion", "Motion");

        Group motionMotion = group(motion, "motion", "Motion");
        motionMotion.inputs.put("mode", new Input(d()
                .id("particle_motion_mode")
                .type(InputType.SELECT)
                .label("Mode")
                .mode_groups("motion", "motion")
                .options("dynamic", "Dynamic", "parametric", "Parametric", "static", "Static")));
        motionMotion.inputs.put("direction_mode", new Input(d()
                .id("particle_direction_mode")
                .type(InputType.SELECT)
                .label("Direction")
                .info("The direction of emitted particles in regards to the emitter shape")
                .enabled_modes("dynamic")
                .options("outwards", "Outwards", "inwards", "Inwards", "direction", "Custom")));
        motionMotion.inputs.put("direction", new Input(d()
                .id("particle_direction_direction")
                .label("Direction")
                .info("The direction of emitted particles")
                .axis_count(3)
                .enabled_modes("dynamic")
                .condition(group -> "dynamic".equals(group.inputs.get("mode").getValue())
                        && "direction".equals(group.inputs.get("direction_mode").getValue()))));
        motionMotion.inputs.put("linear_speed", new Input(d()
                .id("particle_motion_linear_speed")
                .label("Speed")
                .info("Starts the particle with a specified speed, using the direction specified by the emitter shape")
                .enabled_modes("dynamic")
                .required(true)));
        motionMotion.inputs.put("linear_acceleration", new Input(d()
                .id("particle_motion_linear_acceleration")
                .label("Acceleration")
                .info("The linear acceleration applied to the particle in blocks/sec/sec")
                .axis_count(3)
                .enabled_modes("dynamic")));
        motionMotion.inputs.put("linear_drag_coefficient", new Input(d()
                .id("particle_motion_linear_drag_coefficient")
                .label("Linear Drag")
                .info("Think of this as air-drag.  The higher the value, the more drag.")
                .enabled_modes("dynamic")));
        motionMotion.inputs.put("relative_position", new Input(d()
                .id("particle_motion_relative_position")
                .label("Offset")
                .info("Directly set the position relative to the emitter")
                .axis_count(3)
                .enabled_modes("parametric")));
        motionMotion.inputs.put("relative_direction", new Input(d()
                .id("particle_motion_direction")
                .label("Direction")
                .info("Directly set the 3d direction of the particle")
                .axis_count(3)
                .enabled_modes("parametric")));

        Group motionRotation = group(motion, "rotation", "Rotation");
        motionRotation.inputs.put("mode", new Input(d()
                .id("particle_rotation_mode")
                .type(InputType.SELECT)
                .label("Mode")
                .mode_groups("motion", "rotation")
                .options("dynamic", "Dynamic", "parametric", "Parametric")));
        motionRotation.inputs.put("initial_rotation", new Input(d()
                .id("particle_rotation_initial_rotation")
                .label("Start Rotation")
                .info("Specifies the initial rotation in degrees")
                .enabled_modes("dynamic")));
        motionRotation.inputs.put("rotation_rate", new Input(d()
                .id("particle_rotation_rotation_rate")
                .label("Speed")
                .info("Specifies the spin rate in degrees/second")
                .enabled_modes("dynamic")));
        motionRotation.inputs.put("rotation_acceleration", new Input(d()
                .id("particle_rotation_rotation_acceleration")
                .label("Acceleration")
                .info("Acceleration applied to the rotation speed of the particle in degrees/sec/sec.")
                .enabled_modes("dynamic")));
        motionRotation.inputs.put("rotation_drag_coefficient", new Input(d()
                .id("particle_rotation_rotation_drag_coefficient")
                .label("Linear Drag")
                .info("Rotation resistance. Higher numbers will retard the rotation over time.")
                .enabled_modes("dynamic")));
        motionRotation.inputs.put("rotation", new Input(d()
                .id("particle_rotation_rotation")
                .label("Rotation")
                .info("Directly set the rotation of the particle")
                .enabled_modes("parametric")));

        Group motionCollision = group(motion, "collision", "Collision");
        motionCollision.inputs.put("toggle", new Input(d()
                .id("particle_collision_toggle")
                .label("Collide")
                .info("Enable collision of the particle with the world")
                .type(InputType.CHECKBOX)
                .mode_groups("motion", "collision")));
        motionCollision.inputs.put("collision_radius", new Input(d()
                .id("particle_collision_collision_radius")
                .label("Radius")
                .info("The radius of the particle, as used for collision.")
                .min(0.0)
                .max(0.5)
                .step(0.05)
                .required(true)
                .type(InputType.NUMBER)
                .enabled_modes(true)));
        motionCollision.inputs.put("collision_drag", new Input(d()
                .id("particle_collision_collision_drag")
                .label("Collision Drag")
                .info("Alters the speed of the particle when it has collided")
                .type(InputType.NUMBER)
                .step(0.1)
                .enabled_modes(true)));
        motionCollision.inputs.put("coefficient_of_restitution", new Input(d()
                .id("particle_collision_coefficient_of_restitution")
                .label("Bounciness")
                .info("The amount of momentum that is maintained on bounce. Set to 0.0 to not bounce, 1.0 to bounce back up to original height")
                .type(InputType.NUMBER)
                .step(0.1)
                .enabled_modes(true)));
        motionCollision.inputs.put("condition", new Input(d()
                .id("particle_collision_enabled")
                .label("Condition")
                .info("Enables collision when true / non-zero or unset")
                .enabled_modes(true)));
        motionCollision.inputs.put("events", new Input(d()
                .id("particle_collision_events")
                .label("Events")
                .info("Events to fire when the particle collides")
                .type(InputType.EVENT_SPEED_LIST)
                .enabled_modes(true)));
        motionCollision.inputs.put("expire_on_contact", new Input(d()
                .id("particle_collision_expire_on_contact")
                .label("Expire On Contact")
                .info("Removes the particle when it hits a block")
                .type(InputType.CHECKBOX)
                .enabled_modes(true)));

        // ============================== appearance ==============================
        Subject appearance = subject("appearance", "Appearance");

        Group appearanceAppearance = group(appearance, "appearance", "Appearance");
        appearanceAppearance.inputs.put("size", new Input(d()
                .id("particle_appearance_size")
                .label("Size")
                .info("Specifies the x/y size of the particle billboard.")
                .axis_count(2)
                .value(list(0.2, 0.2))));
        appearanceAppearance.inputs.put("material", new Input(d()
                .id("particle_appearance_material")
                .type(InputType.SELECT_CUSTOM)
                .info("Material to use for the particles")
                .label("Material")
                .options("particles_alpha", "Alpha Test", "particles_blend", "Blend",
                        "particles_add", "Additive", "particles_opaque", "Opaque", "custom", "Custom:")));
        appearanceAppearance.inputs.put("facing_camera_mode", new Input(d()
                .id("particle_appearance_facing_camera_mode")
                .type(InputType.SELECT)
                .info("Modes to orient the particle billboards facing the camera")
                .label("Facing")
                .options("rotate_xyz", "Rotate XYZ", "rotate_y", "Rotate Y",
                        "lookat_xyz", "Look at XYZ", "lookat_y", "Look at Y",
                        "lookat_direction", "Look at Direction", "direction_x", "Direction X",
                        "direction_y", "Direction Y", "direction_z", "Direction Z",
                        "emitter_transform_xy", "Emitter XY-Plane", "emitter_transform_xz", "Emitter XZ-Plane",
                        "emitter_transform_yz", "Emitter YZ-Plane")));
        appearanceAppearance.inputs.put("direction_mode", new Input(d()
                .id("particle_appearance_direction_mode")
                .type(InputType.SELECT)
                .info("Specifies how to calculate the billboard direction of a particle")
                .label("Direction")
                .options("derive_from_velocity", "From Motion", "custom", "Custom")
                .condition(group -> {
                    String facing = (String) group.inputs.get("facing_camera_mode").getValue();
                    return JsSemantics.jsSubstr(facing, 0, 9).equals("direction")
                            || "lookat_direction".equals(facing);
                })));
        appearanceAppearance.inputs.put("speed_threshold", new Input(d()
                .id("particle_appearance_speed_threshold")
                .label("Min Speed")
                .info("The direction is set if the speed of the particle is above the threshold. The default is 0.01")
                .type(InputType.NUMBER)
                .step(0.01)
                // JS 优先级怪癖 as-is：substr=='direction' || (lookat_direction && derive_from_velocity)
                .condition(group -> {
                    String facing = (String) group.inputs.get("facing_camera_mode").getValue();
                    return JsSemantics.jsSubstr(facing, 0, 9).equals("direction")
                            || ("lookat_direction".equals(facing)
                            && "derive_from_velocity".equals(group.inputs.get("direction_mode").getValue()));
                })));
        appearanceAppearance.inputs.put("direction", new Input(d()
                .id("particle_appearance_direction")
                .label("Direction")
                .info("The facing direction of emitted particles")
                .axis_count(3)
                // JS 优先级怪癖 as-is：substr=='direction' || (lookat_direction && custom)
                .condition(group -> {
                    String facing = (String) group.inputs.get("facing_camera_mode").getValue();
                    return JsSemantics.jsSubstr(facing, 0, 9).equals("direction")
                            || ("lookat_direction".equals(facing)
                            && "custom".equals(group.inputs.get("direction_mode").getValue()));
                })));
        appearanceAppearance.inputs.put("light", new Input(d()
                .id("particle_color_light")
                .label("Environment Lighting")
                .type(InputType.CHECKBOX)
                .info("If enabled, the particle gets darker at night and in darker areas. Otherwise the particle always displays at full brightness.")));

        Group appearanceColor = group(appearance, "color", "Color");
        appearanceColor.inputs.put("mode", new Input(d()
                .id("particle_color_mode")
                .type(InputType.SELECT)
                .label("Color Mode")
                .mode_groups("appearance", "color")
                .options("static", "Static", "gradient", "Gradient", "expression", "Expression")
                .onchange(e -> {
                    if ("gradient".equals(in("appearance", "color", "mode").getValue())) {
                        // JS: range.value == 0（loose equality）
                        if (JsSemantics.toNumber(in("appearance", "color", "range").getValue()) == 0) {
                            in("appearance", "color", "range").setValue(1.0);
                        }
                        if (!JsSemantics.truthy(in("appearance", "color", "interpolant").getValue())) {
                            in("appearance", "color", "interpolant").setValue("v.particle_age / v.particle_lifetime");
                        }
                    }
                })));
        appearanceColor.inputs.put("picker", new Input(d()
                .id("particle_color_static")
                .label("Color")
                .type(InputType.COLOR)
                .enabled_modes("static")
                .info("Set a static color for all emitted particles. Transparency is supported if the material supports blending.")));
        appearanceColor.inputs.put("interpolant", new Input(d()
                .id("particle_color_interpolant")
                .label("Interpolant")
                .info("Interpolant for the gradient value. A color for each particle will be picked from the gradient using the result of this expression, between 0 and \"Range\".")
                .enabled_modes("gradient")));
        appearanceColor.inputs.put("range", new Input(d()
                .id("particle_color_range")
                .label("Range")
                .info("The range of the color gradient. By default, gradients range from 0 to 1.")
                .type(InputType.NUMBER)
                .value(1.0)
                .enabled_modes("gradient")));
        appearanceColor.inputs.put("gradient", new Gradient(d()
                .id("particle_color_gradient")
                .label("Gradient")
                .info("Create a gradient by setting, arranging, and recoloring individual color points.")
                .type(InputType.GRADIENT)
                .enabled_modes("gradient")));
        appearanceColor.inputs.put("expression", new Input(d()
                .id("particle_color_expression")
                .label("Color")
                .info("Set the color per particle using Molang expressions in RGBA channels between 0 and 1. Alpha channel is supported if the material supports blending.")
                .axis_count(4)
                .enabled_modes("expression")));

        // ============================== texture ==============================
        Subject texture = subject("texture", "Texture & UV");

        Group textureTexture = group(texture, "texture", "Texture");
        textureTexture.inputs.put("path", new Input(d()
                .id("particle_texture_path")
                .type(InputType.TEXT)
                .info("Path to the texture, starting from the texture pack. Example: textures/particle/snowflake")
                .placeholder("textures/particle/particles")
                .label("Texture")
                .updatePreview(e -> EditorRuntime.Config.updateTexture())));
        textureTexture.inputs.put("image", new Input(d()
                .id("particle_texture_image")
                .type(InputType.IMAGE)
                .allow_upload(!VSCODE)
                .updatePreview(e -> EditorRuntime.Config.updateTexture())));

        Group textureUv = group(texture, "uv", "UV");
        textureUv.inputs.put("mode", new Input(d()
                .id("particle_texture_mode")
                .type(InputType.SELECT)
                .label("UV Mode")
                .mode_groups("texture", "uv")
                .options("static", "Static", "full", "Full Size", "animated", "Animated")));
        textureUv.inputs.put("size", new Input(d()
                .id("particle_texture_size")
                .label("Texture Size")
                .info("Resolution of the texture, used for UV mapping")
                .type(InputType.NUMBER)
                .axis_count(2)
                .required(true)
                .value(list(16.0, 16.0))
                .enabled_modes("static", "animated")));
        textureUv.inputs.put("uv", new Input(d()
                .id("particle_texture_uv")
                .label("UV Start")
                .info("UV start coordinates")
                .axis_count(2)
                .required(true)
                .value(list(0.0, 0.0))
                .enabled_modes("static", "animated")));
        textureUv.inputs.put("uv_size", new Input(d()
                .id("particle_texture_uv_size")
                .label("UV Size")
                .info("UV size coordinates")
                .axis_count(2)
                .value(list(16.0, 16.0))
                .enabled_modes("static", "animated")));
        textureUv.inputs.put("uv_step", new Input(d()
                .id("particle_texture_uv_step")
                .label("UV Step")
                .info("UV Offset per frame")
                .axis_count(2)
                .enabled_modes("animated")));
        textureUv.inputs.put("frames_per_second", new Input(d()
                .id("particle_texture_frames_per_second")
                .label("FPS")
                .info("Animation frames per second")
                .type(InputType.NUMBER)
                .min(0.0)
                .enabled_modes("animated")));
        textureUv.inputs.put("max_frame", new Input(d()
                .id("particle_texture_max_frame")
                .label("Max Frame")
                .info("Maximum amount of animation frames to draw from the flipbook")
                .enabled_modes("animated")));
        textureUv.inputs.put("stretch_to_lifetime", new Input(d()
                .id("particle_texture_stretch_to_lifetime")
                .label("Stretch To Lifetime")
                .info("Enable to stretch the frames in the animation to the expected lifetime of the particle")
                .type(InputType.CHECKBOX)
                .enabled_modes("animated")));
        textureUv.inputs.put("loop", new Input(d()
                .id("particle_texture_loop")
                .label("Loop")
                .info("Loop the texture animation")
                .type(InputType.CHECKBOX)
                .enabled_modes("animated")));

        // ============================== lifetime ==============================
        Subject lifetime = subject("lifetime", "Time");

        Group lifetimeLifetime = group(lifetime, "lifetime", "Particle Lifetime");
        lifetimeLifetime.inputs.put("max_lifetime", new Input(d()
                .id("particle_lifetime_max_lifetime")
                .label("Max Age")
                .info("Maximum age of the particle in seconds")
                .value(1.0)));
        lifetimeLifetime.inputs.put("expiration_expression", new Input(d()
                .id("particle_lifetime_expiration_expression")
                .label("Kill Expression")
                .info("This expression makes the particle expire when true (non-zero)")));
        lifetimeLifetime.inputs.put("kill_plane", new Input(d()
                .id("particle_lifetime_kill_plane")
                .label("Kill Plane")
                .type(InputType.NUMBER)
                .step(0.1)
                .info("Particles that cross this plane expire. The plane is relative to the emitter, but oriented in world space. The four parameters are the usual 4 elements of a plane equation.")
                .axis_count(4)));
        lifetimeLifetime.inputs.put("expire_in", new Input(d()
                .id("particle_lifetime_expire_in")
                .label("Kill in Blocks")
                .info("List of blocks that let the particle expire on contact. Enter the block IDs including the namespace.")
                .placeholder("minecraft:stone")
                .axis_count(-1)
                .type(InputType.TEXT)));
        lifetimeLifetime.inputs.put("expire_outside", new Input(d()
                .id("particle_lifetime_expire_outside")
                .label("Only in Blocks")
                .info("List of blocks outside of which the particle expires. Enter the block IDs including the namespace.")
                .placeholder("minecraft:air")
                .axis_count(-1)
                .type(InputType.TEXT)));

        // ============================== events ==============================
        Subject events = subject("events", "Events");

        Group eventsEvents = group(events, "events", "Events");
        eventsEvents.type = "events"; // JS: {type: 'events', events: []}

        Group eventsEmitterEvents = group(events, "emitter_events", "Emitter Event Triggers");
        eventsEmitterEvents.inputs.put("creation", new Input(d()
                .id("emitter_events_creation")
                .label("On Creation")
                .info("Events to fire when the emitter is spawned")
                .type(InputType.EVENT_LIST)));
        eventsEmitterEvents.inputs.put("expiration", new Input(d()
                .id("emitter_events_expiration")
                .label("On Expiration")
                .info("Events to fire when the emitter stops to exist")
                .type(InputType.EVENT_LIST)));
        eventsEmitterEvents.inputs.put("travel_distance", new Input(d()
                .id("emitter_events_distance")
                .label("Travel Distance")
                .info("Fire an event when the emitter has moved a certain distance. Emitters can move when they are attached to an entity that moves. Distances are specified in blocks.")
                .type(InputType.EVENT_TIMELINE)));
        eventsEmitterEvents.inputs.put("looping_travel_distance", new Input(d()
                .id("emitter_events_distance_looping")
                .label("Travel Distance Looping")
                .info("Fire an event every time the emitter has moved a certain distance. Emitters can move when they are attached to an entity that moves. Distances are specified in blocks.")
                .type(InputType.EVENT_TIMELINE)));
        eventsEmitterEvents.inputs.put("timeline", new Input(d()
                .id("emitter_events_timeline")
                .label("Timeline")
                .info("Fire events at specific times during the lifetime of the emitter")
                .type(InputType.EVENT_TIMELINE)));

        Group eventsParticleEvents = group(events, "particle_events", "Particle Event Triggers");
        eventsParticleEvents.inputs.put("creation", new Input(d()
                .id("particle_events_creation")
                .label("On Creation")
                .info("Events to fire when the particle is spawned")
                .type(InputType.EVENT_LIST)));
        eventsParticleEvents.inputs.put("expiration", new Input(d()
                .id("particle_events_expiration")
                .label("On Expiration")
                .info("Events to fire when the particle stops to exist")
                .type(InputType.EVENT_LIST)));
        eventsParticleEvents.inputs.put("timeline", new Input(d()
                .id("particle_events_timeline")
                .label("Timeline")
                .info("Fire events at specific times during the lifetime of the particle")
                .type(InputType.EVENT_TIMELINE)));

        // ============================== variables ==============================
        Subject variables = subject("variables", "Variables & Curves");

        Group variablesVariables = group(variables, "variables", "Variables");
        variablesVariables.inputs.put("creation_vars", new Input(d()
                .id("variables_creation_vars")
                .label("Start Variables")
                .info("Set up Molang variables when the emitter starts")
                .placeholder("variable.name = value;")
                .type(InputType.MOLANG)
                .axis_count(-1)
                // JS 函数体整块注释掉（Emitter.creation_variables 解析），as-is 保留为空函数
                .onchange(e -> {
                })));
        variablesVariables.inputs.put("tick_vars", new Input(d()
                .id("variables_tick_vars")
                .label("Tick Variables")
                .info("Molang variables that get processed for every emitter update")
                .placeholder("variable.name = value;")
                .type(InputType.MOLANG)
                .axis_count(-1)
                // JS 函数体整块注释掉（Emitter.tick_variables 解析），as-is 保留为空函数
                .onchange(e -> {
                })));
        variablesVariables.inputs.put("particle_update", new Input(d()
                .id("particle_update_expression")
                .label("Particle Update")
                .info("Run an expression on the emitter for every particle when the emitter updates")
                .placeholder("variable.name = value;")
                .type(InputType.MOLANG)
                .axis_count(-1)));
        variablesVariables.inputs.put("particle_render", new Input(d()
                .id("particle_render_expression")
                .label("Particle Render")
                .info("Run an expression on the emitter each time a particle is rendered")
                .placeholder("variable.name = value;")
                .type(InputType.MOLANG)
                .axis_count(-1)));

        Group variablesCurves = group(variables, "curves", "Curves");
        variablesCurves.type = "curves"; // JS: {type: 'curves', curves: []}
    }
}

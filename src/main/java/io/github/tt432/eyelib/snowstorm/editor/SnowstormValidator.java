package io.github.tt432.eyelib.snowstorm.editor;

import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;

import java.util.ArrayList;
import java.util.List;

/**
 * WarningDialog.vue {@code validate()} 逐字移植：3 条警告规则（纯数据函数）。
 *
 * <ol>
 *   <li>不透明材质（particles_alpha/particles_opaque）使用 alpha（static 颜色第 4 字节非 FF /
 *       expression 第 4 通道非 ''/'1'/'1.0'）；</li>
 *   <li>direction 朝向（derive_from_velocity）但没有速度（dynamic 无 linear_speed /
 *       parametric 无 direction 分量）；</li>
 *   <li>steady 模式下发射间隔 ≥ 活跃时间（粒子可能不生成）。</li>
 * </ol>
 *
 * <p>偏离：JS 复用模块级 errors 数组（{@code errors.splice(0)} 后返回同引用）；Java 为纯函数，
 * 每次返回新列表（Preview.vue 仅读 length/遍历，语义等价）。
 */
public final class SnowstormValidator {

    /** JS 警告条目 {@code {text}}。 */
    public static final class Warning {
        public final String text;

        public Warning(String text) {
            this.text = text;
        }
    }

    private SnowstormValidator() {
    }

    /** WarningDialog.vue validate()：对 {@link EditorRuntime#Config} 运行 3 条规则，返回警告列表。 */
    public static List<Warning> validate() {
        Config Config = EditorRuntime.Config;
        List<Warning> errors = new ArrayList<>();

        if (("particles_alpha".equals(Config.particle_appearance_material)
                || "particles_opaque".equals(Config.particle_appearance_material)) && (
                ("static".equals(Config.particle_color_mode)
                        && Config.particle_color_static.length() == 9
                        && !"FF".equals(lastChars(Config.particle_color_static, 2).toUpperCase(java.util.Locale.ROOT)))
                        || ("expression".equals(Config.particle_color_mode)
                        && !isOpaqueAlpha(listGet(Config.particle_color_expression, 3)))
        )) {
            errors.add(new Warning("The effect attempts to use opacity but the selected material does not support opacity"));
        }

        if (Config.particle_appearance_facing_camera_mode.contains("direction")
                && "derive_from_velocity".equals(Config.particle_appearance_direction_mode)) {
            if ("dynamic".equals(Config.particle_motion_mode)
                    && !(JsSemantics.truthy(Config.particle_motion_linear_speed)
                    && JsonValues.jsParseFloat(Config.particle_motion_linear_speed) != 0)) {
                errors.add(new Warning("The particles are set to face a direction, but no speed is set. Only particles with an initial speed support directions"));
            } else if ("parametric".equals(Config.particle_motion_mode)
                    && Config.particle_motion_direction.stream()
                    .noneMatch(v -> JsSemantics.truthy(v) && JsonValues.jsParseFloat(v) != 0)) {
                errors.add(new Warning("The particles are set to face a direction, but no parametric direction is set"));
            }
        }

        if ("steady".equals(Config.emitter_rate_mode)
                && !"expression".equals(Config.emitter_lifetime_mode) // JS !== 严格不等
                && !Double.isNaN(JsSemantics.toNumber(Config.emitter_rate_rate)) // JS: !isNaN(...)
                && 1 / JsonValues.jsParseFloat(Config.emitter_rate_rate)
                >= JsonValues.jsParseFloat(Config.emitter_lifetime_active_time)) {
            errors.add(new Warning("The emitter rate is lower than the emitter lifetime, so particles may not spawn"));
        }

        return errors;
    }

    /** JS {@code str.substr(-2)}（负 start 从尾部计，无 length 取到末尾）。 */
    private static String lastChars(String s, int count) {
        return s.substring(Math.max(0, s.length() - count));
    }

    private static @org.jspecify.annotations.Nullable Object listGet(List<Object> list, int i) {
        return i < list.size() ? list.get(i) : null;
    }

    /** JS {@code ['', '1', '1.0'].includes(v)}（SameValueZero 严格相等，数字 1 不等于 '1'）。 */
    private static boolean isOpaqueAlpha(@org.jspecify.annotations.Nullable Object v) {
        return "".equals(v) || "1".equals(v) || "1.0".equals(v);
    }
}

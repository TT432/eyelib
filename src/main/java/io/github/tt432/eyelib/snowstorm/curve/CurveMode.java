package io.github.tt432.eyelib.snowstorm.curve;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;

/**
 * Snowstorm 曲线插值 mode（curves.js inputs.mode 的 options）。
 *
 * <p>数据层 as-is：{@code Config.Curve.mode} 仍是字符串，本枚举仅承载 id/label、
 * select options 与合法性校验（JS {@code ['catmull_rom','linear','bezier','bezier_chain'].includes(...)}）。
 * 各 mode 的采样语义见 wintersky {@code Emitter.calculateCurve}。
 */
public enum CurveMode {

    LINEAR("linear", "Linear"),
    CATMULL_ROM("catmull_rom", "Catmull Rom"),
    BEZIER("bezier", "Bézier"),
    BEZIER_CHAIN("bezier_chain", "Bézier Chain");

    /** JS mode 字符串（Config.Curve.mode / 导出 JSON 的 type）。 */
    public final String id;
    /** JS options 的显示名。 */
    public final String label;

    CurveMode(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static @Nullable CurveMode byId(@Nullable String id) {
        for (CurveMode mode : values()) {
            if (mode.id.equals(id)) return mode;
        }
        return null;
    }

    /** JS options 对象的 key/label 平铺对（供 {@code Input.Data.options(String...)}）。 */
    public static String[] optionPairs() {
        return new String[]{
                CATMULL_ROM.id, CATMULL_ROM.label,
                LINEAR.id, LINEAR.label,
                BEZIER.id, BEZIER.label,
                BEZIER_CHAIN.id, BEZIER_CHAIN.label,
        };
    }

    /** JS options 对象（键序 as-is：catmull_rom / linear / bezier / bezier_chain）。 */
    public static LinkedHashMap<String, String> options() {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put(CATMULL_ROM.id, CATMULL_ROM.label);
        options.put(LINEAR.id, LINEAR.label);
        options.put(BEZIER.id, BEZIER.label);
        options.put(BEZIER_CHAIN.id, BEZIER_CHAIN.label);
        return options;
    }
}

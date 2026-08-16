package io.github.tt432.eyelib.importer.addon;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bedrock 附加包 manifest format_version 3 的 {@code settings} 条目（类型化模型）。
 * <p>
 * 官方四种控件（Microsoft Learn《Create a Pack With Custom Settings》）：
 * {@code label}（纯文本）、{@code toggle}（布尔开关）、{@code slider}（min/max/step 数值）、
 * {@code dropdown}（固定选项）。运行时经 molang 读取：
 * {@code query.get_pack_setting}（slider）、{@code query.is_pack_setting_enabled}（toggle）、
 * {@code query.is_pack_setting_selected}（dropdown）。{@code text}/选项 {@code text}
 * 支持本地化键（texts/*.lang）。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BedrockPackSetting(
        Type type,
        /** 设置名（如 "mypack:explosion_hands"）；label 无名为 null。 */
        @Nullable String name,
        /** 显示文本或本地化键。 */
        String text,
        /** toggle 默认值。 */
        boolean defaultBoolean,
        /** slider 默认值/范围。 */
        double defaultNumber,
        double min,
        double max,
        double step,
        /** dropdown 默认选项 name。 */
        @Nullable String defaultOption,
        /** dropdown 选项表。 */
        List<Option> options
) {
    public enum Type {
        LABEL, TOGGLE, SLIDER, DROPDOWN;

        static @Nullable Type of(String raw) {
            return switch (raw.toLowerCase(java.util.Locale.ROOT)) {
                case "label" -> LABEL;
                case "toggle" -> TOGGLE;
                case "slider" -> SLIDER;
                case "dropdown" -> DROPDOWN;
                default -> null;
            };
        }
    }

    /** dropdown 选项：{@code name} 为值（molang 比较对象），{@code text} 为显示文本/本地化键。 */
    public record Option(String name, String text) {
    }

    /** 从 manifest.settings 的原始 ObjectValue 解析一条；缺 type/未知 type 返回 null。 */
    public static @Nullable BedrockPackSetting parse(BedrockResourceValue.ObjectValue obj) {
        Map<String, BedrockResourceValue> values = obj.values();
        String typeRaw = stringOf(values.get("type"));
        if (typeRaw == null) {
            return null;
        }
        Type type = Type.of(typeRaw);
        if (type == null) {
            return null;
        }
        String text = stringOf(values.get("text"));
        String name = stringOf(values.get("name"));
        boolean defaultBoolean = values.get("default") instanceof BedrockResourceValue.BooleanValue b && b.value();
        double defaultNumber = numberOf(values.get("default"), 0);
        double min = numberOf(values.get("min"), 0);
        double max = numberOf(values.get("max"), 1);
        double step = numberOf(values.get("step"), 1);
        String defaultOption = type == Type.DROPDOWN ? stringOf(values.get("default")) : null;
        List<Option> options = new ArrayList<>();
        if (values.get("options") instanceof BedrockResourceValue.ArrayValue array) {
            for (BedrockResourceValue element : array.values()) {
                if (element instanceof BedrockResourceValue.ObjectValue optionObj) {
                    String optionName = stringOf(optionObj.values().get("name"));
                    if (optionName != null) {
                        String optionText = stringOf(optionObj.values().get("text"));
                        options.add(new Option(optionName, optionText != null ? optionText : optionName));
                    }
                }
            }
        }
        return new BedrockPackSetting(type, name, text != null ? text : "",
                defaultBoolean, defaultNumber, min, max, step, defaultOption, List.copyOf(options));
    }

    /** 解析 manifest.settings 全表（坏条目跳过，保持声明顺序）。 */
    public static List<BedrockPackSetting> parseList(List<BedrockResourceValue.ObjectValue> raw) {
        List<BedrockPackSetting> result = new ArrayList<>(raw.size());
        for (BedrockResourceValue.ObjectValue obj : raw) {
            BedrockPackSetting setting = parse(obj);
            if (setting != null) {
                result.add(setting);
            }
        }
        return List.copyOf(result);
    }

    private static @Nullable String stringOf(@Nullable BedrockResourceValue value) {
        return value instanceof BedrockResourceValue.StringValue s ? s.value() : null;
    }

    private static double numberOf(@Nullable BedrockResourceValue value, double fallback) {
        return value instanceof BedrockResourceValue.NumberValue n ? n.value().doubleValue() : fallback;
    }

    /** slider 默认值约束到 [min,max]（防御非法 manifest）。 */
    public double clampedDefault() {
        return Math.max(min, Math.min(max, defaultNumber));
    }

    /** slider 值吸附到 step 网格。 */
    public double snapToStep(double value) {
        if (step <= 0) {
            return Math.max(min, Math.min(max, value));
        }
        double snapped = min + Math.round((value - min) / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }
}

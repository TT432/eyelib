package io.github.tt432.eyelib.snowstorm.input;

/**
 * input.js {@code data.type} 的 12 种取值。JS 侧为字符串，{@link #jsName} 与之一一对应。
 */
public enum InputType {
    MOLANG("molang"),
    TEXT("text"),
    NUMBER("number"),
    CHECKBOX("checkbox"),
    SELECT("select"),
    SELECT_CUSTOM("select_custom"),
    COLOR("color"),
    GRADIENT("gradient"),
    IMAGE("image"),
    EVENT_LIST("event_list"),
    EVENT_TIMELINE("event_timeline"),
    EVENT_SPEED_LIST("event_speed_list");

    /** JS 中的类型字符串（如 {@code 'select_custom'}）。 */
    public final String jsName;

    InputType(String jsName) {
        this.jsName = jsName;
    }

    /** JS 字符串 → 枚举；未知字符串返回 null（JS 中不会出现）。 */
    public static @org.jspecify.annotations.Nullable InputType fromJsName(String jsName) {
        for (InputType t : values()) {
            if (t.jsName.equals(jsName)) return t;
        }
        return null;
    }
}

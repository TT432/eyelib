package io.github.tt432.eyelib.snowstorm.gradient;

import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Snowstorm v3.2.2 渐变 stop：{@code {percent, color: '#rrggbbaa', id}}。
 *
 * <p>as-is 说明：JS 中 stop 是普通对象字面量，且作为 Input value 直接进入 Config、
 * 最终经 compileJSON 序列化。为保持字节级导出兼容，本类继承 {@link LinkedHashMap}
 * （键序 percent/color/id 与 JS 字面量一致）， typed 访问器只是 Map 视图。
 * 注意 JS {@code addPoint()} 创建的 stop 没有 id 键（as-is 保留）。
 */
public class GradientStop extends LinkedHashMap<String, Object> {

    /** JS 对象字面量 {@code {percent, color, id}}；id 为 null 时省略 id 键（addPoint as-is）。 */
    public GradientStop(double percent, String color, @Nullable String id) {
        put("percent", percent);
        put("color", color);
        if (id != null) put("id", id);
    }

    /** 空 stop（字段由调用方填充）。 */
    public GradientStop() {
    }

    /** Config JSON 回填的 Map → GradientStop（键序归一为 percent/color/id）。 */
    public static GradientStop fromJson(@Nullable Object json) {
        GradientStop stop = new GradientStop();
        if (json instanceof Map<?, ?> map) {
            Object percent = map.get("percent");
            Object color = map.get("color");
            Object id = map.get("id");
            if (percent != null) stop.put("percent", JsSemantics.toNumber(percent));
            if (color != null) stop.put("color", JsSemantics.toJsString(color));
            if (id != null) stop.put("id", JsSemantics.toJsString(id));
        }
        return stop;
    }

    public double percent() {
        return JsSemantics.toNumber(get("percent"));
    }

    public void percent(double percent) {
        put("percent", percent);
    }

    public String color() {
        return JsSemantics.toJsString(get("color"));
    }

    public void color(String color) {
        put("color", color);
    }

    public @Nullable String id() {
        Object id = get("id");
        return id != null ? JsSemantics.toJsString(id) : null;
    }
}

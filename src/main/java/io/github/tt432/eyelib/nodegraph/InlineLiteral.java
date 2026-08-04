package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.regex.Pattern;

/**
 * ANY 端口行内文本 ↔ JSON 字面值（规格 §2.8：编辑器内联编辑器的共享解析口径）。
 *
 * <p>智能解析：整数字面量 → int、小数字面量 → float、{@code true/false} → bool、
 * 其余 → 字符串（codegen 发射为 molang 字符串字面量）。行内字段是<b>字面值</b>编辑器；
 * 需要动态表达式时应当连线而不是在字段里写 molang。
 */
public final class InlineLiteral {
    private InlineLiteral() {
    }

    private static final Pattern INT = Pattern.compile("[-+]?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("[-+]?(?:\\d+\\.\\d*|\\.\\d+|\\d+)(?:[eE][-+]?\\d+)?");

    /** 行内文本 → JSON 字面值（int / float / bool / string）。空串 → 空字符串。 */
    public static JsonElement parse(String text) {
        String t = text == null ? "" : text.trim();
        if (INT.matcher(t).matches()) {
            try {
                return new JsonPrimitive(Long.parseLong(t));
            } catch (NumberFormatException ignored) {
                // 溢出按 double 处理
            }
        }
        if (DECIMAL.matcher(t).matches()) {
            try {
                return new JsonPrimitive(Double.parseDouble(t));
            } catch (NumberFormatException ignored) {
                // 落到字符串
            }
        }
        if ("true".equals(t)) {
            return new JsonPrimitive(true);
        }
        if ("false".equals(t)) {
            return new JsonPrimitive(false);
        }
        return new JsonPrimitive(t);
    }

    /** JSON 字面值 → 行内文本（数字去 .0；bool → true/false；字符串原样；非基元 → 空串）。 */
    public static String toText(JsonElement value) {
        if (value instanceof JsonPrimitive p) {
            if (p.isNumber()) {
                return MolangLiterals.formatNumber(p.getAsDouble());
            }
            if (p.isBoolean()) {
                return Boolean.toString(p.getAsBoolean());
            }
            if (p.isString()) {
                return p.getAsString();
            }
        }
        return "";
    }
}

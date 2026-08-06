package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

/**
 * Molang 字面量文本工具（codegen / 组装器 / 编辑器内联值共用，单一口径）。
 */
public final class MolangLiterals {
    private MolangLiterals() {
    }

    /** 数字格式化：整数去 .0（1.0 → "1"，1.5 → "1.5"）。 */
    public static String formatNumber(double v) {
        if (v == Math.rint(v) && Double.isFinite(v) && Math.abs(v) < 9.0e15) {
            return Long.toString((long) v);
        }
        return Double.toString(v);
    }

    /** 字符串字面量：单引号包裹，转义 ' 与 \。 */
    public static String quote(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    /** JSON 基元 → molang 字面量文本：数字去 .0；bool → 1/0；string → 单引号转义。非基元 → null。 */
    public static @Nullable String literal(JsonElement e) {
        if (e instanceof JsonPrimitive p) {
            if (p.isNumber()) {
                return formatNumber(p.getAsDouble());
            }
            if (p.isBoolean()) {
                return p.getAsBoolean() ? "1" : "0";
            }
            if (p.isString()) {
                return quote(p.getAsString());
            }
        }
        return null;
    }
}

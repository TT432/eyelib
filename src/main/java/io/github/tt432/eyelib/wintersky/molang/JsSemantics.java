package io.github.tt432.eyelib.wintersky.molang;

import org.jspecify.annotations.Nullable;

/**
 * JavaScript 动态类型语义复刻：MolangJS 逐字移植所需的 JS 行为。
 *
 * <p>MolangJS 内部值只有两种动态类型：number（Java {@link Double}）与
 * string（Java {@link String}，仅由引号字面量/上下文函数引入）。
 * 本类实现移植代码依赖的 JS 运算符语义：truthiness、ToNumber、
 * {@code ==}/{@code ===}、{@code x || 0}、{@code Math.round}、substring/substr。
 */
final class JsSemantics {

    private JsSemantics() {
    }

    /** JS truthiness：undefined/null→false，0/NaN→false，""→false，其余 true。 */
    static boolean truthy(@Nullable Object v) {
        if (v == null) return false;
        if (v instanceof Double d) return d != 0 && !Double.isNaN(d);
        if (v instanceof String s) return !s.isEmpty();
        return true;
    }

    /**
     * JS {@code ToNumber}：undefined→NaN，Double→自身，String→按 JS Number() 解析。
     * 与 JS 的差异：不支持 0x 十六进制与 0b/0o 字面量（molang 表达式不会产出）。
     */
    static double toNumber(@Nullable Object v) {
        if (v == null) return Double.NaN;
        if (v instanceof Double d) return d;
        if (v instanceof String s) {
            String t = s.trim();
            if (t.isEmpty()) return 0;
            if (t.equals("Infinity") || t.equals("+Infinity")) return Double.POSITIVE_INFINITY;
            if (t.equals("-Infinity")) return Double.NEGATIVE_INFINITY;
            // JS Number() 不接受 "12f" 这类后缀，Double.parseDouble 接受，故先按 JS 十进制语法过滤
            if (!t.matches("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?")) return Double.NaN;
            try {
                return Double.parseDouble(t);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    /** JS {@code ===}：类型不同即 false；NaN !== NaN；-0 === 0。 */
    static boolean strictEquals(@Nullable Object a, @Nullable Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Double da && b instanceof Double db) return da.doubleValue() == db.doubleValue();
        if (a instanceof String sa && b instanceof String sb) return sa.equals(sb);
        return false;
    }

    /** JS {@code ==}（仅 number/string 两域）：number==string 时对 string 做 ToNumber。 */
    static boolean looseEquals(@Nullable Object a, @Nullable Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Double da && b instanceof Double db) return da.doubleValue() == db.doubleValue();
        if (a instanceof String sa && b instanceof String sb) return sa.equals(sb);
        if (a instanceof Double d && b instanceof String s) return d.doubleValue() == toNumber(s);
        if (a instanceof String s && b instanceof Double d) return toNumber(s) == d.doubleValue();
        return false;
    }

    /** JS {@code x || 0}：falsy → 0.0，否则原值（可能为 String）。 */
    static Object orZero(@Nullable Object v) {
        return truthy(v) ? v : 0.0;
    }

    /**
     * JS {@code parse()} 返回值的数值化：{@code calculate(...) || 0}。
     * JS 中 calculate 返回 truthy 字符串时 parse 会返回字符串；wintersky 全部调用点
     * 均按数值消费，字符串在后续算术中变为 NaN。此处直接 ToNumber + NaN→0，行为等价。
     */
    static double resultToNumber(@Nullable Object v) {
        if (v instanceof Double d) return (d == 0 || Double.isNaN(d)) ? 0.0 : d;
        if (v instanceof String s) {
            double n = toNumber(s);
            return Double.isNaN(n) ? 0.0 : n;
        }
        return 0.0;
    }

    /** JS number→string（用于 {@code +} 字符串拼接路径）：5→"5"，5.5→"5.5"。 */
    static String toJsString(@Nullable Object v) {
        if (v == null) return "undefined";
        if (v instanceof String s) return s;
        if (v instanceof Double d) {
            if (Double.isNaN(d)) return "NaN";
            if (d == Double.POSITIVE_INFINITY) return "Infinity";
            if (d == Double.NEGATIVE_INFINITY) return "-Infinity";
            if (d == Math.rint(d) && Math.abs(d) < 1e21) {
                return Long.toString((long) d.doubleValue());
            }
            return Double.toString(d);
        }
        return String.valueOf(v);
    }

    /**
     * JS {@code Math.round}：四舍五入、平局向 +∞；(-0.5, 0) 区间返回 -0。
     * Java {@link Math#round} 在这些区间返回 +0 且返回 long，大数会溢出，故单独实现。
     */
    static double jsRound(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x) || x == 0) return x;
        double r = Math.floor(x + 0.5);
        if (r == 0 && x < 0) return -0.0;
        return r;
    }

    /** JS {@code String.prototype.substring}：负值截 0、start>end 交换。 */
    static String jsSubstring(String s, int start, int end) {
        int len = s.length();
        int a = Math.min(Math.max(start, 0), len);
        int b = Math.min(Math.max(end, 0), len);
        return s.substring(Math.min(a, b), Math.max(a, b));
    }

    /** JS {@code String.prototype.substr}：负 start 从尾部计，负 length 得空串。 */
    static String jsSubstr(String s, int start, int length) {
        int len = s.length();
        if (start < 0) start = Math.max(len + start, 0);
        if (start >= len || length <= 0) return "";
        return s.substring(start, Math.min(start + length, len));
    }
}

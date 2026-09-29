package io.github.tt432.eyelib.snowstorm.util;

import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import io.github.tt432.eyelib.wintersky.rng.WinterskyRandom;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Snowstorm v3.2.2 {@code util.js} 逐字移植（原作者 JannisX11，GPL-3.0）。
 *
 * <p>JS 侧挂到 {@code Math}/{@code Array.prototype} 上的扩展在此全部为静态方法。
 *
 * <p>偏离记录：
 * <ul>
 *   <li>{@code Math.random()} → {@link WinterskyRandom}（同 ADR-0034 先例）。</li>
 *   <li>{@code convertTouchEvent}/{@code calculateOffset}/{@code isFirefox}/{@code IO} 为浏览器
 *       DOM/FileReader 依赖，不移植；导入导出按 ADR-0036 D6 走 MC 侧 FileDialogService +
 *       {@code run/snowstorm_exports/}。</li>
 *   <li>{@code compileJSON} 的对象键序：JS for..in 会把整数形态键（如 "0"、"1"）按数值升序提前，
 *       Java LinkedHashMap 保持插入序；其余键序一致。</li>
 *   <li>{@code compileJSON} 数字 toString：JS 对 |x|≥1e21 或 ≤1e-7 用指数记法（"1e+21"），
 *       Java {@link Double#toString} 格式（"1.0E21"）不同。</li>
 *   <li>{@code lineify} 来自 import.js 的局部函数（设计规格 §3 指定归入本类）；JS 中它同时设置
 *       {@code input.expanded = true}，该副作用由调用方（import 切片）负责。</li>
 * </ul>
 */
public final class SnowstormUtil {

    private SnowstormUtil() {
    }

    // ---------------------------------------------------------------- util.js 函数

    /** JS {@code bbuid(l)}：随机标识串；{@code l = l || 1}。 */
    public static String bbuid(int l) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder s = new StringBuilder();
        while (l > 0) {
            int n = (int) Math.floor(WinterskyRandom.nextDouble() * 62);
            if (n > 9) {
                s.append(chars.charAt(n - 10));
            } else {
                // JS：s += n（数字拼字符串）
                s.append(n);
            }
            l--;
        }
        return s.toString();
    }

    /** JS {@code bbuid()}：默认长度 1。 */
    public static String bbuid() {
        return bbuid(1);
    }

    /** JS {@code guid()}：8-4-4-4-12 十六进制随机串（非 RFC4122 变体位，as-is）。 */
    public static String guid() {
        return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
    }

    private static String s4() {
        // JS：Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1)
        return Integer.toHexString((int) Math.floor((1 + WinterskyRandom.nextDouble()) * 0x10000)).substring(1);
    }

    // ---------------------------------------------------------------- Math.* 扩展

    /** JS {@code Math.clamp}：NaN → min（as-is）。 */
    public static double clamp(double number, double min, double max) {
        if (number > max) number = max;
        if (number < min || Double.isNaN(number)) number = min;
        return number;
    }

    /** JS {@code Math.randomab}。 */
    public static double randomab(double a, double b) {
        return a + WinterskyRandom.nextDouble() * (b - a);
    }

    /** JS {@code Math.radToDeg}。 */
    public static double radToDeg(double rad) {
        return rad / Math.PI * 180;
    }

    /** JS {@code Math.degToRad}：逐字保留 JS 写法 {@code Math.PI / (180 / deg)}。 */
    public static double degToRad(double deg) {
        return Math.PI / (180 / deg);
    }

    /** JS {@code Math.roundTo}。 */
    public static double roundTo(double num, int digits) {
        double d = Math.pow(10, digits);
        return JsSemantics.jsRound(num * d) / d;
    }

    /** JS {@code Math.lerp}。 */
    public static double lerp(double a, double b, double m) {
        return a + (b - a) * m;
    }

    /** JS {@code Math.isBetween}。 */
    public static boolean isBetween(double n, double a, double b) {
        return (n - a) * (n - b) <= 0;
    }

    /** JS {@code Math.trimDeg}：逐字保留 {@code (a+180*15)%360-180}（JS % 与 Java % 对 double 同义）。 */
    public static double trimDeg(double a) {
        return (a + 180 * 15) % 360 - 180;
    }

    /** JS {@code Math.isPowerOfTwo}：JS 位运算按 int32，Java 侧同样截断到 int。 */
    public static boolean isPowerOfTwo(double x) {
        int i = (int) x;
        return (x > 1) && ((i & (i - 1)) == 0);
    }

    /** JS {@code Math.snapToValues(val, snap_points)}：epsilon 默认 12。 */
    public static double snapToValues(double val, double[] snapPoints) {
        return snapToValues(val, snapPoints, 12);
    }

    /** JS {@code Math.snapToValues(val, snap_points, epsilon)}：按 |val-p| 升序排后取最近点吸附。 */
    public static double snapToValues(double val, double[] snapPoints, double epsilon) {
        // JS 怪癖：空 snap_points 时 snaps[0] 为 undefined，|undefined - val| = NaN < epsilon 不成立 → 返回 val
        if (snapPoints.length == 0) return val;
        double[] snaps = snapPoints.clone();
        // JS：slice().sort((a, b) => |val-a| - |val-b|)；JS/Java sort 均稳定
        for (int i = 1; i < snaps.length; i++) {
            double key = snaps[i];
            double keyDist = Math.abs(val - key);
            int j = i - 1;
            while (j >= 0 && Math.abs(val - snaps[j]) > keyDist) {
                snaps[j + 1] = snaps[j];
                j--;
            }
            snaps[j + 1] = key;
        }
        if (Math.abs(snaps[0] - val) < epsilon) {
            return snaps[0];
        } else {
            return val;
        }
    }

    // ---------------------------------------------------------------- Array.prototype 扩展（List 静态化）

    /** JS {@code arr.safePush(...items)}：推入尚未包含的项，返回是否有新增。 */
    @SafeVarargs

    public static <T> boolean safePush(List<T> list, T... items) {
        boolean included = false;
        for (T item : items) {
            if (!list.contains(item)) {
                list.add(item);
                included = true;
            }
        }
        return included;
    }

    /** JS {@code arr.equals(array)}：逐元素 ==（嵌套数组递归）；JS 数字域即 double 比较。 */
    public static boolean equals(@Nullable List<?> list, @Nullable List<?> array) {
        if (array == null || list == null) return false;
        if (list.size() != array.size()) return false;
        for (int i = 0, l = list.size(); i < l; i++) {
            Object a = list.get(i);
            Object b = array.get(i);
            if (a instanceof List<?> la && b instanceof List<?> lb) {
                if (!equals(la, lb)) return false;
            } else if (!jsLooseElementEquals(a, b)) {
                return false;
            }
        }
        return true;
    }

    /** JS {@code !=}：数字间按 double 值，其余按引用/equals（Snowstorm 数组元素仅 string/number/array）。 */
    private static boolean jsLooseElementEquals(@Nullable Object a, @Nullable Object b) {
        if (a instanceof Number na && b instanceof Number nb) {
            return na.doubleValue() == nb.doubleValue();
        }
        return Objects.equals(a, b);
    }

    /**
     * JS {@code arr.remove(item)}：返回删除下标，未找到返回 false。
     * as-is 保留 JS 怪癖：返回 {@link Integer} 下标或 {@link Boolean#FALSE}；
     * 注意 JS 调用方若用真值判断，下标 0 为 falsy。
     */
    public static Object remove(List<?> list, Object item) {
        int index = list.indexOf(item);
        if (index > -1) {
            list.remove(index);
            return index;
        }
        return Boolean.FALSE;
    }

    /** JS {@code arr.empty()}：清空并返回自身。 */
    public static <T extends List<?>> T empty(T list) {
        list.clear();
        return list;
    }

    /** JS {@code arr.purge()}：同 empty。 */
    public static <T extends List<?>> T purge(T list) {
        list.clear();
        return list;
    }

    /** JS {@code arr.findInArray(key, value)}：返回首个 {@code this[i][key] === value} 的元素，否则 false。 */
    public static Object findInArray(List<? extends Map<String, Object>> list, String key, Object value) {
        for (Map<String, Object> element : list) {
            if (Objects.equals(element.get(key), value)) return element;
        }
        return Boolean.FALSE;
    }

    /** JS {@code arr.positiveItems()}：统计 truthy 元素个数。 */
    public static int positiveItems(List<?> list) {
        int x = 0, i = 0;
        while (i < list.size()) {
            if (JsSemantics.truthy(list.get(i))) x++;
            i++;
        }
        return x;
    }

    /** JS {@code arr.allEqual(s)}：全部元素 === s。 */
    public static boolean allEqual(List<?> list, @Nullable Object s) {
        int i = 0;
        while (i < list.size()) {
            Object e = list.get(i);
            // JS ===：类型不同即 false；数字按 double 值
            boolean equal = (e instanceof Number ne && s instanceof Number ns)
                    ? ne.doubleValue() == ns.doubleValue()
                    : Objects.equals(e, s);
            if (!equal) return false;
            i++;
        }
        return true;
    }

    /** JS {@code arr.random()}：随机元素；空数组 JS 得 undefined，此处返回 null。 */
    public static <T> @Nullable T random(List<T> list) {
        if (list.isEmpty()) return null;
        return list.get((int) Math.floor(WinterskyRandom.nextDouble() * list.size()));
    }

    // ---------------------------------------------------------------- String 工具

    /** JS {@code capitalizeFirstLetter}。 */
    public static String capitalizeFirstLetter(String string) {
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    /** JS {@code pluralS(arr)}：数组长度为 1 时返回空串，否则 's'。 */
    public static String pluralS(List<?> arr) {
        return arr.size() == 1 ? "" : "s";
    }

    /** JS {@code pluralS(1)}：数值 1 时返回空串（JS 中 arr === 1 分支）。 */
    public static String pluralS(int count) {
        return count == 1 ? "" : "s";
    }

    private static final Pattern EXTENSION_STRIP = Pattern.compile("\\.\\w+$");
    private static final Pattern PATH_SPLIT = Pattern.compile("[/\\\\]");

    /**
     * JS {@code pathToName(path, extension)}。
     *
     * @param extension JS 第三态参数：{@code Boolean.TRUE} 保留扩展名；{@code "mobs_id"} 走 mobs_id 分支；
     *                  其他/ null 去掉末尾扩展名
     */
    public static String pathToName(String path, @Nullable Object extension) {
        // JS：path.split('/').join('\\').split('\\')
        String[] pathArray = PATH_SPLIT.split(path, -1);
        String last = pathArray[pathArray.length - 1];
        if (Boolean.TRUE.equals(extension)) {
            return last;
        } else if ("mobs_id".equals(extension)) {
            // JS：last.split('.').slice(0, -1).join('.')
            String[] parts = last.split("\\.", -1);
            String name = String.join(".", java.util.Arrays.copyOf(parts, Math.max(parts.length - 1, 0)));
            if (name.equals("mobs") && pathArray.length >= 3 && !pathArray[pathArray.length - 3].isEmpty()) {
                name = name + " (" + JsSemantics.jsSubstr(pathArray[pathArray.length - 3], 0, 8) + "...)";
            }
            return name;
        } else {
            return EXTENSION_STRIP.matcher(last).replaceAll("");
        }
    }

    /** JS {@code pathToName(path)}：去扩展名。 */
    public static String pathToName(String path) {
        return pathToName(path, null);
    }

    private static final Pattern EXTENSION_MATCH = Pattern.compile("\\.\\w{2,24}$");

    /** JS {@code pathToExtension(path)}：无匹配返回 ''。 */
    public static String pathToExtension(String path) {
        Matcher m = EXTENSION_MATCH.matcher(path);
        if (!m.find()) return "";
        return m.group().replace(".", "").toLowerCase();
    }

    /**
     * JS {@code trimFloatNumber(val, max_digits = 4)}。
     *
     * <p>as-is 怪癖：返回类型为 JS 动态值——正常为 {@link String}；结果为 "-0" 时返回数字 0
     * （此处为 {@link Double} 0.0）。JS 的 {@code val == ''} 早退分支仅对字符串输入有意义，
     * Java 签名限定 double，不移植。
     */
    public static Object trimFloatNumber(double val, int maxDigits) {
        // JS：val.toFixed(max_digits) —— 十进制四舍五入（平局取较大 n）
        java.math.BigDecimal fixed = new java.math.BigDecimal(val)
                .setScale(maxDigits, java.math.RoundingMode.HALF_UP);
        String string = fixed.toPlainString().replaceAll("0+$", "").replaceAll("\\.$", "");
        // JS：string == -0 为松散相等——"0" 与 "-0" 都满足，返回数字 0
        if (string.equals("0") || string.equals("-0")) return 0.0;
        return string;
    }

    /** JS {@code trimFloatNumber(val)}：max_digits 默认 4。 */
    public static Object trimFloatNumber(double val) {
        return trimFloatNumber(val, 4);
    }

    // ---------------------------------------------------------------- compileJSON

    /**
     * compileJSON 的 oneLiner 标记：JS 检查 {@code o.constructor.name !== 'oneLiner'}。
     * Snowstorm 源中并不存在 oneLiner 构造器（恒为换行），此处保留结构钩子：
     * {@link Map} 实现本接口时按单行输出（as-is 语义等价）。
     */
    public interface OneLiner {
    }

    /** JS {@code compileJSON(object)}。 */
    public static String compileJSON(@Nullable Object object) {
        return compileJSON(object, false);
    }

    /**
     * JS {@code compileJSON(object, {small})}：把 Gson↔JS 值（Map/List/String/Double/Boolean/null）
     * 序列化为 Snowstorm 风格的 JSON 文本（tab 缩进、数字保留 5 位小数）。
     *
     * <p>注意：JS 的 compileJSON 返回纯文本字符串，并不产出 HTML 高亮标记——语法高亮是 UI 层
     * Prism 的职责，Java 侧同样返回纯文本（无偏离）。
     */
    public static String compileJSON(@Nullable Object object, boolean small) {
        return handleVar(object, 1, small);
    }

    private static String newLine(int tabs, boolean small) {
        if (small) return "";
        StringBuilder s = new StringBuilder("\n");
        for (int i = 0; i < tabs; i++) {
            s.append('\t');
        }
        return s.toString();
    }

    /** JS {@code typeof o === 'object'}：含 null。 */
    private static boolean isJsObject(@Nullable Object o) {
        return o == null || o instanceof Map<?, ?> || o instanceof List<?>;
    }

    private static String handleVar(@Nullable Object o, int tabs, boolean small) {
        StringBuilder out = new StringBuilder();
        if (o instanceof String str) {
            // String
            out.append('"').append(str
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replaceAll("\n|\r\n", "\\\\n")
                    .replace("\t", "\\t")).append('"');
        } else if (o instanceof Boolean b) {
            // Boolean
            out.append(b ? "true" : "false");
        } else if (o instanceof Number num) {
            // Number；JS typeof Infinity/NaN 也是 'number'，经 jsRound/toJsString 得
            // "Infinity"/"-Infinity"/"NaN"（as-is，非法 JSON 怪癖保留）
            out.append(JsSemantics.toJsString(JsSemantics.jsRound(num.doubleValue() * 100000) / 100000));
        } else if (o == null) {
            // Null（JS 此分支还列了 ±Infinity，但被 number 分支抢先，不可达）
            out.append("null");
        } else if (o instanceof List<?> list) {
            // Array
            boolean hasContent = false;
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                Object element = list.get(i);
                String compiled = handleVar(element, tabs + 1, small);
                if (!compiled.isEmpty()) {
                    boolean breaks = isJsObject(element);
                    if (hasContent) {
                        out.append(',');
                        if (!breaks && !small) out.append(' ');
                    }
                    if (breaks) out.append(newLine(tabs, small));
                    out.append(compiled);
                    hasContent = true;
                }
            }
            if (!list.isEmpty() && isJsObject(list.get(list.size() - 1))) {
                out.append(newLine(tabs - 1, small));
            }
            out.append(']');
        } else if (o instanceof Map<?, ?> map) {
            // Object
            boolean breaks = !(o instanceof OneLiner);
            boolean hasContent = false;
            out.append('{');
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String compiled = handleVar(entry.getValue(), tabs + 1, small);
                if (!compiled.isEmpty()) {
                    if (hasContent) {
                        out.append(',');
                        if (!breaks && !small) out.append(' ');
                    }
                    if (breaks) out.append(newLine(tabs, small));
                    out.append('"').append(String.valueOf(entry.getKey())).append("\":");
                    if (!small) out.append(' ');
                    out.append(compiled);
                    hasContent = true;
                }
            }
            if (breaks && hasContent) out.append(newLine(tabs - 1, small));
            out.append('}');
        }
        return out.toString();
    }

    // ---------------------------------------------------------------- lineify（import.js 局部函数，规格 §3 归入本类）

    /**
     * import.js 的 {@code lineify}：molang 多行展开——含分号的字符串按 {@code ;} 断行。
     * JS 中副作用 {@code input.expanded = true} 由调用方设置；null/空串/无分号时原样返回。
     */
    public static @Nullable String lineify(@Nullable String string) {
        if (string != null && !string.isEmpty() && string.contains(";")) {
            return string.replaceAll(";\\s*", ";\n").replaceAll("\n+$", "");
        }
        return string;
    }

    /** import.js lineify 的数组形态辅助：逐元素展开（as-is 语义）。 */
    public static List<@Nullable String> lineifyAll(List<@Nullable String> strings) {
        List<@Nullable String> result = new ArrayList<>(strings.size());
        for (String s : strings) {
            result.add(lineify(s));
        }
        return result;
    }
}

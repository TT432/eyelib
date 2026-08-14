package io.github.tt432.eyelib.molang.type;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * 用户面向的 molang 值类型。molang 运行时是动态类型（{@link MolangObject} 数字一律 float、
 * bool 即 0/1、无独立 int），本类型是<b>声明/展示层</b>的类型标注：
 * <ul>
 *   <li>{@code number<float>} / {@code number<int>} / {@code number<bool>} —— number 三子类型，
 *       运行时同值域，互为可赋（见节点图 PortType 兼容矩阵）；</li>
 *   <li>{@code string} —— 字符串；</li>
 *   <li>{@code array} —— molang 数组（仅 query 返回值可产出）；</li>
 *   <li>{@code object} —— struct 值（成员为命名 molang 值，{@link MolangStruct}）；</li>
 *   <li>{@code dynamic} —— 未标注/无法推断。</li>
 * </ul>
 *
 * <p>运行时推断（{@link #infer(MolangObject)}）只能可靠区分 string/array/number——
 * int 与 bool 在运行时不存在，必须来自声明（节点端口、变量声明、用户选择的显示类型）。
 */
public enum MolangType {
    FLOAT,
    INT,
    BOOL,
    STRING,
    ARRAY,
    /** struct/object 值（官方 syntax-guide Structs；成员为命名 molang 值）。 */
    OBJECT,
    DYNAMIC;

    /** 是否为 number 子类型（float/int/bool）。 */
    public boolean isNumber() {
        return this == FLOAT || this == INT || this == BOOL;
    }

    /**
     * 从运行时值推断展示类型。数字恒为 {@link #FLOAT}（bool/int 运行时不可区分，
     * 需要语义时请走声明侧类型）；null 为 {@link #DYNAMIC}。
     */
    public static MolangType infer(@Nullable MolangObject value) {
        if (value == null) {
            return DYNAMIC;
        }
        if (value instanceof MolangString) {
            return STRING;
        }
        if (value instanceof MolangArray) {
            return ARRAY;
        }
        if (value instanceof MolangStruct) {
            return OBJECT;
        }
        return value.isNumber() ? FLOAT : DYNAMIC;
    }

    /**
     * 按本类型格式化值用于显示：
     * <ul>
     *   <li>FLOAT：去尾零（{@code 3.0 → "3"}，{@code 3.5 → "3.5"}）；</li>
     *   <li>INT：向零取整；</li>
     *   <li>BOOL：{@code "true (1)"} / {@code "false (0)"}（保留底层值便于核对）；</li>
     *   <li>STRING：单引号包裹；</li>
     *   <li>ARRAY：{@code [a, b, ...]}（元素按推断类型格式化）；</li>
     *   <li>DYNAMIC：{@link MolangObject#asString()}。</li>
     * </ul>
     */
    public String format(@Nullable MolangObject value) {
        if (value == null) {
            return "<null>";
        }
        return switch (this) {
            case FLOAT -> formatFloat(value.asFloat());
            case INT -> Integer.toString((int) value.asFloat());
            case BOOL -> value.asBoolean() + " (" + (value.asBoolean() ? 1 : 0) + ")";
            case STRING -> "'" + value.asString() + "'";
            case ARRAY -> {
                if (value instanceof MolangArray<?> array) {
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < array.value().size(); i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        MolangObject element = array.value().get(i);
                        sb.append(infer(element).format(element));
                    }
                    yield sb.append(']').toString();
                }
                yield value.asString();
            }
            case DYNAMIC -> value.asString();
            case OBJECT -> {
                if (value instanceof MolangStruct struct) {
                    StringBuilder sb = new StringBuilder("{");
                    boolean first = true;
                    for (var entry : struct.members().entrySet()) {
                        if (!first) {
                            sb.append(", ");
                        }
                        first = false;
                        sb.append(entry.getKey()).append(": ")
                                .append(infer(entry.getValue()).format(entry.getValue()));
                    }
                    yield sb.append('}').toString();
                }
                yield value.asString();
            }
        };
    }

    /** 用户面向显示名：{@code number<float>} / {@code number<int>} / {@code number<bool>} / {@code string} / {@code array} / {@code dynamic}。 */
    public String displayName() {
        if (isNumber()) {
            return "number<" + name().toLowerCase(Locale.ROOT) + ">";
        }
        return name().toLowerCase(Locale.ROOT);
    }

    /** 浮点格式化：整数去 .0，其余保留原样（与节点图 codegen 的 formatNumber 同规则）。 */
    public static String formatFloat(float v) {
        if (v == Math.rint(v) && Float.isFinite(v) && Math.abs(v) < 9.0e15) {
            return Long.toString((long) v);
        }
        return Float.toString(v);
    }
}

package io.github.tt432.eyelib.molang.scratch;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scratch 积木种类定义：每种积木的形状、字段、插槽、语句体与语义分类。
 *
 * <p>形状语言对齐 Scratch 3.0：{@link Shape#PILL} 值积木（圆角胶囊）、{@link Shape#BOOL}
 * 谓词积木（六边形）、{@link Shape#STACK} 堆叠语句（顶凹底凸）、{@link Shape#C} 包体语句
 * （loop / for_each / 裸块）。比较与逻辑运算的结果是布尔语义，{@link #shapeOf} 动态判为
 * BOOL——同一 BINARY kind 按 op 字段切换形状。
 *
 * @author TT432
 */
public enum ScratchKind {
    // ---------- 表达式积木 ----------
    /** 数字字面量，field text = 原文（含科学计数法）。 */
    NUM(ScratchShape.PILL, ScratchCategory.LITERAL, fields("text", "0")),
    /** 字符串字面量，field text = 不含引号的内容。 */
    STR(ScratchShape.PILL, ScratchCategory.LITERAL, fields("text", "")),
    /** this。 */
    THIS(ScratchShape.PILL, ScratchCategory.VARIABLE),
    /** 点链变量访问：variable.x / temp.y / query.z / context.w，field path = 点链全名。 */
    VAR(ScratchShape.PILL, ScratchCategory.VARIABLE, fields("path", "variable.x")),
    /**
     * 函数调用，field name = 点链函数名（math.sin / query.is_name_any / 自定义裸名）。
     * 插槽数量可变（变长函数），由 UI 增删。
     */
    CALL(ScratchShape.PILL, ScratchCategory.QUERY, fields("name", "math.abs")),
    /** 二元运算，field op ∈ BINARY_OPS。比较/逻辑 op 时形状为 BOOL。 */
    BINARY(ScratchShape.PILL, ScratchCategory.OPERATOR, fields("op", "+"),
            sockets("left", "right")),
    /** 一元运算，field op ∈ {-, !}；"!" 时形状为 BOOL。 */
    UNARY(ScratchShape.PILL, ScratchCategory.OPERATOR, fields("op", "-"),
            sockets("expr")),
    /** 空合并 ??。 */
    NULLCO(ScratchShape.PILL, ScratchCategory.OPERATOR, sockets("left", "right")),
    /** 三元 cond ? whenTrue : whenFalse。 */
    TERNARY(ScratchShape.PILL, ScratchCategory.OPERATOR,
            sockets("cond", "whenTrue", "whenFalse")),
    /** 二元条件 cond ? whenFalse（molang 特有，cond 真值取 cond 否则取 whenFalse）。 */
    COND_BINARY(ScratchShape.PILL, ScratchCategory.OPERATOR,
            sockets("cond", "whenFalse")),
    /** 任意表达式的成员访问 owner.member（owner 非点链时使用；点链用 VAR）。 */
    MEMBER(ScratchShape.PILL, ScratchCategory.ACCESS, fields("member", "x"),
            sockets("owner")),
    /** 下标访问 owner[index]。 */
    INDEX(ScratchShape.PILL, ScratchCategory.ACCESS, sockets("owner", "index")),
    /** 箭头访问 target->owner.member（如 query.get_default_bone_pivot->context.other.x）。 */
    ARROW(ScratchShape.PILL, ScratchCategory.ACCESS, fields("owner", "context", "member", "x"),
            sockets("target")),
    /** 原文直通表达式（导入兜底/高级逃生舱），field text = molang 原文。 */
    RAW(ScratchShape.PILL, ScratchCategory.RAW, fields("text", "0")),

    // ---------- 语句积木 ----------
    /** 表达式语句（副作用调用等），导出为裸表达式或语句序列成员。 */
    STMT_EXPR(ScratchShape.STACK, ScratchCategory.ACTION, sockets("expr")),
    /** 赋值 target = value。 */
    STMT_ASSIGN(ScratchShape.STACK, ScratchCategory.VARIABLE, sockets("target", "value")),
    /** return value。 */
    STMT_RETURN(ScratchShape.STACK, ScratchCategory.CONTROL, sockets("value")),
    /** break [value]（value 插槽可留空）。 */
    STMT_BREAK(ScratchShape.STACK, ScratchCategory.CONTROL, optionalSockets("value")),
    /** continue [value]。 */
    STMT_CONTINUE(ScratchShape.STACK, ScratchCategory.CONTROL, optionalSockets("value")),
    /** loop(count, { body })。 */
    CTRL_LOOP(ScratchShape.C, ScratchCategory.CONTROL, sockets("count")),
    /** for_each(variable, collection, { body })。 */
    CTRL_FOREACH(ScratchShape.C, ScratchCategory.CONTROL, sockets("variable", "collection")),
    /** 裸语句块 { body }（作为语句使用的 BlockExpr）。 */
    CTRL_BLOCK(ScratchShape.C, ScratchCategory.CONTROL);

    /** 合法二元运算符（导入/导出/编辑共用口径）。 */
    public static final Set<String> BINARY_OPS =
            Set.of("+", "-", "*", "/", "<", "<=", ">", ">=", "==", "!=", "&&", "||");
    /** 结果形状为 BOOL 的二元运算符。 */
    public static final Set<String> BOOL_BINARY_OPS =
            Set.of("<", "<=", ">", ">=", "==", "!=", "&&", "||");
    /** 合法一元运算符。 */
    public static final Set<String> UNARY_OPS = Set.of("-", "!");

    private final ScratchShape baseShape;
    private final ScratchCategory category;
    private final Map<String, String> fields;
    private final List<SocketDef> sockets;

    ScratchKind(ScratchShape baseShape, ScratchCategory category) {
        this(baseShape, category, Map.of(), List.of());
    }

    ScratchKind(ScratchShape baseShape, ScratchCategory category, Map<String, String> fields) {
        this(baseShape, category, fields, List.of());
    }

    ScratchKind(ScratchShape baseShape, ScratchCategory category, List<SocketDef> sockets) {
        this(baseShape, category, Map.of(), sockets);
    }

    ScratchKind(ScratchShape baseShape, ScratchCategory category,
                Map<String, String> fields, List<SocketDef> sockets) {
        this.baseShape = baseShape;
        this.category = category;
        this.fields = fields;
        this.sockets = sockets;
    }

    private static Map<String, String> fields(String... nameAndDefaults) {
        if (nameAndDefaults.length % 2 != 0) {
            throw new IllegalArgumentException("fields 需要 name/default 成对");
        }
        var map = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < nameAndDefaults.length; i += 2) {
            map.put(nameAndDefaults[i], nameAndDefaults[i + 1]);
        }
        return Map.copyOf(map);
    }

    private static List<SocketDef> sockets(String... names) {
        return java.util.Arrays.stream(names).map(n -> new SocketDef(n, false)).toList();
    }

    private static List<SocketDef> optionalSockets(String... names) {
        return java.util.Arrays.stream(names).map(n -> new SocketDef(n, true)).toList();
    }

    /** 基础形状（不含字段动态判定）。 */
    public ScratchShape baseShape() {
        return baseShape;
    }

    /** 静态分类（CALL 等动态分类用 {@link #categoryOf(ScratchBlock)}）。 */
    public ScratchCategory category() {
        return category;
    }

    /** 字段默认值表（有序）。 */
    public Map<String, String> fieldDefaults() {
        return fields;
    }

    /** 静态插槽定义（CALL 为变长，返回空表，插槽由实例自行管理）。 */
    public List<SocketDef> sockets() {
        return sockets;
    }

    /** 是否语句类积木（STACK/C）。 */
    public boolean isStatement() {
        return baseShape == ScratchShape.STACK || baseShape == ScratchShape.C;
    }

    /** 是否 C 形包体积木。 */
    public boolean hasBody() {
        return baseShape == ScratchShape.C;
    }

    /**
     * 按字段内容动态判定渲染形状：比较/逻辑二元运算与逻辑非为 BOOL 六边形，
     * 其余按 {@link #baseShape}。
     */
    public static ScratchShape shapeOf(ScratchBlock block) {
        if (block.kind() == BINARY && BOOL_BINARY_OPS.contains(block.field("op"))) {
            return ScratchShape.BOOL;
        }
        if (block.kind() == UNARY && "!".equals(block.field("op"))) {
            return ScratchShape.BOOL;
        }
        return block.kind().baseShape;
    }

    /**
     * 按字段内容动态判定语义分类：CALL 按函数名前缀分流
     * （math.* → MATH，query.* → QUERY，自定义裸名 → CUSTOM）。
     */
    public static ScratchCategory categoryOf(ScratchBlock block) {
        if (block.kind() == CALL) {
            String name = block.field("name");
            if (name.startsWith("math.")) {
                return ScratchCategory.MATH;
            }
            if (name.indexOf('.') < 0) {
                return ScratchCategory.CUSTOM;
            }
            return ScratchCategory.QUERY;
        }
        return block.kind().category;
    }

    /** 插槽定义：名称 + 是否可空（可空插槽留空 = 语法上省略，如 break 无值）。 */
    public record SocketDef(String name, boolean optional) {
    }
}

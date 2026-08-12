package io.github.tt432.eyelib.client.scratch;

import io.github.tt432.eyelib.molang.scratch.ScratchBlock;
import io.github.tt432.eyelib.molang.scratch.ScratchCategory;
import io.github.tt432.eyelib.molang.scratch.ScratchKind;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Scratch 调色板内容定义：分类 → 积木模板列表。拖出 = {@link ScratchBlock#copy()}。
 *
 * <p>零 MC 依赖，可单测。条目显式携带 {@link ScratchCategory}（不用实例相等反查——
 * 含 lambda 字段的 record 不可做关联查找，见历史教训）。
 *
 * @author TT432
 */
public final class ScratchPalette {
    private ScratchPalette() {}

    /** 调色板条目：分类 + 模板积木（每次拖出都深拷贝，模板本体永不入工作区）。 */
    public record Entry(ScratchCategory category, ScratchBlock template) {}

    /** 分类展示顺序（对齐 Scratch 调色板直觉：值 → 运算 → 控制 → 动作 → 逃生舱）。 */
    public static final List<ScratchCategory> CATEGORY_ORDER = List.of(
            ScratchCategory.LITERAL,
            ScratchCategory.VARIABLE,
            ScratchCategory.QUERY,
            ScratchCategory.MATH,
            ScratchCategory.OPERATOR,
            ScratchCategory.ACCESS,
            ScratchCategory.CONTROL,
            ScratchCategory.ACTION,
            ScratchCategory.CUSTOM,
            ScratchCategory.RAW
    );

    /** 全部条目（按 CATEGORY_ORDER 分组有序）。 */
    public static List<Entry> entries() {
        List<Entry> all = new ArrayList<>();
        for (ScratchCategory cat : CATEGORY_ORDER) {
            all.addAll(BY_CATEGORY.getOrDefault(cat, List.of()));
        }
        return all;
    }

    private static final Map<ScratchCategory, List<Entry>> BY_CATEGORY = build();

    private static Map<ScratchCategory, List<Entry>> build() {
        Map<ScratchCategory, List<Entry>> map = new EnumMap<>(ScratchCategory.class);

        map.put(ScratchCategory.LITERAL, List.of(
                entry(ScratchCategory.LITERAL, ScratchKind.NUM),
                entry(ScratchCategory.LITERAL, ScratchKind.STR)));

        map.put(ScratchCategory.VARIABLE, List.of(
                entry(ScratchCategory.VARIABLE, ScratchKind.THIS),
                entry(ScratchCategory.VARIABLE, ScratchKind.VAR),
                entry(ScratchCategory.VARIABLE, field(ScratchKind.VAR, "path", "temp.x")),
                entry(ScratchCategory.VARIABLE, field(ScratchKind.VAR, "path", "query.x")),
                entry(ScratchCategory.VARIABLE, field(ScratchKind.VAR, "path", "context.x")),
                entry(ScratchCategory.VARIABLE, ScratchKind.STMT_ASSIGN)));

        map.put(ScratchCategory.QUERY, List.of(
                entry(ScratchCategory.QUERY, field(ScratchKind.CALL, "name", "query.time")),
                entry(ScratchCategory.QUERY, field(ScratchKind.CALL, "name", "query.distance_from_camera"))));

        map.put(ScratchCategory.MATH, List.of(
                entry(ScratchCategory.MATH, field(ScratchKind.CALL, "name", "math.sin")),
                entry(ScratchCategory.MATH, field(ScratchKind.CALL, "name", "math.cos")),
                entry(ScratchCategory.MATH, field(ScratchKind.CALL, "name", "math.abs")),
                entry(ScratchCategory.MATH, field(ScratchKind.CALL, "name", "math.clamp")),
                entry(ScratchCategory.MATH, field(ScratchKind.CALL, "name", "math.lerp")),
                entry(ScratchCategory.MATH, field(ScratchKind.CALL, "name", "math.random"))));

        map.put(ScratchCategory.OPERATOR, List.of(
                entry(ScratchCategory.OPERATOR, field(ScratchKind.BINARY, "op", "+")),
                entry(ScratchCategory.OPERATOR, field(ScratchKind.BINARY, "op", "<")),
                entry(ScratchCategory.OPERATOR, field(ScratchKind.BINARY, "op", "==")),
                entry(ScratchCategory.OPERATOR, field(ScratchKind.BINARY, "op", "&&")),
                entry(ScratchCategory.OPERATOR, field(ScratchKind.BINARY, "op", "||")),
                entry(ScratchCategory.OPERATOR, field(ScratchKind.UNARY, "op", "-")),
                entry(ScratchCategory.OPERATOR, field(ScratchKind.UNARY, "op", "!")),
                entry(ScratchCategory.OPERATOR, ScratchKind.NULLCO),
                entry(ScratchCategory.OPERATOR, ScratchKind.TERNARY),
                entry(ScratchCategory.OPERATOR, ScratchKind.COND_BINARY)));

        map.put(ScratchCategory.ACCESS, List.of(
                entry(ScratchCategory.ACCESS, ScratchKind.MEMBER),
                entry(ScratchCategory.ACCESS, ScratchKind.INDEX),
                entry(ScratchCategory.ACCESS, ScratchKind.ARROW)));

        map.put(ScratchCategory.CONTROL, List.of(
                entry(ScratchCategory.CONTROL, ScratchKind.STMT_RETURN),
                entry(ScratchCategory.CONTROL, ScratchKind.STMT_BREAK),
                entry(ScratchCategory.CONTROL, ScratchKind.STMT_CONTINUE),
                entry(ScratchCategory.CONTROL, ScratchKind.CTRL_LOOP),
                entry(ScratchCategory.CONTROL, ScratchKind.CTRL_FOREACH),
                entry(ScratchCategory.CONTROL, ScratchKind.CTRL_BLOCK)));

        map.put(ScratchCategory.ACTION, List.of(
                entry(ScratchCategory.ACTION, ScratchKind.STMT_EXPR)));

        map.put(ScratchCategory.CUSTOM, List.of(
                entry(ScratchCategory.CUSTOM, field(ScratchKind.CALL, "name", "my_func"))));

        map.put(ScratchCategory.RAW, List.of(
                entry(ScratchCategory.RAW, ScratchKind.RAW)));

        return map;
    }

    private static Entry entry(ScratchCategory cat, ScratchKind kind) {
        return new Entry(cat, ScratchBlock.of(kind));
    }

    private static Entry entry(ScratchCategory cat, ScratchBlock template) {
        return new Entry(cat, template);
    }

    private static ScratchBlock field(ScratchKind kind, String name, String value) {
        ScratchBlock block = ScratchBlock.of(kind);
        block.setField(name, value);
        return block;
    }
}

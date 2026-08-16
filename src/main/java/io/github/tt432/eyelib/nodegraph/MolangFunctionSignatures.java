package io.github.tt432.eyelib.nodegraph;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * molang 函数签名表：query.call / math.call / exec.call 的端口生成与编辑器下拉候选的依据。
 *
 * <p>内置条目按 eyelib molang 实现（{@code bridge/molang}）逐一核对——可见参数即
 * VISIBLE_ARG（scope/宿主注入参数不算）。仅收录**带参**函数；零参函数（query.anim_time
 * 等约 120 个）无端口生成需求，不入表。
 *
 * <p>自定义 .emolang 函数经 {@link #registerCustom} 挂入（供下拉候选与端口签名共用）；
 * 本类的内置表只读。
 *
 * @author TT432
 */
public final class MolangFunctionSignatures {
    private MolangFunctionSignatures() {
    }

    /** 参数的 molang 层面类型（数值 = molang 主类型；字符串 = 标签/名称/标识符类）。 */
    public enum ArgKind {
        NUMBER,
        STRING
    }

    /**
     * 单个参数。
     *
     * @param name 显示名（端口标签用；端口 id 恒为 argN，不受重命名影响）
     * @param kind 参数类型（仅标注语义，端口物理类型保持 ANY——ANY 端口才有行内字面值编辑器）
     */
    public record Arg(String name, ArgKind kind) {
    }

    /**
     * 函数签名。
     *
     * @param fixed  固定前缀参数（可为空表 = 纯可变参数函数）
     * @param varArg 可变尾参（null = 定长函数）；name 为基准名，端口标签派生为 {@code name[i]}
     */
    public record Signature(List<Arg> fixed, @Nullable Arg varArg) {
        /** 定长函数的参数个数。 */
        public int fixedArity() {
            return varArg == null ? fixed.size() : -1;
        }
    }

    private static final Map<String, Signature> BUILT_IN = buildTable();
    private static final Map<String, Signature> CUSTOMS = new HashMap<>();

    /** 查签名（全名，如 {@code "query.is_name_any"} / {@code "math.clamp"}）；未知 → null。 */
    public static @Nullable Signature find(String functionName) {
        Signature custom = CUSTOMS.get(functionName);
        return custom != null ? custom : BUILT_IN.get(functionName);
    }

    /** 内置表全部函数名（只读视图；完备性测试用，防签名表与映射树漂移）。 */
    static java.util.Set<String> builtInNames() {
        return BUILT_IN.keySet();
    }

    /** 是否定长签名（定长函数的 args 列表选项无意义，编辑器隐藏之）。 */
    public static boolean isFixedArity(String functionName) {
        Signature sig = find(functionName);
        return sig != null && sig.varArg() == null;
    }

    /**
     * args 列表行的显示标签：变长函数 → {@code 名[...]}（string 元素带 {@code : str[]}）；
     * 未知函数 → {@code "args[...]"}；定长函数 → null（不显示列表行）。
     */
    public static @Nullable String variadicListLabel(String functionName) {
        Signature sig = find(functionName);
        if (sig == null) {
            return "args[...]";
        }
        Arg varArg = sig.varArg();
        if (varArg == null) {
            return null;
        }
        return varArg.name() + "[...]" + (varArg.kind() == ArgKind.STRING ? ": str[]" : "");
    }

    /**
     * 自定义函数名候选：根前缀匹配（如 {@code "query"} 匹配 {@code "query.xxx"}）；
     * 无根裸名（.emolang 自定义函数）并入全部根的候选（值语境通用）。
     */
    public static List<String> customNames(String root) {
        String prefix = root + ".";
        List<String> names = new ArrayList<>();
        for (String name : CUSTOMS.keySet()) {
            if (name.startsWith(prefix) || name.indexOf('.') < 0) {
                names.add(name);
            }
        }
        return List.copyOf(names);
    }

    /** 注册自定义函数签名（.emolang 加载；重名覆盖）。 */
    public static void registerCustom(String functionName, Signature signature) {
        CUSTOMS.put(functionName, signature);
    }

    /** 清空全部自定义函数（.emolang 重载前调用）。 */
    public static void clearCustoms() {
        CUSTOMS.clear();
    }

    // ---------- 内置表 ----------

    private static Arg num(String name) {
        return new Arg(name, ArgKind.NUMBER);
    }

    private static Arg str(String name) {
        return new Arg(name, ArgKind.STRING);
    }

    private static Signature fixed(Arg... args) {
        return new Signature(List.of(args), null);
    }

    private static Signature varArgs(Arg tail) {
        return new Signature(List.of(), tail);
    }

    private static Signature varArgs(Arg first, Arg tail) {
        return new Signature(List.of(first), tail);
    }

    private static Signature varArgs(List<Arg> fixedPrefix, Arg tail) {
        return new Signature(fixedPrefix, tail);
    }

    private static Map<String, Signature> buildTable() {
        Map<String, Signature> t = new HashMap<>();

        // ---------- math.*（59 个，全部带参、定长；命名对齐 Microsoft molang 文档） ----------
        Map<String, String[]> math = new HashMap<>();
        math.put("abs", new String[]{"value"});
        math.put("acos", new String[]{"x"});
        math.put("asin", new String[]{"x"});
        math.put("atan", new String[]{"x"});
        math.put("atan2", new String[]{"y", "x"});
        math.put("ceil", new String[]{"x"});
        math.put("clamp", new String[]{"value", "min", "max"});
        math.put("cos", new String[]{"x"});
        math.put("degrees", new String[]{"x"});
        math.put("die_roll", new String[]{"num", "low", "high"});
        math.put("die_roll_integer", new String[]{"num", "low", "high"});
        math.put("exp", new String[]{"x"});
        math.put("floor", new String[]{"x"});
        math.put("hermite_blend", new String[]{"x"});
        math.put("lerp", new String[]{"start", "end", "t"});
        math.put("lerprotate", new String[]{"start", "end", "t"});
        math.put("ln", new String[]{"x"});
        math.put("max", new String[]{"a", "b"});
        math.put("min", new String[]{"a", "b"});
        math.put("min_angle", new String[]{"x"});
        math.put("mod", new String[]{"x", "denominator"});
        math.put("pow", new String[]{"base", "exponent"});
        math.put("radians", new String[]{"x"});
        math.put("random", new String[]{"low", "high"});
        math.put("random_integer", new String[]{"low", "high"});
        math.put("round", new String[]{"x"});
        math.put("sin", new String[]{"x"});
        math.put("sqrt", new String[]{"x"});
        math.put("trunc", new String[]{"x"});
        String[] ease = {"start", "end", "t"};
        for (String family : new String[]{"ease_in", "ease_inout", "ease_out"}) {
            for (String kind : new String[]{"back", "bounce", "circ", "cubic", "elastic",
                    "expo", "quad", "quart", "quint", "sine"}) {
                math.put(family + "_" + kind, ease);
            }
        }
        for (var entry : math.entrySet()) {
            Arg[] args = new Arg[entry.getValue().length];
            for (int i = 0; i < args.length; i++) {
                args[i] = num(entry.getValue()[i]);
            }
            t.put("math." + entry.getKey(), fixed(args));
        }

        // ---------- query.*（33 个带参；按 bridge/molang/MolangBuiltInQuery 实现核对） ----------
        t.put("query.any", varArgs(num("value"), num("candidates")));
        t.put("query.armor_color_slot", fixed(num("slot")));
        t.put("query.armor_texture_slot", fixed(num("slot")));
        t.put("query.bone_orientation_trs", fixed(str("bone")));
        t.put("query.bone_origin", fixed(str("bone")));
        t.put("query.boots_is", varArgs(str("items")));
        t.put("query.camera_distance_range_lerp", fixed(num("min"), num("max")));
        t.put("query.camera_rotation", fixed(num("axis")));
        t.put("query.chestplate_is", varArgs(str("items")));
        t.put("query.cooldown_time", varArgs(str("slots")));
        t.put("query.cooldown_time_remaining", varArgs(str("slots")));
        t.put("query.equipped_item_any_tag", varArgs(str("slot"), str("tags")));
        t.put("query.entity_biome_has_any_identifier", varArgs(str("identifiers")));
        t.put("query.entity_biome_has_any_tags", varArgs(str("tags")));
        t.put("query.get_root_locator_offset", fixed(str("locator"), str("axis")));
        t.put("query.graphics_mode_is_any", varArgs(str("modes")));
        t.put("query.has_property", fixed(str("property")));
        t.put("query.has_armor_slot", fixed(num("slot")));
        t.put("query.head_x_rotation", fixed(num("head")));
        t.put("query.head_y_rotation", fixed(num("head")));
        t.put("query.helmet_is", varArgs(str("items")));
        t.put("query.in_range", fixed(num("value"), num("min"), num("max")));
        t.put("query.isItemNameAny", varArgs(List.of(str("hand"), num("index")), str("items")));
        t.put("query.is_damage_by", varArgs(str("damage_types")));
        t.put("query.is_item_equipped", fixed(str("hand")));
        t.put("query.is_item_name_any", varArgs(str("hand"), str("items")));
        t.put("query.is_name_any", varArgs(str("names")));
        t.put("query.is_owner_identifier_any", varArgs(str("identifiers")));
        t.put("query.is_pack_setting_enabled", fixed(str("setting")));
        t.put("query.is_pack_setting_selected", fixed(str("setting"), str("selection")));
        t.put("query.get_pack_setting", fixed(str("setting")));
        t.put("query.is_riding_any_entity_of_type", varArgs(str("types")));
        t.put("query.itemIsCharged", fixed(str("hand")));
        t.put("query.item_slot_to_bone_name", fixed(str("slot")));
        t.put("query.leggings_is", varArgs(str("items")));
        t.put("query.main_hand_is", varArgs(str("items")));
        t.put("query.off_hand_is", varArgs(str("items")));
        t.put("query.position", fixed(num("axis")));
        t.put("query.position_delta", fixed(num("axis")));
        t.put("query.property", fixed(str("property")));
        t.put("query.relative_block_has_all_tags",
                varArgs(List.of(num("x"), num("y"), num("z")), str("tags")));
        t.put("query.relative_block_has_any_tag",
                varArgs(List.of(num("x"), num("y"), num("z")), str("tags")));
        t.put("query.rotation_to_camera", fixed(num("axis")));

        return Map.copyOf(t);
    }
}

package io.github.tt432.eyelib.nodegraph.codegen;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.MolangLiterals;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** 节点图发射的公共静态工具：节点选项取值、连线查询、结果拼接。 */
final class EmitSupport {
    /** 可直接文本复制的表达式：全名标识符 / 数字字面量 / 字符串字面量。 */
    private static final Pattern DUPLICATABLE = Pattern.compile(
            "(?:variable|temp|query|math|context|geometry|texture|material)\\.[A-Za-z0-9_.]+"
                    + "|-?[0-9]+(?:\\.[0-9]+)?"
                    + "|'(?:[^'\\\\]|\\\\.)*'");

    private EmitSupport() {
    }

    /** 永不提取的平凡节点：常量、单变量/属性引用（§2.4-3）。 */
    static boolean isTrivial(NodeType type, NodeInstance node) {
        return switch (type.kind()) {
            case CONST_NUMBER, CONST_INT, CONST_BOOL, CONST_STRING, CONST_COLOR, COLOR_COMPOSE,
                 VARIABLE, TEMP_GET, CONTEXT_GET,
                 REF_GEOMETRY, REF_TEXTURE, REF_MATERIAL -> true;
            case QUERY_CALL, MATH_CALL -> {
                // v11：无固定前缀端口且 args 列表为空 = 零参调用（属性访问形），永不提取
                var sig = io.github.tt432.eyelib.nodegraph.MolangFunctionSignatures
                        .find(node.optionString("function", ""));
                yield (sig == null || sig.fixed().isEmpty()) && listArgLiterals(node).isEmpty();
            }
            default -> false;
        };
    }

    static boolean isDuplicatable(String expr) {
        return DUPLICATABLE.matcher(expr).matches();
    }

    /** args 列表选项 → molang 字面值参数（空串条目跳过——编辑器半成品行不发射）。 */
    static List<String> listArgLiterals(NodeInstance node) {
        JsonElement raw = node.options().get("args");
        if (raw == null || !raw.isJsonArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonElement e : raw.getAsJsonArray()) {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() && e.getAsString().isEmpty()) {
                continue;
            }
            String lit = MolangLiterals.literal(e);
            if (lit != null) {
                out.add(lit);
            }
        }
        return out;
    }

    static String withRoot(String root, String name) {
        return name.startsWith(root + ".") ? name : root + "." + name;
    }

    static String string(NodeInstance node, NodeType type, String id) {
        return node.option(id, type).map(JsonElement::getAsString).orElse("");
    }

    static String number(NodeInstance node, NodeType type, String id) {
        return node.option(id, type)
                .filter(JsonElement::isJsonPrimitive)
                .map(e -> formatNumber(e.getAsDouble()))
                .orElse("0");
    }

    static boolean bool(NodeInstance node, NodeType type, String id) {
        return node.option(id, type)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsBoolean)
                .orElse(false);
    }

    /** 数字格式化：整数去 .0（1.0 → "1"，1.5 → "1.5"）。 */
    static String formatNumber(double v) {
        return MolangLiterals.formatNumber(v);
    }

    /** 字符串字面量：单引号包裹，转义 ' 与 \。 */
    static String quote(String s) {
        return MolangLiterals.quote(s);
    }

    /** 多条入线（非法单连接）时取 from (node, port) 字典序最小者，保证确定性。 */
    static Optional<Wire> wireInto(GraphData graph, String nodeUid, String portId) {
        return graph.wires().stream()
                .filter(w -> w.to().node().equals(nodeUid) && w.to().port().equals(portId))
                .min(Comparator.comparing((Wire w) -> w.from().node()).thenComparing(w -> w.from().port()));
    }

    /** 某输出端口的出线（多条时取 to (node, port) 字典序最小者，与 EXEC_FANOUT 诊断共存兜底）。 */
    static Optional<Wire> wireOutOf(GraphData graph, String nodeUid, String portId) {
        return graph.wires().stream()
                .filter(w -> w.from().node().equals(nodeUid) && w.from().port().equals(portId))
                .min(Comparator.comparing((Wire w) -> w.to().node()).thenComparing(w -> w.to().port()));
    }

    static Out combine(List<Out> parts, String expr) {
        List<String> preludes = new ArrayList<>();
        for (Out part : parts) {
            preludes.addAll(part.preludes());
        }
        return new Out(preludes, expr);
    }

    static List<String> withPreludes(Out out, String statement) {
        List<String> statements = new ArrayList<>(out.preludes());
        statements.add(statement);
        return statements;
    }
}

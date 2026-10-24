package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** 运算区块：二元/一元操作符表（一符一类型）与 op.ternary / op.null_coalesce。 */
final class NodeTypesOperators {
    private NodeTypesOperators() {
    }

    /**
     * 二元操作符符号表：{符号, id 名}（节点类型 id = "op." + 名，v14 起一符一类型）。
     * 顺序 = 节点库目录顺序。
     */
    static final String[][] BINARY_OP_TABLE = {
            {"+", "add"}, {"-", "subtract"}, {"*", "multiply"}, {"/", "divide"},
            {"==", "equal"}, {"!=", "not_equal"}, {"<", "less"}, {"<=", "less_equal"},
            {">", "greater"}, {">=", "greater_equal"}, {"&&", "and"}, {"||", "or"},
    };

    /** 一元操作符符号表（同上）。 */
    static final String[][] UNARY_OP_TABLE = {
            {"-", "negate"}, {"!", "not"},
    };

    /** 节点类型 id → 操作符符号（含二元与一元）。 */
    static final Map<String, String> OP_SYMBOL_BY_TYPE = new LinkedHashMap<>();

    // v14 起一符一节点类型（规格 nodegraph-workbench §2 拆分决策）：操作符是类型身份，
    // 不再有 op 实例选项；输出端口类型在注册时固化（逻辑/比较 → BOOL，其余 → FLOAT）。

    /** 二元操作符：符号 → 节点类型（符号表顺序）。 */
    static final Map<String, NodeType> BINARY_OP_TYPES = registerOps(BINARY_OP_TABLE, true);
    /** 一元操作符：符号 → 节点类型。 */
    static final Map<String, NodeType> UNARY_OP_TYPES = registerOps(UNARY_OP_TABLE, false);

    static Map<String, NodeType> registerOps(String[][] table, boolean binary) {
        Map<String, NodeType> map = new LinkedHashMap<>();
        for (String[] row : table) {
            String symbol = row[0];
            PortType out = binary
                    ? (NodeTypes.LOGICAL_OPS.contains(symbol) ? PortType.BOOL : PortType.FLOAT)
                    : ("!".equals(symbol) ? PortType.BOOL : PortType.FLOAT);
            List<PortDef> inputs = binary
                    ? List.of(PortDef.in("a", PortType.ANY, new JsonPrimitive(0)),
                              PortDef.in("b", PortType.ANY, new JsonPrimitive(0)))
                    : List.of(PortDef.in("a", PortType.ANY, new JsonPrimitive(0)));
            NodeType type = NodeTypes.register(NodeType.of(
                    "op." + row[1], binary ? NodeType.Kind.OP_BINARY : NodeType.Kind.OP_UNARY,
                    NodeTypes.CAT_OPERATOR, List.of(), inputs, List.of(PortDef.out("out", out))));
            map.put(symbol, type);
            OP_SYMBOL_BY_TYPE.put(type.id(), symbol);
        }
        return Map.copyOf(map);
    }

    /** 二元操作符符号 → 节点类型；未知符号返回 null。 */
    static @Nullable NodeType binaryOp(String symbol) {
        return BINARY_OP_TYPES.get(symbol);
    }

    /** 一元操作符符号 → 节点类型；未知符号返回 null。 */
    static @Nullable NodeType unaryOp(String symbol) {
        return UNARY_OP_TYPES.get(symbol);
    }

    /** 操作符节点类型 id → 符号；非操作符类型抛 {@link IllegalArgumentException}。 */
    static String opSymbolOf(String typeId) {
        String symbol = OP_SYMBOL_BY_TYPE.get(typeId);
        if (symbol == null) {
            throw new IllegalArgumentException("not an operator node type: " + typeId);
        }
        return symbol;
    }

    static final NodeType OP_TERNARY = NodeTypes.register(NodeType.of(
            "op.ternary", NodeType.Kind.OP_TERNARY, NodeTypes.CAT_OPERATOR,
            List.of(),
            List.of(
                    PortDef.in("cond", PortType.BOOL, new JsonPrimitive(1)),
                    PortDef.in("a", PortType.ANY, new JsonPrimitive(0)),
                    PortDef.in("b", PortType.ANY, new JsonPrimitive(0))),
            List.of(PortDef.out("out", PortType.ANY))));

    static final NodeType OP_NULLCOALESCE = NodeTypes.register(NodeType.of(
            "op.null_coalesce", NodeType.Kind.OP_NULLCOALESCE, NodeTypes.CAT_OPERATOR,
            List.of(),
            List.of(
                    PortDef.in("a", PortType.ANY, new JsonPrimitive(0)),
                    PortDef.in("b", PortType.ANY, new JsonPrimitive(0))),
            List.of(PortDef.out("out", PortType.ANY))));
}

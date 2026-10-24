package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Optional;

/** 执行流区块：exec.loop / exec.for_each / exec.break / exec.continue / exec.return。 */
final class NodeTypesExecFlow {
    private NodeTypesExecFlow() {
    }

    static final NodeType EXEC_LOOP = NodeTypes.register(NodeType.of(
            "exec.loop", NodeType.Kind.EXEC_LOOP, NodeTypes.CAT_EXEC,
            List.of(),
            List.of(
                    NodeTypePorts.execIn(),
                    PortDef.in("count", PortType.INT, new JsonPrimitive(10)),
                    new PortDef("body", PortDirection.IN, PortType.EXEC, Optional.empty(), false)),
            List.of(NodeTypePorts.execOut())));

    static final NodeType EXEC_FOREACH = NodeTypes.register(NodeType.of(
            "exec.for_each", NodeType.Kind.EXEC_FOREACH, NodeTypes.CAT_EXEC,
            List.of(NodeOptionDef.string("var_name", "temp.item")),
            List.of(
                    NodeTypePorts.execIn(),
                    PortDef.in("array", PortType.ARRAY),
                    new PortDef("body", PortDirection.IN, PortType.EXEC, Optional.empty(), false)),
            List.of(NodeTypePorts.execOut())));

    // 终端节点（break/continue/return）也带 exec_out：我们的链是「反向汇入槽」模型，
    // 没有 exec_out 的节点无法接入 loop.body 等 EXEC 槽；其后的语句为死代码（语义上正确）。
    static final NodeType EXEC_BREAK = NodeTypes.register(NodeType.of(
            "exec.break", NodeType.Kind.EXEC_BREAK, NodeTypes.CAT_EXEC,
            List.of(),
            List.of(NodeTypePorts.execIn()),
            List.of(NodeTypePorts.execOut())));

    static final NodeType EXEC_CONTINUE = NodeTypes.register(NodeType.of(
            "exec.continue", NodeType.Kind.EXEC_CONTINUE, NodeTypes.CAT_EXEC,
            List.of(),
            List.of(NodeTypePorts.execIn()),
            List.of(NodeTypePorts.execOut())));

    static final NodeType EXEC_RETURN = NodeTypes.register(NodeType.of(
            "exec.return", NodeType.Kind.EXEC_RETURN, NodeTypes.CAT_EXEC,
            List.of(),
            List.of(
                    NodeTypePorts.execIn(),
                    PortDef.in("value", PortType.ANY, new JsonPrimitive(0))),
            List.of(NodeTypePorts.execOut())));
}

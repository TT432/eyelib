package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonPrimitive;
import java.util.List;

/** 变量区块：variable 与 context.get/temp.get/exec.set_var/exec.set_temp。 */
final class NodeTypesVariables {
    private NodeTypesVariables() {
    }

    /**
     * variable：黑板变量本身（变量面板拖拽产物）。name 不带根（与 {@link VariableDecl#name()} 一致）。
     * 左读右写（规格 §3.1，v9）：out（右）接任意值端口 = 隐式读；in（左，multi）接
     * exec.set_var.target / ref write: 输出 = 写入通道。写入边不参与环检测与布局分层。
     */
    static final NodeType VARIABLE = NodeTypes.register(NodeType.of(
            "variable", NodeType.Kind.VARIABLE, NodeTypes.CAT_VARIABLE,
            List.of(NodeOptionDef.string("name", "foo")),
            List.of(PortDef.inMulti("in", PortType.VARIABLE)),
            List.of(PortDef.out("out", PortType.VARIABLE))));

    /** 写入通道判定：指向 variable 节点 in 端口的边（v9 左读右写）。 */
    static boolean isVariableWriteInput(String nodeTypeId, String portId) {
        return VARIABLE.id().equals(nodeTypeId) && "in".equals(portId);
    }

    static final NodeType CONTEXT_GET = NodeTypes.register(NodeType.of(
            "context.get", NodeType.Kind.CONTEXT_GET, NodeTypes.CAT_VARIABLE,
            List.of(NodeOptionDef.string("name", "context.other")),
            List.of(),
            List.of(PortDef.out("out", PortType.ANY))));

    static final NodeType TEMP_GET = NodeTypes.register(NodeType.of(
            "temp.get", NodeType.Kind.TEMP_GET, NodeTypes.CAT_VARIABLE,
            List.of(NodeOptionDef.string("name", "temp.t")),
            List.of(),
            List.of(PortDef.out("out", PortType.ANY))));

    /** exec.set_var：黑板变量写入。target（右侧输出）接 variable 节点的 in（写身份）；value 为写入值。 */
    static final NodeType EXEC_SET_VAR = NodeTypes.register(NodeType.of(
            "exec.set_var", NodeType.Kind.EXEC_SET_VAR, NodeTypes.CAT_EXEC,
            List.of(),
            List.of(NodeTypePorts.execIn(),
                    PortDef.in("value", PortType.ANY, new JsonPrimitive(0))),
            List.of(NodeTypePorts.execOut(), PortDef.outSingle("target", PortType.VARIABLE))));

    /** exec.set_temp：瞬态 temp 赋值（不进变量面板；name 可带不带 temp. 前缀）。 */
    static final NodeType EXEC_SET_TEMP = NodeTypes.register(NodeType.of(
            "exec.set_temp", NodeType.Kind.EXEC_SET_TEMP, NodeTypes.CAT_EXEC,
            List.of(NodeOptionDef.string("name", "temp.t")),
            List.of(NodeTypePorts.execIn(), PortDef.in("value", PortType.ANY, new JsonPrimitive(0))),
            List.of(NodeTypePorts.execOut())));
}

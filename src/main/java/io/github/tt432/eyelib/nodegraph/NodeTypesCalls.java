package io.github.tt432.eyelib.nodegraph;

import java.util.ArrayList;
import java.util.List;

/** 查询与数学区块：query.call / math.call / exec.call（含按 MolangFunctionSignatures 签名生成的 arg 端口）。 */
final class NodeTypesCalls {
    private NodeTypesCalls() {
    }

    /**
     * call 类节点的 arg 端口（v11）：函数在 {@link MolangFunctionSignatures} 有签名时按签名生成
     * 固定前缀端口（端口 id 恒 argN、参数名/类型进显示 label、物理类型 ANY——ANY 输入才有
     * 行内字面值编辑器）；变长尾参与未知函数的全部参数不产生端口，走 {@code args}
     * 字面值列表选项在节点内编辑（规格 nodegraph-variadic-call-list）。
     */
    static List<PortDef> callArgPorts(NodeInstance instance) {
        MolangFunctionSignatures.Signature sig =
                MolangFunctionSignatures.find(instance.optionString("function", ""));
        if (sig == null) {
            return List.of();
        }
        List<PortDef> ports = new ArrayList<>(sig.fixed().size());
        for (int i = 0; i < sig.fixed().size(); i++) {
            MolangFunctionSignatures.Arg arg = sig.fixed().get(i);
            String label = arg.kind() == MolangFunctionSignatures.ArgKind.STRING
                    ? arg.name() + ": str"
                    : arg.name();
            ports.add(PortDef.inLabeled("arg" + (i + 1), PortType.ANY, label));
        }
        return ports;
    }

    static final NodeType QUERY_CALL = NodeTypes.register(NodeType.dynamic(
            "query.call", NodeType.Kind.QUERY_CALL, NodeTypes.CAT_QUERY,
            List.of(
                    NodeOptionDef.asset("function", "query.anim_time", "molang.query"),
                    NodeOptionDef.list("args")),
            (instance, resolver) -> callArgPorts(instance),
            NodeType.PortProvider.fixed(List.of(PortDef.out("out", PortType.ANY)))));

    static final NodeType MATH_CALL = NodeTypes.register(NodeType.dynamic(
            "math.call", NodeType.Kind.MATH_CALL, NodeTypes.CAT_QUERY,
            List.of(
                    NodeOptionDef.asset("function", "math.sin", "molang.math"),
                    NodeOptionDef.list("args")),
            (instance, resolver) -> callArgPorts(instance),
            NodeType.PortProvider.fixed(List.of(PortDef.out("out", PortType.FLOAT)))));

    static final NodeType EXEC_CALL = NodeTypes.register(NodeType.dynamic(
            "exec.call", NodeType.Kind.EXEC_CALL, NodeTypes.CAT_EXEC,
            List.of(
                    NodeOptionDef.asset("function", "query.foo", "molang.call"),
                    NodeOptionDef.list("args")),
            (instance, resolver) -> {
                List<PortDef> ports = new ArrayList<>();
                ports.add(NodeTypePorts.execIn());
                ports.addAll(callArgPorts(instance));
                return ports;
            },
            NodeType.PortProvider.fixed(List.of(NodeTypePorts.execOut()))));
}

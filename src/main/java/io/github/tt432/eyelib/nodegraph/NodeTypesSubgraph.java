package io.github.tt432.eyelib.nodegraph;

import java.util.List;
import java.util.Optional;

/** 子图区块：subgraph.call / subgraph.input / subgraph.output。 */
final class NodeTypesSubgraph {
    private NodeTypesSubgraph() {
    }

    static final NodeType SUBGRAPH_CALL = NodeTypes.register(NodeType.dynamic(
            "subgraph.call", NodeType.Kind.SUBGRAPH_CALL, NodeTypes.CAT_SUBGRAPH,
            List.of(NodeOptionDef.string("subgraph", "")),
            (instance, resolver) -> {
                String name = instance.optionString("subgraph", "");
                return resolver.resolve(name)
                        .map(iface -> iface.inputs().stream()
                                .<PortDef>map(p -> p.defaultValue()
                                        .map(d -> new PortDef(p.name(), PortDirection.IN, p.type(), Optional.of(d), false))
                                        .orElse(PortDef.in(p.name(), p.type())))
                                .toList())
                        .orElse(List.of());
            },
            (instance, resolver) -> {
                String name = instance.optionString("subgraph", "");
                return resolver.resolve(name)
                        .map(iface -> List.of(PortDef.out("result", iface.output().type())))
                        .orElse(List.of(PortDef.out("result", PortType.ANY)));
            }));

    /** 子图输入锚点：动态输出 = 所在图接口的输入参数。 */
    static final NodeType SUBGRAPH_INPUT = NodeTypes.register(NodeType.dynamic(
            "subgraph.input", NodeType.Kind.SUBGRAPH_INPUT, NodeTypes.CAT_SUBGRAPH,
            List.of(),
            NodeType.PortProvider.fixed(List.of()),
            (instance, resolver) -> resolver.self()
                    .map(iface -> iface.inputs().stream()
                            .<PortDef>map(p -> PortDef.out(p.name(), p.type()))
                            .toList())
                    .orElse(List.of())));

    /** 子图输出锚点：exec_in（可空）+ result 输入（类型随所在图接口输出）。 */
    static final NodeType SUBGRAPH_OUTPUT = NodeTypes.register(NodeType.dynamic(
            "subgraph.output", NodeType.Kind.SUBGRAPH_OUTPUT, NodeTypes.CAT_SUBGRAPH,
            List.of(),
            (instance, resolver) -> {
                PortType resultType = resolver.self()
                        .map(iface -> iface.output().type())
                        .orElse(PortType.ANY);
                return List.of(
                        new PortDef("exec_in", PortDirection.IN, PortType.EXEC, Optional.empty(), false),
                        PortDef.in("result", resultType));
            },
            NodeType.PortProvider.fixed(List.of())));
}


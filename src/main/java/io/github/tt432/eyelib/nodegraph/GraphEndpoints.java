package io.github.tt432.eyelib.nodegraph;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * wire 端点解析与资源引用误用检查（检查 11）。
 *
 * <p>纯静态工具；端点解析依赖调用方传入的实例端口索引，不持有图状态。
 */
final class GraphEndpoints {

    private GraphEndpoints() {
    }

    /** 只能接入装配槽（animate.entry.ref）或 entity.root 动画声明端口的引用节点。 */
    private static final Set<String> ASSEMBLY_ONLY_REFS = Set.of("ref.animation", "ref.ac");
    /** 可接表达式槽、list/material 条目 value 或 RC 锚点声明端口的引用节点。 */
    private static final Set<String> VALUE_REFS = Set.of("ref.geometry", "ref.texture", "ref.material");

    /** wire 端点解析结果。 */
    record Endpoint(NodeInstance node, PortDef port, boolean output) {
    }

    /**
     * 解析 wire 端点：节点必须存在、端口 id 必须在该实例端口集内（动态端口经 resolver 推导）。
     * 失败时产生 {@link GraphValidator#UNKNOWN_WIRE_ENDPOINT} 诊断并返回 null。
     */
    static @Nullable Endpoint resolveEndpoint(PortRef ref, Map<String, NodeInstance> byUid,
                                             Map<String, NodeType> types,
                                             Map<String, List<PortDef>> inputs,
                                             Map<String, List<PortDef>> outputs,
                                             List<Diagnostic> out) {
        NodeInstance node = byUid.get(ref.node());
        if (node == null) {
            out.add(Diagnostic.error(GraphValidator.UNKNOWN_WIRE_ENDPOINT, "连线端点节点不存在：" + ref.node(), ref.node()));
            return null;
        }
        if (!types.containsKey(ref.node())) {
            return null; // 未知类型已报 UNKNOWN_NODE_TYPE
        }
        Optional<PortDef> asOut = findPort(outputs.getOrDefault(ref.node(), List.of()), ref.port());
        if (asOut.isPresent()) {
            return new Endpoint(node, asOut.get(), true);
        }
        Optional<PortDef> asIn = findPort(inputs.getOrDefault(ref.node(), List.of()), ref.port());
        if (asIn.isPresent()) {
            return new Endpoint(node, asIn.get(), false);
        }
        out.add(Diagnostic.error(GraphValidator.UNKNOWN_WIRE_ENDPOINT,
                "连线端点端口不存在：" + ref + "（节点类型 " + node.type() + "）", ref.node()));
        return null;
    }

    private static Optional<PortDef> findPort(List<PortDef> ports, String id) {
        return ports.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    /** 检查 11：资源引用节点的输出只能接规定目标。 */
    static void checkRefMisuse(Wire wire, Endpoint from, Endpoint to, List<Diagnostic> out) {
        String fromType = from.node().type();
        if (ASSEMBLY_ONLY_REFS.contains(fromType)) {
            // 变量引用声明通道（write:<名> 右侧输出）不参与装配用途判定
            if (NodeTypes.isVarRefPort(from.port().id())) {
                return;
            }
            // ref.animation/ref.ac：animate.entry.ref 或 entity.root 动画声明端口（v4）
            String declarationPort = NodeTypes.ENTITY_DECLARATION_PORTS.get(fromType);
            boolean ok = (to.node().type().equals("animate.entry") && to.port().id().equals("ref"))
                    || (declarationPort != null && to.node().type().equals("entity.root")
                            && to.port().id().equals(declarationPort));
            if (!ok) {
                out.add(Diagnostic.error(GraphValidator.REF_MISUSE,
                        fromType + " 的输出只能连接 animate.entry.ref / entity.root." + declarationPort
                                + "，实际连接 " + wire.to(), from.node().uid()));
            }
        } else if (fromType.equals("ref.particle")) {
            // ref.particle：particle.entry.ref 或 entity.root.particles（v10）
            boolean ok = (to.node().type().equals("particle.entry") && to.port().id().equals("ref"))
                    || (to.node().type().equals("entity.root") && to.port().id().equals("particles"));
            if (!ok) {
                out.add(Diagnostic.error(GraphValidator.REF_MISUSE,
                        "ref.particle 的输出只能连接 particle.entry.ref / entity.root.particles，实际连接 "
                                + wire.to(), from.node().uid()));
            }
        } else if (fromType.equals("ref.sound")) {
            // ref.sound：entity.root.sounds 或 ac.state.sounds（v10）
            boolean ok = (to.node().type().equals("entity.root") && to.port().id().equals("sounds"))
                    || (to.node().type().equals("ac.state") && to.port().id().equals("sounds"));
            if (!ok) {
                out.add(Diagnostic.error(GraphValidator.REF_MISUSE,
                        "ref.sound 的输出只能连接 entity.root.sounds / ac.state.sounds，实际连接 "
                                + wire.to(), from.node().uid()));
            }
        } else if (fromType.equals("ref.rc")) {
            // ref.rc：只能接 entity.root.render_controllers（v4）
            boolean ok = to.node().type().equals("entity.root")
                    && to.port().id().equals("render_controllers");
            if (!ok) {
                out.add(Diagnostic.error(GraphValidator.REF_MISUSE,
                        "ref.rc 的输出只能连接 entity.root.render_controllers，实际连接 " + wire.to(),
                        from.node().uid()));
            }
        } else if (fromType.equals("rc.root") && from.port().id().equals("controller")) {
            // rc.root.controller：只能接 entity.root.render_controllers（v4）
            boolean ok = to.node().type().equals("entity.root")
                    && to.port().id().equals("render_controllers");
            if (!ok) {
                out.add(Diagnostic.error(GraphValidator.REF_MISUSE,
                        "rc.root 的 controller 只能连接 entity.root.render_controllers，实际连接 " + wire.to(),
                        from.node().uid()));
            }
        } else if (VALUE_REFS.contains(fromType)) {
            // 声明端口类型（GEOMETRY_REF 等）本身是值类型，isValue 天然放行；接错类别由类型检查报
            if (!to.port().type().isValue()) {
                out.add(Diagnostic.error(GraphValidator.REF_MISUSE,
                        fromType + " 的输出只能连接表达式槽、material.entry.value / list.entry.value 或 RC 锚点的 "
                                + NodeTypes.RC_DECLARATION_PORTS.get(fromType)
                                + " 声明端口，实际连接 " + wire.to(), from.node().uid()));
            }
        }
    }
}

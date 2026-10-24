package io.github.tt432.eyelib.nodegraph;

import java.util.List;

/** 事件区块：event.initialize / event.pre_animation / event.parent_setup。 */
final class NodeTypesEvents {
    private NodeTypesEvents() {
    }

    /** event.initialize：ClientEntity scripts.initialize 的执行时机源节点。 */
    static final NodeType EVENT_INITIALIZE = NodeTypes.register(NodeType.of(
            "event.initialize", NodeType.Kind.EVENT, NodeTypes.CAT_EVENT,
            List.of(), List.of(), List.of(NodeTypePorts.execOut())));
    /** event.pre_animation：scripts.pre_animation 的执行时机源节点。 */
    static final NodeType EVENT_PRE_ANIMATION = NodeTypes.register(NodeType.of(
            "event.pre_animation", NodeType.Kind.EVENT, NodeTypes.CAT_EVENT,
            List.of(), List.of(), List.of(NodeTypePorts.execOut())));
    /** event.parent_setup：scripts.parent_setup 的执行时机源节点。 */
    static final NodeType EVENT_PARENT_SETUP = NodeTypes.register(NodeType.of(
            "event.parent_setup", NodeType.Kind.EVENT, NodeTypes.CAT_EVENT,
            List.of(), List.of(), List.of(NodeTypePorts.execOut())));
}

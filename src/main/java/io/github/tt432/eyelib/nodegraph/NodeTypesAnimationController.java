package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Optional;

/** AnimationController 区块：ac.root / ac.state / ac.transition / particle.entry。 */
final class NodeTypesAnimationController {
    private NodeTypesAnimationController() {
    }

    static final NodeType AC_ROOT = NodeTypes.register(NodeType.of(
            "ac.root", NodeType.Kind.AC_ROOT, NodeTypes.CAT_AC,
            List.of(
                    NodeOptionDef.asset("identifier", "controller.animation.example.main", "ac")),
            // v10：initial = 初始 state 的图边（替代 initial_state 字符串选项，规格 §2.1）
            List.of(NodeTypePorts.slotIn("states"),
                    new PortDef("initial", PortDirection.IN, PortType.SLOT, Optional.empty(), false)),
            List.of()));

    static final NodeType AC_STATE = NodeTypes.register(NodeType.of(
            "ac.state", NodeType.Kind.AC_STATE, NodeTypes.CAT_AC,
            List.of(
                    // name 仅内部使用（导出 states 键；编辑器 UI 行隐藏，同 short_name 处理）
                    NodeOptionDef.string("name", "default"),
                    NodeOptionDef.number("blend_transition", 0.2),
                    NodeOptionDef.bool("blend_via_shortest_path", false)),
            List.of(
                    NodeTypePorts.slotIn("animations"),
                    NodeTypePorts.slotIn("transitions"),
                    // v10：incoming = 指向本状态的 transition 图边；粒子/音效（规格 §2.1/2.3）
                    NodeTypePorts.slotIn("incoming"),
                    NodeTypePorts.slotIn("particles"),
                    PortDef.inMulti("sounds", PortType.SOUND_REF)),
            // v13：on_entry/on_exit 翻转为 EXEC OUT 时机源端口——链从 state 出发，
            // 与 event.* 节点同模型（规格 nodegraph-event-nodes）
            List.of(new PortDef("on_entry", PortDirection.OUT, PortType.EXEC, Optional.empty(), false),
                    new PortDef("on_exit", PortDirection.OUT, PortType.EXEC, Optional.empty(), false),
                    NodeTypePorts.slotOut("state"))));

    static final NodeType AC_TRANSITION = NodeTypes.register(NodeType.of(
            "ac.transition", NodeType.Kind.AC_TRANSITION, NodeTypes.CAT_AC,
            // v10：target 从字符串选项改为指向目标 state 的 SLOT 图边
            List.of(),
            List.of(PortDef.in("condition", PortType.FLOAT, new JsonPrimitive(1))),
            List.of(NodeTypePorts.slotOut("transition"),
                    new PortDef("target", PortDirection.OUT, PortType.SLOT, Optional.empty(), false))));

    /** particle.entry：AC state 的粒子效果条目（v10）。ref 槽接 ref.particle；
     * script = pre_effect_script（molang 值槽，未连线 = 不输出该字段）。 */
    static final NodeType PARTICLE_ENTRY = NodeTypes.register(NodeType.of(
            "particle.entry", NodeType.Kind.PARTICLE_ENTRY, NodeTypes.CAT_AC,
            List.of(
                    NodeOptionDef.string("locator", ""),
                    NodeOptionDef.bool("bind_to_actor", true)),
            List.of(
                    PortDef.in("ref", PortType.ANY),
                    PortDef.in("script", PortType.ANY)),
            List.of(NodeTypePorts.slotOut("entry"))));
}

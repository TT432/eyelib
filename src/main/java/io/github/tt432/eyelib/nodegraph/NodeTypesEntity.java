package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonPrimitive;
import java.util.List;

/** 实体装配区块：entity.root 与 animate.entry。 */
final class NodeTypesEntity {
    private NodeTypesEntity() {
    }

    static final NodeType ENTITY_ROOT = NodeTypes.register(NodeType.of(
            "entity.root", NodeType.Kind.ENTITY_ROOT, NodeTypes.CAT_ENTITY,
            List.of(NodeOptionDef.string("identifier", "example:my_entity"),
                    // BE 标准 description 字段建模（导入映射；构建仅在非缺省时输出）
                    NodeOptionDef.bool("enable_attachables", false),
                    NodeOptionDef.bool("held_item_ignores_lighting", false),
                    NodeOptionDef.bool("should_update_effects_offscreen", false),
                    NodeOptionDef.string("spawn_egg_texture", ""),
                    NodeOptionDef.integer("spawn_egg_texture_index", 0),
                    NodeOptionDef.string("spawn_egg_base_color", ""),
                    NodeOptionDef.string("spawn_egg_overlay_color", "")),
            List.of(
                    // v13：initialize/pre_animation/parent_setup 执行槽 → event.* 源节点
                    // （见 EVENT_SCRIPT_SLOTS；规格 nodegraph-event-nodes）
                    PortDef.in("scale", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.in("scale_x", PortType.FLOAT),
                    PortDef.in("scale_y", PortType.FLOAT),
                    PortDef.in("scale_z", PortType.FLOAT),
                    // 声明端口（v4：动画/AC 在实体级；geo/tex/mat 锚点在 RC，见 RC_DECLARATION_PORTS）
                    PortDef.inMulti("animations", PortType.ANIMATION_REF),
                    PortDef.inMulti("animation_controllers", PortType.AC_REF),
                    // v10：粒子/音效声明表（spec nodegraph-ac-graph-and-effects §2.2）
                    PortDef.inMulti("particles", PortType.PARTICLE_REF),
                    PortDef.inMulti("sounds", PortType.SOUND_REF),
                    NodeTypePorts.slotIn("animate"),
                    // v4：直连 rc.root.controller / ref.rc.ref（条件在源节点的 condition 端口）
                    PortDef.inMulti("render_controllers", PortType.RC_REF)),
            List.of()));

    static final NodeType ANIMATE_ENTRY = NodeTypes.register(NodeType.of(
            "animate.entry", NodeType.Kind.ANIMATE_ENTRY, NodeTypes.CAT_ENTITY,
            List.of(),
            List.of(
                    PortDef.in("ref", PortType.ANY),
                    PortDef.in("weight", PortType.FLOAT, new JsonPrimitive(1))),
            List.of(NodeTypePorts.slotOut("entry"))));
}

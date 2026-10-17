package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonPrimitive;
import java.util.List;

/** RenderController 区块：rc.root 与 list/material/part_visibility 条目节点。 */
final class NodeTypesRenderController {
    private NodeTypesRenderController() {
    }

    static final NodeType RC_ROOT = NodeTypes.register(NodeType.of(
            "rc.root", NodeType.Kind.RC_ROOT, NodeTypes.CAT_RC,
            List.of(
                    NodeOptionDef.string("identifier", "controller.render.example"),
                    NodeOptionDef.bool("ignore_lighting", false),
                    NodeOptionDef.of("arrays", NodeOptionDef.OptionType.TEXT, new JsonPrimitive(""))),
            List.of(
                    // v4：内联进实体画布时的挂载条件（规格 §2.1）；独立 RC 库中闲置
                    // v6：decl_* 已删——geo/tex/mat 引用集由下方 geometry/textures/materials
                    // 三端口的连线决定（规格 nodegraph-ordered-entries-and-rc-reference-set §3.1）
                    PortDef.in("condition", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.in("geometry", PortType.STRING, new JsonPrimitive("geometry.default")),
                    NodeTypePorts.slotIn("textures"),
                    NodeTypePorts.slotIn("materials"),
                    NodeTypePorts.slotIn("part_visibility"),
                    PortDef.in("color", PortType.COLOR),
                    PortDef.in("is_hurt_color", PortType.COLOR),
                    PortDef.in("on_fire_color", PortType.COLOR),
                    PortDef.in("overlay_color", PortType.COLOR)),
            // v4：接 entity.root.render_controllers = 本实体定义并挂载该 RC
            List.of(PortDef.out("controller", PortType.RC_REF))));

    static final NodeType LIST_ENTRY = NodeTypes.register(NodeType.of(
            "list.entry", NodeType.Kind.LIST_ENTRY, NodeTypes.CAT_RC,
            List.of(),
            // v6：next 链（单连接 SLOT）——数组顺序 = 链序（规格 §2.1）
            List.of(PortDef.in("value", PortType.ANY),
                    PortDef.in("next", PortType.SLOT)),
            List.of(NodeTypePorts.slotOut("entry"))));

    static final NodeType MATERIAL_ENTRY = NodeTypes.register(NodeType.of(
            "material.entry", NodeType.Kind.MATERIAL_ENTRY, NodeTypes.CAT_RC,
            List.of(NodeOptionDef.string("pattern", "*")),
            List.of(PortDef.in("value", PortType.ANY),
                    PortDef.in("next", PortType.SLOT)),
            List.of(NodeTypePorts.slotOut("entry"))));

    static final NodeType PART_VISIBILITY_ENTRY = NodeTypes.register(NodeType.of(
            "part_visibility.entry", NodeType.Kind.PART_VISIBILITY_ENTRY, NodeTypes.CAT_RC,
            List.of(NodeOptionDef.string("bone_pattern", "*")),
            List.of(PortDef.in("condition", PortType.BOOL, new JsonPrimitive(1)),
                    PortDef.in("next", PortType.SLOT)),
            List.of(NodeTypePorts.slotOut("entry"))));
}

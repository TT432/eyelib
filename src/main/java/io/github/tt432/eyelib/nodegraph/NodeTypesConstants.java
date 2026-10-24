package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonPrimitive;
import java.util.List;

/** 常量区块：const.number/int/bool/string/color 与 color.compose。 */
final class NodeTypesConstants {
    private NodeTypesConstants() {
    }

    static final NodeType CONST_NUMBER = NodeTypes.register(NodeType.of(
            "const.number", NodeType.Kind.CONST_NUMBER, NodeTypes.CAT_CONSTANT,
            List.of(NodeOptionDef.number("value", 0)),
            List.of(),
            List.of(PortDef.out("out", PortType.FLOAT))));

    static final NodeType CONST_INT = NodeTypes.register(NodeType.of(
            "const.int", NodeType.Kind.CONST_INT, NodeTypes.CAT_CONSTANT,
            List.of(NodeOptionDef.integer("value", 0)),
            List.of(),
            List.of(PortDef.out("out", PortType.INT))));

    static final NodeType CONST_BOOL = NodeTypes.register(NodeType.of(
            "const.bool", NodeType.Kind.CONST_BOOL, NodeTypes.CAT_CONSTANT,
            List.of(NodeOptionDef.bool("value", false)),
            List.of(),
            List.of(PortDef.out("out", PortType.BOOL))));

    static final NodeType CONST_STRING = NodeTypes.register(NodeType.of(
            "const.string", NodeType.Kind.CONST_STRING, NodeTypes.CAT_CONSTANT,
            List.of(NodeOptionDef.string("value", "")),
            List.of(),
            List.of(PortDef.out("out", PortType.STRING))));

    /** const.color：取色器选择的 RGBA 常量（选项 #AARRGGBB，8bit/通道）。 */
    static final NodeType CONST_COLOR = NodeTypes.register(NodeType.of(
            "const.color", NodeType.Kind.CONST_COLOR, NodeTypes.CAT_CONSTANT,
            List.of(NodeOptionDef.color("value", ColorValues.WHITE)),
            List.of(),
            List.of(PortDef.out("out", PortType.COLOR))));

    /** color.compose：四通道（各默认 1）合成颜色；通道可接任意 float 表达式（动态颜色）。 */
    static final NodeType COLOR_COMPOSE = NodeTypes.register(NodeType.of(
            "color.compose", NodeType.Kind.COLOR_COMPOSE, NodeTypes.CAT_OPERATOR,
            List.of(),
            List.of(PortDef.in("r", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.in("g", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.in("b", PortType.FLOAT, new JsonPrimitive(1)),
                    PortDef.in("a", PortType.FLOAT, new JsonPrimitive(1))),
            List.of(PortDef.out("out", PortType.COLOR))));
}

//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphInterface;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.Placemat;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.StickyNote;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link EvmGraphMapper} 单测：GraphData ↔ 中间模型的双向规则
 * （纯翻译，不启 MC、不触碰 LDLib 类型）。
 */
class EvmGraphMapperTest {

    private static NodeInstance node(String uid, String type, float x, float y,
                                     Map<String, JsonElement> options, Map<String, JsonElement> constants) {
        return new NodeInstance(uid, type, x, y, options, constants);
    }

    private static Map<String, JsonElement> opts(Object... kv) {
        Map<String, JsonElement> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            JsonElement e = v instanceof String s ? new JsonPrimitive(s)
                    : v instanceof Number n ? new JsonPrimitive(n)
                    : v instanceof Boolean b ? new JsonPrimitive(b)
                    : (JsonElement) v;
            map.put((String) kv[i], e);
        }
        return map;
    }

    private static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    // ---------- 往返恒等 ----------

    @Test
    void roundTripPreservesNodesWiresVariablesAndDocumentOnlyParts() {
        GraphInterface iface = new GraphInterface(
                List.of(GraphInterface.Param.of("x", PortType.FLOAT)),
                GraphInterface.Param.of("result", PortType.FLOAT));
        GraphData source = new GraphData(
                List.of(
                        node("c1", "const.number", 10, 20, opts("value", 2), Map.of()),
                        node("n", "op.binary", 30, 40, opts("op", "+"), opts("b", 5)),
                        node("sc", "subgraph.call", 50, 60, opts("subgraph", "sg"), Map.of())),
                List.of(wire("c1", "out", "n", "a"), wire("n", "out", "sc", "x")),
                List.of(
                        VariableDecl.of("foo", PortType.FLOAT),
                        new VariableDecl("c", PortType.BOOL, Optional.of("a/b"),
                                Optional.of(new JsonPrimitive(true)))),
                List.of(new Placemat("g1", "title", "#FF00FF", List.of("c1"))),
                List.of(new StickyNote("note1", "hello", 1, 2, 200, 100, "#FFFF88")),
                Optional.of(iface));

        GraphData roundTripped = EvmGraphMapper.toData(EvmGraphMapper.toModel(source), source);

        assertEquals(source, roundTripped);
    }

    // ---------- 变量标识符规则（D6 平铺全路径） ----------

    @Test
    void variableIdentifierFlatteningIsBijective() {
        assertEquals("foo", EvmGraphMapper.flatten(VariableDecl.of("foo", PortType.FLOAT)));
        assertEquals("a/b/c", EvmGraphMapper.flatten(
                new VariableDecl("c", PortType.FLOAT, Optional.of("a/b"), Optional.empty())));

        assertEquals("foo", EvmGraphMapper.variableName("foo"));
        assertEquals(Optional.empty(), EvmGraphMapper.variableGroup("foo"));
        assertEquals("c", EvmGraphMapper.variableName("a/b/c"));
        assertEquals(Optional.of("a/b"), EvmGraphMapper.variableGroup("a/b/c"));
    }

    // ---------- ParameterNode → variable ----------

    @Test
    void parameterNodeBecomesVariable() {
        EvmGraphModel model = new EvmGraphModel(
                List.of(EvmGraphModel.EvmNodeModel.parameter("guid-1", "a/b/foo", 7, 8)),
                List.of(new EvmGraphModel.EvmEdgeModel("guid-1", "out", "n1", "a")),
                List.of(new EvmGraphModel.EvmParamModel("a/b/foo", PortType.FLOAT, Optional.empty())));

        GraphData data = EvmGraphMapper.toData(model, GraphData.empty());

        NodeInstance varGet = data.nodes().get(0);
        assertEquals(NodeTypes.VARIABLE.id(), varGet.type());
        assertEquals("guid-1", varGet.uid());
        assertEquals(7, varGet.x());
        assertEquals(new JsonPrimitive("foo"), varGet.options().get("name"));

        // 黑板变量还原出分组
        assertEquals(List.of(new VariableDecl("foo", PortType.FLOAT, Optional.of("a/b"), Optional.empty())),
                data.variables());

        // 连线端口 id 直通
        assertEquals(List.of(wire("guid-1", "out", "n1", "a")), data.wires());
    }

    @Test
    void variableEvmNodeRoundTripsAsEvmNotParameter() {
        GraphData source = new GraphData(
                List.of(node("v", "variable", 1, 2, opts("name", "foo"), Map.of())),
                List.of(), List.of(VariableDecl.of("foo", PortType.FLOAT)),
                List.of(), List.of(), Optional.empty());

        EvmGraphModel model = EvmGraphMapper.toModel(source);

        assertEquals(EvmGraphModel.EvmNodeModel.Kind.EVM, model.nodes().get(0).kind());
        assertEquals(source, EvmGraphMapper.toData(model, source));
    }

    // ---------- 文档专属部分从 previous 保留（D6 降级） ----------

    @Test
    void documentOnlyPartsComeFromPreviousNotModel() {
        GraphData previous = new GraphData(
                List.of(), List.of(), List.of(),
                List.of(new Placemat("g", "t", "#808080", List.of())),
                List.of(new StickyNote("s", "text", 3, 4, 200, 100, "#FFFF88")),
                Optional.of(new GraphInterface(List.of(), GraphInterface.Param.of("r", PortType.ANY))));

        GraphData data = EvmGraphMapper.toData(new EvmGraphModel(List.of(), List.of(), List.of()), previous);

        assertEquals(previous.placemats(), data.placemats());
        assertEquals(previous.stickyNotes(), data.stickyNotes());
        assertEquals(previous.graphInterface(), data.graphInterface());
        assertTrue(data.nodes().isEmpty());
    }
}
//?}

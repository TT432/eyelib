package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if !legacy {
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphInterface;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.Placemat;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.StickyNote;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@link EvmGraphTranslator} 双向翻译单测（零 MC 引导：只触达 GraphModel 建模 API，
 * 不走 NBT 序列化 / RegistryAccess）。
 *
 * <p>uid 映射：载入时 domain uid 经 {@link EvmGraphTranslator#uidOf} 确定性映射，
 * 保存时取 {@code model.getUid().toString()}——因此比较以「类型+坐标」为节点键，
 * 连线端点用 uidOf 换算期望值。
 */
class EvmGraphTranslatorTest {

    private static Map<String, JsonElement> opts(Object... kv) {
        Map<String, JsonElement> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            map.put((String) kv[i], v instanceof String s ? new JsonPrimitive(s)
                    : v instanceof Number n ? new JsonPrimitive(n)
                    : v instanceof Boolean b ? new JsonPrimitive(b)
                    : (JsonElement) v);
        }
        return map;
    }

    private static GraphLibrary sampleLibrary() {
        List<NodeInstance> mainNodes = List.of(
                new NodeInstance("n1", "const.number", 10, 20, opts("value", 3.5), Map.of()),
                new NodeInstance("n2", "math.call", 200, 20,
                        opts("function", "math.sin", "arg_count", 1), Map.of()),
                new NodeInstance("n3", "variable", -500, 400, opts("name", "foo"), Map.of()),
                new NodeInstance("set1", "exec.set_var", -200, 600, Map.of(), Map.of()),
                new NodeInstance("root_node", "entity.root", 1000, 500,
                        opts("identifier", "example:my_entity"), Map.of()),
                new NodeInstance("call1", "subgraph.call", 500, 20, opts("subgraph", "double"), Map.of()));
        List<Wire> mainWires = List.of(
                new Wire(new PortRef("n1", "out"), new PortRef("n2", "arg1")),
                new Wire(new PortRef("n2", "out"), new PortRef("root_node", "scale")),
                new Wire(new PortRef("n1", "out"), new PortRef("call1", "x")),
                new Wire(new PortRef("call1", "result"), new PortRef("root_node", "scale_y")),
                new Wire(new PortRef("n3", "out"), new PortRef("set1", "target")));
        GraphData main = new GraphData(
                mainNodes,
                mainWires,
                List.of(new VariableDecl("foo", PortType.FLOAT,
                        Optional.of("a/b"), Optional.of(new JsonPrimitive(1.5)))),
                List.of(new Placemat("p1", "Group A", "#FF00FF00", List.of("n1", "n2"))),
                List.of(new StickyNote("s1", "hello", 0, 0, 200, 100, "#FFFFFF00")),
                Optional.empty());

        GraphInterface iface = new GraphInterface(
                List.of(new GraphInterface.Param("x", PortType.FLOAT, Optional.of(new JsonPrimitive(0)))),
                GraphInterface.Param.of("result", PortType.FLOAT));
        GraphData sub = new GraphData(
                List.of(
                        NodeInstance.of("in_anchor", "subgraph.input", 0, 0),
                        new NodeInstance("mul", "math.call", 200, 0,
                                opts("function", "math.pow", "arg_count", 2),
                                Map.of("arg2", new JsonPrimitive(2))),
                        NodeInstance.of("out_anchor", "subgraph.output", 400, 0)),
                List.of(
                        new Wire(new PortRef("in_anchor", "x"), new PortRef("mul", "arg1")),
                        new Wire(new PortRef("mul", "out"), new PortRef("out_anchor", "result"))),
                List.of(), List.of(), List.of(),
                Optional.of(iface));

        Map<String, GraphData> graphs = new LinkedHashMap<>();
        graphs.put("root", main);
        graphs.put("double", sub);
        return new GraphLibrary(GraphLibrary.CURRENT_FORMAT_VERSION, GraphKind.CLIENT_ENTITY, "root", graphs);
    }

    @Test
    void paletteCoversEveryDomainNodeType() {
        assertEquals(NodeTypes.all().size(), EvmNodes.ALL.size(),
                "every domain node type needs exactly one LDLib2 node class");
        for (NodeType type : NodeTypes.all()) {
            assertNotNull(EvmNodes.classOf(type.id()), "missing LDLib2 node class for " + type.id());
        }
    }

    @Test
    void roundTripPreservesStructure() {
        GraphLibrary original = sampleLibrary();
        List<Diagnostic> diags = new ArrayList<>();

        EvmGraph graph = EvmGraphTranslator.toGraph(original, diags);
        GraphLibrary restored = EvmGraphTranslator.toLibrary(graph, diags);

        assertEquals(Set.of("root", "double"), restored.graphs().keySet());
        assertEquals(original.kind(), restored.kind());
        assertEquals(original.main(), restored.main());

        assertGraphEquals(original.mainGraph(), restored.mainGraph(), original, restored, "root");
        assertGraphEquals(original.graphs().get("double"), restored.graphs().get("double"),
                original, restored, "double");

        GraphInterface restoredIface = restored.graphs().get("double").graphInterface().orElseThrow();
        assertEquals(1, restoredIface.inputs().size());
        assertEquals("x", restoredIface.inputs().get(0).name());
        assertEquals(PortType.FLOAT, restoredIface.inputs().get(0).type());
        assertEquals("result", restoredIface.output().name());
        assertEquals(PortType.FLOAT, restoredIface.output().type());
    }

    @Test
    void forwardModelHasDynamicPortsAndSubgraphPorts() {
        GraphLibrary original = sampleLibrary();
        EvmGraph graph = EvmGraphTranslator.toGraph(original, new ArrayList<>());

        Map<String, NodeModel> nodesByUid = new HashMap<>();
        for (var nm : graph.graphModel.getNodeModels()) {
            if (nm instanceof NodeModel model) {
                nodesByUid.put(model.getUid().toString(), model);
            }
        }
        NodeModel n2 = nodesByUid.get(EvmGraphTranslator.uidOf("n2").toString());
        assertNotNull(n2, "math.call node present");
        assertNotNull(n2.getInputsById().get("arg1"), "dynamic arg1 port from arg_count=1");

        NodeModel n3 = nodesByUid.get(EvmGraphTranslator.uidOf("n3").toString());
        assertNotNull(n3, "variable node present");
        assertTrue(n3 instanceof VariableNodeModel, "variable node binds blackboard declaration");

        NodeModel call1 = nodesByUid.get(EvmGraphTranslator.uidOf("call1").toString());
        assertNotNull(call1, "subgraph.call node present");
        assertTrue(call1 instanceof SubgraphNodeModel, "subgraph.call maps to native SubgraphNodeModel");
        assertEquals(1, call1.getInputsById().size(), "one mirrored input (x)");
        assertEquals(1, call1.getOutputsById().size(), "one mirrored output (result)");

        assertEquals(5, graph.graphModel.getWireModels().size(), "all main wires created");

        var sub = graph.graphModel.getLocalSubGraphs().get(0);
        Map<String, NodeModel> subNodes = new HashMap<>();
        for (var nm : sub.getNodeModels()) {
            if (nm instanceof NodeModel model) {
                subNodes.put(model.getUid().toString(), model);
            }
        }
        NodeModel mul = subNodes.get(EvmGraphTranslator.uidOf("mul").toString());
        assertNotNull(mul);
        assertNotNull(mul.getInputsById().get("arg2"), "arg_count=2 gives arg2");
        NodeModel inAnchor = subNodes.get(EvmGraphTranslator.uidOf("in_anchor").toString());
        assertNotNull(inAnchor);
        assertNotNull(inAnchor.getOutputsById().get("x"), "subgraph.input mirrors interface input x");
        NodeModel outAnchor = subNodes.get(EvmGraphTranslator.uidOf("out_anchor").toString());
        assertNotNull(outAnchor);
        assertNotNull(outAnchor.getInputsById().get("result"), "subgraph.output takes result input");
        assertEquals(2, sub.getWireModels().size(), "all subgraph wires created");
    }

    // ---------- 比较辅助 ----------

    private static void assertGraphEquals(GraphData expected, GraphData actual,
                                          GraphLibrary expectedLib, GraphLibrary actualLib, String graphName) {
        assertEquals(expected.nodes().size(), actual.nodes().size(), graphName + " node count");
        assertEquals(expected.wires().size(), actual.wires().size(), graphName + " wire count");

        Map<String, NodeInstance> actualByKey = new HashMap<>();
        for (NodeInstance n : actual.nodes()) {
            actualByKey.put(nodeKey(n), n);
        }
        NodeType.SubgraphResolver expectedResolver = NodeType.SubgraphResolver.of(
                expectedLib, expected.graphInterface());
        NodeType.SubgraphResolver actualResolver = NodeType.SubgraphResolver.of(
                actualLib, actual.graphInterface());
        for (NodeInstance exp : expected.nodes()) {
            NodeInstance act = actualByKey.get(nodeKey(exp));
            assertNotNull(act, graphName + " missing node " + nodeKey(exp));
            assertEquals(EvmGraphTranslator.uidOf(exp.uid()).toString(), act.uid(),
                    graphName + " uid mapping for " + exp.uid());
            assertOptionsEqual(exp, act, graphName);
            assertConstantsEqual(exp, act, expectedResolver, actualResolver, graphName);
        }

        Set<String> expectedWires = new TreeSet<>();
        for (Wire w : expected.wires()) {
            expectedWires.add(wireKey(new Wire(
                    new PortRef(EvmGraphTranslator.uidOf(w.from().node()).toString(), w.from().port()),
                    new PortRef(EvmGraphTranslator.uidOf(w.to().node()).toString(), w.to().port()))));
        }
        Set<String> actualWires = new TreeSet<>();
        for (Wire w : actual.wires()) {
            actualWires.add(wireKey(w));
        }
        assertEquals(expectedWires, actualWires, graphName + " wires");

        assertEquals(expected.variables().size(), actual.variables().size(), graphName + " variable count");
        for (VariableDecl expVar : expected.variables()) {
            VariableDecl actVar = actual.variables().stream()
                    .filter(v -> v.name().equals(expVar.name())).findFirst().orElse(null);
            assertNotNull(actVar, graphName + " missing variable " + expVar.name());
            assertEquals(expVar.type(), actVar.type(), graphName + " variable type " + expVar.name());
            assertEquals(expVar.group(), actVar.group(), graphName + " variable group " + expVar.name());
            assertJsonEquals(expVar.defaultValue().orElse(null), actVar.defaultValue().orElse(null),
                    graphName + " variable default " + expVar.name());
        }

        assertEquals(expected.placemats().size(), actual.placemats().size(), graphName + " placemat count");
        for (Placemat expP : expected.placemats()) {
            Placemat actP = actual.placemats().stream()
                    .filter(p -> p.title().equals(expP.title())).findFirst().orElse(null);
            assertNotNull(actP, graphName + " missing placemat " + expP.title());
            Set<String> expMembers = new HashSet<>();
            expP.nodeUids().forEach(u -> expMembers.add(EvmGraphTranslator.uidOf(u).toString()));
            assertEquals(expMembers, new HashSet<>(actP.nodeUids()), graphName + " placemat members");
            assertEquals(EvmGraphTranslator.parseColor(expP.color(), 0),
                    EvmGraphTranslator.parseColor(actP.color(), 0), graphName + " placemat color");
        }

        assertEquals(expected.stickyNotes().size(), actual.stickyNotes().size(), graphName + " sticky count");
        for (StickyNote expS : expected.stickyNotes()) {
            StickyNote actS = actual.stickyNotes().stream()
                    .filter(s -> s.text().equals(expS.text())).findFirst().orElse(null);
            assertNotNull(actS, graphName + " missing sticky " + expS.text());
            assertEquals(expS.width(), actS.width(), graphName + " sticky width");
            assertEquals(expS.height(), actS.height(), graphName + " sticky height");
            assertEquals(EvmGraphTranslator.parseColor(expS.color(), 0),
                    EvmGraphTranslator.parseColor(actS.color(), 0), graphName + " sticky color");
        }

        assertEquals(expected.graphInterface().isPresent(), actual.graphInterface().isPresent(),
                graphName + " interface presence");
    }

    private static String nodeKey(NodeInstance n) {
        return n.type() + "|" + n.x() + "|" + n.y();
    }

    private static String wireKey(Wire w) {
        return w.from() + "=>" + w.to();
    }

    private static void assertOptionsEqual(NodeInstance exp, NodeInstance act, String graphName) {
        NodeType type = NodeTypes.require(exp.type());
        for (var def : type.options()) {
            JsonElement e = exp.option(def.id(), type).orElse(null);
            JsonElement a = act.option(def.id(), type).orElse(null);
            assertJsonEquals(e, a, graphName + " option " + exp.type() + "." + def.id());
        }
    }

    private static void assertConstantsEqual(NodeInstance exp, NodeInstance act,
                                             NodeType.SubgraphResolver expR, NodeType.SubgraphResolver actR,
                                             String graphName) {
        NodeType type = NodeTypes.require(exp.type());
        List<PortDef> expInputs = type.inputsOf(exp, expR);
        List<PortDef> actInputs = type.inputsOf(act, actR);
        assertEquals(expInputs.stream().map(PortDef::id).sorted().toList(),
                actInputs.stream().map(PortDef::id).sorted().toList(),
                graphName + " input port ids of " + exp.type());
        for (PortDef def : expInputs) {
            JsonElement e = exp.constants().getOrDefault(def.id(), def.defaultValue().orElse(null));
            JsonElement a = act.constants().getOrDefault(def.id(), def.defaultValue().orElse(null));
            assertJsonEquals(e, a, graphName + " constant " + exp.type() + "." + def.id());
        }
    }

    private static void assertJsonEquals(JsonElement e, JsonElement a, String what) {
        if (e == null || a == null) {
            assertEquals(e, a, what);
            return;
        }
        if (e.isJsonPrimitive() && a.isJsonPrimitive()
                && e.getAsJsonPrimitive().isNumber() && a.getAsJsonPrimitive().isNumber()) {
            assertEquals(e.getAsDouble(), a.getAsDouble(), 1e-6, what);
            return;
        }
        assertEquals(e, a, what);
    }
}
//?}

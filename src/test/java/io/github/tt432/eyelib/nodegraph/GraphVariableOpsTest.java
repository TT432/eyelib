package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link GraphVariableOps}：变量面板的图改写契约（规格 §3.2）——
 * 增删改是纯函数；重命名级联改写图内 variable 节点。
 */
class GraphVariableOpsTest {

    private static GraphData graph(List<NodeInstance> nodes, List<VariableDecl> variables) {
        return new GraphData(nodes, List.of(), variables, List.of(), List.of(), Optional.empty());
    }

    private static NodeInstance varNode(String uid, String name) {
        return new NodeInstance(uid, "variable", 0, 0,
                Map.of("name", new JsonPrimitive(name)), Map.of());
    }

    @Test
    void upsertAddsAndReplaces() {
        GraphData g = GraphVariableOps.upsert(graph(List.of(), List.of()),
                VariableDecl.of("a", PortType.FLOAT));
        assertEquals(1, g.variables().size());
        g = GraphVariableOps.upsert(g, VariableDecl.of("a", PortType.INT));
        assertEquals(1, g.variables().size());
        assertEquals(PortType.INT, g.variables().get(0).type());
    }

    @Test
    void removeDropsDeclOnly() {
        GraphData g = graph(List.of(varNode("v", "a")), List.of(VariableDecl.of("a", PortType.FLOAT)));
        g = GraphVariableOps.remove(g, "a");
        assertTrue(g.variables().isEmpty());
        // variable 节点保留（验证器报 UNDECLARED_VARIABLE）
        assertEquals(1, g.nodes().size());
    }

    @Test
    void renameCascadesToVariableNodes() {
        GraphData g = graph(
                List.of(varNode("v1", "old"), varNode("v2", "old"), varNode("v3", "other")),
                List.of(VariableDecl.of("old", PortType.FLOAT), VariableDecl.of("other", PortType.BOOL)));
        g = GraphVariableOps.rename(g, "old", "new");
        assertEquals("new", g.variables().get(0).name());
        assertEquals(PortType.FLOAT, g.variables().get(0).type());
        assertEquals("new", g.nodes().get(0).options().get("name").getAsString());
        assertEquals("new", g.nodes().get(1).options().get("name").getAsString());
        assertEquals("other", g.nodes().get(2).options().get("name").getAsString());
    }

    @Test
    void retypeKeepsGroupAndDefault() {
        JsonElement def = new JsonPrimitive(3);
        GraphData g = graph(List.of(), List.of(
                new VariableDecl("a", PortType.FLOAT, Optional.of("grp"), Optional.of(def))));
        g = GraphVariableOps.retype(g, "a", PortType.INT);
        VariableDecl decl = g.variables().get(0);
        assertEquals(PortType.INT, decl.type());
        assertEquals(Optional.of("grp"), decl.group());
        assertEquals(Optional.of(def), decl.defaultValue());
    }

    @Test
    void findReturnsDecl() {
        GraphData g = graph(List.of(), List.of(VariableDecl.of("a", PortType.FLOAT)));
        assertTrue(GraphVariableOps.find(g, "a").isPresent());
        assertTrue(GraphVariableOps.find(g, "b").isEmpty());
    }
}

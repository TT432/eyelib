package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link VariableDecl.Scope} 域层契约（规格 nodegraph-variable-table）：
 * codec 兼容扩展（缺省 VARIABLE）、GraphVariableOps 保 scope、setScope、
 * 验证器 TEMP_NEVER_WRITTEN。
 */
class VariableScopeTest {

    private static GraphData graph(List<NodeInstance> nodes, List<Wire> wires, List<VariableDecl> variables) {
        return new GraphData(nodes, wires, variables, List.of(), List.of(), Optional.empty());
    }

    private static NodeInstance varNode(String uid, String name) {
        return new NodeInstance(uid, "variable", 0, 0,
                Map.of("name", new JsonPrimitive(name)), Map.of());
    }

    // ---------- codec ----------

    @Test
    void codecRoundTripsScope() {
        VariableDecl decl = new VariableDecl("hp", PortType.FLOAT, Optional.empty(),
                Optional.of(new JsonPrimitive(3)), VariableDecl.Scope.TEMP);
        JsonElement json = TestCodecUtil.unwrap(VariableDecl.CODEC.encodeStart(JsonOps.INSTANCE, decl));
        assertEquals("temp", json.getAsJsonObject().get("scope").getAsString());
        VariableDecl back = TestCodecUtil.unwrap(VariableDecl.CODEC.parse(JsonOps.INSTANCE, json));
        assertEquals(VariableDecl.Scope.TEMP, back.scope());
        assertEquals("hp", back.name());
    }

    @Test
    void codecDefaultsToVariableScope() {
        JsonObject json = new JsonObject();
        json.addProperty("name", "hp");
        json.addProperty("type", "float");
        VariableDecl back = TestCodecUtil.unwrap(VariableDecl.CODEC.parse(JsonOps.INSTANCE, json));
        assertEquals(VariableDecl.Scope.VARIABLE, back.scope());
    }

    // ---------- GraphVariableOps ----------

    @Test
    void renameAndRetypePreserveScope() {
        GraphData g = graph(List.of(varNode("v", "a")), List.of(),
                List.of(new VariableDecl("a", PortType.FLOAT, Optional.empty(), Optional.empty(),
                        VariableDecl.Scope.TEMP)));
        g = GraphVariableOps.rename(g, "a", "b");
        assertEquals(VariableDecl.Scope.TEMP, g.variables().get(0).scope());
        g = GraphVariableOps.retype(g, "b", PortType.INT);
        assertEquals(VariableDecl.Scope.TEMP, g.variables().get(0).scope());
    }

    @Test
    void setScopeChangesOnlyScope() {
        GraphData g = graph(List.of(), List.of(),
                List.of(new VariableDecl("a", PortType.FLOAT, Optional.of("grp"),
                        Optional.of(new JsonPrimitive(1)), VariableDecl.Scope.VARIABLE)));
        g = GraphVariableOps.setScope(g, "a", VariableDecl.Scope.TEMP);
        VariableDecl v = g.variables().get(0);
        assertEquals(VariableDecl.Scope.TEMP, v.scope());
        assertEquals(PortType.FLOAT, v.type());
        assertEquals(Optional.of("grp"), v.group());
        assertEquals(1, v.defaultValue().orElseThrow().getAsInt());
    }

    // ---------- 验证器 TEMP_NEVER_WRITTEN ----------

    private static GraphLibrary lib(GraphData main) {
        return new GraphLibrary(6, GraphKind.CLIENT_ENTITY, "root", Map.of("root", main));
    }

    private static boolean hasDiagnostic(GraphLibrary lib, String code) {
        return GraphValidator.validateGraph(lib, "root", lib.graphs().get("root")).stream()
                .anyMatch(d -> d.code().equals(code));
    }

    @Test
    void tempReadWithoutWriteWarns() {
        GraphData main = graph(
                List.of(new NodeInstance("root", "entity.root", 0, 0, Map.of(), Map.of()),
                        varNode("v", "scratch")),
                List.of(new Wire(new PortRef("v", "out"), new PortRef("root", "scale"))),
                List.of(new VariableDecl("scratch", PortType.FLOAT, Optional.empty(),
                        Optional.empty(), VariableDecl.Scope.TEMP)));
        assertTrue(hasDiagnostic(lib(main), GraphValidator.TEMP_NEVER_WRITTEN));
    }

    @Test
    void tempReadWithWriteIsClean() {
        GraphData main = graph(
                List.of(new NodeInstance("root", "entity.root", 0, 0, Map.of(), Map.of()),
                        varNode("vw", "scratch"),
                        varNode("vr", "scratch"),
                        new NodeInstance("s", "exec.set_var", 0, 0, Map.of(), Map.of())),
                List.of(new Wire(new PortRef("vw", "out"), new PortRef("s", "target")),
                        new Wire(new PortRef("s", "exec_out"), new PortRef("root", "initialize")),
                        new Wire(new PortRef("vr", "out"), new PortRef("root", "scale"))),
                List.of(new VariableDecl("scratch", PortType.FLOAT, Optional.empty(),
                        Optional.empty(), VariableDecl.Scope.TEMP)));
        assertFalse(hasDiagnostic(lib(main), GraphValidator.TEMP_NEVER_WRITTEN));
    }

    @Test
    void variableScopeNeverWarnsTemp() {
        GraphData main = graph(
                List.of(new NodeInstance("root", "entity.root", 0, 0, Map.of(), Map.of()),
                        varNode("v", "hp")),
                List.of(new Wire(new PortRef("v", "out"), new PortRef("root", "scale"))),
                List.of(VariableDecl.of("hp", PortType.FLOAT)));
        assertFalse(hasDiagnostic(lib(main), GraphValidator.TEMP_NEVER_WRITTEN));
    }
}

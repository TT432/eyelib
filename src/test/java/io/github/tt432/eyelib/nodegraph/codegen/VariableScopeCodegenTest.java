package io.github.tt432.eyelib.nodegraph.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 变量 molang 作用域的 codegen 契约（规格 nodegraph-variable-table §2.2）：
 * variable 节点按黑板声明的 scope 选根（TEMP → temp.*），未声明回落 variable.*；
 * exec.set_var 写身份走同一路径。
 */
class VariableScopeCodegenTest {

    private static NodeInstance varNode(String uid, String name) {
        return new NodeInstance(uid, "variable", 0, 0,
                Map.of("name", new JsonPrimitive(name)), Map.of());
    }

    private static GraphLibrary lib(GraphData main) {
        return new GraphLibrary(6, GraphKind.CLIENT_ENTITY, "root", Map.of("root", main));
    }

    private static String scaleExpr(GraphLibrary lib) {
        CodegenResult r = new MolangGenerator(lib).emitExpressionFor("root", "root", "scale");
        assertFalse(r.hasErrors(), () -> "unexpected errors: " + r.diagnostics());
        return r.code();
    }

    private static GraphData graphWithDecl(NodeInstance extraRoot, List<NodeInstance> nodes,
                                           List<Wire> wires, List<VariableDecl> variables) {
        java.util.List<NodeInstance> all = new java.util.ArrayList<>(nodes);
        all.add(extraRoot);
        return new GraphData(all, wires, variables, List.of(), List.of(), Optional.empty());
    }

    @Test
    void tempScopeEmitsTempRoot() {
        GraphData main = graphWithDecl(
                new NodeInstance("root", "entity.root", 0, 0, Map.of(), Map.of()),
                List.of(varNode("v", "scratch")),
                List.of(new Wire(new PortRef("v", "out"), new PortRef("root", "scale"))),
                List.of(new VariableDecl("scratch", PortType.FLOAT, Optional.empty(),
                        Optional.empty(), VariableDecl.Scope.TEMP)));
        assertEquals("temp.scratch", scaleExpr(lib(main)));
    }

    @Test
    void variableScopeEmitsVariableRoot() {
        GraphData main = graphWithDecl(
                new NodeInstance("root", "entity.root", 0, 0, Map.of(), Map.of()),
                List.of(varNode("v", "hp")),
                List.of(new Wire(new PortRef("v", "out"), new PortRef("root", "scale"))),
                List.of(VariableDecl.of("hp", PortType.FLOAT)));
        assertEquals("variable.hp", scaleExpr(lib(main)));
    }

    @Test
    void undeclaredFallsBackToVariableRoot() {
        GraphData main = graphWithDecl(
                new NodeInstance("root", "entity.root", 0, 0, Map.of(), Map.of()),
                List.of(varNode("v", "ghost")),
                List.of(new Wire(new PortRef("v", "out"), new PortRef("root", "scale"))),
                List.of());
        assertEquals("variable.ghost", scaleExpr(lib(main)));
    }

    @Test
    void setVarWriteFollowsTempScope() {
        GraphData main = graphWithDecl(
                new NodeInstance("root", "entity.root", 0, 0, Map.of(), Map.of()),
                List.of(new NodeInstance("s", "exec.set_var", 0, 0, Map.of(), Map.of()),
                        varNode("t", "scratch"),
                        new NodeInstance("one", "const.int", 0, 0,
                                Map.<String, JsonElement>of("value", new JsonPrimitive(1)), Map.of())),
                List.of(new Wire(new PortRef("t", "out"), new PortRef("s", "target")),
                        new Wire(new PortRef("one", "out"), new PortRef("s", "value")),
                        new Wire(new PortRef("s", "exec_out"), new PortRef("root", "initialize"))),
                List.of(new VariableDecl("scratch", PortType.FLOAT, Optional.empty(),
                        Optional.empty(), VariableDecl.Scope.TEMP)));
        CodegenResult r = new MolangGenerator(lib(main)).emitStatementListFor("root", "root", "initialize");
        assertFalse(r.hasErrors(), () -> "unexpected errors: " + r.diagnostics());
        assertEquals("temp.scratch = 1", r.code());
    }
}

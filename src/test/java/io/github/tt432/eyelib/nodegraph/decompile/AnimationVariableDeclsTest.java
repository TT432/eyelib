package io.github.tt432.eyelib.nodegraph.decompile;

import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AnimationVariableDecls} 单测：节点/边/声明合并、空输入 no-op、缺 ref 跳过。
 */
class AnimationVariableDeclsTest {

    private static GraphLibrary lib(List<NodeInstance> nodes) {
        GraphData main = new GraphData(nodes, List.of(), List.of(), List.of(), List.of(),
                Optional.empty());
        return new GraphLibrary(6, GraphKind.CLIENT_ENTITY, "root", Map.of("root", main));
    }

    @Test
    void wiresVariableNodesIntoDeclPortAndMergesDecls() {
        GraphLibrary lib = lib(List.of(
                new NodeInstance("ra", "ref.animation", 100, 200, Map.of(), Map.of())));
        GraphLibrary out = AnimationVariableDecls.wire(lib,
                Map.of("ra", Set.of("edfatt", "dxvpbr")));

        GraphData main = out.mainGraph();
        assertEquals(3, main.nodes().size());
        assertEquals(2, main.wires().size());
        assertTrue(main.wires().stream().allMatch(
                w -> w.to().node().equals("ra") && w.to().port().equals("decl_variables")));
        // 声明合并：两个新名字
        assertEquals(2, main.variables().size());
        assertTrue(main.variables().stream().map(VariableDecl::name).toList()
                .containsAll(List.of("edfatt", "dxvpbr")));
        // 位置：ref 节点下方竖排
        NodeInstance ref = main.findNode("ra").orElseThrow();
        for (NodeInstance n : main.nodes()) {
            if (n.uid().startsWith("declvar-")) {
                assertEquals(ref.x(), n.x());
                assertTrue(n.y() > ref.y());
            }
        }
    }

    @Test
    void emptyInputIsNoOp() {
        GraphLibrary lib = lib(List.of(new NodeInstance("ra", "ref.animation", 0, 0,
                Map.of(), Map.of())));
        assertSame(lib, AnimationVariableDecls.wire(lib, Map.of()));
        assertSame(lib, AnimationVariableDecls.wire(lib, Map.of("ra", Set.of())));
    }

    @Test
    void missingRefNodeIsSkipped() {
        GraphLibrary lib = lib(List.of());
        GraphLibrary out = AnimationVariableDecls.wire(lib, Map.of("ghost", Set.of("x")));
        assertSame(lib, out);
    }
}

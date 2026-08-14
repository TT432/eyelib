package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortDirection;
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
 * {@link AnimationVariableDecls} 单测：var_refs 快照、read:/write: 命名端口接线、
 * 声明合并、空输入 no-op、缺 ref 跳过、命名端口形态（label/类型/前缀豁免）。
 */
class AnimationVariableDeclsTest {

    private static GraphLibrary lib(List<NodeInstance> nodes) {
        GraphData main = new GraphData(nodes, List.of(), List.of(), List.of(), List.of(),
                Optional.empty());
        return new GraphLibrary(7, GraphKind.CLIENT_ENTITY, "root", Map.of("root", main));
    }

    private static MolangVariableRefs.Refs refs(Set<String> reads, Set<String> writes) {
        return new MolangVariableRefs.Refs(reads, writes);
    }

    @Test
    void writesSnapshotAndNamedPortWires() {
        GraphLibrary lib = lib(List.of(
                new NodeInstance("ra", "ref.animation", 100, 200, Map.of(), Map.of())));
        GraphLibrary out = AnimationVariableDecls.wire(lib,
                Map.of("ra", refs(Set.of("edfatt", "dxvpbr"), Set.of("edfatt"))));

        GraphData main = out.mainGraph();
        // 快照选项落位
        NodeInstance ref = main.findNode("ra").orElseThrow();
        JsonObject snapshot = ref.options().get(NodeTypes.VAR_REFS_OPTION).getAsJsonObject();
        assertEquals(2, snapshot.getAsJsonArray("reads").size());
        assertEquals(1, snapshot.getAsJsonArray("writes").size());

        // 节点 + 命名端口线：edfatt 读写拆双节点（左读右写不反向），dxvpbr 读一个
        assertEquals(4, main.nodes().size());
        assertEquals(3, main.wires().size());
        assertTrue(main.wires().stream().anyMatch(
                w -> w.to().node().equals("ra") && w.to().port().equals("read:edfatt")
                        && w.from().node().equals("declvar-r-ra-edfatt")));
        // v9 左读右写：write 是 ref 的右侧输出，接 declvar 节点的 in（写入通道）
        assertTrue(main.wires().stream().anyMatch(
                w -> w.from().node().equals("ra") && w.from().port().equals("write:edfatt")
                        && w.to().node().equals("declvar-w-ra-edfatt") && w.to().port().equals("in")));
        assertTrue(main.wires().stream().anyMatch(
                w -> w.to().node().equals("ra") && w.to().port().equals("read:dxvpbr")
                        && w.from().node().equals("declvar-r-ra-dxvpbr")));

        // 快照驱动的命名端口定义：read 在输入侧、write 在输出侧（v9 修正：write 曾错误地
        // 随输入 provider 注册，导致端口渲染在左侧——位置即语义，label 不带「读/写」标记）
        List<PortDef> ins = NodeTypes.REF_ANIMATION.inputsOf(ref, null);
        assertEquals(2, ins.size());
        assertTrue(ins.stream().anyMatch(p -> p.id().equals("read:edfatt")
                && p.label().orElse("").equals("v.edfatt")
                && p.direction() == PortDirection.IN));
        List<PortDef> outs = NodeTypes.REF_ANIMATION.outputsOf(ref, null);
        assertTrue(outs.stream().anyMatch(p -> p.id().equals("ref")));
        assertTrue(outs.stream().anyMatch(p -> p.id().equals("write:edfatt")
                && p.label().orElse("").equals("v.edfatt")
                && p.direction() == PortDirection.OUT));
        assertTrue(ins.stream().allMatch(p -> NodeTypes.isVarRefPort(p.id())));
        assertTrue(outs.stream().filter(p -> !p.id().equals("ref"))
                .allMatch(p -> NodeTypes.isVarRefPort(p.id())));
        // 位置：ref 节点下方竖排
        for (NodeInstance n : main.nodes()) {
            if (n.uid().startsWith("declvar-")) {
                assertEquals(ref.x(), n.x());
                assertTrue(n.y() > ref.y());
            }
        }
        // 声明合并
        assertEquals(2, main.variables().size());
        assertTrue(main.variables().stream().map(VariableDecl::name).toList()
                .containsAll(List.of("edfatt", "dxvpbr")));
    }

    @Test
    void dottedMemberGetsObjectParentAndSanitizedPort() {
        GraphLibrary lib = lib(List.of(
                new NodeInstance("ra", "ref.animation", 100, 200, Map.of(), Map.of())));
        GraphLibrary out = AnimationVariableDecls.wire(lib,
                Map.of("ra", refs(Set.of("qpptaw.y"), Set.of("qpptaw.r"))));

        GraphData main = out.mainGraph();
        // 端口 id 编码 '.'→':'（LDLib2 端口 id 禁 '.'），label 保留点分名
        assertTrue(main.wires().stream().anyMatch(
                w -> w.to().node().equals("ra") && w.to().port().equals("read:qpptaw:y")));
        assertTrue(main.wires().stream().anyMatch(
                w -> w.from().node().equals("ra") && w.from().port().equals("write:qpptaw:r")));
        NodeInstance ref = main.findNode("ra").orElseThrow();
        List<PortDef> ins = NodeTypes.REF_ANIMATION.inputsOf(ref, null);
        assertTrue(ins.stream().anyMatch(p -> p.id().equals("read:qpptaw:y")
                && p.label().orElse("").equals("v.qpptaw.y")));
        List<PortDef> outs = NodeTypes.REF_ANIMATION.outputsOf(ref, null);
        assertTrue(outs.stream().anyMatch(p -> p.id().equals("write:qpptaw:r")
                && p.label().orElse("").equals("v.qpptaw.r")));

        // 声明：成员 UNKNOWN + 父链 OBJECT 补齐
        var byName = new java.util.HashMap<String, io.github.tt432.eyelib.nodegraph.PortType>();
        main.variables().forEach(d -> byName.put(d.name(), d.type()));
        assertEquals(io.github.tt432.eyelib.nodegraph.PortType.UNKNOWN, byName.get("qpptaw.r"));
        assertEquals(io.github.tt432.eyelib.nodegraph.PortType.UNKNOWN, byName.get("qpptaw.y"));
        assertEquals(io.github.tt432.eyelib.nodegraph.PortType.OBJECT, byName.get("qpptaw"));
    }

    @Test
    void emptyInputIsNoOp() {
        GraphLibrary lib = lib(List.of(new NodeInstance("ra", "ref.animation", 0, 0,
                Map.of(), Map.of())));
        assertSame(lib, AnimationVariableDecls.wire(lib, Map.of()));
        assertSame(lib, AnimationVariableDecls.wire(lib,
                Map.of("ra", refs(Set.of(), Set.of()))));
    }

    @Test
    void missingRefNodeIsSkipped() {
        GraphLibrary lib = lib(List.of());
        GraphLibrary out = AnimationVariableDecls.wire(lib,
                Map.of("ghost", refs(Set.of("x"), Set.of())));
        assertSame(lib, out);
    }
}

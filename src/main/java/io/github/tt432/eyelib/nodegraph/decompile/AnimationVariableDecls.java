package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 动画/AC 变量引用声明的接线（规格 nodegraph-animation-variable-refs §2.3）：
 * 按（变量名, ref 节点）对新增 variable 节点并接入 ref 的 {@code decl_variables}
 * 端口，新变量名并入图声明，产出新图库。
 *
 * <p>纯元数据变换：不改变任何已有节点/连线，导出产物不变。节点竖排于 ref 节点
 * 下方（x 对齐，y 步进 40）；uid 形如 {@code declvar-<refUid>-<name>}（确定性）。
 */
public final class AnimationVariableDecls {
    private AnimationVariableDecls() {
    }

    /**
     * 接线。varsByRefUid：ref 节点 uid → 该 ref 引用的变量名集（空集/缺键 = 不动）。
     * 全部为空时返回原库。
     */
    public static GraphLibrary wire(GraphLibrary library, Map<String, Set<String>> varsByRefUid) {
        GraphData main = library.mainGraph();
        List<NodeInstance> nodes = new ArrayList<>(main.nodes());
        List<Wire> wires = new ArrayList<>(main.wires());
        Set<String> declNames = new LinkedHashSet<>();
        for (VariableDecl d : main.variables()) {
            declNames.add(d.name());
        }
        List<VariableDecl> newDecls = new ArrayList<>();
        boolean changed = false;

        for (Map.Entry<String, Set<String>> e : varsByRefUid.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }
            Optional<NodeInstance> refOpt = main.findNode(e.getKey());
            if (refOpt.isEmpty()) {
                continue;
            }
            NodeInstance ref = refOpt.get();
            int row = 0;
            for (String name : e.getValue()) {
                String uid = "declvar-" + ref.uid() + "-" + name;
                nodes.add(new NodeInstance(uid, NodeTypes.VARIABLE.id(),
                        ref.x(), ref.y() + 40 + row * 40,
                        Map.of("name", new JsonPrimitive(name)), new LinkedHashMap<>()));
                wires.add(new Wire(new PortRef(uid, "out"), new PortRef(ref.uid(), NodeTypes.DECL_VARIABLES)));
                row++;
                changed = true;
                if (declNames.add(name)) {
                    newDecls.add(new VariableDecl(name, PortType.UNKNOWN,
                            Optional.empty(), Optional.empty(), VariableDecl.Scope.VARIABLE));
                }
            }
        }
        if (!changed) {
            return library;
        }
        List<VariableDecl> variables = new ArrayList<>(main.variables());
        variables.addAll(newDecls);
        Map<String, GraphData> graphs = new LinkedHashMap<>(library.graphs());
        graphs.put(library.main(), new GraphData(nodes, wires, variables,
                main.placemats(), main.stickyNotes(), main.graphInterface()));
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }
}

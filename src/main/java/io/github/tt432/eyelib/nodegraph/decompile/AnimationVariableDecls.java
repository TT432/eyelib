package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
 * 动画/AC 变量引用的命名端口接线（规格 nodegraph-animation-variable-refs §2.3）：
 * 按被引内容提取的读/写变量集，给 ref 节点写入 {@code var_refs} 选项快照
 * （驱动 {@link NodeTypes#varRefPorts} 的命名端口），并按（变量名, ref 节点）对
 * 新增 variable 节点接入对应 {@code read:<name>} / {@code write:<name>} 端口，
 * 新变量名并入图声明，产出新图库。
 *
 * <p>纯元数据变换：不改变任何已有连线的语义，导出产物不变。节点竖排于 ref 节点
 * 下方（x 对齐，y 步进 40）；uid 形如 {@code declvar-<refUid>-<name>}（确定性）。
 */
public final class AnimationVariableDecls {
    private AnimationVariableDecls() {
    }

    /**
     * 接线。refsByRefUid：ref 节点 uid → 该 ref 的读/写变量集（空集/缺键 = 不动）。
     * 全部为空时返回原库。
     */
    public static GraphLibrary wire(GraphLibrary library, Map<String, MolangVariableRefs.Refs> refsByRefUid) {
        GraphData main = library.mainGraph();
        List<NodeInstance> nodes = new ArrayList<>();
        List<Wire> wires = new ArrayList<>(main.wires());
        Set<String> declNames = new LinkedHashSet<>();
        for (VariableDecl d : main.variables()) {
            declNames.add(d.name());
        }
        List<VariableDecl> newDecls = new ArrayList<>();
        boolean changed = false;

        for (NodeInstance node : main.nodes()) {
            // v8：只接 ref.animation——AC 的变量引用由 AC 图自身承担（exec 链内 variable
            // 节点），ref.ac 不建命名端口（规格 §2.1，用户决策 2026-08-08）
            if (!NodeTypes.REF_ANIMATION.id().equals(node.type())) {
                nodes.add(node);
                continue;
            }
            MolangVariableRefs.Refs refs = refsByRefUid.get(node.uid());
            if (refs == null || refs.isEmpty()) {
                nodes.add(node);
                continue;
            }
            // 写入 var_refs 快照（端口定义的驱动源；uid/坐标/常量不变）
            Map<String, com.google.gson.JsonElement> options = new LinkedHashMap<>(node.options());
            options.put(NodeTypes.VAR_REFS_OPTION, snapshotOf(refs));
            nodes.add(new NodeInstance(node.uid(), node.type(), node.x(), node.y(),
                    options, new LinkedHashMap<>(node.constants())));
            changed = true;

            int row = 0;
            for (String name : refs.union()) {
                String uid = "declvar-" + node.uid() + "-" + name;
                nodes.add(new NodeInstance(uid, NodeTypes.VARIABLE.id(),
                        node.x(), node.y() + 40 + row * 40,
                        Map.of("name", new JsonPrimitive(name)), new LinkedHashMap<>()));
                if (refs.reads().contains(name)) {
                    wires.add(new Wire(new PortRef(uid, "out"),
                            new PortRef(node.uid(), NodeTypes.VAR_READ_PREFIX + name)));
                }
                if (refs.writes().contains(name)) {
                    wires.add(new Wire(new PortRef(uid, "out"),
                            new PortRef(node.uid(), NodeTypes.VAR_WRITE_PREFIX + name)));
                }
                row++;
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

    /** 选项快照：{"reads":[...],"writes":[...]}（保持提取顺序，端口按此序生成）。 */
    private static JsonObject snapshotOf(MolangVariableRefs.Refs refs) {
        JsonObject obj = new JsonObject();
        JsonArray reads = new JsonArray();
        refs.reads().forEach(reads::add);
        JsonArray writes = new JsonArray();
        refs.writes().forEach(writes::add);
        obj.add("reads", reads);
        obj.add("writes", writes);
        return obj;
    }
}

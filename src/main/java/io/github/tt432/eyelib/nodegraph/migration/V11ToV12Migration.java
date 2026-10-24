package io.github.tt432.eyelib.nodegraph.migration;

import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphInterface;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * v11 → v12：变量声明与子图接口参数的 any → object。
 */
public final class V11ToV12Migration implements VersionMigration {
    @Override
    public int fromVersion() {
        return 11;
    }

    @Override
    public GraphLibrary apply(GraphLibrary library) {
        return migrateV11ToV12(library);
    }

    /**
     * v12 对象类型落地（规格 nodegraph-object-variables，用户决策 2026-08-14）：
     * v11 及以前变量声明/子图接口参数可选类型里的「object」实际存为 {@code any}
     * （ANY↔OBJECT 共用 LDLib2 OBJECT handle）；v12 起 object 是独立的 struct 类型
     * （{@link PortType#OBJECT}），any 回归纯通配语义。旧文件里声明为 any 的
     * 变量/接口参数全部视为 object 迁移——any 声明在旧版没有第三种语义来源
     * （新建占位是 unknown，导入产物的 any 同样是「对象/未细化」的展示语义）。
     */
    private static GraphLibrary migrateV11ToV12(GraphLibrary library) {
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, GraphData> entry : library.graphs().entrySet()) {
            graphs.put(entry.getKey(), migrateAnyDeclsV12(entry.getValue()));
        }
        return new GraphLibrary(library.formatVersion(), library.kind(), library.main(), graphs);
    }

    private static GraphData migrateAnyDeclsV12(GraphData g) {
        List<VariableDecl> variables = new ArrayList<>(g.variables().size());
        boolean changed = false;
        for (VariableDecl v : g.variables()) {
            if (v.type() == PortType.ANY) {
                variables.add(new VariableDecl(v.name(), PortType.OBJECT, v.group(),
                        v.defaultValue(), v.scope()));
                changed = true;
            } else {
                variables.add(v);
            }
        }
        Optional<GraphInterface> iface = g.graphInterface();
        if (iface.isPresent()) {
            GraphInterface old = iface.get();
            List<GraphInterface.Param> inputs = new ArrayList<>(old.inputs().size());
            boolean ifaceChanged = false;
            for (GraphInterface.Param p : old.inputs()) {
                if (p.type() == PortType.ANY) {
                    inputs.add(new GraphInterface.Param(p.name(), PortType.OBJECT, p.defaultValue()));
                    ifaceChanged = true;
                } else {
                    inputs.add(p);
                }
            }
            GraphInterface.Param out = old.output();
            if (out.type() == PortType.ANY) {
                out = new GraphInterface.Param(out.name(), PortType.OBJECT, out.defaultValue());
                ifaceChanged = true;
            }
            if (ifaceChanged) {
                iface = Optional.of(new GraphInterface(List.copyOf(inputs), out));
                changed = true;
            }
        }
        if (!changed) {
            return g;
        }
        return new GraphData(g.nodes(), g.wires(), List.copyOf(variables), g.placemats(),
                g.stickyNotes(), iface);
    }
}

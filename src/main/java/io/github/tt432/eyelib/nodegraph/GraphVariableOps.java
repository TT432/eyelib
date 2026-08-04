package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 黑板变量的图改写纯函数（变量面板用；规格 nodegraph-eproject-variables §3.2）。
 *
 * <p>变量是结构而非字符串：重命名级联改写图内全部 variable 节点的 name 选项。
 * 全部方法返回新 {@link GraphData}，不修改入参。
 */
public final class GraphVariableOps {
    private GraphVariableOps() {
    }

    /** 新增变量（同名覆盖声明）。 */
    public static GraphData upsert(GraphData graph, VariableDecl decl) {
        List<VariableDecl> variables = new ArrayList<>();
        boolean replaced = false;
        for (VariableDecl existing : graph.variables()) {
            if (existing.name().equals(decl.name())) {
                variables.add(decl);
                replaced = true;
            } else {
                variables.add(existing);
            }
        }
        if (!replaced) {
            variables.add(decl);
        }
        return withVariables(graph, List.copyOf(variables));
    }

    /** 删除变量声明（variable 节点保留；验证器报 UNDECLARED_VARIABLE）。 */
    public static GraphData remove(GraphData graph, String name) {
        List<VariableDecl> variables = graph.variables().stream()
                .filter(v -> !v.name().equals(name))
                .toList();
        return withVariables(graph, variables);
    }

    /** 重命名：声明 + 图内全部 variable 节点的 name 选项同步改写。 */
    public static GraphData rename(GraphData graph, String oldName, String newName) {
        List<VariableDecl> variables = graph.variables().stream()
                .map(v -> v.name().equals(oldName)
                        ? new VariableDecl(newName, v.type(), v.group(), v.defaultValue())
                        : v)
                .toList();
        List<NodeInstance> nodes = graph.nodes().stream()
                .map(n -> "variable".equals(n.type()) && oldName.equals(optionName(n))
                        ? new NodeInstance(n.uid(), n.type(), n.x(), n.y(),
                                Map.of("name", new JsonPrimitive(newName)), n.constants())
                        : n)
                .toList();
        return new GraphData(nodes, graph.wires(), variables, graph.placemats(),
                graph.stickyNotes(), graph.graphInterface());
    }

    /** 改类型（保留分组与默认值）。 */
    public static GraphData retype(GraphData graph, String name, PortType type) {
        List<VariableDecl> variables = graph.variables().stream()
                .map(v -> v.name().equals(name)
                        ? new VariableDecl(v.name(), type, v.group(), v.defaultValue())
                        : v)
                .toList();
        return withVariables(graph, variables);
    }

    /** 查找声明。 */
    public static Optional<VariableDecl> find(GraphData graph, String name) {
        return graph.variables().stream().filter(v -> v.name().equals(name)).findFirst();
    }

    private static GraphData withVariables(GraphData graph, List<VariableDecl> variables) {
        return new GraphData(graph.nodes(), graph.wires(), variables, graph.placemats(),
                graph.stickyNotes(), graph.graphInterface());
    }

    private static String optionName(NodeInstance node) {
        return node.options().get("name") instanceof JsonPrimitive p ? p.getAsString() : "";
    }
}

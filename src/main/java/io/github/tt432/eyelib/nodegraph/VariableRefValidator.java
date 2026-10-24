package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 变量引用校验：未声明变量、未选类型声明、TEMP 作用域有读无写、set_var 写目标。
 *
 * <p>纯静态工具，只读取图与节点类型索引。
 */
final class VariableRefValidator {

    private VariableRefValidator() {
    }

    /** 检查 19：variable 节点引用未声明的黑板变量。 */
    static void checkVariableRefs(GraphData graph, Map<String, NodeInstance> byUid,
                                  Map<String, NodeType> types, List<Diagnostic> out) {
        Set<String> declared = new HashSet<>();
        for (VariableDecl var : graph.variables()) {
            declared.add(var.name());
            // 检查 19c（规格 nodegraph-variable-table §2.6）：未选类型的声明（unknown 过渡态）
            if (var.type() == PortType.UNKNOWN) {
                out.add(Diagnostic.warning(GraphValidator.VARIABLE_TYPE_UNKNOWN,
                        "变量未选择类型（unknown）：" + var.name()));
            }
        }
        for (NodeInstance node : byUid.values()) {
            NodeType type = types.get(node.uid());
            if (type == null || type.kind() != NodeType.Kind.VARIABLE) {
                continue;
            }
            String name = node.option("name", type).map(JsonElement::getAsString).orElse("");
            String stripped = name.startsWith("variable.") ? name.substring("variable.".length()) : name;
            if (!declared.contains(stripped)) {
                out.add(Diagnostic.warning(GraphValidator.UNDECLARED_VARIABLE,
                        "引用了黑板未声明的变量：" + name, node.uid()));
            }
        }

        // 检查 19b（规格 nodegraph-variable-table §2.5）：TEMP 作用域变量同图有读无写——
        // temp 仅在当次求值内存活，跨求值读恒为 0。
        Set<String> tempDeclared = new HashSet<>();
        for (VariableDecl var : graph.variables()) {
            if (var.scope() == VariableDecl.Scope.TEMP) {
                tempDeclared.add(var.name());
            }
        }
        if (!tempDeclared.isEmpty()) {
            Set<String> written = new HashSet<>();
            Set<String> read = new HashSet<>();
            Map<String, String> varNodeName = new HashMap<>();
            for (NodeInstance node : byUid.values()) {
                NodeType type = types.get(node.uid());
                if (type != null && type.kind() == NodeType.Kind.VARIABLE) {
                    String name = node.option("name", type).map(JsonElement::getAsString).orElse("");
                    varNodeName.put(node.uid(), name.startsWith("variable.")
                            ? name.substring("variable.".length()) : name);
                }
            }
            for (Wire w : graph.wires()) {
                // v9 左读右写：写 = exec.set_var.target 输出 → variable.in；读 = variable.out 的全部出线
                String sinkName = "in".equals(w.to().port()) ? varNodeName.get(w.to().node()) : null;
                if (sinkName != null && tempDeclared.contains(sinkName)) {
                    NodeType producerType = types.get(w.from().node());
                    if (producerType != null && producerType.kind() == NodeType.Kind.EXEC_SET_VAR
                            && w.from().port().equals("target")) {
                        written.add(sinkName);
                    }
                    continue;
                }
                String sourceName = "out".equals(w.from().port()) ? varNodeName.get(w.from().node()) : null;
                if (sourceName == null || !tempDeclared.contains(sourceName)) {
                    continue;
                }
                read.add(sourceName);
            }
            for (String name : read) {
                if (!written.contains(name)) {
                    out.add(Diagnostic.warning(GraphValidator.TEMP_NEVER_WRITTEN,
                            "temp 作用域变量被读但同图无写入（跨求值读恒为 0）：" + name));
                }
            }
        }
    }

    /** 检查 20：exec.set_var 的 target 引脚必须连线到 variable 节点（写身份）。 */
    static void checkSetVarTargets(GraphData graph, Map<String, NodeInstance> byUid,
                                   Map<String, NodeType> types, List<Diagnostic> out) {
        for (NodeInstance node : byUid.values()) {
            NodeType type = types.get(node.uid());
            if (type == null || type.kind() != NodeType.Kind.EXEC_SET_VAR) {
                continue;
            }
            boolean ok = false;
            for (Wire wire : graph.wires()) {
                if (wire.from().node().equals(node.uid()) && wire.from().port().equals("target")) {
                    NodeInstance consumer = byUid.get(wire.to().node());
                    NodeType consumerType = consumer == null ? null : types.get(consumer.uid());
                    // v9：target（右侧输出）必须接到 variable 节点的 in（写入通道）
                    ok = consumerType != null && consumerType.kind() == NodeType.Kind.VARIABLE
                            && wire.to().port().equals("in");
                    break;
                }
            }
            if (!ok) {
                out.add(Diagnostic.error(GraphValidator.SET_TARGET_NOT_VARIABLE,
                        "exec.set_var 的 target 必须连线到 variable 节点", node.uid()));
            }
        }
    }
}

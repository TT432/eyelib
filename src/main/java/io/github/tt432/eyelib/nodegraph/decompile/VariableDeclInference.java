package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.nodegraph.MolangLiterals;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypePropagation;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 导入路径的变量声明类型推断：exec.set_var 的 value 来源（连线源端口的静态类型，
 * 或端口行内字面量）按 {@link NodeTypePropagation#merge} 规则归并为声明类型；
 * 点分成员名（{@code qpptaw.r}）补齐 OBJECT 父链声明（{@code qpptaw}）。
 *
 * <p>推不出（无写入、写入源为 ANY/动态/冲突）→ {@link PortType#UNKNOWN} 占位由用户定型
 * （ANY 自 v12 起不再是可声明类型：旧「object 下拉存 any」语义已迁移为 OBJECT）。
 */
public final class VariableDeclInference {
    private VariableDeclInference() {
    }

    private static final NodeType.SubgraphResolver NO_SUBGRAPHS =
            name -> Optional.empty();

    /**
     * 推断每个被写变量的归并类型。键 = variable 节点 name（不带根）；无写入或
     * 全部写入源未知 → 无条目（调用方落 UNKNOWN）。
     */
    public static Map<String, PortType> inferWrittenTypes(List<NodeInstance> nodes, List<Wire> wires) {
        // variable 节点 uid → 声明名
        Map<String, String> variableNames = new LinkedHashMap<>();
        Map<String, NodeInstance> byUid = new LinkedHashMap<>();
        for (NodeInstance n : nodes) {
            byUid.put(n.uid(), n);
            if (NodeTypes.VARIABLE.id().equals(n.type())) {
                variableNames.put(n.uid(), n.optionString("name", ""));
            }
        }
        // exec.set_var.uid → 目标变量名（target 出 → variable.in）
        Map<String, String> setVarTarget = new LinkedHashMap<>();
        // exec.set_var.uid → value 连线源
        Map<String, Wire> setVarValueWire = new LinkedHashMap<>();
        for (Wire w : wires) {
            NodeInstance to = byUid.get(w.to().node());
            if (to == null) {
                continue;
            }
            if (NodeTypes.VARIABLE.id().equals(to.type())
                    && NodeTypes.isVariableWriteInput(to.type(), w.to().port())) {
                NodeInstance from = byUid.get(w.from().node());
                if (from != null && NodeTypes.EXEC_SET_VAR.id().equals(from.type())
                        && "target".equals(w.from().port())) {
                    setVarTarget.put(from.uid(), variableNames.get(to.uid()));
                }
            } else if (NodeTypes.EXEC_SET_VAR.id().equals(to.type()) && "value".equals(w.to().port())) {
                setVarValueWire.put(to.uid(), w);
            }
        }

        Map<String, PortType> merged = new LinkedHashMap<>();
        for (NodeInstance n : nodes) {
            if (!NodeTypes.EXEC_SET_VAR.id().equals(n.type())) {
                continue;
            }
            String name = setVarTarget.get(n.uid());
            if (name == null || name.isEmpty()) {
                continue;
            }
            PortType candidate = valueSourceType(n, setVarValueWire.get(n.uid()), byUid);
            if (candidate == null) {
                continue;
            }
            merged.merge(name, candidate, VariableDeclInference::strictMerge);
        }
        // 归并冲突（ANY）= 无把握 → 移除（调用方落 UNKNOWN）
        merged.values().removeIf(t -> t == PortType.ANY);
        return Map.copyOf(merged);
    }

    /**
     * 声明归并（与 {@link NodeTypePropagation#merge} 的显示层让位语义不同：冲突粘性——
     * ANY 入=已有冲突，不被后续写入覆盖；候选从不为 ANY/未知，调用方已过滤）。
     */
    private static PortType strictMerge(PortType a, PortType b) {
        if (a == PortType.ANY || b == PortType.ANY) {
            return PortType.ANY;
        }
        if (a == b) {
            return a;
        }
        if (a.isNumber() && b.isNumber()) {
            return PortType.FLOAT;
        }
        return PortType.ANY;
    }

    /** 声明类型收口：推断表 + 名字 → 声明类型（推断缺失 → UNKNOWN）。 */
    public static PortType declType(Map<String, PortType> inferred, String name) {
        return inferred.getOrDefault(name, PortType.UNKNOWN);
    }

    /**
     * 点分成员名的父链补 OBJECT 声明（{@code qpptaw.r} → {@code qpptaw: OBJECT}；
     * 多级 {@code a.b.c} 补 {@code a} 与 {@code a.b}）。已有同名声明不覆盖。
     */
    public static List<VariableDecl> completeObjectParents(List<VariableDecl> decls) {
        Set<String> present = new LinkedHashSet<>();
        for (VariableDecl d : decls) {
            present.add(d.name());
        }
        List<VariableDecl> out = new ArrayList<>(decls);
        for (VariableDecl d : decls) {
            String name = d.name();
            int dot = name.indexOf('.');
            while (dot > 0) {
                String parent = name.substring(0, dot);
                if (present.add(parent)) {
                    out.add(VariableDecl.of(parent, PortType.OBJECT));
                }
                dot = name.indexOf('.', dot + 1);
            }
        }
        return List.copyOf(out);
    }

    /** set_var 的 value 来源类型：连线 → 源节点 out 端口静态类型；未连线 → 行内字面量类型。 */
    private static @Nullable PortType valueSourceType(NodeInstance setVar, @Nullable Wire valueWire,
                                                      Map<String, NodeInstance> byUid) {
        if (valueWire != null) {
            NodeInstance src = byUid.get(valueWire.from().node());
            if (src == null) {
                return null;
            }
            NodeType type = NodeTypes.get(src.type()).orElse(null);
            if (type == null) {
                return null;
            }
            for (PortDef out : type.outputsOf(src, NO_SUBGRAPHS)) {
                if (out.id().equals(valueWire.from().port())) {
                    // VARIABLE 身份/ANY 静态端口 = 未知（variable 读不携带声明类型到本层）
                    return out.type() == PortType.ANY || out.type() == PortType.VARIABLE
                            ? null : out.type();
                }
            }
            return null;
        }
        JsonElement inline = setVar.constants().get("value");
        return inline != null ? MolangLiterals.portTypeOf(inline) : null;
    }
}

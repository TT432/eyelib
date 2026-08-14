package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ICustomNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import io.github.tt432.eyelib.nodegraph.NodeTypePropagation;
import io.github.tt432.eyelib.nodegraph.PortType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 透传节点（ternary/null_coalesce）的连线类型传播：a/b 输入的源类型经
 * {@link NodeTypePropagation#merge} 汇合后写回 out 端口的 TypeHandle，
 * 让画布显示真实类型而不是 Object（ANY）满天飞。
 *
 * <p>domain 端口定义保持 ANY（PortProvider 无图上下文，见 NodeType）；本类是编辑器
 * 显示/连线检查层的推导，由 {@link EvmGraph.EvmGraphModel} 的 addWire/removeWire
 * 钩子与变量改型入口触发。定点迭代处理 ternary→ternary 级联，轮数上限 = 节点数 + 1
 * （环在 domain 验证器拒绝，这里仅防御）。
 *
 * <p>行内字面值（未连线的 ANY 端口常量）不参与推导——无连线的输入按未知处理。
 */
public final class EvmTypePropagation {
    private EvmTypePropagation() {
    }

    /** 全图重算全部透传节点的输出类型（幂等；无变化即时退出）。 */
    public static void refresh(GraphModel model) {
        record Target(NodeModel nodeModel, String typeId) {
        }
        List<Target> propagating = new ArrayList<>();
        for (AbstractNodeModel nodeModel : model.getNodeModels()) {
            // NodeModel → API Node 经 ICustomNodeModel（与 EvmGraphTranslator 同径）
            if (nodeModel instanceof NodeModel nm
                    && nm instanceof ICustomNodeModel custom
                    && custom.getNode() instanceof EvmNodeBase evm
                    && NodeTypePropagation.isMergeAbOut(evm.type().id())) {
                propagating.add(new Target(nm, evm.type().id()));
            }
        }
        // 定点迭代：本轮有改动才继续，级联链长不超过节点数
        for (int round = 0; round <= propagating.size(); round++) {
            boolean changed = false;
            for (Target target : propagating) {
                changed |= refreshNode(model, target.nodeModel());
            }
            if (!changed) {
                return;
            }
        }
    }

    /** 单节点重算；out handle 变化时写回并返回 true。 */
    private static boolean refreshNode(GraphModel model, NodeModel node) {
        PortModel out = node.getOutputsById().get("out");
        if (out == null) {
            return false;
        }
        PortType merged = NodeTypePropagation.merge(
                sourceType(model, node.getInputsById().get("a")),
                sourceType(model, node.getInputsById().get("b")));
        TypeHandle handle = EvmTypeHandles.toHandle(merged);
        if (handle.equals(out.getDataTypeHandle())) {
            return false;
        }
        out.setDataTypeHandle(handle);
        return true;
    }

    /** 输入端口的源类型：未连线 → null（未知）；连线源为 variable 读出口 → 声明类型。 */
    private static @Nullable PortType sourceType(GraphModel model, @Nullable PortModel input) {
        if (input == null) {
            return null;
        }
        List<WireModel> wires = model.getWiresForPort(input);
        if (wires.isEmpty()) {
            return null;
        }
        PortModel source = wires.get(0).getFromPort();
        if (source == null) {
            return null;
        }
        // variable 读出口的显示类型是 VARIABLE 身份（EvmVariableNodeModel.getDataType），
        // 传播要的是声明的值类型
        if (source.getNodeModel() instanceof VariableNodeModel variableNode
                && variableNode.getVariableDeclarationModel() != null) {
            return EvmTypeHandles.toPortType(
                    variableNode.getVariableDeclarationModel().getDataTypeHandle());
        }
        return EvmTypeHandles.toPortType(source.getDataTypeHandle());
    }
}
//?}

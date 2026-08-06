//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@code GraphData ↔ EvmGraphModel} 纯翻译（零 MC / 零 LDLib 依赖，可单测）。
 *
 * <p>规则：
 * <ul>
 *   <li>域节点 ↔ EVM 画布节点：uid/类型/坐标/选项/常量直通；</li>
 *   <li>连线 ↔ 画布边：直通（端口 id 本来就是域 id——{@link EvmNode} 全部端口以
 *       {@code PortData.identifier = PortDef.id} 承载）；</li>
 *   <li>黑板变量 ↔ ExposedParameter：标识符 = 分组全路径
 *       （{@code group + "/" + name}，无分组即 {@code name}；按最后一个 {@code "/"} 切分还原，
 *       即「黑板分组平铺全路径名」的 D6 降级，且保持双向无损）；</li>
 *   <li>PARAMETER 画布节点（LDLib 参数面板产物）→ variable 域节点
 *       （name 选项 = 变量名，不带根，变量名为标识符最后一段）；</li>
 *   <li>placemats / stickyNotes / graphInterface 不在画布模型内——回写时从既有
 *       {@link GraphData} 原样保留（规格 D6：分组仅持久化在文档）。</li>
 * </ul>
 */
public final class EvmGraphMapper {
    private EvmGraphMapper() {
    }

    // ---------- GraphData → model ----------

    public static EvmGraphModel toModel(GraphData data) {
        List<EvmGraphModel.EvmNodeModel> nodes = new ArrayList<>(data.nodes().size());
        for (NodeInstance node : data.nodes()) {
            nodes.add(EvmGraphModel.EvmNodeModel.evm(
                    node.uid(), node.type(), node.x(), node.y(), node.options(), node.constants()));
        }
        List<EvmGraphModel.EvmEdgeModel> edges = new ArrayList<>(data.wires().size());
        for (Wire wire : data.wires()) {
            edges.add(new EvmGraphModel.EvmEdgeModel(
                    wire.from().node(), wire.from().port(), wire.to().node(), wire.to().port()));
        }
        List<EvmGraphModel.EvmParamModel> params = new ArrayList<>(data.variables().size());
        for (VariableDecl variable : data.variables()) {
            params.add(new EvmGraphModel.EvmParamModel(
                    flatten(variable), variable.type(), variable.defaultValue()));
        }
        return new EvmGraphModel(nodes, edges, params);
    }

    // ---------- model → GraphData ----------

    /**
     * @param model    画布模型
     * @param previous 该图既有文档（placemats/stickyNotes/graphInterface 从此保留）
     */
    public static GraphData toData(EvmGraphModel model, GraphData previous) {
        List<NodeInstance> nodes = new ArrayList<>(model.nodes().size());
        for (EvmGraphModel.EvmNodeModel node : model.nodes()) {
            nodes.add(switch (node.kind()) {
                // 工厂方法保证 kind 与字段对应（evm→typeId、parameter→parameterId 非空）
                case EVM -> new NodeInstance(node.uid(), Objects.requireNonNull(node.typeId()), node.x(), node.y(),
                        node.options(), node.constants());
                case PARAMETER -> new NodeInstance(node.uid(), NodeTypes.VARIABLE.id(), node.x(), node.y(),
                        Map.of("name", new JsonPrimitive(variableName(Objects.requireNonNull(node.parameterId())))),
                        Map.of());
            });
        }
        List<Wire> wires = new ArrayList<>(model.edges().size());
        for (EvmGraphModel.EvmEdgeModel edge : model.edges()) {
            wires.add(new Wire(new PortRef(edge.fromUid(), edge.fromPort()),
                    new PortRef(edge.toUid(), edge.toPort())));
        }
        List<VariableDecl> variables = new ArrayList<>(model.params().size());
        for (EvmGraphModel.EvmParamModel param : model.params()) {
            variables.add(new VariableDecl(
                    variableName(param.identifier()), param.type(), variableGroup(param.identifier()),
                    param.defaultValue()));
        }
        return new GraphData(nodes, wires, variables,
                previous.placemats(), previous.stickyNotes(), previous.graphInterface());
    }

    // ---------- 变量标识符规则 ----------

    /** VariableDecl → ExposedParameter 标识符（分组全路径）。 */
    public static String flatten(VariableDecl variable) {
        return variable.group().map(g -> g + "/" + variable.name()).orElse(variable.name());
    }

    /** 标识符 → 变量名（最后一个 "/" 之后）。 */
    public static String variableName(String identifier) {
        int slash = identifier.lastIndexOf('/');
        return slash < 0 ? identifier : identifier.substring(slash + 1);
    }

    /** 标识符 → 分组路径（最后一个 "/" 之前；无 = 未分组）。 */
    public static Optional<String> variableGroup(String identifier) {
        int slash = identifier.lastIndexOf('/');
        return slash < 0 ? Optional.empty() : Optional.of(identifier.substring(0, slash));
    }
}
//?}

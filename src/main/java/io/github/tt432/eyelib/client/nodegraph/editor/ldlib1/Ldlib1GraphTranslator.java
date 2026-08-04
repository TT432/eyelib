//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.PortEdge;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ParameterNode;
import com.lowdragmc.lowdraglib.utils.Position;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * {@code EvmGraphModel ↔ BaseGraph} 薄绑定（唯一直接触碰 LDLib 类型的翻译层）。
 *
 * <p>规则要点：
 * <ul>
 *   <li>域 uid 任意字符串，graphprocessor GUID 必须可 {@code UUID.fromString}
 *       （{@code BaseGraph.addGUID}）——载入时 GUID = {@code nameUUIDFromBytes(uid)}，
 *       域 uid 另存 {@link EvmNode#evmUid}；画布新建节点无 evmUid，回译取 GUID 作 uid；</li>
 *   <li>连线经 {@code graph.connect(inputPort, outputPort)}（参数序：输入在前），
 *       端口按（承载字段 "in"/"out"，identifier = 域端口 id）反查；</li>
 *   <li>黑板变量 → ExposedParameter（accessor=Get），显示名 = 分组全路径（D6 平铺）；</li>
 *   <li>ParameterNode（旧参数面板拖拽产物，仅兼容粘贴/旧画布）回译为 variable 域节点；
 *       accessor=Set 的参数节点本域无对应表达，跳过并告警。</li>
 * </ul>
 */
public final class Ldlib1GraphTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ldlib1GraphTranslator.class);

    /** BaseNode.GUID 反射句柄（懒初始化：无 setter，uid→GUID 写入用）。 */
    private static @Nullable Field guidField;

    private Ldlib1GraphTranslator() {
    }

    // ---------- GraphData → BaseGraph ----------

    /** 把库中一张图翻译为可编辑的 {@link EvmBaseGraph}。 */
    public static EvmBaseGraph toGraph(String libraryName, String graphName, GraphLibrary library) {
        GraphData data = library.graphs().get(graphName);
        if (data == null) {
            throw new IllegalArgumentException("graph '" + graphName + "' not found in library '" + libraryName + "'");
        }
        EvmGraphModel model = EvmGraphMapper.toModel(data);
        NodeType.SubgraphResolver resolver = NodeType.SubgraphResolver.of(library, data.graphInterface());

        List<ExposedParameter<?>> params = new ArrayList<>(model.params().size());
        for (EvmGraphModel.EvmParamModel param : model.params()) {
            params.add(toExposedParameter(param));
        }
        EvmBaseGraph graph = new EvmBaseGraph(libraryName, graphName, library, params);

        Map<String, BaseNode> byUid = new HashMap<>();
        for (EvmGraphModel.EvmNodeModel nodeModel : model.nodes()) {
            if (nodeModel.kind() != EvmGraphModel.EvmNodeModel.Kind.EVM || nodeModel.typeId() == null) {
                continue;
            }
            EvmNode node = new EvmNode(nodeModel.typeId(), resolver);
            node.evmUid = nodeModel.uid();
            node.options.putAll(nodeModel.options());
            node.constants.putAll(nodeModel.constants());
            node.position = new Position(Math.round(nodeModel.x()), Math.round(nodeModel.y()));
            setGuid(node, UUID.nameUUIDFromBytes(nodeModel.uid().getBytes(StandardCharsets.UTF_8)).toString());
            graph.addNode(node);
            byUid.put(nodeModel.uid(), node);
        }
        for (EvmGraphModel.EvmEdgeModel edge : model.edges()) {
            BaseNode from = byUid.get(edge.fromUid());
            BaseNode to = byUid.get(edge.toUid());
            if (from == null || to == null) {
                LOGGER.warn("[nodegraph] skip wire {}.{} -> {}.{}: endpoint node missing",
                        edge.fromUid(), edge.fromPort(), edge.toUid(), edge.toPort());
                continue;
            }
            NodePort outputPort = from.getPort("out", edge.fromPort());
            NodePort inputPort = to.getPort("in", edge.toPort());
            if (outputPort == null || inputPort == null) {
                LOGGER.warn("[nodegraph] skip wire {}.{} -> {}.{}: port not found",
                        edge.fromUid(), edge.fromPort(), edge.toUid(), edge.toPort());
                continue;
            }
            graph.connect(inputPort, outputPort);
        }
        return graph;
    }

    private static ExposedParameter<?> toExposedParameter(EvmGraphModel.EvmParamModel param) {
        ExposedParameter<?> exposed = switch (param.type()) {
            case FLOAT -> new ExposedParameter.Float(param.identifier());
            case BOOL -> new ExposedParameter.Bool(param.identifier());
            case STRING -> new ExposedParameter.String(param.identifier());
            default -> new ExposedParameter<>(param.identifier(), Object.class);
        };
        exposed.setDisplayName(param.identifier());
        param.defaultValue().ifPresent(v -> exposed.setValue(fromJson(v)));
        return exposed;
    }

    private static @Nullable Object fromJson(JsonElement json) {
        if (json instanceof JsonPrimitive primitive) {
            if (primitive.isNumber()) return primitive.getAsFloat();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isString()) return primitive.getAsString();
        }
        return null;
    }

    // ---------- BaseGraph → GraphData ----------

    /** 把当前画布状态回译为 {@link GraphData}（placemats 等从 previous 保留）。 */
    public static GraphData toData(EvmBaseGraph graph, GraphData previous) {
        return EvmGraphMapper.toData(extract(graph), previous);
    }

    /** BaseGraph → 中间模型（含 ParameterNode 特判）。 */
    public static EvmGraphModel extract(EvmBaseGraph graph) {
        Set<BaseNode> skipped = new HashSet<>();
        List<EvmGraphModel.EvmNodeModel> nodes = new ArrayList<>(graph.nodes.size());
        for (BaseNode node : graph.nodes) {
            if (node instanceof EvmNode evm) {
                nodes.add(EvmGraphModel.EvmNodeModel.evm(
                        uidOf(evm), evm.nodeTypeId,
                        evm.position == null ? 0 : evm.position.x,
                        evm.position == null ? 0 : evm.position.y,
                        evm.options, evm.constants));
            } else if (node instanceof ParameterNode parameter
                    && parameter.parameter != null
                    && parameter.parameter.getAccessor() == ExposedParameter.ParameterAccessor.Get) {
                nodes.add(EvmGraphModel.EvmNodeModel.parameter(
                        parameter.getGUID(), parameter.parameterIdentifier,
                        parameter.position == null ? 0 : parameter.position.x,
                        parameter.position == null ? 0 : parameter.position.y));
            } else {
                skipped.add(node);
                LOGGER.warn("[nodegraph] skip unsupported canvas node: {}", node.getClass().getName());
            }
        }

        List<EvmGraphModel.EvmEdgeModel> edges = new ArrayList<>(graph.edges.size());
        for (PortEdge edge : graph.edges) {
            if (edge.outputNode == null || edge.inputNode == null
                    || skipped.contains(edge.outputNode) || skipped.contains(edge.inputNode)) {
                continue;
            }
            String fromPort = edge.outputNode instanceof ParameterNode
                    ? "out" : portId(edge.outputFieldName, edge.outputPortIdentifier);
            String toPort = edge.inputNode instanceof ParameterNode
                    ? "out" : portId(edge.inputFieldName, edge.inputPortIdentifier);
            edges.add(new EvmGraphModel.EvmEdgeModel(uidOf(edge.outputNode), fromPort, uidOf(edge.inputNode), toPort));
        }

        List<EvmGraphModel.EvmParamModel> params = new ArrayList<>(graph.exposedParameters.size());
        for (ExposedParameter<?> param : graph.exposedParameters.values()) {
            params.add(new EvmGraphModel.EvmParamModel(
                    param.identifier, EvmLinks.toPortType(param.type), toJson(param.getValue())));
        }
        return new EvmGraphModel(nodes, edges, params);
    }

    private static String uidOf(BaseNode node) {
        if (node instanceof EvmNode evm && !evm.evmUid.isEmpty()) {
            return evm.evmUid;
        }
        return node.getGUID();
    }

    /** LDLib 端口（承载字段 + identifier）→ 域端口 id。 */
    private static String portId(String fieldName, @Nullable String identifier) {
        return identifier == null || identifier.isEmpty() ? fieldName : identifier;
    }

    private static Optional<JsonElement> toJson(@Nullable Object value) {
        if (value instanceof Number number) return Optional.of(new JsonPrimitive(number));
        if (value instanceof Boolean bool) return Optional.of(new JsonPrimitive(bool));
        if (value instanceof String string) return Optional.of(new JsonPrimitive(string));
        return Optional.empty();
    }

    static void setGuid(BaseNode node, String guid) {
        try {
            guidField().set(node, guid);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("cannot assign graphprocessor GUID", e);
        }
    }

    private static synchronized Field guidField() {
        if (guidField == null) {
            try {
                guidField = BaseNode.class.getDeclaredField("GUID");
                guidField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        return guidField;
    }
}
//?}

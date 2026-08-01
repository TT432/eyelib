package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if !legacy {
import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import io.github.tt432.eyelib.nodegraph.GraphInterface;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.PortType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * eyelib EVM 图的 LDLib2 承载：一个 {@link EvmGraph} = domain 库中的一张图（主图或子图）。
 * 主图实例的 {@link GraphModel#getLocalSubGraphs()} 持有全部命名子图（D5 ↔ LDLib2 local subgraph）。
 *
 * <p>节点动态端口所需的 {@link NodeType.SubgraphResolver} 由本类提供：
 * 库内子图按名解析（根图 {@link LibraryContext} 的名字 → 子图模型表），
 * 自身接口由本图 INPUT/OUTPUT 变量现推导（用户在黑板改子图变量即时反映到
 * subgraph.input/output 锚点与 subgraph.call 端口）。
 */
public class EvmGraph extends Graph {

    /**
     * 库上下文：仅根图持有，子图经 parentGraph 链访问。
     * 名字 ↔ 子图 GraphModel 的双向表（domain 子图名是权威，LDLib2 侧无命名概念）。
     */
    public static final class LibraryContext {
        public final Map<String, GraphModel> subgraphsByName = new LinkedHashMap<>();
        public final Map<UUID, String> namesBySubgraphUid = new LinkedHashMap<>();
        /** 未命名（用户在编辑器里新建）的 local subgraph 的取名计数。 */
        public int unnamedSubgraphCounter;
    }

    /** 所属库种类（保存时回写 GraphLibrary.kind）。 */
    public GraphKind libraryKind = GraphKind.CLIENT_ENTITY;
    /** 主图在 GraphLibrary.graphs 中的键（保存时回写 GraphLibrary.main）。 */
    public String mainGraphName = "root";
    /** 仅根图非 null。 */
    private @Nullable LibraryContext context;

    public EvmGraph() {
    }

    public void setContext(@Nullable LibraryContext context) {
        this.context = context;
    }

    /** 库上下文（沿 parentGraph 链向上找到根图）。无父链时返回自身 context（可为 null）。 */
    public @Nullable LibraryContext context() {
        if (context != null) return context;
        GraphModel model = graphModel;
        while (model.getParentGraph() != null) {
            model = model.getParentGraph();
        }
        if (model instanceof CustomGraphModelImpl custom && custom.getGraph() instanceof EvmGraph root) {
            return root.context;
        }
        return null;
    }

    /** 节点动态端口用的子图接口解析器。 */
    public NodeType.SubgraphResolver resolver() {
        return new NodeType.SubgraphResolver() {
            @Override
            public Optional<GraphInterface> resolve(String subgraphName) {
                LibraryContext ctx = context();
                if (ctx == null) return Optional.empty();
                GraphModel sub = ctx.subgraphsByName.get(subgraphName);
                return sub == null ? Optional.empty() : interfaceOf(sub);
            }

            @Override
            public Optional<GraphInterface> self() {
                return interfaceOf(graphModel);
            }
        };
    }

    /**
     * 由 GraphModel 的变量推导 domain 子图接口：
     * INPUT 变量（声明序）→ inputs；首个 OUTPUT 变量 → output。
     */
    public static Optional<GraphInterface> interfaceOf(GraphModel model) {
        List<GraphInterface.Param> inputs = new ArrayList<>();
        GraphInterface.Param output = null;
        for (VariableDeclarationModelBase var : model.getGraphVariableModels()) {
            if (var == null) continue;
            if (var.isInput()) {
                inputs.add(new GraphInterface.Param(var.getName(), portTypeOf(var), variableDefault(var)));
            } else if (var.isOutput() && output == null) {
                output = new GraphInterface.Param(var.getName(), portTypeOf(var), variableDefault(var));
            }
        }
        if (output == null && inputs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new GraphInterface(inputs,
                output != null ? output : GraphInterface.Param.of("result", PortType.ANY)));
    }

    private static PortType portTypeOf(VariableDeclarationModelBase var) {
        PortType type = EvmTypeHandles.toPortType(var.getDataTypeHandle());
        return type != null ? type : PortType.ANY;
    }

    private static Optional<JsonElement> variableDefault(VariableDeclarationModelBase var) {
        var init = var.getInitializationModel();
        if (init == null) return Optional.empty();
        return Optional.ofNullable(EvmValues.javaToJson(init.getValue()));
    }

    @Override
    protected CustomGraphModelImpl createGraphModel() {
        return new EvmGraphModel(this);
    }

    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        return EvmNodes.ALL;
    }

    @Override
    public List<TypeHandle> getSupportTypes() {
        return EvmTypeHandles.allSupported();
    }

    /**
     * 连线类型检查走 domain 兼容矩阵（规格 §2.1：bool↔float 隐式、ref→string 等），
     * 不认得的 handle 回落 LDLib2 默认 Java 类型检查。
     */
    public static final class EvmGraphModel extends CustomGraphModelImpl {
        public EvmGraphModel(Graph graph) {
            super(graph);
        }

        @Override
        public boolean canAssignTo(PortModel destination, PortModel source) {
            PortType dest = EvmTypeHandles.toPortType(destination.getDataTypeHandle());
            PortType src = EvmTypeHandles.toPortType(source.getDataTypeHandle());
            if (dest != null && src != null) {
                return src.isAssignableTo(dest);
            }
            return super.canAssignTo(destination, source);
        }
    }
}
//?}

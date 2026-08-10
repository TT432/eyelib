package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if >=1.20.1 {
import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import io.github.tt432.eyelib.nodegraph.GraphInterface;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
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
        /**
         * 变量模型 uid → molang 作用域（规格 nodegraph-variable-table §2.4）：LDLib2 变量模型
         * 无作用域字段，eyelib 侧在此持有。加载时由 translator 从 VariableDecl 登记；
         * 会话内改名 uid 不变、作用域跟随；变量表面板新建/改作用域也写这里。
         */
        public final Map<UUID, VariableDecl.Scope> variableScopes = new LinkedHashMap<>();
        /**
         * 节点模型 uid → ref.animation 节点的 var_refs 快照（规格
         * nodegraph-animation-variable-refs）：快照无 NodeOptionDef（不进模型选项），
         * LDLib2 模型往返会丢——与 variableScopes 同 pattern 侧表持有：加载时 translator
         * 登记，{@code EvmNodeBase#currentInstanceView} 合并进端口视图，保存时回写。
         */
        public final Map<UUID, com.google.gson.JsonElement> varRefs = new LinkedHashMap<>();
        /**
         * 当前库在 {@code GraphLibraryManager} 的键（含项目前缀，如 allay/allay）：
         * 变量表读/写引用计数做闭包级统计时按前缀找同项目的 AC 图库（规格
         * nodegraph-variable-table §2.4）。打开编辑器时由 Ldlib2NodegraphEditor 写入。
         */
        public @Nullable String libraryKey;
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

    /** 黑板变量类型候选不含 VARIABLE 身份类型（它只是 variable 节点端口的身份标注）。 */
    @Override
    public List<TypeHandle> getVariableSupportTypes() {
        return EvmTypeHandles.allVariableDeclTypes();
    }

    /**
     * 黑板变量节点（拖拽/翻译/粘贴统一走本类，见 {@link EvmGraphModel#getVariableNodeType}
     * 与 {@link EvmGraphModel#createNodeFromDiscriminator}）。
     *
     * <p>读出口类型固定为 {@link PortType#VARIABLE} 身份 handle（原样会取声明的具体类型）：
     * 连线兼容由 {@link EvmGraphModel#canAssignTo} 走 domain 矩阵——VARIABLE 可接一切值端口
     * （隐式读）且可接 exec.set_var.target（写身份）；若保留声明类型，FLOAT 变量将连不上
     * STRING 输入与 VARIABLE 身份的 target。写入口（子图接口 OUTPUT 变量，WRITE 修饰）
     * 保留声明的具体类型——喂入值仍按声明类型检查。
     */
    public static final class EvmVariableNodeModel extends VariableNodeModelImpl {
        /** v9 左读右写：domain variable 节点有 in（写入）+ out（读取）双口。 */
        public static final String WRITE_IN_PORT_ID = "in";

        @Override
        protected void onDefineNode(
                com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.NodeDefinitionScope<? extends NodeModel> scope) {
            super.onDefineNode(scope);
            // 主口承担读取（右出）；补写入入口（左入）——接 exec.set_var.target / ref write: 输出。
            // WRITE 修饰（子图接口 OUTPUT 变量）的主口本身是输入，不重复补。
            if (getVariableDeclarationModel() == null
                    || !getVariableDeclarationModel().getModifiers().hasFlag(ModifierFlags.WRITE)) {
                var writeIn = scope.nodeModel.addInputPort(WRITE_IN_PORT_ID,
                        EvmTypeHandles.toHandle(PortType.VARIABLE), null, null, null, null, null);
                // 左读右写：位置即语义，不显示 "in" 文本标签（空标题 → 连接器隐藏 label）
                writeIn.setTitle(net.minecraft.network.chat.Component.empty());
            }
        }

        @Override
        public TypeHandle getDataType() {
            var decl = getVariableDeclarationModel();
            if (decl == null) return super.getDataType();
            if (decl.getModifiers() == ModifierFlags.WRITE) {
                return decl.getDataTypeHandle();
            }
            return EvmTypeHandles.toHandle(PortType.VARIABLE);
        }

        @Override
        public com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement<?> createElementUI() {
            // 默认 VariableNodeElement 只渲主口；写入入口 in 需要 EvmVariableNodeElement 补渲
            return new EvmVariableNodeElement(this);
        }
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
        protected Class<? extends VariableNodeModel> getVariableNodeType() {
            return EvmVariableNodeModel.class;
        }

        /** 粘贴/撤销经鉴别子重建变量节点，保持同一实现（端口身份类型规则一致）。 */
        @Override
        protected AbstractNodeModel createNodeFromDiscriminator(String type) {
            if ("variable".equals(type)) return new EvmVariableNodeModel();
            return super.createNodeFromDiscriminator(type);
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

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if !legacy {
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.IPort;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortCapacity;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeOptionDef;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.PortDef;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 全部 eyelib 节点的 LDLib2 基类：子类只声明 {@link #type()}（domain 节点类型）。
 *
 * <p>选项：{@link NodeOptionDef} → LDLib2 节点选项（STRING/TEXT/ENUM/IDENTIFIER→String、
 * INT→Integer、FLOAT→Float、BOOL→Boolean）。动态端口节点（query/math/exec.call 的 arg_count、
 * op.binary 的 op、subgraph.call 的子图名）按 TestAddNode 模式在 {@link #onDefinePorts}
 * 读当前选项值重算端口——选项值变更时 LDLib2 会重跑 defineNode（OptionBuilder setterAction）。
 *
 * <p>端口：端口 id 与 domain {@link PortDef#id()} 完全一致（addInputPort 第一个参数即 id，
 * 也是缺省显示名）。容量按 domain multi 标志显式设置（LDLib2 默认输入 SINGLE/输出 MULTIPLE）。
 */
public abstract class EvmNodeBase extends Node {

    /** domain 节点类型（目录条目）。 */
    public abstract NodeType type();

    /** 本节点所在的 EvmGraph（经 nodeModel → graphModel → graph 链）。 */
    protected @Nullable EvmGraph evmGraph() {
        var model = getNodeModel();
        if (model != null && model.getGraphModel() instanceof CustomGraphModelImpl custom
                && custom.getGraph() instanceof EvmGraph graph) {
            return graph;
        }
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(EvmNodes.displayName(type().id()));
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        super.onDefineOptions(context);
        for (NodeOptionDef def : type().options()) {
            context.addOption(def.id(), EvmValues.optionJavaType(def.type()))
                    .withDefaultValue(EvmValues.optionDefault(def))
                    .withDisplayName(Component.literal(def.id()));
        }
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        EvmGraph graph = evmGraph();
        NodeType.SubgraphResolver resolver = graph != null
                ? graph.resolver()
                : name -> Optional.empty();
        NodeInstance view = currentInstanceView();
        for (PortDef port : type().inputsOf(view, resolver)) {
            var builder = context.addInputPort(port.id(), EvmTypeHandles.toHandle(port.type()))
                    .withDisplayName(Component.literal(port.id()));
            port.defaultValue().ifPresent(dv -> {
                Object value = EvmValues.portJsonToJava(dv, port.type());
                if (value != null) builder.withDefaultValue(value);
            });
            applyCapacity(builder.build(), port);
        }
        for (PortDef port : type().outputsOf(view, resolver)) {
            var builder = context.addOutputPort(port.id(), EvmTypeHandles.toHandle(port.type()))
                    .withDisplayName(Component.literal(port.id()));
            applyCapacity(builder.build(), port);
        }
    }

    /** LDLib2 默认容量（输入 SINGLE/输出 MULTIPLE）与 domain multi 对齐。 */
    private static void applyCapacity(IPort port, PortDef def) {
        if (port instanceof PortModel model) {
            model.setPortCapacity(def.multi() ? PortCapacity.MULTIPLE : PortCapacity.SINGLE);
        }
    }

    /**
     * 用当前选项值拼一个临时 {@link NodeInstance}，供 domain 端口提供器计算动态端口
     * （端口提供器只读 options，不读 uid/坐标）。
     */
    private NodeInstance currentInstanceView() {
        Map<String, com.google.gson.JsonElement> options = new HashMap<>();
        for (NodeOptionDef def : type().options()) {
            Object value = readOptionValue(def);
            var json = EvmValues.javaToJson(value != null ? value : EvmValues.optionDefault(def));
            if (json != null) {
                options.put(def.id(), json);
            }
        }
        return new NodeInstance("", type().id(), 0, 0, options, Map.of());
    }

    private @Nullable Object readOptionValue(NodeOptionDef def) {
        INodeOption option = getNodeOptionById(def.id());
        if (option == null) return null;
        return option.tryGetValue(EvmValues.optionJavaType(def.type())).result().orElse(null);
    }
}
//?}

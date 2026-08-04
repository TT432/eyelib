package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if !legacy {
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.IPort;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortCapacity;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodePreviewContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import io.github.tt432.eyelib.client.nodegraph.AssetSuggestions;
import io.github.tt432.eyelib.client.nodegraph.preview.PreviewViewState;
import io.github.tt432.eyelib.nodegraph.ColorValues;
import io.github.tt432.eyelib.nodegraph.InlineLiteral;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeOptionDef;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.ShortNames;
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

    /** ref 预览的交互视角（节点级持有，预览元素重建不丢；不序列化）。 */
    private final PreviewViewState previewViewState = new PreviewViewState();

    /** ref 预览的交互视角（{@link EvmRefPreviewElement} 使用）。 */
    public PreviewViewState previewViewState() {
        return previewViewState;
    }

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

    /**
     * ref.geometry / ref.texture 节点启用 LDLib2 内建节点预览面板（{@code NodePreviewModel}，
     * {@code NodeElement.buildPreviewPart} 装配）；内容由 {@link EvmRefPreviewElement} 绘制。
     */
    @Override
    public boolean hasNodePreview() {
        return type().kind() == NodeType.Kind.REF_GEOMETRY || type().kind() == NodeType.Kind.REF_TEXTURE;
    }

    @Override
    public void onBuildNodePreview(NodePreviewContext context) {
        if (!hasNodePreview()) return;
        context.container().addChild(new EvmRefPreviewElement(this));
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        super.onDefineOptions(context);
        boolean isRef = ShortNames.valueOptionOf(type().id()) != null;
        for (NodeOptionDef def : type().options()) {
            var builder = context.addOption(def.id(), EvmValues.optionJavaType(def.type()))
                    .withDefaultValue(EvmValues.optionDefault(def))
                    .withDisplayName(Component.literal(def.id()));
            // ref 节点的 short_name：绑定有效短名而非底层选项（见 shortNameBinding）
            if (isRef && ShortNames.SHORT_NAME_OPTION.equals(def.id())) {
                builder.withConfigurable(shortNameBinding());
            }
            // COLOR 选项：取色器绑定（hex 字符串 ↔ ARGB int，见 colorBinding）
            if (def.type() == NodeOptionDef.OptionType.COLOR) {
                builder.withConfigurable(colorBinding());
            }
            // 规格 §4.2：带 suggestionKey 的字符串选项挂资产候选下拉
            if (def.suggestionKey().isPresent()
                    && EvmValues.optionJavaType(def.type()) == String.class) {
                builder.withConfigurable(assetSuggestions(def.suggestionKey().get()));
            }
        }
    }

    /**
     * ref 节点 short_name 的自定义绑定：字段始终显示有效短名——底层空时显示自动生成的
     * 派生值（forceUpdate 每帧拉取 → identifier 改动即时跟随，且不触发短名自身变更事件）；
     * 用户编辑写显式值，清空（空串 = 底层默认）即回自动模式
     * （domain 语义不变：空 = 派生，见 {@link ShortNames#effective}）。
     */
    private ITypeConfigurable shortNameBinding() {
        return (valueConfigurable, typeHandle) -> IConfigurable.create(father ->
                father.addConfigurator(new StringConfigurator("",
                        this::effectiveShortName, valueConfigurable::setValue,
                        "", valueConfigurable.forceUpdate())));
    }

    /** 当前有效短名：显式 short_name 非空 ? 显式值 : 对标识符选项派生（{@link ShortNames#derive}）。 */
    private String effectiveShortName() {
        String explicit = java.util.Objects.toString(readStringOption(ShortNames.SHORT_NAME_OPTION), "");
        if (!explicit.isEmpty()) {
            return explicit;
        }
        String valueOption = ShortNames.valueOptionOf(type().id());
        return valueOption == null ? ""
                : ShortNames.derive(java.util.Objects.toString(readStringOption(valueOption), ""));
    }

    /** 读字符串选项当前值（未设/缺失 → null）。 */
    private @Nullable String readStringOption(String id) {
        INodeOption option = getNodeOptionById(id);
        if (option == null) return null;
        return option.<String>tryGetValue(String.class).result().orElse(null);
    }

    /**
     * 资产候选配置器（规格 §4.2）：StringConfigurator 保留自由输入（外部契约逃生舱），
     * SelectorConfigurator 供给运行时注册表候选（检查器每次打开取 COW 快照）；两行绑同一选项值。
     */
    private static ITypeConfigurable assetSuggestions(String suggestionKey) {
        return (valueConfigurable, typeHandle) -> IConfigurable.create(father -> {
            java.util.function.Supplier<String> getter = valueConfigurable::getValue;
            java.util.function.Consumer<String> setter = valueConfigurable::setValue;
            String fallback = java.util.Objects.toString(valueConfigurable.getDefaultValue(), "");
            boolean forceUpdate = valueConfigurable.forceUpdate();
            father.addConfigurator(new StringConfigurator("", getter, setter, fallback, forceUpdate));
            father.addConfigurator(new SelectorConfigurator<>("", getter, setter, fallback, forceUpdate,
                    AssetSuggestions.suggest(suggestionKey), s -> s));
        });
    }

    /**
     * COLOR 选项的取色器绑定：底层值为 {@code #AARRGGBB} hex 字符串，取色器侧按 ARGB int
     * 读写（{@link ColorValues#toArgbInt}/{@link ColorValues#fromArgbInt} 互转；非法 hex 显示不透明白）。
     */
    private static ITypeConfigurable colorBinding() {
        return (valueConfigurable, typeHandle) -> IConfigurable.create(father ->
                father.addConfigurator(new ColorConfigurator("",
                        () -> ColorValues.toArgbInt(java.util.Objects.toString(
                                valueConfigurable.getValue(), null)),
                        argb -> valueConfigurable.setValue(ColorValues.fromArgbInt(argb)),
                        ColorValues.toArgbInt(java.util.Objects.toString(
                                valueConfigurable.getDefaultValue(), null)),
                        valueConfigurable.forceUpdate())));
    }

    /**
     * ANY 输入端口的行内文本绑定：文本 ↔ JSON 字面值（{@link InlineLiteral} 智能解析：
     * 整数→int、小数→float、true/false→bool、其余→字符串）→ Java 值
     * （{@link EvmValues#portJsonToJava}，Float/Boolean/String）。
     */
    private static ITypeConfigurable anyTextBinding() {
        return (valueConfigurable, typeHandle) -> IConfigurable.create(father ->
                father.addConfigurator(new StringConfigurator("",
                        () -> InlineLiteral.toText(EvmValues.javaToJson(valueConfigurable.getValue())),
                        text -> valueConfigurable.setValue(EvmValues.portJsonToJava(
                                InlineLiteral.parse(text), PortType.ANY)),
                        InlineLiteral.toText(EvmValues.javaToJson(valueConfigurable.getDefaultValue())),
                        valueConfigurable.forceUpdate())));
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
            // ANY 输入端口：挂行内字面值文本编辑器（见 anyTextBinding）
            if (port.type() == PortType.ANY) {
                builder.withConfigurable(anyTextBinding());
            }
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

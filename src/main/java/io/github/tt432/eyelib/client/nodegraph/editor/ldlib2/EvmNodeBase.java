package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if >=1.20.1 {
import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
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
            // ref 节点的 short_name 行不显示（用户决策 2026-08-07）：默认派生、显式值保留在
            // 选项数据里照常导出，UI 不再暴露（派生语义见 ShortNames.effective）
            if (isRef && ShortNames.SHORT_NAME_OPTION.equals(def.id())) {
                continue;
            }
            // 定长签名函数的 arg_count 无意义（端口数由签名决定），隐藏减少干扰
            if ("arg_count".equals(def.id())
                    && io.github.tt432.eyelib.nodegraph.MolangFunctionSignatures
                            .isFixedArity(java.util.Objects.toString(readStringOption("function"), ""))) {
                continue;
            }
            var builder = context.addOption(def.id(), EvmValues.optionJavaType(def.type()))
                    .withDefaultValue(EvmValues.optionDefault(def))
                    .withDisplayName(Component.literal(def.id()));
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

    /** 读字符串选项当前值（未设/缺失 → null）。 */
    private @Nullable String readStringOption(String id) {
        INodeOption option = getNodeOptionById(id);
        if (option == null) return null;
        return option.<String>tryGetValue(String.class).result().orElse(null);
    }

    /**
     * 资产候选配置器（规格 §4.2）：仅 EvmSelectorConfigurator 下拉（懒构建，见 LazySelector），
     * 宽度随选中内容自适应。不再保留自由输入行（用户决策 2026-08-07：标识符一律下拉选择）。
     */
    private static ITypeConfigurable assetSuggestions(String suggestionKey) {
        return (valueConfigurable, typeHandle) -> IConfigurable.create(father -> {
            java.util.function.Supplier<String> getter = valueConfigurable::getValue;
            java.util.function.Consumer<String> setter = valueConfigurable::setValue;
            String fallback = java.util.Objects.toString(valueConfigurable.getDefaultValue(), "");
            boolean forceUpdate = valueConfigurable.forceUpdate();
            father.addConfigurator(new EvmSelectorConfigurator<>("", getter, setter, fallback, forceUpdate,
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
                                valueConfigurable.getValue(), "")),
                        argb -> valueConfigurable.setValue(ColorValues.fromArgbInt(argb)),
                        ColorValues.toArgbInt(java.util.Objects.toString(
                                valueConfigurable.getDefaultValue(), "")),
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
                        () -> inlineTextOf(valueConfigurable.getValue()),
                        text -> valueConfigurable.setValue(EvmValues.portJsonToJava(
                                InlineLiteral.parse(text), PortType.ANY)),
                        inlineTextOf(valueConfigurable.getDefaultValue()),
                        valueConfigurable.forceUpdate())));
    }

    /** Java 值 → 行内显示文本；javaToJson 不可映射（null）时显示空串。 */
    private static String inlineTextOf(@Nullable Object value) {
        JsonElement json = EvmValues.javaToJson(value);
        return json == null ? "" : InlineLiteral.toText(json);
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
                    .withDisplayName(Component.literal(port.label().orElse(port.id())));
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

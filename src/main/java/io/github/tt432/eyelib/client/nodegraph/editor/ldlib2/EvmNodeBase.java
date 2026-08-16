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
import io.github.tt432.eyelib.client.nodegraph.DiagnosticsCenter;
import io.github.tt432.eyelib.client.nodegraph.MolangImplementations;
import io.github.tt432.eyelib.client.nodegraph.preview.PreviewViewState;
import io.github.tt432.eyelib.nodegraph.ColorValues;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.InlineLiteral;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeOptionDef;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortDef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.ShortNames;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 全部 eyelib 节点的 LDLib2 基类：子类只声明 {@link #type()}（domain 节点类型）。
 *
 * <p>选项：{@link NodeOptionDef} → LDLib2 节点选项（STRING/TEXT/ENUM/IDENTIFIER→String、
 * INT→Integer、FLOAT→Float、BOOL→Boolean）。动态端口节点（query/math/exec.call 的变长实参、
 * subgraph.call 的子图名）按 TestAddNode 模式在 {@link #onDefinePorts}
 * 读当前选项值重算端口——选项值变更时 LDLib2 会重跑 defineNode（OptionBuilder setterAction）。
 * （v14 起 op.* 一符一类型、无 op 选项，操作符节点已不再是动态端口节点。）
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
        var base = Component.literal(EvmNodes.displayName(type().id()));
        if (hasUnimplementedFunction()) {
            // 颜色样式可用：节点标题经 LDLib2 Label（TextElement）渲染，其文本管线
            // （TextUtilities.computeFormattedLines → LDLibFonts.drawText）在 vanilla
            // Font.StringRenderOutput 与 LDTextLayoutCache 两条路径上均以 Component 内嵌
            // style 颜色覆盖绘制色（已对照 1.20.1 Font 源码与 LDTextLayoutCache.emit 核实）
            base.append(Component.literal(" ⚠ 未实现").withStyle(ChatFormatting.RED));
        }
        return base;
    }

    /**
     * call 类节点的 function 是否指向未实现函数（图上错误标记与诊断上报的共用判定）；
     * 非 call 节点 / 空 function → false。早期 define 阶段 nodeModel 可能为 null——
     * {@link #effectiveFunction} 经 readStringOption 空安全回退 initialFunction，不会 NPE。
     */
    private boolean hasUnimplementedFunction() {
        NodeType.Kind kind = type().kind();
        if (kind != NodeType.Kind.QUERY_CALL && kind != NodeType.Kind.MATH_CALL
                && kind != NodeType.Kind.EXEC_CALL) {
            return false;
        }
        return !MolangImplementations.isImplemented(effectiveFunction());
    }

    /**
     * ref.geometry / ref.texture 节点启用 LDLib2 内建节点预览面板（{@code NodePreviewModel}，
     * {@code NodeElement.buildPreviewPart} 装配）；内容由 {@link EvmRefPreviewElement} 绘制。
     */
    @Override
    public boolean hasNodePreview() {
        return type().kind() == NodeType.Kind.REF_GEOMETRY || type().kind() == NodeType.Kind.REF_TEXTURE
                || type().kind() == NodeType.Kind.REF_PARTICLE || type().kind() == NodeType.Kind.REF_SOUND;
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
            // ac.state 的 name 行不显示（用户决策 2026-08-09：state 名仅内部使用——导出
            // states 键与 transition/initial 图边解析用，同 short_name 处理：数据保留照常导出）
            if (type().kind() == NodeType.Kind.AC_STATE && "name".equals(def.id())) {
                continue;
            }
            // v11：args 字面值列表行——定长函数不显示；零参内建（映射树可见参数 0）也不显示；
            // 变长与真正未知的函数显示（未知行兼作逃生舱，列表数据不丢）。注意 define 期
            // 选项值未必可读（LDLib2 两阶段加载：define 后才回写保存值）——首选实时读，
            // 读不到回退翻译期写入的 initialFunction（见 EvmGraphTranslator.createNode）
            if (def.type() == NodeOptionDef.OptionType.LIST) {
                String fn = effectiveFunction();
                String listLabel = io.github.tt432.eyelib.nodegraph.MolangFunctionSignatures
                        .variadicListLabel(fn);
                if (listLabel == null || ("args[...]".equals(listLabel) && isKnownZeroArgFunction(fn))) {
                    continue;
                }
                var builder = context.addOption(def.id(), String.class)
                        .withDefaultValue(def.defaultValue().toString())
                        .withDisplayName(Component.literal(listLabel));
                builder.withConfigurable(listBinding());
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

    /** 加载期 function 值兜底（翻译器在首个 defineNode 前写入；交互新建节点为 null）。 */
    private @Nullable String initialFunction;

    /** 翻译期注入域 function 值（define 期选项值不可读，见 onDefineOptions LIST 分支）。 */
    public void withInitialFunction(@Nullable String function) {
        this.initialFunction = function;
    }

    /** function 选项当前有效值：首选实时读，读不到/空串回退翻译期 initialFunction。 */
    private String effectiveFunction() {
        String fn = readStringOption("function");
        if (fn == null || fn.isEmpty()) {
            fn = initialFunction != null ? initialFunction : "";
        }
        return fn;
    }

    /** 零参内建函数（映射树可见参数 0）→ args 列表行不显示。 */
    private static boolean isKnownZeroArgFunction(String fn) {
        int dot = fn.indexOf('.');
        if (dot < 0) {
            return false;
        }
        var rootNode = io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries
                .mappingTree().toplevelNode.children.get(fn.substring(0, dot));
        if (rootNode == null) {
            return false;
        }
        var infos = rootNode.actualFunctions.get(fn.substring(dot + 1));
        if (infos == null || infos.isEmpty()) {
            return false;
        }
        return infos.get(0).parameterRoles().stream()
                .noneMatch(role -> role.role()
                        == io.github.tt432.eyelib.molang.mapping.api.MolangFunction.ParameterRole.VISIBLE_ARG);
    }

    /** args 列表选项的节点内列表编辑器绑定（底层值 = JSON 数组文本）。 */
    private static com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable listBinding() {
        return (valueConfigurable, typeHandle) -> IConfigurable.create(father ->
                        father.addConfigurator(new EvmListConfigurator(
                                valueConfigurable::getValue,
                                valueConfigurable::setValue,
                                java.util.Objects.toString(valueConfigurable.getDefaultValue(), "[]"),
                                valueConfigurable.forceUpdate())));
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
            var builder = context.addOutputPort(port.id(), EvmTypeHandles.toHandle(effectiveOutType(port)))
                    .withDisplayName(Component.literal(port.label().orElse(port.id())));
            applyCapacity(builder.build(), port);
        }
        // 未实现函数标记回写标题：CustomNodeModelImpl 只在 initCustomNode 时 setTitle 一次，
        // function 选项变更触发的 defineNode 重跑需在此刷新标题（setTitle 内部等值短路，
        // 且经 ChangeHint.STYLE 让 NodeTitleElement 更新 Label 文本）
        var model = getNodeModel();
        if (model != null) {
            model.setTitle(getDisplayName());
        }
        reportUnimplementedDiagnostic();
    }

    /** 上一次检查的 function 值（变化时重新武装诊断上报）。 */
    private @Nullable String lastCheckedFunction;
    /** 上一次已上报诊断的 function 值（同一值只报一次，防 defineNode 重跑刷屏）。 */
    private @Nullable String lastReportedUnimplementedFunction;

    /**
     * 未实现函数的 DiagnosticsCenter 上报（挂在 onDefinePorts 末尾，defineNode 重跑时执行）。
     * 去重语义：function 值不变只报一次；function 变化（含变成已实现再变回）重新武装、可再报。
     * 注意 DiagnosticsCenter 只保留最新一批——这里是节点级增量上报，会被下一批覆盖。
     */
    private void reportUnimplementedDiagnostic() {
        NodeType.Kind kind = type().kind();
        if (kind != NodeType.Kind.QUERY_CALL && kind != NodeType.Kind.MATH_CALL
                && kind != NodeType.Kind.EXEC_CALL) {
            return;
        }
        String fn = effectiveFunction();
        if (!fn.equals(lastCheckedFunction)) {
            lastCheckedFunction = fn;
            lastReportedUnimplementedFunction = null;
        }
        if (fn.isEmpty() || MolangImplementations.isImplemented(fn)) {
            return;
        }
        if (fn.equals(lastReportedUnimplementedFunction)) {
            return;
        }
        lastReportedUnimplementedFunction = fn;
        var model = getNodeModel();
        String uid = model != null && model.getUid() != null ? model.getUid().toString() : null;
        String message = "molang 函数未实现: " + fn + "（节点类型 " + type().id() + "）";
        DiagnosticsCenter.report("evm-nodegraph", "节点 " + type().id(),
                List.of(uid != null
                        ? Diagnostic.error("MOLANG_UNIMPLEMENTED", message, uid)
                        : Diagnostic.error("MOLANG_UNIMPLEMENTED", message)));
    }

    /**
     * call 类节点 out 端口的有效类型（v13）：query.call/math.call 按函数返回类型显示
     * （{@link MolangReturnTypes}），替代 domain 静态 ANY——类型随 function 选项变更
     * 重跑 defineNode 自动刷新；其余端口保持 domain 声明类型。
     */
    private PortType effectiveOutType(PortDef port) {
        NodeType.Kind kind = type().kind();
        if ((kind == NodeType.Kind.QUERY_CALL || kind == NodeType.Kind.MATH_CALL)
                && "out".equals(port.id())) {
            String fn = effectiveFunction();
            return io.github.tt432.eyelib.client.nodegraph.MolangReturnTypes.returnTypeOf(fn);
        }
        return port.type();
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
        // var_refs 快照无 NodeOptionDef（不在上面循环）——从侧表合并（ref.animation 命名
        // 变量端口的驱动源；不合并则端口在编辑器不渲染、保存时丢失，规格 §2.1）
        var nodeModel = getNodeModel();
        EvmGraph graph = evmGraph();
        if (nodeModel != null && graph != null && graph.context() != null) {
            com.google.gson.JsonElement varRefs = graph.context().varRefs.get(nodeModel.getUid());
            if (varRefs != null) {
                options.put(NodeTypes.VAR_REFS_OPTION, varRefs);
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

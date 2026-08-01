//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.configurator.BooleanConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.NumberConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.SelectorConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.StringConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.CustomPortBehavior;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import io.github.tt432.eyelib.client.nodegraph.preview.NodeAssetPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.PortData;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.PortEdge;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeOptionDef;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortDef;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 通用 EVM 节点：一个 BaseNode 子类承载 {@link NodeTypes} 目录中的全部节点类型。
 *
 * <p>graphprocessor 的端口必须挂在真实 public 字段上（{@code NodePort} 构造时反射
 * {@code getField(fieldName)} 校验），逐类型写字段无法覆盖动态端口，故采用单一承载字段方案：
 * 全部输入端口挂在 {@link #in}、全部输出端口挂在 {@link #out} 上，经
 * {@link CustomPortBehavior} 按 {@link NodeType} 的端口提供策略产出
 * {@code PortData.identifier = PortDef.id}——域端口 id 由此与画布端口一一对应。
 *
 * <p>选项（{@link NodeOptionDef}）与未连接输入内联值（constants）经
 * {@link #buildConfigurator} 渲染为节点内容区控件；选项变更后调
 * {@code updatePortsForField} 重算动态端口并刷新节点 widget。
 *
 * <p>本类不出现在 {@code @LDLRegister} 自动扫描中（一个类对应多个注册名），
 * 由 {@link EvmNodeRegistration} 按域节点类型逐个显式注册。
 */
public class EvmNode extends BaseNode {
    /** 全部输入端口的承载字段（端口 id 见 PortData.identifier）。 */
    @InputPort
    public Object in;
    /** 全部输出端口的承载字段（端口 id 见 PortData.identifier）。 */
    @OutputPort
    public Object out;

    /** 域节点类型 id（{@link NodeTypes} 注册表键）。 */
    public final String nodeTypeId;
    /** 选项值（键 = 选项 id；未设的取类型默认）。 */
    public final Map<String, JsonElement> options = new LinkedHashMap<>();
    /** 未连接输入端口的内联值（键 = 端口 id）。 */
    public final Map<String, JsonElement> constants = new LinkedHashMap<>();
    /** 域 uid（空串 = 画布新建，回译时取 GUID）。 */
    public String evmUid = "";
    /** 子图接口解析器（构造时由会话提供；不序列化）。 */
    public final transient NodeType.SubgraphResolver resolver;
    /** 选项变更后的 widget 刷新钩子（由 EvmGraphViewWidget 装配；不序列化）。 */
    public transient @Nullable Runnable uiRefresh;

    public EvmNode(String nodeTypeId, NodeType.SubgraphResolver resolver) {
        this.nodeTypeId = nodeTypeId;
        this.resolver = resolver;
        // BaseNode 字段初始化器 displayName = name() 运行时 nodeTypeId 尚未赋值，这里修正
        this.displayName = nodeTypeId;
        this.titleColor = categoryColor(nodeType() == null ? "" : nodeType().category());
    }

    // ---------- ILDLRegister（一个类多个注册名，覆盖实例级语义） ----------

    @Override
    public String name() {
        return nodeTypeId == null ? "evm.unknown" : nodeTypeId;
    }

    @Override
    public String group() {
        NodeType type = nodeType();
        return EvmNodeRegistration.GROUP_PREFIX + "." + (type == null ? "misc" : type.category());
    }

    public @Nullable NodeType nodeType() {
        return NodeTypes.get(nodeTypeId).orElse(null);
    }

    /** 读字符串选项（实例值优先，缺省给默认）。 */
    public String optionString(String id, String defaultValue) {
        JsonElement own = options.get(id);
        return own instanceof JsonPrimitive primitive && primitive.isString()
                ? primitive.getAsString() : defaultValue;
    }

    // ---------- 动态端口 ----------

    @CustomPortBehavior(field = "in")
    public List<PortData> evmInputPorts(List<PortEdge> edges) {
        return portDatas(true);
    }

    @CustomPortBehavior(field = "out")
    public List<PortData> evmOutputPorts(List<PortEdge> edges) {
        return portDatas(false);
    }

    private List<PortData> portDatas(boolean input) {
        NodeType type = nodeType();
        if (type == null) return List.of();
        NodeInstance instance = selfInstance();
        List<PortDef> defs = input ? type.inputsOf(instance, resolver) : type.outputsOf(instance, resolver);
        List<PortData> result = new ArrayList<>(defs.size());
        for (PortDef def : defs) {
            result.add(new PortData()
                    .identifier(def.id())
                    .displayName(def.id())
                    .displayType(EvmLinks.toClass(def.type()))
                    .acceptMultipleEdges(def.multi()));
        }
        return result;
    }

    /** 当前实例状态投影（动态端口推导只读 options/constants）。 */
    private NodeInstance selfInstance() {
        return new NodeInstance(evmUid, nodeTypeId, 0, 0, options, constants);
    }

    /** 选项变更后：重算动态端口（含跨节点传播）+ 刷新节点 widget。 */
    public void refreshDynamicPorts() {
        updatePortsForField("in");
        updatePortsForField("out");
        if (uiRefresh != null) {
            uiRefresh.run();
        }
    }

    // ---------- 节点内容区（选项 + 内联常量） ----------

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        NodeType type = nodeType();
        if (type == null) return;
        for (NodeOptionDef option : type.options()) {
            buildOptionConfigurator(father, option);
        }
        for (PortDef input : type.inputsOf(selfInstance(), resolver)) {
            input.defaultValue().ifPresent(def -> buildConstantConfigurator(father, input, def));
        }
        if (type == NodeTypes.REF_GEOMETRY || type == NodeTypes.REF_TEXTURE) {
            father.addConfigurators(new WrapperConfigurator("preview", new ImageWidget(0, 0, PREVIEW_SIZE, PREVIEW_SIZE,
                    (IGuiTexture) (graphics, mouseX, mouseY, x, y, w, h) -> drawRefPreview(graphics, x, y, w, h))));
        }
    }

    // ---------- 资源引用预览（规格 §3.3） ----------

    private static final int PREVIEW_SIZE = 64;
    private static final ResourceLocation CHECKERBOARD =
            new ResourceLocation("eyelib", "textures/gui/nodegraph/checkerboard.png");

    /** 每帧重读选项值绘制预览（选项编辑后自动同步；引用缺失画「未找到」占位）。 */
    private void drawRefPreview(GuiGraphics graphics, float fx, float fy, float fw, float fh) {
        int x = (int) fx, y = (int) fy, w = (int) fw, h = (int) fh;
        // 棋盘格底衬透明
        graphics.blit(CHECKERBOARD, x, y, 0, 0, w, h, w, h);
        NodeType type = nodeType();
        if (type == NodeTypes.REF_TEXTURE) {
            ResourceLocation texture = NodeAssetPreview.resolveTexture(optionWithDefault("path"));
            if (texture == null) {
                drawNotFound(graphics, x, y, w, h);
            } else {
                graphics.blit(texture, x, y, 0, 0, w, h, w, h);
            }
        } else if (type == NodeTypes.REF_GEOMETRY) {
            NodeAssetPreview.ModelHandle handle = NodeAssetPreview.resolveModel(optionWithDefault("identifier"));
            if (handle == null) {
                drawNotFound(graphics, x, y, w, h);
            } else {
                NodeAssetPreview.renderModel(handle, graphics, x, y, w, h, 0f);
            }
        }
    }

    private static void drawNotFound(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.drawCenteredString(Minecraft.getInstance().font, "未找到", x + w / 2, y + h / 2 - 4, 0xFFFF5555);
    }

    /** 读字符串选项；节点未显式设置时回退到 {@link NodeOptionDef} 默认值。 */
    private String optionWithDefault(String id) {
        NodeType type = nodeType();
        if (type != null) {
            for (NodeOptionDef def : type.options()) {
                if (def.id().equals(id)) {
                    return optionString(id, def.defaultValue().getAsString());
                }
            }
        }
        return optionString(id, "");
    }

    private void buildOptionConfigurator(ConfiguratorGroup father, NodeOptionDef option) {
        String id = option.id();
        JsonElement current = options.getOrDefault(id, option.defaultValue());
        switch (option.type()) {
            case STRING, TEXT, IDENTIFIER -> father.addConfigurators(new StringConfigurator(
                    id, () -> options.getOrDefault(id, option.defaultValue()).getAsString(),
                    v -> setOption(id, new JsonPrimitive(v)), option.defaultValue().getAsString(), true));
            case FLOAT -> father.addConfigurators(new NumberConfigurator(
                    id, () -> options.getOrDefault(id, option.defaultValue()).getAsFloat(),
                    v -> setOption(id, new JsonPrimitive(v.floatValue())), current.getAsFloat(), true));
            case INT -> father.addConfigurators(new NumberConfigurator(
                    id, () -> options.getOrDefault(id, option.defaultValue()).getAsInt(),
                    v -> setOption(id, new JsonPrimitive(v.intValue())), current.getAsInt(), true));
            case BOOL -> father.addConfigurators(new BooleanConfigurator(
                    id, () -> options.getOrDefault(id, option.defaultValue()).getAsBoolean(),
                    v -> setOption(id, new JsonPrimitive(v)), current.getAsBoolean(), true));
            case ENUM -> father.addConfigurators(new SelectorConfigurator<>(
                    id, () -> options.getOrDefault(id, option.defaultValue()).getAsString(),
                    v -> setOption(id, new JsonPrimitive(v)), option.defaultValue().getAsString(), true,
                    option.choices().orElse(List.of()), Function.identity()));
        }
    }

    private void setOption(String id, JsonElement value) {
        options.put(id, value);
        refreshDynamicPorts();
    }

    private void buildConstantConfigurator(ConfiguratorGroup father, PortDef port, JsonElement def) {
        String id = port.id();
        if (def instanceof JsonPrimitive primitive && primitive.isBoolean()) {
            father.addConfigurators(new BooleanConfigurator(
                    id, () -> constants.getOrDefault(id, def).getAsBoolean(),
                    v -> constants.put(id, new JsonPrimitive(v)), def.getAsBoolean(), true));
        } else if (def instanceof JsonPrimitive primitive && primitive.isNumber()) {
            father.addConfigurators(new NumberConfigurator(
                    id, () -> constants.getOrDefault(id, def).getAsFloat(),
                    v -> constants.put(id, new JsonPrimitive(v.floatValue())), def.getAsFloat(), true));
        } else if (def instanceof JsonPrimitive primitive && primitive.isString()) {
            father.addConfigurators(new StringConfigurator(
                    id, () -> constants.getOrDefault(id, def).getAsString(),
                    v -> constants.put(id, new JsonPrimitive(v)), def.getAsString(), true));
        }
    }

    // ---------- NBT（仅服务画布内复制/粘贴；权威格式是本域 JSON） ----------

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putString("evm_uid", evmUid);
        tag.putString("evm_options", toJson(options));
        tag.putString("evm_constants", toJson(constants));
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        if (tag.contains("evm_uid")) {
            evmUid = tag.getString("evm_uid");
        }
        if (tag.contains("evm_options")) {
            fromJson(tag.getString("evm_options"), options);
        }
        if (tag.contains("evm_constants")) {
            fromJson(tag.getString("evm_constants"), constants);
        }
    }

    private static String toJson(Map<String, JsonElement> map) {
        JsonObject obj = new JsonObject();
        map.forEach(obj::add);
        return obj.toString();
    }

    private static void fromJson(String json, Map<String, JsonElement> into) {
        into.clear();
        for (Map.Entry<String, JsonElement> entry : JsonParser.parseString(json).getAsJsonObject().entrySet()) {
            into.put(entry.getKey(), entry.getValue());
        }
    }

    private static int categoryColor(String category) {
        return switch (category) {
            case NodeTypes.CAT_CONSTANT -> ColorPattern.GREEN.color;
            case NodeTypes.CAT_VARIABLE -> ColorPattern.CYAN.color;
            case NodeTypes.CAT_QUERY -> ColorPattern.BLUE.color;
            case NodeTypes.CAT_OPERATOR -> ColorPattern.PURPLE.color;
            case NodeTypes.CAT_EXEC -> ColorPattern.YELLOW.color;
            case NodeTypes.CAT_REF -> ColorPattern.LIME.color;
            case NodeTypes.CAT_ENTITY -> ColorPattern.ORANGE.color;
            case NodeTypes.CAT_RC -> ColorPattern.MAGENTA.color;
            case NodeTypes.CAT_AC -> ColorPattern.PINK.color;
            case NodeTypes.CAT_SUBGRAPH -> ColorPattern.BROWN.color;
            default -> ColorPattern.GRAY.color;
        };
    }
}
//?}

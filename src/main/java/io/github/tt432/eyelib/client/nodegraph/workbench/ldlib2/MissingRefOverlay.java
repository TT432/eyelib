package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if >=1.20.1 {

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ICustomNodeModel;
import dev.vfyjxf.taffy.style.TaffyPosition;
import io.github.tt432.eyelib.client.nodegraph.MissingRefCheck;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmNodeBase;
import io.github.tt432.eyelib.nodegraph.NodeType;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缺失引用红框 overlay（规格 nodegraph-missing-refs §3）：
 * 与 {@link BadgeOverlay} 同通道（GraphView.canvas 最上层、屏幕空间、不拦截鼠标），
 * 每帧枚举当前图节点的 ref 选项值，{@link MissingRefCheck} 判定不存在即在节点
 * 外缘画红色描边（边框宽不随画布缩放变形——纯屏幕空间重绘）。
 *
 * <p>占位 ref（选项值等于 NodeOptionDef 默认值）不报；判定结果 500ms 记忆
 * （注册表整体替换后最长 500ms 自愈）。
 */
final class MissingRefOverlay extends UIElement {
    private final GraphView toolkitView;
    private final IGuiTexture border = new ColorBorderTexture(0xFFFF5555, 0x00FFFFFF);

    /** ref 节点 kind → 引用值选项 id（与 GraphValidator.REF_VALUE_OPTIONS 同口径）。 */
    private static final Map<NodeType.Kind, String> REF_OPTION_IDS = Map.of(
            NodeType.Kind.REF_GEOMETRY, "identifier",
            NodeType.Kind.REF_TEXTURE, "path",
            NodeType.Kind.REF_MATERIAL, "material",
            NodeType.Kind.REF_ANIMATION, "identifier",
            NodeType.Kind.REF_AC, "identifier",
            NodeType.Kind.REF_PARTICLE, "identifier",
            NodeType.Kind.REF_SOUND, "identifier",
            NodeType.Kind.REF_RC, "identifier");

    private record Rect(float x, float y, float w, float h) {
    }

    private final List<Rect> draws = new ArrayList<>();
    private final Map<String, long[]> checkMemo = new HashMap<>(); // key → [millis, 0/1]
    private float viewX;
    private float viewY;
    private float viewW;
    private float viewH;

    private MissingRefOverlay(GraphView toolkitView) {
        this.toolkitView = toolkitView;
        setAllowHitTest(false);
        layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0).top(0)
                .widthPercent(100)
                .heightPercent(100));
    }

    /** 挂到指定 GraphView 的画布层（root 图视图；潜入子图时 root 视图脱离 DOM，红框自然隐藏）。 */
    static MissingRefOverlay attach(GraphView toolkitView) {
        MissingRefOverlay overlay = new MissingRefOverlay(toolkitView);
        toolkitView.canvas.addChild(overlay);
        return overlay;
    }

    private void collect() {
        draws.clear();
        var g = toolkitView.graphView;
        viewX = g.getPositionX();
        viewY = g.getPositionY();
        viewW = g.getSizeWidth();
        viewH = g.getSizeHeight();
        float offsetX = g.getOffsetX();
        float offsetY = g.getOffsetY();
        float scale = g.getScale();

        for (AbstractNodeModel nodeModel : toolkitView.getGraph().graphModel.getNodeModels()) {
            if (!(nodeModel instanceof ICustomNodeModel custom)
                    || !(custom.getNode() instanceof EvmNodeBase node)) {
                continue;
            }
            String optionId = REF_OPTION_IDS.get(node.type().kind());
            if (optionId == null) {
                continue;
            }
            String value = "";
            var option = node.getNodeOptionById(optionId);
            if (option != null) {
                value = option.tryGetValue(String.class).result().map(Object::toString).orElse("");
            }
            if (value.isBlank() || value.equals(defaultOf(node, optionId))) {
                continue; // 占位 ref 不报
            }
            if (!isMissing(node.type().id(), value)) {
                continue;
            }
            ModelElement element = toolkitView.getModelElement(nodeModel.getUid());
            if (element == null) {
                continue;
            }
            Vector2f pos = nodeModel.getPosition();
            float x = viewX + (pos.x - offsetX) * scale;
            float y = viewY + (pos.y - offsetY) * scale;
            float w = element.getSizeWidth() * scale;
            float h = element.getSizeHeight() * scale;
            if (x + w < viewX || x > viewX + viewW || y + h < viewY || y > viewY + viewH) {
                continue;
            }
            draws.add(new Rect(x, y, w, h));
        }
    }

    private String defaultOf(EvmNodeBase node, String optionId) {
        return node.type().options().stream()
                .filter(d -> d.id().equals(optionId)).findFirst()
                .map(d -> d.defaultValue().getAsString()).orElse("");
    }

    private boolean isMissing(String typeId, String value) {
        String key = typeId + '\0' + value;
        long now = System.currentTimeMillis();
        long[] memo = checkMemo.get(key);
        if (memo != null && now - memo[0] < 500) {
            return memo[1] == 1;
        }
        boolean missing = !MissingRefCheck.INSTANCE.exists(typeId, value);
        checkMemo.put(key, new long[]{now, missing ? 1 : 0});
        return missing;
    }

    //? if modern {
    @Override
    protected void drawBackgroundAdditional(com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext context) {
        collect();
        for (Rect rect : draws) {
            context.drawTexture(border, rect.x(), rect.y(), rect.w(), rect.h());
        }
    }
    //?} else {
    @Override
    public void drawBackgroundAdditional(com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext guiContext) {
        collect();
        for (Rect rect : draws) {
            guiContext.drawTexture(border, rect.x(), rect.y(), rect.w(), rect.h());
        }
    }
    //?}
}
//?}

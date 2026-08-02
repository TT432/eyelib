package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if !legacy {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import dev.vfyjxf.taffy.style.TaffyPosition;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmGraphTranslator;
import io.github.tt432.eyelib.client.nodegraph.workbench.NodeDebugOverlayModel;
import io.github.tt432.eyelib.client.nodegraph.workbench.NodeDebugOverlayModel.Badge;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 画布节点值徽标 overlay（规格 §W3，与 ldlib1 工作台逐值对齐）：
 * 徽标画在节点底边 +2px 处，半透明黑底 pill + 类型着色文本；截断提示画在画布左上角。
 *
 * <p>作为 toolkit {@link GraphView#canvas} 的最上层子元素（屏幕空间、
 * {@code setAllowHitTest(false)} 不拦截任何鼠标事件），每帧在
 * {@code drawBackgroundAdditional} 里 {@link NodeDebugOverlayModel#tick()} 求值后绘制。
 * 节点图坐标 → 屏幕坐标经内层 ui GraphView 的 offset/scale 换算
 * （{@code screen = view + (graph - offset) * scale}），文本恒为屏幕大小（不随缩放变形）。
 *
 * <p>UDF 差异：1.21.1 的绘制入口是 {@code public drawBackgroundAdditional(GUIContext)}；
 * 26.1 改为 {@code protected drawBackgroundAdditional(IGUIContext)}（IGUIContext 仅 26.1 存在，
 * 签名全限定名内联），文本绘制经 {@link WorkbenchFonts} 分支。坐标收集（{@link #collect()}）两版共享。
 */
final class BadgeOverlay extends UIElement {
    private final NodeDebugOverlayModel model;
    private final GraphView toolkitView;
    /** 复用的 pill 底色纹理（逐帧复用同一实例，避免分配）。 */
    private final ColorRectTexture pill = new ColorRectTexture(WorkbenchColors.BADGE_PILL_BG);

    /** 一个待绘制徽标（屏幕坐标 + 预计算文本宽）。 */
    private record BadgeDraw(float x, float y, float width, String text, int color) {
    }

    private final List<BadgeDraw> draws = new ArrayList<>();
    private boolean showTruncated;
    private float viewX;
    private float viewY;

    private BadgeOverlay(NodeDebugOverlayModel model, GraphView toolkitView) {
        this.model = model;
        this.toolkitView = toolkitView;
        setAllowHitTest(false);
        layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0).top(0)
                .widthPercent(100)
                .heightPercent(100));
    }

    /** 挂到指定 GraphView 的画布层（root 图视图；潜入子图时 root 视图脱离 DOM，徽标自然隐藏）。 */
    static BadgeOverlay attach(GraphView toolkitView, NodeDebugOverlayModel model) {
        BadgeOverlay overlay = new BadgeOverlay(model, toolkitView);
        toolkitView.canvas.addChild(overlay);
        return overlay;
    }

    /** 每帧收集：模型求值 → 节点图坐标换算屏幕坐标 → 可视区裁剪。 */
    private void collect() {
        draws.clear();
        model.tick();
        var g = toolkitView.graphView;
        viewX = g.getPositionX();
        viewY = g.getPositionY();
        float viewW = g.getSizeWidth();
        float viewH = g.getSizeHeight();
        float offsetX = g.getOffsetX();
        float offsetY = g.getOffsetY();
        float scale = g.getScale();

        for (Map.Entry<String, Badge> entry : model.badges().entrySet()) {
            ModelElement element = toolkitView.getModelElement(EvmGraphTranslator.uidOf(entry.getKey()));
            if (element == null || !(element.getModel() instanceof AbstractNodeModel nodeModel)) {
                continue;
            }
            Vector2f pos = nodeModel.getPosition();
            float x = viewX + (pos.x - offsetX) * scale;
            float y = viewY + (pos.y + element.getSizeHeight() + 2 - offsetY) * scale;
            Badge badge = entry.getValue();
            float width = WorkbenchFonts.width(badge.text());
            // 画布可视区裁剪（整体落在视图外即跳过）
            if (x + width < viewX || x > viewX + viewW || y + 10 < viewY || y > viewY + viewH) {
                continue;
            }
            draws.add(new BadgeDraw(x, y, width, badge.text(),
                    WorkbenchColors.badgeColor(badge.error(), badge.type())));
        }
        showTruncated = model.truncated();
    }

    //? if modern {
    @Override
    protected void drawBackgroundAdditional(com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext context) {
        collect();
        GUIContext guiContext = context instanceof GUIContext gc ? gc : null;
        for (BadgeDraw draw : draws) {
            context.drawTexture(pill, draw.x() - 2, draw.y() - 1, draw.width() + 4, 11);
            if (guiContext != null) {
                WorkbenchFonts.draw(guiContext.graphics, draw.text(), draw.x(), draw.y() + 1, draw.color());
            }
        }
        if (showTruncated && guiContext != null) {
            WorkbenchFonts.draw(guiContext.graphics,
                    "节点过多，徽标已截断（>" + NodeDebugOverlayModel.MAX_BADGES + "）",
                    viewX + 4, viewY + 4, WorkbenchColors.DIM);
        }
    }
    //?} else {
    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        collect();
        for (BadgeDraw draw : draws) {
            guiContext.drawTexture(pill, draw.x() - 2, draw.y() - 1, draw.width() + 4, 11);
            WorkbenchFonts.draw(guiContext.graphics, draw.text(), draw.x(), draw.y() + 1, draw.color());
        }
        if (showTruncated) {
            WorkbenchFonts.draw(guiContext.graphics,
                    "节点过多，徽标已截断（>" + NodeDebugOverlayModel.MAX_BADGES + "）",
                    viewX + 4, viewY + 4, WorkbenchColors.DIM);
        }
    }
    //?}
}
//?}

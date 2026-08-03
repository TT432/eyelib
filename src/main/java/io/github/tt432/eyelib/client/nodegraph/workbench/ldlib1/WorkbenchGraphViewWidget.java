//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.NodeWidget;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.FreeGraphView;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib1.EvmBaseGraph;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib1.EvmGraphViewWidget;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib1.EvmNode;
import io.github.tt432.eyelib.client.nodegraph.workbench.NodeDebugOverlayModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 带节点值徽标的 EVM 画布（规格 §W3）：在 {@link EvmGraphViewWidget} 之上每帧
 * {@link NodeDebugOverlayModel#tick()} 求值，并把 {@link NodeDebugOverlayModel#badges()}
 * 画到对应节点 widget 下方（UE 蓝图式画布调试）。
 *
 * <p>徽标层是 FreeGraphView 的最后一个子 widget（随 loadGraph 重建后重新挂到最末），
 * 因此：绘制在节点之上、被画布浮动面板（节点面板/参数面板）与右侧侧栏自然遮挡、
 * 被 FreeGraphView 的 scissor 裁剪在画布内。徽标在视图坐标系下绘制并反向缩放
 * 文字（{@code 1/scale}），缩放画布时文字保持屏幕等宽。
 */
public final class WorkbenchGraphViewWidget extends EvmGraphViewWidget {
    private static final int COLOR_BADGE_BG = 0xA0101010;
    private static final int COLOR_TRUNCATED = 0xFFFFFF55;
    private static final float BADGE_SCALE = 0.8f;

    private final NodeDebugOverlayModel overlayModel;
    /** 徽标层（超类构造触发 loadGraph 时尚未创建，故可空并在 loadGraph 中判空）。 */
    private @Nullable BadgeLayer badgeLayer;

    public WorkbenchGraphViewWidget(NodeDebugOverlayModel overlay, EvmBaseGraph graph,
                                    int x, int y, int width, int height) {
        super(graph, x, y, width, height);
        this.overlayModel = overlay;
        badgeLayer = new BadgeLayer();
        getFreeGraphView().addWidget(badgeLayer);
    }

    @Override
    public void loadGraph() {
        super.loadGraph();
        // loadGraph 会 clearAllWidgets 后重挂节点，把徽标层重新抬到最末（节点之上）
        if (badgeLayer != null) {
            getFreeGraphView().addWidget(badgeLayer);
        }
        fitToContent();
    }

    /**
     * 视口适配内容：全部节点包围盒居中，缩放收敛 [0.15, 1]。
     * 不用 LDLib 内建 fit（缩放下限 0.5，对导入的大图≈无效）——用户报告
     * 「导入后没有节点」的根因就是视口停在原点而图布局在远处（规格 §W2 布局）。
     */
    public void fitToContent() {
        var nodes = getNodeMap();
        if (nodes.isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        int anchorX = 0, anchorY = 0;
        for (var entry : nodes.entrySet()) {
            Position pos = entry.getKey().position;
            if (pos == null) {
                continue;
            }
            minX = Math.min(minX, pos.x);
            minY = Math.min(minY, pos.y);
            if (pos.x + entry.getValue().getSizeWidth() > maxX) {
                anchorX = pos.x + entry.getValue().getSizeWidth() / 2;
                anchorY = pos.y + entry.getValue().getSizeHeight() / 2;
            }
            maxX = Math.max(maxX, pos.x + entry.getValue().getSizeWidth());
            maxY = Math.max(maxY, pos.y + entry.getValue().getSizeHeight());
        }
        if (minX > maxX) {
            return;
        }
        float pad = 60;
        float w = maxX - minX + pad * 2;
        float h = maxY - minY + pad * 2;
        FreeGraphView fgv = getFreeGraphView();
        float scale = Math.min(1f, Math.min(getSizeWidth() / w, getSizeHeight() / h));
        if (scale >= 0.15f) {
            fgv.setScale(scale);
            fgv.setXOffset(minX - pad + (w - getSizeWidth() / scale) / 2);
            fgv.setYOffset(minY - pad + (h - getSizeHeight() / scale) / 2);
            return;
        }
        // 图过大（如导入的实体图）：全图缩到可读缩放无意义——适配装配根邻域
        // （布局不变式：装配根在最右列 = max-x 节点），用户从汇开始向外导航。
        fitToRegion(anchorX, anchorY);
    }

    /** 以 (anchorX, anchorY) 为中心的邻域适配（约 1200×800 视图单位，缩放收敛 [0.15, 1]）。 */
    private void fitToRegion(int anchorX, int anchorY) {
        float w = 1200, h = 800;
        float scale = Math.min(1f, Math.min(getSizeWidth() / w, getSizeHeight() / h));
        scale = Math.max(0.15f, scale);
        FreeGraphView fgv = getFreeGraphView();
        fgv.setScale(scale);
        fgv.setXOffset(anchorX - getSizeWidth() / scale / 2);
        fgv.setYOffset(anchorY - getSizeHeight() / scale / 2);
    }

    /** 徽标绘制层：零尺寸（不参与命中），在 FreeGraphView 视图坐标系内绘制。 */
    private final class BadgeLayer extends Widget {
        BadgeLayer() {
            super(0, 0, 0, 0);
        }

        @Override
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            overlayModel.tick();
            Map<String, NodeDebugOverlayModel.Badge> badges = overlayModel.badges();
            FreeGraphView freeGraphView = getFreeGraphView();
            float scale = freeGraphView.getScale();
            var font = Minecraft.getInstance().font;

            for (Map.Entry<BaseNode, NodeWidget> entry : getNodeMap().entrySet()) {
                NodeDebugOverlayModel.Badge badge = badges.get(uidOf(entry.getKey()));
                if (badge == null) {
                    continue;
                }
                Position position = entry.getKey().position;
                if (position == null) {
                    continue;
                }
                // 徽标锚点：节点 widget 左下角下方 2px（视图坐标）
                drawBadge(graphics, font, position.x + 2,
                        position.y + entry.getValue().getSizeHeight() + 2, scale, badge);
            }

            if (overlayModel.target() != null && overlayModel.truncated()) {
                // 视口左上角（视图坐标 = offset + 屏幕边距/scale）
                drawBadgeText(graphics, font,
                        freeGraphView.getXOffset() + 4 / scale, freeGraphView.getYOffset() + 4 / scale,
                        scale, "徽标已截断（>" + NodeDebugOverlayModel.MAX_BADGES + " 个节点）",
                        COLOR_TRUNCATED, true);
            }
        }

        private void drawBadge(GuiGraphics graphics, net.minecraft.client.gui.Font font,
                               int viewX, int viewY, float scale, NodeDebugOverlayModel.Badge badge) {
            int color = badge.error() ? MolangTypeColors.ERROR : MolangTypeColors.of(badge.type());
            drawBadgeText(graphics, font, viewX, viewY, scale, badge.text(), color, false);
        }

        /** 反向缩放绘制：无论画布缩放如何，文字保持屏幕等宽（{@link #BADGE_SCALE}）。 */
        private void drawBadgeText(GuiGraphics graphics, net.minecraft.client.gui.Font font,
                                   float viewX, float viewY, float scale,
                                   String text, int color, boolean shadow) {
            graphics.pose().pushPose();
            graphics.pose().translate(viewX, viewY, 0);
            graphics.pose().scale(1f / scale, 1f / scale, 1f);
            int textWidth = (int) (font.width(text) * BADGE_SCALE);
            DrawerHelper.drawSolidRect(graphics, -2, -1, textWidth + 4, 9, COLOR_BADGE_BG);
            DrawerHelper.drawText(graphics, text, 0, 0, BADGE_SCALE, color, shadow);
            graphics.pose().popPose();
        }
    }

    /** 与 {@code Ldlib1GraphTranslator.uidOf} 同一规则：域 uid 优先，画布新建取 GUID。 */
    private static String uidOf(BaseNode node) {
        if (node instanceof EvmNode evm && !evm.evmUid.isEmpty()) {
            return evm.evmUid;
        }
        return node.getGUID();
    }
}
//?}

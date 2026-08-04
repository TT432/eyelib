package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if !legacy {
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import io.github.tt432.eyelib.client.nodegraph.DiagnosticsCenter;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmGraphTranslator;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 诊断浮动面板（规格 nodegraph-declaration-wiring §4.3，IDEA Problems 工具窗口风格）：
 * 左下角常驻「问题」开关按钮 + E/W 徽标（0/0 置灰）；点击开关浮动面板
 * （360×220，位于按钮上方）：标题栏（来源 + 时间 + E/W 统计 + ✕ 关闭）+ 可滚动列表
 * （节 label 作分组头；行 = 严重级色点 + code + message）。
 *
 * <p>数据取 {@link DiagnosticsCenter} 最新一批（只展示当前问题，不留历史）：构造时读
 * {@link DiagnosticsCenter#latest()}，之后经 listener 自动刷新（{@link #onRemoved()}
 * 摘除监听，屏幕关闭后不滞留引用）。不自动弹出。
 * 行带 nodeUid 时点击 → 画布选中并居中该节点（{@link #focusNode}，best effort）。
 */
final class DiagnosticsPanel extends UIElement {
    /** 浮动面板尺寸。 */
    private static final int WIDTH = 360;
    private static final int HEIGHT = 220;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final GraphEditorView editorView;
    private final Consumer<DiagnosticsCenter.Batch> listener = this::onBatch;
    private final UIElement floatingPanel;
    private final TextElement titleText;
    private final ScrollerView list;
    private final TextElement errorBadge;
    private final TextElement warningBadge;

    DiagnosticsPanel(GraphEditorView editorView) {
        this.editorView = editorView;
        // 绝对定位贴左下角；列方向：浮动面板在上、开关按钮行贴底
        layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(4).bottom(4)
                .width(WIDTH)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(2));

        // ---- 浮动面板（默认隐藏，不自动弹出） ----
        floatingPanel = new UIElement()
                .layout(layout -> layout.widthPercent(100).height(HEIGHT).paddingAll(4).gapAll(2));
        WorkbenchWidgets.panelBackground(floatingPanel);
        floatingPanel.setDisplay(false);

        titleText = WorkbenchWidgets.textLine("暂无诊断", WorkbenchColors.DIM);
        titleText.layout(layout -> layout.flex(1).minWidth(0));
        Button closeButton = new Button();
        closeButton.setText(Component.literal("✕"));
        closeButton.textStyle(style -> style.fontSize(9).textColor(WorkbenchColors.DIM));
        closeButton.setOnClick(event -> floatingPanel.setDisplay(false));
        closeButton.layout(layout -> layout.width(14).heightPercent(100));
        UIElement titleBar = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100).height(12)
                        .flexDirection(FlexDirection.ROW).gapAll(2));
        titleBar.addChildren(titleText, closeButton);

        list = new ScrollerView();
        list.layout(layout -> layout.widthPercent(100).flex(1));
        floatingPanel.addChildren(titleBar, list);

        // ---- 开关按钮行（贴底常驻） ----
        Button toggleButton = new Button();
        toggleButton.setText(Component.literal("问题"));
        toggleButton.textStyle(style -> style.fontSize(9));
        toggleButton.setOnClick(event -> floatingPanel.setDisplay(!floatingPanel.isDisplayed()));
        toggleButton.layout(layout -> layout.width(40).heightPercent(100));
        errorBadge = badgeLine();
        warningBadge = badgeLine();
        UIElement toggleRow = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100).height(14)
                        .flexDirection(FlexDirection.ROW).gapAll(4));
        toggleRow.addChildren(toggleButton, errorBadge, warningBadge);

        addChildren(floatingPanel, toggleRow);

        DiagnosticsCenter.addListener(listener);
        onBatch(DiagnosticsCenter.latest());
    }

    @Override
    protected void onRemoved() {
        super.onRemoved();
        DiagnosticsCenter.removeListener(listener);
    }

    // ==================== 刷新 ====================

    private void onBatch(DiagnosticsCenter.@Nullable Batch batch) {
        long errors = batch == null ? 0 : batch.errors();
        long warnings = batch == null ? 0 : batch.warnings();
        setBadge(errorBadge, "E " + errors, errors > 0 ? WorkbenchColors.ERROR : WorkbenchColors.DIM);
        setBadge(warningBadge, "W " + warnings, warnings > 0 ? WorkbenchColors.WARNING : WorkbenchColors.DIM);
        if (batch == null) {
            titleText.setText(Component.literal("暂无诊断"));
            return;
        }
        titleText.setText(Component.literal(batch.source()
                + " · " + TIME_FORMAT.format(Instant.ofEpochMilli(batch.epochMillis()))
                + " · E" + errors + " W" + warnings));
        rebuildList(batch);
    }

    private void rebuildList(DiagnosticsCenter.Batch batch) {
        list.clearAllScrollViewChildren();
        for (DiagnosticsCenter.Section section : batch.sections()) {
            if (section.diagnostics().isEmpty()) {
                continue;
            }
            list.addScrollViewChild(WorkbenchWidgets.sectionTitle(section.label()));
            for (Diagnostic d : section.diagnostics()) {
                list.addScrollViewChild(row(d));
            }
        }
    }

    // ==================== 行构造 ====================

    /** 诊断行：严重级色点 + code + message；带 nodeUid 时行为可点按钮（点击定位节点）。 */
    private UIElement row(Diagnostic d) {
        TextElement dot = WorkbenchWidgets.textLine("●", severityColor(d.severity()));
        dot.layout(layout -> layout.width(10).height(WorkbenchWidgets.ROW_HEIGHT));
        String text = d.code() + "  " + d.message();
        UIElement row = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100).height(WorkbenchWidgets.ROW_HEIGHT)
                        .flexDirection(FlexDirection.ROW).gapAll(2));
        if (d.nodeUid().isPresent()) {
            String uid = d.nodeUid().get();
            // rowButton 预设 widthPercent(100)，改由 flex 吃掉色点旁剩余宽度
            Button button = WorkbenchWidgets.rowButton(text, WorkbenchColors.TEXT, false,
                    () -> focusNode(uid));
            button.layout(layout -> layout.widthAuto().flex(1).minWidth(0).heightPercent(100));
            row.addChildren(dot, button);
        } else {
            TextElement line = WorkbenchWidgets.textLine(text, WorkbenchColors.TEXT);
            line.layout(layout -> layout.flex(1).minWidth(0));
            row.addChildren(dot, line);
        }
        return row;
    }

    private static int severityColor(Diagnostic.Severity severity) {
        return switch (severity) {
            case ERROR -> WorkbenchColors.ERROR;
            case WARNING -> WorkbenchColors.WARNING;
            default -> WorkbenchColors.DIM;
        };
    }

    private static TextElement badgeLine() {
        TextElement badge = WorkbenchWidgets.textLine("E 0", WorkbenchColors.DIM);
        badge.layout(layout -> layout.width(30));
        return badge;
    }

    private static void setBadge(TextElement badge, String text, int color) {
        badge.setText(Component.literal(text));
        badge.textStyle(style -> style.textColor(color));
    }

    // ==================== 节点定位 ====================

    /**
     * 选中并居中 uid 对应节点（best effort）：先在当前视图找（用户可能潜入子图），
     * 找不到回退 root 图视图；均无（节点已删/未翻译进画布）则静默放弃。
     * 居中保持当前缩放：{@code offset = 节点中心 − 视图尺寸 / (2·scale)}
     * （坐标系换算同 {@code BadgeOverlay}：screen = view + (graph − offset) · scale）。
     */
    private void focusNode(String nodeUid) {
        UUID uuid = EvmGraphTranslator.uidOf(nodeUid);
        GraphView view = editorView.getCurrentView();
        ModelElement element = view.getModelElement(uuid);
        if (element == null && view != editorView.graphView) {
            view = editorView.graphView;
            element = view.getModelElement(uuid);
        }
        if (element == null || !(element.getModel() instanceof AbstractNodeModel node)) {
            return;
        }
        view.clearAllSelected();
        view.addSelected(node);
        var g = view.graphView;
        float scale = g.getScale();
        if (scale <= 0) {
            return;
        }
        Vector2f pos = node.getPosition();
        g.setOffsetX(pos.x + element.getSizeWidth() / 2f - g.getSizeWidth() / (2f * scale));
        g.setOffsetY(pos.y + element.getSizeHeight() / 2f - g.getSizeHeight() / (2f * scale));
    }
}
//?}

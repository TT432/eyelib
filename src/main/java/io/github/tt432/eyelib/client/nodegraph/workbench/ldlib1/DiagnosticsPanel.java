//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import io.github.tt432.eyelib.client.nodegraph.DiagnosticsCenter;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 诊断浮动面板（规格 nodegraph-declaration-wiring §4.3，LDLib1 版）：画布左下角常驻
 * 「问题」开关按钮 + 红 error / 黄 warning 徽标（0/0 置灰），点击在按钮上方弹出
 * 360×220 浮动面板（标题栏 = 来源 + 时间 + E/W 统计 + 关闭 ×，其下为分节可滚动
 * 诊断列表：节 label 作分组头，行 = 严重级色点 + code + message，长文截断）。
 *
 * <p>监听 {@link DiagnosticsCenter} 自动刷新（编辑器关闭时经
 * {@link ModularUI#registerCloseListener} 摘除监听）；不自动弹出。
 * 带 nodeUid 的行点击 → {@code nodeFocus} 回调（画布选中并居中该节点）。
 */
public final class DiagnosticsPanel extends WidgetGroup {
    private static final int POPUP_WIDTH = 360;
    private static final int POPUP_HEIGHT = 220;
    private static final int COLOR_ERROR = 0xFFFF5555;
    private static final int COLOR_WARNING = 0xFFFFFF55;
    private static final int COLOR_INFO = 0xFF55FFFF;
    private static final int COLOR_GRAY = 0xFF888888;
    private static final int COLOR_HEADER = 0xFF7EB6FF;
    private static final int COLOR_TEXT = 0xFFDDDDDD;
    private static final int COLOR_HOVER = 0x33FFFFFF;
    private static final int COLOR_SCROLLBAR = 0x66FFFFFF;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 当前批次（volatile：上报线程与渲染线程可能不同；空批次中心不上报，保留上一批）。 */
    private volatile DiagnosticsCenter.@Nullable Batch batch;
    /** 当前批次展开后的行（含节头），随批次整体替换为不可变列表。 */
    private volatile List<Row> rows = List.of();
    private final Consumer<DiagnosticsCenter.Batch> listener = this::onBatch;
    private final WidgetGroup popup;
    private final DiagnosticListView listView;

    /**
     * @param nodeFocus 诊断行定位回调（参数为节点 uid；best effort，找不到静默）
     */
    public DiagnosticsPanel(int x, int y, int width, int height, Consumer<String> nodeFocus) {
        super(x, y, width, height);
        setClientSideWidget();

        int buttonY = height - 16;
        addWidget(new ButtonWidget(4, buttonY, 40, 13, new TextTexture("问题"), cd -> togglePopup()));

        // 弹层吞掉落在区域内的点击/滚轮，避免穿透到画布（框选/缩放）
        popup = new WidgetGroup(4, Math.max(2, buttonY - 4 - POPUP_HEIGHT), POPUP_WIDTH, POPUP_HEIGHT) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return super.mouseClicked(mouseX, mouseY, button) || isMouseOverElement(mouseX, mouseY);
            }

            @Override
            public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
                return super.mouseWheelMove(mouseX, mouseY, wheelDelta) || isMouseOverElement(mouseX, mouseY);
            }
        };
        popup.setBackground(new ColorRectTexture(0xF0141414));
        popup.addWidget(new LabelWidget(4, 4, this::titleText));
        popup.addWidget(new ButtonWidget(POPUP_WIDTH - 16, 3, 12, 10, new TextTexture("×"),
                cd -> setPopupOpen(false)));
        listView = new DiagnosticListView(2, 16, POPUP_WIDTH - 4, POPUP_HEIGHT - 20, nodeFocus);
        popup.addWidget(listView);
        setPopupOpen(false);
        addWidget(popup);

        batch = DiagnosticsCenter.latest();
        rebuildRows();
        DiagnosticsCenter.addListener(listener);
    }

    /** 编辑器关闭时摘除诊断中心监听（ModularUIContainer.removed → triggerCloseListeners）。 */
    @Override
    public void setGui(ModularUI gui) {
        super.setGui(gui);
        if (gui != null) {
            gui.registerCloseListener(() -> DiagnosticsCenter.removeListener(listener));
        }
    }

    private void onBatch(DiagnosticsCenter.Batch newBatch) {
        batch = newBatch;
        rebuildRows();
        listView.resetScroll();
    }

    private void rebuildRows() {
        DiagnosticsCenter.Batch current = batch;
        if (current == null) {
            rows = List.of();
            return;
        }
        List<Row> list = new ArrayList<>();
        for (DiagnosticsCenter.Section section : current.sections()) {
            // 空节跳过（与 ldlib2 面板一致）：闭包导入中无诊断的 RC/AC 不占行
            if (section.diagnostics().isEmpty()) {
                continue;
            }
            list.add(Row.header(section.label()));
            for (Diagnostic diagnostic : section.diagnostics()) {
                list.add(Row.diagnostic(diagnostic));
            }
        }
        rows = List.copyOf(list);
    }

    private void togglePopup() {
        setPopupOpen(!popup.isVisible());
    }

    private void setPopupOpen(boolean open) {
        popup.setVisible(open);
        popup.setActive(open);
    }

    /** 标题栏文本：来源 + 时间 + E/W 统计（按可用宽度截断）。 */
    private String titleText() {
        DiagnosticsCenter.Batch current = batch;
        String text = current == null ? "问题（暂无诊断）"
                : current.source() + " · "
                + TIME_FORMAT.format(Instant.ofEpochMilli(current.epochMillis()).atZone(ZoneId.systemDefault()))
                + " · E" + current.errors() + " W" + current.warnings();
        return Minecraft.getInstance().font.plainSubstrByWidth(text, POPUP_WIDTH - 26);
    }

    @Override
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        // 「问题」按钮右侧的 E/W 徽标（0 置灰）
        DiagnosticsCenter.Batch current = batch;
        long errors = current == null ? 0 : current.errors();
        long warnings = current == null ? 0 : current.warnings();
        var font = Minecraft.getInstance().font;
        int badgeX = getPositionX() + 48;
        int badgeY = getPositionY() + getSizeHeight() - 14;
        String errorText = "E" + errors;
        graphics.drawString(font, errorText, badgeX, badgeY,
                errors > 0 ? COLOR_ERROR : COLOR_GRAY, false);
        graphics.drawString(font, "W" + warnings, badgeX + font.width(errorText) + 6, badgeY,
                warnings > 0 ? COLOR_WARNING : COLOR_GRAY, false);
    }

    /** 一行：节头（header=true，text=节 label）或诊断（diagnostic 非空）。 */
    private record Row(boolean header, @Nullable String text, @Nullable Diagnostic diagnostic) {
        static Row header(String label) {
            return new Row(true, label, null);
        }

        static Row diagnostic(Diagnostic d) {
            return new Row(false, null, d);
        }
    }

    /**
     * 诊断行列表：滚动/裁剪/滚动条模式同 {@link ListRowView}，但行按严重级着色
     * （色点 + code + message，长文截断），带 nodeUid 的行点击触发定位回调。
     */
    private final class DiagnosticListView extends Widget {
        private static final int ROW_HEIGHT = 12;

        private final Consumer<String> nodeFocus;
        /** 滚动偏移（像素）。 */
        private int scrollOffset;

        DiagnosticListView(int x, int y, int width, int height, Consumer<String> nodeFocus) {
            super(x, y, width, height);
            this.nodeFocus = nodeFocus;
        }

        void resetScroll() {
            scrollOffset = 0;
        }

        @Override
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            List<Row> current = rows;
            int x = getPositionX();
            int y = getPositionY();
            int width = getSizeWidth();
            int height = getSizeHeight();

            var trans = graphics.pose().last().pose();
            var p1 = trans.transform(new Vector4f(x, y, 0, 1));
            var p2 = trans.transform(new Vector4f(x + width, y + height, 0, 1));
            graphics.enableScissor((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);

            var font = Minecraft.getInstance().font;
            int firstRow = scrollOffset / ROW_HEIGHT;
            int rowY = y - (scrollOffset % ROW_HEIGHT);
            for (int i = firstRow; i < current.size() && rowY < y + height; i++, rowY += ROW_HEIGHT) {
                Row row = current.get(i);
                if (row.header()) {
                    graphics.drawString(font, truncate(row.text(), width - 6, font),
                            x + 3, rowY + 2, COLOR_HEADER, false);
                    continue;
                }
                Diagnostic d = row.diagnostic();
                if (d == null) {
                    continue;
                }
                if (isMouseOver(x, rowY, width, ROW_HEIGHT, mouseX, mouseY)) {
                    DrawerHelper.drawSolidRect(graphics, x, rowY, width, ROW_HEIGHT, COLOR_HOVER);
                }
                int severityColor = switch (d.severity()) {
                    case ERROR -> COLOR_ERROR;
                    case WARNING -> COLOR_WARNING;
                    case INFO -> COLOR_INFO;
                };
                DrawerHelper.drawSolidRect(graphics, x + 4, rowY + 4, 4, 4, severityColor);
                int textX = x + 12;
                graphics.drawString(font, d.code(), textX, rowY + 2, COLOR_GRAY, false);
                textX += font.width(d.code()) + 4;
                graphics.drawString(font, truncate(d.message(), x + width - 3 - textX, font),
                        textX, rowY + 2, COLOR_TEXT, false);
            }
            graphics.disableScissor();

            // 内容超高时右侧细滚动条（仅指示位置，不可拖拽）
            int contentHeight = current.size() * ROW_HEIGHT;
            if (contentHeight > height) {
                int barHeight = Math.max(10, height * height / contentHeight);
                int barY = y + (int) ((float) scrollOffset / (contentHeight - height) * (height - barHeight));
                DrawerHelper.drawSolidRect(graphics, x + width - 2, barY, 2, barHeight, COLOR_SCROLLBAR);
            }
        }

        /** 按可用宽度截断，超长补省略号。 */
        private static String truncate(@Nullable String text, int availWidth,
                                       net.minecraft.client.gui.Font font) {
            if (text == null) {
                return "";
            }
            String shown = font.plainSubstrByWidth(text, Math.max(0, availWidth));
            if (shown.length() < text.length()) {
                shown = font.plainSubstrByWidth(text, Math.max(0, availWidth - font.width("…"))) + "…";
            }
            return shown;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOverElement(mouseX, mouseY)) {
                return false;
            }
            List<Row> current = rows;
            int index = (int) (mouseY - getPositionY() + scrollOffset) / ROW_HEIGHT;
            if (index >= 0 && index < current.size()) {
                Row row = current.get(index);
                Diagnostic d = row.diagnostic();
                if (d != null && d.nodeUid().isPresent()) {
                    nodeFocus.accept(d.nodeUid().get());
                }
            }
            // 吞掉落在区域内的点击，避免穿透到画布
            return true;
        }

        @Override
        public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
            if (!isMouseOverElement(mouseX, mouseY)) {
                return false;
            }
            int maxScroll = Math.max(0, rows.size() * ROW_HEIGHT - getSizeHeight());
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - wheelDelta * ROW_HEIGHT * 2));
            return true;
        }
    }
}
//?}

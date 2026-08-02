//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1;

import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 轻量字符串行列表（LDLib1 没有开箱即用的纯文本列表 widget）：
 * 行高 12px，滚轮滚动，单击选中（高亮 + 回调）。行数据由 supplier 每帧提供，
 * 过滤/刷新只需改 supplier 背后的状态，无需重建 widget。
 */
public final class ListRowView extends Widget {
    private static final int ROW_HEIGHT = 12;
    private static final int COLOR_SELECTED = 0xFF2A4A6A;
    private static final int COLOR_HOVER = 0x33FFFFFF;
    private static final int COLOR_TEXT = 0xFFDDDDDD;
    private static final int COLOR_SCROLLBAR = 0x66FFFFFF;

    /**
     * 一行：id 为选中态标识（列表内唯一），label 为显示文本。
     */
    public record Row(String id, String label) {
    }

    private final Supplier<List<Row>> rowSupplier;
    private final Consumer<Row> onSelect;
    private @Nullable String selectedId;
    /** 滚动偏移（像素）。 */
    private int scrollOffset;

    public ListRowView(int x, int y, int width, int height,
                       Supplier<List<Row>> rowSupplier, Consumer<Row> onSelect) {
        super(x, y, width, height);
        this.rowSupplier = rowSupplier;
        this.onSelect = onSelect;
    }

    public void setSelectedId(@Nullable String selectedId) {
        this.selectedId = selectedId;
    }

    public @Nullable String selectedId() {
        return selectedId;
    }

    @Override
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        List<Row> rows = rowSupplier.get();
        int x = getPositionX();
        int y = getPositionY();
        int width = getSizeWidth();
        int height = getSizeHeight();

        var trans = graphics.pose().last().pose();
        var p1 = trans.transform(new Vector4f(x, y, 0, 1));
        var p2 = trans.transform(new Vector4f(x + width, y + height, 0, 1));
        graphics.enableScissor((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);

        int firstRow = scrollOffset / ROW_HEIGHT;
        int rowY = y - (scrollOffset % ROW_HEIGHT);
        for (int i = firstRow; i < rows.size() && rowY < y + height; i++, rowY += ROW_HEIGHT) {
            Row row = rows.get(i);
            if (row.id().equals(selectedId)) {
                DrawerHelper.drawSolidRect(graphics, x, rowY, width, ROW_HEIGHT, COLOR_SELECTED);
            } else if (isMouseOver(x, rowY, width, ROW_HEIGHT, mouseX, mouseY)) {
                DrawerHelper.drawSolidRect(graphics, x, rowY, width, ROW_HEIGHT, COLOR_HOVER);
            }
            graphics.drawString(Minecraft.getInstance().font, row.label(), x + 3, rowY + 2, COLOR_TEXT, false);
        }
        graphics.disableScissor();

        // 内容超高时右侧细滚动条（仅指示位置，不可拖拽）
        int contentHeight = rows.size() * ROW_HEIGHT;
        if (contentHeight > height) {
            int barHeight = Math.max(10, height * height / contentHeight);
            int barY = y + (int) ((float) scrollOffset / (contentHeight - height) * (height - barHeight));
            DrawerHelper.drawSolidRect(graphics, x + width - 2, barY, 2, barHeight, COLOR_SCROLLBAR);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOverElement(mouseX, mouseY)) {
            return false;
        }
        List<Row> rows = rowSupplier.get();
        int index = (int) (mouseY - getPositionY() + scrollOffset) / ROW_HEIGHT;
        if (index >= 0 && index < rows.size()) {
            Row row = rows.get(index);
            selectedId = row.id();
            onSelect.accept(row);
        }
        return true;
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (!isMouseOverElement(mouseX, mouseY)) {
            return false;
        }
        int maxScroll = Math.max(0, rowSupplier.get().size() * ROW_HEIGHT - getSizeHeight());
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - wheelDelta * ROW_HEIGHT * 2));
        return true;
    }
}
//?}

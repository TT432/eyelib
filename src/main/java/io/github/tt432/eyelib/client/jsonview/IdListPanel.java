package io.github.tt432.eyelib.client.jsonview;

import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIScrollPanel;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * JSON 查看器左侧的实体 id 列表：支持子串过滤、点击选中与选中高亮。
 *
 * @author TT432
 */
final class IdListPanel extends UIScrollPanel {
    private static final int COLOR_TEXT = 0xFFDDDDDD;
    private static final int COLOR_SELECTED_BG = 0x553A6EA5;
    private static final int COLOR_HOVER_BG = 0x22FFFFFF;
    private static final int COLOR_EMPTY = 0xFF777777;

    private final int rowHeight;
    private final Consumer<String> onSelect;

    private List<String> ids = List.of();
    private List<String> filtered = List.of();
    /** 已转为小写、去除首尾空白的过滤串；空串表示不过滤。 */
    private String filter = "";
    private int selected = -1;

    IdListPanel(int x, int y, int width, int height, int rowHeight, Consumer<String> onSelect) {
        super(x, y, width, height);
        this.border = 2;
        this.rowHeight = rowHeight;
        this.onSelect = onSelect;
    }

    /**
     * 替换列表数据（tab 切换 / 初始化时调用），重置选中与滚动位置。
     */
    void setIds(List<String> ids) {
        this.ids = List.copyOf(ids);
        this.selected = -1;
        applyFilter();
        setScrollDistance(0);
    }

    void setFilter(String filter) {
        this.filter = filter.trim().toLowerCase(Locale.ROOT);
        applyFilter();
        setScrollDistance(0);
    }

    private void applyFilter() {
        filtered = filter.isEmpty()
                ? ids
                : ids.stream().filter(id -> id.toLowerCase(Locale.ROOT).contains(filter)).toList();
        if (selected >= filtered.size()) {
            selected = -1;
        }
    }

    @Override
    protected void renderContent(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (filtered.isEmpty()) {
            gfx.drawText("（无条目）", x + 4, y + border + 2, COLOR_EMPTY);
            return;
        }
        int textOffset = Math.max(0, (rowHeight - gfx.fontHeight()) / 2);
        for (int i = 0; i < filtered.size(); i++) {
            int rowY = y + border + i * rowHeight;
            if (i == selected) {
                gfx.fill(x, rowY, x + width, rowY + rowHeight, COLOR_SELECTED_BG);
            } else if (mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + rowHeight) {
                gfx.fill(x, rowY, x + width, rowY + rowHeight, COLOR_HOVER_BG);
            }
            gfx.drawText(filtered.get(i), x + 4, rowY + textOffset, COLOR_TEXT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY) || button != 0) {
            return false;
        }
        int index = (int) ((mouseY - y - border + scrollDistance) / rowHeight);
        if (index >= 0 && index < filtered.size()) {
            selected = index;
            onSelect.accept(filtered.get(index));
        }
        return true;
    }

    @Override
    public int getContentHeight() {
        return Math.max(height, filtered.size() * rowHeight + border * 2);
    }
}

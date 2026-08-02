//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1;

import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector4f;

import java.util.List;
import java.util.function.Supplier;

/**
 * 着色多行文本视图（JSON / scope 表共用）：行 → {@link JsonColors.Segment} 段列表，
 * 逐段着色绘制，滚轮滚动。行数据由 supplier 每帧提供（scope 快照等每帧刷新场景
 * 无需重建 widget）。
 */
public final class ScrollableTextView extends Widget {
    private static final int LINE_HEIGHT = 10;
    private static final int COLOR_SCROLLBAR = 0x66FFFFFF;

    private final Supplier<List<List<JsonColors.Segment>>> linesSupplier;
    /** 滚动偏移（像素）。 */
    private int scrollOffset;

    public ScrollableTextView(int x, int y, int width, int height,
                              Supplier<List<List<JsonColors.Segment>>> linesSupplier) {
        super(x, y, width, height);
        this.linesSupplier = linesSupplier;
    }

    /** 内容变化后重置滚动位置（如资产检查器切换选中项）。 */
    public void resetScroll() {
        scrollOffset = 0;
    }

    @Override
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        List<List<JsonColors.Segment>> lines = linesSupplier.get();
        int x = getPositionX();
        int y = getPositionY();
        int width = getSizeWidth();
        int height = getSizeHeight();

        var trans = graphics.pose().last().pose();
        var p1 = trans.transform(new Vector4f(x, y, 0, 1));
        var p2 = trans.transform(new Vector4f(x + width, y + height, 0, 1));
        graphics.enableScissor((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);

        var font = Minecraft.getInstance().font;
        int firstLine = scrollOffset / LINE_HEIGHT;
        int lineY = y + 2 - (scrollOffset % LINE_HEIGHT);
        for (int i = firstLine; i < lines.size() && lineY < y + height; i++, lineY += LINE_HEIGHT) {
            int drawX = x + 3;
            for (JsonColors.Segment segment : lines.get(i)) {
                graphics.drawString(font, segment.text(), drawX, lineY, segment.color(), false);
                drawX += font.width(segment.text());
            }
        }
        graphics.disableScissor();

        int contentHeight = lines.size() * LINE_HEIGHT + 4;
        if (contentHeight > height) {
            int barHeight = Math.max(10, height * height / contentHeight);
            int barY = y + (int) ((float) scrollOffset / (contentHeight - height) * (height - barHeight));
            DrawerHelper.drawSolidRect(graphics, x + width - 2, barY, 2, barHeight, COLOR_SCROLLBAR);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 纯展示：吞掉落在区域内的点击，避免穿透到画布
        return isMouseOverElement(mouseX, mouseY);
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (!isMouseOverElement(mouseX, mouseY)) {
            return false;
        }
        int maxScroll = Math.max(0, linesSupplier.get().size() * LINE_HEIGHT + 4 - getSizeHeight());
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - wheelDelta * LINE_HEIGHT * 2));
        return true;
    }
}
//?}

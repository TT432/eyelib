//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.configurator.StringConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 可输入下拉（规格 §4.2）：文本框自由输入 + 右侧下拉按钮弹出候选列表。
 * 候选由 supplier 在<b>每次打开弹层时</b>运行时取（注册表 COW 快照，随资产增减自动刷新）；
 * 选中候选即写回文本框，未选中任何候选时手输值原样保留（外部契约逃生舱）。
 *
 * <p>弹层绘制/命中/焦点收起全部照搬 {@code SelectorWidget} 的做法：常态绘制时把弹层
 * 藏起来，在 {@link #drawInForeground} 里抬 z 轴 +200 单独画（否则被后续配置行覆盖）；
 * {@link #isMouseOverElement} 把弹层纳入命中；失焦/点空白收起。
 */
final class SuggestionConfigurator extends StringConfigurator {
    /** 弹层最多显示行数（超出出滚动条）。 */
    private static final int MAX_VISIBLE_ROWS = 8;
    private static final int ROW_HEIGHT = 12;

    private final Supplier<List<String>> candidates;
    private @Nullable DraggableScrollableWidgetGroup popUp;
    private boolean isShow;

    SuggestionConfigurator(String name, Supplier<String> supplier, Consumer<String> onUpdate,
                           String defaultValue, boolean forceUpdate, Supplier<List<String>> candidates) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        this.candidates = candidates;
    }

    @Override
    public void init(int width) {
        super.init(width);
        int fieldWidth = width - leftWidth - 3 - rightWidth;
        // 文本框让出右侧 11px 给下拉按钮
        textFieldWidget.setSize(new Size(fieldWidth - 12, 10));
        addWidget(new ButtonWidget(leftWidth + fieldWidth - 11, 2, 11, 10, Icons.DOWN,
                cd -> {
                    if (cd.isRemote) {
                        togglePopup(fieldWidth);
                    }
                }));
        popUp = new DraggableScrollableWidgetGroup(leftWidth, 13, fieldWidth, 0);
        popUp.setBackground(new ColorRectTexture(0xE0101010));
        popUp.setVisible(false);
        popUp.setActive(false);
        addWidget(popUp);
    }

    private void togglePopup(int fieldWidth) {
        if (isShow) {
            setShow(false);
            return;
        }
        // 打开瞬间取候选：运行时装配/资源重载后的新资产即时可见
        List<String> latest = candidates.get();
        DraggableScrollableWidgetGroup popUp = this.popUp;
        if (popUp == null) return;
        popUp.clearAllWidgets();
        boolean scroll = latest.size() > MAX_VISIBLE_ROWS;
        popUp.setSize(new Size(fieldWidth, Math.min(latest.size(), MAX_VISIBLE_ROWS) * ROW_HEIGHT));
        if (scroll) {
            popUp.setYScrollBarWidth(4).setYBarStyle(null, new ColorRectTexture(-1));
        }
        int rowWidth = fieldWidth - (scroll ? 4 : 0);
        int y = 0;
        for (String candidate : latest) {
            SelectableWidgetGroup row = new SelectableWidgetGroup(0, y, rowWidth, ROW_HEIGHT);
            row.addWidget(new ImageWidget(0, 0, rowWidth, ROW_HEIGHT,
                    new TextTexture(candidate).setWidth(rowWidth).setType(TextTexture.TextType.ROLL)));
            row.setSelectedTexture(-1, -1);
            row.setOnSelected(s -> {
                onValueUpdate(candidate);
                updateValue();
                setShow(false);
            });
            popUp.addWidget(row);
            y += ROW_HEIGHT;
        }
        popUp.setScrollYOffset(0);
        setShow(true);
    }

    private void setShow(boolean show) {
        isShow = show;
        if (popUp != null) {
            popUp.setVisible(show);
            popUp.setActive(show);
        }
    }

    // ---------- 弹层绘制/命中/收起（SelectorWidget 同款） ----------

    @Override
    public boolean isMouseOverElement(double mouseX, double mouseY) {
        return super.isMouseOverElement(mouseX, mouseY)
                || (isShow && popUp != null && popUp.isMouseOverElement(mouseX, mouseY));
    }

    @Override
    public void onFocusChanged(@Nullable Widget lastFocus, Widget focus) {
        if (lastFocus != null && !lastFocus.isParent(this) && focus != this) {
            setShow(false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!super.mouseClicked(mouseX, mouseY, button)) {
            setFocus(false);
            return false;
        }
        return true;
    }

    @Override
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (popUp == null) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            return;
        }
        boolean lastVisible = popUp.isVisible();
        popUp.setVisible(false);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        popUp.setVisible(lastVisible);
    }

    @Override
    public void drawInForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (popUp == null) {
            super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
            return;
        }
        boolean lastVisible = popUp.isVisible();
        popUp.setVisible(false);
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        popUp.setVisible(lastVisible);
        if (isShow) {
            graphics.pose().translate(0, 0, 200);
            popUp.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            popUp.drawInForeground(graphics, mouseX, mouseY, partialTicks);
            graphics.pose().translate(0, 0, -200);
        }
    }
}
//?}

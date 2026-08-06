package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import net.minecraft.network.chat.Component;

/**
 * 工作台各面板共用的 LDLib2 小控件构造器（列表行按钮、分节标题、面板底色）。
 */
final class WorkbenchWidgets {
    private WorkbenchWidgets() {
    }

    /** 列表行高（9px 字体 + 间距）。 */
    static final int ROW_HEIGHT = 12;

    /** 面板/工具条底色（与 LDLib2 编辑器 header 同款）。 */
    static UIElement panelBackground(UIElement element) {
        return element.style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
    }

    /** 分节标题（小号灰字）。 */
    static Label sectionTitle(String text) {
        Label label = new Label();
        label.setText(Component.literal(text));
        label.textStyle(style -> style.fontSize(9).textColor(WorkbenchColors.DIM));
        label.layout(layout -> layout.widthPercent(100).height(10));
        return label;
    }

    /**
     * 列表行按钮：透明底、hover 灰、左对齐小字；选中态给半透明白底。
     * 文本原样显示（literal，不走 translatable）。
     */
    static Button rowButton(String text, int textColor, boolean selected, Runnable onClick) {
        Button row = new Button();
        row.buttonStyle(style -> style
                        .baseTexture(selected ? ColorPattern.T_WHITE.rectTexture() : IGuiTexture.EMPTY)
                        .hoverTexture(ColorPattern.T_GRAY.rectTexture())
                        .pressedTexture(ColorPattern.T_GRAY.rectTexture()))
                .setOnClick(event -> onClick.run())
                .setText(Component.literal(text))
                .textStyle(style -> style
                        .fontSize(9)
                        .textColor(textColor)
                        .textAlignHorizontal(Horizontal.LEFT))
                .layout(layout -> layout.widthPercent(100).height(ROW_HEIGHT));
        return row;
    }

    /** 单行文本（literal，不翻译）。 */
    static TextElement textLine(String text, int color) {
        TextElement element = new TextElement();
        element.setText(Component.literal(text));
        element.textStyle(style -> style.fontSize(9).textColor(color));
        element.layout(layout -> layout.height(10));
        return element;
    }
}
//?}

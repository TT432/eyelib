package io.github.tt432.eyelib.client.gui.snowstorm.dialog;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.editor.SnowstormValidator;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * WarningDialog.vue as-is（P5-D）：警告列表弹层。
 * 数据源 {@link SnowstormValidator#validate()}（打开时现算，WarningDialog.vue
 * 模块级 errors 数组在 validate() 内 splice 重建的同效语义）。
 *
 * <p>接入接缝（Main 集成）：StageFooterBar ⚠ 按钮 →
 * {@code WarningDialogView.open(screenRoot)}。</p>
 */
public final class WarningDialogView extends UIElement {

    /** WarningDialog.vue li.warning 颜色 #ffc107。 */
    private static final int WARNING_COLOR = 0xFFFFC107;
    private static final int BLACKOUT = 0x50000000; // App.vue #dialog_blackout #00000050

    private WarningDialogView() {
        // 全屏 blackout 覆盖层
        layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0).top(0).right(0).bottom(0)
                .flexDirection(FlexDirection.COLUMN)
                .justifyContent(AlignContent.CENTER)
                .alignItems(AlignItems.CENTER));
        style(style -> style.backgroundTexture(new ColorRectTexture(BLACKOUT)));

        List<SnowstormValidator.Warning> errors = SnowstormValidator.validate();

        UIElement panel = new UIElement().layout(layout -> layout
                .width(280)
                .flexDirection(FlexDirection.COLUMN)
                .paddingAll(10)
                .gapAll(4));
        panel.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)));

        // 标题行：h2 + 关闭按钮（JS unicode_icon \u2A09）
        UIElement titleBar = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(14)
                .flexDirection(FlexDirection.ROW));
        TextElement title = text("Warnings", SnowstormTheme.HIGHLIGHT, 12);
        title.layout(layout -> layout.flex(1));
        Button close = new Button();
        close.setText(Component.literal("⤫"))
                .textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT))
                .buttonStyle(style -> style
                        .baseTexture(IGuiTexture.EMPTY)
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                .setOnClick(event -> removeSelf());
        close.layout(layout -> layout.width(14).heightPercent(100));
        titleBar.addChildren(title, close);

        // JS: There are {{errors.length}} warning {{errors.length == 1 ? 'note' : 'notes'}}:
        TextElement summary = text(
                "There are " + errors.size() + " warning " + (errors.size() == 1 ? "note" : "notes") + ":",
                SnowstormTheme.TEXT, 9);
        summary.textStyle(style -> style.textAlignHorizontal(Horizontal.LEFT));

        ScrollerView list = new ScrollerView();
        list.layout(layout -> layout.widthPercent(100).height(Math.min(160, Math.max(20, errors.size() * 14))));
        for (SnowstormValidator.Warning warning : errors) {
            TextElement row = text("• " + warning.text, WARNING_COLOR, 9);
            row.textStyle(style -> style.textAlignHorizontal(Horizontal.LEFT));
            row.layout(layout -> layout.widthPercent(100).paddingVertical(4));
            list.addScrollViewChild(row);
        }

        panel.addChildren(titleBar, summary, list);
        addChild(panel);
    }

    /**
     * 打开警告弹层（attachTo = 编辑器根 UIElement；blackout 绝对定位铺满）。
     * 返回弹层实例（调用方无需持有，✕ 自关闭）。
     */
    public static WarningDialogView open(UIElement attachTo) {
        WarningDialogView dialog = new WarningDialogView();
        attachTo.addChild(dialog);
        return dialog;
    }

    private static TextElement text(String text, int color, int fontSize) {
        TextElement element = new TextElement();
        element.setText(Component.literal(text));
        element.textStyle(style -> style.fontSize(fontSize).textColor(color));
        return element;
    }
}
//?}

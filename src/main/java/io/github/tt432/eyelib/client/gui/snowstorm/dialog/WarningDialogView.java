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
        // App.vue #dialog_blackout：全屏半透明黑（#00000050）+ 点击关闭
        layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0).top(0).right(0).bottom(0)
                .flexDirection(FlexDirection.ROW)
                .justifyContent(AlignContent.CENTER));
        style(style -> style.backgroundTexture(new ColorRectTexture(BLACKOUT)));
        addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.CLICK, event -> removeSelf());

        List<SnowstormValidator.Warning> errors = SnowstormValidator.validate();

        // App.vue dialog：宽 800、max-width 100%、top 20 / bottom 20（顶对齐非居中）、
        // padding 20px 28px、1px solid bar、radius 4、shadow
        UIElement panel = new UIElement().layout(layout -> layout
                .width(800)
                .maxWidthPercent(100)
                .marginTop(20)
                .marginBottom(20)
                .flexDirection(FlexDirection.COLUMN)
                .paddingHorizontal(28)
                .paddingVertical(20)
                .gapAll(4));
        panel.style(style -> style.backgroundTexture(
                com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup.of(
                        new ColorRectTexture(SnowstormTheme.INTERFACE),
                        new com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture(-1, SnowstormTheme.BAR))));
        panel.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.CLICK,
                event -> event.stopPropagation()); // 面板内点击不关弹层

        // 标题行：h2 + 关闭钮（dialog .close_button：absolute top 6 right 6、高 30、✕ 24px）
        UIElement titleBar = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(30)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER));
        TextElement title = text("Warnings", SnowstormTheme.HIGHLIGHT, 13);
        title.layout(layout -> layout.flex(1));
        Button close = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.ghost(
                "x", 24, event -> removeSelf());
        close.layout(layout -> layout.width(30).height(30));
        titleBar.addChildren(title, close);

        // JS: There are {{errors.length}} warning {{errors.length == 1 ? 'note' : 'notes'}}:
        TextElement summary = text(
                "There are " + errors.size() + " warning " + (errors.size() == 1 ? "note" : "notes") + ":",
                SnowstormTheme.TEXT, 10);
        summary.textStyle(style -> style.textAlignHorizontal(Horizontal.LEFT));

        ScrollerView list = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsScroller.plain(new ScrollerView());
        list.layout(layout -> layout.widthPercent(100).flex(1));
        for (SnowstormValidator.Warning warning : errors) {
            // WarningDialog.vue li.warning：list-style inside、padding 10px、#ffc107
            TextElement row = text("• " + warning.text, WARNING_COLOR, 10);
            row.textStyle(style -> style.textAlignHorizontal(Horizontal.LEFT));
            row.layout(layout -> layout.widthPercent(100).paddingAll(10));
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

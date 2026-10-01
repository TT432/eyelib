package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import net.minecraft.network.chat.Component;

/**
 * Snowstorm 样式按钮（common.css button as-is）：bar 底、hover accent 底黑字、
 * padding 8px 12px、radius 1px。
 *
 * <p>用法：{@code SsButton.of("文本", e -> ...)}。
 */
public final class SsButton {

    private SsButton() {
    }

    public static Button of(String text, com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener onClick) {
        Button button = new Button();
        button.setText(Component.literal(text))
                .textStyle(style -> style.fontSize(10).textColor(SnowstormTheme.TEXT))
                .buttonStyle(style -> style
                        .baseTexture(new ColorRectTexture(SnowstormTheme.BAR))
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.ACCENT))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.ACCENT)))
                .setOnClick(onClick);
        return button;
    }

    /** 无边框图标钮（sidebar help「?」/工具栏同款：hover 高亮文字）。 */
    public static Button ghost(String text, com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener onClick) {
        Button button = new Button();
        button.setText(Component.literal(text))
                .textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED))
                .buttonStyle(style -> style
                        .baseTexture(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY)
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                .setOnClick(onClick);
        return button;
    }
}
//?}

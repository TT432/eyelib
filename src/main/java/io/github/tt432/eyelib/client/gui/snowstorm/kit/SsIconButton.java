package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import net.minecraft.network.chat.Component;

/**
 * 图标按钮（lucide 图标 + 无文字）：替代文字占位符按钮（⌄/⴩/≡/⚡ 等）。
 */
public final class SsIconButton {

    private SsIconButton() {
    }

    /** ghost 形态（无底色，hover selection）。 */
    public static Button ghost(String lucideName, int size,
                               com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener onClick) {
        return make(lucideName, size, onClick, IGuiTexture.EMPTY,
                new ColorRectTexture(SnowstormTheme.SELECTION));
    }

    /** bar 形态（bar 底色，hover accent）。 */
    public static Button bar(String lucideName, int size,
                             com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener onClick) {
        return make(lucideName, size, onClick, new ColorRectTexture(SnowstormTheme.BAR),
                new ColorRectTexture(SnowstormTheme.SELECTION));
    }

    private static Button make(String lucideName, int size,
                               com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener onClick,
                               IGuiTexture base, IGuiTexture hover) {
        Button button = new Button();
        button.setText(Component.empty());
        button.buttonStyle(style -> style
                        .baseTexture(base)
                        .hoverTexture(hover)
                        .pressedTexture(hover))
                .setOnClick(onClick);
        button.layout(layout -> layout
                .justifyContent(AlignContent.CENTER)
                .alignItems(AlignItems.CENTER));
        button.addChild(SsIcon.of(lucideName, size));
        return button;
    }
}
//?}

package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import io.github.tt432.eyelib.util.PortResourceLocation;

/**
 * lucide 图标元素（assets/eyelib/snowstorm/icons/&lt;kebab&gt;.png，48×48 光栅化资产，
 * 管线见 scripts/snowstorm-icons/README.md）。
 *
 * <p>用法：{@code SsIcon.of("wand", 12)}。
 */
public final class SsIcon {

    private SsIcon() {
    }

    /** 图标元素（SpriteTexture 绘制，默认 text 色已由资产烘焙）。 */
    public static UIElement of(String lucideName, int size) {
        UIElement icon = new UIElement();
        icon.layout(layout -> layout.width(size).height(size));
        icon.style(style -> style.backgroundTexture(SpriteTexture.of(
                PortResourceLocation.of("eyelib", "snowstorm/icons/" + lucideName + ".png").toString())));
        return icon;
    }

    /** 图标纹理（自定义布局内联用）。 */
    public static IGuiTexture texture(String lucideName) {
        return SpriteTexture.of(
                PortResourceLocation.of("eyelib", "snowstorm/icons/" + lucideName + ".png").toString());
    }
}
//?}

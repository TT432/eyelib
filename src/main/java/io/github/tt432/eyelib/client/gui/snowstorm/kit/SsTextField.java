package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Snowstorm 样式输入框（common.css input as-is）：dark 底、1px border 边、text 色、
 * Consolas 等价（MC 默认字体代替，像素字体）。placeholder 支持。
 */
public final class SsTextField {

    private SsTextField() {
    }

    public static TextField of(String initial, Consumer<String> responder) {
        TextField field = new TextField();
        field.textFieldStyle(style -> style.fontSize(9));
        field.setText(initial, false);
        field.setTextResponder(responder);
        field.style(style -> style.backgroundTexture(
                new ColorRectTexture(SnowstormTheme.DARK)));
        return field;
    }

    public static TextField withPlaceholder(String placeholder, String initial, Consumer<String> responder) {
        TextField field = of(initial, responder);
        field.textFieldStyle(style -> style.placeholder(Component.literal(placeholder)));
        return field;
    }
}
//?}

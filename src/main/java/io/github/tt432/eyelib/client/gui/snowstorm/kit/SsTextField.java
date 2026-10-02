package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Snowstorm 样式输入框（common.css input as-is）：dark 底、1px border 边、text 色、
 * Consolas 等价（MC 默认字体代替，像素字体）。placeholder 支持。
 *
 * <p>实证 2026-10-01：LDLib2 样式表引擎在元素入树时重放默认 lss 规则，构造期
 * {@code textFieldStyle(...)} 的 plain set 对 text-color/font-size 等被规则覆盖
 * （text-shadow 等无规则属性不受影响）。文字属性必须走
 * {@link Style#importantPipeline}（important origin）才能生效。
 */
public final class SsTextField {

    private SsTextField() {
    }

    /**
     * 文字属性（important origin）：字号 9、无阴影（web 无 text-shadow）、指定字色、
     * 空占位符（LDLib2 默认 placeholder「Empty」非原版行为）。
     */
    public static void applyTextStyle(TextField field, int textColor) {
        Style.importantPipeline(field.getTextFieldStyle(), style -> {
            style.textColor(textColor);
            style.fontSize(9);
            style.textShadow(false);
            style.placeholder(Component.empty());
        });
    }

    public static TextField of(String initial, Consumer<String> responder) {
        TextField field = new TextField();
        applyTextStyle(field, SnowstormTheme.TEXT);
        field.setText(initial, false);
        field.setTextResponder(responder);
        field.style(style -> style.backgroundTexture(
                com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup.of(
                        new ColorRectTexture(SnowstormTheme.DARK),
                        // common.css input border: 1px solid var(--color-border)（内描边）
                        new com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture(-1, SnowstormTheme.BORDER))));
        return field;
    }

    public static TextField withPlaceholder(String placeholder, String initial, Consumer<String> responder) {
        TextField field = of(initial, responder);
        field.textFieldStyle(style -> style.placeholder(Component.literal(placeholder)));
        return field;
    }
}
//?}

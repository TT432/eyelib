package io.github.tt432.eyelib.client.gui.snowstorm.inputs;
//? if >=1.20.1 {

import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

/**
 * InputGroup.vue 的 color 控件（vue-color Chrome 取色器）。
 *
 * <p>JS 行为 as-is：取色 {@code v-model="input.value"}（hex8 #RRGGBBAA 字符串）+
 * {@code v-on:input="input.change($event, $el)}"（node 非 null → registerEdit cooldown，
 * 拖动合并 600ms）。LDLib2 ColorConfigurator 以 ARGB int 读写，视图侧做
 * hex8 ↔ ARGB 互转。
 *
 * <p>偏离：取色器外观为 LDLib2 ColorConfigurator（非 Chrome 取色器布局）。
 */
public final class ColorInputView extends UIElement {

    public ColorInputView(Input input) {
        layout(layout -> layout
                .widthPercent(100)
                .flexDirection(FlexDirection.ROW));
        ColorConfigurator configurator = new ColorConfigurator("",
                () -> hex8ToArgb(asString(input.getValue())),
                argb -> {
                    input.setValue(argbToHex8(argb)); // JS v-model
                    // JS change($event, $el)：node 非 null → registerEdit cooldown（拖动合并）
                    input.change(InputUiEvents.simpleEvent(), this);
                    InputViewFactory.notifyChanged();
                },
                0xFFFFFFFF,
                true);
        configurator.layout(layout -> layout.widthPercent(100));
        addChild(configurator);
    }

    private static String asString(@Nullable Object value) {
        return value != null ? JsSemantics.toJsString(value) : "#ffffff";
    }

    /** #RRGGBBAA（vue-color hex8）→ ARGB int；#RRGGBB 补 FF；非法 → 不透明白。 */
    static int hex8ToArgb(String hex) {
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        if (s.length() == 6) s = s + "ff";
        if (s.length() != 8) return 0xFFFFFFFF;
        long v;
        try {
            v = Long.parseLong(s, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
        int r = (int) (v >>> 24) & 0xFF;
        int g = (int) (v >>> 16) & 0xFF;
        int b = (int) (v >>> 8) & 0xFF;
        int a = (int) v & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** ARGB int → #RRGGBBAA。 */
    static String argbToHex8(int argb) {
        return String.format("#%02x%02x%02x%02x",
                (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }
}
//?}

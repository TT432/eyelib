package io.github.tt432.eyelib.client.gui.snowstorm.inputs;
//? if >=1.20.1 {

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * InputGroup.vue 的 number 控件（{@code <input type=number>}）：axis_count==1 单框，&gt;1 按轴分框。
 *
 * <p>JS 行为 as-is：{@code v-model="input.value"}（value setter：_value 保留原始文本、
 * Config 收 parseFloat 值）+ {@code v-on:input="input.change($event)}"（InputEvent → typing
 * 合并）；多轴元素直写（原始字符串，不 parse——export 侧 processValue 处理）。
 *
 * <p>偏离：step/min/max 在 JS 仅是浏览器 spinner 提示（不约束键入），LDLib2 STRING 框无
 * spinner，故不实现加减按钮也不做范围约束；number 输入无 focus→ExpressionBar 绑定（JS 同）。
 */
public final class NumberInputView extends UIElement {

    public NumberInputView(Input input) {
        layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.ROW)
                .gapAll(2));
        if (input.axis_count == 1) {
            addChild(field(input, -1, input.getValue()));
        } else {
            List<Object> values = values(input);
            for (int i = 0; i < input.axis_count; i++) {
                addChild(field(input, i, i < values.size() ? values.get(i) : ""));
            }
        }
    }

    private static List<Object> values(Input input) {
        Object v = input.getValue();
        return v instanceof List<?> list ? cast(list) : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> cast(List<?> list) {
        return (List<Object>) list;
    }

    static TextField field(Input input, int axis, @Nullable Object value) {
        TextField field = new TextField();
        // common.css input[type=number]：色 #b99cff；1px border var(--color-border)
        io.github.tt432.eyelib.client.gui.snowstorm.kit.SsTextField.applyTextStyle(field,
                io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme.NUMBER);
        field.style(style -> style.backgroundTexture(
                com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup.of(
                        new com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture(
                                io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme.DARK),
                        new com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture(-1,
                                io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme.BORDER))));
        field.setText(value != null ? JsSemantics.toJsString(value) : "", false);
        field.setTextResponder(text -> {
            if (axis < 0) {
                input.setValue(text); // JS v-model（number setter：Config 收 parseFloat）
            } else {
                List<Object> list = cast((List<?>) java.util.Objects.requireNonNull(input.getValue()));
                while (list.size() <= axis) list.add("");
                list.set(axis, text); // JS：原始字符串直写
            }
            input.change(InputUiEvents.typingEvent()); // JS v-on:input
            InputViewFactory.notifyChanged();
        });
        field.layout(layout -> layout.flex(1).heightPercent(100));
        return field;
    }
}
//?}

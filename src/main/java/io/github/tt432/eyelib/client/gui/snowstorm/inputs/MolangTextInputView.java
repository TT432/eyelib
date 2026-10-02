package io.github.tt432.eyelib.client.gui.snowstorm.inputs;
//? if >=1.20.1 {

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * InputGroup.vue 的 molang/text 控件（prism-editor）：axis_count==1 单框，&gt;1 按轴分框。
 *
 * <p>JS 行为 as-is：单轴 {@code v-model="input.value"}（value setter → Config 回写）+
 * {@code v-on:input="input.emitInput($event)}"（typing 合并）；多轴
 * {@code v-model="input.value[i-1]"}" 数组元素直接赋值（与 Config 共享引用，隐式更新）；
 * {@code v-on:focus="input.focus(...)"} → ExpressionBar 联动接缝。
 *
 * <p>偏离：LDLib2 TextField 单行（prism-editor 多行高亮/自动补全不移植，归 P5 ExpressionBar）；
 * expand 仅以行高放大近似。
 */
public final class MolangTextInputView extends UIElement {

    public MolangTextInputView(Input input) {
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

    /** 一个文本框；axis&lt;0 为单轴（JS focus(-1)），否则轴索引。 */
    static TextField field(Input input, int axis, @Nullable Object value) {
        TextField field = new TextField();
        io.github.tt432.eyelib.client.gui.snowstorm.kit.SsTextField.applyTextStyle(field,
                io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme.TEXT);
        // placeholder 同 origin（important）后写覆盖先写，避免被 applyTextStyle 的空占位压制
        com.lowdragmc.lowdraglib2.gui.ui.Style.importantPipeline(field.getTextFieldStyle(), style -> style
                .placeholder(Component.literal(input.placeholder != null ? input.placeholder : "")));
        // .prism-editor-component border: 1px solid var(--color-border)
        field.style(style -> style.backgroundTexture(
                com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup.of(
                        new com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture(
                                io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme.DARK),
                        new com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture(-1,
                                io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme.BORDER))));
        field.setText(value != null ? JsSemantics.toJsString(value) : "", false);
        field.setTextResponder(text -> {
            if (axis < 0) {
                input.setValue(text); // JS v-model="input.value"（setter）
            } else {
                // JS v-model="input.value[i-1]"：元素直写，越界则扩展数组
                List<Object> list = cast((List<?>) java.util.Objects.requireNonNull(input.getValue()));
                while (list.size() <= axis) list.add("");
                list.set(axis, text);
            }
            input.change(InputUiEvents.typingEvent()); // JS v-on:input（InputEvent → typing 合并）
            InputViewFactory.notifyChanged();
        });
        // JS v-on:focus="input.focus(...)（prism-editor 专属，number 无此绑定）
        field.addEventListener(UIEvents.FOCUS, event -> input.focus(axis));
        field.layout(layout -> layout.flex(1).heightPercent(100));
        return field;
    }
}
//?}

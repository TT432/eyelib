package io.github.tt432.eyelib.client.gui.snowstorm.inputs;
//? if >=1.20.1 {

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputType;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Objects;

/**
 * InputGroup.vue 的 select / select_custom 控件。
 *
 * <p>select：{@code <select v-model="input.meta_value">}（显示 label，option id 为 key）
 * + {@code v-on:change="input.change($event)}"（change() 经 selectedOptions[0].id 反查 key
 * 写 value）。LDLib2 Selector 直接以 key 为候选：onValueChanged(key) → setValue(key)
 * （setter 反查 meta_value）→ change（select change event，registerEdit 立即）。
 *
 * <p>select_custom 追加：{@code v-if="input.meta_value == input.options.custom"} 时显示
 * 自定义文本框（v-model="input.value" + input event → change，typing 合并）。
 */
public final class SelectInputView extends UIElement {

    public SelectInputView(Input input) {
        layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.ROW)
                .gapAll(2));

        Selector<String> selector = new Selector<>();
        java.util.List<String> keys = new ArrayList<>(input.options != null ? input.options.keySet() : java.util.List.<String>of());
        selector.setCandidates(keys);
        selector.setCandidateUIProvider(key -> {
            // Selector 初始化会传 null 候选（setupDialog 实证栈）：label 与 key 双守卫
            String label = key == null ? "" : (input.options != null ? input.options.get(key) : key);
            TextElement element = InputViewFactory.text(label != null ? label : key != null ? key : "", SnowstormTheme.TEXT, 9);
            element.textStyle(style -> style.textAlignHorizontal(Horizontal.LEFT));
            return element;
        });
        // 当前选中：value 为 key；select_custom 的自定义值不在 options 中 → 选中 'custom' 项
        // （JS：v-model 绑定 meta_value label，浏览器匹配到 custom 选项）
        String current = JsSemantics.toJsString(input.getValue());
        if (!keys.contains(current) && input.type == InputType.SELECT_CUSTOM) {
            current = "custom";
        }
        selector.setSelected(current, false);
        selector.setOnValueChanged(key -> {
            input.setValue(key); // setter：Config 回写 + meta_value 反查
            input.change(InputUiEvents.selectChangeEvent(key)); // JS <select> change event
            InputViewFactory.notifyChanged(); // meta_value 变化可能影响 custom 框可见性
        });
        // common.css select：dark 底 + 1px border var(--color-border)
        selector.style(style -> style.backgroundTexture(
                com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup.of(
                        new com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture(SnowstormTheme.DARK),
                        new com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture(-1, SnowstormTheme.BORDER))));
        selector.layout(layout -> layout.flex(1).heightPercent(100));
        addChild(selector);

        if (input.type == InputType.SELECT_CUSTOM
                && input.options != null
                && Objects.equals(input.meta_value, input.options.get("custom"))) {
            com.lowdragmc.lowdraglib2.gui.ui.elements.TextField customField =
                    new com.lowdragmc.lowdraglib2.gui.ui.elements.TextField();
            io.github.tt432.eyelib.client.gui.snowstorm.kit.SsTextField.applyTextStyle(customField, SnowstormTheme.TEXT);
            customField.setText(JsSemantics.toJsString(input.getValue()), false);
            customField.setTextResponder(text -> {
                input.setValue(text); // JS v-model="input.value"
                input.change(InputUiEvents.typingEvent()); // JS v-on:input
                InputViewFactory.notifyChanged();
            });
            customField.layout(layout -> layout.flex(1).heightPercent(100));
            addChild(customField);
        }
    }

}
//?}

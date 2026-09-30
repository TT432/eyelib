package io.github.tt432.eyelib.client.gui.snowstorm.inputs;
//? if >=1.20.1 {

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;

/**
 * InputGroup.vue 的 checkbox 控件（Form/Checkbox.vue：点击切换 Square/CheckSquare）。
 *
 * <p>JS 行为 as-is：toggle → {@code $emit('input', !value)}（v-model → value setter →
 * Config 回写）+ {@code $emit('change')}（{@code input.change($event)}，click event →
 * registerEdit 立即派发）。
 *
 * <p>偏离：外观用 LDLib2 Toggle（滑块样式）替代 Square/CheckSquare 图标。
 */
public final class CheckboxInputView extends UIElement {

    public CheckboxInputView(Input input) {
        layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER));
        Toggle toggle = new Toggle();
        toggle.setOn(JsSemantics.truthy(input.getValue()), false);
        toggle.setOnToggleChanged(on -> {
            input.setValue(on); // JS v-model（setter → Config）
            input.change(InputUiEvents.simpleEvent()); // JS @change（click event）
            InputViewFactory.notifyChanged();
        });
        toggle.layout(layout -> layout.width(20).height(10));
        addChild(toggle);
    }
}
//?}

package io.github.tt432.eyelib.client.gui.snowstorm.inputs;
//? if >=1.20.1 {

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIcon;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;

/**
 * InputGroup.vue 的 checkbox 控件（Form/Checkbox.vue as-is：Square/CheckSquare 图标切换）。
 *
 * <p>JS 行为 as-is：toggle → {@code $emit('input', !value)}（v-model → value setter →
 * Config 回写）+ {@code $emit('change')}（{@code input.change($event)}，click event →
 * registerEdit 立即派发）。
 */
public final class CheckboxInputView extends UIElement {

    public CheckboxInputView(Input input) {
        layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER));
        rebuild(input);
        addEventListener(UIEvents.CLICK, event -> {
            input.setValue(!JsSemantics.truthy(input.getValue())); // JS v-model（setter → Config）
            input.change(InputUiEvents.simpleEvent()); // JS @change（click event）
            InputViewFactory.notifyChanged();
            rebuild(input);
        });
    }

    /** 勾选态图标刷新（Square/CheckSquare as-is）。 */
    private void rebuild(Input input) {
        clearAllChildren();
        addChild(SsIcon.of(JsSemantics.truthy(input.getValue()) ? "check-square" : "square", 10));
    }
}
//?}

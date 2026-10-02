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

    /** 勾选态图标刷新（Square/CheckSquare 21×21 as-is；勾选 path 色 #20ddff → 图标着色 ACCENT）。 */
    private void rebuild(Input input) {
        clearAllChildren();
        boolean checked = JsSemantics.truthy(input.getValue());
        UIElement icon = SsIcon.of(checked ? "check-square" : "square",
                io.github.tt432.eyelib.client.gui.snowstorm.kit.SsMetrics.CHECKBOX_SIZE);
        if (checked) {
            // Checkbox.vue: 勾选 path stroke #20ddff stroke-width 3 → 整图标着色近似
            icon.style(s -> s.backgroundTexture(
                    ((com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture)
                            io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIcon.texture("check-square"))
                            .setColor(io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme.ACCENT)));
        }
        addChild(icon);
    }
}
//?}

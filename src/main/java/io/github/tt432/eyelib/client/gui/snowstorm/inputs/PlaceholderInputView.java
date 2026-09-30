package io.github.tt432.eyelib.client.gui.snowstorm.inputs;
//? if >=1.20.1 {

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.input.Input;

import java.util.function.Consumer;

/**
 * gradient / image / event_list / event_timeline / event_speed_list 的占位条
 * （编辑器本体归 P5 切片）：暗底标签 + 点击占位回调（{@link InputViewFactory#placeholderAction}
 * 接缝，P5 接入时替换为对应编辑器视图）。
 */
public final class PlaceholderInputView extends UIElement {

    public PlaceholderInputView(Input input, String kindName) {
        layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.ROW)
                .justifyContent(AlignContent.CENTER)
                .alignItems(AlignItems.CENTER));
        style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.DARK)));
        addChild(InputViewFactory.text("[" + kindName + " — P5]", SnowstormTheme.TEXT_GRAYED, 9));
        addEventListener(UIEvents.CLICK, event -> {
            Consumer<Input> action = InputViewFactory.placeholderAction;
            if (action != null) action.accept(input);
        });
    }
}
//?}

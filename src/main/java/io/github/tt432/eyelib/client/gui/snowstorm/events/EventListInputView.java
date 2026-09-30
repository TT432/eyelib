//? if >=1.20.1 {
package io.github.tt432.eyelib.client.gui.snowstorm.events;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
//? if <26.1 {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
//?} else {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
//?}
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.input.Input;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * InputGroup.vue 的 event_list 输入形态：事件 id 行（标签 + X 删除）+
 * EventPicker 追加。JS as-is：{@code input.value.remove(event_id); input.change($event)}
 * / {@code input.value.push($event); input.change(event);}（click → 普通 Event，
 * registerEdit 立即派发）。value 为 Config 共享的字符串数组（引用语义 as-is）。
 */
public class EventListInputView extends UIElement {

    private final Input input;
    private String lastSig = "";

    public EventListInputView(Input input) {
        this.input = input;
        layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));
        rebuild();
    }

    @SuppressWarnings("unchecked")
    private List<Object> values() {
        return (List<Object>) input.getValue();
    }

    private String sig() {
        return String.valueOf(values().size());
    }

    //? if <26.1 {
    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        if (!sig().equals(lastSig)) rebuild();
    }
    //?} else {
    @Override
    protected void drawBackgroundAdditional(IGUIContext context) {
        if (!sig().equals(lastSig)) rebuild();
    }
    //?}

    private void rebuild() {
        lastSig = sig();
        clearAllChildren();
        List<Object> values = values();
        for (Object value : values) {
            String id = String.valueOf(value);
            UIElement row = new UIElement();
            row.layout(l -> l.widthPercent(100).height(14).flexDirection(FlexDirection.ROW));
            TextElement label = new TextElement();
            label.setText(Component.literal(id));
            label.textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT));
            label.layout(l -> l.flex(1).heightPercent(100));
            row.addChild(label);
            Button remove = new Button();
            remove.setText(Component.literal("×"))
                    .textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED))
                    .buttonStyle(s -> s
                            .baseTexture(IGuiTexture.EMPTY)
                            .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                    .setOnClick(e -> {
                        values.remove(value);
                        input.change(EventUiEvents.simple(), null);
                    });
            remove.layout(l -> l.width(16).heightPercent(100));
            row.addChild(remove);
            addChild(row);
        }
        addChild(new EventPickerView(id -> {
            values.add(id);
            input.change(EventUiEvents.simple(), null);
        }));
    }
}
//?}

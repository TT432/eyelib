//? if >=1.20.1 {
package io.github.tt432.eyelib.client.gui.snowstorm.events;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
//? if <26.1 {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
//?} else {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
//?}
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * InputGroup.vue 的 event_speed_list 输入形态（motion collision events 唯一用法，
 * input_structure.js）：{@code {event, min_speed}} 对象行（事件 id 标签 + X +
 * Min Speed 数值框）+ EventPicker 追加（{@code push({event, min_speed: 0})}）。
 *
 * <p>JS as-is：X/picker 走 {@code input.change($event)}（click → 立即派发）；
 * min_speed 用 v-model.number 实写、@change（提交）才 {@code input.change}
 * ——此处数值框 responder 实写 Map（v-model 等价）、BLUR 提交 change
 * （ChangeEvent 立即派发；per-keystroke 注册的偏差以 blur 口径收敛）。</p>
 */
public class EventSpeedListView extends UIElement {

    private final Input input;
    private String lastSig = "";

    public EventSpeedListView(Input input) {
        this.input = input;
        layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));
        rebuild();
    }

    @SuppressWarnings("unchecked")
    private List<Object> values() {
        return (List<Object>) java.util.Objects.requireNonNull(input.getValue());
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

    @SuppressWarnings("unchecked")
    private void rebuild() {
        lastSig = sig();
        clearAllChildren();
        List<Object> values = values();
        for (Object value : values) {
            Map<String, Object> eventObj = (Map<String, Object>) value;
            String id = String.valueOf(eventObj.get("event"));
            UIElement row = new UIElement();
            row.layout(l -> l.widthPercent(100).height(14).flexDirection(FlexDirection.ROW).gapAll(2));

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

            // Min Speed（v-model.number + @change）
            TextElement speedLabel = new TextElement();
            speedLabel.setText(Component.literal("Min Speed"));
            speedLabel.textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED));
            speedLabel.layout(l -> l.heightPercent(100));
            row.addChild(speedLabel);
            TextField speed = new TextField();
            speed.textFieldStyle(s -> s.fontSize(9));
            speed.setText(JsSemantics.toJsString(eventObj.get("min_speed")), false);
            speed.setTextResponder(text ->
                    eventObj.put("min_speed", JsonValues.jsParseFloat(text))); // v-model.number 实写
            speed.addEventListener(UIEvents.BLUR, e -> input.change(EventUiEvents.simple(), null)); // @change 提交
            speed.layout(l -> l.width(48).heightPercent(100));
            row.addChild(speed);

            addChild(row);
        }
        addChild(new EventPickerView(id -> {
            Map<String, Object> eventObj = new LinkedHashMap<>();
            eventObj.put("event", id);
            eventObj.put("min_speed", 0.0);
            values.add(eventObj);
            input.change(EventUiEvents.simple(), null);
        }));
    }
}
//?}

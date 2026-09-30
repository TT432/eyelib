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
import io.github.tt432.eyelib.snowstorm.editor.EditHistory;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * InputGroup.vue 的 event_timeline 输入形态：顶部 EventPicker 追加时间点
 * （{@code {uuid: guid(), event: [event_id], time: 0}} → {@code input.change}），
 * 每时间点一行：时间数值框（{@code @input edit('change event timeline')} typing +
 * {@code @blur input.change}）+ 行内事件列表（X 删除 / EventPicker 追加）+
 * 行尾 X 删除时间点。
 *
 * <p>{@code input.timeline} 读写与 {@code input.change()} 的 timeline→value 回写
 * 均由 {@link Input} 模型完成（as-is）。</p>
 */
public class EventTimelineView extends UIElement {

    private final Input input;
    private String lastSig = "";

    public EventTimelineView(Input input) {
        this.input = input;
        layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));
        rebuild();
    }

    private String sig() {
        StringBuilder sb = new StringBuilder();
        sb.append(input.timeline.size());
        for (Input.TimelineEntry entry : input.timeline) {
            sb.append('|').append(System.identityHashCode(entry)).append(':').append(entry.event.size());
        }
        return sb.toString();
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

        // 顶部 addEventToTimeline picker
        addChild(new EventPickerView(id -> {
            Input.TimelineEntry entry = new Input.TimelineEntry();
            entry.uuid = SnowstormUtil.guid();
            entry.event.add(id);
            entry.time = 0;
            input.timeline.add(entry);
            input.change(EventUiEvents.simple(), null);
        }));

        for (Input.TimelineEntry entry : input.timeline) {
            addChild(buildEntry(entry));
        }
    }

    private UIElement buildEntry(Input.TimelineEntry entry) {
        UIElement container = new UIElement();
        container.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));

        UIElement row = new UIElement();
        row.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(2));

        // 时间数值框：v-model.number entry.time + @input edit('change event timeline') + @blur input.change
        TextField time = new TextField();
        time.textFieldStyle(s -> s.fontSize(9));
        time.setText(JsSemantics.toJsString(entry.time), false);
        time.setTextResponder(text -> {
            entry.time = JsonValues.jsParseFloat(text); // JS v-model.number（空串 quirk 见报告）
            EditHistory.registerEdit("change event timeline", true); // JS edit($event, ...) InputEvent → typing
        });
        time.addEventListener(UIEvents.BLUR, e -> input.change(EventUiEvents.simple(), null));
        time.layout(l -> l.width(48).height(14));
        row.addChild(time);

        // 行内事件列表（label + X / picker 追加）
        UIElement eventList = new UIElement();
        eventList.layout(l -> l.flex(1).flexDirection(FlexDirection.COLUMN).gapAll(2));
        for (Object eventObj : entry.event) {
            String id = String.valueOf(eventObj);
            UIElement eventRow = new UIElement();
            eventRow.layout(l -> l.widthPercent(100).height(14).flexDirection(FlexDirection.ROW));
            TextElement label = new TextElement();
            label.setText(Component.literal(id));
            label.textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT));
            label.layout(l -> l.flex(1).heightPercent(100));
            eventRow.addChild(label);
            Button remove = new Button();
            remove.setText(Component.literal("×"))
                    .textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED))
                    .buttonStyle(s -> s
                            .baseTexture(IGuiTexture.EMPTY)
                            .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                    .setOnClick(e -> {
                        entry.event.remove(eventObj);
                        input.change(EventUiEvents.simple(), null);
                    });
            remove.layout(l -> l.width(16).heightPercent(100));
            eventRow.addChild(remove);
            eventList.addChild(eventRow);
        }
        eventList.addChild(new EventPickerView(id -> {
            entry.event.add(id);
            input.change(EventUiEvents.simple(), null);
        }));
        row.addChild(eventList);

        // 删除时间点
        Button removeEntry = new Button();
        removeEntry.setText(Component.literal("×"))
                .textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED))
                .buttonStyle(s -> s
                        .baseTexture(IGuiTexture.EMPTY)
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                .setOnClick(e -> {
                    input.timeline.remove(entry);
                    input.change(EventUiEvents.simple(), null);
                });
        removeEntry.layout(l -> l.width(16).height(14));
        row.addChild(removeEntry);

        container.addChild(row);
        return container;
    }
}
//?}

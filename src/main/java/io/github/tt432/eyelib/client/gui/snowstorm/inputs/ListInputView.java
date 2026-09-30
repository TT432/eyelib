package io.github.tt432.eyelib.client.gui.snowstorm.inputs;
//? if >=1.20.1 {

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * InputGroup.vue 的 axis_count==-1 列表形态（Form/ListAddButton.vue）：
 * 顶部 add 工具 + 每行文本框 + 行尾删除工具。
 *
 * <p>JS 行为 as-is：add {@code input.value.push('')}（<b>不触发 change/registerEdit</b>，
 * quirk 保留；数组与 Config 共享引用故隐式生效）；行编辑
 * {@code v-model="input.value[index]"} + {@code emitInput}（typing 合并）+
 * {@code focus(index)}；删除 {@code input.value.remove(item); input.change($event)}。
 *
 * <p>JS 模板里 number 列表行绑定的是整个 {@code input.value}（Vue bug，且 81 输入骨架中
 * 无 axis_count==-1 的 number 输入，不可达）；此处按意图实现为逐元素绑定并记录。
 */
public final class ListInputView extends UIElement {

    private final Input input;

    public ListInputView(Input input) {
        this.input = input;
        layout(layout -> layout
                .widthPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(2));
        rebuild();
    }

    @SuppressWarnings("unchecked")
    private List<Object> values() {
        return (List<Object>) input.getValue();
    }

    private void rebuild() {
        clearAllChildren();
        // JS .list_add_tool（＋，U+FF0B）
        Button add = new Button();
        add.setText(Component.literal("＋"))
                .textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED))
                .setOnClick(event -> {
                    values().add(""); // as-is quirk：push 不调 change()
                    rebuild();
                    InputViewFactory.notifyChanged();
                });
        add.layout(layout -> layout.widthPercent(100).height(10));
        addChild(add);

        List<Object> values = values();
        for (int i = 0; i < values.size(); i++) {
            final int index = i;
            final Object item = values.get(i);
            UIElement row = new UIElement().layout(layout -> layout
                    .widthPercent(100)
                    .height(InputViewFactory.ROW_HEIGHT)
                    .flexDirection(FlexDirection.ROW)
                    .gapAll(2));
            TextField field = input.type == io.github.tt432.eyelib.snowstorm.input.InputType.NUMBER
                    ? NumberInputView.field(input, index, item)
                    : MolangTextInputView.field(input, index, item);
            row.addChild(field);
            // JS 删除工具（✕，U+2A09）：remove(item) + change
            Button remove = new Button();
            remove.setText(Component.literal("✕"))
                    .textStyle(style -> style.fontSize(8).textColor(SnowstormTheme.TEXT_GRAYED))
                    .setOnClick(event -> {
                        values().remove(item); // JS Array.prototype.remove（按值删首个）
                        input.change(InputUiEvents.simpleEvent());
                        rebuild();
                        InputViewFactory.notifyChanged();
                    });
            remove.layout(layout -> layout.width(10).heightPercent(100));
            row.addChild(remove);
            addChild(row);
        }
    }
}
//?}

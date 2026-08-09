//? if >=1.20.1 {
package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib2.configurator.ui.ValueConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.nodegraph.InlineLiteral;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 字面值列表配置器（v11 变长 call 参数，规格 nodegraph-variadic-call-list）：
 * 节点内列表编辑器——每行一个字面值文本框（行内字面值口径，{@link InlineLiteral}）
 * + 删除键，底部 + 追加行。底层选项值是 JSON 数组文本（{@code String}）。
 *
 * <p>条目编辑只写回不重建行（重建会丢输入焦点）；增删行才重建。外部值变更
 * （undo 等）经 {@link #onValueUpdatePassively} 重建。
 */
public final class EvmListConfigurator extends ValueConfigurator<String> {

    private final UIElement listContainer;

    public EvmListConfigurator(Supplier<String> supplier, Consumer<String> onUpdate,
                               String defaultValue, boolean forceUpdate) {
        super("", supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) {
            value = defaultValue;
        }
        inlineContainer.addChild(listContainer = new UIElement()
                .layout(layout -> layout.widthPercent(100)));
        rebuildRows();
    }

    private void rebuildRows() {
        listContainer.clearAllChildren();
        JsonArray entries = parse(value);
        for (int i = 0; i < entries.size(); i++) {
            final int index = i;
            UIElement row = new UIElement().layout(layout -> layout
                    .widthPercent(100)
                    .height(12)
                    .flexDirection(FlexDirection.ROW));
            TextField field = new TextField();
            field.textFieldStyle(style -> style.fontSize(9));
            field.setText(InlineLiteral.toText(entries.get(i)), false);
            field.setTextResponder(text -> updateEntry(index, text));
            field.layout(layout -> layout.flex(1).heightPercent(100));
            Button delete = new Button();
            delete.setText(Component.literal("×"));
            delete.textStyle(style -> style.fontSize(9));
            delete.setOnClick(event -> removeEntry(index));
            delete.layout(layout -> layout.width(12).heightPercent(100));
            row.addChildren(field, delete);
            listContainer.addChild(row);
        }
        Button add = new Button();
        add.setText(Component.literal("+"));
        add.textStyle(style -> style.fontSize(9));
        add.setOnClick(event -> addEntry());
        add.layout(layout -> layout.widthPercent(100).height(12));
        listContainer.addChild(add);
    }

    /** 条目编辑：写回不重建（重建丢焦点）。 */
    private void updateEntry(int index, String text) {
        JsonArray entries = parse(value);
        if (index >= entries.size()) {
            return;
        }
        entries.set(index, InlineLiteral.parse(text));
        updateValueActively(entries.toString());
    }

    private void addEntry() {
        JsonArray entries = parse(value);
        entries.add("");
        updateValueActively(entries.toString());
        rebuildRows();
    }

    private void removeEntry(int index) {
        JsonArray entries = parse(value);
        if (index >= entries.size()) {
            return;
        }
        entries.remove(index);
        updateValueActively(entries.toString());
        rebuildRows();
    }

    @Override
    protected void onValueUpdatePassively(String newValue) {
        if (newValue == null) {
            newValue = defaultValue;
        }
        if (newValue.equals(value)) {
            return;
        }
        super.onValueUpdatePassively(newValue);
        rebuildRows();
    }

    /** 解析 JSON 数组文本；非法/非数组 → 空数组。 */
    private static JsonArray parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new JsonArray();
        }
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            return parsed.isJsonArray() ? parsed.getAsJsonArray() : new JsonArray();
        } catch (Exception e) {
            return new JsonArray();
        }
    }
}
//?}

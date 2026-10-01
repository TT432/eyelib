//? if >=1.20.1 {
package io.github.tt432.eyelib.client.gui.snowstorm.events;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.event.EventPicker;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * EventPicker.vue 的 LDLib2 复刻：高亮按钮（Plus/Zap 图标）+ 事件 id 下拉。
 * 选项来自 {@code Object.keys(Config.events)}（blacklist 过滤），
 * 数据侧复用 {@link EventPicker}（EditorRuntime.Config）。
 *
 * <p>偏离：JS 下拉为 absolute 浮层且 document 级外点关闭；此处为文档流内展开列表，
 * 选择或再次点击按钮关闭（外点关闭无 MUI 级钩子）。图标以字符近似
 * （Plus → ＋，Zap → ⚡，字形缺失时占位）。</p>
 */
public class EventPickerView extends UIElement {

    private final EventPicker picker = new EventPicker(EditorRuntime.Config);
    private final Consumer<String> onSelect;
    private final Button button;
    private final UIElement list = new UIElement();
    private boolean isOpen;

    /** JS 无 blacklist/replace 的常用形态（+ 按钮）。 */
    public EventPickerView(Consumer<String> onSelect) {
        this(onSelect, null, false);
    }

    /**
     * @param blacklist JS props.blacklist（过滤的事件 id）
     * @param replace   JS props.replace（Zap 图标，用于替换语义）
     */
    public EventPickerView(Consumer<String> onSelect, @Nullable List<String> blacklist, boolean replace) {
        this.onSelect = onSelect;
        picker.blacklist = blacklist;

        layout(l -> l.flexDirection(FlexDirection.COLUMN));
        // lucide 图标（replace=Zap / 追加=Plus，as-is）
        button = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.bar(
                replace ? "zap" : "plus", 10, e -> toggleMenu());
        button.layout(l -> l.width(22).height(14));
        list.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN));
        addChildren(button, list);
    }

    /** JS openMenu/closeMenu（外点关闭改为再次点击关闭，见类文档）。 */
    private void toggleMenu() {
        isOpen = !isOpen;
        rebuildList();
    }

    private void rebuildList() {
        list.clearAllChildren();
        if (!isOpen) return;
        List<String> ids = picker.getEventIDs();
        if (ids.isEmpty()) {
            // JS: No events available
            TextElement empty = new TextElement();
            empty.setText(Component.literal("No events available"));
            empty.textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED));
            empty.layout(l -> l.widthPercent(100).height(14));
            list.addChild(empty);
            return;
        }
        for (String id : ids) {
            Button item = new Button();
            item.setText(Component.literal("⚡ " + id))
                    .textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT))
                    .buttonStyle(s -> s
                            .baseTexture(new ColorRectTexture(SnowstormTheme.DARK))
                            .hoverTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)))
                    .setOnClick(e -> selectEntry(id));
            item.layout(l -> l.widthPercent(100).height(14));
            list.addChild(item);
        }
    }

    /** JS select(option, event) → $emit('select', option) + closeMenu。 */
    private void selectEntry(String id) {
        onSelect.accept(id);
        isOpen = false;
        rebuildList();
    }
}
//?}

package io.github.tt432.eyelib.client.gui.snowstorm.inputs;
//? if >=1.20.1 {

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * InputGroup.vue 的控件分发（12 种 InputType + axis_count 形态）。
 *
 * <p>每种类型一个视图类：molang/text → {@link MolangTextInputView}、number →
 * {@link NumberInputView}、checkbox → {@link CheckboxInputView}、select/select_custom →
 * {@link SelectInputView}、color → {@link ColorInputView}；axis_count == -1 →
 * {@link ListInputView}（列表形态）；gradient/image/event_list/event_timeline/
 * event_speed_list → 各编辑器视图（P5 切片落地，集成接线）。
 *
 * <p>Vue 响应式替代（ADR R3）：任何控件 change/toggle/结构变化后调
 * {@link #notifyChanged()}，宿主（Sidebar 集成）重建 group 以刷新
 * isVisible/_selected_mode/meta_value 驱动的可见性与显示。
 */
public final class InputViewFactory {

    /** 行高（SidebarView.INPUT_ROW_HEIGHT=11 同量级）。 */
    public static final int ROW_HEIGHT = 12;
    /** expand 状态行高（.expanded 的近似：展开的多行编辑区）。 */
    public static final int EXPANDED_HEIGHT = 44;
    public static final int LABEL_WIDTH = 62;

    /** 任一输入变更后的重绘请求（Vue 响应式替代；由 Sidebar 集成注入）。 */
    public static @Nullable Runnable onInputChanged;

    private InputViewFactory() {
    }

    public static void notifyChanged() {
        Runnable callback = onInputChanged;
        if (callback != null) callback.run();
    }

    /** InputGroup.vue li.input_wrapper：label（可选）+ expand 按钮（expandable）+ 控件。 */
    public static UIElement create(Input input) {
        boolean tall = input.expandable && input.expanded;
        UIElement row = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(tall ? EXPANDED_HEIGHT : ROW_HEIGHT)
                .flexDirection(FlexDirection.ROW)
                .paddingHorizontal(2)
                .gapAll(2));
        // JS v-bind:title="input.info"
        if (input.info != null && !input.info.isEmpty()) {
            row.style(style -> style.tooltips(input.info));
        }
        if (input.label != null) {
            TextElement label = text(input.label, SnowstormTheme.TEXT, 9);
            label.layout(layout -> layout.width(LABEL_WIDTH).heightPercent(100));
            row.addChild(label);
        }
        if (input.expandable) {
            // InputGroup.vue .input_expand_button（ChevronDown/Up → lucide 图标）
            Button expand = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.ghost(
                    input.expanded ? "chevron-up" : "chevron-down", 10, event -> {
                        input.toggleExpand(); // InputGroup.vue toggleExpand as-is
                        notifyChanged();
                    });
            expand.layout(layout -> layout.width(12).heightPercent(100));
            row.addChild(expand);
        }
        UIElement control = control(input);
        control.layout(layout -> layout.flex(1).heightPercent(100));
        row.addChild(control);
        return row;
    }

    /** InputGroup.vue {@code v-for + v-show=isVisible}：group 内全部可见输入行（键序 as-is）。 */
    public static List<UIElement> createForGroup(InputStructure.Group group) {
        List<UIElement> rows = new ArrayList<>();
        for (Input input : group.inputs.values()) {
            if (input.isVisible(group)) {
                rows.add(create(input));
            }
        }
        return rows;
    }

    /** 按 InputType 分发控件（axis_count == -1 一律列表形态，InputGroup.vue 模板分支 as-is）。 */
    public static UIElement control(Input input) {
        if (input.axis_count == -1) {
            return new ListInputView(input);
        }
        return switch (input.type) {
            case MOLANG, TEXT -> new MolangTextInputView(input);
            case NUMBER -> new NumberInputView(input);
            case CHECKBOX -> new CheckboxInputView(input);
            case SELECT, SELECT_CUSTOM -> new SelectInputView(input);
            case COLOR -> new ColorInputView(input);
            // 集成（P6）：gradient→GradientEditorView（Gradient extends Input）；image→TextureEditorView
            // （绑定 TextureClass.Texture 全局单例，as-is）；event 三类→events 包视图
            case GRADIENT -> new io.github.tt432.eyelib.client.gui.snowstorm.gradient.GradientEditorView(
                    (io.github.tt432.eyelib.snowstorm.gradient.Gradient) input);
            case IMAGE -> io.github.tt432.eyelib.client.gui.snowstorm.texture.TextureEditorView.create();
            case EVENT_LIST -> new io.github.tt432.eyelib.client.gui.snowstorm.events.EventListInputView(input);
            case EVENT_TIMELINE -> new io.github.tt432.eyelib.client.gui.snowstorm.events.EventTimelineView(input);
            case EVENT_SPEED_LIST -> new io.github.tt432.eyelib.client.gui.snowstorm.events.EventSpeedListView(input);
        };
    }

    static TextElement text(String text, int color, int fontSize) {
        TextElement element = new TextElement();
        element.setText(Component.literal(text));
        element.textStyle(style -> style.fontSize(fontSize).textColor(color));
        return element;
    }
}
//?}

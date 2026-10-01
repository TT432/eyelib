//? if >=1.20.1 {
package io.github.tt432.eyelib.client.gui.snowstorm.events;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
//? if <26.1 {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
//?} else {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
//?}
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.event.EventSubpart;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * EventSubpart.vue 递归子部件渲染的 LDLib2 复刻：sequence（排序列）/ randomize
 * （weight 列 + 排序）/ particle_effect（identifier + type 4 种下拉 +
 * pre_effect_expression + 子效果文件按钮）/ sound_effect / expression +
 * create_bar（canCreate* 互斥可见性，domain 方法直用）。
 *
 * <p>文本编辑走 JS v-model 等价：字段直写 + {@code subpart.modifyEvent(null, "text")}
 * （JS 中 typing 由 DOM InputEvent 供给，domain 以 type=='text' 表达，效果一致）；
 * select @change 走 {@code modifyEvent(null, null)}（JS ChangeEvent 立即派发）。
 *
 * <p>结构签名（各节存在性 + sequence/randomize 长度）每帧对比，变化才重建
 * （Vue 重渲染等价；文本键入不触发重建，焦点保留）。</p>
 *
 * <p>偏离：prism-editor 高亮/自动完成不移植（P3 输入视图口径）；子效果文件对话框
 * （Select File/Select Texture）为宿主接缝静态字段（P6 接线，未接时点击无操作）；
 * Create New Particle 的模态 dialog 以行内表单条复刻；图标字符近似（≡/×/＋）。</p>
 */
public class EventSubpartView extends UIElement {

    /** Select File 宿主接缝：对话框得到 identifier 后应调 {@link EventSubpart#applySelectedParticleFile}。 */
    public static java.util.function.@Nullable Consumer<EventSubpart> selectParticleFileAction;
    /** Select Texture 宿主接缝：得到 imageUrl 后应调 {@link EventSubpart#applySelectedParticleTexture}。 */
    public static java.util.function.@Nullable Consumer<EventSubpart> selectParticleTextureAction;

    private static final Object DRAG_SEQUENCE = new Object();
    private static final Object DRAG_RANDOMIZE = new Object();

    private final EventSubpart subpart;
    private String lastSig = "";

    /** 当前 sequence/randomize 的 option 容器（排序 hover 计算用，rebuild 时重填）。 */
    private final List<UIElement> optionContainers = new ArrayList<>();
    private @Nullable Object dragKind;
    private int dragOriginal = -1;
    private int dragHover = -1;

    /** Create New Particle 行内表单（JS new_particle_dialog）。 */
    private boolean newParticleFormOpen;
    /** type 下拉展开态。 */
    private boolean typeDropdownOpen;

    public EventSubpartView(EventSubpart subpart) {
        this.subpart = subpart;
        layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN));
        rebuild();
    }

    private String sig() {
        return (subpart.sequence == null ? -1 : subpart.sequence.size()) + "|"
                + (subpart.randomize == null ? -1 : subpart.randomize.size()) + "|"
                + (subpart.particle_effect != null) + "|"
                + (subpart.sound_effect != null) + "|"
                + (subpart.expression != null) + "|"
                + newParticleFormOpen;
    }

    //? if <26.1 {
    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        syncFrame();
    }
    //?} else {
    @Override
    protected void drawBackgroundAdditional(IGUIContext context) {
        syncFrame();
    }
    //?}

    private void syncFrame() {
        if (!sig().equals(lastSig)) rebuild();
    }

    // ==================================================================
    // 结构重建
    // ==================================================================

    private void rebuild() {
        lastSig = sig();
        clearAllChildren();
        optionContainers.clear();
        dragKind = null;
        dragOriginal = dragHover = -1;

        if (subpart.sequence != null) {
            addChild(descriptorBar("Sequence", null));
            List<EventSubpart> sequence = subpart.sequence;
            for (int i = 0; i < sequence.size(); i++) {
                EventSubpart option = sequence.get(i);
                UIElement container = new UIElement();
                container.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN));
                container.addChild(optionHeader("grip-vertical", "#" + i, null,
                        () -> subpart.removeSequenceOption(option), DRAG_SEQUENCE, i));
                container.addChild(new EventSubpartView(option));
                optionContainers.add(container);
                addChild(container);
            }
            addChild(listAddButton("Add Sequence Option", subpart::addSequenceOption));
        }

        if (subpart.randomize != null) {
            addChild(descriptorBar("Randomize", null));
            List<EventSubpart> randomize = subpart.randomize;
            for (int i = 0; i < randomize.size(); i++) {
                EventSubpart option = randomize.get(i);
                UIElement container = new UIElement();
                container.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN));
                // Weight 数值框（v-model.number + @input modifyEvent → typing）
                TextField weight = numberField(
                        option.weight == null ? "" : JsSemantics.toJsString(option.weight), null, text -> {
                            double parsed = JsonValues.jsParseFloat(text);
                            if (!Double.isNaN(parsed)) option.weight = parsed; // JS 非数字存字符串，domain 无双精度外形态
                            subpart.modifyEvent(null, "text");
                        });
                weight.layout(l -> l.width(48).heightPercent(100));
                container.addChild(optionHeader("grip-vertical", "Weight", weight,
                        () -> subpart.removeRandomizeOption(option), DRAG_RANDOMIZE, i));
                container.addChild(new EventSubpartView(option));
                optionContainers.add(container);
                addChild(container);
            }
            addChild(listAddButton("Add Random Option", subpart::addRandomizeOption));
        }

        if (subpart.particle_effect != null) {
            addChild(buildParticleSection());
        }
        if (subpart.sound_effect != null) {
            addChild(buildSoundSection());
        }
        if (subpart.expression != null) {
            // title="Run a Molang expression on the event firing emitter"
            UIElement section = new UIElement();
            section.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN));
            section.style(s -> s.tooltips("Run a Molang expression on the event firing emitter"));
            section.addChild(descriptorBar("Expression", subpart::disableExpressionSection));
            TextField expression = numberField(subpart.expression, "", text -> {
                subpart.expression = text;
                subpart.modifyEvent(null, "text");
            });
            expression.layout(l -> l.widthPercent(100).height(14));
            section.addChild(expression);
            addChild(section);
        }

        addChild(buildCreateBar());
    }

    // ==================================================================
    // 各节
    // ==================================================================

    private UIElement buildParticleSection() {
        EventSubpart.ParticleEffect particle = java.util.Objects.requireNonNull(subpart.particle_effect);
        UIElement section = new UIElement();
        section.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN));
        section.addChild(descriptorBar("Particle Effect", subpart::disableParticleSection));

        // Identifier 行：文本框 + 文件按钮组（JS v-if 条件 as-is）
        UIElement identifierRow = new UIElement();
        identifierRow.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(2));
        identifierRow.addChild(label("Identifier", 52));
        TextField identifier = numberField(particle.effect, "space:name", text -> {
            particle.effect = text;
            subpart.modifyEvent(null, "text");
        });
        identifier.layout(l -> l.flex(1).heightPercent(100));
        identifierRow.addChild(identifier);
        if (!EventSubpart.is_extension && !subpart.canEditParticleFile()) {
            // Create New Particle（JS dialog → 行内表单）
            identifierRow.addChild(iconButton("file-plus-2", "Create New Particle", () -> {
                newParticleFormOpen = !newParticleFormOpen;
                rebuild();
            }));
        }
        if (!EventSubpart.is_extension) {
            identifierRow.addChild(iconButton("upload", "Select File", () -> {
                if (selectParticleFileAction != null) selectParticleFileAction.accept(subpart);
            }));
        }
        if (!EventSubpart.is_extension && subpart.canEditParticleFile()) {
            identifierRow.addChild(iconButton("image-plus", "Select Texture", () -> {
                if (selectParticleTextureAction != null) selectParticleTextureAction.accept(subpart);
            }));
        }
        if (subpart.canEditParticleFile()) {
            identifierRow.addChild(iconButton("pencil", "Edit Linked Particle Effect", subpart::editParticleFile));
        }
        section.addChild(identifierRow);

        // Create New Particle 行内表单（JS <dialog>：identifier 输入 + Confirm/Cancel）
        if (newParticleFormOpen) {
            UIElement form = new UIElement();
            form.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(2));
            form.addChild(label("Identifier", 52));
            TextField newId = numberField(particle.effect, "space:name", text -> {
            });
            newId.layout(l -> l.flex(1).heightPercent(100));
            form.addChild(newId);
            form.addChild(textButton("Confirm", () -> {
                subpart.applyCreateNewParticleFile(newId.getValue());
                newParticleFormOpen = false;
                rebuild();
            }));
            form.addChild(textButton("Cancel", () -> {
                newParticleFormOpen = false;
                rebuild();
            }));
            section.addChild(form);
        }

        // Type 行（emitter_type_options 4 种下拉）
        UIElement typeRow = new UIElement();
        typeRow.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(2));
        typeRow.addChild(label("Type", 52));
        typeRow.addChild(typeDropdown(particle));
        section.addChild(typeRow);

        // Expression 行（pre_effect_expression）
        UIElement expressionRow = new UIElement();
        expressionRow.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(2));
        expressionRow.addChild(label("Expression", 52));
        TextField preExpression = numberField(particle.pre_effect_expression, "", text -> {
            particle.pre_effect_expression = text;
            subpart.modifyEvent(null, "text");
        });
        preExpression.layout(l -> l.flex(1).heightPercent(100));
        expressionRow.addChild(preExpression);
        section.addChild(expressionRow);
        return section;
    }

    /** type 下拉（emitter / emitter_bound / particle / particle_with_velocity）。 */
    private UIElement typeDropdown(EventSubpart.ParticleEffect particle) {
        UIElement dropdown = new UIElement();
        dropdown.layout(l -> l.flex(1).flexDirection(FlexDirection.COLUMN));
        Button current = new Button();
        current.setText(Component.literal(EventSubpart.EMITTER_TYPE_OPTIONS.getOrDefault(particle.type, particle.type)))
                .textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT))
                .buttonStyle(s -> s
                        .baseTexture(new ColorRectTexture(SnowstormTheme.DARK))
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION)));
        current.layout(l -> l.widthPercent(100).height(14));
        UIElement options = new UIElement();
        options.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN));
        current.setOnClick(e -> {
            typeDropdownOpen = !typeDropdownOpen;
            options.clearAllChildren();
            if (typeDropdownOpen) {
                for (Map.Entry<String, String> entry : EventSubpart.EMITTER_TYPE_OPTIONS.entrySet()) {
                    Button item = new Button();
                    item.setText(Component.literal(entry.getValue()))
                            .textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT))
                            .buttonStyle(s -> s
                                    .baseTexture(new ColorRectTexture(SnowstormTheme.DARK))
                                    .hoverTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)))
                            .setOnClick(ev -> {
                                particle.type = entry.getKey();
                                subpart.modifyEvent(null, null); // JS @change → ChangeEvent 立即派发
                                typeDropdownOpen = false;
                                options.clearAllChildren();
                                current.setText(Component.literal(entry.getValue()));
                            });
                    item.layout(l -> l.widthPercent(100).height(14));
                    options.addChild(item);
                }
            }
        });
        dropdown.addChildren(current, options);
        return dropdown;
    }

    private UIElement buildSoundSection() {
        EventSubpart.SoundEffect sound = java.util.Objects.requireNonNull(subpart.sound_effect);
        UIElement section = new UIElement();
        section.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN));
        section.addChild(descriptorBar("Sound", subpart::disableSoundSection));
        UIElement row = new UIElement();
        row.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(2));
        row.addChild(label("Sound Event", 52));
        TextField name = numberField(sound.event_name, "block.bamboo.hit", text -> {
            sound.event_name = text;
            subpart.modifyEvent(null, "text");
        });
        name.layout(l -> l.flex(1).heightPercent(100));
        row.addChild(name);
        section.addChild(row);
        return section;
    }

    /** create_bar（canCreate* 互斥规则 → domain 方法 as-is）。 */
    private UIElement buildCreateBar() {
        UIElement bar = new UIElement();
        bar.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(2));
        if (subpart.canCreateParticleSection()) {
            bar.addChild(createButton("Particle", subpart::createParticleSection));
        }
        if (subpart.canCreateSoundSection()) {
            bar.addChild(createButton("Sound", subpart::createSoundSection));
        }
        if (subpart.canCreateExpressionSection()) {
            bar.addChild(createButton("Expression", subpart::createExpressionSection));
        }
        if (subpart.canCreateSequenceSection()) {
            bar.addChild(createButton("Sequence", subpart::createSequenceSection));
        }
        if (subpart.canCreateRandomizeSection()) {
            bar.addChild(createButton("Randomize", subpart::createRandomizeSection));
        }
        return bar;
    }

    // ==================================================================
    // 排序（sort.js → LDLib2 拖拽；数据侧 domain moveSequenceOption/moveRandomizeOption）
    // ==================================================================

    private void onSortDragUpdate(UIEvent e) {
        if (e.dragHandler == null || e.dragHandler.getDraggingObject() != dragKind || dragOriginal < 0) return;
        // JS move()：找到 :hover 的行，cursor 过半则 +1；无命中 → 移到末尾
        int hover = optionContainers.size();
        for (int i = 0; i < optionContainers.size(); i++) {
            UIElement row = optionContainers.get(i);
            float top = row.getPositionY();
            float height = row.getSizeHeight();
            if (e.y >= top && e.y < top + height) {
                hover = (e.y - top > height / 2) ? i + 1 : i;
                break;
            }
        }
        dragHover = hover;
    }

    private void onSortDragEnd(UIEvent e) {
        if (e.dragHandler == null || dragOriginal < 0) return;
        Object kind = e.dragHandler.getDraggingObject();
        if (kind != DRAG_SEQUENCE && kind != DRAG_RANDOMIZE) return;
        if (dragHover >= 0 && dragHover != dragOriginal) {
            // JS end()：sort() 结束后不触发 registerEdit（domain 口径）
            if (kind == DRAG_SEQUENCE) subpart.moveSequenceOption(dragOriginal, dragHover);
            else subpart.moveRandomizeOption(dragOriginal, dragHover);
        }
        dragKind = null;
        dragOriginal = dragHover = -1;
        rebuild();
    }

    // ==================================================================
    // 小部件工厂
    // ==================================================================

    /** 节标题条（descriptor 标签 + 可选禁用 X）。 */
    private UIElement descriptorBar(String title, @Nullable Runnable onDisable) {
        UIElement bar = new UIElement();
        bar.layout(l -> l.widthPercent(100).height(14).flexDirection(FlexDirection.ROW));
        bar.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.BAR)));
        TextElement label = new TextElement();
        label.setText(Component.literal(title));
        label.textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED));
        label.layout(l -> l.flex(1).heightPercent(100));
        bar.addChild(label);
        if (onDisable != null) {
            bar.addChild(iconButton("x", "Disable " + title, onDisable));
        }
        return bar;
    }

    /** sequence/randomize option 头条：排序握把 + 标签/字段 + fill + 删除。 */
    private UIElement optionHeader(String grip, String labelText, @Nullable TextField extraField,
                                   Runnable onRemove, Object dragToken, int index) {
        UIElement header = new UIElement();
        header.layout(l -> l.widthPercent(100).height(14).flexDirection(FlexDirection.ROW).gapAll(2));
        Button gripButton = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.ghost(
                grip, 10, e -> {
                });
        gripButton.layout(l -> l.width(12).heightPercent(100));
        gripButton.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button != 0) return;
            dragKind = dragToken;
            dragOriginal = index;
            dragHover = index;
            gripButton.startDrag(dragToken, null);
        });
        gripButton.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onSortDragUpdate);
        gripButton.addEventListener(UIEvents.DRAG_END, this::onSortDragEnd);
        header.addChild(gripButton);
        header.addChild(label(labelText, 40));
        if (extraField != null) header.addChild(extraField);
        UIElement fill = new UIElement();
        fill.layout(l -> l.flex(1).heightPercent(100));
        header.addChild(fill);
        header.addChild(iconButton("x", "Remove Option", onRemove));
        return header;
    }

    /** ListAddButton 形态（虚线框 + 居中 + 文本）。 */
    private UIElement listAddButton(String title, Runnable onClick) {
        Button button = new Button();
        button.setText(Component.literal("+ " + title))
                .textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED))
                .buttonStyle(s -> s
                        .baseTexture(new ColorRectTexture(SnowstormTheme.DARK))
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)))
                .setOnClick(e -> onClick.run());
        button.layout(l -> l.widthPercent(100).height(14));
        button.style(s -> s.tooltips(title));
        return button;
    }

    private UIElement createButton(String name, Runnable onClick) {
        Button button = new Button();
        button.setText(Component.literal("＋ " + name))
                .textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT))
                .buttonStyle(s -> s
                        .baseTexture(new ColorRectTexture(SnowstormTheme.BAR))
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                .setOnClick(e -> onClick.run());
        button.layout(l -> l.height(14));
        return button;
    }

    private UIElement iconButton(String icon, String title, Runnable onClick) {
        Button button = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.ghost(
                icon, 9, e -> onClick.run());
        button.layout(l -> l.width(16).heightPercent(100));
        button.style(s -> s.tooltips(title));
        return button;
    }

    private UIElement textButton(String text, Runnable onClick) {
        Button button = new Button();
        button.setText(Component.literal(text))
                .textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT))
                .buttonStyle(s -> s
                        .baseTexture(new ColorRectTexture(SnowstormTheme.BAR))
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                .setOnClick(e -> onClick.run());
        button.layout(l -> l.height(14));
        return button;
    }

    private TextElement label(String text, int width) {
        TextElement label = new TextElement();
        label.setText(Component.literal(text));
        label.textStyle(s -> s.fontSize(9).textColor(SnowstormTheme.TEXT));
        label.layout(l -> l.width(width).heightPercent(100));
        return label;
    }

    private TextField numberField(String initial, @Nullable String placeholder,
                                  java.util.function.Consumer<String> responder) {
        TextField field = new TextField();
        field.textFieldStyle(s -> {
            s.fontSize(9);
            if (placeholder != null) s.placeholder(Component.literal(placeholder));
        });
        field.setText(initial, false);
        field.setTextResponder(responder);
        return field;
    }
}
//?}

package io.github.tt432.eyelib.client.gui.snowstorm.stage;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.wintersky.Emitter;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Preview.vue 页脚控制条 as-is（ADR-0036 D4）：
 * loop_mode 下拉（Auto/Looping/Once）、parent_mode 下拉（World/Entity/Locator）、
 * ground_collision 开关、placeholder 栏「#」开关、play/pause、警告计数、粒子数/FPS。
 *
 * <p>占位偏差：警告按钮点击（WarningDialog 弹层）与 placeholder bake 对话框属 P5；
 * placeholder 栏的显隐持久化（localStorage snowstorm_show_placeholder_bar）未接
 * （EditorOptions 无此项），会话内保持。</p>
 */
public final class StageFooterBar extends UIElement {

    /** Preview.vue --footer-height: 34px。 */
    static final int HEIGHT = 34;
    /** footer 控件行高（footer > * padding 4px 8px → 内容 26px；取 26）。 */
    private static final int CONTROL_HEIGHT = 26;

    /** loop_mode 选项（Preview.vue select#loop_mode as-is）。 */
    private static final List<String> LOOP_MODES = List.of("auto", "looping", "once");
    private static final List<String> LOOP_LABELS = List.of("Auto", "Looping", "Once");
    /** parent_mode 选项（Preview.vue select#parent_mode as-is）。 */
    private static final List<String> PARENT_MODES = List.of("world", "entity", "locator");
    private static final List<String> PARENT_LABELS = List.of("World", "Entity", "Locator");

    private final ParticleStageView stage;

    /** Preview.vue data collision: true（显示态初值 as-is）。 */
    private boolean collisionDisplay = true;

    private final Button collisionButton;
    private final Button placeholderButton;
    private final TextElement warningLabel;
    private final TextElement particleLabel;
    private final TextElement fpsLabel;
    private final UIElement placeholderBar;
    private boolean placeholderBarVisible;

    public StageFooterBar(ParticleStageView stage) {
        this.stage = stage;
        layout(layout -> layout
                .flexDirection(FlexDirection.COLUMN));
        // Preview.vue footer：background var(--color-bar) + border-top 1px var(--color-border)
        //（ColorBorderTexture 四边同画，不符；顶边框用 1px 条实现）
        style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.BAR)));
        UIElement topBorder = new UIElement().layout(l -> l
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left(0).right(0).top(0).height(1));
        topBorder.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.BORDER)));
        addChild(topBorder);

        // placeholder 栏（Preview.vue .placeholder_bar：absolute bottom 34 覆盖画布下缘、
        // min-height 35、90% 透明底 + 顶边框；blur(4px) LDLib2 无等价——半透明近似）
        placeholderBar = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .paddingHorizontal(10)
                .paddingVertical(2)
                .gapAll(12));
        placeholderBar.style(style -> style.backgroundTexture(
                new ColorRectTexture(0xE629323A))); // color-mix(background 90%) 近似
        UIElement phTopBorder = new UIElement().layout(l -> l
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left(0).right(0).top(0).height(1));
        phTopBorder.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.BORDER)));
        placeholderBar.addChild(phTopBorder);
        placeholderBar.setDisplay(dev.vfyjxf.taffy.style.TaffyDisplay.NONE);

        UIElement bar = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(HEIGHT)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .paddingHorizontal(8)
                .gapAll(4));

        bar.addChild(buildSelector("loop_mode", LOOP_MODES, LOOP_LABELS,
                EditorRuntime.Emitter.loop_mode, value -> EditorRuntime.Emitter.loop_mode = value));
        bar.addChild(buildSelector("parent_mode", PARENT_MODES, PARENT_LABELS,
                EditorRuntime.Emitter.parent_mode, value -> EditorRuntime.Emitter.parent_mode = value));

        collisionButton = toolButton("flip-vertical-2", collisionDisplay, event -> {
            // Preview.vue toggleCollision()
            Emitter emitter = EditorRuntime.Emitter;
            emitter.ground_collision = !emitter.ground_collision;
            collisionDisplay = emitter.ground_collision;
            // 经事件源取按钮（避免 lambda 捕获初始化中的字段）
            refreshToggle((Button) event.currentElement, collisionDisplay);
        });
        refreshToggle(collisionButton, collisionDisplay);
        bar.addChild(collisionButton);

        placeholderButton = toolButton("hash", false, event -> togglePlaceholderBar());
        bar.addChild(placeholderButton);

        bar.addChild(new UIElement().layout(layout -> layout.flex(1).height(1))); // spacing

        bar.addChild(toolButton("play", false, event -> PlaybackController.startAnimation()));
        bar.addChild(toolButton("pause", false, event -> PlaybackController.togglePause()));

        bar.addChild(new UIElement().layout(layout -> layout.flex(1).height(1))); // spacing

        warningLabel = text("", SnowstormTheme.WARNING);
        // Preview.vue warnings_count 点击 → WarningDialog 弹层（有警告时）
        warningLabel.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.CLICK, event -> {
            if (stage.warningCount() > 0) {
                io.github.tt432.eyelib.client.gui.snowstorm.dialog.WarningDialogView.open(
                        getModularUI().ui.rootElement);
            }
        });
        bar.addChild(warningLabel);
        // Preview.vue div.stat：色 --color-text、min-width 72、右对齐
        particleLabel = text("0 P", SnowstormTheme.TEXT);
        particleLabel.textStyle(s -> s.textAlignHorizontal(Horizontal.RIGHT));
        particleLabel.layout(layout -> layout.minWidth(72));
        bar.addChild(particleLabel);
        fpsLabel = text("0 FPS", SnowstormTheme.TEXT);
        fpsLabel.textStyle(s -> s.textAlignHorizontal(Horizontal.RIGHT));
        fpsLabel.layout(layout -> layout.minWidth(72));
        bar.addChild(fpsLabel);

        addChildren(bar);
    }

    /** Preview.vue .placeholder_bar 浮层（由 {@link ParticleStageView} 绝对定位挂到画布下缘 bottom=34）。 */
    public UIElement placeholderBar() {
        return placeholderBar;
    }

    /** 每帧由舞台刷新统计（fps/粒子数/警告计数在舞台侧按 JS 周期采样）。 */
    void refreshStats() {
        int warnings = stage.warningCount();
        warningLabel.setText(Component.literal(warnings > 0 ? "⚠ " + warnings : ""));
        particleLabel.setText(Component.literal(formatThousands(stage.particleCount()) + " P"));
        fpsLabel.setText(Component.literal(stage.fps() + " FPS"));
    }

    /** Preview.vue computed particle_counter：千分位（(\d)(?=(\d{3})+(?!\d)) → $1,）。 */
    private static String formatThousands(int value) {
        String s = Integer.toString(value);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            int remaining = s.length() - i - 1;
            out.append(s.charAt(i));
            if (remaining > 0 && remaining % 3 == 0) {
                out.append(',');
            }
        }
        return out.toString();
    }

    // ==================================================================
    // placeholder 栏（Preview.vue placeholder_bar）
    // ==================================================================

    private void togglePlaceholderBar() {
        placeholderBarVisible = !placeholderBarVisible;
        placeholderBar.setDisplay(placeholderBarVisible
                ? dev.vfyjxf.taffy.style.TaffyDisplay.FLEX
                : dev.vfyjxf.taffy.style.TaffyDisplay.NONE);
        refreshToggle(placeholderButton, placeholderBarVisible);
        if (placeholderBarVisible) {
            // Preview.vue showPlaceholderBar()：updateVariablePlaceholderList(placeholder_keys)
            EditorRuntime.updateVariablePlaceholderList();
            rebuildPlaceholderBar();
        }
    }

    /** 按 placeholder_keys 重建栏内容（EditListeners['placeholder_bar'] 同款刷新语义）。 */
    void rebuildPlaceholderBar() {
        if (!placeholderBarVisible) {
            return;
        }
        placeholderBar.clearAllChildren();
        List<String> keys = EditorRuntime.placeholder_keys;
        if (!keys.isEmpty()) {
            for (String key : keys) {
                TextElement label = text(shortPlaceholderLabel(key), SnowstormTheme.TEXT);
                label.layout(layout -> layout.height(CONTROL_HEIGHT));
                TextField field = new TextField();
                io.github.tt432.eyelib.client.gui.snowstorm.kit.SsTextField.applyTextStyle(field, SnowstormTheme.NUMBER);
                Object current = EditorRuntime.placeholder_variables.get(key);
                field.setText(current != null ? JsSemantics.toJsString(current) : "0");
                field.setTextResponder(text -> updatePlaceholderValue(key, text));
                field.layout(layout -> layout.width(70).height(CONTROL_HEIGHT));
                placeholderBar.addChildren(label, field);
            }
        } else {
            TextElement empty = text("No undefined variables found", SnowstormTheme.TEXT_GRAYED);
            empty.layout(layout -> layout.height(CONTROL_HEIGHT));
            placeholderBar.addChild(empty);
        }
        // Preview.vue placeholder 栏 bake 按钮 → bake 对话框
        Button bake = new Button();
        bake.setText(net.minecraft.network.chat.Component.literal("Bake"))
                .textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT));
        bake.setOnClick(event -> io.github.tt432.eyelib.client.gui.snowstorm.dialog.PlaceholderBakeDialogView
                .open(getModularUI().ui.rootElement));
        bake.layout(layout -> layout.width(36).height(CONTROL_HEIGHT));
        placeholderBar.addChild(bake);
    }

    /** JS key.replace(key.substring(1, key.indexOf('.')), '')：variable.speed → v.speed。 */
    private static String shortPlaceholderLabel(String key) {
        int dot = key.indexOf('.');
        if (dot <= 1) {
            return key;
        }
        return key.substring(0, 1) + key.substring(dot);
    }

    /** Preview.vue updatePlaceholderValue：parseFloat(value)||0 后全量重建 placeholder_variables。 */
    private static void updatePlaceholderValue(String key, String text) {
        double parsed = JsonValues.jsParseFloat(text);
        double value = Double.isNaN(parsed) ? 0 : parsed;
        EditorRuntime.placeholder_variables.put(key, value);
        for (String k : EditorRuntime.placeholder_keys) {
            Object v = EditorRuntime.placeholder_variables.get(k);
            if (!JsSemantics.truthy(v)) {
                EditorRuntime.placeholder_variables.put(k, 0.0);
            }
        }
    }

    // ==================================================================
    // 控件构造
    // ==================================================================

    private static Selector<String> buildSelector(String id, List<String> values, List<String> labels,
                                                  @Nullable String initial,
                                                  java.util.function.Consumer<String> onChange) {
        Selector<String> selector = new Selector<>();
        selector.setCandidates(values);
        selector.setCandidateUIProvider(UIElementProvider.text(
                // LDLib2 初始/未知值会传 null 或候选外值：indexOf(null) NPE（ListN）、-1 越界，双守卫
                value -> {
                    int idx = value == null ? -1 : values.indexOf(value);
                    return Component.literal(idx >= 0 ? labels.get(idx) : "");
                }));
        selector.registerValueListener(onChange::accept);
        if (initial != null && values.contains(initial)) {
            selector.setValue(initial, false);
        } else {
            selector.setValue(values.get(0), false);
        }
        // Preview.vue footer select：height 100%（=34）、padding 2px 6px、margin-left 4、dark 底无边框；
        // 宽随最长标签（LDLib2 无内容自适应，用 MC font 度量 + 下拉箭头余量）
        int maxLabel = 0;
        for (String label : labels) {
            maxLabel = Math.max(maxLabel, net.minecraft.client.Minecraft.getInstance().font.width(label));
        }
        final int selectWidth = maxLabel + 14; // padding 6×2 + 余量（无箭头）
        // Preview.vue footer select：height 100%（=34）、padding 2px 6px、margin-left 4、dark 底、
        // 1px border（top 与 footer 顶边框重叠）；appearance:none → 隐藏 LDLib2 Selector 自带箭头
        selector.buttonIcon.setDisplay(false);
        selector.style(style -> style.backgroundTexture(
                com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup.of(
                        new ColorRectTexture(SnowstormTheme.DARK),
                        new com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture(-1, SnowstormTheme.BORDER))));
        selector.layout(layout -> layout.heightPercent(100).width(selectWidth).marginLeft(4)
                .paddingHorizontal(6));
        return selector;
    }

    private static Button toolButton(String icon, boolean selected,
                                     com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener listener) {
        // App.vue .tool：宽 35px、padding 2px 8px、图标 20px
        Button button = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.ghost(
                icon, 20, listener);
        button.layout(layout -> layout.width(35).heightPercent(100));
        refreshToggle(button, selected);
        return button;
    }

    /** Preview.vue .tool.toggle_enabled（选中态底 = --color-background，比 footer 的 BAR 更深）。 */
    private static void refreshToggle(Button button, boolean enabled) {
        button.buttonStyle(style -> style
                .baseTexture(enabled ? new ColorRectTexture(SnowstormTheme.BACKGROUND) : IGuiTexture.EMPTY)
                .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)));
    }

    private static TextElement text(String text, int color) {
        TextElement element = new TextElement();
        element.setText(Component.literal(text));
        element.textStyle(style -> style
                .fontSize(9)
                .textColor(color)
                .textAlignHorizontal(Horizontal.LEFT));
        return element;
    }
}
//?}

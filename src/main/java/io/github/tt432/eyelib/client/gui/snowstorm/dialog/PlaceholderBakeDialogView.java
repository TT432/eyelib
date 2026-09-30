package io.github.tt432.eyelib.client.gui.snowstorm.dialog;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.editor.VariablePlaceholders;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Preview.vue bake_placeholder_confirm_dialog as-is（P5-D）：placeholder bake 对话框。
 * placeholder_keys 逐键一行（短标签 + 值输入 + Bake 钮），Bake 进入内联确认态
 * （JS 文案 "Do you want to replace all occurrences of '{key}' with the value '{value}'?"），
 * Confirm 执行 {@link VariablePlaceholders#bakePlaceholderVariable} 后关闭。
 *
 * <p>偏离：JS 确认弹层复用 placeholder 栏的值且确认后栏保持打开；本对话框自带值输入，
 * Confirm 后整框关闭（一次性操作语义）。</p>
 *
 * <p>接入接缝（Main 集成）：placeholder 栏 bake 钮 →
 * {@code PlaceholderBakeDialogView.open(screenRoot)}。</p>
 */
public final class PlaceholderBakeDialogView extends UIElement {

    private static final int BLACKOUT = 0x50000000; // App.vue #dialog_blackout #00000050

    private final UIElement panel;

    /** 确认态目标（JS bake_placeholder_key）；null = 键列表态。 */
    private @Nullable String bakeKey;
    private @Nullable TextField pendingValueField;

    private PlaceholderBakeDialogView() {
        layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0).top(0).right(0).bottom(0)
                .flexDirection(FlexDirection.COLUMN)
                .justifyContent(AlignContent.CENTER)
                .alignItems(AlignItems.CENTER));
        style(style -> style.backgroundTexture(new ColorRectTexture(BLACKOUT)));

        panel = new UIElement().layout(layout -> layout
                .width(308) // JS modal_dialog max-width: 308px
                .flexDirection(FlexDirection.COLUMN)
                .paddingAll(10)
                .gapAll(4));
        panel.style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)));
        addChild(panel);

        rebuild();
    }

    /** 打开 bake 对话框（attachTo = 编辑器根 UIElement）。 */
    public static PlaceholderBakeDialogView open(UIElement attachTo) {
        PlaceholderBakeDialogView dialog = new PlaceholderBakeDialogView();
        attachTo.addChild(dialog);
        return dialog;
    }

    private void rebuild() {
        panel.clearAllChildren();
        if (bakeKey == null) {
            buildKeyList();
        } else {
            buildConfirm(bakeKey);
        }
    }

    // ==================================================================
    // 键列表态：placeholder_keys 逐键一行（Preview.vue placeholder_bar 的 bake 路径）
    // ==================================================================

    private void buildKeyList() {
        EditorRuntime.updateVariablePlaceholderList();
        List<String> keys = EditorRuntime.placeholder_keys;

        TextElement title = text("Bake Placeholder", SnowstormTheme.HIGHLIGHT, 12);
        panel.addChild(title);

        if (keys.isEmpty()) {
            // JS placeholder_bar 空态文案
            panel.addChild(text("No undefined variables found", SnowstormTheme.TEXT_GRAYED, 9));
        }
        ScrollerView list = new ScrollerView();
        list.layout(layout -> layout.widthPercent(100).height(Math.min(140, Math.max(18, keys.size() * 16))));
        for (String key : keys) {
            if (!key.startsWith("variable")) {
                // JS：bake 钮仅 variable.* 键显示（CheckCheck v-if="key.startsWith('variable')"）
                continue;
            }
            UIElement row = new UIElement().layout(layout -> layout
                    .widthPercent(100)
                    .height(16)
                    .flexDirection(FlexDirection.ROW)
                    .alignItems(AlignItems.CENTER)
                    .gapAll(4));

            TextElement label = text(shortPlaceholderLabel(key), SnowstormTheme.TEXT, 9);
            label.layout(layout -> layout.flex(1));

            TextField valueField = new TextField();
            Object current = EditorRuntime.placeholder_variables.get(key);
            valueField.setText(current != null ? JsSemantics.toJsString(current) : "0");
            valueField.setTextResponder(value -> {
                double parsed = JsonValues.jsParseFloat(value);
                EditorRuntime.placeholder_variables.put(key, Double.isNaN(parsed) ? 0.0 : parsed);
            });
            valueField.layout(layout -> layout.width(60).height(14));

            Button bake = new Button();
            bake.setText(Component.literal("Bake"))
                    .textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT))
                    .buttonStyle(style -> style
                            .baseTexture(new ColorRectTexture(SnowstormTheme.BAR))
                            .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                            .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                    .setOnClick(event -> {
                        bakeKey = key;
                        pendingValueField = valueField;
                        rebuild();
                    });
            bake.layout(layout -> layout.width(30).height(14));

            row.addChildren(label, valueField, bake);
            list.addScrollViewChild(row);
        }

        panel.addChild(list);
        panel.addChild(closeButton("Cancel"));
    }

    // ==================================================================
    // 确认态（bake_placeholder_confirm_dialog as-is 文案）
    // ==================================================================

    private void buildConfirm(String key) {
        String value = pendingValue();
        // JS: Do you want to replace all occurrences of '{{key}}' with the value '{{value}}'?
        TextElement question = text(
                "Do you want to replace all occurrences of '" + key + "' with the value '" + value + "'?",
                SnowstormTheme.TEXT, 9);
        question.textStyle(style -> style.textAlignHorizontal(Horizontal.LEFT));
        panel.addChild(question);

        UIElement buttonBar = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(16)
                .flexDirection(FlexDirection.ROW)
                .gapAll(6));
        Button confirm = actionButton("Confirm", () -> {
            VariablePlaceholders.bakePlaceholderVariable(key, value);
            removeSelf();
        });
        confirm.layout(layout -> layout.flex(1).heightPercent(100));
        Button cancel = actionButton("Cancel", () -> {
            bakeKey = null;
            rebuild();
        });
        cancel.layout(layout -> layout.flex(1).heightPercent(100));
        buttonBar.addChildren(confirm, cancel);
        panel.addChild(buttonBar);
    }

    /** JS placeholder_values[key] || 0：解析当前字段值，NaN/缺失 → 0，替换串为 JS 数字串。 */
    private String pendingValue() {
        String raw = pendingValueField != null ? pendingValueField.getText() : "";
        double parsed = JsonValues.jsParseFloat(raw);
        return JsSemantics.toJsString(Double.isNaN(parsed) ? 0.0 : parsed);
    }

    /** JS key.replace(key.substring(1, key.indexOf('.')), '')：variable.speed → v.speed。 */
    private static String shortPlaceholderLabel(String key) {
        int dot = key.indexOf('.');
        if (dot <= 1) {
            return key;
        }
        return key.substring(0, 1) + key.substring(dot);
    }

    private Button closeButton(String label) {
        Button button = actionButton(label, this::removeSelf);
        button.layout(layout -> layout.widthPercent(100).height(16));
        return button;
    }

    private static Button actionButton(String label, Runnable onClick) {
        Button button = new Button();
        button.setText(Component.literal(label))
                .textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT))
                .buttonStyle(style -> style
                        .baseTexture(new ColorRectTexture(SnowstormTheme.BAR))
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.SELECTION))
                        .pressedTexture(new ColorRectTexture(SnowstormTheme.SELECTION)))
                .setOnClick(event -> onClick.run());
        return button;
    }

    private static TextElement text(String text, int color, int fontSize) {
        TextElement element = new TextElement();
        element.setText(Component.literal(text));
        element.textStyle(style -> style.fontSize(fontSize).textColor(color));
        return element;
    }
}
//?}

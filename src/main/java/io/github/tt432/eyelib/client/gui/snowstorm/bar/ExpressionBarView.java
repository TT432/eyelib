package io.github.tt432.eyelib.client.gui.snowstorm.bar;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputType;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Snowstorm ExpressionBar.vue（ExpandedInput）as-is 移植（LDLib2，P3-C）。
 *
 * <p>构造即注入 {@link Input#expressionBar} 接缝：molang/text 输入 {@code focus()} 时
 * 经 {@link #updateText} 显示文本；栏内编辑经 {@code updateInput(text, true)} 回写
 * input.set/change（axis_count!=1 时按 axis 替换数组元素）。
 *
 * <p>偏离（vs JS）：
 * <ul>
 *   <li>Prism 语法高亮/自动补全不移植（纯文本单行 TextField）。</li>
 *   <li>focusing 分支的 prism 撤销历史重置无等价物（Prism 不移植）。</li>
 *   <li>ExpandedInput.axis 的 JS 初值 null 在接缝声明为 int（默认 0）。</li>
 * </ul>
 */
public final class ExpressionBarView extends UIElement implements Input.ExpressionBar {

    /** JS {@code input.change(event)}：prism-editor @input 的 InputEvent（typing 合并）。 */
    private static final Input.UiEvent TYPING_EVENT = new Input.UiEvent() {
        @Override
        public String nodeName() {
            return "TEXTAREA";
        }

        @Override
        public Input.@Nullable UiFile firstFile() {
            return null;
        }

        @Override
        public @Nullable String selectedOptionId() {
            return null;
        }

        @Override
        public boolean isTypingEvent() {
            return true;
        }
    };

    private final TextField textField;

    /** ExpandedInput.input（JS 初值 0 → null）。 */
    private @Nullable Input input;
    /** ExpandedInput.axis（JS 初值 null；接缝声明 int）。 */
    private int axis;
    /** ExpressionBar.vue data: code。 */
    private String code = "";
    /** ExpressionBar.vue data: language（'molang' | 'generic'）。 */
    private String language = "generic";
    /** ExpandedInput.setup（mounted 后置 true）。 */
    private boolean setup;
    /** 程序化 setText 时抑制 responder（JS prism v-model 无此回环问题）。 */
    private boolean suppressResponder;

    public ExpressionBarView() {
        // ExpressionBar.vue #expression_bar：padding 5px 8px、border-bottom 1px var(--color-border)
        layout(layout -> layout.widthPercent(100)
                .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN)
                .paddingHorizontal(8).paddingTop(5));
        style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.DARK)));

        textField = new TextField();
        textField.layout(layout -> layout.widthPercent(100).flex(1).marginBottom(4));
        // prism-editor 无占位文案（JS data code: ''）；LDLib2 默认 placeholder「Empty」清空，
        // 默认输入框底纹去除（#expression_bar 背景即 DARK，编辑器无自身底框）
        io.github.tt432.eyelib.client.gui.snowstorm.kit.SsTextField.applyTextStyle(textField,
                SnowstormTheme.TEXT);
        textField.style(style -> style.backgroundTexture(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY));
        // ExpressionBar.vue: @input="updateInput($event, true)"
        textField.setTextResponder(this::onBarInput);
        addChild(textField);

        // border-bottom 1px solid var(--color-border)
        UIElement bottomBorder = new UIElement().layout(l -> l.widthPercent(100).height(1));
        bottomBorder.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.BORDER)));
        addChild(bottomBorder);
    }

    // ==================================================================
    // ExpandedInput 接缝（Input.ExpressionBar）
    // ==================================================================

    /** ExpressionBar.vue mounted：setup = true + 注入接缝。 */
    @Override
    protected void onAdded() {
        Input.expressionBar = this;
        setup = true;
    }

    /** 编辑器 Screen 关闭时释放接缝（ADR-0036 I4）。 */
    @Override
    protected void onRemoved() {
        if (Input.expressionBar == this) {
            Input.expressionBar = null;
        }
        setup = false;
    }

    @Override
    public boolean setup() {
        return setup;
    }

    @Override
    public int axis() {
        return axis;
    }

    @Override
    public void axis(int axis) {
        this.axis = axis;
    }

    @Override
    public void input(@Nullable Input input) {
        this.input = input;
    }

    /** ExpressionBar.vue mounted 的 ExpandedInput.updateText(text, language, focusing)。 */
    @Override
    public void updateText(@Nullable Object text, String language, boolean focusing) {
        // JS: if (language && language !== this.language) this.language = language
        if (language != null && !language.isEmpty() && !language.equals(this.language)) {
            this.language = language;
        }
        // focusing 分支（重置 prism 撤销历史）无等价物，见类 doc 偏离
        updateInput(text, false);
    }

    // ==================================================================
    // ExpressionBar.vue updateInput(text, edit)
    // ==================================================================

    private void onBarInput(String text) {
        if (suppressResponder) return;
        updateInput(text, true);
    }

    /** ExpressionBar.vue updateInput(text, edit) 逐字移植。 */
    private void updateInput(@Nullable Object text, boolean edit) {
        // JS: if (!text && typeof text !== 'string' && typeof text !== 'number') text = '';
        if (!JsSemantics.truthy(text) && !(text instanceof String) && !(text instanceof Number)) {
            text = "";
        }
        // JS: if (typeof text !== 'string') text = text.toString();
        String str = text instanceof String s ? s : JsSemantics.toJsString(text);

        this.code = str;
        suppressResponder = true;
        textField.setText(str);
        suppressResponder = false;

        if (this.input == null || !edit) return; // JS: if (!ExpandedInput.input || !edit) return;
        Input in = this.input;
        if (in.axis_count != 1) {
            // JS: len = input.axis_count > 0 ? input.axis_count : (input.value && input.value.length)
            Object value = in.getValue();
            int len = in.axis_count > 0 ? in.axis_count : (value instanceof List<?> list ? list.size() : 0);
            List<Object> arr = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                arr.add(i == this.axis ? str
                        : value instanceof List<?> list && i < list.size() ? list.get(i) : null);
            }
            in.set(arr);
        } else {
            in.setValue(str); // JS: input.value = text（value setter）
        }
        // JS: if (typeof input.change == 'function') input.change(event)
        in.change(TYPING_EVENT);
    }

    // ==================================================================
    // 状态读取（集成/测试用）
    // ==================================================================

    /** ExpressionBar.vue data: code。 */
    public String code() {
        return code;
    }

    /** ExpressionBar.vue data: language。 */
    public String language() {
        return language;
    }
}
//?}

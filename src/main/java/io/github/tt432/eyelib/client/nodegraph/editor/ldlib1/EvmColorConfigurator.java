//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ColorConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.HsbColorWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import io.github.tt432.eyelib.bridge.ui.UiPort;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * LDLib1 {@link ColorConfigurator} 的取色弹窗修补：原实现在非 LDLib Editor 环境下
 * 把弹窗按色板的「父级相对坐标」加进 mainGroup（y ≈ -108 → 屏外，点击色板看起来
 * 无反应），且 {@link HsbColorWidget} 未启用 alpha 滑条（本项目颜色值为 #AARRGGBB）。
 *
 * <p>子类仅替换色板按钮的点击回调：弹窗宿主沿 parent 链爬根组、位置取鼠标处
 * （节点控件坐标是画布坐标系，非屏幕坐标），并打开 alpha 通道编辑。弹窗附加
 * RGBA / HSL 数字输入框（{@link TextFieldWidget} supplier 双向同步：拖取色区时
 * 输入框跟随刷新，输入框编辑时取色区跟随刷新）。其余行为（右键复制/粘贴菜单、
 * 拖拽换色）保持 LDLib 原样。
 */
public final class EvmColorConfigurator extends ColorConfigurator {

    private static final int DIALOG_WIDTH = 152;
    private static final int DIALOG_HEIGHT = 124;

    public EvmColorConfigurator(String name, Supplier<Number> supplier, Consumer<Number> onUpdate,
                                Number defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
    }

    @Override
    public void init(int width) {
        super.init(width);
        for (Widget widget : widgets) {
            if (widget instanceof ButtonWidget button) {
                button.setOnPressCallback(this::openColorDialog);
            }
        }
    }

    /** 按鼠标位置弹取色窗（夹取屏内；节点在画布坐标系下，控件绝对坐标不是屏幕坐标）。 */
    private void openColorDialog(ClickData clickData) {
        // 节点内容区的配置器不经 ModularUI.setGui 挂载（gui == null，LDLib 原实现在
        // 此环境必 NPE/弹窗出屏），沿 parent 链爬到根 WidgetGroup 作为弹窗宿主。
        WidgetGroup root = null;
        for (Widget w = this; w != null; w = w.getParent()) {
            if (w instanceof WidgetGroup group) {
                root = group;
            }
        }
        if (root == null) {
            return;
        }
        int mouseX = UiPort.guiMouseX();
        int mouseY = UiPort.guiMouseY();
        int x = Math.max(0, Math.min(mouseX, UiPort.guiScaledWidth() - DIALOG_WIDTH));
        int y = Math.max(0, Math.min(mouseY, UiPort.guiScaledHeight() - DIALOG_HEIGHT));
        DialogWidget dialog = new DialogWidget(x, y, DIALOG_WIDTH, DIALOG_HEIGHT);
        dialog.setClickClose(true);
        dialog.setBackground(new GuiTextureGroup(
                ColorPattern.BLACK.rectTexture(), ColorPattern.T_WHITE.borderTexture(-1)));
        HsbColorWidget picker = new HsbColorWidget(5, 5, 100, 100)
                .setOnChanged(this::onDialogColorChanged)
                .setColorSupplier(() -> value.intValue())
                .setColor(value.intValue())
                .setShowAlpha(true);
        dialog.addWidget(picker);
        // RGBA 输入（取色区右栏，0-255）
        int[] shifts = {16, 8, 0, 24};
        String[] names = {"r", "g", "b", "a"};
        for (int i = 0; i < 4; i++) {
            int shift = shifts[i];
            dialog.addWidget(new LabelWidget(108, 7 + i * 12, names[i]));
            dialog.addWidget(new NumberFieldWidget(116, 5 + i * 12, 31, 10,
                    () -> String.valueOf((value.intValue() >> shift) & 0xFF),
                    text -> onRgbChannelEdited(shift, text, picker))
                    .setNumbersOnly(0, 255)
                    .setMaxStringLength(3)
                    .setClientSideWidget());
        }
        // HSL 输入（取色区底行：h 0-360，s/l 0-100；alpha 不参与 HSL 换算）
        addHslField(dialog, picker, 5, 13, "h", 0, 360, 0);
        addHslField(dialog, picker, 52, 60, "s", 0, 100, 1);
        addHslField(dialog, picker, 99, 107, "l", 0, 100, 2);
        root.addWidget(dialog);
    }

    /** HSL 输入框：x 为标签位、fieldX 为输入框位；component 0=h 1=s 2=l。 */
    private void addHslField(DialogWidget dialog, HsbColorWidget picker,
                             int labelX, int fieldX, String label, int min, int max, int component) {
        dialog.addWidget(new LabelWidget(labelX, 110, label));
        dialog.addWidget(new NumberFieldWidget(fieldX, 108, component == 0 ? 34 : 38, 10,
                () -> String.valueOf(Math.round(hslOf(value.intValue())[component])),
                text -> onHslEdited(component, text, picker))
                .setNumbersOnly(min, max)
                .setMaxStringLength(3)
                .setClientSideWidget());
    }

    /** RGB 通道输入回写：替换单通道，其余通道（含 alpha）不变。 */
    private void onRgbChannelEdited(int shift, String text, HsbColorWidget picker) {
        Integer channel = parseClamped(text, 0, 255);
        if (channel == null) {
            return;
        }
        int newColor = (value.intValue() & ~(0xFF << shift)) | (channel << shift);
        applyEditedColor(newColor, picker);
    }

    /** HSL 输入回写：RGB → HSL 替换单分量 → 转回 RGB；alpha 原样保留。 */
    private void onHslEdited(int component, String text, HsbColorWidget picker) {
        Integer componentValue = parseClamped(text, 0, component == 0 ? 360 : 100);
        if (componentValue == null) {
            return;
        }
        float[] hsl = hslOf(value.intValue());
        hsl[component] = componentValue;
        int rgb = hslToRgb(hsl[0], hsl[1], hsl[2]);
        applyEditedColor((value.intValue() & 0xFF000000) | rgb, picker);
    }

    /** 手动输入生效：回写值 + 同步取色区显示（输入框侧经 supplier 自动跟随）。 */
    private void applyEditedColor(int newColor, HsbColorWidget picker) {
        onDialogColorChanged(newColor);
        picker.setColor(newColor);
    }

    private static @org.jspecify.annotations.Nullable Integer parseClamped(String text, int min, int max) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(text.trim())));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 取色窗变更回写（同 LDLib 原回调语义：直接改值 + updateValue + 刷新色板）。 */
    private void onDialogColorChanged(int newColor) {
        value = newColor;
        updateValue();
        image.setImage(commonColor(newColor));
    }

    /** 色板材质（同 LDLib ColorConfigurator.getCommonColor，该方法为 private 无法复用）。 */
    private static IGuiTexture commonColor(int color) {
        return new GuiTextureGroup(
                new ColorRectTexture(color).setRadius(5),
                new ColorBorderTexture(ColorPattern.WHITE.color, -1).setRadius(5));
    }

    /**
     * 数字输入框：计算器式输入（追加数字、退格截尾、删空回 0、光标位置不影响结果）。
     * 截尾后经 setCurrentString 更新并手动触发 responder 回写——不触发 responder 时
     * 下一帧 updateScreen 会从 supplier 把旧值刷回来。
     *
     * <p>注：LDLib 1.0.49 的 {@link TextFieldWidget#keyPressed} 其实会把 ESC 以外的键
     * 全部转发给内部 EditBox（字节码 + 1.20.1 实机验证 2026-08-06，退格/方向键在真实
     * 路由下均正常）；早期「只转发 ESC」的反编译结论有误。本子类保留手动处理是为了
     * 计算器式语义，且不依赖内部 EditBox 的焦点同步。
     */
    private static final class NumberFieldWidget extends TextFieldWidget {
        private final Consumer<String> responder;

        NumberFieldWidget(int x, int y, int width, int height,
                          Supplier<String> supplier, Consumer<String> responder) {
            super(x, y, width, height, supplier, responder);
            this.responder = responder;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (isFocus() && (keyCode == 259 || keyCode == 261)) { // BACKSPACE / DELETE
                String current = getCurrentString();
                if (!current.isEmpty()) {
                    // 数字框不允许空值：删空回 "0"（空串被消费方忽略后会被 supplier 刷新
                    // 还原出残留位，实证 bs×3+键入 序列因此错位）
                    String next = current.length() <= 1 ? "0" : current.substring(0, current.length() - 1);
                    setCurrentString(next);
                    responder.accept(next);
                }
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            if (isFocus() && codePoint >= 32 && codePoint != 127) {
                // 计算器式输入（实证必要：setCurrentString→EditBox.setValue 会夹取光标而非
                // 置尾，super.charTyped 按光标插入在退格后序列下顺序错乱）。数字框只收数字，
                // 其余可打印字符吞掉；光标位置对输入结果不再有影响。
                if (Character.isDigit(codePoint)) {
                    String current = getCurrentString();
                    String base = "0".equals(current) ? "" : current;
                    if (base.length() < 3) {
                        String next = base + codePoint;
                        setCurrentString(next);
                        responder.accept(next);
                    }
                }
                return true;
            }
            return super.charTyped(codePoint, modifiers);
        }
    }

    /** ARGB int → HSL（h 0-360，s/l 0-100；忽略 alpha）。 */
    private static float[] hslOf(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2f;
        if (max == min) {
            return new float[]{0f, 0f, l * 100f};
        }
        float d = max - min;
        float s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
        float h;
        if (max == r) {
            h = (g - b) / d + (g < b ? 6f : 0f);
        } else if (max == g) {
            h = (b - r) / d + 2f;
        } else {
            h = (r - g) / d + 4f;
        }
        return new float[]{h * 60f, s * 100f, l * 100f};
    }

    /** HSL（h 0-360，s/l 0-100）→ RGB int（0xRRGGBB，无 alpha）。 */
    private static int hslToRgb(float h, float s, float l) {
        float hn = (h % 360f + 360f) % 360f / 360f;
        float sn = s / 100f;
        float ln = l / 100f;
        float q = ln < 0.5f ? ln * (1f + sn) : ln + sn - ln * sn;
        float p = 2f * ln - q;
        int r = Math.round(hueToRgb(p, q, hn + 1f / 3f) * 255f);
        int g = Math.round(hueToRgb(p, q, hn) * 255f);
        int b = Math.round(hueToRgb(p, q, hn - 1f / 3f) * 255f);
        return (r << 16) | (g << 8) | b;
    }

    private static float hueToRgb(float p, float q, float t) {
        float tn = t;
        if (tn < 0f) tn += 1f;
        if (tn > 1f) tn -= 1f;
        if (tn < 1f / 6f) return p + (q - p) * 6f * tn;
        if (tn < 1f / 2f) return q;
        if (tn < 2f / 3f) return p + (q - p) * (2f / 3f - tn) * 6f;
        return p;
    }
}
//?}

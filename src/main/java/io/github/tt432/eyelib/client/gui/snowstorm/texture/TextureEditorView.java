package io.github.tt432.eyelib.client.gui.snowstorm.texture;
//? if >=1.20.1 {

import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.editor.EditHistory;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import io.github.tt432.eyelib.snowstorm.texture.RasterCanvas;
import io.github.tt432.eyelib.snowstorm.texture.TextureClass;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import io.github.tt432.eyelib.wintersky.molang.Molang;
import io.github.tt432.eyelib.wintersky.tinycolor.TinyColor;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TextureInput.vue 逐行为移植（ADR-0036 D5/U3）：贴图编辑器视图（LDLib2 UIElement）。
 *
 * <p>数据层复用 {@link TextureClass}（brush/eraser/fill/undo/redo/pickColor/createEmpty/save 全部
 * as-is 语义在 domain）；本类移植 Vue 的视口交互：滚轮缩放/右键平移、UV 框拖拽（周界移动+采样+
 * 尺寸手柄）、动画帧步进、信息栏、工具栏 5 工具 + undo/redo + 颜色预览 + 取色、新建对话框。
 *
 * <p>偏离（vs TextureInput.vue）：
 * <ul>
 *   <li>lucide 图标 → ASCII 文字按钮（sel/br/er/fl/pk 等）；行为/布局角色 as-is。</li>
 *   <li>viewport 滚动条（viewport_scrollbar/slideScrollBar）与 resize_line（slideEditorHeight）
 *       不在本切片范围，未移植；viewport_size 固定 256（JS data 初值）。</li>
 *   <li>触摸屏分支（onTouchStart/convertTouchEvent/pointerType touch 平移）无 MC 对应，剔除。</li>
 *   <li>取色器 overlay 用 LDLib2 {@link ColorConfigurator}（ARGB int ↔ hex8 经 TinyColor 往返），
 *       替代 vue-color Chrome 组件；document 级点击关闭 → 视口 MOUSE_DOWN 关闭（近似）。</li>
 *   <li>Ctrl+Z/Y 走视口键盘事件（LDLib2 键盘事件路由到焦点元素；JS 为 document 级 +
 *       .texture_input:hover 判定）：视口 MOUSE_DOWN 时 focus()，Ctrl+Z/Y 在焦点态生效。</li>
 *   <li>image_element.naturalWidth ≡ Texture.canvas 宽高（codec 保证同步，TextureBridge）。</li>
 *   <li>brush outline 的 mix-blend-mode:difference 无对应 → 单层白描边。</li>
 *   <li>新建对话框宽度输入走 {@code TextField.setNumbersOnlyInt}（JS number input 允许 NaN 的
 *       quirk 不复刻）。</li>
 * </ul>
 */
public final class TextureEditorView extends UIElement {

    // ---------------------------------------------------------------- JS data() as-is

    /** JS data 的 Texture（texture_edit.js 单例）。 */
    private final TextureClass Texture = TextureClass.Texture;
    /** JS paint_color.hex8。 */
    private String paint_color = "#ffffffff";
    private float zoom = 1;
    private final float[] offset = {0, 0};
    /** JS viewport_size（resize_line 未移植，固定初值）。 */
    private static final int VIEWPORT_SIZE = 256;
    /** JS tool：'select' | 'brush' | 'eraser' | 'fill_tool' | 'color_picker'。 */
    private String tool = "select";
    private boolean color_picker_open = false;
    private final int[] new_texture_size = {16, 16};
    /** JS cursor_position{x, y, active}（UV 单位）。 */
    private int cursor_x, cursor_y;
    private boolean cursor_active;
    /** JS pixel_position{x, y}（图像像素单位）。 */
    private int pixel_x, pixel_y;

    // ---------------------------------------------------------------- 手势状态（JS document 监听的闭包变量）

    /** JS usePaintTool 的 pointermove/pointerup 监听对。 */
    private TextureClass.@Nullable PaintStroke activeStroke;
    private final TextureClass.PaintContext paintContext = new TextureClass.PaintContext();
    /** JS onMouseDown 平移闭包（initial_offset + 起点鼠标）。 */
    private boolean panning;
    private float panStartMouseX, panStartMouseY;
    private float panStartOffsetX, panStartOffsetY;
    /** JS dragUV 闭包。 */
    private boolean uvDragging;
    private @Nullable Input uvTargetInput;
    private Object @Nullable [] uvInitialValue;
    private int uvInitialX, uvInitialY;
    private int uvLastX, uvLastY;

    /** TextureInput.vue 的 molangjs 实例（UV 值求值）。 */
    private final Molang parser = new Molang();

    // ---------------------------------------------------------------- 子元素

    private final Viewport viewport = new Viewport();
    private final ColorRectTexture colorSwatch = new ColorRectTexture(0xFFFFFFFF);
    private final UIElement colorOverlay;
    private final TextElement dimsText = text("");
    private final TextElement cursorText = text("");
    private final TextElement zoomText = text("");
    private final Button undoButton;
    private final Button redoButton;
    private final Button saveButton;
    private final Button reloadButton;
    private final Button prevFrameButton;
    private final Button nextFrameButton;
    private final List<ToolButton> toolButtons = new ArrayList<>();
    // 条件可见性缓存（避免每帧 setDisplay 触发 layout dirty）
    private boolean lastSaveVisible, lastReloadVisible, lastFrameBarVisible, lastOverlayVisible;

    private TextureEditorView() {
        TextureBridge.install();

        layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));

        // 工具栏（JS .toolbar：5 工具 + undo/redo + 颜色预览）
        UIElement toolbar = new UIElement()
                .layout(l -> l.widthPercent(100).height(20).flexDirection(FlexDirection.ROW)
                        .alignItems(AlignItems.CENTER).gapAll(1));
        addToolButton(toolbar, "select", "sel", "Select");
        addToolButton(toolbar, "brush", "br", "Brush");
        addToolButton(toolbar, "eraser", "er", "Eraser");
        addToolButton(toolbar, "fill_tool", "fl", "Paint Bucket");
        addToolButton(toolbar, "color_picker", "pk", "Color Picker");
        undoButton = smallButton("undo", () -> Texture.undo());
        redoButton = smallButton("redo", () -> Texture.redo());
        toolbar.addChildren(undoButton, redoButton);
        // 颜色预览（JS .color_preview：点击切换取色器 overlay）
        UIElement swatch = new UIElement()
                .layout(l -> l.width(18).height(18).marginAll(1))
                .style(s -> s.backgroundTexture(colorSwatch));
        swatch.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                color_picker_open = !color_picker_open;
                event.stopPropagation();
            }
        });
        toolbar.addChild(swatch);

        // 取色器 overlay（JS #color_picker_overlay，绝对定位右侧）
        colorOverlay = new UIElement()
                .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).right(0).top(22).width(120));
        colorOverlay.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.INTERFACE)));
        colorOverlay.addChild(new ColorConfigurator("",
                this::getPaintColorArgb, this::setPaintColorArgb, 0xFFFFFFFF, true)
                .layout(l -> l.widthPercent(100)));

        // 信息栏（JS .texture_info_bar）
        UIElement infoBar = new UIElement()
                .layout(l -> l.widthPercent(100).height(12).flexDirection(FlexDirection.ROW)
                        .alignItems(AlignItems.CENTER).gapAll(2));
        dimsText.layout(l -> l.flex(1));
        cursorText.layout(l -> l.flex(1));
        zoomText.layout(l -> l.flex(1));
        prevFrameButton = smallButton("<", () -> moveByFrame(-1));
        nextFrameButton = smallButton(">", () -> moveByFrame(1));
        infoBar.addChildren(dimsText, cursorText, zoomText, prevFrameButton, nextFrameButton,
                smallIconButton("maximize", this::maximizeViewport));

        // meta 工具栏（JS .meta.toolbar：reset / reload / new / save）
        UIElement metaBar = new UIElement()
                .layout(l -> l.widthPercent(100).height(16).flexDirection(FlexDirection.ROW).gapAll(1));
        metaBar.addChild(smallIconButton("x", this::onReset));
        reloadButton = smallIconButton("refresh-ccw", this::onReload);
        metaBar.addChild(reloadButton);
        metaBar.addChild(smallIconButton("file-plus-2", this::openNewTextureDialog));
        saveButton = smallIconButton("save", this::onSave);
        metaBar.addChild(saveButton);

        addChildren(toolbar, viewport, infoBar, metaBar, colorOverlay);
        viewport.layout(l -> l.widthPercent(100).height(VIEWPORT_SIZE + 2));

        // 键盘 undo/redo（JS 模块级 keydown：Ctrl+Z/Y；输入框焦点时除外——LDLib2 键盘事件
        // 路由焦点元素，天然满足"输入框焦点不触发"）
        viewport.addEventListener(UIEvents.KEY_DOWN, event -> {
            if (!event.isCtrlDown()) return;
            if (event.keyCode == 90) { // GLFW_KEY_Z
                Texture.undo();
                event.stopPropagation();
            } else if (event.keyCode == 89) { // GLFW_KEY_Y
                Texture.redo();
                event.stopPropagation();
            }
        });

        updateToolSelection();
    }

    /** 装配入口（Sidebar 纹理组宿主调用）。 */
    public static TextureEditorView create() {
        return new TextureEditorView();
    }

    // ---------------------------------------------------------------- 小控件构造

    private static final class ToolButton {
        final String tool;
        final Button button;

        ToolButton(String tool, Button button) {
            this.tool = tool;
            this.button = button;
        }
    }

    /** 工具 id → lucide 图标名（TextureInput.vue 工具栏 as-is）。 */
    private static final java.util.Map<String, String> TOOL_ICONS = java.util.Map.of(
            "select", "mouse-pointer",
            "brush", "paintbrush",
            "eraser", "eraser",
            "fill_tool", "paint-bucket",
            "color_picker", "pipette");

    private void addToolButton(UIElement parent, String toolId, String label, String tooltip) {
        Button button = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.ghost(
                TOOL_ICONS.getOrDefault(toolId, "help-circle"), 10, event -> {
                    // JS selectTool(tool)
                    this.tool = toolId;
                    updateToolSelection();
                });
        button.layout(l -> l.width(18).height(14));
        toolButtons.add(new ToolButton(toolId, button));
        parent.addChild(button);
    }

    private void updateToolSelection() {
        // JS .tool.selected（--color-title 底 + --color-highlight 字）
        for (ToolButton tb : toolButtons) {
            boolean selected = tb.tool.equals(this.tool);
            tb.button.buttonStyle(s -> s
                    .baseTexture(selected
                            ? new ColorRectTexture(SnowstormTheme.TITLE) : IGuiTexture.EMPTY)
                    .hoverTexture(new ColorRectTexture(SnowstormTheme.BAR)));
            tb.button.textStyle(s -> s.textColor(selected ? SnowstormTheme.HIGHLIGHT : SnowstormTheme.TEXT));
        }
    }

    private Button smallButton(String label, Runnable onClick) {
        Button button = new Button();
        button.setOnClick(event -> onClick.run())
                .setText(Component.literal(label))
                .textStyle(s -> s.fontSize(8).textColor(SnowstormTheme.TEXT))
                .buttonStyle(s -> s
                        .baseTexture(new ColorRectTexture(SnowstormTheme.BAR))
                        .hoverTexture(new ColorRectTexture(SnowstormTheme.TITLE)))
                .layout(l -> l.width(Math.max(14, label.length() * 5 + 6)).height(14));
        return button;
    }

    /** 图标小按钮（kit SsIconButton ghost 形态 + 尺寸）。 */
    private Button smallIconButton(String lucideName, Runnable onClick) {
        Button button = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.ghost(
                lucideName, 10, event -> onClick.run());
        button.buttonStyle(s -> s
                .baseTexture(new ColorRectTexture(SnowstormTheme.BAR))
                .hoverTexture(new ColorRectTexture(SnowstormTheme.TITLE)));
        button.layout(l -> l.width(18).height(14));
        return button;
    }

    private static TextElement text(String text) {
        TextElement element = new TextElement();
        element.setText(Component.literal(text));
        element.textStyle(s -> s.fontSize(8).textColor(SnowstormTheme.TEXT_GRAYED));
        element.layout(l -> l.height(10));
        return element;
    }

    private static void setTextCached(TextElement element, String text, String[] cache, int index) {
        if (!text.equals(cache[index])) {
            cache[index] = text;
            element.setText(Component.literal(text));
        }
    }

    // ---------------------------------------------------------------- 颜色（hex8 ↔ ARGB int，TinyColor 往返）

    private int getPaintColorArgb() {
        TinyColor.Rgba rgb = new TinyColor(paint_color).toRgb();
        return ((int) Math.round(rgb.a * 255) << 24)
                | ((int) rgb.r << 16) | ((int) rgb.g << 8) | (int) rgb.b;
    }

    private void setPaintColorArgb(int argb) {
        // JS setPaintColor(color)：color.hex8 = '#rrggbbaa'
        paint_color = String.format("#%02x%02x%02x%02x",
                (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
        colorSwatch.setColor(argb);
    }

    // ---------------------------------------------------------------- meta 按钮（JS 方法 as-is）

    /** JS Texture.reset() 按钮（allow_upload 分支的 X）。 */
    private void onReset() {
        Texture.reset();
    }

    /** JS reloadTexture()：{@code if (!Texture.source) return; Texture.reload();} */
    private void onReload() {
        if (Texture.source.isEmpty()) return;
        Texture.reload();
    }

    /** JS saveTexture()：{@code if (!Texture.source) return; Texture.save();} */
    private void onSave() {
        if (Texture.source.isEmpty()) return;
        Texture.save();
    }

    // ---------------------------------------------------------------- 新建对话框（JS #new_texture_dialog）

    private void openNewTextureDialog() {
        Dialog dialog = new Dialog().setTitle("New Texture").setClickOutsideClose(true);
        TextField widthField = new TextField();
        widthField.setNumbersOnlyInt(1, 4096);
        widthField.setText(String.valueOf(new_texture_size[0]), false);
        widthField.layout(l -> l.width(48).height(14));
        TextField heightField = new TextField();
        heightField.setNumbersOnlyInt(1, 4096);
        heightField.setText(String.valueOf(new_texture_size[1]), false);
        heightField.layout(l -> l.width(48).height(14));
        UIElement form = new UIElement()
                .layout(l -> l.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(2).paddingAll(2));
        form.addChildren(text("W"), widthField, text("H"), heightField);
        dialog.addContent(form);
        // JS newTextureConfirm()：读 new_texture_size → Texture.createEmpty
        Button confirm = smallButton("Confirm", () -> {
            new_texture_size[0] = parseIntOr(widthField.getValue(), 16);
            new_texture_size[1] = parseIntOr(heightField.getValue(), 16);
            Texture.createEmpty(new_texture_size[0], new_texture_size[1]);
            dialog.close();
        });
        Button cancel = smallButton("Cancel", dialog::close);
        dialog.addButton(confirm);
        dialog.addButton(cancel);
        dialog.show(this);
    }

    private static int parseIntOr(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ---------------------------------------------------------------- InputStructure 访问（JS this.data.texture.uv.inputs）

    private static InputStructure.@Nullable Group uvGroup() {
        InputStructure.Subject subject = InputStructure.Data.get("texture");
        return subject != null ? subject.group("uv") : null;
    }

    private static @Nullable Input uvInput(String key) {
        InputStructure.Group group = uvGroup();
        return group != null ? group.inputs.get(key) : null;
    }

    /** JS uv_inputs.size.value（number 双轴 → List<Double>）。 */
    @SuppressWarnings("unchecked")
    private static double[] uvSizeValue() {
        Input size = uvInput("size");
        Object value = size != null ? size.getValue() : null;
        if (value instanceof List<?> list && list.size() >= 2) {
            return new double[]{JsSemantics.toNumber(list.get(0)), JsSemantics.toNumber(list.get(1))};
        }
        return new double[]{16, 16};
    }

    /** JS uv_inputs.mode.value（select → key 字符串）。 */
    private static String uvDefinitionMode() {
        Input mode = uvInput("mode");
        Object value = mode != null ? mode.getValue() : null;
        return value != null ? value.toString() : "static";
    }

    /** JS value.map(v => parser.parse(v[, vars]))：molangjs parse 对 number/string 双态。 */
    private double parseUv(Object v, Map<String, Object> @Nullable [] vars) {
        if (v instanceof Number n) {
            return parser.parse(n.doubleValue());
        }
        String s = v != null ? v.toString() : "";
        return vars != null ? parser.parse(s, vars[0]) : parser.parse(s);
    }

    @SuppressWarnings("unchecked")
    private static Object @Nullable [] inputArrayValue(@Nullable Input input) {
        Object value = input != null ? input.getValue() : null;
        if (value instanceof List<?> list && list.size() >= 2) {
            return new Object[]{list.get(0), list.get(1)};
        }
        return null;
    }

    // ---------------------------------------------------------------- JS computed

    /** JS computed width/size：viewport_size * zoom。 */
    private float frameWidth() {
        return VIEWPORT_SIZE * zoom;
    }

    /** JS computed ratio。 */
    private double ratio() {
        if ("full".equals(uvDefinitionMode())) {
            // JS image_ratio：image_element.naturalWidth 回退 Texture.canvas（本端口两者恒等）
            RasterCanvas canvas = Texture.canvas;
            return canvas.width / (double) canvas.height;
        }
        double[] size = uvSizeValue();
        return size[0] / size[1];
    }

    /** JS computed height：viewport_size * zoom / ratio。 */
    private float frameHeight() {
        return (float) (VIEWPORT_SIZE * zoom / ratio());
    }

    // ---------------------------------------------------------------- JS maximizeViewport / viewportIsCentered

    private boolean viewportIsCentered() {
        return zoom == 1 && offset[0] == 0 && offset[1] == 0;
    }

    private void maximizeViewport() {
        if (viewportIsCentered()) {
            zoom = (float) Math.min(1, ratio());
        } else {
            offset[0] = 0;
            offset[1] = 0;
            zoom = 1;
        }
    }

    // ---------------------------------------------------------------- JS moveByFrame（动画帧步进）

    private void moveByFrame(int steps) {
        Input stepInput = uvInput("uv_step");
        Object[] stepValue = inputArrayValue(stepInput);
        if (stepValue == null) return;
        double[] size = uvSizeValue();
        double step0 = parseUv(stepValue[0], null);
        double step1 = parseUv(stepValue[1], null);
        step0 *= (frameWidth() / size[0]) * steps;
        step1 *= (frameHeight() / size[1]) * steps;
        offset[0] -= (float) step0;
        offset[1] -= (float) step1;
    }

    // ---------------------------------------------------------------- JS offsetUVValue（molang 感知数值偏移，逐字）

    private static final Pattern UV_START = Pattern.compile("^-?\\s*\\d+(\\.\\d+)?\\s*(\\+|-)");
    private static final Pattern UV_END = Pattern.compile("(\\+|-)\\s*\\d*(\\.\\d+)?\\s*$");

    /** JS offsetUVValue(value, amount)：返回值经调用方 toString（数字走 JS number toString）。 */
    /** JS trimFloatNumber 包装：NaN/Infinity 时 JS toFixed 返回 "NaN"/"Infinity"/"-Infinity"
     *  （SnowstormUtil.trimFloatNumber 的 BigDecimal 路径不接受非有限值，util 切片文件不动）。 */
    private static String trimFloatText(double v) {
        if (Double.isNaN(v)) return "NaN";
        if (v == Double.POSITIVE_INFINITY) return "Infinity";
        if (v == Double.NEGATIVE_INFINITY) return "-Infinity";
        return SnowstormUtil.trimFloatNumber(v).toString();
    }

    private static @Nullable Object offsetUVValue(@Nullable Object value, int amount) {
        if (amount == 0) return value;
        if (!JsSemantics.truthy(value) || "0".equals(value)) {
            return Integer.toString(amount);
        }
        // JS: typeof value == 'string' && !isNaN(value) → value = parseFloat(value)
        if (value instanceof String s && isFullyNumeric(s)) {
            value = Double.parseDouble(s.trim());
        }
        // JS: typeof value === 'number' → value + amount
        if (value instanceof Number n) {
            return n.doubleValue() + amount;
        }
        String str = java.util.Objects.requireNonNull(value).toString();
        Matcher start = UV_START.matcher(str);
        if (start.find()) {
            String match = start.group();
            double number = JsonValues.jsParseFloat(match.substring(0, match.length() - 1)) + amount;
            if (number == 0) {
                String rest = str.substring(match.length() + (str.charAt(match.length() - 1) == '+' ? 0 : -1));
                return rest.trim();
            } else {
                return trimFloatText(number)
                        + (match.length() >= 2 && match.charAt(match.length() - 2) == ' ' ? " " : "")
                        + str.substring(match.length() - 1);
            }
        }
        Matcher end = UV_END.matcher(str);
        if (end.find()) {
            double number = JsonValues.jsParseFloat(end.group()) + amount;
            return str.substring(0, end.start())
                    + (JsSemantics.toJsString(number).startsWith("-") ? "" : "+")
                    + trimFloatText(number);
        } else {
            return trimFloatText(amount)
                    + (str.startsWith("-") ? "" : "+") + str;
        }
    }

    /** JS !isNaN(string)（Number() 强转全串，trim 后全数值）。 */
    private static boolean isFullyNumeric(String s) {
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return false; // JS Number('') = 0 但 '' 已被 !value 拦截
        try {
            Double.parseDouble(trimmed);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** JS number→string（dragUV 的 .toString()）。 */
    private static String jsToString(@Nullable Object value) {
        return JsSemantics.toJsString(value);
    }

    // ---------------------------------------------------------------- viewport（视口绘制 + 交互）

    /**
     * JS .texture_viewport + .input_texture_wrapper：棋盘格底 + 画布纹理 + UV overlay +
     * brush outline；滚轮缩放（ctrl）/平移（shift 横向）、右键/中键平移、工具手势。
     */
    private final class Viewport extends UIElement {
        private final SpriteTexture canvasTexture = SpriteTexture.of(TextureBridge.CANVAS_TEXTURE_ID);
        private final String[] infoCache = {"", "", ""};

        Viewport() {
            addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
            addEventListener(UIEvents.MOUSE_MOVE, this::onMouseMove);
            addEventListener(UIEvents.MOUSE_UP, this::onMouseUp);
            addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);
            addEventListener(UIEvents.MOUSE_ENTER, event -> cursor_active = true);
            addEventListener(UIEvents.MOUSE_LEAVE, event -> cursor_active = false);
        }

        // ------------------------------------------------------------ 几何（JS $refs 矩形）

        /** JS wrapper content 左缘（视口局部坐标）：margin auto 居中 + offset 平移。 */
        private float contentLeft() {
            return (getSizeWidth() - frameWidth()) / 2f + offset[0];
        }

        private float contentTop() {
            return (getSizeHeight() - frameHeight()) / 2f + offset[1];
        }

        /** JS onMouseMove 的位置更新（cursor_position UV 单位 + pixel_position 像素单位）。 */
        private void updatePositions(UIEvent event) {
            Vector2f local = getLocalMouse(event.x, event.y);
            double[] uvSize = uvSizeValue();
            float fw = frameWidth();
            float fh = frameHeight();
            // JS: Math.floor((event.clientX-1-rect.left) / frame_width * uv_width)
            // rect 为 border-box（1px 边框），-1 后即 content 相对坐标
            float rx = local.x - contentLeft();
            float ry = local.y - contentTop();
            cursor_x = (int) Math.floor(rx / fw * uvSize[0]);
            cursor_y = (int) Math.floor(ry / fh * uvSize[1]);
            pixel_x = (int) Math.floor(rx / fw * Texture.canvas.width);
            pixel_y = (int) Math.floor(ry / fh * Texture.canvas.height);
        }

        // ------------------------------------------------------------ JS onMouseDown
        private void onMouseDown(UIEvent event) {
            // 取色 overlay 近似「document 点击关闭」
            color_picker_open = false;

            // JS: event.button == 2 || event.button == 1（右键/中键平移；touch 分支剔除）
            if (event.button == 2 || event.button == 1) {
                panning = true;
                panStartMouseX = event.x;
                panStartMouseY = event.y;
                panStartOffsetX = offset[0];
                panStartOffsetY = offset[1];
                event.stopPropagation();
                return;
            }
            if (event.button != 0) return;

            // JS: this.onMouseMove(event) 同步触点位置
            updatePositions(event);
            viewport.focus(); // 键盘 undo/redo 需要焦点（偏离记录）

            if (event.isAltDown() || "color_picker".equals(tool)) {
                // JS: let color = Texture.pickColor(event, context); this.paint_color.hex8 = color
                paint_color = Texture.pickColor(pixel_x, pixel_y);
                colorSwatch.setColor(getPaintColorArgb());
            } else if ("brush".equals(tool) || "eraser".equals(tool)) {
                paintContext.tool = tool;
                paintContext.color = paint_color;
                paintContext.position.x = pixel_x;
                paintContext.position.y = pixel_y;
                activeStroke = Texture.usePaintTool(event.isShiftDown(), paintContext);
            } else if ("fill_tool".equals(tool)) {
                paintContext.tool = tool;
                paintContext.color = paint_color;
                paintContext.position.x = pixel_x;
                paintContext.position.y = pixel_y;
                Texture.useFillTool(paintContext);
            } else if ("select".equals(tool)) {
                startUvDragIfHit(event);
            }
        }

        /** JS dragUV(e1, size)：uv_preview 与尺寸手柄的 pointerdown（button 1/2 与 tool 守卫在其内）。 */
        private void startUvDragIfHit(UIEvent event) {
            if ("full".equals(uvDefinitionMode())) return;
            Vector2f local = getLocalMouse(event.x, event.y);
            float rx = local.x - contentLeft();
            float ry = local.y - contentTop();
            float[] sample = calculateUVSample();
            boolean resize = false;
            if (sample != null) {
                // 尺寸手柄（JS .uv_preview_size_handle：bottom/right -10px、10×10、hover 显示）
                if (isHover() && rx >= sample[0] + sample[2] && rx <= sample[0] + sample[2] + 10
                        && ry >= sample[1] + sample[3] && ry <= sample[1] + sample[3] + 10) {
                    resize = true;
                }
            }
            boolean hit = resize
                    || (sample != null && rx >= sample[0] && rx <= sample[0] + sample[2]
                            && ry >= sample[1] && ry <= sample[1] + sample[3]);
            if (!hit) {
                float[] perimeter = calculateUVPerimeter();
                hit = perimeter != null && rx >= perimeter[0] && rx <= perimeter[0] + perimeter[2]
                        && ry >= perimeter[1] && ry <= perimeter[1] + perimeter[3];
            }
            if (!hit) return;

            // JS: let target_input = this.data.texture.uv.inputs[size ? 'uv_size' : 'uv']
            uvTargetInput = uvInput(resize ? "uv_size" : "uv");
            uvInitialValue = inputArrayValue(uvTargetInput);
            if (uvInitialValue == null) {
                uvTargetInput = null;
                return;
            }
            uvDragging = true;
            uvInitialX = uvLastX = cursor_x;
            uvInitialY = uvLastY = cursor_y;
            event.stopPropagation();
        }

        // ------------------------------------------------------------ JS onMouseMove / 手势推进

        private void onMouseMove(UIEvent event) {
            updatePositions(event);
            if (panning) {
                // JS 平移 onMove：offset = initial + (e2.client - event.client)
                offset[0] = panStartOffsetX + (event.x - panStartMouseX);
                offset[1] = panStartOffsetY + (event.y - panStartMouseY);
                return;
            }
            if (uvDragging) {
                // JS dragUV onMove：位置未变早退；offsetUVValue 后 target_input.set([..])
                if (uvLastX == cursor_x && uvLastY == cursor_y) return;
                int dx = cursor_x - uvInitialX;
                int dy = cursor_y - uvInitialY;
                Input target = uvTargetInput;
                Object[] initial = uvInitialValue;
                if (target != null && initial != null) {
                    List<Object> values = new ArrayList<>(2);
                    values.add(jsToString(offsetUVValue(initial[0], dx)));
                    values.add(jsToString(offsetUVValue(initial[1], dy)));
                    target.set(values);
                }
                uvLastX = cursor_x;
                uvLastY = cursor_y;
                return;
            }
            if (activeStroke != null) {
                activeStroke.onMove(pixel_x, pixel_y);
            }
        }

        private void onMouseUp(UIEvent event) {
            if (panning && (event.button == 2 || event.button == 1)) {
                panning = false;
            }
            if (uvDragging && event.button == 0) {
                uvDragging = false;
                uvTargetInput = null;
                uvInitialValue = null;
                // JS dragUV onEnd：registerEdit('edit uv')
                EditHistory.registerEdit("edit uv");
            }
            if (activeStroke != null && event.button == 0) {
                activeStroke.onEnd();
                activeStroke = null;
            }
        }

        // ------------------------------------------------------------ JS onMouseWheel

        private void onMouseWheel(UIEvent event) {
            updatePositions(event);
            if (event.isCtrlDown()) {
                // JS: ctrl/meta → deltaY > 1 ? zoom/1.1 : zoom*1.1
                float newZoom = event.deltaY > 1 ? zoom / 1.1f : zoom * 1.1f;
                setZoom(newZoom, event);
            } else if (event.isShiftDown()) {
                offset[0] -= Math.signum(event.deltaY) * 50;
            } else {
                offset[1] -= Math.signum(event.deltaY) * 50;
            }
            event.stopPropagation();
        }

        /** JS setZoom(zoom, event)：1 吸附 + clamp + 鼠标锚点补偿。 */
        private void setZoom(float newZoom, UIEvent event) {
            float initial_zoom = zoom;
            if (newZoom > 1 / 1.1 && newZoom < 1.1) newZoom = 1;
            newZoom = (float) SnowstormUtil.clamp(newZoom, Math.min(0.5, ratio()), 8);
            if (newZoom == zoom) return;

            // JS 读 DOM clientWidth（响应式未刷新 → 缩放前宽度）
            float oldWidth = frameWidth();
            float oldHeight = frameHeight();
            zoom = newZoom;

            Vector2f local = getLocalMouse(event.x, event.y);
            // JS mouse_pos = (clientX-1-rect.left, clientY-1-rect.top)（content 相对，用旧几何）
            float oldContentLeft = (getSizeWidth() - oldWidth) / 2f + offset[0];
            float oldContentTop = (getSizeHeight() - oldHeight) / 2f + offset[1];
            float mouseX = local.x - oldContentLeft;
            float mouseY = local.y - oldContentTop;
            float zoom_offset = 1 - zoom / initial_zoom;
            boolean is_wider_than_viewport = oldWidth > getSizeWidth();
            offset[0] += mouseX * zoom_offset * (is_wider_than_viewport ? 1 : 0);
            offset[1] += mouseY * zoom_offset;
        }

        // ------------------------------------------------------------ UV overlay 计算（JS calculateUV*）

        /** JS calculateUVSample()：{left, top, width, height}（wrapper content 坐标）。 */
        private float @Nullable [] calculateUVSample() {
            Input uv = uvInput("uv");
            Input uvSize = uvInput("uv_size");
            Object[] uvValue = inputArrayValue(uv);
            Object[] sizeValue = inputArrayValue(uvSize);
            if (uvValue == null || sizeValue == null) return null;
            double[] texSize = uvSizeValue();
            double off0 = parseUv(uvValue[0], null);
            double off1 = parseUv(uvValue[1], null);
            double sz0 = parseUv(sizeValue[0], null);
            double sz1 = parseUv(sizeValue[1], null);
            return new float[]{
                    (float) (off0 / texSize[0] * frameWidth()),
                    (float) (off1 / texSize[1] * frameHeight()),
                    (float) (sz0 / texSize[0] * frameWidth()),
                    (float) (sz1 / texSize[1] * frameHeight())};
        }

        /** JS calculateUVPerimeter()：5 个 random 采样的包围盒。 */
        @SuppressWarnings("unchecked")
        private float @Nullable [] calculateUVPerimeter() {
            Input uv = uvInput("uv");
            Input uvSize = uvInput("uv_size");
            Object[] uvValue = inputArrayValue(uv);
            Object[] sizeValue = inputArrayValue(uvSize);
            if (uvValue == null || sizeValue == null) return null;
            double[] texSize = uvSizeValue();
            double[] box = {texSize[0], texSize[1], 0, 0};
            // JS: for (let random = 0; random < 1; random += 0.249999999)
            for (double random = 0; random < 1; random += 0.249999999) {
                Map<String, Object> vars = new java.util.LinkedHashMap<>();
                for (int i = 1; i <= 4; i++) {
                    vars.put("variable.particle_random_" + i, random);
                    vars.put("variable.emitter_random_" + i, random);
                }
                Map<String, Object>[] varsArray = new Map[]{vars};
                double off0 = parseUv(uvValue[0], varsArray);
                double off1 = parseUv(uvValue[1], varsArray);
                double sz0 = parseUv(sizeValue[0], varsArray);
                double sz1 = parseUv(sizeValue[1], varsArray);
                box[0] = Math.min(box[0], Math.min(off0, off0 + sz0));
                box[1] = Math.min(box[1], Math.min(off1, off1 + sz1));
                box[2] = Math.max(box[2], Math.max(off0, off0 + sz0));
                box[3] = Math.max(box[3], Math.max(off1, off1 + sz1));
            }
            return new float[]{
                    (float) (box[0] / texSize[0] * frameWidth()),
                    (float) (box[1] / texSize[1] * frameHeight()),
                    (float) ((box[2] - box[0]) / texSize[0] * frameWidth()),
                    (float) ((box[3] - box[1]) / texSize[1] * frameHeight())};
        }

        // ------------------------------------------------------------ 条件可见性（JS v-if / :style display）

        private void updateVisibility() {
            // JS: save 按钮 v-if="Texture.internal_changes"
            boolean saveVisible = Texture.internal_changes;
            if (saveVisible != lastSaveVisible) {
                lastSaveVisible = saveVisible;
                saveButton.setDisplay(saveVisible ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
            }
            // JS: reload v-if="!input.allow_upload"（image 输入 allow_upload = !vscode → false 常态）
            Input image = uvImageInput();
            boolean allowUpload = image == null || image.allow_upload == null || image.allow_upload;
            boolean reloadVisible = !allowUpload;
            if (reloadVisible != lastReloadVisible) {
                lastReloadVisible = reloadVisible;
                reloadButton.setDisplay(reloadVisible ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
            }
            // JS: 帧步进 v-if="UVDefinitionMode() == 'animated'"
            boolean frameBar = "animated".equals(uvDefinitionMode());
            if (frameBar != lastFrameBarVisible) {
                lastFrameBarVisible = frameBar;
                prevFrameButton.setDisplay(frameBar ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
                nextFrameButton.setDisplay(frameBar ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
            }
            if (color_picker_open != lastOverlayVisible) {
                lastOverlayVisible = color_picker_open;
                colorOverlay.setDisplay(color_picker_open ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
            }
            // 信息栏（JS .texture_info_bar：尺寸 / 光标 / 缩放百分比）
            setTextCached(dimsText, Texture.canvas.width + " x " + Texture.canvas.height + " px", infoCache, 0);
            setTextCached(cursorText, cursor_active ? cursor_x + " x " + cursor_y : "", infoCache, 1);
            setTextCached(zoomText, Math.round(zoom * 100) + "%", infoCache, 2);
        }

        /** JS this.data.texture.texture.inputs.image（image 输入）。 */
        private static @Nullable Input uvImageInput() {
            InputStructure.Subject subject = InputStructure.Data.get("texture");
            InputStructure.Group group = subject != null ? subject.group("texture") : null;
            return group != null ? group.inputs.get("image") : null;
        }

        // ------------------------------------------------------------ 绘制（两版本守卫同 BadgeOverlay 先例）

        /** 共享填色纹理（drawTexture 立即绘制，帧内串行复用安全；26.1 无 vanilla GuiGraphics，
         *  实心矩形一律走 drawTexture(ColorRectTexture) 版本稳定 API，同 BadgeOverlay 先例）。 */
        private final ColorRectTexture fillTexture = new ColorRectTexture(0xFFFFFFFF);
        private final ColorRectTexture checkerLight = new ColorRectTexture(0xFF6B6B6B);
        private final ColorRectTexture checkerDark = new ColorRectTexture(0xFF454545);

        private void fillRect(com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext guiContext,
                              float x0, float y0, float x1, float y1, int color) {
            fillTexture.setColor(color);
            guiContext.drawTexture(fillTexture, x0, y0, x1 - x0, y1 - y0);
        }

        private void drawViewport(com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext guiContext) {
            updateVisibility();
            float vx = getPositionX();
            float vy = getPositionY();
            float vw = getSizeWidth();
            float vh = getSizeHeight();

            // JS .texture_viewport background-color: var(--color-dark)
            fillRect(guiContext, vx, vy, vx + vw, vy + vh, SnowstormTheme.DARK);

            float fw = frameWidth();
            float fh = frameHeight();
            if (fw <= 0 || fh <= 0 || Float.isNaN(fw) || Float.isNaN(fh)) return;
            float cx = vx + contentLeft();
            float cy = vy + contentTop();

            guiContext.enableScissor(vx, vy, vx + vw, vy + vh);
            // JS wrapper border: 1px solid var(--color-border)（border-box 外扩 1px）
            fillRect(guiContext, cx - 1, cy - 1, cx + fw + 1, cy + fh + 1, SnowstormTheme.BORDER);

            // 棋盘格（JS .checkerboard，8px 双灰，仅 content 区）
            drawCheckerboard(guiContext, cx, cy, fw, fh);
            // 画布纹理（nearest；DynamicTexture 由 TextureBridge 维护）
            guiContext.drawTexture(canvasTexture, cx, cy, fw, fh);

            // UV overlay（JS v-if="UVDefinitionMode() != 'full'"）
            if (!"full".equals(uvDefinitionMode())) {
                float[] perimeter = calculateUVPerimeter();
                if (perimeter != null) {
                    drawOutline(guiContext, cx + perimeter[0], cy + perimeter[1],
                            perimeter[2], perimeter[3], SnowstormTheme.TEXT);
                }
                float[] sample = calculateUVSample();
                if (sample != null) {
                    drawOutline(guiContext, cx + sample[0], cy + sample[1],
                            sample[2], sample[3], SnowstormTheme.ACCENT);
                    // 尺寸手柄（JS hover 显示）
                    if (isHover()) {
                        float hx = cx + sample[0] + sample[2];
                        float hy = cy + sample[1] + sample[3];
                        fillRect(guiContext, hx, hy, hx + 10, hy + 10, SnowstormTheme.TEXT);
                        drawOutline(guiContext, hx, hy, 10, 10, SnowstormTheme.BORDER);
                    }
                }
            }

            // brush outline（JS：cursor 激活且 brush/eraser/color_picker；difference 混合→单层白描边）
            if (cursor_active && ("brush".equals(tool) || "eraser".equals(tool) || "color_picker".equals(tool))) {
                float imgW = Texture.canvas.width;
                float imgH = Texture.canvas.height;
                float bx = cx + pixel_x * (fw / imgW);
                float by = cy + pixel_y * (fh / imgH);
                drawOutline(guiContext, bx, by, fw / imgW, fh / imgH, 0xFFFFFFFF);
            }
            guiContext.disableScissor();
        }

        private void drawCheckerboard(com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext guiContext,
                                      float x, float y, float w, float h) {
            int cell = 8;
            int x0 = (int) x, y0 = (int) y;
            int x1 = (int) Math.ceil(x + w), y1 = (int) Math.ceil(y + h);
            for (int yy = y0; yy < y1; yy += cell) {
                for (int xx = x0; xx < x1; xx += cell) {
                    boolean odd = ((xx - x0) / cell + (yy - y0) / cell) % 2 == 0;
                    guiContext.drawTexture(odd ? checkerLight : checkerDark,
                            xx, yy, Math.min(xx + cell, x1) - xx, Math.min(yy + cell, y1) - yy);
                }
            }
        }

        private void drawOutline(com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext guiContext,
                                 float x, float y, float w, float h, int color) {
            int x0 = Math.round(x), y0 = Math.round(y);
            int x1 = Math.round(x + w), y1 = Math.round(y + h);
            fillRect(guiContext, x0, y0, x1, y0 + 1, color);
            fillRect(guiContext, x0, y1 - 1, x1, y1, color);
            fillRect(guiContext, x0, y0, x0 + 1, y1, color);
            fillRect(guiContext, x1 - 1, y0, x1, y1, color);
        }

        //? if modern {
        @Override
        protected void drawBackgroundAdditional(com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext context) {
            if (context instanceof com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext gc) {
                drawViewport(gc);
            }
        }
        //?} else {
        @Override
        public void drawBackgroundAdditional(com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext guiContext) {
            drawViewport(guiContext);
        }
        //?}
    }
}
//?}

package io.github.tt432.eyelib.client.gui.snowstorm.gradient;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.gradient.Gradient;
import io.github.tt432.eyelib.snowstorm.gradient.GradientStop;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Snowstorm Gradient.vue 渐变编辑器（P5-B，LDLib2 适配层）：
 * 渐变条（stops 按 percent 分布、#RRGGBBAA 颜色、alpha 棋盘格底）+ 拖拽手柄 +
 * add/remove 工具钮 + 取色器（vue-color Chrome → LDLib2 {@link ColorConfigurator}）。
 *
 * <p>交互 as-is（Gradient.vue dragPoint）：
 * <ul>
 *   <li>手柄 mousedown 即选中；位移 &gt;4px 解锁拖拽；
 *       {@code percent = clamp(round(original + distance/width*100), 0, 100)}，
 *       拖动中持续 {@code sortValues()}；mouseup 时 {@code input.registerEdit()}。</li>
 *   <li>＋ 钮 {@code addPoint()}；⨉ 钮 {@code removePoint()}（仅 selected 有颜色时显示）。</li>
 *   <li>取色 oninput → {@code input.change(e, node)}；node 的 :active 检测（滑动合并）
 *       由「取色弹层打开期间视为 sliding」近似（见偏离）。</li>
 * </ul>
 *
 * <p>偏离记录：
 * <ul>
 *   <li>vue-color Chrome 内联取色面板 → LDLib2 ColorConfigurator（色块点击弹 ColorSelector
 *       对话框），LDLib2 无内联 Chrome 等价物。</li>
 *   <li>is_sliding（{@code node.parentNode.querySelector(':active')}）→ 弹层 show/hide 期间
 *       恒 true：弹层内连续调色走 600ms 合并，最后一次变更 600ms 后派发；弹层关闭本身不触发
 *       变更事件，与 JS 最后一次 oninput 后派发的行为一致，但 JS 单击取色（非按压）会立即
 *       派发，本实现同样合并——记录差异。</li>
 *   <li>点击渐变条空白不 addPoint——Gradient.vue 本就无此交互（添加走 ＋ 工具钮），
 *       按 as-is 实现（任务书所述「点击空白处 addPoint」与源不符）。</li>
 *   <li>CSS 渐变按浏览器语义做预乘 alpha 插值；棋盘格为 16px tile / 8px 双色格
 *       （common.css .checkerboard，INTERFACE/BAR 双色）。</li>
 *   <li>手柄 :title 提示（percent%）与选中 box-shadow 未实现（LDLib2 无等价轻量 API）。</li>
 * </ul>
 */
public final class GradientEditorView extends UIElement {

    /** .gradient_container：height 20 + 手柄下探 6px（JS top:10 height:16 溢出区域收入容器）。 */
    private static final int BAR_HEIGHT = 20;
    private static final int CONTAINER_HEIGHT = 26;
    private static final int HANDLE_WIDTH = 10;
    private static final int HANDLE_HEIGHT = 16;
    private static final int HANDLE_TOP = 10;
    /** 拖拽解锁阈值（JS Math.abs(distance) > 4）。 */
    private static final float DRAG_UNLOCK_PX = 4;

    private static final int CHECKER_A = SnowstormTheme.INTERFACE;
    private static final int CHECKER_B = SnowstormTheme.BAR;
    private static final int BAR_BORDER = 0xFF000000;
    private static final int HANDLE_BORDER = 0xFF212529;
    private static final int HANDLE_BORDER_SELECTED = 0xFFFFFFFF;

    private final Gradient input;

    private final BarElement barContainer;
    private final UIElement toolRow;
    /** stop → 手柄元素（对象身份键，as-is :key=point.id 但 addPoint 无 id）。 */
    private final Map<GradientStop, UIElement> handles = new IdentityHashMap<>();
    /** stop → 手柄填充色纹理（取色时原地改色，避免重建取色器丢焦点）。 */
    private final Map<GradientStop, ColorRectTexture> handleFills = new IdentityHashMap<>();
    private @Nullable GradientColorConfigurator colorConfigurator;
    private boolean pickerOpen;

    // 拖拽态（JS dragPoint 闭包变量）
    private @Nullable GradientStop dragStop;
    private float dragStartX;
    private double dragOriginalPercent;
    private boolean dragUnlocked;
    private @Nullable UIEventListener dragMoveListener;
    /** stop → 手柄边框纹理（选中态切换白/暗边）。 */
    private final Map<GradientStop, ColorRectTexture> handleBorders = new IdentityHashMap<>();
    private @Nullable UIEventListener dragUpListener;

    public GradientEditorView(Gradient input) {
        this.input = input;
        layout(layout -> layout.flexDirection(FlexDirection.COLUMN).widthPercent(100));

        // .gradient_container：margin 0 5px 10px 5px
        barContainer = new BarElement();
        barContainer.layout(layout -> layout
                .widthPercent(100)
                .height(CONTAINER_HEIGHT)
                .marginHorizontal(5)
                .marginBottom(10));
        barContainer.addEventListener(UIEvents.LAYOUT_CHANGED, event -> repositionHandles());

        toolRow = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .height(14)
                .flexDirection(FlexDirection.ROW)
                .marginHorizontal(5));

        addChildren(barContainer, toolRow);
        rebuild();
    }

    /** 结构变化（add/remove/reset/导入回填）后整体刷新。 */
    public void rebuild() {
        rebuildHandles();
        rebuildTools();
        rebuildPicker();
    }

    // ---------------------------------------------------------------- 手柄

    private void rebuildHandles() {
        barContainer.clearAllChildren();
        handles.clear();
        handleFills.clear();
        handleBorders.clear();
        for (GradientStop stop : input.value()) {
            UIElement handle = new UIElement();
            handle.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .top(HANDLE_TOP)
                    .width(HANDLE_WIDTH)
                    .height(HANDLE_HEIGHT));
            ColorRectTexture border = new ColorRectTexture(HANDLE_BORDER);
            ColorRectTexture fill = new ColorRectTexture(hex8ToArgb(stop.color()));
            handle.style(style -> style.backgroundTexture(border));
            UIElement inner = new UIElement().layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(1).top(1).right(1).bottom(1));
            inner.style(style -> style.backgroundTexture(fill));
            handle.addChild(inner);
            handle.addEventListener(UIEvents.MOUSE_DOWN, event -> onHandleMouseDown(stop, event));
            handles.put(stop, handle);
            handleFills.put(stop, fill);
            handleBorders.put(stop, border);
            barContainer.addChild(handle);
        }
        repositionHandles();
    }

    /** 手柄 left = percent% 处居中（JS style left: percent + '%'，margin-left:-5px）。 */
    private void repositionHandles() {
        float width = barContainer.getSizeWidth();
        for (Map.Entry<GradientStop, UIElement> e : handles.entrySet()) {
            float left = (float) (e.getKey().percent() / 100.0) * width - HANDLE_WIDTH / 2f;
            e.getValue().layout(layout -> layout.left(left));
            ColorRectTexture border = handleBorders.get(e.getKey());
            if (border != null) {
                border.color = e.getKey() == input.selected ? HANDLE_BORDER_SELECTED : HANDLE_BORDER;
            }
        }
    }

    private void onHandleMouseDown(GradientStop stop, UIEvent event) {
        if (event.button != 0) return;
        // JS dragPoint：input.selected = point（mousedown 即选中）
        input.selected = stop;
        startDrag(stop, event.x);
        rebuild();
    }

    // ---------------------------------------------------------------- 拖拽（JS dragPoint onDrag/onDragEnd）

    private void startDrag(GradientStop stop, float startX) {
        dragStop = stop;
        dragStartX = startX;
        dragOriginalPercent = stop.percent();
        dragUnlocked = false;

        // JS：document.addEventListener('mousemove'/'mouseup')——挂根元素 capture，
        // 保证拖出渐变条区域仍收到事件（as-is document 级监听）
        UIElement root = rootElement();
        dragMoveListener = this::onDragMove;
        dragUpListener = this::onDragEnd;
        root.addEventListener(UIEvents.MOUSE_MOVE, dragMoveListener, true);
        root.addEventListener(UIEvents.MOUSE_UP, dragUpListener, true);
    }

    private void onDragMove(UIEvent event) {
        GradientStop stop = dragStop;
        if (stop == null) return;
        if (!isMouseDown(0)) { // 鼠标在 UI 外松开等异常路径
            endDrag();
            return;
        }
        float distance = event.x - dragStartX;
        if (Math.abs(distance) > DRAG_UNLOCK_PX) dragUnlocked = true;
        if (dragUnlocked) {
            float width = barContainer.getSizeWidth();
            if (width <= 0) return;
            double percent = dragOriginalPercent + (distance / width) * 100;
            // JS：Math.clamp(Math.round(percent), 0, 100)
            stop.percent(SnowstormUtil.clamp(JsSemantics.jsRound(percent), 0, 100));
            input.sortValues();
            repositionHandles();
        }
    }

    private void onDragEnd(UIEvent event) {
        endDrag();
    }

    private void endDrag() {
        UIElement root = rootElement();
        if (dragMoveListener != null) root.removeEventListener(UIEvents.MOUSE_MOVE, dragMoveListener, true);
        if (dragUpListener != null) root.removeEventListener(UIEvents.MOUSE_UP, dragUpListener, true);
        dragMoveListener = null;
        dragUpListener = null;
        if (dragStop != null) {
            // JS onDragEnd：input.registerEdit()（无解锁也派发，as-is）
            input.registerEdit();
            dragStop = null;
        }
        rebuild();
    }

    private UIElement rootElement() {
        UIElement element = this;
        while (element.getParent() != null) {
            element = element.getParent();
        }
        return element;
    }

    // ---------------------------------------------------------------- 工具钮（＋ / ⨉）

    private void rebuildTools() {
        toolRow.clearAllChildren();
        UIElement spacer = new UIElement().layout(layout -> layout.flex(1).heightPercent(100));
        toolRow.addChild(spacer);
        // JS float:right：＋ 恒在；⨉ 仅 selected 有颜色时
        toolRow.addChild(toolButton("plus", event -> {
            input.addPoint();
            rebuild();
        }));
        if (input.selected != null && JsSemantics.truthy(input.selected.color())) {
            toolRow.addChild(toolButton("x", event -> {
                input.removePoint();
                rebuild();
            }));
        }
    }

    private static Button toolButton(String icon, UIEventListener onClick) {
        // lucide 图标钮（文字占位已退役，kit SsIconButton）
        Button button = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.ghost(
                icon, 10, onClick);
        button.layout(layout -> layout.width(14).heightPercent(100));
        return button;
    }

    // ---------------------------------------------------------------- 取色器（vue-color Chrome → ColorConfigurator）

    private void rebuildPicker() {
        boolean wantPicker = input.selected != null && JsSemantics.truthy(input.selected.color());
        if (wantPicker && colorConfigurator == null) {
            colorConfigurator = new GradientColorConfigurator();
            colorConfigurator.layout(layout -> layout.widthPercent(100));
            addChild(colorConfigurator);
        } else if (!wantPicker && colorConfigurator != null) {
            removeChild(colorConfigurator);
            colorConfigurator = null;
            pickerOpen = false;
        }
    }

    /** 取色弹层 show/hide 期间置 pickerOpen（SliderNode 接缝的滑动语义，见类注释偏离）。 */
    private final class GradientColorConfigurator extends ColorConfigurator {
        GradientColorConfigurator() {
            super("",
                    () -> input.selected != null ? hex8ToArgb(input.selected.color()) : 0xFFFFFFFF,
                    argb -> onColorInput(argb),
                    0xFFFFFFFF,
                    false);
        }

        @Override
        public void show() {
            super.show();
            pickerOpen = true;
        }

        @Override
        public void hide() {
            super.hide();
            pickerOpen = false;
        }
    }

    /** color-picker @input → input.change($event, $el)（as-is 经数据层接缝）。 */
    private void onColorInput(int argb) {
        GradientStop selected = input.selected;
        if (selected == null) return;
        String hex8 = argbToHex8(argb);
        input.change(() -> hex8, () -> pickerOpen);
        // 原地更新手柄填充色（不 rebuild——重建会销毁取色器丢焦点）
        ColorRectTexture fill = handleFills.get(selected);
        if (fill != null) fill.color = argb;
    }

    // ---------------------------------------------------------------- #RRGGBBAA ↔ ARGB int

    /** Snowstorm hex8（#RRGGBBAA）→ LDLib2 ARGB int；非法输入回退不透明白。 */
    static int hex8ToArgb(String hex8) {
        if (hex8 == null || hex8.length() != 9 || hex8.charAt(0) != '#') return 0xFFFFFFFF;
        try {
            long rgba = Long.parseLong(hex8.substring(1), 16);
            int rgb = (int) (rgba >>> 8);
            int a = (int) (rgba & 0xFF);
            return (a << 24) | rgb;
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }

    /** ARGB int → Snowstorm hex8（#RRGGBBAA，小写，as-is vue-color 输出形态）。 */
    static String argbToHex8(int argb) {
        return String.format(java.util.Locale.ROOT, "#%06x%02x", argb & 0xFFFFFF, (argb >>> 24) & 0xFF);
    }

    // ---------------------------------------------------------------- 渐变条绘制

    /**
     * .gradient_container 本体：棋盘格底（16px tile / 8px 双色）+ 预乘 alpha 插值渐变 +
     * 1px 黑边。手柄为子元素（绘制在其后，自然在上层）。
     */
    private final class BarElement extends UIElement {
        /** 逐帧复用，避免分配。 */
        private final ColorRectTexture cell = new ColorRectTexture();

        //? if modern {
        @Override
        protected void drawBackgroundAdditional(com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext context) {
            drawBar(context::drawTexture);
        }
        //?} else {
        @Override
        public void drawBackgroundAdditional(GUIContext guiContext) {
            drawBar(guiContext::drawTexture);
        }
        //?}

        private void drawBar(TextureDrawer drawer) {
            float x = getPositionX(), y = getPositionY(), w = getSizeWidth();
            if (w <= 0) return;

            // 棋盘格（8px 格，双色交错）
            for (int row = 0; row < BAR_HEIGHT / 8 + 1; row++) {
                for (int col = 0; col * 8 < w; col++) {
                    cell.color = ((row + col) & 1) == 0 ? CHECKER_A : CHECKER_B;
                    float cw = Math.min(8, w - col * 8);
                    float ch = Math.min(8, BAR_HEIGHT - row * 8);
                    if (ch > 0) drawer.draw(cell, x + col * 8, y + row * 8, cw, ch);
                }
            }

            // 渐变列（预乘 alpha 插值，浏览器 CSS linear-gradient 语义）
            List<GradientStop> stops = input.value();
            if (!stops.isEmpty()) {
                int columns = (int) w;
                for (int px = 0; px < columns; px++) {
                    double percent = (px + 0.5) / w * 100;
                    cell.color = sampleGradient(stops, percent);
                    drawer.draw(cell, x + px, y, 1, BAR_HEIGHT);
                }
            }

            // 1px 黑边（.gradient_container border: 1px solid black）
            cell.color = BAR_BORDER;
            drawer.draw(cell, x, y, w, 1);
            drawer.draw(cell, x, y + BAR_HEIGHT - 1, w, 1);
            drawer.draw(cell, x, y, 1, BAR_HEIGHT);
            drawer.draw(cell, x + w - 1, y, 1, BAR_HEIGHT);
        }
    }

    @FunctionalInterface
    private interface TextureDrawer {
        void draw(IGuiTexture texture, float x, float y, float width, float height);
    }

    /** CSS linear-gradient 采样：stops 间预乘 alpha 线性插值，两端外推取端点色。 */
    private static int sampleGradient(List<GradientStop> stops, double percent) {
        GradientStop first = stops.get(0);
        if (percent <= first.percent() || stops.size() == 1) return hex8ToArgb(first.color());
        GradientStop last = stops.get(stops.size() - 1);
        if (percent >= last.percent()) return hex8ToArgb(last.color());
        for (int i = 0; i + 1 < stops.size(); i++) {
            GradientStop a = stops.get(i);
            GradientStop b = stops.get(i + 1);
            if (percent >= a.percent() && percent <= b.percent()) {
                double span = b.percent() - a.percent();
                double t = span <= 0 ? 0 : (percent - a.percent()) / span;
                return lerpArgbPremultiplied(hex8ToArgb(a.color()), hex8ToArgb(b.color()), t);
            }
        }
        return hex8ToArgb(last.color());
    }

    /** 预乘 alpha 空间插值（CSS Images 4 浏览器渐变语义）。 */
    private static int lerpArgbPremultiplied(int c0, int c1, double t) {
        double a0 = (c0 >>> 24) / 255.0, a1 = (c1 >>> 24) / 255.0;
        double a = a0 + (a1 - a0) * t;
        int r0 = (c0 >>> 16) & 0xFF, g0 = (c0 >>> 8) & 0xFF, b0 = c0 & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        if (a <= 0) return 0;
        double r = (r0 * a0 + (r1 * a1 - r0 * a0) * t) / a;
        double g = (g0 * a0 + (g1 * a1 - g0 * a0) * t) / a;
        double b = (b0 * a0 + (b1 * a1 - b0 * a0) * t) / a;
        return ((int) Math.round(a * 255) << 24)
                | (Math.min(255, (int) Math.round(r)) << 16)
                | (Math.min(255, (int) Math.round(g)) << 8)
                | Math.min(255, (int) Math.round(b));
    }
}
//?}

//? if >=1.20.1 {
package io.github.tt432.eyelib.client.gui.snowstorm.curve;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
//? if <26.1 {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
//?} else {
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
//?}
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.snowstorm.curve.Curve;
import io.github.tt432.eyelib.snowstorm.editor.EditHistory;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Snowstorm Curve.vue 的 LDLib2 as-is 复刻（单个曲线编辑器）：
 * 曲线显示区（SVG 等价绘制）+ 点选项栏 + 页脚（高度拖拽 + Remove Curve）。
 * 上方的 InputGroup（id/mode/input/range 四个输入）属 P3 输入视图切片，不在本类。
 *
 * <p>渲染为每帧重算（对应 Vue 响应式 + updateSVG 置标志，R3 先例）：
 * linear 折线 / bezier 单段三次曲线 / catmull_rom 转 bezier（tension 0.5，即 tens=6）/
 * bezier_chain Hermite 分段，几何公式逐字取自 Curve.vue updateSVG；
 * 水平参考线 0/±1（dashed）、垂直虚线、min/max 文本、节点点与 slope 手柄。
 *
 * <p>与 JS 的偏离（另见各方法注释）：
 * <ul>
 *   <li>绘制基元为 2px 采样方块（LDLib2 无矢量线）；圆点以方块近似；dash 按 dasharray 6/8。</li>
 *   <li>JS 节点命中盒为 li 元素盒（非 chain 全高列 / chain 20×20），此处非 chain 列宽按
 *       同一 flex 几何复算（首尾 add 帽 20px），chain 节点以点中心 ±10 盒近似。</li>
 *   <li>chain 节点 li 与 SVG 路径在 JS 中有 2px 横向错位（li 左 +10 vs 路径 8+width·time），
 *       此处点与路径对齐绘制。</li>
 *   <li>ctrlKey||metaKey 用 LDLib2 UIEvent.isCtrlDown()（26.1 无 Screen.hasControlDown，版本稳定）。</li>
 *   <li>节点值悬浮标签：JS 为 CSS :hover 显示，此处按当前悬停/选中节点绘制。</li>
 * </ul>
 */
public class CurveEditorView extends UIElement {

    // ---- Snowstorm CSS 变量（common.css / Curve.vue scoped） ----
    private static final int COLOR_BAR = 0xFF34404A;          // --color-bar
    private static final int COLOR_CURVE = 0xFF657C86;        // --color-curve
    private static final int COLOR_SELECTION = 0x4C6E8EBF;    // --color-selection rgba(110,142,191,.3)
    private static final int COLOR_TEXT = 0xFFBCC3CA;         // --color-text
    private static final int COLOR_GRAYED = 0xFF939AA3;       // --color-text_grayed
    private static final int COLOR_ACCENT = 0xFF20DDFF;       // --color-accent
    private static final int COLOR_HOVER_BG = 0x19AED9FF;     // rgba(174,217,255,.1)

    private static final IGuiTexture TEX_BG = new ColorRectTexture(COLOR_BAR);
    private static final IGuiTexture TEX_CURVE = new ColorRectTexture(COLOR_CURVE);
    private static final IGuiTexture TEX_DASH = new ColorRectTexture(COLOR_SELECTION);
    private static final IGuiTexture TEX_DOT = new ColorRectTexture(COLOR_TEXT);
    private static final IGuiTexture TEX_DOT_HOVER = new ColorRectTexture(0xFFFFFFFF);
    private static final IGuiTexture TEX_DOT_SEL = new ColorRectTexture(COLOR_ACCENT);
    private static final IGuiTexture TEX_HANDLE = new ColorRectTexture(COLOR_GRAYED);
    private static final IGuiTexture TEX_HOVER_BG = new ColorRectTexture(COLOR_HOVER_BG);

    /** JS data: height = 140。 */
    private static final int DEFAULT_HEIGHT = 140;
    /** JS data: handle_offset = 24。 */
    private static final double HANDLE_OFFSET = 24;
    /** 非 chain 布局：首尾 add 帽宽（CSS max-width 20px）。 */
    private static final double ADD_CAP = 20;

    private static final Object DRAG_SLIDE = new Object();
    private static final Object DRAG_HEIGHT = new Object();

    private final Curve curve;
    /** JS data height（slideCurveHeight 可拖）。 */
    private int height = DEFAULT_HEIGHT;
    private int lastLayoutHeight = -1;

    private final CurveDisplay display = new CurveDisplay();
    private final UIElement optionsBar = new UIElement();
    private final UIElement footerFill = new UIElement();

    // ---- 选项栏变更检测（每帧对比，避免重建丢焦点） ----
    private String lastMode = "";
    private int lastSelected = Integer.MIN_VALUE;
    private boolean lastSplitValue;
    private boolean lastSplitSlope;

    // ---- slideValue 拖拽状态（JS 闭包变量） ----
    private int slideIndex = -1;
    private boolean slideHandle;
    private boolean slideRight;
    private double slideStartX, slideStartY;
    private double startValue, startSlope, startTime, slideThreshold;
    private boolean syncedValue, syncedSlope;

    // ---- slideCurveHeight 拖拽状态 ----
    private double heightStartY;
    private int heightStartValue;

    // ---- 悬停追踪（26.1 IGUIContext 无鼠标位置，事件追踪代替） ----
    private double hoverX = -1, hoverY = -1;
    private boolean hoverInside;

    // 选项栏字段（重建时赋值，拖拽时刷新文本）
    private final List<TextField> optionFields = new ArrayList<>();

    public CurveEditorView(Curve curve) {
        this.curve = curve;
        // .curve：padding-top 12px、padding-bottom 8px
        layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN)
                .paddingTop(12).paddingBottom(8));

        display.layout(l -> l.widthPercent(100).height(height + 10));
        display.addEventListener(UIEvents.MOUSE_DOWN, this::onDisplayMouseDown);
        display.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDisplayDragUpdate);
        display.addEventListener(UIEvents.DRAG_END, this::onDisplayDragEnd);
        display.addEventListener(UIEvents.MOUSE_MOVE, this::onDisplayMouseMove);
        display.addEventListener(UIEvents.MOUSE_ENTER, e -> hoverInside = true);
        display.addEventListener(UIEvents.MOUSE_LEAVE, e -> hoverInside = false);

        optionsBar.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.ROW));

        // JS .curve_footer：fill_line（高度拖拽）+ Remove Curve 按钮
        UIElement footer = new UIElement();
        footer.layout(l -> l.widthPercent(100).height(30).flexDirection(FlexDirection.ROW));
        footerFill.layout(l -> l.flex(1).heightPercent(100));
        footerFill.addEventListener(UIEvents.MOUSE_DOWN, this::onHeightMouseDown);
        footerFill.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onHeightDragUpdate);
        Button removeCurve = new Button();
        removeCurve.setText(Component.literal("Remove Curve"));
        removeCurve.setOnClick(e -> {
            curve.remove();
            // Vue 从 Data 列表移除后重渲染；此处同步移除自身视图（Sidebar 重建前的即时反馈）
            if (getParent() != null) getParent().removeChild(this);
        });
        removeCurve.layout(l -> l.heightPercent(100));
        footer.addChildren(footerFill, removeCurve);

        addChildren(display, optionsBar, footer);
    }

    public Curve getCurve() {
        return curve;
    }

    private String mode() {
        return curve.config.mode;
    }

    private boolean isChain() {
        return "bezier_chain".equals(mode());
    }

    private double displayWidth() {
        return display.getSizeWidth();
    }

    // ==================================================================
    // 每帧同步（root draw 钩子）：选项栏重建检测 + 显示区高度布局
    // ==================================================================

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
        if (height != lastLayoutHeight) {
            lastLayoutHeight = height;
            display.layout(l -> l.height(height + 10));
        }
        String mode = mode();
        int sel = curve.selected_point;
        boolean valid = sel >= 0 && sel < curve.nodes.size();
        boolean splitV = false, splitS = false;
        if (valid && isChain() && curve.nodes.get(sel) instanceof Config.BezierNode node) {
            splitV = node.left_value != node.right_value;
            splitS = node.left_slope != node.right_slope;
        }
        if (!mode.equals(lastMode) || sel != lastSelected
                || splitV != lastSplitValue || splitS != lastSplitSlope) {
            lastMode = mode;
            lastSelected = sel;
            lastSplitValue = splitV;
            lastSplitSlope = splitS;
            rebuildOptionsBar();
        }
    }

    // ==================================================================
    // 点选项栏（JS .curve_point_options）
    // ==================================================================

    private void rebuildOptionsBar() {
        optionsBar.clearAllChildren();
        optionFields.clear();
        int sel = curve.selected_point;
        if (sel < 0 || sel >= curve.nodes.size()) return; // v-if nodes[selected_point] !== undefined

        if (!isChain()) {
            // `${selected_point+1}/${nodes.length}` + 数值输入 + （非 bezier）删除工具
            Label index = new Label();
            index.setText(Component.literal((sel + 1) + "/" + curve.nodes.size()));
            index.layout(l -> l.heightPercent(100));
            optionsBar.addChild(index);

            TextField value = new TextField();
            io.github.tt432.eyelib.client.gui.snowstorm.kit.SsTextField.applyTextStyle(value, io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme.TEXT);
            value.setText(JsSemantics.toJsString(curve.nodes.get(sel)), false);
            value.setTextResponder(text -> curve.setNode(curve.selected_point, text));
            value.layout(l -> l.flex(1).heightPercent(100));
            optionsBar.addChild(value);
            optionFields.add(value);
        } else {
            Config.BezierNode node = (Config.BezierNode) curve.nodes.get(sel);
            optionsBar.addChild(label("T:"));
            // T: v-model node.time + registerNodeChange('time')
            TextField time = numberField(JsSemantics.toJsString(node.time), text -> {
                Config.BezierNode n = selectedNode();
                if (n != null) {
                    n.time = JsonValues.jsParseFloat(text);
                    registerNodeChange("time");
                }
            });
            optionsBar.addChild(time);

            optionsBar.addChild(label("Value:"));
            TextField left = numberField(JsSemantics.toJsString(node.left_value), text -> {
                Config.BezierNode n = selectedNode();
                if (n != null) {
                    n.left_value = JsonValues.jsParseFloat(text);
                    registerNodeChange("left_value");
                }
            });
            optionsBar.addChild(left);
            if (node.left_value != node.right_value) {
                TextField right = numberField(JsSemantics.toJsString(node.right_value), text -> {
                    Config.BezierNode n = selectedNode();
                    if (n != null) {
                        n.right_value = JsonValues.jsParseFloat(text);
                        registerNodeChange("right_value");
                    }
                });
                optionsBar.addChild(right);
            }
            // Connect Sides（值）：left==right → right+=0.1，否则 right=left
            Button connectV = new Button();
            connectV.setText(Component.literal(node.left_value == node.right_value ? "=" : "≠"));
            connectV.setOnClick(e -> {
                Config.BezierNode n = selectedNode();
                if (n != null) {
                    if (n.left_value == n.right_value) n.right_value += 0.1;
                    else n.right_value = n.left_value;
                    registerNodeChange(null);
                }
            });
            connectV.layout(l -> l.width(24).heightPercent(100));
            optionsBar.addChild(connectV);

            optionsBar.addChild(label("Slope:"));
            TextField leftSlope = numberField(JsSemantics.toJsString(node.left_slope), text -> {
                Config.BezierNode n = selectedNode();
                if (n != null) {
                    n.left_slope = JsonValues.jsParseFloat(text);
                    registerNodeChange("left_slope");
                }
            });
            optionsBar.addChild(leftSlope);
            if (node.left_slope != node.right_slope) {
                TextField rightSlope = numberField(JsSemantics.toJsString(node.right_slope), text -> {
                    Config.BezierNode n = selectedNode();
                    if (n != null) {
                        n.right_slope = JsonValues.jsParseFloat(text);
                        registerNodeChange("right_slope");
                    }
                });
                optionsBar.addChild(rightSlope);
            }
            Button connectS = new Button();
            connectS.setText(Component.literal(node.left_slope == node.right_slope ? "=" : "≠"));
            connectS.setOnClick(e -> {
                Config.BezierNode n = selectedNode();
                if (n != null) {
                    if (n.left_slope == n.right_slope) n.right_slope += 0.1;
                    else n.right_slope = n.left_slope;
                    registerNodeChange(null);
                }
            });
            connectS.layout(l -> l.width(24).heightPercent(100));
            optionsBar.addChild(connectS);
        }

        // 删除节点工具（v-if mode !== 'bezier'；JS 字符 ⴩）
        if (!"bezier".equals(mode())) {
            Button removeNode = io.github.tt432.eyelib.client.gui.snowstorm.kit.SsIconButton.ghost(
                    "x", 9, e -> curve.removeNode(curve.selected_point));
            removeNode.layout(l -> l.width(24).heightPercent(100));
            optionsBar.addChild(removeNode);
        }
        // .curve_point_options：label padding 4px 8px + input 30px 高 → 行 30px
        optionsBar.layout(l -> l.height(30).alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
    }

    private Label label(String text) {
        Label label = new Label();
        label.setText(Component.literal(text));
        label.layout(l -> l.heightPercent(100));
        return label;
    }

    private TextField numberField(String initial, java.util.function.Consumer<String> responder) {
        TextField field = new TextField();
        io.github.tt432.eyelib.client.gui.snowstorm.kit.SsTextField.applyTextStyle(field, io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme.TEXT);
        field.setText(initial, false);
        field.setTextResponder(responder);
        field.layout(l -> l.flex(1).heightPercent(100));
        optionFields.add(field);
        return field;
    }

    private Config.@org.jspecify.annotations.Nullable BezierNode selectedNode() {
        int sel = curve.selected_point;
        if (sel >= 0 && sel < curve.nodes.size() && curve.nodes.get(sel) instanceof Config.BezierNode node) {
            return node;
        }
        return null;
    }

    /** JS registerNodeChange(key)：parseFloat 四字段（Java 已为 double）+ updateMinMax + 左改同步右 + registerEdit。 */
    private void registerNodeChange(@org.jspecify.annotations.Nullable String key) {
        Config.BezierNode point = selectedNode();
        if (isChain() && point != null) {
            curve.updateMinMax();
            if ("left_value".equals(key)) point.right_value = point.left_value;
            if ("left_slope".equals(key)) point.right_slope = point.left_slope;
        }
        EditHistory.registerEdit("edit curve node");
        curve.updateSVG();
    }

    /** 拖拽中刷新选项栏文本（Vue v-model 双向同步等价；notify=false 不回触发 responder）。 */
    private void refreshOptionTexts() {
        int sel = curve.selected_point;
        if (sel < 0 || sel >= curve.nodes.size()) return;
        if (!isChain()) {
            if (!optionFields.isEmpty()) {
                optionFields.get(0).setText(JsSemantics.toJsString(curve.nodes.get(sel)), false);
            }
            return;
        }
        if (!(curve.nodes.get(sel) instanceof Config.BezierNode node)) return;
        // 字段顺序：time, left, [right], leftSlope, [rightSlope]
        int i = 0;
        if (i < optionFields.size()) optionFields.get(i++).setText(JsSemantics.toJsString(node.time), false);
        if (i < optionFields.size()) optionFields.get(i++).setText(JsSemantics.toJsString(node.left_value), false);
        if (node.left_value != node.right_value && i < optionFields.size()) {
            optionFields.get(i++).setText(JsSemantics.toJsString(node.right_value), false);
        }
        if (i < optionFields.size()) optionFields.get(i++).setText(JsSemantics.toJsString(node.left_slope), false);
        if (node.left_slope != node.right_slope && i < optionFields.size()) {
            optionFields.get(i).setText(JsSemantics.toJsString(node.right_slope), false);
        }
    }

    // ==================================================================
    // 显示区交互（JS slideValue / addNode / addBezierChainNode）
    // ==================================================================

    private void onDisplayMouseMove(UIEvent e) {
        Vector2f local = display.getLocalMouse(e.x, e.y);
        hoverX = local.x;
        hoverY = local.y;
    }

    private void onDisplayMouseDown(UIEvent e) {
        if (e.button != 0) return;
        Vector2f local = display.getLocalMouse(e.x, e.y);
        double lx = local.x, ly = local.y;
        String mode = mode();
        int n = curve.nodes.size();

        if (isChain()) {
            // slope 手柄优先（JS: 手柄是 li 内子元素，mousedown 先命中手柄）
            for (int i = 0; i < n; i++) {
                Config.BezierNode node = (Config.BezierNode) curve.nodes.get(i);
                double dotX = chainDotX(node), dotY = valueY(node.left_value);
                if (hasLeftHandle(i)) {
                    double hx = dotX - HANDLE_OFFSET * Math.cos(Math.atan(node.left_slope));
                    double hy = dotY + HANDLE_OFFSET * Math.sin(Math.atan(node.left_slope));
                    if (Math.abs(lx - hx) <= 8 && Math.abs(ly - hy) <= 8) {
                        beginSlide(i, true, false, lx, ly);
                        e.stopPropagation();
                        return;
                    }
                }
                if (hasRightHandle(i)) {
                    double hx = dotX + HANDLE_OFFSET * Math.cos(Math.atan(node.right_slope));
                    double hy = dotY - HANDLE_OFFSET * Math.sin(Math.atan(node.right_slope));
                    if (Math.abs(lx - hx) <= 8 && Math.abs(ly - hy) <= 8) {
                        beginSlide(i, true, true, lx, ly);
                        e.stopPropagation();
                        return;
                    }
                }
            }
            // 节点（点中心 ±10 盒，见类文档偏离）
            for (int i = 0; i < n; i++) {
                Config.BezierNode node = (Config.BezierNode) curve.nodes.get(i);
                if (Math.abs(lx - chainDotX(node)) <= 10 && Math.abs(ly - valueY(node.left_value)) <= 10) {
                    beginSlide(i, false, false, lx, ly);
                    e.stopPropagation();
                    return;
                }
            }
            // 空白区：addBezierChainNode（JS 要求 target 是 .curve_controls 本体）
            addBezierChainNode(lx, ly);
            return;
        }

        // 非 chain：列几何（首尾 add 帽 20px + flex 单位）
        double w = displayWidth();
        if ("bezier".equals(mode)) {
            double u = w / n;
            int index = (int) Math.min(n - 1, Math.max(0, Math.floor(lx / u)));
            beginSlide(index, false, false, lx, ly);
            e.stopPropagation();
            return;
        }
        double u = (w - 2 * ADD_CAP) / (2.0 * n - 1);
        if (lx < ADD_CAP) { // add_0
            addNode(0);
            return;
        }
        if (lx >= w - ADD_CAP) { // add_n
            addNode(n);
            return;
        }
        double rel = lx - ADD_CAP;
        int cell = (int) Math.floor(rel / u);
        if (cell % 2 == 0) { // 节点列
            beginSlide(cell / 2, false, false, lx, ly);
            e.stopPropagation();
        } else { // 中间 add 列
            addNode(cell / 2 + 1);
        }
    }

    /** JS slideValue 的拖拽开始部分（selected_point 置位 + 起点快照）。 */
    private void beginSlide(int index, boolean handle, boolean right, double lx, double ly) {
        curve.selected_point = index;
        slideIndex = index;
        slideHandle = handle;
        slideRight = right;
        slideStartX = lx;
        slideStartY = ly;
        slideThreshold = 0.03 * (curve.max - curve.min);
        if (isChain()) {
            Config.BezierNode node = (Config.BezierNode) curve.nodes.get(index);
            startTime = node.time;
            startValue = node.left_value;
            startSlope = node.left_slope;
            syncedValue = node.left_value == node.right_value;
            syncedSlope = node.left_slope == node.right_slope;
        } else {
            startValue = ((Number) curve.nodes.get(index)).doubleValue();
            startSlope = 0;
            startTime = 0;
            syncedValue = syncedSlope = false;
        }
        display.startDrag(DRAG_SLIDE, null);
    }

    private void onDisplayDragUpdate(UIEvent e) {
        if (e.dragHandler == null || e.dragHandler.getDraggingObject() != DRAG_SLIDE || slideIndex < 0) return;
        Vector2f local = display.getLocalMouse(e.x, e.y);
        slideUpdate(local.x, local.y, e.isCtrlDown());
    }

    /** JS slideValue.slide（事件层 mousemove）；ctrl 经 UIEvent.isCtrlDown() 传入（26.1 无 Screen.hasControlDown）。 */
    private void slideUpdate(double mx, double my, boolean ctrl) {
        // JS: e2.ctrlKey || e2.metaKey → 调用方传 UIEvent.isCtrlDown()
        if (slideHandle) {
            Config.BezierNode node = (Config.BezierNode) curve.nodes.get(slideIndex);
            double slope = startSlope + (slideStartY - my) * 0.05 * (slideRight ? 1 : -1);
            slope = SnowstormUtil.snapToValues(slope, new double[]{0}, slideThreshold);
            if (syncedSlope || !slideRight) node.left_slope = SnowstormUtil.roundTo(slope, 2);
            if (syncedSlope || slideRight) node.right_slope = SnowstormUtil.roundTo(slope, 2);
            curve.updateSVG();
        } else {
            double value = startValue + (slideStartY - my) / height * (curve.max - curve.min);
            value = SnowstormUtil.snapToValues(value, new double[]{1, 0, -1}, slideThreshold);
            if (isChain()) {
                Config.BezierNode node = (Config.BezierNode) curve.nodes.get(slideIndex);
                double time = startTime - (slideStartX - mx) / (displayWidth() - 16);
                node.time = SnowstormUtil.clamp(SnowstormUtil.roundTo(time, 3), 0, 1);
                if (syncedValue || !ctrl) node.left_value = SnowstormUtil.roundTo(value, 2);
                if (syncedValue || ctrl) node.right_value = SnowstormUtil.roundTo(value, 2);
                curve.updateMinMax();
                curve.updateSVG();
            } else {
                curve.setNode(slideIndex, value);
            }
        }
        refreshOptionTexts();
    }

    private void onDisplayDragEnd(UIEvent e) {
        if (e.dragHandler == null || e.dragHandler.getDraggingObject() != DRAG_SLIDE) return;
        // JS stopSlide：任何 mouseup 结束滑动即 registerEdit（含未移动的点击）
        EditHistory.registerEdit("edit curve node");
        slideIndex = -1;
    }

    /** JS addNode(index)：插入相邻两点均值（两位舍入）。 */
    private void addNode(int index) {
        double value = 0;
        if (index - 1 >= 0 && index < curve.nodes.size()) {
            value = (((Number) curve.nodes.get(index - 1)).doubleValue()
                    + ((Number) curve.nodes.get(index)).doubleValue()) / 2;
        }
        value = JsSemantics.jsRound(value * 100) / 100;
        curve.nodes.add(index, value);
        curve.updateSVG();
        EditHistory.registerEdit("add curve node");
    }

    /** JS addBezierChainNode(event)：由点击局部坐标换算 time/value（offsetY 第一行死代码略）。 */
    private void addBezierChainNode(double lx, double ly) {
        double value = curve.min + (curve.max - curve.min) * ((height + 5) - ly) / height;
        value = SnowstormUtil.roundTo(value, 2);
        Config.BezierNode node = new Config.BezierNode();
        node.time = SnowstormUtil.clamp(SnowstormUtil.roundTo(lx / displayWidth(), 2), 0, 1);
        node.left_value = value;
        node.right_value = value;
        node.left_slope = 0;
        node.right_slope = 0;
        curve.nodes.add(node);
        curve.updateSVG();
        EditHistory.registerEdit("add curve node");
    }

    // ==================================================================
    // 页脚高度拖拽（JS slideCurveHeight）
    // ==================================================================

    private void onHeightMouseDown(UIEvent e) {
        if (e.button != 0) return;
        heightStartY = e.y;
        heightStartValue = height;
        footerFill.startDrag(DRAG_HEIGHT, null);
    }

    private void onHeightDragUpdate(UIEvent e) {
        if (e.dragHandler == null || e.dragHandler.getDraggingObject() != DRAG_HEIGHT) return;
        height = (int) (heightStartValue + (e.y - heightStartY)); // JS 无 clamp，as-is
        curve.updateSVG();
    }

    // ==================================================================
    // 几何（Curve.vue updateSVG / CSS flex 布局的确定性复算）
    // ==================================================================

    /** JS getPoint 的 y：(height+5) - ((v-min)/(max-min))*height。 */
    private double valueY(double v) {
        return (height + 5) - ((v - curve.min) / (curve.max - curve.min)) * height;
    }

    /** chain 节点 x（路径对齐）：8 + (W-16)·time。 */
    private double chainDotX(Config.BezierNode node) {
        return 8 + (displayWidth() - 16) * node.time;
    }

    /** JS v-if: nodes.find(n -> n.time < point.time)（DOM 序查找，时间比较）。 */
    private boolean hasLeftHandle(int index) {
        double t = ((Config.BezierNode) curve.nodes.get(index)).time;
        for (Object o : curve.nodes) {
            if (((Config.BezierNode) o).time < t) return true;
        }
        return false;
    }

    private boolean hasRightHandle(int index) {
        double t = ((Config.BezierNode) curve.nodes.get(index)).time;
        for (Object o : curve.nodes) {
            if (((Config.BezierNode) o).time > t) return true;
        }
        return false;
    }

    /** 非 chain 节点列中心 x（dot 中心；路径 x = 此值 + 1，见 updateSVG 的 +5/-parent_offset+1）。 */
    private double nodeCenterX(int i) {
        int n = curve.nodes.size();
        double w = displayWidth();
        if ("bezier".equals(mode())) {
            return (w / n) * (i + 0.5);
        }
        double u = (w - 2 * ADD_CAP) / (2.0 * n - 1);
        return ADD_CAP + 2 * i * u + u / 2;
    }

    // ==================================================================
    // 显示区绘制（JS updateSVG 的 SVG 等价）
    // ==================================================================

    /** 绘制基元抽象：两版本 GUIContext 均为 drawTexture(IGuiTexture, x, y, w, h)。 */
    private interface Sink {
        void rect(IGuiTexture tex, float x, float y, float w, float h);

        /** CSS border-radius 50% 圆形近似（弦分割 5 条带；Curve.vue .curve_point/.curve_handle_point）。 */
        default void disc(IGuiTexture tex, float cx, float cy, float r) {
            // 弦高比例（半宽/半径）: 1.0, 0.95, 0.8, 0.55 —— 5 条带近似圆
            float[] half = {1.0f, 0.95f, 0.8f, 0.55f};
            float step = r / half.length;
            for (int i = 0; i < half.length; i++) {
                float hw = r * half[i];
                float y0 = cy - r + i * step;
                rect(tex, cx - hw, y0, 2 * hw, step);
                rect(tex, cx - hw, cy + r - (i + 1) * step, 2 * hw, step);
            }
        }
    }

    private class CurveDisplay extends UIElement {

        //? if <26.1 {
        @Override
        public void drawBackgroundAdditional(GUIContext guiContext) {
            render(guiContext::drawTexture);
        }
        //?} else {
        @Override
        protected void drawBackgroundAdditional(IGUIContext context) {
            render(context::drawTexture);
        }
        //?}

        private void render(Sink s) {
            float x = getPositionX(), y = getPositionY(), w = getSizeWidth(), h = getSizeHeight();
            s.rect(TEX_BG, x, y, w, h);
            if (w <= 0 || curve.nodes.isEmpty()) return;

            // 列悬停高亮（.curve_node:hover 背景）
            if (hoverInside && !isChain() && hoverX >= 0) {
                drawColumnHover(s, x, y, w, h);
            }

            // 水平参考线（ground=0 / ceiling=1 / negative=-1，dasharray 6）
            dashedHLine(s, x, x + w, (float) (y + valueY(0)), 6);
            dashedHLine(s, x, x + w, (float) (y + valueY(1)), 6);
            dashedHLine(s, x, x + w, (float) (y + valueY(-1)), 6);

            switch (mode()) {
                case "linear" -> renderLinear(s, x, y, w);
                case "bezier" -> renderBezier(s, x, y, w);
                case "bezier_chain" -> renderChain(s, x, y, w);
                default -> renderCatmullRom(s, x, y, w);
            }

            // min/max 文本（左上/左下，pointer-events:none）
            s.rect(new TextTexture(JsSemantics.toJsString(curve.max), COLOR_TEXT), x + 2, y, 0, 0);
            s.rect(new TextTexture(JsSemantics.toJsString(curve.min), COLOR_TEXT), x + 2, y + h - 9, 0, 0);
        }

        /** 非 chain：节点列几何 + 折线路径（JS getPoint: x = i*gap + start）。 */
        private double[] nodePathXs() {
            int n = curve.nodes.size();
            double[] xs = new double[n];
            if (n == 1) {
                xs[0] = nodeCenterX(0) + 1;
                return xs;
            }
            double start = nodeCenterX(0) + 1;
            double end = nodeCenterX(n - 1) + 1;
            double gap = (end - start) / (n - 1);
            for (int i = 0; i < n; i++) xs[i] = start + i * gap;
            return xs;
        }

        private void renderLinear(Sink s, float ox, float oy, float w) {
            int n = curve.nodes.size();
            double[] xs = nodePathXs();
            double[] ys = new double[n];
            for (int i = 0; i < n; i++) ys[i] = valueY(((Number) curve.nodes.get(i)).doubleValue());
            for (int i = 1; i < n; i++) {
                segment(s, TEX_CURVE, ox + xs[i - 1], oy + ys[i - 1], ox + xs[i], oy + ys[i]);
            }
            // 垂直虚线（首尾，dasharray 8）
            dashedVLine(s, ox + xs[0], oy, oy + getSizeHeight(), 8);
            dashedVLine(s, ox + xs[n - 1], oy, oy + getSizeHeight(), 8);
            drawDots(s, ox, oy, xs, ys);
        }

        private void renderBezier(Sink s, float ox, float oy, float w) {
            int n = curve.nodes.size();
            if (n < 4) return; // bezier 恒 4 节点（mode onchange 截断补零）
            double[] xs = nodePathXs();
            double[] ys = new double[n];
            for (int i = 0; i < n; i++) ys[i] = valueY(((Number) curve.nodes.get(i)).doubleValue());
            cubic(s, TEX_CURVE, ox + xs[0], oy + ys[0], ox + xs[1], oy + ys[1],
                    ox + xs[2], oy + ys[2], ox + xs[3], oy + ys[3]);
            // bezier_handles：P0-P1 / P2-P3
            segment(s, TEX_DASH, ox + xs[0], oy + ys[0], ox + xs[1], oy + ys[1]);
            segment(s, TEX_DASH, ox + xs[2], oy + ys[2], ox + xs[3], oy + ys[3]);
            dashedVLine(s, ox + xs[0], oy, oy + getSizeHeight(), 8);
            dashedVLine(s, ox + xs[3], oy, oy + getSizeHeight(), 8);
            drawDots(s, ox, oy, xs, ys);
        }

        private void renderCatmullRom(Sink s, float ox, float oy, float w) {
            int n = curve.nodes.size();
            double[] xs = nodePathXs();
            double[] ys = new double[n];
            for (int i = 0; i < n; i++) ys[i] = valueY(((Number) curve.nodes.get(i)).doubleValue());
            // JS toCatmullRomBezier(points, tension=0.5, closing=false)：tens = 0.5*12 = 6，
            // 端点复制；段 i ∈ [0, n-2]
            double tens = 6;
            for (int i = 0; i < n - 1; i++) {
                double[] p0 = i == 0 ? new double[]{xs[0], ys[0]} : new double[]{xs[i - 1], ys[i - 1]};
                double[] p1 = {xs[i], ys[i]};
                double[] p2 = {xs[i + 1], ys[i + 1]};
                double[] p3 = i == n - 2 ? new double[]{xs[n - 1], ys[n - 1]} : new double[]{xs[i + 2], ys[i + 2]};
                double c1x = p1[0] + (p2[0] - p0[0]) / tens, c1y = p1[1] + (p2[1] - p0[1]) / tens;
                double c2x = p2[0] - (p3[0] - p1[0]) / tens, c2y = p2[1] - (p3[1] - p1[1]) / tens;
                cubic(s, TEX_CURVE, ox + p1[0], oy + p1[1], ox + c1x, oy + c1y, ox + c2x, oy + c2y, ox + p2[0], oy + p2[1]);
            }
            // 垂直虚线（start+gap / end-gap）
            if (n >= 2) {
                double gap = xs[1] - xs[0];
                dashedVLine(s, ox + xs[0] + gap, oy, oy + getSizeHeight(), 8);
                dashedVLine(s, ox + xs[n - 1] - gap, oy, oy + getSizeHeight(), 8);
            }
            drawDots(s, ox, oy, xs, ys);
        }

        private void renderChain(Sink s, float ox, float oy, float w) {
            int n = curve.nodes.size();
            double width = w - 16;
            // JS: nodes.slice().sort((a,b) => a.time - b.time)（稳定排序副本）
            List<Config.BezierNode> sorted = new ArrayList<>(n);
            for (Object o : curve.nodes) sorted.add((Config.BezierNode) o);
            sorted.sort((a, b) -> Double.compare(a.time, b.time));

            for (int i = 0; i < sorted.size() - 1; i++) {
                Config.BezierNode before = sorted.get(i);
                Config.BezierNode after = sorted.get(i + 1);
                double dt = after.time - before.time;
                double p0x = 8 + width * (before.time + dt * (0 / 3.0)), p0y = before.right_value;
                double p1x = 8 + width * (before.time + dt * (1 / 3.0)), p1y = before.right_value + before.right_slope * (1 / 3.0);
                double p2x = 8 + width * (before.time + dt * (2 / 3.0)), p2y = after.left_value - after.left_slope * (1 / 3.0);
                double p3x = 8 + width * (before.time + dt * (3 / 3.0)), p3y = after.left_value;
                double q0y = valueY(p0y), q1y = valueY(p1y), q2y = valueY(p2y), q3y = valueY(p3y);
                // i==0：M 8,{p0y} L {p0x},{p0y} 引入线
                if (i == 0) segment(s, TEX_CURVE, ox + 8, oy + q0y, ox + p0x, oy + q0y);
                cubic(s, TEX_CURVE, ox + p0x, oy + q0y, ox + p1x, oy + q1y, ox + p2x, oy + q2y, ox + p3x, oy + q3y);
                // i==n-2：尾线 L 8+width
                if (i == sorted.size() - 2) segment(s, TEX_CURVE, ox + p3x, oy + q3y, ox + 8 + width, oy + q3y);
                // bezier_handles
                segment(s, TEX_DASH, ox + p0x, oy + q0y, ox + p1x, oy + q1y);
                segment(s, TEX_DASH, ox + p2x, oy + q2y, ox + p3x, oy + q3y);
            }

            // 垂直虚线（start/end，DOM 序首末节点，as-is quirk；li 中心 ≈ 11+width·time）
            double start = 11 + width * ((Config.BezierNode) curve.nodes.get(0)).time;
            double end = 11 + width * ((Config.BezierNode) curve.nodes.get(n - 1)).time;
            dashedVLine(s, ox + start, oy, oy + getSizeHeight(), 8);
            dashedVLine(s, ox + end, oy, oy + getSizeHeight(), 8);

            // 节点点 + slope 手柄
            for (int i = 0; i < n; i++) {
                Config.BezierNode node = (Config.BezierNode) curve.nodes.get(i);
                double dotX = chainDotX(node), dotY = valueY(node.left_value);
                boolean selected = curve.selected_point == i;
                IGuiTexture tex = selected ? TEX_DOT_SEL : TEX_DOT;
                // .curve_node .curve_point：8×8（选中 10×10）圆形
                double r = selected ? 5 : 4;
                s.disc(tex, (float) (ox + dotX), (float) (oy + dotY), (float) r);
                if (hasLeftHandle(i)) {
                    double hx = dotX - HANDLE_OFFSET * Math.cos(Math.atan(node.left_slope));
                    double hy = dotY + HANDLE_OFFSET * Math.sin(Math.atan(node.left_slope));
                    s.disc(TEX_HANDLE, (float) (ox + hx), (float) (oy + hy), 4);
                }
                if (hasRightHandle(i)) {
                    double hx = dotX + HANDLE_OFFSET * Math.cos(Math.atan(node.right_slope));
                    double hy = dotY - HANDLE_OFFSET * Math.sin(Math.atan(node.right_slope));
                    s.disc(TEX_HANDLE, (float) (ox + hx), (float) (oy + hy), 4);
                }
                if (selected || (hoverInside && Math.abs(hoverX - dotX) <= 10 && Math.abs(hoverY - dotY) <= 10)) {
                    String valueText = node.left_value != node.right_value
                            ? JsSemantics.toJsString(node.left_value) + " / " + JsSemantics.toJsString(node.right_value)
                            : JsSemantics.toJsString(node.left_value);
                    String label = valueText + " / " + JsSemantics.toJsString(SnowstormUtil.roundTo(node.time, 2));
                    s.rect(new TextTexture(label, COLOR_TEXT), (float) (ox + dotX + 12), (float) (oy + dotY - 8), 0, 0);
                }
            }
        }

        /** 非 chain 节点点（±256 可见性 v-show）+ add 标记。 */
        private void drawDots(Sink s, float ox, float oy, double[] xs, double[] ys) {
            int n = curve.nodes.size();
            for (int i = 0; i < n; i++) {
                double v = ((Number) curve.nodes.get(i)).doubleValue();
                if (v > 256 || v < -256) continue; // v-show="value <= 256 && value >= -256"
                boolean selected = curve.selected_point == i;
                boolean hovered = hoverInside && !isChain() && hoverColumnIndex() == i;
                IGuiTexture tex = selected ? TEX_DOT_SEL : (hovered ? TEX_DOT_HOVER : TEX_DOT);
                double r = selected ? 5 : 4;
                // 点以列中心（非路径 +1）对齐，同 CSS li 内居中；CSS 圆形 → disc
                double cx = xs[i] - 1;
                s.disc(tex, (float) (ox + cx), (float) (oy + ys[i]), (float) r);
                if (hovered || selected) {
                    s.rect(new TextTexture(JsSemantics.toJsString(v), COLOR_TEXT),
                            (float) (ox + cx + 12), (float) (oy + ys[i] - 8), 0, 0);
                }
            }
        }

        private int hoverColumnIndex() {
            int n = curve.nodes.size();
            if ("bezier".equals(mode())) {
                double u = displayWidth() / n;
                return (int) Math.min(n - 1, Math.max(0, Math.floor(hoverX / u)));
            }
            double u = (displayWidth() - 2 * ADD_CAP) / (2.0 * n - 1);
            if (hoverX < ADD_CAP || hoverX >= displayWidth() - ADD_CAP) return -1;
            int cell = (int) Math.floor((hoverX - ADD_CAP) / u);
            return cell % 2 == 0 ? cell / 2 : -1;
        }

        private void drawColumnHover(Sink s, float ox, float oy, float w, float h) {
            int n = curve.nodes.size();
            double u, x0, x1;
            if ("bezier".equals(mode())) {
                u = w / n;
                int index = (int) Math.min(n - 1, Math.max(0, Math.floor(hoverX / u)));
                x0 = index * u;
                x1 = x0 + u;
            } else {
                if (hoverX < 0 || hoverX > w) return;
                u = (w - 2 * ADD_CAP) / (2.0 * n - 1);
                if (hoverX < ADD_CAP) {
                    x0 = 0;
                    x1 = ADD_CAP;
                } else if (hoverX >= w - ADD_CAP) {
                    x0 = w - ADD_CAP;
                    x1 = w;
                } else {
                    int cell = (int) Math.floor((hoverX - ADD_CAP) / u);
                    x0 = ADD_CAP + cell * u;
                    x1 = x0 + u;
                }
            }
            s.rect(TEX_HOVER_BG, (float) (ox + x0), oy, (float) (x1 - x0), h);
        }
    }

    // ==================================================================
    // 绘制基元
    // ==================================================================

    /** 2px 实线段（采样步长 1px）。 */
    private static void segment(Sink s, IGuiTexture tex, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0;
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(dx, dy)));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            s.rect(tex, (float) (x0 + dx * t - 1), (float) (y0 + dy * t - 1), 2, 2);
        }
    }

    /** 三次贝塞尔采样（24 段）。 */
    private static void cubic(Sink s, IGuiTexture tex, double x0, double y0, double x1, double y1,
                              double x2, double y2, double x3, double y3) {
        double px = x0, py = y0;
        for (int i = 1; i <= 24; i++) {
            double t = i / 24.0, u = 1 - t;
            double bx = u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3;
            double by = u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y3;
            segment(s, tex, px, py, bx, by);
            px = bx;
            py = by;
        }
    }

    private static void dashedHLine(Sink s, float x0, float x1, float y, float dash) {
        for (float x = x0; x < x1; x += dash * 2) {
            s.rect(TEX_DASH, x, y - 1, Math.min(dash, x1 - x), 2);
        }
    }

    private static void dashedVLine(Sink s, double x, float y0, float y1, float dash) {
        for (float y = y0; y < y1; y += dash * 2) {
            s.rect(TEX_DASH, (float) x - 1, y, 2, Math.min(dash, y1 - y));
        }
    }
}
//?}

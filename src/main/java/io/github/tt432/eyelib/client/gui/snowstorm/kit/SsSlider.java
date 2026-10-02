package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Snowstorm 样式滑杆（common.css input[type=range] as-is）：track 4px bar 色、
 * thumb 20px 圆（2px accent 边 + interface 心；LDLib2 无圆纹理，用同心方块近似）。
 *
 * <p>用法：{@code new SsSlider(0, 10, 1.5, v -> ...)}；拖拽经 LDLib2 拖拽事件。
 */
public final class SsSlider extends UIElement {

    private final double min;
    private final double max;
    private final Consumer<Double> onChange;
    private double value;

    public SsSlider(double min, double max, double value, Consumer<Double> onChange) {
        this.min = min;
        this.max = max;
        this.value = value;
        this.onChange = onChange;
        // 宽度由父布局决定（widthPercent(100) 在 flex 行内会挤飞兄弟，实证 2026-10-01）
        layout(l -> l.height(20));

        // 轨道
        UIElement track = new UIElement();
        track.layout(l -> l.widthPercent(100).height(SsMetrics.SLIDER_TRACK_HEIGHT)
                .alignSelf(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        track.style(s -> s.backgroundTexture(new ColorRectTexture(SnowstormTheme.BAR)));
        addChild(track);

        // 拖拽：MOUSE_DOWN 起拖 + DRAG_SOURCE_UPDATE 跟手 + MOUSE_UP 收尾
        addEventListener(UIEvents.MOUSE_DOWN, e -> startDrag(null, null));
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> applyMouse(e.x));
    }

    public double value() {
        return value;
    }

    private void applyMouse(double mouseX) {
        double w = getSizeWidth();
        if (w <= 0) return;
        // 事件坐标为 GUI 绝对坐标 → 减元素左缘
        double t = Math.max(0, Math.min(1, (mouseX - getPositionX()) / w));
        setValue(min + t * (max - min));
    }

    public void setValue(double v) {
        double nv = Math.max(min, Math.min(max, v));
        if (nv != value) {
            value = nv;
            onChange.accept(nv);
        }
        markThumbDirty();
    }

    private void markThumbDirty() {
        // thumb 由 onDraw 按 value 现算位置，无需缓存失效
    }

    // ---- 绘制（GUIContext 版本分叉同 CurveEditorView 先例）----

    //? if <26.1 {
    @Override
    public void drawBackgroundAdditional(com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext ctx) {
        drawThumb(ctx::drawTexture);
    }
    //?} else {
    @Override
    protected void drawBackgroundAdditional(com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext ctx) {
        drawThumb(ctx::drawTexture);
    }
    //?}

    private void drawThumb(Sink sink) {
        float x = getPositionX();
        float w = getSizeWidth();
        float h = getSizeHeight();
        if (w <= 0 || h <= 0) return;
        double t = (value - min) / (max - min);
        float cx = x + (float) (t * w);
        float half = SsMetrics.SLIDER_THUMB_SIZE / 2f;
        float cy = getPositionY() + h / 2f;
        sink.rect(new ColorRectTexture(SnowstormTheme.ACCENT),
                cx - half, cy - half, SsMetrics.SLIDER_THUMB_SIZE, SsMetrics.SLIDER_THUMB_SIZE);
        int inset = 2;
        sink.rect(new ColorRectTexture(SnowstormTheme.INTERFACE),
                cx - half + inset, cy - half + inset,
                SsMetrics.SLIDER_THUMB_SIZE - inset * 2f, SsMetrics.SLIDER_THUMB_SIZE - inset * 2f);
    }

    private interface Sink {
        void rect(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture tex, float x, float y, float w, float h);
    }
}
//?}

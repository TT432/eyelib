//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import io.github.tt432.eyelib.client.nodegraph.preview.PreviewViewState;

/**
 * ref 节点预览图（ldlib1）：左键拖动 = 轨道球旋转、右键拖动 = 平移、滚轮 = 缩放、双击 = 重置。
 * 视角状态存在 {@link EvmNode} 上（{@link #view}），节点 widget 随选项变更重建时不丢。
 *
 * <p>事件只在预览区命中时处理并消费（{@code isMouseOverElement}），区外一律交还父级，
 * 画布平移/框选/节点拖动不受影响。注意：画布的右键框选（GraphViewWidget.mouseClicked）
 * 在子控件之前拦截右键，故右键平移由 {@link EvmGraphViewWidget} 命中检测后委托进来
 * （{@link #beginPanFromGraph} 系列）。
 */
final class RefPreviewWidget extends ImageWidget {
    /** 视角状态（EvmNode 持有，widget 重建不丢）。 */
    private final PreviewViewState view;
    /** false（ref.texture 静态贴图预览）= 不消费任何鼠标事件，全部交还父级。 */
    private final boolean interactive;
    private boolean rotating;
    private boolean panning;
    /** 双击检测：上次左键点击的 tick（双击窗口 10 tick，同 NodeWidget 标题双击展开）。 */
    private long lastClickTick = -100;

    RefPreviewWidget(int x, int y, int size, IGuiTexture texture, PreviewViewState view, boolean interactive) {
        super(x, y, size, size, texture);
        this.view = view;
        this.interactive = interactive;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (interactive && isMouseOverElement(mouseX, mouseY)) {
            if (button == 0) {
                long tick = getGui() == null ? 0 : getGui().getTickCount();
                if (tick - lastClickTick < 10) {
                    // 双击重置视角（不进入旋转手势）
                    view.reset();
                    lastClickTick = -100;
                    return true;
                }
                lastClickTick = tick;
                rotating = true;
                return true;
            }
            if (button == 1) {
                beginPanFromGraph();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 画布委托的右键平移入口（GraphViewWidget 先行拦截右键，见类注释）。 */
    void beginPanFromGraph() {
        panning = true;
    }

    boolean isInteractive() {
        return interactive;
    }

    /** 画布委托的右键平移移动（drag 增量已换算为画布视图坐标）。 */
    void panDragFromGraph(double dragX, double dragY) {
        if (panning) {
            view.panBy((float) dragX, (float) dragY);
        }
    }

    /** 画布委托的右键平移结束。 */
    void endPanFromGraph() {
        panning = false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (rotating) {            view.rotateBy((float) dragX, (float) dragY);
            return true;
        }
        if (panning) {
            view.panBy((float) dragX, (float) dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = rotating || panning;
        rotating = false;
        panning = false;
        if (wasDragging) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (interactive && isMouseOverElement(mouseX, mouseY)) {
            view.zoomBy((float) wheelDelta);
            return true;
        }
        return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
    }
}
//?}

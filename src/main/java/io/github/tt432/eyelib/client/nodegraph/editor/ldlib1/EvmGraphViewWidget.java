//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.DebugPanelWidget;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.GraphViewWidget;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.NodeWidget;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.ParameterPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.FreeGraphView;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import io.github.tt432.eyelib.client.nodegraph.workbench.GridLodRenderer;
import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

/**
 * EVM 画布：在 GraphViewWidget 之上做四件事——
 * <ul>
 *   <li>节点面板只留 EVM 组（清空 LDLib 内建节点组——内建节点无法回译为域模型，
 *       保存时静默丢弃，是用户陷阱）；</li>
 *   <li>禁用每 tick 的图执行（{@code setProcessor(null)}——编辑器只编辑不运行，
 *       避免 TriggerProcessor 空转），并移除假定 processor 非空的 DebugPanelWidget
 *       （其 step 按钮在 processor 为 null 时触发 LDLib 内部 NPE 崩客户端）；</li>
 *   <li>关闭 dev 环境默认开启的节点调试信息（红色 compute order 标题，面向用户是噪声）；</li>
 *   <li>给 {@link EvmNode} 装配 {@code uiRefresh} 钩子（选项变更 → 动态端口重算后
 *       重排节点 widget；graphprocessor 的 onPortsUpdated 在 UI 层无监听者，须手动刷新）。</li>
 * </ul>
 */
public class EvmGraphViewWidget extends GraphViewWidget {
    public EvmGraphViewWidget(EvmBaseGraph graph, int x, int y, int width, int height) {
        // additionalGroups：清空 LDLib 内建节点组（graph_processor.node.*）——
        // 内建节点无法回译为域模型（保存时静默丢弃，是用户陷阱），面板只留 EVM 组
        super(graph, x, y, width, height, groups -> {
            groups.clear();
            groups.add(EvmNodeRegistration.GROUP_PREFIX);
        });
        setProcessor(null);
        // LDLib 在 dev 环境默认开启节点调试信息（红色 compute order 标题），
        // 本编辑器面向用户，默认关闭
        setShowDebugInfo(false);
        // DebugPanelWidget 的 run/step 按钮直调 GraphViewWidget.runStep/runAll；
        // runStep 在 processor == null 时 NPE（stepIterator 未初始化即 hasNext，LDLib 自身缺陷）。
        // EVM 编辑器只编辑不运行（processor 恒为 null），必须整体移除调试面板，否则点 step 即崩客户端。
        widgets.stream().filter(DebugPanelWidget.class::isInstance).findFirst().ifPresent(this::removeWidget);
        // LDLib 参数面板（ParameterPanelWidget）只读且无增删，由工作台变量面板
        // （VariablesPanel，规格 §3.2）取代——整体移除，避免两套变量入口并存
        widgets.stream().filter(ParameterPanelWidget.class::isInstance).findFirst().ifPresent(this::removeWidget);
        // LOD 网格：关闭 LDLib 固定 50 单位网格（缩小时线宽亚像素锯齿/糊成灰霾，
        // 用户实测报告），改挂程序化 LOD 网格层（GridLodRenderer，间距按 2 幂升档、
        // 线宽恒 1 物理 px），挂在所有节点之下（index 0）
        getFreeGraphView().setDrawGrid(false);
        gridLayer = new GridLayer();
        getFreeGraphView().addWidget(0, gridLayer);
    }

    /** LOD 网格层（超类构造触发 loadGraph 时尚未创建，故可空并在 loadGraph 中判空）。 */
    private @Nullable GridLayer gridLayer;

    /** LOD 网格绘制层：零尺寸（不参与命中），在 FreeGraphView 视图坐标系内绘制，挂在节点之下。 */
    private final class GridLayer extends Widget {
        GridLayer() {
            super(0, 0, 0, 0);
        }

        @Override
        public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            FreeGraphView fgv = getFreeGraphView();
            float scale = fgv.getScale();
            if (scale <= 0) {
                return;
            }
            GridLodRenderer.draw(graphics, fgv.getXOffset(), fgv.getYOffset(),
                    fgv.getSizeWidth() / scale, fgv.getSizeHeight() / scale, 50f);
        }
    }

    @Override
    public EvmBaseGraph getGraph() {
        return (EvmBaseGraph) super.getGraph();
    }

    @Override
    public void loadGraph() {
        super.loadGraph();
        // loadGraph 会 clearAllWidgets 后重挂节点，把网格层重新压回所有节点之下
        if (gridLayer != null) {
            getFreeGraphView().addWidget(0, gridLayer);
        }
        replaceWithEvmNodeWidgets();
        wireRefreshHooks();
    }

    @Override
    public void addNode(BaseNode node) {
        super.addNode(node);
        replaceWithEvmNodeWidgets();
        wireRefreshHook(node);
    }

    @Override
    public void pasteTo(double mouseX, double mouseY) {
        super.pasteTo(mouseX, mouseY);
        replaceWithEvmNodeWidgets();
        wireRefreshHooks();
    }

    /**
     * LDLib 的 loadGraph/addNode/pasteTo 硬编码 {@code new NodeWidget}——把 EVM 节点的
     * 裸 NodeWidget 原位换成 {@link EvmNodeWidget}（端口行内编辑器依赖其 reloadWidget 钩子）。
     */
    private void replaceWithEvmNodeWidgets() {
        for (var entry : new ArrayList<>(getNodeMap().entrySet())) {
            if (entry.getKey() instanceof EvmNode && !(entry.getValue() instanceof EvmNodeWidget)) {
                NodeWidget old = entry.getValue();
                getFreeGraphView().removeWidget(old);
                EvmNodeWidget widget = new EvmNodeWidget(this, entry.getKey());
                getFreeGraphView().addWidget(widget);
                getNodeMap().put(entry.getKey(), widget);
            }
        }
    }

    private void wireRefreshHooks() {
        for (BaseNode node : getGraph().nodes) {
            wireRefreshHook(node);
        }
    }

    private void wireRefreshHook(BaseNode node) {
        if (node instanceof EvmNode evm) {
            evm.uiRefresh = () -> {
                NodeWidget widget = getNodeMap().get(evm);
                if (widget != null) {
                    widget.reloadWidget();
                }
            };
        }
    }

    // ---------- ref 预览的右键平移委托 ----------
    // GraphViewWidget.mouseClicked 在子控件之前拦截右键做框选，预览控件永远收不到右键；
    // 这里在命中 ref 预览控件时优先把整段右键手势（按下/拖动/抬起）委托给它，
    // 跳过画布框选（含 mouseDragged 里无条件置位的 isDraggingArea，否则拖出蓝色框选残影）。
    // 命中检测用画布视图坐标（freeGraphView 的子控件都在视图坐标系）。

    /** 正在平移视角的预览控件（一段右键手势期间非空）。 */
    private @Nullable RefPreviewWidget panningPreview;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && isMouseOverElement(mouseX, mouseY)) {
            var viewMouse = getFreeGraphView().getViewPosition((float) mouseX, (float) mouseY);
            if (getFreeGraphView().getHoverElement(viewMouse.x, viewMouse.y) instanceof RefPreviewWidget preview
                    && preview.isInteractive()) {
                panningPreview = preview;
                preview.beginPanFromGraph();
                return true;
            }
        }
        // 新手势开始：清掉可能残留的委托（如上次 mouseReleased 未到达）
        panningPreview = null;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panningPreview != null && button == 1) {
            // 换算成画布视图坐标增量（同 FreeGraphView.mouseDragged 对子控件的换算），平移与光标 1:1
            float scale = getFreeGraphView().getScale();
            panningPreview.panDragFromGraph(dragX / scale, dragY / scale);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (panningPreview != null) {
            RefPreviewWidget preview = panningPreview;
            panningPreview = null;
            preview.endPanFromGraph();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
//?}

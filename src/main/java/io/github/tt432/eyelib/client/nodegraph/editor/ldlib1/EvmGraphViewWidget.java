//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.DebugPanelWidget;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.GraphViewWidget;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.NodeWidget;
import java.util.ArrayList;

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
    }

    @Override
    public EvmBaseGraph getGraph() {
        return (EvmBaseGraph) super.getGraph();
    }

    @Override
    public void loadGraph() {
        super.loadGraph();
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
}
//?}

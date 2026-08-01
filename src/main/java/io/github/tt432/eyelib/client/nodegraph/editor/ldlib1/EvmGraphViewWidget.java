//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.GraphViewWidget;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.NodeWidget;

/**
 * EVM 画布：在 GraphViewWidget 之上做三件事——
 * <ul>
 *   <li>放行 EVM 节点分组前缀（{@link EvmNodeRegistration#GROUP_PREFIX}）；</li>
 *   <li>禁用每 tick 的图执行（{@code setProcessor(null)}——编辑器只编辑不运行，
 *       避免 TriggerProcessor 空转）；</li>
 *   <li>给 {@link EvmNode} 装配 {@code uiRefresh} 钩子（选项变更 → 动态端口重算后
 *       重排节点 widget；graphprocessor 的 onPortsUpdated 在 UI 层无监听者，须手动刷新）。</li>
 * </ul>
 */
public class EvmGraphViewWidget extends GraphViewWidget {
    public EvmGraphViewWidget(EvmBaseGraph graph, int x, int y, int width, int height) {
        super(graph, x, y, width, height, groups -> groups.add(EvmNodeRegistration.GROUP_PREFIX));
        setProcessor(null);
    }

    @Override
    public EvmBaseGraph getGraph() {
        return (EvmBaseGraph) super.getGraph();
    }

    @Override
    public void loadGraph() {
        super.loadGraph();
        wireRefreshHooks();
    }

    @Override
    public void addNode(BaseNode node) {
        super.addNode(node);
        wireRefreshHook(node);
    }

    @Override
    public void pasteTo(double mouseX, double mouseY) {
        super.pasteTo(mouseX, mouseY);
        wireRefreshHooks();
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

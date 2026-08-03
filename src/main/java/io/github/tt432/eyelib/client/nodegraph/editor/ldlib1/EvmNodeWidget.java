//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.GraphViewWidget;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.NodeWidget;

/**
 * EVM 节点 widget：在 LDLib 重建（reloadWidget）后把行内编辑器嵌回端口行
 * （{@link EvmInlinePortFields#embed}）。任何触发 reloadWidget 的路径
 * （加载、连线变更、选项变更、粘贴）都会经本类重新嵌入。
 */
public class EvmNodeWidget extends NodeWidget {
    public EvmNodeWidget(GraphViewWidget graphView, BaseNode node) {
        super(graphView, node);
    }

    @Override
    public void reloadWidget() {
        super.reloadWidget();
        if (getNode() instanceof EvmNode evm) {
            EvmInlinePortFields.embed(this, evm);
        }
    }
}
//?}

//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseGraph;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.widget.NodePortWidget;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.bridge.ui.UiPort;
import io.github.tt432.eyelib.nodegraph.PortDef;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 拖线落空自动连接菜单：从端口拖线松手在画布空白处时，按可连接性过滤出可创建并
 * 自动连线的节点类型（例：从 COLOR 输入拖出 → 只列有 COLOR 输出的节点），点选后
 * 在松手位置创建节点并连线。
 *
 * <p>可连接性判定与画布成边完全一致（{@link BaseGraph#areTypesConnectable} +
 * {@link EvmLinks} 注册的类型适配器），筛选结果即真实可连；候选端口取节点类型的
 * 缺省端口布局（动态端口节点如 query.call 按缺省选项计）。
 */
final class EvmPortConnectMenu {
    private static final int DIALOG_WIDTH = 150;
    private static final int ROW_HEIGHT = 12;
    private static final int MAX_ROWS = 12;

    private EvmPortConnectMenu() {
    }

    /** 可连接的候选：节点类型 + 其上的目标端口（每类型取第一个可连端口）。 */
    private record Candidate(NodeType type, PortDef port) {
    }

    /**
     * 打开菜单。mouseX/mouseY 为 GUI 屏幕坐标。
     *
     * @param view   画布
     * @param source 拖线起始端口（super.mouseReleased 清 clickedPort 之前捕获）
     */
    static void open(EvmGraphViewWidget view, NodePortWidget source, double mouseX, double mouseY) {
        Class<?> sourceClass = source.port.portData.displayType;
        if (sourceClass == null) {
            return;
        }
        boolean sourceIsInput = source.isInput;
        var resolver = Ldlib1EditorSession.currentResolver();
        List<Candidate> candidates = new ArrayList<>();
        for (NodeType type : NodeTypes.all()) {
            NodeInstance defaultView = new NodeInstance("", type.id(), 0, 0, Map.of(), Map.of());
            List<PortDef> ports = sourceIsInput
                    ? type.outputsOf(defaultView, resolver)
                    : type.inputsOf(defaultView, resolver);
            for (PortDef port : ports) {
                // areTypesConnectable(from=输出类型, to=输入类型)，与 NodePortWidget 成边判定同序
                boolean connectable = sourceIsInput
                        ? BaseGraph.areTypesConnectable(EvmLinks.toClass(port.type()), sourceClass)
                        : BaseGraph.areTypesConnectable(sourceClass, EvmLinks.toClass(port.type()));
                if (connectable) {
                    candidates.add(new Candidate(type, port));
                    break;
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        // 弹窗宿主：画布控件不经 ModularUI.setGui 挂载（gui == null），沿 parent 链爬根组
        WidgetGroup root = null;
        for (Widget w = view; w != null; w = w.getParent()) {
            if (w instanceof WidgetGroup group) {
                root = group;
            }
        }
        if (root == null) {
            return;
        }
        int rows = Math.min(candidates.size(), MAX_ROWS);
        int dialogHeight = rows * ROW_HEIGHT + 8;
        int x = Math.max(0, Math.min((int) mouseX, UiPort.guiScaledWidth() - DIALOG_WIDTH));
        int y = Math.max(0, Math.min((int) mouseY, UiPort.guiScaledHeight() - dialogHeight));
        DialogWidget dialog = new DialogWidget(x, y, DIALOG_WIDTH, dialogHeight);
        dialog.setClickClose(true);
        dialog.setBackground(new GuiTextureGroup(
                ColorPattern.BLACK.rectTexture(), ColorPattern.T_WHITE.borderTexture(-1)));
        DraggableScrollableWidgetGroup list = new DraggableScrollableWidgetGroup(
                4, 4, DIALOG_WIDTH - 8, rows * ROW_HEIGHT);
        list.setYScrollBarWidth(4);
        dialog.addWidget(list);
        for (int i = 0; i < candidates.size(); i++) {
            Candidate candidate = candidates.get(i);
            String label = candidate.type().id() + "  (" + candidate.port().id() + ")";
            list.addWidget(new ButtonWidget(0, i * ROW_HEIGHT, DIALOG_WIDTH - 16, ROW_HEIGHT,
                    new TextTexture(label).setWidth(DIALOG_WIDTH - 16),
                    clickData -> choose(view, source, candidate, mouseX, mouseY, dialog)));
        }
        root.addWidget(dialog);
    }

    /** 点选候选：在松手位置建节点并连线（addEdge(in, out)，与 NodePortWidget 成边同序）。 */
    /** 点选候选：在松手位置建节点并连线（addEdge(in, out)，与 NodePortWidget 成边同序）。 */
    private static void choose(EvmGraphViewWidget view, NodePortWidget source, Candidate candidate,
                               double mouseX, double mouseY, DialogWidget dialog) {
        dialog.close();
        EvmNode node = new EvmNode(candidate.type().id(), Ldlib1EditorSession.currentResolver());
        view.addNode(node, (int) mouseX, (int) mouseY);
        // 源是输入 → 目标在新节点的输出侧，反之亦然
        NodePort target = findPort(node, candidate.port().id(), source.isInput);
        if (target != null) {
            if (source.isInput) {
                view.addEdge(source.port, target);
            } else {
                view.addEdge(target, source.port);
            }
        }
        var widget = view.getNodeMap().get(node);
        if (widget != null) {
            widget.reloadWidget();
        }
        source.getNodeWidget().reloadWidget();
    }

    /** 在新建节点上按域端口 id 找 graphprocessor 端口（PortData.identifier = PortDef.id）。 */
    private static @Nullable NodePort findPort(BaseNode node, String portId, boolean output) {
        var container = output ? node.outputPorts : node.inputPorts;
        for (NodePort port : container) {
            if (portId.equals(port.portData.identifier)) {
                return port;
            }
        }
        return null;
    }
}
//?}

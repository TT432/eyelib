//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1;

import com.google.gson.JsonPrimitive;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib1.EvmNode;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib1.Ldlib1EditorSession;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphVariableOps;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.VariableDecl;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * 变量面板（规格 §3.2，ldlib1 自建形态；UE My Blueprint 风）：列出当前图黑板变量
 * （名称 + 类型 + 默认值），支持新增/删除/重命名/改类型——全部改写经宿主回调走
 * {@link GraphVariableOps} 纯函数（重命名级联改写图内 variable 节点），改完由宿主刷新画布模型。
 *
 * <p>每行 {@code setDraggingProvider} 拖到画布 → 落点创建 {@code variable} 节点
 * （name 绑定该变量；{@code NodePanelWidget}/{@code ParameterPanelWidget} 先例，
 * 画布 {@code GraphViewWidget} 已接受 BaseNode drop）。本面板取代 LDLib 内建
 * {@code ParameterPanelWidget}（只读无增删，编辑器已将其移除）。
 *
 * <p>默认收起由编辑器根容器控制（visible/active 开关）；本面板只负责内容。
 */
public final class VariablesPanel extends WidgetGroup {
    /** 变量面板宿主回调（编辑器根实现）：面板不直接触碰画布与库，全部经此出入。 */
    public interface Host {
        /** 当前编辑图的黑板变量声明。 */
        List<VariableDecl> variables();

        /** 应用一次 {@link GraphVariableOps} 改写：落库 + 刷新画布模型 + 刷新本面板。 */
        void applyVariableChange(Function<GraphData, GraphData> op);

        /** 模态对话框宿主（编辑器根；面板自身太小，对话框要罩全屏）。 */
        WidgetGroup dialogParent();

        /** 编辑器内 toast 提示。 */
        void toast(String message);
    }

    /** 改类型循环顺序（只放面板可表达的值类型）。 */
    private static final List<PortType> CYCLABLE_TYPES =
            List.of(PortType.FLOAT, PortType.INT, PortType.BOOL, PortType.STRING);

    private final Host host;
    private final DraggableScrollableWidgetGroup rows;

    public VariablesPanel(int x, int y, int width, int height, Host host) {
        super(x, y, width, height);
        this.host = host;
        // 先标记客户端 widget：子 widget 在 addWidget 时继承
        setClientSideWidget();
        setBackground(new ColorRectTexture(0xF0141414));

        int inner = width - 8;
        addWidget(new LabelWidget(4, 6, "变量"));
        addWidget(new ButtonWidget(width - 48, 4, 44, 13, new TextTexture("新增"), cd -> addVariable()));

        rows = new DraggableScrollableWidgetGroup(4, 22, inner, height - 26);
        rows.setYScrollBarWidth(4).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(2));
        addWidget(rows);
        refreshRows();
    }

    /** 重建变量行（变量增删/改名/改类型、潜入/返回切换图后由宿主调用）。 */
    public void refreshRows() {
        rows.clearAllWidgets();
        int rowWidth = rows.getSizeWidth() - 6;
        int y = 0;
        for (VariableDecl decl : host.variables()) {
            rows.addWidget(buildRow(decl, rowWidth, y));
            y += 16;
        }
        rows.computeMax();
    }

    private WidgetGroup buildRow(VariableDecl decl, int width, int y) {
        WidgetGroup row = new WidgetGroup(0, y, width, 14);
        // 拖拽/显示区（与按钮平级，避免子按钮吃不到点击；NodePanelWidget 同构）
        WidgetGroup dragArea = new WidgetGroup(0, 0, width - 70, 14);
        dragArea.setBackground(ColorPattern.GRAY.rectTexture().setRadius(5));
        String label = decl.name() + " : " + typeLabel(decl.type())
                + decl.defaultValue().map(v -> " = " + v).orElse("");
        dragArea.addWidget(new ImageWidget(3, 1, width - 76, 12,
                new TextTexture(label).setWidth(width - 76).setType(TextTexture.TextType.LEFT_ROLL)));
        dragArea.setDraggingProvider(() -> createVariableNode(decl),
                (node, pos) -> new TextTexture("variable." + decl.name()));
        dragArea.setHoverTooltips("拖到画布创建 variable 节点");
        row.addWidget(dragArea);
        row.addWidget(new ButtonWidget(width - 68, 1, 16, 12, new TextTexture("名"),
                cd -> renameVariable(decl)).setHoverTooltips("重命名（级联改写图内 variable 节点）"));
        row.addWidget(new ButtonWidget(width - 50, 1, 34, 12, new TextTexture(typeLabel(decl.type())),
                cd -> cycleType(decl)).setHoverTooltips("点击切换类型"));
        row.addWidget(new ButtonWidget(width - 14, 1, 14, 12, new TextTexture("删"),
                cd -> removeVariable(decl)).setHoverTooltips("删除变量（variable 节点保留，验证器报未声明）"));
        return row;
    }

    // ---------- 变量操作（一律经 GraphVariableOps） ----------

    private void addVariable() {
        DialogWidget.showStringEditorDialog(host.dialogParent(), "新增变量（不带 variable. 根）", "foo",
                VariablesPanel::validName, name -> {
                    if (name == null) return;
                    if (exists(name)) {
                        host.toast("变量 " + name + " 已存在");
                        return;
                    }
                    host.applyVariableChange(g -> GraphVariableOps.upsert(g, VariableDecl.of(name, PortType.FLOAT)));
                });
    }

    private void renameVariable(VariableDecl decl) {
        DialogWidget.showStringEditorDialog(host.dialogParent(), "重命名变量 " + decl.name(), decl.name(),
                VariablesPanel::validName, name -> {
                    if (name == null || name.equals(decl.name())) return;
                    if (exists(name)) {
                        host.toast("变量 " + name + " 已存在");
                        return;
                    }
                    host.applyVariableChange(g -> GraphVariableOps.rename(g, decl.name(), name));
                });
    }

    private void removeVariable(VariableDecl decl) {
        host.applyVariableChange(g -> GraphVariableOps.remove(g, decl.name()));
    }

    private void cycleType(VariableDecl decl) {
        int index = CYCLABLE_TYPES.indexOf(decl.type());
        PortType next = CYCLABLE_TYPES.get((index + 1) % CYCLABLE_TYPES.size());
        host.applyVariableChange(g -> GraphVariableOps.retype(g, decl.name(), next));
    }

    private boolean exists(String name) {
        return host.variables().stream().anyMatch(v -> v.name().equals(name));
    }

    /** 拖拽产物：type=variable 的 EVM 节点，name 选项绑定该变量（不带根）。 */
    private EvmNode createVariableNode(VariableDecl decl) {
        EvmNode node = new EvmNode(NodeTypes.VARIABLE.id(), Ldlib1EditorSession.currentResolver());
        node.options.put("name", new JsonPrimitive(decl.name()));
        return node;
    }

    private static String typeLabel(PortType type) {
        return switch (type) {
            case FLOAT -> "float";
            case INT -> "int";
            case BOOL -> "bool";
            case STRING -> "string";
            default -> type.name().toLowerCase(Locale.ROOT);
        };
    }

    private static boolean validName(String name) {
        return name != null && name.matches("[a-zA-Z0-9_.]+");
    }
}
//?}

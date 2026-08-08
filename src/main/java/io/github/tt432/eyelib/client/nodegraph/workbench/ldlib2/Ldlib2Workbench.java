package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.nodegraph.workbench.NodeDebugOverlayModel;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;

/**
 * LDLib2 节点图工作台编排器（规格 §3 版本策略的 ldlib2 薄壳）：
 * 把 {@link GraphEditorView} 包进 顶部工具条 + 左资产侧栏 + 右侧标签页（变量表/调试互斥） 的容器，
 * 并持有本编辑器会话唯一的 {@link NodeDebugOverlayModel}（画布徽标模型）。
 * 左下角另挂诊断浮动面板（{@link DiagnosticsPanel}，规格 nodegraph-declaration-wiring §4.3）。
 *
 * <p>接线约定（{@code Ldlib2NodegraphEditor.open}）：
 * 构造 {@link #create}（附加徽标 overlay 到 root 图视图、初始 {@code updateGraph}）→
 * {@code loadGraph} 的保存回调里把翻译回的 {@link GraphLibrary} 传给
 * {@link #onPersisted}（徽标表达式随保存重发射）。
 */
public final class Ldlib2Workbench {
    private final UIElement root;
    private final NodeDebugOverlayModel overlayModel = new NodeDebugOverlayModel();
    private final AssetInspectorPanel assetPanel;
    private final DebugPanel debugPanel;
    private final VariablesPanel variablesPanel;
    private final com.lowdragmc.lowdraglib2.gui.ui.elements.Button variablesTabButton;
    private final com.lowdragmc.lowdraglib2.gui.ui.elements.Button debugTabButton;

    private Ldlib2Workbench(GraphLibrary initialLibrary, GraphEditorView editorView,
                            Runnable onNormalize) {
        assetPanel = new AssetInspectorPanel();
        debugPanel = new DebugPanel(overlayModel::setTarget);
        variablesPanel = new VariablesPanel(editorView);
        // 右侧标签页：变量表（默认）/调试 互斥切换，标签条常显不受页显隐影响
        debugPanel.setDisplay(false);
        // root 先建：工具条 lambda 捕获 final 字段 root（definite assignment）
        root = new UIElement()
                .layout(layout -> layout.widthPercent(100).heightPercent(100));
        WorkbenchToolbar toolbar = new WorkbenchToolbar(
                () -> ImportDialogs.openImportMenu(root),
                () -> assetPanel.setDisplay(!assetPanel.isDisplayed()),
                onNormalize);

        // blackboard 与变量表合并：藏掉 LDLib2 内建黑板面板，变量 UI 统一走 VariablesPanel
        hideBuiltinBlackboard(editorView);

        // 行内剩余空间全给编辑器：grow=1 + basis=0（widthPercent(100) 会把固定宽侧栏挤出版面）
        editorView.layout(layout -> layout.flex(1).flexBasisPercent(0).minWidth(0).heightPercent(100));

        UIElement tabBar = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .height(14)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(2));
        variablesTabButton = tabButton("变量", true, () -> selectTab(true));
        debugTabButton = tabButton("调试", false, () -> selectTab(false));
        tabBar.addChildren(variablesTabButton, debugTabButton);
        UIElement tabBody = new UIElement()
                .layout(layout -> layout.flex(1).flexDirection(FlexDirection.ROW));
        tabBody.addChildren(variablesPanel, debugPanel);
        UIElement rightTabs = new UIElement()
                .layout(layout -> layout.heightPercent(100).flexDirection(FlexDirection.COLUMN));
        rightTabs.addChildren(tabBar, tabBody);

        UIElement contentRow = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .flex(1)
                        .flexDirection(FlexDirection.ROW));
        contentRow.addChildren(assetPanel, editorView, rightTabs);
        root.addChildren(toolbar, contentRow);

        BadgeOverlay.attach(editorView.graphView, overlayModel);
        overlayModel.updateGraph(initialLibrary, initialLibrary.main());

        //? if legacy {
        // LDLib2 2.2.27（1.20.1 移植版）的 GraphView 未处理 EXECUTE_COMMAND(SAVE)
        // （上游 2.2.28+ 才加，转发 editorView.notifySaved），在 1.20.1 上补齐 Ctrl+S。
        // 注意不可下沉到其他版本：2.2.32 内建同义监听器，双挂会重复保存。
        editorView.graphView.addEventListener(
                com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.EXECUTE_COMMAND, event -> {
                    if (com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents.SAVE.equals(event.command)) {
                        editorView.notifySaved();
                    }
                });
        //?}

        // 诊断浮动面板（规格 D3）：绝对定位左下角，挂在 root 上不被侧栏折叠影响
        root.addChild(new DiagnosticsPanel(editorView));
    }

    /**
     * 组装工作台；{@code editorView} 的 loadGraph 由调用方随后完成。
     *
     * @param onNormalize 「规范化」按钮动作（规格 D4；由编辑器侧提供——持久化当前画布后改写重开）
     */
    public static Ldlib2Workbench create(GraphLibrary initialLibrary, GraphEditorView editorView,
                                         Runnable onNormalize) {
        return new Ldlib2Workbench(initialLibrary, editorView, onNormalize);
    }

    /** 根元素（交给 {@code UI.of(...)}）。 */
    public UIElement root() {
        return root;
    }

    /** 标签页切换：变量表/调试互斥显示，活动标签高亮。 */
    private void selectTab(boolean variables) {
        variablesPanel.setDisplay(variables);
        debugPanel.setDisplay(!variables);
        variablesTabButton.buttonStyle(style -> style.baseTexture(
                variables ? com.lowdragmc.lowdraglib2.gui.ColorPattern.T_WHITE.rectTexture()
                        : com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY));
        debugTabButton.buttonStyle(style -> style.baseTexture(
                !variables ? com.lowdragmc.lowdraglib2.gui.ColorPattern.T_WHITE.rectTexture()
                        : com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY));
    }

    /** 标签按钮：小字、活动态半透明白底。 */
    private static com.lowdragmc.lowdraglib2.gui.ui.elements.Button tabButton(
            String text, boolean active, Runnable onClick) {
        com.lowdragmc.lowdraglib2.gui.ui.elements.Button button =
                new com.lowdragmc.lowdraglib2.gui.ui.elements.Button();
        button.buttonStyle(style -> style.baseTexture(active
                        ? com.lowdragmc.lowdraglib2.gui.ColorPattern.T_WHITE.rectTexture()
                        : com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY))
                .setOnClick(event -> onClick.run())
                .setText(net.minecraft.network.chat.Component.literal(text))
                .textStyle(style -> style.fontSize(9))
                .layout(layout -> layout.width(28).heightPercent(100));
        return button;
    }

    /** 藏掉内建黑板：沿 blackboard 的父链找到承载它的 GraphPanel 整体隐藏。 */
    private static void hideBuiltinBlackboard(GraphEditorView editorView) {
        UIElement element = editorView.graphView.blackboard;
        while (element != null && !(element instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphPanel)) {
            element = element.getParent();
        }
        if (element != null) {
            element.setDisplay(false);
        }
    }

    /** 保存回调点：翻译回的库灌入徽标模型（引用比较，COW 文档变了才重发射）。 */
    public void onPersisted(GraphLibrary library) {
        overlayModel.updateGraph(library, library.main());
    }
}
//?}

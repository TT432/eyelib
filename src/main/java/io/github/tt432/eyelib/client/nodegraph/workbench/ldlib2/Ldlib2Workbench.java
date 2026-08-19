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
    /** GraphView.header 与 WorkbenchToolbar 统一的行高（px）。 */
    private static final int CHROME_ROW_HEIGHT = 22;

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
                onNormalize,
                () -> AutoLayoutApplier.apply(editorView));

        // blackboard 与变量表合并：藏掉 LDLib2 内建黑板面板，变量 UI 统一走 VariablesPanel
        hideBuiltinBlackboard(editorView);
        // saveButton 由 GraphEditorView 构造期后挂进 header（styledGraphView 走不到），同样拉满行高
        editorView.saveButton.layout(layout -> layout.heightPercent(100));

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
        MissingRefOverlay.attach(editorView.graphView);
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

    /**
     * GraphView 工厂（{@code new GraphEditorView(Supplier)} 用；root 与每次子图潜入的新视图都经此）：
     * 内建 header（Save/Undo/Redo 行）默认高 16/padding 1，与 WorkbenchToolbar（22/padding 2）
     * 行高不一、按钮基线错开（用户实机截图指为「错位」）；统一为 22/padding 2。
     */
    public static com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView styledGraphView() {
        var view = new com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView();
        view.header.layout(layout -> layout.height(CHROME_ROW_HEIGHT).paddingAll(2));
        // 内建按钮（Undo/Redo/snap/fit）默认固定高 14，在 22 行高里偏上；统一拉满行内容高
        stretchHeaderButtons(view.header);
        // 画布不透明兜底：2.2.34 dock 化后 GraphPanel 不再铺满画布，而编辑器 UI 不加载样式表
        // （mc.lss 不在场），canvas 无背景会透出世界。代码默认色与 mc.lss 同色（#191919），
        // defaultPipeline 可被样式表覆盖。
        com.lowdragmc.lowdraglib2.gui.ui.Style.defaultPipeline(view.canvas.getStyle(),
                style -> style.backgroundTexture(
                        new com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture(0xFF191919)));
        // BLOCK 级别连线不绘制（WireElement 对 BLOCK 直接跳过），缩略全景时图结构不可读。
        // 阈值压 0 = 永不触发 BLOCK（pixelScale 恒 > 0），缩到最小保持 SIMPLIFIED：
        // 节点平块 + 标题条 + 连线。style 机制（用户指定），root 与子图潜入视图统一生效。
        // 仅 <26.1：LDLib2 26.1.2.33 尚无 lodBlockPixelScale style API，该节点保留上游行为。
        //? if <26.1 {
        com.lowdragmc.lowdraglib2.gui.ui.Style.defaultPipeline(view.graphView.getGraphViewStyle(),
                style -> style.lodBlockPixelScale(0f));
        //?}
        return view;
    }

    /** header 后代里的 Button/Toggle 高度拉满（saveButton 由 GraphEditorView 后挂，见 create 调用点）。 */
    private static void stretchHeaderButtons(UIElement element) {
        for (UIElement child : element.getChildren()) {
            if (child instanceof com.lowdragmc.lowdraglib2.gui.ui.elements.Button
                    || child instanceof com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle) {
                child.layout(layout -> layout.heightPercent(100));
            }
            stretchHeaderButtons(child);
        }
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

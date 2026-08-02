package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if !legacy {
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.nodegraph.workbench.NodeDebugOverlayModel;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;

/**
 * LDLib2 节点图工作台编排器（规格 §3 版本策略的 ldlib2 薄壳）：
 * 把 {@link GraphEditorView} 包进 顶部工具条 + 左资产侧栏 + 右调试侧栏 的容器，
 * 并持有本编辑器会话唯一的 {@link NodeDebugOverlayModel}（画布徽标模型）。
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

    private Ldlib2Workbench(GraphLibrary initialLibrary, GraphEditorView editorView) {
        assetPanel = new AssetInspectorPanel();
        debugPanel = new DebugPanel(overlayModel::setTarget);
        // root 先建：工具条 lambda 捕获 final 字段 root（definite assignment）
        root = new UIElement()
                .layout(layout -> layout.widthPercent(100).heightPercent(100));
        WorkbenchToolbar toolbar = new WorkbenchToolbar(
                () -> ImportDialogs.openImportMenu(root),
                () -> assetPanel.setDisplay(!assetPanel.isDisplayed()),
                () -> debugPanel.setDisplay(!debugPanel.isDisplayed()));

        // 行内剩余空间全给编辑器：grow=1 + basis=0（widthPercent(100) 会把固定宽侧栏挤出版面）
        editorView.layout(layout -> layout.flex(1).flexBasisPercent(0).minWidth(0).heightPercent(100));

        UIElement contentRow = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .flex(1)
                        .flexDirection(FlexDirection.ROW));
        contentRow.addChildren(assetPanel, editorView, debugPanel);
        root.addChildren(toolbar, contentRow);

        BadgeOverlay.attach(editorView.graphView, overlayModel);
        overlayModel.updateGraph(initialLibrary, initialLibrary.main());
    }

    /** 组装工作台；{@code editorView} 的 loadGraph 由调用方随后完成。 */
    public static Ldlib2Workbench create(GraphLibrary initialLibrary, GraphEditorView editorView) {
        return new Ldlib2Workbench(initialLibrary, editorView);
    }

    /** 根元素（交给 {@code UI.of(...)}）。 */
    public UIElement root() {
        return root;
    }

    /** 保存回调点：翻译回的库灌入徽标模型（引用比较，COW 文档变了才重发射）。 */
    public void onPersisted(GraphLibrary library) {
        overlayModel.updateGraph(library, library.main());
    }
}
//?}

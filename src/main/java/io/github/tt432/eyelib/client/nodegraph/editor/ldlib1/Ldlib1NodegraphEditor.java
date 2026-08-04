//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import io.github.tt432.eyelib.client.nodegraph.EprojectService;
import io.github.tt432.eyelib.client.nodegraph.GraphLibraryManager;
import io.github.tt432.eyelib.client.nodegraph.NodegraphBuildService;
import io.github.tt432.eyelib.client.nodegraph.workbench.NodeDebugOverlayModel;
import io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1.AssetInspectorPanel;
import io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1.DebugSidebarPanel;
import io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1.ImportDialog;
import io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1.VariablesPanel;
import io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1.WorkbenchGraphViewWidget;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.GraphValidator;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.ShortNameOps;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * LDLib 1.x（1.20.1 graphprocessor）节点图编辑器入口（{@code NodegraphGate} 反射调用点，
 * 签名不得偏离）。
 *
 * <p>宿主：外层 {@link EditorRoot}（保存/构建/潜入/返回按钮 + 面包屑栏）内嵌
 * {@link EvmGraphViewWidget}，经 ModularUI → ModularUIGuiContainer → setScreen 打开
 * （参照 LDLib ClientCommands 的客户端打开路径）。
 */
public final class Ldlib1NodegraphEditor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ldlib1NodegraphEditor.class);

    private Ldlib1NodegraphEditor() {
    }

    /**
     * 打开节点图编辑器。
     *
     * @param libraryName 图文档库名；null/空 = 新建 client_entity 库（主图 root + entity.root 节点）
     */
    public static void open(@Nullable String libraryName) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            LOGGER.warn("[nodegraph] cannot open editor without a client player");
            return;
        }
        EvmNodeRegistration.ensureRegistered();

        String name = libraryName == null || libraryName.isEmpty() ? null : libraryName;
        GraphLibrary library = name == null ? null : GraphLibraryManager.INSTANCE.get(name);
        if (library == null) {
            name = name == null ? "untitled" : name;
            library = newClientEntityLibrary();
            if (libraryName != null && !libraryName.isEmpty()) {
                chat("[nodegraph] library '" + name + "' not found, created a new client_entity library");
            }
        }

        Ldlib1EditorSession.enter(library, Optional.empty());
        int width = io.github.tt432.eyelib.bridge.ui.UiPort.guiScaledWidth();
        int height = io.github.tt432.eyelib.bridge.ui.UiPort.guiScaledHeight();
        EditorRoot root = new EditorRoot(name, library, width, height);

        ModularUI modularUI = new ModularUI(root, IUIHolder.EMPTY, player);
        modularUI.initWidgets();
        ModularUIGuiContainer screen = new ModularUIGuiContainer(modularUI, player.containerMenu.containerId);
        minecraft.setScreen(screen);
        player.containerMenu = screen.getMenu();
    }

    private static GraphLibrary newClientEntityLibrary() {
        NodeInstance root = NodeInstance.of("root", NodeTypes.ENTITY_ROOT.id(), 60, 60);
        GraphData main = new GraphData(List.of(root), List.of(), List.of(), List.of(), List.of(), Optional.empty());
        return new GraphLibrary(GraphLibrary.CURRENT_FORMAT_VERSION, GraphKind.CLIENT_ENTITY,
                "root", Map.of("root", main));
    }

    /** 聊天栏反馈（工作台面板共用）。 */
    public static void chat(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui != null) {
            minecraft.gui.getChat().addMessage(Component.literal(message));
        }
    }

    /** 诊断双通道反馈：聊天栏摘要 + 日志（工作台面板共用）。 */
    public static void reportDiagnostics(String action, List<Diagnostic> diagnostics) {
        long errors = diagnostics.stream().filter(d -> d.severity() == Diagnostic.Severity.ERROR).count();
        long warnings = diagnostics.size() - errors;
        chat("[nodegraph] " + action + ": " + errors + " error(s), " + warnings + " warning(s)");
        diagnostics.stream().limit(10).forEach(d -> chat("  " + d));
        if (diagnostics.size() > 10) {
            chat("  ... and " + (diagnostics.size() - 10) + " more (see log)");
        }
        diagnostics.forEach(d -> LOGGER.info("[nodegraph] {} {}", action, d));
    }

    /** 库键末段（新建项目名：规格 §2.3，键形如 {project}/{libId} 或 eyelib:nodegraph/x）。 */
    private static String lastKeySegment(String key) {
        int cut = Math.max(key.lastIndexOf('/'), Math.max(key.lastIndexOf('.'), key.lastIndexOf(':')));
        return cut >= 0 ? key.substring(cut + 1) : key;
    }

    /**
     * 编辑器根容器：顶栏（保存/构建/潜入/返回 + 面包屑）+ 画布。
     *
     * <p>子图潜入（规格 D5 在 1.20.1 的实现）：graphprocessor 无子图概念，
     * 「潜入」= 同步当前画布回库 → 面包屑压栈 → 用子图翻译产物重建 GraphViewWidget
     * （{@code GraphViewWidget.graph} 是 final，无法换图，只能整棵重建）。
     * 分组（规格 D6）：placemat/便签不在画布显示，回写时从文档原样保留；
     * 黑板变量分组平铺为全路径显示名。
     */
    static final class EditorRoot extends WidgetGroup implements VariablesPanel.Host {
        private static final int BAR_HEIGHT = 18;
        /** 右侧侧栏（资产检查器/调试/变量）宽度。 */
        private static final int SIDE_WIDTH = 210;

        /** 当前库键（另存为项目后切换为新键，见 {@link #save()}）。 */
        private String libraryName;
        private GraphLibrary library;
        private final int panelWidth;
        private final int panelHeight;
        /** 面包屑栈：底 = 主图名，顶 = 当前图名。 */
        private final Deque<String> breadcrumbs = new ArrayDeque<>();
        /** 画布节点值徽标模型（规格 §W3；编辑器只负责绘制）。 */
        private final NodeDebugOverlayModel overlay = new NodeDebugOverlayModel();
        private final AssetInspectorPanel assetPanel;
        private final DebugSidebarPanel debugPanel;
        private final VariablesPanel variablesPanel;
        private final LabelWidget toastLabel;
        private @Nullable EvmGraphViewWidget view;
        /** toast 文本与过期 tick（编辑器内轻量提示；空串 = 不显示）。 */
        private String toastText = "";
        private long toastExpireTick;

        EditorRoot(String libraryName, GraphLibrary library, int width, int height) {
            super(0, 0, width, height);
            this.libraryName = libraryName;
            this.library = library;
            this.panelWidth = width;
            this.panelHeight = height - BAR_HEIGHT;
            breadcrumbs.addLast(library.main());

            // 侧栏先建后挂：默认收起，不遮挡画布；三栏互斥（同位置叠放）。
            // 必须先于按钮创建——按钮 lambda 捕获这两个 final 字段（definite assignment）。
            assetPanel = new AssetInspectorPanel(width - SIDE_WIDTH, BAR_HEIGHT, SIDE_WIDTH, panelHeight);
            debugPanel = new DebugSidebarPanel(width - SIDE_WIDTH, BAR_HEIGHT, SIDE_WIDTH, panelHeight, overlay);
            variablesPanel = new VariablesPanel(width - SIDE_WIDTH, BAR_HEIGHT, SIDE_WIDTH, panelHeight, this);
            assetPanel.setVisible(false);
            assetPanel.setActive(false);
            debugPanel.setVisible(false);
            debugPanel.setActive(false);
            variablesPanel.setVisible(false);
            variablesPanel.setActive(false);
            // toast 底部提示（在 rebuildView 里随侧栏一起抬到画布之上）
            toastLabel = new LabelWidget(8, height - 16, () -> toastText);

            addWidget(new ButtonWidget(4, 3, 40, 12, new TextTexture("保存"), cd -> save()));
            addWidget(new ButtonWidget(48, 3, 40, 12, new TextTexture("构建"), cd -> build()));
            addWidget(new ButtonWidget(92, 3, 40, 12, new TextTexture("潜入"), cd -> dive()));
            addWidget(new ButtonWidget(136, 3, 40, 12, new TextTexture("返回"), cd -> surface()));
            addWidget(new ButtonWidget(180, 3, 40, 12, new TextTexture("导入"), cd -> new ImportDialog(this)));
            addWidget(new ButtonWidget(224, 3, 40, 12, new TextTexture("资产"),
                    cd -> togglePanel(assetPanel, debugPanel, variablesPanel)));
            addWidget(new ButtonWidget(268, 3, 40, 12, new TextTexture("调试"),
                    cd -> togglePanel(debugPanel, assetPanel, variablesPanel)));
            addWidget(new ButtonWidget(312, 3, 40, 12, new TextTexture("变量"),
                    cd -> togglePanel(variablesPanel, assetPanel, debugPanel)));
            addWidget(new ButtonWidget(356, 3, 48, 12, new TextTexture("规范化"), cd -> normalizeShortNames()));
            addWidget(new LabelWidget(410, 5, () -> String.join(" / ", breadcrumbs)));

            // rebuildView 会把侧栏抬到画布之上（侧栏先建，重建时保持顶层）
            rebuildView();
        }

        /** 侧栏开关：展开当前栏并收起其余栏（三栏同位置互斥）。 */
        private void togglePanel(WidgetGroup panel, WidgetGroup... others) {
            boolean show = !panel.isVisible();
            panel.setVisible(show);
            panel.setActive(show);
            if (show) {
                for (WidgetGroup other : others) {
                    other.setVisible(false);
                    other.setActive(false);
                }
            }
        }

        private String currentGraphName() {
            return breadcrumbs.peekLast();
        }

        private void rebuildView() {
            rebuildView(false);
        }

        /**
         * 重建画布。
         *
         * @param preserveViewport true = 保留缩放/偏移（变量面板改写后的画布模型刷新；
         *                         重命名级联需重译画布节点，但不应甩用户视口）
         */
        private void rebuildView(boolean preserveViewport) {
            float oldScale = 0;
            float oldXOffset = 0;
            float oldYOffset = 0;
            if (preserveViewport && view != null) {
                oldScale = view.getFreeGraphView().getScale();
                oldXOffset = view.getFreeGraphView().getXOffset();
                oldYOffset = view.getFreeGraphView().getYOffset();
            }
            if (view != null) {
                removeWidget(view);
            }
            EvmBaseGraph graph = Ldlib1GraphTranslator.toGraph(libraryName, currentGraphName(), library);
            view = new WorkbenchGraphViewWidget(overlay, graph, 0, BAR_HEIGHT, panelWidth, panelHeight);
            if (preserveViewport && oldScale > 0) {
                view.getFreeGraphView().setScale(oldScale);
                view.getFreeGraphView().setXOffset(oldXOffset);
                view.getFreeGraphView().setYOffset(oldYOffset);
            } else {
                // LDLib 构造函数末尾会用内建 fit（缩放下限 0.5，大图≈无效）覆盖 loadGraph 里的适配，
                // 必须在构造完成后再调一次我们的 fitToContent（用户报告「导入后没有节点」的根因）
                ((WorkbenchGraphViewWidget) view).fitToContent();
            }
            addWidget(view);
            // 重建后画布是最后挂载的子节点，把侧栏与 toast 重新抬到顶层
            removeWidget(assetPanel);
            removeWidget(debugPanel);
            removeWidget(variablesPanel);
            removeWidget(toastLabel);
            addWidget(assetPanel);
            addWidget(debugPanel);
            addWidget(variablesPanel);
            addWidget(toastLabel);
            // 打开/潜入/返回：同步徽标发射目标图
            overlay.updateGraph(library, currentGraphName());
            // 变量跟随图切换（变量是 GraphData 级，潜入子图后列的是子图变量）
            variablesPanel.refreshRows();
        }

        /** 画布 → 文档：回译当前图并替换库内对应图（就地更新管理器与会话）。 */
        private void syncCanvasToLibrary() {
            if (view == null) return;
            String graphName = currentGraphName();
            GraphData previous = library.graphs().getOrDefault(graphName, GraphData.empty());
            GraphData data = Ldlib1GraphTranslator.toData(view.getGraph(), previous);
            Map<String, GraphData> graphs = new LinkedHashMap<>(library.graphs());
            graphs.put(graphName, data);
            library = new GraphLibrary(library.formatVersion(), library.kind(), library.main(),
                    Map.copyOf(graphs));
            GraphLibraryManager.INSTANCE.put(libraryName, library);
            Ldlib1EditorSession.enter(library, data.graphInterface());
            // 画布变更落库：重发射节点值徽标（规格 §W3 缓存：引用变化才重发射）
            overlay.updateGraph(library, currentGraphName());
        }

        /**
         * 保存（规格 §5 / 契约 3）：画布落库 → 已绑定项目则按来源形态写回；未绑定
         * （资源包/内存导入的库）则以库键末段为项目名新建文件夹形态项目并保存。
         * 结果在编辑器内 toast 反馈。
         */
        private void save() {
            syncCanvasToLibrary();
            if (EprojectService.saveProjectOf(libraryName)) {
                toast("已保存" + EprojectService.projectOf(libraryName)
                        .map(ref -> "项目「" + ref.name() + "」").orElse(""));
            } else {
                EprojectService.ProjectRef project =
                        EprojectService.saveAsProject(libraryName, lastKeySegment(libraryName));
                // 另存后切换到新库键——否则二次保存会又叉出一个新项目
                libraryName = project.libraryKeys().get(0);
                toast("已新建项目「" + project.name() + "」并保存");
            }
            reportDiagnostics("validate", GraphValidator.validate(library));
        }

        /** Ctrl+S 保存（Screen.isSave 是 1.20.2+ API，1.20.1 手写等价判断；先例 GraphViewWidget Ctrl+C）。 */
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (super.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_S && Screen.hasControlDown()
                    && !Screen.hasShiftDown() && !Screen.hasAltDown()) {
                save();
                return true;
            }
            return false;
        }

        // ---------- 编辑器内 toast（规格 §5） ----------

        @Override
        public void toast(String message) {
            toastText = message;
            toastExpireTick = gui == null ? 0 : gui.getTickCount() + 60;
        }

        @Override
        public void updateScreen() {
            super.updateScreen();
            if (!toastText.isEmpty() && gui != null && gui.getTickCount() > toastExpireTick) {
                toastText = "";
            }
        }

        // ---------- VariablesPanel.Host ----------

        @Override
        public List<VariableDecl> variables() {
            GraphData data = library.graphs().get(currentGraphName());
            return data == null ? List.of() : data.variables();
        }

        /**
         * 变量面板改写（规格 §3.2）：画布先落库保证改写基于最新状态 → GraphVariableOps
         * 纯函数改写 → 写回库/会话 → 保视口重建画布（重命名级联的 variable 节点名同步进画布模型）。
         */
        @Override
        public void applyVariableChange(Function<GraphData, GraphData> op) {
            syncCanvasToLibrary();
            String graphName = currentGraphName();
            GraphData previous = library.graphs().getOrDefault(graphName, GraphData.empty());
            GraphData updated = op.apply(previous);
            Map<String, GraphData> graphs = new LinkedHashMap<>(library.graphs());
            graphs.put(graphName, updated);
            library = new GraphLibrary(library.formatVersion(), library.kind(), library.main(),
                    Map.copyOf(graphs));
            GraphLibraryManager.INSTANCE.put(libraryName, library);
            Ldlib1EditorSession.enter(library, updated.graphInterface());
            overlay.updateGraph(library, graphName);
            rebuildView(true);
        }

        @Override
        public WidgetGroup dialogParent() {
            return this;
        }

        private void build() {
            syncCanvasToLibrary();
            NodegraphBuildService.BuildResult result = NodegraphBuildService.build(libraryName, library);
            reportDiagnostics("build", result.diagnostics());
            if (result.injectedId() != null) {
                chat("[nodegraph] injected: " + result.injectedId());
            }
        }

        /**
         * 规范化短名（规格 D4）：同步画布 → 剥全部显式 short_name（派生接管）→ 重建画布。
         * 适用全闭包已入图；同 pack 未导入文档引用旧短名将断（按钮即明示动作）。
         */
        private void normalizeShortNames() {
            syncCanvasToLibrary();
            ShortNameOps.RewriteResult r = ShortNameOps.normalize(library);
            if (r.changed() == 0) {
                chat("[nodegraph] 无显式短名可清除（已是派生状态）");
                return;
            }
            library = r.library();
            GraphLibraryManager.INSTANCE.put(libraryName, library);
            rebuildView();
            chat("[nodegraph] 规范化：已清除 " + r.changed() + " 个显式短名（派生接管）；"
                    + "注意：未导入的同包文档若引用旧短名将失效");
        }

        /** 潜入选中的 subgraph.call 节点（无子图概念的原生画布 → 重建 widget）。 */
        private void dive() {
            if (view == null) return;
            EvmNode call = null;
            for (var node : view.getSelectedNodes()) {
                if (node instanceof EvmNode evm && NodeTypes.SUBGRAPH_CALL.id().equals(evm.nodeTypeId)) {
                    call = evm;
                    break;
                }
            }
            if (call == null) {
                chat("[nodegraph] select a subgraph.call node first");
                return;
            }
            String target = call.optionString("subgraph", "");
            Optional<GraphData> graph = library.graph(target);
            if (target.isEmpty() || graph.isEmpty() || graph.get().graphInterface().isEmpty()) {
                chat("[nodegraph] cannot dive: subgraph '" + target + "' not found in library");
                return;
            }
            syncCanvasToLibrary();
            breadcrumbs.addLast(target);
            Ldlib1EditorSession.enter(library, graph.get().graphInterface());
            rebuildView();
        }

        private void surface() {
            if (breadcrumbs.size() <= 1) {
                chat("[nodegraph] already at main graph");
                return;
            }
            syncCanvasToLibrary();
            breadcrumbs.removeLast();
            GraphData current = library.graphs().get(currentGraphName());
            Ldlib1EditorSession.enter(library,
                    current == null ? Optional.empty() : current.graphInterface());
            rebuildView();
        }
    }
}
//?}

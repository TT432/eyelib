//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.nodegraph.GraphLibraryManager;
import io.github.tt432.eyelib.client.nodegraph.NodegraphBuildService;
import io.github.tt432.eyelib.client.nodegraph.workbench.NodeDebugOverlayModel;
import io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1.AssetInspectorPanel;
import io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1.DebugSidebarPanel;
import io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1.ImportDialog;
import io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1.WorkbenchGraphViewWidget;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.GraphValidator;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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

    /** 落盘：config/eyelib/nodegraph/&lt;name&gt;.json（Gson pretty）。 */
    private static void writeToDisk(String name, GraphLibrary library) {
        Optional<JsonElement> encoded = GraphLibrary.CODEC.encodeStart(JsonOps.INSTANCE, library)
                .resultOrPartial(err -> LOGGER.error("[nodegraph] encode failed for '{}': {}", name, err));
        if (encoded.isEmpty()) {
            chat("[nodegraph] save failed: codec encode error (see log)");
            return;
        }
        try {
            // 与 BedrockAddonAutoLoader 同款版本中性路径（等价 FMLPaths.CONFIGDIR 默认布局）
            Path dir = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config").resolve("eyelib").resolve("nodegraph");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(sanitizeFileName(name) + ".json"), GSON.toJson(encoded.get()));
            chat("[nodegraph] saved to config/eyelib/nodegraph/" + sanitizeFileName(name) + ".json");
        } catch (IOException e) {
            LOGGER.error("[nodegraph] failed to write library '{}'", name, e);
            chat("[nodegraph] save failed: " + e.getMessage());
        }
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
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
    static final class EditorRoot extends WidgetGroup {
        private static final int BAR_HEIGHT = 18;
        /** 右侧侧栏（资产检查器/调试）宽度。 */
        private static final int SIDE_WIDTH = 210;

        private final String libraryName;
        private GraphLibrary library;
        private final int panelWidth;
        private final int panelHeight;
        /** 面包屑栈：底 = 主图名，顶 = 当前图名。 */
        private final Deque<String> breadcrumbs = new ArrayDeque<>();
        /** 画布节点值徽标模型（规格 §W3；编辑器只负责绘制）。 */
        private final NodeDebugOverlayModel overlay = new NodeDebugOverlayModel();
        private final AssetInspectorPanel assetPanel;
        private final DebugSidebarPanel debugPanel;
        private @Nullable EvmGraphViewWidget view;

        EditorRoot(String libraryName, GraphLibrary library, int width, int height) {
            super(0, 0, width, height);
            this.libraryName = libraryName;
            this.library = library;
            this.panelWidth = width;
            this.panelHeight = height - BAR_HEIGHT;
            breadcrumbs.addLast(library.main());

            // 侧栏先建后挂：默认收起，不遮挡画布；两栏互斥（同位置叠放）。
            // 必须先于按钮创建——按钮 lambda 捕获这两个 final 字段（definite assignment）。
            assetPanel = new AssetInspectorPanel(width - SIDE_WIDTH, BAR_HEIGHT, SIDE_WIDTH, panelHeight);
            debugPanel = new DebugSidebarPanel(width - SIDE_WIDTH, BAR_HEIGHT, SIDE_WIDTH, panelHeight, overlay);
            assetPanel.setVisible(false);
            assetPanel.setActive(false);
            debugPanel.setVisible(false);
            debugPanel.setActive(false);

            addWidget(new ButtonWidget(4, 3, 40, 12, new TextTexture("保存"), cd -> save()));
            addWidget(new ButtonWidget(48, 3, 40, 12, new TextTexture("构建"), cd -> build()));
            addWidget(new ButtonWidget(92, 3, 40, 12, new TextTexture("潜入"), cd -> dive()));
            addWidget(new ButtonWidget(136, 3, 40, 12, new TextTexture("返回"), cd -> surface()));
            addWidget(new ButtonWidget(180, 3, 40, 12, new TextTexture("导入"), cd -> new ImportDialog(this)));
            addWidget(new ButtonWidget(224, 3, 40, 12, new TextTexture("资产"), cd -> togglePanel(assetPanel, debugPanel)));
            addWidget(new ButtonWidget(268, 3, 40, 12, new TextTexture("调试"), cd -> togglePanel(debugPanel, assetPanel)));
            addWidget(new LabelWidget(314, 5, () -> String.join(" / ", breadcrumbs)));

            // rebuildView 会把侧栏抬到画布之上（侧栏先建，重建时保持顶层）
            rebuildView();
        }

        /** 侧栏开关：展开当前栏并收起另一栏（两栏同位置互斥）。 */
        private void togglePanel(WidgetGroup panel, WidgetGroup other) {
            boolean show = !panel.isVisible();
            panel.setVisible(show);
            panel.setActive(show);
            if (show) {
                other.setVisible(false);
                other.setActive(false);
            }
        }

        private String currentGraphName() {
            return breadcrumbs.peekLast();
        }

        private void rebuildView() {
            if (view != null) {
                removeWidget(view);
            }
            EvmBaseGraph graph = Ldlib1GraphTranslator.toGraph(libraryName, currentGraphName(), library);
            view = new WorkbenchGraphViewWidget(overlay, graph, 0, BAR_HEIGHT, panelWidth, panelHeight);
            // LDLib 构造函数末尾会用内建 fit（缩放下限 0.5，大图≈无效）覆盖 loadGraph 里的适配，
            // 必须在构造完成后再调一次我们的 fitToContent（用户报告「导入后没有节点」的根因）
            ((WorkbenchGraphViewWidget) view).fitToContent();
            addWidget(view);
            // 重建后画布是最后挂载的子节点，把侧栏重新抬到顶层
            removeWidget(assetPanel);
            removeWidget(debugPanel);
            addWidget(assetPanel);
            addWidget(debugPanel);
            // 打开/潜入/返回：同步徽标发射目标图
            overlay.updateGraph(library, currentGraphName());
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

        private void save() {
            syncCanvasToLibrary();
            writeToDisk(libraryName, library);
            reportDiagnostics("validate", GraphValidator.validate(library));
        }

        private void build() {
            syncCanvasToLibrary();
            NodegraphBuildService.BuildResult result = NodegraphBuildService.build(libraryName, library);
            reportDiagnostics("build", result.diagnostics());
            if (result.injectedId() != null) {
                chat("[nodegraph] injected: " + result.injectedId());
            }
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

package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if !legacy {
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import io.github.tt432.eyelib.client.nodegraph.EprojectService;
import io.github.tt432.eyelib.client.nodegraph.GraphLibraryManager;
import io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2.Ldlib2Workbench;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.GraphValidator;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.ShortNameOps;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LDLib2（1.21.1 / 26.1.2 nodegraphtookit）节点图编辑器入口
 * （{@code NodegraphGate} 反射调用点，签名不得偏离）。
 *
 * <p>宿主：{@link GraphEditorView}（含保存/脏标记/面包屑/黑板/检查器/小地图）
 * 经工作台容器（工具条 + 资产/调试侧栏 + 画布徽标，规格 nodegraph-workbench §W1/W3）→
 * ModularUI → ModularUIScreen → setScreen 打开。保存（Ctrl+S / 保存按钮，内建 SAVE 命令链
 * ModularUI→CommandEvents.SAVE→GraphView→notifySaved）把 GraphModel 翻译回
 * {@link GraphLibrary}，写入 {@link GraphLibraryManager} 后经 {@link EprojectService}
 * 按来源形态写回 eproject 项目（规格 §2.3/§5），同时灌入画布徽标模型。
 */
public final class Ldlib2NodegraphEditor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ldlib2NodegraphEditor.class);

    private Ldlib2NodegraphEditor() {
    }

    /**
     * 打开节点图编辑器。
     *
     * @param libraryName 图文档库名（{@link GraphLibraryManager} 的键）；null/空 = 新建 client_entity 库
     */
    public static void open(@Nullable String libraryName) {
        String name;
        GraphLibrary library;
        if (libraryName == null || libraryName.isEmpty()) {
            name = freshLibraryName();
            library = newClientEntityLibrary();
        } else {
            GraphLibrary existing = GraphLibraryManager.INSTANCE.get(libraryName);
            if (existing == null) {
                LOGGER.warn("[nodegraph] library '{}' not found in GraphLibraryManager", libraryName);
                return;
            }
            name = libraryName;
            library = existing;
        }

        List<Diagnostic> openDiags = new ArrayList<>(GraphValidator.validate(library));
        EvmGraph graph = EvmGraphTranslator.toGraph(library, openDiags);
        EvmDiagnostics.report(openDiags);

        GraphEditorView editorView = new GraphEditorView();
        Ldlib2Workbench workbench = Ldlib2Workbench.create(library, editorView,
                () -> normalizeAndReopen(name, graph));
        editorView.loadGraph(graph, savedTag -> workbench.onPersisted(save(name, graph)));
        // 视口适配内容：导入/打开后用户必须立刻看到节点（导入布局可能离原点很远）
        editorView.graphView.fitGraphChildren(15f);

        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ModularUIScreen(
                new ModularUI(UI.of(workbench.root()), mc.player),
                Component.literal("Nodegraph: " + name)));
    }

    /**
     * 规范化短名（规格 D4）：持久化当前画布 → 剥全部显式 short_name（派生接管）→ 落盘重开。
     * 适用全闭包已入图；同 pack 未导入文档引用旧短名将断（按钮即明示动作）。
     */
    private static void normalizeAndReopen(String name, EvmGraph graph) {
        GraphLibrary persisted = persistToManager(name, graph);
        ShortNameOps.RewriteResult r = ShortNameOps.normalize(persisted);
        if (r.changed() == 0) {
            EvmDiagnostics.info("无显式短名可清除（已是派生状态）");
            return;
        }
        GraphLibraryManager.INSTANCE.put(name, r.library());
        saveViaEproject(name);
        EvmDiagnostics.info("规范化：已清除 " + r.changed() + " 个显式短名（派生接管）；"
                + "注意：未导入的同包文档若引用旧短名将失效");
        open(name);
    }

    /** 保存：翻译入库（见下）+ eproject 写回；返回入库的库（供徽标模型重发射）。 */
    private static GraphLibrary save(String name, EvmGraph graph) {
        GraphLibrary library = persistToManager(name, graph);
        saveViaEproject(name);
        return library;
    }

    /** 翻译入库：GraphModel → GraphLibrary → 验证诊断 → 注册表（不落盘；落盘见 {@link #saveViaEproject}）。 */
    private static GraphLibrary persistToManager(String name, EvmGraph graph) {
        List<Diagnostic> diags = new ArrayList<>();
        GraphLibrary library = EvmGraphTranslator.toLibrary(graph, diags);
        diags.addAll(GraphValidator.validate(library));
        EvmDiagnostics.report(diags);
        GraphLibraryManager.INSTANCE.put(name, library);
        return library;
    }

    /**
     * eproject 写回（规格 §2.3/§5）：库键已绑定项目 → 项目内全部库按来源形态写回；
     * 未绑定（资源包/内存导入/新建库）→ 以库键末段为名就地新建文件夹形态项目。
     */
    private static void saveViaEproject(String libraryKey) {
        Optional<EprojectService.ProjectRef> bound = EprojectService.projectOf(libraryKey);
        if (bound.isPresent()) {
            boolean ok = EprojectService.saveProjectOf(libraryKey);
            EvmDiagnostics.info(ok
                    ? "已保存项目 " + bound.get().name()
                    : "项目保存失败（详见日志）");
            return;
        }
        String projectName = libraryKey.substring(libraryKey.lastIndexOf('/') + 1);
        EprojectService.ProjectRef created = EprojectService.saveAsProject(libraryKey, projectName);
        EvmDiagnostics.info("已新建项目 " + created.name() + " 并保存（" + created.path() + "）");
    }

    /** 新建库的占位名：untitled / untitled_2 / …（取注册表中未占用的第一个）。 */
    private static String freshLibraryName() {
        String name = "untitled";
        int suffix = 2;
        while (GraphLibraryManager.INSTANCE.get(name) != null) {
            name = "untitled_" + suffix++;
        }
        return name;
    }

    /** 新建 client_entity 库：主图 root + 1 个 entity.root 节点实例于原点。 */
    private static GraphLibrary newClientEntityLibrary() {
        GraphData root = new GraphData(
                List.of(NodeInstance.of("entity_root_1", "entity.root", 0, 0)),
                List.of(), List.of(), List.of(), List.of(), Optional.empty());
        return new GraphLibrary(GraphLibrary.CURRENT_FORMAT_VERSION, GraphKind.CLIENT_ENTITY,
                "root", Map.of("root", root));
    }
}
//?}

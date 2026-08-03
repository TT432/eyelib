package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if !legacy {
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.nodegraph.GraphLibraryManager;
import io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2.Ldlib2Workbench;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.GraphValidator;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * ModularUI → ModularUIScreen → setScreen 打开。保存回调把 GraphModel 翻译回
 * {@link GraphLibrary}，写入 {@link GraphLibraryManager} 并落盘
 * {@code config/eyelib/nodegraph/<name>.json}（规格 §3.2、D2），同时灌入画布徽标模型。
 */
public final class Ldlib2NodegraphEditor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ldlib2NodegraphEditor.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
        Ldlib2Workbench workbench = Ldlib2Workbench.create(library, editorView);
        editorView.loadGraph(graph, savedTag -> workbench.onPersisted(persist(name, graph)));
        // 视口适配内容：导入/打开后用户必须立刻看到节点（导入布局可能离原点很远）
        editorView.graphView.fitGraphChildren(15f);

        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ModularUIScreen(
                new ModularUI(UI.of(workbench.root()), mc.player),
                Component.literal("Nodegraph: " + name)));
    }

    /** 保存：GraphModel → GraphLibrary → 验证诊断 → 注册表 + 落盘；返回入库的库（供徽标模型重发射）。 */
    private static GraphLibrary persist(String name, EvmGraph graph) {
        List<Diagnostic> diags = new ArrayList<>();
        GraphLibrary library = EvmGraphTranslator.toLibrary(graph, diags);
        diags.addAll(GraphValidator.validate(library));
        EvmDiagnostics.report(diags);
        GraphLibraryManager.INSTANCE.put(name, library);
        writeToDisk(name, library);
        return library;
    }

    /** 落盘：config/eyelib/nodegraph/&lt;name&gt;.json（Gson pretty print，规格 §3.2）。 */
    private static void writeToDisk(String name, GraphLibrary library) {
        Optional<JsonElement> encoded = GraphLibrary.CODEC.encodeStart(JsonOps.INSTANCE, library)
                .resultOrPartial(err -> LOGGER.error("[nodegraph] encode failed for '{}': {}", name, err));
        if (encoded.isEmpty()) {
            return;
        }
        try {
            Path dir = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("config").resolve("eyelib").resolve("nodegraph");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(sanitizeFileName(name) + ".json"), GSON.toJson(encoded.get()));
            EvmDiagnostics.info("saved to config/eyelib/nodegraph/" + sanitizeFileName(name) + ".json");
        } catch (IOException e) {
            LOGGER.error("[nodegraph] failed to write library '{}'", name, e);
            EvmDiagnostics.info("save failed: " + e.getMessage());
        }
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
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

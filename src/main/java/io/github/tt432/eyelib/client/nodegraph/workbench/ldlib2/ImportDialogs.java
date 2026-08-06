package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if >=1.20.1 {
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import io.github.tt432.eyelib.client.jsonview.EntityJsonService;
import io.github.tt432.eyelib.client.nodegraph.DiagnosticsCenter;
import io.github.tt432.eyelib.client.nodegraph.GraphLibraryManager;
import io.github.tt432.eyelib.client.nodegraph.ImportClosure;
import io.github.tt432.eyelib.client.nodegraph.KnownRefTables;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmDiagnostics;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.Ldlib2NodegraphEditor;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.decompile.ImportResult;
import io.github.tt432.eyelib.nodegraph.decompile.JsonGraphImporters;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * JSON → 图 导入对话框组（规格 §W2，语义与 ldlib1 工作台对齐）：
 *
 * <ul>
 *   <li>注册表导入：客户端实体 / 渲染控制器 id 列表（{@link EntityJsonService}），
 *       单击即导入；</li>
 *   <li>粘贴 JSON 文本：多行 {@link TextArea}，按根键自动判别三形态
 *       （{@code minecraft:client_entity} / {@code render_controllers} /
 *       {@code animation_controllers}）；RC/AC 名取文件内该根对象的第一个键，不弹输入；</li>
 *   <li>从文件导入：LDLib2 自带 {@link Dialog#showFileDialog}（.json 过滤）。</li>
 * </ul>
 *
 * ClientEntity 走闭包导入（规格 nodegraph-declaration-wiring §3 D2，
 * {@link ImportClosure#importWithClosure}）：实体库先注册，主图 ref.rc / ref.ac
 * 引用的 RC/AC 文档跟随导入为独立库。直接导入 RC/AC 文件的路径不变。
 *
 * 成功后（诊断含 error 也照常）：新名注册 {@link GraphLibraryManager} →
 * 诊断上报 {@link DiagnosticsCenter}（分节：实体节=实体标识符，RC 节 "rc:<id>"，
 * AC 节 "ac:<id>"）→ {@link EvmDiagnostics#info} 成功提示 →
 * {@link Ldlib2NodegraphEditor#open} 重开编辑器到新库。
 */
final class ImportDialogs {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportDialogs.class);

    private ImportDialogs() {
    }

    /** 导入来源类别（注册表导入两类，同 ldlib1）。 */
    private enum SourceKind {
        CLIENT_ENTITY("客户端实体"),
        RENDER_CONTROLLER("渲染控制器");

        final String label;

        SourceKind(String label) {
            this.label = label;
        }
    }

    /** 注册表导入条目。 */
    private record Entry(SourceKind kind, String id) {
    }

    // ==================== 入口菜单 ====================

    /** 工具条「导入」按钮：三个来源的入口对话框。 */
    static void openImportMenu(UIElement host) {
        ModularUI mui = host.getModularUI();
        if (mui == null) {
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setTitle("导入 JSON 为节点图");
        dialog.overlay.layout(layout -> layout.width(280));
        dialog.addContent(menuButton("从注册表导入（客户端实体 / 渲染控制器）", () -> {
            dialog.close();
            openRegistryDialog(mui);
        }));
        dialog.addContent(menuButton("粘贴 JSON 文本", () -> {
            dialog.close();
            openTextDialog(mui);
        }));
        dialog.addContent(menuButton("从 JSON 文件导入", () -> {
            dialog.close();
            openFileDialog(mui);
        }));
        dialog.addButton(new Button()
                .setOnClick(event -> dialog.close())
                .setText("ldlib.gui.tips.cancel")
                .addClass("__cancel-button__"));
        dialog.show(mui);
    }

    private static Button menuButton(String text, Runnable onClick) {
        Button button = new Button();
        button.setText(Component.literal(text));
        button.textStyle(style -> style.fontSize(9));
        button.setOnClick(event -> onClick.run());
        button.layout(layout -> layout.widthPercent(100).height(16));
        return button;
    }

    // ==================== 注册表导入 ====================

    private static void openRegistryDialog(ModularUI mui) {
        EntityJsonService service = new EntityJsonService();
        List<Entry> all = new ArrayList<>();
        for (String id : service.clientEntityIds()) {
            all.add(new Entry(SourceKind.CLIENT_ENTITY, id));
        }
        for (String name : service.renderControllerIds()) {
            all.add(new Entry(SourceKind.RENDER_CONTROLLER, name));
        }

        Dialog dialog = new Dialog();
        dialog.setTitle("从注册表导入");
        dialog.overlay.layout(layout -> layout.width(360));

        TextField filter = new TextField();
        filter.textFieldStyle(style -> style.placeholder(Component.literal("过滤 id…")));
        filter.layout(layout -> layout.widthPercent(100).height(14));

        ScrollerView list = new ScrollerView();
        list.layout(layout -> layout.widthPercent(100).height(220));

        Consumer<String> rebuild = text -> {
            String needle = text.toLowerCase(Locale.ROOT);
            list.clearAllScrollViewChildren();
            for (Entry entry : all) {
                if (!needle.isEmpty() && !entry.id().toLowerCase(Locale.ROOT).contains(needle)) {
                    continue;
                }
                list.addScrollViewChild(WorkbenchWidgets.rowButton(
                        "[" + entry.kind().label + "] " + entry.id(),
                        WorkbenchColors.TEXT, false, () -> {
                            dialog.close();
                            importFromRegistry(service, entry);
                        }));
            }
        };
        filter.setTextResponder(rebuild);
        rebuild.accept("");

        dialog.addContent(filter);
        dialog.addContent(list);
        dialog.addButton(new Button()
                .setOnClick(event -> dialog.close())
                .setText("ldlib.gui.tips.cancel")
                .addClass("__cancel-button__"));
        dialog.show(mui);
    }

    private static void importFromRegistry(EntityJsonService service, Entry entry) {
        Optional<String> json = switch (entry.kind()) {
            case CLIENT_ENTITY -> service.clientEntityJson(entry.id());
            case RENDER_CONTROLLER -> service.renderControllerJson(entry.id());
        };
        if (json.isEmpty()) {
            reportError("注册表导入", "JSON 编码失败: " + entry.id() + "（详见日志）");
            return;
        }
        try {
            JsonObject fileJson = JsonParser.parseString(json.get()).getAsJsonObject();
            if (entry.kind() == SourceKind.CLIENT_ENTITY) {
                importEntityWithClosure(fileJson, entry.id());
            } else {
                // D4 跨文档关联：携已知短名表回填裸短名 ref 的标识符
                finish(JsonGraphImporters.importRenderController(
                        fileJson, entry.id(), KnownRefTables.collectForRc(entry.id())), entry.id());
            }
        } catch (RuntimeException e) {
            LOGGER.warn("[nodegraph] registry import failed for '{}'", entry.id(), e);
            reportError("注册表导入", String.valueOf(e.getMessage()));
        }
    }

    // ==================== JSON 文本 / 文件导入 ====================

    private static void openTextDialog(ModularUI mui) {
        Dialog dialog = new Dialog();
        dialog.setTitle("粘贴 JSON 文本导入");
        dialog.overlay.layout(layout -> layout.width(420));

        TextArea textArea = new TextArea();
        textArea.textAreaStyle(style -> style.placeholder(Component.literal(
                "粘贴 client_entity / render_controllers / animation_controllers 文件 JSON…")));
        textArea.layout(layout -> layout.widthPercent(100).height(220));

        dialog.addContent(textArea);
        dialog.addButton(new Button()
                .setOnClick(event -> {
                    String text = String.join("\n", textArea.getValue());
                    dialog.close();
                    importFromText(text);
                })
                .setText("ldlib.gui.tips.confirm")
                .addClass("__confirm-button__"));
        dialog.addButton(new Button()
                .setOnClick(event -> dialog.close())
                .setText("ldlib.gui.tips.cancel")
                .addClass("__cancel-button__"));
        dialog.show(mui);
    }

    private static void openFileDialog(ModularUI mui) {
        File dir = Minecraft.getInstance().gameDirectory;
        Dialog.showFileDialog("导入 JSON 文件", dir, true, Dialog.suffixFilter("json"),
                file -> {
                    try {
                        importFromText(Files.readString(file.toPath()));
                    } catch (IOException e) {
                        LOGGER.warn("[nodegraph] failed to read import file '{}'", file, e);
                        reportError("文件导入", "无法读取文件: " + e.getMessage());
                    }
                }).show(mui);
    }

    /** 按根键自动判别三种文件形态（RC/AC 名取根对象第一个键）。 */
    private static void importFromText(String text) {
        JsonObject root;
        try {
            root = JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException e) {
            reportError("文本导入", "JSON 解析失败: " + e.getMessage());
            return;
        }
        try {
            if (root.has("minecraft:client_entity")) {
                importEntityWithClosure(root, clientEntityBaseName(root));
                return;
            }
            String rcName = firstKey(root, "render_controllers");
            if (rcName != null) {
                finish(JsonGraphImporters.importRenderController(root, rcName, KnownRefTables.collectForRc(rcName)), rcName);
                return;
            }
            String acName = firstKey(root, "animation_controllers");
            if (acName != null) {
                finish(JsonGraphImporters.importAnimationControllers(root, acName, KnownRefTables.collect()), acName);
                return;
            }
            reportError("文本导入", "无法判别 JSON 形态：根键需为 minecraft:client_entity / render_controllers / animation_controllers 之一");
        } catch (RuntimeException e) {
            LOGGER.warn("[nodegraph] text import failed", e);
            reportError("文本导入", String.valueOf(e.getMessage()));
        }
    }

    /** 根对象第一个键（空或缺失返回 null）。 */
    private static @Nullable String firstKey(JsonObject root, String rootKey) {
        if (!root.has(rootKey) || !root.get(rootKey).isJsonObject()) {
            return null;
        }
        for (Map.Entry<String, com.google.gson.JsonElement> entry : root.getAsJsonObject(rootKey).entrySet()) {
            return entry.getKey();
        }
        return null;
    }

    /** client_entity 文件 → 库名基底（description.identifier，取不到回退 imported，同 ldlib1）。 */
    private static String clientEntityBaseName(JsonObject root) {
        try {
            return root.getAsJsonObject("minecraft:client_entity")
                    .getAsJsonObject("description")
                    .get("identifier").getAsString();
        } catch (RuntimeException e) {
            return "imported";
        }
    }

    // ==================== 收尾 ====================

    /**
     * ClientEntity 闭包导入（规格 inline-render-controller §6）：RC 已内联进实体主图，
     * 只需注册 AC 闭包库；诊断分节上报，成功提示保留，最后打开实体库。
     */
    private static void importEntityWithClosure(JsonObject root, String baseName) {
        String entityName = freshLibraryName(baseName);
        ImportClosure.Result closure = ImportClosure.importWithClosure(root, entityName);
        String entityId = closure.entityId() != null ? closure.entityId() : entityName;
        List<DiagnosticsCenter.Section> sections = new ArrayList<>();
        sections.add(new DiagnosticsCenter.Section(entityId, closure.entity().diagnostics()));
        int libraries = 1;
        for (ImportClosure.NamedImport ac : closure.acs()) {
            if (ac.result() != null) {
                GraphLibraryManager.INSTANCE.put(freshLibraryName(ac.id()), ac.result().library());
                libraries++;
            }
            sections.add(new DiagnosticsCenter.Section("ac:" + ac.id(), ac.diagnostics()));
        }
        DiagnosticsCenter.report("导入 " + entityId, sections);
        EvmDiagnostics.info("imported as nodegraph library '" + entityName + "' (client_entity, "
                + libraries + " libraries with closure)");
        Ldlib2NodegraphEditor.open(entityName);
    }

    /** 导入入口级错误上报（规格 §4：走 DiagnosticsCenter + 浮动面板，不再弹通知）。 */
    private static void reportError(String label, String message) {
        DiagnosticsCenter.report("导入", label,
                List.of(Diagnostic.error(ImportClosure.IMPORT_FAILURE, message)));
    }

    /** 注册新库并重开编辑器（诊断含 error 也照常导入，同 ldlib1）。 */
    private static void finish(ImportResult result, String baseName) {
        String name = freshLibraryName(baseName);
        GraphLibraryManager.INSTANCE.put(name, result.library());
        DiagnosticsCenter.report("导入 " + baseName, name, result.diagnostics());
        EvmDiagnostics.info("imported as nodegraph library '" + name + "' ("
                + kindName(result.library().kind()) + ")");
        Ldlib2NodegraphEditor.open(name);
    }

    private static String kindName(GraphKind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    /** 导入库名：id 去命名空间 + 文件名合法化 + 去重（untitled 同款后缀策略）。 */
    private static String freshLibraryName(String baseName) {
        String base = baseName;
        int colon = base.lastIndexOf(':');
        if (colon >= 0) {
            base = base.substring(colon + 1);
        }
        base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (base.isEmpty()) {
            base = "imported";
        }
        String name = base;
        int suffix = 2;
        while (GraphLibraryManager.INSTANCE.get(name) != null) {
            name = base + "_" + suffix++;
        }
        return name;
    }
}
//?}

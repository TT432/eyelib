//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import io.github.tt432.eyelib.client.gui.manager.io.FileDialogService;
import io.github.tt432.eyelib.client.jsonview.EntityJsonService;
import io.github.tt432.eyelib.client.nodegraph.DiagnosticsCenter;
import io.github.tt432.eyelib.client.nodegraph.GraphLibraryManager;
import io.github.tt432.eyelib.client.nodegraph.ImportClosure;
import io.github.tt432.eyelib.client.nodegraph.KnownRefTables;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib1.Ldlib1NodegraphEditor;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.decompile.ImportResult;
import io.github.tt432.eyelib.nodegraph.decompile.JsonGraphImporters;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 「导入」模态对话框（规格 §W2，LDLib1 薄壳）：JSON → 图反编译入口。
 *
 * <p>来源二选一：
 * <ul>
 *   <li>注册表 id：种类（客户端实体/渲染控制器）+ 过滤 id 列表 →
 *       {@link EntityJsonService} 取 JSON 文本 → 对应 importer；</li>
 *   <li>JSON 文件：复用 ManagerImportActions 同款的
 *       {@link FileDialogService#selectJsonFile} 文件对话框，按文件根键自动判别
 *       三种形态（{@code minecraft:client_entity} / {@code render_controllers} /
 *       {@code animation_controllers}）；RC/AC 名取文件内该根对象的第一个键。</li>
 * </ul>
 *
 * <p>ClientEntity 走闭包导入（规格 nodegraph-declaration-wiring §3 D2，
 * {@link ImportClosure#importWithClosure}）：实体库先注册，主图 ref.rc / ref.ac
 * 引用的 RC/AC 文档跟随导入为独立库。直接导入 RC/AC 文件的路径不变。
 *
 * <p>导入产物以 freshLibraryName 风格新名（imported / imported_2 / …）注册进
 * {@link GraphLibraryManager}（不落盘，保存由编辑器负责），诊断上报
 * {@link DiagnosticsCenter}（有 error 也导入——占位语义已在库内），随后重开编辑器到新库。
 */
public final class ImportDialog extends DialogWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportDialog.class);

    private enum SourceKind {
        CLIENT_ENTITY,
        RENDER_CONTROLLER
    }

    private final EntityJsonService service = new EntityJsonService();
    private final ListRowView idList;

    private SourceKind kind = SourceKind.CLIENT_ENTITY;
    private String filter = "";

    public ImportDialog(WidgetGroup parent) {
        super(parent, true);
        WidgetGroup content = DialogWidget.createContainer(this, 260, 180, "导入");

        int inner = 260 - 8;
        content.addWidget(new LabelWidget(4, 2, "从注册表导入："));
        content.addWidget(new ButtonWidget(4, 14, 100, 13, new TextTexture("客户端实体"),
                cd -> switchKind(SourceKind.CLIENT_ENTITY)));
        content.addWidget(new ButtonWidget(108, 14, 100, 13, new TextTexture("渲染控制器"),
                cd -> switchKind(SourceKind.RENDER_CONTROLLER)));
        TextFieldWidget filterField = new TextFieldWidget(4, 30, inner, 13, null,
                text -> filter = text == null ? "" : text);
        filterField.setHoverTooltips("过滤 id");
        content.addWidget(filterField);

        idList = new ListRowView(4, 46, inner, 66, this::rows, row -> {
        });
        content.addWidget(idList);

        content.addWidget(new ButtonWidget(4, 116, 60, 14, new TextTexture("导入"),
                cd -> importSelected()));
        content.addWidget(new ButtonWidget(70, 116, 120, 14, new TextTexture("从 JSON 文件导入"),
                cd -> importFromFile()));
        content.addWidget(new ButtonWidget(196, 116, 56, 14, new TextTexture("关闭"),
                cd -> close()));
    }

    // ---------- 注册表来源 ----------

    private List<ListRowView.Row> rows() {
        List<String> ids = switch (kind) {
            case CLIENT_ENTITY -> service.clientEntityIds();
            case RENDER_CONTROLLER -> service.renderControllerIds();
        };
        String needle = filter.toLowerCase(Locale.ROOT);
        return ids.stream()
                .filter(id -> needle.isEmpty() || id.toLowerCase(Locale.ROOT).contains(needle))
                .map(id -> new ListRowView.Row(id, id))
                .toList();
    }

    private void switchKind(SourceKind newKind) {
        kind = newKind;
        idList.setSelectedId(null);
    }

    private void importSelected() {
        String id = idList.selectedId();
        if (id == null) {
            reportImportError("注册表导入", "请先在列表中选择要导入的 id");
            return;
        }
        Optional<String> json = switch (kind) {
            case CLIENT_ENTITY -> service.clientEntityJson(id);
            case RENDER_CONTROLLER -> service.renderControllerJson(id);
        };
        if (json.isEmpty()) {
            reportImportError("注册表导入", "无法获取 '" + id + "' 的 JSON（详见日志）");
            return;
        }
        JsonObject root = JsonParser.parseString(json.get()).getAsJsonObject();
        if (kind == SourceKind.CLIENT_ENTITY) {
            importEntityWithClosure(root, id);
        } else {
            // D4 跨文档关联：携已知短名表回填裸短名 ref 的标识符
            finish(JsonGraphImporters.importRenderController(root, id, KnownRefTables.collectForRc(id)), id);
        }
    }

    // ---------- 文件来源 ----------

    private void importFromFile() {
        FileDialogService.selectJsonFile("读取 JSON 文件", Path.of("/"))
                .whenComplete((selected, throwable) -> {
                    if (throwable != null) {
                        LOGGER.warn("[nodegraph] json file dialog failed", throwable);
                        return;
                    }
                    if (selected.isEmpty() || !selected.get().toString().endsWith(".json")) {
                        return;
                    }
                    Path path = selected.get();
                    // 文件对话框在独立线程完成，导入与打开编辑器必须回到客户端线程
                    Minecraft.getInstance().execute(() -> {
                        try {
                            JsonObject root = JsonParser.parseString(
                                    Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                            importByRootShape(root);
                        } catch (Exception e) {
                            LOGGER.warn("[nodegraph] import file '{}' failed", path, e);
                            reportImportError(path.getFileName().toString(), "导入失败：" + e.getMessage());
                        }
                    });
                });
    }

    /** 按文件根键判别三种 JSON 形态（RC/AC 名取根对象的第一个键）。 */
    private void importByRootShape(JsonObject root) {
        if (root.has("minecraft:client_entity")) {
            importEntityWithClosure(root, clientEntityIdentifier(root));
        } else if (root.get("render_controllers") instanceof JsonObject renderControllers) {
            String name = firstKey(renderControllers);
            if (name == null) {
                reportImportError("render_controllers", "render_controllers 为空，无法导入");
            } else {
                finish(JsonGraphImporters.importRenderController(root, name, KnownRefTables.collectForRc(name)), name);
            }
        } else if (root.get("animation_controllers") instanceof JsonObject animationControllers) {
            String name = firstKey(animationControllers);
            if (name == null) {
                reportImportError("animation_controllers", "animation_controllers 为空，无法导入");
            } else {
                finish(JsonGraphImporters.importAnimationControllers(root, name, KnownRefTables.collect()), name);
            }
        } else {
            reportImportError("文件导入", "无法识别的 JSON 形态"
                    + "（需 minecraft:client_entity / render_controllers / animation_controllers 根键）");
        }
    }

    /** 从 client_entity 文件 JSON 取 description.identifier（取不到回退 null）。 */
    private static @Nullable String clientEntityIdentifier(JsonObject root) {
        if (root.get("minecraft:client_entity") instanceof JsonObject body
                && body.get("description") instanceof JsonObject description
                && description.get("identifier") instanceof JsonPrimitive identifier
                && identifier.isString()) {
            return identifier.getAsString();
        }
        return null;
    }

    private static @Nullable String firstKey(JsonObject object) {
        return object.keySet().isEmpty() ? null : object.keySet().iterator().next();
    }

    // ---------- 收尾 ----------

    /**
     * ClientEntity 闭包导入（规格 inline-render-controller §6）：RC 已内联进实体主图，
     * 只需注册 AC 闭包库；诊断分节上报（实体节=实体标识符，AC 节 "ac:<id>"），
     * 最后打开实体库。
     */
    private void importEntityWithClosure(JsonObject root, @Nullable String sourceId) {
        String entityName = freshLibraryName(sourceId);
        ImportClosure.Result closure = ImportClosure.importWithClosure(root, entityName);
        String entityId = closure.entityId() != null ? closure.entityId()
                : sourceId != null ? sourceId : entityName;
        List<DiagnosticsCenter.Section> sections = new ArrayList<>();
        sections.add(new DiagnosticsCenter.Section(entityId, closure.entity().diagnostics()));
        for (ImportClosure.NamedImport ac : closure.acs()) {
            if (ac.result() != null) {
                GraphLibraryManager.INSTANCE.put(freshLibraryName(ac.id()), ac.result().library());
            }
            sections.add(new DiagnosticsCenter.Section("ac:" + ac.id(), ac.diagnostics()));
        }
        DiagnosticsCenter.report("导入 " + entityId, sections);
        Ldlib1NodegraphEditor.open(entityName);
    }

    /** 导入入口级错误上报（规格 §4：不再刷聊天栏，走 DiagnosticsCenter + 浮动面板）。 */
    private static void reportImportError(String label, String message) {
        DiagnosticsCenter.report("导入", label,
                List.of(Diagnostic.error(ImportClosure.IMPORT_FAILURE, message)));
    }

    /** 注册新库 → 报告诊断（有 error 也导入）→ 重开编辑器到新库。 */
    private void finish(ImportResult result, @Nullable String sourceId) {
        String name = freshLibraryName(sourceId);
        GraphLibraryManager.INSTANCE.put(name, result.library());
        DiagnosticsCenter.report("导入 " + (sourceId != null ? sourceId : name), name, result.diagnostics());
        Ldlib1NodegraphEditor.open(name);
    }

    /**
     * 导入库名：来源 id 派生（去命名空间前缀，{@code [^a-zA-Z0-9._-]} 清洗为 '_'），
     * 冲突加 _2/_3 后缀；空/未知名回退 imported / imported_2 / …。
     */
    private static String freshLibraryName(@Nullable String sourceId) {
        String base = "imported";
        if (sourceId != null && !sourceId.isEmpty()) {
            int colon = sourceId.indexOf(':');
            String derived = colon >= 0 ? sourceId.substring(colon + 1) : sourceId;
            derived = derived.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (!derived.isEmpty()) {
                base = derived;
            }
        }
        String name = base;
        int suffix = 2;
        while (GraphLibraryManager.INSTANCE.get(name) != null) {
            name = base + "_" + suffix;
            suffix++;
        }
        return name;
    }
}
//?}

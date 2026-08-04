package io.github.tt432.eyelib.client.nodegraph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.decompile.ImportResult;
import io.github.tt432.eyelib.nodegraph.decompile.JsonGraphImporters;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 闭包导入（规格 nodegraph-inline-render-controller §6）：UI 导入 ClientEntity 时，
 * 引用的 RenderController 文档经 resolver **内联反编译进实体主图**（单库产物）；
 * AnimationController 保持独立库闭包（双消费端语义，不内联）。
 *
 * <p>流程（{@link #importWithClosure}）：
 * <ol>
 *   <li>{@link JsonGraphImporters#importClientEntity} 以 RC resolver 导入实体并注册进
 *       {@link GraphLibraryManager}：resolver 注册表优先（{@link EntityJsonService 编码路径}
 *       ——vanilla/BedrockAddonLoader 来源的 RC 不在资源目录），回落
 *       {@code eyelib/render_controllers} 资源目录扫描；未命中 → ref.rc 外部引用
 *       （诊断 RC_INLINE_MISS 在实体节）；</li>
 *   <li>主图 ref.ac 的 identifier 收集 → 逐个扫 {@code eyelib/animation_controllers}
 *       导入为独立库；找不到 → 该文档记 {@link #CLOSURE_MISS} warning，不阻断。</li>
 * </ol>
 *
 * <p>资源扫描经 {@link FileToIdConverter}（SimpleJsonWithSuffixResourceReloadListener
 * 已消化的跨版本模式；{@code var} 规避 26.1 ResourceLocation → Identifier 改名）。
 * 两次调用间不缓存（导入是低频操作）；单次闭包内按目录缓存已扫文件。
 */
public final class ImportClosure {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportClosure.class);
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    /** 闭包文档缺失：资源包中扫不到含目标 id 的 AC 文件（warning，不阻断）。 */
    public static final String CLOSURE_MISS = "CLOSURE_MISS";

    /** 导入入口级错误（JSON 解析失败 / 形态不识别 / 读取失败等，两版 ImportDialog 共用）。 */
    public static final String IMPORT_FAILURE = "IMPORT_FAILURE";

    private static final String RC_DIR = "eyelib/render_controllers";
    private static final String AC_DIR = "eyelib/animation_controllers";

    private ImportClosure() {
    }

    /**
     * 闭包中一个具名 AC 文档的导入产物。
     *
     * @param id          AC 标识符
     * @param result      导入结果；文档未找到时为 null（不注册库）
     * @param diagnostics 该文档的诊断（找到 = 导入器诊断；未找到 = CLOSURE_MISS warning）
     */
    public record NamedImport(String id, @Nullable ImportResult result, List<Diagnostic> diagnostics) {
        public NamedImport {
            diagnostics = List.copyOf(diagnostics);
        }

        private static NamedImport found(String id, ImportResult result) {
            return new NamedImport(id, result, result.diagnostics());
        }

        private static NamedImport miss(String id) {
            return new NamedImport(id, null, List.of(Diagnostic.warning(CLOSURE_MISS,
                    "资源包中找不到 '" + id + "'，跳过导入")));
        }
    }

    /**
     * 闭包导入产物。
     *
     * @param entity   实体导入结果（库已由 {@link #importWithClosure} 注册；RC 已内联）
     * @param entityId 实体标识符（产物库 root 选项；取不到为 null）
     * @param acs      ref.ac 闭包（uid 序去重，独立库）
     */
    public record Result(ImportResult entity, @Nullable String entityId,
                         List<NamedImport> acs) {
        public Result {
            acs = List.copyOf(acs);
        }
    }

    /**
     * 在资源目录 {@code dir} 的全部 .json 中找「根集合含 {@code id} 键」的文件，
     * 返回整个文件 JSON（单文件可含多控制器，调用方再取目标键）。
     * 集合键取目录末段（{@code eyelib/render_controllers} → {@code render_controllers}）。
     * 无跨调用缓存；单次闭包导入内的目录缓存由 {@link #importWithClosure} 自持。
     */
    public static Optional<JsonObject> findDocument(String dir, String id) {
        return findIn(scanDocuments(dir), collectionKey(dir), id);
    }

    /**
     * 实体 + AC 引用闭包导入（v4：RC 内联）。实体库以 {@code entityLibraryName} 注册；
     * AC 库的命名与注册由调用方负责。
     */
    public static Result importWithClosure(JsonObject entityFileJson, String entityLibraryName) {
        LazyScan rcScan = new LazyScan(RC_DIR);
        ImportResult entity = JsonGraphImporters.importClientEntity(entityFileJson,
                rcId -> registryRenderController(rcId).or(() -> rcScan.find(rcId)));
        GraphLibraryManager.INSTANCE.put(entityLibraryName, entity.library());

        String identifier = entity.library().mainGraph().nodes().stream()
                .filter(node -> "root".equals(node.uid()))
                .findFirst()
                .map(node -> node.optionString("identifier", ""))
                .orElse("");
        String entityId = identifier.isEmpty() ? null : identifier;

        List<NamedImport> acs = new ArrayList<>();
        LazyScan acScan = new LazyScan(AC_DIR);
        for (String acId : collectRefIdentifiers(entity, NodeTypes.REF_AC.id())) {
            Optional<JsonObject> doc = acScan.find(acId);
            acs.add(doc.<NamedImport>map(json -> NamedImport.found(acId,
                            JsonGraphImporters.importAnimationControllers(
                                    json, acId, KnownRefTables.collect())))
                    .orElseGet(() -> NamedImport.miss(acId)));
        }

        return new Result(entity, entityId, acs);
    }

    /** 产物库主图中指定 ref 类型的 identifier 选项（非空、去重；节点插入序即 uid 序）。 */
    private static List<String> collectRefIdentifiers(ImportResult entity, String refType) {
        Set<String> out = new LinkedHashSet<>();
        for (NodeInstance node : entity.library().mainGraph().nodes()) {
            if (refType.equals(node.type())) {
                String identifier = node.optionString("identifier", "");
                if (!identifier.isEmpty()) {
                    out.add(identifier);
                }
            }
        }
        return List.copyOf(out);
    }

    /**
     * 扫描资源目录全部 .json 并解析（解析失败的文件记日志跳过）。
     * 键类型（ResourceLocation/Identifier）经 {@code var} 规避，无需 //? 分支。
     */
    private static List<JsonObject> scanDocuments(String dir) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        List<JsonObject> docs = new ArrayList<>();
        var converter = new FileToIdConverter(dir, ".json");
        for (var entry : converter.listMatchingResources(resourceManager).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                if (GSON.fromJson(reader, JsonElement.class) instanceof JsonObject doc) {
                    docs.add(doc);
                }
            } catch (Exception e) {
                LOGGER.warn("[nodegraph] closure scan: failed to parse '{}'", entry.getKey(), e);
            }
        }
        return docs;
    }

    private static Optional<JsonObject> findIn(List<JsonObject> docs, String collectionKey, String id) {
        for (JsonObject doc : docs) {
            if (doc.get(collectionKey) instanceof JsonObject collection && collection.has(id)) {
                return Optional.of(doc);
            }
        }
        return Optional.empty();
    }

    /** 目录扫描懒加载：首次 find 时才扫（注册表命中时完全不扫）。 */
    private static final class LazyScan {
        private final String dir;
        private @Nullable List<JsonObject> docs;

        LazyScan(String dir) {
            this.dir = dir;
        }

        Optional<JsonObject> find(String id) {
            if (docs == null) {
                docs = scanDocuments(dir);
            }
            return findIn(docs, collectionKey(dir), id);
        }
    }

    /** 注册表 RC → render_controllers 文件形态 JSON（EntityJsonService 编码路径）。 */
    private static Optional<JsonObject> registryRenderController(String rcId) {
        return new io.github.tt432.eyelib.client.jsonview.EntityJsonService()
                .renderControllerJson(rcId)
                .map(text -> com.google.gson.JsonParser.parseString(text).getAsJsonObject());
    }

    /** 集合键 = 资源目录末段（{@code eyelib/render_controllers} → {@code render_controllers}）。 */
    private static String collectionKey(String dir) {
        int slash = dir.lastIndexOf('/');
        return slash >= 0 ? dir.substring(slash + 1) : dir;
    }
}

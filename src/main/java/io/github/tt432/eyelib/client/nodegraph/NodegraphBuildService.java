package io.github.tt432.eyelib.client.nodegraph;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.GraphValidator;
import io.github.tt432.eyelib.nodegraph.assembly.AnimationControllerAssembler;
import io.github.tt432.eyelib.nodegraph.assembly.AssemblyResult;
import io.github.tt432.eyelib.nodegraph.assembly.ClientEntityAssembler;
import io.github.tt432.eyelib.nodegraph.assembly.RenderControllerAssembler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 图文档库 → 运行时资源构建管线（规格 §2.6）。
 *
 * <p>流程：取库 → 验证 → 组装为 Bedrock JSON → 现有 CODEC 往返解析 → 注入对应 Manager。
 * CODEC 往返提供免费的 schema 校验；产物 JSON 同时是导出落盘形态。
 *
 * @author TT432
 */
public final class NodegraphBuildService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodegraphBuildService.class);

    private NodegraphBuildService() {
    }

    /**
     * @param diagnostics 全量诊断（验证 + 组装 + CODEC 往返）
     * @param injectedId  注入成功的资源标识符（未注入 = null）
     */
    public record BuildResult(List<Diagnostic> diagnostics, @Nullable String injectedId) {
        public boolean success() {
            return injectedId != null && diagnostics.stream()
                    .noneMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
        }
    }

    /** 构建并注入指定图文档库。 */
    public static BuildResult build(String libraryName) {
        GraphLibrary library = GraphLibraryManager.INSTANCE.get(libraryName);
        if (library == null) {
            return new BuildResult(List.of(
                    Diagnostic.error("LIBRARY_NOT_FOUND", "graph library not found: " + libraryName)), null);
        }
        return build(libraryName, library);
    }

    /** 构建并注入给定图文档库（不查 Manager，编辑器直接用内存态）。 */
    public static BuildResult build(String libraryName, GraphLibrary library) {
        List<Diagnostic> diagnostics = new ArrayList<>(GraphValidator.validate(library));
        if (diagnostics.stream().anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR)) {
            return new BuildResult(diagnostics, null);
        }

        AssemblyResult assembly = switch (library.kind()) {
            case CLIENT_ENTITY -> ClientEntityAssembler.assemble(library);
            case RENDER_CONTROLLER -> RenderControllerAssembler.assemble(library);
            case ANIMATION_CONTROLLER -> AnimationControllerAssembler.assemble(library);
            case EXPRESSION_LIB -> new AssemblyResult(new JsonObject(), List.of(
                    Diagnostic.error("NOT_BUILDABLE", "expression_lib 库不可单独构建，仅供其他库引用")));
        };
        diagnostics.addAll(assembly.diagnostics());
        if (assembly.hasErrors()) {
            return new BuildResult(diagnostics, null);
        }

        return switch (library.kind()) {
            case CLIENT_ENTITY -> injectClientEntity(diagnostics, assembly.json());
            case RENDER_CONTROLLER -> injectRenderController(diagnostics, library, assembly.json());
            case ANIMATION_CONTROLLER -> injectAnimationController(diagnostics, library, assembly.json());
            case EXPRESSION_LIB -> new BuildResult(diagnostics, null);
        };
    }

    private static BuildResult injectClientEntity(List<Diagnostic> diagnostics, JsonObject json) {
        var parsed = BrClientEntity.CODEC.parse(JsonOps.INSTANCE, json);
        if (parsed.error().isPresent()) {
            diagnostics.add(Diagnostic.error("CODEC_ROUNDTRIP",
                    "client_entity CODEC 往返失败: " + parsed.error().get().message()));
            return new BuildResult(diagnostics, null);
        }
        BrClientEntity entity = parsed.result().orElseThrow();
        ClientEntityManager.INSTANCE.put(entity.identifier(), entity);
        LOGGER.info("[nodegraph] injected client entity {}", entity.identifier());
        warnModelTextureMeshCoverage(diagnostics, entity);
        return new BuildResult(diagnostics, entity.identifier());
    }

    /**
     * D9 模型短名覆盖度检查：图内引用几何的 texture_meshes 短名须能经实体纹理表解析，
     * 否则运行时静默跳过该 mesh（RenderControllerEntry:230）。缺 "default" 键时才报
     * （有 default 时运行时有回退）。per-bone material 运行时暂无消费端，不检查。
     */
    private static void warnModelTextureMeshCoverage(List<Diagnostic> diagnostics, BrClientEntity entity) {
        Set<String> textureKeys = entity.textures().keySet();
        if (textureKeys.contains("default")) {
            return;
        }
        Set<String> missing = new TreeSet<>();
        for (String geometryId : new LinkedHashSet<>(entity.geometry().values())) {
            Model model = ModelManager.INSTANCE.get(geometryId);
            if (model == null) {
                continue;
            }
            for (var boneEntry : model.allBones().int2ObjectEntrySet()) {
                for (Model.TextureMesh tm : boneEntry.getValue().textureMeshes()) {
                    if (!textureKeys.contains(tm.texture())) {
                        missing.add(tm.texture() + "（几何 " + geometryId + "）");
                    }
                }
            }
        }
        for (String name : missing) {
            diagnostics.add(Diagnostic.warning("UNCOVERED_TEXTURE_MESH",
                    "模型 texture_mesh 短名 '" + name + "' 未被实体纹理表覆盖（运行时将跳过该体素化 mesh）；"
                            + "请为对应 ref.texture 设置显式 short_name 覆盖"));
        }
    }

    private static BuildResult injectRenderController(List<Diagnostic> diagnostics, GraphLibrary library, JsonObject json) {
        // 组装产物形如 {"render_controllers": {"<id>": {...}}}，直接走运行时 RenderControllers.CODEC
        var parsed = RenderControllers.CODEC.parse(JsonOps.INSTANCE, json);
        if (parsed.error().isPresent()) {
            diagnostics.add(Diagnostic.error("CODEC_ROUNDTRIP",
                    "render_controller CODEC 往返失败: " + parsed.error().get().message()));
            return new BuildResult(diagnostics, null);
        }
        RenderControllers controllers = parsed.result().orElseThrow();
        String[] firstId = {null};
        controllers.render_controllers().forEach((key, entry) -> {
            RenderControllerManager.INSTANCE.put(key, entry);
            if (firstId[0] == null) {
                firstId[0] = key;
            }
        });
        LOGGER.info("[nodegraph] injected render controller(s) from library, count={}", controllers.render_controllers().size());
        return new BuildResult(diagnostics, firstId[0]);
    }

    private static BuildResult injectAnimationController(List<Diagnostic> diagnostics, GraphLibrary library, JsonObject json) {
        var parsed = BrAnimationControllerSet.CODEC.parse(JsonOps.INSTANCE, json);
        if (parsed.error().isPresent()) {
            diagnostics.add(Diagnostic.error("CODEC_ROUNDTRIP",
                    "animation_controller CODEC 往返失败: " + parsed.error().get().message()));
            return new BuildResult(diagnostics, null);
        }
        BrAnimationControllers controllers = BrAnimationControllers.fromSchemaSet(parsed.result().orElseThrow());
        AnimationAssetRegistry.stageControllers(Map.of(library.main(), controllers));
        LOGGER.info("[nodegraph] staged animation controller(s) from library {}", library.main());
        return new BuildResult(diagnostics, library.main());
    }
}

package io.github.tt432.eyelib.client.gui.manager.reload;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.gui.manager.io.FileDialogService;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import io.github.tt432.eyelib.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelib.util.codec.CodecOps;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ManagerImportActions {
    private static final Gson GSON = new Gson();

    public static void importAnimation(Logger logger) {
        importJson("读取文件", json -> AnimationAssetRegistry.publishAnimation(
                BrAnimation.fromSchemaSet(CodecOps.getOrThrowLog(BrAnimationSet.CODEC.parse(JsonOps.INSTANCE, json), logger))));
    }

    public static void importAnimationController(Logger logger) {
        importJson("读取文件", json -> AnimationAssetRegistry.publishAnimationController(
                BrAnimationControllers.fromSchemaSet(CodecOps.getOrThrowLog(BrAnimationControllerSet.CODEC.parse(JsonOps.INSTANCE, json), logger))));
    }

    public static void importRenderController(Logger logger) {
        importJson("读取文件", json -> {
            var controller = CodecOps.getOrThrowLog(RenderControllers.CODEC.parse(JsonOps.INSTANCE, json), logger);
            controller.render_controllers().forEach((key, entry) -> {
                RenderControllerEntry existing = RenderControllerManager.INSTANCE.get(key);
                if (existing != null && existing.part_visibility().size() > entry.part_visibility().size()) {
                    return;
                }
                RenderControllerManager.INSTANCE.put(key, entry);
            });
        });
    }

    private static void importJson(String title, Consumer<JsonObject> action) {
        FileDialogService.selectJsonFile(title, Path.of("/"))
                         .whenComplete((path, throwable) ->
                                               path.ifPresent(selected -> {
                                                   if (!selected.toString()
                                                                .endsWith(".json")) {
                                                       return;
                                                   }

                                                   try (FileInputStream input = new FileInputStream(selected.toFile())) {
                                                       String fileContent = IOUtils.toString(input, StandardCharsets.UTF_8);
                                                       action.accept(GSON.fromJson(fileContent, JsonObject.class));
                                                   } catch (IOException e) {
                                                       throw new RuntimeException(e);
                                                   }
                                               }));
    }
}

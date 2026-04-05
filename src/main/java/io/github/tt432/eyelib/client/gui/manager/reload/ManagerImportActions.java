package io.github.tt432.eyelib.client.gui.manager.reload;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.gui.manager.io.FileDialogService;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.client.registry.RenderControllerAssetRegistry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ManagerImportActions {
    private static final Gson GSON = new Gson();

    public static void importAnimation(Logger logger) {
        importJson("读取文件", json -> AnimationAssetRegistry.publishAnimation(
                BrAnimation.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, logger::warn)));
    }

    public static void importAnimationController(Logger logger) {
        importJson("读取文件", json -> AnimationAssetRegistry.publishAnimationController(
                BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, logger::warn)));
    }

    public static void importRenderController(Logger logger) {
        importJson("读取文件", json -> RenderControllerAssetRegistry.publishRenderController(
                RenderControllers.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, logger::warn)));
    }

    private static void importJson(String title, Consumer<JsonObject> action) {
        FileDialogService.selectJsonFile(title, Path.of("/")).whenComplete((path, throwable) ->
                path.ifPresent(selected -> {
                    if (!selected.toString().endsWith(".json")) {
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

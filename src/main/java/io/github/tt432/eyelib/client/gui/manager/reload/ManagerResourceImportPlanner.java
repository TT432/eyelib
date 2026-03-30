package io.github.tt432.eyelib.client.gui.manager.reload;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.bedrock.BedrockModelLoader;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.registry.ClientAssetRegistry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import io.github.tt432.eyelib.event.TextureChangedEvent;
import io.github.tt432.eyelib.util.client.NativeImages;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ManagerResourceImportPlanner {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerResourceImportPlanner.class);
    private static final BedrockModelLoader BEDROCK_MODEL_LOADER = new BedrockModelLoader();

    public static void loadResourceFolder(Path basePath, Logger logger) {
        Map<String, BrAnimation> animations = loadJsonFiles(basePath, "animations",
                jo -> BrAnimation.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn));

        Map<String, BrAnimationControllers> animationControllers = loadJsonFiles(basePath, "animation_controllers",
                jo -> BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn));
        ClientAssetRegistry.replaceAnimationAssets(animations, animationControllers);

        Map<String, RenderControllers> renderControllers = loadJsonFiles(basePath, "render_controllers",
                jo -> RenderControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn));
        ClientAssetRegistry.replaceRenderControllers(renderControllers);

        Map<String, BrParticle> particles = loadJsonFiles(basePath, "particles",
                jo -> BrParticle.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn));
        ClientAssetRegistry.replaceParticles(particles);

        Map<String, BrClientEntity> parsedEntities = loadJsonFiles(basePath, "entity",
                jo -> BrClientEntity.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn));
        LinkedHashMap<ResourceLocation, BrClientEntity> entities = new LinkedHashMap<>();
        int index = 0;
        for (BrClientEntity entity : parsedEntities.values()) {
            entities.put(new ResourceLocation("eyelib", "manager_import_" + index++), entity);
        }
        ClientAssetRegistry.replaceClientEntities(entities);

        Map<String, Map<String, Model>> parsedModels = loadJsonFiles(basePath, "models",
                BEDROCK_MODEL_LOADER::load);
        LinkedHashMap<String, Model> models = new LinkedHashMap<>();
        parsedModels.values().forEach(models::putAll);
        ClientAssetRegistry.replaceModels(models);

        loadTextures(basePath);
    }

    public static void loadSingleFile(Path basePath, Path file, Logger logger) {
        String relative = basePath.relativize(file).toString().replace("\\", "/");

        try {
            if (relative.endsWith(".json")) {
                JsonObject jo = GSON.fromJson(IOUtils.toString(new FileInputStream(file.toFile()), StandardCharsets.UTF_8), JsonObject.class);

                if (relative.startsWith("animations/")) {
                    var animation = BrAnimation.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn);
                    ClientAssetRegistry.publishAnimation(animation);
                } else if (relative.startsWith("animation_controllers/")) {
                    var animation = BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn);
                    ClientAssetRegistry.publishAnimationController(animation);
                } else if (relative.startsWith("render_controllers/")) {
                    var controller = RenderControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn);
                    ClientAssetRegistry.publishRenderController(controller);
                } else if (relative.startsWith("entity/")) {
                    var entity = BrClientEntity.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn);
                    ClientAssetRegistry.publishClientEntity(entity);
                } else if (relative.startsWith("particles/")) {
                    var particle = BrParticle.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn);
                    ClientAssetRegistry.publishParticle(particle);
                } else if (relative.startsWith("models/")) {
                    ClientAssetRegistry.publishModels(BEDROCK_MODEL_LOADER.load(jo));
                }
            } else if (isTextureFile(relative)) {
                NativeImage nativeImage = NativeImages.loadImage(new FileInputStream(file.toFile()));
                NativeImages.uploadImage(toTextureLocation(basePath, file), nativeImage);
                MinecraftForge.EVENT_BUS.post(new TextureChangedEvent());
            }
        } catch (Exception e) {
            LOGGER.error("can't load single file.", e);
        }
    }

    static boolean isTextureFile(String relativePath) {
        return relativePath.endsWith(".png") && relativePath.startsWith("textures/");
    }

    static ResourceLocation toTextureLocation(Path basePath, Path textureFile) {
        return new ResourceLocation(textureFile.toString()
                .replace(basePath.toString(), "")
                .replace("\\", "/")
                .substring(1)
                .toLowerCase(Locale.ROOT));
    }

    private static void loadTextures(Path basePath) {
        Path texturePath = basePath.resolve("textures");
        if (!Files.exists(texturePath) || !Files.isDirectory(texturePath)) {
            return;
        }

        List<Path> pngFiles = new ArrayList<>();

        try {
            Files.walkFileTree(texturePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().endsWith(".png")) {
                        pngFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    System.err.println("无法访问文件: " + file + "，错误: " + exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("can't load files.", e);
        }

        pngFiles.forEach(pngFile -> {
            try {
                NativeImage nativeImage = NativeImages.loadImage(new FileInputStream(pngFile.toFile()));
                NativeImages.uploadImage(toTextureLocation(basePath, pngFile), nativeImage);
            } catch (IOException e) {
                LOGGER.error("can't load file.", e);
            }
        });

        if (!pngFiles.isEmpty()) {
            MinecraftForge.EVENT_BUS.post(new TextureChangedEvent());
        }
    }

    private interface JsonFileParser<T> {
        T parse(JsonObject jsonObject) throws Exception;
    }

    private static <T> LinkedHashMap<String, T> loadJsonFiles(Path basePath, String subFolder, JsonFileParser<T> parser) {
        Path subPath = basePath.resolve(subFolder);
        if (!Files.exists(subPath) || !Files.isDirectory(subPath)) {
            return new LinkedHashMap<>();
        }

        List<Path> jsonFiles = new ArrayList<>();
        LinkedHashMap<String, T> result = new LinkedHashMap<>();

        try {
            Files.walkFileTree(subPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().endsWith(".json")) {
                        jsonFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    System.err.println("无法访问文件: " + file + "，错误: " + exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("can't load files.", e);
        }

        jsonFiles.forEach(jsonFile -> {
            try {
                JsonObject json = GSON.fromJson(IOUtils.toString(new FileInputStream(jsonFile.toFile()), StandardCharsets.UTF_8), JsonObject.class);
                T parsed = parser.parse(json);
                result.put(jsonFile.toString(), parsed);
            } catch (Exception e) {
                LOGGER.error("can't load file.", e);
            }
        });

        return result;
    }
}

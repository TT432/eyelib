package io.github.tt432.eyelib.client.gui.manager.reload;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelib.client.model.importer.ModelImporter;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.client.registry.ClientEntityAssetRegistry;
import io.github.tt432.eyelib.client.registry.ModelAssetRegistry;
import io.github.tt432.eyelib.client.registry.ParticleAssetRegistry;
import io.github.tt432.eyelib.client.registry.RenderControllerAssetRegistry;
import io.github.tt432.eyelib.client.render.texture.NativeImageIO;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import io.github.tt432.eyelib.event.TextureChangedEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ManagerResourceImportPlanner {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerResourceImportPlanner.class);

    public static void loadResourceFolder(Path basePath, Logger logger) {
        Map<String, BrAnimation> animations = loadJsonFiles(basePath, "animations",
                jo -> BrAnimation.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn));

        Map<String, BrAnimationControllers> animationControllers = loadJsonFiles(basePath, "animation_controllers",
                jo -> BrAnimationControllers.fromSchemaSet(
                        BrAnimationControllerSet.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn)
                ));
        AnimationAssetRegistry.replaceAssets(animations, animationControllers);

        Map<String, RenderControllers> renderControllers = loadJsonFiles(basePath, "render_controllers",
                jo -> RenderControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn));
        RenderControllerAssetRegistry.replaceRenderControllers(renderControllers);

        Map<String, BrParticle> particles = loadJsonFiles(basePath, "particles",
                jo -> BrParticle.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn));
        ParticleAssetRegistry.replaceParticles(particles);

        Map<String, BrClientEntity> parsedEntities = loadJsonFiles(basePath, "entity",
                jo -> BrClientEntity.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn));
        ClientEntityAssetRegistry.replaceClientEntities(parsedEntities.values());

        Map<String, Map<String, Model>> parsedModels = loadModelFiles(basePath);
        LinkedHashMap<String, Model> models = new LinkedHashMap<>();
        parsedModels.values().forEach(models::putAll);
        ModelAssetRegistry.replaceModels(models);

        loadTextures(basePath);
    }

    public static void loadSingleFile(Path basePath, Path file, Logger logger) {
        ManagerResourceReloadPlan.ReloadTarget target = ManagerResourceReloadPlan.classifySingleFile(basePath, file);

        try {
            switch (target) {
                case ANIMATION_JSON,
                     ANIMATION_CONTROLLER_JSON,
                     RENDER_CONTROLLER_JSON,
                     ENTITY_JSON,
                     PARTICLE_JSON,
                     MODEL_JSON -> {
                    JsonObject jo = GSON.fromJson(IOUtils.toString(new FileInputStream(file.toFile()), StandardCharsets.UTF_8), JsonObject.class);
                    switch (target) {
                        case ANIMATION_JSON -> {
                            var animation = BrAnimation.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn);
                            AnimationAssetRegistry.publishAnimation(animation);
                        }
                        case ANIMATION_CONTROLLER_JSON -> {
                            var animation = BrAnimationControllers.fromSchemaSet(
                                    BrAnimationControllerSet.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn)
                            );
                            AnimationAssetRegistry.publishAnimationController(animation);
                        }
                        case RENDER_CONTROLLER_JSON -> {
                            var controller = RenderControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn);
                            RenderControllerAssetRegistry.publishRenderController(controller);
                        }
                        case ENTITY_JSON -> {
                            var entity = BrClientEntity.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn);
                            ClientEntityAssetRegistry.publishClientEntity(entity);
                        }
                        case PARTICLE_JSON -> {
                            var particle = BrParticle.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn);
                            ParticleAssetRegistry.publishParticle(particle);
                        }
                        case MODEL_JSON -> {
                            ModelAssetRegistry.publishModels(ModelImporter.importFile(file));
                        }
                        default -> {
                        }
                    }
                }
                case MODEL_BBMODEL -> {
                    ModelAssetRegistry.publishModels(ModelImporter.importFile(file));
                }
                case TEXTURE_PNG -> {
                    NativeImage nativeImage = NativeImageIO.load(new FileInputStream(file.toFile()));
                    NativeImageIO.upload(ManagerResourceReloadPlan.toTextureKey(basePath, file), nativeImage);
                    MinecraftForge.EVENT_BUS.post(new TextureChangedEvent());
                }
                case UNSUPPORTED -> {
                }
            }
        } catch (Exception e) {
            LOGGER.error("can't load single file.", e);
        }
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
                NativeImage nativeImage = NativeImageIO.load(new FileInputStream(pngFile.toFile()));
                NativeImageIO.upload(ManagerResourceReloadPlan.toTextureKey(basePath, pngFile), nativeImage);
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

    private static LinkedHashMap<String, Map<String, Model>> loadModelFiles(Path basePath) {
        Path subPath = basePath.resolve("models");
        if (!Files.exists(subPath) || !Files.isDirectory(subPath)) {
            return new LinkedHashMap<>();
        }

        List<Path> modelFiles = new ArrayList<>();
        LinkedHashMap<String, Map<String, Model>> result = new LinkedHashMap<>();

        try {
            Files.walkFileTree(subPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    if (fileName.endsWith(".bbmodel") || fileName.endsWith(".json")) {
                        modelFiles.add(file);
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
            LOGGER.error("can't load model files.", e);
        }

        modelFiles.forEach(modelFile -> {
            try {
                result.put(modelFile.toString(), ModelImporter.importFile(modelFile));
            } catch (Exception e) {
                LOGGER.error("can't load model file.", e);
            }
        });

        return result;
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

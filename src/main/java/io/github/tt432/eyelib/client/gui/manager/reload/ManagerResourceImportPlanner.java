package io.github.tt432.eyelib.client.gui.manager.reload;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelibprocessor.manager.reload.ManagerResourceBatchPlanner;
import io.github.tt432.eyelibprocessor.manager.reload.ManagerResourceReloadPlan;
import io.github.tt432.eyelibimporter.addon.BedrockAddon;
import io.github.tt432.eyelibimporter.addon.BedrockAddonLoader;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelibimporter.model.importer.ImportedImageData;
import io.github.tt432.eyelib.client.loader.BedrockAddonRuntimeBridge;
import io.github.tt432.eyelibmaterial.material.BrMaterial;
import io.github.tt432.eyelib.client.model.importer.ModelImporter;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.client.registry.AttachableAssetRegistry;
import io.github.tt432.eyelib.client.registry.ClientEntityAssetRegistry;
import io.github.tt432.eyelib.client.registry.MaterialAssetRegistry;
import io.github.tt432.eyelib.client.registry.ModelAssetRegistry;
import io.github.tt432.eyelib.client.registry.RenderControllerAssetRegistry;
import io.github.tt432.eyelib.client.render.texture.NativeImageIO;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import io.github.tt432.eyelibparticle.loading.ParticleResourcePublication;
import io.github.tt432.eyelib.event.TextureChangedEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ManagerResourceImportPlanner {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerResourceImportPlanner.class);

    public static boolean loadResourceFolder(Path basePath, Logger logger) {
        Optional<BedrockAddon> addon = tryLoadAddon(basePath, logger);
        if (addon.isPresent()) {
            BedrockAddon addonValue = addon.get();
            BedrockAddonRuntimeBridge.replaceFromAddon(addonValue);
            ParticleResourcePublication.replaceFromSchemas(addonValue.aggregate().resourcePack().particleFiles(), logger);
            loadAddonTextures(addonValue.aggregate().textures());
            return true;
        }

        loadLegacyResourceFolder(basePath, logger);
        return false;
    }

    private static Optional<BedrockAddon> tryLoadAddon(Path basePath, Logger logger) {
        try {
            BedrockAddon addon = BedrockAddonLoader.load(basePath);
            if (!addon.packs().isEmpty()) {
                return Optional.of(addon);
            }
        } catch (Exception exception) {
            logger.warn("can't load addon source {}, fallback to plain folder mode.", basePath, exception);
        }
        return Optional.empty();
    }

    private static void loadLegacyResourceFolder(Path basePath, Logger logger) {
        Map<String, BrAnimation> animations = ManagerResourceBatchPlanner.loadStructuredFiles(
                basePath,
                "animations",
                ".json",
                jsonFile -> parseJsonFile(jsonFile, jo -> BrAnimation.fromSchemaSet(
                        BrAnimationSet.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn)
                )),
                LOGGER
        );

        Map<String, BrAnimationControllers> animationControllers = ManagerResourceBatchPlanner.loadStructuredFiles(
                basePath,
                "animation_controllers",
                ".json",
                jsonFile -> parseJsonFile(jsonFile, jo -> BrAnimationControllers.fromSchemaSet(
                        BrAnimationControllerSet.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn)
                )),
                LOGGER
        );
        AnimationAssetRegistry.replaceAssets(animations, animationControllers);

        Map<String, RenderControllers> renderControllers = ManagerResourceBatchPlanner.loadStructuredFiles(
                basePath,
                "render_controllers",
                ".json",
                jsonFile -> parseJsonFile(jsonFile,
                        jo -> RenderControllers.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn)),
                LOGGER
        );
        RenderControllerAssetRegistry.replaceRenderControllers(renderControllers);

        Map<String, JsonObject> particles = ManagerResourceBatchPlanner.loadStructuredFiles(
                basePath,
                "particles",
                ".json",
                jsonFile -> parseJsonFile(jsonFile, jo -> jo),
                LOGGER
        );
        LinkedHashMap<String, com.google.gson.JsonElement> particleResources = new LinkedHashMap<>();
        particleResources.putAll(particles);
        ParticleResourcePublication.replaceFromJsonResources(particleResources, logger);

        Map<String, BrClientEntity> parsedEntities = ManagerResourceBatchPlanner.loadStructuredFiles(
                basePath,
                "entity",
                ".json",
                jsonFile -> parseJsonFile(jsonFile,
                        jo -> BrClientEntity.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn)),
                LOGGER
        );
        ClientEntityAssetRegistry.replaceClientEntities(parsedEntities.values());

        Map<String, BrClientEntity> parsedAttachables = ManagerResourceBatchPlanner.loadStructuredFiles(
                basePath,
                "attachables",
                ".json",
                jsonFile -> parseJsonFile(jsonFile,
                        jo -> BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn)),
                LOGGER
        );
        AttachableAssetRegistry.replaceAttachables(parsedAttachables.values());

        Map<String, Map<String, Model>> parsedModels = ManagerResourceBatchPlanner.loadModelFiles(basePath, ModelImporter::importFile, LOGGER);
        LinkedHashMap<String, Model> models = new LinkedHashMap<>();
        parsedModels.values().forEach(models::putAll);
        ModelAssetRegistry.replaceModels(models);

        Map<String, BrMaterial> parsedMaterials = ManagerResourceBatchPlanner.loadStructuredFiles(
                basePath,
                "materials",
                ".material",
                jsonFile -> parseJsonFile(jsonFile,
                        jo -> BrMaterial.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn)),
                LOGGER
        );
        MaterialAssetRegistry.replaceMaterials(parsedMaterials);

        loadTextures(basePath);
    }

    private static void loadAddonTextures(Map<String, ImportedImageData> textures) {
        if (textures.isEmpty()) {
            return;
        }

        textures.forEach((relativePath, imageData) -> {
            try {
                NativeImageIO.upload(relativePath.toLowerCase(Locale.ROOT), NativeImageIO.fromImportedImageData(imageData));
            } catch (RuntimeException exception) {
                LOGGER.error("can't upload addon texture {}.", relativePath, exception);
            }
        });

        MinecraftForge.EVENT_BUS.post(new TextureChangedEvent());
    }

    public static void loadSingleFile(Path basePath, Path file, Logger logger) {
        ManagerResourceReloadPlan.ReloadTarget target = ManagerResourceReloadPlan.classifySingleFile(basePath, file);

        try {
            switch (target) {
                case ANIMATION_JSON,
                     ANIMATION_CONTROLLER_JSON,
                     RENDER_CONTROLLER_JSON,
                     ENTITY_JSON,
                     ATTACHABLE_JSON,
                     PARTICLE_JSON,
                      MODEL_JSON -> {
                    JsonObject jo;
                    try (InputStream inputStream = Files.newInputStream(file)) {
                        jo = GSON.fromJson(IOUtils.toString(inputStream, StandardCharsets.UTF_8), JsonObject.class);
                    }
                    switch (target) {
                        case ANIMATION_JSON -> {
                            var animation = BrAnimation.fromSchemaSet(
                                    BrAnimationSet.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn)
                            );
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
                        case ATTACHABLE_JSON -> {
                            var attachable = BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn);
                            AttachableAssetRegistry.publishAttachable(attachable);
                        }
                        case PARTICLE_JSON -> {
                            ParticleResourcePublication.publishFromJsonResource(file.toString(), jo, logger);
                        }
                        case MODEL_JSON -> ModelAssetRegistry.publishModels(ModelImporter.importFile(file));
                        default -> {
                        }
                    }
                }
                case MODEL_BBMODEL -> ModelAssetRegistry.publishModels(ModelImporter.importFile(file));
                case MATERIAL_FILE -> {
                    try (InputStream inputStream = Files.newInputStream(file)) {
                        JsonObject jo = GSON.fromJson(IOUtils.toString(inputStream, StandardCharsets.UTF_8), JsonObject.class);
                        var material = BrMaterial.CODEC.parse(JsonOps.INSTANCE, jo).getOrThrow(false, logger::warn);
                        MaterialAssetRegistry.publishMaterial(material);
                    }
                }
                case TEXTURE_PNG -> {
                    try (InputStream inputStream = Files.newInputStream(file)) {
                        NativeImage nativeImage = NativeImageIO.load(inputStream);
                        NativeImageIO.upload(ManagerResourceReloadPlan.toTextureKey(basePath, file), nativeImage);
                    }
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
        List<Path> pngFiles = ManagerResourceBatchPlanner.collectTexturePngFiles(basePath, LOGGER);

        pngFiles.forEach(pngFile -> {
            try (InputStream inputStream = Files.newInputStream(pngFile)) {
                NativeImage nativeImage = NativeImageIO.load(inputStream);
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

    private static <T> T parseJsonFile(Path file, JsonFileParser<T> parser) throws Exception {
        try (InputStream inputStream = Files.newInputStream(file)) {
            JsonObject json = GSON.fromJson(IOUtils.toString(inputStream, StandardCharsets.UTF_8), JsonObject.class);
            return parser.parse(json);
        }
    }
}

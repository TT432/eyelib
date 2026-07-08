package io.github.tt432.eyelib.client.gui.manager.reload;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.bridge.client.render.texture.NativeImagePort;
import io.github.tt432.eyelib.bridge.event.TextureChangedEventPublisher;
import io.github.tt432.eyelib.bridge.util.CodecOps;
import io.github.tt432.eyelib.client.loader.BedrockAddonRuntimeBridge;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.client.manager.MaterialManager;
import io.github.tt432.eyelib.client.manager.ModelManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.registry.AnimationAssetRegistry;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.client.render.controller.RenderControllers;
import io.github.tt432.eyelib.importer.addon.BedrockAddon;
import io.github.tt432.eyelib.importer.addon.BedrockAddonLoader;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.importer.model.importer.ImportedImageData;
import io.github.tt432.eyelib.importer.model.importer.ModelImporter;
import io.github.tt432.eyelib.material.material.BrMaterial;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.particle.loading.ParticleResourcePublication;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ManagerResourceImportPlanner {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerResourceImportPlanner.class);

    public static boolean loadResourceFolder(Path basePath, Logger logger) {
        Optional<BedrockAddon> addon = tryLoadAddon(basePath, logger);
        if (addon.isPresent()) {
            BedrockAddon addonValue = addon.get();
            BedrockAddonRuntimeBridge.replaceFromAddon(addonValue);
            ParticleResourcePublication.replaceFromSchemas(addonValue.aggregate()
                                                                     .resourcePack()
                                                                     .particleFiles(), logger);
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
                        CodecOps.parseOrThrow(BrAnimationSet.CODEC, jo, logger)
                )),
                LOGGER
        );

        Map<String, BrAnimationControllers> animationControllers = ManagerResourceBatchPlanner.loadStructuredFiles(
                basePath,
                "animation_controllers",
                ".json",
                jsonFile -> parseJsonFile(jsonFile, jo -> BrAnimationControllers.fromSchemaSet(
                        CodecOps.parseOrThrow(BrAnimationControllerSet.CODEC, jo, logger)
                )),
                LOGGER
        );
        AnimationAssetRegistry.stageAnimations(animations);
        AnimationAssetRegistry.stageControllers(animationControllers);

        Map<String, RenderControllers> renderControllers = ManagerResourceBatchPlanner.loadStructuredFiles(
                basePath,
                "render_controllers",
                ".json",
                jsonFile -> parseJsonFile(jsonFile,
                                           jo -> CodecOps.parseOrThrow(RenderControllers.CODEC, jo, logger)),
                LOGGER
        );
        // 替换渲染控制器
        {
            for (RenderControllers value : renderControllers.values()) {
                value.render_controllers().forEach((key, entry) -> {
                    RenderControllerEntry existing = RenderControllerManager.INSTANCE.get(key);
                    if (existing != null && existing.part_visibility().size() > entry.part_visibility().size()) {
                        return;
                    }
                    RenderControllerManager.INSTANCE.put(key, entry);
                });
            }
        }

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
                                           jo -> CodecOps.parseOrThrow(BrClientEntity.CODEC, jo, logger)),
                LOGGER
        );
        // 替换客户端实体
        {
            LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
            parsedEntities.values().forEach(entity -> flattened.put(entity.identifier(), entity));
            ClientEntityManager.INSTANCE.replaceAll(flattened);
        }

        Map<String, BrClientEntity> parsedAttachables = ManagerResourceBatchPlanner.loadStructuredFiles(
                basePath,
                "attachables",
                ".json",
                jsonFile -> parseJsonFile(jsonFile,
                                           jo -> CodecOps.parseOrThrow(BrClientEntity.ATTACHABLE_CODEC, jo, logger)),
                LOGGER
        );
        // 替换附着物
        {
            LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
            parsedAttachables.values().forEach(attachable -> flattened.put(attachable.identifier(), attachable));
            AttachableManager.INSTANCE.replaceAll(flattened);
        }

        Map<String, Map<String, Model>> parsedModels = ManagerResourceBatchPlanner.loadModelFiles(basePath, ModelImporter::importFile, LOGGER);
        LinkedHashMap<String, Model> models = new LinkedHashMap<>();
        parsedModels.values().forEach(models::putAll);
        // 替换模型
        ModelManager.INSTANCE.replaceAll(new LinkedHashMap<>(models));

        Map<String, BrMaterial> parsedMaterials = ManagerResourceBatchPlanner.loadStructuredFiles(
                basePath,
                "materials",
                ".material",
                jsonFile -> parseJsonFile(jsonFile,
                                           jo -> CodecOps.parseOrThrow(BrMaterial.CODEC, jo, logger)),
                LOGGER
        );
        // 替换材质
        {
            LinkedHashMap<String, BrMaterialEntry> flattened = new LinkedHashMap<>();
            for (BrMaterial value : parsedMaterials.values()) {
                value.materials().forEach(flattened::put);
            }
            MaterialManager.INSTANCE.replaceAll(flattened);
        }

        loadTextures(basePath);
    }

    private static void loadAddonTextures(Map<String, ImportedImageData> textures) {
        if (textures.isEmpty()) {
            return;
        }

        textures.forEach((relativePath, imageData) -> {
            try {
                NativeImagePort.uploadFromImportedImageData(relativePath.toLowerCase(Locale.ROOT), imageData);
            } catch (RuntimeException exception) {
                LOGGER.error("can't upload addon texture {}.", relativePath, exception);
            }
        });

        TextureChangedEventPublisher.post();
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
                                    CodecOps.parseOrThrow(BrAnimationSet.CODEC, jo, logger)
                            );
                            AnimationAssetRegistry.publishAnimation(animation);
                        }
                        case ANIMATION_CONTROLLER_JSON -> {
                            var animation = BrAnimationControllers.fromSchemaSet(
                                    CodecOps.parseOrThrow(BrAnimationControllerSet.CODEC, jo, logger)
                            );
                            AnimationAssetRegistry.publishAnimationController(animation);
                        }
                        case RENDER_CONTROLLER_JSON -> {
                            var controller = CodecOps.parseOrThrow(RenderControllers.CODEC, jo, logger);
                            // publishRenderController
                            controller.render_controllers().forEach((key, entry) -> {
                                RenderControllerEntry existing = RenderControllerManager.INSTANCE.get(key);
                                if (existing != null && existing.part_visibility().size() > entry.part_visibility().size()) {
                                    return;
                                }
                                RenderControllerManager.INSTANCE.put(key, entry);
                            });
                        }
                        case ENTITY_JSON -> {
                            var entity = CodecOps.parseOrThrow(BrClientEntity.CODEC, jo, logger);
                            ClientEntityManager.INSTANCE.put(entity.identifier(), entity);
                        }
                        case ATTACHABLE_JSON -> {
                            var attachable = CodecOps.parseOrThrow(BrClientEntity.ATTACHABLE_CODEC, jo, logger);
                            AttachableManager.INSTANCE.put(attachable.identifier(), attachable);
                        }
                        case PARTICLE_JSON -> {
                            ParticleResourcePublication.publishFromJsonResource(file.toString(), jo, logger);
                        }
                        case MODEL_JSON -> ModelManager.INSTANCE.replaceAll(ModelImporter.importFile(file));
                        default -> {
                        }
                    }
                }
                case MODEL_BBMODEL -> ModelManager.INSTANCE.replaceAll(ModelImporter.importFile(file));
                case MATERIAL_FILE -> {
                    try (InputStream inputStream = Files.newInputStream(file)) {
                        JsonObject jo = GSON.fromJson(IOUtils.toString(inputStream, StandardCharsets.UTF_8), JsonObject.class);
                        var material = CodecOps.parseOrThrow(BrMaterial.CODEC, jo, logger);
                        material.materials().forEach(MaterialManager.INSTANCE::put);
                    }
                }
                case TEXTURE_PNG -> {
                    try (InputStream inputStream = Files.newInputStream(file)) {
                        NativeImagePort.loadAndUpload(ManagerResourceReloadPlan.toTextureKey(basePath, file), inputStream);
                    }
                    TextureChangedEventPublisher.post();
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
                NativeImagePort.loadAndUpload(ManagerResourceReloadPlan.toTextureKey(basePath, pngFile), inputStream);
            } catch (IOException e) {
                LOGGER.error("can't load file.", e);
            }
        });

        if (!pngFiles.isEmpty()) {
            TextureChangedEventPublisher.post();
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


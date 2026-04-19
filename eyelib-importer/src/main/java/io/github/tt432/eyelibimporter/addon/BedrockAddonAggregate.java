package io.github.tt432.eyelibimporter.addon;

import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibimporter.material.BrMaterialEntry;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllerEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BedrockAddonAggregate(
        BedrockAddonSideAggregate resourcePack,
        BedrockAddonSideAggregate behaviorPack
) {
    public static BedrockAddonAggregate fromPacks(List<BedrockAddonPack> packs, List<BedrockAddonWarning> warnings) {
        return new BedrockAddonAggregate(
                fromSidePacks(packs.stream().filter(BedrockAddonPack::isResourcePack).toList(), warnings),
                fromSidePacks(packs.stream().filter(BedrockAddonPack::isDataPack).toList(), warnings)
        );
    }

    private static BedrockAddonSideAggregate fromSidePacks(List<BedrockAddonPack> packs, List<BedrockAddonWarning> warnings) {
        var animations = new java.util.LinkedHashMap<String, BrAnimationEntrySchema>();
        var animationControllers = new java.util.LinkedHashMap<String, BrAnimationControllerSchema>();
        var clientEntities = new java.util.LinkedHashMap<String, BrClientEntity>();
        var attachables = new java.util.LinkedHashMap<String, BrClientEntity>();
        var behaviorEntities = new java.util.LinkedHashMap<String, BrBehaviorEntityFile>();
        var models = new java.util.LinkedHashMap<String, Model>();
        var textures = new java.util.LinkedHashMap<String, io.github.tt432.eyelibimporter.model.importer.ImportedImageData>();
        var soundIndexFiles = new java.util.LinkedHashMap<String, BrSoundIndex>();
        var soundDefinitionFiles = new java.util.LinkedHashMap<String, BrSoundDefinitions>();
        var languageFiles = new java.util.LinkedHashMap<String, BrLanguageFile>();
        var soundFiles = new java.util.LinkedHashMap<String, BedrockBinaryAsset>();
        var textureIndexFiles = new java.util.LinkedHashMap<String, BrTextureIndexFile>();
        var textureMetadataFiles = new java.util.LinkedHashMap<String, BrTextureMetadataFile>();
        var renderControllerFiles = new java.util.LinkedHashMap<String, io.github.tt432.eyelibimporter.render.controller.BrRenderControllers>();
        var particleFiles = new java.util.LinkedHashMap<String, BrParticle>();
        var materialFiles = new java.util.LinkedHashMap<String, io.github.tt432.eyelibimporter.material.BrMaterial>();

        for (BedrockAddonPack pack : packs) {
            mergeWithWarnings(animations, flattenAnimations(pack.animationFiles()), pack.sourceName(), warnings, BedrockResourceFamily.ANIMATION);
            mergeWithWarnings(animationControllers, flattenAnimationControllers(pack.animationControllerFiles()), pack.sourceName(), warnings, BedrockResourceFamily.ANIMATION_CONTROLLER);
            mergeWithWarnings(clientEntities, flattenClientEntities(pack.clientEntityFiles()), pack.sourceName(), warnings, BedrockResourceFamily.CLIENT_ENTITY);
            mergeWithWarnings(attachables, flattenClientEntities(pack.attachableFiles()), pack.sourceName(), warnings, BedrockResourceFamily.ATTACHABLE);
            mergeWithWarnings(behaviorEntities, flattenBehaviorEntities(pack.behaviorEntityFiles()), pack.sourceName(), warnings, BedrockResourceFamily.BEHAVIOR_ENTITY);
            mergeWithWarnings(models, flattenModels(pack.modelFiles()), pack.sourceName(), warnings, BedrockResourceFamily.MODEL);
            textures.putAll(pack.textures());
            soundIndexFiles.putAll(pack.soundIndexFiles());
            soundDefinitionFiles.putAll(pack.soundDefinitionFiles());
            languageFiles.putAll(pack.languageFiles());
            soundFiles.putAll(pack.soundFiles());
            textureIndexFiles.putAll(pack.textureIndexFiles());
            textureMetadataFiles.putAll(pack.textureMetadataFiles());
            renderControllerFiles.putAll(pack.renderControllerFiles());
            particleFiles.putAll(pack.particleFiles());
            materialFiles.putAll(pack.materialFiles());
        }

        return new BedrockAddonSideAggregate(
                animations,
                animationControllers,
                clientEntities,
                attachables,
                behaviorEntities,
                models,
                textures,
                soundIndexFiles,
                soundDefinitionFiles,
                languageFiles,
                soundFiles,
                textureIndexFiles,
                textureMetadataFiles,
                renderControllerFiles,
                particleFiles,
                materialFiles
        );
    }

    private static <T> void mergeWithWarnings(Map<String, T> target, Map<String, T> incoming, String packSource,
                                              List<BedrockAddonWarning> warnings, BedrockResourceFamily family) {
        incoming.forEach((key, value) -> {
            if (target.containsKey(key)) {
                warnings.add(new BedrockAddonWarning(
                        BedrockAddonWarningSeverity.WARNING,
                        BedrockAddonWarningCode.DUPLICATE_OVERRIDE,
                        packSource,
                        key,
                        "Flattened resource key was overridden for family " + family + ": " + key
                ));
            }
            target.put(key, value);
        });
    }

    private static Map<String, BrAnimationEntrySchema> flattenAnimations(Map<String, BrAnimationSet> animationFiles) {
        LinkedHashMap<String, BrAnimationEntrySchema> flattened = new LinkedHashMap<>();
        animationFiles.forEach((path, set) -> flattened.putAll(set.animations()));
        return flattened;
    }

    private static Map<String, BrAnimationControllerSchema> flattenAnimationControllers(Map<String, BrAnimationControllerSet> animationControllerFiles) {
        LinkedHashMap<String, BrAnimationControllerSchema> flattened = new LinkedHashMap<>();
        animationControllerFiles.forEach((path, set) -> flattened.putAll(set.animationControllers()));
        return flattened;
    }

    private static Map<String, BrClientEntity> flattenClientEntities(Map<String, BrClientEntity> clientEntityFiles) {
        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        clientEntityFiles.forEach((path, entity) -> flattened.put(entity.identifier(), entity));
        return flattened;
    }

    private static Map<String, BrBehaviorEntityFile> flattenBehaviorEntities(Map<String, BrBehaviorEntityFile> behaviorEntityFiles) {
        LinkedHashMap<String, BrBehaviorEntityFile> flattened = new LinkedHashMap<>();
        behaviorEntityFiles.forEach((path, entity) -> flattened.put(entity.identifier(), entity));
        return flattened;
    }

    private static Map<String, Model> flattenModels(Map<String, BedrockImportedModels> modelFiles) {
        LinkedHashMap<String, Model> flattened = new LinkedHashMap<>();
        modelFiles.forEach((path, models) -> flattened.putAll(models.models()));
        return flattened;
    }

    public Map<String, BrAnimationEntrySchema> animations() {
        return resourcePack.animations();
    }

    public Map<String, BrAnimationControllerSchema> animationControllers() {
        return resourcePack.animationControllers();
    }

    public Map<String, BrClientEntity> clientEntities() {
        return resourcePack.clientEntities();
    }

    public Map<String, BrClientEntity> attachables() {
        return resourcePack.attachables();
    }

    public Map<String, BrBehaviorEntityFile> behaviorEntities() {
        return behaviorPack.behaviorEntities();
    }

    public Map<String, Model> models() {
        return resourcePack.models();
    }

    public Map<String, io.github.tt432.eyelibimporter.model.importer.ImportedImageData> textures() {
        return resourcePack.textures();
    }

    public Map<String, BrSoundIndex> soundIndexFiles() {
        return resourcePack.soundIndexFiles();
    }

    public Map<String, BrSoundDefinitions> soundDefinitionFiles() {
        return resourcePack.soundDefinitionFiles();
    }

    public Map<String, BrLanguageFile> languageFiles() {
        return resourcePack.languageFiles();
    }

    public Map<String, BedrockBinaryAsset> soundFiles() {
        return resourcePack.soundFiles();
    }

    public Map<String, BrTextureIndexFile> textureIndexFiles() {
        return resourcePack.textureIndexFiles();
    }

    public Map<String, BrTextureMetadataFile> textureMetadataFiles() {
        return resourcePack.textureMetadataFiles();
    }

    public Map<String, io.github.tt432.eyelibimporter.render.controller.BrRenderControllers> renderControllerFiles() {
        return resourcePack.renderControllerFiles();
    }

    public Map<String, BrParticle> particleFiles() {
        return resourcePack.particleFiles();
    }

    public Map<String, io.github.tt432.eyelibimporter.material.BrMaterial> materialFiles() {
        return resourcePack.materialFiles();
    }

    public Map<String, Model> modelsView() {
        return resourcePack.modelsView();
    }

    public Map<String, BrRenderControllerEntry> flattenedRenderControllers() {
        return resourcePack.flattenedRenderControllers();
    }

    public Map<String, BrParticle> particlesByIdentifier() {
        return resourcePack.particlesByIdentifier();
    }

    public Map<String, BrMaterialEntry> flattenedMaterialEntries() {
        return resourcePack.flattenedMaterialEntries();
    }
}

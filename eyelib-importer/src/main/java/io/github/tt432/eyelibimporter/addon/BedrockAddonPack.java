package io.github.tt432.eyelibimporter.addon;

import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibimporter.material.BrMaterial;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelibimporter.model.importer.ImportedImageData;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllerEntry;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllers;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public record BedrockAddonPack(
        String sourceName,
        BedrockPackManifest manifest,
        LinkedHashMap<String, BrAnimationSet> animationFiles,
        LinkedHashMap<String, BrAnimationControllerSet> animationControllerFiles,
        LinkedHashMap<String, BrClientEntity> clientEntityFiles,
        LinkedHashMap<String, BrClientEntity> attachableFiles,
        LinkedHashMap<String, BedrockImportedModels> modelFiles,
        LinkedHashMap<String, ImportedImageData> textures,
        LinkedHashMap<String, BrSoundIndex> soundIndexFiles,
        LinkedHashMap<String, BrSoundDefinitions> soundDefinitionFiles,
        LinkedHashMap<String, BrLanguageFile> languageFiles,
        LinkedHashMap<String, BrBehaviorEntityFile> behaviorEntityFiles,
        LinkedHashMap<String, BedrockBinaryAsset> soundFiles,
        LinkedHashMap<String, BrRenderControllers> renderControllerFiles,
        LinkedHashMap<String, BrParticle> particleFiles,
        LinkedHashMap<String, BrMaterial> materialFiles,
        LinkedHashMap<String, BedrockUnmanagedResource> unmanagedResources,
        java.util.List<BedrockAddonWarning> warnings,
        @Nullable ImportedImageData packIcon
) {
    public BedrockAddonPack {
        animationFiles = new LinkedHashMap<>(animationFiles);
        animationControllerFiles = new LinkedHashMap<>(animationControllerFiles);
        clientEntityFiles = new LinkedHashMap<>(clientEntityFiles);
        attachableFiles = new LinkedHashMap<>(attachableFiles);
        modelFiles = new LinkedHashMap<>(modelFiles);
        textures = new LinkedHashMap<>(textures);
        soundIndexFiles = new LinkedHashMap<>(soundIndexFiles);
        soundDefinitionFiles = new LinkedHashMap<>(soundDefinitionFiles);
        languageFiles = new LinkedHashMap<>(languageFiles);
        behaviorEntityFiles = new LinkedHashMap<>(behaviorEntityFiles);
        soundFiles = new LinkedHashMap<>(soundFiles);
        renderControllerFiles = new LinkedHashMap<>(renderControllerFiles);
        particleFiles = new LinkedHashMap<>(particleFiles);
        materialFiles = new LinkedHashMap<>(materialFiles);
        unmanagedResources = new LinkedHashMap<>(unmanagedResources);
        warnings = java.util.List.copyOf(warnings);
    }

    public boolean isResourcePack() {
        return manifest.isResourcePack();
    }

    public boolean isDataPack() {
        return manifest.isDataPack();
    }

    public Map<String, Model> modelsView() {
        LinkedHashMap<String, Model> flattened = new LinkedHashMap<>();
        modelFiles.forEach((path, models) -> flattened.putAll(models.models()));
        return Map.copyOf(flattened);
    }

    public Map<String, BrAnimationEntrySchema> animations() {
        LinkedHashMap<String, BrAnimationEntrySchema> flattened = new LinkedHashMap<>();
        animationFiles.forEach((path, set) -> flattened.putAll(set.animations()));
        return Map.copyOf(flattened);
    }

    public Map<String, BrAnimationControllerSchema> animationControllers() {
        LinkedHashMap<String, BrAnimationControllerSchema> flattened = new LinkedHashMap<>();
        animationControllerFiles.forEach((path, set) -> flattened.putAll(set.animationControllers()));
        return Map.copyOf(flattened);
    }

    public Map<String, BrClientEntity> clientEntities() {
        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        clientEntityFiles.forEach((path, entity) -> flattened.put(entity.identifier(), entity));
        return Map.copyOf(flattened);
    }

    public Map<String, BrClientEntity> attachables() {
        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        attachableFiles.forEach((path, attachable) -> flattened.put(attachable.identifier(), attachable));
        return Map.copyOf(flattened);
    }

    public Map<String, BrBehaviorEntityFile> behaviorEntities() {
        LinkedHashMap<String, BrBehaviorEntityFile> flattened = new LinkedHashMap<>();
        behaviorEntityFiles.forEach((path, entity) -> flattened.put(entity.identifier(), entity));
        return Map.copyOf(flattened);
    }

    public Map<String, BrRenderControllerEntry> flattenedRenderControllers() {
        LinkedHashMap<String, BrRenderControllerEntry> flattened = new LinkedHashMap<>();
        renderControllerFiles.forEach((path, controllers) -> flattened.putAll(controllers.renderControllers()));
        return Map.copyOf(flattened);
    }

    public Map<String, BrParticle> particlesByIdentifier() {
        LinkedHashMap<String, BrParticle> flattened = new LinkedHashMap<>();
        particleFiles.forEach((path, particle) -> flattened.put(particle.particleEffect().description().identifier(), particle));
        return Map.copyOf(flattened);
    }

    public Map<String, io.github.tt432.eyelibimporter.material.BrMaterialEntry> flattenedMaterialEntries() {
        LinkedHashMap<String, io.github.tt432.eyelibimporter.material.BrMaterialEntry> flattened = new LinkedHashMap<>();
        materialFiles.forEach((path, material) -> flattened.putAll(material.materials()));
        return Map.copyOf(flattened);
    }
}

package io.github.tt432.eyelibimporter.addon;

import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibimporter.material.BrMaterial;
import io.github.tt432.eyelibimporter.material.BrMaterialEntry;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelibimporter.model.importer.ImportedImageData;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllerEntry;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BedrockAddon(
        List<BedrockAddonPack> packs,
        java.util.List<BedrockAddonWarning> warnings,
        LinkedHashMap<String, BedrockUnmanagedResource> unmanagedResources,
        LinkedHashMap<String, BrAnimationEntrySchema> animations,
        LinkedHashMap<String, BrAnimationControllerSchema> animationControllers,
        LinkedHashMap<String, BrClientEntity> clientEntities,
        LinkedHashMap<String, BrClientEntity> attachables,
        LinkedHashMap<String, BrBehaviorEntityFile> behaviorEntities,
        LinkedHashMap<String, Model> models,
        LinkedHashMap<String, ImportedImageData> textures,
        LinkedHashMap<String, BrSoundIndex> soundIndexFiles,
        LinkedHashMap<String, BrSoundDefinitions> soundDefinitionFiles,
        LinkedHashMap<String, BrLanguageFile> languageFiles,
        LinkedHashMap<String, BedrockBinaryAsset> soundFiles,
        LinkedHashMap<String, BrRenderControllers> renderControllerFiles,
        LinkedHashMap<String, BrParticle> particleFiles,
        LinkedHashMap<String, BrMaterial> materialFiles
) {
    public BedrockAddon {
        packs = List.copyOf(packs);
        warnings = java.util.List.copyOf(warnings);
        unmanagedResources = new LinkedHashMap<>(unmanagedResources);
        animations = new LinkedHashMap<>(animations);
        animationControllers = new LinkedHashMap<>(animationControllers);
        clientEntities = new LinkedHashMap<>(clientEntities);
        attachables = new LinkedHashMap<>(attachables);
        behaviorEntities = new LinkedHashMap<>(behaviorEntities);
        models = new LinkedHashMap<>(models);
        textures = new LinkedHashMap<>(textures);
        soundIndexFiles = new LinkedHashMap<>(soundIndexFiles);
        soundDefinitionFiles = new LinkedHashMap<>(soundDefinitionFiles);
        languageFiles = new LinkedHashMap<>(languageFiles);
        soundFiles = new LinkedHashMap<>(soundFiles);
        renderControllerFiles = new LinkedHashMap<>(renderControllerFiles);
        particleFiles = new LinkedHashMap<>(particleFiles);
        materialFiles = new LinkedHashMap<>(materialFiles);
    }

    public List<BedrockAddonPack> resourcePacks() {
        return packs.stream().filter(BedrockAddonPack::isResourcePack).toList();
    }

    public List<BedrockAddonPack> dataPacks() {
        return packs.stream().filter(BedrockAddonPack::isDataPack).toList();
    }

    public Map<String, Model> modelsView() {
        return Map.copyOf(models);
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

    public Map<String, BrMaterialEntry> flattenedMaterialEntries() {
        LinkedHashMap<String, BrMaterialEntry> flattened = new LinkedHashMap<>();
        materialFiles.forEach((path, material) -> flattened.putAll(material.materials()));
        return Map.copyOf(flattened);
    }
}

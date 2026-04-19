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

public record BedrockAddonAggregate(
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
        LinkedHashMap<String, BrTextureIndexFile> textureIndexFiles,
        LinkedHashMap<String, BrTextureMetadataFile> textureMetadataFiles,
        LinkedHashMap<String, BrRenderControllers> renderControllerFiles,
        LinkedHashMap<String, BrParticle> particleFiles,
        LinkedHashMap<String, BrMaterial> materialFiles
) {
    public BedrockAddonAggregate {
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
        textureIndexFiles = new LinkedHashMap<>(textureIndexFiles);
        textureMetadataFiles = new LinkedHashMap<>(textureMetadataFiles);
        renderControllerFiles = new LinkedHashMap<>(renderControllerFiles);
        particleFiles = new LinkedHashMap<>(particleFiles);
        materialFiles = new LinkedHashMap<>(materialFiles);
    }

    public static BedrockAddonAggregate fromPacks(List<BedrockAddonPack> packs, List<BedrockAddonWarning> warnings) {
        LinkedHashMap<String, BrAnimationEntrySchema> animations = new LinkedHashMap<>();
        LinkedHashMap<String, BrAnimationControllerSchema> animationControllers = new LinkedHashMap<>();
        LinkedHashMap<String, BrClientEntity> clientEntities = new LinkedHashMap<>();
        LinkedHashMap<String, BrClientEntity> attachables = new LinkedHashMap<>();
        LinkedHashMap<String, BrBehaviorEntityFile> behaviorEntities = new LinkedHashMap<>();
        LinkedHashMap<String, Model> models = new LinkedHashMap<>();
        LinkedHashMap<String, ImportedImageData> textures = new LinkedHashMap<>();
        LinkedHashMap<String, BrSoundIndex> soundIndexFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrSoundDefinitions> soundDefinitionFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrLanguageFile> languageFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BedrockBinaryAsset> soundFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrTextureIndexFile> textureIndexFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrTextureMetadataFile> textureMetadataFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrRenderControllers> renderControllerFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrParticle> particleFiles = new LinkedHashMap<>();
        LinkedHashMap<String, BrMaterial> materialFiles = new LinkedHashMap<>();

        for (BedrockAddonPack pack : packs) {
            mergeWithWarnings(animations, pack.animations(), pack.sourceName(), warnings, BedrockResourceFamily.ANIMATION);
            mergeWithWarnings(animationControllers, pack.animationControllers(), pack.sourceName(), warnings, BedrockResourceFamily.ANIMATION_CONTROLLER);
            mergeWithWarnings(clientEntities, pack.clientEntities(), pack.sourceName(), warnings, BedrockResourceFamily.CLIENT_ENTITY);
            mergeWithWarnings(attachables, pack.attachables(), pack.sourceName(), warnings, BedrockResourceFamily.ATTACHABLE);
            mergeWithWarnings(behaviorEntities, pack.behaviorEntities(), pack.sourceName(), warnings, BedrockResourceFamily.BEHAVIOR_ENTITY);
            mergeWithWarnings(models, pack.modelsView(), pack.sourceName(), warnings, BedrockResourceFamily.MODEL);
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

        return new BedrockAddonAggregate(
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

package io.github.tt432.eyelibimporter.addon;

import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSet;
import io.github.tt432.eyelibimporter.block.BrBlock;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibimporter.item.BrItem;
import io.github.tt432.eyelibimporter.material.BrMaterial;
import io.github.tt432.eyelibimporter.model.importer.ImportedImageData;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import io.github.tt432.eyelibimporter.recipe.BrRecipe;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllers;
import io.github.tt432.eyelibimporter.trading.BrTrading;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;

/** @author TT432 */
@NullMarked
public record BedrockAddonPack(
        String sourceName,
        BedrockPackManifest manifest,
        @Nullable String selectedSubpack,
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
        LinkedHashMap<String, BrTextureIndexFile> textureIndexFiles,
        LinkedHashMap<String, BrTextureMetadataFile> textureMetadataFiles,
        LinkedHashMap<String, BrRenderControllers> renderControllerFiles,
        LinkedHashMap<String, BrParticle> particleFiles,
        LinkedHashMap<String, BrMaterial> materialFiles,
        LinkedHashMap<String, BrSpawnRule> spawnRulesFiles,
        LinkedHashMap<String, BrLootTable> lootTableFiles,
        LinkedHashMap<String, BrItem> itemFiles,
        LinkedHashMap<String, BrBlock> blockFiles,
        LinkedHashMap<String, BrRecipe> recipeFiles,
        LinkedHashMap<String, BrTrading> tradeFiles,
        LinkedHashMap<String, BedrockUnmanagedResource> unmanagedResources,
        java.util.List<BedrockAddonWarning> warnings,
        @Nullable ImportedImageData packIcon,
        @Nullable BedrockResourceValue splashIndex
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
        textureIndexFiles = new LinkedHashMap<>(textureIndexFiles);
        textureMetadataFiles = new LinkedHashMap<>(textureMetadataFiles);
        renderControllerFiles = new LinkedHashMap<>(renderControllerFiles);
        particleFiles = new LinkedHashMap<>(particleFiles);
        materialFiles = new LinkedHashMap<>(materialFiles);
        spawnRulesFiles = new LinkedHashMap<>(spawnRulesFiles);
        lootTableFiles = new LinkedHashMap<>(lootTableFiles);
        itemFiles = new LinkedHashMap<>(itemFiles);
        blockFiles = new LinkedHashMap<>(blockFiles);
        recipeFiles = new LinkedHashMap<>(recipeFiles);
        tradeFiles = new LinkedHashMap<>(tradeFiles);
        unmanagedResources = new LinkedHashMap<>(unmanagedResources);
        warnings = java.util.List.copyOf(warnings);
    }

    public boolean isResourcePack() {
        return manifest.isResourcePack();
    }

    public boolean isDataPack() {
        return manifest.isDataPack();
    }
}

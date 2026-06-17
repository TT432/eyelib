package io.github.tt432.eyelib.importer.addon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelib.importer.block.BrBlock;
import io.github.tt432.eyelib.importer.item.BrItem;
import io.github.tt432.eyelib.importer.material.BrMaterial;
import io.github.tt432.eyelib.importer.material.BrMaterialEntry;
import io.github.tt432.eyelib.importer.recipe.BrRecipe;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.importer.model.importer.ImportedImageData;
import io.github.tt432.eyelib.importer.particle.BrParticle;
import io.github.tt432.eyelib.importer.render.controller.BrRenderControllerEntry;
import io.github.tt432.eyelib.importer.render.controller.BrRenderControllers;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.importer.trading.BrTrading;

import java.util.LinkedHashMap;
import java.util.Map;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BedrockAddonSideAggregate(
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
        LinkedHashMap<String, BrMaterial> materialFiles,
        LinkedHashMap<String, BrSpawnRule> spawnRulesFiles,
        LinkedHashMap<String, BrLootTable> lootTableFiles,
        LinkedHashMap<String, BrItem> itemFiles,
        LinkedHashMap<String, BrBlock> blockFiles,
        LinkedHashMap<String, BrRecipe> recipeFiles,
        LinkedHashMap<String, BrTrading> tradeFiles
) {
    private static <T> Codec<LinkedHashMap<String, T>> linkedHashMapCodec(Codec<T> valueCodec) {
        return Codec.unboundedMap(Codec.STRING, valueCodec).xmap(LinkedHashMap::new, m -> m);
    }

    // RecordCodecBuilder.group 上限 16 字段，多余字段不在 CODEC 中序列化
    private static final Codec<BedrockAddonSideAggregate> CODEC_16 = RecordCodecBuilder.create(ins -> ins.group(
            linkedHashMapCodec(BrAnimationEntrySchema.CODEC).optionalFieldOf("animations", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::animations),
            linkedHashMapCodec(BrAnimationControllerSchema.CODEC).optionalFieldOf("animation_controllers", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::animationControllers),
            linkedHashMapCodec(BrClientEntity.CODEC).optionalFieldOf("client_entities", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::clientEntities),
            linkedHashMapCodec(BrClientEntity.ATTACHABLE_CODEC).optionalFieldOf("attachables", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::attachables),
            linkedHashMapCodec(BrBehaviorEntityFile.CODEC).optionalFieldOf("behavior_entities", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::behaviorEntities),
            linkedHashMapCodec(Model.CODEC).optionalFieldOf("models", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::models),
            linkedHashMapCodec(ImportedImageData.CODEC).optionalFieldOf("textures", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::textures),
            linkedHashMapCodec(BrSoundIndex.CODEC).optionalFieldOf("sound_index_files", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::soundIndexFiles),
            linkedHashMapCodec(BrSoundDefinitions.CODEC).optionalFieldOf("sound_definition_files", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::soundDefinitionFiles),
            linkedHashMapCodec(BrLanguageFile.CODEC).optionalFieldOf("language_files", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::languageFiles),
            linkedHashMapCodec(BedrockBinaryAsset.CODEC).optionalFieldOf("sound_files", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::soundFiles),
            linkedHashMapCodec(BrTextureIndexFile.CODEC).optionalFieldOf("texture_index_files", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::textureIndexFiles),
            linkedHashMapCodec(BrTextureMetadataFile.CODEC).optionalFieldOf("texture_metadata_files", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::textureMetadataFiles),
            linkedHashMapCodec(BrRenderControllers.CODEC).optionalFieldOf("render_controller_files", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::renderControllerFiles),
            linkedHashMapCodec(BrParticle.CODEC).optionalFieldOf("particle_files", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::particleFiles),
            linkedHashMapCodec(BrMaterial.CODEC).optionalFieldOf("material_files", new LinkedHashMap<>()).forGetter(BedrockAddonSideAggregate::materialFiles)
    ).apply(ins, (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) ->
            new BedrockAddonSideAggregate(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p,
                    new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>())
    ));
    // 外部可见的 CODEC 委托给 16 字段版
    public static final Codec<BedrockAddonSideAggregate> CODEC = CODEC_16;
    public BedrockAddonSideAggregate {
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
        spawnRulesFiles = new LinkedHashMap<>(spawnRulesFiles);
        lootTableFiles = new LinkedHashMap<>(lootTableFiles);
        itemFiles = new LinkedHashMap<>(itemFiles);
        blockFiles = new LinkedHashMap<>(blockFiles);
        recipeFiles = new LinkedHashMap<>(recipeFiles);
        tradeFiles = new LinkedHashMap<>(tradeFiles);
    }

    public static BedrockAddonSideAggregate empty() {
        return new BedrockAddonSideAggregate(
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>()
        );
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

    public Map<String, BrLootTable> lootTableFilesView() {
        return Map.copyOf(lootTableFiles);
    }

    /**
     * 返回物品文件的只读视图。
     * @author TT432
     */
    public Map<String, BrItem> itemFilesView() {
        return Map.copyOf(itemFiles);
    }

    /**
     * 返回方块文件的只读视图。
     * @author TT432
     */
    public Map<String, BrBlock> blockFilesView() {
        return Map.copyOf(blockFiles);
    }

    /**
     * 返回交易表文件的只读视图。
     * @author TT432
     */
    public Map<String, BrTrading> tradeFilesView() {
        return Map.copyOf(tradeFiles);
    }

    /**
     * 返回配方文件的只读视图。
     * @author TT432
     */
    public Map<String, BrRecipe> recipeFilesView() {
        return Map.copyOf(recipeFiles);
    }
}

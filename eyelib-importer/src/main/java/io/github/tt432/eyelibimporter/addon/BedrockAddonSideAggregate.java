package io.github.tt432.eyelibimporter.addon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelibimporter.material.BrMaterial;
import io.github.tt432.eyelibimporter.material.BrMaterialEntry;
import io.github.tt432.eyelibmodel.Model;
import io.github.tt432.eyelibimporter.model.importer.ImportedImageData;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllerEntry;
import io.github.tt432.eyelibimporter.render.controller.BrRenderControllers;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;

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
        LinkedHashMap<String, BrMaterial> materialFiles
) {
    private static <T> Codec<LinkedHashMap<String, T>> linkedHashMapCodec(Codec<T> valueCodec) {
        return Codec.unboundedMap(Codec.STRING, valueCodec).xmap(LinkedHashMap::new, m -> m);
    }

    public static final Codec<BedrockAddonSideAggregate> CODEC = RecordCodecBuilder.create(ins -> ins.group(
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
    ).apply(ins, BedrockAddonSideAggregate::new));
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
}

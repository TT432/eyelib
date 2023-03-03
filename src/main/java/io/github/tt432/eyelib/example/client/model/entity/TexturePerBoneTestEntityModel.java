package io.github.tt432.eyelib.example.client.model.entity;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;
import io.github.tt432.eyelib.example.client.EntityResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class TexturePerBoneTestEntityModel<T extends LivingEntity & Animatable> extends AnimatedGeoModel<T> {

    protected final ResourceLocation MODEL_RESLOC;
    protected final ResourceLocation TEXTURE_DEFAULT;
    protected final String ENTITY_REGISTRY_PATH_NAME;

    public TexturePerBoneTestEntityModel(ResourceLocation model, ResourceLocation textureDefault, String entityName) {
        super();
        this.MODEL_RESLOC = model;
        this.TEXTURE_DEFAULT = textureDefault;
        this.ENTITY_REGISTRY_PATH_NAME = entityName;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(T animatable) {
        return EntityResources.TEXTUREPERBONE_ANIMATIONS;
    }

    @Override
    public ResourceLocation getModelLocation(T object) {
        return MODEL_RESLOC;
    }

    @Override
    public ResourceLocation getTextureLocation(T object) {
        return TEXTURE_DEFAULT;
    }

}

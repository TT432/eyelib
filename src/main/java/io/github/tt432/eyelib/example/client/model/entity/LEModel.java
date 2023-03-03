package io.github.tt432.eyelib.example.client.model.entity;

import io.github.tt432.eyelib.common.bedrock.model.AnimatedTickingGeoModel;
import io.github.tt432.eyelib.example.client.EntityResources;
import io.github.tt432.eyelib.example.entity.LEEntity;
import net.minecraft.resources.ResourceLocation;

public class LEModel extends AnimatedTickingGeoModel<LEEntity> {

    @Override
    public ResourceLocation getModelLocation(LEEntity object) {
        return EntityResources.LAYER_EXAMPLE_MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(LEEntity object) {
        return EntityResources.LAYER_EXAMPLE_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(LEEntity animatable) {
        return EntityResources.LAYER_EXAMPLE_ANIMATIONS;
    }

}
package io.github.tt432.eyelib.example.client.model.entity;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;
import io.github.tt432.eyelib.example.client.EntityResources;
import net.minecraft.resources.ResourceLocation;

public class ReplacedCreeperModel extends AnimatedGeoModel {
    @Override
    public ResourceLocation getModelLocation(Object object) {
        return EntityResources.CREEPER_MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(Object object) {
        return EntityResources.CREEPER_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(Animatable animatable) {
        return EntityResources.CREEPER_ANIMATIONS;
    }
}
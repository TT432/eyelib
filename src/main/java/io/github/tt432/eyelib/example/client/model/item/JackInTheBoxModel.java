package io.github.tt432.eyelib.example.client.model.item;

import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;
import io.github.tt432.eyelib.example.client.EntityResources;
import io.github.tt432.eyelib.example.item.JackInTheBoxItem;
import net.minecraft.resources.ResourceLocation;

public class JackInTheBoxModel extends AnimatedGeoModel<JackInTheBoxItem> {
    @Override
    public ResourceLocation getModelLocation(JackInTheBoxItem object) {
        return EntityResources.JACKINTHEBOX_MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(JackInTheBoxItem object) {
        return EntityResources.JACKINTHEBOX_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(JackInTheBoxItem animatable) {
        return EntityResources.JACKINTHEBOX_ANIMATIONS;
    }
}

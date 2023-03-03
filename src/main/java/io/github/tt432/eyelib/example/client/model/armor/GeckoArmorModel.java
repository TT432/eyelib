package io.github.tt432.eyelib.example.client.model.armor;

import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;
import io.github.tt432.eyelib.example.client.EntityResources;
import io.github.tt432.eyelib.example.item.GeckoArmorItem;
import net.minecraft.resources.ResourceLocation;

public class GeckoArmorModel extends AnimatedGeoModel<GeckoArmorItem> {
    @Override
    public ResourceLocation getModelLocation(GeckoArmorItem object) {
        return EntityResources.GECKOARMOR_MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(GeckoArmorItem object) {
        return EntityResources.GECKOARMOR_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(GeckoArmorItem animatable) {
        return EntityResources.GECKOARMOR_ANIMATIONS;
    }
}

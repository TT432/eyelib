package io.github.tt432.eyelib.example.client.model.item;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;
import io.github.tt432.eyelib.example.item.FistItem;
import net.minecraft.resources.ResourceLocation;

/**
 * @author DustW
 */
public class FistModel extends AnimatedGeoModel<FistItem> {
    public static final ResourceLocation MODEL = new ResourceLocation(Eyelib.MOD_ID, "geo/models/fist.geo.json");
    public static final ResourceLocation TEXTURE = new ResourceLocation(Eyelib.MOD_ID,
            "textures/item/fist.png");
    public static final ResourceLocation ANIMATIONS = new ResourceLocation(Eyelib.MOD_ID,
            "geo/animations/fist.animation.json");

    @Override
    public ResourceLocation getModelLocation(FistItem object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(FistItem object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(FistItem animatable) {
        return ANIMATIONS;
    }
}

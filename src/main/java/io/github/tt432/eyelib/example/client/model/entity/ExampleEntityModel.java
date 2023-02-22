package io.github.tt432.eyelib.example.client.model.entity;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedTickingGeoModel;
import io.github.tt432.eyelib.example.entity.GeoExampleEntity;
import net.minecraft.resources.ResourceLocation;

public class ExampleEntityModel extends AnimatedTickingGeoModel<GeoExampleEntity> {
    public static final String NAME = "nuoaier";

    public static final ResourceLocation BAT_MODEL =
            new ResourceLocation(Eyelib.MOD_ID, "geo/models/" + NAME + ".geo.json");
    public static final ResourceLocation BAT_TEXTURE =
            new ResourceLocation(Eyelib.MOD_ID, "textures/model/entity/" + NAME + ".png");
    public static final ResourceLocation BAT_ANIMATIONS =
            new ResourceLocation(Eyelib.MOD_ID, "geo/animations/" + NAME + ".animation.json");

    @Override
    public ResourceLocation getAnimationFileLocation(GeoExampleEntity entity) {
        return BAT_ANIMATIONS;
    }

    @Override
    public ResourceLocation getModelLocation(GeoExampleEntity entity) {
        return BAT_MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(GeoExampleEntity entity) {
        // return new ResourceLocation(Eyelib.MOD_ID, NAME);
        return BAT_TEXTURE;
    }
}

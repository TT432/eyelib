package io.github.tt432.eyelib.example.client.model.entity;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedTickingGeoModel;
import io.github.tt432.eyelib.example.entity.GeoExampleEntity;
import net.minecraft.resources.ResourceLocation;

public class ExampleEntityModel extends AnimatedTickingGeoModel<GeoExampleEntity> {
	public static final ResourceLocation BAT_MODEL = new ResourceLocation(Eyelib.MOD_ID, "geo/models/hammer.geo.json");
	public static final ResourceLocation BAT_TEXTURE = new ResourceLocation(Eyelib.MOD_ID,
			"textures/model/entity/hammer.png");
	public static final ResourceLocation BAT_ANIMATIONS = new ResourceLocation(Eyelib.MOD_ID,
			"geo/animations/hammer.animation.json");

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
		return BAT_TEXTURE;
	}
}

package io.github.tt432.eyelib.example.client.model.entity;

import io.github.tt432.eyelib.example.client.EntityResources;
import io.github.tt432.eyelib.example.entity.BikeEntity;
import net.minecraft.resources.ResourceLocation;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;

public class BikeModel extends AnimatedGeoModel<BikeEntity> {
	@Override
	public ResourceLocation getAnimationFileLocation(BikeEntity entity) {
		return EntityResources.BIKE_ANIMATIONS;
	}

	@Override
	public ResourceLocation getModelLocation(BikeEntity entity) {
		return EntityResources.BIKE_MODEL;
	}

	@Override
	public ResourceLocation getTextureLocation(BikeEntity entity) {
		return EntityResources.BIKE_TEXTURE;
	}
}
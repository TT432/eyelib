package io.github.tt432.eyelib.example.client.model.entity;

import io.github.tt432.eyelib.example.client.EntityResources;
import io.github.tt432.eyelib.example.entity.CarEntity;
import net.minecraft.resources.ResourceLocation;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;

public class CarModel extends AnimatedGeoModel<CarEntity> {
	@Override
	public ResourceLocation getAnimationFileLocation(CarEntity entity) {
		return EntityResources.CAR_ANIMATIONS;
	}

	@Override
	public ResourceLocation getModelLocation(CarEntity entity) {
		return EntityResources.CAR_MODEL;
	}

	@Override
	public ResourceLocation getTextureLocation(CarEntity entity) {
		return EntityResources.CAR_TEXTURE;
	}
}
package io.github.tt432.eyelib.example.client.model.entity;

import io.github.tt432.eyelib.example.client.EntityResources;
import net.minecraft.resources.ResourceLocation;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;

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
	public ResourceLocation getAnimationFileLocation(Object animatable) {
		return EntityResources.CREEPER_ANIMATIONS;
	}
}
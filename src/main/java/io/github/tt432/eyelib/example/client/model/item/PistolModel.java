package io.github.tt432.eyelib.example.client.model.item;

import io.github.tt432.eyelib.example.client.EntityResources;
import io.github.tt432.eyelib.example.item.PistolItem;
import net.minecraft.resources.ResourceLocation;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;

public class PistolModel extends AnimatedGeoModel<PistolItem> {
	@Override
	public ResourceLocation getModelLocation(PistolItem object) {
		return EntityResources.PISTOL_MODEL;
	}

	@Override
	public ResourceLocation getTextureLocation(PistolItem object) {
		return EntityResources.PISTOL_TEXTURE;
	}

	@Override
	public ResourceLocation getAnimationFileLocation(PistolItem animatable) {
		return EntityResources.PISTOL_ANIMATIONS;
	}
}

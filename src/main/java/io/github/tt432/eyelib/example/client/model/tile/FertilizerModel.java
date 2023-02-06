package io.github.tt432.eyelib.example.client.model.tile;

import io.github.tt432.eyelib.example.block.tile.FertilizerTileEntity;
import io.github.tt432.eyelib.example.client.EntityResources;
import net.minecraft.resources.ResourceLocation;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;

public class FertilizerModel extends AnimatedGeoModel<FertilizerTileEntity> {
	@Override
	public ResourceLocation getAnimationFileLocation(FertilizerTileEntity animatable) {
		if (animatable.getLevel().isRaining())
			return EntityResources.FERTILIZER_ANIMATIONS;

		return EntityResources.BOTARIUM_ANIMATIONS;
	}

	@Override
	public ResourceLocation getModelLocation(FertilizerTileEntity animatable) {
		if (animatable.getLevel().isRaining())
			return EntityResources.FERTILIZER_MODEL;

		return EntityResources.BOTARIUM_MODEL;
	}

	@Override
	public ResourceLocation getTextureLocation(FertilizerTileEntity entity) {
		if (entity.getLevel().isRaining())
			return EntityResources.FERTILIZER_TEXTURE;

		return EntityResources.BOTARIUM_TEXTURE;
	}
}
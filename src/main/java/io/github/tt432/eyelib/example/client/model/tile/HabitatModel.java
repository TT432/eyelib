package io.github.tt432.eyelib.example.client.model.tile;

import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;
import io.github.tt432.eyelib.example.block.tile.HabitatTileEntity;
import io.github.tt432.eyelib.example.client.EntityResources;
import net.minecraft.resources.ResourceLocation;

/**
 * @author VoutVouniern Copyright (c) 03.06.2022 Developed by VoutVouniern
 */
public class HabitatModel extends AnimatedGeoModel<HabitatTileEntity> {
    @Override
    public ResourceLocation getAnimationFileLocation(HabitatTileEntity entity) {
        return EntityResources.HABITAT_ANIMATIONS;
    }

    @Override
    public ResourceLocation getModelLocation(HabitatTileEntity animatable) {
        return EntityResources.HABITAT_MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(HabitatTileEntity entity) {
        return EntityResources.HABITAT_TEXTURE;
    }
}
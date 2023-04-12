package io.github.tt432.eyelib.api.bedrock.model;

import io.github.tt432.eyelib.common.bedrock.BedrockResourceManager;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;
import net.minecraft.resources.ResourceLocation;

public abstract class GeoModelProvider<T> {
    protected double seekTime;

    public GeoModel getModel(ResourceLocation location) {
        return BedrockResourceManager.getInstance().getGeoModels().get(location);
    }

    public abstract ResourceLocation getModelLocation(T object);

    public abstract ResourceLocation getTextureLocation(T object);
}

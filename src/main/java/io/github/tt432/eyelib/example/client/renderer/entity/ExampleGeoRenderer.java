package io.github.tt432.eyelib.example.client.renderer.entity;

import io.github.tt432.eyelib.common.bedrock.renderer.GeoEntityRenderer;
import io.github.tt432.eyelib.example.client.model.entity.ExampleEntityModel;
import io.github.tt432.eyelib.example.entity.GeoExampleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ExampleGeoRenderer extends GeoEntityRenderer<GeoExampleEntity> {
	public ExampleGeoRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new ExampleEntityModel());
	}
}

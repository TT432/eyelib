package io.github.tt432.eyelib.example.client.renderer.entity;

import io.github.tt432.eyelib.common.bedrock.renderer.GeoEntityRenderer;
import io.github.tt432.eyelib.example.client.model.entity.LEModel;
import io.github.tt432.eyelib.example.client.renderer.entity.layer.GeoExampleLayer;
import io.github.tt432.eyelib.example.entity.LEEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class LERenderer extends GeoEntityRenderer<LEEntity> {
    public LERenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LEModel());

        this.shadowRadius = 0.25f;

        addLayer(new GeoExampleLayer(this));
    }
}

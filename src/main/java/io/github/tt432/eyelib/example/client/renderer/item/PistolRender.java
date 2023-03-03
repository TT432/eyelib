package io.github.tt432.eyelib.example.client.renderer.item;

import io.github.tt432.eyelib.common.bedrock.renderer.GeoItemRenderer;
import io.github.tt432.eyelib.example.client.model.item.PistolModel;
import io.github.tt432.eyelib.example.item.PistolItem;

public class PistolRender extends GeoItemRenderer<PistolItem> {
    public PistolRender() {
        super(new PistolModel());
    }

}
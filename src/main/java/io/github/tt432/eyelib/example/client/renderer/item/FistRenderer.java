package io.github.tt432.eyelib.example.client.renderer.item;

import io.github.tt432.eyelib.common.bedrock.renderer.GeoItemRenderer;
import io.github.tt432.eyelib.example.client.model.item.FistModel;
import io.github.tt432.eyelib.example.item.FistItem;

/**
 * @author DustW
 */
public class FistRenderer extends GeoItemRenderer<FistItem> {
    public FistRenderer() {
        super(new FistModel());
    }
}
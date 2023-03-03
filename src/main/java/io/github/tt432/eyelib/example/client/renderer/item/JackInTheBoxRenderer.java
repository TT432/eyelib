package io.github.tt432.eyelib.example.client.renderer.item;

import io.github.tt432.eyelib.common.bedrock.renderer.GeoItemRenderer;
import io.github.tt432.eyelib.example.client.model.item.JackInTheBoxModel;
import io.github.tt432.eyelib.example.item.JackInTheBoxItem;

public class JackInTheBoxRenderer extends GeoItemRenderer<JackInTheBoxItem> {
    public JackInTheBoxRenderer() {
        super(new JackInTheBoxModel());
    }
}

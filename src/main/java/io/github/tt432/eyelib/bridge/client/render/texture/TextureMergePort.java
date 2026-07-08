package io.github.tt432.eyelib.bridge.client.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.bridge.client.render.texture.adapter.TextureLayerMerger;
import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.util.PortResourceLocation;

import java.util.List;

public interface TextureMergePort {
    static NativeImage merge(List<PortResourceLocation> textures) {
        return TextureLayerMerger.merge(textures.stream().map(ResourceLocationBridge::toMc).toList());
    }
}

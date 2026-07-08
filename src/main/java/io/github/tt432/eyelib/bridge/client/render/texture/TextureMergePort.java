package io.github.tt432.eyelib.bridge.client.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.bridge.client.render.texture.adapter.TextureLayerMerger;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}

import java.util.List;

public interface TextureMergePort {
    //? if <26.1 {
    static NativeImage merge(List<ResourceLocation> textures) {
    //?} else {
    static NativeImage merge(List<Identifier> textures) {
    //?}
        return TextureLayerMerger.merge(textures);
    }
}

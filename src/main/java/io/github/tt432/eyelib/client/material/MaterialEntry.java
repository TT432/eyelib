package io.github.tt432.eyelib.client.material;

import io.github.tt432.eyelib.client.model.tree.ModelCubeNode;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * @author TT432
 */
public record MaterialEntry(
        ResourceLocation texture,
        Function<ResourceLocation, RenderType> factory
) implements ModelCubeNode {
    public RenderType renderType() {
        return factory.apply(texture);
    }
}

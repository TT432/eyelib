package io.github.tt432.eyelib.client.render.sections.cache;

import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.BakedModel;

/**
 * @author Argon4W
 */
public record DynamicModelCache(BakedModel model, BakedModelsCache cache) implements IBakedModelCache {
    @Override
    public BakedModel getTransformedModel(Transformation transformation) {
        return new DynamicTransformedBakedModel(model, transformation, cache);
    }

    @Override
    public int size() {
        return 0;
    }
}

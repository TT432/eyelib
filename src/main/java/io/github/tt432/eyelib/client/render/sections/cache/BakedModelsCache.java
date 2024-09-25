package io.github.tt432.eyelib.client.render.sections.cache;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Argon4W
 */
public class BakedModelsCache {
    private final Map<BakedModel, IBakedModelCache> modelCache;

    public BakedModelsCache() {
        this.modelCache = new ConcurrentHashMap<>();
    }

    public BakedModel getTransformedModel(BakedModel model, PoseStack poseStack) {
        return getTransformedModel(model, new Transformation(poseStack.last().pose()));
    }

    public BakedModel getTransformedModel(BakedModel model, Transformation transformation) {
        return modelCache.compute(model, (model1, cache) -> cache == null ? createModelCache(model) : (cache.size() > 32 ? new DynamicModelCache(model1, this) : cache)).getTransformedModel(transformation);
    }

    public IBakedModelCache createModelCache(BakedModel model) {
        return model instanceof SimpleBakedModel simple ? new SimpleModelCache(simple) : new DynamicModelCache(model, this);
    }
}

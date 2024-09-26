package io.github.tt432.eyelib.client.render.sections.cache;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import io.github.tt432.eyelib.client.model.UnBakedBrModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Argon4W
 */
public class DefaultRendererBakedModelsCache implements IRendererBakedModelsCache {
    private final Map<BakedModel, IBakedModelCache> modelCache;

    public DefaultRendererBakedModelsCache() {
        this.modelCache = new ConcurrentHashMap<>();
    }

    @Override
    public BakedModel getTransformedModel(BakedModel model, PoseStack poseStack) {
        return getTransformedModel(model, new Transformation(poseStack.last().pose()));
    }

    @Override
    public BakedModel getTransformedModel(BakedModel model, Transformation transformation) {
        return modelCache.compute(model, (model1, cache) -> cache == null ? createModelCache(model) : (cache.size() > 32 ? new DynamicModelCache(model1, this) : cache)).getTransformedModel(transformation);
    }

    @Override
    public int getSize() {
        return modelCache.values().stream().mapToInt(IBakedModelCache::size).sum();
    }

    public IBakedModelCache createModelCache(BakedModel model) {
        if (model instanceof SimpleBakedModel simple) {
            return new SimpleModelCache(simple);
        }

        if (model instanceof UnBakedBrModel.BakedBrModel brModel && brModel.getOriginalModel() instanceof SimpleBakedModel simple) {
            return new BakedBrModelCache(brModel.visitors, simple);
        }

        return new DynamicModelCache(model, this);
    }
}

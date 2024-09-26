package io.github.tt432.eyelib.client.render.sections.cache;

import com.mojang.math.Transformation;
import io.github.tt432.eyelib.client.render.sections.SimpleBakedModelExtension;
import io.github.tt432.eyelib.util.EntryStreams;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Argon4W
 */
public class SimpleModelCache implements BakedModelCache {
    private final SimpleBakedModel model;
    private final Map<Transformation, BakedModel> modelCache;

    public SimpleModelCache(SimpleBakedModel model) {
        this.model = model;
        this.modelCache = new ConcurrentHashMap<>();
    }

    @Override
    public BakedModel getTransformedModel(Transformation transformation) {
        return modelCache.computeIfAbsent(transformation, transformation1 -> getTransformedModel(QuadTransformers.applying(transformation1)));
    }

    public BakedModel getTransformedModel(IQuadTransformer transformer) {
        return new SimpleBakedModel(model.unculledFaces.stream().map(transformer::process).toList(), model.culledFaces.entrySet().stream().map(EntryStreams.mapEntryValue(list -> list.stream().map(transformer::process).toList())).collect(EntryStreams.of()), model.useAmbientOcclusion(), model.usesBlockLight(), model.isGui3d(), model.getParticleIcon(), model.getTransforms(), model.getOverrides(), model instanceof SimpleBakedModelExtension extension ? extension.eyelib$getRenderTypeGroup() : RenderTypeGroup.EMPTY);
    }

    @Override
    public int size() {
        return modelCache.size();
    }
}

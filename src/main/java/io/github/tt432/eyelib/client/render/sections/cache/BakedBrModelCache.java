package io.github.tt432.eyelib.client.render.sections.cache;

import io.github.tt432.eyelib.client.model.UnBakedBrModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import org.joml.Matrix4f;

import java.util.Map;

public class BakedBrModelCache extends SimpleModelCache {
    private final Map<String, Matrix4f> visitors;

    public BakedBrModelCache(Map<String, Matrix4f> visitors, SimpleBakedModel originalModel) {
        super(originalModel);
        this.visitors = visitors;
    }

    public BakedModel getTransformedModel(IQuadTransformer transformer) {
        return new UnBakedBrModel.BakedBrModel(super.getTransformedModel(transformer), Map.copyOf(visitors));
    }
}

package io.github.tt432.eyelib.client.render.sections.cache;

import io.github.tt432.eyelib.client.model.UnBakedBrModel;
import io.github.tt432.eyelib.util.EntryStreams;
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

    @Override
    public BakedModel getTransformedModel(Matrix4f matrix4f, IQuadTransformer transformer) {
        return new UnBakedBrModel.BakedBrModel(super.getTransformedModel(matrix4f, transformer), visitors.entrySet().stream().map(EntryStreams.mapEntryValue(m -> new Matrix4f(m).mul(matrix4f))).collect(EntryStreams.of()));
    }
}

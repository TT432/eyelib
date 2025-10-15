package io.github.tt432.eyelib.client.compat.ar;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IBufferGraph;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.meshes.IMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.CulledMeshCollector;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bake.BakedModel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.experimental.ExtensionMethod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Map;

@ExtensionMethod(VertexConsumerExtension.class)
public class AcceleratedBakedBoneRenderer implements IAcceleratedRenderer<BakedModel.BakedBone> {
    public static final AcceleratedBakedBoneRenderer INSTANCE = new AcceleratedBakedBoneRenderer();

    private final Map<BakedModel.BakedBone, Map<IBufferGraph, IMesh>> boneMeshes = new Reference2ObjectOpenHashMap<>();

    @Override
    public void render(VertexConsumer vertexConsumer, BakedModel.BakedBone bakedBone, Matrix4f transform, Matrix3f normal, int light, int overlay, int color) {
        var extension = vertexConsumer.getAccelerated();

        extension.beginTransform(transform, normal);

        boneMeshes
                .computeIfAbsent(bakedBone, b -> new Object2ObjectOpenHashMap<>())
                .computeIfAbsent(extension, c -> {
            var culledMeshCollector = new CulledMeshCollector(extension);
            var meshBuilder = extension.decorate(culledMeshCollector);

            for (int nIdx = 0; nIdx < bakedBone.vertexSize(); nIdx++) {
                meshBuilder.addVertex(
                        bakedBone.position()[nIdx * 3],
                        bakedBone.position()[nIdx * 3 + 1],
                        bakedBone.position()[nIdx * 3 + 2],
                        0xFF_FF_FF_FF, bakedBone.u()[nIdx], bakedBone.v()[nIdx], overlay, 0,
                        bakedBone.normal()[nIdx * 3],
                        bakedBone.normal()[nIdx * 3 + 1],
                        bakedBone.normal()[nIdx * 3 + 2]
                );
            }

            culledMeshCollector.flush();

            return AcceleratedEntityRenderingFeature
                    .getMeshType()
                    .getBuilder()
                    .build(culledMeshCollector);
        }).write(
                extension,
                color,
                light,
                overlay
        );

        extension.endTransform();
    }
}

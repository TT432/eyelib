//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.compat.ar.adapter;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IBufferGraph;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.meshes.IMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.CulledMeshCollector;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.experimental.ExtensionMethod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import java.util.Map;

/**
 * 加速渲染管线中骨骼模型的自定义加速渲染器。
 * 将 {@link ARBoneData} 的顶点数据提交到加速缓冲区图结构中。
 *
 * @author TT432
 */
@ExtensionMethod(VertexConsumerExtension.class)
public class AcceleratedBakedBoneRenderer implements IAcceleratedRenderer<ARBoneData> {
    public static final AcceleratedBakedBoneRenderer INSTANCE = new AcceleratedBakedBoneRenderer();

    private final Map<ARBoneData, Map<IBufferGraph, IMesh>> boneMeshes = new Reference2ObjectOpenHashMap<>();

    @Override
    public void render(VertexConsumer vertexConsumer, ARBoneData data, Matrix4f transform, Matrix3f normal, int light, int overlay, int color) {
        var extension = vertexConsumer.getAccelerated();

        extension.beginTransform(transform, normal);

        boneMeshes
                .computeIfAbsent(data, b -> new Object2ObjectOpenHashMap<>())
                .computeIfAbsent(extension, c -> {
                    var culledMeshCollector = new CulledMeshCollector(extension);
                    var meshBuilder = extension.decorate(culledMeshCollector);

                    for (int nIdx = 0; nIdx < data.vertexSize(); nIdx++) {
                        //? if <1.20.6 {
                        meshBuilder.vertex(
                                data.positions()[nIdx * 3],
                                data.positions()[nIdx * 3 + 1],
                                data.positions()[nIdx * 3 + 2],
                                1, 1, 1, 1,
                                data.u()[nIdx], data.v()[nIdx], overlay, 0,
                                data.normals()[nIdx * 3],
                                data.normals()[nIdx * 3 + 1],
                                data.normals()[nIdx * 3 + 2]
                        );
                        //?} else {
                        meshBuilder.addVertex(
                                        data.positions()[nIdx * 3],
                                        data.positions()[nIdx * 3 + 1],
                                        data.positions()[nIdx * 3 + 2]
                                )
                                .setColor(255, 255, 255, 255)
                                .setUv(data.u()[nIdx], data.v()[nIdx])
                                .setOverlay(overlay)
                                .setLight(0)
                                .setNormal(
                                        data.normals()[nIdx * 3],
                                        data.normals()[nIdx * 3 + 1],
                                        data.normals()[nIdx * 3 + 2]
                                );
                        //?}
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
//?}

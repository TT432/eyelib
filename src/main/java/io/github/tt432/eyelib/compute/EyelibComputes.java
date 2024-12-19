package io.github.tt432.eyelib.compute;

import com.mojang.blaze3d.vertex.MeshData;
import lombok.experimental.UtilityClass;

/**
 * @author TT432
 */
@UtilityClass
public class EyelibComputes {

    public void compute(MeshData meshData, int vertexBufferId) {
        if (meshData instanceof LazyComputeMeshData lc) {
            VertexComputeHelper helper = lc.getHelper();

            if (helper != null) {
                helper.compute(vertexBufferId);
                helper.clear();
            }
        }
    }
}

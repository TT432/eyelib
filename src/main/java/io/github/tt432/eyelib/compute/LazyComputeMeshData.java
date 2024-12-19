package io.github.tt432.eyelib.compute;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TT432
 */
@Getter
@Setter
public class LazyComputeMeshData extends MeshData {
    VertexComputeHelper helper;

    public LazyComputeMeshData(ByteBufferBuilder.Result vertexBuffer, DrawState drawState, VertexComputeHelper helper) {
        super(vertexBuffer, drawState);
        this.helper = helper;
    }
}

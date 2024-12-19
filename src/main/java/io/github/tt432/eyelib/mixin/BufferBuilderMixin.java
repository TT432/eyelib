package io.github.tt432.eyelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import io.github.tt432.eyelib.compute.LazyComputeBufferBuilder;
import io.github.tt432.eyelib.compute.LazyComputeMeshData;
import io.github.tt432.eyelib.compute.VertexComputeHelper;
import lombok.Getter;
import lombok.Setter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @author TT432
 */
@Mixin(BufferBuilder.class)
public class BufferBuilderMixin implements LazyComputeBufferBuilder {
    @Getter
    @Setter
    @Unique
    VertexComputeHelper eyelib$helper;

    @WrapOperation(method = "storeMesh", at = @At(value = "NEW", target = "(Lcom/mojang/blaze3d/vertex/ByteBufferBuilder$Result;Lcom/mojang/blaze3d/vertex/MeshData$DrawState;)Lcom/mojang/blaze3d/vertex/MeshData;"))
    private MeshData eyelib$newMeshData(ByteBufferBuilder.Result vertexBuffer, MeshData.DrawState drawState, Operation<MeshData> original) {
        return new LazyComputeMeshData(vertexBuffer, drawState, eyelib$helper);
    }
}

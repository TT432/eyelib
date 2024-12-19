package io.github.tt432.eyelib.mixin;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import io.github.tt432.eyelib.compute.EyelibComputes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author TT432
 */
@Mixin(VertexBuffer.class)
public class VertexBufferMixin {
    @Shadow
    public int vertexBufferId;

    @Inject(method = "upload", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;uploadVertexBuffer(Lcom/mojang/blaze3d/vertex/MeshData$DrawState;Ljava/nio/ByteBuffer;)Lcom/mojang/blaze3d/vertex/VertexFormat;"))
    private void eyelib$upload(MeshData meshData, CallbackInfo ci) {
        EyelibComputes.compute(meshData, vertexBufferId);
    }
}

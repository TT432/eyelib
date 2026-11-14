//? if <26.1 {
package io.github.tt432.eyelib.mixin.client;

import io.github.tt432.eyelib.debug.rendercount.RenderTypeDrawStats;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <1.20.6 {
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
//?} else {
import com.mojang.blaze3d.vertex.MeshData;
//?}

/**
 * 统计每次 RenderType 的实际 draw 调用，供 F3 条目显示。
 *
 * <p>1.20.1 的绘制入口是 {@code RenderType.end}（内部遇到在建 buffer 才真正上传）；
 * 1.21.1 是 {@code RenderType.draw}。两处每个 RenderType 实例都会经过。
 */
@Mixin(RenderType.class)
@SuppressWarnings({"unused", "PMD.UnusedPrivateMethod", "PMD.UnusedFormalParameter"})
public abstract class RenderTypeDrawCounterMixin {
    //? if <1.20.6 {
    @Inject(method = "end", at = @At("HEAD"))
    private void eyelib$countRenderTypeDraw(BufferBuilder builder, VertexSorting sorting, CallbackInfo ci) {
        if (builder.building()) {
            RenderTypeDrawStats.record(((RenderStateShardNameAccessor) this).eyelib$name());
        }
    }
    //?} else {
    @Inject(method = "draw", at = @At("HEAD"))
    private void eyelib$countRenderTypeDraw(MeshData mesh, CallbackInfo ci) {
        RenderTypeDrawStats.record(((RenderStateShardNameAccessor) this).eyelib$name());
    }
    //?}
}
//?}

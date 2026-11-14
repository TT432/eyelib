//? if >=26.1 {
package io.github.tt432.eyelib.mixin.client;

import com.mojang.blaze3d.vertex.MeshData;
import io.github.tt432.eyelib.debug.rendercount.RenderTypeDrawStats;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 统计每次 RenderType 的实际 draw 调用，供 F3 条目显示。
 *
 * <p>26.1.2 起 {@code RenderType} 不再是 abstract 状态巨类，{@code draw} 是唯一的
 * 立即模式提交入口（{@code MultiBufferSource} 批量后调用），因此直接注入它。
 */
@Mixin(RenderType.class)
@SuppressWarnings({"unused", "PMD.UnusedPrivateMethod", "PMD.UnusedFormalParameter"})
public abstract class RenderTypeDrawCounterMixin26 {
    @Inject(method = "draw", at = @At("HEAD"))
    private void eyelib$countRenderTypeDraw(MeshData mesh, CallbackInfo ci) {
        RenderTypeDrawStats.record(((RenderTypeNameAccessor26) this).eyelib$name());
    }
}
//?}

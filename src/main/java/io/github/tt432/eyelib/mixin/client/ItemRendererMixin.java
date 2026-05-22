package io.github.tt432.eyelib.mixin.client;

import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.track.ItemTrackRenderer;
import io.github.tt432.eyelibanimation.AnimationComponent;
import io.github.tt432.eyelibanimation.AnimationEffects;
import io.github.tt432.eyelibanimation.BrAnimator;
import io.github.tt432.eyelibanimation.ModelRuntimeData;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 接入 ItemRenderer 渲染管线，为 TrackableItem 准备 RenderData 并 tick 动画。
 *
 * @author TT432
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onPreRender(ItemStack itemStack, ItemDisplayContext displayContext,
                             boolean leftHand, com.mojang.blaze3d.vertex.PoseStack poseStack,
                             net.minecraft.client.renderer.MultiBufferSource buffer,
                             int combinedLight, int combinedOverlay,
                             net.minecraft.client.resources.model.BakedModel pModel,
                             CallbackInfo ci) {
        RenderData<ItemStack> rd = ItemTrackRenderer.prepareRenderData(itemStack, displayContext);
        if (rd == null) {
            return;
        }

        AnimationComponent ac = rd.getAnimationComponent();
        if (ac.getSerializableInfo() == null) {
            return;
        }

        AnimationEffects effects = new AnimationEffects();
        float partialTick = ItemTrackRenderer.getPartialTick();
        rd.getScope().set("variable.partial_tick", partialTick);

        ModelRuntimeData tickedInfos = BrAnimator.tickAnimation(ac, rd.getScope(), effects,
                                                                (ClientTickHandler.getTick() + partialTick) / 20, () -> {
                });
        ac.tickedInfos = tickedInfos;
        ac.effects = effects;
    }
}

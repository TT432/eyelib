package io.github.tt432.eyelib.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.entity.AttachableResolver;
import io.github.tt432.eyelib.client.render.AttachableItemRenderSetup;
import io.github.tt432.eyelib.animation.AnimationComponent;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.BrAnimator;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 注入 ItemInHandRenderer，为手持 attachable 物品提供通用渲染（覆盖所有 LivingEntity）。
 *
 * @author TT432
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void onRenderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                              boolean left, PoseStack poseStack, MultiBufferSource buffer,
                              int light, CallbackInfo ci) {
        BrClientEntity attachable = AttachableResolver.resolve(entity, stack);
        if (attachable == null) {
            return;
        }

        InteractionHand hand = left ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        RenderData<ItemStack> rd = AttachableItemRenderSetup.getOrPrepare(entity, hand);
        if (rd == null) {
            return;
        }

        AnimationComponent ac = rd.getAnimationComponent();
        if (ac.getSerializableInfo() != null && ac.tickedInfos == null) {
            //? if <1.20.6
            float partialTick = net.minecraft.client.Minecraft.getInstance().getFrameTime();
            //? if >=1.20.6
            float partialTick = net.minecraft.client.Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
            rd.getScope().set("variable.partial_tick", partialTick);

            AnimationEffects effects = new AnimationEffects();
            ModelRuntimeData tickedInfos = BrAnimator.tickAnimation(ac, rd.getScope(), effects,
                    (io.github.tt432.eyelib.client.ClientTickHandler.getTick() + partialTick) / 20, () -> {
                        var ce = rd.getClientEntityComponent().getClientEntity();
                        if (ce != null) {
                            ce.scripts().ifPresent(s -> s.pre_animation().eval(rd.getScope()));
                        }
                    });
            ac.tickedInfos = tickedInfos;
            ac.effects = effects;
        }

        AttachableItemRenderSetup.renderAttachable(rd, poseStack, buffer, entity, light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        ci.cancel();
    }
}

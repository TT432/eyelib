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
import io.github.tt432.eyelib.molang.MolangScope;
//? if >=26.1
import net.minecraft.client.renderer.SubmitNodeCollector;
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
 * <p>跨版本签名差异（ADR-0016 //? 切分）：
 * <ul>
 *   <li>&lt;26.1：{@code renderItem(LivingEntity, ItemStack, ItemDisplayContext, boolean left, PoseStack, MultiBufferSource, int)}</li>
 *   <li>&gt;=26.1：{@code renderItem(LivingEntity, ItemStack, ItemDisplayContext, PoseStack, SubmitNodeCollector, int)}，
 *       移除 {@code boolean left}（左右手从 {@link ItemDisplayContext} 推断），{@code MultiBufferSource}→{@code SubmitNodeCollector}</li>
 * </ul>
 * &gt;=26.1 下 attachable 模型渲染使用全局 bufferSource（与 RenderLivingEventAdapter 一致）。
 *
 * @author TT432
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    //? if <26.1 {
    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void onRenderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                              boolean left, PoseStack poseStack, MultiBufferSource buffer,
                              int light, CallbackInfo ci) {
        tryRenderAttachable(entity, stack, left, poseStack, buffer, light, ci);
    }
    //?} else {
    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void onRenderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                              PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                              int light, CallbackInfo ci) {
        boolean left = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                    || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        MultiBufferSource buffer = net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
        tryRenderAttachable(entity, stack, left, poseStack, buffer, light, ci);
    }
    //?}

    private void tryRenderAttachable(LivingEntity entity, ItemStack stack, boolean left,
                                     PoseStack poseStack, MultiBufferSource buffer,
                                     int light, CallbackInfo ci) {
        BrClientEntity attachable = AttachableResolver.resolve(entity, stack);
        if (attachable == null) {
            return;
        }

        InteractionHand hand = left ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        RenderData<ItemStack> rd = AttachableItemRenderSetup.getOrPrepare(entity, hand, true);
        if (rd == null) {
            return;
        }

        AnimationComponent ac = rd.getAnimationComponent();
        if (ac.getSerializableInfo() != null && ac.tickedInfos == null) {
            //? if <1.20.6
            float partialTick = net.minecraft.client.Minecraft.getInstance().getFrameTime();
            //? if >=1.20.6 && <26.1
            float partialTick = net.minecraft.client.Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
            //? if >=26.1
            float partialTick = net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();
            MolangScope scope = rd.getScope();
            if (scope == null) return;
            scope.set("variable.partial_tick", partialTick);

            AnimationEffects effects = new AnimationEffects();
            ModelRuntimeData tickedInfos = BrAnimator.tickAnimation(ac, scope, effects,
                    (io.github.tt432.eyelib.bridge.client.ClientTickHandler.getTick() + partialTick) / 20, () -> {
                        var ce = rd.getClientEntityComponent().getClientEntity();
                        if (ce != null) {
                            ce.scripts().ifPresent(s -> s.pre_animation().eval(scope));
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

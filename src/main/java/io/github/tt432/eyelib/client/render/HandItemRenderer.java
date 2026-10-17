package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.ModelVisitContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 手持物（主手/副手物品与 attachable）渲染簇，从 {@link EntityRenderOrchestrator} 提取。
 *
 * @author TT432
 */
final class HandItemRenderer {
    private static final int leftitem = GlobalBoneIdHandler.get("leftitem");
    private static final int rightitem = GlobalBoneIdHandler.get("rightitem");

    private HandItemRenderer() {
    }

    static boolean renderItemInHand(ModelVisitContext context, SimpleRenderAction<?> action,
                                    LivingEntity renderTarget, int light) {
        PoseStack poseStack = new PoseStack();
        var locators = context
                .<Int2ObjectMap<PoseStack.Pose>>orCreate("bones", Int2ObjectOpenHashMap::new);
        // 收集的骨骼姿态为「绕 pivot 的动画变换」，不包含 pivot 平移（applyBoneTranslate 中 +pivot/-pivot 抵消）。
        // attachable 需要附着到骨骼原点，必须补回骨骼 pivot 平移。
        var bindBones = action.renderData().bindBones();
        var offHandBone = bindBones.get(leftitem);
        var mainHandBone = bindBones.get(rightitem);
        var offHandPose = locators.get(leftitem);
        if (offHandPose != null && offHandBone != null) {
            RenderPorts.get().renderSystemPort().pushPoseRaw(poseStack, offHandPose);
            var pivot = offHandBone.pivot();
            poseStack.translate(pivot.x(), pivot.y(), pivot.z());
            ItemStack itemInHand = renderTarget.getItemInHand(InteractionHand.OFF_HAND);
            renderHandItemOrAttachable(action.multiBufferSource(), action.sink(), renderTarget, itemInHand,
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND, light, poseStack, true, InteractionHand.OFF_HAND);
        }

        var mainHandPose = locators.get(rightitem);
        if (mainHandPose != null && mainHandBone != null) {
            RenderPorts.get().renderSystemPort().pushPoseRaw(poseStack, mainHandPose);
            var pivot = mainHandBone.pivot();
            poseStack.translate(pivot.x(), pivot.y(), pivot.z());
            ItemStack itemInHand = renderTarget.getItemInHand(InteractionHand.MAIN_HAND);
            renderHandItemOrAttachable(action.multiBufferSource(), action.sink(), renderTarget, itemInHand,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, light, poseStack, false, InteractionHand.MAIN_HAND);
        }

        return true;
    }

    private static void renderHandItemOrAttachable(MultiBufferSource bufferSource, RenderSink sink, LivingEntity le, ItemStack item,
                                                   ItemDisplayContext context, int light, PoseStack poseStack,
                                                   boolean left, InteractionHand hand) {
        if (item.isEmpty()) {
            return;
        }

        var rd = AttachableItemRenderSetup.getOrPrepare(le, hand, false);
        if (rd != null) {
            poseStack.pushPose();
            AttachableItemRenderSetup.renderAttachable(rd, poseStack, bufferSource, le, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            return;
        }

        renderHandItem(sink, le, item, context, light, poseStack, left);
    }

    private static void renderHandItem(RenderSink sink, LivingEntity le, ItemStack item,
                                       ItemDisplayContext context, int light, PoseStack poseStack, boolean left) {
        if (!item.isEmpty()) {
            poseStack.pushPose();

            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(-0.25, 0.1, -1.15);
            RenderPorts.get().renderSystemPort().renderItemDirect(le, item, context, left, poseStack, sink, light);
            poseStack.popPose();
        }
    }
}

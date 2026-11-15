package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.model.Model;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * head 槽非盔甲物品渲染（vanilla {@code CustomHeadLayer} 的非盔甲分支），由
 * {@link ProxyAttachments} 的 {@code eyelib:head_item} 代理驱动。
 * <p>
 * eyelib 取消 {@code LivingEntityRenderer} 后，原版 CustomHeadLayer 不再执行；而
 * {@link ArmorAttachmentRenderer} 只处理人形盔甲。掠夺者的不祥旗帜是放在 HEAD 槽的
 * 普通物品（{@code Raid} 用 {@code EquipmentSlot.HEAD} 装备），由 CustomHeadLayer 以
 * {@link ItemDisplayContext#HEAD} 的物品模型绘制——没有这条路径旗帜永远不可见。
 * <p>
 * 几何/变换与 vanilla 一致：head 骨骼帧 → 颈部 pivot → vanilla 模型空间
 * {@code scale(-1,-1,1)} → {@code CustomHeadLayer.translateToHead}（下移 0.25、绕 Y 180°、
 * 0.625 缩放）。盔甲物品由 {@code eyelib:armor_head} 代理处理（HEAD 槽按物品类型分流，不会双画）。
 *
 * @author TT432
 */
final class HeadItemRenderer {

    private HeadItemRenderer() {
    }

    /**
     * 每实体每帧一次，必须在模型几何提交之前（26.1 的 submit 节点在几何 writer 内不能新增）。
     */
    static boolean render(SimpleRenderAction<?> action, LivingEntity holder, Model model, ModelRuntimeData tickedInfos,
                          Matrix4f modelRoot, ProxyAttachable proxy, int light) {
        ItemStack stack = holder.getItemBySlot(proxy.slot());
        if (stack.isEmpty()) {
            return false;
        }
        int boneId = proxy.resolveBindTarget(model);
        if (boneId < 0) {
            return false;
        }
        Model.Bone bindBone = model.allBones().get(boneId);
        if (bindBone == null) {
            return false;
        }
        var frame = ModelPoseTransforms.resolveBonePose(model, tickedInfos, boneId, modelRoot);
        if (frame.isEmpty()) {
            return false;
        }

        PoseStack poseStack = RenderPorts.get().renderSystemPort().createPoseStackFromMatrix(frame.get());
        var pivot = bindBone.pivot();
        poseStack.translate(pivot.x(), pivot.y(), pivot.z());
        // vanilla part space（与 ArmorAttachmentRenderer 同一空间换算）
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // CustomHeadLayer.translateToHead
        poseStack.translate(0.0F, -0.25F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.625F, -0.625F, -0.625F);

        RenderPorts.get().renderSystemPort()
                .renderItemDirect(holder, stack, ItemDisplayContext.HEAD, false, poseStack, action.sink(), light);
        return true;
    }
}

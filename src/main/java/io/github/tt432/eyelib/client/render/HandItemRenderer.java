package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.entity.AttachablePolicy;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.model.Model;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * 手持物（主手/副手物品与 attachable）渲染簇，由 {@link ProxyAttachments} 的
 * {@code eyelib:held_item_main} / {@code eyelib:held_item_off} 代理驱动。
 * <p>
 * 挂载点取代理声明的候选链（{@code rightitem}/{@code leftitem} → 手骨骼 → 手部 locator）；
 * 候选链全部缺失时**不绘制**：基岩版没有「找不到挂点就退回模型原点」的可用语义，任何兜底位置都是错位。
 * <p>
 * 骨骼帧由 {@link ModelPoseTransforms#resolveBonePose} 从模型根位姿累积得到；在其上
 * {@code translate(pivot)} 的结果正是 pivot 点在当前帧中的位置（矩阵平移 + 旋转后的 pivot），
 * 即基岩版 attachable 的挂载原点。之后的方向由两类挂载物各自决定：
 * <ul>
 *   <li><b>attachable 模型</b>：几何与焊接动画都写在「持有者模型空间里的 item 骨骼帧」中，
 *       直接渲染，不追加旋转或平移；</li>
 *   <li><b>普通物品</b>（无 attachable 定义）：物品模型处在 Java 物品空间，需要 {@code Rx(-90)}
 *       才呈「握在手里」的方向。该角度可由两个模型空间的关系推导：基岩版模型空间相对 Java 实体
 *       模型空间是绕 Z 轴 180°（x、y 取反），把它代入 vanilla {@code ItemInHandLayer} 的
 *       {@code Rx(-90)·Ry(180)} 后 {@code Ry(180)} 被抵消，只剩 {@code Rx(-90)}。</li>
 * </ul>
 *
 * @author TT432
 */
final class HandItemRenderer {

    private HandItemRenderer() {
    }

    /**
     * 单个手持槽（主手或副手）。必须在模型几何提交之前调用（26.1 的 submit 节点在几何 writer 内
     * 已不能接收新内容）。
     *
     * @param modelRoot 模型根位姿（已含实体位姿；模型 Y 翻转由 {@link ModelPoseTransforms} 施加）
     */
    static boolean render(SimpleRenderAction<?> action, LivingEntity holder, Model model, ModelRuntimeData tickedInfos,
                          Matrix4f modelRoot, ProxyAttachable proxy, int light) {
        boolean offhand = proxy.slot() == EquipmentSlot.OFFHAND;
        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemDisplayContext displayContext = offhand
                ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;

        ItemStack item = holder.getItemInHand(hand);
        if (item.isEmpty()) {
            return false;
        }
        // 手持物专有的两个 client entity 开关（与真实 attachable 手持物共用同一求值）：
        // held_item_ignores_lighting → 全亮；held_item_scale → Molang 缩放（<=0 回落 1.0）
        BrClientEntity holderCe = AttachablePolicy.clientEntityOf(holder);
        int itemLight = AttachablePolicy.heldItemLight(holderCe, light);
        float itemScale = AttachablePolicy.heldItemScale(holderCe, holderScope(holder));
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
        if (itemScale != 1F) {
            poseStack.scale(itemScale, itemScale, itemScale);
        }

        var attachable = AttachableItemRenderSetup.getOrPrepare(holder, hand, false);
        if (attachable != null) {
            AttachableItemRenderSetup.renderAttachable(attachable, poseStack, action.multiBufferSource(), holder,
                    itemLight, OverlayTexture.NO_OVERLAY);
            return true;
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        RenderPorts.get().renderSystemPort()
                .renderItemDirect(holder, item, displayContext, offhand, poseStack, action.sink(), itemLight);
        return true;
    }

    /** holder 的 Molang 作用域（`held_item_scale` 求值用）；未托管实体返回 null。 */
    private static @Nullable MolangScope holderScope(LivingEntity holder) {
        var rd = RenderData.peekComponent(holder);
        return rd == null ? null : rd.getScope();
    }
}

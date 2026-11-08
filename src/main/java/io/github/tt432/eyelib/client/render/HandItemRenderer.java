package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * 手持物（主手/副手物品与 attachable）渲染簇。
 * <p>
 * 挂载点按基岩版语义取自 {@code q.item_slot_to_bone_name(context.item_slot)} 对应的
 * <b>item 骨骼</b>（{@code rightitem}/{@code leftitem}，骨骼名大小写不敏感）。该骨骼是模型作者
 * 在几何里给出的挂点（基岩版 humanoid 几何的 {@code rightItem} 位于手臂链末端）。骨骼缺失时不绘制：
 * 基岩版没有「找不到挂点就退回模型原点」的可用语义，任何兜底位置都是错位。
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
    private static final int leftitem = GlobalBoneIdHandler.get("leftitem");
    private static final int rightitem = GlobalBoneIdHandler.get("rightitem");

    private HandItemRenderer() {
    }

    /**
     * 每实体每帧渲染一次双手，必须在模型几何提交之前调用（26.1 的 submit 节点在几何 writer 内
     * 已不能接收新内容）。
     *
     * @param modelRoot   模型根位姿（已含实体位姿；模型 Y 翻转由 {@link ModelPoseTransforms} 施加）
     * @param tickedInfos 本帧动画采样结果
     */
    static void render(SimpleRenderAction<?> action, LivingEntity holder, Model model, ModelRuntimeData tickedInfos,
                       Matrix4f modelRoot, int light) {
        renderHand(action, holder, model, tickedInfos, modelRoot, InteractionHand.OFF_HAND, leftitem,
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND, light, true);
        renderHand(action, holder, model, tickedInfos, modelRoot, InteractionHand.MAIN_HAND, rightitem,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, light, false);
    }

    private static void renderHand(SimpleRenderAction<?> action, LivingEntity holder, Model model,
                                   ModelRuntimeData tickedInfos, Matrix4f modelRoot, InteractionHand hand, int boneId,
                                   ItemDisplayContext displayContext, int light, boolean left) {
        ItemStack item = holder.getItemInHand(hand);
        if (item.isEmpty()) {
            return;
        }
        Model.Bone bindBone = model.allBones().get(boneId);
        if (bindBone == null) {
            return;
        }
        var frame = ModelPoseTransforms.resolveBonePose(model, tickedInfos, boneId, modelRoot);
        if (frame.isEmpty()) {
            return;
        }

        PoseStack poseStack = RenderPorts.get().renderSystemPort().createPoseStackFromMatrix(frame.get());
        var pivot = bindBone.pivot();
        poseStack.translate(pivot.x(), pivot.y(), pivot.z());

        var attachable = AttachableItemRenderSetup.getOrPrepare(holder, hand, false);
        if (attachable != null) {
            AttachableItemRenderSetup.renderAttachable(attachable, poseStack, action.multiBufferSource(), holder, light,
                    OverlayTexture.NO_OVERLAY);
            return;
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        RenderPorts.get().renderSystemPort()
                .renderItemDirect(holder, item, displayContext, left, poseStack, action.sink(), light);
    }
}

package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 搬运方块（末影人等）的附加渲染。
 *
 * <p>Bedrock 的 {@code query.is_carrying_block} / 搬运动画只负责姿态，方块本体由引擎按
 * 实体当前搬运的 block state 绘制（资源包不定义方块几何）。eyelib 接管实体渲染后原版
 * {@code CarriedBlockLayer} 不会执行，因此这里补上同等职责：
 * <ul>
 *   <li>方块来源：宿主的实体搬运状态（{@code getCarriedBlockState}），不写死方块 id；</li>
 *   <li>方块模型/贴图/渲染类型：宿主 {@code BlockRenderDispatcher}，任意 block state 可用；</li>
 *   <li>位置：模型双手骨骼的当前世界坐标中点——随模型与搬运剪辑走，不写死模型空间偏移；</li>
 *   <li>朝向/缩放：模型根骨骼的当前姿态，方块随实体朝向与缩放。</li>
 * </ul>
 *
 * <p>资源包没有绑定双手骨骼时不渲染方块（不做坐标兜底），避免把方块放到错误位置。
 * 本渲染在实体 submit 阶段直接执行（不放进 deferred geometry writer），因此 26.1 的
 * {@code SubmitNodeCollector} 仍能接收方块模型。
 *
 * @author TT432
 */
final class CarriedBlockRenderer {
    private static final int LEFT_HAND = GlobalBoneIdHandler.get("lefthand");
    private static final int RIGHT_HAND = GlobalBoneIdHandler.get("righthand");
    /** Bedrock/原版搬运方块的显示尺寸（1 方块模型按 0.5 缩放）。 */
    private static final float BLOCK_SCALE = 0.5F;

    private CarriedBlockRenderer() {
    }

    /**
     * 尝试渲染一次搬运方块。
     *
     * @param entityPose 已施加实体位姿/身体旋转/缩放的 pose（{@code renderComponents} 内）
     * @param model      当前 model component 的模型（提供骨骼层级与 bind pivot）
     * @param infos      当前帧动画结果；无动画时传 {@link ModelRuntimeData#EMPTY}
     * @return 是否实际提交了方块
     */
    static boolean render(Entity entity, PoseStack entityPose, Model model,
                          ModelRuntimeData infos, int light, RenderSink sink) {
        BlockState state = RenderPorts.get().renderSystemPort().getCarriedBlockState(entity);
        if (state == null) {
            return false;
        }

        Model.Bone leftHand = model.allBones().get(LEFT_HAND);
        Model.Bone rightHand = model.allBones().get(RIGHT_HAND);
        if (leftHand == null || rightHand == null) {
            return false;
        }

        // 与 ModelVisitor.visitPreModel 一致：实体位姿之后先绕 Y 转 180°，再走骨骼层级。
        Matrix4f modelRoot = new Matrix4f(entityPose.last().pose()).rotateY((float) Math.PI);
        Vector3f left = null;
        Vector3f right = null;
        Matrix4f rootPose = null;

        Deque<Object[]> stack = new ArrayDeque<>();
        for (Model.Bone bone : model.toplevelBones().values()) {
            stack.push(new Object[]{bone, new Matrix4f(modelRoot)});
        }
        while (!stack.isEmpty()) {
            Object[] item = stack.pop();
            Model.Bone bone = (Model.Bone) item[0];
            Matrix4f parent = (Matrix4f) item[1];
            Matrix4f pose = new Matrix4f(parent);
            ModelPoseTransforms.applyBone(pose, bone, infos);
            if (rootPose == null) {
                rootPose = new Matrix4f(pose);
            }
            if (bone.id() == LEFT_HAND) {
                left = pose.transformPosition(new Vector3f(bone.pivot()));
            } else if (bone.id() == RIGHT_HAND) {
                right = pose.transformPosition(new Vector3f(bone.pivot()));
            }
            for (Model.Bone child : bone.children().values()) {
                stack.push(new Object[]{child, pose});
            }
        }

        if (left == null || right == null || rootPose == null) {
            return false;
        }

        Vector3f center = left.add(right).mul(0.5F);
        // 根骨骼矩阵提供朝向/缩放；平移替换为双手中点（骨骼矩阵不含 pivot 平移）。
        Matrix4f transform = new Matrix4f(rootPose);
        transform.setTranslation(center);
        PoseStack poseStack = RenderPorts.get().renderSystemPort().createPoseStackFromMatrix(transform);
        // 方块模型自身以 [0,1]^3 为体：围绕体心旋转（原版 CarriedBlockLayer 的 20°X/45°Y 朝向，
        // 基岩坐标下 X/Y 取反），再缩放并把体心平移到双手中点。
        poseStack.scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE);
        poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        RenderPorts.get().renderSystemPort().renderBlockState(state, poseStack, sink, light);
        return true;
    }
}

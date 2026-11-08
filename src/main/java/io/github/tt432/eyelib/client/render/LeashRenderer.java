package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.render.LeashRenderPort;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.locator.LocatorEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 栓绳渲染：eyelib 取消原版 {@code RenderLivingEvent.Pre} 后，原版
 * {@code MobRenderer/EntityRenderer} 里的栓绳绘制随之失效（1.21.1 起的
 * {@code LivingEntityRenderer.render} 早退会连带跳过 {@code EntityRenderer.render} 的 leash 段），
 * 1.20.1 的 {@code MobRenderer.render} 虽仍在早退后绘制，但用的是原版模型偏移。
 *
 * <p>锚点按 Bedrock 约定取模型上名为 {@code lead} 的 locator（vanilla 资源包用 locator
 * 控制牵引点；cospack 模型已带该 locator）；没有 locator 的资源包回落到
 * {@link Entity#getLeashOffset(float)} + 身体朝向的原版算法，避免无锚点模型画到错误位置。
 *
 * <p>1.20.1 的原版栓绳由 {@code MobRenderer.render} 在事件早退后继续绘制，需在混入层
 * 抑制一次（只对 eyelib 实际接管的实体），否则与这里的绳重复。
 *
 * @author TT432
 */
final class LeashRenderer {
    private static final String LEAD_LOCATOR = "lead";

    private LeashRenderer() {
    }

    /**
     * 当前模型的 locator 树里是否存在任意名为 {@code lead} 的 locator。
     */
    static boolean hasLead(@Nullable Model model) {
        if (model == null) {
            return false;
        }
        for (Model.Bone bone : model.allBones().values()) {
            List<LocatorEntry> locators = bone.locator().cubes();
            if (locators == null) {
                continue;
            }
            for (LocatorEntry locator : locators) {
                if (LEAD_LOCATOR.equals(locator.name())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 每个实体渲染时调用一次（模型几何已提交后）。
     *
     * @param basePose     实体根位姿（仅相机旋转 + 实体位置平移；{@code setupEntityClientEntityData} 之前）
     * @param entityPose   施加了脚本缩放/身体朝向后的实体根位姿；为 {@code null} 表示本帧无可用模型
     * @param model        用于解析 {@code lead} locator 的模型
     * @param infos        当前帧动画结果（无动画时传 {@link ModelRuntimeData#EMPTY}）
     */
    static void render(SimpleRenderAction<?> data, @Nullable Model model, @Nullable ModelRuntimeData infos,
                       Matrix4f basePose, @Nullable Matrix4f entityPose) {
        Entity entity = data.entity();
        if (!(entity instanceof Mob mob)) {
            return;
        }
        Entity holder = mob.getLeashHolder();
        if (holder == null) {
            return;
        }

        float partialTick = data.partialTick();
        ModelRuntimeData animation = infos != null ? infos : ModelRuntimeData.EMPTY;
        Vector3f anchor = anchorOffset(mob, model, animation, basePose, entityPose, partialTick);
        Vec3 entityPos = entity.getPosition(partialTick);
        Vec3 holderRope = holder.getRopeHoldPosition(partialTick);

        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        int startPacked = dispatcher.getPackedLightCoords(entity, partialTick);
        int endPacked = dispatcher.getPackedLightCoords(holder, partialTick);

        LeashRenderPort.renderRope(data.poseStack(), data.sink(), anchor, entityPos, holderRope,
                startPacked, endPacked);
    }

    /**
     * 锚点相对实体原点的世界轴位移（方块单位）。
     *
     * <p>locator 分支：{@code resolveLocatorPose} 产出相机空间坐标，乘 {@code basePose} 的逆矩阵
     * 得到「相机旋转/位置之外」的位移，即实体局部（含脚本缩放与身体朝向）在世界轴上的位移。
     * 该位移直接作为原版 {@code poseStack.translate(d1, y, d2)} 的入参。
     */
    private static Vector3f anchorOffset(Mob entity, @Nullable Model model, ModelRuntimeData infos,
                                         Matrix4f basePose, @Nullable Matrix4f entityPose, float partialTick) {
        if (model != null && entityPose != null) {
            Optional<Matrix4f> locator = ModelPoseTransforms.resolveLocatorPose(model, infos, LEAD_LOCATOR, entityPose);
            if (locator.isPresent()) {
                Vector3f cameraPoint = locator.get().getTranslation(new Vector3f());
                return new Matrix4f(basePose).invert().transformPosition(cameraPoint);
            }
        }

        // 原版算法：leashOffset 在实体局部空间，按身体朝向旋转到世界轴。
        Vec3 offset = entity.getLeashOffset(partialTick);
        float bodyRot = LeashRenderPort.bodyRotationDegrees(entity, partialTick);
        double yaw = (double) (bodyRot * (float) (Math.PI / 180.0)) + (Math.PI / 2.0);
        double d1 = Math.cos(yaw) * offset.z + Math.sin(yaw) * offset.x;
        double d2 = Math.sin(yaw) * offset.z - Math.cos(yaw) * offset.x;
        return new Vector3f((float) d1, (float) offset.y, (float) d2);
    }
}

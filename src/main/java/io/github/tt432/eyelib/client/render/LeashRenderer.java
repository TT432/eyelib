package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.locator.LocatorEntry;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.LightCoordsUtil;
//?} else {
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
//?}
import net.minecraft.util.Mth;
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
    /** 原版栓绳分段数（{@code MobRenderer.LEASH_RENDER_STEPS}）。 */
    private static final int LEASH_RENDER_STEPS = 24;
    /** 原版栓绳半宽（0.05 格总宽的一半）。 */
    private static final float LEASH_WIDTH = 0.025F;

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

        //? if >=26.1 {
        SubmitNodeCollector collector = data.sink().submitNodeCollector();
        EntityRenderState.LeashState leash = new EntityRenderState.LeashState();
        leash.offset = new Vec3(anchor.x, anchor.y, anchor.z);
        leash.start = entityPos.add(leash.offset);
        leash.end = holderRope;
        leash.startBlockLight = LightCoordsUtil.block(startPacked);
        leash.endBlockLight = LightCoordsUtil.block(endPacked);
        leash.startSkyLight = LightCoordsUtil.sky(startPacked);
        leash.endSkyLight = LightCoordsUtil.sky(endPacked);
        // 原版非 quad 路径不置 slack（默认 false）：悬垂曲线与 1.20.1/1.21.1 的原版实现不同，
        // 这里跟随 26.1 自身 EntityRenderer 的取值，不自行发明。
        leash.slack = false;
        collector.submitLeash(data.poseStack(), leash);
        //?} else {
        PoseStack poseStack = data.poseStack();
        poseStack.pushPose();
        poseStack.translate(anchor.x, anchor.y, anchor.z);

        float f = (float) (holderRope.x - (entityPos.x + anchor.x));
        float f1 = (float) (holderRope.y - (entityPos.y + anchor.y));
        float f2 = (float) (holderRope.z - (entityPos.z + anchor.z));
        VertexConsumer vertexConsumer = data.multiBufferSource().getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();
        float f3 = Mth.invSqrt(f * f + f2 * f2) * LEASH_WIDTH / 2.0F;
        float f4 = f2 * f3;
        float f5 = f * f3;

        int startBlock = LightTexture.block(startPacked);
        int endBlock = LightTexture.block(endPacked);
        int startSky = LightTexture.sky(startPacked);
        int endSky = LightTexture.sky(endPacked);

        for (int i = 0; i <= LEASH_RENDER_STEPS; i++) {
            addVertexPair(vertexConsumer, matrix, f, f1, f2, startBlock, endBlock, startSky, endSky,
                    LEASH_WIDTH, LEASH_WIDTH, f4, f5, i, false);
        }
        for (int i = LEASH_RENDER_STEPS; i >= 0; i--) {
            addVertexPair(vertexConsumer, matrix, f, f1, f2, startBlock, endBlock, startSky, endSky,
                    LEASH_WIDTH, 0.0F, f4, f5, i, true);
        }

        poseStack.popPose();
        //?}
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
        //? if <1.21 {
        float bodyRot = Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        //?} else {
        float bodyRot = entity.getPreciseBodyRotation(partialTick);
        //?}
        double yaw = (double) (bodyRot * (float) (Math.PI / 180.0)) + (Math.PI / 2.0);
        double d1 = Math.cos(yaw) * offset.z + Math.sin(yaw) * offset.x;
        double d2 = Math.sin(yaw) * offset.z - Math.cos(yaw) * offset.x;
        return new Vector3f((float) d1, (float) offset.y, (float) d2);
    }

    //? if <26.1 {
    /** 与原版 {@code MobRenderer/EntityRenderer.addVertexPair} 同构。 */
    private static void addVertexPair(VertexConsumer buffer, Matrix4f pose,
                                      float startX, float startY, float startZ,
                                      int entityBlockLight, int holderBlockLight,
                                      int entitySkyLight, int holderSkyLight,
                                      float yOffset, float dy, float dx, float dz,
                                      int index, boolean reverse) {
        float f = (float) index / (float) LEASH_RENDER_STEPS;
        int blockLight = (int) Mth.lerp(f, (float) entityBlockLight, (float) holderBlockLight);
        int skyLight = (int) Mth.lerp(f, (float) entitySkyLight, (float) holderSkyLight);
        int packedLight = LightTexture.pack(blockLight, skyLight);
        float brightness = index % 2 == (reverse ? 1 : 0) ? 0.7F : 1.0F;
        float red = 0.5F * brightness;
        float green = 0.4F * brightness;
        float blue = 0.3F * brightness;
        float x = startX * f;
        float y = startY > 0.0F ? startY * f * f : startY - startY * (1.0F - f) * (1.0F - f);
        float z = startZ * f;
        //? if <1.21 {
        buffer.vertex(pose, x - dx, y + dy, z + dz).color(red, green, blue, 1.0F).uv2(packedLight).endVertex();
        buffer.vertex(pose, x + dx, y + yOffset - dy, z - dz).color(red, green, blue, 1.0F).uv2(packedLight).endVertex();
        //?} else {
        buffer.addVertex(pose, x - dx, y + dy, z + dz).setColor(red, green, blue, 1.0F).setLight(packedLight);
        buffer.addVertex(pose, x + dx, y + yOffset - dy, z - dz).setColor(red, green, blue, 1.0F).setLight(packedLight);
        //?}
    }
    //?}
}

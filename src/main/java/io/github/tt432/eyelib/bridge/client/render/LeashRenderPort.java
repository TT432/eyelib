package io.github.tt432.eyelib.bridge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.LightCoordsUtil;
//?} else {
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
//?}

/**
 * 栓绳绘制 Port：收敛 {@code client/render/LeashRenderer} 的版本差异（ADR-0016 §6 的 ACL 栖息地）。
 *
 * <p>三节点语义各自照抄原实现，不引入新观感：{@code <26.1} 立即写原版那条 25 段 × 2 的四边形带；
 * {@code >=26.1} 提交 {@code EntityRenderState.LeashState} 由原版 LeashFeatureRenderer 绘制。
 *
 * @author TT432
 */
public interface LeashRenderPort {
    /** 原版栓绳分段数（{@code MobRenderer.LEASH_RENDER_STEPS}）。 */
    int LEASH_RENDER_STEPS = 24;
    /** 原版栓绳半宽（0.05 格总宽的一半）。 */
    float LEASH_WIDTH = 0.025F;

    /**
     * 身体朝向（世界 yaw 度，partialTick 插值）。
     *
     * <p>{@code getPreciseBodyRotation} 与 {@code Mth.lerp(partialTick, yBodyRotO, yBodyRot)} 等价，
     * 只是 1.21 起才有该方法。
     */
    static float bodyRotationDegrees(LivingEntity entity, float partialTick) {
        //? if <1.21 {
        return Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        //?} else {
        return entity.getPreciseBodyRotation(partialTick);
        //?}
    }

    /**
     * 画一条栓绳。
     *
     * @param poseStack           实体根位姿（{@code data.poseStack()}）
     * @param sink                当前实体渲染 sink（延迟实现取 {@code SubmitNodeCollector}，立即实现取 {@code MultiBufferSource}）
     * @param anchorOffset        锚点相对实体原点的世界轴位移
     * @param entityPosition      {@code entity.getPosition(partialTick)}
     * @param holderRopePosition  {@code holder.getRopeHoldPosition(partialTick)}
     * @param startPackedLight    实体打包光照
     * @param endPackedLight      持绳者打包光照
     */
    static void renderRope(PoseStack poseStack, RenderSink sink, Vector3f anchorOffset,
                           Vec3 entityPosition, Vec3 holderRopePosition,
                           int startPackedLight, int endPackedLight) {
        //? if >=26.1 {
        SubmitNodeCollector collector = sink.submitNodeCollector();
        EntityRenderState.LeashState leash = new EntityRenderState.LeashState();
        leash.offset = new Vec3(anchorOffset.x, anchorOffset.y, anchorOffset.z);
        leash.start = entityPosition.add(leash.offset);
        leash.end = holderRopePosition;
        leash.startBlockLight = LightCoordsUtil.block(startPackedLight);
        leash.endBlockLight = LightCoordsUtil.block(endPackedLight);
        leash.startSkyLight = LightCoordsUtil.sky(startPackedLight);
        leash.endSkyLight = LightCoordsUtil.sky(endPackedLight);
        // 原版非 quad 路径不置 slack（默认 false）：悬垂曲线与 1.20.1/1.21.1 的原版实现不同，
        // 这里跟随 26.1 自身 EntityRenderer 的取值，不自行发明。
        leash.slack = false;
        collector.submitLeash(poseStack, leash);
        //?} else {
        poseStack.pushPose();
        poseStack.translate(anchorOffset.x, anchorOffset.y, anchorOffset.z);

        float f = (float) (holderRopePosition.x - (entityPosition.x + anchorOffset.x));
        float f1 = (float) (holderRopePosition.y - (entityPosition.y + anchorOffset.y));
        float f2 = (float) (holderRopePosition.z - (entityPosition.z + anchorOffset.z));
        VertexConsumer vertexConsumer = sink.multiBufferSource().getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();
        float f3 = Mth.invSqrt(f * f + f2 * f2) * LEASH_WIDTH / 2.0F;
        float f4 = f2 * f3;
        float f5 = f * f3;

        int startBlock = LightTexture.block(startPackedLight);
        int endBlock = LightTexture.block(endPackedLight);
        int startSky = LightTexture.sky(startPackedLight);
        int endSky = LightTexture.sky(endPackedLight);

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

package io.github.tt432.eyelib.bridge.particle.adapter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.Emitter;
import io.github.tt432.eyelib.wintersky.Particle;
import io.github.tt432.eyelib.wintersky.Scene;
import io.github.tt432.eyelib.wintersky.three.BufferAttribute;
import io.github.tt432.eyelib.wintersky.three.ShaderMaterial;
import net.minecraft.client.Minecraft;
//? if <26.1 {
import net.minecraft.client.renderer.LightTexture;
//?}
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;

/**
 * wintersky 粒子渲染器（ADR-0035 §3.2）：遍历 Scene → emitters → particles，
 * 按 {@code mesh.matrixWorld}（wintersky three 场景图，1 单位 = 1 方块）写 quad。
 *
 * <p>wintersky 本身不做 GL 绘制（原是 three WebGLRenderer 的职责），本类即渲染后端：
 * 顶点局部坐标 (±1,±1,0) 经 matrixWorld 完成全部位移/旋转/缩放；
 * 顶点色/UV 读 {@code clr}/{@code uv} BufferAttribute（wintersky 逐 tick 重写）；
 * 光照恒 FULL_BRIGHT（D2）。
 */
public final class WinterskyParticleRenderer {

    private final PoseStack poseStack;

    public WinterskyParticleRenderer(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    public void render() {
        Scene scene = WinterskySceneManager.scene();
        if (scene.emitters.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        var camera = minecraft.gameRenderer.getMainCamera();
        //? if <26.1 {
        Vec3 cameraPos = camera.getPosition();
        //?} else {
        Vec3 cameraPos = camera.position();
        //?}
        WinterskySceneManager.updateCameraProxy(cameraPos, camera.rotation());
        scene.updateFacingRotation(WinterskySceneManager.cameraProxy());
        scene.space.updateMatrixWorld(true);

        for (Emitter emitter : new ArrayList<>(scene.emitters)) {
            renderEmitter(emitter, cameraPos);
        }
    }

    private void renderEmitter(Emitter emitter, Vec3 cameraPos) {
        if (!emitter.initialized || emitter.particles.isEmpty()) {
            return;
        }
        ShaderMaterial material = emitter.material;
        if (material == null) {
            return;
        }
        PortResourceLocation texture = resolveTexture(emitter);
        if (texture == null) {
            return;
        }
        //? if <26.1 {
        VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource()
                .getBuffer((net.minecraft.client.renderer.RenderType)
                        WinterskyRenderTypes.get(emitter.config.particle_appearance_material, texture));
        //?} else {
        VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource()
                .getBuffer((net.minecraft.client.renderer.rendertype.RenderType)
                        WinterskyRenderTypes.get(emitter.config.particle_appearance_material, texture));
        //?}

        for (Particle particle : new ArrayList<>(emitter.particles)) {
            renderParticle(particle, buffer, cameraPos);
        }
    }

    private void renderParticle(Particle particle, VertexConsumer buffer, Vec3 cameraPos) {
        BufferAttribute position = particle.geometry.getAttribute("position");
        BufferAttribute uv = particle.geometry.getAttribute("uv");
        BufferAttribute clr = particle.geometry.getAttribute("clr");
        if (position == null || uv == null || clr == null) {
            return;
        }

        poseStack.pushPose();
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        pose.translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);
        pose.mul(toJoml(particle.mesh.matrixWorld.elements, new Matrix4f()));

        Matrix3f normalPose = last.normal().set(pose).invert().transpose();
        Vector3f normal = new Vector3f(0, 0, 1).mul(normalPose);
        int light = fullBright();

        // PlaneGeometry 顶点序为 TL,TR,BL,BR；GL_QUADS (0,1,2,3) 按周界序组装，
        // 直写会拼成自交 bowtie（绕序翻转被 cull）。映射到生产同款周界序 (1,0,2,3)。
        final int[] order = {1, 0, 2, 3};
        for (int i = 0; i < 4; i++) {
            int vi = order[i];
            float x = position.array.get(vi * 3);
            float y = position.array.get(vi * 3 + 1);
            float z = position.array.get(vi * 3 + 2);
            Vector3f p = new Vector3f(x, y, z).mulPosition(pose);
            int color = packColor(
                    clr.array.get(vi * 4),
                    clr.array.get(vi * 4 + 1),
                    clr.array.get(vi * 4 + 2),
                    clr.array.get(vi * 4 + 3));
            // three.js 默认 flipY=true：uv.v=0 对应图像底部；MC 纹理不翻转 → v' = 1 - v
            BedrockParticleRenderer.vertex(buffer, p, color,
                    uv.array.get(vi * 2), 1.0F - uv.array.get(vi * 2 + 1), light, normal);
        }
        poseStack.popPose();
    }

    /** wintersky Matrix4（double[16] 列主序）→ JOML。 */
    private static Matrix4f toJoml(double[] e, Matrix4f out) {
        return out.set(
                (float) e[0], (float) e[1], (float) e[2], (float) e[3],
                (float) e[4], (float) e[5], (float) e[6], (float) e[7],
                (float) e[8], (float) e[9], (float) e[10], (float) e[11],
                (float) e[12], (float) e[13], (float) e[14], (float) e[15]);
    }

    private static int packColor(float r, float g, float b, float a) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(float channel) {
        if (Float.isNaN(channel)) {
            return 0;
        }
        return Math.min(255, Math.max(0, Math.round(channel * 255.0F)));
    }

    /**
     * {@code texture.image.src} → PortResourceLocation：
     * 带冒号直解析（fetchTexture 钩子返回值）；{@code wintersky/} 前缀 → eyelib 内置纹理。
     */
    private static PortResourceLocation resolveTexture(Emitter emitter) {
        var image = emitter.config.texture.image;
        String src = image != null ? image.getSrc() : null;
        if (src == null || src.isEmpty()) {
            src = Config.MISSING_TEX;
        }
        if (src.indexOf(':') >= 0) {
            return PortResourceLocation.parse(src);
        }
        return PortResourceLocation.of("eyelib", src);
    }


    private static int fullBright() {
        //? if <26.1 {
        return LightTexture.FULL_BRIGHT;
        //?} else {
        return net.minecraft.util.LightCoordsUtil.FULL_BRIGHT;
        //?}
    }
}

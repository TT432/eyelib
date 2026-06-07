package io.github.tt432.eyelibparticle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelibmaterial.render.RenderTypeResolver;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleInstance;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.ParticleComponentManager;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.appearance.ParticleAppearanceBillboard;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.appearance.ParticleAppearanceLighting;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.particle.appearance.ParticleAppearanceTinting;
import io.github.tt432.eyelibparticle.runtime.support.ParticleMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

/**
 * Minecraft render-buffer adapter for module-owned Bedrock particle instances.
 */
/** @author TT432 */
public final class BedrockParticleRenderer implements ParticleRenderManager.ParticleRenderer {
    private final PoseStack poseStack;

    public BedrockParticleRenderer(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    @Override
    public void render(BedrockParticleInstance particle) {
        String material = particle.emitter().definition().material();
        RenderTypeResolver.EntityRenderTypeData factory = RenderTypeResolver.resolveParticle(material);
        ResourceLocation texture = new ResourceLocation(particle.emitter().definition().texture()).withSuffix(".png");
        VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(
                factory.factory().apply(texture)
        );
        render(particle, poseStack, buffer);
    }

    private static void render(BedrockParticleInstance particle, PoseStack poseStack, VertexConsumer vertexConsumer) {
        BedrockParticleEmitter emitter = particle.emitter();
        List<ParticleParticleComponent> components = ParticleComponentManager.particleComponents(emitter.definition());
        ParticleAppearanceBillboard billboard = component(components, ParticleAppearanceBillboard.class);
        ParticleAppearanceLighting lighting = component(components, ParticleAppearanceLighting.class);
        ParticleAppearanceTinting tinting = component(components, ParticleAppearanceTinting.class);

        poseStack.pushPose();
        Vector3f cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f();

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normalPose = last.normal();

        pose.translate(new Vector3f(cameraPos).negate()).translate(particle.position());

        if (emitter.space().position() && emitter.space().rotation()) {
            pose.mul(emitter.baseRotation()).rotateY(ParticleMath.PI);
        } else {
            pose.translate(emitter.position());
        }

        if (billboard != null) {
            ParticleAppearanceBillboard.CameraAccess camera = new ParticleAppearanceBillboard.CameraAccess(
                    Minecraft.getInstance().gameRenderer.getMainCamera().rotation(),
                    cameraPos
            );
            pose.rotate(billboard.getRotation(particle, camera, Minecraft.getInstance().getPartialTick()));
        }

        pose.rotateZ((float) Math.toRadians(particle.rotation()));

        Vector2f size = billboard == null ? new Vector2f(1, 1) : billboard.getSize(particle);
        Vector4f uv = billboard == null ? new Vector4f(0, 0, 1, 1) : billboard.getUV(particle);
        int color = tinting == null ? 0xFF_FF_FF_FF : tinting.getColor(particle);

        float x = size.x;
        float y = size.y;
        Vector3f p0 = new Vector3f(x, y, 0).mulPosition(pose);
        Vector3f p1 = new Vector3f(-x, y, 0).mulPosition(pose);
        Vector3f p2 = new Vector3f(-x, -y, 0).mulPosition(pose);
        Vector3f p3 = new Vector3f(x, -y, 0).mulPosition(pose);

        normalPose.set(pose).invert().transpose();
        Vector3f normal = new Vector3f(0, 0, 1).mul(normalPose);
        int light = getLight(particle, lighting);

        vertex(vertexConsumer, p0, color, uv.x, uv.y, light, normal);
        vertex(vertexConsumer, p1, color, uv.x + uv.z, uv.y, light, normal);
        vertex(vertexConsumer, p2, color, uv.x + uv.z, uv.y + uv.w, light, normal);
        vertex(vertexConsumer, p3, color, uv.x, uv.y + uv.w, light, normal);

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vertexConsumer, Vector3f position, int color, float u, float v, int light, Vector3f normal) {
        vertexConsumer.vertex(position.x, position.y, position.z,
                FastColor.ABGR32.red(color),
                FastColor.ABGR32.green(color),
                FastColor.ABGR32.blue(color),
                FastColor.ABGR32.alpha(color),
                u, v,
                OverlayTexture.NO_OVERLAY, light, normal.x, normal.y, normal.z);
    }

    private static int getLight(BedrockParticleInstance particle, @Nullable ParticleAppearanceLighting lighting) {
        if (lighting != null) {
            return LightTexture.FULL_BRIGHT;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return LightTexture.FULL_BRIGHT;
        }
        BlockPos blockPosition = new BlockPos(
                Mth.floor(particle.position().x),
                Mth.floor(particle.position().y),
                Mth.floor(particle.position().z)
        );
        return LightTexture.pack(
                level.getBrightness(LightLayer.BLOCK, blockPosition),
                level.getBrightness(LightLayer.SKY, blockPosition)
        );
    }

    @Nullable
    private static <T> T component(List<ParticleParticleComponent> components, Class<T> type) {
        return components.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElse(null);
    }
}

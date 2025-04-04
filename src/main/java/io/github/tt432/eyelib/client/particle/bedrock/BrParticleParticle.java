package io.github.tt432.eyelib.client.particle.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.appearance.ParticleAppearanceBillboard;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.appearance.ParticleAppearanceLighting;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.appearance.ParticleAppearanceTinting;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.util.Blackboard;
import io.github.tt432.eyelib.util.SimpleTimer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public class BrParticleParticle {
    @Getter
    private final BrParticleEmitter emitter;
    @Getter
    private final Vector3f position = new Vector3f();
    @Getter
    private final Vector3f velocity = new Vector3f();

    @Getter
    @Setter
    private float rotation;
    @Getter
    @Setter
    private float rotationRate;

    private final List<ParticleParticleComponent> components;

    public final MolangScope molangScope = new MolangScope();

    private final ParticleAppearanceBillboard billboard;
    private final ParticleAppearanceLighting lighting;
    private final ParticleAppearanceTinting tinting;

    @Getter
    private final Blackboard blackboard = new Blackboard();

    @Getter
    private final SimpleTimer timer;

    @Getter
    @Setter
    private float speed;

    @Getter
    private boolean removed;
    @Setter
    @Getter
    private float lifetime;

    @Getter
    private final float random1;
    @Getter
    private final float random2;
    @Getter
    private final float random3;
    @Getter
    private final float random4;

    public BrParticleParticle(BrParticleEmitter emitter) {
        this.emitter = emitter;

        timer = new SimpleTimer();

        random1 = emitter.getRandom().nextFloat();
        random2 = emitter.getRandom().nextFloat();
        random3 = emitter.getRandom().nextFloat();
        random4 = emitter.getRandom().nextFloat();

        molangScope.setParent(emitter.molangScope);
        molangScope.setOwner(this);
        molangScope.set("variable.particle_age", this::getAge);
        molangScope.set("variable.particle_lifetime", this::getLifetime);
        molangScope.set("variable.particle_random_1", this::getRandom1);
        molangScope.set("variable.particle_random_2", this::getRandom2);
        molangScope.set("variable.particle_random_3", this::getRandom3);
        molangScope.set("variable.particle_random_4", this::getRandom4);

        BrParticle.ParticleEffect particleEffect = emitter.getParticle().particleEffect();
        billboard = particleEffect.<ParticleAppearanceBillboard>getComponent(ResourceLocation
                        .withDefaultNamespace("particle_appearance_billboard"))
                .orElse(null);
        lighting = particleEffect.<ParticleAppearanceLighting>getComponent(ResourceLocation
                        .withDefaultNamespace("particle_appearance_lighting"))
                .orElse(null);
        tinting = particleEffect.<ParticleAppearanceTinting>getComponent(ResourceLocation
                        .withDefaultNamespace("particle_appearance_tinting"))
                .orElse(null);

        components = new ArrayList<>();
        for (ParticleComponent particleComponent : particleEffect.components().values()) {
            if (particleComponent instanceof ParticleParticleComponent particleParticleComponent) {
                components.add(particleParticleComponent);
            }
        }

        components.forEach(c -> c.onStart(this));
    }

    public Level level() {
        return emitter.getLevel();
    }

    public float getAge() {
        return Math.min(lifetime, timer.getNanoTime() / 1_000_000_000F);
    }

    public BlockPos getBlockPosition() {
        return new BlockPos(
                Mth.floor(position.x),
                Mth.floor(position.y),
                Mth.floor(position.z)
        );
    }

    public void remove() {
        if (!removed) {
            removed = true;
            emitter.onParticleRemove();
        }
    }

    public void onRenderFrame() {
        components.forEach(p -> p.onFrame(this));
    }

    public ResourceLocation getTexture() {
        return emitter.getParticle().particleEffect().description().basicRenderParameters().texture();
    }

    public void render(PoseStack poseStack, VertexConsumer vertexConsumer) {
        poseStack.pushPose();
        Vector3f cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f();

        PoseStack.Pose last = poseStack.last();
        Matrix4f m4 = last.pose();
        Matrix3f m3 = last.normal();

        if (billboard != null) {
            billboard.transform(this, poseStack);
        }

        m4.rotateZ((float) Math.toRadians(rotation));

        var size = billboard == null ? new Vector2f(1, 1) : billboard.getSize(this);

        Vector4f uv = billboard != null ? billboard.getUV(this) : new Vector4f(0, 0, 1, 1);

        var color = tinting != null ? tinting.getColor(this) : 0xFF_FF_FF_FF;

        float x = size.x;
        float y = size.y;
        var p0 = new Vector3f(x, y, 0).mulPosition(m4).add(position).add(emitter.getPosition()).sub(cameraPos);
        var p1 = new Vector3f(-x, y, 0).mulPosition(m4).add(position).add(emitter.getPosition()).sub(cameraPos);
        var p2 = new Vector3f(-x, -y, 0).mulPosition(m4).add(position).add(emitter.getPosition()).sub(cameraPos);
        var p3 = new Vector3f(x, -y, 0).mulPosition(m4).add(position).add(emitter.getPosition()).sub(cameraPos);

        Vector3f normal = new Vector3f(0, 0, 1).mul(m3);
        int light;

        if (lighting != null) {
            light = LightTexture.FULL_BRIGHT;
        } else {
            BlockPos blockPosition = getBlockPosition();
            Level level = emitter.getLevel();
            light = LightTexture.pack(
                    level.getBrightness(LightLayer.BLOCK, blockPosition),
                    level.getBrightness(LightLayer.SKY, blockPosition)
            );
        }

        vertexConsumer.addVertex(p0.x, p0.y, p0.z, color, uv.x, uv.y,
                OverlayTexture.NO_OVERLAY, light, normal.x, normal.y, normal.z);
        vertexConsumer.addVertex(p1.x, p1.y, p1.z, color, uv.x + uv.z, uv.y,
                OverlayTexture.NO_OVERLAY, light, normal.x, normal.y, normal.z);
        vertexConsumer.addVertex(p2.x, p2.y, p2.z, color, uv.x + uv.z, uv.y + uv.w,
                OverlayTexture.NO_OVERLAY, light, normal.x, normal.y, normal.z);
        vertexConsumer.addVertex(p3.x, p3.y, p3.z, color, uv.x, uv.y + uv.w,
                OverlayTexture.NO_OVERLAY, light, normal.x, normal.y, normal.z);

        poseStack.popPose();
    }
}

package io.github.tt432.eyelib.common.bedrock.particle;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import io.github.tt432.eyelib.common.bedrock.particle.component.particle.*;
import io.github.tt432.eyelib.common.bedrock.particle.component.particle.motion.ParticleMotionComponent;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.util.math.Vec4d;
import lombok.Builder;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author DustW
 */
public class ParticleInstance {
    @NotNull
    Level level;
    @NotNull
    Vec3 worldPos;
    Vec3 prePos;

    @NotNull
    ParticleLifetimeExpression lifetime;
    double maxLifetime;

    @NotNull
    // TODO need impl
    ParticleInitialSpeed speed;
    double speedValue;

    @Nullable
    // TODO need impl
    ParticleInitialSpin spin;
    double spinStart;
    double spinRate;

    @Nullable
    // TODO need impl
    ParticleKillPlane killPlane;

    @NotNull
    // TODO need impl
    ParticleAppearanceBillboard billboard;
    double yaw;
    double oYaw;
    double pitch;
    double oPitch;
    double roll;
    double oRoll;

    @Nullable
    ParticleAppearanceLighting lighting;
    @NotNull
    ParticleAppearanceTinting tinting;

    @Nullable
    ParticleMotionComponent motionComponent;

    @Nullable
    ParticleExpireIfInBlocks inBlocks;
    @Nullable
    ParticleExpireIfNotInBlocks notInBlocks;
    @Nullable
    ParticleMotionCollision collision;

    @Builder
    public ParticleInstance(@NotNull Level level, @NotNull Vec3 worldPos, @NotNull ParticleLifetimeExpression lifetime,
                            @NotNull ParticleInitialSpeed speed, @Nullable ParticleInitialSpin spin,
                            @Nullable ParticleKillPlane killPlane, @NotNull ParticleAppearanceBillboard billboard,
                            @Nullable ParticleAppearanceLighting lighting, @NotNull ParticleAppearanceTinting tinting,
                            @Nullable ParticleExpireIfInBlocks inBlocks, @Nullable ParticleExpireIfNotInBlocks notInBlocks,
                            @Nullable ParticleMotionComponent motionComponent, @Nullable ParticleMotionCollision collision) {
        this.level = level;
        this.worldPos = prePos = worldPos;
        this.lifetime = lifetime;
        this.speed = speed;
        this.spin = spin;
        this.killPlane = killPlane;
        this.billboard = billboard;
        this.lighting = lighting;
        this.tinting = tinting;
        this.inBlocks = inBlocks;
        this.notInBlocks = notInBlocks;
        this.motionComponent = motionComponent;
        this.collision = collision;
    }

    int age;
    double random1;
    double random2;
    double random3;
    double random4;

    public boolean canCreate(MolangVariableScope scope) {
        // TODO 和文档行为不一致，需要核实
        return lifetime.getExpirationExpression() == null || lifetime.getExpirationExpression().evaluate(scope) < 1;
    }

    public void evaluateStart(MolangVariableScope scope) {
        scope.getDataSource().addSource(this);

        maxLifetime = lifetime.getMaxLifetime().evaluate(scope);
        speedValue = speed.getSpeed().evaluate(scope);

        if (spin != null) {
            spinStart = spin.getStart().evaluate(scope);
            spinRate = spin.getRate().evaluate(scope);
        }

        random1 = ParticleEmitter.random.nextDouble();
        random2 = ParticleEmitter.random.nextDouble();
        random3 = ParticleEmitter.random.nextDouble();
        random4 = ParticleEmitter.random.nextDouble();
    }

    public boolean canContinue(MolangVariableScope scope) {
        return (lifetime.getExpirationExpression() == null || lifetime.getExpirationExpression().evaluate(scope) < 1)
                && age / 20D < maxLifetime;
    }

    public void tick(MolangVariableScope scope) {
        age++;

        prePos = worldPos;

        if (motionComponent != null) {
            worldPos = motionComponent.getNewPos(scope, worldPos);
        }
    }

    public void setWorldPos(Vec3 pos) {
        this.prePos = this.worldPos = pos;
    }

    public void render(MolangVariableScope scope, BufferBuilder bufferbuilder, Camera camera, float partialTicks) {
        scope.getDataSource().addSource(this);

        Vec3 vec3 = camera.getPosition();
        float f = (float) (Mth.lerp(partialTicks, prePos.x, worldPos.x) - vec3.x());
        float f1 = (float) (Mth.lerp(partialTicks, prePos.y, worldPos.y) - vec3.y());
        float f2 = (float) (Mth.lerp(partialTicks, prePos.z, worldPos.z) - vec3.z());
        Quaternion quaternion;

        if (this.roll == 0.0F) {
            quaternion = camera.rotation();
        } else {
            quaternion = new Quaternion(camera.rotation());
            float f3 = (float) Mth.lerp(partialTicks, this.oRoll, this.roll);
            quaternion.mul(Vector3f.ZP.rotation(f3));
        }

        Vector3f vector3f1 = new Vector3f(-1.0F, -1.0F, 0.0F);
        vector3f1.transform(quaternion);
        Vector3f[] avector3f = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)};

        var size = billboard.getSize().evaluate(scope);
        float quadSize = (float) size.getX();

        for (int i = 0; i < 4; ++i) {
            Vector3f vector3f = avector3f[i];
            vector3f.transform(quaternion);
            vector3f.mul(quadSize);
            vector3f.add(f, f1, f2);
        }

        Vec4d uv = billboard.getUv().getUV(scope);
        float u0 = (float) uv.getX();
        float u1 = (float) uv.getY();
        float v0 = (float) uv.getZ();
        float v1 = (float) uv.getW();
        int j = this.getLightColor();
        int color = tinting.getColor(scope);
        bufferbuilder.vertex(avector3f[0].x(), avector3f[0].y(), avector3f[0].z()).uv(u1, v1).color(color).uv2(j).endVertex();
        bufferbuilder.vertex(avector3f[1].x(), avector3f[1].y(), avector3f[1].z()).uv(u1, v0).color(color).uv2(j).endVertex();
        bufferbuilder.vertex(avector3f[2].x(), avector3f[2].y(), avector3f[2].z()).uv(u0, v0).color(color).uv2(j).endVertex();
        bufferbuilder.vertex(avector3f[3].x(), avector3f[3].y(), avector3f[3].z()).uv(u0, v1).color(color).uv2(j).endVertex();
    }

    private int getLightColor() {
        if (lighting != null) {
            return LightTexture.FULL_BRIGHT;
        }

        BlockPos blockpos = new BlockPos(worldPos.x, worldPos.y, worldPos.z);
        return this.level.hasChunkAt(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;
    }

    public AABB getBoundingBox(MolangVariableScope scope) {
        var size = billboard.getSize().evaluate(scope);
        return new AABB(size.getX(), size.getY(), size.getX(), -size.getX(), -size.getY(), -size.getX());
    }

    // TODO 粒子碰撞计算
    public void collision() {

    }
}

package io.github.tt432.eyelib.client.particle.bedrock.component.particle.appearance;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleParticle;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue2;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.util.math.EyeMath;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

import java.lang.Math;

/**
 * @author TT432
 */
@RegisterParticleComponent(value = "particle_appearance_billboard", target = ComponentTarget.PARTICLE)
public record ParticleAppearanceBillboard(
        MolangValue2 size,
        FaceCameraMode facingCameraMode,
        Direction direction,
        UV uv
) implements ParticleParticleComponent {
    public static final Codec<ParticleAppearanceBillboard> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue2.CODEC.fieldOf("size").forGetter(o -> o.size),
            FaceCameraMode.CODEC.fieldOf("facing_camera_mode").forGetter(o -> o.facingCameraMode),
            Direction.CODEC.optionalFieldOf("direction", Direction.EMPTY).forGetter(o -> o.direction),
            UV.CODEC.optionalFieldOf("uv", UV.EMPTY).forGetter(o -> o.uv)
    ).apply(ins, ParticleAppearanceBillboard::new));

    public Vector4f getUV(BrParticleParticle particle) {
        return uv.getUV(particle.molangScope, particle.getLifetime(), particle.getAge());
    }

    public Vector2f getSize(BrParticleParticle particle) {
        return size.eval(particle.molangScope);
    }

    public void transform(BrParticleParticle particle, PoseStack poseStack) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        Quaternionf quaternion = new Quaternionf();
        facingCameraMode.setRotation(particle, particle.getEmitter(), quaternion, camera,
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        poseStack.mulPose(quaternion);
    }

    /**
     * 指定粒子的UV坐标
     *
     * @param textureWidth
     * @param textureHeight 如果为 1，使用标准化纹理（float）
     */
    public record UV(
            int textureWidth,
            int textureHeight,
            MolangValue2 uv,
            MolangValue2 uvSize,
            Flipbook flipbook
    ) {
        public static final UV EMPTY = new UV(1, 1,
                MolangValue2.ZERO, MolangValue2.ONE, Flipbook.EMPTY);

        public static final Codec<UV> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.INT.optionalFieldOf("texture_width", 1).forGetter(o -> o.textureWidth),
                Codec.INT.optionalFieldOf("texture_height", 1).forGetter(o -> o.textureHeight),
                MolangValue2.CODEC.optionalFieldOf("uv", MolangValue2.ZERO).forGetter(o -> o.uv),
                MolangValue2.CODEC.optionalFieldOf("uv_size", MolangValue2.ONE).forGetter(o -> o.uvSize),
                Flipbook.CODEC.optionalFieldOf("flipbook", Flipbook.EMPTY).forGetter(o -> o.flipbook)
        ).apply(ins, UV::new));

        public Vector4f getUV(MolangScope scope, float lifetime, float time) {
            if (flipbook.isEmpty()) {
                Vector2f uv = this.uv.eval(scope);
                Vector2f size = uvSize.eval(scope);
                return new Vector4f(uv.x, uv.y, size.x, size.y).div(textureWidth, textureHeight, textureWidth, textureHeight);
            } else {
                return flipbook.get(scope, lifetime, time).div(textureWidth, textureHeight, textureWidth, textureHeight);
            }
        }

        public record Flipbook(
                MolangValue2 baseUV,
                MolangValue2 sizeUV,
                MolangValue2 stepUV,
                MolangValue framesPerSecond,
                MolangValue maxFrame,
                // todo
                boolean stretchToLifetime,
                boolean loop
        ) {
            public static final Flipbook EMPTY = new Flipbook(
                    null, null, null, null, null,
                    false, false
            );

            public static final Codec<Flipbook> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    MolangValue2.CODEC.fieldOf("base_UV").forGetter(o -> o.baseUV),
                    MolangValue2.CODEC.fieldOf("size_UV").forGetter(o -> o.sizeUV),
                    MolangValue2.CODEC.fieldOf("step_UV").forGetter(o -> o.stepUV),
                    MolangValue.CODEC.optionalFieldOf("frames_per_second", MolangValue.ZERO)
                            .forGetter(o -> o.framesPerSecond),
                    MolangValue.CODEC.fieldOf("max_frame").forGetter(o -> o.maxFrame),
                    Codec.BOOL.optionalFieldOf("stretch_to_lifetime", false).forGetter(o -> o.stretchToLifetime),
                    Codec.BOOL.optionalFieldOf("loop", false).forGetter(o -> o.loop)
            ).apply(ins, Flipbook::new));

            public Vector4f get(MolangScope scope, float lifetime, float time) {
                int max = Mth.floor(maxFrame.eval(scope)) - 1;
                int frame;
                if (stretchToLifetime) frame = Mth.floor((time / lifetime) * max);
                else frame = Mth.floor(framesPerSecond.eval(scope) * time);

                if (frame > max) {
                    if (loop) {
                        frame %= (max + 1);
                    } else {
                        frame = max;
                    }
                }

                Vector2f base = baseUV.eval(scope).add(stepUV.eval(scope).mul(frame));
                return new Vector4f(
                        base.x,
                        base.y,
                        sizeUV.getX(scope),
                        sizeUV.getY(scope)
                );
            }

            public boolean isEmpty() {
                return this == EMPTY;
            }
        }
    }

    @FunctionalInterface
    private interface FacingCameraMode {
        void setRotation(BrParticleParticle particle, BrParticleEmitter emitter, Quaternionf quaternion, Camera camera, float partialTick);
    }

    private static final Vector3f UP = new Vector3f(0, 1, 0);

    /**
     * 表示面向相机的模式
     */
    public enum FaceCameraMode implements StringRepresentable, FacingCameraMode {
        /**
         * 与相机对齐，垂直于视轴旋转
         */
        ROTATE_XYZ {
            @Override
            public void setRotation(BrParticleParticle particle, BrParticleEmitter emitter, Quaternionf quaternion, Camera camera, float partialTick) {
                quaternion.set(camera.rotation());
            }
        },

        /**
         * 与相机对齐，但围绕世界 y 轴旋转
         */
        ROTATE_Y {
            @Override
            public void setRotation(BrParticleParticle particle, BrParticleEmitter emitter, Quaternionf quaternion, Camera camera, float partialTick) {
                quaternion.set(0, camera.rotation().y, 0, camera.rotation().w);
            }
        },

        /**
         * 瞄准相机，偏向世界 y 轴向上
         */
        LOOKAT_XYZ {
            @Override
            public void setRotation(BrParticleParticle particle, BrParticleEmitter emitter, Quaternionf quaternion, Camera camera, float partialTick) {
                Vector3f pp = particle.getPosition();
                Vector3f ep = particle.getEmitter().getPosition();
                var cp = camera.getPosition();

                new Matrix4f().lookAt(
                        pp.x + ep.x,
                        pp.y + ep.y,
                        pp.z + ep.z,
                        (float) cp.x,
                        (float) cp.y,
                        (float) cp.z,
                        0,
                        1,
                        0
                ).invert().rotateY(Mth.PI).getNormalizedRotation(quaternion);
            }
        },

        /**
         * 瞄准相机，但围绕世界 y 轴旋转
         */
        LOOKAT_Y {
            @Override
            public void setRotation(BrParticleParticle particle, BrParticleEmitter emitter, Quaternionf quaternion, Camera camera, float partialTick) {
                Vector3f pp = particle.getPosition();
                Vector3f ep = particle.getEmitter().getPosition();
                var cp = camera.getPosition();

                new Matrix4f().lookAt(
                        pp.x + ep.x,
                        pp.y + ep.y,
                        pp.z + ep.z,
                        (float) cp.x,
                        pp.y + ep.y,
                        (float) cp.z,
                        0,
                        1,
                        0
                ).invert().rotateY(Mth.PI).getNormalizedRotation(quaternion);
            }
        },

        /**
         * 未旋转的粒子 x 轴沿着方向向量，未旋转的 y 轴试图向上对齐
         */
        DIRECTION_X {
            @Override
            public void setRotation(BrParticleParticle particle, BrParticleEmitter emitter, Quaternionf quaternion, Camera camera, float partialTick) {
                Vector3f vec = new Vector3f(particle.getVelocity());
                double y = Math.atan2(vec.x, vec.z);
                double z = Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)));
                quaternion
                        .rotateY((float) (y - Math.PI / 2))
                        .rotateZ((float) z);
            }
        },

        /**
         * 未旋转的粒子 y 轴沿着方向向量，未旋转的 x 轴试图向上对齐
         */
        DIRECTION_Y {
            @Override
            public void setRotation(BrParticleParticle particle, BrParticleEmitter emitter, Quaternionf quaternion, Camera camera, float partialTick) {
                Vector3f vec = new Vector3f(particle.getVelocity());
                double y = Math.atan2(vec.x, vec.z);
                double x = Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)));
                quaternion.identity()
                        .rotateY((float) (y - Math.PI))
                        .rotateX((float) (x - Math.PI / 2));
            }
        },

        /**
         * 广告牌面朝向方向向量，未旋转的 y 轴试图向上对齐
         */
        DIRECTION_Z {
            @Override
            public void setRotation(BrParticleParticle particle, BrParticleEmitter emitter, Quaternionf quaternion, Camera camera, float partialTick) {
                Vector3f vec = new Vector3f(particle.getVelocity());
                double y = Math.atan2(vec.x, vec.z);
                double x = Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)));
                quaternion.identity()
                        .rotateY((float) y)
                        .rotateX((float) -x);
            }
        },

        /**
         * 使粒子匹配发射器的变换（广告牌平面将匹配变换的 xy 平面）
         */
        EMITTER_TRANSFORM_XY {
            @Override
            public void setRotation(BrParticleParticle particle, BrParticleEmitter emitter, Quaternionf quaternion, Camera camera, float partialTick) {
                Quaternionf base = new Quaternionf();
                emitter.getBaseRotation().getNormalizedRotation(base);
                quaternion.set(base);
            }
        },

        /**
         * 使粒子匹配发射器的变换（广告牌平面将匹配变换的 xz 平面）
         */
        EMITTER_TRANSFORM_XZ {
            @Override
            public void setRotation(BrParticleParticle particle, BrParticleEmitter emitter, Quaternionf quaternion, Camera camera, float partialTick) {
                Quaternionf base = new Quaternionf();
                emitter.getBaseRotation().getNormalizedRotation(base);
                quaternion.set(base).rotateX(90 * EyeMath.DEGREES_TO_RADIANS);
            }
        },

        /**
         * 使粒子匹配发射器的变换（广告牌平面将匹配变换的 yz 平面）
         */
        EMITTER_TRANSFORM_YZ {
            @Override
            public void setRotation(BrParticleParticle particle, BrParticleEmitter emitter, Quaternionf quaternion, Camera camera, float partialTick) {
                Quaternionf base = new Quaternionf();
                emitter.getBaseRotation().getNormalizedRotation(base);
                quaternion.set(base).rotateY(90 * EyeMath.DEGREES_TO_RADIANS);
            }
        };
        public static final Codec<FaceCameraMode> CODEC = StringRepresentable.fromEnum(FaceCameraMode::values);

        @Override
        @NotNull
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    /**
     * 指定如何计算粒子的方向，这将用于需要方向作为输入的面向模式（例如：lookat_direction 和 direction）<br/>
     * 如果未定义方向子部分，默认将是 "derive_from_velocity" 模式，"min_speed_threshold" 为 0.01
     *
     * @param minSpeedThreshold 仅用于 "derive_from_velocity" 模式。当粒子的速度高于此阈值时设置方向。默认值为 0.01
     * @param customDirection   仅用于 "custom_direction" 模式。指定方向向量
     */
    public record Direction(
            Mode mode,
            float minSpeedThreshold,
            MolangValue3 customDirection
    ) {
        public static final Direction EMPTY = new Direction(Mode.DERIVE_FROM_VELOCITY,
                0.01F, MolangValue3.ZERO);

        public static final Codec<Direction> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Mode.CODEC.optionalFieldOf("mode", Mode.DERIVE_FROM_VELOCITY).forGetter(o -> o.mode),
                Codec.FLOAT.optionalFieldOf("min_speed_threshold", 0.01F)
                        .forGetter(o -> o.minSpeedThreshold),
                MolangValue3.CODEC.optionalFieldOf("custom_direction", MolangValue3.ZERO)
                        .forGetter(o -> o.customDirection)
        ).apply(ins, Direction::new));

        public enum Mode implements StringRepresentable {
            /**
             * 方向与速度方向一致
             */
            DERIVE_FROM_VELOCITY,
            /**
             * 方向在 json 定义中 molang 表达式的向量指定
             */
            CUSTOM_DIRECTION;

            public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

            @Override
            @NotNull
            public String getSerializedName() {
                return name().toLowerCase();
            }
        }
    }
}

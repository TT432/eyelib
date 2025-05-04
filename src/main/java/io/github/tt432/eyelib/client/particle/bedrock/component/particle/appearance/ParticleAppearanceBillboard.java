package io.github.tt432.eyelib.client.particle.bedrock.component.particle.appearance;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

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
        PoseStack.Pose last = poseStack.last();
        var m4 = last.pose();

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var emitterRotation = particle.getEmitter().getRotation();

        switch (facingCameraMode) {
            case ROTATE_XYZ -> m4.rotate(camera.rotation());
            case ROTATE_Y -> m4.rotationY(180 * EyeMath.DEGREES_TO_RADIANS
                    - camera.rotation().getEulerAnglesZYX(new Vector3f()).y);

            case LOOKAT_XYZ -> m4.rotate(camera.rotation());
            case LOOKAT_Y -> m4.rotationY(180 * EyeMath.DEGREES_TO_RADIANS
                    - camera.rotation().getEulerAnglesZYX(new Vector3f()).y);

            case DIRECTION_X -> applyDirectionX(m4, direction(particle, m4));
            case DIRECTION_Y -> applyDirectionY(m4, direction(particle, m4));
            case DIRECTION_Z -> applyDirectionZ(m4, direction(particle, m4));

            case EMITTER_TRANSFORM_XY -> {
                // 粒子匹配发射器的 xy 平面变换
                m4.rotateZYX(emitterRotation);
            }
            case EMITTER_TRANSFORM_XZ -> {
                // 粒子匹配发射器的 xz 平面变换
                m4.rotateZYX(emitterRotation).rotateX(90 * EyeMath.DEGREES_TO_RADIANS);
            }
            case EMITTER_TRANSFORM_YZ -> {
                // 粒子匹配发射器的 yz 平面变换
                m4.rotateZYX(emitterRotation).rotateY(90 * EyeMath.DEGREES_TO_RADIANS);
            }
        }
    }

    Vector3f direction(BrParticleParticle particle, Matrix4f m4) {
        MolangScope scope = particle.molangScope;
        float velocity = particle.getVelocity().length();
        Vector3f direction = velocity > direction().minSpeedThreshold ? switch (direction().mode) {
            case DERIVE_FROM_VELOCITY ->
                    particle.getVelocity().equals(new Vector3f()) ? new Vector3f() : particle.getVelocity().normalize(new Vector3f());
            case CUSTOM_DIRECTION -> direction().customDirection.eval(scope);
        } : new Vector3f();

        if (!direction.equals(new Vector3f())) {
            m4.lookAlong(direction, new Vector3f(0, 1, 0));
        }

        return direction;
    }

    static final Vector3f up = new Vector3f(0, 1, 0);

    public static void applyDirectionX(Matrix4f m4, Vector3f direction) {
        // x 轴沿方向向量
        Vector3f xAxis = new Vector3f(direction.x == 0 && direction.y == 0 && direction.z == 0 ? new Vector3f(1,0,0) : direction);
        // 计算 y 轴
        Vector3f yAxis = new Vector3f();
        up.cross(xAxis, yAxis).normalize();
        // 计算 z 轴
        Vector3f zAxis = new Vector3f();
        xAxis.cross(yAxis, zAxis).normalize();

        // 设置矩阵的坐标轴
        m4.setColumn(0, new Vector4f(xAxis.x, xAxis.y, xAxis.z, 0));
        m4.setColumn(1, new Vector4f(yAxis.x, yAxis.y, yAxis.z, 0));
        m4.setColumn(2, new Vector4f(zAxis.x, zAxis.y, zAxis.z, 0));
    }

    public static void applyDirectionY(Matrix4f m4, Vector3f direction) {
        // y 轴沿方向向量
        Vector3f yAxis = new Vector3f(direction.x == 0 && direction.y == 0 && direction.z == 0 ? new Vector3f(0,1,0) : direction);
        // 计算 x 轴
        Vector3f xAxis = new Vector3f();
        up.cross(yAxis, xAxis).normalize();
        // 计算 z 轴
        Vector3f zAxis = new Vector3f();
        yAxis.cross(xAxis, zAxis).normalize();

        // 设置矩阵的坐标轴
        m4.setColumn(0, new Vector4f(xAxis.x, xAxis.y, xAxis.z, 0));
        m4.setColumn(1, new Vector4f(yAxis.x, yAxis.y, yAxis.z, 0));
        m4.setColumn(2, new Vector4f(zAxis.x, zAxis.y, zAxis.z, 0));
    }

    public static void applyDirectionZ(Matrix4f m4, Vector3f direction) {
        // z 轴沿方向向量
        Vector3f zAxis = new Vector3f(direction.x == 0 && direction.y == 0 && direction.z == 0 ? new Vector3f(0,0,1) : direction);
        // 计算 y 轴
        Vector3f yAxis = new Vector3f();
        up.cross(zAxis, yAxis).normalize();
        // 计算 x 轴
        Vector3f xAxis = new Vector3f();
        yAxis.cross(zAxis, xAxis).normalize();

        // 设置矩阵的坐标轴
        m4.setColumn(0, new Vector4f(xAxis.x, xAxis.y, xAxis.z, 0));
        m4.setColumn(1, new Vector4f(yAxis.x, yAxis.y, yAxis.z, 0));
        m4.setColumn(2, new Vector4f(zAxis.x, zAxis.y, zAxis.z, 0));
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

    /**
     * 表示面向相机的模式
     */
    public enum FaceCameraMode implements StringRepresentable {
        /**
         * 与相机对齐，垂直于视轴旋转
         */
        ROTATE_XYZ,

        /**
         * 与相机对齐，但围绕世界 y 轴旋转
         */
        ROTATE_Y,

        /**
         * 瞄准相机，偏向世界 y 轴向上
         */
        LOOKAT_XYZ,

        /**
         * 瞄准相机，但围绕世界 y 轴旋转
         */
        LOOKAT_Y,

        /**
         * 未旋转的粒子 x 轴沿着方向向量，未旋转的 y 轴试图向上对齐
         */
        DIRECTION_X,

        /**
         * 未旋转的粒子 y 轴沿着方向向量，未旋转的 x 轴试图向上对齐
         */
        DIRECTION_Y,

        /**
         * 广告牌面朝向方向向量，未旋转的 y 轴试图向上对齐
         */
        DIRECTION_Z,

        /**
         * 使粒子匹配发射器的变换（广告牌平面将匹配变换的 xy 平面）
         */
        EMITTER_TRANSFORM_XY,

        /**
         * 使粒子匹配发射器的变换（广告牌平面将匹配变换的 xz 平面）
         */
        EMITTER_TRANSFORM_XZ,

        /**
         * 使粒子匹配发射器的变换（广告牌平面将匹配变换的 yz 平面）
         */
        EMITTER_TRANSFORM_YZ;
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

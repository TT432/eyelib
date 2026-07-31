package io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.appearance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue2;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelib.particle.runtime.support.ParticleMath;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** @author TT432 */
public record ParticleAppearanceBillboard(
        MolangValue2 size,
        FaceCameraMode facingCameraMode,
        Direction direction,
        UV uv
) implements ParticleParticleComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticleAppearanceBillboard.class);

    public static final Codec<ParticleAppearanceBillboard> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue2.CODEC.fieldOf("size").forGetter(ParticleAppearanceBillboard::size),
            FaceCameraMode.CODEC.optionalFieldOf("facing_camera_mode", FaceCameraMode.ROTATE_XYZ).forGetter(ParticleAppearanceBillboard::facingCameraMode),
            Direction.CODEC.optionalFieldOf("direction", Direction.EMPTY).forGetter(ParticleAppearanceBillboard::direction),
            UV.CODEC.optionalFieldOf("uv", UV.EMPTY).forGetter(ParticleAppearanceBillboard::uv)
    ).apply(ins, ParticleAppearanceBillboard::new));

    public Vector4f getUV(ParticleAccess particle) {
        return uv.getUV(particle.molangScope(), particle.lifetime(), particle.age());
    }

    public Vector2f getSize(ParticleAccess particle) {
        return size.eval(particle.molangScope());
    }

    public Quaternionf getRotation(ParticleAccess particle, CameraAccess camera, float partialTick) {
        Quaternionf quaternion = new Quaternionf();
        facingCameraMode.setRotation(particle, quaternion, camera, partialTick);
        return quaternion;
    }

    public record CameraAccess(Quaternionf rotation, Vector3f position) {
        public CameraAccess {
            rotation = new Quaternionf(rotation);
            position = new Vector3f(position);
        }
    }

    public record UV(
            int textureWidth,
            int textureHeight,
            MolangValue2 uv,
            MolangValue2 uvSize,
            Flipbook flipbook
    ) {
        public static final UV EMPTY = new UV(1, 1, MolangValue2.ZERO, MolangValue2.ONE, Flipbook.EMPTY);

        public static final Codec<UV> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.INT.optionalFieldOf("texture_width", 1).forGetter(UV::textureWidth),
                Codec.INT.optionalFieldOf("texture_height", 1).forGetter(UV::textureHeight),
                MolangValue2.CODEC.optionalFieldOf("uv", MolangValue2.ZERO).forGetter(UV::uv),
                MolangValue2.CODEC.optionalFieldOf("uv_size", MolangValue2.ONE).forGetter(UV::uvSize),
                Flipbook.CODEC.optionalFieldOf("flipbook", Flipbook.EMPTY).forGetter(UV::flipbook)
        ).apply(ins, UV::new));

        public Vector4f getUV(MolangScope scope, float lifetime, float time) {
            if (flipbook.isEmpty()) {
                Vector2f base = uv.eval(scope);
                Vector2f size = uvSize.eval(scope);
                return new Vector4f(base.x, base.y, size.x, size.y)
                        .div(textureWidth, textureHeight, textureWidth, textureHeight);
            }
            return flipbook.get(scope, lifetime, time, textureWidth, textureHeight)
                    .div(textureWidth, textureHeight, textureWidth, textureHeight);
        }

        public record Flipbook(
                @Nullable MolangValue2 baseUV,
                @Nullable MolangValue2 sizeUV,
                @Nullable MolangValue2 stepUV,
                @Nullable MolangValue framesPerSecond,
                @Nullable MolangValue maxFrame,
                boolean stretchToLifetime,
                boolean loop
        ) {
            public static final Flipbook EMPTY = new Flipbook(null, null, null, null, null, false, false);

            public static final Codec<Flipbook> CODEC = RecordCodecBuilder.create(ins -> {
                var baseUv = MolangValue2.CODEC.fieldOf("base_UV").forGetter(Flipbook::baseUV);
                var sizeUv = MolangValue2.CODEC.fieldOf("size_UV").forGetter(Flipbook::sizeUV);
                var stepUv = MolangValue2.CODEC.fieldOf("step_UV").forGetter(Flipbook::stepUV);
                var framesPerSecond = MolangValue.CODEC.optionalFieldOf("frames_per_second", MolangValue.ZERO).forGetter(Flipbook::framesPerSecond);
                // max_frame 官方为可选字段（缺省时按纹理网格推导帧数），见
                // particle_appearance_billboard_flipbook_data：Default Value 为 not set
                var maxFrame = MolangValue.CODEC.optionalFieldOf("max_frame").forGetter((Flipbook f) -> Optional.ofNullable(f.maxFrame));
                var stretchToLifetime = Codec.BOOL.optionalFieldOf("stretch_to_lifetime", false).forGetter(Flipbook::stretchToLifetime);
                var loop = Codec.BOOL.optionalFieldOf("loop", false).forGetter(Flipbook::loop);
                return ins.group(baseUv, sizeUv, stepUv, framesPerSecond, maxFrame, stretchToLifetime, loop)
                        .apply(ins, (b, s, st, fps, mf, stl, lp) -> new Flipbook(b, s, st, fps, mf.orElse(null), stl, lp));
            });

            public Vector4f get(MolangScope scope, float lifetime, float time, float textureWidth, float textureHeight) {
                // 调用方契约：仅在非 EMPTY 实例上调用（getUV 已用 isEmpty 守卫）
                MolangValue2 buv = Objects.requireNonNull(baseUV);
                MolangValue2 suv = Objects.requireNonNull(stepUV);
                MolangValue2 szuv = Objects.requireNonNull(sizeUV);
                float sizeX = szuv.getX(scope);
                float sizeY = szuv.getY(scope);
                int max;
                if (maxFrame != null) {
                    max = (int) Math.floor(maxFrame.eval(scope)) - 1;
                } else {
                    // max_frame 缺省：按纹理网格列数×行数推导最后一帧索引
                    int columns = sizeX > 0 ? (int) Math.floor(textureWidth / sizeX) : 1;
                    int rows = sizeY > 0 ? (int) Math.floor(textureHeight / sizeY) : 1;
                    max = Math.max(columns, 1) * Math.max(rows, 1) - 1;
                }
                int frame = stretchToLifetime
                        ? (int) Math.floor((time / lifetime) * max)
                        : (int) Math.floor(Objects.requireNonNull(framesPerSecond).eval(scope) * time);
                if (frame > max) {
                    frame = loop ? frame % (max + 1) : max;
                }
                Vector2f base = buv.eval(scope).add(suv.eval(scope).mul(frame));
                return new Vector4f(base.x, base.y, sizeX, sizeY);
            }

            public boolean isEmpty() {
                return this == EMPTY;
            }
        }
    }

    @FunctionalInterface
    private interface FacingCameraMode {
        void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick);
    }

    public enum FaceCameraMode implements FacingCameraMode {
        ROTATE_XYZ {
            @Override
            public void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick) {
                quaternion.set(camera.rotation());
            }
        },
        ROTATE_Y {
            @Override
            public void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick) {
                quaternion.set(0, camera.rotation().y, 0, camera.rotation().w);
            }
        },
        LOOKAT_XYZ {
            @Override
            public void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick) {
                Vector3f pp = particle.position();
                Vector3f ep = particle.emitterPosition();
                Vector3f cp = camera.position();
                new Matrix4f().lookAt(pp.x + ep.x, pp.y + ep.y, pp.z + ep.z,
                        cp.x, cp.y, cp.z, 0, 1, 0).invert().rotateY(ParticleMath.PI).getNormalizedRotation(quaternion);
            }
        },
        LOOKAT_Y {
            @Override
            public void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick) {
                Vector3f pp = particle.position();
                Vector3f ep = particle.emitterPosition();
                Vector3f cp = camera.position();
                new Matrix4f().lookAt(pp.x + ep.x, pp.y + ep.y, pp.z + ep.z,
                        cp.x, pp.y + ep.y, cp.z, 0, 1, 0).invert().rotateY(ParticleMath.PI).getNormalizedRotation(quaternion);
            }
        },
        DIRECTION_X {
            @Override
            public void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick) {
                Vector3f vec = particle.velocity();
                double y = Math.atan2(vec.x, vec.z);
                double z = Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)));
                quaternion.rotateY((float) (y - Math.PI / 2)).rotateZ((float) z);
            }
        },
        DIRECTION_Y {
            @Override
            public void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick) {
                Vector3f vec = particle.velocity();
                double y = Math.atan2(vec.x, vec.z);
                double x = Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)));
                quaternion.identity().rotateY((float) (y - Math.PI)).rotateX((float) (x - Math.PI / 2));
            }
        },
        DIRECTION_Z {
            @Override
            public void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick) {
                Vector3f vec = particle.velocity();
                double y = Math.atan2(vec.x, vec.z);
                double x = Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)));
                quaternion.identity().rotateY((float) y).rotateX((float) -x);
            }
        },
        EMITTER_TRANSFORM_XY {
            @Override
            public void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick) {
            }
        },
        EMITTER_TRANSFORM_XZ {
            @Override
            public void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick) {
                quaternion.rotateX(90 * ParticleMath.DEGREES_TO_RADIANS);
            }
        },
        EMITTER_TRANSFORM_YZ {
            @Override
            public void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick) {
                quaternion.rotateY(90 * ParticleMath.DEGREES_TO_RADIANS);
            }
        },
        LOOKAT_DIRECTION {
            @Override
            public void setRotation(ParticleAccess particle, Quaternionf quaternion, CameraAccess camera, float partialTick) {
                Vector3f vel = particle.velocity();
                if (vel.x == 0 && vel.y == 0 && vel.z == 0) {
                    quaternion.identity();
                } else {
                    Vector3f pp = particle.position();
                    Vector3f forward = new Vector3f(vel).normalize();
                    Vector3f up = new Vector3f(0, 1, 0);
                    if (Math.abs(forward.dot(up)) > 0.999) up = new Vector3f(0, 0, 1);
                    new Matrix4f().lookAt(pp.x, pp.y, pp.z, pp.x + forward.x, pp.y + forward.y, pp.z + forward.z,
                            0, 1, 0).invert().getNormalizedRotation(quaternion);
                }
            }
        };

        public static final Codec<FaceCameraMode> CODEC = Codec.STRING.xmap(
                FaceCameraMode::byName,
                mode -> mode.name().toLowerCase()
        );

        private static FaceCameraMode byName(String name) {
            try {
                return FaceCameraMode.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Unknown particle facing_camera_mode '{}', falling back to rotate_xyz", name);
                return ROTATE_XYZ;
            }
        }
    }

    public record Direction(
            Mode mode,
            float minSpeedThreshold,
            MolangValue3 customDirection
    ) {
        public static final Direction EMPTY = new Direction(Mode.DERIVE_FROM_VELOCITY, 0.01F, MolangValue3.ZERO);
        public static final Codec<Direction> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Mode.CODEC.optionalFieldOf("mode", Mode.DERIVE_FROM_VELOCITY).forGetter(Direction::mode),
                Codec.FLOAT.optionalFieldOf("min_speed_threshold", 0.01F).forGetter(Direction::minSpeedThreshold),
                MolangValue3.CODEC.optionalFieldOf("custom_direction", MolangValue3.ZERO).forGetter(Direction::customDirection)
        ).apply(ins, Direction::new));

        public enum Mode {
            DERIVE_FROM_VELOCITY,
            CUSTOM_DIRECTION;

            public static final Codec<Mode> CODEC = Codec.STRING.xmap(
                    Mode::byName,
                    mode -> mode.name().toLowerCase()
            );

            private static Mode byName(String name) {
                return switch (name.toLowerCase(Locale.ROOT)) {
                    // Bedrock 官方值为 custom_direction；部分资源包写作 custom，按同一语义兼容
                    case "custom_direction", "custom" -> CUSTOM_DIRECTION;
                    case "derive_from_velocity" -> DERIVE_FROM_VELOCITY;
                    default -> {
                        LOGGER.warn("Unknown particle direction mode '{}', falling back to derive_from_velocity", name);
                        yield DERIVE_FROM_VELOCITY;
                    }
                };
            }
        }
    }
}
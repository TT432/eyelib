package io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.shape;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue3;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Objects;

/** @author TT432 */
public record Direction(Type type, @Nullable MolangValue3 custom) {
    public static final Direction EMPTY = new Direction(Type.OUTWARDS, null);

    public static final Codec<Direction> CODEC = Codec.either(
            Codec.STRING.xmap(value -> switch (value) {
                        case "inwards" -> new Direction(Type.INWARDS, MolangValue3.ZERO);
                        default -> new Direction(Type.OUTWARDS, MolangValue3.ZERO);
                    },
                    direction -> direction.type.name().toLowerCase()),
            MolangValue3.CODEC.xmap(value -> new Direction(Type.CUSTOM, value), Direction::custom)
    ).xmap(either -> either.map(left -> left, right -> right), Either::left);

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public Vector3f getVec(MolangScope scope, Vector3f center, Vector3f other) {
        return switch (type) {
            case INWARDS -> center.sub(other, new Vector3f()).normalize().mul(16);
            case OUTWARDS -> other.sub(center, new Vector3f()).normalize().mul(16);
            // 与 INWARDS/OUTWARDS 一致：方向向量归一化并乘 16（运动积分按 1/16 单位空间，
            // 见 ParticleMotionDynamic）；此前 CUSTOM 原样返回导致初速度只有预期的 1/16。
            // 零向量（如 [0,0,0]）normalize 产生 NaN，保持原样返回由速度积分自然得零位移。
            case CUSTOM -> {
                Vector3f v = Objects.requireNonNull(custom, "custom direction").eval(scope);
                yield v.lengthSquared() > 1.0E-6f ? v.normalize().mul(16) : v;
            }
        };
    }

    public enum Type {
        INWARDS,
        OUTWARDS,
        CUSTOM
    }
}
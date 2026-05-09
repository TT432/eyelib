package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue3;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Objects;

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
            case CUSTOM -> Objects.requireNonNull(custom, "custom direction").eval(scope);
        };
    }

    public enum Type {
        INWARDS,
        OUTWARDS,
        CUSTOM
    }
}

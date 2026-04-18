package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue3;
import io.github.tt432.eyelib.util.codec.CodecHelper;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * @author TT432
 */
public record Direction(
        Type type,
        @Nullable
        MolangValue3 custom
) {
    public static final Direction EMPTY = new Direction(Type.OUTWARDS, null);

    public static final Codec<Direction> CODEC = CodecHelper.withAlternative(
            Codec.STRING.xmap(s -> switch (s) {
                        case "inwards" -> new Direction(Type.INWARDS, MolangValue3.ZERO);
                        default -> new Direction(Type.OUTWARDS, MolangValue3.ZERO);
                    },
                    d -> d.type.name().toLowerCase()),
            MolangValue3.CODEC.xmap(mv3 -> new Direction(Type.CUSTOM, mv3), d -> d.custom)
    );

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

    public enum Type implements StringRepresentable {
        INWARDS,
        OUTWARDS,
        CUSTOM;
        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }
}


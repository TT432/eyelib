package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.shape;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue3;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public record Direction(
        Type type,
        MolangValue3 custom
) {
    public static final Direction EMPTY = new Direction(Type.OUTWARDS, null);

    public static final Codec<Direction> CODEC = Codec.withAlternative(
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
            case INWARDS -> center.sub(other, new Vector3f()).normalize();
            case OUTWARDS -> other.sub(center, new Vector3f()).normalize();
            case CUSTOM -> custom.eval(scope);
        };
    }

    public enum Type implements StringRepresentable {
        INWARDS,
        OUTWARDS,
        CUSTOM;
        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        @Override
        @NotNull
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }
}

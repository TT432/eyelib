package io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.shape;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.MolangValue3;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import org.joml.Vector3f;

public record EmitterDisc(
        MolangValue3 planeNormal,
        MolangValue3 offset,
        MolangValue radius,
        boolean surfaceOnly,
        Direction direction
) implements EmitterParticleComponent {
    public static final Codec<EmitterDisc> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.either(Codec.STRING.xmap(value -> switch (value) {
                                case "x" -> MolangValue3.AXIS_X;
                                case "z" -> MolangValue3.AXIS_Z;
                                default -> MolangValue3.AXIS_Y;
                            }, value -> {
                                if (value.equals(MolangValue3.AXIS_X)) return "x";
                                if (value.equals(MolangValue3.AXIS_Z)) return "z";
                                return "y";
                            }),
                            MolangValue3.CODEC)
                    .xmap(either -> either.map(left -> left, right -> right), Either::right)
                    .optionalFieldOf("plane_normal", MolangValue3.AXIS_Y).forGetter(EmitterDisc::planeNormal),
            MolangValue3.CODEC.optionalFieldOf("offset", MolangValue3.ZERO).forGetter(EmitterDisc::offset),
            MolangValue.CODEC.optionalFieldOf("radius", MolangValue.TRUE_VALUE).forGetter(EmitterDisc::radius),
            Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(EmitterDisc::surfaceOnly),
            Direction.CODEC.optionalFieldOf("direction", Direction.EMPTY).forGetter(EmitterDisc::direction)
    ).apply(ins, EmitterDisc::new));

    @Override
    public EvalVector3f getEmitPosition(EmitterAccess emitter) {
        return scope -> {
            Vector3f center = offset.eval(scope);
            Vector3f normal = planeNormal.eval(scope).normalize();
            float r = surfaceOnly ? radius.eval(scope) : radius.eval(scope) * (float) Math.sqrt(emitter.random().nextFloat());
            float angle = emitter.random().nextFloat() * (float) Math.PI * 2;
            float x = r * (float) Math.cos(angle);
            float y = r * (float) Math.sin(angle);
            Vector3f u = new Vector3f().orthogonalize(normal);
            Vector3f v = normal.cross(u, new Vector3f());
            return new Vector3f(center).add(u.mul(x, new Vector3f())).add(v.mul(y, new Vector3f()));
        };
    }
}

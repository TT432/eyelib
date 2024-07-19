package io.github.tt432.eyelib.client.particle.bedrock.component.particle.appearance;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleParticle;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.RegisterParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.particle.ParticleParticleComponent;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue4;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import org.joml.Vector4f;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * @author TT432
 */
@RegisterParticleComponent(value = "particle_appearance_tinting", target = ComponentTarget.PARTICLE)
public record ParticleAppearanceTinting(
        boolean isGradient,
        MolangValue4 staticColor,
        Color gradientColor
) implements ParticleParticleComponent {
    public static final Codec<ParticleAppearanceTinting> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.either(
                    Color.CODEC,
                    MolangValue4.CODEC
            ).fieldOf("color").forGetter(t -> t.isGradient
                    ? Either.left(t.gradientColor)
                    : Either.right(t.staticColor))
    ).apply(ins, e -> e.map(
            color -> new ParticleAppearanceTinting(true, null, color),
            mv4 -> new ParticleAppearanceTinting(false, mv4, null)
    )));

    public int getColor(BrParticleParticle particle) {
        if (!isGradient) {
            Vector4f mul = staticColor.eval(particle.molangScope).mul(255);
            return 0xFF_00_00_00 | ((int) mul.x) << 16 | ((int) mul.y) << 8 | ((int) mul.z);
        } else {
            return gradientColor.getColor(particle.molangScope);
        }
    }

    public record Color(
            TreeMap<Float, Integer> gradient,
            MolangValue interpolant
    ) {
        public static final Codec<Color> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                EyelibCodec.treeMap(
                        EyelibCodec.STR_FLOAT_CODEC,
                        Codec.STRING.xmap(s -> Integer.parseUnsignedInt(s.substring(1), 16), Float::toString),
                        Comparator.comparingDouble(k -> k)
                ).fieldOf("gradient").forGetter(o -> o.gradient),
                MolangValue.CODEC.fieldOf("interpolant").forGetter(o -> o.interpolant)
        ).apply(ins, Color::new));

        public int getColor(MolangScope scope) {
            float v = interpolant.eval(scope);

            var before = gradient.floorEntry(v);
            var after = gradient.higherEntry(v);

            if (before == null) return after.getValue();
            else if (after == null) return before.getValue();
            else return interpolateColor(before.getValue(), after.getValue(),
                        (v - before.getKey()) / (after.getKey() - before.getKey()));
        }

        public static int interpolateColor(int c1, int c2, float t) {
            int c1r = (c1 >> 16) & 255;
            int c1g = (c1 >> 8) & 255;
            int c1b = c1 & 255;

            int c2r = (c2 >> 16) & 255;
            int c2g = (c2 >> 8) & 255;
            int c2b = c2 & 255;

            int r = (int) (c1r + (c2r - c1r) * t);
            int g = (int) (c1g + (c2g - c1g) * t);
            int b = (int) (c1b + (c2b - c1b) * t);

            return (r << 16) | (g << 8) | b;
        }
    }
}

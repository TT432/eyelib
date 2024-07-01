package io.github.tt432.eyelib.client.particle.bedrock.component.particle.appearance;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue4;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * @author TT432
 */
@ParticleComponent(value = "particle_appearance_tinting", target = ComponentTarget.PARTICLE)
public record ParticleAppearanceTinting(
        boolean isGradient,
        MolangValue4 staticColor,
        Color gradientColor
) {
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

    public record Color(
            TreeMap<Float, Integer> gradient,
            MolangValue interpolant
    ) {
        public static final Codec<Color> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                EyelibCodec.treeMap(
                        Codec.STRING.xmap(Float::parseFloat, String::valueOf),
                        Codec.STRING.xmap(s -> Integer.parseUnsignedInt(s.substring(1), 16), Float::toString),
                        Comparator.comparingDouble(k -> k)
                ).fieldOf("gradient").forGetter(o -> o.gradient),
                MolangValue.CODEC.fieldOf("interpolant").forGetter(o -> o.interpolant)
        ).apply(ins, Color::new));
    }
}

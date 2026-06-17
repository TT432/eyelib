package io.github.tt432.eyelib.importer.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author TT432
 */
@NullMarked
public record BrClientEntityScripts(
        MolangValue initialize,
        MolangValue pre_animation,
        MolangValue parent_setup,
        Map<String, MolangValue> animate,
        MolangValue scale,
        Optional<MolangValue> scaleX,
        Optional<MolangValue> scaleY,
        Optional<MolangValue> scaleZ
) {
    public static final Codec<BrClientEntityScripts> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("initialize", MolangValue.ZERO).forGetter(BrClientEntityScripts::initialize),
            MolangValue.CODEC.optionalFieldOf("pre_animation", MolangValue.ZERO).forGetter(BrClientEntityScripts::pre_animation),
            MolangValue.CODEC.optionalFieldOf("parent_setup", MolangValue.ZERO).forGetter(BrClientEntityScripts::parent_setup),
            Codec.either(Codec.STRING, Codec.unboundedMap(Codec.STRING, MolangValue.CODEC))
                    .listOf()
                    .xmap(le -> {
                                Map<String, MolangValue> map = new HashMap<>();
                                for (Either<String, Map<String, MolangValue>> stringMapEither : le) {
                                    stringMapEither.left().ifPresent(s -> map.put(s, MolangValue.ONE));
                                    stringMapEither.right().ifPresent(map::putAll);
                                }
                                return map;
                            },
                            map -> List.of(Either.right(map))
                    ).optionalFieldOf("animate", Map.of()).forGetter(BrClientEntityScripts::animate),
            MolangValue.CODEC.optionalFieldOf("scale", MolangValue.ONE).forGetter(BrClientEntityScripts::scale),
            MolangValue.CODEC.optionalFieldOf("scaleX").forGetter(BrClientEntityScripts::scaleX),
            MolangValue.CODEC.optionalFieldOf("scaleY").forGetter(BrClientEntityScripts::scaleY),
            MolangValue.CODEC.optionalFieldOf("scaleZ").forGetter(BrClientEntityScripts::scaleZ)
    ).apply(ins, BrClientEntityScripts::new));

    public float getScaleX(MolangScope scope) {
        return scaleX.map(mv -> mv.eval(scope)).orElse(scale.eval(scope));
    }

    public float getScaleY(MolangScope scope) {
        return scaleY.map(mv -> mv.eval(scope)).orElse(scale.eval(scope));
    }

    public float getScaleZ(MolangScope scope) {
        return scaleZ.map(mv -> mv.eval(scope)).orElse(scale.eval(scope));
    }
}
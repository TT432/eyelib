package io.github.tt432.eyelib.client.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @param initialize    该脚本在实体首次初始化时运行，即在实体生成时以及每次加载时运行。
 *                      这意味着每次您登录到您的世界时，它都会运行此脚本中的任何内容。这对于设置自定义变量的默认值很有用。
 * @param pre_animation 该脚本在动画播放之前运行每一帧。这对于计算动画运行之前需要计算的动画中使用的变量非常有用。
 * @param animate       该脚本在pre_animation之后的每一帧运行。这是运行动画和动画控制器的地方。每一帧中的每个动画或动画控制器都会运行此键。
 * @author TT432
 */
public record BrClientEntityScripts(
        MolangValue initialize,
        MolangValue pre_animation,
        Map<String, MolangValue> animate,
        MolangValue scale,
        Optional<MolangValue> scaleX,
        Optional<MolangValue> scaleY,
        Optional<MolangValue> scaleZ
) {
    public static final Codec<BrClientEntityScripts> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            MolangValue.CODEC.optionalFieldOf("initialize", MolangValue.ZERO).forGetter(BrClientEntityScripts::initialize),
            MolangValue.CODEC.optionalFieldOf("pre_animation", MolangValue.ZERO).forGetter(BrClientEntityScripts::pre_animation),
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

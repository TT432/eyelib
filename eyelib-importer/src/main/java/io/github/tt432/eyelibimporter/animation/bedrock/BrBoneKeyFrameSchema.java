package io.github.tt432.eyelibimporter.animation.bedrock;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.MolangValue3;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record BrBoneKeyFrameSchema(
        List<MolangValue3> dataPoints,
        LerpMode lerpMode
) {
    public enum LerpMode {
        LINEAR,
        CATMULLROM;

        public static final Codec<LerpMode> CODEC = Codec.STRING.xmap(
                value -> switch (value.toLowerCase()) {
                    case "catmullrom" -> CATMULLROM;
                    default -> LINEAR;
                },
                value -> value.name().toLowerCase()
        );
    }

    private static <T> T unwrap(Either<? extends T, ? extends T> either) {
        return either.map(Function.identity(), Function.identity());
    }

    private static <A> Codec<List<A>> singleOrList(Codec<A> codec) {
        return Codec.either(codec.xmap(List::of, list -> list.get(0)), codec.listOf()).xmap(BrBoneKeyFrameSchema::unwrap, Either::right);
    }

    private static <A> Codec<A> checked(Codec<A> sourceCodec, Function<A, DataResult<A>> checker) {
        return sourceCodec.flatXmap(checker, checker);
    }

    public static final Codec<BrBoneKeyFrameSchema> CODEC;

    static {
        Codec<BrBoneKeyFrameSchema> sourceCodec = RecordCodecBuilder.create(ins -> ins.group(
                LerpMode.CODEC.optionalFieldOf("lerp_mode", LerpMode.LINEAR).forGetter(BrBoneKeyFrameSchema::lerpMode),
                MolangValue3.CODEC.optionalFieldOf("pre").forGetter(schema -> Optional.of(schema.dataPoints().get(0))),
                MolangValue3.CODEC.optionalFieldOf("post").forGetter(schema -> schema.dataPoints().size() < 2 ? Optional.empty() : Optional.of(schema.dataPoints().get(schema.dataPoints().size() - 1)))
        ).apply(ins, (mode, pre, post) -> {
            if (pre.isPresent() && post.isEmpty()) {
                return new BrBoneKeyFrameSchema(List.of(pre.get()), mode);
            } else if (post.isPresent() && (pre.isEmpty() || mode == LerpMode.CATMULLROM)) {
                return new BrBoneKeyFrameSchema(List.of(post.get()), mode);
            } else {
                return new BrBoneKeyFrameSchema(List.of(pre.orElseThrow(), post.orElseThrow()), mode);
            }
        }));

        CODEC = Codec.either(
                Codec.either(MolangValue3.CODEC, MolangValue.CODEC.xmap(value -> new MolangValue3(value, value, value), MolangValue3::x))
                        .xmap(BrBoneKeyFrameSchema::unwrap, Either::left)
                        .xmap(value -> new BrBoneKeyFrameSchema(List.of(value), LerpMode.LINEAR), schema -> schema.dataPoints().get(0)),
                checked(sourceCodec, schema -> schema.dataPoints().isEmpty()
                        ? DataResult.error(() -> "BoneKeyFrame need pre or post.")
                        : DataResult.success(schema))
        ).xmap(BrBoneKeyFrameSchema::unwrap, Either::right);
    }
}

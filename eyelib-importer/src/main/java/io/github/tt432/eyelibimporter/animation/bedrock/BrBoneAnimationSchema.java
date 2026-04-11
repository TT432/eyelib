package io.github.tt432.eyelibimporter.animation.bedrock;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public record BrBoneAnimationSchema(
        TreeMap<Float, BrBoneKeyFrameSchema> rotation,
        TreeMap<Float, BrBoneKeyFrameSchema> position,
        TreeMap<Float, BrBoneKeyFrameSchema> scale
) {
    private static <T> T unwrap(Either<? extends T, ? extends T> either) {
        return either.map(Function.identity(), Function.identity());
    }

    private static <A> Codec<A> withAlternative(Codec<A> primary, Codec<? extends A> alternative) {
        return Codec.either(primary, alternative).xmap(BrBoneAnimationSchema::unwrap, Either::left);
    }

    private static Codec<TreeMap<Float, BrBoneKeyFrameSchema>> keyFrameListCodec() {
        Codec<TreeMap<Float, BrBoneKeyFrameSchema>> treeCodec = Codec.unboundedMap(Codec.STRING, BrBoneKeyFrameSchema.CODEC)
                .xmap(map -> {
                    TreeMap<Float, BrBoneKeyFrameSchema> result = new TreeMap<>(Float::compare);
                    map.forEach((key, value) -> result.put(Float.parseFloat(key), value));
                    return result;
                }, map -> {
                    LinkedHashMap<String, BrBoneKeyFrameSchema> result = new LinkedHashMap<>();
                    map.forEach((key, value) -> result.put(Float.toString(key), value));
                    return result;
                });
        return withAlternative(
                treeCodec,
                BrBoneKeyFrameSchema.CODEC.xmap(schema -> {
                    TreeMap<Float, BrBoneKeyFrameSchema> result = new TreeMap<>(Float::compare);
                    result.put(0F, schema);
                    return result;
                }, map -> map.get(0F))
        );
    }

    public static final Codec<BrBoneAnimationSchema> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            keyFrameListCodec().optionalFieldOf("rotation", new TreeMap<>(Float::compare)).forGetter(BrBoneAnimationSchema::rotation),
            keyFrameListCodec().optionalFieldOf("position", new TreeMap<>(Float::compare)).forGetter(BrBoneAnimationSchema::position),
            keyFrameListCodec().optionalFieldOf("scale", new TreeMap<>(Float::compare)).forGetter(BrBoneAnimationSchema::scale)
    ).apply(ins, BrBoneAnimationSchema::new));
}

package io.github.tt432.eyelibimporter.animation.bedrock;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public record BrAnimationEntrySchema(
        BrLoopType loop,
        float animationLength,
        boolean overridePreviousAnimation,
        MolangValue animTimeUpdate,
        MolangValue blendWeight,
        MolangValue startDelay,
        MolangValue loopDelay,
        TreeMap<Float, List<BrEffectsKeyFrame>> soundEffects,
        TreeMap<Float, List<BrEffectsKeyFrame>> particleEffects,
        TreeMap<Float, List<MolangValue>> timeline,
        Map<String, BrBoneAnimationSchema> bones
) {
    private static <T> T unwrap(Either<? extends T, ? extends T> either) {
        return either.map(Function.identity(), Function.identity());
    }

    private static <A> Codec<List<A>> singleOrList(Codec<A> codec) {
        return Codec.either(codec.xmap(List::of, list -> list.get(0)), codec.listOf()).xmap(BrAnimationEntrySchema::unwrap, Either::right);
    }

    private static Codec<TreeMap<Float, List<BrEffectsKeyFrame>>> effectsCodec() {
        return Codec.unboundedMap(
                Codec.STRING,
                singleOrList(BrEffectsKeyFrame.Factory.CODEC).xmap(
                        factoryList -> {
                            List<BrEffectsKeyFrame> frames = new ArrayList<>();
                            for (BrEffectsKeyFrame.Factory factory : factoryList) {
                                frames.add(factory.to(0));
                            }
                            return frames;
                        },
                        frames -> frames.stream().map(BrEffectsKeyFrame.Factory::from).toList()
                )
        ).xmap(map -> {
            TreeMap<Float, List<BrEffectsKeyFrame>> result = new TreeMap<>(Comparator.comparingDouble(key -> key));
            map.forEach((key, value) -> {
                float timestamp = Float.parseFloat(key);
                result.put(timestamp, value.stream().map(frame -> new BrEffectsKeyFrame(timestamp, frame.effect(), frame.locator(), frame.preEffectScript())).toList());
            });
            return result;
        }, map -> {
            Map<String, List<BrEffectsKeyFrame>> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(Float.toString(key), value));
            return result;
        });
    }

    private static Codec<TreeMap<Float, List<MolangValue>>> timelineCodec() {
        Codec<List<MolangValue>> elementCodec = singleOrList(MolangValue.CODEC);
        return Codec.unboundedMap(Codec.STRING, elementCodec).xmap(map -> {
            TreeMap<Float, List<MolangValue>> result = new TreeMap<>(Comparator.comparingDouble(key -> key));
            map.forEach((key, value) -> result.put(Float.parseFloat(key), value));
            return result;
        }, map -> {
            Map<String, List<MolangValue>> result = new HashMap<>();
            map.forEach((key, value) -> result.put(Float.toString(key), value));
            return result;
        });
    }

    public static final Codec<BrAnimationEntrySchema> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            BrLoopType.CODEC.optionalFieldOf("loop", BrLoopType.ONCE).forGetter(BrAnimationEntrySchema::loop),
            Codec.FLOAT.optionalFieldOf("animation_length", 0F).forGetter(BrAnimationEntrySchema::animationLength),
            Codec.BOOL.optionalFieldOf("override_previous_animation", false).forGetter(BrAnimationEntrySchema::overridePreviousAnimation),
            MolangValue.CODEC.optionalFieldOf("anim_time_update", new MolangValue("query.anim_time + query.delta_time")).forGetter(BrAnimationEntrySchema::animTimeUpdate),
            MolangValue.CODEC.optionalFieldOf("blend_weight", MolangValue.ONE).forGetter(BrAnimationEntrySchema::blendWeight),
            MolangValue.CODEC.optionalFieldOf("start_delay", MolangValue.ZERO).forGetter(BrAnimationEntrySchema::startDelay),
            MolangValue.CODEC.optionalFieldOf("loop_delay", MolangValue.ZERO).forGetter(BrAnimationEntrySchema::loopDelay),
            effectsCodec().optionalFieldOf("sound_effects", new TreeMap<>(Comparator.comparingDouble(key -> key))).forGetter(BrAnimationEntrySchema::soundEffects),
            effectsCodec().optionalFieldOf("particle_effects", new TreeMap<>(Comparator.comparingDouble(key -> key))).forGetter(BrAnimationEntrySchema::particleEffects),
            timelineCodec().optionalFieldOf("timeline", new TreeMap<>(Comparator.comparingDouble(key -> key))).forGetter(BrAnimationEntrySchema::timeline),
            Codec.unboundedMap(Codec.STRING, BrBoneAnimationSchema.CODEC).optionalFieldOf("bones", Map.of()).forGetter(BrAnimationEntrySchema::bones)
    ).apply(ins, BrAnimationEntrySchema::new));
}

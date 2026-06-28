package io.github.tt432.eyelib.importer.animation.bedrock.controller;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangValue;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.List;
import java.util.Map;

import java.util.function.Function;

/**
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BrAcState(
        Map<String, MolangValue> animations,
        MolangValue onEntry,
        MolangValue onExit,
        List<BrAcParticleEffect> particleEffects,
        List<String> soundEffects,
        Map<String, MolangValue> transitions,
        float blendTransition,
        boolean blendViaShortestPath
) {
    private static <T> T unwrap(Either<? extends T, ? extends T> either) {
        return either.map(Function.identity(), Function.identity());
    }

    private static Codec<Map<String, MolangValue>> animationEntryCodec() {
        return Codec.either(
                Codec.unboundedMap(Codec.STRING, MolangValue.CODEC),
                Codec.STRING.xmap(s -> Map.of(s, MolangValue.TRUE_VALUE), map -> map.keySet().iterator().next())
        ).xmap(BrAcState::unwrap, Either::left);
    }

    private static Codec<Map<String, MolangValue>> mapOrListedMap(Codec<Map<String, MolangValue>> elementCodec) {
        return Codec.either(
                elementCodec,
                elementCodec.listOf().xmap(BrAcState::mergeMaps, List::of)
        ).xmap(BrAcState::unwrap, Either::left);
    }

    private static Map<String, MolangValue> mergeMaps(List<Map<String, MolangValue>> list) {
        Map<String, MolangValue> result = new Object2ObjectOpenHashMap<>();
        for (Map<String, MolangValue> map : list) {
            result.putAll(map);
        }
        return result;
    }

    public static final Codec<BrAcState> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            mapOrListedMap(animationEntryCodec()).optionalFieldOf("animations", Map.of()).forGetter(o -> o.animations),
            MolangValue.CODEC.optionalFieldOf("on_entry", MolangValue.ZERO).forGetter(o -> o.onEntry),
            MolangValue.CODEC.optionalFieldOf("on_exit", MolangValue.ZERO).forGetter(o -> o.onExit),
            BrAcParticleEffect.CODEC.listOf().optionalFieldOf("particle_effects", List.of()).forGetter(o -> o.particleEffects),
            Codec.STRING.fieldOf("effect").codec().listOf().optionalFieldOf("sound_effects", List.of()).forGetter(o -> o.soundEffects),
            mapOrListedMap(Codec.unboundedMap(Codec.STRING, MolangValue.CODEC)).optionalFieldOf("transitions", Map.of()).forGetter(o -> o.transitions),
            Codec.FLOAT.optionalFieldOf("blend_transition", 0F).forGetter(o -> o.blendTransition),
            Codec.BOOL.optionalFieldOf("blend_via_shortest_path", false).forGetter(o -> o.blendViaShortestPath)
    ).apply(ins, BrAcState::new));
}
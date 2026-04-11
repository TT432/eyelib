package io.github.tt432.eyelibimporter.animation.bedrock.controller;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangValue;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @param animations           动画名称 -> 混合系数（动画变换倍数）
 * @param transitions          stateName -> condition
 * @param blendTransition      插值时间
 * @param blendViaShortestPath 线性 t/f 平滑 (存疑)
 * @author TT432
 */
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

    public static final Codec<BrAcState> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            animationEntryCodec().listOf().xmap(
                    l -> {
                        Map<String, MolangValue> result = new Object2ObjectOpenHashMap<>();
                        for (Map<String, MolangValue> stringMolangValueMap : l) {
                            Set<Map.Entry<String, MolangValue>> entrySet = stringMolangValueMap.entrySet();
                            for (Map.Entry<String, MolangValue> entry : entrySet) {
                                result.put(entry.getKey(), entry.getValue());
                            }
                        }
                        return result;
                    },
                    List::of
            ).optionalFieldOf("animations", Map.of()).forGetter(o -> o.animations),
            MolangValue.CODEC.optionalFieldOf("on_entry", MolangValue.ZERO).forGetter(o -> o.onEntry),
            MolangValue.CODEC.optionalFieldOf("on_exit", MolangValue.ZERO).forGetter(o -> o.onExit),
            BrAcParticleEffect.CODEC.listOf().optionalFieldOf("particle_effects", List.of()).forGetter(o -> o.particleEffects),
            Codec.STRING.listOf().optionalFieldOf("sound_effects", List.of()).forGetter(o -> o.soundEffects),
            Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).listOf().xmap(
                    l -> {
                        Map<String, MolangValue> map = new Object2ObjectOpenHashMap<>();
                        for (Map<String, MolangValue> stringMolangValueMap : l) {
                            Set<Map.Entry<String, MolangValue>> entrySet = stringMolangValueMap.entrySet();
                            for (Map.Entry<String, MolangValue> entry : entrySet) {
                                map.put(entry.getKey(), entry.getValue());
                            }
                        }
                        return map;
                    },
                    List::of
            ).optionalFieldOf("transitions", Map.of()).forGetter(o -> o.transitions),
            Codec.FLOAT.optionalFieldOf("blend_transition", 0F).forGetter(o -> o.blendTransition),
            Codec.BOOL.optionalFieldOf("blend_via_shortest_path", false).forGetter(o -> o.blendViaShortestPath)
    ).apply(ins, BrAcState::new));
}

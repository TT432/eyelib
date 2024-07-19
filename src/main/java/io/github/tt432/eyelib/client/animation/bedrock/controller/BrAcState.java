package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangValue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public static final Codec<BrAcState> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.withAlternative(
                    Codec.STRING.xmap(s -> Map.of(s, MolangValue.TRUE_VALUE), map -> map.keySet().iterator().next()),
                    Codec.unboundedMap(Codec.STRING, MolangValue.CODEC)
            ).listOf().xmap(
                    l -> l.stream()
                            .map(Map::entrySet)
                            .flatMap(Set::stream)
                            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b)),
                    List::of
            ).optionalFieldOf("animations", Map.of()).forGetter(o -> o.animations),
            MolangValue.CODEC.optionalFieldOf("on_entry", MolangValue.ZERO).forGetter(o -> o.onEntry),
            MolangValue.CODEC.optionalFieldOf("on_exit", MolangValue.ZERO).forGetter(o -> o.onExit),
            BrAcParticleEffect.CODEC.listOf().optionalFieldOf("particle_effects", List.of()).forGetter(o -> o.particleEffects),
            Codec.STRING.listOf().optionalFieldOf("sound_effects", List.of()).forGetter(o -> o.soundEffects),
            Codec.unboundedMap(Codec.STRING, MolangValue.CODEC).listOf().xmap(
                    l -> l.stream()
                            .map(Map::entrySet)
                            .flatMap(Set::stream)
                            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b)),
                    List::of
            ).optionalFieldOf("transitions", Map.of()).forGetter(o -> o.transitions),
            Codec.FLOAT.optionalFieldOf("blend_transition", 0F).forGetter(o -> o.blendTransition),
            Codec.BOOL.optionalFieldOf("blend_via_shortest_path", false).forGetter(o -> o.blendViaShortestPath)
    ).apply(ins, BrAcState::new));
}

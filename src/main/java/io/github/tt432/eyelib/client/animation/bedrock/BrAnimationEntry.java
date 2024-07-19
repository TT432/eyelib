package io.github.tt432.eyelib.client.animation.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.codec.EyelibCodec;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @param override_previous_animation TODO 不确定
 * @param anim_time_update            TODO 不确定
 * @param blendWeight                 动画混合时的权重
 * @param start_delay                 TODO 不确定
 * @param loop_delay                  TODO 不确定
 * @author TT432
 */
public record BrAnimationEntry(
        BrLoopType loop,
        float animationLength,
        boolean override_previous_animation,
        @Nullable
        MolangValue anim_time_update,
        MolangValue blendWeight,
        @Nullable
        MolangValue start_delay,
        @Nullable
        MolangValue loop_delay,
        TreeMap<Float, List<BrEffectsKeyFrame>> soundEffects,
        TreeMap<Float, List<BrEffectsKeyFrame>> particleEffects,
        TreeMap<Float, List<MolangValue>> timeline,
        Map<String, BrBoneAnimation> bones
) {
    private static final Codec<TreeMap<Float, List<BrEffectsKeyFrame>>> EFFECTS_CODEC = EyelibCodec.treeMap(
            Codec.FLOAT,
            EyelibCodec.singleOrList(BrEffectsKeyFrame.CODEC),
            Comparator.comparingDouble(k -> k)
    );

    public static final Codec<BrAnimationEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            BrLoopType.CODEC.optionalFieldOf("loop", BrLoopType.ONCE).forGetter(o -> o.loop),
            Codec.FLOAT.optionalFieldOf("animation_length", 0F).forGetter(o -> o.animationLength),
            Codec.BOOL.optionalFieldOf("override_previous_animation", false).forGetter(o -> o.override_previous_animation),
            MolangValue.CODEC.optionalFieldOf("anim_time_update", MolangValue.ZERO).forGetter(o -> o.anim_time_update),
            MolangValue.CODEC.optionalFieldOf("blendWeight", MolangValue.ONE).forGetter(o -> o.blendWeight),
            MolangValue.CODEC.optionalFieldOf("start_delay", MolangValue.ZERO).forGetter(o -> o.start_delay),
            MolangValue.CODEC.optionalFieldOf("loop_delay", MolangValue.ZERO).forGetter(o -> o.loop_delay),
            EFFECTS_CODEC.optionalFieldOf("sound_effects", new TreeMap<>()).forGetter(o -> o.soundEffects),
            EFFECTS_CODEC.optionalFieldOf("particle_effects", new TreeMap<>()).forGetter(o -> o.particleEffects),
            EyelibCodec.treeMap(
                    Codec.FLOAT,
                    EyelibCodec.singleOrList(MolangValue.CODEC),
                    Comparator.comparingDouble(k -> k)
            ).optionalFieldOf("timeline", new TreeMap<>()).forGetter(o -> o.timeline),
            Codec.unboundedMap(Codec.STRING, BrBoneAnimation.CODEC).optionalFieldOf("bones", Map.of()).forGetter(o -> o.bones)
    ).apply(ins, BrAnimationEntry::new));
}

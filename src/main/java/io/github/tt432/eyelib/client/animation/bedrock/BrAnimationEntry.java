package io.github.tt432.eyelib.client.animation.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

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
        AnimationEffect<BrEffectsKeyFrame> soundEffects,
        AnimationEffect<BrEffectsKeyFrame> particleEffects,
        AnimationEffect<MolangValue> timeline,
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
            EFFECTS_CODEC.xmap(map -> new AnimationEffect<>(map, (scope, frame) ->
                    scope.getOwner().ownerAs(Entity.class).ifPresent(e -> {
                        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(ResourceLocations.of(frame.effect()));
                        if (!e.isSilent()) {
                            e.level().playSound(Minecraft.getInstance().player,
                                    e.getX(), e.getY(), e.getZ(), soundEvent, e.getSoundSource(), 1, 1);
                        }
                    })), AnimationEffect::data
            ).optionalFieldOf("sound_effects", AnimationEffect.empty()).forGetter(o -> o.soundEffects),
            EFFECTS_CODEC.xmap(map -> new AnimationEffect<>(map, (scope, frame) -> {
                        // todo
                    }), AnimationEffect::data
            ).optionalFieldOf("particle_effects", AnimationEffect.empty()).forGetter(o -> o.particleEffects),
            EyelibCodec.treeMap(Codec.FLOAT, EyelibCodec.singleOrList(MolangValue.CODEC), Comparator.comparingDouble(k -> k))
                    .xmap(map -> new AnimationEffect<>(map, (scope, mv) -> mv.eval(scope)), AnimationEffect::data)
                    .optionalFieldOf("timeline", AnimationEffect.empty())
                    .forGetter(o -> o.timeline),
            Codec.unboundedMap(Codec.STRING, BrBoneAnimation.CODEC).optionalFieldOf("bones", Map.of()).forGetter(o -> o.bones)
    ).apply(ins, BrAnimationEntry::new));
}

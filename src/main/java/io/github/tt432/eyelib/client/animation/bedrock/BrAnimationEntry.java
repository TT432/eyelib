package io.github.tt432.eyelib.client.animation.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelib.client.animation.AnimationSet;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import io.github.tt432.eyelib.util.math.EyeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @param override_previous_animation TODO 不确定
 * @param anim_time_update            TODO 不确定
 * @param blendWeight                 动画混合时的权重
 * @param start_delay                 TODO 不确定
 * @param loop_delay                  TODO 不确定
 * @author TT432
 */
public record BrAnimationEntry(
        String name,
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
) implements Animation<Object> {
    private static final Codec<TreeMap<Float, List<BrEffectsKeyFrame>>> EFFECTS_CODEC = EyelibCodec.treeMap(
            Codec.FLOAT,
            EyelibCodec.singleOrList(BrEffectsKeyFrame.CODEC),
            Comparator.comparingDouble(k -> k)
    );

    public record Factory(
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
        public static final Codec<Factory> CODEC = RecordCodecBuilder.create(ins -> ins.group(
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
        ).apply(ins, Factory::new));

        public BrAnimationEntry create(String name) {
            return new BrAnimationEntry(
                    name,
                    loop,
                    animationLength,
                    override_previous_animation,
                    anim_time_update,
                    blendWeight,
                    start_delay,
                    loop_delay,
                    soundEffects,
                    particleEffects,
                    timeline,
                    bones);
        }

        public static Factory from(BrAnimationEntry entry) {
            return new Factory(
                    entry.loop(),
                    entry.animationLength(),
                    entry.override_previous_animation(),
                    entry.anim_time_update(),
                    entry.blendWeight(),
                    entry.start_delay(),
                    entry.loop_delay(),
                    entry.soundEffects(),
                    entry.particleEffects(),
                    entry.timeline(),
                    entry.bones()
            );
        }
    }

    @Override
    public Object createData() {
        return new Object();
    }

    @Override
    public List<AnimationEffect<?>> getAllEffect() {
        return List.of(soundEffects, particleEffects, timeline);
    }

    @Override
    public void tickAnimation(Object data, AnimationSet animationSet, MolangScope scope,
                              float ticks, float multiplier, BoneRenderInfos infos,
                              List<AnimationEffect.Runtime<?>> runtime, Runnable loopAction) {
        evaluateAnimationTime(ticks, loopAction).ifPresent(animTick -> {
            runtime.forEach(r -> AnimationEffect.Runtime.processEffect(r, ticks, scope));

            bones().forEach((boneName, boneAnim) -> {
                BoneRenderInfoEntry entry = infos.get(boneName);

                boneAnim.lerpPosition(scope, animTick).ifPresent(p ->
                        entry.getRenderPosition().add(p.mul(multiplier / 16)));

                boneAnim.lerpRotation(scope, animTick).ifPresent(r ->
                        entry.getRenderRotation()
                                .add(r.mul(multiplier * EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1)));

                boneAnim.lerpScale(scope, animTick).ifPresent(s -> entry.getRenderScala().mul(
                        EyeMath.notZero(1 + (s.x - 1) * multiplier, 0.00001F),
                        EyeMath.notZero(1 + (s.y - 1) * multiplier, 0.00001F),
                        EyeMath.notZero(1 + (s.z - 1) * multiplier, 0.00001F)
                ));
            });
        });
    }

    private Optional<Float> evaluateAnimationTime(float startedTime, Runnable loopAction) {
        if (isAnimationFinished(startedTime) && animationLength() > 0) {
            switch (loop()) {
                case LOOP:
                    loopAction.run();
                    return Optional.of(startedTime % animationLength());
                case ONCE:
                    return Optional.empty();
            }
        }

        return Optional.of(startedTime);
    }
}

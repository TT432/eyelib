package io.github.tt432.eyelib.client.animation.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.chin.codec.ChinExtraCodecs;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.github.tt432.eyelib.util.math.EyeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @param override_previous_animation TODO 不确定
 * @param anim_time_update            动画播放速度
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
) implements Animation<BrAnimationEntry.Data> {
    public static Codec<BrAnimationEntry> codec(String name) {
        return RecordCodecBuilder.create(ins -> {
            final Codec<List<MolangValue>> elementCodec = ChinExtraCodecs.singleOrList(MolangValue.CODEC);
            Comparator<Float> comparator = Comparator.comparingDouble(k -> k);
            return ins.group(
                    BrLoopType.CODEC.optionalFieldOf("loop", BrLoopType.ONCE).forGetter(o -> o.loop),
                    Codec.FLOAT.optionalFieldOf("animation_length", 0F).forGetter(o -> o.animationLength),
                    Codec.BOOL.optionalFieldOf("override_previous_animation", false).forGetter(o -> o.override_previous_animation),
                    MolangValue.CODEC.optionalFieldOf("anim_time_update", new MolangValue("query.anim_time + query.delta_time")).forGetter(o -> o.anim_time_update),
                    MolangValue.CODEC.optionalFieldOf("blend_weight", MolangValue.ONE).forGetter(o -> o.blendWeight),
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
                    Codec.unboundedMap(Codec.STRING, elementCodec).xmap(map -> {
                                TreeMap<Float, List<MolangValue>> result = new TreeMap<>(comparator);
                                map.forEach((k, v) -> result.put(Float.parseFloat(k), v));
                                return result;
                            }, map -> {
                                Map<String, List<MolangValue>> result = new HashMap<>();
                                map.forEach((k, v) -> result.put(k.toString(), v));
                                return result;
                            }).xmap(map -> new AnimationEffect<>(map, (scope, mv) -> mv.eval(scope)), AnimationEffect::data)
                            .optionalFieldOf("timeline", AnimationEffect.empty())
                            .forGetter(o -> o.timeline),
                    Codec.unboundedMap(Codec.STRING.xmap(s -> s.toLowerCase(Locale.ROOT), s -> s), BrBoneAnimation.CODEC).optionalFieldOf("bones", Map.of()).forGetter(o -> o.bones)
            ).apply(ins, (a, b, c, d, e, f, g, h, i, j, k) -> new BrAnimationEntry(name, a, b, c, d, e, f, g, h, i, j, k));
        });
    }

    private static final Codec<TreeMap<Float, List<BrEffectsKeyFrame>>> EFFECTS_CODEC = Codec.dispatchedMap(
            Codec.STRING,
            f -> ChinExtraCodecs.singleOrList(BrEffectsKeyFrame.Factory.CODEC).xmap(
                    fList -> fList.stream().map(v -> v.to(Float.parseFloat(f))).toList(),
                    vList -> vList.stream().map(BrEffectsKeyFrame.Factory::from).toList()
            )
    ).xmap(map -> {
        TreeMap<Float, List<BrEffectsKeyFrame>> result = new TreeMap<>(Comparator.comparingDouble(k -> k));
        map.forEach((k, v) -> result.put(Float.parseFloat(k), v));
        return result;
    }, map -> {
        Map<String, List<BrEffectsKeyFrame>> result = new HashMap<>();
        map.forEach((k, v) -> result.put(k.toString(), v));
        return result;
    });

    public final class Data {
        int loopedTimes;
        private final List<AnimationEffect.Runtime<?>> effects = new ArrayList<>();

        private float lastTicks;
        public float animTime;
        public float deltaTime;

        private Data resetEffects() {
            effects.add(soundEffects.runtime());
            effects.add(particleEffects.runtime());
            effects.add(timeline.runtime());
            return this;
        }
    }

    @Override
    public void onFinish(Data data) {
        data.resetEffects();
    }

    @Override
    public boolean isAnimationFinished(Data data) {
        return data.animTime > animationLength;
    }

    @Override
    public Data createData() {
        return new Data().resetEffects();
    }

    @Override
    public void tickAnimation(Data data, Map<String, String> animations, MolangScope scope,
                              float ticks, float playSpeed, float multiplier, BoneRenderInfos infos) {
        multiplier *= Math.clamp(blendWeight().eval(scope), 0, 1);

        scope.getOwner().replace(Data.class, data);
        data.deltaTime = ticks - data.lastTicks;
        data.lastTicks = ticks;
        var animTimeUpdate = data.animTime + (anim_time_update().eval(scope) - data.animTime) * playSpeed;
        data.animTime = animTimeUpdate;

        float animTick;

        if (animationLength() > 0) {
            animTick = switch (loop()) {
                case LOOP -> {
                    int loopedTimes = (int) (animTimeUpdate / animationLength());
                    if (loopedTimes > data.loopedTimes) {
                        data.loopedTimes = loopedTimes;
                        data.resetEffects();
                    }
                    yield animTimeUpdate % animationLength();
                }
                case ONCE -> animTimeUpdate;
                default -> Math.min(animTimeUpdate, animationLength());
            };
        } else {
            animTick = animTimeUpdate;
        }

        for (AnimationEffect.Runtime<?> r : data.effects) {
            AnimationEffect.Runtime.processEffect(r, animTick, scope);
        }

        for (Map.Entry<String, BrBoneAnimation> boneEntry : bones().entrySet()) {
            var boneName = boneEntry.getKey();
            var boneAnim = boneEntry.getValue();
            BoneRenderInfoEntry entry = infos.getData(boneName);

            Vector3f pos = boneAnim.lerpPosition(scope, animTick);

            if (pos != null) {
                pos.mul(multiplier).div(16).mul(-1, 1, 1);
                entry.getRenderPosition().add(pos);
            }

            Vector3f rotation = boneAnim.lerpRotation(scope, animTick);

            if (rotation != null) {
                rotation.mul(multiplier).mul(EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1);
                entry.getRenderRotation().add(rotation);
            }

            Vector3f scale = boneAnim.lerpScale(scope, animTick);

            if (scale != null) {
                scale.sub(1, 1, 1).mul(multiplier).add(1, 1, 1);
                entry.getRenderScala().mul(scale);
            }
        }
    }
}

package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelibimporter.animation.bedrock.BrEffectsKeyFrame;
import io.github.tt432.eyelibimporter.animation.bedrock.BrLoopType;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibimporter.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.particle.ParticleSpawnService;
import io.github.tt432.eyelib.client.particle.ParticleLookup;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelib.util.ResourceLocations;
import io.github.tt432.eyelib.util.codec.ChinExtraCodecs;
import io.github.tt432.eyelib.util.codec.CodecHelper;
import io.github.tt432.eyelib.util.math.EyeMath;
import io.github.tt432.eyelib.util.math.MathHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

import org.jetbrains.annotations.Nullable;
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
        Int2ObjectMap<BrBoneAnimation> bones
) implements Animation<BrAnimationEntry.Data> {
    public static BrAnimationEntry fromSchema(String name, BrAnimationEntrySchema schema) {
        TreeMap<Float, List<BrEffectsKeyFrame>> soundEffectsData = new TreeMap<>(schema.soundEffects());
        TreeMap<Float, List<BrEffectsKeyFrame>> particleEffectsData = new TreeMap<>(schema.particleEffects());
        TreeMap<Float, List<MolangValue>> timelineData = new TreeMap<>(schema.timeline());
        Int2ObjectMap<BrBoneAnimation> boneAnimations = new Int2ObjectOpenHashMap<>();
        schema.bones().forEach((boneName, boneSchema) -> boneAnimations.put(GlobalBoneIdHandler.get(boneName), BrBoneAnimation.fromSchema(boneSchema)));

        return new BrAnimationEntry(
                name,
                schema.loop(),
                schema.animationLength(),
                schema.overridePreviousAnimation(),
                schema.animTimeUpdate(),
                schema.blendWeight(),
                schema.startDelay(),
                schema.loopDelay(),
                new AnimationEffect<>(soundEffectsData, (scope, ticks, frame) ->
                        scope.getOwner().onHiveOwners(Entity.class, BrClientEntity.class, (e, clientEntity) -> {
                            String s = clientEntity.sound_effects().get(frame.effect());

                            if (s != null) {
                                SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(ResourceLocations.of(s));

                                if (!e.isSilent()) {
                                    e.level().playSound(Minecraft.getInstance().player,
                                            e.getX(), e.getY(), e.getZ(), soundEvent, e.getSoundSource(), 1, 1);
                                }
                            }
                            return Boolean.TRUE;
                        })),
                new AnimationEffect<>(particleEffectsData, (scope, ticks, frame) ->
                        scope.getOwner().onHiveOwners(Entity.class, Data.class, BrClientEntity.class, (entity, data, clientEntity) -> {
                            String s = clientEntity.particle_effects().get(frame.effect());

                            if (s != null) {
                                BrParticle brParticle = ParticleLookup.get(ResourceLocations.of(s));
                                if (brParticle != null) {
                                    String uuid = UUID.randomUUID().toString();
                                    BrParticleEmitter emitter = new BrParticleEmitter(brParticle, scope, entity.level(), new Vector3f());
                                    data.particles.add(new RuntimeParticlePlayData(uuid, emitter, frame.locator().orElse(null), ticks));
                                    ParticleSpawnService.spawnEmitter(uuid, emitter);
                                }
                            }
                            return Boolean.TRUE;
                        })),
                new AnimationEffect<>(timelineData, (scope, ticks, mv) -> mv.eval(scope)),
                boneAnimations
        );
    }

    // Helper to return null from lambdas with proper NullAway suppression
    @SuppressWarnings("NullAway")
    @Nullable
    private static <T> T nil() {
        @SuppressWarnings("NullAway")
        T result = null;
        return result;
    }
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
                    EFFECTS_CODEC.xmap(map -> new AnimationEffect<>(map, (scope, ticks, frame) ->
                            scope.getOwner().onHiveOwners(Entity.class, BrClientEntity.class, (e, clientEntity) -> {
                                String s = clientEntity.sound_effects().get(frame.effect());

                                if (s != null) {
                                    SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(ResourceLocations.of(s));

                                    if (!e.isSilent()) {
                                        e.level().playSound(Minecraft.getInstance().player,
                                                e.getX(), e.getY(), e.getZ(), soundEvent, e.getSoundSource(), 1, 1);
                                    }
                                }
                                return Boolean.TRUE;
                            })), AnimationEffect::data
                    ).optionalFieldOf("sound_effects", AnimationEffect.empty()).forGetter(o -> o.soundEffects),
                    EFFECTS_CODEC.xmap(map -> new AnimationEffect<>(map, (scope, ticks, frame) ->
                            scope.getOwner().onHiveOwners(Entity.class, Data.class, BrClientEntity.class, (entity, data, clientEntity) -> {
                                String s = clientEntity.particle_effects().get(frame.effect());

                                if (s != null) {
                                    BrParticle brParticle = ParticleLookup.get(ResourceLocations.of(s));
                                    if (brParticle != null) {
                                        String uuid = UUID.randomUUID().toString();
                                        BrParticleEmitter emitter = new BrParticleEmitter(brParticle, scope, entity.level(), new Vector3f());
                                        data.particles.add(new RuntimeParticlePlayData(uuid, emitter, frame.locator().orElse(null), ticks));
                                        ParticleSpawnService.spawnEmitter(uuid, emitter);
                                    }
                                }
                                return Boolean.TRUE;
                            })), AnimationEffect::data
                    ).optionalFieldOf("particle_effects", AnimationEffect.empty()).forGetter(o -> o.particleEffects),
                    Codec.unboundedMap(Codec.STRING, elementCodec).xmap(map -> {
                                TreeMap<Float, List<MolangValue>> result = new TreeMap<>(comparator);
                                map.forEach((k, v) -> result.put(Float.parseFloat(k), v));
                                return result;
                            }, map -> {
                                Map<String, List<MolangValue>> result = new HashMap<>();
                                map.forEach((k, v) -> result.put(k.toString(), v));
                                return result;
                            }).xmap(map -> new AnimationEffect<>(map, (scope, ticks, mv) -> mv.eval(scope)), AnimationEffect::data)
                            .optionalFieldOf("timeline", AnimationEffect.empty())
                            .forGetter(o -> o.timeline),
                    GlobalBoneIdHandler.map(BrBoneAnimation.CODEC).optionalFieldOf("bones", new Int2ObjectOpenHashMap<>()).forGetter(o -> o.bones)
            ).apply(ins, (a, b, c, d, e, f, g, h, i, j, k) -> new BrAnimationEntry(name, a, b, c, d, e, f, g, h, i, j, k));
        });
    }

    private static final Codec<TreeMap<Float, List<BrEffectsKeyFrame>>> EFFECTS_CODEC = CodecHelper.dispatchedMap(
            Codec.STRING,
            f -> ChinExtraCodecs.singleOrList(BrEffectsKeyFrame.Factory.CODEC).xmap(
                    fList -> {
                        List<BrEffectsKeyFrame> list = new ArrayList<>();
                        for (BrEffectsKeyFrame.Factory v : fList) {
                            BrEffectsKeyFrame brEffectsKeyFrame = v.to(Float.parseFloat(f));
                            list.add(brEffectsKeyFrame);
                        }
                        return list;
                    },
                    vList -> {
                        List<BrEffectsKeyFrame.Factory> list = new ArrayList<>();
                        for (BrEffectsKeyFrame brEffectsKeyFrame : vList) {
                            BrEffectsKeyFrame.Factory from = BrEffectsKeyFrame.Factory.from(brEffectsKeyFrame);
                            list.add(from);
                        }
                        return list;
                    }
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
        private final BrAnimationPlaybackState playbackState = new BrAnimationPlaybackState();
        private final List<AnimationEffect.Runtime<?>> effects = new ArrayList<>();

        private final List<RuntimeParticlePlayData> particles = new ArrayList<>();

        public int loopedTimes;
        public float lastTicks;
        public float animTime;
        public float deltaTime;

        private Data resetEffects() {
            effects.clear();
            effects.add(soundEffects.runtime());
            effects.add(particleEffects.runtime());
            effects.add(timeline.runtime());
            return this;
        }

        private void syncStateFields() {
            loopedTimes = playbackState.loopedTimes();
            lastTicks = playbackState.lastTicks();
            animTime = playbackState.animTime();
            deltaTime = playbackState.deltaTime();
        }
    }

    @Override
    public void onFinish(Data data) {
        data.playbackState.reset();
        data.syncStateFields();
        data.resetEffects();

        for (var particle : data.particles) {
            ParticleSpawnService.removeEmitter(particle.particleUUID());
        }

        data.particles.clear();
    }

    @Override
    public boolean anyAnimationFinished(Data data) {
        return data.playbackState.anyAnimationFinished(animationLength);
    }

    @Override
    public boolean allAnimationFinished(Data data) {
        return anyAnimationFinished(data);
    }

    @Override
    public Data createData() {
        return new Data().resetEffects();
    }

    @Override
    public void tickAnimation(Data data, Map<String, String> animations, MolangScope scope,
                              float ticks, float multiplier, ModelRuntimeData infos, AnimationEffects effects,
                              Runnable animationStartFeedback) {
        multiplier *= MathHelper.clamp(blendWeight().eval(scope), 0, 1);

        if (data.animTime == 0) {
            animationStartFeedback.run();
        }

        scope.getOwner().replace(Data.class, data);
        var animTimeUpdate = anim_time_update().eval(scope);
        BrAnimationPlaybackState.TickResult tickResult = data.playbackState.tick(loop(), animationLength(), ticks, animTimeUpdate);
        data.syncStateFields();

        if (tickResult.loopRestarted()) {
            data.resetEffects();
            animationStartFeedback.run();
        }

        float animTick = tickResult.animTick();

        for (int i = 0; i < data.effects.size(); i++) {
            AnimationEffect.Runtime<?> r = data.effects.get(i);
            AnimationEffect.Runtime.processEffect(r, animTick, scope);
        }

        float finalMultiplier = multiplier;
        bones().int2ObjectEntrySet().forEach((entry) -> {
            var boneName = entry.getIntKey();
            var boneAnim = entry.getValue();
            var renderInfoEntry = infos.getData(boneName);
            if (renderInfoEntry == null) {
                return;
            }

            Vector3f pos = boneAnim.lerpPosition(scope, animTick);

            if (pos != null) {
                pos.mul(finalMultiplier).div(16).mul(-1, 1, 1);
                renderInfoEntry.position.add(pos);
            }

            Vector3f rotation = boneAnim.lerpRotation(scope, animTick);

            if (rotation != null) {
                rotation.mul(finalMultiplier).mul(EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1);
                renderInfoEntry.rotation.add(rotation);
            }

            Vector3f scale = boneAnim.lerpScale(scope, animTick);

            if (scale != null) {
                scale.sub(1, 1, 1).mul(finalMultiplier).add(1, 1, 1);
                renderInfoEntry.scale.mul(scale);
            }
        });

        effects.particles.add(data.particles);
    }
}

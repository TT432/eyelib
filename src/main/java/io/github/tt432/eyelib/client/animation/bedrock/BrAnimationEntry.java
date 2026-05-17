package io.github.tt432.eyelib.client.animation.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelibutil.codec.ChinExtraCodecs;
import io.github.tt432.eyelibutil.codec.CodecHelper;
import io.github.tt432.eyelibimporter.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelibimporter.animation.bedrock.BrLoopType;
import io.github.tt432.eyelibimporter.model.GlobalBoneIdHandler;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @param override_previous_animation TODO 不确定
 * @param anim_time_update            动画播放速度
 * @param blendWeight                 动画混合时的权重
 * @param start_delay                 TODO 不确定
 * @param loop_delay                  TODO 不确定
 *
 * @author TT432
 */
public final class BrAnimationEntry implements Animation {
    private final BrAnimationEntryDefinition definition;

    public BrAnimationEntry(
            String name,
            BrLoopType loop,
            float animationLength,
            boolean override_previous_animation,
            MolangValue anim_time_update,
            MolangValue blendWeight,
            @Nullable MolangValue start_delay,
            @Nullable MolangValue loop_delay,
            AnimationEffect<BrEffectsKeyFrameDefinition> soundEffects,
            AnimationEffect<BrEffectsKeyFrameDefinition> particleEffects,
            AnimationEffect<MolangValue> timeline,
            Int2ObjectMap<BrBoneAnimation> bones
    ) {
        this(new BrAnimationEntryDefinition(
                name,
                loop,
                animationLength,
                override_previous_animation,
                anim_time_update,
                blendWeight,
                start_delay,
                loop_delay,
                BrAnimationEntryTracksDefinition.of(soundEffects, particleEffects, timeline, bones)
        ));
    }

    private BrAnimationEntry(BrAnimationEntryDefinition definition) {
        this.definition = definition;
    }

    public static BrAnimationEntry fromSchema(String name, BrAnimationEntrySchema schema) {
        return new BrAnimationEntry(BrAnimationEntryDefinition.fromSchema(name, schema));
    }

    public static Codec<BrAnimationEntry> codec(String name) {
        return RecordCodecBuilder.create(ins -> {
            final Codec<List<MolangValue>> elementCodec = ChinExtraCodecs.singleOrList(MolangValue.CODEC);
            Comparator<Float> comparator = Comparator.comparingDouble(k -> k);
            return ins.group(
                              BrLoopType.CODEC.optionalFieldOf("loop", BrLoopType.ONCE).forGetter(BrAnimationEntry::loop),
                              Codec.FLOAT.optionalFieldOf("animation_length", 0F).forGetter(BrAnimationEntry::animationLength),
                              Codec.BOOL.optionalFieldOf("override_previous_animation", false)
                                        .forGetter(BrAnimationEntry::override_previous_animation),
                              MolangValue.CODEC.optionalFieldOf("anim_time_update", new MolangValue("query.anim_time + query.delta_time"))
                                               .forGetter(BrAnimationEntry::anim_time_update),
                              MolangValue.CODEC.optionalFieldOf("blend_weight", MolangValue.ONE).forGetter(BrAnimationEntry::blendWeight),
                              MolangValue.CODEC.optionalFieldOf("start_delay", MolangValue.ZERO).forGetter(BrAnimationEntry::start_delay),
                              MolangValue.CODEC.optionalFieldOf("loop_delay", MolangValue.ZERO).forGetter(BrAnimationEntry::loop_delay),
                              EFFECTS_CODEC.xmap(BrAnimationEntryDefinition::soundEffect, AnimationEffect::data)
                                           .optionalFieldOf("sound_effects", AnimationEffect.empty()).forGetter(BrAnimationEntry::soundEffects),
                              EFFECTS_CODEC.xmap(BrAnimationEntryDefinition::particleEffect, AnimationEffect::data)
                                           .optionalFieldOf("particle_effects", AnimationEffect.empty()).forGetter(BrAnimationEntry::particleEffects),
                              Codec.unboundedMap(Codec.STRING, elementCodec)
                                   .xmap(map -> {
                                       TreeMap<Float, List<MolangValue>> result = new TreeMap<>(comparator);
                                       map.forEach((k, v) -> result.put(Float.parseFloat(k), v));
                                       return result;
                                   }, map -> {
                                       Map<String, List<MolangValue>> result = new HashMap<>();
                                       map.forEach((k, v) -> result.put(k.toString(), v));
                                       return result;
                                   })
                                   .xmap(BrAnimationEntryDefinition::timelineEffect, AnimationEffect::data)
                                   .optionalFieldOf("timeline", AnimationEffect.empty())
                                   .forGetter(BrAnimationEntry::timeline),
                              GlobalBoneIdHandler.map(BrBoneAnimation.CODEC)
                                                 .optionalFieldOf("bones", new Int2ObjectOpenHashMap<>())
                                                 .forGetter(BrAnimationEntry::bones)
                      )
                      .apply(ins, (a, b, c, d, e, f, g, h, i, j, k) -> new BrAnimationEntry(name, a, b, c, d, e, f, g, h, i, j, k));
        });
    }

    private static final Codec<TreeMap<Float, List<BrEffectsKeyFrameDefinition>>> EFFECTS_CODEC = CodecHelper.dispatchedMap(
            Codec.STRING,
            f -> ChinExtraCodecs.singleOrList(BrEffectsKeyFrameDefinition.Factory.CODEC).xmap(
                    fList -> {
                        List<BrEffectsKeyFrameDefinition> list = new ArrayList<>();
                        for (BrEffectsKeyFrameDefinition.Factory v : fList) {
                            BrEffectsKeyFrameDefinition brEffectsKeyFrame = v.to(Float.parseFloat(f));
                            list.add(brEffectsKeyFrame);
                        }
                        return list;
                    },
                    vList -> {
                        List<BrEffectsKeyFrameDefinition.Factory> list = new ArrayList<>();
                        for (BrEffectsKeyFrameDefinition brEffectsKeyFrame : vList) {
                            BrEffectsKeyFrameDefinition.Factory from = BrEffectsKeyFrameDefinition.Factory.from(brEffectsKeyFrame);
                            list.add(from);
                        }
                        return list;
                    }
            )
    ).xmap(map -> {
        TreeMap<Float, List<BrEffectsKeyFrameDefinition>> result = new TreeMap<>(Comparator.comparingDouble(k -> k));
        map.forEach((k, v) -> result.put(Float.parseFloat(k), v));
        return result;
    }, map -> {
        Map<String, List<BrEffectsKeyFrameDefinition>> result = new HashMap<>();
        map.forEach((k, v) -> result.put(k.toString(), v));
        return result;
    });

    public final class Data {
        private final BrClipStateOwner owner = new BrClipStateOwner();

        BrClipStateOwner owner() {
            return owner;
        }

        public int loopedTimes() {
            return owner.loopedTimes();
        }

        public float lastTicks() {
            return owner.lastTicks();
        }

        public float animTime() {
            return owner.animTime();
        }

        public float deltaTime() {
            return owner.deltaTime();
        }
    }

    @Override
    public String name() {
        return definition.name();
    }

    public BrAnimationEntryDefinition definition() {
        return definition;
    }

    public BrLoopType loop() {
        return definition.loop();
    }

    public float animationLength() {
        return definition.animationLength();
    }

    public boolean override_previous_animation() {
        return definition.overridePreviousAnimation();
    }

    public MolangValue anim_time_update() {
        return definition.animTimeUpdate();
    }

    public MolangValue blendWeight() {
        return definition.blendWeight();
    }

    public @Nullable MolangValue start_delay() {
        return definition.startDelay();
    }

    public @Nullable MolangValue loop_delay() {
        return definition.loopDelay();
    }

    public AnimationEffect<BrEffectsKeyFrameDefinition> soundEffects() {
        return definition.soundEffects();
    }

    public AnimationEffect<BrEffectsKeyFrameDefinition> particleEffects() {
        return definition.particleEffects();
    }

    public AnimationEffect<MolangValue> timeline() {
        return definition.timeline();
    }

    public Int2ObjectMap<BrBoneAnimation> bones() {
        return definition.bones();
    }

    @Override
    public void onFinish(Object data) {
        if (data instanceof Data d) {
            d.owner().finish(soundEffects(), particleEffects(), timeline());
        }
    }

    @Override
    public boolean anyAnimationFinished(Object data) {
        return data instanceof Data d && d.owner().playbackState().anyAnimationFinished(animationLength());
    }

    @Override
    public boolean allAnimationFinished(Object data) {
        return anyAnimationFinished(data);
    }

    @Override
    public Data createData() {
        Data data = new Data();
        data.owner().resetEffects(soundEffects(), particleEffects(), timeline());
        return data;
    }

    @Override
    public void tickAnimation(Object data, Map<String, String> animations, MolangScope scope,
                              float ticks, float multiplier, ModelRuntimeData infos, AnimationEffects effects,
                              Runnable animationStartFeedback) {
        if (data instanceof Data d) {
            BrClipExecutor.tick(this, d, animations, scope, ticks, multiplier, infos, effects, animationStartFeedback);
        }
    }
}


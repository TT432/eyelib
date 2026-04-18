package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelibimporter.animation.bedrock.BrBoneAnimationSchema;
import io.github.tt432.eyelibimporter.animation.bedrock.BrBoneKeyFrameSchema;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelib.util.ImmutableFloatTreeMap;
import io.github.tt432.eyelib.util.codec.CodecHelper;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * if rotation_global
 * relative_to / rotation -> 'entity'
 * rotation -> [0, 0, 0.01]
 * ---
 * if rotation_global
 * [2] = 0.01
 *
 * @author TT432
 */
public record BrBoneAnimation(
        Map<String, BrAnimationChannel<BrBoneKeyFrame>> channels,
        BrBoneAnimationDefinition compiledDefinition
) {
    public static final String ROTATION = "rotation";
    public static final String POSITION = "position";
    public static final String SCALE = "scale";

    public BrBoneAnimation {
        channels = Collections.unmodifiableMap(new LinkedHashMap<>(channels));
        compiledDefinition = compiledDefinition != null ? compiledDefinition : compileDefinition(channels);
    }

    public BrBoneAnimation(Map<String, BrAnimationChannel<BrBoneKeyFrame>> channels) {
        this(channels, compileDefinition(channels));
    }

    public BrBoneAnimation(ImmutableFloatTreeMap<BrBoneKeyFrame> rotation,
                           ImmutableFloatTreeMap<BrBoneKeyFrame> position,
                           ImmutableFloatTreeMap<BrBoneKeyFrame> scale) {
        this(createChannels(rotation, position, scale));
    }

    public static BrBoneAnimation fromSchema(BrBoneAnimationSchema schema) {
        return new BrBoneAnimation(
                toImmutableMap(schema.rotation()),
                toImmutableMap(schema.position()),
                toImmutableMap(schema.scale())
        );
    }

    public BrBoneAnimationDefinition definition() {
        return compiledDefinition;
    }

    private static BrBoneAnimationDefinition compileDefinition(Map<String, BrAnimationChannel<BrBoneKeyFrame>> channels) {
        LinkedHashMap<String, BrAnimationChannel<BrBoneKeyFrameDefinition>> result = new LinkedHashMap<>();
        channels.forEach((name, channel) -> {
            Float2ObjectOpenHashMap<BrBoneKeyFrameDefinition> data = new Float2ObjectOpenHashMap<>();
            channel.keyFrames().getData().forEach((key, value) -> data.put(key, value.definition()));
            result.put(name, new BrAnimationChannel<>(name, ImmutableFloatTreeMap.of(channel.keyFrames().getSortedKeys(), data)));
        });
        return new BrBoneAnimationDefinition(result);
    }

    private static Map<String, BrAnimationChannel<BrBoneKeyFrame>> createChannels(
            ImmutableFloatTreeMap<BrBoneKeyFrame> rotation,
            ImmutableFloatTreeMap<BrBoneKeyFrame> position,
            ImmutableFloatTreeMap<BrBoneKeyFrame> scale
    ) {
        LinkedHashMap<String, BrAnimationChannel<BrBoneKeyFrame>> channels = new LinkedHashMap<>();
        channels.put(ROTATION, new BrAnimationChannel<>(ROTATION, rotation));
        channels.put(POSITION, new BrAnimationChannel<>(POSITION, position));
        channels.put(SCALE, new BrAnimationChannel<>(SCALE, scale));
        return channels;
    }

    private static ImmutableFloatTreeMap<BrBoneKeyFrame> toImmutableMap(TreeMap<Float, BrBoneKeyFrameSchema> schemaMap) {
        if (schemaMap.isEmpty()) {
            return ImmutableFloatTreeMap.empty();
        }
        float[] sortedKeys = new float[schemaMap.size()];
        Float2ObjectOpenHashMap<BrBoneKeyFrame> data = new Float2ObjectOpenHashMap<>();
        int index = 0;
        for (var entry : schemaMap.entrySet()) {
            float key = entry.getKey();
            sortedKeys[index++] = key;
            data.put(key, BrBoneKeyFrame.fromSchema(key, entry.getValue()));
        }
        return ImmutableFloatTreeMap.of(sortedKeys, data);
    }

    private static final Codec<ImmutableFloatTreeMap<BrBoneKeyFrame>> KEY_FRAME_LIST_CODEC = CodecHelper.withAlternative(
            ImmutableFloatTreeMap.dispatched(f -> BrBoneKeyFrame.Factory.CODEC.xmap(
                    factory -> factory.create(f),
                    BrBoneKeyFrame.Factory::from
            )),
            BrBoneKeyFrame.Factory.CODEC.xmap(f -> f.create(0), BrBoneKeyFrame.Factory::from)
                    .xmap(ImmutableFloatTreeMap::of, map -> map.floorEntry(0F))
    );

    public static final Codec<BrBoneAnimation> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            KEY_FRAME_LIST_CODEC.optionalFieldOf(ROTATION, ImmutableFloatTreeMap.empty()).forGetter(BrBoneAnimation::rotation),
            KEY_FRAME_LIST_CODEC.optionalFieldOf(POSITION, ImmutableFloatTreeMap.empty()).forGetter(BrBoneAnimation::position),
            KEY_FRAME_LIST_CODEC.optionalFieldOf(SCALE, ImmutableFloatTreeMap.empty()).forGetter(BrBoneAnimation::scale)
    ).apply(ins, BrBoneAnimation::new));

    public ImmutableFloatTreeMap<BrBoneKeyFrame> rotation() {
        return channel(ROTATION).keyFrames();
    }

    public ImmutableFloatTreeMap<BrBoneKeyFrame> position() {
        return channel(POSITION).keyFrames();
    }

    public ImmutableFloatTreeMap<BrBoneKeyFrame> scale() {
        return channel(SCALE).keyFrames();
    }

    public BrAnimationChannel<BrBoneKeyFrame> channel(String name) {
        BrAnimationChannel<BrBoneKeyFrame> channel = channels.get(name);
        return channel != null ? channel : new BrAnimationChannel<>(name, ImmutableFloatTreeMap.empty());
    }

    @Nullable
    public Vector3f lerpRotation(MolangScope scope, float currentTick) {
        return sample(ROTATION, scope, currentTick);
    }

    @Nullable
    public Vector3f lerpPosition(MolangScope scope, float currentTick) {
        return sample(POSITION, scope, currentTick);
    }

    @Nullable
    public Vector3f lerpScale(MolangScope scope, float currentTick) {
        return sample(SCALE, scope, currentTick);
    }

    @Nullable
    public Vector3f sample(String channelName, MolangScope scope, float currentTick) {
        return BrBoneAnimationSampler.sample(compiledDefinition, channelName, scope, currentTick);
    }

    /**
     * 计算插值
     *
     * @param frames      frames
     * @param currentTick 当前 tick
     * @return 值
     */
    @Nullable
    public static Vector3f lerp(MolangScope scope,
                                ImmutableFloatTreeMap<BrBoneKeyFrame> frames,
                                float currentTick) {
        BrBoneKeyFrame before = frames.floorEntry(currentTick);
        BrBoneKeyFrame after = frames.higherEntry(currentTick);

        if (before != null && after != null) {
            var weight = EyeMath.getWeight(before.timestamp(), after.timestamp(), currentTick);

            if (before.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR && after.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR) {
                return before.linearLerp(scope, after, weight);
            } else if (before.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM || after.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM) {
                var beforePlus = frames.lowerEntry(before.timestamp());
                var afterPlus = frames.higherEntry(after.timestamp());

                if (beforePlus == null || afterPlus == null) {
                    return before.linearLerp(scope, after, weight);
                }
                return BrBoneKeyFrame.catmullromLerp(scope, beforePlus, before, after, afterPlus, weight);
            }
        } else if (before != null) {
            return before.get(before.timestamp() >= currentTick).eval(scope);
        } else if (after != null) {
            return after.get(after.timestamp() >= currentTick).eval(scope);
        }

        return null;
    }
}


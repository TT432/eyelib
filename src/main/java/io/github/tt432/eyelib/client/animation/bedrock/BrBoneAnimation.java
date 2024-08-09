package io.github.tt432.eyelib.client.animation.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.util.ImmutableFloatTreeMap;
import io.github.tt432.eyelib.util.math.EyeMath;
import org.joml.Vector3f;

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
        ImmutableFloatTreeMap<BrBoneKeyFrame> rotation,
        ImmutableFloatTreeMap<BrBoneKeyFrame> position,
        ImmutableFloatTreeMap<BrBoneKeyFrame> scale
) {
    private static final Codec<ImmutableFloatTreeMap<BrBoneKeyFrame>> KEY_FRAME_LIST_CODEC = Codec.withAlternative(
            ImmutableFloatTreeMap.dispatched(f -> BrBoneKeyFrame.Factory.CODEC.xmap(
                    factory -> factory.create(f),
                    BrBoneKeyFrame.Factory::from
            )),
            BrBoneKeyFrame.Factory.CODEC.xmap(f -> f.create(0), BrBoneKeyFrame.Factory::from)
                    .xmap(ImmutableFloatTreeMap::of, map -> map.getData().get(0))
    );

    public static final Codec<BrBoneAnimation> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            KEY_FRAME_LIST_CODEC.optionalFieldOf("rotation", ImmutableFloatTreeMap.empty()).forGetter(o -> o.rotation),
            KEY_FRAME_LIST_CODEC.optionalFieldOf("position", ImmutableFloatTreeMap.empty()).forGetter(o -> o.position),
            KEY_FRAME_LIST_CODEC.optionalFieldOf("scale", ImmutableFloatTreeMap.empty()).forGetter(o -> o.scale)
    ).apply(ins, BrBoneAnimation::new));

    public Vector3f lerpRotation(MolangScope scope, float currentTick) {
        return lerp(scope, rotation, currentTick);
    }

    public Vector3f lerpPosition(MolangScope scope, float currentTick) {
        return lerp(scope, position, currentTick);
    }

    public Vector3f lerpScale(MolangScope scope, float currentTick) {
        return lerp(scope, scale, currentTick);
    }

    private static final float epsilon = 1F / 1200F;

    /**
     * 计算插值
     *
     * @param frames      frames
     * @param currentTick 当前 tick
     * @return 值
     */
    public static Vector3f lerp(MolangScope scope,
                                ImmutableFloatTreeMap<BrBoneKeyFrame> frames,
                                float currentTick) {
        BrBoneKeyFrame before = frames.floorEntry(currentTick);
        BrBoneKeyFrame after = frames.higherEntry(currentTick);
        BrBoneKeyFrame result = null;

        boolean isBeforeTime = before != null && EyeMath.epsilon(before.timestamp(), currentTick, epsilon);
        boolean isAfterTime = after != null && EyeMath.epsilon(after.timestamp(), currentTick, epsilon);
        boolean onlyBefore = before != null && after == null;
        boolean onlyAfter = after != null && before == null;

        if (isBeforeTime || (!isAfterTime && onlyBefore)) {
            result = before;
        } else if (isAfterTime || onlyAfter) {
            result = after;
        } else if (after != null) {
            var weight = EyeMath.getWeight(before.timestamp(), after.timestamp(), currentTick);

            if (before.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR && after.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR) {
                return before.linearLerp(scope, after, weight);
            } else if (before.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM || after.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM) {
                var beforePlus = frames.lowerEntry(before.timestamp());
                var afterPlus = frames.higherEntry(after.timestamp());

                return BrBoneKeyFrame.catmullromLerp(scope, beforePlus, before, after, afterPlus, weight);
            }
        }

        if (result != null) {
            return result.get(result.timestamp() > currentTick || EyeMath.epsilon(result.timestamp(), currentTick, epsilon)).eval(scope);
        } else {
            return null;
        }
    }
}

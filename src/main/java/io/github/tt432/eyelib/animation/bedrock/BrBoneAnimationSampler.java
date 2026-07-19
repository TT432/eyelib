package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.util.collection.ImmutableFloatTreeMap;
import io.github.tt432.eyelib.util.math.EyeMath;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

/**
 * 骨骼关键帧采样器，对定义数据进行线性或 Catmull-Rom 插值。
 *
 * @author TT432
 */
public final class BrBoneAnimationSampler {
    private BrBoneAnimationSampler() {
    }

    @Nullable
    public static Vector3f sample(BrBoneAnimationDefinition definition, String channelName, MolangScope scope, float currentTick,
                                  float thisX, float thisY, float thisZ) {
        return lerp(scope, definition.channel(channelName).keyFrames(), currentTick, thisX, thisY, thisZ);
    }

    @Nullable
    public static Vector3f lerp(MolangScope scope,
                                ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames,
                                float currentTick,
                                float thisX, float thisY, float thisZ) {
        BrBoneKeyFrameDefinition before = frames.floorEntry(currentTick);
        BrBoneKeyFrameDefinition after = frames.higherEntry(currentTick);

        if (before != null && after != null) {
            var weight = EyeMath.getWeight(before.timestamp(), after.timestamp(), currentTick);

            if (before.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR && after.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR) {
                return BrBoneKeyFrame.linearLerp(scope, before, after, weight, thisX, thisY, thisZ);
            } else if (before.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM || after.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM) {
                var beforePlus = frames.lowerEntry(before.timestamp());
                var afterPlus = frames.higherEntry(after.timestamp());

                if (beforePlus == null || afterPlus == null) {
                    return BrBoneKeyFrame.linearLerp(scope, before, after, weight, thisX, thisY, thisZ);
                }
                return BrBoneKeyFrame.catmullromLerp(scope, beforePlus, before, after, afterPlus, weight, thisX, thisY, thisZ);
            }
        } else if (before != null) {
            return BrBoneKeyFrame.getValue(before, before.timestamp() >= currentTick).evalWithThis(scope, thisX, thisY, thisZ);
        } else if (after != null) {
            return BrBoneKeyFrame.getValue(after, after.timestamp() >= currentTick).evalWithThis(scope, thisX, thisY, thisZ);
        }

        return null;
    }
}
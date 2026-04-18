package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelib.util.ImmutableFloatTreeMap;
import io.github.tt432.eyelib.util.math.EyeMath;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

public final class BrBoneAnimationSampler {
    private BrBoneAnimationSampler() {
    }

    @Nullable
    public static Vector3f sample(BrBoneAnimationDefinition definition, String channelName, MolangScope scope, float currentTick) {
        return lerp(scope, definition.channel(channelName).keyFrames(), currentTick);
    }

    @Nullable
    public static Vector3f lerp(MolangScope scope,
                                ImmutableFloatTreeMap<BrBoneKeyFrameDefinition> frames,
                                float currentTick) {
        BrBoneKeyFrameDefinition before = frames.floorEntry(currentTick);
        BrBoneKeyFrameDefinition after = frames.higherEntry(currentTick);

        if (before != null && after != null) {
            var weight = EyeMath.getWeight(before.timestamp(), after.timestamp(), currentTick);

            if (before.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR && after.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR) {
                return BrBoneKeyFrame.linearLerp(scope, before, after, weight);
            } else if (before.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM || after.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM) {
                var beforePlus = frames.lowerEntry(before.timestamp());
                var afterPlus = frames.higherEntry(after.timestamp());

                if (beforePlus == null || afterPlus == null) {
                    return BrBoneKeyFrame.linearLerp(scope, before, after, weight);
                }
                return BrBoneKeyFrame.catmullromLerp(scope, beforePlus, before, after, afterPlus, weight);
            }
        } else if (before != null) {
            return BrBoneKeyFrame.getValue(before, before.timestamp() >= currentTick).eval(scope);
        } else if (after != null) {
            return BrBoneKeyFrame.getValue(after, after.timestamp() >= currentTick).eval(scope);
        }

        return null;
    }
}


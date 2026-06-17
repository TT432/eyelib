package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.animation.AnimationDefinition;
import io.github.tt432.eyelib.util.collection.ImmutableFloatTreeMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author TT432
 */
public record BrBoneAnimationDefinition(
        Map<String, BrAnimationChannel<BrBoneKeyFrameDefinition>> channels
) implements AnimationDefinition<BrBoneKeyFrameDefinition, BrAnimationChannel<BrBoneKeyFrameDefinition>> {
    public BrBoneAnimationDefinition {
        channels = Collections.unmodifiableMap(new LinkedHashMap<>(channels));
    }

    @Override
    public BrAnimationChannel<BrBoneKeyFrameDefinition> emptyChannel(String name) {
        return new BrAnimationChannel<>(name, ImmutableFloatTreeMap.empty());
    }
}
package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibanimation.AnimationDefinition;
import io.github.tt432.eyelibutil.collection.ImmutableFloatTreeMap;
import org.jspecify.annotations.NullMarked;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author TT432
 */
@NullMarked
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
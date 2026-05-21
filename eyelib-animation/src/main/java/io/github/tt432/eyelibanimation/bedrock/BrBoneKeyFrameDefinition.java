package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibanimation.AnimationKeyframeDefinition;
import io.github.tt432.eyelibmolang.MolangValue3;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public record BrBoneKeyFrameDefinition(
        float timestamp,
        List<MolangValue3> dataPoints,
        BrBoneKeyFrame.LerpMode lerpMode
) implements AnimationKeyframeDefinition {
    public BrBoneKeyFrameDefinition {
        dataPoints = List.copyOf(dataPoints);
    }
}
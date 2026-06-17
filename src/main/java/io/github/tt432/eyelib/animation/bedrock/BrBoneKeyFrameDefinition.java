package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.animation.AnimationKeyframeDefinition;
import io.github.tt432.eyelib.molang.MolangValue3;
import java.util.List;

/**
 * @author TT432
 */
public record BrBoneKeyFrameDefinition(
        float timestamp,
        List<MolangValue3> dataPoints,
        BrBoneKeyFrame.LerpMode lerpMode
) implements AnimationKeyframeDefinition {
    public BrBoneKeyFrameDefinition {
        dataPoints = List.copyOf(dataPoints);
    }
}
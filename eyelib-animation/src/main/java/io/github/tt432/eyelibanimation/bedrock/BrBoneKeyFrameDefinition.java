package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibanimation.AnimationKeyframeDefinition;
import io.github.tt432.eyelibmolang.MolangValue3;

import java.util.List;

public record BrBoneKeyFrameDefinition(
        float timestamp,
        List<MolangValue3> dataPoints,
        BrBoneKeyFrame.LerpMode lerpMode
) implements AnimationKeyframeDefinition {
    public BrBoneKeyFrameDefinition {
        dataPoints = List.copyOf(dataPoints);
    }
}

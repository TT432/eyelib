package io.github.tt432.eyelibanimation.bedrock;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public record BrAnimationEntryBoneTrackDefinition(
        BrAnimationEntryTrackName name,
        Int2ObjectMap<BrBoneAnimation> bones
) implements BrAnimationEntryTrackDefinition {
}

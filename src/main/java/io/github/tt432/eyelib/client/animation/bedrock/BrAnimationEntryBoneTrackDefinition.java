package io.github.tt432.eyelib.client.animation.bedrock;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public record BrAnimationEntryBoneTrackDefinition(
        BrAnimationEntryTrackName name,
        Int2ObjectMap<BrBoneAnimation> bones
) implements BrAnimationEntryTrackDefinition {
}

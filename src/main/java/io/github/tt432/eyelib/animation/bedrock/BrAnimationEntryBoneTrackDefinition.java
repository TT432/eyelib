package io.github.tt432.eyelib.animation.bedrock;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
/**
 * @author TT432
 */
public record BrAnimationEntryBoneTrackDefinition(
        BrAnimationEntryTrackName name,
        Int2ObjectMap<BrBoneAnimation> bones
) implements BrAnimationEntryTrackDefinition {
}
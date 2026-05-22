package io.github.tt432.eyelibanimation.bedrock;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public record BrAnimationEntryBoneTrackDefinition(
        BrAnimationEntryTrackName name,
        Int2ObjectMap<BrBoneAnimation> bones
) implements BrAnimationEntryTrackDefinition {
}
package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibimporter.animation.NamedTrackDefinition;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
/** @author TT432 */
public sealed interface BrAnimationEntryTrackDefinition
        extends NamedTrackDefinition<BrAnimationEntryTrackName>
        permits BrAnimationEntryEffectTrackDefinition, BrAnimationEntryBoneTrackDefinition {
    BrAnimationEntryTrackName name();
}
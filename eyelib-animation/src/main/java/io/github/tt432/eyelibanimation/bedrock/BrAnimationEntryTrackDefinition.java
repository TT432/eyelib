package io.github.tt432.eyelibanimation.bedrock;

import io.github.tt432.eyelibimporter.animation.NamedTrackDefinition;
import org.jspecify.annotations.NullMarked;

/**
 * 动画条目具名轨道的密封接口，区分效果轨道和骨骼轨道。
 *
 * @author TT432
 */
@NullMarked
public sealed interface BrAnimationEntryTrackDefinition
        extends NamedTrackDefinition<BrAnimationEntryTrackName>
        permits BrAnimationEntryEffectTrackDefinition, BrAnimationEntryBoneTrackDefinition {
    BrAnimationEntryTrackName name();
}
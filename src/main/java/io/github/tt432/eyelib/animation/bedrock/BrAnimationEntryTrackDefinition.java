package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.importer.animation.NamedTrackDefinition;
/**
 * 动画条目具名轨道的密封接口，区分效果轨道和骨骼轨道。
 *
 * @author TT432
 */
public sealed interface BrAnimationEntryTrackDefinition
        extends NamedTrackDefinition<BrAnimationEntryTrackName>
        permits BrAnimationEntryEffectTrackDefinition, BrAnimationEntryBoneTrackDefinition {
    BrAnimationEntryTrackName name();
}
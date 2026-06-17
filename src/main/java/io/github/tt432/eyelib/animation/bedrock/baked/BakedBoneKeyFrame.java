package io.github.tt432.eyelib.animation.bedrock.baked;

import io.github.tt432.eyelib.importer.animation.bedrock.BrBoneKeyFrameSchema;
import io.github.tt432.eyelib.molang.MolangValue3;
import java.util.List;

/**
 * 烘焙后的骨骼关键帧数据（纯数据，无 Minecraft 依赖）。
 *
 * @author TT432
 */
public record BakedBoneKeyFrame(
        float timestamp,
        List<MolangValue3> dataPoints,
        BakedBoneKeyFrame.BakedLerpMode lerpMode
) {
    public enum BakedLerpMode {
        LINEAR,
        CATMULLROM;

        public static BakedLerpMode fromSchemaName(String name) {
            return switch (name.toLowerCase()) {
                case "catmullrom" -> CATMULLROM;
                default -> LINEAR;
            };
        }
    }

    static BakedBoneKeyFrame fromSchema(float timestamp, BrBoneKeyFrameSchema schema) {
        return new BakedBoneKeyFrame(
                timestamp,
                schema.dataPoints(),
                BakedLerpMode.fromSchemaName(schema.lerpMode().name())
        );
    }
}
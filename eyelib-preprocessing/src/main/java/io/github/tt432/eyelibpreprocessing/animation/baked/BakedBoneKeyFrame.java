package io.github.tt432.eyelibpreprocessing.animation.baked;

import io.github.tt432.eyelibimporter.animation.bedrock.BrBoneKeyFrameSchema;
import io.github.tt432.eyelibmolang.MolangValue3;

import java.util.List;

/**
 * Intermediate Representation: baked bone keyframe data.
 * Pure data — no Minecraft/Forge dependencies, no MolangScope.
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

        /**
         * Factory mapping from schema enum name — NOT StringRepresentable.
         */
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

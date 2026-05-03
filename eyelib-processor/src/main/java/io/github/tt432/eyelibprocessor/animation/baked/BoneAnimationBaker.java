package io.github.tt432.eyelibprocessor.animation.baked;

import io.github.tt432.eyelibimporter.animation.bedrock.BrBoneKeyFrameSchema;

import java.util.TreeMap;

/**
 * Converts importer schema types into intermediate baked data types.
 * Pure conversion — no Minecraft/Forge types, no root runtime types.
 *
 * @author TT432
 */
public final class BoneAnimationBaker {

    private BoneAnimationBaker() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static BakedBoneKeyFrame bakeKeyFrame(float timestamp, BrBoneKeyFrameSchema schema) {
        return BakedBoneKeyFrame.fromSchema(timestamp, schema);
    }

    /**
     * Convert a single channel (rotation/position/scale) from schema to baked keyframes.
     *
     * @param channel a TreeMap from the schema (e.g. {@code schema.rotation()})
     * @return baked keyframes keyed by timestamp
     */
    public static TreeMap<Float, BakedBoneKeyFrame> bakeBoneAnimation(TreeMap<Float, BrBoneKeyFrameSchema> channel) {
        TreeMap<Float, BakedBoneKeyFrame> result = new TreeMap<>(Float::compare);
        channel.forEach((key, value) -> result.put(key, bakeKeyFrame(key, value)));
        return result;
    }
}

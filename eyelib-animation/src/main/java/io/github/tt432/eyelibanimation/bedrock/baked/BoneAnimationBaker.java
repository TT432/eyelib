package io.github.tt432.eyelibanimation.bedrock.baked;

import io.github.tt432.eyelibimporter.animation.bedrock.BrBoneKeyFrameSchema;
import org.jspecify.annotations.NullMarked;

import java.util.TreeMap;

/**
 * 将 importer schema 类型转换为中间烘焙数据类型（纯转换，无 Minecraft 依赖）。
 *
 * @author TT432
 */
@NullMarked
public final class BoneAnimationBaker {

    private BoneAnimationBaker() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static BakedBoneKeyFrame bakeKeyFrame(float timestamp, BrBoneKeyFrameSchema schema) {
        return BakedBoneKeyFrame.fromSchema(timestamp, schema);
    }

    public static TreeMap<Float, BakedBoneKeyFrame> bakeBoneAnimation(TreeMap<Float, BrBoneKeyFrameSchema> channel) {
        TreeMap<Float, BakedBoneKeyFrame> result = new TreeMap<>(Float::compare);
        channel.forEach((key, value) -> result.put(key, bakeKeyFrame(key, value)));
        return result;
    }
}
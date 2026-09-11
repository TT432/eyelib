package io.github.tt432.eyelib.importer.model.bbmodel;

import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelib.importer.animation.bedrock.BrBoneAnimationSchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrBoneKeyFrameSchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrEffectsKeyFrame;
import io.github.tt432.eyelib.importer.animation.bedrock.BrLoopType;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * bbmodel 内嵌动画 → bedrock 动画 schema 转换。
 * 坐标约定：Blockbench 编辑器空间转 bedrock 实体空间——position 取反 x、rotation 取反 x/y、
 * scale 不变；只有编译期常量参与取反，molang 表达式原样透传。
 *
 * @author TT432
 */
public final class BbModelAnimations {
    private static final MolangValue DEFAULT_ANIM_TIME_UPDATE = new MolangValue("query.anim_time + query.delta_time");

    private BbModelAnimations() {
    }

    /**
     * 转换模型内嵌的全部动画，动画 id 为 {@code <namespace>.<动画名>}。
     */
    public static BrAnimationSet toAnimationSet(BBModel model, String namespace) {
        Map<String, BrAnimationEntrySchema> animations = new LinkedHashMap<>();
        for (BbModelAnimation animation : model.animations()) {
            animations.put(namespace + "." + animation.name(), toEntrySchema(animation));
        }
        return new BrAnimationSet(animations);
    }

    public static BrAnimationEntrySchema toEntrySchema(BbModelAnimation animation) {
        Map<String, BrBoneAnimationSchema> bones = new LinkedHashMap<>();
        for (BbModelAnimation.Animator animator : animation.animators().values()) {
            TreeMap<Float, BrBoneKeyFrameSchema> position = new TreeMap<>();
            TreeMap<Float, BrBoneKeyFrameSchema> rotation = new TreeMap<>();
            TreeMap<Float, BrBoneKeyFrameSchema> scale = new TreeMap<>();
            for (BbModelAnimation.Keyframe keyframe : animator.keyframes()) {
                if (keyframe.dataPoints().isEmpty()) {
                    continue;
                }
                TreeMap<Float, BrBoneKeyFrameSchema> channel = switch (keyframe.channel()) {
                    case "position" -> position;
                    case "rotation" -> rotation;
                    case "scale" -> scale;
                    default -> null;
                };
                if (channel == null) {
                    continue;
                }
                BrBoneKeyFrameSchema.LerpMode lerpMode = keyframe.interpolation().equals("catmullrom")
                        ? BrBoneKeyFrameSchema.LerpMode.CATMULLROM
                        : BrBoneKeyFrameSchema.LerpMode.LINEAR;
                channel.put(keyframe.time(), new BrBoneKeyFrameSchema(
                        List.of(convertPoint(keyframe.dataPoints().get(0), keyframe.channel())), lerpMode));
            }
            BrBoneAnimationSchema existing = bones.get(animator.name());
            if (existing != null) {
                existing.rotation().putAll(rotation);
                existing.position().putAll(position);
                existing.scale().putAll(scale);
            } else if (!position.isEmpty() || !rotation.isEmpty() || !scale.isEmpty()) {
                bones.put(animator.name(), new BrBoneAnimationSchema(rotation, position, scale));
            }
        }
        return new BrAnimationEntrySchema(
                loopType(animation.loop()),
                animation.length(),
                animation.override(),
                molangOrDefault(animation.animTimeUpdate(), DEFAULT_ANIM_TIME_UPDATE),
                molangOrDefault(animation.blendWeight(), MolangValue.ONE),
                molangOrDefault(animation.startDelay(), MolangValue.ZERO),
                molangOrDefault(animation.loopDelay(), MolangValue.ZERO),
                emptyEffects(),
                emptyEffects(),
                new TreeMap<>(Comparator.comparingDouble(Float::doubleValue)),
                bones
        );
    }

    private static BrLoopType loopType(String loop) {
        return switch (loop) {
            case "loop" -> BrLoopType.LOOP;
            case "hold" -> BrLoopType.HOLD_ON_LAST_FRAME;
            default -> BrLoopType.ONCE;
        };
    }

    private static MolangValue molangOrDefault(String raw, MolangValue fallback) {
        return raw.isEmpty() ? fallback : new MolangValue(raw);
    }

    private static TreeMap<Float, List<BrEffectsKeyFrame>> emptyEffects() {
        return new TreeMap<>(Comparator.comparingDouble(Float::doubleValue));
    }

    private static MolangValue3 convertPoint(BbModelAnimation.DataPoint point, String channel) {
        boolean negateX = channel.equals("position") || channel.equals("rotation");
        boolean negateY = channel.equals("rotation");
        return new MolangValue3(axis(point.x(), negateX), axis(point.y(), negateY), axis(point.z(), false));
    }

    /** 数值（含数字字符串）取反后取常量；其余按 molang 表达式编译透传。 */
    private static MolangValue axis(String raw, boolean negate) {
        try {
            float value = Float.parseFloat(raw);
            return MolangValue.getConstant(negate ? -value : value);
        } catch (NumberFormatException ignored) {
            return new MolangValue(raw);
        }
    }
}

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
 * 坐标约定（2026-10-06 以 web.blockbench.net 实测校准）：bbmodel 内嵌动画值与 bedrock
 * .animation.json 同为剪辑空间——Blockbench 显示 bedrock 动画时旋转取反 x/y、位置取反 x
 * （实测：待机躯体 UpBody 显示 (-11.17,-48.92,2.56)° = flip_xy(文件值)；bone24 显示
 * (14.53,16.13,17.24)° = rest(37.4,71,39.1) + flip(关键帧)，即关键帧叠加在 rest 上而非替换）。
 * 本库注册表统一剪辑空间，渲染期由模型 flipAnimation=true 翻转一次即得 Blockbench 显示姿态，
 * 故转换期必须原样透传：不取反、不做 bind 相对化。
 * 历史教训：转换期取反曾与渲染期翻转构成双翻转（净恒等，实机姿态镜像）；0a5eb246 的
 * bind 相对化亦被实测定伪。验证管线：Blockbench web 抽 mesh.matrixWorld 与游戏渲染顶点
 * 逐 cube 对比（修复后 0.00006/294 cube）。
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
        // 剪辑空间原样透传（见类注释）；数值字符串规范化为常量，molang 表达式不动。
        return new MolangValue3(axis(point.x()), axis(point.y()), axis(point.z()));
    }

    /** 数值（含数字字符串）取常量；molang 表达式透传。 */
    private static MolangValue axis(String raw) {
        try {
            return MolangValue.getConstant(Float.parseFloat(raw));
        } catch (NumberFormatException ignored) {
            return new MolangValue(raw);
        }
    }
}

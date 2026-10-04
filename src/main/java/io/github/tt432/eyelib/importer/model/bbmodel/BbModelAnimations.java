package io.github.tt432.eyelib.importer.model.bbmodel;

import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationEntrySchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrAnimationSet;
import io.github.tt432.eyelib.importer.animation.bedrock.BrBoneAnimationSchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrBoneKeyFrameSchema;
import io.github.tt432.eyelib.importer.animation.bedrock.BrEffectsKeyFrame;
import io.github.tt432.eyelib.importer.animation.bedrock.BrLoopType;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * bbmodel 内嵌动画 → bedrock 动画 schema 转换。
 * 坐标约定：bbmodel 存储的是 Blockbench UI/显示空间的值——Blockbench 基岩动画导出器
 * （blockbench 源码 js/formats/bedrock/bedrock_animation.js {@code getKeyframeDataPoints}）
 * 在导出 .animation.json 时对 position.x 与 rotation.x/y 无条件 invertMolang，几何导出同样
 * 镜像 X。因此 bbmodel 内嵌值与"显示空间"同构，而本库注册表内所有剪辑统一为基岩剪辑空间
 * （渲染期由模型 flipAnimation=true 补偿一次得到编辑器姿态，与基岩 .animation.json 一致）。
 * 故转换期必须取反：position 取反 x、rotation 取反 x/y、scale 不变；数字字符串同数值取反；
 * molang 表达式包裹 {@code -(...)} 取反（eyelib molang 支持一元负号）。
 * （9754bbea 的"UI 值透传 + 渲染期补偿一次即编辑器姿态"结论已被废弃：其前提
 * "Blockbench 显示姿态 = flip(UI 值)"与 Blockbench 导出源码矛盾，实机表现为姿态镜像。）
 * <p>
 * 旋转关键帧的绝对/相对语义：Blockbench/基岩中旋转关键帧是<strong>绝对值</strong>
 * （替换骨骼 rest 旋转），而运行时应用为 bind + Σ剪辑贡献（见
 * {@code ModelRuntimeData#rotation(Model.Bone)}）。几何导入保留组的 rest 旋转
 * （{@code ImportedModelData} 不镜像、仅度转弧度），若直接透传绝对关键帧，
 * rest 非零的骨骼会被双重旋转（实测掠夺者弩弓 bone24/28 rest ±71°Y → 弩指向偏约 90°）。
 * 故转换期把绝对关键帧减去同文件内骨骼 rest 旋转，化为 bind 相对偏移：
 * 最终 = bind + (keyframe − bind) = keyframe，多剪辑并发 = bind + Σ(kf_i − bind)，
 * 与基岩并发叠加语义一致。position bind 恒零向量、scale bind 恒 1（见
 * {@code ImportedModelBuilder#buildBone}），无需同样处理。
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
        Map<String, Vector3f> bindRotations = bindRotations(model);
        Map<String, BrAnimationEntrySchema> animations = new LinkedHashMap<>();
        for (BbModelAnimation animation : model.animations()) {
            animations.put(namespace + "." + animation.name(), toEntrySchema(animation, bindRotations));
        }
        return new BrAnimationSet(animations);
    }

    /**
     * 收集模型全部骨骼的 rest 旋转（显示空间，度），键为骨骼名。
     * 与 {@code ImportedModelData#processOutlinerEntry} 相同的组解析顺序
     * （顶层 groups 表按 uuid 优先，其次 outliner 内联 group）。
     */
    public static Map<String, Vector3f> bindRotations(BBModel model) {
        Map<String, Group> groupMap = new HashMap<>();
        for (Group group : model.groups()) {
            groupMap.put(group.uuid(), group);
        }
        Map<String, Vector3f> result = new HashMap<>();
        for (Outliner.CubeOrOutliner entry : model.outliner()) {
            collectBindRotations(entry.outliner(), groupMap, result);
        }
        return result;
    }

    private static void collectBindRotations(@Nullable Outliner outliner, Map<String, Group> groupMap,
                                             Map<String, Vector3f> result) {
        if (outliner == null) {
            return;
        }
        Group group = groupMap.get(outliner.uuid());
        if (group == null && outliner.group().isPresent()) {
            group = outliner.group().get();
        }
        if (group != null && group.rotation() != null) {
            Vector3f rotation = group.rotation();
            if (rotation.x() != 0 || rotation.y() != 0 || rotation.z() != 0) {
                result.put(group.name(), rotation);
            }
        }
        for (Outliner child : outliner.children()) {
            collectBindRotations(child, groupMap, result);
        }
    }

    public static BrAnimationEntrySchema toEntrySchema(BbModelAnimation animation, Map<String, Vector3f> bindRotations) {
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
                        List.of(convertPoint(keyframe.dataPoints().get(0), keyframe.channel(),
                                bindRotations.get(animator.name()))), lerpMode));
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

    private static MolangValue3 convertPoint(BbModelAnimation.DataPoint point, String channel,
                                             @Nullable Vector3f bindRotation) {
        boolean negateX = channel.equals("position") || channel.equals("rotation");
        boolean negateY = channel.equals("rotation");
        boolean isRotation = channel.equals("rotation");
        // 仅 rotation 通道化为 bind 相对偏移（见类注释）；position bind 恒零、scale bind 恒 1
        float subX = isRotation && bindRotation != null ? bindRotation.x() : 0;
        float subY = isRotation && bindRotation != null ? bindRotation.y() : 0;
        float subZ = isRotation && bindRotation != null ? bindRotation.z() : 0;
        return new MolangValue3(axis(point.x(), negateX, subX), axis(point.y(), negateY, subY), axis(point.z(), false, subZ));
    }

    /** 数值（含数字字符串）先减 bind 偏移、取反通道再取反后取常量；molang 表达式先包 {@code (raw) - (sub)}，取反通道再包 {@code -(...)}，其余透传。 */
    private static MolangValue axis(String raw, boolean negate, float subtract) {
        try {
            float value = Float.parseFloat(raw) - subtract;
            return MolangValue.getConstant(negate ? -value : value);
        } catch (NumberFormatException ignored) {
            String base = subtract != 0 && !raw.isEmpty() ? "(" + raw + ") - (" + subtract + ")" : raw;
            return new MolangValue(negate && !base.isEmpty() ? "-(" + base + ")" : base);
        }
    }
}

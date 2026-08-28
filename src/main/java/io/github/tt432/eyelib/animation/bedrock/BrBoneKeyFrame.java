package io.github.tt432.eyelib.animation.bedrock;

import io.github.tt432.eyelib.importer.animation.bedrock.BrBoneKeyFrameSchema;
import io.github.tt432.eyelib.animation.bedrock.baked.BakedBoneKeyFrame;
import io.github.tt432.eyelib.animation.bedrock.baked.BoneAnimationBaker;


import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.util.codec.ChinExtraCodecs;
import io.github.tt432.eyelib.util.codec.CodecHelper;
import io.github.tt432.eyelib.util.collection.ListAccessors;
import io.github.tt432.eyelib.util.math.Curves;
import io.github.tt432.eyelib.util.math.EyeMath;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author TT432
 */
public record BrBoneKeyFrame(
        float timestamp,
        List<MolangValue3> dataPoints,
        BrBoneKeyFrame.LerpMode lerpMode,
        BrBoneKeyFrameDefinition compiledDefinition
) {
    public BrBoneKeyFrame {
        dataPoints = List.copyOf(dataPoints);
        compiledDefinition = compiledDefinition != null
                ? compiledDefinition
                : new BrBoneKeyFrameDefinition(timestamp, dataPoints, lerpMode);
    }

    public BrBoneKeyFrame(float timestamp, List<MolangValue3> dataPoints, BrBoneKeyFrame.LerpMode lerpMode) {
        this(timestamp, dataPoints, lerpMode, new BrBoneKeyFrameDefinition(timestamp, dataPoints, lerpMode));
    }

    public BrBoneKeyFrameDefinition definition() {
        return compiledDefinition;
    }

    public static BrBoneKeyFrame fromSchema(float timestamp, BrBoneKeyFrameSchema schema) {
        BakedBoneKeyFrame baked = BoneAnimationBaker.bakeKeyFrame(timestamp, schema);
        return new BrBoneKeyFrame(baked.timestamp(), baked.dataPoints(), LerpMode.valueOf(baked.lerpMode().name()));
    }

    public enum LerpMode implements PortStringRepresentable {
        LINEAR,
        CATMULLROM;
        public static final Codec<LerpMode> CODEC = PortStringRepresentable.fromEnum(LerpMode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    /**
     * 平滑插值
     *
     * @param beforePlus p0
     * @param before     p1
     * @param after      p2
     * @param afterPlus  p3
     * @param weight     weight
     * @return 值
     */
    public static Vector3f catmullromLerp(MolangScope scope,
                                          BrBoneKeyFrame beforePlus,
                                          BrBoneKeyFrame before,
                                          BrBoneKeyFrame after,
                                          BrBoneKeyFrame afterPlus,
                                          float weight,
                                          float thisX, float thisY, float thisZ) {
        return catmullromLerp(scope, beforePlus.definition(), before.definition(), after.definition(), afterPlus.definition(), weight, thisX, thisY, thisZ);
    }

    public static Vector3f catmullromLerp(MolangScope scope,
                                          BrBoneKeyFrameDefinition beforePlus,
                                          BrBoneKeyFrameDefinition before,
                                          BrBoneKeyFrameDefinition after,
                                          BrBoneKeyFrameDefinition afterPlus,
                                          float weight,
                                          float thisX, float thisY, float thisZ) {
        boolean firstPointPredicate = beforePlus != null && before.dataPoints().size() == 1;
        boolean lastPointPredicate = afterPlus != null && after.dataPoints().size() == 1;
        weight = weight + (beforePlus != null ? 1 : 0);

        var xArray = setupCurvePoints(scope, beforePlus, before, after, afterPlus,
                firstPointPredicate, lastPointPredicate, new AxisSampler(0), thisX);

        var yArray = setupCurvePoints(scope, beforePlus, before, after, afterPlus,
                firstPointPredicate, lastPointPredicate, new AxisSampler(1), thisY);

        var zArray = setupCurvePoints(scope, beforePlus, before, after, afterPlus,
                firstPointPredicate, lastPointPredicate, new AxisSampler(2), thisZ);

        return new Vector3f(
                Curves.lerpSplineCurve(xArray, weight / (xArray.size() - 1)),
                Curves.lerpSplineCurve(yArray, weight / (yArray.size() - 1)),
                Curves.lerpSplineCurve(zArray, weight / (zArray.size() - 1))
        );
    }

    /** 轴采样器：按轴索引从 MolangValue3（molang 求值）或预计算常量向量（零求值）取值。 */
    record AxisSampler(int axis) {
        float apply(MolangValue3 mv3, MolangScope scope) {
            return switch (axis) {
                case 0 -> mv3.getX(scope);
                case 1 -> mv3.getY(scope);
                default -> mv3.getZ(scope);
            };
        }

        float ofConstant(MolangValue3 mv3) {
            return switch (axis) {
                case 0 -> mv3.constantAxis(0);
                case 1 -> mv3.constantAxis(1);
                default -> mv3.constantAxis(2);
            };
        }
    }

    /**
     * 轴取值：三轴全常量时走构造期预计算向量（常量不读 scope、不写 temp、不抛异常，
     * 见 {@link MolangValue#isConstant()} 契约），跳过逐轴 clearTempVariables+dispatch 链。
     */
    private static float axisValue(AxisSampler sampler, MolangValue3 mv3, MolangScope scope) {
        if (BrBoneAnimationSampler.SAMPLE_OPT) {
            if (mv3.allAxesConstant()) {
                return sampler.ofConstant(mv3);
            }
        }
        return sampler.apply(mv3, scope);
    }

    private static ArrayList<Vector2f> setupCurvePoints(MolangScope scope,
                                                        BrBoneKeyFrame beforePlus, BrBoneKeyFrame before,
                                                        BrBoneKeyFrame after, BrBoneKeyFrame afterPlus,
                                                        boolean firstPointPredicate, boolean lastPointPredicate,
                                                        AxisSampler function, float thisValue) {
        ArrayList<Vector2f> points = new ArrayList<>();

        if (firstPointPredicate) {
            scope.setThis(thisValue);
            points.add(new Vector2f(beforePlus.timestamp(), axisValue(function, beforePlus.getPost(), scope)));
        }

        scope.setThis(thisValue);
        points.add(new Vector2f(before.timestamp(), axisValue(function, before.getPost(), scope)));

        scope.setThis(thisValue);
        points.add(new Vector2f(after.timestamp(), axisValue(function, after.getPre(), scope)));

        if (lastPointPredicate) {
            scope.setThis(thisValue);
            points.add(new Vector2f(afterPlus.timestamp(), axisValue(function, afterPlus.getPre(), scope)));
        }

        return points;
    }

    private static ArrayList<Vector2f> setupCurvePoints(MolangScope scope,
                                                        BrBoneKeyFrameDefinition beforePlus, BrBoneKeyFrameDefinition before,
                                                        BrBoneKeyFrameDefinition after, BrBoneKeyFrameDefinition afterPlus,
                                                        boolean firstPointPredicate, boolean lastPointPredicate,
                                                        AxisSampler function, float thisValue) {
        ArrayList<Vector2f> points = new ArrayList<>();

        if (firstPointPredicate) {
            scope.setThis(thisValue);
            points.add(new Vector2f(beforePlus.timestamp(), axisValue(function, getValue(beforePlus, false), scope)));
        }

        scope.setThis(thisValue);
        points.add(new Vector2f(before.timestamp(), axisValue(function, getValue(before, false), scope)));

        scope.setThis(thisValue);
        points.add(new Vector2f(after.timestamp(), axisValue(function, getValue(after, true), scope)));

        if (lastPointPredicate) {
            scope.setThis(thisValue);
            points.add(new Vector2f(afterPlus.timestamp(), axisValue(function, getValue(afterPlus, true), scope)));
        }

        return points;
    }

    /**
     * 线性插值
     *
     * @param other  另一个关键帧
     * @param weight 权重
     * @return 值
     */
    public Vector3f linearLerp(MolangScope scope, BrBoneKeyFrame other, float weight,
                               float thisX, float thisY, float thisZ) {
        return linearLerp(scope, definition(), other.definition(), weight, thisX, thisY, thisZ);
    }

    public static Vector3f linearLerp(MolangScope scope, BrBoneKeyFrameDefinition current,
                                      BrBoneKeyFrameDefinition other, float weight,
                                      float thisX, float thisY, float thisZ) {
        var am3 = current.dataPoints().size() > 1 && current.timestamp() < other.timestamp() ? getValue(current, false) : getValue(current, true);
        var bm3 = other.dataPoints().size() > 1 && current.timestamp() > other.timestamp() ? getValue(other, false) : getValue(other, true);

        if (BrBoneAnimationSampler.SAMPLE_OPT) {
            if (am3.allAxesConstant() && bm3.allAxesConstant()) {
                // setThis 保留：维持 this 残态语义（常量不读 this，最终落点 this=thisZ 不变）
                scope.setThis(thisX);
                scope.setThis(thisY);
                scope.setThis(thisZ);
                return new Vector3f(
                        EyeMath.lerp(am3.constantAxis(0), bm3.constantAxis(0), weight),
                        EyeMath.lerp(am3.constantAxis(1), bm3.constantAxis(1), weight),
                        EyeMath.lerp(am3.constantAxis(2), bm3.constantAxis(2), weight)
                );
            }
        }

        scope.setThis(thisX);
        float ax = am3.getX(scope);
        float bx = bm3.getX(scope);

        scope.setThis(thisY);
        float ay = am3.getY(scope);
        float by = bm3.getY(scope);

        scope.setThis(thisZ);
        float az = am3.getZ(scope);
        float bz = bm3.getZ(scope);

        return new Vector3f(
                EyeMath.lerp(ax, bx, weight),
                EyeMath.lerp(ay, by, weight),
                EyeMath.lerp(az, bz, weight)
        );
    }

    public MolangValue3 get(boolean isPre) {
        return isPre ? getPre() : getPost();
    }

    public static MolangValue3 getValue(BrBoneKeyFrameDefinition keyFrame, boolean isPre) {
        return isPre ? ListAccessors.first(keyFrame.dataPoints()) : ListAccessors.last(keyFrame.dataPoints());
    }

    public MolangValue3 getPre() {
        return ListAccessors.first(dataPoints);
    }

    public MolangValue3 getPost() {
        return ListAccessors.last(dataPoints);
    }

    public record Factory(
            List<MolangValue3> dataPoints,
            LerpMode lerpMode
    ) {
        public static final Codec<Factory> CODEC;

        static {
            Codec<Factory> sourceCodec = RecordCodecBuilder.create(ins -> ins.group(
                    LerpMode.CODEC.optionalFieldOf("lerp_mode", LerpMode.LINEAR).forGetter(Factory::lerpMode),
                    MolangValue3.CODEC.optionalFieldOf("pre").forGetter(f -> Optional.of(ListAccessors.first(f.dataPoints()))),
                    MolangValue3.CODEC.optionalFieldOf("post").forGetter(f -> f.dataPoints().size() < 2 ? Optional.empty() : Optional.of(ListAccessors.last(f.dataPoints())))
            ).apply(ins, (mode, pre, post) -> {
                if (pre.isPresent() && post.isEmpty()) {
                    return new Factory(List.of(pre.get()), mode);
                } else if (post.isPresent() && (pre.isEmpty() || mode == LerpMode.CATMULLROM)) {
                    return new Factory(List.of(post.get()), mode);
                } else {
                    var builder = ImmutableList.<MolangValue3>builder();
                    pre.ifPresent(builder::add);
                    post.ifPresent(builder::add);
                    return new Factory(builder.build(), mode);
                }
            }));
            CODEC = CodecHelper.withAlternative(
                    CodecHelper.withAlternative(
                            MolangValue3.CODEC,
                            MolangValue.CODEC.xmap(mv -> new MolangValue3(mv, mv, mv), MolangValue3::x)
                    ).xmap(m3 -> new Factory(List.of(m3), LerpMode.LINEAR), f -> ListAccessors.first(f.dataPoints())),
                    ChinExtraCodecs.check(sourceCodec, f1 -> f1.dataPoints.isEmpty()
                            ? DataResult.error(() -> "BoneKeyFrame need pre or post.")
                            : DataResult.success(f1))
            );
        }

        public static Factory from(BrBoneKeyFrame keyFrame) {
            return new Factory(keyFrame.dataPoints, keyFrame.lerpMode);
        }

        public BrBoneKeyFrame create(float timestamp) {
            return new BrBoneKeyFrame(timestamp, dataPoints, lerpMode);
        }
    }
}
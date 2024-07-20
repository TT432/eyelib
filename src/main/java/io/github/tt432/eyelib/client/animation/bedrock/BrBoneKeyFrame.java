package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangValue3;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import io.github.tt432.eyelib.util.math.Curves;
import io.github.tt432.eyelib.util.math.EyeMath;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
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
        BrBoneKeyFrame.LerpMode lerpMode
) {
    public enum LerpMode implements StringRepresentable {
        LINEAR,
        CATMULLROM;
        public static final Codec<LerpMode> CODEC = StringRepresentable.fromEnum(LerpMode::values);

        @Override
        @NotNull
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    private static final List<Vector2f> catmullromArray = new ArrayList<>();
    private static final Vector2f cTempP1 = new Vector2f();
    private static final Vector2f cTempP2 = new Vector2f();
    private static final Vector2f cTempP3 = new Vector2f();
    private static final Vector2f cTempP4 = new Vector2f();

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
                                          float weight) {
        boolean firstPointPredicate = beforePlus != null && before.dataPoints.size() == 1;
        boolean lastPointPredicate = afterPlus != null && after.dataPoints.size() == 1;
        weight = weight + (beforePlus != null ? 1 : 0);

        var xArray = setupCurvePoints(scope, beforePlus, before, after, afterPlus,
                firstPointPredicate, lastPointPredicate, MolangValue3::getX);

        var yArray = setupCurvePoints(scope, beforePlus, before, after, afterPlus,
                firstPointPredicate, lastPointPredicate, MolangValue3::getY);

        var zArray = setupCurvePoints(scope, beforePlus, before, after, afterPlus,
                firstPointPredicate, lastPointPredicate, MolangValue3::getZ);

        return new Vector3f(
                Curves.lerpSplineCurve(xArray, weight / (xArray.size() - 1)),
                Curves.lerpSplineCurve(yArray, weight / (yArray.size() - 1)),
                Curves.lerpSplineCurve(zArray, weight / (zArray.size() - 1))
        );
    }

    @FunctionalInterface
    interface MolangValue3AxisFunction {
        float apply(MolangValue3 mv3, MolangScope scope);
    }

    private static ArrayList<Vector2f> setupCurvePoints(MolangScope scope,
                                                        BrBoneKeyFrame beforePlus, BrBoneKeyFrame before,
                                                        BrBoneKeyFrame after, BrBoneKeyFrame afterPlus,
                                                        boolean firstPointPredicate, boolean lastPointPredicate,
                                                        MolangValue3AxisFunction function) {
        ArrayList<Vector2f> points = new ArrayList<>();

        if (firstPointPredicate)
            points.add(new Vector2f(beforePlus.timestamp(), function.apply(beforePlus.getPost(), scope)));

        points.add(new Vector2f(before.timestamp(), function.apply(before.getPost(), scope)));

        points.add(new Vector2f(after.timestamp(), function.apply(after.getPre(), scope)));

        if (lastPointPredicate)
            points.add(new Vector2f(afterPlus.timestamp(), function.apply(afterPlus.getPre(), scope)));

        return points;
    }

    /**
     * 线性插值
     *
     * @param other  另一个关键帧
     * @param weight 权重
     * @return 值
     */
    public Vector3f linearLerp(MolangScope scope, BrBoneKeyFrame other, float weight) {
        var am3 = this.dataPoints.size() > 1 && this.timestamp() < other.timestamp() ? getPost() : getPre();
        var bm3 = other.dataPoints.size() > 1 && this.timestamp() > other.timestamp() ? other.getPost() : other.getPre();

        float ax = am3.getX(scope);
        float bx = bm3.getX(scope);

        float ay = am3.getY(scope);
        float by = bm3.getY(scope);

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

    public MolangValue3 getPre() {
        return dataPoints.getFirst();
    }

    public MolangValue3 getPost() {
        return dataPoints.getLast();
    }

    public record Factory(
            List<MolangValue3> dataPoints,
            LerpMode lerpMode
    ) {
        public static final Codec<Factory> CODEC = Codec.withAlternative(
                Codec.withAlternative(
                        MolangValue3.CODEC,
                        MolangValue.CODEC.xmap(mv -> new MolangValue3(mv, mv, mv), MolangValue3::x)
                ).xmap(m3 -> new Factory(List.of(m3), LerpMode.LINEAR), f -> f.dataPoints().getFirst()),
                EyelibCodec.check(RecordCodecBuilder.create(ins -> ins.group(
                        LerpMode.CODEC.optionalFieldOf("lerp_mode", LerpMode.LINEAR).forGetter(Factory::lerpMode),
                        MolangValue3.CODEC.optionalFieldOf("pre").forGetter(f -> Optional.of(f.dataPoints().getFirst())),
                        MolangValue3.CODEC.optionalFieldOf("post").forGetter(f -> f.dataPoints().size() < 2 ? Optional.empty() : Optional.of(f.dataPoints().getLast()))
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
                })), f -> f.dataPoints.isEmpty()
                        ? DataResult.error(() -> "BoneKeyFrame need pre or post.")
                        : DataResult.success(f))
        );

        public static Factory from(BrBoneKeyFrame keyFrame) {
            return new Factory(keyFrame.dataPoints, keyFrame.lerpMode);
        }

        public BrBoneKeyFrame create(float timestamp) {
            return new BrBoneKeyFrame(timestamp, dataPoints, lerpMode);
        }
    }
}
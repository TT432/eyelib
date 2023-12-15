package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.util.MolangValue3;
import io.github.tt432.eyelib.util.math.Curves;
import io.github.tt432.eyelib.util.math.MathE;
import net.minecraft.util.Mth;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author TT432
 */
public record BrBoneKeyFrame(
        float timestamp,
        MolangValue3[] dataPoints,
        BrBoneKeyFrame.LerpMode lerpMode
) {

    public BrBoneKeyFrame copy(MolangScope scope) {
        return new BrBoneKeyFrame(timestamp, Arrays.stream(dataPoints).map(v -> v.copy(scope)).toArray(MolangValue3[]::new), lerpMode);
    }

    public enum LerpMode {
        LINEAR,
        CATMULLROM
    }

    public float getTick() {
        return timestamp;
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
    public static Vector3f catmullromLerp(BrBoneKeyFrame beforePlus,
                                          BrBoneKeyFrame before,
                                          BrBoneKeyFrame after,
                                          BrBoneKeyFrame afterPlus,
                                          float weight,
                                          Vector3f result) {
        catmullromArray.clear();

        boolean firstPointPredicate = beforePlus != null && before.dataPoints.length == 1;
        boolean lastPointPredicate = afterPlus != null && after.dataPoints.length == 1;

        if (firstPointPredicate)
            catmullromArray.add(cTempP1.set(beforePlus.getTick(), beforePlus.get(1).getX()));

        catmullromArray.add(cTempP2.set(before.getTick(), before.get(1).getX()));

        catmullromArray.add(cTempP3.set(after.getTick(), after.get(0).getX()));

        if (lastPointPredicate)
            catmullromArray.add(cTempP4.set(afterPlus.getTick(), afterPlus.get(0).getX()));

        float time = (weight + (beforePlus != null ? 1 : 0)) / (catmullromArray.size() - 1);

        var x = Curves.lerpSplineCurve(catmullromArray, time).y;

        catmullromArray.clear();

        if (firstPointPredicate)
            catmullromArray.add(cTempP1.set(beforePlus.getTick(), beforePlus.get(1).getY()));

        catmullromArray.add(cTempP2.set(before.getTick(), before.get(1).getY()));

        catmullromArray.add(cTempP3.set(after.getTick(), after.get(0).getY()));

        if (lastPointPredicate)
            catmullromArray.add(cTempP4.set(afterPlus.getTick(), afterPlus.get(0).getY()));

        time = (weight + (beforePlus != null ? 1 : 0)) / (catmullromArray.size() - 1);

        var y = Curves.lerpSplineCurve(catmullromArray, time).y;

        catmullromArray.clear();

        if (firstPointPredicate)
            catmullromArray.add(cTempP1.set(beforePlus.getTick(), beforePlus.get(1).getZ()));

        catmullromArray.add(cTempP2.set(before.getTick(), before.get(1).getZ()));

        catmullromArray.add(cTempP3.set(after.getTick(), after.get(0).getZ()));

        if (lastPointPredicate)
            catmullromArray.add(cTempP4.set(afterPlus.getTick(), afterPlus.get(0).getZ()));

        time = (weight + (beforePlus != null ? 1 : 0)) / (catmullromArray.size() - 1);

        var z = Curves.lerpSplineCurve(catmullromArray, time).y;

        return result.set(x, y, z);
    }

    /**
     * 线性插值
     *
     * @param other  另一个关键帧
     * @param weight 权重
     * @return 值
     */
    public Vector3f linearLerp(BrBoneKeyFrame other, Vector3f result, float weight) {
        var aDataPoint = this.dataPoints.length > 1 && getTick() < other.getTick() ? 1 : 0;
        var bDataPoint = other.dataPoints.length > 1 && getTick() > other.getTick() ? 1 : 0;

        MolangValue3 am3 = get(aDataPoint);
        MolangValue3 bm3 = get(bDataPoint);

        float ax = am3.getX();
        float bx = bm3.getX();

        float ay = am3.getY();
        float by = bm3.getY();

        float az = am3.getZ();
        float bz = bm3.getZ();

        return result.set(
                ax == bx ? ax : MathE.lerp(ax, bx, weight),
                ay == by ? ay : MathE.lerp(ay, by, weight),
                az == bz ? az : MathE.lerp(az, bz, weight)
        );
    }

    /**
     * 获取对应数据点的对应轴的指定索引的值
     *
     * @param dataPoint 索引
     * @return 值
     */
    public MolangValue3 get(int dataPoint) {
        if (dataPoint != 0) {
            dataPoint = Mth.clamp(dataPoint, 0, dataPoints.length - 1);
        }

        return dataPoints[dataPoint];
    }

    public static BrBoneKeyFrame parse(MolangScope scope, float timestamp, JsonElement json) throws JsonParseException {
        MolangValue3[] dataPoints;
        BrBoneKeyFrame.LerpMode lerpMode;

        if (json.isJsonArray()) {
            lerpMode = LerpMode.LINEAR;

            dataPoints = new MolangValue3[]{MolangValue3.parse(scope, json.getAsJsonArray())};
        } else if (json.isJsonPrimitive()) {
            lerpMode = LerpMode.LINEAR;
            MolangValue value = MolangValue.parse(scope, json.getAsString());
            dataPoints = new MolangValue3[]{new MolangValue3(value, value, value)};
        } else if (json.isJsonObject()) {
            JsonObject jo = json.getAsJsonObject();

            if (jo.has("lerp_mode"))
                lerpMode = LerpMode.valueOf(jo.get("lerp_mode").getAsString().toUpperCase());
            else lerpMode = LerpMode.LINEAR;

            String pre = "pre";
            String post = "post";

            if (jo.has(pre) && !jo.has(post)) {
                dataPoints = new MolangValue3[]{MolangValue3.parse(scope, jo.get(pre).getAsJsonArray())};
            } else if (jo.has(post) && (!jo.has(pre) || lerpMode == LerpMode.CATMULLROM)) {
                dataPoints = new MolangValue3[]{MolangValue3.parse(scope, jo.get(post).getAsJsonArray())};
            } else {
                dataPoints = new MolangValue3[]{
                        MolangValue3.parse(scope, jo.get(pre).getAsJsonArray()),
                        MolangValue3.parse(scope, jo.get(post).getAsJsonArray())
                };
            }
        } else {
            throw new JsonParseException("");
        }

        return new BrBoneKeyFrame(timestamp, dataPoints, lerpMode);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BrBoneKeyFrame bbkf && bbkf.timestamp == timestamp
                && Arrays.equals(bbkf.dataPoints, dataPoints) && bbkf.lerpMode == lerpMode;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(timestamp) & Arrays.hashCode(dataPoints) & lerpMode.hashCode();
    }

    @Override
    public String toString() {
        return "BrBoneKeyFrame { timestamp : %f ; dataPints : %s ; lerpMode : %s ; }"
                .formatted(timestamp, Arrays.toString(dataPoints), lerpMode.name());
    }
}
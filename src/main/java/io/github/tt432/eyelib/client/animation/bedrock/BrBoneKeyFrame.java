package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.util.MolangValue3;
import io.github.tt432.eyelib.util.math.Axis;
import io.github.tt432.eyelib.util.math.MathE;
import io.github.tt432.eyelib.util.math.curve.SplineCurve;
import net.minecraft.util.Mth;
import org.joml.Vector2f;

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

    /**
     * 平滑插值
     *
     * @param beforePlus p0
     * @param before     p1
     * @param after      p2
     * @param afterPlus  p3
     * @param axis       轴
     * @param weight     weight
     * @return 值
     */
    public static float catmullromLerp(BrBoneKeyFrame beforePlus,
                                       BrBoneKeyFrame before,
                                       BrBoneKeyFrame after,
                                       BrBoneKeyFrame afterPlus,
                                       Axis axis, float weight) {
        List<Vector2f> vectors = new ArrayList<>();

        if (beforePlus != null && before.dataPoints.length == 1)
            vectors.add(new Vector2f(beforePlus.getTick(), beforePlus.get(axis, 1)));

        vectors.add(new Vector2f(before.getTick(), before.get(axis, 1)));

        vectors.add(new Vector2f(after.getTick(), after.get(axis, 0)));

        if (afterPlus != null && after.dataPoints.length == 1)
            vectors.add(new Vector2f(afterPlus.getTick(), afterPlus.get(axis, 0)));

        SplineCurve curve = new SplineCurve(vectors.toArray(new Vector2f[0]));
        float time = (weight + (beforePlus != null ? 1 : 0)) / (vectors.size() - 1);

        return curve.getPoint(time).y;
    }

    /**
     * 线性插值
     *
     * @param other  另一个关键帧
     * @param axis   轴
     * @param weight 权重
     * @return 值
     */
    public float linearLerp(BrBoneKeyFrame other, Axis axis, float weight) {
        var aDataPoint = this.dataPoints.length > 1 && getTick() < other.getTick() ? 1 : 0;
        var bDataPoint = other.dataPoints.length > 1 && getTick() > other.getTick() ? 1 : 0;

        if (this.get(axis, aDataPoint) == other.get(axis, bDataPoint)) {
            return this.get(axis, aDataPoint);
        } else {
            return MathE.lerp(this.get(axis, aDataPoint), other.get(axis, bDataPoint), weight);
        }
    }

    /**
     * 获取对应数据点的对应轴的指定索引的值
     *
     * @param axis      轴
     * @param dataPoint 索引
     * @return 值
     */
    public float get(Axis axis, int dataPoint) {
        if (dataPoint != 0) {
            dataPoint = Mth.clamp(dataPoint, 0, dataPoints.length - 1);
        }

        return dataPoints[dataPoint].get(axis);
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
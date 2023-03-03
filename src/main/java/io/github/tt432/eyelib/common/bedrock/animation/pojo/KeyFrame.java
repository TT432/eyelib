package io.github.tt432.eyelib.common.bedrock.animation.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.util.Value3;
import io.github.tt432.eyelib.util.Axis;
import io.github.tt432.eyelib.util.math.MathE;
import io.github.tt432.eyelib.util.math.Vec2d;
import io.github.tt432.eyelib.util.math.curve.SplineCurve;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.util.Mth;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static io.github.tt432.eyelib.common.bedrock.animation.pojo.KeyFrame.LerpMode.CATMULLROM;
import static io.github.tt432.eyelib.common.bedrock.animation.pojo.KeyFrame.LerpMode.LINEAR;

/**
 * @author DustW
 */
@Data
@NoArgsConstructor
@JsonAdapter(KeyFrame.Serializer.class)
public class KeyFrame {
    private Timestamp timestamp;
    private Value3[] dataPoints;
    private LerpMode lerpMode;

    public enum LerpMode {
        LINEAR,
        CATMULLROM,
        BEZIER,
        STEP
    }

    public double getTick() {
        return timestamp.getTick();
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
    public static double catmullromLerp(KeyFrame beforePlus,
                                        KeyFrame before,
                                        KeyFrame after,
                                        KeyFrame afterPlus,
                                        Axis axis, double weight) {
        List<Vec2d> vectors = new ArrayList<>();

        if (beforePlus != null && before.dataPoints.length == 1)
            vectors.add(new Vec2d(beforePlus.getTick(), beforePlus.get(axis, 1)));

        vectors.add(new Vec2d(before.getTick(), before.get(axis, 1)));

        vectors.add(new Vec2d(after.getTick(), after.get(axis, 0)));

        if (afterPlus != null && after.dataPoints.length == 1)
            vectors.add(new Vec2d(afterPlus.getTick(), afterPlus.get(axis, 0)));

        SplineCurve curve = new SplineCurve(vectors.toArray(new Vec2d[0]));
        double time = (weight + (beforePlus != null ? 1 : 0)) / (vectors.size() - 1);

        return curve.getPoint(time).getY();
    }

    /**
     * 线性插值
     *
     * @param other  另一个关键帧
     * @param axis   轴
     * @param weight 权重
     * @return 值
     */
    public double linearLerp(KeyFrame other, Axis axis, double weight) {
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
    public double get(Axis axis, int dataPoint) {
        if (dataPoint != 0) {
            dataPoint = Mth.clamp(dataPoint, 0, dataPoints.length - 1);
        }

        return dataPoints[dataPoint].get(axis).evaluate(MolangParser.getGlobalScope());
    }

    protected static class Serializer implements JsonDeserializer<KeyFrame> {
        @Override
        public KeyFrame deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            KeyFrame frame = new KeyFrame();

            if (json.isJsonArray()) {
                frame.lerpMode = LINEAR;

                frame.dataPoints = new Value3[]{context.deserialize(json, Value3.class)};
            } else if (json.isJsonPrimitive()) {
                frame.lerpMode = LINEAR;
                MolangValue value = context.deserialize(json, MolangValue.class);
                frame.dataPoints = new Value3[]{new Value3(value, value, value)};
            } else if (json.isJsonObject()) {
                JsonObject jo = json.getAsJsonObject();

                if (jo.has("lerp_mode"))
                    frame.lerpMode = LerpMode.valueOf(jo.get("lerp_mode").getAsString().toUpperCase());
                else frame.lerpMode = LINEAR;

                if (frame.lerpMode == CATMULLROM || frame.lerpMode == LINEAR) {
                    String pre = "pre";
                    String post = "post";

                    if (jo.has(pre) && !jo.has(post)) {
                        frame.dataPoints = new Value3[]{context.deserialize(jo.get(pre), Value3.class)};
                    } else if (jo.has(post) && (!jo.has(pre) || frame.lerpMode == CATMULLROM)) {
                        frame.dataPoints = new Value3[]{context.deserialize(jo.get(post), Value3.class)};
                    } else {
                        frame.dataPoints = new Value3[]{
                                context.deserialize(jo.get(pre), Value3.class),
                                context.deserialize(jo.get(post), Value3.class)
                        };
                    }
                }
            }

            return frame;
        }
    }
}

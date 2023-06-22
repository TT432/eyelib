package io.github.tt432.eyelib.common.bedrock.animation.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.util.Axis;
import io.github.tt432.eyelib.util.math.MathE;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

/**
 * @author DustW
 */
@Data
@JsonAdapter(BoneAnimation.Serializer.class)
public class BoneAnimation {
    /**
     * 注意：读取的数据为角度制
     */
    private final TreeMap<Timestamp, KeyFrame> rotation;
    private final TreeMap<Timestamp, KeyFrame> position;
    private final TreeMap<Timestamp, KeyFrame> scale;

    private static class Ref {
        KeyFrame before = null;
        KeyFrame after = null;
        KeyFrame result = null;
    }

    public Vector3d lerpRotation(double currentTick) {
        return lerp(rotation, currentTick);
    }

    public Vector3d lerpPosition(double currentTick) {
        return lerp(position, currentTick);
    }

    public Vector3d lerpScale(double currentTick) {
        return lerp(scale, currentTick);
    }

    public double getLastTick() {
        return Math.max(last(scale), Math.max(last(rotation), last(position)));
    }

    double last(TreeMap<Timestamp, KeyFrame> frames) {
        return frames != null && !frames.isEmpty() ? frames.lastKey().getTick() : 0;
    }

    /**
     * 计算插值
     *
     * @param frames      frames
     * @param currentTick 当前 tick
     * @return 值
     */
    public static Vector3d lerp(TreeMap<Timestamp, KeyFrame> frames, double currentTick) {
        var ref = new Ref();
        double epsilon = 1D / 1200D;

        Timestamp currTimestamp = new Timestamp(currentTick);

        Map.Entry<Timestamp, KeyFrame> floorEntry = frames.floorEntry(currTimestamp);

        if (floorEntry != null) {
            ref.before = floorEntry.getValue();
        }

        Map.Entry<Timestamp, KeyFrame> higherEntry = frames.higherEntry(currTimestamp);

        if (higherEntry != null) {
            ref.after = higherEntry.getValue();
        }

        boolean isBeforeTime = ref.before != null && MathE.epsilon(ref.before.getTick(), currentTick, epsilon);
        boolean isAfterTime = ref.after != null && MathE.epsilon(ref.after.getTick(), currentTick, epsilon);
        boolean onlyBefore = ref.before != null && ref.after == null;
        boolean onlyAfter = ref.after != null && ref.before == null;

        if (isBeforeTime || (!isAfterTime && onlyBefore)) {
            ref.result = ref.before;
        } else if (isAfterTime || onlyAfter) {
            ref.result = ref.after;
        } else if (ref.after != null) {
            assert floorEntry != null;
            assert higherEntry != null;

            double weight = MathE.getWeight(ref.before.getTick(), ref.after.getTick(), currentTick);

            if (ref.before.getLerpMode() == KeyFrame.LerpMode.LINEAR && ref.after.getLerpMode() == KeyFrame.LerpMode.LINEAR) {
                return mapAxes(axis -> ref.before.linearLerp(ref.after, axis, weight));
            } else if (ref.before.getLerpMode() == KeyFrame.LerpMode.CATMULLROM || ref.after.getLerpMode() == KeyFrame.LerpMode.CATMULLROM) {
                var beforePlus = frames.lowerEntry(floorEntry.getKey());
                var afterPlus = frames.higherEntry(higherEntry.getKey());

                return mapAxes(axis -> KeyFrame.catmullromLerp(
                        beforePlus != null ? beforePlus.getValue() : null,
                        ref.before, ref.after,
                        afterPlus != null ? afterPlus.getValue() : null,
                        axis, weight));
            } else if (ref.before.getLerpMode() == KeyFrame.LerpMode.BEZIER || ref.after.getLerpMode() == KeyFrame.LerpMode.BEZIER) {
                // todo 实现 bezier
            } else if (ref.after.getLerpMode() == KeyFrame.LerpMode.STEP) {
                // TODO 实现 step
            }
        }

        if (ref.result != null) {
            var index = ref.result.getTick() > currentTick ||
                    MathE.epsilon(ref.result.getTick(), currentTick, epsilon) ? 0 : ref.result.getDataPoints().length - 1;

            return mapAxes(axis -> ref.result.get(axis, index));
        }

        return null;
    }

    public static Vector3d mapAxes(ToDoubleFunction<Axis> func) {
        return new Vector3d(
                func.applyAsDouble(Axis.X),
                func.applyAsDouble(Axis.Y),
                func.applyAsDouble(Axis.Z)
        );
    }

    @Slf4j
    protected static class Serializer implements JsonDeserializer<BoneAnimation> {
        @Override
        public BoneAnimation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();

            JsonElement rotationJson = object.get("rotation");
            JsonElement positionJson = object.get("position");
            JsonElement scaleJson = object.get("scale");

            return new BoneAnimation(
                    toKeyFrameList(rotationJson, context),
                    toKeyFrameList(positionJson, context),
                    toKeyFrameList(scaleJson, context)
            );
        }

        TreeMap<Timestamp, KeyFrame> toKeyFrameList(JsonElement element, JsonDeserializationContext context) {
            TreeMap<Timestamp, KeyFrame> result = new TreeMap<>(Comparator.comparingDouble(Timestamp::getTick));

            if (element == null) {
                return result;
            }

            if (element.isJsonObject()) {
                return process(element.getAsJsonObject(), context);
            } else {
                KeyFrame keyFrame = context.deserialize(element, KeyFrame.class);
                keyFrame.setTimestamp(Timestamp.ZERO);
                result.put(Timestamp.ZERO, keyFrame);
                return result;
            }
        }

        public static TreeMap<Timestamp, KeyFrame> process(JsonObject jsonObject, JsonDeserializationContext context) {
            TreeMap<Timestamp, KeyFrame> result = new TreeMap<>(Comparator.comparingDouble(Timestamp::getTick));

            jsonObject.entrySet().forEach(e -> {
                KeyFrame keyFrame = context.deserialize(e.getValue(), KeyFrame.class);
                Timestamp timestamp = Timestamp.valueOf(e.getKey());
                keyFrame.setTimestamp(timestamp);
                result.put(timestamp, keyFrame);
            });

            return result;
        }
    }
}

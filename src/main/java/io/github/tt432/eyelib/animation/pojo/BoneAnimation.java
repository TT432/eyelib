package io.github.tt432.eyelib.animation.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.mojang.math.Vector3d;
import io.github.tt432.eyelib.util.math.MathE;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import io.github.tt432.eyelib.util.Axis;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

import static io.github.tt432.eyelib.animation.pojo.KeyFrame.LerpMode.*;

/**
 * @author DustW
 */
@Data
@JsonAdapter(BoneAnimation.Serializer.class)
public class BoneAnimation {
    /**
     * 注意：读取的数据为角度制
     */
    private final List<KeyFrame> rotation;
    private final List<KeyFrame> position;
    private final List<KeyFrame> scale;

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

    /**
     * 计算插值
     *
     * @param frames      frames
     * @param currentTick 当前 tick
     * @return 值
     */
    public static Vector3d lerp(List<KeyFrame> frames, double currentTick) {
        var ref = new Ref();
        double epsilon = 1D / 1200D;

        findKeyFrame(frames, currentTick, ref);

        boolean isBeforeTime = ref.before != null && MathE.epsilon(ref.before.getTick(), currentTick, epsilon);
        boolean isAfterTime = ref.after != null && MathE.epsilon(ref.after.getTick(), currentTick, epsilon);
        boolean onlyBefore = ref.before != null && ref.after == null;
        boolean onlyAfter = ref.after != null && ref.before == null;

        if (isBeforeTime || (!isAfterTime && onlyBefore)) {
            ref.result = ref.before;
        } else if (isAfterTime || onlyAfter) {
            ref.result = ref.after;
        } else if (ref.after != null) {
            double weight = MathE.getWeight(ref.before.getTick(), ref.after.getTick(), currentTick);

            if (ref.before.getLerpMode() == LINEAR && ref.after.getLerpMode() == LINEAR) {
                return mapAxes(axis -> ref.before.linearLerp(ref.after, axis, weight));
            } else if (ref.before.getLerpMode() == CATMULLROM || ref.after.getLerpMode() == CATMULLROM) {
                var beforeIndex = frames.indexOf(ref.before);

                if (beforeIndex != -1) {
                    var beforePlus = beforeIndex > 1 ? frames.get(beforeIndex - 1) : null;
                    var afterPlus = beforeIndex + 2 < frames.size() ? frames.get(beforeIndex + 2) : null;
                    return mapAxes(axis -> KeyFrame.getCatmullromLerp(beforePlus, ref.before,
                            ref.after, afterPlus, axis, weight));
                }
            } else if (ref.before.getLerpMode() == BEZIER || ref.after.getLerpMode() == BEZIER) {
                // todo 实现 bezier
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

    /**
     * 在 frames 中寻找 before 和 after
     *
     * @param frames      frames
     * @param currentTick 当前 tick
     * @param ref         引用
     */
    static void findKeyFrame(List<KeyFrame> frames, double currentTick, Ref ref) {
        for (KeyFrame frame : frames) {
            if (frame.getTick() < currentTick) {
                if (ref.before == null || frame.getTick() > ref.before.getTick()) {
                    ref.before = frame;
                }
            } else {
                if (ref.after == null || frame.getTick() < ref.after.getTick()) {
                    ref.after = frame;
                }
            }
        }
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

        List<KeyFrame> toKeyFrameList(JsonElement element, JsonDeserializationContext context) {
            if (element == null)
                return Collections.emptyList();

            if (element.isJsonObject()) {
                return process(element.getAsJsonObject(), context);
            } else {
                KeyFrame keyFrame = context.deserialize(element, KeyFrame.class);
                keyFrame.setTimestamp(Timestamp.ZERO);
                return Collections.singletonList(keyFrame);
            }
        }

        public static List<KeyFrame> process(JsonObject jsonObject, JsonDeserializationContext context) {
            return jsonObject.entrySet().stream()
                    .map(e -> {
                        KeyFrame result = context.deserialize(e.getValue(), KeyFrame.class);
                        result.setTimestamp(Timestamp.valueOf(e.getKey()));
                        return result;
                    })
                    .sorted(Comparator.comparingDouble(KeyFrame::getTick))
                    .toList();
        }
    }
}

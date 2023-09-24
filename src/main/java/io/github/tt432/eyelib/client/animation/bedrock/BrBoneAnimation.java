package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.util.math.Axis;
import io.github.tt432.eyelib.util.math.MathE;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * if rotation_global
 * relative_to / rotation -> 'entity'
 * rotation -> [0, 0, 0.01]
 * ---
 * if rotation_global
 * [2] = 0.01
 *
 * @author TT432
 */
@AllArgsConstructor
@Getter
public class BrBoneAnimation {
    TreeMap<Float, BrBoneKeyFrame> rotation;
    TreeMap<Float, BrBoneKeyFrame> position;
    TreeMap<Float, BrBoneKeyFrame> scale;

    private static class Ref {
        BrBoneKeyFrame before = null;
        BrBoneKeyFrame after = null;
        BrBoneKeyFrame result = null;
    }

    public Vector3f lerpRotation(float currentTick) {
        return lerp(rotation, currentTick);
    }

    public Vector3f lerpPosition(float currentTick) {
        return lerp(position, currentTick);
    }

    public Vector3f lerpScale(float currentTick) {
        return lerp(scale, currentTick);
    }

    public float getLastTick() {
        return Math.max(last(scale), Math.max(last(rotation), last(position)));
    }

    float last(TreeMap<Float, BrBoneKeyFrame> frames) {
        return frames != null && !frames.isEmpty() ? frames.lastKey() : 0;
    }

    /**
     * 计算插值
     *
     * @param frames      frames
     * @param currentTick 当前 tick
     * @return 值
     */
    public static Vector3f lerp(TreeMap<Float, BrBoneKeyFrame> frames, float currentTick) {
        var ref = new Ref();
        double epsilon = 1D / 1200D;

        var floorEntry = frames.floorEntry(currentTick);

        if (floorEntry != null) {
            ref.before = floorEntry.getValue();
        }

        var higherEntry = frames.higherEntry(currentTick);

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

            var weight = MathE.getWeight(ref.before.getTick(), ref.after.getTick(), currentTick);

            if (ref.before.getLerpMode() == BrBoneKeyFrame.LerpMode.LINEAR && ref.after.getLerpMode() == BrBoneKeyFrame.LerpMode.LINEAR) {
                return mapAxes(axis -> ref.before.linearLerp(ref.after, axis, weight));
            } else if (ref.before.getLerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM || ref.after.getLerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM) {
                var beforePlus = frames.lowerEntry(floorEntry.getKey());
                var afterPlus = frames.higherEntry(higherEntry.getKey());

                return mapAxes(axis -> BrBoneKeyFrame.catmullromLerp(
                        beforePlus != null ? beforePlus.getValue() : null,
                        ref.before, ref.after,
                        afterPlus != null ? afterPlus.getValue() : null,
                        axis, weight));
            }
        }

        if (ref.result != null) {
            var index = ref.result.getTick() > currentTick ||
                    MathE.epsilon(ref.result.getTick(), currentTick, epsilon) ? 0 : ref.result.getDataPoints().length - 1;

            return mapAxes(axis -> ref.result.get(axis, index));
        }

        return null;
    }

    public static Vector3f mapAxes(Function<Axis, Float> func) {
        return new Vector3f(
                func.apply(Axis.X),
                func.apply(Axis.Y),
                func.apply(Axis.Z)
        );
    }

    public static BrBoneAnimation parse(MolangScope scope, JsonElement json) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();

        JsonElement rotationJson = object.get("rotation");
        JsonElement positionJson = object.get("position");
        JsonElement scaleJson = object.get("scale");

        return new BrBoneAnimation(
                toKeyFrameList(scope, rotationJson),
                toKeyFrameList(scope, positionJson),
                toKeyFrameList(scope, scaleJson)
        );
    }

    static TreeMap<Float, BrBoneKeyFrame> toKeyFrameList(MolangScope scope, JsonElement element) {
        TreeMap<Float, BrBoneKeyFrame> result = new TreeMap<>(Comparator.comparingDouble(k -> k));

        if (element == null) {
            return result;
        }

        if (element.isJsonObject()) {
            return process(scope,element.getAsJsonObject());
        } else {
            BrBoneKeyFrame keyFrame = BrBoneKeyFrame.parse(scope, element);
            keyFrame.setTimestamp(0);
            result.put(0F, keyFrame);
            return result;
        }
    }

    public static TreeMap<Float, BrBoneKeyFrame> process(MolangScope scope, JsonObject jsonObject) {
        TreeMap<Float, BrBoneKeyFrame> result = new TreeMap<>(Comparator.comparingDouble(k -> k));

        jsonObject.entrySet().forEach(e -> {
            BrBoneKeyFrame keyFrame = BrBoneKeyFrame.parse(scope, e.getValue());
            float timestamp = Float.parseFloat(e.getKey());
            keyFrame.setTimestamp(timestamp);
            result.put(timestamp, keyFrame);
        });

        return result;
    }
}

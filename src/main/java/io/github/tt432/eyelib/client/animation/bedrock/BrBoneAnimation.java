package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.util.MolangValue3;
import io.github.tt432.eyelib.util.math.MathE;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.TreeMap;

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
public record BrBoneAnimation(
        TreeMap<Float, BrBoneKeyFrame> rotation,
        TreeMap<Float, BrBoneKeyFrame> position,
        TreeMap<Float, BrBoneKeyFrame> scale
) {

    public BrBoneAnimation copy(MolangScope scope) {
        TreeMap<Float, BrBoneKeyFrame> copiedRotation = new TreeMap<>(rotation.comparator());
        rotation.forEach((key, value) -> copiedRotation.put(key, value.copy(scope)));
        TreeMap<Float, BrBoneKeyFrame> copiedPosition = new TreeMap<>(position.comparator());
        position.forEach((key, value) -> copiedPosition.put(key, value.copy(scope)));
        TreeMap<Float, BrBoneKeyFrame> copiedScale = new TreeMap<>(scale.comparator());
        scale.forEach((key, value) -> copiedScale.put(key, value.copy(scope)));

        return new BrBoneAnimation(copiedRotation, copiedPosition, copiedScale);
    }

    private static BrBoneKeyFrame before = null;
    private static BrBoneKeyFrame after = null;
    private static BrBoneKeyFrame result = null;
    private static final Vector3f tempResult = new Vector3f();

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
        before = null;
        after = null;
        result = null;

        double epsilon = 1D / 1200D;

        var floorEntry = frames.floorEntry(currentTick);

        if (floorEntry != null) {
            before = floorEntry.getValue();
        }

        var higherEntry = frames.higherEntry(currentTick);

        if (higherEntry != null) {
            after = higherEntry.getValue();
        }

        boolean isBeforeTime = before != null && MathE.epsilon(before.getTick(), currentTick, epsilon);
        boolean isAfterTime = after != null && MathE.epsilon(after.getTick(), currentTick, epsilon);
        boolean onlyBefore = before != null && after == null;
        boolean onlyAfter = after != null && before == null;

        if (isBeforeTime || (!isAfterTime && onlyBefore)) {
            result = before;
        } else if (isAfterTime || onlyAfter) {
            result = after;
        } else if (after != null) {
            assert floorEntry != null;
            assert higherEntry != null;

            var weight = MathE.getWeight(before.getTick(), after.getTick(), currentTick);

            if (before.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR && after.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR) {
                return before.linearLerp(after, tempResult, weight);
            } else if (before.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM || after.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM) {
                var beforePlus = frames.lowerEntry(floorEntry.getKey());
                var afterPlus = frames.higherEntry(higherEntry.getKey());

                return BrBoneKeyFrame.catmullromLerp(
                        beforePlus != null ? beforePlus.getValue() : null,
                        before, after,
                        afterPlus != null ? afterPlus.getValue() : null,
                        weight, tempResult);
            }
        }

        if (result != null) {
            MolangValue3 m3 = result.get(result.getTick() > currentTick ||
                    MathE.epsilon(result.getTick(), currentTick, epsilon) ? 0 : result.dataPoints().length - 1);

            return tempResult.set(m3.getX(), m3.getY(), m3.getZ());
        }

        return null;
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
            return process(scope, element.getAsJsonObject());
        } else {
            BrBoneKeyFrame keyFrame = BrBoneKeyFrame.parse(scope, 0, element);
            result.put(0F, keyFrame);
            return result;
        }
    }

    public static TreeMap<Float, BrBoneKeyFrame> process(MolangScope scope, JsonObject jsonObject) {
        TreeMap<Float, BrBoneKeyFrame> result = new TreeMap<>(Comparator.comparingDouble(k -> k));

        jsonObject.entrySet().forEach(e -> {
            float timestamp = Float.parseFloat(e.getKey());
            BrBoneKeyFrame keyFrame = BrBoneKeyFrame.parse(scope, timestamp, e.getValue());
            result.put(timestamp, keyFrame);
        });

        return result;
    }
}

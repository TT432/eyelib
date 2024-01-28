package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.tt432.eyelib.molang.MolangSystemScope;
import io.github.tt432.eyelib.molang.util.MolangValue3;
import io.github.tt432.eyelib.util.ImmutableFloatTreeMap;
import io.github.tt432.eyelib.util.math.MathE;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

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
        ImmutableFloatTreeMap<BrBoneKeyFrame> rotation,
        ImmutableFloatTreeMap<BrBoneKeyFrame> position,
        ImmutableFloatTreeMap<BrBoneKeyFrame> scale
) {
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

    float last(ImmutableFloatTreeMap<BrBoneKeyFrame> frames) {
        return frames != null && !frames.isEmpty() ? frames.lastKey() : 0;
    }

    private static final float epsilon = 1F / 1200F;

    /**
     * 计算插值
     *
     * @param frames      frames
     * @param currentTick 当前 tick
     * @return 值
     */
    public static Vector3f lerp(ImmutableFloatTreeMap<BrBoneKeyFrame> frames, float currentTick) {
        before = frames.floorEntry(currentTick);
        after = frames.higherEntry(currentTick);
        result = null;

        boolean isBeforeTime = before != null && MathE.epsilon(before.getTick(), currentTick, epsilon);
        boolean isAfterTime = after != null && MathE.epsilon(after.getTick(), currentTick, epsilon);
        boolean onlyBefore = before != null && after == null;
        boolean onlyAfter = after != null && before == null;

        if (isBeforeTime || (!isAfterTime && onlyBefore)) {
            result = before;
        } else if (isAfterTime || onlyAfter) {
            result = after;
        } else if (after != null) {
            var weight = MathE.getWeight(before.getTick(), after.getTick(), currentTick);

            if (before.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR && after.lerpMode() == BrBoneKeyFrame.LerpMode.LINEAR) {
                return before.linearLerp(after, tempResult, weight);
            } else if (before.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM || after.lerpMode() == BrBoneKeyFrame.LerpMode.CATMULLROM) {
                var beforePlus = frames.lowerEntry(before.getTick());
                var afterPlus = frames.higherEntry(after.getTick());

                return BrBoneKeyFrame.catmullromLerp(
                        beforePlus,
                        before, after,
                        afterPlus,
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

    public static BrBoneAnimation parse(MolangSystemScope scope, JsonElement json) throws JsonParseException {
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

    static ImmutableFloatTreeMap<BrBoneKeyFrame> toKeyFrameList(MolangSystemScope scope, JsonElement element) {
        if (element == null) {
            return ImmutableFloatTreeMap.empty();
        }

        if (element.isJsonObject()) {
            return process(scope, element.getAsJsonObject());
        } else {
            BrBoneKeyFrame keyFrame = BrBoneKeyFrame.parse(scope, 0, element);
            Float2ObjectOpenHashMap<BrBoneKeyFrame> data = new Float2ObjectOpenHashMap<>();
            data.put(0, keyFrame);
            return ImmutableFloatTreeMap.of(new float[]{0}, data);
        }
    }

    private static int i = 0;

    public static ImmutableFloatTreeMap<BrBoneKeyFrame> process(MolangSystemScope scope, JsonObject jsonObject) {
        Float2ObjectOpenHashMap<BrBoneKeyFrame> data = new Float2ObjectOpenHashMap<>();

        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();

        int size = entries.size();

        float[] keys = new float[size];

        i = 0;

        entries.forEach(e -> {
            float timestamp = Float.parseFloat(e.getKey());
            BrBoneKeyFrame keyFrame = BrBoneKeyFrame.parse(scope, timestamp, e.getValue());
            data.put(timestamp, keyFrame);
            keys[i++] = timestamp;
        });

        Arrays.sort(keys);

        return ImmutableFloatTreeMap.of(keys, data);
    }
}

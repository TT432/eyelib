package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author TT432
 */
public class BrAnimationEntry {
    BrLoopType loop;
    float animation_length;
    /**
     * TODO 不确定
     */
    boolean override_previous_animation;
    /**
     * TODO 不确定
     */
    MolangValue anim_time_update;
    /**
     * TODO 不确定
     */
    MolangValue blend_weight;
    /**
     * TODO 不确定
     */
    MolangValue start_delay;
    /**
     * TODO 不确定
     */
    MolangValue loop_delay;

    TreeMap<Float, BrEffectsKeyFrame[]> soundEffects;
    TreeMap<Float, BrEffectsKeyFrame[]> particleEffects;
    TreeMap<Float, MolangValue[]> timeline;

    Map<String, BrBoneAnimation> bones;

    public static BrAnimationEntry parse(MolangScope scope, JsonObject jsonObject) {
        BrAnimationEntry result = new BrAnimationEntry();

        result.loop = BrLoopType.parse(jsonObject.get("loop"));
        result.animation_length = jsonObject.get("animation_length") instanceof JsonPrimitive jp ? jp.getAsFloat() : 0;
        result.soundEffects = loadMap(jsonObject, "sound_effects", scope);
        result.particleEffects = loadMap(jsonObject, "particle_effects", scope);

        result.timeline = new TreeMap<>(Comparator.comparingDouble(k -> k));

        if (jsonObject.get("timeline") instanceof JsonObject timelineJson) {
            timelineJson.asMap().forEach((key, value) -> {
                float timestamp = Float.parseFloat(key);

                if (value instanceof JsonArray ja) {
                    MolangValue[] molangValues = new MolangValue[ja.size()];

                    for (int i = 0; i < ja.asList().size(); i++) {
                        molangValues[i] = MolangValue.parse(scope, ja.get(i).getAsString());
                    }

                    result.timeline.put(timestamp, molangValues);
                } else {
                    result.timeline.put(timestamp, new MolangValue[]{MolangValue.parse(scope, value.getAsString())});
                }
            });
        }

        result.bones = jsonObject.get("bones") instanceof JsonObject jo
                ? jo.asMap().entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), BrBoneAnimation.parse(scope, entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                : new HashMap<>();

        return result;
    }

    private static TreeMap<Float, BrEffectsKeyFrame[]> loadMap(JsonObject jsonObject, String effectKey, MolangScope scope) {
        if (jsonObject.get(effectKey) instanceof JsonObject jo) {
            TreeMap<Float, BrEffectsKeyFrame[]> map = new TreeMap<>(Comparator.comparingDouble(k -> k));

            jo.asMap().forEach((key, value) -> {
                float timestamp = Float.parseFloat(key);

                if (value instanceof JsonArray ja) {
                    BrEffectsKeyFrame[] keyFrames = new BrEffectsKeyFrame[ja.size()];

                    for (int i = 0; i < ja.asList().size(); i++) {
                        keyFrames[i] = BrEffectsKeyFrame.parse(scope, timestamp, ja.get(i).getAsJsonObject());
                    }

                    map.put(timestamp, keyFrames);
                } else {
                    map.put(timestamp, new BrEffectsKeyFrame[]{BrEffectsKeyFrame.parse(scope, timestamp, value.getAsJsonObject())});
                }
            });

            return map;
        } else {
            return new TreeMap<>(Comparator.comparingDouble(k -> k));
        }
    }
}

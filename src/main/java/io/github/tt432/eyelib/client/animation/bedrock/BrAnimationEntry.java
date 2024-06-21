package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.MolangValue;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @param override_previous_animation TODO 不确定
 * @param anim_time_update            TODO 不确定
 * @param blendWeight                 动画混合时的权重
 * @param start_delay                 TODO 不确定
 * @param loop_delay                  TODO 不确定
 * @author TT432
 */
public record BrAnimationEntry(
        BrLoopType loop,
        float animationLength,
        boolean override_previous_animation,
        @Nullable
        MolangValue anim_time_update,
        MolangValue blendWeight,
        @Nullable
        MolangValue start_delay,
        @Nullable
        MolangValue loop_delay,
        TreeMap<Float, BrEffectsKeyFrame[]> soundEffects,
        TreeMap<Float, BrEffectsKeyFrame[]> particleEffects,
        TreeMap<Float, MolangValue[]> timeline,
        Map<String, BrBoneAnimation> bones
) {
    public static BrAnimationEntry parse(JsonObject jsonObject) {
        final BrLoopType loop;
        final float animationLength;
        final boolean override_previous_animation;
        final MolangValue anim_time_update;
        final MolangValue blend_weight;
        final MolangValue start_delay;
        final MolangValue loop_delay;
        final TreeMap<Float, BrEffectsKeyFrame[]> soundEffects;
        final TreeMap<Float, BrEffectsKeyFrame[]> particleEffects;
        final TreeMap<Float, MolangValue[]> timeline;
        final Map<String, BrBoneAnimation> bones = new HashMap<>();

        loop = BrLoopType.parse(jsonObject.get("loop"));
        animationLength = jsonObject.get("animation_length") instanceof JsonPrimitive jp ? jp.getAsFloat() : 0;
        soundEffects = loadMap(jsonObject, "sound_effects");
        particleEffects = loadMap(jsonObject, "particle_effects");
        override_previous_animation = jsonObject.get("override_previous_animation") instanceof JsonPrimitive jp && jp.getAsBoolean();
        anim_time_update = MolangValue.parse(jsonObject.get("anim_time_update"));
        blend_weight = MolangValue.parse(jsonObject.get("blend_weight"), MolangValue.TRUE_VALUE);
        start_delay = MolangValue.parse(jsonObject.get("start_delay"));
        loop_delay = MolangValue.parse(jsonObject.get("loop_delay"));

        timeline = new TreeMap<>(Comparator.comparingDouble(k -> k));

        if (jsonObject.get("timeline") instanceof JsonObject timelineJson) {
            timelineJson.entrySet().forEach(entry -> {
                float timestamp = Float.parseFloat(entry.getKey());

                if (entry.getValue() instanceof JsonArray ja) {
                    MolangValue[] molangValues = new MolangValue[ja.size()];

                    for (int i = 0; i < ja.size(); i++) {
                        molangValues[i] = MolangValue.parse(ja.get(i).getAsString());
                    }

                    timeline.put(timestamp, molangValues);
                } else {
                    timeline.put(timestamp, new MolangValue[]{MolangValue.parse(entry.getValue().getAsString())});
                }
            });
        }

        if (jsonObject.get("bones") instanceof JsonObject jo) {
            for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
                bones.put(entry.getKey(), BrBoneAnimation.parse(entry.getValue()));
            }
        }

        return new BrAnimationEntry(loop, animationLength, override_previous_animation, anim_time_update, blend_weight,
                start_delay, loop_delay, soundEffects, particleEffects, timeline, bones);
    }

    private static TreeMap<Float, BrEffectsKeyFrame[]> loadMap(JsonObject jsonObject, String effectKey) {
        if (jsonObject.get(effectKey) instanceof JsonObject jo) {
            TreeMap<Float, BrEffectsKeyFrame[]> map = new TreeMap<>(Comparator.comparingDouble(k -> k));

            jo.entrySet().forEach(entry -> {
                var value = entry.getValue();
                float timestamp = Float.parseFloat(entry.getKey());

                if (value instanceof JsonArray ja) {
                    BrEffectsKeyFrame[] keyFrames = new BrEffectsKeyFrame[ja.size()];

                    for (int i = 0; i < ja.size(); i++) {
                        keyFrames[i] = BrEffectsKeyFrame.parse(timestamp, ja.get(i).getAsJsonObject());
                    }

                    map.put(timestamp, keyFrames);
                } else {
                    map.put(timestamp, new BrEffectsKeyFrame[]{BrEffectsKeyFrame.parse(timestamp, value.getAsJsonObject())});
                }
            });

            return map;
        } else {
            return new TreeMap<>(Comparator.comparingDouble(k -> k));
        }
    }
}

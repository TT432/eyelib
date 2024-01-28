package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.MolangSystemScope;
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
    public static BrAnimationEntry parse(MolangSystemScope scope, JsonObject jsonObject) {
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
        soundEffects = loadMap(jsonObject, "sound_effects", scope);
        particleEffects = loadMap(jsonObject, "particle_effects", scope);
        override_previous_animation = jsonObject.get("override_previous_animation") instanceof JsonPrimitive jp && jp.getAsBoolean();
        anim_time_update = jsonObject.get("anim_time_update") instanceof JsonPrimitive jp ? MolangValue.parse(scope, jp.getAsString()) : null;
        blend_weight = jsonObject.get("blend_weight") instanceof JsonPrimitive jp ? MolangValue.parse(scope, jp.getAsString()) : MolangValue.TRUE_VALUE;
        start_delay = jsonObject.get("start_delay") instanceof JsonPrimitive jp ? MolangValue.parse(scope, jp.getAsString()) : null;
        loop_delay = jsonObject.get("loop_delay") instanceof JsonPrimitive jp ? MolangValue.parse(scope, jp.getAsString()) : null;

        timeline = new TreeMap<>(Comparator.comparingDouble(k -> k));

        if (jsonObject.get("timeline") instanceof JsonObject timelineJson) {
            timelineJson.asMap().forEach((key, value) -> {
                float timestamp = Float.parseFloat(key);

                if (value instanceof JsonArray ja) {
                    MolangValue[] molangValues = new MolangValue[ja.size()];

                    for (int i = 0; i < ja.asList().size(); i++) {
                        molangValues[i] = MolangValue.parse(scope, ja.get(i).getAsString());
                    }

                    timeline.put(timestamp, molangValues);
                } else {
                    timeline.put(timestamp, new MolangValue[]{MolangValue.parse(scope, value.getAsString())});
                }
            });
        }

        if (jsonObject.get("bones") instanceof JsonObject jo) {
            for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
                bones.put(entry.getKey(), BrBoneAnimation.parse(scope, entry.getValue()));
            }
        }

        return new BrAnimationEntry(loop, animationLength, override_previous_animation, anim_time_update, blend_weight,
                start_delay, loop_delay, soundEffects, particleEffects, timeline, bones);
    }

    private static TreeMap<Float, BrEffectsKeyFrame[]> loadMap(JsonObject jsonObject, String effectKey, MolangSystemScope scope) {
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

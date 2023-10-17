package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.google.gson.*;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
@AllArgsConstructor
public class BrAcState {
    /**
     * 动画名称 -> 混合系数（动画变换倍数）
     */
    final Map<String, MolangValue> animations;
    final MolangValue onEntry;
    final MolangValue onExit;
    final List<BrAcParticleEffect> particleEffects;
    final List<String> soundEffects;
    /**
     * stateName -> condition
     */
    final Map<String, MolangValue> transitions;
    /**
     * 插值时间
     */
    final float blendTransition;
    /**
     * 线性 t/f 平滑 (存疑)
     */
    final boolean blendViaShortestPath;

    public static BrAcState parse(MolangScope scope, JsonElement value) throws JsonParseException {
        final Map<String, MolangValue> animations;
        final MolangValue onEntry;
        final MolangValue onExit;
        final List<BrAcParticleEffect> particleEffects = new ArrayList<>();
        final List<String> soundEffects = new ArrayList<>();
        final Map<String, MolangValue> transitions = new HashMap<>();
        final float blendTransition;
        final boolean blendViaShortestPath;

        animations = new HashMap<>();

        if (!(value instanceof JsonObject jo)) {
            throw new JsonParseException("can't parse animation controller state entry.");
        }

        if (jo.get("animations") instanceof JsonArray ja) {
            for (JsonElement jsonElement : ja.asList()) {
                if (jsonElement instanceof JsonObject animationJson) {
                    for (Map.Entry<String, JsonElement> entry : animationJson.entrySet()) {
                        animations.put(entry.getKey(), MolangValue.parse(scope, entry.getValue().getAsString()));
                    }
                } else if (jsonElement instanceof JsonPrimitive animationName) {
                    animations.put(animationName.getAsString(), MolangValue.parse(scope, "1"));
                }
            }
        }

        if (jo.get("on_entry") instanceof JsonArray ja) {
            StringBuilder molangText = new StringBuilder();

            for (JsonElement jsonElement : ja) {
                molangText.append(jsonElement.getAsString());
            }

            onEntry = MolangValue.parse(scope, molangText.toString());
        } else {
            onEntry = null;
        }

        if (jo.get("on_exit") instanceof JsonArray ja) {
            StringBuilder molangText = new StringBuilder();

            for (JsonElement jsonElement : ja) {
                molangText.append(jsonElement.getAsString());
            }

            onExit = MolangValue.parse(scope, molangText.toString());
        } else {
            onExit = null;
        }

        if (jo.get("particle_effects") instanceof JsonArray ja) {
            List<BrAcParticleEffect> particles = new ArrayList<>();

            for (JsonElement jsonElement : ja) {
                if (jsonElement instanceof JsonObject particleEffectObject)
                    particles.add(BrAcParticleEffect.parse(scope, particleEffectObject));
            }

            particleEffects.addAll(particles);
        }

        if (jo.get("sound_effects") instanceof JsonArray ja) {
            List<String> sounds = new ArrayList<>();

            for (JsonElement jsonElement : ja) {
                if (jsonElement instanceof JsonObject soundEffect) {
                    sounds.add(soundEffect.get("effect").getAsString());
                }
            }

            soundEffects.addAll(sounds);
        }

        if (jo.get("transitions") instanceof JsonArray ja) {
            Map<String, MolangValue> transitionMap = new HashMap<>();

            for (JsonElement jsonElement : ja) {
                if (jsonElement instanceof JsonObject transitionObject) {
                    transitionObject.asMap().forEach((k, v) ->
                            transitionMap.put(k, MolangValue.parse(scope, v.getAsString().replace("\n", ""))));
                }
            }

            transitions.putAll(transitionMap);
        }

        blendTransition = jo.get("blend_transition") instanceof JsonPrimitive jp ? jp.getAsFloat() : 0;
        blendViaShortestPath = jo.get("blend_via_shortest_path") instanceof JsonPrimitive jp && jp.getAsBoolean();

        return new BrAcState(animations, onEntry, onExit, particleEffects, soundEffects, transitions, blendTransition, blendViaShortestPath);
    }
}

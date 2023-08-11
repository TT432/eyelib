package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
public class BrAcState {
    /**
     * 动画名称 -> 混合系数（动画变换倍数）
     */
    Map<String, MolangValue> animations;
    MolangValue onEntry;
    MolangValue onExit;
    List<BrAcParticleEffect> particleEffects;
    List<String> soundEffects;
    /**
     * stateName -> condition
     */
    Map<String, MolangValue> transitions;
    /**
     * 插值时间
     */
    float blendTransition;
    /**
     * 线性 t/f 平滑 (存疑)
     */
    boolean blendViaShortestPath;

    public static BrAcState parse(MolangScope scope, JsonElement value) {
        BrAcState result = new BrAcState();

        result.animations = new HashMap<>();

        if (value instanceof JsonObject jo) {
            if (jo.get("animations") instanceof JsonArray ja) {
                for (JsonElement jsonElement : ja.asList()) {
                    if (jsonElement instanceof JsonObject animationJson) {
                        animationJson.asMap().forEach((animationName, blendValue) ->
                                result.animations.put(animationName, MolangValue.parse(scope, blendValue.getAsString())));
                    } else if (jsonElement instanceof JsonPrimitive animationName) {
                        result.animations.put(animationName.getAsString(), MolangValue.parse(scope, "1"));
                    }
                }
            }

            if (jo.get("on_entry") instanceof JsonArray ja) {
                StringBuilder molangText = new StringBuilder();

                for (JsonElement jsonElement : ja) {
                    molangText.append(jsonElement.getAsString());
                }

                result.onEntry = MolangValue.parse(scope, molangText.toString());
            }

            if (jo.get("on_exit") instanceof JsonArray ja) {
                StringBuilder molangText = new StringBuilder();

                for (JsonElement jsonElement : ja) {
                    molangText.append(jsonElement.getAsString());
                }

                result.onExit = MolangValue.parse(scope, molangText.toString());
            }

            if (jo.get("particle_effects") instanceof JsonArray ja) {
                List<BrAcParticleEffect> particles = new ArrayList<>();

                for (JsonElement jsonElement : ja) {
                    if (jsonElement instanceof JsonObject particleEffectObject)
                        particles.add(BrAcParticleEffect.parse(scope, particleEffectObject));
                }

                result.particleEffects = particles;
            }

            if (jo.get("sound_effects") instanceof JsonArray ja) {
                List<String> sounds = new ArrayList<>();

                for (JsonElement jsonElement : ja) {
                    if (jsonElement instanceof JsonObject soundEffect) {
                        sounds.add(soundEffect.get("effect").getAsString());
                    }
                }

                result.soundEffects = sounds;
            }

            if (jo.get("transitions") instanceof JsonArray ja) {
                Map<String, MolangValue> transitionMap = new HashMap<>();

                for (JsonElement jsonElement : ja) {
                    if (jsonElement instanceof JsonObject transitionObject) {
                        transitionObject.asMap().forEach((k, v) ->
                                transitionMap.put(k, MolangValue.parse(scope, v.getAsString().replace("\n", ""))));
                    }
                }

                result.transitions = transitionMap;
            }

            result.blendTransition = jo.get("blend_transition") instanceof JsonPrimitive jp ? jp.getAsFloat() : 0;
            result.blendViaShortestPath = jo.get("blend_via_shortest_path") instanceof JsonPrimitive jp && jp.getAsBoolean();
        }

        return result;
    }
}

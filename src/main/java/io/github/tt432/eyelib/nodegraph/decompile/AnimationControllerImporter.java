package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.ShortNameOps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** AnimationController 文档导入：ac.root / states / transitions / 粒子音效。 */
final class AnimationControllerImporter {
    private AnimationControllerImporter() {
    }

    static final Set<String> AC_TOP_KEYS = Set.of("format_version", "animation_controllers");
    static final Set<String> AC_SCHEMA_KEYS = Set.of("initial_state", "states");
    static final Set<String> AC_STATE_KEYS = Set.of(
            "on_entry", "on_exit", "animations", "transitions",
            "blend_transition", "blend_via_shortest_path",
            "particle_effects", "sound_effects");

    /** animation controllers 文件 JSON + 目标 AC 名 → ANIMATION_CONTROLLER 库。 */
    static ImportResult importAnimationControllers(JsonObject fileJson, String acName) {
        return importAnimationControllers(fileJson, acName, ShortNameOps.KnownTables.EMPTY);
    }

    /** 同上传入已知短名表（规格 D4）：state animations 裸短名 ref 命中表项时回填标识符。 */
    static ImportResult importAnimationControllers(JsonObject fileJson, String acName,
                                                   ShortNameOps.KnownTables known) {
        return importAnimationControllers(fileJson, acName, known, false);
    }

    /** 同上，{@code inlineVariables=true} 时启用导入期变量内联。 */
    static ImportResult importAnimationControllers(JsonObject fileJson, String acName,
                                                   ShortNameOps.KnownTables known,
                                                   boolean inlineVariables) {
        ImportGraphBuilder b = new ImportGraphBuilder();
        b.inlineVariables(inlineVariables);
        JsonObject controllers = ImportValueOps.objOrNull(fileJson, "animation_controllers");
        JsonObject schema = controllers == null ? null : ImportValueOps.objOrNull(controllers, acName);
        if (schema == null) {
            b.error(DecompileDiagnostics.MISSING_ENTRY,
                    "animation_controllers 中找不到 '" + acName + "'");
            b.addRoot(NodeTypes.AC_ROOT.id(), ImportGraphBuilder.opts("identifier", acName));
            return b.build(GraphKind.ANIMATION_CONTROLLER);
        }

        Map<String, JsonElement> rootOptions = new LinkedHashMap<>();
        rootOptions.put("identifier", new JsonPrimitive(acName));
        // v10：initial_state 不再是选项——暂存名字，states 导入完后解析为 initial 图边
        JsonElement initialState = schema.get("initial_state");
        String initialStateName = null;
        if (initialState instanceof JsonPrimitive p && p.isString()) {
            initialStateName = p.getAsString();
        } else if (initialState != null) {
            ImportValueOps.invalidField(b, acName + ".initial_state", initialState);
        }
        b.addRoot(NodeTypes.AC_ROOT.id(), rootOptions);

        // AC 文件无声明表：动画短名按需补 ref.animation（跨状态去重，不报 UNKNOWN_REFERENCE）
        Map<String, String> animationRefs = new LinkedHashMap<>();
        Map<String, String> acRefs = new LinkedHashMap<>();
        // v10：state 名 → uid（initial/transition 图边解析）；粒子/音效短名按需补 ref（跨状态去重）
        Map<String, String> stateUids = new LinkedHashMap<>();
        List<String[]> pendingTransitionTargets = new ArrayList<>();
        Map<String, String> particleRefs = new LinkedHashMap<>();
        Map<String, String> soundRefs = new LinkedHashMap<>();

        JsonElement states = schema.get("states");
        if (states != null && !states.isJsonObject()) {
            ImportValueOps.invalidField(b, acName + ".states", states);
        } else if (states != null) {
            for (Map.Entry<String, JsonElement> stateEntry : states.getAsJsonObject().entrySet()) {
                importAcState(b, acName, stateEntry.getKey(), stateEntry.getValue(),
                        animationRefs, acRefs, stateUids, pendingTransitionTargets,
                        particleRefs, soundRefs);
            }
        }

        // initial / transition.target 图边接线（目标 state 不存在 → 诊断，组装器另有 UNKNOWN_STATE）
        if (initialStateName != null) {
            String target = stateUids.get(initialStateName);
            if (target != null) {
                b.wire(target, "state", "root", "initial");
            } else {
                b.warn(DecompileDiagnostics.UNKNOWN_REFERENCE,
                        "initial_state '" + initialStateName + "' 不在 states 键集中，未生成 initial 连线");
            }
        }
        for (String[] pending : pendingTransitionTargets) {
            String target = stateUids.get(pending[1]);
            if (target != null) {
                b.wire(pending[0], "target", target, "incoming");
            } else {
                b.warn(DecompileDiagnostics.UNKNOWN_REFERENCE,
                        "transition 目标 '" + pending[1] + "' 不在 states 键集中，未生成 target 连线");
            }
        }

        ImportValueOps.unknownFields(b, acName, schema, AC_SCHEMA_KEYS);
        ImportValueOps.unknownFields(b, "<file>", fileJson, AC_TOP_KEYS);
        return ImportDocSupport.backfilled(b.build(GraphKind.ANIMATION_CONTROLLER), known);
    }

    static void importAcState(ImportGraphBuilder b, String acName, String stateName,
                              JsonElement stateValue, Map<String, String> animationRefs,
                              Map<String, String> acRefs, Map<String, String> stateUids,
                              List<String[]> pendingTransitionTargets,
                              Map<String, String> particleRefs, Map<String, String> soundRefs) {
        String label = acName + ".states." + stateName;
        if (!stateValue.isJsonObject()) {
            ImportValueOps.invalidField(b, label, stateValue);
            return;
        }
        JsonObject state = stateValue.getAsJsonObject();
        Map<String, JsonElement> options = new LinkedHashMap<>();
        options.put("name", new JsonPrimitive(stateName));
        JsonElement blendTransition = state.get("blend_transition");
        if (blendTransition instanceof JsonPrimitive p && p.isNumber()) {
            options.put("blend_transition", p);
        } else if (blendTransition != null) {
            ImportValueOps.invalidField(b, label + ".blend_transition", blendTransition);
        }
        JsonElement shortestPath = state.get("blend_via_shortest_path");
        if (shortestPath instanceof JsonPrimitive p && p.isBoolean()) {
            options.put("blend_via_shortest_path", p);
        } else if (shortestPath != null) {
            ImportValueOps.invalidField(b, label + ".blend_via_shortest_path", shortestPath);
        }
        String stateUid = b.addNode("st", NodeTypes.AC_STATE.id(), options);
        b.wire(stateUid, "state", "root", "states");
        stateUids.put(stateName, stateUid);

        JsonElement onEntry = state.get("on_entry");
        if (onEntry != null) {
            b.wireStatementsFrom(ImportValueOps.statementSources(b, label + ".on_entry", onEntry),
                    stateUid, "on_entry");
        }
        JsonElement onExit = state.get("on_exit");
        if (onExit != null) {
            b.wireStatementsFrom(ImportValueOps.statementSources(b, label + ".on_exit", onExit),
                    stateUid, "on_exit");
        }
        JsonElement animations = state.get("animations");
        if (animations != null) {
            List<JsonElement> items =
                    ImportValueOps.arrayOrSingle(b, label + ".animations", animations, true);
            if (items != null) {
                for (JsonElement item : items) {
                    ImportDocSupport.importAnimateEntry(b, stateUid, "animations", item,
                            animationRefs, acRefs, false);
                }
            }
        }
        JsonElement transitions = state.get("transitions");
        if (transitions != null) {
            List<JsonObject> objects = ImportValueOps.singleKeyObjects(b, label + ".transitions", transitions);
            if (objects != null) {
                for (JsonObject obj : objects) {
                    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                        // v10：target 从选项改为图边——暂存目标名，states 全量导入后统一接线
                        String trUid = b.addNode("tr", NodeTypes.AC_TRANSITION.id(), Map.of());
                        ImportValueOps.valueSlot(b, trUid, "condition",
                                label + ".transitions." + e.getKey(), e.getValue());
                        b.wire(trUid, "transition", stateUid, "transitions");
                        pendingTransitionTargets.add(new String[]{trUid, e.getKey()});
                    }
                }
            }
        }

        // v10：粒子效果条目（particle.entry + 按需 ref.particle；pre_effect_script 走 molang 值槽）
        JsonElement particleEffects = state.get("particle_effects");
        if (particleEffects != null) {
            if (!particleEffects.isJsonArray()) {
                ImportValueOps.invalidField(b, label + ".particle_effects", particleEffects);
            } else {
                int row = 0;
                for (JsonElement item : particleEffects.getAsJsonArray()) {
                    String itemLabel = label + ".particle_effects[" + row++ + "]";
                    if (!item.isJsonObject()) {
                        ImportValueOps.invalidField(b, itemLabel, item);
                        continue;
                    }
                    JsonObject pe = item.getAsJsonObject();
                    JsonElement effect = pe.get("effect");
                    if (!(effect instanceof JsonPrimitive ep) || !ep.isString()) {
                        ImportValueOps.invalidField(b, itemLabel + ".effect", effect);
                        continue;
                    }
                    Map<String, JsonElement> peOptions = new LinkedHashMap<>();
                    JsonElement locator = pe.get("locator");
                    if (locator instanceof JsonPrimitive lp && lp.isString()) {
                        peOptions.put("locator", lp);
                    } else if (locator != null) {
                        ImportValueOps.invalidField(b, itemLabel + ".locator", locator);
                    }
                    JsonElement bind = pe.get("bind_to_actor");
                    if (bind instanceof JsonPrimitive bp && bp.isBoolean()) {
                        peOptions.put("bind_to_actor", bp);
                    } else if (bind != null) {
                        ImportValueOps.invalidField(b, itemLabel + ".bind_to_actor", bind);
                    }
                    String entryUid = b.addNode("pe", NodeTypes.PARTICLE_ENTRY.id(), peOptions);
                    String effectName = ep.getAsString();
                    String refUid = particleRefs.computeIfAbsent(effectName,
                            k -> b.addNode("rp", NodeTypes.REF_PARTICLE.id(),
                                    ImportGraphBuilder.opts("short_name", k)));
                    b.wire(refUid, "ref", entryUid, "ref");
                    JsonElement script = pe.get("pre_effect_script");
                    if (script != null) {
                        ImportValueOps.valueSlot(b, entryUid, "script", itemLabel + ".pre_effect_script", script);
                    }
                    b.wire(entryUid, "entry", stateUid, "particles");
                }
            }
        }

        // v10：音效（BE 形态 [{"effect":"短名"}]，容忍裸字符串；按需 ref.sound 直插 sounds 槽）
        JsonElement soundEffects = state.get("sound_effects");
        if (soundEffects != null) {
            if (!soundEffects.isJsonArray()) {
                ImportValueOps.invalidField(b, label + ".sound_effects", soundEffects);
            } else {
                int row = 0;
                for (JsonElement item : soundEffects.getAsJsonArray()) {
                    String itemLabel = label + ".sound_effects[" + row++ + "]";
                    String effectName = null;
                    if (item instanceof JsonPrimitive sp && sp.isString()) {
                        effectName = sp.getAsString();
                    } else if (item.isJsonObject()
                            && item.getAsJsonObject().get("effect") instanceof JsonPrimitive ep2
                            && ep2.isString()) {
                        effectName = ep2.getAsString();
                    }
                    if (effectName == null) {
                        ImportValueOps.invalidField(b, itemLabel, item);
                        continue;
                    }
                    String refUid = soundRefs.computeIfAbsent(effectName,
                            k -> b.addNode("rs", NodeTypes.REF_SOUND.id(),
                                    ImportGraphBuilder.opts("short_name", k)));
                    b.wire(refUid, "ref", stateUid, "sounds");
                }
            }
        }
        ImportValueOps.unknownFields(b, label, state, AC_STATE_KEYS);
    }
}

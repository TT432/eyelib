package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Bedrock JSON 资产 → 图库 导入器（规格 nodegraph-workbench §W2）：
 * 结构镜像 {@code ClientEntityAssembler} / {@code RenderControllerAssembler} /
 * {@code AnimationControllerAssembler} 的逆。
 *
 * <p>字段映射：
 * <ul>
 *   <li>ClientEntity：description.identifier → entity.root 选项；geometry/textures/materials/
 *       animations/animation_controllers 声明表 → ref.* 节点（无需连线，assembler 全局收集）；
 *       scripts.initialize/pre_animation/parent_setup（string 或 string[]）→ exec 链；
 *       scale/scaleX/scaleY/scaleZ → 表达式槽（数值/布尔 → 内联常量，字符串 → molang 反编译连线）；
 *       animate → animate.entry（短名解析到声明表 ref.animation/ref.ac，未声明 → 补独立
 *       ref.animation + UNKNOWN_REFERENCE）；render_controllers → rc.condition_entry + ref.rc；</li>
 *   <li>RenderController：geometry → 表达式槽（典型 {@code geometry.x} → ref.geometry 连线）；
 *       textures → list.entry；materials/part_visibility（object 或 single-key object 数组）
 *       → material.entry / part_visibility.entry；arrays → rc.root TEXT 选项（原文）；
 *       ignore_lighting → 布尔选项；color/is_hurt_color/on_fire_color/overlay_color
 *       → 四通道表达式槽；</li>
 *   <li>AnimationController：initial_state → ac.root 选项；states → ac.state
 *       （on_entry/on_exit → exec 链；animations → animate.entry；transitions → ac.transition；
 *       blend_transition/blend_via_shortest_path → 数值/布尔选项）。</li>
 * </ul>
 *
 * <p>未知/多余 JSON 字段不丢：StickyNote 记录原文 + UNKNOWN_FIELD 诊断；
 * 已知字段值类型非法 → 跳过 + INVALID_FIELD 诊断 + 便签。
 */
public final class JsonGraphImporters {
    private JsonGraphImporters() {
    }

    private static final Set<String> ENTITY_TOP_KEYS = Set.of("format_version", "minecraft:client_entity");
    private static final Set<String> ENTITY_DESC_KEYS = Set.of(
            "identifier", "scripts", "geometry", "textures", "materials",
            "animations", "animation_controllers", "render_controllers", "render_controller_conditions");
    private static final Set<String> ENTITY_SCRIPT_KEYS = Set.of(
            "initialize", "pre_animation", "parent_setup", "animate",
            "scale", "scaleX", "scaleY", "scaleZ");
    private static final Set<String> RC_TOP_KEYS = Set.of("format_version", "render_controllers");
    private static final Set<String> RC_ENTRY_KEYS = Set.of(
            "geometry", "textures", "materials", "part_visibility", "arrays", "ignore_lighting",
            "color", "is_hurt_color", "on_fire_color", "overlay_color");
    private static final Set<String> AC_TOP_KEYS = Set.of("format_version", "animation_controllers");
    private static final Set<String> AC_SCHEMA_KEYS = Set.of("initial_state", "states");
    private static final Set<String> AC_STATE_KEYS = Set.of(
            "on_entry", "on_exit", "animations", "transitions",
            "blend_transition", "blend_via_shortest_path");

    // ---------- ClientEntity ----------

    /** client_entity 文件 JSON（含 minecraft:client_entity 包装）→ CLIENT_ENTITY 库。 */
    public static ImportResult importClientEntity(JsonObject fileJson) {
        ImportGraphBuilder b = new ImportGraphBuilder();
        JsonObject wrapper = objOrNull(fileJson, "minecraft:client_entity");
        JsonObject desc = wrapper == null ? null : objOrNull(wrapper, "description");
        if (desc == null) {
            b.error(DecompileDiagnostics.MISSING_ENTRY, "缺少 minecraft:client_entity.description");
            b.addRoot(NodeTypes.ENTITY_ROOT.id(), Map.of());
            return b.build(GraphKind.CLIENT_ENTITY);
        }

        b.addRoot(NodeTypes.ENTITY_ROOT.id(), stringOption(desc, "identifier", b, "description"));

        // 声明表（ref.* 节点无需连线，assembler 从可达图全局收集）
        Map<String, String> animationRefs = new LinkedHashMap<>();
        Map<String, String> acRefs = new LinkedHashMap<>();
        importRefTable(b, desc, "geometry", NodeTypes.REF_GEOMETRY.id(), "identifier", null);
        importRefTable(b, desc, "textures", NodeTypes.REF_TEXTURE.id(), "path", null);
        importRefTable(b, desc, "materials", NodeTypes.REF_MATERIAL.id(), "material", null);
        importRefTable(b, desc, "animations", NodeTypes.REF_ANIMATION.id(), "identifier", animationRefs);
        importRefTable(b, desc, "animation_controllers", NodeTypes.REF_AC.id(), "identifier", acRefs);

        // scripts
        JsonElement scripts = desc.get("scripts");
        if (scripts != null) {
            if (scripts.isJsonObject()) {
                importEntityScripts(b, scripts.getAsJsonObject(), animationRefs, acRefs);
            } else {
                invalidField(b, "description.scripts", scripts);
            }
        }

        // render_controllers
        JsonElement rcs = desc.get("render_controllers");
        if (rcs != null) {
            if (rcs.isJsonArray()) {
                for (JsonElement entry : rcs.getAsJsonArray()) {
                    importRcConditionEntry(b, entry);
                }
            } else {
                invalidField(b, "description.render_controllers", rcs);
            }
        }

        // render_controller_conditions：map 形态（运行时与内联条件合并、显式优先）；
        // 与内联 {id: condition} 同构——导入为同一批 rc.condition_entry 节点，
        // 再 build 时以数组内联形态回出（语义等价，见 ADR-0022 D2 往返承诺）
        JsonElement rcc = desc.get("render_controller_conditions");
        if (rcc != null) {
            if (rcc.isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : rcc.getAsJsonObject().entrySet()) {
                    String refUid = b.addNode("rc", NodeTypes.REF_RC.id(),
                            ImportGraphBuilder.opts("identifier", e.getKey()));
                    String entryUid = b.addNode("rce", NodeTypes.RC_CONDITION_ENTRY.id(), Map.of());
                    b.wire(refUid, "ref", entryUid, "rc");
                    valueSlot(b, entryUid, "condition", "render_controller_conditions." + e.getKey(), e.getValue());
                    b.wire(entryUid, "entry", "root", "render_controllers");
                }
            } else {
                invalidField(b, "description.render_controller_conditions", rcc);
            }
        }

        unknownFields(b, "description", desc, ENTITY_DESC_KEYS);
        unknownFields(b, "<file>", fileJson, ENTITY_TOP_KEYS);
        return b.build(GraphKind.CLIENT_ENTITY);
    }

    /** 声明表：object 或 single-key object 数组（animation_controllers 的 Bedrock 惯例）。 */
    private static void importRefTable(ImportGraphBuilder b, JsonObject desc, String field,
                                       String refType, String valueOption,
                                       @Nullable Map<String, String> refsOut) {
        JsonElement table = desc.get(field);
        if (table == null) {
            return;
        }
        List<JsonObject> objects = singleKeyObjects(b, "description." + field, table);
        if (objects == null) {
            return;
        }
        String prefix = switch (field) {
            case "geometry" -> "geo";
            case "textures" -> "tex";
            case "materials" -> "mat";
            case "animations" -> "anim";
            default -> "acref";
        };
        for (JsonObject obj : objects) {
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                if (!e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isString()) {
                    invalidField(b, "description." + field + "." + e.getKey(), e.getValue());
                    continue;
                }
                String uid = b.addNode(prefix, refType, ImportGraphBuilder.opts(
                        "short_name", e.getKey(),
                        valueOption, e.getValue().getAsString()));
                if (refsOut != null) {
                    refsOut.putIfAbsent(e.getKey(), uid);
                }
            }
        }
    }

    private static void importEntityScripts(ImportGraphBuilder b, JsonObject scripts,
                                            Map<String, String> animationRefs,
                                            Map<String, String> acRefs) {
        for (String slot : List.of("initialize", "pre_animation", "parent_setup")) {
            JsonElement v = scripts.get(slot);
            if (v != null) {
                b.wireStatements(statementSources(b, "scripts." + slot, v), "root", slot);
            }
        }
        JsonElement animate = scripts.get("animate");
        if (animate != null) {
            if (animate.isJsonArray()) {
                for (JsonElement entry : animate.getAsJsonArray()) {
                    importAnimateEntry(b, "root", "animate", entry, animationRefs, acRefs, true);
                }
            } else {
                invalidField(b, "scripts.animate", animate);
            }
        }
        valueSlot(b, "root", "scale", "scripts.scale", scripts.get("scale"));
        valueSlot(b, "root", "scale_x", "scripts.scaleX", scripts.get("scaleX"));
        valueSlot(b, "root", "scale_y", "scripts.scaleY", scripts.get("scaleY"));
        valueSlot(b, "root", "scale_z", "scripts.scaleZ", scripts.get("scaleZ"));
        unknownFields(b, "scripts", scripts, ENTITY_SCRIPT_KEYS);
    }

    /** render_controllers 条目：纯字符串（condition 恒 1）或 {identifier: condition}。 */
    private static void importRcConditionEntry(ImportGraphBuilder b, JsonElement entry) {
        if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
            String refUid = b.addNode("rc", NodeTypes.REF_RC.id(),
                    ImportGraphBuilder.opts("identifier", entry.getAsString()));
            String entryUid = b.addNode("rce", NodeTypes.RC_CONDITION_ENTRY.id(), Map.of());
            b.wire(refUid, "ref", entryUid, "rc");
            b.wire(entryUid, "entry", "root", "render_controllers");
        } else if (entry.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : entry.getAsJsonObject().entrySet()) {
                String refUid = b.addNode("rc", NodeTypes.REF_RC.id(),
                        ImportGraphBuilder.opts("identifier", e.getKey()));
                String entryUid = b.addNode("rce", NodeTypes.RC_CONDITION_ENTRY.id(), Map.of());
                b.wire(refUid, "ref", entryUid, "rc");
                valueSlot(b, entryUid, "condition", "render_controllers." + e.getKey(), e.getValue());
                b.wire(entryUid, "entry", "root", "render_controllers");
            }
        } else {
            invalidField(b, "render_controllers[]", entry);
        }
    }

    /**
     * animate / ac.state animations 条目：纯字符串（weight 缺省 1）或 {short_name: weight}。
     * 短名解析：animations 声明表 → ref.animation；animation_controllers 声明表 → ref.ac；
     * 未声明 → 补仅 short_name 的 ref.animation（warnIfUnresolved 时 + UNKNOWN_REFERENCE）。
     */
    private static void importAnimateEntry(ImportGraphBuilder b, String ownerUid, String slotPort,
                                           JsonElement entry, Map<String, String> animationRefs,
                                           Map<String, String> acRefs, boolean warnIfUnresolved) {
        if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
            String entryUid = b.addNode("ae", NodeTypes.ANIMATE_ENTRY.id(), Map.of());
            b.wire(resolveAnimationRef(b, entry.getAsString(), animationRefs, acRefs, warnIfUnresolved),
                    "ref", entryUid, "ref");
            b.wire(entryUid, "entry", ownerUid, slotPort);
        } else if (entry.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : entry.getAsJsonObject().entrySet()) {
                String entryUid = b.addNode("ae", NodeTypes.ANIMATE_ENTRY.id(), Map.of());
                b.wire(resolveAnimationRef(b, e.getKey(), animationRefs, acRefs, warnIfUnresolved),
                        "ref", entryUid, "ref");
                valueSlot(b, entryUid, "weight", "animate." + e.getKey(), e.getValue());
                b.wire(entryUid, "entry", ownerUid, slotPort);
            }
        } else {
            invalidField(b, "animate[]", entry);
        }
    }

    private static String resolveAnimationRef(ImportGraphBuilder b, String shortName,
                                              Map<String, String> animationRefs,
                                              Map<String, String> acRefs, boolean warnIfUnresolved) {
        String uid = animationRefs.get(shortName);
        if (uid != null) {
            return uid;
        }
        uid = acRefs.get(shortName);
        if (uid != null) {
            return uid;
        }
        uid = b.addNode("ra", NodeTypes.REF_ANIMATION.id(),
                ImportGraphBuilder.opts("short_name", shortName));
        animationRefs.put(shortName, uid);
        if (warnIfUnresolved) {
            b.warn(DecompileDiagnostics.UNKNOWN_REFERENCE,
                    "animate 短名 '" + shortName + "' 不在声明表中，已补独立 ref.animation");
        }
        return uid;
    }

    // ---------- RenderController ----------

    /** render_controllers 文件 JSON + 目标 RC 名 → RENDER_CONTROLLER 库。 */
    public static ImportResult importRenderController(JsonObject fileJson, String rcName) {
        ImportGraphBuilder b = new ImportGraphBuilder();
        JsonObject controllers = objOrNull(fileJson, "render_controllers");
        JsonObject entry = controllers == null ? null : objOrNull(controllers, rcName);
        if (entry == null) {
            b.error(DecompileDiagnostics.MISSING_ENTRY,
                    "render_controllers 中找不到 '" + rcName + "'");
            b.addRoot(NodeTypes.RC_ROOT.id(), ImportGraphBuilder.opts("identifier", rcName));
            return b.build(GraphKind.RENDER_CONTROLLER);
        }

        Map<String, JsonElement> rootOptions = new LinkedHashMap<>();
        rootOptions.put("identifier", new JsonPrimitive(rcName));
        JsonElement ignoreLighting = entry.get("ignore_lighting");
        if (ignoreLighting instanceof JsonPrimitive p && p.isBoolean()) {
            rootOptions.put("ignore_lighting", p);
        } else if (ignoreLighting != null) {
            invalidField(b, rcName + ".ignore_lighting", ignoreLighting);
        }
        JsonElement arrays = entry.get("arrays");
        if (arrays != null) {
            if (arrays.isJsonObject()) {
                rootOptions.put("arrays", new JsonPrimitive(arrays.toString()));
            } else {
                invalidField(b, rcName + ".arrays", arrays);
            }
        }
        b.addRoot(NodeTypes.RC_ROOT.id(), rootOptions);

        valueSlot(b, "root", "geometry", rcName + ".geometry", entry.get("geometry"));

        JsonElement textures = entry.get("textures");
        if (textures != null) {
            List<JsonElement> items = arrayOrSingle(b, rcName + ".textures", textures, false);
            if (items != null) {
                for (JsonElement item : items) {
                    String entryUid = b.addNode("le", NodeTypes.LIST_ENTRY.id(), Map.of());
                    valueSlot(b, entryUid, "value", rcName + ".textures[]", item);
                    b.wire(entryUid, "entry", "root", "textures");
                }
            }
        }

        importPatternMap(b, entry.get("materials"), rcName + ".materials",
                NodeTypes.MATERIAL_ENTRY.id(), "pattern", "value", "me", "materials");
        importPatternMap(b, entry.get("part_visibility"), rcName + ".part_visibility",
                NodeTypes.PART_VISIBILITY_ENTRY.id(), "bone_pattern", "condition", "pve", "part_visibility");

        importColor(b, entry, rcName, "color", "color");
        importColor(b, entry, rcName, "is_hurt_color", "is_hurt");
        importColor(b, entry, rcName, "on_fire_color", "on_fire");
        importColor(b, entry, rcName, "overlay_color", "overlay");

        unknownFields(b, rcName, entry, RC_ENTRY_KEYS);
        unknownFields(b, "<file>", fileJson, RC_TOP_KEYS);
        return b.build(GraphKind.RENDER_CONTROLLER);
    }

    /** materials / part_visibility：object 或 single-key object 数组 → 条目节点接入 SLOT。 */
    private static void importPatternMap(ImportGraphBuilder b, @Nullable JsonElement value, String label,
                                         String entryType, String patternOption, String valuePort,
                                         String uidPrefix, String slotPort) {
        if (value == null) {
            return;
        }
        List<JsonObject> objects = singleKeyObjects(b, label, value);
        if (objects == null) {
            return;
        }
        for (JsonObject obj : objects) {
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                String entryUid = b.addNode(uidPrefix, entryType,
                        ImportGraphBuilder.opts(patternOption, e.getKey()));
                valueSlot(b, entryUid, valuePort, label + "." + e.getKey(), e.getValue());
                b.wire(entryUid, "entry", "root", slotPort);
            }
        }
    }

    /** 颜色组：{r,g,b,a} 四通道表达式槽（数字 → 内联常量；字符串 → molang 反编译连线）。 */
    private static void importColor(ImportGraphBuilder b, JsonObject entry, String rcName,
                                    String field, String portPrefix) {
        JsonElement color = entry.get(field);
        if (color == null) {
            return;
        }
        if (!color.isJsonObject()) {
            invalidField(b, rcName + "." + field, color);
            return;
        }
        JsonObject obj = color.getAsJsonObject();
        valueSlot(b, "root", portPrefix + "_r", rcName + "." + field + ".r", obj.get("r"));
        valueSlot(b, "root", portPrefix + "_g", rcName + "." + field + ".g", obj.get("g"));
        valueSlot(b, "root", portPrefix + "_b", rcName + "." + field + ".b", obj.get("b"));
        valueSlot(b, "root", portPrefix + "_a", rcName + "." + field + ".a", obj.get("a"));
    }

    // ---------- AnimationController ----------

    /** animation controllers 文件 JSON + 目标 AC 名 → ANIMATION_CONTROLLER 库。 */
    public static ImportResult importAnimationControllers(JsonObject fileJson, String acName) {
        ImportGraphBuilder b = new ImportGraphBuilder();
        JsonObject controllers = objOrNull(fileJson, "animation_controllers");
        JsonObject schema = controllers == null ? null : objOrNull(controllers, acName);
        if (schema == null) {
            b.error(DecompileDiagnostics.MISSING_ENTRY,
                    "animation_controllers 中找不到 '" + acName + "'");
            b.addRoot(NodeTypes.AC_ROOT.id(), ImportGraphBuilder.opts("identifier", acName));
            return b.build(GraphKind.ANIMATION_CONTROLLER);
        }

        Map<String, JsonElement> rootOptions = new LinkedHashMap<>();
        rootOptions.put("identifier", new JsonPrimitive(acName));
        JsonElement initialState = schema.get("initial_state");
        if (initialState instanceof JsonPrimitive p && p.isString()) {
            rootOptions.put("initial_state", p);
        } else if (initialState != null) {
            invalidField(b, acName + ".initial_state", initialState);
        }
        b.addRoot(NodeTypes.AC_ROOT.id(), rootOptions);

        // AC 文件无声明表：动画短名按需补 ref.animation（跨状态去重，不报 UNKNOWN_REFERENCE）
        Map<String, String> animationRefs = new LinkedHashMap<>();
        Map<String, String> acRefs = new LinkedHashMap<>();

        JsonElement states = schema.get("states");
        if (states != null && !states.isJsonObject()) {
            invalidField(b, acName + ".states", states);
        } else if (states != null) {
            for (Map.Entry<String, JsonElement> stateEntry : states.getAsJsonObject().entrySet()) {
                importAcState(b, acName, stateEntry.getKey(), stateEntry.getValue(),
                        animationRefs, acRefs);
            }
        }

        unknownFields(b, acName, schema, AC_SCHEMA_KEYS);
        unknownFields(b, "<file>", fileJson, AC_TOP_KEYS);
        return b.build(GraphKind.ANIMATION_CONTROLLER);
    }

    private static void importAcState(ImportGraphBuilder b, String acName, String stateName,
                                      JsonElement stateValue, Map<String, String> animationRefs,
                                      Map<String, String> acRefs) {
        String label = acName + ".states." + stateName;
        if (!stateValue.isJsonObject()) {
            invalidField(b, label, stateValue);
            return;
        }
        JsonObject state = stateValue.getAsJsonObject();
        Map<String, JsonElement> options = new LinkedHashMap<>();
        options.put("name", new JsonPrimitive(stateName));
        JsonElement blendTransition = state.get("blend_transition");
        if (blendTransition instanceof JsonPrimitive p && p.isNumber()) {
            options.put("blend_transition", p);
        } else if (blendTransition != null) {
            invalidField(b, label + ".blend_transition", blendTransition);
        }
        JsonElement shortestPath = state.get("blend_via_shortest_path");
        if (shortestPath instanceof JsonPrimitive p && p.isBoolean()) {
            options.put("blend_via_shortest_path", p);
        } else if (shortestPath != null) {
            invalidField(b, label + ".blend_via_shortest_path", shortestPath);
        }
        String stateUid = b.addNode("st", NodeTypes.AC_STATE.id(), options);
        b.wire(stateUid, "state", "root", "states");

        JsonElement onEntry = state.get("on_entry");
        if (onEntry != null) {
            b.wireStatements(statementSources(b, label + ".on_entry", onEntry), stateUid, "on_entry");
        }
        JsonElement onExit = state.get("on_exit");
        if (onExit != null) {
            b.wireStatements(statementSources(b, label + ".on_exit", onExit), stateUid, "on_exit");
        }
        JsonElement animations = state.get("animations");
        if (animations != null) {
            List<JsonElement> items = arrayOrSingle(b, label + ".animations", animations, true);
            if (items != null) {
                for (JsonElement item : items) {
                    importAnimateEntry(b, stateUid, "animations", item, animationRefs, acRefs, false);
                }
            }
        }
        JsonElement transitions = state.get("transitions");
        if (transitions != null) {
            List<JsonObject> objects = singleKeyObjects(b, label + ".transitions", transitions);
            if (objects != null) {
                for (JsonObject obj : objects) {
                    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                        String trUid = b.addNode("tr", NodeTypes.AC_TRANSITION.id(),
                                ImportGraphBuilder.opts("target", e.getKey()));
                        valueSlot(b, trUid, "condition", label + ".transitions." + e.getKey(), e.getValue());
                        b.wire(trUid, "transition", stateUid, "transitions");
                    }
                }
            }
        }
        unknownFields(b, label, state, AC_STATE_KEYS);
    }

    // ---------- 共享工具 ----------

    /** 值槽接线：字符串 → molang 反编译连线；字符串数组 → "; " 拼接后连线（ExprSet）；
     * 数值/布尔 → 内联常量；其余 → INVALID_FIELD + 便签。 */
    private static void valueSlot(ImportGraphBuilder b, String nodeUid, String portId,
                                  String label, @Nullable JsonElement value) {
        if (value == null) {
            return;
        }
        if (value.isJsonPrimitive()) {
            JsonPrimitive p = value.getAsJsonPrimitive();
            if (p.isString()) {
                b.wireExpression(p.getAsString(), nodeUid, portId);
            } else {
                b.putConstant(nodeUid, portId, p);
            }
        } else if (value.isJsonArray() && allStrings(value.getAsJsonArray())) {
            // Bedrock 允许 weight/条件等值为 molang 字符串数组（ExprSet 语义）；
            // 拼接后由反编译器统一处理（单元素等价字符串直译）
            List<String> parts = new ArrayList<>();
            for (JsonElement el : value.getAsJsonArray()) {
                parts.add(el.getAsString());
            }
            b.wireExpression(String.join("; ", parts), nodeUid, portId);
        } else {
            invalidField(b, label, value);
        }
    }

    private static boolean allStrings(JsonArray array) {
        for (JsonElement el : array) {
            if (!(el.isJsonPrimitive() && el.getAsJsonPrimitive().isString())) {
                return false;
            }
        }
        return true;
    }

    /** 语句源：string → 单段；string[] → 多段（顺序拼接）；其余 → INVALID_FIELD + 便签。 */
    private static List<String> statementSources(ImportGraphBuilder b, String label, JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            return List.of(value.getAsString());
        }
        if (value.isJsonArray()) {
            List<String> out = new ArrayList<>();
            for (JsonElement el : value.getAsJsonArray()) {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                    out.add(el.getAsString());
                } else {
                    invalidField(b, label + "[]", el);
                }
            }
            return out;
        }
        invalidField(b, label, value);
        return List.of();
    }

    /** 数组 → 元素列表；单 object（allowObject）/单 primitive → 单元素列表；其余 → INVALID_FIELD + 便签（null）。 */
    private static @Nullable List<JsonElement> arrayOrSingle(ImportGraphBuilder b, String label,
                                                             JsonElement value, boolean allowObject) {
        if (allowObject && value.isJsonObject()) {
            return List.of(value);
        }
        if (value.isJsonArray()) {
            List<JsonElement> out = new ArrayList<>();
            value.getAsJsonArray().forEach(out::add);
            return out;
        }
        if (value.isJsonPrimitive()) {
            return List.of(value);
        }
        invalidField(b, label, value);
        return null;
    }

    /** object 或 single-key object 数组 → object 列表；其余 → INVALID_FIELD + 便签（null）。 */
    private static @Nullable List<JsonObject> singleKeyObjects(ImportGraphBuilder b, String label,
                                                               JsonElement value) {
        if (value.isJsonObject()) {
            return List.of(value.getAsJsonObject());
        }
        if (value.isJsonArray()) {
            List<JsonObject> out = new ArrayList<>();
            for (JsonElement el : value.getAsJsonArray()) {
                if (el.isJsonObject()) {
                    out.add(el.getAsJsonObject());
                } else {
                    invalidField(b, label + "[]", el);
                }
            }
            return out;
        }
        invalidField(b, label, value);
        return null;
    }

    private static Map<String, JsonElement> stringOption(JsonObject obj, String key,
                                                         ImportGraphBuilder b, String label) {
        JsonElement v = obj.get(key);
        if (v == null) {
            return Map.of();
        }
        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
            return ImportGraphBuilder.opts(key, v.getAsString());
        }
        invalidField(b, label + "." + key, v);
        return Map.of();
    }

    private static void unknownFields(ImportGraphBuilder b, String label, JsonObject obj, Set<String> known) {
        for (String key : obj.keySet()) {
            if (!known.contains(key)) {
                b.warn(DecompileDiagnostics.UNKNOWN_FIELD,
                        "未知字段 " + label + "." + key + "，原文已保留在便签");
                b.sticky(key + " = " + obj.get(key));
            }
        }
    }

    private static void invalidField(ImportGraphBuilder b, String label, JsonElement value) {
        b.warn(DecompileDiagnostics.INVALID_FIELD,
                "字段 " + label + " 值类型非法，已跳过，原文保留在便签");
        b.sticky(label + " = " + value);
    }

    private static @Nullable JsonObject objOrNull(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
    }
}

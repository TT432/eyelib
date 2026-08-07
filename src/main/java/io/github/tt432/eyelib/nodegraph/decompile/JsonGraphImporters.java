package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.ColorValues;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.ShortNameOps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Bedrock JSON 资产 → 图库 导入器（规格 nodegraph-workbench §W2）：
 * 结构镜像 {@code ClientEntityAssembler} / {@code RenderControllerAssembler} /
 * {@code AnimationControllerAssembler} 的逆。
 *
 * <p>字段映射：
 * <ul>
 *   <li>ClientEntity：description.identifier → entity.root 选项；geometry/textures/materials/
 *       animations/animation_controllers 声明表 → ref.* 节点，建节点即接线到 entity.root
 *       对应声明端口（规格 D1：声明 = 连线）；
 *       scripts.initialize/pre_animation/parent_setup（string 或 string[]）→ exec 链；
 *       scale/scaleX/scaleY/scaleZ → 表达式槽（数值/布尔 → 内联常量，字符串 → molang 反编译连线）；
 *       animate → animate.entry（短名解析到声明表 ref.animation/ref.ac，未声明 → 补独立
 *       ref.animation + UNKNOWN_REFERENCE）；render_controllers → rc.condition_entry + ref.rc；</li>
 *   <li>RenderController：geometry → 表达式槽（典型 {@code geometry.x} → ref.geometry 连线）；
 *       textures → list.entry；materials/part_visibility（object 或 single-key object 数组）
 *       → material.entry / part_visibility.entry；arrays → rc.root TEXT 选项（原文）；
 *       ignore_lighting → 布尔选项；color/is_hurt_color/on_fire_color/overlay_color
 *       → const.color（全数值且 8bit 精确）或 color.compose 接 COLOR 端口；</li>
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

    /** client_entity 文件 JSON（含 minecraft:client_entity 包装）→ CLIENT_ENTITY 库（RC 全部为外部引用）。 */
    public static ImportResult importClientEntity(JsonObject fileJson) {
        return importClientEntity(fileJson, id -> Optional.empty());
    }

    /**
     * v4 内联导入（规格 nodegraph-inline-render-controller §6）：{@code rcResolver}
     * 按 RC id 解析 render_controllers 文件形态文档；命中 → rc.root 内联反编译进同一主图
     * （controller/condition 接线齐全）；未命中 → ref.rc 外部引用 + {@link DecompileDiagnostics#RC_INLINE_MISS}。
     * geo/tex/mat 声明表不再建 entity 级 ref：先入 KnownTables 供 RC 表达式裸短名回填，
     * 表内未被任何 RC 字段实体化的条目凾底挂第一个 RC 锚点的声明端口。
     */
    public static ImportResult importClientEntity(JsonObject fileJson,
                                                  Function<String, Optional<JsonObject>> rcResolver) {
        ImportGraphBuilder b = new ImportGraphBuilder();
        JsonObject wrapper = objOrNull(fileJson, "minecraft:client_entity");
        JsonObject desc = wrapper == null ? null : objOrNull(wrapper, "description");
        if (desc == null) {
            b.error(DecompileDiagnostics.MISSING_ENTRY, "缺少 minecraft:client_entity.description");
            b.addRoot(NodeTypes.ENTITY_ROOT.id(), Map.of());
            return b.build(GraphKind.CLIENT_ENTITY);
        }

        b.addRoot(NodeTypes.ENTITY_ROOT.id(), stringOption(desc, "identifier", b, "description"));

        // geo/tex/mat 声明表 → 纯数据（v4：锚点在 RC，不再建 entity 级 ref）
        Map<String, String> geometryTable = parseRefTable(b, desc, "geometry");
        Map<String, String> textureTable = parseRefTable(b, desc, "textures");
        Map<String, String> materialTable = parseRefTable(b, desc, "materials");
        // 动画/AC 声明表 → entity.root 端口（v4 保留实体级：双消费端）
        Map<String, String> animationRefs = new LinkedHashMap<>();
        Map<String, String> acRefs = new LinkedHashMap<>();
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

        // render_controllers 与 render_controller_conditions 按 RC id 合并（map 条目优先，
        // 与运行时合并语义一致——同 id 出现两次会导致运行时同一 RC 渲染两遍）。
        Map<String, JsonElement> conditionMap = new LinkedHashMap<>();
        JsonElement rcc = desc.get("render_controller_conditions");
        if (rcc != null) {
            if (rcc.isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : rcc.getAsJsonObject().entrySet()) {
                    conditionMap.put(e.getKey(), e.getValue());
                }
            } else {
                invalidField(b, "description.render_controller_conditions", rcc);
            }
        }
        List<String> anchorUids = new ArrayList<>();
        JsonElement rcs = desc.get("render_controllers");
        if (rcs != null) {
            if (rcs.isJsonArray()) {
                for (JsonElement entry : rcs.getAsJsonArray()) {
                    importRenderControllerRef(b, entry, conditionMap, rcResolver, anchorUids);
                }
            } else {
                invalidField(b, "description.render_controllers", rcs);
            }
        }
        // 只在 map 中出现的 id（数组未列出）：按 map 条件补条目
        for (Map.Entry<String, JsonElement> e : conditionMap.entrySet()) {
            anchorUids.add(mountRenderController(b, e.getKey(), e.getValue(),
                    "render_controller_conditions." + e.getKey(), rcResolver));
        }

        // 凾底：表内未被 RC 字段实体化的条目 → ref 挂第一个 RC 锚点的声明端口（无锚点 → 不接线）
        String firstAnchor = anchorUids.stream().min(String::compareTo).orElse(null);
        attachLeftoverEntries(b, geometryTable, NodeTypes.REF_GEOMETRY.id(), "identifier", "geo",
                "decl_geometries", firstAnchor);
        attachLeftoverEntries(b, textureTable, NodeTypes.REF_TEXTURE.id(), "path", "tex",
                "decl_textures", firstAnchor);
        attachLeftoverEntries(b, materialTable, NodeTypes.REF_MATERIAL.id(), "material", "mat",
                "decl_materials", firstAnchor);

        unknownFields(b, "description", desc, ENTITY_DESC_KEYS);
        unknownFields(b, "<file>", fileJson, ENTITY_TOP_KEYS);

        // KnownTables = 实体自身声明表：RC 表达式裸短名 ref 回填标识符（显式短名保留）
        ShortNameOps.KnownTables.Builder known = new ShortNameOps.KnownTables.Builder();
        geometryTable.forEach((k, v) -> known.put("ref.geometry", k, v));
        textureTable.forEach((k, v) -> known.put("ref.texture", k, v));
        materialTable.forEach((k, v) -> known.put("ref.material", k, v));
        return backfilled(b.build(GraphKind.CLIENT_ENTITY), known.build());
    }

    /**
     * render_controllers 条目：纯字符串（condition 恒 1）或 {identifier: condition}。
     * {@code conditionOverrides}（render_controller_conditions map）按 id 优先覆盖内联条件；
     * 命中即 remove（调用方据剩余键补「只在 map 出现」的条目）。
     */
    private static void importRenderControllerRef(ImportGraphBuilder b, JsonElement entry,
                                                  Map<String, JsonElement> conditionOverrides,
                                                  Function<String, Optional<JsonObject>> rcResolver,
                                                  List<String> anchorUids) {
        if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
            String id = entry.getAsString();
            JsonElement override = conditionOverrides.remove(id);
            anchorUids.add(mountRenderController(b, id, override,
                    "render_controller_conditions." + id, rcResolver));
        } else if (entry.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : entry.getAsJsonObject().entrySet()) {
                // map 优先于内联（与运行时合并语义一致）
                JsonElement effective = conditionOverrides.remove(e.getKey());
                anchorUids.add(mountRenderController(b, e.getKey(),
                        effective != null ? effective : e.getValue(),
                        (effective != null ? "render_controller_conditions." : "render_controllers.") + e.getKey(),
                        rcResolver));
            }
        } else {
            invalidField(b, "render_controllers[]", entry);
        }
    }

    /**
     * 挂载一个 RC：resolver 命中 → rc.root 内联反编译（controller → render_controllers，
     * 条件 → condition 端口）；未命中 → ref.rc 外部引用（同接线）。返回锚点 uid。
     */
    private static String mountRenderController(ImportGraphBuilder b, String rcId,
                                                @Nullable JsonElement condition, String conditionLabel,
                                                Function<String, Optional<JsonObject>> rcResolver) {
        JsonObject entry = rcResolver.apply(rcId)
                .map(doc -> objOrNull(doc, "render_controllers"))
                .map(controllers -> objOrNull(controllers, rcId))
                .orElse(null);
        String anchorUid;
        if (entry != null) {
            anchorUid = b.addNode("rcr", NodeTypes.RC_ROOT.id(), rcRootOptions(b, entry, rcId));
            importRcEntry(b, anchorUid, entry, rcId);
            b.wire(anchorUid, "controller", "root", "render_controllers");
        } else {
            anchorUid = b.addNode("rc", NodeTypes.REF_RC.id(),
                    ImportGraphBuilder.opts("identifier", rcId));
            b.wire(anchorUid, "ref", "root", "render_controllers");
            b.warn(DecompileDiagnostics.RC_INLINE_MISS,
                    "RenderController '" + rcId + "' 文档未找到，以外部引用 ref.rc 导入");
        }
        if (condition != null) {
            valueSlot(b, anchorUid, "condition", conditionLabel, condition);
        }
        return anchorUid;
    }

    /**
     * 表内未被 RC 字段实体化的条目 → 补 ref 节点。
     * v6：锚点 ref.rc → 接 decl_* 声明端口（不变）；锚点 rc.root → 不接线
     * （协议短名由 DeclarationTables carve-out 覆盖；其余行不进表 + INFO 诊断）。
     */
    private static void attachLeftoverEntries(ImportGraphBuilder b, Map<String, String> table,
                                              String refType, String valueOption, String uidPrefix,
                                              String declPort, @Nullable String firstAnchor) {
        boolean anchorIsRefRc = firstAnchor != null && "ref.rc".equals(b.typeOf(firstAnchor));
        for (Map.Entry<String, String> e : table.entrySet()) {
            if (b.refReachesAnchor(refType, e.getKey())) {
                continue;
            }
            String uid = b.addNode(uidPrefix, refType, ImportGraphBuilder.opts(
                    "short_name", e.getKey(), valueOption, e.getValue()));
            if (firstAnchor == null) {
                continue;
            }
            if (anchorIsRefRc) {
                b.wire(uid, "ref", firstAnchor, declPort);
            } else if (!ShortNameOps.isProtocolShortName(refType, e.getKey())) {
                b.info(DecompileDiagnostics.DECL_LEFTOVER_DROPPED,
                        "声明表行 '" + e.getKey() + "' 未被任何 RC 字段引用且非协议短名，v6 起不进表"
                                + "（ref 节点已保留在画布，需要请接入 rc.root 的值端口）");
            }
        }
    }

    /** 声明表字段 → 短名到资产的纯数据映射（object 或 single-key object 数组）。 */
    private static Map<String, String> parseRefTable(ImportGraphBuilder b, JsonObject desc, String field) {
        Map<String, String> out = new LinkedHashMap<>();
        JsonElement table = desc.get(field);
        if (table == null) {
            return out;
        }
        List<JsonObject> objects = singleKeyObjects(b, "description." + field, table);
        if (objects == null) {
            return out;
        }
        for (JsonObject obj : objects) {
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                if (!e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isString()) {
                    invalidField(b, "description." + field + "." + e.getKey(), e.getValue());
                    continue;
                }
                out.putIfAbsent(e.getKey(), e.getValue().getAsString());
            }
        }
        return out;
    }

    /** 声明表（animations / animation_controllers）：ref 节点建后立即接线到 entity.root（v4 实体级）。 */
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
        String prefix = field.equals("animations") ? "anim" : "acref";
        String declarationPort = field.equals("animations") ? "animations" : "animation_controllers";
        for (JsonObject obj : objects) {
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                if (!e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isString()) {
                    invalidField(b, "description." + field + "." + e.getKey(), e.getValue());
                    continue;
                }
                String uid = b.addNode(prefix, refType, ImportGraphBuilder.opts(
                        "short_name", e.getKey(),
                        valueOption, e.getValue().getAsString()));
                b.wire(uid, "ref", "root", declarationPort);
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

    /**
     * animate / ac.state animations 条目：纯字符串（weight 缺省 1）或 {short_name: weight}。
     * 短名解析：animations 声明表 → ref.animation；animation_controllers 声明表 → ref.ac；
     * 未声明 → 补仅 short_name 的 ref.animation（warnIfUnresolved 时 + UNKNOWN_REFERENCE；
     * 不连线声明端口，规格 §2.5）。
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
        return importRenderController(fileJson, rcName, ShortNameOps.KnownTables.EMPTY);
    }

    /**
     * 同上传入已知短名表（规格 D4 跨文档关联）：{@code geometry.x} 等裸短名 ref
     * 命中表项时回填标识符（显式短名保留）。
     */
    public static ImportResult importRenderController(JsonObject fileJson, String rcName,
                                                      ShortNameOps.KnownTables known) {
        ImportGraphBuilder b = new ImportGraphBuilder();
        JsonObject controllers = objOrNull(fileJson, "render_controllers");
        JsonObject entry = controllers == null ? null : objOrNull(controllers, rcName);
        if (entry == null) {
            b.error(DecompileDiagnostics.MISSING_ENTRY,
                    "render_controllers 中找不到 '" + rcName + "'");
            b.addRoot(NodeTypes.RC_ROOT.id(), ImportGraphBuilder.opts("identifier", rcName));
            return b.build(GraphKind.RENDER_CONTROLLER);
        }

        b.addRoot(NodeTypes.RC_ROOT.id(), rcRootOptions(b, entry, rcName));
        importRcEntry(b, "root", entry, rcName);
        unknownFields(b, "<file>", fileJson, RC_TOP_KEYS);
        return backfilled(b.build(GraphKind.RENDER_CONTROLLER), known);
    }

    /** rc.root 选项：identifier + ignore_lighting（布尔）+ arrays（原文 JSON 文本）。 */
    private static Map<String, JsonElement> rcRootOptions(ImportGraphBuilder b, JsonObject entry, String rcName) {
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
        return rootOptions;
    }

    /** RC entry 反编译进指定 rc.root 节点（独立库 root 或实体内联锚点）。 */
    private static void importRcEntry(ImportGraphBuilder b, String rcUid, JsonObject entry, String rcName) {
        valueSlot(b, rcUid, "geometry", rcName + ".geometry", entry.get("geometry"));

        JsonElement textures = entry.get("textures");
        if (textures != null) {
            List<JsonElement> items = arrayOrSingle(b, rcName + ".textures", textures, false);
            if (items != null) {
                String prevUid = null;
                for (JsonElement item : items) {
                    String entryUid = b.addNode("le", NodeTypes.LIST_ENTRY.id(), Map.of());
                    valueSlot(b, entryUid, "value", rcName + ".textures[]", item);
                    // v6 链式：首条目接 rc.root，后续接前一条目的 next（数组序 = 链序）
                    if (prevUid == null) {
                        b.wire(entryUid, "entry", rcUid, "textures");
                    } else {
                        b.wire(entryUid, "entry", prevUid, "next");
                    }
                    prevUid = entryUid;
                }
            }
        }

        importPatternMap(b, rcUid, entry.get("materials"), rcName + ".materials",
                NodeTypes.MATERIAL_ENTRY.id(), "pattern", "value", "me", "materials");
        importPatternMap(b, rcUid, entry.get("part_visibility"), rcName + ".part_visibility",
                NodeTypes.PART_VISIBILITY_ENTRY.id(), "bone_pattern", "condition", "pve", "part_visibility");

        importColor(b, rcUid, entry, rcName, "color");
        importColor(b, rcUid, entry, rcName, "is_hurt_color");
        importColor(b, rcUid, entry, rcName, "on_fire_color");
        importColor(b, rcUid, entry, rcName, "overlay_color");

        unknownFields(b, rcName, entry, RC_ENTRY_KEYS);
    }

    /** materials / part_visibility：object 或 single-key object 数组 → 条目节点链（v6：数组序 = 链序）。 */
    private static void importPatternMap(ImportGraphBuilder b, String rcUid, @Nullable JsonElement value,
                                         String label, String entryType, String patternOption,
                                         String valuePort, String uidPrefix, String slotPort) {
        if (value == null) {
            return;
        }
        List<JsonObject> objects = singleKeyObjects(b, label, value);
        if (objects == null) {
            return;
        }
        String prevUid = null;
        for (JsonObject obj : objects) {
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                String entryUid = b.addNode(uidPrefix, entryType,
                        ImportGraphBuilder.opts(patternOption, e.getKey()));
                valueSlot(b, entryUid, valuePort, label + "." + e.getKey(), e.getValue());
                if (prevUid == null) {
                    b.wire(entryUid, "entry", rcUid, slotPort);
                } else {
                    b.wire(entryUid, "entry", prevUid, "next");
                }
                prevUid = entryUid;
            }
        }
    }

    /** 颜色组：{r,g,b,a} 全数值且 8bit 精确 → const.color 接 COLOR 端口；
     * 含表达式/非 8bit 精确数值 → color.compose（通道走表达式槽语义）接线。 */
    private static void importColor(ImportGraphBuilder b, String rcUid, JsonObject entry, String rcName,
                                    String field) {
        JsonElement color = entry.get(field);
        if (color == null) {
            return;
        }
        if (!color.isJsonObject()) {
            invalidField(b, rcName + "." + field, color);
            return;
        }
        JsonObject obj = color.getAsJsonObject();
        JsonElement[] ch = {obj.get("r"), obj.get("g"), obj.get("b"), obj.get("a")};
        boolean allByteExact = true;
        for (JsonElement c : ch) {
            if (c == null) {
                continue; // 缺省通道 → compose 端口默认 1（8bit 精确）
            }
            if (!(c.isJsonPrimitive() && c.getAsJsonPrimitive().isNumber()
                    && ColorValues.isByteExact(c.getAsDouble()))) {
                allByteExact = false;
                break;
            }
        }
        if (allByteExact) {
            float[] v = {1, 1, 1, 1};
            for (int i = 0; i < 4; i++) {
                if (ch[i] != null) {
                    v[i] = (float) ch[i].getAsDouble();
                }
            }
            String colorUid = b.addNode("color", NodeTypes.CONST_COLOR.id(),
                    ImportGraphBuilder.opts("value", ColorValues.toHex(v[0], v[1], v[2], v[3])));
            b.wire(colorUid, "out", rcUid, field);
        } else {
            String composeUid = b.addNode("color", NodeTypes.COLOR_COMPOSE.id(), Map.of());
            String[] names = {"r", "g", "b", "a"};
            for (int i = 0; i < 4; i++) {
                valueSlot(b, composeUid, names[i], rcName + "." + field + "." + names[i], ch[i]);
            }
            b.wire(composeUid, "out", rcUid, field);
        }
    }

    // ---------- AnimationController ----------

    /** animation controllers 文件 JSON + 目标 AC 名 → ANIMATION_CONTROLLER 库。 */
    public static ImportResult importAnimationControllers(JsonObject fileJson, String acName) {
        return importAnimationControllers(fileJson, acName, ShortNameOps.KnownTables.EMPTY);
    }

    /** 同上传入已知短名表（规格 D4）：state animations 裸短名 ref 命中表项时回填标识符。 */
    public static ImportResult importAnimationControllers(JsonObject fileJson, String acName,
                                                          ShortNameOps.KnownTables known) {
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
        return backfilled(b.build(GraphKind.ANIMATION_CONTROLLER), known);
    }

    /** 应用标识符补全并重组 ImportResult（表空时原样返回）。 */
    private static ImportResult backfilled(ImportResult result, ShortNameOps.KnownTables known) {
        var rewrite = ShortNameOps.backfillIdentifiers(result.library(), known);
        return rewrite.changed() == 0 ? result : new ImportResult(rewrite.library(), result.diagnostics());
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

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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/** ClientEntity 文档导入：description 全字段 → entity.root 与声明节点/脚本/RC 挂载。 */
final class ClientEntityImporter {
    private ClientEntityImporter() {
    }

    static final Set<String> ENTITY_TOP_KEYS = Set.of("format_version", "minecraft:client_entity");
    static final Set<String> ENTITY_DESC_KEYS = Set.of(
            "identifier", "scripts", "geometry", "textures", "materials",
            "animations", "animation_controllers", "render_controllers", "render_controller_conditions",
            "particle_effects", "sound_effects",
            // 建模为 entity.root 选项（规格 nodegraph-entity-description-fields）
            "enable_attachables", "held_item_ignores_lighting", "should_update_effects_offscreen",
            "spawn_egg");
    static final Set<String> ENTITY_SCRIPT_KEYS = Set.of(
            "initialize", "pre_animation", "parent_setup", "animate",
            "scale", "scaleX", "scaleY", "scaleZ");

    /** client_entity 文件 JSON（含 minecraft:client_entity 包装）→ CLIENT_ENTITY 库（RC 全部为外部引用）。 */
    static ImportResult importClientEntity(JsonObject fileJson) {
        return importClientEntity(fileJson, id -> Optional.empty());
    }

    /**
     * v4 内联导入（规格 nodegraph-inline-render-controller §6）：{@code rcResolver}
     * 按 RC id 解析 render_controllers 文件形态文档；命中 → rc.root 内联反编译进同一主图
     * （controller/condition 接线齐全）；未命中 → ref.rc 外部引用 + {@link DecompileDiagnostics#RC_INLINE_MISS}。
     * geo/tex/mat 声明表不再建 entity 级 ref：先入 KnownTables 供 RC 表达式裸短名回填，
     * 表内未被任何 RC 字段实体化的条目凾底挂第一个 RC 锚点的声明端口。
     */
    static ImportResult importClientEntity(JsonObject fileJson,
                                           Function<String, Optional<JsonObject>> rcResolver) {
        return importClientEntity(fileJson, rcResolver, false);
    }

    /** 同上，{@code inlineVariables=true} 时启用导入期变量内联（规格 nodegraph-import-inline-variables）。 */
    static ImportResult importClientEntity(JsonObject fileJson,
                                           Function<String, Optional<JsonObject>> rcResolver,
                                           boolean inlineVariables) {
        ImportGraphBuilder b = new ImportGraphBuilder();
        b.inlineVariables(inlineVariables);
        JsonObject wrapper = ImportValueOps.objOrNull(fileJson, "minecraft:client_entity");
        JsonObject desc = wrapper == null ? null : ImportValueOps.objOrNull(wrapper, "description");
        if (desc == null) {
            b.error(DecompileDiagnostics.MISSING_ENTRY, "缺少 minecraft:client_entity.description");
            b.addRoot(NodeTypes.ENTITY_ROOT.id(), Map.of());
            return b.build(GraphKind.CLIENT_ENTITY);
        }

        b.addRoot(NodeTypes.ENTITY_ROOT.id(), entityRootOptions(b, desc));

        // geo/tex/mat 声明表 → 纯数据（v4：锚点在 RC，不再建 entity 级 ref）
        Map<String, String> geometryTable = parseRefTable(b, desc, "geometry");
        Map<String, String> textureTable = parseRefTable(b, desc, "textures");
        Map<String, String> materialTable = parseRefTable(b, desc, "materials");
        // 动画/AC 声明表 → entity.root 端口（v4 保留实体级：双消费端）
        Map<String, String> animationRefs = new LinkedHashMap<>();
        Map<String, String> acRefs = new LinkedHashMap<>();
        importRefTable(b, desc, "animations", NodeTypes.REF_ANIMATION.id(), "identifier", animationRefs, acRefs);
        importRefTable(b, desc, "animation_controllers", NodeTypes.REF_AC.id(), "identifier", acRefs);
        // v10：粒子/音效声明表同机制（无 AC 归位特例，spec nodegraph-ac-graph-and-effects §2.2）
        importSimpleRefTable(b, desc, "particle_effects", "particle", NodeTypes.REF_PARTICLE.id(), "particles");
        importSimpleRefTable(b, desc, "sound_effects", "sound", NodeTypes.REF_SOUND.id(), "sounds");

        // scripts
        JsonElement scripts = desc.get("scripts");
        if (scripts != null) {
            if (scripts.isJsonObject()) {
                importEntityScripts(b, scripts.getAsJsonObject(), animationRefs, acRefs);
            } else {
                ImportValueOps.invalidField(b, "description.scripts", scripts);
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
                ImportValueOps.invalidField(b, "description.render_controller_conditions", rcc);
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
                ImportValueOps.invalidField(b, "description.render_controllers", rcs);
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

        ImportValueOps.unknownFields(b, "description", desc, ENTITY_DESC_KEYS);
        ImportValueOps.unknownFields(b, "<file>", fileJson, ENTITY_TOP_KEYS);

        // KnownTables = 实体自身声明表：RC 表达式裸短名 ref 回填标识符（显式短名保留）
        ShortNameOps.KnownTables.Builder known = new ShortNameOps.KnownTables.Builder();
        geometryTable.forEach((k, v) -> known.put("ref.geometry", k, v));
        textureTable.forEach((k, v) -> known.put("ref.texture", k, v));
        materialTable.forEach((k, v) -> known.put("ref.material", k, v));
        return ImportDocSupport.backfilled(b.build(GraphKind.CLIENT_ENTITY), known.build());
    }

    /**
     * render_controllers 条目：纯字符串（condition 恒 1）或 {identifier: condition}。
     * {@code conditionOverrides}（render_controller_conditions map）按 id 优先覆盖内联条件；
     * 命中即 remove（调用方据剩余键补「只在 map 出现」的条目）。
     */
    static void importRenderControllerRef(ImportGraphBuilder b, JsonElement entry,
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
            ImportValueOps.invalidField(b, "render_controllers[]", entry);
        }
    }

    /**
     * 挂载一个 RC：resolver 命中 → rc.root 内联反编译（controller → render_controllers，
     * 条件 → condition 端口）；未命中 → ref.rc 外部引用（同接线）。返回锚点 uid。
     */
    static String mountRenderController(ImportGraphBuilder b, String rcId,
                                        @Nullable JsonElement condition, String conditionLabel,
                                        Function<String, Optional<JsonObject>> rcResolver) {
        JsonObject entry = rcResolver.apply(rcId)
                .map(doc -> ImportValueOps.objOrNull(doc, "render_controllers"))
                .map(controllers -> ImportValueOps.objOrNull(controllers, rcId))
                .orElse(null);
        String anchorUid;
        if (entry != null) {
            anchorUid = b.addNode("rcr", NodeTypes.RC_ROOT.id(),
                    RenderControllerImporter.rcRootOptions(b, entry, rcId));
            RenderControllerImporter.importRcEntry(b, anchorUid, entry, rcId);
            b.wire(anchorUid, "controller", "root", "render_controllers");
        } else {
            anchorUid = b.addNode("rc", NodeTypes.REF_RC.id(),
                    ImportGraphBuilder.opts("identifier", rcId));
            b.wire(anchorUid, "ref", "root", "render_controllers");
            b.warn(DecompileDiagnostics.RC_INLINE_MISS,
                    "RenderController '" + rcId + "' 文档未找到，以外部引用 ref.rc 导入");
        }
        if (condition != null) {
            ImportValueOps.valueSlot(b, anchorUid, "condition", conditionLabel, condition);
        }
        return anchorUid;
    }

    /**
     * 表内未被 RC 字段实体化的条目 → 补 ref 节点。
     * v6：锚点 ref.rc → 接 decl_* 声明端口（不变）；锚点 rc.root → 不接线
     * （协议短名由 DeclarationTables carve-out 覆盖；其余行不进表 + INFO 诊断）。
     */
    static void attachLeftoverEntries(ImportGraphBuilder b, Map<String, String> table,
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
    static Map<String, String> parseRefTable(ImportGraphBuilder b, JsonObject desc, String field) {
        Map<String, String> out = new LinkedHashMap<>();
        JsonElement table = desc.get(field);
        if (table == null) {
            return out;
        }
        List<JsonObject> objects = ImportValueOps.singleKeyObjects(b, "description." + field, table);
        if (objects == null) {
            return out;
        }
        for (JsonObject obj : objects) {
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                if (!e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isString()) {
                    ImportValueOps.invalidField(b, "description." + field + "." + e.getKey(), e.getValue());
                    continue;
                }
                out.putIfAbsent(e.getKey(), e.getValue().getAsString());
            }
        }
        return out;
    }

    /** 声明表（animations / animation_controllers）：ref 节点建后立即接线到 entity.root（v4 实体级）。 */
    static void importRefTable(ImportGraphBuilder b, JsonObject desc, String field,
                               String refType, String valueOption,
                               @Nullable Map<String, String> refsOut) {
        importRefTable(b, desc, field, refType, valueOption, refsOut, null);
    }

    /** 简单声明表（v10 粒子/音效：无 AC 归位特例）：ref 节点建后立即接线到 entity.root 声明端口。 */
    static void importSimpleRefTable(ImportGraphBuilder b, JsonObject desc, String field,
                                     String nodePrefix, String refType, String declarationPort) {
        JsonElement table = desc.get(field);
        if (table == null) {
            return;
        }
        List<JsonObject> objects = ImportValueOps.singleKeyObjects(b, "description." + field, table);
        if (objects == null) {
            return;
        }
        for (JsonObject obj : objects) {
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                if (!e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isString()) {
                    ImportValueOps.invalidField(b, "description." + field + "." + e.getKey(), e.getValue());
                    continue;
                }
                String uid = b.addNode(nodePrefix, refType, ImportGraphBuilder.opts(
                        "short_name", e.getKey(),
                        "identifier", e.getValue().getAsString()));
                b.wire(uid, "ref", "root", declarationPort);
            }
        }
    }

    /**
     * @param acRefsOut 仅 animations 表：值为 {@code controller.animation.*} 的条目归位为
     *                  ref.ac + animation_controllers 端口（Bedrock 实体常把 AC 混声明在
     *                  animations 表；导出时两端口仍合写 animations 表，见 ClientEntityAssembler D6）
     */
    static void importRefTable(ImportGraphBuilder b, JsonObject desc, String field,
                               String refType, String valueOption,
                               @Nullable Map<String, String> refsOut,
                               @Nullable Map<String, String> acRefsOut) {
        JsonElement table = desc.get(field);
        if (table == null) {
            return;
        }
        List<JsonObject> objects = ImportValueOps.singleKeyObjects(b, "description." + field, table);
        if (objects == null) {
            return;
        }
        String declarationPort = field.equals("animations") ? "animations" : "animation_controllers";
        for (JsonObject obj : objects) {
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                if (!e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isString()) {
                    ImportValueOps.invalidField(b, "description." + field + "." + e.getKey(), e.getValue());
                    continue;
                }
                String value = e.getValue().getAsString();
                boolean acEntry = field.equals("animations") && value.startsWith("controller.animation");
                String uid = b.addNode(acEntry || !field.equals("animations") ? "acref" : "anim",
                        acEntry ? NodeTypes.REF_AC.id() : refType, ImportGraphBuilder.opts(
                        "short_name", e.getKey(),
                        valueOption, value));
                b.wire(uid, "ref", "root", acEntry ? "animation_controllers" : declarationPort);
                Map<String, String> out = acEntry && acRefsOut != null ? acRefsOut : refsOut;
                if (out != null) {
                    out.putIfAbsent(e.getKey(), uid);
                }
            }
        }
    }

    static void importEntityScripts(ImportGraphBuilder b, JsonObject scripts,
                                    Map<String, String> animationRefs,
                                    Map<String, String> acRefs) {
        // v13 事件模型：scripts 执行槽 → event.* 源节点 + 其 exec_out 引出的链
        for (String slot : List.of("initialize", "pre_animation", "parent_setup")) {
            JsonElement v = scripts.get(slot);
            if (v != null) {
                List<String> sources = ImportValueOps.statementSources(b, "scripts." + slot, v);
                if (!sources.isEmpty()) {
                    String eventUid = b.addNode("ev", "event." + slot, Map.of());
                    b.wireStatementsFrom(sources, eventUid, "exec_out");
                }
            }
        }
        JsonElement animate = scripts.get("animate");
        if (animate != null) {
            if (animate.isJsonArray()) {
                for (JsonElement entry : animate.getAsJsonArray()) {
                    ImportDocSupport.importAnimateEntry(b, "root", "animate", entry, animationRefs, acRefs, true);
                }
            } else {
                ImportValueOps.invalidField(b, "scripts.animate", animate);
            }
        }
        ImportValueOps.valueSlot(b, "root", "scale", "scripts.scale", scripts.get("scale"));
        ImportValueOps.valueSlot(b, "root", "scale_x", "scripts.scaleX", scripts.get("scaleX"));
        ImportValueOps.valueSlot(b, "root", "scale_y", "scripts.scaleY", scripts.get("scaleY"));
        ImportValueOps.valueSlot(b, "root", "scale_z", "scripts.scaleZ", scripts.get("scaleZ"));
        ImportValueOps.unknownFields(b, "scripts", scripts, ENTITY_SCRIPT_KEYS);
    }

    /** entity.root 选项映射（规格 nodegraph-entity-description-fields）：identifier + 标准
     * description 字段建模（enable_attachables/held_item_ignores_lighting/
     * should_update_effects_offscreen/spawn_egg）+ 未建模字段原文收集进 extra_fields。 */
    static Map<String, JsonElement> entityRootOptions(ImportGraphBuilder b, JsonObject desc) {
        Map<String, JsonElement> opts = new LinkedHashMap<>();
        opts.putAll(ImportValueOps.stringOption(desc, "identifier", b, "description"));
        ImportValueOps.boolOption(opts, desc, "enable_attachables", b);
        ImportValueOps.boolOption(opts, desc, "held_item_ignores_lighting", b);
        ImportValueOps.boolOption(opts, desc, "should_update_effects_offscreen", b);
        JsonElement egg = desc.get("spawn_egg");
        if (egg != null) {
            if (egg.isJsonObject()) {
                JsonObject eggObj = egg.getAsJsonObject();
                JsonElement tex = eggObj.get("texture");
                if (tex != null && tex.isJsonPrimitive() && tex.getAsJsonPrimitive().isString()) {
                    opts.put("spawn_egg_texture", tex);
                }
                JsonElement idx = eggObj.get("texture_index");
                if (idx != null && idx.isJsonPrimitive() && idx.getAsJsonPrimitive().isNumber()) {
                    opts.put("spawn_egg_texture_index", new JsonPrimitive(idx.getAsInt()));
                }
                // 另一形态：base_color/overlay_color（#rrggbb）
                for (String colorKey : new String[]{"base_color", "overlay_color"}) {
                    JsonElement c = eggObj.get(colorKey);
                    if (c != null && c.isJsonPrimitive() && c.getAsJsonPrimitive().isString()) {
                        opts.put("spawn_egg_" + colorKey, c);
                    }
                }
            } else {
                ImportValueOps.invalidField(b, "description.spawn_egg", egg);
            }
        }
        // 原文直通：未建模键原样收进 extra_fields（构建时回写，规格 §直通）
        JsonObject extra = new JsonObject();
        for (String key : desc.keySet()) {
            if (!ENTITY_DESC_KEYS.contains(key)) {
                extra.add(key, desc.get(key));
            }
        }
        if (!extra.keySet().isEmpty()) {
            opts.put(NodeTypes.EXTRA_FIELDS_OPTION, new JsonPrimitive(extra.toString()));
        }
        // scripts 级直通：scripts 对象内未建模键（如 A&S 的 variables/should_update_effects_offscreen）
        JsonElement scripts = desc.get("scripts");
        if (scripts != null && scripts.isJsonObject()) {
            JsonObject extraScripts = new JsonObject();
            for (String key : scripts.getAsJsonObject().keySet()) {
                if (!ENTITY_SCRIPT_KEYS.contains(key)) {
                    extraScripts.add(key, scripts.getAsJsonObject().get(key));
                }
            }
            if (!extraScripts.keySet().isEmpty()) {
                opts.put(NodeTypes.EXTRA_SCRIPTS_OPTION, new JsonPrimitive(extraScripts.toString()));
            }
        }
        return opts;
    }
}

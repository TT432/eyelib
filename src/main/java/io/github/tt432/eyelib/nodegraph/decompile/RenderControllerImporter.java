package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.ColorValues;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.ShortNameOps;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** RenderController 文档导入：rc.root + 条目链 + 颜色组反编译（也为实体内联挂载复用）。 */
final class RenderControllerImporter {
    private RenderControllerImporter() {
    }

    static final Set<String> RC_TOP_KEYS = Set.of("format_version", "render_controllers");
    static final Set<String> RC_ENTRY_KEYS = Set.of(
            "geometry", "textures", "materials", "part_visibility", "arrays", "ignore_lighting",
            "color", "is_hurt_color", "on_fire_color", "overlay_color");

    /** render_controllers 文件 JSON + 目标 RC 名 → RENDER_CONTROLLER 库。 */
    static ImportResult importRenderController(JsonObject fileJson, String rcName) {
        return importRenderController(fileJson, rcName, ShortNameOps.KnownTables.EMPTY);
    }

    /**
     * 同上传入已知短名表（规格 D4 跨文档关联）：{@code geometry.x} 等裸短名 ref
     * 命中表项时回填标识符（显式短名保留）。
     */
    static ImportResult importRenderController(JsonObject fileJson, String rcName,
                                               ShortNameOps.KnownTables known) {
        return importRenderController(fileJson, rcName, known, false);
    }

    /** 同上，{@code inlineVariables=true} 时启用导入期变量内联。 */
    static ImportResult importRenderController(JsonObject fileJson, String rcName,
                                               ShortNameOps.KnownTables known,
                                               boolean inlineVariables) {
        ImportGraphBuilder b = new ImportGraphBuilder();
        b.inlineVariables(inlineVariables);
        JsonObject controllers = ImportValueOps.objOrNull(fileJson, "render_controllers");
        JsonObject entry = controllers == null ? null : ImportValueOps.objOrNull(controllers, rcName);
        if (entry == null) {
            b.error(DecompileDiagnostics.MISSING_ENTRY,
                    "render_controllers 中找不到 '" + rcName + "'");
            b.addRoot(NodeTypes.RC_ROOT.id(), ImportGraphBuilder.opts("identifier", rcName));
            return b.build(GraphKind.RENDER_CONTROLLER);
        }

        b.addRoot(NodeTypes.RC_ROOT.id(), rcRootOptions(b, entry, rcName));
        importRcEntry(b, "root", entry, rcName);
        ImportValueOps.unknownFields(b, "<file>", fileJson, RC_TOP_KEYS);
        return ImportDocSupport.backfilled(b.build(GraphKind.RENDER_CONTROLLER), known);
    }

    /** rc.root 选项：identifier + ignore_lighting（布尔）+ arrays（原文 JSON 文本）。 */
    static Map<String, JsonElement> rcRootOptions(ImportGraphBuilder b, JsonObject entry, String rcName) {
        Map<String, JsonElement> rootOptions = new LinkedHashMap<>();
        rootOptions.put("identifier", new JsonPrimitive(rcName));
        JsonElement ignoreLighting = entry.get("ignore_lighting");
        if (ignoreLighting instanceof JsonPrimitive p && p.isBoolean()) {
            rootOptions.put("ignore_lighting", p);
        } else if (ignoreLighting != null) {
            ImportValueOps.invalidField(b, rcName + ".ignore_lighting", ignoreLighting);
        }
        JsonElement arrays = entry.get("arrays");
        if (arrays != null) {
            if (arrays.isJsonObject()) {
                rootOptions.put("arrays", new JsonPrimitive(arrays.toString()));
            } else {
                ImportValueOps.invalidField(b, rcName + ".arrays", arrays);
            }
        }
        return rootOptions;
    }

    /** RC entry 反编译进指定 rc.root 节点（独立库 root 或实体内联锚点）。 */
    static void importRcEntry(ImportGraphBuilder b, String rcUid, JsonObject entry, String rcName) {
        ImportValueOps.valueSlot(b, rcUid, "geometry", rcName + ".geometry", entry.get("geometry"));

        JsonElement textures = entry.get("textures");
        if (textures != null) {
            List<JsonElement> items = ImportValueOps.arrayOrSingle(b, rcName + ".textures", textures, false);
            if (items != null) {
                String prevUid = null;
                for (JsonElement item : items) {
                    String entryUid = b.addNode("le", NodeTypes.LIST_ENTRY.id(), Map.of());
                    ImportValueOps.valueSlot(b, entryUid, "value", rcName + ".textures[]", item);
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

        ImportValueOps.unknownFields(b, rcName, entry, RC_ENTRY_KEYS);
    }

    /** materials / part_visibility：object 或 single-key object 数组 → 条目节点链（v6：数组序 = 链序）。 */
    static void importPatternMap(ImportGraphBuilder b, String rcUid, @Nullable JsonElement value,
                                 String label, String entryType, String patternOption,
                                 String valuePort, String uidPrefix, String slotPort) {
        if (value == null) {
            return;
        }
        List<JsonObject> objects = ImportValueOps.singleKeyObjects(b, label, value);
        if (objects == null) {
            return;
        }
        String prevUid = null;
        for (JsonObject obj : objects) {
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                String entryUid = b.addNode(uidPrefix, entryType,
                        ImportGraphBuilder.opts(patternOption, e.getKey()));
                ImportValueOps.valueSlot(b, entryUid, valuePort, label + "." + e.getKey(), e.getValue());
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
    static void importColor(ImportGraphBuilder b, String rcUid, JsonObject entry, String rcName,
                            String field) {
        JsonElement color = entry.get(field);
        if (color == null) {
            return;
        }
        if (!color.isJsonObject()) {
            ImportValueOps.invalidField(b, rcName + "." + field, color);
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
                ImportValueOps.valueSlot(b, composeUid, names[i], rcName + "." + field + "." + names[i], ch[i]);
            }
            b.wire(composeUid, "out", rcUid, field);
        }
    }
}

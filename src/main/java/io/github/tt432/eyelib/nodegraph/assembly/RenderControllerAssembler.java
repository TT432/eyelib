package io.github.tt432.eyelib.nodegraph.assembly;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RenderController 文档组装器（规格 §2.6/T6）：
 * kind=render_controller 的图库 → Bedrock render_controller 文档 JSON。
 *
 * <p>产出 {@code {"format_version":"1.8.0","render_controllers":{"<identifier>":{...}}}}。
 * entry 字段：geometry（恒输出）；textures（list.entry 的 value 表达式列表，uid 字典序）；
 * materials（material.entry → [{pattern: value 表达式}]）；part_visibility
 * （part_visibility.entry → [{bone_pattern: condition}]，同 pattern 后来者覆盖、去重保序）；
 * arrays（选项 JSON 文本解析原样放入，解析失败 → INVALID_ARRAYS）；ignore_lighting（布尔选项）；
 * color/is_hurt_color/on_fire_color/overlay_color（四通道全未连线且无常数则不输出，
 * 否则 {r,g,b,a} 全输出，无内容通道取端口默认，无默认回落 "1"——与 BrRcColor 缺省 1 同语义）。
 */
public final class RenderControllerAssembler {
    private RenderControllerAssembler() {
    }

    public static AssemblyResult assemble(GraphLibrary library) {
        JsonObject entry = new JsonObject();
        String identifier = "";
        List<Diagnostic> diagnostics = new ArrayList<>();
        GraphData main = library.graphs().get(library.main());
        if (main == null) {
            diagnostics.add(Diagnostic.error(AssemblySupport.MISSING_ROOT, "主图 '" + library.main() + "' 不存在"));
        } else {
            AssemblySupport.Ctx ctx = new AssemblySupport.Ctx(library, main);
            Optional<NodeInstance> root = AssemblySupport.findRoot(ctx, "rc.root");
            if (root.isPresent()) {
                NodeInstance rootNode = root.get();
                identifier = AssemblySupport.optionString(rootNode, NodeTypes.RC_ROOT, "identifier");
                assembleEntry(ctx, rootNode, entry);
            }
            diagnostics.addAll(ctx.diagnostics);
        }
        JsonObject controllers = new JsonObject();
        controllers.add(identifier, entry);
        JsonObject doc = new JsonObject();
        doc.addProperty("format_version", "1.8.0");
        doc.add("render_controllers", controllers);
        return new AssemblyResult(doc, diagnostics);
    }

    static void assembleEntry(AssemblySupport.Ctx ctx, NodeInstance root, JsonObject entry) {
        GraphData main = ctx.main;
        entry.addProperty("geometry", ctx.emitExpression(root.uid(), "geometry"));

        JsonArray textures = new JsonArray();
        for (NodeInstance e : AssemblySupport.wiredSources(main, root.uid(), "textures")) {
            textures.add(ctx.emitExpression(e.uid(), "value"));
        }
        if (textures.size() > 0) {
            entry.add("textures", textures);
        }

        JsonArray materials = new JsonArray();
        for (NodeInstance e : AssemblySupport.wiredSources(main, root.uid(), "materials")) {
            JsonObject obj = new JsonObject();
            obj.addProperty(AssemblySupport.optionString(e, NodeTypes.MATERIAL_ENTRY, "pattern"),
                    ctx.emitExpression(e.uid(), "value"));
            materials.add(obj);
        }
        if (materials.size() > 0) {
            entry.add("materials", materials);
        }

        // 同 pattern 后来者覆盖；LinkedHashMap 保持首现位置（去重保序），先按 pattern 归并再发射
        Map<String, NodeInstance> visibility = new LinkedHashMap<>();
        for (NodeInstance e : AssemblySupport.wiredSources(main, root.uid(), "part_visibility")) {
            visibility.put(AssemblySupport.optionString(e, NodeTypes.PART_VISIBILITY_ENTRY, "bone_pattern"), e);
        }
        if (!visibility.isEmpty()) {
            JsonArray pv = new JsonArray();
            visibility.forEach((pattern, e) -> {
                JsonObject obj = new JsonObject();
                obj.addProperty(pattern, ctx.emitExpression(e.uid(), "condition"));
                pv.add(obj);
            });
            entry.add("part_visibility", pv);
        }

        String arrays = AssemblySupport.optionString(root, NodeTypes.RC_ROOT, "arrays");
        if (!arrays.isBlank()) {
            try {
                JsonElement parsed = JsonParser.parseString(arrays);
                if (parsed.isJsonObject()) {
                    entry.add("arrays", parsed.getAsJsonObject());
                } else {
                    ctx.error(AssemblySupport.INVALID_ARRAYS,
                            "rc.root 的 arrays 选项不是 JSON 对象", root.uid());
                }
            } catch (JsonParseException e) {
                ctx.error(AssemblySupport.INVALID_ARRAYS,
                        "rc.root 的 arrays 选项不是合法 JSON：" + e.getMessage(), root.uid());
            }
        }

        entry.add("ignore_lighting", AssemblySupport.optionValue(root, NodeTypes.RC_ROOT, "ignore_lighting"));

        addColor(ctx, root, entry, "color");
        addColor(ctx, root, entry, "is_hurt_color");
        addColor(ctx, root, entry, "on_fire_color");
        addColor(ctx, root, entry, "overlay_color");
    }

    /** 颜色组：COLOR 端口未连线 → 不输出；已连线 → {r,g,b,a} 四通道全输出（规格 §2.4-11）。 */
    private static void addColor(AssemblySupport.Ctx ctx, NodeInstance root, JsonObject entry, String field) {
        var code = ctx.emitColor(root.uid(), field);
        if (code == null) {
            return;
        }
        JsonObject color = new JsonObject();
        color.addProperty("r", code.r());
        color.addProperty("g", code.g());
        color.addProperty("b", code.b());
        color.addProperty("a", code.a());
        entry.add(field, color);
    }
}

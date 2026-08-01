package io.github.tt432.eyelib.nodegraph.assembly;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ClientEntity 文档组装器（规格 §2.5/§2.6/T6）：
 * kind=client_entity 的图库 → Bedrock client_entity 文档 JSON。
 *
 * <p>产出 {@code {"format_version":"1.10.0","minecraft:client_entity":{"description":{...}}}}。
 * 主图恰 1 个 entity.root（验证器保证）。description 字段：
 * identifier；scripts（initialize/pre_animation/parent_setup EXEC 槽未连线则跳过，
 * scale/scaleX/scaleY/scaleZ 有连线或内联值才输出，animate 恒输出 weight 表达式）；
 * 声明表（geometry/textures/materials/animations/animation_controllers，
 * 从主图可达全部图收集 REF_* 节点，同 short_name 去重，非空才输出）；
 * render_controllers（condition 恒 "1" → 纯字符串，否则 {identifier: condition}）。
 */
public final class ClientEntityAssembler {
    private ClientEntityAssembler() {
    }

    public static AssemblyResult assemble(GraphLibrary library) {
        JsonObject description = new JsonObject();
        List<Diagnostic> diagnostics = new ArrayList<>();
        GraphData main = library.graphs().get(library.main());
        if (main == null) {
            diagnostics.add(Diagnostic.error(AssemblySupport.MISSING_ROOT, "主图 '" + library.main() + "' 不存在"));
        } else {
            AssemblySupport.Ctx ctx = new AssemblySupport.Ctx(library, main);
            Optional<NodeInstance> root = AssemblySupport.findRoot(ctx, "entity.root");
            if (root.isPresent()) {
                NodeInstance rootNode = root.get();
                description.addProperty("identifier",
                        AssemblySupport.optionString(rootNode, NodeTypes.ENTITY_ROOT, "identifier"));
                JsonObject scripts = assembleScripts(ctx, rootNode);
                if (!scripts.entrySet().isEmpty()) {
                    description.add("scripts", scripts);
                }
                addDeclarationTables(ctx, description);
                JsonArray renderControllers = assembleRenderControllers(ctx, rootNode);
                if (renderControllers.size() > 0) {
                    description.add("render_controllers", renderControllers);
                }
            }
            diagnostics.addAll(ctx.diagnostics);
        }
        JsonObject clientEntity = new JsonObject();
        clientEntity.add("description", description);
        JsonObject doc = new JsonObject();
        doc.addProperty("format_version", "1.10.0");
        doc.add("minecraft:client_entity", clientEntity);
        return new AssemblyResult(doc, diagnostics);
    }

    // ---------- scripts ----------

    private static JsonObject assembleScripts(AssemblySupport.Ctx ctx, NodeInstance root) {
        JsonObject scripts = new JsonObject();
        GraphData main = ctx.main;
        for (String slot : List.of("initialize", "pre_animation", "parent_setup")) {
            if (AssemblySupport.hasWire(main, root.uid(), slot)) {
                scripts.addProperty(slot, ctx.emitStatements(root.uid(), slot));
            }
        }
        JsonArray animate = assembleAnimate(ctx, root);
        if (animate.size() > 0) {
            scripts.add("animate", animate);
        }
        if (AssemblySupport.hasContent(main, root, "scale")) {
            scripts.addProperty("scale", ctx.emitExpression(root.uid(), "scale"));
        }
        for (List<String> pair : List.of(
                List.of("scale_x", "scaleX"), List.of("scale_y", "scaleY"), List.of("scale_z", "scaleZ"))) {
            if (AssemblySupport.hasContent(main, root, pair.get(0))) {
                scripts.addProperty(pair.get(1), ctx.emitExpression(root.uid(), pair.get(0)));
            }
        }
        return scripts;
    }

    /** scripts.animate：animate.entry（uid 字典序）→ [{short_name: weight 表达式}]。 */
    private static JsonArray assembleAnimate(AssemblySupport.Ctx ctx, NodeInstance root) {
        JsonArray animate = new JsonArray();
        for (NodeInstance entry : AssemblySupport.slotEntries(ctx.main, root.uid(), "animate")) {
            NodeInstance refNode = AssemblySupport.resolveEntryRef(ctx, entry, "ref");
            if (refNode == null) {
                continue;
            }
            String shortName = switch (refNode.type()) {
                case "ref.animation" -> AssemblySupport.optionString(refNode, NodeTypes.REF_ANIMATION, "short_name");
                case "ref.ac" -> AssemblySupport.optionString(refNode, NodeTypes.REF_AC, "short_name");
                default -> null;
            };
            if (shortName == null) {
                ctx.error(AssemblySupport.INVALID_ENTRY_REF,
                        "animate.entry 的 ref 槽只能连接 ref.animation / ref.ac，实际 " + refNode.type(), entry.uid());
                continue;
            }
            // weight 恒输出（恒 1 也不省略）；未连线时 codegen 回落端口默认 1
            JsonObject obj = new JsonObject();
            obj.addProperty(shortName, ctx.emitExpression(entry.uid(), "weight"));
            animate.add(obj);
        }
        return animate;
    }

    // ---------- 声明表 ----------

    /**
     * 声明表：从主图可达全部图（含子图，与 GraphValidator REF_CONFLICT 同范围）收集 REF_* 节点，
     * 按 uid 字典序、同 short_name 去重（保留先者；同短名不同标识符的冲突由验证器 REF_CONFLICT 报告）。
     */
    private static void addDeclarationTables(AssemblySupport.Ctx ctx, JsonObject description) {
        List<NodeInstance> nodes = new ArrayList<>();
        for (GraphData graph : AssemblySupport.reachableGraphs(ctx.library)) {
            nodes.addAll(graph.nodes());
        }
        nodes.sort(Comparator.comparing(NodeInstance::uid));
        Map<String, String> geometry = new LinkedHashMap<>();
        Map<String, String> textures = new LinkedHashMap<>();
        Map<String, String> materials = new LinkedHashMap<>();
        Map<String, String> animations = new LinkedHashMap<>();
        Map<String, String> controllers = new LinkedHashMap<>();
        for (NodeInstance node : nodes) {
            switch (node.type()) {
                case "ref.geometry" -> geometry.putIfAbsent(
                        AssemblySupport.optionString(node, NodeTypes.REF_GEOMETRY, "short_name"),
                        AssemblySupport.optionString(node, NodeTypes.REF_GEOMETRY, "identifier"));
                case "ref.texture" -> textures.putIfAbsent(
                        AssemblySupport.optionString(node, NodeTypes.REF_TEXTURE, "short_name"),
                        // 不带 .png，CODEC 层会补
                        AssemblySupport.optionString(node, NodeTypes.REF_TEXTURE, "path"));
                case "ref.material" -> materials.putIfAbsent(
                        AssemblySupport.optionString(node, NodeTypes.REF_MATERIAL, "short_name"),
                        AssemblySupport.optionString(node, NodeTypes.REF_MATERIAL, "material"));
                case "ref.animation" -> animations.putIfAbsent(
                        AssemblySupport.optionString(node, NodeTypes.REF_ANIMATION, "short_name"),
                        AssemblySupport.optionString(node, NodeTypes.REF_ANIMATION, "identifier"));
                case "ref.ac" -> controllers.putIfAbsent(
                        AssemblySupport.optionString(node, NodeTypes.REF_AC, "short_name"),
                        AssemblySupport.optionString(node, NodeTypes.REF_AC, "identifier"));
                default -> {
                }
            }
        }
        if (!geometry.isEmpty()) {
            description.add("geometry", toObject(geometry));
        }
        if (!textures.isEmpty()) {
            description.add("textures", toObject(textures));
        }
        if (!materials.isEmpty()) {
            description.add("materials", toObject(materials));
        }
        if (!animations.isEmpty()) {
            description.add("animations", toObject(animations));
        }
        if (!controllers.isEmpty()) {
            // Bedrock 惯例：单元素 map 的 list
            JsonArray list = new JsonArray();
            controllers.forEach((shortName, identifier) -> {
                JsonObject entry = new JsonObject();
                entry.addProperty(shortName, identifier);
                list.add(entry);
            });
            description.add("animation_controllers", list);
        }
    }

    private static JsonObject toObject(Map<String, String> table) {
        JsonObject obj = new JsonObject();
        table.forEach(obj::addProperty);
        return obj;
    }

    // ---------- render_controllers ----------

    /** rc.condition_entry（uid 字典序）：condition 恒 "1" → 纯字符串；否则 {identifier: condition}。 */
    private static JsonArray assembleRenderControllers(AssemblySupport.Ctx ctx, NodeInstance root) {
        JsonArray out = new JsonArray();
        for (NodeInstance entry : AssemblySupport.slotEntries(ctx.main, root.uid(), "render_controllers")) {
            NodeInstance refNode = AssemblySupport.resolveEntryRef(ctx, entry, "rc");
            if (refNode == null) {
                continue;
            }
            if (!refNode.type().equals("ref.rc")) {
                ctx.error(AssemblySupport.INVALID_ENTRY_REF,
                        "rc.condition_entry 的 rc 槽只能连接 ref.rc，实际 " + refNode.type(), entry.uid());
                continue;
            }
            String identifier = AssemblySupport.optionString(refNode, NodeTypes.REF_RC, "identifier");
            String condition = ctx.emitExpression(entry.uid(), "condition");
            if (condition.trim().equals("1")) {
                out.add(identifier);
            } else {
                JsonObject obj = new JsonObject();
                obj.addProperty(identifier, condition);
                out.add(obj);
            }
        }
        return out;
    }
}

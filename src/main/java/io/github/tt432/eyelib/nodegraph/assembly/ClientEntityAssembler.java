package io.github.tt432.eyelib.nodegraph.assembly;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.ShortNames;
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
 * = entity.root 五个声明端口的连线源节点，同 short_name 去重，非空才输出）；
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
                addDeclarationTables(ctx, rootNode, description);
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
        for (NodeInstance entry : AssemblySupport.wiredSources(ctx.main, root.uid(), "animate")) {
            NodeInstance refNode = AssemblySupport.resolveEntryRef(ctx, entry, "ref");
            if (refNode == null) {
                continue;
            }
            String shortName = switch (refNode.type()) {
                case "ref.animation" -> ShortNames.effective(refNode, NodeTypes.REF_ANIMATION);
                case "ref.ac" -> ShortNames.effective(refNode, NodeTypes.REF_AC);
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
     * 声明表 = entity.root 五个声明端口（geometries/textures/materials/animations/
     * animation_controllers）的连线源节点（规格 D1：声明 = 连线，全局扫描语义废止）。
     * 按 uid 字典序、同有效短名去重（putIfAbsent 保留先者；同短名不同标识符的冲突由验证器
     * REF_CONFLICT 报告）。端口源节点类型与该端口声明类别不匹配 → INVALID_DECLARATION_REF 并跳过。
     * 有效短名 = 显式 short_name 非空 ? 显式 : 派生（ShortNames.effective，规格 D1/D3）。
     * ref.ac 与 ref.animation 同发进 animations 表（规格 D6：Bedrock animate 命名空间两表合并，
     * eyelib 运行时只读 animations 表）。geometry/textures/materials 三表按 D2 发 default 别名。
     */
    private static void addDeclarationTables(AssemblySupport.Ctx ctx, NodeInstance root,
                                             JsonObject description) {
        Map<String, String> geometry = new LinkedHashMap<>();
        Map<String, String> textures = new LinkedHashMap<>();
        Map<String, String> materials = new LinkedHashMap<>();
        Map<String, String> animations = new LinkedHashMap<>();
        collectDeclaration(ctx, root, "geometries", "ref.geometry", geometry);
        collectDeclaration(ctx, root, "textures", "ref.texture", textures);
        collectDeclaration(ctx, root, "materials", "ref.material", materials);
        // D6：animations 与 animation_controllers 两端口汇入同一 animations 表；合并后重排 uid 字典序
        List<NodeInstance> animSources = new ArrayList<>();
        animSources.addAll(declarationSources(ctx, root, "animations", "ref.animation"));
        animSources.addAll(declarationSources(ctx, root, "animation_controllers", "ref.ac"));
        animSources.sort(Comparator.comparing(NodeInstance::uid));
        for (NodeInstance source : animSources) {
            putDeclaration(source, animations);
        }
        // D2：单资产表自动补 default 别名（保运行时回退与外部 RC 的 geometry.default 惯例）；
        // 多资产表不发（歧义，需要 default 的场景走显式覆盖）
        addDefaultAlias(geometry);
        addDefaultAlias(textures);
        addDefaultAlias(materials);
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
    }

    /** 单个声明端口：类型校验后按有效短名去重入表。 */
    private static void collectDeclaration(AssemblySupport.Ctx ctx, NodeInstance root, String port,
                                           String refType, Map<String, String> table) {
        for (NodeInstance source : declarationSources(ctx, root, port, refType)) {
            putDeclaration(source, table);
        }
    }

    /** 声明端口的连线源节点（uid 字典序）；类型不匹配 → INVALID_DECLARATION_REF 并剔除。 */
    private static List<NodeInstance> declarationSources(AssemblySupport.Ctx ctx, NodeInstance root,
                                                         String port, String refType) {
        List<NodeInstance> sources = AssemblySupport.wiredSources(ctx.main, root.uid(), port);
        List<NodeInstance> out = new ArrayList<>();
        for (NodeInstance source : sources) {
            if (!source.type().equals(refType)) {
                ctx.error(AssemblySupport.INVALID_DECLARATION_REF,
                        "声明端口 " + port + " 只接受 " + refType + "，实际连接 " + source.type(), source.uid());
                continue;
            }
            out.add(source);
        }
        return out;
    }

    private static void putDeclaration(NodeInstance source, Map<String, String> table) {
        NodeType type = NodeTypes.require(source.type());
        table.putIfAbsent(ShortNames.effective(source, type),
                AssemblySupport.optionString(source, type, ShortNames.valueOptionOf(source.type())));
    }

    /** 不同资产恰 1 个且无 default 键时，补 "default" 别名指向该资产。 */
    private static void addDefaultAlias(Map<String, String> table) {
        if (table.isEmpty() || table.containsKey("default")) {
            return;
        }
        java.util.Set<String> distinct = new java.util.HashSet<>(table.values());
        if (distinct.size() == 1) {
            table.put("default", distinct.iterator().next());
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
        for (NodeInstance entry : AssemblySupport.wiredSources(ctx.main, root.uid(), "render_controllers")) {
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

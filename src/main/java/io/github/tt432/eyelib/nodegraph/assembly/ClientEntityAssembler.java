package io.github.tt432.eyelib.nodegraph.assembly;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.DeclarationTables;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.MolangLiterals;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.ShortNames;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ClientEntity 文档组装器（规格 nodegraph-inline-render-controller §3）：
 * kind=client_entity 的图库 → Bedrock client_entity 文档 JSON。
 *
 * <p>产出 {@code {"format_version":"1.10.0","minecraft:client_entity":{"description":{...}}}}。
 * 主图恰 1 个 entity.root（验证器保证）。description 字段：
 * identifier；scripts（initialize/pre_animation/parent_setup EXEC 槽未连线则跳过，
 * scale/scaleX/scaleY/scaleZ 有连线或内联值才输出，animate 恒输出 weight 表达式）；
 * 声明表（geometry/textures/materials = {@link DeclarationTables} 从 RC 锚点派生；
 * animations = entity.root 两端口，ref.ac 同发进 animations 表）；
 * render_controllers（render_controllers 端口连线源：rc.root → 内联 RC，
 * 同时组装 RC 文档条目进 {@link AssemblyResult#extraDocs()}；ref.rc → 外部引用；
 * condition 恒 "1" → 纯字符串，否则 {identifier: condition}）。
 */
public final class ClientEntityAssembler {
    private ClientEntityAssembler() {
    }

    public static AssemblyResult assemble(GraphLibrary library) {
        JsonObject description = new JsonObject();
        List<Diagnostic> diagnostics = new ArrayList<>();
        Map<String, JsonObject> inlineControllers = new LinkedHashMap<>();
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
                JsonArray renderControllers = assembleRenderControllers(ctx, rootNode, inlineControllers);
                if (renderControllers.size() > 0) {
                    description.add("render_controllers", renderControllers);
                }
                // 标准 description 字段（非缺省才输出）+ 未建模字段直通补缺
                // （规格 nodegraph-entity-description-fields）
                emitDescriptionFields(rootNode, description);
            }
            diagnostics.addAll(ctx.diagnostics);
        }
        JsonObject clientEntity = new JsonObject();
        clientEntity.add("description", description);
        JsonObject doc = new JsonObject();
        doc.addProperty("format_version", "1.10.0");
        doc.add("minecraft:client_entity", clientEntity);
        List<JsonObject> extraDocs = inlineControllers.isEmpty() ? List.of() : List.of(renderControllerDoc(inlineControllers));
        return new AssemblyResult(doc, diagnostics, extraDocs);
    }

    /** 标准 description 字段发射（非缺省才输出）+ extra_fields 原文直通（只补缺键，不覆盖已组装键）。 */
    private static void emitDescriptionFields(NodeInstance root, JsonObject description) {
        emitBoolIfSet(root, description, "enable_attachables");
        emitBoolIfSet(root, description, "held_item_ignores_lighting");
        emitBoolIfSet(root, description, "should_update_effects_offscreen");
        String eggTexture = AssemblySupport.optionString(root, NodeTypes.ENTITY_ROOT, "spawn_egg_texture");
        String eggBase = AssemblySupport.optionString(root, NodeTypes.ENTITY_ROOT, "spawn_egg_base_color");
        String eggOverlay = AssemblySupport.optionString(root, NodeTypes.ENTITY_ROOT, "spawn_egg_overlay_color");
        if (!eggTexture.isEmpty() || !eggBase.isEmpty() || !eggOverlay.isEmpty()) {
            JsonObject egg = new JsonObject();
            if (!eggTexture.isEmpty()) {
                egg.addProperty("texture", eggTexture);
            }
            int eggIndex = AssemblySupport.optionValue(root, NodeTypes.ENTITY_ROOT, "spawn_egg_texture_index").getAsInt();
            if (eggIndex != 0) {
                egg.addProperty("texture_index", eggIndex);
            }
            if (!eggBase.isEmpty()) {
                egg.addProperty("base_color", eggBase);
            }
            if (!eggOverlay.isEmpty()) {
                egg.addProperty("overlay_color", eggOverlay);
            }
            description.add("spawn_egg", egg);
        }
        String extraText = AssemblySupport.optionString(root, NodeTypes.ENTITY_ROOT, NodeTypes.EXTRA_FIELDS_OPTION);
        if (!extraText.isEmpty()) {
            try {
                JsonObject extra = com.google.gson.JsonParser.parseString(extraText).getAsJsonObject();
                for (Map.Entry<String, com.google.gson.JsonElement> e : extra.entrySet()) {
                    if (!description.has(e.getKey())) {
                        description.add(e.getKey(), e.getValue());
                    }
                }
            } catch (RuntimeException ignored) {
                // extra_fields 文本非法 JSON：静默跳过（仅可能来自手改存档；导入路径只写合法 JSON）
            }
        }
    }

    private static void emitBoolIfSet(NodeInstance root, JsonObject description, String key) {
        if (AssemblySupport.optionValue(root, NodeTypes.ENTITY_ROOT, key).getAsBoolean()) {
            description.addProperty(key, true);
        }
    }

    // ---------- scripts ----------

    private static JsonObject assembleScripts(AssemblySupport.Ctx ctx, NodeInstance root) {
        JsonObject scripts = new JsonObject();
        GraphData main = ctx.main;
        // 变量声明默认值 → initialize 初始化语句（规格 nodegraph-variable-table §2.3）：
        // 仅 VARIABLE 作用域（temp 当次求值即清，初始化无意义），前置在用户语句之前
        // （用户 initialize 语句可覆盖初始化值）。
        List<String> varInits = new ArrayList<>();
        for (VariableDecl decl : main.variables()) {
            // OBJECT 声明跳过：对象无标量字面值默认值（根上写标量会遮蔽成员；
            // 成员默认值声明在成员变量上，如 qpptaw.r = 0）
            if (decl.scope() != VariableDecl.Scope.VARIABLE || decl.defaultValue().isEmpty()
                    || decl.type() == PortType.OBJECT) {
                continue;
            }
            String literal = MolangLiterals.literal(decl.defaultValue().get());
            if (literal != null) {
                varInits.add("variable." + decl.name() + " = " + literal);
            }
        }
        for (String slot : List.of("initialize", "pre_animation", "parent_setup")) {
            if (AssemblySupport.hasWire(main, root.uid(), slot)) {
                String statements = ctx.emitStatements(root.uid(), slot);
                if (slot.equals("initialize") && !varInits.isEmpty()) {
                    statements = String.join("; ", varInits) + "; " + statements;
                }
                scripts.addProperty(slot, statements);
            }
        }
        if (!varInits.isEmpty() && !scripts.has("initialize")) {
            scripts.addProperty("initialize", String.join("; ", varInits));
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
        // scripts 级原文直通（规格 nodegraph-entity-description-fields §2.2）：只补缺键
        String extraScripts = AssemblySupport.optionString(root, NodeTypes.ENTITY_ROOT, NodeTypes.EXTRA_SCRIPTS_OPTION);
        if (!extraScripts.isEmpty()) {
            try {
                JsonObject extra = com.google.gson.JsonParser.parseString(extraScripts).getAsJsonObject();
                for (Map.Entry<String, com.google.gson.JsonElement> e : extra.entrySet()) {
                    if (!scripts.has(e.getKey())) {
                        scripts.add(e.getKey(), e.getValue());
                    }
                }
            } catch (RuntimeException ignored) {
                // 同 extra_fields：仅手改存档可能触发
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
     * 声明表（v4 规格 §3.1）：geometry/textures/materials = {@link DeclarationTables}
     * 从主图 RC 锚点（rc.root / ref.rc）派生；animations = entity.root 的
     * animations / animation_controllers 两端口汇入同一表（D6：Bedrock animate
     * 命名空间两表合并，eyelib 运行时只读 animations 表）。
     * 声明端口源节点类别不匹配 → INVALID_DECLARATION_REF 并跳过。
     * geometry/textures/materials 三表按 D2 发 default 别名。
     */
    private static void addDeclarationTables(AssemblySupport.Ctx ctx, NodeInstance root,
                                             JsonObject description) {
        for (DeclarationTables.InvalidRef invalid : DeclarationTables.invalidDeclarationSources(ctx.main)) {
            ctx.error(AssemblySupport.INVALID_DECLARATION_REF,
                    "声明端口 " + invalid.port() + " 只接受对应类别的 ref，实际连接 "
                            + invalid.source().type(), invalid.source().uid());
        }
        DeclarationTables.Tables tables = DeclarationTables.collect(ctx.main);
        // D2：单资产表自动补 default 别名（保运行时回退与外部 RC 的 geometry.default 惯例）；
        // 多资产表不发（歧义，需要 default 的场景走显式覆盖）
        addDefaultAlias(tables.geometry());
        addDefaultAlias(tables.textures());
        addDefaultAlias(tables.materials());
        if (!tables.geometry().isEmpty()) {
            description.add("geometry", toObject(tables.geometry()));
        }
        if (!tables.textures().isEmpty()) {
            description.add("textures", toObject(tables.textures()));
        }
        if (!tables.materials().isEmpty()) {
            description.add("materials", toObject(tables.materials()));
        }
        // animations：entity.root 两端口汇入同一表；合并后重排 uid 字典序
        Map<String, String> animations = new LinkedHashMap<>();
        List<NodeInstance> animSources = new ArrayList<>();
        animSources.addAll(typedSources(ctx, root, "animations", "ref.animation"));
        animSources.addAll(typedSources(ctx, root, "animation_controllers", "ref.ac"));
        animSources.sort(Comparator.comparing(NodeInstance::uid));
        for (NodeInstance source : animSources) {
            putDeclaration(source, animations);
        }
        if (!animations.isEmpty()) {
            description.add("animations", toObject(animations));
        }
        // v10：粒子/音效声明表（spec nodegraph-ac-graph-and-effects §2.2，同 animations 机制）
        Map<String, String> particles = new LinkedHashMap<>();
        List<NodeInstance> particleSources = new ArrayList<>(typedSources(ctx, root, "particles", "ref.particle"));
        particleSources.sort(Comparator.comparing(NodeInstance::uid));
        for (NodeInstance source : particleSources) {
            putDeclaration(source, particles);
        }
        if (!particles.isEmpty()) {
            description.add("particle_effects", toObject(particles));
        }
        Map<String, String> sounds = new LinkedHashMap<>();
        List<NodeInstance> soundSources = new ArrayList<>(typedSources(ctx, root, "sounds", "ref.sound"));
        soundSources.sort(Comparator.comparing(NodeInstance::uid));
        for (NodeInstance source : soundSources) {
            putDeclaration(source, sounds);
        }
        if (!sounds.isEmpty()) {
            description.add("sound_effects", toObject(sounds));
        }
    }

    /** entity.root 动画声明端口的连线源（uid 字典序）；类型不匹配 → INVALID_DECLARATION_REF 并剔除。 */
    private static List<NodeInstance> typedSources(AssemblySupport.Ctx ctx, NodeInstance root,
                                                   String port, String refType) {
        List<NodeInstance> out = new ArrayList<>();
        for (NodeInstance source : AssemblySupport.wiredSources(ctx.main, root.uid(), port)) {
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
        var type = NodeTypes.require(source.type());
        // 无实例标识符（占位 ref）→ 空串入表：类型默认值是 example 占位，不应进入产物；
        // 与 DeclarationTables.putShortName 同口径，验证器 UNKNOWN_REFERENCE 先行提示
        String valueOption = ShortNames.valueOptionOf(source.type());
        table.putIfAbsent(ShortNames.effective(source, type),
                valueOption == null ? "" : source.option(valueOption, type)
                        .map(com.google.gson.JsonElement::getAsString).orElse(""));
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

    /**
     * render_controllers 端口连线源（uid 字典序，v4 规格 §3.2）：
     * rc.root → 内联 RC（组装文档条目进 {@code inlineControllers}）；
     * ref.rc → 外部引用。condition 恒 "1" → 纯字符串；否则 {identifier: condition}。
     */
    private static JsonArray assembleRenderControllers(AssemblySupport.Ctx ctx, NodeInstance root,
                                                       Map<String, JsonObject> inlineControllers) {
        JsonArray out = new JsonArray();
        for (NodeInstance source : AssemblySupport.wiredSources(ctx.main, root.uid(), "render_controllers")) {
            String identifier;
            switch (source.type()) {
                case "rc.root" -> {
                    identifier = AssemblySupport.optionString(source, NodeTypes.RC_ROOT, "identifier");
                    JsonObject entry = new JsonObject();
                    RenderControllerAssembler.assembleEntry(ctx, source, entry);
                    inlineControllers.put(identifier, entry);
                }
                case "ref.rc" ->
                        identifier = AssemblySupport.optionString(source, NodeTypes.REF_RC, "identifier");
                default -> {
                    ctx.error(AssemblySupport.INVALID_ENTRY_REF,
                            "render_controllers 只能连接 rc.root / ref.rc，实际 " + source.type(), source.uid());
                    continue;
                }
            }
            String condition = ctx.emitExpression(source.uid(), "condition");
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

    /** 内联 RC 的合并 render_controllers 文档（与独立 RC 库产物同构）。 */
    private static JsonObject renderControllerDoc(Map<String, JsonObject> controllers) {
        JsonObject map = new JsonObject();
        controllers.forEach(map::add);
        JsonObject doc = new JsonObject();
        doc.addProperty("format_version", "1.8.0");
        doc.add("render_controllers", map);
        return doc;
    }
}

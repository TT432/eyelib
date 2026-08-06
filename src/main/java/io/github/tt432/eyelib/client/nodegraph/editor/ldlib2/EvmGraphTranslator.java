package io.github.tt432.eyelib.client.nodegraph.editor.ldlib2;
//? if !legacy {
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.IGroupItemModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.SectionModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ConstantNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ICustomNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableScope;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.StickyNoteModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphInterface;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeOptionDef;
import io.github.tt432.eyelib.nodegraph.NodeType;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.Placemat;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.StickyNote;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 图文档 ↔ LDLib2 GraphModel 双向翻译（规格 D2/D5/D6）。
 *
 * <p>正向（打开）：{@link GraphLibrary} → 根 {@link EvmGraph}；命名子图 → local subgraph
 * （{@code createLocalSubgraphInstance} + {@code addLocalSubgraph}）；子图接口 ↔ INPUT/OUTPUT
 * 变量（SubgraphNodeModel 据其镜像端口）；subgraph.call ↔ {@link SubgraphNodeModel}（可潜入）；
 * 变量分组路径（a/b/c）↔ 嵌套 {@link GroupModel}；placemat 成员集合 → 几何包围盒
 * （LDLib2 PlacematModel 无成员列表，归属靠位置包含）。
 *
 * <p>反向（保存）：uid 取 {@code model.getUid().toString()}；载入时 uid 经 {@link #uidOf}
 * 确定性映射（合法 UUID 直通，否则 nameUUIDFromBytes），保证同一文档反复打开/保存 uid 稳定。
 * 选项/常量与类型默认值相等时不写回（保持文档干净）；翻译中的不可表示项降级为 warning 诊断。
 */
public final class EvmGraphTranslator {
    private EvmGraphTranslator() {
    }

    private static final float PLACEMAT_PAD = 40f;
    private static final float PLACEMAT_NODE_W = 220f;
    private static final float PLACEMAT_NODE_H = 120f;

    /** domain uid → 确定性 UUID（合法 UUID 字符串直通，否则 v3 哈希）。 */
    public static UUID uidOf(String domainUid) {
        try {
            return UUID.fromString(domainUid);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(("eyelib:" + domainUid).getBytes(StandardCharsets.UTF_8));
        }
    }

    // ==================== 正向：GraphLibrary → EvmGraph ====================

    public static EvmGraph toGraph(GraphLibrary library, List<Diagnostic> diags) {
        EvmGraph root = new EvmGraph();
        root.libraryKind = library.kind();
        root.mainGraphName = library.main();
        EvmGraph.LibraryContext ctx = new EvmGraph.LibraryContext();
        root.setContext(ctx);
        GraphModel rootModel = root.graphModel;

        Map<String, GraphModel> modelsByName = new LinkedHashMap<>();
        modelsByName.put(library.main(), rootModel);
        for (Map.Entry<String, GraphData> e : library.graphs().entrySet()) {
            if (e.getKey().equals(library.main())) continue;
            GraphModel sub = rootModel.createLocalSubgraphInstance();
            if (sub == null) {
                diags.add(Diagnostic.warning("EDITOR_TRANSLATE", "cannot create local subgraph for graph '" + e.getKey() + "'"));
                continue;
            }
            rootModel.addLocalSubgraph(sub);
            sub.setUid(uidOf("subgraph:" + e.getKey()));
            ctx.subgraphsByName.put(e.getKey(), sub);
            ctx.namesBySubgraphUid.put(sub.getUid(), e.getKey());
            modelsByName.put(e.getKey(), sub);
        }
        for (Map.Entry<String, GraphModel> e : modelsByName.entrySet()) {
            GraphData data = library.graphs().get(e.getKey());
            if (data != null) populateVariables(e.getValue(), e.getKey(), data, diags);
        }
        for (Map.Entry<String, GraphModel> e : modelsByName.entrySet()) {
            GraphData data = library.graphs().get(e.getKey());
            if (data != null) populateContent(e.getValue(), data, ctx, diags);
        }
        return root;
    }

    private static void populateVariables(GraphModel model, String graphName, GraphData data, List<Diagnostic> diags) {
        for (VariableDecl v : data.variables()) {
            createVariable(model, graphName, v.name(), v.type(), v.group(), v.defaultValue(),
                    ModifierFlags.NONE, VariableScope.LOCAL, diags);
        }
        data.graphInterface().ifPresent(iface -> {
            for (GraphInterface.Param p : iface.inputs()) {
                createVariable(model, graphName, p.name(), p.type(), Optional.empty(), p.defaultValue(),
                        ModifierFlags.READ, VariableScope.EXPOSED, diags);
            }
            GraphInterface.Param out = iface.output();
            createVariable(model, graphName, out.name(), out.type(), Optional.empty(), out.defaultValue(),
                    ModifierFlags.WRITE, VariableScope.EXPOSED, diags);
        });
    }

    private static void createVariable(GraphModel model, String graphName, String varName, PortType type,
                                       Optional<String> groupPath, Optional<JsonElement> defaultValue,
                                       ModifierFlags modifiers, VariableScope scope, List<Diagnostic> diags) {
        TypeHandle handle = EvmTypeHandles.toHandle(type);
        Constant constant = model.createConstantValue(handle);
        if (constant != null) {
            defaultValue.map(j -> EvmValues.portJsonToJava(j, type)).ifPresent(v -> {
                constant.setDefaultValue(v);
                constant.setValue(v);
            });
        }
        GroupModel group = groupPath.map(p -> ensureGroupPath(model, p)).orElse(null);
        model.createGraphVariableDeclaration(handle, varName, modifiers, scope, group,
                Integer.MAX_VALUE, constant, uidOf(graphName + "/var/" + varName), null);
    }

    /** 分组路径 a/b/c → 嵌套 GroupModel（缺则建），返回最内层组。 */
    private static GroupModel ensureGroupPath(GraphModel model, String path) {
        GroupModel container = model.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        if (container == null) {
            container = model.createSection(GraphModel.DEFAULT_SECTION_NAME);
        }
        for (String segment : path.split("/")) {
            if (segment.isEmpty()) continue;
            GroupModel next = null;
            for (IGroupItemModel item : container.getItems()) {
                if (item instanceof GroupModel g && !(item instanceof SectionModel) && segment.equals(g.getName())) {
                    next = g;
                    break;
                }
            }
            if (next == null) {
                next = model.createGroup(segment, null);
                container.insertItem(next, Integer.MAX_VALUE);
            }
            container = next;
        }
        return container;
    }

    private static void populateContent(GraphModel model, GraphData data,
                                        EvmGraph.LibraryContext ctx, List<Diagnostic> diags) {
        Map<String, AbstractNodeModel> nodesByUid = new LinkedHashMap<>();
        for (NodeInstance n : data.nodes()) {
            AbstractNodeModel nm = createNode(model, n, ctx, diags);
            if (nm != null) nodesByUid.put(n.uid(), nm);
        }
        for (Wire w : data.wires()) {
            PortModel from = resolvePort(nodesByUid, w.from(), PortDirection.OUTPUT, diags);
            PortModel to = resolvePort(nodesByUid, w.to(), PortDirection.INPUT, diags);
            if (from != null && to != null) {
                model.createWire(to, from);
            } else {
                diags.add(Diagnostic.warning("EDITOR_TRANSLATE",
                        "wire " + w.from() + " -> " + w.to() + " skipped: endpoint port unresolved"));
            }
        }
        for (Placemat p : data.placemats()) {
            createPlacemat(model, p, nodesByUid, diags);
        }
        for (StickyNote s : data.stickyNotes()) {
            StickyNoteModel sm = model.createStickyNote(new Vector2f(s.x(), s.y()));
            sm.setUid(uidOf(s.uid()));
            sm.setContent(s.text());
            sm.setSize(new Vector2f(s.width(), s.height()));
            sm.setColor(parseColor(s.color(), 0xFFFFEB3B));
        }
    }

    private static @Nullable AbstractNodeModel createNode(GraphModel model, NodeInstance n,
                                                          EvmGraph.LibraryContext ctx, List<Diagnostic> diags) {
        Vector2f pos = new Vector2f(n.x(), n.y());
        UUID uid = uidOf(n.uid());
        Optional<NodeType> typeOpt = NodeTypes.get(n.type());
        if (typeOpt.isEmpty()) {
            diags.add(Diagnostic.warning("EDITOR_TRANSLATE", "unknown node type '" + n.type() + "'", n.uid()));
            return null;
        }
        NodeType type = typeOpt.get();
        if (type.kind() == NodeType.Kind.VARIABLE) {
            return createVariableNode(model, n, uid, pos, diags);
        }
        if (type.kind() == NodeType.Kind.SUBGRAPH_CALL) {
            String subName = n.optionString("subgraph", "");
            GraphModel sub = ctx.subgraphsByName.get(subName);
            if (sub == null) {
                diags.add(Diagnostic.warning("EDITOR_TRANSLATE",
                        "subgraph.call target '" + subName + "' not in library; created as plain node", n.uid()));
                return createEvmNode(model, type, n, uid, pos, diags);
            }
            SubgraphNodeModel node = model.createNodeWithType(SubgraphNodeModel.class, subName, pos, uid,
                    m -> m.setLocalSubgraph(sub), null);
            for (VariableDeclarationModelBase var : sub.getGraphVariableModels()) {
                if (var == null || !var.isInput()) continue;
                JsonElement value = n.constants().get(var.getName());
                if (value == null) continue;
                Constant c = node.getInputConstantsById().get(var.getUid().toString());
                PortType portType = EvmTypeHandles.toPortType(var.getDataTypeHandle());
                Object java = EvmValues.portJsonToJava(value, portType != null ? portType : PortType.ANY);
                if (c != null && java != null) c.setValue(java);
            }
            return node;
        }
        return createEvmNode(model, type, n, uid, pos, diags);
    }

    /**
     * domain variable 节点 → LDLib2 {@link VariableNodeModel}：按 name（不带根，与
     * {@link VariableDecl#name()} 一致）解析黑板声明并绑定——重命名/改型由声明引用自动同步。
     * 黑板未声明时退回选项形态的 variable 节点保住画布内容（验证器报 UNDECLARED_VARIABLE）。
     */
    private static @Nullable AbstractNodeModel createVariableNode(GraphModel model, NodeInstance n,
                                                                  UUID uid, Vector2f pos, List<Diagnostic> diags) {
        String name = n.optionString("name", "");
        for (VariableDeclarationModelBase var : model.getGraphVariableModels()) {
            if (var != null && !var.isInputOrOutput() && var.getName().equals(name)) {
                return model.createVariableNode(var, pos, uid, null);
            }
        }
        return createEvmNode(model, NodeTypes.VARIABLE, n, uid, pos, diags);
    }

    private static @Nullable NodeModel createEvmNode(GraphModel model, NodeType type, NodeInstance n,
                                                     UUID uid, Vector2f pos, List<Diagnostic> diags) {
        Class<? extends EvmNodeBase> cls = EvmNodes.classOf(n.type());
        if (cls == null) {
            diags.add(Diagnostic.warning("EDITOR_TRANSLATE", "no LDLib2 node class for type '" + n.type() + "'", n.uid()));
            return null;
        }
        EvmNodeBase node;
        try {
            node = cls.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            diags.add(Diagnostic.warning("EDITOR_TRANSLATE", "cannot instantiate node class " + cls.getName(), n.uid()));
            return null;
        }
        NodeModel nm = model.createNodeWithType(CustomNodeModelImpl.class, "", pos, uid,
                m -> m.initCustomNode(node), null);
        boolean redefine = false;
        for (Map.Entry<String, JsonElement> e : n.options().entrySet()) {
            NodeOptionDef def = type.option(e.getKey()).orElse(null);
            Constant c = nm.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + e.getKey());
            Object value = def != null ? EvmValues.jsonToJava(e.getValue(), def.type()) : null;
            if (c != null && value != null) {
                c.setValue(value);
                redefine = true;
            }
        }
        if (redefine) {
            nm.defineNode();
        }
        for (Map.Entry<String, JsonElement> e : n.constants().entrySet()) {
            Constant c = nm.getInputConstantsById().get(e.getKey());
            if (c == null) {
                diags.add(Diagnostic.warning("EDITOR_TRANSLATE",
                        "constant '" + e.getKey() + "' has no matching input port", n.uid()));
                continue;
            }
            PortType pt = portTypeOf(nm, e.getKey());
            Object value = EvmValues.portJsonToJava(e.getValue(), pt);
            if (value != null) c.setValue(value);
        }
        return nm;
    }

    private static PortType portTypeOf(NodeModel nm, String portId) {
        PortModel port = nm.getInputsById().get(portId);
        PortType type = port != null ? EvmTypeHandles.toPortType(port.getDataTypeHandle()) : null;
        return type != null ? type : PortType.ANY;
    }

    /** 连线端点解析：普通节点按端口 id；SubgraphNodeModel 按子图变量名/result → 变量 uid 端口；
     *  VariableNodeModel 只有唯一主口（读=出/写=入），不看请求的端口 id。 */
    private static @Nullable PortModel resolvePort(Map<String, AbstractNodeModel> nodesByUid, PortRef ref,
                                                   PortDirection direction, List<Diagnostic> diags) {
        AbstractNodeModel node = nodesByUid.get(ref.node());
        if (!(node instanceof NodeModel nm)) {
            diags.add(Diagnostic.warning("EDITOR_TRANSLATE", "wire endpoint node '" + ref.node() + "' missing"));
            return null;
        }
        if (node instanceof VariableNodeModel varNode) {
            return direction == PortDirection.OUTPUT ? varNode.getOutputPort() : varNode.getInputPort();
        }
        if (node instanceof SubgraphNodeModel sub) {
            GraphModel target = sub.getSubgraphModel();
            if (target == null) return null;
            if (direction == PortDirection.OUTPUT) {
                for (VariableDeclarationModelBase var : target.getGraphVariableModels()) {
                    if (var != null && var.isOutput()) {
                        return nm.getOutputsById().get(var.getUid().toString());
                    }
                }
                return null;
            }
            for (VariableDeclarationModelBase var : target.getGraphVariableModels()) {
                if (var != null && var.isInput() && var.getName().equals(ref.port())) {
                    return nm.getInputsById().get(var.getUid().toString());
                }
            }
            return null;
        }
        return direction == PortDirection.OUTPUT
                ? nm.getOutputsById().get(ref.port())
                : nm.getInputsById().get(ref.port());
    }

    private static void createPlacemat(GraphModel model, Placemat p,
                                       Map<String, AbstractNodeModel> nodesByUid, List<Diagnostic> diags) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        int members = 0;
        for (String uid : p.nodeUids()) {
            AbstractNodeModel nm = nodesByUid.get(uid);
            if (nm == null) continue;
            Vector2f pos = nm.getPosition();
            minX = Math.min(minX, pos.x);
            minY = Math.min(minY, pos.y);
            maxX = Math.max(maxX, pos.x);
            maxY = Math.max(maxY, pos.y);
            members++;
        }
        if (members == 0) {
            diags.add(Diagnostic.warning("EDITOR_TRANSLATE", "placemat '" + p.title() + "' has no resolvable member nodes"));
            return;
        }
        PlacematModel pm = model.createPlacemat(p.title(),
                new Vector2f(minX - PLACEMAT_PAD, minY - PLACEMAT_PAD),
                new Vector2f(maxX - minX + PLACEMAT_PAD * 2 + PLACEMAT_NODE_W,
                        maxY - minY + PLACEMAT_PAD * 2 + PLACEMAT_NODE_H));
        pm.setUid(uidOf(p.uid()));
        pm.setColor(parseColor(p.color(), 0x90606AEE));
    }

    // ==================== 反向：EvmGraph → GraphLibrary ====================

    public static GraphLibrary toLibrary(EvmGraph root, List<Diagnostic> diags) {
        GraphModel rootModel = root.graphModel;
        EvmGraph.LibraryContext ctx = root.context();
        Map<String, GraphData> graphs = new LinkedHashMap<>();
        graphs.put(root.mainGraphName, graphDataOf(rootModel, false, ctx, diags));
        List<GraphModel> subs = rootModel.getLocalSubGraphs();
        if (subs != null) {
            for (GraphModel sub : subs) {
                if (sub == null) continue;
                String name = subgraphName(sub, ctx);
                graphs.put(name, graphDataOf(sub, true, ctx, diags));
            }
        }
        return new GraphLibrary(GraphLibrary.CURRENT_FORMAT_VERSION, root.libraryKind, root.mainGraphName, graphs);
    }

    private static String subgraphName(GraphModel sub, EvmGraph.@Nullable LibraryContext ctx) {
        if (ctx != null) {
            String name = ctx.namesBySubgraphUid.get(sub.getUid());
            if (name != null) return name;
            String generated;
            do {
                generated = "subgraph_" + (++ctx.unnamedSubgraphCounter);
            } while (ctx.subgraphsByName.containsKey(generated));
            ctx.subgraphsByName.put(generated, sub);
            ctx.namesBySubgraphUid.put(sub.getUid(), generated);
            return generated;
        }
        return "subgraph_" + sub.getUid().toString().substring(0, 8);
    }

    private static GraphData graphDataOf(GraphModel model, boolean isSubgraph,
                                         EvmGraph.@Nullable LibraryContext ctx, List<Diagnostic> diags) {
        List<VariableDecl> variables = new ArrayList<>();
        for (VariableDeclarationModelBase var : model.getGraphVariableModels()) {
            if (var == null || var.isInputOrOutput()) continue;
            PortType type = EvmTypeHandles.toPortType(var.getDataTypeHandle());
            Optional<JsonElement> def = Optional.ofNullable(var.getInitializationModel())
                    .map(c -> EvmValues.javaToJson(c.getValue()));
            variables.add(new VariableDecl(var.getName(), type != null ? type : PortType.ANY,
                    groupPathOf(var), def));
        }
        List<NodeInstance> nodes = new ArrayList<>();
        for (AbstractNodeModel nm : model.getNodeModels()) {
            if (nm == null) continue;
            NodeInstance inst = nodeInstanceOf(nm, ctx, diags);
            if (inst != null) nodes.add(inst);
        }
        List<Wire> wires = new ArrayList<>();
        for (WireModel w : model.getWireModels()) {
            if (w == null || w.getFromPort() == null || w.getToPort() == null) continue;
            PortRef from = endpointOf(w.getFromPort(), diags);
            PortRef to = endpointOf(w.getToPort(), diags);
            if (from != null && to != null) {
                wires.add(new Wire(from, to));
            }
        }
        List<Placemat> placemats = new ArrayList<>();
        for (PlacematModel p : model.getPlacematModels()) {
            if (p == null) continue;
            List<String> members = new ArrayList<>();
            for (AbstractNodeModel member : p.getContainedNodes(null)) {
                members.add(member.getUid().toString());
            }
            placemats.add(new Placemat(p.getUid().toString(), p.getName(), colorHex(p.getElementColor()), members));
        }
        List<StickyNote> notes = new ArrayList<>();
        for (StickyNoteModel s : model.getStickyNoteModels()) {
            if (s == null) continue;
            notes.add(new StickyNote(s.getUid().toString(), s.getContent(),
                    s.getPosition().x, s.getPosition().y, s.getSize().x, s.getSize().y,
                    colorHex(s.getElementColor())));
        }
        Optional<GraphInterface> iface = isSubgraph ? EvmGraph.interfaceOf(model) : Optional.empty();
        return new GraphData(nodes, wires, variables, placemats, notes, iface);
    }

    private static @Nullable NodeInstance nodeInstanceOf(AbstractNodeModel nm,
                                                         EvmGraph.@Nullable LibraryContext ctx,
                                                         List<Diagnostic> diags) {
        String uid = nm.getUid().toString();
        Vector2f pos = nm.getPosition();
        if (nm instanceof SubgraphNodeModel sub) {
            GraphModel target = sub.getSubgraphModel();
            String subName = target != null ? subgraphName(target, ctx) : "";
            Map<String, JsonElement> options = new LinkedHashMap<>();
            options.put("subgraph", new JsonPrimitive(subName));
            Map<String, JsonElement> constants = new LinkedHashMap<>();
            if (target != null) {
                for (VariableDeclarationModelBase var : target.getGraphVariableModels()) {
                    if (var == null || !var.isInput()) continue;
                    Constant c = sub.getInputConstantsById().get(var.getUid().toString());
                    if (c == null || java.util.Objects.equals(c.getValue(), c.getDefaultValue())) continue;
                    JsonElement j = EvmValues.javaToJson(c.getValue());
                    if (j != null) constants.put(var.getName(), j);
                }
            }
            return new NodeInstance(uid, "subgraph.call", pos.x, pos.y, options, constants);
        }
        if (nm instanceof VariableNodeModel varNode) {
            var decl = varNode.getVariableDeclarationModel();
            if (decl == null) {
                diags.add(Diagnostic.warning("EDITOR_TRANSLATE", "variable node without declaration skipped"));
                return null;
            }
            // variable 节点即变量本身：导出 name（不带根）；写身份靠 exec.set_var.target 连线表达
            Map<String, JsonElement> options = new LinkedHashMap<>();
            options.put("name", new JsonPrimitive(decl.getName()));
            return new NodeInstance(uid, "variable", pos.x, pos.y, options, Map.of());
        }
        if (nm instanceof ConstantNodeModel constNode) {
            Object value = constNode.getConstant() != null ? constNode.getConstant().getValue() : null;
            if (value instanceof Boolean b) {
                return new NodeInstance(uid, "const.bool", pos.x, pos.y,
                        Map.of("value", new JsonPrimitive(b)), Map.of());
            }
            if (value instanceof Number num) {
                return new NodeInstance(uid, "const.number", pos.x, pos.y,
                        Map.of("value", new JsonPrimitive(num)), Map.of());
            }
            if (value instanceof String s) {
                return new NodeInstance(uid, "const.string", pos.x, pos.y,
                        Map.of("value", new JsonPrimitive(s)), Map.of());
            }
            diags.add(Diagnostic.warning("EDITOR_TRANSLATE", "constant node of unsupported value type skipped", uid));
            return null;
        }
        if (nm instanceof ICustomNodeModel custom && custom.getNode() instanceof EvmNodeBase evm) {
            return evmNodeInstanceOf(nm, evm.type(), uid, pos, diags);
        }
        diags.add(Diagnostic.warning("EDITOR_TRANSLATE",
                "unsupported node model " + nm.getClass().getSimpleName() + " skipped", uid));
        return null;
    }

    private static NodeInstance evmNodeInstanceOf(AbstractNodeModel nm, NodeType type, String uid,
                                                  Vector2f pos, List<Diagnostic> diags) {
        NodeModel model = (NodeModel) nm;
        Map<String, JsonElement> options = new LinkedHashMap<>();
        for (NodeOptionDef def : type.options()) {
            Constant c = model.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + def.id());
            if (c == null) continue;
            JsonElement j = EvmValues.javaToJson(c.getValue());
            if (j != null) {
                options.put(def.id(), j);
            }
        }
        Map<String, JsonElement> constants = new LinkedHashMap<>();
        for (Map.Entry<String, Constant> e : model.getInputConstantsById().entrySet()) {
            String key = e.getKey();
            if (key.startsWith(NodeOption.PORT_ID_PREFIX)) continue;
            Constant c = e.getValue();
            if (java.util.Objects.equals(c.getValue(), c.getDefaultValue())) continue;
            JsonElement j = EvmValues.javaToJson(c.getValue());
            if (j != null) {
                constants.put(key, j);
            }
        }
        return new NodeInstance(uid, type.id(), pos.x, pos.y, options, constants);
    }

    /** 连线端点 → domain PortRef；不可表示（variable node 输入等）返回 null 并记诊断。 */
    private static @Nullable PortRef endpointOf(PortModel port, List<Diagnostic> diags) {
        var owner = port.getNodeModel();
        String nodeUid = owner.getUid().toString();
        if (owner instanceof SubgraphNodeModel sub) {
            if (port.getDirection() == PortDirection.OUTPUT) {
                return new PortRef(nodeUid, "result");
            }
            String raw = port.getPortId();
            if (raw.endsWith("-in")) raw = raw.substring(0, raw.length() - 3);
            GraphModel target = sub.getSubgraphModel();
            if (target != null) {
                for (VariableDeclarationModelBase var : target.getGraphVariableModels()) {
                    if (var != null && var.isInput() && var.getUid().toString().equals(raw)) {
                        return new PortRef(nodeUid, var.getName());
                    }
                }
            }
            diags.add(Diagnostic.warning("EDITOR_TRANSLATE",
                    "subgraph node input port '" + port.getPortId() + "' has no matching interface variable", nodeUid));
            return null;
        }
        if (owner instanceof VariableNodeModel) {
            if (port.getDirection() == PortDirection.OUTPUT) {
                return new PortRef(nodeUid, "out");
            }
            // 写入口仅子图接口 OUTPUT 变量（WRITE 修饰）才有；domain 用 subgraph.output 锚点表达赋值
            diags.add(Diagnostic.warning("EDITOR_TRANSLATE",
                    "wire into a variable node's input dropped (domain 无写入口形态，请用 subgraph.output 锚点)", nodeUid));
            return null;
        }
        if (owner instanceof ConstantNodeModel) {
            return new PortRef(nodeUid, "out");
        }
        return new PortRef(nodeUid, port.getPortId());
    }

    private static Optional<String> groupPathOf(VariableDeclarationModelBase var) {
        List<String> segments = new ArrayList<>();
        GroupModelBase parent = var.getParentGroup();
        while (parent != null && !(parent instanceof SectionModel)) {
            if (parent instanceof GroupModel group) {
                segments.add(0, group.getName());
            }
            parent = parent.getParentGroup();
        }
        return segments.isEmpty() ? Optional.empty() : Optional.of(String.join("/", segments));
    }

    // ==================== 颜色 ====================

    static int parseColor(String color, int fallback) {
        if (color == null || !color.startsWith("#")) return fallback;
        try {
            String hex = color.substring(1);
            if (hex.length() == 6) {
                return 0xFF000000 | (int) Long.parseLong(hex, 16);
            }
            if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }

    static String colorHex(int argb) {
        return String.format("#%08X", argb);
    }
}
//?}

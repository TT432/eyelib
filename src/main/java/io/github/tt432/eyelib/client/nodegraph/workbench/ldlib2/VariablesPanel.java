package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.SearchableSelector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.NodeCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.VariableDeclarationCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.PortElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableScope;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.nodegraph.GraphLibraryManager;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmGraph;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmTypeHandles;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmValues;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.InlineLiteral;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import io.github.tt432.eyelib.nodegraph.Wire;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 变量表面板（规格 nodegraph-variable-table §2.4）：当前图的变量声明表格视图——
 * 名字/类型/作用域/默认值/引用数五行内编辑 + 新建 + 筛选 + 单行删除。
 *
 * <p>作用域是 eyelib 域概念（molang temp/variable），LDLib2 变量模型无此字段，
 * 读写走 {@link EvmGraph.LibraryContext#variableScopes} 侧表（uid 键，保存时由
 * translator 回写域 VariableDecl）。子图接口变量（INPUT/OUTPUT）只读展示作用域
 * （in/out 标签），不提供删除（接口编辑走既有子图机制）。
 *
 * <p>每 tick 以「当前模型身份 + 变量名/类型/作用域摘要」为脏键增量重建；
 * 跟随潜入的子图（{@link GraphEditorView#getCurrentView()}）。
 */
final class VariablesPanel extends UIElement {
    /** 拖拽载体：把声明从变量表拖上画布（DRAG_END 时悬停画布则生成变量节点）。 */
    private record VariableDrag(VariableDeclarationModelBase decl) {
    }

    /** 双击判定窗口（毫秒）。 */
    private static final long DOUBLE_CLICK_MS = 350;

    /** 侧栏展开宽度（会话内保持，拖左缘调整）。 */
    private static int panelWidth = 320;
    private static final int MIN_WIDTH = 180;
    private static final int MAX_WIDTH = 640;

    /** 固定列宽（px）；名字列 flex(1) 吸收余量（实机实证：全固定宽在面板变窄时整体溢出压滚动条）。 */
    private static final int TYPE_W = 62;
    private static final int SCOPE_W = 44;
    private static final int DEFAULT_W = 56;
    private static final int READS_W = 18;
    private static final int WRITES_W = 18;
    private static final int DELETE_W = 16;
    /** 行内 7 列的 6 个 2px 间隙和。 */
    private static final int ROW_GAPS_W = 12;
    /** 滚动区相对面板内容宽的水平损耗：视口左右 inset 各 5 + 纵向滚动条 5（实机几何实测）。 */
    private static final int SCROLLER_CHROME_W = 15;

    private final GraphEditorView editorView;
    private final TextField filterField;
    private final ScrollerView table;
    private final UIElement headerRow;

    private String filterText = "";
    private boolean resizing = false;
    private float resizeStartX;
    private int resizeStartWidth;
    private @Nullable GraphModel lastModel;
    private String lastDirtyKey = "";

    VariablesPanel(GraphEditorView editorView) {
        this.editorView = editorView;
        layout(layout -> layout
                .width(panelWidth)
                .heightPercent(100)
                .paddingAll(4)
                .gapAll(2));
        WorkbenchWidgets.panelBackground(this);

        // 顶部：新建 + 筛选（变量/调试切换是外层标签页职责，见 Ldlib2Workbench）
        UIElement bar = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .height(14)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(2));
        Button addButton = new Button();
        addButton.setText(Component.literal("+ 新建"));
        addButton.textStyle(style -> style.fontSize(9));
        addButton.setOnClick(event -> createVariable());
        addButton.layout(layout -> layout.width(40).heightPercent(100));
        filterField = new TextField();
        filterField.textFieldStyle(style -> style.placeholder(Component.literal("筛选变量…")));
        filterField.setTextResponder(text -> {
            filterText = text.trim().toLowerCase();
            rebuild();
        });
        filterField.layout(layout -> layout.flex(1).heightPercent(100));
        bar.addChildren(addButton, filterField);

        // 表头。左右 padding 对齐滚动区：行内容比面板内容区右偏 5（视口 inset）、右缩 15
        // （inset 5 + 滚动条 5），表头不同步内缩会出现表头与数据列恒差 5px（实机截图实证）
        headerRow = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .height(12)
                        .flexDirection(FlexDirection.ROW)
                        .paddingLeft(5)
                        .paddingRight(SCROLLER_CHROME_W - 5)
                        .gapAll(2));
        headerRow.addChildren(
                headerCellFlex("名字"),
                headerCell("类型", TYPE_W),
                headerCell("作用域", SCOPE_W),
                headerCell("默认值", DEFAULT_W),
                headerCell("读", READS_W),
                headerCell("写", WRITES_W),
                headerCell("", DELETE_W));

        table = new ScrollerView();
        table.layout(layout -> layout.widthPercent(100).flex(1));

        addChildren(bar, headerRow, table);

        // 左缘拖拽调宽（规格 §2.8）：4px 手柄，拖动向左增宽
        UIElement resizeHandle = new UIElement()
                .layout(layout -> layout
                        .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                        .left(0).top(0).bottom(0).width(4));
        resizeHandle.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                resizing = true;
                resizeStartX = event.x;
                resizeStartWidth = panelWidth;
            }
        }, true);
        addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_MOVE, event -> {
            if (!resizing) {
                return;
            }
            if (!isMouseDown(0)) {
                resizing = false;
                return;
            }
            panelWidth = Math.max(MIN_WIDTH,
                    Math.min(MAX_WIDTH, resizeStartWidth + (int) (resizeStartX - event.x)));
            VariablesPanel.this.layout(layout -> layout.width(panelWidth));
            // 名字列是 flex(1)：拖宽后重排并重新截断名字文本
            rebuild();
        }, true);
        addChild(resizeHandle);
    }

    private static UIElement headerCell(String text, int width) {
        return WorkbenchWidgets.textLine(text, WorkbenchColors.DIM)
                .layout(layout -> layout.width(width).heightPercent(100));
    }

    /** 名字列表头：与数据行名字格同构（flex(1)），保证表头/数据列恒等宽。 */
    private static UIElement headerCellFlex(String text) {
        return WorkbenchWidgets.textLine(text, WorkbenchColors.DIM)
                .layout(layout -> layout.flex(1).minWidth(0).heightPercent(100));
    }

    /** 名字列当前可用像素宽（面板内容宽 − 滚动区损耗 − 固定列 − 间隙）。 */
    private static int nameColumnWidth() {
        return Math.max(24, panelWidth - 8 - SCROLLER_CHROME_W
                - (TYPE_W + SCOPE_W + DEFAULT_W + READS_W + WRITES_W + DELETE_W) - ROW_GAPS_W);
    }

    /**
     * 名字文本按列宽截断（Label 不裁剪，长名会溢出画进类型列——实机截图实证
     * 「actions_and_stuff」渗进类型下拉）。截断时悬浮提示全名。
     */
    private static String fitName(String name, int indentPx) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int avail = nameColumnWidth() - indentPx;
        if (font.width(name) <= avail) {
            return name;
        }
        return font.plainSubstrByWidth(name, Math.max(0, avail - font.width("…"))) + "…";
    }

    // ==================== 刷新 ====================

    @Override
    public void screenTick() {
        super.screenTick();
        GraphModel model = currentModel();
        if (model == null) {
            return;
        }
        String dirtyKey = computeDirtyKey(model);
        if (model != lastModel || !dirtyKey.equals(lastDirtyKey)) {
            lastModel = model;
            lastDirtyKey = dirtyKey;
            rebuild();
        }
    }

    /**
     * 脏键：模型身份 + 全部声明的名字/类型/修饰/作用域/默认值摘要。
     * 直调 {@link #rebuild()} 的入口（改名提交、删除）必须随后调 {@link #markRebuilt(GraphModel)}
     * 同步本键——否则 tick 的脏检查会跳过（如「改名→undo」键值净零时表格停在旧名）。
     */
    private String computeDirtyKey(GraphModel model) {
        StringBuilder key = new StringBuilder();
        for (VariableDeclarationModelBase var : model.getGraphVariableModels()) {
            if (var == null) continue;
            key.append(var.getName()).append(' ')
                    .append(var.getDataTypeHandle()).append(' ')
                    .append(var.getModifiers()).append(' ')
                    .append(scopeOf(var)).append(' ')
                    .append(defaultText(var)).append(';');
        }
        // 变量节点的连线数进键：接/拔线后引用计数列即时刷新
        int varWires = 0;
        for (var nodeModel : model.getNodeModels()) {
            if (nodeModel instanceof VariableNodeModel variableNode) {
                for (PortModel port : variableNode.getInputPorts()) {
                    varWires += model.getWiresForPort(port).size();
                }
                for (PortModel port : variableNode.getOutputPorts()) {
                    varWires += model.getWiresForPort(port).size();
                }
            }
        }
        return System.identityHashCode(model) + "|" + key + "|w" + varWires;
    }

    /** 直调 rebuild 后同步脏键（见 {@link #computeDirtyKey} 注释）。 */
    private void markRebuilt(GraphModel model) {
        lastModel = model;
        lastDirtyKey = computeDirtyKey(model);
    }

    /** 当前潜入位置的图模型（无编辑器/图时 null）。 */
    private @Nullable GraphModel currentModel() {
        var view = editorView.getCurrentView();
        if (view == null || !(view.getGraph() instanceof EvmGraph graph)) {
            return null;
        }
        return graph.graphModel;
    }

    /** 当前潜入位置的图视图（无编辑器/图时 null）。 */
    private @Nullable GraphView currentView() {
        var view = editorView.getCurrentView();
        return view != null && view.getGraph() instanceof EvmGraph ? view : null;
    }

    private EvmGraph.@Nullable LibraryContext currentContext() {
        var view = editorView.getCurrentView();
        if (view == null || !(view.getGraph() instanceof EvmGraph graph)) {
            return null;
        }
        return graph.context();
    }

    private VariableDecl.Scope scopeOf(VariableDeclarationModelBase var) {
        EvmGraph.LibraryContext ctx = currentContext();
        if (ctx == null) {
            return VariableDecl.Scope.VARIABLE;
        }
        return ctx.variableScopes.getOrDefault(var.getUid(), VariableDecl.Scope.VARIABLE);
    }

    /**
     * 读/写引用计数（返回 [读, 写]）：写入边（→variable.in / 活模型 INPUT 口）计写、
     * 读取边（variable.out→ / 活模型 OUTPUT 口）计读。当前潜入图取活模型（编辑即时反映）；
     * 同项目其余图库取域快照按名统计——variable.* 是实体级作用域，AC 图库里的读写与实体库
     * 同源（hizljo 实证 2026-08-09：AC transition 的读在实体库内不可见，单库计数会漏报）。
     */
    private int[] countReadWrite(GraphModel model, VariableDeclarationModelBase var) {
        int reads = 0;
        int writes = 0;
        for (var nodeModel : model.getNodeModels()) {
            if (!(nodeModel instanceof VariableNodeModel variableNode)
                    || variableNode.getVariableDeclarationModel() != var) {
                continue;
            }
            for (PortModel port : variableNode.getInputPorts()) {
                writes += model.getWiresForPort(port).size();
            }
            for (PortModel port : variableNode.getOutputPorts()) {
                reads += model.getWiresForPort(port).size();
            }
        }
        EvmGraph.LibraryContext ctx = currentContext();
        if (ctx == null) {
            return new int[]{reads, writes};
        }
        String key = ctx.libraryKey;
        if (key == null) {
            return new int[]{reads, writes};
        }
        io.github.tt432.eyelib.nodegraph.GraphLibrary currentLib = GraphLibraryManager.INSTANCE.get(key);
        if (currentLib == null) {
            return new int[]{reads, writes};
        }
        // 当前潜入图已由活模型统计，域侧跳过；库内其余图（子图）照常计入
        var view = editorView.getCurrentView();
        String displayedGraph = ctx.namesBySubgraphUid.get(model.getUid());
        if (displayedGraph == null && view != null && view.getGraph() instanceof EvmGraph graph) {
            displayedGraph = graph.mainGraphName;
        }
        for (var ge : currentLib.graphs().entrySet()) {
            if (ge.getKey().equals(displayedGraph)) {
                continue;
            }
            int[] rw = countReadWriteInGraph(ge.getValue(), var.getName());
            reads += rw[0];
            writes += rw[1];
        }
        // 闭包级：本库 ref.ac 引用的 AC 图库（按键直查，其次按 ac.root identifier 扫描）——
        // variable.* 是实体级作用域，AC 图里的读写与实体库同源（hizljo 实证 2026-08-09：
        // AC transition 的读在实体库内不可见，单库计数会漏报）
        java.util.Set<String> acIds = new java.util.LinkedHashSet<>();
        for (GraphData data : currentLib.graphs().values()) {
            for (NodeInstance n : data.nodes()) {
                if ("ref.ac".equals(n.type()) && n.options().containsKey("identifier")) {
                    acIds.add(n.options().get("identifier").getAsString());
                }
            }
        }
        for (String acId : acIds) {
            io.github.tt432.eyelib.nodegraph.GraphLibrary acLib = GraphLibraryManager.INSTANCE.get(acId);
            if (acLib == null) {
                acLib = findAcLibraryByIdentifier(acId);
            }
            if (acLib == null) {
                continue;
            }
            for (GraphData data : acLib.graphs().values()) {
                int[] rw = countReadWriteInGraph(data, var.getName());
                reads += rw[0];
                writes += rw[1];
            }
        }
        return new int[]{reads, writes};
    }

    /** 按 ac.root 的 identifier 选项在注册表里找 AC 图库（键不等于 id 时的回落）。 */
    private static io.github.tt432.eyelib.nodegraph.@Nullable GraphLibrary findAcLibraryByIdentifier(String acId) {
        for (var e : GraphLibraryManager.INSTANCE.snapshot().all().entrySet()) {
            for (GraphData data : e.getValue().graphs().values()) {
                for (NodeInstance n : data.nodes()) {
                    if ("ac.root".equals(n.type()) && n.options().containsKey("identifier")
                            && acId.equals(n.options().get("identifier").getAsString())) {
                        return e.getValue();
                    }
                }
            }
        }
        return null;
    }

    /** 域图内按名统计 variable 节点的读/写边（in 口入边=写，out 口出边=读）。 */
    private static int[] countReadWriteInGraph(GraphData data, String name) {
        int reads = 0;
        int writes = 0;
        for (NodeInstance n : data.nodes()) {
            if (!"variable".equals(n.type()) || !n.options().containsKey("name")
                    || !name.equals(n.options().get("name").getAsString())) {
                continue;
            }
            for (Wire w : data.wires()) {
                if (w.to().node().equals(n.uid()) && "in".equals(w.to().port())) {
                    writes++;
                } else if (w.from().node().equals(n.uid()) && "out".equals(w.from().port())) {
                    reads++;
                }
            }
        }
        return new int[]{reads, writes};
    }

    // ==================== 行构建 ====================

    /** 成员行每级缩进宽（px）：object 父声明的点分前缀成员缩进跟随（截图布局）。 */
    private static final int MEMBER_INDENT_W = 8;

    private void rebuild() {
        table.clearAllScrollViewChildren();
        GraphModel model = lastModel;
        if (model == null) {
            return;
        }
        List<VariableDeclarationModelBase> vars = new ArrayList<>();
        for (VariableDeclarationModelBase var : model.getGraphVariableModels()) {
            if (var == null) continue;
            if (!filterText.isEmpty() && !var.getName().toLowerCase().contains(filterText)) {
                continue;
            }
            vars.add(var);
        }
        if (!filterText.isEmpty()) {
            // 筛选态保持平铺（成员脱离父上下文，缩进无参照）
            for (VariableDeclarationModelBase var : vars) {
                table.addScrollViewChild(buildRow(model, var, 0));
            }
            return;
        }
        for (VariableDisplayGrouping.Row<VariableDeclarationModelBase> row : VariableDisplayGrouping.group(
                vars, VariableDeclarationModelBase::getName,
                v -> EvmTypeHandles.toPortType(v.getDataTypeHandle()) == PortType.OBJECT)) {
            table.addScrollViewChild(buildRow(model, row.item(), row.depth()));
        }
    }

    private UIElement buildRow(GraphModel model, VariableDeclarationModelBase var, int depth) {
        boolean interfaceVar = var.isInputOrOutput();
        UIElement row = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .height(14)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(2));

        // 名字：可拖动标签（拖上画布生成变量节点）+ 双击改名（接口变量只读）；
        // 成员行（object 声明的点分前缀成员）按深度缩进
        UIElement nameCell = buildNameCell(model, var, interfaceVar, depth * MEMBER_INDENT_W);
        if (depth > 0) {
            nameCell.layout(layout -> layout.paddingLeft(depth * MEMBER_INDENT_W));
        }

        // 类型（同内建黑板属性面板的直连语义）；候选显示用编辑器类型名而非 handle 原文
        SearchableSelector<TypeHandle> type = new SearchableSelector<>();
        type.setSearchTextProvider(VariablesPanel::typeDisplayName);
        List<TypeHandle> candidates = new ArrayList<>(model.getVariableSupportTypes());
        if (!candidates.contains(var.getDataTypeHandle())) {
            candidates.add(var.getDataTypeHandle());
        }
        type.setCandidates(candidates);
        type.setCandidateUIProvider(UIElementProvider.text(
                handle -> Component.literal(typeDisplayName(handle))));
        type.setSelected(var.getDataTypeHandle(), false);
        type.registerValueListener(handle -> {
            GraphView view = currentView();
            if (view != null && handle != null && handle != var.getDataTypeHandle()) {
                EvmUndo.push(view, model, currentContext(), "修改变量类型",
                        "var:type:" + var.getUid(), () -> {
                            var.setDataTypeHandle(handle);
                            // 声明类型是 variable 读出口的传播源——改型后重算透传节点
                            io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmTypePropagation.refresh(model);
                        });
            }
        });
        type.layout(layout -> layout.width(TYPE_W).heightPercent(100));

        // 作用域：接口变量只读；LOCAL 变量循环切换 temp/variable（写侧表）
        UIElement scopeCell;
        if (interfaceVar) {
            scopeCell = WorkbenchWidgets.textLine(var.isInput() ? "in" : "out", WorkbenchColors.DIM)
                    .layout(layout -> layout.width(SCOPE_W).heightPercent(100));
        } else {
            Button scopeButton = new Button();
            scopeButton.setText(Component.literal(scopeOf(var) == VariableDecl.Scope.TEMP ? "temp" : "variable"));
            scopeButton.textStyle(style -> style.fontSize(9));
            scopeButton.setOnClick(event -> {
                EvmGraph.LibraryContext ctx = currentContext();
                GraphView view = currentView();
                if (ctx == null || view == null) {
                    return;
                }
                VariableDecl.Scope next = scopeOf(var) == VariableDecl.Scope.TEMP
                        ? VariableDecl.Scope.VARIABLE : VariableDecl.Scope.TEMP;
                EvmUndo.push(view, model, ctx, "切换变量作用域", "var:scope:" + var.getUid(), () -> {
                    ctx.variableScopes.put(var.getUid(), next);
                    scopeButton.setText(Component.literal(next == VariableDecl.Scope.TEMP ? "temp" : "variable"));
                });
            });
            scopeButton.layout(layout -> layout.width(SCOPE_W).heightPercent(100));
            scopeCell = scopeButton;
        }

        // 默认值（行内字面值口径；写 initialization model）
        TextField defaultField = new TextField();
        defaultField.textFieldStyle(style -> style.fontSize(9));
        defaultField.setText(defaultText(var), false);
        defaultField.setTextResponder(text -> {
            GraphView view = currentView();
            if (view != null) {
                EvmUndo.push(view, model, currentContext(), "修改变量默认值",
                        "var:default:" + var.getUid(), () -> applyDefault(model, var, text));
            }
        });
        defaultField.layout(layout -> layout.width(DEFAULT_W).heightPercent(100));

        // 引用数（读/写分列，闭包级统计，见 countReadWrite）
        int[] refs = countReadWrite(model, var);
        UIElement readsLabel = WorkbenchWidgets.textLine(String.valueOf(refs[0]), WorkbenchColors.DIM)
                .layout(layout -> layout.width(READS_W).heightPercent(100));
        UIElement writesLabel = WorkbenchWidgets.textLine(String.valueOf(refs[1]), WorkbenchColors.DIM)
                .layout(layout -> layout.width(WRITES_W).heightPercent(100));

        // 点击行 → 图上高亮该变量的全部引用节点（用内建选择态呈现）
        row.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_DOWN, event -> {
            if (event.button != 0) {
                return;
            }
            GraphView view = currentView();
            if (view == null) {
                return;
            }
            view.clearAllSelected();
            for (var nodeModel : model.getNodeModels()) {
                if (nodeModel instanceof VariableNodeModel variableNode
                        && variableNode.getVariableDeclarationModel() == var) {
                    view.addSelected(variableNode);
                }
            }
        });

        row.addChildren(nameCell, type, scopeCell, defaultField, readsLabel, writesLabel);
        if (!interfaceVar) {
            Button delete = new Button();
            delete.setText(Component.literal("×"));
            delete.textStyle(style -> style.fontSize(9));
            delete.setOnClick(event -> {
                EvmGraph.LibraryContext ctx = currentContext();
                GraphView view = currentView();
                if (view == null) {
                    return;
                }
                EvmUndo.push(view, model, ctx, "删除变量", null, () -> {
                    if (ctx != null) {
                        ctx.variableScopes.remove(var.getUid());
                    }
                    model.deleteVariableDeclaration(var, true);
                });
                rebuild();
                markRebuilt(model);
            });
            delete.layout(layout -> layout.width(DELETE_W).heightPercent(100));
            row.addChild(delete);
        }
        return row;
    }

    private static String defaultText(VariableDeclarationModelBase var) {
        var init = var.getInitializationModel();
        if (init == null) {
            return "";
        }
        var json = EvmValues.javaToJson(init.getValue());
        return json == null ? "" : InlineLiteral.toText(json);
    }

    private static void applyDefault(GraphModel model, VariableDeclarationModelBase var, String text) {
        var init = var.getInitializationModel();
        if (init == null) {
            init = model.createConstantValue(var.getDataTypeHandle());
            if (init == null) {
                return;
            }
            var.setInitializationModel(init);
        }
        PortType portType = EvmTypeHandles.toPortType(var.getDataTypeHandle());
        Object value = EvmValues.portJsonToJava(InlineLiteral.parse(text),
                portType != null ? portType : PortType.ANY);
        if (value != null) {
            init.setValue(value);
        }
    }

    // ==================== 新建 ====================

    private void createVariable() {
        var view = editorView.getCurrentView();
        GraphModel model = currentModel();
        if (view == null || model == null) {
            return;
        }
        List<TypeHandle> types = model.getVariableSupportTypes();
        if (types.isEmpty()) {
            return;
        }
        String baseName = "new_var";
        String name = baseName;
        int serial = 1;
        while (findVariable(model, name) != null) {
            name = baseName + "_" + (++serial);
        }
        view.dispatchCommand(new VariableDeclarationCommands.CreateGraphVariableDeclarationCommand(
                name, VariableScope.LOCAL, model.getVariableDeclarationModelType(), TypeHandles.UNKNOWN,
                null, Integer.MAX_VALUE, ModifierFlags.NONE, null));
        VariableDeclarationModelBase created = findVariable(model, name);
        EvmGraph.LibraryContext ctx = currentContext();
        if (created != null && ctx != null) {
            ctx.variableScopes.put(created.getUid(), VariableDecl.Scope.VARIABLE);
        }
        rebuild();
    }

    // ==================== 名字单元格（拖拽 + 双击改名） ====================

    /**
     * 名字单元格：常态是可拖动标签（同 LDLib2 黑板行交互——按住移出即发起拖拽，
     * 落在画布上生成绑定该声明的变量节点）；双击切换为输入框改名，Enter/失焦提交。
     * 接口变量（in/out）不改名但可拖拽（绑定接口声明的变量节点合法）。
     */
    private UIElement buildNameCell(GraphModel model, VariableDeclarationModelBase var, boolean interfaceVar,
                                    int indentPx) {
        UIElement cell = new UIElement()
                .layout(layout -> layout.flex(1).minWidth(0).heightPercent(100));
        Label label = new Label();
        label.textStyle(style -> style.fontSize(9));
        String fitted = fitName(var.getName(), indentPx);
        label.setText(fitted);
        if (!fitted.equals(var.getName())) {
            label.getStyle().tooltips(Component.literal(var.getName()));
        }
        label.layout(layout -> layout.widthPercent(100).heightPercent(100));
        cell.addChild(label);

        // 拖拽上画布（同黑板行：MOUSE_DOWN 武装、按住移出发起、DRAG_END 悬停画布落点生成）
        final long[] downAt = {0};
        final long[] lastClickAt = {0};
        label.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button != 0) {
                return;
            }
            downAt[0] = System.currentTimeMillis();
            if (!interfaceVar && downAt[0] - lastClickAt[0] < DOUBLE_CLICK_MS) {
                enterRename(cell, model, var);
            }
            lastClickAt[0] = downAt[0];
        });
        label.addEventListener(UIEvents.MOUSE_LEAVE, event -> {
            if (downAt[0] != 0 && label.isMouseDown(0)) {
                label.startDrag(new VariableDrag(var), new TextTexture(var.getName()));
            }
            downAt[0] = 0;
        }, true);
        label.addEventListener(UIEvents.MOUSE_UP, event -> downAt[0] = 0);
        label.addEventListener(UIEvents.DRAG_END, event -> {
            if (!(event.dragHandler.getDraggingObject() instanceof VariableDrag dragged)) {
                return;
            }
            GraphView view = currentView();
            if (view == null || !editorView.graphView.isSelfOrChildHover()) {
                return;
            }
            Vector2f position = editorView.graphView.getContentViewContainer()
                    .worldToLocalLayoutOffset(new Vector2f(event.x, event.y));
            var command = new NodeCommands.CreateNodeCommand();
            var portTarget = event.target.getFirstAncestorOfType(PortElement.class);
            if (portTarget != null && portTarget.canAcceptDrop(dragged.decl())) {
                command.withNodeOnPort(dragged.decl(), portTarget.getModel(), position, null);
            } else {
                command.withNodeOnGraph(dragged.decl(), position, null);
            }
            // CreateNodeCommand 是 UndoableGraphCommand，经 historyStack 天然可撤销
            view.dispatchCommand(command);
        });
        return cell;
    }

    /** 双击进入改名：换入输入框并聚焦；Enter/失焦提交（空串或 ESC 视为取消）。 */
    private void enterRename(UIElement cell, GraphModel model, VariableDeclarationModelBase var) {
        cell.clearAllChildren();
        TextField edit = new TextField();
        edit.textFieldStyle(style -> style.fontSize(9));
        edit.setText(var.getName(), false);
        edit.layout(layout -> layout.widthPercent(100).heightPercent(100));
        cell.addChild(edit);
        edit.focus();
        edit.addEventListener(UIEvents.KEY_DOWN, event -> {
            if (event.keyCode == 257 || event.keyCode == 335) { // ENTER / NUMPAD ENTER
                commitRename(edit.getValue(), model, var);
            }
        });
        edit.addEventListener(UIEvents.BLUR, event -> commitRename(edit.getValue(), model, var));
    }

    /** 改名提交：非空且变化才入 undo 栈；随后整表重建（名字/引用数随动）。 */
    private void commitRename(String text, GraphModel model, VariableDeclarationModelBase var) {
        String newName = text == null ? "" : text.trim();
        GraphView view = currentView();
        if (view != null && !newName.isEmpty() && !newName.equals(var.getName())) {
            EvmUndo.push(view, model, currentContext(), "重命名变量",
                    "var:name:" + var.getUid(), () -> var.setName(newName));
        }
        rebuild();
        markRebuilt(model);
    }

    private static @Nullable VariableDeclarationModelBase findVariable(GraphModel model, String name) {
        for (VariableDeclarationModelBase var : model.getGraphVariableModels()) {
            if (var != null && var.getName().equals(name)) {
                return var;
            }
        }
        return null;
    }

    /** 类型列显示名（编辑器口径）：内置 OBJECT → object、UNKNOWN → unknown，其余走 domain 名。 */
    private static String typeDisplayName(@Nullable TypeHandle handle) {
        if (handle == null) {
            return "unknown";
        }
        if (handle.equals(com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles.OBJECT)) {
            return "object";
        }
        PortType portType = EvmTypeHandles.toPortType(handle);
        return portType != null ? portType.getSerializedName() : handle.toString();
    }
}
//?}

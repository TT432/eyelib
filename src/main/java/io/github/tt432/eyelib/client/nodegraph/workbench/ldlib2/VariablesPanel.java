package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.VariableDeclarationCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableScope;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmGraph;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmTypeHandles;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.EvmValues;
import io.github.tt432.eyelib.nodegraph.InlineLiteral;
import io.github.tt432.eyelib.nodegraph.PortType;
import io.github.tt432.eyelib.nodegraph.VariableDecl;
import net.minecraft.network.chat.Component;
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
    /** 侧栏展开宽度（会话内保持，拖左缘调整）。 */
    private static int panelWidth = 300;
    private static final int MIN_WIDTH = 180;
    private static final int MAX_WIDTH = 640;

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

        // 表头
        headerRow = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .height(12)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(2));
        headerRow.addChildren(
                headerCell("名字", 76),
                headerCell("类型", 62),
                headerCell("作用域", 44),
                headerCell("默认值", 56),
                headerCell("引用", 22),
                headerCell("", 16));

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
        }, true);
        addChild(resizeHandle);
    }

    private static UIElement headerCell(String text, int width) {
        return WorkbenchWidgets.textLine(text, WorkbenchColors.DIM)
                .layout(layout -> layout.width(width).heightPercent(100));
    }

    // ==================== 刷新 ====================

    @Override
    public void screenTick() {
        super.screenTick();
        GraphModel model = currentModel();
        if (model == null) {
            return;
        }
        StringBuilder key = new StringBuilder();
        for (VariableDeclarationModelBase var : model.getGraphVariableModels()) {
            if (var == null) continue;
            key.append(var.getName()).append(' ')
                    .append(var.getDataTypeHandle()).append(' ')
                    .append(var.getModifiers()).append(' ')
                    .append(scopeOf(var)).append(';');
        }
        String dirtyKey = System.identityHashCode(model) + "|" + key;
        if (model != lastModel || !dirtyKey.equals(lastDirtyKey)) {
            lastModel = model;
            lastDirtyKey = dirtyKey;
            rebuild();
        }
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

    // ==================== 行构建 ====================

    private void rebuild() {
        table.clearAllScrollViewChildren();
        GraphModel model = lastModel;
        if (model == null) {
            return;
        }
        for (VariableDeclarationModelBase var : model.getGraphVariableModels()) {
            if (var == null) continue;
            if (!filterText.isEmpty() && !var.getName().toLowerCase().contains(filterText)) {
                continue;
            }
            table.addScrollViewChild(buildRow(model, var));
        }
    }

    private UIElement buildRow(GraphModel model, VariableDeclarationModelBase var) {
        boolean interfaceVar = var.isInputOrOutput();
        UIElement row = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .height(14)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(2));

        // 名字（改名经声明引用自动同步图内变量节点）
        TextField name = new TextField();
        name.textFieldStyle(style -> style.fontSize(9));
        name.setText(var.getName(), false);
        name.setTextResponder(text -> {
            String newName = text.trim();
            GraphView view = currentView();
            if (view != null && !newName.isEmpty() && !newName.equals(var.getName())) {
                EvmUndo.push(view, model, currentContext(), "重命名变量",
                        "var:name:" + var.getUid(), () -> var.setName(newName));
            }
        });
        name.layout(layout -> layout.width(76).heightPercent(100));

        // 类型（同内建黑板属性面板的直连语义）；候选显示用编辑器类型名而非 handle 原文
        Selector<TypeHandle> type = new Selector<>();
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
                        "var:type:" + var.getUid(), () -> var.setDataTypeHandle(handle));
            }
        });
        type.layout(layout -> layout.width(62).heightPercent(100));

        // 作用域：接口变量只读；LOCAL 变量循环切换 temp/variable（写侧表）
        UIElement scopeCell;
        if (interfaceVar) {
            scopeCell = WorkbenchWidgets.textLine(var.isInput() ? "in" : "out", WorkbenchColors.DIM)
                    .layout(layout -> layout.width(44).heightPercent(100));
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
            scopeButton.layout(layout -> layout.width(44).heightPercent(100));
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
        defaultField.layout(layout -> layout.width(56).heightPercent(100));

        // 引用数（绑定该声明的变量节点）
        int refs = 0;
        for (var nodeModel : model.getNodeModels()) {
            if (nodeModel instanceof VariableNodeModel variableNode
                    && variableNode.getVariableDeclarationModel() == var) {
                refs++;
            }
        }
        UIElement refsLabel = WorkbenchWidgets.textLine(String.valueOf(refs), WorkbenchColors.DIM)
                .layout(layout -> layout.width(22).heightPercent(100));

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

        row.addChildren(name, type, scopeCell, defaultField, refsLabel);
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
            });
            delete.layout(layout -> layout.width(16).heightPercent(100));
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

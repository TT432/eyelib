package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib2;
//? if !legacy {
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import dev.vfyjxf.taffy.style.FlexDirection;
import io.github.tt432.eyelib.client.molangdebug.MolangDebugService;
import io.github.tt432.eyelib.client.molangdebug.MolangDebugService.DebugTarget;
import io.github.tt432.eyelib.client.molangdebug.MolangDebugService.EvalResult;
import io.github.tt432.eyelib.client.molangdebug.MolangDebugService.ScopeVar;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Molang 调试侧栏（规格 §W3，语义与 ldlib1 工作台对齐）：
 * 目标实体列表（单击选中，无 scope 目标降色标注）+ scope 变量表（parent 链上层 ↑n 降色）
 * + watch 表达式列表（行尾 [x] 删除，显示类型 auto/float/int/bool/string 循环）。
 *
 * <p>数据每 client tick 经 {@link #screenTick()} 刷新（scope 值经渲染链求值，20Hz 与
 * 游戏刻同频）；所有 MC/molang 访问都在 {@link MolangDebugService}。选中目标经
 * {@code targetListener} 同步给画布徽标模型（{@code NodeDebugOverlayModel#setTarget}）。
 */
final class DebugPanel extends UIElement {
    /** 侧栏展开宽度。 */
    static final int WIDTH = 260;

    /** watch 显示类型选项标签；下标 0 = auto（运行时推断），其余对应 {@link #DISPLAY_TYPES}。 */
    private static final String[] DISPLAY_TYPE_LABELS = {"auto", "float", "int", "bool", "string"};
    private static final MolangType[] DISPLAY_TYPES =
            {MolangType.FLOAT, MolangType.INT, MolangType.BOOL, MolangType.STRING};

    /**
     * 已格式化的表格行（名称/类型/值三列）。
     *
     * @param parentLayer 是否来自 scope parent 链上层（降色标注）
     */
    private record TableRow(String name, String type, String value, boolean error, boolean parentLayer) {
    }

    /** 一条 watch 表达式；displayTypeIndex 见 {@link #DISPLAY_TYPE_LABELS}。 */
    private record WatchEntry(String expression, int displayTypeIndex) {
    }

    private final Consumer<@Nullable Entity> targetListener;
    private final TextElement statusLine;
    private final TextField filterField;
    private final ScrollerView targetList;
    private final ScrollerView scopeTable;
    private final TextField watchInput;
    private final Button displayTypeButton;
    private final ScrollerView watchList;

    private List<DebugTarget> allTargets = List.of();
    private String filterText = "";
    private @Nullable DebugTarget selected;
    private int displayTypeIndex;
    private final List<WatchEntry> watches = new ArrayList<>();

    DebugPanel(Consumer<@Nullable Entity> targetListener) {
        this.targetListener = targetListener;
        layout(layout -> layout
                .width(WIDTH)
                .heightPercent(100)
                .paddingAll(4)
                .gapAll(2));
        WorkbenchWidgets.panelBackground(this);

        statusLine = WorkbenchWidgets.textLine("未选择调试目标", WorkbenchColors.DIM);
        statusLine.layout(layout -> layout.widthPercent(100));

        // 目标过滤 + 刷新
        UIElement targetBar = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .height(14)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(2));
        filterField = new TextField();
        filterField.textFieldStyle(style -> style.placeholder(Component.literal("过滤实体…")));
        filterField.setTextResponder(text -> {
            filterText = text;
            applyFilter();
        });
        filterField.layout(layout -> layout.flex(1).heightPercent(100));
        Button refreshButton = new Button();
        refreshButton.setText(Component.literal("刷新"));
        refreshButton.textStyle(style -> style.fontSize(9));
        refreshButton.setOnClick(event -> refreshTargets());
        refreshButton.layout(layout -> layout.width(34).heightPercent(100));
        targetBar.addChildren(filterField, refreshButton);

        targetList = new ScrollerView();
        targetList.layout(layout -> layout.widthPercent(100).height(96));

        scopeTable = new ScrollerView();
        scopeTable.layout(layout -> layout.widthPercent(100).flex(1));

        // watch 输入行：表达式 + 显示类型循环 + 添加
        UIElement watchBar = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .height(14)
                        .flexDirection(FlexDirection.ROW)
                        .gapAll(2));
        watchInput = new TextField();
        watchInput.textFieldStyle(style -> style.placeholder(Component.literal("molang 表达式，如 query.health")));
        watchInput.layout(layout -> layout.flex(1).heightPercent(100));
        displayTypeButton = new Button();
        displayTypeButton.setText(Component.literal(DISPLAY_TYPE_LABELS[0]));
        displayTypeButton.textStyle(style -> style.fontSize(9));
        displayTypeButton.setOnClick(event -> {
            displayTypeIndex = (displayTypeIndex + 1) % DISPLAY_TYPE_LABELS.length;
            displayTypeButton.setText(Component.literal(DISPLAY_TYPE_LABELS[displayTypeIndex]));
        });
        displayTypeButton.layout(layout -> layout.width(38).heightPercent(100));
        Button addWatchButton = new Button();
        addWatchButton.setText(Component.literal("添加"));
        addWatchButton.textStyle(style -> style.fontSize(9));
        addWatchButton.setOnClick(event -> addWatch());
        addWatchButton.layout(layout -> layout.width(34).heightPercent(100));
        watchBar.addChildren(watchInput, displayTypeButton, addWatchButton);

        watchList = new ScrollerView();
        watchList.layout(layout -> layout.widthPercent(100).height(84));

        addChildren(
                WorkbenchWidgets.sectionTitle("Molang 调试"),
                statusLine,
                targetBar,
                targetList,
                WorkbenchWidgets.sectionTitle("scope"),
                scopeTable,
                watchBar,
                watchList);

        refreshTargets();
    }

    // ==================== 刷新 ====================

    @Override
    public void screenTick() {
        super.screenTick();
        // 徽标模型目标同步：目标死亡时置 null（徽标显示「—」）；setTarget 引用去重，每 tick 调用零开销
        DebugTarget sel = selected;
        targetListener.accept(sel != null && MolangDebugService.isAlive(sel.entity()) ? sel.entity() : null);
        refreshStatus();
        refreshScopeTable();
        refreshWatchList();
    }

    private void refreshStatus() {
        DebugTarget sel = selected;
        boolean alive = sel != null && MolangDebugService.isAlive(sel.entity());
        if (sel == null) {
            setStatus("未选择调试目标（从列表选择实体）", WorkbenchColors.DIM);
        } else if (!alive) {
            setStatus("目标实体已被移除: " + sel.displayName(), WorkbenchColors.ERROR);
        } else {
            setStatus("目标: " + sel.displayName(), WorkbenchColors.TEXT);
        }
    }

    private void setStatus(String text, int color) {
        statusLine.setText(Component.literal(text));
        statusLine.textStyle(style -> style.textColor(color));
    }

    /** 重新枚举世界中的调试目标；选中引用是实体实例，重列后自然保持。 */
    private void refreshTargets() {
        allTargets = MolangDebugService.listTargets();
        applyFilter();
    }

    private void applyFilter() {
        String needle = filterText.toLowerCase(Locale.ROOT);
        targetList.clearAllScrollViewChildren();
        for (DebugTarget target : allTargets) {
            if (!needle.isEmpty()
                    && !target.displayName().toLowerCase(Locale.ROOT).contains(needle)
                    && !target.typeId().toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            boolean isSelected = selected != null && selected.entity() == target.entity();
            String label = target.hasScope() ? target.displayName() : target.displayName() + "（无 scope）";
            int color = target.hasScope() ? WorkbenchColors.TEXT : WorkbenchColors.DIM;
            targetList.addScrollViewChild(WorkbenchWidgets.rowButton(label, color, isSelected,
                    () -> selectTarget(target)));
        }
    }

    private void selectTarget(DebugTarget target) {
        selected = target;
        targetListener.accept(target.entity());
        applyFilter();
    }

    /** scope 变量表（每 tick 快照；无目标/无 scope 时优雅降级，同独立屏幕）。 */
    private void refreshScopeTable() {
        DebugTarget sel = selected;
        boolean alive = sel != null && MolangDebugService.isAlive(sel.entity());
        MolangScope scope = (sel != null && alive) ? MolangDebugService.resolveScope(sel.entity()) : null;
        scopeTable.clearAllScrollViewChildren();
        if (sel == null) {
            return;
        }
        if (scope == null) {
            scopeTable.addScrollViewChild(WorkbenchWidgets.textLine(
                    "无 molang 上下文（该实体未挂 eyelib 渲染数据）", WorkbenchColors.DIM));
            return;
        }
        for (ScopeVar var : MolangDebugService.snapshotScope(scope)) {
            scopeTable.addScrollViewChild(tableRow(formatScopeVar(var)));
        }
    }

    /** watch 求值（每 tick，渲染线程）。 */
    private void refreshWatchList() {
        DebugTarget sel = selected;
        boolean alive = sel != null && MolangDebugService.isAlive(sel.entity());
        MolangScope scope = (sel != null && alive) ? MolangDebugService.resolveScope(sel.entity()) : null;
        watchList.clearAllScrollViewChildren();
        for (int i = 0; i < watches.size(); i++) {
            TableRow row = evalWatch(watches.get(i), scope, sel != null && alive);
            watchList.addScrollViewChild(watchRow(row, i));
        }
    }

    // ==================== 数据操作 ====================

    private void addWatch() {
        String expression = watchInput.getValue().trim();
        if (expression.isEmpty()) {
            return;
        }
        watches.add(new WatchEntry(expression, displayTypeIndex));
        watchInput.setText("");
    }

    private void removeWatch(int index) {
        if (index >= 0 && index < watches.size()) {
            watches.remove(index);
        }
    }

    // ==================== 行构造与格式化 ====================

    /** 三列表格行（名称 42% / 类型 24% / 值 34%），配色同独立屏幕。 */
    private static UIElement tableRow(TableRow row) {
        UIElement element = new UIElement()
                .layout(layout -> layout
                        .widthPercent(100)
                        .height(WorkbenchWidgets.ROW_HEIGHT)
                        .flexDirection(FlexDirection.ROW));
        element.addChildren(
                cell(row.name(), row.parentLayer() ? WorkbenchColors.DIM : WorkbenchColors.TEXT, 42),
                cell(row.type(), WorkbenchColors.DIM, 24),
                cell(row.value(), row.error() ? WorkbenchColors.ERROR : WorkbenchColors.TEXT, 34));
        return element;
    }

    /** watch 行：三列 + 行尾 [x] 删除按钮。 */
    private UIElement watchRow(TableRow row, int index) {
        UIElement element = tableRow(row);
        Button delete = new Button();
        delete.setText(Component.literal("x"));
        delete.textStyle(style -> style.fontSize(9).textColor(WorkbenchColors.DIM));
        delete.setOnClick(event -> removeWatch(index));
        delete.layout(layout -> layout.width(12).heightPercent(100));
        element.addChild(delete);
        return element;
    }

    private static TextElement cell(String text, int color, int widthPercent) {
        TextElement element = WorkbenchWidgets.textLine(text, color);
        element.layout(layout -> layout.widthPercent(widthPercent));
        return element;
    }

    private static MolangType resolveDisplayType(int typeIndex, @Nullable MolangObject value) {
        return typeIndex == 0 ? MolangType.infer(value) : DISPLAY_TYPES[typeIndex - 1];
    }

    /** scope 变量 → 表格行；parent 链上层的键加 ↑n 前缀并降色标注来源层。 */
    private static TableRow formatScopeVar(ScopeVar var) {
        MolangType type = MolangType.infer(var.value());
        String name = var.depth() == 0 ? var.name() : "↑" + var.depth() + " " + var.name();
        return new TableRow(name, type.displayName(), type.format(var.value()), false, var.depth() > 0);
    }

    /** 对当前目标 scope 求值一条 watch；无目标/无 scope 时优雅降级为提示文本。 */
    private static TableRow evalWatch(WatchEntry watch, @Nullable MolangScope scope, boolean targetReady) {
        if (!targetReady) {
            return new TableRow(watch.expression, "-", "无调试目标", true, false);
        }
        if (scope == null) {
            return new TableRow(watch.expression, "-", "无 molang 上下文", true, false);
        }
        EvalResult result = MolangDebugService.eval(scope, watch.expression());
        if (!result.success()) {
            return new TableRow(watch.expression, "-", String.valueOf(result.error()), true, false);
        }
        MolangType type = resolveDisplayType(watch.displayTypeIndex(), result.value());
        return new TableRow(watch.expression, type.displayName(), type.format(result.value()), false, false);
    }
}
//?}

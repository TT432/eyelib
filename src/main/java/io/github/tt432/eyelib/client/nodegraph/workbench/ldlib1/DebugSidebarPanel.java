//? if <1.20.6 {

package io.github.tt432.eyelib.client.nodegraph.workbench.ldlib1;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import io.github.tt432.eyelib.client.molangdebug.MolangDebugService;
import io.github.tt432.eyelib.client.nodegraph.workbench.NodeDebugOverlayModel;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangType;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 调试侧栏（规格 §W3，LDLib1 薄壳）：目标实体列表（过滤 + 刷新，选中即
 * {@link NodeDebugOverlayModel#setTarget}）、watch 表达式（添加/删除 + 逐帧值）、
 * scope 变量表。数据走 {@link MolangDebugService}。
 */
public final class DebugSidebarPanel extends WidgetGroup {
    private final NodeDebugOverlayModel overlay;
    private final TextFieldWidget watchInput;
    private final WidgetGroup watchRows;

    private List<MolangDebugService.DebugTarget> targets = List.of();
    private String targetFilter = "";
    private final List<String> watches = new ArrayList<>();

    public DebugSidebarPanel(int x, int y, int width, int height, NodeDebugOverlayModel overlay) {
        super(x, y, width, height);
        this.overlay = overlay;
        setClientSideWidget();
        setBackground(new ColorRectTexture(0xF0141414));

        int inner = width - 8;
        addWidget(new LabelWidget(4, 6, "目标实体"));
        addWidget(new ButtonWidget(width - 48, 4, 44, 13, new TextTexture("刷新"),
                cd -> refreshTargets()));
        TextFieldWidget targetFilterField = new TextFieldWidget(4, 20, inner, 13, null,
                text -> targetFilter = text == null ? "" : text);
        targetFilterField.setHoverTooltips("过滤目标");
        addWidget(targetFilterField);

        int targetListHeight = Math.max(48, height / 5);
        addWidget(new ListRowView(4, 36, inner, targetListHeight, this::targetRows,
                row -> selectTarget(row.id())));

        int watchY = 40 + targetListHeight;
        addWidget(new LabelWidget(4, watchY + 2, "watch 表达式"));
        watchInput = new TextFieldWidget(4, watchY + 14, inner - 48, 13, null, text -> {
        });
        watchInput.setHoverTooltips("输入 molang 表达式后点「添加」");
        addWidget(watchInput);
        addWidget(new ButtonWidget(width - 48, watchY + 14, 44, 13, new TextTexture("添加"),
                cd -> addWatch()));

        int watchRowsHeight = Math.max(30, height / 5);
        watchRows = new WidgetGroup(4, watchY + 30, inner, watchRowsHeight);
        addWidget(watchRows);

        int scopeY = watchY + 34 + watchRowsHeight;
        addWidget(new LabelWidget(4, scopeY, "scope 变量"));
        addWidget(new ScrollableTextView(4, scopeY + 12, inner, Math.max(20, height - scopeY - 16),
                this::scopeLines));

        refreshTargets();
    }

    // ---------- 目标 ----------

    private void refreshTargets() {
        targets = MolangDebugService.listTargets();
    }

    private List<ListRowView.Row> targetRows() {
        String needle = targetFilter.toLowerCase(Locale.ROOT);
        return targets.stream()
                .filter(t -> needle.isEmpty()
                        || t.displayName().toLowerCase(Locale.ROOT).contains(needle))
                .map(t -> new ListRowView.Row(t.displayName(), t.displayName() + (t.hasScope() ? " ●" : "")))
                .toList();
    }

    private void selectTarget(String displayName) {
        for (MolangDebugService.DebugTarget target : targets) {
            if (target.displayName().equals(displayName)) {
                overlay.setTarget(target.entity());
                return;
            }
        }
    }

    // ---------- watch ----------

    private void addWatch() {
        String expression = watchInput.getCurrentString().trim();
        if (expression.isEmpty()) {
            return;
        }
        watches.add(expression);
        watchInput.setCurrentString("");
        rebuildWatchRows();
    }

    private void rebuildWatchRows() {
        watchRows.clearAllWidgets();
        for (int i = 0; i < watches.size(); i++) {
            int index = i;
            String expression = watches.get(i);
            watchRows.addWidget(new ButtonWidget(0, i * 14, 14, 12, new TextTexture("×"),
                    cd -> removeWatch(index)));
            watchRows.addWidget(new LabelWidget(18, i * 14 + 2, () -> watchValue(expression)));
        }
    }

    private void removeWatch(int index) {
        if (index >= 0 && index < watches.size()) {
            watches.remove(index);
            rebuildWatchRows();
        }
    }

    /** 每帧经 LabelWidget supplier 求值（编译缓存去重，与独立屏幕同路径）。 */
    private String watchValue(String expression) {
        Entity target = overlay.target();
        if (target == null) {
            return expression + " = —";
        }
        MolangScope scope = MolangDebugService.resolveScope(target);
        if (scope == null) {
            return expression + " = —";
        }
        MolangDebugService.EvalResult result = MolangDebugService.eval(scope, expression);
        if (result.success()) {
            MolangObject value = result.value();
            return expression + " = " + MolangType.infer(value).format(value);
        }
        return expression + " ! " + result.error();
    }

    // ---------- scope ----------

    private List<List<JsonColors.Segment>> scopeLines() {
        Entity target = overlay.target();
        if (target == null) {
            return singleLine("（未选择目标）", MolangTypeColors.DYNAMIC);
        }
        if (!MolangDebugService.isAlive(target)) {
            return singleLine("（目标已移除）", MolangTypeColors.ERROR);
        }
        MolangScope scope = MolangDebugService.resolveScope(target);
        if (scope == null) {
            return singleLine("（无 molang 上下文）", MolangTypeColors.DYNAMIC);
        }
        List<List<JsonColors.Segment>> lines = new ArrayList<>();
        for (MolangDebugService.ScopeVar var : MolangDebugService.snapshotScope(scope)) {
            MolangType type = MolangType.infer(var.value());
            lines.add(List.of(
                    new JsonColors.Segment("  ".repeat(var.depth()) + var.name(), JsonColors.KEY),
                    new JsonColors.Segment(" = ", JsonColors.PUNCT),
                    new JsonColors.Segment(type.format(var.value()), MolangTypeColors.of(type))));
        }
        if (lines.isEmpty()) {
            return singleLine("（空 scope）", MolangTypeColors.DYNAMIC);
        }
        return lines;
    }

    private static List<List<JsonColors.Segment>> singleLine(String text, int color) {
        return List.of(List.of(new JsonColors.Segment(text, color)));
    }
}
//?}
